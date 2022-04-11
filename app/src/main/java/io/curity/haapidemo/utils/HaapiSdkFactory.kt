package io.curity.haapidemo.utils

import android.content.Context
import java.net.URI
import kotlin.coroutines.CoroutineContext
import io.curity.haapidemo.Configuration
import se.curity.identityserver.haapi.android.driver.ClientAuthenticationMethodConfiguration
import se.curity.identityserver.haapi.android.sdk.*

/*
 * I think the HaapiAccessorFactory should have these main operations
 */
class HaapiSdkFactory(private val configuration: Configuration) {

    /*
     * First the SDK is asked to load, so that the client ID to use is known
     * This will enable the OAuthTokenManager to be returned correctly without requiring a HAAPI access token to be retrieved
     * This should not need to be async
     */
    suspend fun load(context: Context, coroutine: CoroutineContext): HaapiAccessor {

        val accessorFactory = HaapiAccessorFactory(configuration.toHaapiConfiguration())

        if (!configuration.dcrSecret.isNullOrEmpty() &&
            !configuration.dcrTemplateClientId.isNullOrEmpty() &&
            !configuration.dcrClientRegistrationEndpointUri.isNullOrEmpty()
        ) {

            // Customers using DCR fallback call these builder methods
            val dcrConfiguration = DcrConfiguration(
                templateClientId = configuration.dcrTemplateClientId!!,
                clientRegistrationEndpointUri = URI(configuration.dcrClientRegistrationEndpointUri),
                context = context
            )

            val dcrClientCredentials =
                ClientAuthenticationMethodConfiguration.Secret(configuration.dcrSecret!!)

            accessorFactory
                .setDcrConfiguration(dcrConfiguration)
                .setClientAuthenticationMethodConfiguration(dcrClientCredentials)
        }

        // All new customers use the accessor programming model
        val accessor = accessorFactory.create(coroutine)

        // These details should not need to be managed by the app
        val secret = accessor.clientAuthenticationMethodConfiguration as ClientAuthenticationMethodConfiguration.Secret
        val dynamicClient = DynamicClient(accessor.clientId, secret.secret)
        DynamicClientRepository.client = dynamicClient
        
        return accessor
    }

    /*
     * If there is already an access token, the factory will be asked to return the OAuthTokenManager
     * This should be as simple as calling accessor.tokenManager, in all cases
     */
    fun createOAuthTokenManager(): OAuthTokenManager {

        val dynamicClient = DynamicClientRepository.client
        if (dynamicClient != null) {

            val dynamicClientCredentials =
                ClientAuthenticationMethodConfiguration.Secret(dynamicClient.clientSecret)
            return OAuthTokenManager(configuration.toHaapiConfiguration(dynamicClient.clientId), dynamicClientCredentials)

        } else {

            return OAuthTokenManager(
                oauthTokenConfiguration = configuration.toHaapiConfiguration()
            )
        }
    }
}