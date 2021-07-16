/*
 *  Copyright 2021 Curity AB
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
package io.curity.haapidemo.ui.settings

import androidx.lifecycle.*
import io.curity.haapidemo.flow.HaapiFlowConfiguration

class SettingsListViewModel(
    private val repository: HaapiFlowConfigurationRepository
) : ViewModel() {

    private val configsFlow = repository.configurationsFlow

    private val _text = MutableLiveData<String>().apply {
        value = "This is a Fragment"
    }
    val text: LiveData<String> = _text

    val configurations: LiveData<MutableList<HaapiFlowConfiguration>> = configsFlow.asLiveData()

    suspend fun addNewConfiguration(): HaapiFlowConfiguration {
        val result = HaapiFlowConfiguration.newInstance("haapi-android-client")
        repository.appendNewConfiguration(result)
        return result
    }

    suspend fun updateConfiguration(config: HaapiFlowConfiguration, index: Int) {
        repository.updateConfiguration(config, index)
    }

    fun configurationAt(index: Int): HaapiFlowConfiguration? {
        return configurations.value?.get(index)
    }
}

class SettingsListViewModelFactory(
    private val repository: HaapiFlowConfigurationRepository
): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsListViewModel::class.java)) {
            return SettingsListViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class SettingsListViewModel")
    }
}

private fun HaapiFlowConfiguration.Companion.newInstance(name: String): HaapiFlowConfiguration {
    return HaapiFlowConfiguration(
        name = name,
        clientId = "haapi-android-client",
        baseURLString = "https://",
        tokenEndpointURI = "https:///dev/oauth/token",
        authorizationEndpointURI = "https:///dev/oauth/authorize",
        metaDataBaseURLString = "",
        redirectURI = "haapi:start",
        followRedirect = true,
        isSSLTrustVerificationEnabled = false,
        selectedScopes = listOf("open", "profile")
    )
}