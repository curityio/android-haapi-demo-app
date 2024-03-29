/*
 *  Copyright (C) 2022 Curity AB
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R
import org.json.JSONObject

class DecodedTokenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val labelTextView: TextView
    private val verticalLinearLayout: LinearLayout

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.decoded_token_view, this, true)

        labelTextView = root.findViewById(R.id.header)
        verticalLinearLayout = root.findViewById(R.id.vertical_linear_layout)

        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.DecodedTokenView,
            0,
            defStyleAttr
        ).apply {
            try {
                getResourceId(R.styleable.DecodedTokenView_labelStyle,
                    R.style.TextAppearance_BodyMedium).let {
                    labelTextView.setTextAppearance(it)
                }

                getText(R.styleable.DecodedTokenView_labelText).let {
                    labelTextView.text = it
                }
            } finally {
                recycle()
            }
        }
    }

    fun setContent(label: CharSequence, contents: JSONObject) {
        labelTextView.text = label
        verticalLinearLayout.removeAllViews()
        contents.keys().forEach { key ->
            val labelTextView = KeyValueView(context).apply {
                this.setKey(key)
                try {
                    contents.getString(key).let { this.setValue(it) }
                    contents[key].let { this.setValueStyle(obj = it) }
                } catch (e: Throwable) {
                    // Currently this view does not report errors so only output to the console
                    println(e)
                }
            }
            verticalLinearLayout.addView(labelTextView)
        }
    }
}