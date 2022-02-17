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
package io.curity.haapidemo.ui.settings.profile

import android.accounts.NetworkErrorException
import androidx.lifecycle.*
import io.curity.haapidemo.ProfileIndex
import io.curity.haapidemo.Configuration
import io.curity.haapidemo.ui.settings.HaapiFlowConfigurationRepository
import io.curity.haapidemo.utils.disableSslTrustVerification
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.Exception
import kotlin.IllegalArgumentException

class ProfileViewModel(
    private val repository: HaapiFlowConfigurationRepository,
    private var configuration: Configuration,
    private val isActiveConfiguration: Boolean,
    private val scopesAdapter: ScopesAdapter
): ViewModel() {

    private var editableConfiguration: Configuration

    private var _list: MutableLiveData<MutableList<ProfileItem>> = MutableLiveData()
    val listLiveData: LiveData<List<ProfileItem>>
        get() = _list.map {
            it.toList()
        }

    private var _scopesLiveData: MutableLiveData<MutableList<ProfileItem.Checkbox>> = MutableLiveData()
    val scopesLiveData: LiveData<List<ProfileItem.Checkbox>>
        get() = _scopesLiveData.map { it.toList() }

    init {
        editableConfiguration = configuration.copy()
        initialize()
    }

    private fun initialize() {
        val newList: MutableList<ProfileItem> = mutableListOf()
        ProfileIndex.values().forEach { index ->
            when (index) {
                ProfileIndex.SectionBaseConfiguration -> { newList.add(ProfileItem.Header(title = "Base Configuration")) }
                ProfileIndex.ItemName -> { newList.add(ProfileItem.Content(header = "Name", text = configuration.name)) }
                ProfileIndex.ItemClientId -> { newList.add(
                    ProfileItem.Content(
                        header = "Client ID",
                        text = configuration.clientId
                    )
                ) }
                ProfileIndex.ItemBaseURL -> { newList.add(
                    ProfileItem.Content(
                        header = "Base URL",
                        text = configuration.baseURLString
                    )
                ) }
                ProfileIndex.ItemRedirectURI -> { newList.add(
                    ProfileItem.Content(
                        header = "Redirect URI",
                        text = configuration.redirectURI
                    )
                ) }

                ProfileIndex.SectionMetaData -> { newList.add(ProfileItem.Header(title = "META DATA Configuration")) }
                ProfileIndex.ItemMetaDataURL -> { newList.add(
                    ProfileItem.Content(
                        header = "Meta Data URL",
                        text = configuration.metaDataBaseURLString
                    )
                ) }
                ProfileIndex.ItemLoadingMetaData -> { newList.add(
                    ProfileItem.LoadingAction(
                        text = "Fetch latest configuration",
                        dateLong = System.currentTimeMillis()
                    )
                ) }

                ProfileIndex.SectionEndpoints -> { newList.add(ProfileItem.Header(title = "Endpoints")) }
                ProfileIndex.ItemTokenEndpointURI -> { newList.add(
                    ProfileItem.Content(
                        header = "Token endpoint URI",
                        text = configuration.tokenEndpointURI
                    )
                ) }
                ProfileIndex.ItemAuthorizationEndpointURI -> { newList.add(
                    ProfileItem.Content(
                        header = "Authorization endpoint URI",
                        text = configuration.authorizationEndpointURI
                    )
                ) }
                ProfileIndex.ItemUserinfoEndpointURI -> { newList.add(
                    ProfileItem.Content(
                        header = "User info endpoint URI",
                        text = configuration.userInfoEndpointURI
                    )
                ) }

                ProfileIndex.SectionSupportedScopes -> { newList.add(ProfileItem.Header(title = "Supported scopes")) }
                ProfileIndex.ItemScopes -> {
                    newList.add(ProfileItem.Recycler(adapter = scopesAdapter))
                }

                ProfileIndex.SectionToggles -> { newList.add(ProfileItem.Header(title = "Toggles")) }
                ProfileIndex.ItemFollowRedirect -> { newList.add(
                    ProfileItem.Toggle(
                        label = "Follow redirect",
                        isToggled = configuration.followRedirect
                    )
                ) }
                ProfileIndex.ItemAutomaticPolling -> { newList.add(
                    ProfileItem.Toggle(
                        label = "Automatic polling",
                        isToggled = configuration.isAutoPollingEnabled
                    )
                ) }
                ProfileIndex.ItemAutoAuthorizationChallenged -> { newList.add(
                    ProfileItem.Toggle(
                        label = "Automatic authorization challenge",
                        isToggled = configuration.isAutoAuthorizationChallengedEnabled
                    )
                ) }
                ProfileIndex.ItemSSLTrustVerification -> { newList.add(
                    ProfileItem.Toggle(
                        label = "Enable SSL Trust Verification",
                        isToggled = configuration.isSSLTrustVerificationEnabled
                    )
                ) }
            }
        }

        _list.postValue(newList)

        refreshScopes()
    }

    fun update(value: String, atIndex: ProfileIndex) {
        viewModelScope.launch(Dispatchers.IO) {
            when (atIndex) {
                ProfileIndex.ItemName -> { editableConfiguration.name = value }
                ProfileIndex.ItemClientId -> { editableConfiguration.clientId = value }
                ProfileIndex.ItemBaseURL -> { editableConfiguration.baseURLString = value }
                ProfileIndex.ItemRedirectURI -> { editableConfiguration.redirectURI = value }
                ProfileIndex.ItemMetaDataURL -> { editableConfiguration.metaDataBaseURLString = value }
                ProfileIndex.ItemTokenEndpointURI -> { editableConfiguration.tokenEndpointURI = value }
                ProfileIndex.ItemAuthorizationEndpointURI -> { editableConfiguration.authorizationEndpointURI = value }
                ProfileIndex.ItemUserinfoEndpointURI -> { editableConfiguration.userInfoEndpointURI = value }

                else -> throw IllegalArgumentException("Invalid index $atIndex for updating a String to configuration")
            }
            val oldProfileItem = _list.value!![atIndex.ordinal] as ProfileItem.Content
            val newProfileItem = ProfileItem.Content(header = oldProfileItem.header, text = value)
            val newList =_list.value!!
            newList[atIndex.ordinal] = newProfileItem
            _list.postValue(newList)

            updateConfiguration()
        }
    }

    fun updateBoolean(index: ProfileIndex) {
        viewModelScope.launch(Dispatchers.IO) {
            when (index) {
                ProfileIndex.ItemFollowRedirect -> { editableConfiguration.followRedirect = !configuration.followRedirect }
                ProfileIndex.ItemAutomaticPolling -> { editableConfiguration.isAutoPollingEnabled = !configuration.isAutoPollingEnabled }
                ProfileIndex.ItemAutoAuthorizationChallenged -> { editableConfiguration.isAutoAuthorizationChallengedEnabled = !configuration.isAutoAuthorizationChallengedEnabled }
                ProfileIndex.ItemSSLTrustVerification -> { editableConfiguration.isSSLTrustVerificationEnabled = !configuration.isSSLTrustVerificationEnabled }

                else -> throw IllegalArgumentException("Invalid index $index for updating a Boolean to configuration")
            }

            updateConfiguration()
        }
    }

    fun makeConfigurationActive() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveConfiguration(editableConfiguration)
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().disableSslTrustVerification().build()
    }

    /**
     * Fetches the metaData from [Configuration.metaDataBaseURLString]
     *
     * If it succeeds then [listLiveData] and [scopesLiveData] will be triggered.
     * If it fails then an [Exception] will be thrown.
     * @exception Exception An [IllegalArgumentException] or a generic [Exception]
     */
    fun fetchMetaData(coroutineExceptionHandler: CoroutineExceptionHandler) {
        val metaDataURLString = editableConfiguration.metaDataBaseURLString.plus("/.well-known/openid-configuration")
        try {
            val request = Request.Builder()
                .url(metaDataURLString)
                .get()
                .build()

            viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
                try {
                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body

                    if (!response.isSuccessful || responseBody == null) {
                        throw NetworkErrorException("Impossible to fetch the meta data")
                    }

                    val jsonObject = JSONObject(responseBody.string())

                    val scopesJSONArray = jsonObject.getJSONArray("scopes_supported")
                    val scopesList = List(scopesJSONArray.length()) {
                        scopesJSONArray.getString(it)
                    }

                    jsonObject.getString("token_endpoint").let {
                        editableConfiguration.tokenEndpointURI = it

                        val index = ProfileIndex.ItemTokenEndpointURI.ordinal
                        val oldItem = _list.value!![index] as ProfileItem.Content
                        val newItem = ProfileItem.Content(header = oldItem.header, text = editableConfiguration.tokenEndpointURI)
                        _list.value!![index] = newItem
                    }

                    jsonObject.getString("authorization_endpoint").let {
                        editableConfiguration.authorizationEndpointURI = it

                        val index = ProfileIndex.ItemAuthorizationEndpointURI.ordinal
                        val oldItem = _list.value!![index] as ProfileItem.Content
                        val newItem =
                            ProfileItem.Content(header = oldItem.header, text = editableConfiguration.authorizationEndpointURI)
                        _list.value!![index] = newItem
                    }

                    jsonObject.getString("userinfo_endpoint").let {
                        editableConfiguration.userInfoEndpointURI = it

                        val index = ProfileIndex.ItemUserinfoEndpointURI.ordinal
                        val oldItem = _list.value!![index] as ProfileItem.Content
                        val newItem =
                            ProfileItem.Content(header = oldItem.header, text = editableConfiguration.userInfoEndpointURI)
                        _list.value!![index] = newItem
                    }

                    editableConfiguration.supportedScopes = scopesList.sorted()
                    updateConfiguration()

                    refreshLoadingMetaData()
                    refreshScopes()
                } catch (e: Exception) {
                    refreshLoadingMetaData()
                    throw e // coroutineExceptionHandler will consume it
                }
            }
        } catch (e: IllegalArgumentException) {
            refreshLoadingMetaData()
            coroutineExceptionHandler.handleException(Dispatchers.IO, e)
        }
    }

    fun toggleScope(index: Int) {
        val mutableScopes = editableConfiguration.selectedScopes.toMutableList()
        val scope = editableConfiguration.supportedScopes[index]

        if (mutableScopes.contains(scope)) {
            mutableScopes.remove(scope)
        } else {
            mutableScopes.add(scope)
        }

        editableConfiguration.selectedScopes = mutableScopes
        viewModelScope.launch(Dispatchers.IO) {
            updateConfiguration()
        }
    }

    private fun refreshScopes() {
        val scopes = editableConfiguration.supportedScopes.map {
            ProfileItem.Checkbox(
                text = it,
                isChecked = editableConfiguration.selectedScopes.contains(it)
            )
        }.toMutableList()

        _scopesLiveData.postValue(scopes)
    }

    private fun refreshLoadingMetaData() {
        val index = ProfileIndex.ItemLoadingMetaData.ordinal
        val oldProfileItem = _list.value!![index] as ProfileItem.LoadingAction
        val newItem = ProfileItem.LoadingAction(text = oldProfileItem.text, dateLong = System.currentTimeMillis())
        val newList =_list.value!!
        newList[index] = newItem

        _list.postValue(newList)
    }

    private suspend fun updateConfiguration() {
        if (isActiveConfiguration) {
            repository.updateActiveConfiguration(editableConfiguration)
        } else {
            repository.updateConfiguration(
                newConfig = editableConfiguration,
                oldConfig = configuration
            )
        }

        configuration = editableConfiguration
        editableConfiguration = configuration.copy()
    }
}

class ProfileViewModelFactory(
    private val repository: HaapiFlowConfigurationRepository,
    private val configuration: Configuration,
    private val isActiveConfiguration: Boolean,
    private val scopesAdapter: ScopesAdapter
    ): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, configuration, isActiveConfiguration, scopesAdapter) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class ProfileViewModel")
    }
}