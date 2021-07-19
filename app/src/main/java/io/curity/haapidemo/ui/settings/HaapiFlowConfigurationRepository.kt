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
package io.curity.haapidemo.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

class HaapiFlowConfigurationRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val CONFIGS = stringPreferencesKey("io.curity.haapidemo.haapiflowconfigurationrepository.configurations")
        val ACTIVE_CONFIG = stringPreferencesKey("io.curity.haapidemo.haapiflowconfigurationrepository.active_configuration")
    }

    val configurationsFlow: Flow<List<HaapiFlowConfiguration>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.CONFIGS].toHaapiConfigurations()
        }

    val activeConfigurationFlow: Flow<HaapiFlowConfiguration> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVE_CONFIG].toHaapiConfiguration()
        }

    /**
     * Appends a new [config] to the repository list
     *
     * @param config A HaapiFlowConfiguration
     */
    suspend fun appendNewConfiguration(config: HaapiFlowConfiguration) {
        dataStore.edit {
            val list = it[PreferencesKeys.CONFIGS].toHaapiConfigurations()
            list.add(config)
            it[PreferencesKeys.CONFIGS] = Json.encodeToString(list)
        }
    }

    /**
     * Updates the configuration repository with a new [config] at the specified [index].
     * If the [index] is equal to -1 then the [config] will update the activeConfiguration of the repository.
     * If the [index] is out of bound of the repository list then nothing happens.
     * Otherwise, the [config] will replace the previous [config] in the repository list.
     *
     * @param config A HaapiFlowConfiguration
     * @param index An integer
     */
    suspend fun updateConfiguration(config: HaapiFlowConfiguration, index: Int) {
        dataStore.edit { preferences ->
            if (index == -1) {
                preferences[PreferencesKeys.ACTIVE_CONFIG] = Json.encodeToString(config)
            } else {
                val list = preferences[PreferencesKeys.CONFIGS].toHaapiConfigurations()
                if (list.isNotEmpty() && index < list.size) {
                    list[index] = config
                    preferences[PreferencesKeys.CONFIGS] = Json.encodeToString(list)
                }
            }
        }
    }

    /**
     * Removes a [config] from the repository list
     *
     * @param config A HaapiFlowConfiguration
     */
    suspend fun removeConfiguration(config: HaapiFlowConfiguration) {
        dataStore.edit { preferences ->
            val list = preferences[PreferencesKeys.CONFIGS].toHaapiConfigurations()
            list.remove(config)
            preferences[PreferencesKeys.CONFIGS] = Json.encodeToString(list)
        }
    }

    /**
     * Sets a new [config] as the new Active Configuration and moves the former active configuration to the repository
     * list. It will remove the [config] from the repository list if it is present.
     *
     * @param config A HaapiFlowConfiguration
     */
    suspend fun setActiveConfiguration(config: HaapiFlowConfiguration) {
        dataStore.edit { preferences ->
            val oldActiveConfig = preferences[PreferencesKeys.ACTIVE_CONFIG].toHaapiConfiguration()
            val list = preferences[PreferencesKeys.CONFIGS].toHaapiConfigurations()
            list.remove(config)
            list.add(oldActiveConfig)

            preferences[PreferencesKeys.CONFIGS] = Json.encodeToString(list)
            preferences[PreferencesKeys.ACTIVE_CONFIG] = Json.encodeToString(config)
        }
    }
}

//region Private extension for String
private fun String?.toHaapiConfigurations(): MutableList<HaapiFlowConfiguration> {
    return if (this != null) {
        Json.decodeFromString(this)
    } else {
        arrayListOf()
    }
}

private fun String?.toHaapiConfiguration(): HaapiFlowConfiguration {
    return if (this != null) {
        Json.decodeFromString(this)
    } else {
        HaapiFlowConfiguration.newInstance()
    }
}
//endregion