package io.curity.haapidemo.utils

import java.net.URI
import java.security.KeyStore
import android.content.Context
import io.curity.haapidemo.Configuration
import se.curity.identityserver.haapi.android.driver.ClientAuthenticationMethodConfiguration
import se.curity.identityserver.haapi.android.sdk.DcrConfiguration
import se.curity.identityserver.haapi.android.sdk.HaapiAccessor
import se.curity.identityserver.haapi.android.sdk.HaapiAccessorFactory

class HaapiFactory {

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

            /* For demo purposes, this code would present a client certificate embedded into the APK file
            val deviceKeyStore = loadKeyStore(context, R.raw.devicekeystore, "android")
            val serverTrustStore = loadKeyStore(context, R.raw.servertruststore, "android")
            val dcrClientCredentials =
                ClientAuthenticationMethodConfiguration.Mtls(
                    clientKeyStore = deviceKeyStore,
                    clientKeyStorePassword = "android".toCharArray(),
                    serverTrustStore = serverTrustStore
                )
             */

            /* For demo purposes, this code could present a client assertion from a key embedded into the APK file
            val deviceKeyStore = loadKeyStore(context, R.raw.devicekeystore, "android")
            val dcrClientCredentials =
                ClientAuthenticationMethodConfiguration.SignedJwt.Asymmetric(
                    clientKeyStore = deviceKeyStore,
                    clientKeyStorePassword = "android".toCharArray(),
                    alias = "deviceclientcert",
                    algorithmIdentifier = ClientAuthenticationMethodConfiguration.SignedJwt.Asymmetric.AlgorithmIdentifier.RS256
                )
            */

            accessorFactory
                .setDcrConfiguration(dcrConfiguration)
                .setClientAuthenticationMethodConfiguration(dcrClientCredentials)
        }

        return accessorFactory.create()
    }

    fun loadKeyStore(context: Context, resourceId: Int, password: String): KeyStore {

        val inputStream = context.resources.openRawResource(resourceId)
        inputStream.use {
            val keyStore = KeyStore.getInstance("BKS")
            keyStore.load(inputStream, password.toCharArray())
            return keyStore
        }
    }
}