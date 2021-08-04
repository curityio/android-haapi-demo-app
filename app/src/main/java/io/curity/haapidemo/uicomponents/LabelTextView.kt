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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R

class LabelTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val labelTextView: TextView
    private val valueTextView: TextView

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.label_text_view, this, true)

        labelTextView = root.findViewById(R.id.label)
        valueTextView = root.findViewById(R.id.value)

        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.LabelTextView,
            0,
            defStyleAttr
        ).apply {
            try {
                getResourceId(R.styleable.LabelTextView_labelStyle, R.style.TextAppearance_Code14).let {
                    labelTextView.setTextAppearance(it)
                }

                getResourceId(R.styleable.LabelTextView_valueStyle, R.style.TextAppearance_Code14).let {
                    valueTextView.setTextAppearance(it)
                }

                getText(R.styleable.LabelTextView_labelText).let {
                    labelTextView.text = it
                }

                getText(R.styleable.LabelTextView_valueText).let {
                    valueTextView.text = it
                }
            } finally {
                recycle()
            }
        }
    }

    fun setLabel(label: CharSequence) {
        labelTextView.text = label
    }

    fun setValue(value: CharSequence) {
        valueTextView.text = value
    }
}