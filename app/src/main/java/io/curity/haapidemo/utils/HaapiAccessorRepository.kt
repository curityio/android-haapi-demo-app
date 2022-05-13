package io.curity.haapidemo.utils

import java.net.URI
import android.content.Context
import io.curity.haapidemo.Configuration
import se.curity.identityserver.haapi.android.driver.ClientAuthenticationMethodConfiguration
import se.curity.identityserver.haapi.android.sdk.DcrConfiguration
import se.curity.identityserver.haapi.android.sdk.HaapiAccessor
import se.curity.identityserver.haapi.android.sdk.HaapiAccessorFactory

// A utility object to create the accessor before login and close it after logout
// The code example uses a global repository that can be accessed across multiple activities
object HaapiAccessorRepository {

    var instance: HaapiAccessor? = null

    suspend fun get(configuration: Configuration, context: Context): HaapiAccessor {

        var accessor = instance
        if (accessor != null) {
            return accessor;
        }

        val accessorFactory = HaapiAccessorFactory(configuration.toHaapiConfiguration())

        if (!configuration.deviceSecret.isNullOrEmpty() &&
            !configuration.dcrTemplateClientId.isNullOrEmpty() &&
            !configuration.dcrClientRegistrationEndpointUri.isNullOrEmpty()
        ) {

            // Register details which will be needed on some devices for fallback DCR attestation
            // DCR is only be used on devices where signing key attestation fails
            val dcrConfiguration = DcrConfiguration(
                templateClientId = configuration.dcrTemplateClientId!!,
                clientRegistrationEndpointUri = URI(configuration.dcrClientRegistrationEndpointUri),
                context = context
            )

            // The simplest way to use HAAPI  is to use a fixed string client secret
            // More secure options of client certificates and JWT client assertions are also supported
            val dcrClientCredentials =
                ClientAuthenticationMethodConfiguration.Secret(configuration.deviceSecret!!)

            accessorFactory
                .setDcrConfiguration(dcrConfiguration)
                .setClientAuthenticationMethodConfiguration(dcrClientCredentials)
        }

        accessor = accessorFactory.create()
        instance = accessor
        return accessor
    }

    fun close() {

        val accessor = instance
        if (accessor != null) {
            accessor.haapiManager.close()
            instance = null
        }
    }
}