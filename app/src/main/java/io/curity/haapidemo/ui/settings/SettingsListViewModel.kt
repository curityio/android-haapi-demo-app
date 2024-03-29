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
import io.curity.haapidemo.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsListViewModel(
    private val repository: HaapiFlowConfigurationRepository
) : ViewModel() {

    //region Private references
    private var activeConfiguration: Configuration? = null
    private var configurations: List<Configuration> = emptyList()

    private val flowModels: Flow<List<SettingsItem>> = combine(
        repository.activeConfigurationFlow,
        repository.configurationsFlow)
    { activeConfiguration: Configuration, list: List<Configuration> ->

        this.activeConfiguration = activeConfiguration
        configurations = list

        val newList: MutableList<SettingsItem> = mutableListOf()

        newList.add(SettingsItem.Header(title = "Active Profile"))
        newList.add(SettingsItem.Configuration(configuration = activeConfiguration))
        this.activeConfiguration = activeConfiguration

        newList.add(SettingsItem.Header(title = "Profiles"))
        newList.addAll(list.map { SettingsItem.Configuration(configuration = it) })

        return@combine newList
    }
    //endregion

    val models = flowModels.asLiveData(Dispatchers.IO)

    suspend fun addNewConfiguration(): Configuration {
        val result = Configuration.newInstance(configurations.size + 1)
        repository.appendNewConfiguration(result)
        return result
    }

    fun removeConfigurationAt(index: Int) {
        val item = models.value!![index] as SettingsItem.Configuration
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeConfiguration(item.configuration)

        }
    }

    fun settingsItemCanBeSwippedAt(index: Int): Boolean {
        return when (val model = models.value!![index]) {
            is SettingsItem.Header -> { false }
            is SettingsItem.Configuration -> {
                model.configuration != activeConfiguration
            }
        }
    }

    fun isActiveConfiguration(config: Configuration): Boolean {
        return activeConfiguration == config
    }
}

class SettingsListViewModelFactory(
    private val repository: HaapiFlowConfigurationRepository
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsListViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class SettingsListViewModel")
    }
}