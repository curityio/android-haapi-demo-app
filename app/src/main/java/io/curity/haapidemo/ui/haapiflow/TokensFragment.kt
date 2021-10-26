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

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import io.curity.haapidemo.R
import io.curity.haapidemo.models.OAuthTokenResponse
import io.curity.haapidemo.uicomponents.DisclosureContent
import io.curity.haapidemo.uicomponents.DisclosureView
import io.curity.haapidemo.uicomponents.ProgressButton

class TokensFragment: Fragment(R.layout.fragment_tokens) {

    private lateinit var accessDisclosureView: DisclosureView
    private lateinit var idTokenDisclosureView: DisclosureView
    private lateinit var refreshDisclosureView: DisclosureView
    private lateinit var linearLayoutIDToken: LinearLayout

    private lateinit var refreshTokenButton: ProgressButton
    private lateinit var signOutButton: ProgressButton

    private lateinit var tokensViewModel: TokensViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            val oAuthTokenResponse = requireArguments().getSerializable(EXTRA_OAUTH_TOKEN_RESPONSE) as OAuthTokenResponse
            val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
            tokensViewModel = ViewModelProvider(this, TokensViewModelFactory(oAuthTokenResponse, haapiFlowViewModel))
                .get(TokensViewModel::class.java)
        } else {
            throw IllegalArgumentException("TokensFragment was not instantiated with newInstance()")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accessDisclosureView = view.findViewById(R.id.access_disclosure_view)
        idTokenDisclosureView = view.findViewById(R.id.id_token_view)
        refreshDisclosureView = view.findViewById(R.id.refresh_disclosure_view)
        linearLayoutIDToken = view.findViewById(R.id.linear_layout_id_token)

        refreshTokenButton = view.findViewById(R.id.refresh_button)
        signOutButton = view.findViewById(R.id.signout_button)

        accessDisclosureView.setContentText(tokensViewModel.accessToken)

        val idToken = tokensViewModel.idToken
        if (idToken != null) {
            idTokenDisclosureView.setContentText(idToken)
            linearLayoutIDToken.visibility = View.VISIBLE
        } else {
            linearLayoutIDToken.visibility = View.GONE
        }

        accessDisclosureView.setDisclosureContents(tokensViewModel.disclosureContents)

        val refreshToken = tokensViewModel.refreshToken
        if (refreshToken != null) {
            refreshDisclosureView.visibility = View.VISIBLE
            refreshDisclosureView.setContentText(refreshToken)
        } else {
            refreshDisclosureView.visibility = View.GONE
        }

        refreshTokenButton.setOnClickListener {
            tokensViewModel.refreshToken()
        }

        signOutButton.setOnClickListener {
            requireActivity().finish()
        }
    }

    class TokensViewModel(
        private val oAuthTokenResponse: OAuthTokenResponse,
        private val haapiFlowViewModel: HaapiFlowViewModel
    ): ViewModel() {

        val accessToken: String = oAuthTokenResponse.accessToken
        val idToken: String? = oAuthTokenResponse.idToken
        val refreshToken: String? = oAuthTokenResponse.refreshToken
        val disclosureContents: List<DisclosureContent>

        init {
            val mutableDisclosureContents = mutableListOf<DisclosureContent>()
            oAuthTokenResponse.properties.keys().forEach {
                if (it == "id_token" || it == "access_token" || it == "refresh_token") { return@forEach }
                mutableDisclosureContents.add(DisclosureContent(it, oAuthTokenResponse.properties.get(it).toString()))
            }
            disclosureContents = mutableDisclosureContents
        }

        fun refreshToken() {
            if (oAuthTokenResponse.refreshToken != null) {
                haapiFlowViewModel.refreshAccessToken(oAuthTokenResponse.refreshToken)
            }
        }
    }

    class TokensViewModelFactory(
        private val oAuthTokenResponse: OAuthTokenResponse,
        private val haapiFlowViewModel: HaapiFlowViewModel
    ): ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TokensViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TokensViewModel(oAuthTokenResponse, haapiFlowViewModel) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class TokensViewModel")
        }
    }

    companion object {
        private const val EXTRA_OAUTH_TOKEN_RESPONSE = "io.curity.fragment_tokens.extra_oauth_token_response"

        fun newInstance(oAuthTokenResponse: OAuthTokenResponse): TokensFragment {
            val fragment = TokensFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(EXTRA_OAUTH_TOKEN_RESPONSE, oAuthTokenResponse)
            }
            return fragment
        }
    }
}