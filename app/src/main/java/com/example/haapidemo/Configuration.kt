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
        const val authorizationEndpoint = "dev/oauth/authorize"
        const val tokenEndpoint = "dev/oauth/token"
        const val scopes = "openid" // If you need multiple scopes add them as a space-separated string, e.g. "openid profile email"
    }
}
