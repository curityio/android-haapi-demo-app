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
package io.curity.haapidemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.ui.settings.*
import io.curity.haapidemo.ui.settings.profile.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var recyclerView: RecyclerView

    private val adapter = ProfileAdapter(
        clickHandler = { content, position -> selectItem(content, position) },
        toggleHandler = { position -> toggleItem(position) }
    )

    private val scopesAdapter = ScopesAdapter(checkHandler = { index -> viewModel.toggleScope(index) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        val configString = intent.getStringExtra(EXTRA_HAAPI_CONFIGURATION)
        if (configString == null) {
            throw IllegalArgumentException("Missing extra arguments, use ProfileActivity.newIntent(...)")
        }
        val configuration: HaapiFlowConfiguration = Json.decodeFromString(configString)
        val isActiveConfiguration = intent.getBooleanExtra(EXTRA_HAAPI_IS_ACTIVE_CONFIGURATION, false)

        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                HaapiFlowConfigurationRepository(dataStore = configurationDataStore),
                configuration = configuration,
                isActiveConfiguration = isActiveConfiguration,
                scopesAdapter
            )
        ).get(ProfileViewModel::class.java)

        viewModel.listLiveData.observe(this) { list ->
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }

        viewModel.scopesLiveData.observe(this) { list ->
            lifecycleScope.launch(Dispatchers.Main) {
                scopesAdapter.submitList(list)
            }
        }

        val button: Button = findViewById(R.id.button)

        button.visibility = if (isActiveConfiguration) View.GONE else View.VISIBLE
        button.setOnClickListener {
            makeConfigurationActive()
        }
    }

    @SuppressLint("Assert")
    private fun selectItem(item: ProfileItem, atIndex: Int) {
        when (item) {
            is ProfileItem.Content -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(item.header)

                val inputText = EditText(this)
                inputText.setText(item.text)
                builder.setView(inputText)

                builder.setPositiveButton("Save") { _, _ ->
                    viewModel.update(inputText.text.toString(), atIndex = ProfileIndex.fromInt(atIndex))
                }
                builder.setNegativeButton("Cancel") { _, _ -> }
                builder.show()
            }
            is ProfileItem.LoadingAction -> {
                viewModel.fetchMetaData(CoroutineExceptionHandler({ _, exception ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, exception.toString(), Toast.LENGTH_LONG).show()
                    }
                }))
            }
            else -> { assert(value = false, lazyMessage = { "ProfileItem.Type is not handled" }) }
        }
    }

    private fun makeConfigurationActive() {
        viewModel.makeConfigurationActive()
        finish()
    }

    private fun toggleItem(index: Int) {
        viewModel.updateBoolean(index = ProfileIndex.fromInt(index))
    }

    companion object {
        private const val EXTRA_HAAPI_CONFIGURATION = "io.curity.haapidemo.profileActivity.extra_configuration"
        private const val EXTRA_HAAPI_IS_ACTIVE_CONFIGURATION = "io.curity.haapidemo.profileActivity.extra_is_active_configuration"

        fun newIntent(
            context: Context,
            haapiConfiguration: HaapiFlowConfiguration,
            isActiveConfiguration: Boolean): Intent
        {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra(EXTRA_HAAPI_CONFIGURATION, Json.encodeToString(haapiConfiguration))
            intent.putExtra(EXTRA_HAAPI_IS_ACTIVE_CONFIGURATION, isActiveConfiguration)
            return intent
        }
    }
}

enum class ProfileIndex {
    SectionBaseConfiguration,
    ItemName,
    ItemClientId,
    ItemBaseURL,
    ItemRedirectURI,
    SectionMetaData,
    ItemMetaDataURL,
    ItemLoadingMetaData,
    SectionEndpoints,
    ItemTokenEndpointURI,
    ItemAuthorizationEndpointURI,
    SectionSupportedScopes,
    ItemScopes,
    SectionToggles,
    ItemFollowRedirect,
    ItemAutomaticPolling,
    ItemAutoAuthorizationChallenged,
    ItemSSLTrustVerification;

    companion object {
        fun fromInt(value: Int) = values().first { it.ordinal == value }
    }
}