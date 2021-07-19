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

import androidx.lifecycle.*
import io.curity.haapidemo.ProfileIndex
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import kotlin.IllegalArgumentException

class ProfileViewModel(
    private val repository: HaapiFlowConfigurationRepository,
    private val configuration: HaapiFlowConfiguration,
    private val indexConfiguration: Int
): ViewModel() {

    private var _list: MutableLiveData<MutableList<ProfileItem>> = MutableLiveData()
    val listLiveData: LiveData<List<ProfileItem>>
        get() = _list.map {
            it.toList()
        }

    fun initialize() {
        val newList: MutableList<ProfileItem> = mutableListOf()
        ProfileIndex.values().forEach { index ->
            when (index) {
                ProfileIndex.SectionBaseConfiguration -> { newList.add(ProfileItem.Header(title = "Base Configuration")) }
                ProfileIndex.ItemName -> { newList.add(ProfileItem.Content(header = "Name", text = configuration.name)) }
                ProfileIndex.ItemClientId -> { newList.add(ProfileItem.Content(header = "Client ID", text = configuration.clientId)) }
                ProfileIndex.ItemBaseURL -> { newList.add(ProfileItem.Content(header = "Base URL", text = configuration.baseURLString)) }
                ProfileIndex.ItemRedirectURI -> { newList.add(ProfileItem.Content(header = "Redirect URI", text = configuration.redirectURI)) }

                ProfileIndex.SectionMetaData -> { newList.add(ProfileItem.Header(title = "META DATA Configuration")) }
                ProfileIndex.ItemMetaDataURL -> { newList.add(ProfileItem.Content(header = "Meta Data URL", text = configuration.metaDataBaseURLString)) }

                ProfileIndex.SectionEndpoints -> { newList.add(ProfileItem.Header(title = "Endpoints")) }
                ProfileIndex.ItemTokenEndpointURI -> { newList.add(ProfileItem.Content(header = "Token endpoint URI", text = configuration.tokenEndpointURI)) }
                ProfileIndex.ItemAuthorizationEndpointURI -> { newList.add(ProfileItem.Content(header = "Authorization endpoint URI", text = configuration.authorizationEndpointURI)) }

                ProfileIndex.SectionToggles -> { newList.add(ProfileItem.Header(title = "Toggles")) }
                ProfileIndex.ItemFollowRedirect -> { newList.add(ProfileItem.Toggle(label = "Follow redirect", isToggled = configuration.followRedirect)) }
                ProfileIndex.ItemAutomaticPolling -> { newList.add(ProfileItem.Toggle(label = "Automatic polling", isToggled = configuration.isAutoPollingEnabled)) }
                ProfileIndex.ItemAutoAuthorizationChallenged -> { newList.add(ProfileItem.Toggle(label = "Automatic authorization challenge", isToggled = configuration.isAutoAuthorizationChallengedEnabled)) }
                ProfileIndex.ItemSSLTrustVerification -> { newList.add(ProfileItem.Toggle(label = "Enable SSL Trust Verification", isToggled = configuration.isSSLTrustVerificationEnabled)) }
            }
        }

        assert(newList.size == ProfileIndex.values().size) { "Size is not matching for ProfileItem list... ${newList.size} VS ${ProfileIndex.values().size}" }
        _list.postValue(newList)
    }

    suspend fun update(value: String, atIndex: ProfileIndex) {
        when (atIndex) {
            ProfileIndex.ItemName -> { configuration.name = value }
            ProfileIndex.ItemClientId -> { configuration.clientId = value }
            ProfileIndex.ItemBaseURL -> { configuration.baseURLString = value }
            ProfileIndex.ItemRedirectURI -> { configuration.redirectURI = value }
            ProfileIndex.ItemMetaDataURL -> { configuration.metaDataBaseURLString = value }
            ProfileIndex.ItemTokenEndpointURI -> { configuration.tokenEndpointURI = value }
            ProfileIndex.ItemAuthorizationEndpointURI -> { configuration.authorizationEndpointURI = value }

            else -> throw IllegalArgumentException("Invalid index $atIndex for updating a String to configuration")
        }
        val oldProfileItem = _list.value!![atIndex.ordinal] as ProfileItem.Content
        val newProfileItem = ProfileItem.Content(header = oldProfileItem.header, text = value)
        val newList =_list.value!!
        newList[atIndex.ordinal] = newProfileItem
        _list.postValue(newList)
        repository.updateConfiguration(configuration, indexConfiguration)
    }

    suspend fun updateBoolean(index: ProfileIndex) {
        when (index) {
            ProfileIndex.ItemFollowRedirect -> { configuration.followRedirect = !configuration.followRedirect }
            ProfileIndex.ItemAutomaticPolling -> { configuration.isAutoPollingEnabled = !configuration.isAutoPollingEnabled }
            ProfileIndex.ItemAutoAuthorizationChallenged -> { configuration.isAutoAuthorizationChallengedEnabled = !configuration.isAutoAuthorizationChallengedEnabled }
            ProfileIndex.ItemSSLTrustVerification -> { configuration.isSSLTrustVerificationEnabled = !configuration.isSSLTrustVerificationEnabled }

            else -> throw IllegalArgumentException("Invalid index $index for updating a Boolean to configuration")
        }
        repository.updateConfiguration(configuration, indexConfiguration)
    }

    suspend fun makeConfigurationActive() {
        repository.setActiveConfiguration(configuration)
    }
}

class ProfileViewModelFactory(
    private val repository: HaapiFlowConfigurationRepository,
    private val configuration: HaapiFlowConfiguration,
    private val index: Int
    ): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, configuration, index) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class ProfileViewModel")
    }
}