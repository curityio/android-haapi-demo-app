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

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.flow.HaapiFlowManager
import io.curity.haapidemo.models.*
import io.curity.haapidemo.models.haapi.Link
import io.curity.haapidemo.models.haapi.actions.ActionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HaapiFlowViewModel(haapiFlowConfiguration: HaapiFlowConfiguration): ViewModel() {

    val haapiFlowManager = HaapiFlowManager(haapiFlowConfiguration = haapiFlowConfiguration)

    val liveStep: LiveData<HaapiStep?>
        get() = haapiFlowManager.liveStep

    private val _haapiUIBundleLiveData = MutableLiveData<HaapiUIBundle>(HaapiUIBundle(title = "", fragment = EmptyFragment()))
    val haapiUIBundleLiveData: LiveData<HaapiUIBundle>
        get() = _haapiUIBundleLiveData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    val actionModelForm: ActionModel.Form?
        get() {
            return when (val step = liveStep.value) {
                is Redirect -> { step.action.model }
                else -> { return null }
            }
        }

    fun start() {
        viewModelScope.launch {
            val step =
                withContext(Dispatchers.IO) { haapiFlowManager.start() }
            processStep(step)
        }
    }

    fun submit(form: ActionModel.Form, parameters: Map<String, String> = emptyMap()) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val step = withContext(Dispatchers.IO) {
                haapiFlowManager.submitForm(
                    form = form,
                    parameters = parameters
                )
            }
            _isLoading.postValue(false)
            processStep(step)
        }
    }

    fun fetchAccessToken(authorizationCode: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val step = withContext(Dispatchers.IO) {
                haapiFlowManager.fetchAccessToken(authorizationCode)
            }
            _isLoading.postValue(false)
            processStep(step)
        }

    }

    fun followLink(link: Link) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val step = withContext(Dispatchers.IO) {
                haapiFlowManager.followLink(link)
            }
            _isLoading.postValue(false)
            processStep(step)
        }
    }

    private fun processStep(haapiStep: HaapiStep) {
        when (haapiStep) {
            is Redirect -> {
                _haapiUIBundleLiveData.postValue(HaapiUIBundle(title = haapiStep.action.kind, fragment = RedirectFragment.newInstance() ))
            }
            is AuthenticatorSelector -> {
                _haapiUIBundleLiveData.postValue(HaapiUIBundle(title = haapiStep.title.message, fragment = AuthenticatorSelectorFragment.newInstance()))
            }
            is InteractiveForm -> {
                _haapiUIBundleLiveData.postValue(
                    HaapiUIBundle(
                        title = haapiStep.action.title?.message ?: "InteractiveForm",
                        fragment = InteractiveFormFragment.newInstance()
                    )
                )
            }
            is TokensStep -> {
                _haapiUIBundleLiveData.postValue(
                    HaapiUIBundle(
                        title = "Success",
                        fragment = TokensFragment.newInstance(haapiStep.oAuthTokenResponse)
                    )
                )
            }
            is AuthorizationCompleted -> {
                _haapiUIBundleLiveData.postValue(
                    HaapiUIBundle(
                        title = haapiStep.type.discriminator.capitalize(),
                        fragment = AuthorizationCompletedFragment.newInstance()
                    )
                )
            }
            is SystemErrorStep -> {
                // NOP
            }
            is ProblemStep -> {
                // NOP
            }
            else -> {
                _haapiUIBundleLiveData.postValue(HaapiUIBundle(title = "Not supported", fragment = RedirectFragment.newInstance("$haapiStep")))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        haapiFlowManager.close()
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

data class HaapiUIBundle(val title: String?, val fragment: Fragment)