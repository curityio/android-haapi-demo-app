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
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.MessageView
import io.curity.haapidemo.uicomponents.ProgressButton
import se.curity.haapi.models.android.sdk.models.haapi.RedirectionStep
import java.lang.IllegalArgumentException

class RedirectFragment: Fragment(R.layout.fragment_redirect) {

    private var message: String? = null
    private lateinit var redirectionStep: RedirectionStep
    private lateinit var haapiFlowViewModel: HaapiFlowViewModel

    private lateinit var button: ProgressButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            message = requireArguments().getString(EXTRA_MESSAGE)
            redirectionStep = requireArguments().getParcelable(EXTRA_STEP) ?: throw IllegalStateException("Expecting an AuthenticatorSelector")
        } else {
            throw IllegalArgumentException("RedirectFragment.newInstance() was not used")
        }
        haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = message
        if (title != null) {
            val messageView: MessageView = view.findViewById(R.id.message_view)
            messageView.setText(title)
        }

        button = view.findViewById(R.id.button)
        button.setOnClickListener {
            haapiFlowViewModel.submit(redirectionStep.actionForm.model, emptyMap())
        }

        haapiFlowViewModel.isLoading.observe(viewLifecycleOwner) {
            button.setLoading(it)
        }
    }

    companion object {
        private const val EXTRA_MESSAGE = "io.curity.redirectFragment.extra_message"
        private const val EXTRA_STEP = "io.curity.redirectFragment.extra_step"

        fun newInstance(
            redirectionStep: RedirectionStep,
            message: String? = null
        ): RedirectFragment {
            return RedirectFragment().apply {
                val bundle = Bundle()
                bundle.putString(EXTRA_MESSAGE, message)
                bundle.putParcelable(EXTRA_STEP, redirectionStep)

                arguments = bundle
            }
        }
    }
}