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

package io.curity.haapidemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.ui.haapiflow.TokensFragment
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.curity.identityserver.haapi.android.sdk.models.oauth.SuccessfulTokenResponse

interface TokenStateChangeable {
    fun setNewTokenResponse(tokenResponse: SuccessfulTokenResponse)
    fun logout()
}

class AuthenticatedActivity : AppCompatActivity(), TokenStateChangeable {
    private lateinit var viewModel: AuthenticatedActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticated)

        val configString = intent.getStringExtra(EXTRA_AUTHENTICATED_ACTIVITY_HAAPI_CONFIG)
        val tokenResponse = intent.getParcelableExtra<SuccessfulTokenResponse>(EXTRA_AUTHENTICATED_ACTIVITY_TOKEN_RESPONSE)

        if (configString == null || tokenResponse == null) {
            throw IllegalArgumentException("You need to use AuthenticatedActivity.newIntent(...)")
        }

        val configuration: Configuration = Json.decodeFromString(configString)
        val tokensFragment = TokensFragment.newInstance(tokenResponse, configuration)
        tokensFragment.tokenStateChangeable = this

        viewModel = ViewModelProvider(this).get(AuthenticatedActivityViewModel::class.java)
        viewModel.tokenResponse = tokenResponse

        supportFragmentManager.commit {
            replace(R.id.fragment_container, tokensFragment)
        }
    }

    override fun setNewTokenResponse(tokenResponse: SuccessfulTokenResponse) {
        viewModel.tokenResponse = tokenResponse
    }

    override fun logout() {
        finish()
    }

    class AuthenticatedActivityViewModel: ViewModel() {
        var tokenResponse: SuccessfulTokenResponse? = null
    }

    companion object {

        private const val EXTRA_AUTHENTICATED_ACTIVITY_HAAPI_CONFIG = "io.curity.haapidemo.authenticatedActivity.extra_config"
        private const val EXTRA_AUTHENTICATED_ACTIVITY_TOKEN_RESPONSE = "io.curity.haapidemo.authenticatedActivity.extra_token_response"

        fun newIntent(context: Context,
                      configuration: Configuration,
                      tokenResponse: SuccessfulTokenResponse
        ): Intent {
            val intent = Intent(context, AuthenticatedActivity::class.java)
            intent.putExtra(EXTRA_AUTHENTICATED_ACTIVITY_HAAPI_CONFIG, Json.encodeToString(configuration))
            intent.putExtra(EXTRA_AUTHENTICATED_ACTIVITY_TOKEN_RESPONSE, tokenResponse)
            return intent
        }
    }
}