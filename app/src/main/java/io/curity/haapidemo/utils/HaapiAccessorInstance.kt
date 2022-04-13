package io.curity.haapidemo.utils

import android.content.Context
import io.curity.haapidemo.Configuration
import java.net.URI
import se.curity.identityserver.haapi.android.driver.ClientAuthenticationMethodConfiguration
import se.curity.identityserver.haapi.android.sdk.DcrConfiguration
import se.curity.identityserver.haapi.android.sdk.HaapiAccessor
import se.curity.identityserver.haapi.android.sdk.HaapiAccessorFactory

class HaapiAccessorInstance {

    companion object {

        @Volatile
        private var instance: HaapiAccessor? = null

        suspend fun create(configuration: Configuration, context: Context): HaapiAccessor {

            if (instance != null) {
                return instance!!
            }

            val accessorFactory = HaapiAccessorFactory(configuration.toHaapiConfiguration())
            if (!configuration.dcrSecret.isNullOrEmpty() &&
                !configuration.dcrTemplateClientId.isNullOrEmpty() &&
                !configuration.dcrClientRegistrationEndpointUri.isNullOrEmpty()
            ) {

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

            val accessor = accessorFactory.create()
            instance = accessor
            return accessor
        }

        fun destroy() {
            instance = null
        }
    }
}