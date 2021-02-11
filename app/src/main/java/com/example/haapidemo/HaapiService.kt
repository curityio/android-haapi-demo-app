package com.example.haapidemo

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.haapidemo.Configuration.Companion.baseUrl
import com.example.haapidemo.Configuration.Companion.clientId
import com.example.haapidemo.Configuration.Companion.tokenEndpoint
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import se.curity.identityserver.haapi.android.sdk.HaapiTokenManager
import se.curity.identityserver.haapi.android.sdk.okhttp.OkHttpUtils.addHaapiInterceptor
import java.net.URI
import java.util.concurrent.CompletableFuture

private val haapiTokenManager = HaapiTokenManager(
    URI("${baseUrl}/${tokenEndpoint}"),
    clientId
)

class HaapiService(
    context: Context,
    var layout: LinearLayout,
    var loaderView: ProgressBar,
    var apiResponseView: TextView
) {


    private val httpClient = OkHttpClient.Builder()
        .addHaapiInterceptor(haapiTokenManager)
        .build()

    private val haapiResponseProcessor = HaapiResponseProcessor(
        context,
        baseUrl,
        this::callApi,
        this::redraw
    )

    fun startAuthorization(authorizationUrl: String) {
        val request = Request.Builder()
            .get()
            .url(authorizationUrl)
            .build()

        callApi(request, false) { haapiResponseProcessor.processHaapiResponse(it) }
    }

    private fun callApi(request: Request, clearLayout: Boolean, nextAction: (String) -> List<List<View>>) {

        if (clearLayout) {
            layout.post {
                layout.removeAllViews()
            }
        }

        loaderView.post {
            loaderView.visibility = View.VISIBLE
        }

        doCallApi(request).thenApply { response ->
            loaderView.post {
                loaderView.visibility = View.GONE
            }
            val responseBodyString = response.body?.string() ?: "{}"
            setJSONResponse(responseBodyString)

            return@thenApply responseBodyString
        }.thenApply { responseBodyString ->
            return@thenApply nextAction(responseBodyString)
        }.thenAccept { viewsList ->
            layout.post {
                viewsList.forEach { innerViewsList -> innerViewsList.forEach { layout.addView(it) }}
            }
        }
    }

    private fun redraw(viewsList: List<View>, clearLayout: Boolean) {
        layout.post {
            if (clearLayout) {
                layout.removeAllViews()
            }

            viewsList.forEach { view ->
                layout.addView(view)
            }
        }
    }

    private fun doCallApi(request: Request): CompletableFuture<Response> {
        return CompletableFuture.supplyAsync {
            httpClient.newCall(request).execute()
        }
    }

    private fun setJSONResponse(response: String) {
        apiResponseView.post {
            apiResponseView.text = JSONObject(response).toString(4)
        }
    }
}