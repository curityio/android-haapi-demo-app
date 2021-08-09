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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.R
import io.curity.haapidemo.models.AuthorizationCompleted
import io.curity.haapidemo.uicomponents.ProgressButton

class AuthorizationCompletedFragment: Fragment(R.layout.fragment_authorization_completed) {

    private lateinit var contentTextView: TextView
    private lateinit var progressButton: ProgressButton

    private lateinit var authorizationCompletedViewModel: AuthorizationCompletedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
        authorizationCompletedViewModel = ViewModelProvider(this, AuthorizationCompletedViewModelFactory(haapiFlowViewModel))
            .get(AuthorizationCompletedViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentTextView = view.findViewById(R.id.content_text)
        progressButton = view.findViewById(R.id.progress_button)

        contentTextView.text = authorizationCompletedViewModel.code

        authorizationCompletedViewModel.isLoading.observe(viewLifecycleOwner, { isLoading ->
            progressButton.setLoading(isLoading)
        })

        progressButton.setOnClickListener {
            authorizationCompletedViewModel.fetchAccessToken()
        }
    }

    class AuthorizationCompletedViewModel(private val haapiFlowViewModel: HaapiFlowViewModel): ViewModel() {

        private val authorizationCompleted: AuthorizationCompleted

        init {
            val step = haapiFlowViewModel.liveStep.value
            if (step is AuthorizationCompleted) {
                authorizationCompleted = step
            } else {
                throw IllegalStateException("The step should be AuthorizationCompleted when using AuthorizationCompletedViewModel")
            }
        }

        val isLoading: LiveData<Boolean>
            get() = haapiFlowViewModel.isLoading

        val code: String?
            get() = authorizationCompleted.responseParameters.code

        fun fetchAccessToken() {
            if (code != null) {
                haapiFlowViewModel.fetchAccessToken(code!!)
            }
        }
    }

    class AuthorizationCompletedViewModelFactory(private val haapiFlowViewModel: HaapiFlowViewModel): ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthorizationCompletedViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthorizationCompletedViewModel(haapiFlowViewModel) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class AuthorizationCompletedViewModel")
        }
    }

    companion object {

        fun newInstance(): AuthorizationCompletedFragment {
            return AuthorizationCompletedFragment()
        }
    }
}