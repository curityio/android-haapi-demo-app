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

package io.curity.haapidemo.uicomponents

import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import io.curity.haapidemo.utils.Clearable
import java.lang.ref.WeakReference

open class LinearVerticalLayoutViewHolder internal constructor(itemView: View): RecyclerView.ViewHolder(itemView) {
    val linearLayout: LinearLayout = itemView.findViewById(R.id.linear_layout)

    companion object {
        fun inflatedView(layoutInflater: LayoutInflater, parentView: ViewGroup): View {
            return layoutInflater.inflate(R.layout.linear_layout_item, parentView, false)
        }
    }
}

class SpaceViewHolder private constructor(itemView: View): LinearVerticalLayoutViewHolder(itemView) {
    companion object {
        fun from(parentView: ViewGroup, height: Int): SpaceViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            val viewHolder = SpaceViewHolder(view)
            val params = viewHolder.linearLayout.layoutParams
            params.height = height
            viewHolder.linearLayout.layoutParams = params

            return viewHolder
        }
    }
}

class SelectorViewHolder private constructor(itemView: View): LinearVerticalLayoutViewHolder(itemView), ViewStopLoadable {

    private val progressButton = ProgressButton(itemView.context, null, R.style.SecondaryProgressButton)

    init {
        super.linearLayout.addView(progressButton)
    }

    fun bind(label: String, imageResourceId: Int = 0, clickHandler: (WeakReference<ViewStopLoadable>) -> Unit) {
        progressButton.setText(label)
        if (imageResourceId != 0) {
            progressButton.setImage(imageResourceId)
        }
        progressButton.setOnClickListener {
            Log.i(Constant.TAG, "Progress Button clicked")
            progressButton.setLoading(true)
            progressButton.isClickable = false
            it.isClickable = false
            it.isEnabled = false
            clickHandler(WeakReference(this))
        }
    }

    override fun stopLoading() {
        Log.i(Constant.TAG, "Stop loading was called")
        progressButton.setLoading(false)
        progressButton.isEnabled = true
    }

    companion object {
        fun from(parentView: ViewGroup): SelectorViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            return SelectorViewHolder(view)
        }
    }
}

class ProgressButtonViewHolder private constructor(itemView: View): LinearVerticalLayoutViewHolder(itemView), ViewStopLoadable {
    private val progressButton = ProgressButton(itemView.context, null, R.style.PrimaryProgressButton)

    init {
        super.linearLayout.addView(progressButton)
    }

    fun bind(
        text: String,
        onClickListener: (ProgressButtonViewHolder) -> Unit
    ) {
        progressButton.setText(text)
        progressButton.setOnClickListener {
            progressButton.setLoading(true)
            onClickListener(this)
        }
    }

    override fun stopLoading() {
        progressButton.stopLoading()
    }

    companion object {
        fun from(parentView: ViewGroup): ProgressButtonViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            return ProgressButtonViewHolder(view)
        }
    }
}

open class FormTextViewHolder internal constructor(itemView: View): LinearVerticalLayoutViewHolder(itemView), Clearable {

    val formTextView = FormTextView(itemView.context, null, 0)

    private var refTextWatcher: WeakReference<TextWatcher> = WeakReference(null)

    init {
        super.linearLayout.addView(formTextView)
    }

    fun bind(
        label: String,
        hint: String,
        value: String,
        hasError: Boolean,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        textWatcher: TextWatcher)
    {
        clear()

        formTextView.setLabelText(label)
        formTextView.setHint(hint)
        formTextView.inputText = value
        formTextView.enableError(hasError)
        formTextView.setInputType(inputType)

        refTextWatcher = WeakReference(textWatcher)
        formTextView.addTextChangedListener(textWatcher)
    }

    override fun clear() {
        val textWatcher = refTextWatcher.get()
        if (textWatcher != null) {
            formTextView.removeTextChangedListener(textWatcher)
            refTextWatcher = WeakReference(null)
        }
    }

    companion object {
        fun from(parentView: ViewGroup): FormTextViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            return FormTextViewHolder(view)
        }
    }
}

class PasswordTextViewHolder private constructor(itemView: View): FormTextViewHolder(itemView) {

    fun bind(label: String, hint: String, value: String, hasError: Boolean, textWatcher: TextWatcher) {
        super.bind(label, hint, value, hasError = hasError, inputType = InputType.TYPE_CLASS_TEXT, textWatcher)

        formTextView.setPasswordToggleEnabled(true)
    }

    companion object {
        fun from(parentView: ViewGroup): PasswordTextViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            return PasswordTextViewHolder(view)
        }
    }
}

class SectionViewHolder private constructor(itemView: View, defStyleRes: Int = 0): LinearVerticalLayoutViewHolder(itemView) {
    private val textView = TextView(itemView.context, null, 0, defStyleRes)

    init {
        super.linearLayout.addView(textView)
    }

    fun bind(
        text: String
    ) {
        textView.text = text
    }

    companion object {
        fun from(parentView: ViewGroup, defStyleAttr: Int): SectionViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            return SectionViewHolder(view, defStyleAttr)
        }
    }
}

class SelectViewHolder private constructor(itemView: View): LinearVerticalLayoutViewHolder(itemView), Clearable {

    private val selectView = SelectView(itemView.context, null, 0)

    init {
        super.linearLayout.addView(selectView)
    }

    fun bind(
        label: String,
        values: List<String>,
        defaultSelectPosition: Int,
        onItemSelectedListener: AdapterView.OnItemSelectedListener
    ) {
        selectView.setLabelText(label)
        val arrayAdapter = ArrayAdapter<String>(
            itemView.context,
            R.layout.select_view_text_view,
            values
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        selectView.setAdapter(
            arrayAdapter,
            defaultSelectPosition
        )
        selectView.setOnItemSelectedListener(onItemSelectedListener)
    }

    override fun clear() {
        selectView.removeOnItemSelectedListener()
    }

    companion object {
        fun from(parentView: ViewGroup): SelectViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            return SelectViewHolder(view)
        }
    }
}

interface ViewStopLoadable {
    fun stopLoading()
}