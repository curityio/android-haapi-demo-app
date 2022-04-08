package io.curity.haapidemo.utils

import android.content.Context
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.CoroutineContext
import io.curity.haapidemo.Configuration
import se.curity.identityserver.haapi.android.driver.ClientAuthenticationMethodConfiguration
import se.curity.identityserver.haapi.android.sdk.*

class HaapiFactory(private val configuration: Configuration) {

    fun usesDcrFallback(): Boolean {
        return true
    }

    suspend fun registerDynamicClient(context: Context, coroutine: CoroutineContext): HaapiAccessor {

        val dynamicClient = loadDynamicClient()
        if (dynamicClient == null) {

            // Register the first time
            if (configuration.stringDcrSecret.isNullOrEmpty() ||
                configuration.fallbackClientId.isNullOrEmpty() ||
                configuration.clientRegistrationEndpointUri.isNullOrEmpty()) {
                throw IllegalArgumentException("DCR Fallback cannot be used because required configuration properties are missing")
            }

            val dcrClientCredentials =
                ClientAuthenticationMethodConfiguration.Secret(configuration.stringDcrSecret!!)

            val dcrConfiguration = DcrConfiguration(
                templateClientId = configuration.fallbackClientId!!,
                clientRegistrationEndpointUri = URI(configuration.clientRegistrationEndpointUri),
                context = context
            )

            val accessor: HaapiAccessor = HaapiAccessorFactory(getHaapiConfiguration())
                .setClientAuthenticationMethodConfiguration(dcrClientCredentials)
                .setDcrConfiguration(dcrConfiguration)
                .create(coroutine)

            val secret = accessor.clientAuthenticationMethodConfiguration as ClientAuthenticationMethodConfiguration.Secret
            val dynamicClient = DynamicClient(accessor.clientId, secret.secret)
            saveDynamicClient(dynamicClient)
            return accessor


        } else {

            // On subsequent application restarts, get the accessor without DCR details
            val haapiClientCredentials =
                ClientAuthenticationMethodConfiguration.Secret(dynamicClient.clientSecret)

            return HaapiAccessorFactory(getHaapiConfiguration(dynamicClient.clientId))
                .setClientAuthenticationMethodConfiguration(haapiClientCredentials)
                .create(coroutine)
        }
    }

    fun createTokenManager(): OAuthTokenManager {

        // The token manager uses the dynamic client's ID and secret to get a HAAPI token
        // But what about other requests for tokens, using the main mobile client?
        val dynamicClient = DynamicClientRepository.client
        val haapiClientCredentials =
            ClientAuthenticationMethodConfiguration.Secret(dynamicClient!!.clientSecret)
        return OAuthTokenManager(getHaapiConfiguration(dynamicClient.clientId), haapiClientCredentials)
    }

    fun loadDynamicClient(): DynamicClient? {
        return DynamicClientRepository.client
    }

    fun saveDynamicClient(dynamicClient: DynamicClient) {
        DynamicClientRepository.client = dynamicClient
    }

    // Translate application configuration to SDK configuration, which may require more thought
    fun getHaapiConfiguration(dynamicClientId: String? = null): HaapiConfiguration {

        return HaapiConfiguration(
            keyStoreAlias = configuration.keyStoreAlias,
            clientId = dynamicClientId ?: configuration.clientId,
            baseUri = URI.create(configuration.baseURLString),
            tokenEndpointUri = URI.create(configuration.tokenEndpointURI),
            authorizationEndpointUri = URI.create(configuration.authorizationEndpointURI),
            appRedirect = configuration.redirectURI,
            isAutoRedirect = configuration.followRedirect,
            httpUrlConnectionProvider = { url ->
                val urlConnection = url.openConnection()
                urlConnection.connectTimeout = 8000
                if (!configuration.isSSLTrustVerificationEnabled) {
                    urlConnection.disableSslTrustVerification() as HttpURLConnection
                } else {
                    urlConnection as HttpURLConnection
                }
            }
        )
    }
}