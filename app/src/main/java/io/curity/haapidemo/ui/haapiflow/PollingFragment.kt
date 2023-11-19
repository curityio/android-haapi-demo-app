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

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.HeaderView
import io.curity.haapidemo.uicomponents.ProgressButton
import io.curity.haapidemo.uicomponents.ViewStopLoadable
import kotlinx.coroutines.*
import se.curity.identityserver.haapi.android.sdk.models.Link
import se.curity.identityserver.haapi.android.sdk.models.PollingStatus
import se.curity.identityserver.haapi.android.sdk.models.PollingStep
import java.util.*

class PollingFragment: Fragment(R.layout.fragment_polling) {

    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var mainButton: ProgressButton
    private lateinit var cancelButton: ProgressButton
    private lateinit var linksLayout: LinearLayout

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
        linksLayout = view.findViewById(R.id.linear_layout_links)

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

        pollingViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->

            if (!isLoading) {
                selectedButton?.stopLoading()
                selectedButton = null
            }
        }

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

        val links = pollingViewModel.links
        linksLayout.removeAllViews()
        links.forEach { link ->
            if (link.type == "image/png") {
                if (URLUtil.isValidUrl(link.href)) {
                    Log.d(Constant.TAG, "image/png was not handled - you need to load the image")
                } else {
                    val pureBase64Encoded = link.href.substring(link.href.indexOf(",") + 1)
                    val decodedString = Base64.decode(pureBase64Encoded, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    val headerViewLink = HeaderView(requireContext(), null, R.style.HeaderView_Link).apply {
                        this.setImageBitmap(bitmap)
                        this.setText(link.title?.literal ?: "")
                    }
                    linksLayout.addView(headerViewLink)
                }
            }
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
            get() = haapiFlowViewModel.isAutoPolling && pollingStep.properties.status == PollingStatus.PENDING

        val isLoading = haapiFlowViewModel.isLoading
        val mainActionTitle: String
            get() = pollingStep.mainAction.model.actionTitle?.literal ?: pollingStep.mainAction.kind.discriminator.uppercase()
        val cancelActionTitle: String?
            get() = pollingStep.cancelAction?.model?.actionTitle?.literal
        val status: String
            get() = pollingStep.properties.status.value

        val links: List<Link>
            get() {
                return pollingStep.links
            }

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
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PollingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PollingViewModel(step, haapiFlowViewModel) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class PollingViewModel")
        }
    }
}