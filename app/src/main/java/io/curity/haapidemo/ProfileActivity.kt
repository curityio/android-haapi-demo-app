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

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.ui.settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var recyclerView: RecyclerView

    private val adapter by lazy { ProfileAdapter(clickHandler = { content, position -> selectItem(content, position) }) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        val configString = intent.getStringExtra(EXTRA_HAAPI_CONFIGURATION)
        val index = intent.getIntExtra(EXTRA_HAAPI_INDEX_CONFIGURATION, -1)
        if (configString == null || index == -1) {
            throw IllegalArgumentException("Missing extra, use ProfileActivity.newIntent()")
        }
        val configuration: HaapiFlowConfiguration = Json.decodeFromString(configString)

        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                HaapiFlowConfigurationRepository(dataStore = configurationDataStore),
                configuration = configuration,
                index = index
            )
        ).get(ProfileViewModel::class.java)

        viewModel.initialize()
        viewModel.listLiveData.observe(this) { list ->
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }
    }

    private fun selectItem(content: ProfileItem.Content, atIndex: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(content.header)

        val inputText = EditText(this)
        inputText.setText(content.text)
        builder.setView(inputText)

        builder.setPositiveButton("Save") { dialog, id ->
            lifecycleScope.launch(Dispatchers.Default) {
                viewModel.update(inputText.text.toString(), atIndex = ProfileIndex.fromInt(atIndex))
            }
        }
        builder.setNegativeButton("Cancel") { _, _ -> }

        builder.show()
    }

    companion object {
        private const val EXTRA_HAAPI_CONFIGURATION = "io.curity.haapidemo.profileActivity.extra.configuration"
        private const val EXTRA_HAAPI_INDEX_CONFIGURATION = "io.curity.haapidemo.profileActivity.extra.index.configuration"

        fun newIntent(context: Context, haapiConfiguration: HaapiFlowConfiguration, index: Int): Intent {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra(EXTRA_HAAPI_CONFIGURATION, Json.encodeToString(haapiConfiguration))
            intent.putExtra(EXTRA_HAAPI_INDEX_CONFIGURATION, index)
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
    SectionEndpoints,
    ItemTokenEndpointURI,
    ItemAuthorizationEndpointURI;

    companion object {
        fun fromInt(value: Int) = ProfileIndex.values().first { it.ordinal == value }
    }
}