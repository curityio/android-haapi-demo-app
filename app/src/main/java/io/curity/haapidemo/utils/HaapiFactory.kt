package io.curity.haapidemo.utils

import android.content.Context
import io.curity.haapidemo.Configuration
import java.net.URI
import se.curity.identityserver.haapi.android.driver.ClientAuthenticationMethodConfiguration
import se.curity.identityserver.haapi.android.sdk.DcrConfiguration
import se.curity.identityserver.haapi.android.sdk.HaapiAccessor
import se.curity.identityserver.haapi.android.sdk.HaapiAccessorFactory

class HaapiFactory {

    companion object {

        suspend fun create(configuration: Configuration, context: Context): HaapiAccessor {

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

                // The simplest case is to use a fixed string secret, but this is not very secure
                val dcrClientCredentials =
                   ClientAuthenticationMethodConfiguration.Secret(configuration.deviceSecret!!)

                /* The preferred option is to use a client certificate for the particular device
                val dcrClientCredentials = ...
                 */

                /* Or a JWT client assertion for the particular device
                val dcrClientCredentials =
                    ClientAuthenticationMethodConfiguration.SignedJwt.Asymmetric(
                        clientKeyStore = deviceKeyStore,
                        clientKeyStorePassword = deviceKeyStorePassword,
                        alias = "myRsaKey",
                        algorithmIdentifier = ClientAuthenticationMethodConfiguration.SignedJwt.Asymmetric.AlgorithmIdentifier.RS256
                    )
                 */

                accessorFactory
                    .setDcrConfiguration(dcrConfiguration)
                    .setClientAuthenticationMethodConfiguration(dcrClientCredentials)
            }

            return accessorFactory.create()
        }
    }
}