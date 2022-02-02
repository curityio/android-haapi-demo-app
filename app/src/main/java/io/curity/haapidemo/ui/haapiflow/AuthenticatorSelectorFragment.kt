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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.SelectorViewHolder
import io.curity.haapidemo.uicomponents.ViewStopLoadable
import io.curity.haapidemo.utils.getImageResources
import se.curity.identityserver.haapi.android.sdk.models.AuthenticatorSelectorStep
import se.curity.identityserver.haapi.android.sdk.models.actions.FormActionModel
import java.lang.ref.WeakReference

class AuthenticatorSelectorFragment: Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var weakItem: WeakReference<ViewStopLoadable>? = null

    private val adapter = AuthenticatorSelectorAdapter(clickHandler = { form, weakView -> submit(form, weakView) })
    private lateinit var authenticatorSelectorViewModel: AuthenticatorSelectorViewModel

    private var isInterceptingTouch = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_authenticator_selector, container, false)

        recyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.adapter = adapter

        val step = requireArguments().getParcelable<AuthenticatorSelectorStep>(EXTRA_STEP) ?: throw IllegalStateException("Expecting an AuthenticatorSelector")
        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)

        authenticatorSelectorViewModel = ViewModelProvider(this, AuthenticatorSelectorViewModelFactory(step, haapiFlowViewModel))
            .get(AuthenticatorSelectorViewModel::class.java)
        adapter.submitList(authenticatorSelectorViewModel.authenticatorOptions)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authenticatorSelectorViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            Log.i(Constant.TAG, "isLoading: $isLoading")
            isInterceptingTouch = isLoading

            if (!isLoading) {
                weakItem?.get()?.stopLoading()
                weakItem = null
            }
        })

        recyclerView.addOnItemTouchListener(object: RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return isInterceptingTouch
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // NOP
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // NOP
            }
        })
    }

    private fun submit(actionForm: FormActionModel, weakItem: WeakReference<ViewStopLoadable>) {
        this.weakItem = weakItem
        authenticatorSelectorViewModel.submit(actionForm)
    }

    private class AuthenticatorSelectorViewModel(
        private val authenticatorSelectorStep: AuthenticatorSelectorStep,
        private val haapiFlowViewModel: HaapiFlowViewModel
    ): ViewModel() {

        val isLoading: LiveData<Boolean>
            get() = haapiFlowViewModel.isLoading

        val authenticatorOptions: List<AuthenticatorSelectorStep.AuthenticatorOption>
            get() = authenticatorSelectorStep.authenticators

        fun submit(form: FormActionModel) {
            haapiFlowViewModel.submit(
                form,
                emptyMap()
            )
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(Constant.TAG, "AuthenticatorSelectorViewModel was cleared")
        }
    }

    private class AuthenticatorSelectorViewModelFactory(
        private val step: AuthenticatorSelectorStep,
        private val haapiFlowViewModel: HaapiFlowViewModel
    ): ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthenticatorSelectorViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthenticatorSelectorViewModel(step, haapiFlowViewModel) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class AuthenticatorSelectorViewModel")
        }
    }

    private class AuthenticatorSelectorAdapter(
        private val clickHandler: (FormActionModel, WeakReference<ViewStopLoadable>) -> Unit
    ): ListAdapter<AuthenticatorSelectorStep.AuthenticatorOption, SelectorViewHolder>(CONFIG_COMPARATOR) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectorViewHolder {
            return SelectorViewHolder.from(parent)
        }

        override fun onBindViewHolder(holder: SelectorViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(
                item.title.literal,
                imageResourceId = holder.itemView.getImageResources(item.haapiImageName())
            ) {
                clickHandler(item.action.model, it)
            }
        }

        companion object {
            private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<AuthenticatorSelectorStep.AuthenticatorOption>() {
                override fun areItemsTheSame(
                    oldItem: AuthenticatorSelectorStep.AuthenticatorOption,
                    newItem: AuthenticatorSelectorStep.AuthenticatorOption
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(
                    oldItem: AuthenticatorSelectorStep.AuthenticatorOption,
                    newItem: AuthenticatorSelectorStep.AuthenticatorOption
                ): Boolean {
                    return oldItem.type == newItem.type
                }
            }
        }
    }

    companion object {
        private const val EXTRA_STEP = "io.curity.haapidemo.authenticatorSelectorFragment.extra_step"

        fun newInstance(step: AuthenticatorSelectorStep): AuthenticatorSelectorFragment {
            val fragment = AuthenticatorSelectorFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_STEP, step)
            }
            return fragment
        }
    }
}

private fun AuthenticatorSelectorStep.AuthenticatorOption.haapiImageName(): String {
    return "ic_icon_$type"
}