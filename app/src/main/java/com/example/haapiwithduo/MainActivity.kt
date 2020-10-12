package com.example.haapiwithduo

import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.util.Function
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet.WRAP_CONTENT
import androidx.core.content.ContextCompat.getSystemService
import com.example.haapiwithduo.utils.disableSslTrustVerification
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import se.curity.identityserver.haapi.android.sdk.HaapiTokenManager
import se.curity.identityserver.haapi.android.sdk.okhttp.OkHttpUtils.addHaapiInterceptor
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import kotlin.math.log

private val baseUrl = "https://47aea92fde13.ngrok.io"
private val haapiTokenManager = HaapiTokenManager(
//        URI("$baseUrl/dev/oauth/token"),
    URI("$baseUrl/oauth/v2/oauth-token"),
//        "michal-test-www"
    "haapi-public-client"
)

class MainActivity : AppCompatActivity() {

    private val fieldsList = HashMap<Int, HashMap<Int, Pair<String, Function<View, String>>>>()


    private val httpClient = OkHttpClient.Builder()
        .addHaapiInterceptor(haapiTokenManager)
        .disableSslTrustVerification()
        .build()

    private val marginParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)

    init {
        marginParams.topMargin = 20
        marginParams.bottomMargin = 20
    }

    private fun logAppInfo()  {
        val packageInfo =
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signatures =  packageInfo.signingInfo.apkContentsSigners.map { SHA256(it.toByteArray()) }
        Log.d("PackageName", packageName)
        Log.d("AppInfo", "APK signatures $signatures")
    }

    private fun SHA256(bytes: ByteArray): String =
        Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes), Base64.DEFAULT)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
