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

package io.curity.haapidemo.ui.haapiflow

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.ProgressButton
import io.curity.haapidemo.uicomponents.ViewStopLoadable
import kotlinx.coroutines.*
import se.curity.haapi.models.android.sdk.models.haapi.PollingStatus
import se.curity.haapi.models.android.sdk.models.haapi.PollingStep

class PollingFragment: Fragment(R.layout.fragment_polling) {

    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var mainButton: ProgressButton
    private lateinit var cancelButton: ProgressButton

    private var selectedButton: ViewStopLoadable? = null

    private lateinit var pollingViewModel: PollingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val step = requireArguments().getParcelable<PollingStep>(EXTRA_STEP) ?: throw IllegalStateException("Expecting a PollingStep")
        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
        pollingViewModel = ViewModelProvider(this, PollingViewModelFactory(step, haapiFlowViewModel))
            .get(PollingViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusTextView = view.findViewById(R.id.status_text_view)
        progressBar = view.findViewById(R.id.progress_bar)
        mainButton = view.findViewById(R.id.main_progress_button)
        cancelButton = view.findViewById(R.id.cancel_progress_button)

        statusTextView.text = pollingViewModel.status
        mainButton.setText(pollingViewModel.mainActionTitle)

        if (pollingViewModel.cancelActionTitle != null) {
            cancelButton.setText(pollingViewModel.cancelActionTitle!!)
            cancelButton.visibility = View.VISIBLE
        } else {
            cancelButton.visibility = View.GONE
        }

        if (pollingViewModel.shouldShowAutoPolling) {
            progressBar.visibility = View.VISIBLE
            mainButton.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            mainButton.visibility = View.VISIBLE
        }

        pollingViewModel.isLoading.observe(viewLifecycleOwner, { isLoading ->

            if (!isLoading) {
                selectedButton?.stopLoading()
                selectedButton = null
            }
        })

        mainButton.setOnClickListener {
            mainButton.setLoading(true)
            selectedButton = mainButton
            pollingViewModel.submit()
        }

        cancelButton.setOnClickListener {
            cancelButton.setLoading(true)
            selectedButton = cancelButton
            pollingViewModel.cancel()
        }
    }

    override fun onPause() {
        super.onPause()
        pollingViewModel.pausePollingIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        pollingViewModel.startPollingIfNeeded()
    }

    companion object {
        private const val EXTRA_STEP = "io.curity.haapidemo.pollingFragment.extra_step"

        fun newInstance(step: PollingStep): PollingFragment {
            val fragment = PollingFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_STEP, step)
            }
            return fragment
        }
    }

    class PollingViewModel(private val pollingStep: PollingStep, private val haapiFlowViewModel: HaapiFlowViewModel, private val timeMillis: Long = 3000): ViewModel() {

        private var shouldPoll = true
        private var pollingJob: Job? = null

        private val isPolling: Boolean
            get() = shouldShowAutoPolling && shouldPoll

        val shouldShowAutoPolling: Boolean
            get() = haapiFlowViewModel.isAutoPolling && pollingStep.properties.status == PollingStatus.Pending

        val isLoading = haapiFlowViewModel.isLoading
        val mainActionTitle: String
            get() = pollingStep.mainAction.model.actionTitle?.value() ?: pollingStep.mainAction.kind.discriminator.uppercase()
        val cancelActionTitle: String?
            get() = pollingStep.cancelAction?.model?.actionTitle?.value()
        val status: String
            get() = pollingStep.properties.status.discriminator

        fun submit() {
            haapiFlowViewModel.submit(pollingStep.mainAction.model, emptyMap())
        }

        fun cancel() {
            pollingJob?.cancel()
            shouldPoll = false
            val cancelActionForm = pollingStep.cancelAction
            if (cancelActionForm != null) {
                haapiFlowViewModel.submit(cancelActionForm.model, emptyMap())
            }
        }

        fun startPollingIfNeeded() {
            shouldPoll = true
            if (pollingJob == null && isPolling) {
                pollingJob = viewModelScope.launch {
                    while (isPolling) {
                        submit()
                        delay(timeMillis)
                    }
                }
            }
        }

        fun pausePollingIfNeeded() {
            shouldPoll = false
            pollingJob?.cancel()
            pollingJob = null
        }

        override fun onCleared() {
            super.onCleared()
            pausePollingIfNeeded()
            Log.d(Constant.TAG_VIEW_MODEL, "PollingViewModel was deallocated")
        }
    }

    class PollingViewModelFactory(private val step: PollingStep, private val haapiFlowViewModel: HaapiFlowViewModel): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PollingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PollingViewModel(step, haapiFlowViewModel) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class PollingViewModel")
        }
    }
}