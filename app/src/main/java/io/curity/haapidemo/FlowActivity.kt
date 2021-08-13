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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.models.BankIdClientOperation
import io.curity.haapidemo.models.ExternalBrowserClientOperation
import io.curity.haapidemo.models.SystemErrorStep
import io.curity.haapidemo.models.haapi.actions.Action
import io.curity.haapidemo.models.haapi.actions.ActionModel
import io.curity.haapidemo.ui.haapiflow.HaapiFlowViewModel
import io.curity.haapidemo.ui.haapiflow.HaapiFlowViewModelFactory
import io.curity.haapidemo.uicomponents.HeaderView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.IllegalArgumentException

class FlowActivity : AppCompatActivity() {
    private lateinit var haapiFlowViewModel: HaapiFlowViewModel

    /**
     * Reference value to know if the device rotated. Plus, it avoids to override the current fragment (InteractiveFormFragment).
     * [haapiBundleHash] will contain the hash of a HaapiBundle.
     */
    private var haapiBundleHash: Int = INVALID_HAAPI_BUNDLE_HASH

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private var expectedCallback: Int = NO_OPERATION_CALLBACK
    private var pendingContinueAction: ActionModel.Form? = null

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

        haapiBundleHash = savedInstanceState?.getInt(HAAPI_BUNDLE_HASH) ?: INVALID_HAAPI_BUNDLE_HASH

        val configuration: HaapiFlowConfiguration = Json.decodeFromString(configString)

        haapiFlowViewModel = ViewModelProvider(this,
            HaapiFlowViewModelFactory(haapiFlowConfiguration = configuration))
            .get(HaapiFlowViewModel::class.java)


        haapiFlowViewModel.liveStep.observe(this) { step ->
            Log.i(Constant.TAG, "Observed triggered in FlowActivity: ${step.toString()}")
            when (step) {
                null -> { haapiFlowViewModel.start() }
                is SystemErrorStep -> {
                    showAlert(step)
                    progressBar.visibility = GONE
                }
                is ExternalBrowserClientOperation -> {
                    val uri = step.completeUri(haapiFlowViewModel.redirectURI)
                    Log.d(Constant.TAG_HAAPI_OPERATION, uri.toString())
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                        pendingContinueAction = step.actionModel.continueActions.filterIsInstance<Action.Form>().find { it.kind == "continue" }?.model
                        haapiFlowViewModel.applyActions(listOf(step.cancel!!))
                    } catch (exception: ActivityNotFoundException) {
                        if (step.cancel != null) {
                            haapiFlowViewModel.submit(step.cancel.model)
                        } else {
                            haapiFlowViewModel.interrupt(
                                title = resources.getString(R.string.no_action),
                                description = resources.getString(R.string.cannot_open_browswer))
                        }
                        Log.d(Constant.TAG_HAAPI_OPERATION, "Could not open activity : $exception")
                    }
                }
                is BankIdClientOperation -> {
                    Log.d(Constant.TAG_HAAPI_OPERATION, step.actionModel.arguments.href)
                    val intent = Intent()
                        .setAction(Intent.ACTION_VIEW)
                        .setData(Uri.parse(step.actionModel.arguments.href))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        expectedCallback = OPERATION_BANKID_CALLBACK
                        resultLauncher.launch(intent)
                        haapiFlowViewModel.applyActions(step.actionModel.continueActions.filterIsInstance<Action.Form>())
                    } catch (exception: ActivityNotFoundException) {
                        expectedCallback = NO_OPERATION_CALLBACK
                        if (step.cancel != null) {
                            haapiFlowViewModel.submit(step.cancel.model)
                        } else {
                            haapiFlowViewModel.interrupt(
                                title = resources.getString(R.string.no_action),
                                description = resources.getString(R.string.bank_id_is_not_installed)
                            )
                        }
                        Log.d(Constant.TAG_HAAPI_OPERATION, "Could not open activity : $exception")
                    }
                }
                else -> { progressBar.visibility = GONE }
            }
        }

        haapiFlowViewModel.haapiUIBundleLiveData.observe(this) { haapiBundle ->
            headerView.setText(haapiBundle.title ?: "")
            if (haapiBundleHash != haapiBundle.hashCode()) {
                haapiBundleHash = haapiBundle.hashCode()
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, haapiBundle.fragment)
                }
            } else {
                Log.d(Constant.TAG, "Avoid to commit the same fragment : ${haapiBundle.fragment}")
            }
        }

        val closeButton: ImageButton = findViewById(R.id.close_button)
        closeButton.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (expectedCallback == OPERATION_BANKID_CALLBACK) {
                // Always return RESULT_CANCELED... when the new activity is opened
                Log.d(Constant.TAG_HAAPI_OPERATION, "Callback received for BANKID: $result")
                expectedCallback = NO_OPERATION_CALLBACK
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val continueAction = pendingContinueAction
        if (continueAction != null) {
            pendingContinueAction = null
            Log.d(Constant.TAG_HAAPI_OPERATION, "Received an intent for deepLink: $intent")
            val parameters: MutableMap<String, String> = mutableMapOf()
            continueAction.fields.forEach { field ->
                val value = intent?.data?.getQueryParameter(field.name)
                if (value != null) {
                    parameters[field.name] = value
                } else {
                    Log.w(Constant.TAG_HAAPI_OPERATION, "Cannot find field with name: ${field.name}")
                }
            }
            haapiFlowViewModel.submit(continueAction, parameters)
        } else {
            Log.w(Constant.TAG_HAAPI_OPERATION, "Received an intent for deepLink but it is IGNORED: $intent")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Constant.TAG_HAAPI_OPERATION, "FlowActivity was destroyed...")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // On rotation
        outState.putInt(HAAPI_BUNDLE_HASH, haapiBundleHash)
    }

    private fun showAlert(systemErrorStep: SystemErrorStep) {
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle(systemErrorStep.title)
            setMessage(systemErrorStep.description)
        }

        alertDialog.setCancelable(false)
        alertDialog.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
            finish()
        }

        alertDialog.show()
    }

    companion object {
        private const val EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG = "io.curity.haapidemo.flowActivity.extra_config"

        private const val HAAPI_BUNDLE_HASH = "io.curity.haapidemo.flowActivity.haapi_bundle_hash"
        private const val INVALID_HAAPI_BUNDLE_HASH = -1

        private const val NO_OPERATION_CALLBACK = 0
        private const val OPERATION_BANKID_CALLBACK = 1
        fun newIntent(context: Context, haapiFlowConfiguration: HaapiFlowConfiguration): Intent {
            val intent = Intent(context, FlowActivity::class.java)
            intent.putExtra(EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG, Json.encodeToString(haapiFlowConfiguration))
            return intent
        }
    }
}

private fun ExternalBrowserClientOperation.completeUri(redirectUri: String): Uri {
    return Uri.parse(actionModel.arguments.href)
        .buildUpon()
        .appendQueryParameter("redirect_uri", redirectUri)
        .build()
}