logAppInfo()
    }

    fun startLogin(view: View) {
        val authorizeUrl = Uri.Builder()
            .scheme("https")
            .authority("47aea92fde13.ngrok.io")

//            .appendPath("dev")
//            .appendPath("oauth")
//            .appendPath("authorize")

            .appendPath("oauth")
            .appendPath("v2")
            .appendPath("oauth-authorize")

//            .appendQueryParameter("client_id", "michal-test-www")
            .appendQueryParameter("client_id", "haapi-public-client")
            .appendQueryParameter("state", "1586511942384-OcG")
            .appendQueryParameter("scope", "openid read")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge", "ERNHshyzhznDQOKAIEkJl94N048wMAaN4jY-2xlVy_s")
            .appendQueryParameter("code_challenge_method", "S256")
//            .appendQueryParameter("redirect_uri", "https://oauth.tools/callback/code")
            .appendQueryParameter("redirect_uri", "https://localhost:7777/client-callback")
            .build()
            .toString()
        val request = Request.Builder()
            .get()
            .url(authorizeUrl)
            .build()

        view.post {
            view.visibility = GONE
        }

        val authenticatorLayout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        val selectorLayout = findViewById<LinearLayout>(R.id.selectorsLayout)

        authenticatorLayout.post {
            authenticatorLayout.removeAllViews()
        }

        selectorLayout.post {
            selectorLayout.removeAllViews()
        }

        callApi(request)
    }

    private fun processHaapiResponse(haapiResponse: Response) {
        val responseBody = haapiResponse.body?.string() ?: "{}"
        val haapiResponseObject = JSONObject(responseBody)

        //TODO just for now
        showJSONResponse(responseBody)

        when (haapiResponseObject["type"]) {
            "authentication-step" -> processAuthenticationStep(haapiResponseObject.getJSONArray("actions"))
            "polling-status" -> processPollingStatus(haapiResponseObject)
            "oauth-authorization-response" -> processAuthorizationResponse(haapiResponseObject.getJSONObject("properties"))
            else -> return
        }
    }

    private fun processPollingStatus(haapiResponseObject: JSONObject) {
        val properties = haapiResponseObject.getJSONObject("properties")

        when (properties["status"] as Boolean) {
            true -> processAuthenticationStep(haapiResponseObject.getJSONArray("actions"))
            false -> pollStatus(haapiResponseObject)
        }
    }

    private fun pollStatus(haapiResponseObject: JSONObject) {
        val links = haapiResponseObject.getJSONArray("links")

        val link = links.getJSONObject(0)

        val request = Request.Builder()
            .url(link["href"].toString())
            .get()
            .build()

        Handler(Looper.getMainLooper()).postDelayed({
            callApi(request)
        }, 3000)
    }

    private fun processAuthenticationStep(actions: JSONArray) {

        for (i in (0 until actions.length())) {
            val action = actions.getJSONObject(i)

            when (action["template"]) {
                "form" -> processHaapiForm(action)
                "selector" -> processSelector(action)
                else -> return
            }
        }
    }

    private fun processHaapiForm(form: JSONObject) {
        when (form["kind"]) {
            "redirect" -> processHaapiRedirect(form.getJSONObject("model"))
            "form" -> processForm(form.getJSONObject("model"))
            "device-option" -> processDeviceOption(form.getJSONObject("model"))
            else -> return
        }
    }

    private fun processHaapiRedirect(haapiResponseModel: JSONObject) {
        val href = haapiResponseModel["href"].toString()
        val url = if (href.startsWith("http")) href else "$baseUrl$href"

        val request = Request.Builder()
            .method(haapiResponseModel["method"].toString(), getRedirectBody(haapiResponseModel))
            .url(url)
            .build()
        callApi(request)
    }

    private fun getRedirectBody(haapiResponseModel: JSONObject): RequestBody? {
        if (!haapiResponseModel.has("fields")) {
            return null
        }

        val fields = haapiResponseModel.getJSONArray("fields")
        val requestBodyString = StringBuilder()

        for (i in (0 until fields.length())) {
            val field = fields.getJSONObject(i)
            requestBodyString.append("&")
                .append(field["name"])
                .append("=")
                .append(field["value"])
        }

        return requestBodyString.toString().substring(1).toRequestBody(
            haapiResponseModel["type"].toString().toMediaType()
        )
    }

    private fun processSelector(selectorAction: JSONObject) {
        when (selectorAction["kind"]) {
            "selector-authenticator" -> processSelectorAuthenticator(selectorAction.getJSONObject("model"))
            else -> return
        }
    }

    private fun processSelectorAuthenticator(selectorActionModel: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.selectorsLayout)
        layout.post {
            layout.addView(generateHeader(selectorActionModel["title"].toString()))

            val options = selectorActionModel.getJSONArray("options")

            for (i in (0 until options.length())) {
                val selectorOptions = options.getJSONObject(i)
                val selector = Button(this)
                selector.id = generateViewId()
                selector.text = selectorOptions["title"].toString()
                selector.visibility = VISIBLE
                selector.layoutParams = marginParams
                selector.backgroundTintList = ColorStateList.valueOf(getColor(R.color.button))
                selector.setTextColor(getColor(R.color.button_txt))
                selector.setOnClickListener {
                    layout.post {
                        layout.visibility = GONE
                    }

                    selectAuthenticator(selectorOptions["href"].toString())
                }

                layout.addView(selector)
            }

            layout.visibility = VISIBLE
        }
    }

    private fun processForm(form: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        layout.post {
            layout.addView(generateHeader(form["title"].toString()))

            val fieldsArray = form.getJSONArray("fields")
            val fields = HashMap<Int, String>(fieldsArray.length())

            val paramsForFields = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            paramsForFields.setMargins(50, 20, 50, 20)

            for (i in (0 until fieldsArray.length())) {
                val fieldModel = fieldsArray.getJSONObject(i)
                val field = EditText(this)
                field.id = generateViewId()
                fields[field.id] = fieldModel["name"].toString()

                field.layoutParams = paramsForFields
                field.hint = fieldModel["label"].toString()
                field.onFocusChangeListener = FocusChangeListener()

                if (fieldModel["type"].toString() == "password") {
                    field.inputType = InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD or InputType.TYPE_CLASS_TEXT
                }

                layout.addView(field)
            }

            val submitButton = Button(this)
            submitButton.text = form["actionTitle"].toString()
            submitButton.layoutParams = marginParams
            submitButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.button))
            submitButton.setTextColor(getColor(R.color.button_txt))
            submitButton.setOnClickListener {
                val request = Request.Builder()
                    .method(form["method"].toString(), getBody(fields, form["type"].toString()))
                    .url("$baseUrl${form["href"]}")
                    .build()

                layout.post {
                    layout.removeAllViews()
                }

                callApi(request)
            }

            layout.addView(submitButton)
            layout.visibility = VISIBLE
        }

    }

    private fun getBody(fields: Map<Int, String>, mediaType: String): RequestBody {
        val requestString = StringBuilder()
        fields.forEach { entry ->
            val field = findViewById<EditText>(entry.key)
            requestString
                .append("&")
                .append(entry.value)
                .append("=")
                .append(field.text)
        }

        return requestString.toString().substring(1).toRequestBody(mediaType.toMediaType())
    }

    private fun selectAuthenticator(url: String) {
        val request = Request.Builder()
            .get()
            .url("$baseUrl$url")
            .build()
        callApi(request)
    }

    private fun callApi(request: Request): CompletableFuture<Void> {
        val loader = findViewById<ProgressBar>(R.id.loader)
        loader.post {
            loader.visibility = VISIBLE
        }

        return doCallApi(request).thenAccept { response ->
            loader.post {
                loader.visibility = GONE
            }
            processHaapiResponse(response)
        }
    }

    private fun doCallApi(request: Request): CompletableFuture<Response> {
        return CompletableFuture.supplyAsync {
            httpClient.newCall(request).execute()
        }
    }

    private fun showJSONResponse(response: String) {
        val view = findViewById<TextView>(R.id.api_response)
        val animation: Animation = AlphaAnimation(1f, 0f)
        animation.duration = 500
        animation.interpolator = LinearInterpolator()
        animation.repeatCount = 1
        animation.repeatMode = Animation.REVERSE

        view.post {
            view.text = response
            view.visibility = VISIBLE
            view.startAnimation(animation)
        }
    }

    private fun generateHeader(text: String): TextView {
        val selectorHeader = TextView(this)
        selectorHeader.id = generateViewId()
        selectorHeader.text = text
        selectorHeader.textAlignment = TEXT_ALIGNMENT_CENTER
        selectorHeader.layoutParams = marginParams

        return selectorHeader
    }

    private fun processDeviceOption(formModel: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.authenticatorLayout)

        layout.post {
            val form = LinearLayout(this)
            form.id = generateViewId()

            form.addView(generateHeader(formModel["title"].toString()))

            val fields = formModel.getJSONArray("fields")
            fieldsList[form.id] = HashMap()

            for (i in (0 until fields.length())) {
                val field = fields.getJSONObject(i)

                when (field["type"].toString()) {
                    "submit" -> layout.addView(
                        generateSubmitButton(
                            field["label"].toString(),
                            form,
                            formModel["href"].toString(),
                            formModel["method"].toString(),
                            formModel["type"].toString(),
                            field["value"].toString(),
                            field["name"].toString(),
                            form.id,
                            layout
                        )
                    )
                    "select" -> layout.addView(
                        generateSelector(
                            field["name"].toString(), form.id, field.getJSONArray(
                                "options"
                            )
                        )
                    )
                    "text" -> layout.addView(
                        generateInput(
                            field["label"].toString(),
                            field["name"].toString(),
                            form.id
                        )
                    )
                }
            }

            layout.addView(form)
            layout.visibility = VISIBLE
        }
    }

    private fun generateSubmitButton(
        label: String,
        parent: ViewGroup,
        url: String,
        method: String,
        contentType: String,
        value: String,
        name: String,
        formId: Int,
        layout: LinearLayout
    ): Button {
        val button = Button(this)
        button.id = generateViewId()
        button.text = label
        button.layoutParams = marginParams
        button.backgroundTintList = ColorStateList.valueOf(getColor(R.color.button))
        button.setTextColor(getColor(R.color.button_txt))
        button.setOnClickListener {
            val request = Request.Builder()
                .url("$baseUrl$url")
                .method(method, getBodyForRequest(fieldsList[formId]!!, contentType))
                .build()

            layout.post {
                layout.removeAllViews()
            }

            callApi(request)
        }

        val hiddenText = TextView(this)
        hiddenText.visibility = GONE
        hiddenText.text = value
        hiddenText.id = generateViewId()
        parent.addView(hiddenText)

        fieldsList[formId]!![hiddenText.id] = Pair(
            name,
            Function<View, String> { textView -> (textView as TextView).text.toString() })

        return button
    }

    private fun generateSelector(name: String, formId: Int, options: JSONArray): RadioGroup {
        val radioGroup = RadioGroup(this)
        radioGroup.layoutParams = marginParams
        radioGroup.id = generateViewId()

        for (i in (0 until options.length())) {
            val option = options.getJSONObject(i)
            val radioButton = RadioButton(this)
            radioButton.id = generateViewId()
            radioButton.text = option["label"].toString()
            radioButton.tag = option["value"]
            if (i == 0) {
                radioButton.isChecked = true
            }
            radioGroup.addView(radioButton)
        }

        fieldsList[formId]!![radioGroup.id] = Pair(name, Function<View, String> { view ->
            val checkedRadioId = (view as RadioGroup).checkedRadioButtonId

            val checkedRadioButton = findViewById<RadioButton>(checkedRadioId)

            return@Function checkedRadioButton.tag.toString()

        })

        return radioGroup
    }

    private fun generateInput(hint: String, name: String, formId: Int): EditText {
        val widget = EditText(this)
        widget.id = generateViewId()
        widget.layoutParams = marginParams
        widget.hint = hint
        widget.onFocusChangeListener = FocusChangeListener()

        fieldsList[formId]!![widget.id] = Pair(
            name,
            Function<View, String> { view -> (view as EditText).text.toString() })

        return widget
    }

    private fun getBodyForRequest(
        fieldsList: Map<Int, Pair<String, Function<View, String>>>,
        contentType: String
    ): RequestBody {
        val requestBodyString = StringBuilder()

        fieldsList.forEach { entry ->
            val viewField = findViewById<View>(entry.key)
            requestBodyString.append("&")
                .append(entry.value.first)
                .append("=")
                .append(entry.value.second.apply(viewField))
        }

        return requestBodyString.toString().substring(1).toRequestBody(contentType.toMediaType())
    }

    private fun processAuthorizationResponse(properties: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        layout.post {
            layout.removeAllViews()
            val text = TextView(this)
            text.text = getText(R.string.code_response)
            text.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            text.layoutParams = marginParams
            layout.addView(text)

            val code = TextView(this)
            code.text = properties["code"].toString()
            code.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            text.layoutParams = marginParams
            layout.addView(code)

//            layout.addView(getLogoutButton())

            layout.visibility = VISIBLE
        }
    }

    private fun logout(view: View) {
        val logoutUrl = "https://47aea92fde13.ngrok.io/oauth/v2/oauth-session/logout"

        val request = Request.Builder()
            .get()
            .url(logoutUrl)
            .header("Accept", "text/html")
            .build()

        val authenticatorLayout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        val selectorLayout = findViewById<LinearLayout>(R.id.selectorsLayout)

        authenticatorLayout.post {
            authenticatorLayout.removeAllViews()
        }

        selectorLayout.post {
            selectorLayout.removeAllViews()
        }

        CompletableFuture.supplyAsync {
            httpClient.newCall(request).execute()
        }.thenAccept { response ->
            val responseBody = response.body?.string() ?: ""
            println("The code for logout was: ${response.code}")
            println("The response from logout: $responseBody")
        }
            .thenRun {
            val loginButton = findViewById<Button>(R.id.main_login)

            loginButton.post {
                loginButton.visibility = VISIBLE
            }
        }
    }

    private fun getLogoutButton(): Button {
        val logoutButton = Button(this)

        logoutButton.id = generateViewId()
        logoutButton.text = getText(R.string.logout_button)
        logoutButton.setOnClickListener { view -> logout(view) }

        return logoutButton
    }

    /** Util **/

    class FocusChangeListener: OnFocusChangeListener {

        private fun hideKeyboard(view: View) {
            val inputMethodManager: InputMethodManager = getSystemService(view.context, InputMethodManager::class.java)!!
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }
    }
}