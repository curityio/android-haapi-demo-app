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

import android.content.Context
import androidx.lifecycle.*
import io.curity.haapidemo.Configuration
import io.curity.haapidemo.utils.HaapiSdkFactory
import kotlinx.coroutines.*
import se.curity.identityserver.haapi.android.sdk.*
import se.curity.identityserver.haapi.android.sdk.models.HaapiResponse
import se.curity.identityserver.haapi.android.sdk.models.Link
import se.curity.identityserver.haapi.android.sdk.models.OAuthAuthorizationResponseStep
import se.curity.identityserver.haapi.android.sdk.models.PollingStep
import se.curity.identityserver.haapi.android.sdk.models.actions.FormActionModel
import se.curity.identityserver.haapi.android.sdk.models.oauth.TokenResponse
import kotlin.coroutines.CoroutineContext

typealias HaapiResult = Result<HaapiResponse>
typealias OAuthResponse = Result<TokenResponse>

class HaapiFlowViewModel(private val configuration: Configuration): ViewModel() {

    private val sdkFactory = HaapiSdkFactory(configuration)

    // These now require async work to be created
    private var oAuthTokenManager: OAuthTokenManager? = null
    private var haapiManager: HaapiManager? = null
    private var haapiConfiguration: HaapiConfiguration? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, _ -> }

    val isAutoPolling = configuration.isAutoPollingEnabled

    private var _liveStep = MutableLiveData<HaapiResult?>(null)
    val liveStep: LiveData<HaapiResult?>
        get() = _liveStep

    private var _liveOAuthResponse = MutableLiveData<OAuthResponse?>(null)
    val liveOAuthResponse: LiveData<OAuthResponse?>
        get() = _liveOAuthResponse

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private fun executeHaapi(haapiResultCommand: suspend (coroutineContext: CoroutineContext) -> HaapiResponse) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                ensureActive()
                val result = haapiResultCommand(Dispatchers.IO + coroutineExceptionHandler)
                ensureActive()
                _isLoading.postValue(false)
                processHaapiResult(HaapiResult.success(result))
            } catch (e: Exception) {
                coroutineExceptionHandler.handleException(coroutineContext, e)
                processHaapiResult(HaapiResult.failure(e))
            }
        }
    }

    /*
     * The view model must now do async processing before objects can be created
     */
    fun start(context: Context) {

        viewModelScope.launch {

            // The app first asks the SDK to initialize, which will also load the client details if needed
            // This may not require async processing, but the only way to do it currently is like this
            val accessor = withContext(Dispatchers.IO) {
                sdkFactory.load(context, this.coroutineContext)
            }

            // The app should be able to ask the accessor for the token manager, so that it does not need to manage the dynamic client
            // oAuthTokenManager = accessor.tokenManager
            oAuthTokenManager = sdkFactory.createOAuthTokenManager()

            // The accessor should provide the HaapiConfiguration with the correct client ID
            // haapiConfiguration = accessor.configuration
            haapiConfiguration = configuration.toHaapiConfiguration(accessor.clientId)

            // If the app does not have an access token it will create a HAAPI manager
            // This will be async and use the SDK's current design
            //      val haapiManager = withContext(Dispatchers.IO) {
            //          sdkFactory.createHaapiManager(context, this.coroutineContext)
            //      }
            haapiManager = accessor.haapiManager

            // The code example can then start a HAAPI flow when required
            startHaapi()
        }
    }

    /*
     * Return the configuration with the correct Client ID to the FlowActivity
     */
    fun getHaapiConfiguration(): HaapiConfiguration {
        return this.haapiConfiguration!!
    }

    private fun startHaapi() {

        executeHaapi {
            haapiManager!!.start(
                authorizationParameters = OAuthAuthorizationParameters(
                    scope = configuration.selectedScopes
                ),
                it
            )
        }
    }

    fun submit(form: FormActionModel, parameters: Map<String, String> = emptyMap()) {
        executeHaapi {
            haapiManager!!.submitForm(form, parameters, it)
        }
    }

    fun fetchAccessToken(authorizationCode: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                oAuthTokenManager!!.fetchAccessToken(authorizationCode, this.coroutineContext)
            }
            _isLoading.postValue(false)
            _liveOAuthResponse.postValue(OAuthResponse.success(result))
        }
    }

    fun fetchAccessToken(oAuthAuthorizationResponseStep: OAuthAuthorizationResponseStep) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                oAuthTokenManager!!.fetchAccessToken(
                    oAuthAuthorizationResponseStep.properties.code,
                    this.coroutineContext
                )
            }
            _isLoading.postValue(false)
            _liveOAuthResponse.postValue(OAuthResponse.success(result))
        }
    }

    fun refreshAccessToken(refreshToken: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                oAuthTokenManager!!.refreshAccessToken(refreshToken, this.coroutineContext)
            }
            _isLoading.postValue(false)
            _liveOAuthResponse.postValue(OAuthResponse.success(result))
        }
    }

    fun followLink(link: Link) {
        executeHaapi {
            haapiManager!!.followLink(link, it)
        }
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
            configuration.isAutoAuthorizationChallengedEnabled) {
            fetchAccessToken(latestResponse)
        } else {
            _liveStep.postValue(haapiResult)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
        if (haapiManager != null) {
            haapiManager!!.close()
        }
    }
}

class HaapiFlowViewModelFactory(val configuration: Configuration): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HaapiFlowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HaapiFlowViewModel(configuration) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class HaapiFlowViewModel")
    }
}

private fun PollingStep.isContentTheSame(pollingStep: PollingStep): Boolean {
    return this.properties.status == pollingStep.properties.status
            && this.type == pollingStep.type
            && this.mainAction.model.href == pollingStep.mainAction.model.href
}
