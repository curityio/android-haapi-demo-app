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
import io.curity.haapidemo.R
import io.curity.haapidemo.models.OAuthTokenResponse
import io.curity.haapidemo.uicomponents.DisclosureContent
import io.curity.haapidemo.uicomponents.DisclosureView

class TokensFragment: Fragment(R.layout.fragment_tokens) {

    private lateinit var accessDisclosureView: DisclosureView
    private lateinit var idTokenDisclosureView: DisclosureView
    private lateinit var refreshDisclosureView: DisclosureView
    private lateinit var linearLayoutIDToken: LinearLayout

    private lateinit var oAuthTokenResponse: OAuthTokenResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            oAuthTokenResponse = requireArguments().getSerializable(EXTRA_OAUTH_TOKEN_RESPONSE) as OAuthTokenResponse
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

        accessDisclosureView.setContentText(oAuthTokenResponse.accessToken)
        val idToken = oAuthTokenResponse.idToken
        if (idToken != null) {
            idTokenDisclosureView.setContentText(idToken)
            linearLayoutIDToken.visibility = View.VISIBLE
        } else {
            linearLayoutIDToken.visibility = View.GONE
        }

        val disclosureContents: MutableList<DisclosureContent> = mutableListOf()
        oAuthTokenResponse.properties.keys().forEach {
            if (it == "id_token" || it == "access_token" || it == "refresh_token") { return@forEach }
            disclosureContents.add(DisclosureContent(it, oAuthTokenResponse.properties.get(it).toString()))
        }
        accessDisclosureView.setDisclosureContents(disclosureContents)

        if (oAuthTokenResponse.refreshToken != null) {
            refreshDisclosureView.visibility = View.VISIBLE
            refreshDisclosureView.setContentText(oAuthTokenResponse.refreshToken!!)
        } else {
            refreshDisclosureView.visibility = View.GONE
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