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
import io.curity.haapidemo.models.AuthenticatorOption
import io.curity.haapidemo.models.AuthenticatorSelector
import io.curity.haapidemo.models.haapi.actions.ActionModel
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

        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)

        authenticatorSelectorViewModel = ViewModelProvider(this, AuthenticatorSelectorViewModelFactory(haapiFlowViewModel))
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

    private fun submit(actionForm: ActionModel.Form, weakItem: WeakReference<ViewStopLoadable>) {
        this.weakItem = weakItem
        authenticatorSelectorViewModel.submit(actionForm)
    }

    companion object {

        fun newInstance(): AuthenticatorSelectorFragment {
            return AuthenticatorSelectorFragment()
        }
    }
}

class AuthenticatorSelectorViewModel(private val haapiFlowViewModel: HaapiFlowViewModel): ViewModel() {

    val isLoading: LiveData<Boolean>
        get() = haapiFlowViewModel.isLoading

    val authenticatorOptions: List<AuthenticatorOption>
        get() {
            val step = haapiFlowViewModel.liveStep.value
            if (step is AuthenticatorSelector) {
                return step.authenticators
            } else {
                throw IllegalArgumentException("")
            }
        }

    fun submit(form: ActionModel.Form) {
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

class AuthenticatorSelectorViewModelFactory(private val haapiFlowViewModel: HaapiFlowViewModel): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthenticatorSelectorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthenticatorSelectorViewModel(haapiFlowViewModel) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class AuthenticatorSelectorViewModel")
    }
}

private class AuthenticatorSelectorAdapter(private val clickHandler: (ActionModel.Form, WeakReference<ViewStopLoadable>) -> Unit): ListAdapter<AuthenticatorOption, SelectorViewHolder>(
    CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectorViewHolder {
        return SelectorViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: SelectorViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item.label.message ?: "") {
            clickHandler(item.action.model, it)
        }
    }

    companion object {
        private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<AuthenticatorOption>() {
            override fun areItemsTheSame(oldItem: AuthenticatorOption, newItem: AuthenticatorOption): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: AuthenticatorOption, newItem: AuthenticatorOption): Boolean {
                return oldItem.type == newItem.type
            }
        }
    }
}