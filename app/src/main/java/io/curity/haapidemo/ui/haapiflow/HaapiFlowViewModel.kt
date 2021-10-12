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

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import io.curity.haapidemo.Constant
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.flow.HaapiFlowManager
import io.curity.haapidemo.models.*
import io.curity.haapidemo.models.haapi.Link
import io.curity.haapidemo.models.haapi.RepresentationType
import io.curity.haapidemo.models.haapi.actions.Action
import io.curity.haapidemo.models.haapi.actions.ActionModel
import io.curity.haapidemo.models.haapi.problems.AuthorizationProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.StringBuilder
import java.util.*

class HaapiFlowViewModel(haapiFlowConfiguration: HaapiFlowConfiguration): ViewModel() {

    private val haapiFlowManager = HaapiFlowManager(haapiFlowConfiguration = haapiFlowConfiguration)
    val isAutoPolling = haapiFlowConfiguration.isAutoPollingEnabled

    private var _liveStep = MutableLiveData<HaapiStep?>(null)
    val liveStep: LiveData<HaapiStep?>
        get() = _liveStep

    private var _problemStepLiveData = MutableLiveData<ProblemStep?>(null)
    val problemStepLiveData: LiveData<ProblemStep?>
        get() = _problemStepLiveData

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

    fun followLink(link: Link) {
        executeHaapi {
            haapiFlowManager.followLink(link)
        }
    }

    fun applyActions(actions: List<Action.Form>) {
        Log.d(Constant.TAG, "There is/are ${actions.count()} actions. We are only dealing with the first one...")
        val firstAction = actions.firstOrNull()
        if (firstAction != null) {
            val newStep = when (firstAction.kind) {
                "redirect" -> {
                    Redirect(
                        action = firstAction
                    )
                }
                else -> {
                    InteractiveForm(
                    actions = actions,
                    type = RepresentationType.AuthenticationStep,
                    cancel = null,
                    links = emptyList(),
                    messages = emptyList())
                }
            }

            if (newStep is Redirect && haapiFlowManager.haapiFlowConfiguration.followRedirect) {
                submit(firstAction.model, emptyMap())
            } else {
                processStep(newStep)
            }
        } else {
            processStep(
                SystemErrorStep(
                    title = "Something went wrong",
                    description = "No available action"
                )
            )
        }
    }

    private fun processStep(haapiStep: HaapiStep) {
        // Any ProblemStep that comes when the step is:
        // Redirect -> SystemError
        // PollingStep -> SystemError
        // UserConsent -> SystemError
        val processingStep: HaapiStep = if (haapiStep is ProblemStep
            && (liveStep.value is Redirect || liveStep.value is PollingStep || liveStep.value is UserConsentStep)) {
                val description = StringBuilder()
                haapiStep.problem.messages?.forEach { userMessage ->
                    userMessage.text.message.let {
                        description.append(it)
                    }
                }
                if (haapiStep.problem is AuthorizationProblem) {
                    description.append(haapiStep.problem.errorDescription)
                }

                SystemErrorStep(
                    haapiStep.problem.title,
                    description.toString()
                )
        } else {
            haapiStep
        }

        if (processingStep is ProblemStep) {
            _problemStepLiveData.postValue(processingStep)
            return
        } else if (processingStep is PollingStep && liveStep.value is PollingStep) {
            val currentStep = liveStep.value as PollingStep
            if (processingStep.isContentTheSame(currentStep)) {
                return
            }
        } else {
            _problemStepLiveData.postValue(null)
        }
        _liveStep.postValue(processingStep)
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