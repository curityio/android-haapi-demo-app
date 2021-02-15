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

class Configuration {
    companion object {
        /**
         * Change these settings to your instance of the Curity Identity Server
         */
        const val host = "trojan.ngrok.io"
        const val baseUrl = "https://$host"
        const val clientId = "haapi-public-client"
        const val redirectUri = "https://localhost:7777/client-callback"
        const val authorizationEndpoint = "oauth/v2/oauth-authorize"
        const val tokenEndpoint = "oauth/v2/oauth-token"
        const val scopes = "openid" // If you need multiple scopes add them as a space-separated string, e.g. "openid profile email"
    }
}
