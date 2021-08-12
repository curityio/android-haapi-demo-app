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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Space
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import io.curity.haapidemo.models.InteractiveForm
import io.curity.haapidemo.models.haapi.Field
import io.curity.haapidemo.models.haapi.Link
import io.curity.haapidemo.models.haapi.UserMessage
import io.curity.haapidemo.models.haapi.actions.ActionModel
import io.curity.haapidemo.models.haapi.extensions.messageStyle
import io.curity.haapidemo.models.haapi.extensions.toInteractiveFormItemCheckbox
import io.curity.haapidemo.models.haapi.problems.*
import io.curity.haapidemo.uicomponents.*
import io.curity.haapidemo.utils.Clearable
import io.curity.haapidemo.utils.dismissKeyboard
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

class InteractiveFormFragment: Fragment(R.layout.fragment_interactive_form) {

    private lateinit var messagesLayout: LinearLayout
    private lateinit var spacerProblem: Space
    private lateinit var problemView: ProblemView
    private lateinit var recyclerView: RecyclerView
    private lateinit var linksLayout: LinearLayout

    private lateinit var interactiveFormViewModel: InteractiveFormViewModel

    private var weakButton: WeakReference<ViewStopLoadable>? = null
    private var buttonList: MutableList<ProgressButton> = mutableListOf()

    private var isInterceptingTouch = false

