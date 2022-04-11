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
        return (!configuration.stringDcrSecret.isNullOrEmpty() &&
                !configuration.fallbackClientId.isNullOrEmpty() &&
                !configuration.clientRegistrationEndpointUri.isNullOrEmpty())
    }

    suspend fun registerDynamicClient(context: Context, coroutine: CoroutineContext): HaapiAccessor {

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

        // Register the first time, to get the dynamic client
        val accessor: HaapiAccessor = HaapiAccessorFactory(getHaapiConfiguration())
            .setClientAuthenticationMethodConfiguration(dcrClientCredentials)
            .setDcrConfiguration(dcrConfiguration)
            .create(coroutine)

        val secret = accessor.clientAuthenticationMethodConfiguration as ClientAuthenticationMethodConfiguration.Secret
        val dynamicClient = DynamicClient(accessor.clientId, secret.secret)
        DynamicClientRepository.client = dynamicClient
        return accessor
    }

    fun createOAuthTokenManager(): OAuthTokenManager {

        val dynamicClient = DynamicClientRepository.client
        if (dynamicClient != null) {

            val dynamicClientCredentials =
                ClientAuthenticationMethodConfiguration.Secret(dynamicClient.clientSecret)
            return OAuthTokenManager(getHaapiConfiguration(), dynamicClientCredentials)

        } else {

            return OAuthTokenManager(
                oauthTokenConfiguration = getHaapiConfiguration()
            )
        }
    }

    fun getHaapiConfiguration(): HaapiConfiguration {

        var dynamicClientId: String? = null
        if(usesDcrFallback()) {
            val dynamicClient = DynamicClientRepository.client
            if (dynamicClient != null) {
                dynamicClientId = dynamicClient.clientId
            }
        }

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