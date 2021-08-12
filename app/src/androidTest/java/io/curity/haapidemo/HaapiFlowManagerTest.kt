/*
 *  Copyright (C) 2021 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.haapidemo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.curity.haapidemo.flow.HaapiFlowManager
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class HaapiFlowManagerTest {

    private val flowConfiguration: HaapiFlowConfiguration = HaapiFlowConfiguration(
        name = "haapi-android-client",
        clientId = "haapi-android-client",
        baseURLString = "https://$EMULATOR_HOST_IP$PORT",
        tokenEndpointURI = "https://$EMULATOR_HOST_IP$PORT/dev/oauth/token",
        authorizationEndpointURI = "https://$EMULATOR_HOST_IP$PORT/dev/oauth/authorize",
        metaDataBaseURLString = "",
        redirectURI = "haapi:start",
        followRedirect = true,
        isSSLTrustVerificationEnabled = false,
        selectedScopes = listOf("open", "profile")
    )

    private lateinit var flowManager: HaapiFlowManager

    @get:Rule
    val testCoroutineScopeRule = MainCoroutineScopeRule() // Necessary when using Coroutine + runBlockingTest

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule() // Necessary when testing LiveData + Observer

    @After
    fun tearDown() {
        flowManager.close()
    }

    @Test
    fun monitorHaapiStepChanges() = testCoroutineScopeRule.runBlockingTest {
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)

        var count = 0
        val observer = Observer<HaapiStep?> {
            when (count) {
                0 -> Assert.assertNull("Expecting haapiStep to be null before start()", it)
                1 -> Assert.assertTrue("Expecting haapiStep to be a Redirect", it is Redirect)
                2 -> Assert.assertTrue("Expecting haapiStep to be an AuthenticatorSelector", it is AuthenticatorSelector)
                else -> Assert.assertNull("Expecting haapiStep to be null when reset()", it)
            }
            count += 1
        }

        flowManager.liveStep.observeForever(observer)
        flowManager.start()
        flowManager.reset()
        flowManager.liveStep.removeObserver(observer)
    }

    @Test
    fun startFlowValidConfiguration() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user that just created an instance of HaapiFlowManager with a valid configuration with automatic redirect
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        Assert.assertNull("Expecting null because the flow did not start", flowManager.liveStep.value)

        // When: HaapiFlowManager.start() is called
        val haapiStep = flowManager.start()

        // Then: An AuthenticatorSelector should be received
        Assert.assertTrue("Expecting haapiStep to be AuthenticatorSelector", haapiStep is AuthenticatorSelector)
    }

    @Test
    fun startFlowInvalidConfiguration() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user that just created an instance of HaapiFlowManager with a INvalid configuration with automatic redirect
        val invalidConfiguration = flowConfiguration.copy(clientId = "invalid-name")
        Assert.assertEquals("invalid-name", invalidConfiguration.clientId )
        flowManager = HaapiFlowManager(invalidConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)

        // When: HaapiFlowManager.start() is called
        val invalidStep = flowManager.start()

        // Then: A SystemErrorStep should be received
        Assert.assertTrue("Expecting haapiStep to be SystemErrorStep : $invalidStep", invalidStep is SystemErrorStep)
    }

    @Test
    fun reset() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user with an instance of HaapiFlowManager has called start() with a returned not null haapiStep
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        Assert.assertNull("Expecting haapiStep to be null", flowManager.liveStep.value)
        flowManager.start()
        Assert.assertNotNull("Expecting haapiStep not to be null", flowManager.liveStep.value)

        // When: HaapiFlowManager.reset() is called
        flowManager.reset()

        // Then: flowManager.haapiStep is expected to be null
        Assert.assertNull("Expecting haapiStep to be null after reset", flowManager.liveStep.value)
    }

    @Test
    fun fetchAccessTokenWithValidCode() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user using Username authenticator that has an authorization code
        val autoAuthChallengeDisabledConfig = flowConfiguration.copy(isAutoAuthorizationChallengedEnabled = false)
        flowManager = HaapiFlowManager(autoAuthChallengeDisabledConfig, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val usernameAuthenticator = authenticatorSelector.authenticators.first { it.type == "username" && it.label.message == "username" }
        val interactiveForm = flowManager.submitForm(usernameAuthenticator.action.model, emptyMap()) as InteractiveForm
        val newStep = flowManager.submitForm(interactiveForm.actions.first().model, mapOf("username" to "testuser")) as AuthorizationCompleted
        val code = newStep.responseParameters.code!!

        // When: Fetching the access token with a valid authorization code
        val finalStep = flowManager.fetchAccessToken(code)

        // Then: An AccessTokenStep should be received
        Assert.assertTrue("Expecting", finalStep is TokensStep)
        val accessTokenStep = finalStep as TokensStep
        Assert.assertNotNull("AccessToken should not be null", accessTokenStep.oAuthTokenResponse.accessToken)
        Assert.assertNotNull("ExpiresIn should not be null", accessTokenStep.oAuthTokenResponse.expiresIn)
    }

    @Test
    fun fetchAccessTokenWithInvalidCode() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user using Username authenticator that has an authorization code
        val autoAuthChallengeDisabledConfig = flowConfiguration.copy(isAutoAuthorizationChallengedEnabled = false)
        flowManager = HaapiFlowManager(autoAuthChallengeDisabledConfig, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val usernameAuthenticator = authenticatorSelector.authenticators.first { it.type == "username" && it.label.message == "username" }
        val interactiveForm = flowManager.submitForm(usernameAuthenticator.action.model, emptyMap()) as InteractiveForm
        val newStep = flowManager.submitForm(interactiveForm.actions.first().model, mapOf("username" to "testuser")) as AuthorizationCompleted
        val code = newStep.responseParameters.code!!

        // When: Fetching the accesstoken with an invalid authorization code
        val invalidAuthorizationCode = "$code$"
        val finalStep = flowManager.fetchAccessToken(invalidAuthorizationCode)

        // Then: A SystemErrorStep should be received
        Assert.assertTrue("Expecting a SystemErrorStep", finalStep is SystemErrorStep)
    }

    @Test
    fun refreshAccessTokenWithInvalidRefreshToken() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user using Username authenticator that has a valid access_token
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val usernameAuthenticator = authenticatorSelector.authenticators.first { it.type == "username" && it.label.message == "username" }
        val interactiveForm = flowManager.submitForm(usernameAuthenticator.action.model, emptyMap()) as InteractiveForm
        val accessTokenStep = flowManager.submitForm(interactiveForm.actions.first().model, mapOf("username" to "testuser")) as TokensStep

        // When: Refreshing its access token with an invalid refresh_token
        val invalidRefreshToken = accessTokenStep.oAuthTokenResponse.refreshToken!! + "$"
        val refreshStep = flowManager.refreshAccessToken(invalidRefreshToken)

        // Then: A SystemErrorStep should be received
        Assert.assertTrue("Expecting a SystemErrorStep", refreshStep is SystemErrorStep)
    }

    @Test
    fun refreshAccessTokenWithValidRefreshToken() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user using Username authenticator that has a valid access_token
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val usernameAuthenticator = authenticatorSelector.authenticators.first { it.type == "username" && it.label.message == "username" }
        val interactiveForm = flowManager.submitForm(usernameAuthenticator.action.model, emptyMap()) as InteractiveForm
        val accessTokenStep = flowManager.submitForm(interactiveForm.actions.first().model, mapOf("username" to "testuser")) as TokensStep

        // When: Refreshing its access token
        val refreshStep = flowManager.refreshAccessToken(accessTokenStep.oAuthTokenResponse.refreshToken!!)

        // Then: A TokensStep should be received containing a new access_token and refresh_token
        Assert.assertTrue("Expecting to have a TokenStep: $refreshStep", refreshStep is TokensStep)
        val newAccessTokenStep = refreshStep as TokensStep
        Assert.assertNotEquals(
            "Access tokens should be different",
            newAccessTokenStep.oAuthTokenResponse.accessToken,
            accessTokenStep.oAuthTokenResponse.accessToken
        )

        Assert.assertNotEquals(
            "Refresh tokens should be different",
            newAccessTokenStep.oAuthTokenResponse.refreshToken,
            accessTokenStep.oAuthTokenResponse.refreshToken
        )
    }

    @Test
    fun submitFormWhenManagerDidNotStart() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user that has an action and reset the flow
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val sqlAuthenticator = authenticatorSelector.authenticators.first { it.type == "html-form" && it.label.message == "A standard SQL backed authenticator" }
        val sqlAction = sqlAuthenticator.action.model
        flowManager.reset()

        // When: Submit an action instead of start()
        val newStep = flowManager.submitForm(sqlAction, emptyMap())

        // Then: A SystemErrorStep should be received
        Assert.assertTrue("Expecting a SystemErrorStep", newStep is SystemErrorStep)
    }

    @Test
    fun submitFormValid() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user using SQL authenticator that has an InteractiveForm
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val sqlAuthenticator = authenticatorSelector.authenticators.first { it.type == "html-form" && it.label.message == "A standard SQL backed authenticator" }

        // When: Submit a valid action
        val validAction = sqlAuthenticator.action.model
        val newStep = flowManager.submitForm(validAction, emptyMap())

        // Then: A new valid step is received
        Assert.assertTrue("Expecting an InteractiveForm", newStep is InteractiveForm)
        Assert.assertNotEquals("Expecting a new step", authenticatorSelector, newStep)
    }

    @Test
    fun followLinkWhenManagerDidNotStart() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user that has a link and reset the flowManager
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val sqlAuthenticator = authenticatorSelector.authenticators.first { it.type == "html-form" && it.label.message == "A standard SQL backed authenticator" }
        val interactiveForm = flowManager.submitForm(sqlAuthenticator.action.model, emptyMap()) as InteractiveForm
        val aLink = interactiveForm.links.first()
        flowManager.reset()

        // When: Follow a link
        val newStep = flowManager.followLink(aLink)

        // Then: A SystemErrorsStep should be received
        Assert.assertTrue("Expecting a SystemErrorStep", newStep is SystemErrorStep)
    }

    @Test
    fun followLinkValid() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user using SQL authenticator that has an InteractiveForm
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        val authenticatorSelector = flowManager.start() as AuthenticatorSelector
        val sqlAuthenticator = authenticatorSelector.authenticators.first { it.type == "html-form" && it.label.message == "A standard SQL backed authenticator" }
        val interactiveForm = flowManager.submitForm(sqlAuthenticator.action.model, emptyMap()) as InteractiveForm

        // When: Follow a link
        val aLink = interactiveForm.links.firstOrNull()
        Assert.assertNotNull("Expecting a link for a SQL backed authenticator", aLink)
        val newStep = flowManager.followLink(aLink!!)

        // Then: A new step is received
        Assert.assertNotEquals("Expecting a step different than interactiveForm", interactiveForm, newStep)
    }

    @Test
    fun closeAndStart() = testCoroutineScopeRule.runBlockingTest {
        // Given: A user close the flowManager
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        flowManager.close()

        // When: Start()
        var newStep = flowManager.start()
        
        // Then: A SystemErrorsStep should be received
        Assert.assertTrue("Expecting a SystemErrorStep", newStep is SystemErrorStep)
    }

    @Test
    fun completeSQLAuthenticationLoginWithoutRedirect() = testCoroutineScopeRule.runBlockingTest {
        flowManager = HaapiFlowManager(flowConfiguration, dispatcher = testCoroutineScopeRule.dispatcher)
        Assert.assertNull("Expecting haapiStep as null", flowManager.liveStep.value)
        val haapiStep = flowManager.start()
        Assert.assertTrue("Expecting an AuthenticatorSelector : $haapiStep", haapiStep is AuthenticatorSelector)
        val authenticatorSelector = haapiStep as AuthenticatorSelector
        val sqlAuthenticator = authenticatorSelector.authenticators.firstOrNull { it.type == "html-form" && it.label.message == "A standard SQL backed authenticator" }
        Assert.assertNotNull("Expecting a SQL Authenticator", sqlAuthenticator)

        val actionModel = sqlAuthenticator!!.action.model
        val submitStep = flowManager.submitForm(actionModel, emptyMap())
        Assert.assertTrue("Expecting an InteractiveForm", submitStep is InteractiveForm)
        val interactiveForm = submitStep as InteractiveForm

        val step3 = flowManager.submitForm(interactiveForm.actions.first().model, mapOf("userName" to "testuser", "password" to "Password1"))
        Assert.assertTrue("Expecting an Access Token", step3 is TokensStep)

        val accessToken = step3 as TokensStep
        Assert.assertNotNull("AccessToken should not be null", accessToken.oAuthTokenResponse.accessToken)
        Assert.assertNotNull("ExpiresIn should not be null", accessToken.oAuthTokenResponse.expiresIn)
    }

}

@ExperimentalCoroutinesApi
class MainCoroutineScopeRule(val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()) :
    TestWatcher(),
    TestCoroutineScope by TestCoroutineScope(dispatcher) {
    override fun starting(description: Description?) {
        super.starting(description)
        // If your codebase allows the injection of other dispatchers like
        // Dispatchers.Default and Dispatchers.IO, consider injecting all of them here
        // and renaming this class to `CoroutineScopeRule`
        //
        // All injected dispatchers in a test should point to a single instance of
        // TestCoroutineDispatcher.
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        cleanupTestCoroutines()
        Dispatchers.resetMain()
    }
}
