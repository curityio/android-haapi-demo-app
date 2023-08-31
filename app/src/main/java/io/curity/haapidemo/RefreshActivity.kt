package io.curity.haapidemo

/*
 *  Copyright (C) 2023 Curity AB
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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RefreshActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refresh)

        val token = intent.getStringExtra(EXTRA_REFRESH_TOKEN) ?: throw IllegalArgumentException("You need to use RefreshActivity.newIntent(...)")
        val configuration: Configuration = Json.decodeFromString(intent.getStringExtra(EXTRA_CONFIGURATION) ?: throw IllegalArgumentException("You need to use RefreshActivity.newIntent(...)"))
        refreshToken(
            token = token,
            configuration = configuration
        )
    }

    fun refreshToken(token: String, configuration: Configuration) {
        GlobalScope.launch(Dispatchers.IO) {
            val demoApp = application as DemoApplication
            val accessor = demoApp.loadAccessor(configuration)
            val result = accessor.oAuthTokenManager.refreshAccessToken(
                refreshToken = token,
                onCoroutineContext = this.coroutineContext
            )
            if (Looper.getMainLooper().isCurrentThread) {
                Log.d("MAIN", result.toString())
            } else {
                Log.d("BACKGROUND", result.toString())
            }
        }
    }

    companion object {
        private const val EXTRA_REFRESH_TOKEN = "io.curity.haapidemo.refreshActivity.extra_refresh_token"
        private const val EXTRA_CONFIGURATION = "io.curity.haapidemo.refreshActivity.extra_configuration"

        fun newIntent(context: Context,
                      refreshToken: String,
                      configuration: Configuration): Intent
        {
            val intent = Intent(context, RefreshActivity::class.java)
            intent.putExtra(EXTRA_REFRESH_TOKEN, refreshToken)
            intent.putExtra(EXTRA_CONFIGURATION, Json.encodeToString(configuration))
            return intent
        }
    }
}