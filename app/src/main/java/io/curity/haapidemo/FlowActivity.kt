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

package io.curity.haapidemo

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.models.SystemErrorStep
import io.curity.haapidemo.ui.haapiflow.HaapiFlowViewModel
import io.curity.haapidemo.ui.haapiflow.HaapiFlowViewModelFactory
import io.curity.haapidemo.uicomponents.HeaderView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.IllegalArgumentException

class FlowActivity : AppCompatActivity() {
    private lateinit var haapiFlowViewModel: HaapiFlowViewModel

    // UIs
    private val progressBar: ProgressBar by lazy { findViewById(R.id.loader) }
    private val headerView: HeaderView by lazy { findViewById(R.id.header) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flow)

        val configString = intent.getStringExtra(EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG)
        if (configString == null) {
            throw IllegalArgumentException("You need to use FlowActivity.newIntent(...)")
        }

        val configuration: HaapiFlowConfiguration = Json.decodeFromString(configString)

        haapiFlowViewModel = ViewModelProvider(this,
            HaapiFlowViewModelFactory(haapiFlowConfiguration = configuration))
            .get(HaapiFlowViewModel::class.java)


        haapiFlowViewModel.liveStep.observe(this) { step ->
            Log.i(Constant.TAG, step.toString())
            when (step) {
                null -> { haapiFlowViewModel.start() }
                is SystemErrorStep -> {
                    showAlert(step)
                    progressBar.visibility = GONE
                }
                else -> { progressBar.visibility = GONE }
            }
        }

        haapiFlowViewModel.haapiUIBundleLiveData.observe(this) { haapiBundle ->
            headerView.setText(haapiBundle.title ?: "")
            supportFragmentManager.commit {
                replace(R.id.fragment_container, haapiBundle.fragment)
            }
        }

        val closeButton: ImageButton = findViewById(R.id.close_button)
        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun showAlert(systemErrorStep: SystemErrorStep) {
        val alertDialog = AlertDialog.Builder(this).apply {
            title = systemErrorStep.title
            setMessage(systemErrorStep.description)
        }

        alertDialog.setCancelable(false)
        alertDialog.setPositiveButton(R.string.ok) { dialog, which ->
            dialog.dismiss()
            finish()
        }

        alertDialog.show()
    }

    companion object {
        private const val EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG = "io.curity.haapidemo.flowActivity.extra_config"

        fun newIntent(context: Context, haapiFlowConfiguration: HaapiFlowConfiguration): Intent {
            val intent = Intent(context, FlowActivity::class.java)
            intent.putExtra(EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG, Json.encodeToString(haapiFlowConfiguration))
            return intent
        }
    }
}
