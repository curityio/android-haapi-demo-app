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

import androidx.lifecycle.*
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.flow.HaapiFlowManager
import io.curity.haapidemo.models.*
import io.curity.haapidemo.models.haapi.Link
import io.curity.haapidemo.models.haapi.actions.Action
import io.curity.haapidemo.models.haapi.actions.ActionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HaapiFlowViewModel(haapiFlowConfiguration: HaapiFlowConfiguration): ViewModel() {

    private val haapiFlowManager = HaapiFlowManager(haapiFlowConfiguration = haapiFlowConfiguration)
    val isAutoPolling = haapiFlowConfiguration.isAutoPollingEnabled

    private var _liveStep = MutableLiveData<HaapiStep?>(null)
    val liveStep: LiveData<HaapiStep?>
        get() = _liveStep

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    val redirectURI = haapiFlowConfiguration.redirectURI

    private fun executeHaapi(haapiStepCommand: suspend () -> HaapiStep) {
        _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val step = haapiStepCommand()
            _isLoading.postValue(false)
            processStep(step)
        }
    }

    fun start() {
        executeHaapi { haapiFlowManager.start() }
    }

    fun submit(form: ActionModel.Form, parameters: Map<String, String> = emptyMap()) {
        executeHaapi {
            haapiFlowManager.submitForm(
                form = form,
                parameters = parameters
            )
        }
    }

    fun fetchAccessToken(authorizationCode: String) {
        executeHaapi {
            haapiFlowManager.fetchAccessToken(authorizationCode)
        }
    }

    fun refreshAccessToken(refreshToken: String) {
        executeHaapi {
            haapiFlowManager.refreshAccessToken(refreshToken)
        }
    }

    fun followLink(link: Link) {
        executeHaapi {
            haapiFlowManager.followLink(link)
        }
    }

    fun applyActionForm(actionForm: Action.Form) {
        executeHaapi {
            haapiFlowManager.applyActionForm(actionForm = actionForm)
        }
    }

    private fun processStep(haapiStep: HaapiStep) {
        val currentStep = liveStep.value
        if (haapiStep is PollingStep && currentStep is PollingStep && currentStep.isContentTheSame(haapiStep)) {
            // We do not post a new value as the pollingStep is the same. Avoiding to have a flickering for the progressBar
            return
        }
        _liveStep.postValue(haapiStep)
    }

    fun interrupt(title: String, description: String) {
        processStep(
            SystemErrorStep(
                title,
                description
            )
        )
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

private fun PollingStep.isContentTheSame(pollingStep: PollingStep): Boolean {
    return this.properties.status == pollingStep.properties.status
            && this.type.discriminator == pollingStep.type.discriminator
            && this.main.model.href == pollingStep.main.model.href
}