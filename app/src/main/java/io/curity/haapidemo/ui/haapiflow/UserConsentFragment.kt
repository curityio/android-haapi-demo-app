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
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.ProgressButton
import io.curity.haapidemo.uicomponents.ViewStopLoadable
import io.curity.haapidemo.utils.dismissKeyboard
import io.curity.haapidemo.utils.toInteractiveFormItemCheckbox
import io.curity.haapidemo.utils.toMessageViews
import se.curity.haapi.models.android.sdk.models.UserConsentStep
import se.curity.haapi.models.android.sdk.models.UserMessage
import se.curity.haapi.models.android.sdk.models.actions.Action
import se.curity.haapi.models.android.sdk.models.actions.FormActionModel
import se.curity.haapi.models.android.sdk.models.actions.FormField
import java.lang.ref.WeakReference

class UserConsentFragment: Fragment(R.layout.fragment_user_consent) {

    private lateinit var messagesLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView

    private var weakButton: WeakReference<ViewStopLoadable>? = null
    private var buttonList: MutableList<ProgressButton> = mutableListOf()

    private var isInterceptingTouch = false

    private lateinit var userConsentViewModel: UserConsentViewModel

    private val adapter = InteractiveFormAdapter(
        itemChangeHandler = { position, item  ->
            userConsentViewModel.updateItem(item, position)
        },
        selectButtonHandler = { actionmModelForm, progressButtonViewHolder ->
            dismissKeyboard()
            weakButton = WeakReference(progressButtonViewHolder)
            userConsentViewModel.submitActionModelForm(actionmModelForm)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val step = requireArguments().getParcelable<UserConsentStep>(EXTRA_STEP) ?: throw IllegalStateException("Expecting an UserConsentStep")
        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
        userConsentViewModel = ViewModelProvider(this, UserConsentViewModelFactory(step, haapiFlowViewModel))
            .get(UserConsentViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messagesLayout = view.findViewById(R.id.linear_layout_messages)
        recyclerView = view.findViewById(R.id.recycler_view)

        recyclerView.adapter = adapter
        adapter.submitList(userConsentViewModel.interactiveFormItems)
        recyclerView.addOnItemTouchListener(object: RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return isInterceptingTouch
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) { /* NOP */ }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) { /* NOP */ }
        })

        messagesLayout.visibility = if (userConsentViewModel.userMessages.isEmpty()) View.GONE else View.VISIBLE
        userConsentViewModel.userMessages.toMessageViews(requireContext()).forEach {
            messagesLayout.addView(it)
        }

        userConsentViewModel.isLoading.observe(viewLifecycleOwner, { isLoading ->
            isInterceptingTouch = isLoading
            buttonList.forEach { it.isClickable = !isLoading }

            if (!isLoading) {
                weakButton?.get()?.stopLoading()
                weakButton = null
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        Log.d(Constant.TAG, "Saving edit data from userConsentViewModel")
        outState.putSerializable(DATA_COPY, userConsentViewModel.mapDataCopy)
    }

    @Suppress("Unchecked_cast")
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        val dataSaved = savedInstanceState?.getSerializable(DATA_COPY) as? HashMap<Long, Boolean>
        if (dataSaved != null) {
            Log.d(Constant.TAG, "Restoring edit data for userConsentViewModel")
            userConsentViewModel.restore(dataSaved)
        }
    }

    class UserConsentViewModel(private val userConstentStep: UserConsentStep, private val haapiFlowViewModel: HaapiFlowViewModel): ViewModel() {

        private var _interactiveFormItems: MutableList<InteractiveFormItem> = mutableListOf()
        val interactiveFormItems: List<InteractiveFormItem>
            get() = _interactiveFormItems

        val userMessages: List<UserMessage>
            get() = userConstentStep.messages

        val isLoading: LiveData<Boolean>
            get() = haapiFlowViewModel.isLoading

        init {
            setupInteractiveFormItems()
        }
        
        private fun setupInteractiveFormItems() {
            _interactiveFormItems.clear()
            for (action in userConstentStep.actions) {
                if (action is Action.Form) {
                    for (field in action.model.fields) {
                        if (field is FormField.Checkbox) {
                            _interactiveFormItems.add(
                                field.toInteractiveFormItemCheckbox()
                            )
                        }
                    }

                    _interactiveFormItems.add(
                        InteractiveFormItem.Button(
                            key = action.kind.discriminator,
                            label = action.model.actionTitle?.value() ?: "",
                            actionModelForm = action.model
                        )
                    )
                } else {
                    Log.d(Constant.TAG, "Actions that are not Action.Form are not handled")
                }
            }
        }

        val mapDataCopy: HashMap<Long, Boolean>
            get() {
                val result = HashMap<Long, Boolean>()
                _interactiveFormItems.forEach {
                    if (it is InteractiveFormItem.Checkbox) {
                        result[it.id] = it.checked
                    }
                }
                return result
            }
        
        fun restore(mapDataCopy: HashMap<Long, Boolean>) {
            mapDataCopy.forEach { (key, value) ->
                val item = _interactiveFormItems.first{ it.id == key }
                if (item is InteractiveFormItem.Checkbox) {
                    item.checked = value
                } else {
                    Log.w(Constant.TAG, "Something is wrong - expecting only InteractiveFormItem.Checkbox")
                }
            }
        }

        fun updateItem(item: InteractiveFormItem, position: Int) {
            _interactiveFormItems[position] = item
        }

        fun submitActionModelForm(actionModelForm: FormActionModel) {
            val indexAction = interactiveFormItems.indexOfFirst { it.id == actionModelForm.hashCode().toLong() }
            if (indexAction != -1) {
                val parameters: MutableMap<String, String> = mutableMapOf()
                var index = indexAction - 1
                while (index >= 0) {
                    when (val item = interactiveFormItems[index]) {
                        is InteractiveFormItem.Checkbox -> {
                            if (item.checked) {
                                parameters[item.key] = item.value
                            } else {
                                parameters[item.key] = ""
                            }
                        }
                        is InteractiveFormItem.Button -> {
                            index = -1
                        }
                    }
                    index -= 1
                }

                haapiFlowViewModel.submit(actionModelForm, parameters)
            } else {
                throw IllegalArgumentException("$actionModelForm is incorrect")
            }
        }
    }

    class UserConsentViewModelFactory(private val step: UserConsentStep, private val haapiFlowViewModel: HaapiFlowViewModel): ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserConsentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return UserConsentViewModel(step, haapiFlowViewModel) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class UserConsentViewModel")
        }
    }

    companion object {
        private const val DATA_COPY = "io.curity.haapidemo.userConsentFragment.data_copy"
        private const val EXTRA_STEP = "io.curity.haapidemo.userConsentFragment.extra_step"

        fun newInstance(step: UserConsentStep): UserConsentFragment {
            val fragment = UserConsentFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_STEP, step)
            }
            return fragment
        }
    }
}