    private val adapter = InteractiveFormAdapter(
        itemChangeHandler = { position, item -> interactiveFormViewModel.updateItem(item, position) },
        selectButtonHandler = { actionModelForm, progressButtonViewHolder ->
            dismissKeyboard()
            weakButton = WeakReference(progressButtonViewHolder)
            interactiveFormViewModel.submitActionModelForm(actionModelForm)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val haapiFlowViewModel = ViewModelProvider(requireActivity()).get(HaapiFlowViewModel::class.java)
        interactiveFormViewModel = ViewModelProvider(this, InteractiveFormViewModelFactory(haapiFlowViewModel))
            .get(InteractiveFormViewModel::class.java)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messagesLayout = view.findViewById(R.id.linear_layout_messages)
        spacerProblem = view.findViewById(R.id.spacer_problem)
        problemView = view.findViewById(R.id.problem_view)
        recyclerView = view.findViewById(R.id.recycler_view)
        linksLayout = view.findViewById(R.id.linear_layout_links)

        recyclerView.adapter = adapter

        adapter.submitList(interactiveFormViewModel.interactiveFormItems)

        recyclerView.addOnItemTouchListener(object: RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return isInterceptingTouch
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) { /* NOP */ }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) { /* NOP */ }
        })

        interactiveFormViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            isInterceptingTouch = isLoading
            buttonList.forEach {
                it.isClickable = !isLoading
            }

            if (!isLoading) {
                weakButton?.get()?.stopLoading()
                weakButton = null
            }
        })

        if (interactiveFormViewModel.userMessages.isEmpty()) {
            messagesLayout.visibility = View.GONE
        } else {
            messagesLayout.visibility = View.VISIBLE
            messagesLayout.removeAllViews()
            interactiveFormViewModel.userMessages.forEach { userMessage ->
                val messageView = MessageView.newInstance(
                    context = requireContext(),
                    text = userMessage.text.message ?: "",
                    style = userMessage.messageStyle()
                )
                messagesLayout.addView(messageView)
            }
        }

        interactiveFormViewModel.problemsLiveData.observe(viewLifecycleOwner, Observer {  problemContent ->
            val visibility = if (problemContent == null) View.GONE else View.VISIBLE
            problemView.visibility = visibility
            spacerProblem.visibility = visibility

            if (problemContent != null) {
                problemView.setTitle(problemContent.title)
                problemView.setProblemBundles(problemContent.problemBundles)
            }

            adapter.notifyDataSetChanged()
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

    val userMessages: List<UserMessage>
        get() = interactiveFormStep.messages

    val mapDataCopy: HashMap<Long, String>
        get() {
            val result = HashMap<Long, String>()
            _interactiveFormItems.forEach {
                when (it) {
                    is InteractiveFormItem.EditText -> { result[it.id] = it.value }
                    is InteractiveFormItem.Password -> { result[it.id] = it.value }
                    else -> { /* NOP */ }
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
        for (action in interactiveFormStep.actions) {
            for (field in action.model.fields) {
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
                        Log.d(Constant.TAG, "Field.Hidden is ignored")
                    }

                    is Field.Select -> {
                        val selectOptions = field.options.mapNotNull { option ->
                            val label = option.label.message
                            if (label == null) {
                                null
                            } else {
                                SelectOption(
                                    label = label,
                                    value = option.value
                                )
                            }
                        }
                        _interactiveFormItems.add(
                            InteractiveFormItem.Select(
                                key = field.name,
                                label = field.label.message ?: "",
                                selectOptions = selectOptions,
                                selectedIndex = field.options.indexOfFirst { it.selected }
                            )
                        )
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
                                value = field.value ?: ""
                            )
                        )
                    }

                    is Field.Password -> {
                        _interactiveFormItems.add(
                            InteractiveFormItem.Password(
                                key = field.name,
                                label = field.label?.message ?: "",
                                hint = field.label?.message ?: "",
                                value = field.value ?: ""
                            )
                        )
                    }
                    is Field.Checkbox -> {
                        _interactiveFormItems.add(
                            field.toInteractiveFormItemCheckbox()
                        )
                    }
                }
            }
            _interactiveFormItems.add(
                InteractiveFormItem.Button(
                    key = action.kind,
                    label = action.model.actionTitle?.message ?: "",
                    actionModelForm = action.model
                )
            )
        }
    }

    val problemsLiveData: LiveData<ProblemContent?> = haapiFlowViewModel.problemStepLiveData.map { step ->
        if (step != null) {
            Log.d(Constant.TAG, "Got a ProblemStep")
            if (step.problem is InvalidInputProblem) {
                Log.d(Constant.TAG, "Before {$_interactiveFormItems}")
                _interactiveFormItems.forEach { it.hasError = false }
                // Refresh interactiveFormItems
                step.problem.invalidFields.forEach { invalidField ->
                    try {
                        _interactiveFormItems.first { it.key == invalidField.name }.hasError = true
                    } catch (exception: NoSuchElementException ) {
                        Log.e(Constant.TAG_ERROR, "Cannot find {${invalidField.name}}: $exception")
                    }
                }
                Log.d(Constant.TAG, "After {$_interactiveFormItems}")
            }
            ProblemContent(
                title = step.problem.title,
                problemBundles = step.problem.problemBundle()
            )
        } else {
            _interactiveFormItems.forEach { it.hasError = false }
            null
        }
    }

    val isLoading: LiveData<Boolean>
        get() = haapiFlowViewModel.isLoading

    val links: List<Link>
        get() {
            return interactiveFormStep.links
        }

    fun updateItem(item: InteractiveFormItem, position: Int) {
        Log.d(Constant.TAG, "Item was updated at position: $position - $item")
        _interactiveFormItems[position] = item
    }

    fun submitActionModelForm(actionModelForm: ActionModel.Form) {
        val indexAction = interactiveFormItems.indexOfFirst { it.id == actionModelForm.hashCode().toLong() }
        if (indexAction != -1) {
            val parameters: MutableMap<String, String> = mutableMapOf()
            var index = indexAction - 1
            while (index >= 0) {
                when (val item = interactiveFormItems[index]) {
                    is InteractiveFormItem.EditText -> {
                        parameters[item.key] = item.value
                    }
                    is InteractiveFormItem.Password -> {
                        parameters[item.key] = item.value
                    }
                    is InteractiveFormItem.Checkbox -> {
                        if (item.checked) {
                            parameters[item.key] = item.value
                        } else {
                            parameters[item.key] = ""
                        }
                    }
                    is InteractiveFormItem.Select -> {
                        parameters[item.key] = item.selectOptions[item.selectedIndex].value
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

    fun followLink(link: Link) {
        haapiFlowViewModel.followLink(link)
    }

    data class ProblemContent(val title: String, val problemBundles: List<ProblemView.ProblemBundle>)
}

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
    abstract val key: String
    abstract val label: String
    abstract var hasError: Boolean

    data class EditText(
        override val key: String,
        val inputType: Int,
        override val label: String,
        val hint: String,
        var value: String,
        override var hasError: Boolean = false
    ): InteractiveFormItem()
    {
        override val id: Long = key.hashCode().toLong()
    }

    data class Password(
        override val key: String,
        override val label: String,
        val hint: String,
        var value: String,
        override var hasError: Boolean = false
    ): InteractiveFormItem()
    {
        override val id: Long = key.hashCode().toLong()
    }

    data class Button(override val key: String, override val label: String, override var hasError: Boolean = false, val actionModelForm: ActionModel.Form): InteractiveFormItem() {
        override val id: Long = actionModelForm.hashCode().toLong()
    }

    data class Checkbox(override val key: String, override val label: String, val readonly: Boolean, var checked: Boolean, val value: String, override var hasError: Boolean = false): InteractiveFormItem() {
        override val id: Long = key.hashCode().toLong()
    }

    data class Select(override val key: String, override val label: String, val selectOptions: List<SelectOption>, var selectedIndex: Int, override var hasError: Boolean = false): InteractiveFormItem() {
        override val id: Long = key.hashCode().toLong()
    }
}

data class SelectOption(val label: String, val value: String)

class InteractiveFormAdapter(
    val itemChangeHandler: (Int, InteractiveFormItem) -> Unit,
    val selectButtonHandler: (ActionModel.Form, ProgressButtonViewHolder) -> Unit
): ListAdapter<InteractiveFormItem, RecyclerView.ViewHolder>(CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            InteractiveFormType.EditTextView.ordinal -> FormTextViewHolder.from(parent)
            InteractiveFormType.PasswordView.ordinal -> PasswordTextViewHolder.from(parent)
            InteractiveFormType.ButtonView.ordinal -> ProgressButtonViewHolder.from(parent)
            InteractiveFormType.CheckboxView.ordinal -> CheckboxViewHolder.from(parent, leftMargin = 0)
            InteractiveFormType.SelectView.ordinal -> SelectViewHolder.from(parent)
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
                    hasError = item.hasError,
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
                holder.bind(label = item.label, hint = item.hint, hasError = item.hasError, value = item.value, textWatcher = object: TextWatcher {
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
            is ProgressButtonViewHolder -> {
                val item = getItem(position) as InteractiveFormItem.Button
                holder.bind(
                    text = item.label,
                    onClickListener = {
                        selectButtonHandler(item.actionModelForm, it)
                    }
                )
            }
            is CheckboxViewHolder -> {
                val item = getItem(position) as InteractiveFormItem.Checkbox
                holder.bind(
                    text = item.label,
                    isChecked = item.checked,
                    isClickable = !item.readonly,
                    didToggle = {
                        item.checked = !item.checked
                        itemChangeHandler(position, item)
                    }
                )
            }
            is SelectViewHolder -> {
                val item = getItem(position) as InteractiveFormItem.Select
                holder.bind(
                    label = item.label,
                    values = item.selectOptions.map { it.label },
                    defaultSelectPosition = item.selectedIndex,
                    onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, selectedIndex: Int, id: Long) {
                            item.selectedIndex = selectedIndex
                            itemChangeHandler(position, item)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) { /* NOP */ }
                    }
                )
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? Clearable)?.clear()
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is InteractiveFormItem.EditText -> InteractiveFormType.EditTextView.ordinal
            is InteractiveFormItem.Password -> InteractiveFormType.PasswordView.ordinal
            is InteractiveFormItem.Button -> InteractiveFormType.ButtonView.ordinal
            is InteractiveFormItem.Checkbox -> InteractiveFormType.CheckboxView.ordinal
            is InteractiveFormItem.Select -> InteractiveFormType.SelectView.ordinal
        }
    }

    private enum class InteractiveFormType {
        EditTextView,
        PasswordView,
        ButtonView,
        CheckboxView,
        SelectView
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

private fun HaapiProblem.problemBundle(): List<ProblemView.ProblemBundle> {
    val problemBundles: MutableList<ProblemView.ProblemBundle> = mutableListOf()
    when (this) {
        is InvalidInputProblem -> {
            invalidFields.forEach { invalidField ->
                val detail = invalidField.detail
                if (detail != null) {
                    problemBundles.add(
                        ProblemView.ProblemBundle(
                            text = detail,
                            messageStyle = MessageStyle.Error()
                        )
                    )
                }
            }
        }
        is AuthorizationProblem -> { // Should not happen here but let's do it in the worst case
            problemBundles.add(
                ProblemView.ProblemBundle(
                    text = errorDescription,
                    messageStyle = MessageStyle.Error()
                )
            )
        }
        else -> {
            messages?.forEach { userMessage ->
                val text = userMessage.text.message
                if (text != null) {
                    problemBundles.add(
                        ProblemView.ProblemBundle(
                            text = text,
                            messageStyle = userMessage.messageStyle()
                        )
                    )
                }
            }
        }
    }

    return problemBundles.toList()
}