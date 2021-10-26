/*
 * Copyright (C) 2020 Curity AB. All rights reserved.
 *
 * The contents of this file are the property of Curity AB.
 * You may not copy or use this file, in either source code
 * or executable form, except in compliance with terms
 * set by Curity AB.
 *
 * For further information, please contact Curity AB.
 */

package io.curity.haapidemo

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import se.curity.identityserver.haapi.android.sdk.HaapiTokenManager
import se.curity.identityserver.haapi.android.sdk.okhttp.OkHttpUtils.addHaapiInterceptor
import java.net.URI
import java.time.Duration
import io.curity.haapidemo.models.*
import io.curity.haapidemo.models.haapi.Message
import io.curity.haapidemo.models.haapi.PollingStatus
import io.curity.haapidemo.okhttp.*
import io.curity.haapidemo.parsers.*
import org.junit.runner.RunWith

/**
 * Tests that do complete or partial flows
 * - using an OkHttp client with an [HaapiTokenManager] interceptor.
 * - using the [HaapiStep] hierarchy to handle the responses.
 */
@RunWith(AndroidJUnit4::class)
class FlowTests
{
    @Test
    fun can_perform_a_complete_username_flow() = withClient { httpClient ->
        var response: HaapiStep = httpClient.get(AUTHORIZATION_REQUEST_URI).toHaapiStep()
        val redirect = response.assertOfType<Redirect>()

        response = httpClient.submit(redirect.action).toHaapiStep()
        val authenticatorSelection = response.assertOfType<AuthenticatorSelector>()

        authenticatorSelection.title.toText().assertIs("Select Authentication Method")
        val authenticatorOption = authenticatorSelection.authenticators.find {
            it.type == "html-form" && it.label.toText() == "A standard SQL backed authenticator"
        } ?: throw Exception("Unable to find authenticator")

        response = httpClient.submit(authenticatorOption.action).toHaapiStep()
        val authenticatorForm = response.assertOfType<InteractiveForm>()

        response = httpClient.submit(
            authenticatorForm.actions.first(),
            "userName" to "testuser", "password" to "Password1"
        ).toHaapiStep()
        val redirectFromLogin = response.assertOfType<Redirect>()

        response = httpClient.submit(redirectFromLogin.action).toHaapiStep()
        val authorizationResponse = response.assertOfType<AuthorizationCompleted>()

        authorizationResponse.responseParameters.code.assertNonBlank()
        authorizationResponse.responseParameters.state.assertNonBlank()
    }

    @Test
    fun can_perform_a_complete_username_flow_using_implicit() = withClient { httpClient ->
        var response: HaapiStep = httpClient.get(AUTHORIZATION_REQUEST_USING_IMPLICIT_URI).toHaapiStep()
        val redirect = response.assertOfType<Redirect>()

        response = httpClient.submit(redirect.action).toHaapiStep()
        val authenticatorSelection = response.assertOfType<AuthenticatorSelector>()

        authenticatorSelection.title.toText().assertIs("Select Authentication Method")
        val authenticatorOption = authenticatorSelection.authenticators.find {
            it.type == "html-form" && it.label.toText() == "A standard SQL backed authenticator"
        } ?: throw Exception("Unable to find authenticator")

        response = httpClient.submit(authenticatorOption.action).toHaapiStep()
        val authenticatorForm = response.assertOfType<InteractiveForm>()

        response = httpClient.submit(
            authenticatorForm.actions.first(),
            "userName" to "testuser", "password" to "Password1"
        ).toHaapiStep()
        val redirectFromLogin = response.assertOfType<Redirect>()

        response = httpClient.submit(redirectFromLogin.action).toHaapiStep()
        val authorizationResponse = response.assertOfType<AuthorizationCompleted>()

        authorizationResponse.responseParameters.accessToken.assertNonBlank()
        authorizationResponse.responseParameters.state.assertIs("foo")
        authorizationResponse.responseParameters.tokenType.assertIs("bearer")
        authorizationResponse.responseParameters.scope.assertIs("read")
        authorizationResponse.responseParameters.expiresIn.assertIs(Duration.ofSeconds(300))
    }

