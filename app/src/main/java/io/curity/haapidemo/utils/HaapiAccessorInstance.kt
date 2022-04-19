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

                /* Use a JWT client assertion in the code example and mock a software attestation framework
                val dcrClientCredentials =
                    ClientAuthenticationMethodConfiguration.SignedJwt.Asymmetric(
                        clientKeyStore = deviceKeyStore,
                        clientKeyStorePassword = deviceKeyStorePassword,
                        alias = "myRsaKey",
                        algorithmIdentifier = ClientAuthenticationMethodConfiguration.SignedJwt.Asymmetric.AlgorithmIdentifier.RS256
                    )
                 */

                val dcrClientCredentials =
                    ClientAuthenticationMethodConfiguration.Secret(configuration.deviceSecret!!)

                accessorFactory
                    .setDcrConfiguration(dcrConfiguration)
                    .setClientAuthenticationMethodConfiguration(dcrClientCredentials)
            }

            // Create the accessor via signing key attestation, or fallback to DCR if required
            val accessor = accessorFactory.create()
            instance = accessor
            return accessor
        }

        fun destroy() {
            instance = null
        }
    }
}