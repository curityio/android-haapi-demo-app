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

import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.haapidemo.Configuration.Companion.authorizationEndpoint
import com.example.haapidemo.Configuration.Companion.clientId
import com.example.haapidemo.Configuration.Companion.host
import com.example.haapidemo.Configuration.Companion.redirectUri
import com.example.haapidemo.Configuration.Companion.scopes
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    private var jsonVisible = false
    private val authorizationUrl = buildAuthorizationUrl()
    private var haapiService: HaapiService? = null

    private fun logAppInfo()  {
        // You can use this to get the signature that should be registered at the Curity Identity Server
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

        // This has to be initialized after setContentView, but only once per run.
        haapiService = HaapiService(
            this,
            findViewById(R.id.authenticatorLayout),
            findViewById(R.id.loader),
            findViewById(R.id.api_response)
        )
        logAppInfo()
    }

    fun restart(view: View) {
        val intent = intent
        finish()
        startActivity(intent)
        startLogin(view)
    }

    fun startLogin(view: View) {
        view.post {
            view.visibility = GONE
        }

        val authenticatorLayout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        authenticatorLayout.post {
            authenticatorLayout.removeAllViews()
        }

        haapiService?.startAuthorization(authorizationUrl)
    }

    fun toggleJSON(button: View) {
        val view = findViewById<TextView>(R.id.api_response)

        val animationTo = if (jsonVisible) 1600f else 0f
        jsonVisible = !jsonVisible

        ObjectAnimator.ofFloat(view, "translationY", animationTo).apply {
            duration = 500
            interpolator = if (jsonVisible) DecelerateInterpolator() else AccelerateInterpolator()
            start()
        }
    }

    private fun buildAuthorizationUrl(): String {
        val authorizeUrlBuilder = Uri.Builder()
            .scheme("https")
            .authority(host)
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)

            // TODO: this should properly generate code challenge and state
            .appendQueryParameter("code_challenge", "ERNHshyzhznDQOKAIEkJl94N048wMAaN4jY-2xlVy_s")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", "1586511942384-OcG")

        authorizationEndpoint.split("/").forEach { pathSegment -> authorizeUrlBuilder.appendPath(pathSegment) }

        return authorizeUrlBuilder.build().toString()
    }
}
