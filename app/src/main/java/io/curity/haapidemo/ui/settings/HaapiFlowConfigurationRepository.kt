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
    }

    val configurationsFlow: Flow<MutableList<HaapiFlowConfiguration>> = dataStore.data
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

    suspend fun appendNewConfiguration(config: HaapiFlowConfiguration) {
        dataStore.edit {
            val list = it[PreferencesKeys.CONFIGS].toHaapiConfigurations()
            list.add(config)
            it[PreferencesKeys.CONFIGS] = Json.encodeToString(list)
        }
    }

    suspend fun updateConfiguration(config: HaapiFlowConfiguration, index: Int) {
        dataStore.edit { preferences ->
            val list = preferences[PreferencesKeys.CONFIGS].toHaapiConfigurations()
            list[index] = config
            preferences[PreferencesKeys.CONFIGS] = Json.encodeToString(list)
        }
    }

    suspend fun removeConfiguration(config: HaapiFlowConfiguration) {
        dataStore.edit { preferences ->
            val list = preferences[PreferencesKeys.CONFIGS].toHaapiConfigurations()
            list.remove(config)
            preferences[PreferencesKeys.CONFIGS] = Json.encodeToString(list)
        }
    }
}

private fun String?.toHaapiConfigurations(): MutableList<HaapiFlowConfiguration> {
    return if (this != null) {
        Json.decodeFromString<MutableList<HaapiFlowConfiguration>>(this)
    } else {
        arrayListOf<HaapiFlowConfiguration>()
    }
}