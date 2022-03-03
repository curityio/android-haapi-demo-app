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

import kotlinx.serialization.Serializable

/**
 * Configuration for `HaapiFlowManager`
 *
 * @property [name] A String that represents the name for the configuration. The value does not matter when using
 * with `HaapiFlowManager`
 * @property [clientId] A String that represents a clientId. This value is important and needs to match with the curity
 * identity server configuration.
 * @property [baseURLString] A String that represents a base URL that targets to the curity identity server
 * @property [tokenEndpointURI] A String that represents a token endpoint URI. With an incorrect value, it is impossible
 * to receive an access token
 * @property [authorizationEndpointURI] A String that represents an authorization endpoint URI. With an incorrect value,
 * it is impossible to start the flow with the curity identity server.
 * @property [userInfoEndpointURI] A String that represents the user info endpoint URI.
 * @property [metaDataBaseURLString] A String that represents a metadata endpoint
 * @property [redirectURI] A String that represents a redirect URI. Please check with your curity identity server
 * configuration
 * @property [keyStoreAlias] A String that represents the name of the key store entry. `haapi-demo-app` is the default
 * value. It is important this value is unique. Otherwise, `HaapiFlowManager` will not be able to start.
 * @property [followRedirect] A Boolean that represents if `HaapiFlowManager` will automatically or not handle the redirect flow.
 * @property [isAutoPollingEnabled] A Boolean that represents if HaapiFlowManager` will automatically or not handle the polling process.
 * @property [isAutoAuthorizationChallengedEnabled] A Boolean that represents if `HaapiFlowManager` will automatically
 * or not handle the authorization flow to retrieve the access token. `true` is the default value.
 * @property [isSSLTrustVerificationEnabled] A Boolean that represents if `HaapiFlowManager` will trust or not the SSL
 * verification. `true` is the default value. _IMPORTANT_: Setting to `false` is totally insecure. `false` can only be
 * used when performing test against localhost or debugging.
 * @property [selectedScopes] A list of String that represents the selected scopes. By default, there is no scopes.
 */

@Serializable
data class Configuration(
    var name: String,
    var clientId: String,
    var baseURLString: String,
    var tokenEndpointURI: String,
    var authorizationEndpointURI: String,
    var userInfoEndpointURI: String,
    var metaDataBaseURLString: String,
    var redirectURI: String,
    val keyStoreAlias: String = "haapi-demo-app",
    var followRedirect: Boolean = true,
    var isAutoPollingEnabled: Boolean = true,
    var isAutoAuthorizationChallengedEnabled: Boolean = true,
    var isSSLTrustVerificationEnabled: Boolean = true,
    var selectedScopes: List<String> = emptyList(),

    var supportedScopes: List<String> = emptyList()
) {

    companion object {
        // A convenience flag for Curity developers.
        private const val CURITY_DEV_MODE = false

        fun newInstance(name: String = "haapi-android-client"): Configuration =
            if (CURITY_DEV_MODE) newDevInstance(name) else Configuration(
                name = name,
                clientId = "haapi-android-client",
                baseURLString = "https://10.0.2.2:8443",
                tokenEndpointURI = "https://10.0.2.2:8443/oauth/v2/oauth-token",
                authorizationEndpointURI = "https://10.0.2.2:8443/oauth/v2/oauth-authorize",
                userInfoEndpointURI = "https://10.0.2.2:8443/oauth/v2/oauth-userinfo",
                metaDataBaseURLString = "https://10.0.2.2:8443/oauth/v2/oauth-anonymous",
                redirectURI = "app://haapi",
                followRedirect = true,
                isSSLTrustVerificationEnabled = false,
                selectedScopes = listOf("openid", "profile")
            )

        fun newInstance(number: Int): Configuration {
            return newInstance("New Profile ($number)")
        }

        private fun newDevInstance(name: String = "haapi-android-client"): Configuration =
            Configuration(name = name,
                clientId = "haapi-android-client",
                baseURLString = "https://10.0.2.2:8443",
                tokenEndpointURI = "https://10.0.2.2:8443/dev/oauth/token",
                authorizationEndpointURI = "https://10.0.2.2:8443/dev/oauth/authorize",
                userInfoEndpointURI = "https://10.0.2.2:8443/dev/oauth/userinfo",
                metaDataBaseURLString = "https://10.0.2.2:8443/dev/oauth/anonymous",
                redirectURI = "app://haapi",
                followRedirect = true,
                isSSLTrustVerificationEnabled = false,
                selectedScopes = listOf("openid", "profile")
            )
    }
}