    @Test
    fun can_perform_an_email_flow_until_polling() = withClient { httpClient ->
        var response = httpClient.get(AUTHORIZATION_REQUEST_URI).toHaapiStep()
        val redirect = response.assertOfType<Redirect>()

        response = httpClient.submit(redirect.action).toHaapiStep()
        val authenticatorSelection = response.assertOfType<AuthenticatorSelector>()

        authenticatorSelection.title.toText().assertIs("Select Authentication Method")
        val authenticatorOption = authenticatorSelection.authenticators.find {
            it.type == "email" && it.label.toText() == "email1"
        } ?: throw Exception("Unable to find authenticator")

        response = httpClient.submit(authenticatorOption.action).toHaapiStep()
        val authenticatorForm = response.assertOfType<InteractiveForm>()

        response = httpClient.submit(
            authenticatorForm.actions.first(),
            "userName" to "testuser"
        ).toHaapiStep()
        val firstPolling = response.assertOfType<PollingStep>()

        firstPolling.properties.recipientOfCommunication.assertIs("xxxx@xxxxple.com")
        firstPolling.properties.status.assertIs(PollingStatus.Pending)
        firstPolling.cancel.assertNotNull()
        response = httpClient.submit(firstPolling.main).toHaapiStep()
        val secondPolling = response.assertOfType<PollingStep>()

        secondPolling.properties.recipientOfCommunication.assertIs("xxxx@xxxxple.com")
        secondPolling.properties.status.assertIs(PollingStatus.Pending)
        secondPolling.cancel.assertNotNull()

        response = httpClient.submit(secondPolling.cancel!!).toHaapiStep()
        response.assertOfType<InteractiveForm>()

        response = httpClient.submit(
            authenticatorForm.actions.first(),
            "userName" to "testuser"
        ).toHaapiStep()
        response.assertOfType<PollingStep>()
    }

    @Test
    fun can_perform_a_saml_flow_until_external_browser_flow() = withClient { httpClient ->
        var response = httpClient.get(AUTHORIZATION_REQUEST_URI).toHaapiStep()
        val redirect = response.assertOfType<Redirect>()

        response = httpClient.submit(redirect.action).toHaapiStep()
        val authenticatorSelection = response.assertOfType<AuthenticatorSelector>()

        authenticatorSelection.title.toText().assertIs("Select Authentication Method")
        val authenticatorOption = authenticatorSelection.authenticators.find {
            it.type == "saml" && it.label.toText() == "Virtual SAML"
        } ?: throw Exception("Unable to find authenticator")

        response = httpClient.submit(authenticatorOption.action).toHaapiStep()
        val externalBrowserFlowOperation = response.assertOfType<ExternalBrowserClientOperation>()
        val externalBrowserFlowArgs = externalBrowserFlowOperation.actionModel.arguments

        externalBrowserFlowArgs.href.assertNonBlank()

        response = httpClient.submit(externalBrowserFlowOperation.cancel!!).toHaapiStep()
        val anotherRedirect = response.assertOfType<Redirect>()

        response = httpClient.submit(anotherRedirect.action).toHaapiStep()
        response.assertOfType<AuthenticatorSelector>()
    }

}

private fun withClient(block: (OkHttpClient) -> Unit)
{
    HaapiTokenManager(
        URI(TOKEN_ENDPOINT), CLIENT_ID
    ) {
        connectionProvider = UNCHECKED_CONNECTION_PROVIDER
    }.use { tokenManager ->
        val httpClient = OkHttpClient.Builder()
            .addHaapiInterceptor(tokenManager)
            .disableSslTrustVerification()
            .build()

        block(httpClient)
    }
}

private fun request(url: String) = Request.Builder()
    .url(absolute(url))
    .build()

private fun absolute(url: String) =
    if (url.startsWith("http"))
    {
        url
    } else
    {
        "$SCHEME://$HOST_NAME$PORT$url"
    }

private fun request(url: HttpUrl) = Request.Builder()
    .url(url)
    .build()

private fun OkHttpClient.get(url: String) = newCall(request(url)).execute()
private fun OkHttpClient.get(url: HttpUrl) = newCall(request(url)).execute()

private fun Message.toText() = when(this) {
    is Message.OfLiteral -> this.message
    is Message.OfLiteralAndKey -> this.message
    // On a real case, the app could use a translation map here instead of throwing
    is Message.OfKey -> throw ModelException("Messages should have literal values")
}