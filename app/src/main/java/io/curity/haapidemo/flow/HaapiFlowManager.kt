/*
 *  Copyright (C) 2021 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.haapidemo.flow

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.curity.haapidemo.Constant
import io.curity.haapidemo.models.*
import io.curity.haapidemo.models.haapi.Link
import io.curity.haapidemo.models.haapi.actions.ActionModel
import io.curity.haapidemo.models.haapi.problems.AuthorizationProblem
import io.curity.haapidemo.models.haapi.problems.HaapiProblem
import io.curity.haapidemo.parsers.RepresentationParser
import io.curity.haapidemo.parsers.toHaapiStep
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import se.curity.identityserver.haapi.android.sdk.HaapiTokenManager
import se.curity.identityserver.haapi.android.sdk.HttpURLConnectionProvider
import se.curity.identityserver.haapi.android.sdk.UnexpectedTokenAccessException
import se.curity.identityserver.haapi.android.sdk.okhttp.OkHttpUtils.addHaapiInterceptor
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLConnection
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * `HaapiFlowManager` instances are in charge of the flow between a mobile client and a Curity identity server.
 * `HaapiFlowManager` is only using `code` flow and its goal is to return an access
 * token (the final step of the flow). Before an instance of `HaapiFlowManager` returns an access token,
 * few [HaapiStep]s will be return.
 *
 * A `HaapiFlowManager` returns a new HaapiStep by using these callers: [HaapiFlowManager.start],
 * [HaapiFlowManager.submit], [HaapiFlowManager.followLink], [HaapiFlowManager.fetchAccessToken]
 * and [HaapiFlowManager.refreshAccessToken].
 *
 * To use an instance of `HaapiFlowManager`, it is mandatory to call first [HaapiFlowManager.start] method to begin the flow.
 * Otherwise, it returns a [SystemErrorStep].
 *
 * An instance of `HaapiFlowManager` can be _closed_ via [HaapiFlowManager.close] method. It is recommended to close it
 * before "deallocating" it. When it is closed, it is not possible to use this instance anymore.
 * If you need to perform a new flow, then you need to create a new instance of `HaapiFlowManager`.
 *
 * @property [haapiFlowConfiguration] Configuration for HaapiFlowManager. With an incorrect configuration, the HaapiFlowManager
 * returns [SystemErrorStep].
 * @property dispatcher The default dispatcher is [Dispatchers.IO]. It is only recommended to change it when performing
 * unit tests.
 */
