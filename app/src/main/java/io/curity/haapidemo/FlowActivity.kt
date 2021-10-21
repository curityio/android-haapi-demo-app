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
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.models.*
import io.curity.haapidemo.models.haapi.actions.Action
import io.curity.haapidemo.models.haapi.actions.ActionModel
import io.curity.haapidemo.models.haapi.problems.AuthorizationProblem
import io.curity.haapidemo.models.haapi.problems.HaapiProblem
import io.curity.haapidemo.ui.haapiflow.*
import io.curity.haapidemo.uicomponents.HeaderView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.IllegalArgumentException

interface ProblemHandable {
    fun handleProblem(problem: HaapiProblem)
}

class FlowActivity : AppCompatActivity() {
    private lateinit var haapiFlowViewModel: HaapiFlowViewModel

    /**
     * Reference value to know if the device rotated. Plus, it avoids to override the current fragment (InteractiveFormFragment).
     * [haapiBundleHash] will contain the hash of a HaapiBundle.
     */
    private var haapiBundleHash: Int = INVALID_HAAPI_BUNDLE_HASH
    private var title: String? = null

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
            if (intent.data != null && pendingContinueAction == null) {
                Log.w(Constant.TAG_HAAPI_OPERATION, "A deep link was triggered but FlowActivity did not exist. This Activity is discarded !")
                finish()
                return
            } else {
                throw IllegalArgumentException("You need to use FlowActivity.newIntent(...)")
            }
        }

        haapiBundleHash = savedInstanceState?.getInt(HAAPI_BUNDLE_HASH) ?: INVALID_HAAPI_BUNDLE_HASH
        title = savedInstanceState?.getString(TITLE)

        val configuration: HaapiFlowConfiguration = Json.decodeFromString(configString)

        haapiFlowViewModel = ViewModelProvider(this,
            HaapiFlowViewModelFactory(haapiFlowConfiguration = configuration))
            .get(HaapiFlowViewModel::class.java)


        haapiFlowViewModel.liveStep.observe(this) { step ->
            Log.i(Constant.TAG, "Observed triggered in FlowActivity: ${step.toString()}")
            progressBar.visibility = GONE
            when (step) {
                null -> { haapiFlowViewModel.start() }
                is Redirect -> {
                    title = step.action.kind
                    headerView.setText(step.action.kind)
                    commitNewFragment(RedirectFragment.newInstance(), step)
                }
                is AuthenticatorSelector -> {
                    val headerTitle = step.title.message ?: ""
                    title = headerTitle
                    headerView.setText(headerTitle)
                    commitNewFragment(AuthenticatorSelectorFragment.newInstance(), step)
                }
                is InteractiveForm -> {
                    // If we have only one action then we use it for the title otherwise, we use the `type`
                    val headerTitle = if (step.actions.size == 1) {
                        step.actions.first().title?.value() ?: step.type.discriminator
                    } else {
                        step.type.discriminator
                    }
                    title = headerTitle
                    headerView.setText(headerTitle)
                    commitNewFragment(InteractiveFormFragment.newInstance(), step)
                }
                is TokensStep -> {
                    val headerTitle = "Success"
                    title = headerTitle
                    headerView.setText(headerTitle)
                    commitNewFragment(TokensFragment.newInstance(step.oAuthTokenResponse), step)
                }
                is AuthorizationCompleted -> {
                    val headerTitle = step.type.discriminator.uppercase()
                    title = headerTitle
                    headerView.setText(headerTitle)
                    commitNewFragment(AuthorizationCompletedFragment.newInstance(), step)
                }
                is PollingStep -> {
                    val headerTitle = step.type.discriminator.uppercase()
                    title = headerTitle
                    headerView.setText(headerTitle)
                    commitNewFragment(PollingFragment.newInstance(), step)
                }
                is UserConsentStep -> {
                    val headerTitle = step.type.discriminator.uppercase()
                    title = headerTitle
                    headerView.setText(headerTitle)
                    commitNewFragment(UserConsentFragment.newInstance(), step)
                }
                is SystemErrorStep -> {
                    showAlert(step)
                }
                is ExternalBrowserClientOperation -> {
                    val uri = step.completeUri(haapiFlowViewModel.redirectURI)
                    Log.d(Constant.TAG_HAAPI_OPERATION, uri.toString())
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                        pendingContinueAction = step.actionModel.continueActions.filterIsInstance<Action.Form>().first().model
                        // If the browser can be opend then we apply the cancel step to the UI
                        haapiFlowViewModel.applyActionForm(step.cancel!!)
                    } catch (exception: ActivityNotFoundException) {
                        haapiFlowViewModel.interrupt(
                            title = resources.getString(R.string.no_action),
                            description = resources.getString(R.string.cannot_open_browswer))
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
                        // If the BankID can be opened then we apply the first action to the UI
                        haapiFlowViewModel.applyActionForm(step.actionModel.continueActions.filterIsInstance<Action.Form>().first())
                    } catch (exception: ActivityNotFoundException) {
                        expectedCallback = NO_OPERATION_CALLBACK
                        haapiFlowViewModel.interrupt(
                            title = resources.getString(R.string.no_action),
                            description = resources.getString(R.string.bank_id_is_not_installed)
                        )
                        Log.d(Constant.TAG_HAAPI_OPERATION, "Could not open activity : $exception")
                    }
                }
                is ProblemStep -> {
                    val currentFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ProblemHandable
                    if (currentFragment != null) {
                        currentFragment.handleProblem(step.problem)
                    } else {
                        // No fragment as ProblemHandable cannot handle it -> Display the problem as an alert and stop the flow
                        showAlert(step.problem.toSystemError())
                    }
                    // Restore the headerView title when there was a rotation
                    val headerTitle = title
                    if (headerTitle != null) {
                        headerView.setText(headerTitle)
                    }
                }
                else -> {
                    Log.d(Constant.TAG_HAAPI_OPERATION, "This step was not handled: $step")
                }
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
        outState.putString(TITLE, title)
    }

    private fun commitNewFragment(fragment: Fragment, haapiStep: HaapiStep) {
        if (haapiBundleHash != haapiStep.hashCode()) {
            haapiBundleHash = haapiStep.hashCode()
            supportFragmentManager.commit {
                replace(R.id.fragment_container, fragment, FRAGMENT_TAG)
            }
        } else {
            Log.d(Constant.TAG, "Avoid to commit the same fragment for this step : $haapiStep")
        }
        progressBar.visibility = GONE
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
        private const val TITLE = "io.curity.haapidemo.flowActivity.title"
        private const val INVALID_HAAPI_BUNDLE_HASH = -1
        private const val FRAGMENT_TAG = "io.curity.haapidemo.flowActivity.current_fragment"

        private const val NO_OPERATION_CALLBACK = 0
        private const val OPERATION_BANKID_CALLBACK = 1
        fun newIntent(context: Context, haapiFlowConfiguration: HaapiFlowConfiguration): Intent {
            val intent = Intent(context, FlowActivity::class.java)
            intent.putExtra(EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG, Json.encodeToString(haapiFlowConfiguration))
            return intent
        }
    }
}

private fun HaapiProblem.toSystemError() : SystemErrorStep {
    val description = if (this is AuthorizationProblem) {
        errorDescription
    } else {
        messages?.joinToString { it.text.message ?: it.text.key ?: "" } ?: ""
    }
    return SystemErrorStep(
        title = title,
        description = description
    )
}

private fun ExternalBrowserClientOperation.completeUri(redirectUri: String): Uri {
    return Uri.parse(actionModel.arguments.href)
        .buildUpon()
        .appendQueryParameter("redirect_uri", redirectUri)
        .build()
}