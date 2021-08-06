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
import android.widget.LinearLayout
import android.widget.TextView
import io.curity.haapidemo.R

class ProblemView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val textView: TextView
    private val problemsLinearLayout: LinearLayout

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.problem_view, this, true)

        textView = root.findViewById(R.id.title)
        problemsLinearLayout = root.findViewById(R.id.linear_layout_problems)

        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.ProblemView,
            0,
            defStyleAttr
        ).apply {
            try {
                getResourceId(R.styleable.ProblemView_textAppearance, R.style.TextAppearance_Body_Error).let {
                    textView.setTextAppearance(it)
                }

                getText(R.styleable.ProblemView_android_text).let {
                    textView.text = it
                }
            } finally {
                recycle()
            }
        }
    }

    fun setTitle(title: String) {
        textView.text = title
    }

    fun setProblemBundles(problems: List<ProblemBundle>) {
        problemsLinearLayout.removeAllViews()

        problems.forEach {
            val messageView = MessageView(context)
            messageView.setText(it.text)
            messageView.applyStyle(it.messageStyle)

            problemsLinearLayout.addView(messageView)
        }
    }

    data class ProblemBundle(val text: String, val messageStyle: MessageStyle)
}