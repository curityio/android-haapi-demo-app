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

package io.curity.haapidemo.ui.haapiflow

import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import io.curity.haapidemo.R
import io.curity.haapidemo.TokenStateChangeable
import io.curity.haapidemo.Configuration
import io.curity.haapidemo.DemoApplication
import io.curity.haapidemo.uicomponents.DisclosureContent
import io.curity.haapidemo.uicomponents.DisclosureView
import io.curity.haapidemo.uicomponents.HeaderView
import io.curity.haapidemo.uicomponents.ProgressButton
import io.curity.haapidemo.utils.copyTextToClipboard
import io.curity.haapidemo.utils.disableSslTrustVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.curity.identityserver.haapi.android.sdk.HaapiAccessor
import se.curity.identityserver.haapi.android.sdk.models.oauth.SuccessfulTokenResponse
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URI
import io.curity.haapidemo.utils.decodedIDToken
import io.curity.haapidemo.utils.toast

class TokensFragment: Fragment(R.layout.fragment_tokens) {

    private lateinit var headerView: HeaderView
    private lateinit var userInfoTextView: TextView
    private lateinit var accessDisclosureView: DisclosureView
    private lateinit var idTokenDisclosureView: DisclosureView
    private lateinit var refreshDisclosureView: DisclosureView
    private lateinit var linearLayoutIDToken: LinearLayout

    private lateinit var refreshTokenButton: ProgressButton
    private lateinit var signOutButton: ProgressButton

    private lateinit var tokensViewModel: TokensViewModel
    var tokenStateChangeable: TokenStateChangeable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oAuthTokenResponse = requireArguments().getParcelable<SuccessfulTokenResponse>(EXTRA_OAUTH_TOKEN_RESPONSE) ?: throw IllegalStateException("Expecting a TokenResponse")
        val config: Configuration = Json.decodeFromString(requireArguments().getString(EXTRA_CONFIG) ?: throw IllegalStateException("Expecting a configuration"))
        tokensViewModel = ViewModelProvider(this, TokensViewModelFactory(this.requireActivity().application, config, oAuthTokenResponse))
            .get(TokensViewModel::class.java)
        tokensViewModel.start()
        tokensViewModel.tokenStateChangeable = tokenStateChangeable
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerView = view.findViewById(R.id.header)
        userInfoTextView = view.findViewById(R.id.userInfo)
        accessDisclosureView = view.findViewById(R.id.access_disclosure_view)
        idTokenDisclosureView = view.findViewById(R.id.id_token_view)
        refreshDisclosureView = view.findViewById(R.id.refresh_disclosure_view)
        linearLayoutIDToken = view.findViewById(R.id.linear_layout_id_token)

        refreshTokenButton = view.findViewById(R.id.refresh_button)
        signOutButton = view.findViewById(R.id.signout_button)


        headerView.setText(getString(R.string.success))

        tokensViewModel.liveTokenResponse.observe(viewLifecycleOwner, Observer {
            accessDisclosureView.setContentText(tokensViewModel.accessToken)

            val idToken = tokensViewModel.idToken
            if (idToken != null) {
                tokensViewModel.liveTokenResponse.value?.decodedIDToken()?.let {
                    idTokenDisclosureView.setContentText(idToken)
                    idTokenDisclosureView.setDecodedContent(it)
                } ?: run {
                    idTokenDisclosureView.setContentText(idToken)
                }

                linearLayoutIDToken.visibility = View.VISIBLE

                configureCopyToClipboard(textToCopy = idToken, holderView = linearLayoutIDToken)
            } else {
                linearLayoutIDToken.visibility = View.GONE
            }

            accessDisclosureView.setDisclosureContents(tokensViewModel.disclosureContents)
            configureCopyToClipboard(
                textToCopy = tokensViewModel.accessToken,
                holderView = accessDisclosureView
            )

            val refreshToken = tokensViewModel.refreshToken
            if (refreshToken != null) {
                refreshDisclosureView.visibility = View.VISIBLE
                refreshDisclosureView.setContentText(refreshToken)
                configureCopyToClipboard(
                    textToCopy = refreshToken,
                    holderView = refreshDisclosureView
                )
            } else {
                refreshDisclosureView.visibility = View.GONE
            }
        })

