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
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import io.curity.haapidemo.models.InteractiveForm
import io.curity.haapidemo.models.ProblemStep
import io.curity.haapidemo.models.haapi.Field
import io.curity.haapidemo.models.haapi.Link
import io.curity.haapidemo.uicomponents.*
import io.curity.haapidemo.utils.dismissKeyboard

class InteractiveFormFragment private constructor(): Fragment() {

    private lateinit var spacerProblem: Space
    private lateinit var problemView: ProblemView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressButton: ProgressButton
    private lateinit var linksLayout: LinearLayout

    private lateinit var interactiveFormViewModel: InteractiveFormViewModel

    private val adapter = InteractiveFormAdapter(itemChangeHandler = { position, item -> interactiveFormViewModel.updateItem(item, position) })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_interactive_form, container, false)

        spacerProblem = root.findViewById(R.id.spacer_problem)
        problemView = root.findViewById(R.id.problem_view)
        recyclerView = root.findViewById(R.id.recycler_view)
        progressButton = root.findViewById(R.id.progress_button)
        linksLayout = root.findViewById(R.id.linear_layout_links)

        recyclerView.adapter = adapter

        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
        interactiveFormViewModel = ViewModelProvider(this, InteractiveFormViewModelFactory(haapiFlowViewModel))
            .get(InteractiveFormViewModel::class.java)

        adapter.submitList(interactiveFormViewModel.interactiveFormItems)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressButton.setText(interactiveFormViewModel.buttonTitle)
        progressButton.setOnClickListener {
            dismissKeyboard()
            interactiveFormViewModel.submit()
        }

        interactiveFormViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            progressButton.setLoading(isLoading)
        })

        interactiveFormViewModel.problemsLiveData.observe(viewLifecycleOwner, Observer {  problemContent ->
            val visibility = if (problemContent == null) View.GONE else View.VISIBLE
            problemView.visibility = visibility
            spacerProblem.visibility = visibility

            if (problemContent != null) {
                problemView.setTitle(problemContent.title)
                problemView.setProblemBundles(problemContent.problemBundles)
            }
        })
    }

    companion object {
        fun newInstance(): InteractiveFormFragment {
            return InteractiveFormFragment()
        }
    }
}

class InteractiveFormViewModel(private val haapiFlowViewModel: HaapiFlowViewModel): ViewModel() {

    private var _interactiveFormItems: MutableList<InteractiveFormItem> = mutableListOf()
    val interactiveFormItems: List<InteractiveFormItem>
        get() = _interactiveFormItems

    private val interactiveFormStep: InteractiveForm

    init {
        val step = haapiFlowViewModel.liveStep.value
        if (step is InteractiveForm) {
            interactiveFormStep = step
        } else {
            throw IllegalStateException("The step should be InteractiveForm when using InteractiveFormViewModel")
        }

        setupInteractiveFormItems()
    }

    private fun setupInteractiveFormItems() {
        for (field in interactiveFormStep.action.model.fields) {
            when (field) {
                is Field.Text -> {
                    val inputType = when (field.kind) {
                        is Field.TextKind.Number -> InputType.TYPE_CLASS_NUMBER
                        is Field.TextKind.Tel -> InputType.TYPE_CLASS_PHONE
                        else -> InputType.TYPE_CLASS_TEXT
                    }

                    _interactiveFormItems.add(
                        InteractiveFormItem.EditText(
                            key = field.name,
                            inputType = inputType,
                            label = field.placeholder?.capitalize() ?: "",
                            hint = field.placeholder ?: "",
                            value = ""
                        )
                    )
                }

                is Field.Username -> {
                    _interactiveFormItems.add(
                        InteractiveFormItem.EditText(
                            key = field.name,
                            inputType = InputType.TYPE_CLASS_TEXT,
                            label = field.label?.message ?: "",
                            hint = field.label?.message ?: "",
                            value = ""
                        )
                    )
                }

                is Field.Password -> {
                    _interactiveFormItems.add(
                        InteractiveFormItem.Password(
                            key = field.name,
                            label = field.label?.message ?: "",
                            hint = field.label?.message ?: "",
                            value = ""
                        )
                    )
                }
            }
        }
    }