class HaapiFlowManager (
    val haapiFlowConfiguration: HaapiFlowConfiguration,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): HaapiFlowManageable, Closeable {

    private val representationParser = RepresentationParser
    private var didStart = false

    private val _liveStep = MutableLiveData<HaapiStep?>(null)
    override val liveStep: LiveData<HaapiStep?>
        get() = _liveStep

    private val haapiTokenManager: HaapiTokenManager by lazy {
        val tokenManagerBuilder = HaapiTokenManager.Builder(
            haapiFlowConfiguration.uri,
            haapiFlowConfiguration.clientId
        )
            .setKeyStoreAlias(haapiFlowConfiguration.keyStoreAlias)

        if (!haapiFlowConfiguration.isSSLTrustVerificationEnabled) {
            tokenManagerBuilder.setConnectionProvider(UNCHECKED_CONNECTION_PROVIDER)
        }

        tokenManagerBuilder.build()
    }

    private val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(dispatcher)
    }

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder().addHaapiInterceptor(haapiTokenManager)
        if (!haapiFlowConfiguration.isSSLTrustVerificationEnabled) {
            builder.disableSslTrustVerification()
        }
        builder.build()
    }

    override suspend fun start(): HaapiStep {
        didStart = true
        val authorizationURLBuilder = haapiFlowConfiguration.authorizationEndpointURI.toHttpUrl()
            .newBuilder()
            .addQueryParameter("client_id", haapiFlowConfiguration.clientId)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("redirect_uri", haapiFlowConfiguration.redirectURI)

        if (haapiFlowConfiguration.selectedScopes.isNotEmpty()) {
            val scopeParameter = haapiFlowConfiguration.selectedScopes.joinToString(" ")
            authorizationURLBuilder.addQueryParameter("scope", scopeParameter)
        }
        val authorizationURL = authorizationURLBuilder.build()
        val request = Request.Builder().get().url(authorizationURL).build()

        val newStep = requestHaapi(request)
        updateStep(newStep)
        if (haapiFlowConfiguration.followRedirect && newStep is Redirect) {
            return submitForm(newStep.action.model, emptyMap())
        }

        return newStep
    }

    override suspend fun submitForm(form: ActionModel.Form, parameters: Map<String, String>): HaapiStep {
        if (!didStart) {
            return SystemErrorStep(HaapiErrorTitle.INVALID_ACTION.title, "Cannot submitForm because the HaapiFlow did not start or a " +
                    "systemError happened.. Please use start() or reset().")
        }
        val urlBuilder = form.href.toHaapiURL(haapiFlowConfiguration.baseURLString).toHttpUrl().newBuilder()

        val httpURL = urlBuilder.build()
        val requestBuilder: Request.Builder = Request.Builder().url(httpURL)
        val request: Request

        when {
            form.method.toUpperCase() == "GET" -> {
                for (field in form.fields) {
                    if (field.value == null) {
                        Log.d("Missing", "$field has no values")
                        urlBuilder.addQueryParameter(field.name, parameters[field.name])
                    } else {
                        urlBuilder.addQueryParameter(field.name, parameters[field.name] ?: field.value)
                    }
                }
                request = requestBuilder.url(urlBuilder.build()).get().build()
            }
            form.method.toUpperCase() == "POST" -> {
                val requestBody = FormBody.Builder().also {
                    form.fields.forEach { field ->
                        it.tryAdd(field.name, parameters[field.name] ?: field.value)
                    }
                }.build()
                request = requestBuilder.post(requestBody).build()
            }
            else -> {
                val errorStep = SystemErrorStep(HaapiErrorTitle.UNEXPECTED.title, "Unsupported Method")
                updateStep(errorStep)
                return errorStep
            }
        }

        Log.d(Constant.TAG_HAAPI_REPRESENTATION, "Sending request: ${request.url} with parameters: $parameters")
        val step = requestHaapi(request)
        updateStep(step)
        if (haapiFlowConfiguration.followRedirect && step is Redirect) {
            val submittedStep = submitForm(step.action.model, emptyMap())
            updateStep(submittedStep)
            return submittedStep
        }

        if (step is AuthorizationCompleted && haapiFlowConfiguration.isAutoAuthorizationChallengedEnabled) {
            return fetchAccessToken(step.responseParameters.code!!)
        }

        return step
    }

    override suspend fun followLink(link: Link): HaapiStep {
        if (!didStart) {
            return SystemErrorStep(HaapiErrorTitle.INVALID_ACTION.title, "Cannot followLink because the HaapiFlow did not start or a " +
                    "systemError happened. Please use start() or reset().")
        }
        val linkURL = link.href
            .toHaapiURL(haapiFlowConfiguration.baseURLString)
            .toHttpUrl()
            .newBuilder()
            .build()

        val request = Request.Builder()
            .url(linkURL)
            .get()
            .build()

        val newStep = requestHaapi(request)
        updateStep(newStep)

        return newStep
    }

    override suspend fun fetchAccessToken(authorizationCode: String): HaapiStep {
        val requestBody = haapiFlowConfiguration.tokenFormBodyBuilder("authorization_code")
            .add("code", authorizationCode)
            .build()

        val request = haapiFlowConfiguration.tokenRequest(requestBody)
        val okHttpClientBuilder = OkHttpClient.Builder()
        if (!haapiFlowConfiguration.isSSLTrustVerificationEnabled) {
            okHttpClientBuilder.disableSslTrustVerification()
        }

        val newStep = requestHaapi(request, okHttpClientBuilder.build())

        updateStep(newStep)

        return newStep
    }

    override suspend fun refreshAccessToken(refreshToken: String): HaapiStep {
        val requestBody = haapiFlowConfiguration.tokenFormBodyBuilder("refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = haapiFlowConfiguration.tokenRequest(requestBody)
        val okHttpClientBuilder = OkHttpClient.Builder()
        if (!haapiFlowConfiguration.isSSLTrustVerificationEnabled) {
            okHttpClientBuilder.disableSslTrustVerification()
        }

        val newStep = requestHaapi(request, okHttpClientBuilder.build())

        updateStep(newStep)

        return newStep
    }

    override fun reset() {
        updateStep(null)
        didStart = false
    }

    /**
     * Close the HaapiTokenManager.
     *
     * When it is closed, it is impossible to perform any new actions. Creating a new HaapiFlowManager is the only way.
     */
    override fun close() {
        haapiTokenManager.clear()
        haapiTokenManager.close()
    }

    private fun updateStep(newStep: HaapiStep?) {
        _liveStep.postValue(newStep)
    }

    /**
     * Returns a new HaapiStep by sending a new Request to Haapi endpoint through httpClient
     *
     * @param request A Request
     * @param httpClient An OkHttpClient
     * @return HaapiStep
     */
    private suspend fun requestHaapi(request: Request, httpClient: OkHttpClient = this.httpClient): HaapiStep {
        return withContext(coroutineScope.coroutineContext) {
            try {
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body

                if (responseBody == null) {
                    SystemErrorStep(
                        HaapiErrorTitle.UNEXPECTED.name,
                        "ResponseBody is empty with status code ${response.code}"
                    )
                }
                val jsonObject = JSONObject(responseBody!!.string())
                val contentType = response.header("content-type")

                Log.d(Constant.TAG_HAAPI_REPRESENTATION, jsonObject.toString())
                if (response.isSuccessful) {
                    when (contentType) {
                        "application/vnd.auth+json" -> {
                            representationParser.parse(jsonObject).toHaapiStep()
                        }
                        "application/json" -> {
                            val oAuthTokenResponse = representationParser.parseAccessToken(jsonObject)
                            TokensStep(
                                oAuthTokenResponse = oAuthTokenResponse
                            )
                        }
                        else -> {
                            SystemErrorStep(
                                HaapiErrorTitle.UNEXPECTED.title, "Response was successful with " +
                                        "unsupported content-type : ${response.header("content-type")}"
                            )
                        }
                    }
                } else {
                    if (contentType == "application/problem+json") {
                        val problem = representationParser.parseProblem(jsonObject)
                        val systemErrorStep = problem.systemError()
                        systemErrorStep ?: ProblemStep(problem)
                    } else {
                        SystemErrorStep(
                            HaapiErrorTitle.UNEXPECTED.title, "Response was unsuccessful with " +
                                    "unsupported content-type : ${response.header("content-type")}"
                        )
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is IOException, is IllegalStateException -> SystemErrorStep(
                        HaapiErrorTitle.HAAPI.title,
                        e.message ?: ""
                    )
                    is UnexpectedTokenAccessException -> SystemErrorStep(
                        HaapiErrorTitle.HAAPI_DRIVER.title,
                        e.message ?: ""
                    )
                    else -> SystemErrorStep(HaapiErrorTitle.NETWORK.title, e.message ?: "")
                }
            }
        }
    }
}

//region Private extensions
/**
 * Try to add an optional value to a name in the FormBody.Builder. If the value is null, then it returns the
 * FormBody.Builder without changes. When optional, a log will be printed.
 *
 * @param name A String
 * @param value An optional String
 * @return A FormBody.Builder
 */
private fun FormBody.Builder.tryAdd(name: String, value: String?): FormBody.Builder {
    if (value == null) {
        Log.d("Debug", "$name was discarded because there was no value")
        return this
    }
    return this.add(name, value)
}

/**
 * Returns a formatted Haapi URL String
 *
 * @param baseURLString A base URL String that is valid. For example: https://www.valid.com
 * @return A formatted string URL.
 */
private fun String.toHaapiURL(baseURLString: String): String {
    return if (this.startsWith("https")) {
        this
    } else {
        "$baseURLString$this"
    }
}

/**
 * Returns a Request that is configured for tokenEndpoint
 *
 * @param requestBody A RequestBody
 * @return Request
 */
private fun HaapiFlowConfiguration.tokenRequest(requestBody: RequestBody): Request {
    return Request.Builder()
        .url(tokenEndpointURI.toHttpUrl())
        .addHeader("content-type", "application/x-www-form-urlencoded")
        .addHeader("accept", "application/json")
        .post(requestBody)
        .build()
}

/**
 * Returns a [FormBody.Builder] that is configured on HaapiFlowConfiguration (clientId)
 *
 * @return FormBody.Builder
 */
private fun HaapiFlowConfiguration.tokenFormBodyBuilder(grantType: String): FormBody.Builder {
    return FormBody.Builder()
        .add("client_id", clientId)
        .add("redirect_uri", redirectURI)
        .add("grant_type", grantType)
}
//endregion

//region Private extensions for disabling SSL Verification !!!
private val trustManager = object : X509TrustManager
{
    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(p0: Array<out X509Certificate>, p1: String)
    {
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(p0: Array<out X509Certificate>, p1: String)
    {
    }

    override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
}

fun OkHttpClient.Builder.disableSslTrustVerification(): OkHttpClient.Builder
{

    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, arrayOf(trustManager), null)
    return sslSocketFactory(sslContext.socketFactory, trustManager)
        .hostnameVerifier { _, _ -> true }
}

private fun URLConnection.disableSslTrustVerification(): URLConnection
{
    when (this)
    {
        is HttpsURLConnection ->
        {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(trustManager), null)
            this.sslSocketFactory = sslContext.socketFactory
            this.setHostnameVerifier { _, _ -> true }
        }
    }
    return this
}

private val UNCHECKED_CONNECTION_PROVIDER: HttpURLConnectionProvider = {
    it.openConnection().disableSslTrustVerification() as HttpURLConnection
}

private fun HaapiProblem.systemError(): SystemErrorStep? {
    return when (code) {
        "invalid_redirect_uri", "authorization_failed", "access_denied" -> {
            val errorTitle: String
            val description: String
            if (this is AuthorizationProblem) {
                errorTitle = error
                description = errorDescription
            } else {
                errorTitle = title
                description = messages?.joinToString { it.text.message ?: it.text.key ?: "" } ?: ""
            }
            SystemErrorStep(
                title = errorTitle,
                description = description
            )
        }
        else -> null
    }
}
//endregion