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

package io.curity.haapidemo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import io.curity.haapidemo.ui.settings.HaapiFlowConfigurationRepository
import kotlinx.coroutines.Dispatchers

class ActiveHaapiConfigViewModel(repository: HaapiFlowConfigurationRepository): ViewModel() {
    val haapiFlowConfiguration = repository.activeConfigurationFlow.asLiveData(Dispatchers.IO)
}

class ActiveHaapiConfigViewModelFactory(
    private val repository: HaapiFlowConfigurationRepository
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveHaapiConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActiveHaapiConfigViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class ActiveHaapiConfigViewModel")
    }
}