        tokensViewModel.liveUserInfo.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                userInfoTextView.visibility = View.GONE
            } else {
                userInfoTextView.visibility = View.VISIBLE
            }
            userInfoTextView.text = it
        })

        refreshTokenButton.setOnClickListener {
            tokensViewModel.refreshToken()
        }

        signOutButton.setOnClickListener {
            tokenStateChangeable?.logout()
            tokensViewModel.logout()
        }
    }

    private fun configureCopyToClipboard(textToCopy: CharSequence?, holderView: View) {
        val copyButton: ImageButton = holderView.findViewById(R.id.copy_clipboard)
        copyButton.setOnClickListener { _ ->
            context?.copyTextToClipboard(textToCopy ?: "")
            context?.toast(message = context?.getString(R.string.copy_to_clipboard_message) ?: "")
        }
    }

    class TokensViewModel(
        private val app: Application,
        private val configuration: Configuration,
        private var tokenResponse: SuccessfulTokenResponse
    ): AndroidViewModel(app) {

        private var _tokenResponse: MutableLiveData<SuccessfulTokenResponse> = MutableLiveData(tokenResponse)
        val liveTokenResponse: LiveData<SuccessfulTokenResponse>
            get() = _tokenResponse

        private var _liveUserInfo: MutableLiveData<String?> = MutableLiveData(null)
        val liveUserInfo: LiveData<String?>
            get() = _liveUserInfo

        var tokenStateChangeable: TokenStateChangeable? = null

        val accessToken: String
            get() = tokenResponse.accessToken
        val idToken: String?
            get() = tokenResponse.idToken
        val refreshToken: String?
            get() = tokenResponse.refreshToken

        private var _disclosureContents: MutableList<DisclosureContent> = mutableListOf()
        val disclosureContents: List<DisclosureContent>
            get() = _disclosureContents

        private var accessor: HaapiAccessor? = null
        private val uriUserInfo: URI = URI(configuration.userInfoEndpointURI)

        init {
            updateDisclosureContents()
            fetchUserInfo()
        }

        fun start() {

            viewModelScope.launch {

                try {
                    accessor = withContext(Dispatchers.IO) {
                        val demoApp = app as DemoApplication
                        demoApp.loadAccessor(configuration)
                    }
                } catch (e: Throwable) {
                    // Currently this view does not report errors so only output to the console
                    println(e)
                }
            }
        }

        private fun updateDisclosureContents() {
            val mutableDisclosureContents = mutableListOf<DisclosureContent>()
            tokenResponse.tokenType?.let {
                mutableDisclosureContents.add(
                    DisclosureContent(
                        label = "token_type",
                        description = it
                    )
                )
            }
            tokenResponse.scope?.let {
                mutableDisclosureContents.add(
                    DisclosureContent(
                        label = "scope",
                        description = it
                    )
                )
            }
            mutableDisclosureContents.add(
                DisclosureContent(
                    label = "expires_in",
                    description = tokenResponse.expiresIn.seconds.toString()
                )
            )
            _disclosureContents = mutableDisclosureContents
        }

        fun refreshToken() {
            val refreshToken = tokenResponse.refreshToken
            if (refreshToken != null) {
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        val haapiAccessor = accessor ?: throw IllegalStateException("Haapi Accessor is not initialised in refreshToken.")
                        haapiAccessor.oAuthTokenManager.refreshAccessToken(refreshToken, this.coroutineContext)
                    }
                    if (result is SuccessfulTokenResponse) {
                        tokenResponse = result
                        updateDisclosureContents()
                        fetchUserInfo()
                        _tokenResponse.postValue(result)
                        tokenStateChangeable?.setNewTokenResponse(result)
                    }
                }
            }
        }

        // Destroy accessor resources on logout
        fun logout() {
            val demoApp = app as DemoApplication
            demoApp.closeAccessor()
            accessor = null
        }

        private fun fetchUserInfo() {
            viewModelScope.launch {
                val response = withContext(Dispatchers.IO) {
                    val httpURLConnection = uriUserInfo
                        .toURL().openConnection().disableSslTrustVerification() as HttpURLConnection

                    httpURLConnection.requestMethod = "GET"
                    httpURLConnection.doInput = true
                    httpURLConnection.doOutput = false

                    httpURLConnection.setRequestProperty("Authorization", "bearer ${tokenResponse.accessToken}")

                    try {
                        httpURLConnection.inputStream.bufferedReader().use { it.readText() }
                    } catch (fileNotFoundException: FileNotFoundException) {
                        httpURLConnection.errorStream.bufferedReader().use { it.readText() }
                    } finally {
                        httpURLConnection.disconnect()
                    }
                }
                _liveUserInfo.postValue(response)
            }
        }
    }

    class TokensViewModelFactory(
        private val app: Application,
        private val configuration: Configuration,
        private val tokenResponse: SuccessfulTokenResponse
    ): ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TokensViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TokensViewModel(app, configuration, tokenResponse) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class TokensViewModel")
        }
    }

    companion object {
        private const val EXTRA_OAUTH_TOKEN_RESPONSE = "io.curity.fragment_tokens.extra_oauth_token_response"
        private const val EXTRA_CONFIG = "io.curity.fragment_tokens.extra_config"

        fun newInstance(tokenResponse: SuccessfulTokenResponse,
                        configuration: Configuration
        ): TokensFragment {
            val fragment = TokensFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_OAUTH_TOKEN_RESPONSE, tokenResponse)
                putString(EXTRA_CONFIG, Json.encodeToString(configuration))
            }
            return fragment
        }
    }
}