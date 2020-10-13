package com.example.haapiwithduo.utils

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.haapiwithduo.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

class UnusedActivity: AppCompatActivity() {
    private val httpClient = OkHttpClient()

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
                    loginButton.visibility = View.VISIBLE
                }
            }
    }

    private fun getLogoutButton(): Button {
        val logoutButton = Button(this)

        logoutButton.id = View.generateViewId()
        logoutButton.text = getText(R.string.logout_button)
        logoutButton.setOnClickListener { view -> logout(view) }

        return logoutButton
    }
}