    val problemsLiveData: LiveData<ProblemContent?> = haapiFlowViewModel.liveStep.map { step ->
        if (step is ProblemStep) {
            Log.i(Constant.TAG, "Got a ProblemStep")
            val title = step.problem.title
            val problemBundles: MutableList<ProblemView.ProblemBundle> = mutableListOf()
            step.problem.messages?.forEach { userMessage ->
                val text = userMessage.text.message
                if (text != null) {
                    problemBundles.add(ProblemView.ProblemBundle(text = text, MessageStyle.Error()))
                }
            }
            ProblemContent(title = title, problemBundles = problemBundles)
        } else {
            Log.i(Constant.TAG, "Got a NO ProblemStep")
            null
        }
    }

    val isLoading: LiveData<Boolean>
        get() = haapiFlowViewModel.isLoading

    val links: List<Link>
        get() {
            return interactiveFormStep.links
        }

    val buttonTitle: String
        get() = interactiveFormStep.action.model.actionTitle?.message ?: "Submit"

    fun updateItem(item: InteractiveFormItem, position: Int) {
        _interactiveFormItems[position] = item
    }

    fun submit() {
        val parameters: MutableMap<String, String> = mutableMapOf()
        interactiveFormItems.forEach {
            when (it) {
                is InteractiveFormItem.EditText -> {
                    parameters[it.key] = it.value
                }
                is InteractiveFormItem.Password -> {
                    parameters[it.key] = it.value
                }
            }
        }
        haapiFlowViewModel.submit(
            interactiveFormStep.action.model,
            parameters = parameters
        )
    }
}

data class ProblemContent(val title: String, val problemBundles: List<ProblemView.ProblemBundle>)

class InteractiveFormViewModelFactory(private val haapiFlowViewModel: HaapiFlowViewModel): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InteractiveFormViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InteractiveFormViewModel(haapiFlowViewModel) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class InteractiveFormViewModel")
    }
}

sealed class InteractiveFormItem {
    abstract val id: Long

    data class EditText(val key: String, val inputType: Int,  val label: String, val hint: String, var value: String): InteractiveFormItem() {
        override val id: Long = key.hashCode().toLong()
    }

    data class Password(val key: String, val label: String, val hint: String, var value: String): InteractiveFormItem() {
        override val id: Long = key.hashCode().toLong()
    }
}
private class InteractiveFormAdapter(val itemChangeHandler: (Int, InteractiveFormItem) -> Unit): ListAdapter<InteractiveFormItem, RecyclerView.ViewHolder>(CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            InteractiveFormType.EditTextView.ordinal -> FormTextViewHolder.from(parent)
            InteractiveFormType.PasswordView.ordinal -> PasswordTextViewHolder.from(parent)
            else -> throw ClassCastException("No class for viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PasswordTextViewHolder -> {
                val item = getItem(position) as InteractiveFormItem.Password
                holder.bind(
                    label = item.label,
                    hint = item.hint,
                    value = item.value,
                    textWatcher = object: TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            // NOP
                        }

                        override fun afterTextChanged(s: Editable?) {
                            // NOP
                        }

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            item.value = s.toString()
                            itemChangeHandler(position, item)
                        }
                    }
                )
            }
            is FormTextViewHolder -> {
                val item = getItem(position) as InteractiveFormItem.EditText
                holder.bind(label = item.label, hint = item.hint, value = item.value, textWatcher = object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        // NOP
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // NOP
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        item.value = s.toString()
                        itemChangeHandler(position, item)
                    }
                })
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is InteractiveFormItem.EditText -> InteractiveFormType.EditTextView.ordinal
            is InteractiveFormItem.Password -> InteractiveFormType.PasswordView.ordinal
        }
    }

    private enum class InteractiveFormType {
        EditTextView,
        PasswordView
    }

    companion object {
        private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<InteractiveFormItem>() {
            override fun areItemsTheSame(oldItem: InteractiveFormItem, newItem: InteractiveFormItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: InteractiveFormItem, newItem: InteractiveFormItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}