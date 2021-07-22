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
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.flow.disableSslTrustVerification
import io.curity.haapidemo.ui.settings.HaapiFlowConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.Exception
import kotlin.IllegalArgumentException

class ProfileViewModel(
    private val repository: HaapiFlowConfigurationRepository,
    private val configuration: HaapiFlowConfiguration,
    private val indexConfiguration: Int,
    private val scopesAdapter: ScopesAdapter
): ViewModel() {

    private var _list: MutableLiveData<MutableList<ProfileItem>> = MutableLiveData()
    val listLiveData: LiveData<List<ProfileItem>>
        get() = _list.map {
            it.toList()
        }

    private var _scopesLiveData: MutableLiveData<MutableList<ProfileItem.Checkbox>> = MutableLiveData()
    val scopesLiveData: LiveData<List<ProfileItem.Checkbox>>
        get() = _scopesLiveData.map { it.toList() }

    init {
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
    }

    fun updateBoolean(index: ProfileIndex) {
        viewModelScope.launch(Dispatchers.IO) {
            when (index) {
                ProfileIndex.ItemFollowRedirect -> { configuration.followRedirect = !configuration.followRedirect }
                ProfileIndex.ItemAutomaticPolling -> { configuration.isAutoPollingEnabled = !configuration.isAutoPollingEnabled }
                ProfileIndex.ItemAutoAuthorizationChallenged -> { configuration.isAutoAuthorizationChallengedEnabled = !configuration.isAutoAuthorizationChallengedEnabled }
                ProfileIndex.ItemSSLTrustVerification -> { configuration.isSSLTrustVerificationEnabled = !configuration.isSSLTrustVerificationEnabled }

                else -> throw IllegalArgumentException("Invalid index $index for updating a Boolean to configuration")
            }

            repository.updateConfiguration(configuration, indexConfiguration)
        }
    }

    fun makeConfigurationActive() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveConfiguration(configuration)
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().disableSslTrustVerification().build()
    }

    /**
     * Fetches the metaData from [HaapiFlowConfiguration.metaDataBaseURLString]
     *
     * If it succeeds then [listLiveData] and [scopesLiveData] will be triggered.
     * If it fails then an [Exception] will be thrown.
     * @exception Exception An [IllegalArgumentException] or a generic [Exception]
     */
    fun fetchMetaData() {
        val metaDataURLString = configuration.metaDataBaseURLString.plus("/.well-known/openid-configuration")
        val request: Request
        try {
            Request.Builder()
                .url(metaDataURLString)
                .get()
                .build()
        } catch (e: IllegalArgumentException) {
            refreshLoadingMetaData()
            throw e
        }.apply {
            request = this
        }

        viewModelScope.launch(Dispatchers.IO) {
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
                    configuration.tokenEndpointURI = it

                    val index = ProfileIndex.ItemTokenEndpointURI.ordinal
                    val oldItem = _list.value!![index] as ProfileItem.Content
                    val newItem = ProfileItem.Content(header = oldItem.header, text = configuration.tokenEndpointURI)
                    _list.value!![index] = newItem
                }

                jsonObject.getString("authorization_endpoint").let {
                    configuration.authorizationEndpointURI = it

                    val index = ProfileIndex.ItemTokenEndpointURI.ordinal
                    val oldItem = _list.value!![index] as ProfileItem.Content
                    val newItem =
                        ProfileItem.Content(header = oldItem.header, text = configuration.authorizationEndpointURI)
                    _list.value!![index] = newItem
                }

                configuration.supportedScopes = scopesList.sorted()
                repository.updateConfiguration(configuration, indexConfiguration)

                refreshLoadingMetaData()
                refreshScopes()

            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun toggleScope(index: Int) {
        val mutableScopes = configuration.selectedScopes.toMutableList()
        val scope = configuration.supportedScopes[index]

        if (mutableScopes.contains(scope)) {
            mutableScopes.remove(scope)
        } else {
            mutableScopes.add(scope)
        }

        configuration.selectedScopes = mutableScopes
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateConfiguration(configuration,indexConfiguration)
        }
    }

    private fun refreshScopes() {
        val scopes = configuration.supportedScopes.map {
            ProfileItem.Checkbox(
                text = it,
                isChecked = configuration.selectedScopes.contains(it)
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
}

class ProfileViewModelFactory(
    private val repository: HaapiFlowConfigurationRepository,
    private val configuration: HaapiFlowConfiguration,
    private val index: Int,
    private val scopesAdapter: ScopesAdapter
    ): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, configuration, index, scopesAdapter) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class ProfileViewModel")
    }
}