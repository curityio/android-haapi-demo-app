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
import io.curity.haapidemo.ui.haapiflow.*
import io.curity.haapidemo.uicomponents.HeaderView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.curity.identityserver.haapi.android.sdk.models.*
import se.curity.identityserver.haapi.android.sdk.models.actions.Action
import se.curity.identityserver.haapi.android.sdk.models.actions.ActionKind
import se.curity.identityserver.haapi.android.sdk.models.actions.ClientOperationActionModel
import se.curity.identityserver.haapi.android.sdk.models.actions.FormActionModel
import se.curity.identityserver.haapi.android.sdk.models.oauth.ErrorTokenResponse
import se.curity.identityserver.haapi.android.sdk.models.oauth.SuccessfulTokenResponse
import java.lang.IllegalArgumentException

interface ProblemHandable {
    fun handleProblemRepresentation(problem: ProblemRepresentation)
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
    private var externalBrowserOperationStep: ExternalBrowserClientOperationStep? = null

    // UIs
    private val progressBar: ProgressBar by lazy { findViewById(R.id.loader) }
    private val headerView: HeaderView by lazy { findViewById(R.id.header) }

    private var pendingOperation: Action.ClientOperation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flow)

        val configString = intent.getStringExtra(EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG)
        if (configString == null) {
            if (intent.data != null && externalBrowserOperationStep == null) {
                Log.w(Constant.TAG_HAAPI_OPERATION, "A deep link was triggered but FlowActivity did not exist. This Activity is discarded !")
                finish()
                return
            } else {
                throw IllegalArgumentException("You need to use FlowActivity.newIntent(...)")
            }
        }

        haapiBundleHash = savedInstanceState?.getInt(HAAPI_BUNDLE_HASH) ?: INVALID_HAAPI_BUNDLE_HASH
        title = savedInstanceState?.getString(TITLE)

        val configuration: Configuration = Json.decodeFromString(configString)

        haapiFlowViewModel = ViewModelProvider(this,
            HaapiFlowViewModelFactory(app = application, configuration = configuration))
            .get(HaapiFlowViewModel::class.java)

        haapiFlowViewModel.liveStep.observe(this) { newResult ->
            when (newResult) {
                null -> {
                    haapiFlowViewModel.start()
                }
                else -> {
                    val response = newResult.getOrElse {
                        handle(it)
                        return@observe
                    }
                    when (response) {
                        is ClientOperationStep -> { handle(response) }
                        is HaapiRepresentation -> { handle(response) }
                        is ProblemRepresentation -> { handle(response) }
                    }
                }
            }
        }

        haapiFlowViewModel.liveOAuthResponse.observe(this) { newOAuthResponse ->
            when (newOAuthResponse) {
                null -> { /* NOP */ }
                else -> {
                    val response = newOAuthResponse.getOrElse {
                        handle(it)
                        return@observe
                    }
                    when (response) {
                        is SuccessfulTokenResponse -> {
                            val newIntent = Intent()
                            newIntent.putExtra("TOKEN_RESPONSE", response)
                            setResult(RESULT_OK, newIntent)
                            finish()
                        }
                        is ErrorTokenResponse -> {
                            showAlert(
                                message = response.errorDescription,
                                title = response.error
                            )
                        }
                    }
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
            // BankID -> No action is needed
            if (expectedCallback == OPERATION_BANKID_CALLBACK) {
                // Always return RESULT_CANCELED... when the new activity is opened
                Log.d(Constant.TAG_HAAPI_OPERATION, "Callback received for BANKID: $result")
                expectedCallback = NO_OPERATION_CALLBACK
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(Constant.TAG_HAAPI_OPERATION, "Received an intent for deepLink: $intent")
        val extBrowserStep = externalBrowserOperationStep
        if (intent != null && extBrowserStep != null) {
            try {
                val params = extBrowserStep.buildParameters(intent)
                val casted = params.map { e -> e.key to e.value.toString() }.toMap()
                haapiFlowViewModel.submit(extBrowserStep.continueFormActionModel, casted)
            } catch (exception: Exception) {
                Log.w(Constant.TAG_HAAPI_OPERATION, "Something went wrong when getting the formatted params")
            }
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

    private fun updateTitle(title: String?) {
        this.title = title
        headerView.setText(title ?: "")
    }

    private fun handle(haapiRepresentation: HaapiRepresentation) {
        when (haapiRepresentation) {
            is RedirectionStep -> {
                updateTitle(getString(R.string.redirection))
                commitNewFragment(
                    fragment = RedirectFragment.newInstance(haapiRepresentation),
                    representation = haapiRepresentation
                )
            }
            is AuthenticatorSelectorStep -> {
                updateTitle(haapiRepresentation.title.literal)
                commitNewFragment(
                    fragment = AuthenticatorSelectorFragment.newInstance(haapiRepresentation),
                    representation = haapiRepresentation
                )
            }
            is InteractiveFormStep -> {
                val title = if (haapiRepresentation.actions.size == 1) {
                    haapiRepresentation.actions.first().title?.literal
                        ?: if (haapiRepresentation.type == RepresentationType.REGISTRATION_STEP) {
                            "Registration"
                        } else {
                            "Authentication"
                        }
                } else {
                    if (haapiRepresentation.type == RepresentationType.REGISTRATION_STEP) {
                        "Registration"
                    } else {
                        "Authentication"
                    }
                }
                updateTitle(title)
                commitNewFragment(
                    fragment = InteractiveFormFragment.newInstance(
                        model = InteractiveFormModel(
                            messages = haapiRepresentation.messages,
                            actions = haapiRepresentation.actions,
                            links = haapiRepresentation.links
                        )
                    ),
                    representation = haapiRepresentation
                )
            }
            is PollingStep -> {

                if (haapiRepresentation.properties.status == PollingStatus.DONE || haapiRepresentation.properties.status == PollingStatus.FAILED) {
                    pendingOperation = null
                    if (haapiRepresentation.actions.size == 1 && haapiFlowViewModel.haapiConfiguration.isAutoRedirect) {

                        // Kill the PollingFragment to avoid polling and send the "redirect"
                        updateTitle("")
                        commitNewFragment(
                            fragment = EmptyFragment(),
                            representation = haapiRepresentation
                        )

                        progressBar.visibility = VISIBLE
                        haapiFlowViewModel.submit(haapiRepresentation.mainAction.model, emptyMap())
                    }
                } else {
                    updateTitle(getString(R.string.polling))

                    commitNewFragment(
                        fragment = PollingFragment.newInstance(haapiRepresentation),
                        representation = haapiRepresentation
                    )

                    // Logic to start and end interaction with BankID in version 8.0 or later of the Curity Identity Server
                    if (pendingOperation == null) {
                        val clientOperation = haapiRepresentation.actions.filterIsInstance<Action.ClientOperation>().firstOrNull()
                        val bankIdActionModel = clientOperation?.model as ClientOperationActionModel.BankId?
                        if (bankIdActionModel != null) {

                            pendingOperation = clientOperation
                            handle(
                                haapiRepresentation,
                                bankIdActionModel.href,
                                bankIdActionModel.continueActions
                            )
                        }
                    }
                }
            }
            is OAuthAuthorizationResponseStep -> {
                if (haapiFlowViewModel.liveOAuthResponse.value?.getOrNull() !is SuccessfulTokenResponse) {
                    updateTitle(getString(R.string.oauth_authorization_completed))
                    commitNewFragment(
                        fragment = AuthorizationCompletedFragment.newInstance(haapiRepresentation),
                        representation = haapiRepresentation
                    )
                }
            }
            is ContinueSameStep -> {
                showAlert("Continue same step is not implemented")
            }
            is UserConsentStep -> {
                updateTitle(getString(R.string.user_consent))
                commitNewFragment(
                    fragment = UserConsentFragment.newInstance(haapiRepresentation),
                    representation = haapiRepresentation
                )
            }
            is GenericRepresentationStep -> {
                updateTitle("GENERIC STEP")
                commitNewFragment(
                    fragment = GenericHaapiFragment.newInstance(haapiRepresentation),
                    representation = haapiRepresentation
                )
            }
            is ExternalBrowserClientOperationStep, is EncapClientOperationStep, is BankIdClientOperationStep -> {
                throw IllegalStateException("These steps should be handled by another handler. See below")
            }
        }
    }

    private fun handle(operationStep: ClientOperationStep) {
        when (operationStep) {
            is ExternalBrowserClientOperationStep -> {
                Log.d(Constant.TAG_HAAPI_OPERATION, "ExternalBrowserOP")
                try {
                    val uriToLaunch = operationStep.uriToLaunch(
                        redirectTo = haapiFlowViewModel.haapiConfiguration.appRedirect
                    )

                    val intent = Intent(Intent.ACTION_VIEW, uriToLaunch)
                    startActivity(intent)
                    externalBrowserOperationStep = operationStep
                    // If the browser can be opened then we apply the cancel step to the UI
                    val title = operationStep.actionsToPresent.first().title?.literal ?: "External browser operation"
                    updateTitle(title)
                    commitNewFragment(
                        fragment = InteractiveFormFragment.newInstance(
                            model = InteractiveFormModel(
                                messages = operationStep.messages,
                                actions = operationStep.actionsToPresent.filterIsInstance<Action.Form>(),
                                links = operationStep.links
                            )
                        ),
                        representation = operationStep
                    )
                } catch (exception: ActivityNotFoundException) {
                    Log.d(Constant.TAG_HAAPI_OPERATION, "Could not open activity : $exception")
                    showAlert(
                        message = getString(R.string.cannot_open_browswer),
                        title = getString(R.string.no_action)
                    )
                }
            }
            is BankIdClientOperationStep -> {
                // BankID logic in versions older than 8.0 of the Curity Identity Server
                handle(
                    operationStep,
                    operationStep.actionModel.href,
                    operationStep.continueActions)
            }
            is EncapClientOperationStep -> {
                showAlert("Encap client operation step is not implemented")
            }
        }
    }

    /*
     * Logic to launch bankID and handle the continue actions
     */
    private fun handle(
        haapiRepresentation: HaapiRepresentation,
        href: String,
        continueActions: List<Action>) {

        try {
            Log.d(Constant.TAG_HAAPI_OPERATION, href)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(href))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            resultLauncher.launch(intent)

            // If the BankID can be opened then
            if (continueActions.size == 1 &&
                continueActions.first().kind == ActionKind.Redirect &&
                haapiFlowViewModel.haapiConfiguration.isAutoRedirect
            ) {
                // There is only one action and it is a redirect -> we just submit according to our app config
                val continueAction = continueActions.first()
                haapiFlowViewModel.submit((continueAction as Action.Form).model, emptyMap())
            } else {
                // One or many actions - unfortunately if there is one action and it is a redirect, we cannot submit it
                val title = if (continueActions.size == 1) {
                    continueActions.first().title?.literal ?: "BankID operation"
                } else {
                    "BankID operation"
                }
                updateTitle(title)
                @Suppress("UNCHECKED_CAST")
                commitNewFragment(
                    fragment = InteractiveFormFragment.newInstance(
                        model = InteractiveFormModel(
                            messages = haapiRepresentation.messages,
                            actions = continueActions as List<Action.Form>,
                            links = haapiRepresentation.links
                        )
                    ),
                    representation = haapiRepresentation
                )
            }
        } catch (exception: ActivityNotFoundException) {
            Log.d(Constant.TAG_HAAPI_OPERATION, "Could not open bankid activity : $exception")
            expectedCallback = NO_OPERATION_CALLBACK
            showAlert(
                message = getString(R.string.bank_id_is_not_installed),
                title = getString(R.string.no_action)
            )
        }
    }

    private fun handle(problemRepresentation: ProblemRepresentation) {

        pendingOperation = null
        val currentFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ProblemHandable
        if (currentFragment != null) {
            currentFragment.handleProblemRepresentation(problemRepresentation)
        } else {
            // No fragment as ProblemHandable cannot handle it -> Display the problem as an alert and stop the flow
            val message = if (problemRepresentation is AuthorizationProblem) {
                problemRepresentation.errorDescription ?: problemRepresentation.error
            } else {
                problemRepresentation.messages.joinToString { it.text.literal }
            }

            showAlert(
                message = message,
                title = problemRepresentation.title?.literal
            )
        }
        // Restore the headerView title when there was a rotation
        val currentTitle = title
        if (currentTitle != null) {
            updateTitle(currentTitle)
        }
    }

    private fun handle(throwable: Throwable) {
        showAlert(throwable.message ?: "Something bad happened")
    }

    private fun showAlert(message: String, title: String? = null) {
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle(title ?: "Unexpected error")
            setMessage(message)
        }

        alertDialog.setCancelable(false)
        alertDialog.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
            finish()
        }

        if (!isFinishing){
            alertDialog.show()
        }
    }

    private fun commitNewFragment(fragment: Fragment, representation: Any) {
        if (haapiBundleHash != representation.hashCode()) {
            haapiBundleHash = representation.hashCode()
            supportFragmentManager.commit {
                replace(R.id.fragment_container, fragment, FRAGMENT_TAG)
            }
        } else {
            Log.d(Constant.TAG, "Avoid to commit the same fragment for this representation : $representation")
        }
        progressBar.visibility = GONE
    }

    companion object {
        private const val EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG = "io.curity.haapidemo.flowActivity.extra_config"

        private const val HAAPI_BUNDLE_HASH = "io.curity.haapidemo.flowActivity.haapi_bundle_hash"
        private const val TITLE = "io.curity.haapidemo.flowActivity.title"
        private const val INVALID_HAAPI_BUNDLE_HASH = -1
        private const val FRAGMENT_TAG = "io.curity.haapidemo.flowActivity.current_fragment"

        private const val NO_OPERATION_CALLBACK = 0
        private const val OPERATION_BANKID_CALLBACK = 1
        fun newIntent(context: Context, configuration: Configuration): Intent {
            val intent = Intent(context, FlowActivity::class.java)
            intent.putExtra(EXTRA_FLOW_ACTIVITY_HAAPI_CONFIG, Json.encodeToString(configuration))
            return intent
        }
    }
}