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

package io.curity.haapidemo.ui.haapiflow

import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import io.curity.haapidemo.Constant
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.utils.disableSslTrustVerification
import kotlinx.coroutines.*
import se.curity.haapi.models.android.sdk.HaapiConfiguration
import se.curity.haapi.models.android.sdk.HaapiManager
import se.curity.haapi.models.android.sdk.OAuthTokenService
import se.curity.haapi.models.android.sdk.models.haapi.*
import se.curity.haapi.models.android.sdk.models.haapi.actions.Action
import se.curity.haapi.models.android.sdk.models.haapi.actions.ActionModel
import se.curity.haapi.models.android.sdk.models.oauth.OAuthResponse
import java.net.HttpURLConnection
import java.net.URI

class HaapiFlowViewModel(private val haapiFlowConfiguration: HaapiFlowConfiguration): ViewModel() {

    val haapiConfiguration: HaapiConfiguration = HaapiConfiguration(
        keyStoreAlias = haapiFlowConfiguration.keyStoreAlias,
        clientId = haapiFlowConfiguration.clientId,
        baseURI = URI.create(haapiFlowConfiguration.baseURLString),
        tokenEndpointURI = URI.create(haapiFlowConfiguration.tokenEndpointURI),
        authorizationEndpointURI = URI.create(haapiFlowConfiguration.authorizationEndpointURI),
        appRedirectURIString = haapiFlowConfiguration.redirectURI,
        isAutoRedirect = haapiFlowConfiguration.followRedirect,
        scopes = haapiFlowConfiguration.selectedScopes,
        httpURLConnectionProvider = { url ->
            val urlConnection = url.openConnection()
            urlConnection.connectTimeout = 8000
            if (!haapiFlowConfiguration.isSSLTrustVerificationEnabled) {
                urlConnection.disableSslTrustVerification() as HttpURLConnection
            } else {
                urlConnection as HttpURLConnection
            }
        }
    )
    private val haapiManager: HaapiManager = HaapiManager(haapiConfiguration = haapiConfiguration)
    private val oAuthTokenService: OAuthTokenService = OAuthTokenService(haapiConfiguration = haapiConfiguration)
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, _ -> }

    val isAutoPolling = haapiFlowConfiguration.isAutoPollingEnabled

    private var _liveStep = MutableLiveData<HaapiResult?>(null)
    val liveStep: LiveData<HaapiResult?>
        get() = _liveStep

    private var _liveOAuthResponse = MutableLiveData<Result<OAuthResponse>?>(null)
    val liveOAuthResponse: LiveData<Result<OAuthResponse>?>
        get() = _liveOAuthResponse

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private fun executeHaapi(haapiResultCommand: suspend () -> HaapiResult) {
        _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                ensureActive()
                val result = haapiResultCommand()
                ensureActive()
                _isLoading.postValue(false)
                processHaapiResult(result)
            } catch (e: CancellationException) {
                coroutineExceptionHandler.handleException(coroutineContext, e)
                processHaapiResult(HaapiResult.failure(e))
            }
        }
    }

    fun start() {
        executeHaapi { haapiManager.start() }
    }

    fun submit(form: ActionModel.FormActionModel, parameters: Map<String, String> = emptyMap()) {
        executeHaapi {
            haapiManager.submitForm(form, parameters)
        }
    }

    fun fetchAccessToken(authorizationCode: String) {
        _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = oAuthTokenService.fetchAccessToken(authorizationCode)
            _isLoading.postValue(false)
            _liveOAuthResponse.postValue(result)
        }
    }

    fun refreshAccessToken(refreshToken: String) {
        _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = oAuthTokenService.refreshAccessToken(refreshToken)
            _isLoading.postValue(false)
            _liveOAuthResponse.postValue(result)
        }
    }

    fun followLink(link: Link) {
        executeHaapi {
            haapiManager.followLink(link)
        }
    }

    fun applyActionForm(actionForm: Action.Form) {
        executeHaapi {
            haapiManager.applyActionForm(actionForm)
        }
    }

    fun canHandleIntent(intent: Intent): Boolean {
        return haapiManager.canHandleIntent(intent)
    }

    fun formattedParamsForIntent(intent: Intent): Map<String, String> {
        return haapiManager.formattedParamsForIntent(intent)
    }

    private fun processHaapiResult(haapiResult: HaapiResult) {
        val currentResponse = liveStep.value?.getOrNull()
        val latestResponse = haapiResult.getOrNull()
        if (latestResponse is PollingStep && currentResponse is PollingStep && latestResponse.isContentTheSame(currentResponse)) {
            // We do not post a new value as the pollingStep is the same. Avoiding to have a flickering for the progressBar
            return
        }

        // Handle automatic fetchAccessToken
        if (latestResponse is OAuthAuthorizationResponseStep &&
            haapiFlowConfiguration.isAutoAuthorizationChallengedEnabled) {
            val authorizationCode = latestResponse.properties.code
            if (authorizationCode != null) {
                fetchAccessToken(authorizationCode)
            }
        }
        _liveStep.postValue(haapiResult)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
        haapiManager.close()
    }
}

class HaapiFlowViewModelFactory(val haapiFlowConfiguration: HaapiFlowConfiguration): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HaapiFlowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HaapiFlowViewModel(haapiFlowConfiguration) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class HaapiFlowViewModel")
    }
}

private fun PollingStep.isContentTheSame(pollingStep: PollingStep): Boolean {
    return this.properties.status == pollingStep.properties.status
            && this.type.discriminator == pollingStep.type.discriminator
            && this.mainAction.model.href == pollingStep.mainAction.model.href
}
