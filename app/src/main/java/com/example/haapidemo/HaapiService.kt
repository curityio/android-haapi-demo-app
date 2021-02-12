/*
 *  Copyright 2021 Curity AB
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
