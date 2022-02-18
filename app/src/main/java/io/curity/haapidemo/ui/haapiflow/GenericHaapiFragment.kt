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
import androidx.fragment.app.commit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.R
import se.curity.identityserver.haapi.android.sdk.models.GenericRepresentationStep
import se.curity.identityserver.haapi.android.sdk.models.actions.Action

class GenericHaapiFragment: Fragment(R.layout.fragment_generic_haapi) {

    private lateinit var viewModel: GenericHaapiViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val step = requireArguments().getParcelable<GenericRepresentationStep>(EXTRA_STEP) ?: throw IllegalStateException("Expecting a GenericRepresentationStep")
        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)

        viewModel = ViewModelProvider(this, GenericHaapiViewModelFactory(
            step = step,
            haapiFlowViewModel = haapiFlowViewModel)
        )
            .get(GenericHaapiViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().supportFragmentManager.setFragmentResultListener(REQUEST_KEY_SELECT, viewLifecycleOwner)
        { _, bundle ->
            viewModel.select(bundle.getInt(RESULT_INDEX))
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(REQUEST_KEY_LINK, viewLifecycleOwner)
        { _, bundle ->
            viewModel.followLink(bundle.getString(RESULT_INDEX) ?: throw IllegalStateException("Expecting a value for RESULT_INDEX"))
        }

        viewModel.stateLiveData.observe(viewLifecycleOwner) {
            val fragment = when (it) {
                null -> EmptyFragment()
                else -> {
                    when (it) {
                        is DataState.Selection -> {
                            SelectionFragment.newInstance(
                                selectionFragmentModel = SelectionFragment.SelectionFragmentModel(
                                    titles = it.titles,
                                    links = it.links
                                )
                            )
                        }
                        is DataState.Interactive -> {
                            InteractiveFormFragment.newInstance(
                                model = it.formModel
                            )
                        }
                    }

                }
            }

            requireActivity().supportFragmentManager.commit {
                replace(R.id.fragment_generic_container, fragment)
            }
        }
    }

    companion object {
        private const val EXTRA_STEP = "io.curity.haapidemo.genericHaapiFragment.extra_step"

        const val REQUEST_KEY_SELECT = "io.curity.haapidemo.genericHaapiFragment.select"
        const val REQUEST_KEY_LINK = "io.curity.haapidemo.genericHaapiFragment.link"
        const val RESULT_INDEX = "io.curity.haapidemo.genericHaapiFragment.result_index"

        fun newInstance(step: GenericRepresentationStep): GenericHaapiFragment {
            val fragment = GenericHaapiFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_STEP, step)
            }
            return fragment
        }
    }

    class GenericHaapiViewModel(
        private val step: GenericRepresentationStep,
        private val haapiFlowViewModel: HaapiFlowViewModel
    ): ViewModel() {
        private var _stateLiveData: MutableLiveData<DataState?> = MutableLiveData(null)
        val stateLiveData: LiveData<DataState?> = _stateLiveData

        private var currentActions: List<Action>

        init {
            currentActions = step.actions
            updateState()
        }

        fun select(index: Int) {
            when (val selectedAction = currentActions[index]) {
                is Action.Form -> {
                    val interactive = DataState.Interactive(
                        formModel = InteractiveFormModel(
                            messages = emptyList(),
                            actions = listOf(selectedAction),
                            links = step.links
                        )
                    )
                    _stateLiveData.postValue(interactive)
                }
                is Action.Selector -> {
                    currentActions = selectedAction.model.options
                    updateState()
                }
            }
        }

        fun followLink(title: String) {
            val link = step.links.firstOrNull { it.title?.literal == title }
            if (link != null) {
                haapiFlowViewModel.followLink(link)
            }
        }

        private fun updateState() {
            if (currentActions.size == 1) {
                val anAction = step.actions.first()
                if (anAction is Action.Form) {
                    // InteractiveForm
                    val interactive = DataState.Interactive(
                        formModel = InteractiveFormModel(
                            messages = emptyList(),
                            actions = listOf(anAction),
                            links = step.links
                        )
                    )
                    _stateLiveData.postValue(interactive)
                } else if (anAction is Action.Selector) {
                    // Check the options.
                    val selection = DataState.Selection(
                        titles = anAction.model.options.mapNotNull { it.title?.literal },
                        links = step.links.mapNotNull { it.title?.literal }
                    )
                    currentActions = anAction.model.options
                    _stateLiveData.postValue(selection)
                }
            } else {
                if (currentActions.filterIsInstance<Action.Form>().size == currentActions.size) {
                    // InteractiveForm with sections
                    @Suppress("UNCHECKED_CAST")
                    val interactive = DataState.Interactive(
                        formModel = InteractiveFormModel(
                            messages = step.messages,
                            actions = currentActions as List<Action.Form>,
                            links = step.links
                        )
                    )
                    _stateLiveData.postValue(interactive)
                } else {
                    val selection = DataState.Selection(
                        titles = currentActions.mapNotNull { it.title?.literal },
                        links = step.links.mapNotNull { it.title?.literal }
                    )
                    _stateLiveData.postValue(selection)
                }
            }
        }
    }

    class GenericHaapiViewModelFactory(
        private val step: GenericRepresentationStep,
        private val haapiFlowViewModel: HaapiFlowViewModel
    ): ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GenericHaapiViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GenericHaapiViewModel(step = step, haapiFlowViewModel = haapiFlowViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class GenericHaapiViewModel")
        }
    }

    sealed class DataState {

        data class Selection(val titles: List<String>, val links: List<String>): DataState()
        data class Interactive(val formModel: InteractiveFormModel): DataState()
    }
}