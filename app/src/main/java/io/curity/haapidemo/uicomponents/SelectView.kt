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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R

class SelectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val labelTextView: TextView
    private val spinner: Spinner

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.select_view, this, true)

        labelTextView = root.findViewById(R.id.label_text_view)
        spinner = root.findViewById(R.id.spinner)

        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.SelectView,
            0,
            defStyleAttr
        ).apply {
            try {
                getResourceId(R.styleable.SelectView_labelStyle, R.style.TextAppearance_Body).let {
                    labelTextView.setTextAppearance(it)
                }
            } finally {
                recycle()
            }
        }
    }

    fun setLabelText(text: CharSequence) {
        labelTextView.text = text
    }

    fun setAdapter(adapter: ArrayAdapter<String>, selectedPosition: Int) {
        spinner.adapter = adapter
        if (adapter.getItem(selectedPosition) == null) {
            spinner.setSelection(0)
        } else {
            spinner.setSelection(selectedPosition)
        }
    }

    fun setOnItemSelectedListener(onItemSelectedListener: AdapterView.OnItemSelectedListener) {
        spinner.onItemSelectedListener = onItemSelectedListener
    }

    fun removeOnItemSelectedListener() {
        spinner.onItemSelectedListener = null
    }
}