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
import java.lang.ref.WeakReference

class InteractiveFormFragment: Fragment() {

    private lateinit var spacerProblem: Space
    private lateinit var problemView: ProblemView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressButton: ProgressButton
    private lateinit var linksLayout: LinearLayout

    private lateinit var interactiveFormViewModel: InteractiveFormViewModel

    private var weakButton: WeakReference<ProgressButton>? = null
    private var buttonList: MutableList<ProgressButton> = mutableListOf()

    private val adapter = InteractiveFormAdapter(itemChangeHandler = { position, item -> interactiveFormViewModel.updateItem(item, position) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
        // Cannot bind InteractiveFormViewModel with a Fragment lifecycle due to edit data and rotation.
        // Therefore, it has to be bind with an activity and instantiate() needs to be called
        interactiveFormViewModel = ViewModelProvider(requireActivity(), InteractiveFormViewModelFactory(haapiFlowViewModel))
            .get(InteractiveFormViewModel::class.java)
        interactiveFormViewModel.instantiate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        Log.d(Constant.TAG, "Saving edit data from interactiveFormViewModel")
        outState.putSerializable(DATA_COPY, interactiveFormViewModel.mapDataCopy)
    }

    @Suppress("Unchecked_cast")
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        val dataSaved = savedInstanceState?.getSerializable(DATA_COPY) as? HashMap<Long, String>
        if (dataSaved != null) {
            Log.d(Constant.TAG, "Restoring edit data for interactiveFormViewModel")
            interactiveFormViewModel.restore(dataSaved)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_interactive_form, container, false)

        spacerProblem = root.findViewById(R.id.spacer_problem)
        problemView = root.findViewById(R.id.problem_view)
        recyclerView = root.findViewById(R.id.recycler_view)
        progressButton = root.findViewById(R.id.progress_button)
        linksLayout = root.findViewById(R.id.linear_layout_links)

        recyclerView.adapter = adapter

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
        buttonList.add(progressButton)

        interactiveFormViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            weakButton?.get()?.setLoading(isLoading)
            buttonList.forEach {
                it.isClickable = !isLoading
            }
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

        val links = interactiveFormViewModel.links
        linksLayout.removeAllViews()
        links.forEach { link ->
            val button = ProgressButton(requireContext(), null, R.style.LinkProgressButton).apply {
                this.setText(link.title?.message ?: "")
                this.setOnClickListener {
                    weakButton = WeakReference(this)
                    interactiveFormViewModel.followLink(link)
                }
            }
            buttonList.add(button)
            linksLayout.addView(button)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Constant.TAG_FRAGMENT_LIFECYCLE, "InteractiveForm is destroyed")
    }

    companion object {
        private const val DATA_COPY = "io.curity.haapidemo.interactiveFormFragment.data_copy"

        fun newInstance(): InteractiveFormFragment {
            return InteractiveFormFragment()
        }
    }
}

class InteractiveFormViewModel(private val haapiFlowViewModel: HaapiFlowViewModel): ViewModel() {

    private var _interactiveFormItems: MutableList<InteractiveFormItem> = mutableListOf()
    val interactiveFormItems: List<InteractiveFormItem>
        get() = _interactiveFormItems

    private lateinit var interactiveFormStep: InteractiveForm

    fun instantiate() {
        val step = haapiFlowViewModel.liveStep.value
        if (step is InteractiveForm) {
            interactiveFormStep = step
        } else {
            throw IllegalStateException("The step should be InteractiveForm when using InteractiveFormViewModel")
        }

        setupInteractiveFormItems()
    }

    val mapDataCopy: HashMap<Long, String>
        get() {
            val result = HashMap<Long, String>()
            _interactiveFormItems.forEach {
                when (it) {
                    is InteractiveFormItem.EditText -> { result[it.id] = it.value }
                    is InteractiveFormItem.Password -> { result[it.id] = it.value }
                }
            }
            return result
        }

    fun restore(mapDataCopy: HashMap<Long, String>) {
        mapDataCopy.forEach { (key, value) ->
            when (val item = _interactiveFormItems.first { it.id == key }) {
                is InteractiveFormItem.EditText -> { item.value = value }
                is InteractiveFormItem.Password -> { item.value = value }
            }
        }
    }

    private fun setupInteractiveFormItems() {
        _interactiveFormItems.clear()

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
                            label = field.label?.message ?: "",
                            hint = field.placeholder ?: "",
                            value = field.value ?: ""
                        )
                    )
                }
                is Field.Hidden -> {
                    Log.d(Constant.TAG, "Field.Hidden was not handled")
                }

                is Field.Select -> {
                    Log.d(Constant.TAG, "Field.Select was not handled")
                }

                is Field.Context -> {
                    Log.d(Constant.TAG, "Field.Context was not handled")
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

                else -> {
                    Log.d(Constant.TAG, "$field was not handled")
                }
            }
        }
    }

    val problemsLiveData: LiveData<ProblemContent?> = haapiFlowViewModel.liveStep.map { step ->
        if (step is ProblemStep) {
            Log.d(Constant.TAG, "Got a ProblemStep")
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
        Log.d(Constant.TAG, "Item was updated at position: $position - $item")
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

    fun followLink(link: Link) {
        haapiFlowViewModel.followLink(link)
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