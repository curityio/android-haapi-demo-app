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
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R
import io.curity.haapidemo.models.DecodedJwtData
import kotlinx.android.synthetic.main.disclosure_view.view.*

class DisclosureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val titleTextView: TextView
    private val toggleButton: ToggleButton

    private val collapseLinearLayout: LinearLayout
    private val contentTextView: TextView
    private val verticalLinearLayout: LinearLayout

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.disclosure_view, this, true)

        titleTextView = root.findViewById(R.id.title)
        toggleButton = root.findViewById(R.id.toggle_button)

        collapseLinearLayout = root.findViewById(R.id.collapse_linear_layout)
        contentTextView = root.findViewById(R.id.content_text)
        verticalLinearLayout = root.findViewById(R.id.vertical_linear_layout)

        loadAttrs(attrs, defStyleAttr)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                collapseLinearLayout.visibility = View.VISIBLE
            } else {
                collapseLinearLayout.visibility = View.GONE
            }
        }

        root.setOnClickListener {
            toggleButton.isChecked = !toggleButton.isChecked
        }
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.DisclosureView,
            0,
            defStyleAttr
        ).apply {
            try {
                getResourceId(R.styleable.DisclosureView_titleStyle, R.style.TextAppearance_BodyMedium).let {
                    titleTextView.setTextAppearance(it)
                }

                getResourceId(R.styleable.DisclosureView_contentStyle, R.style.TextAppearance_Code18).let {
                    contentTextView.setTextAppearance(it)
                }

                getString(R.styleable.DisclosureView_titleText).let {
                    titleTextView.text = it
                }

                getString(R.styleable.DisclosureView_contentText).let {
                    contentTextView.text = it
                }
            } finally {
                recycle()
            }
        }
    }

    @Suppress("Unused")
    fun setTitleText(titleText: CharSequence) {
        titleTextView.text = titleText
    }

    @Suppress("Unused")
    fun setContentText(contentText: CharSequence) {
        contentTextView.text = contentText
    }

    @Suppress("Unused")
    fun setDecodedContent(decodedContent: DecodedJwtData) {
        verticalLinearLayout.removeAllViews()

        verticalLinearLayout.addView(
            DecodedTokenView(context).apply {
                decodedContent.getHeaderObj()?.let {
                    this.setContent(label = context.getString(R.string.decoded_jwt_header),
                        contents = it
                    )
                }
            }
        )

        verticalLinearLayout.addView(
            DecodedTokenView(context).apply {
                decodedContent.getPayloadObj()?.let {
                    this.setContent(label = context.getString(R.string.decoded_jwt_payload),
                        contents = it
                    )
                }
            }
        )

        contentTextView.maxLines = 3
        contentTextView.ellipsize = TextUtils.TruncateAt.END
    }

    @Suppress("Unused")
    fun setDisclosureContents(contents: List<DisclosureContent>) {
        verticalLinearLayout.removeAllViews()
        contents.forEach { content ->
            val labelTextView = LabelTextView(context).apply {
                this.setLabel(content.label)
                this.setValue(content.description)
            }

            verticalLinearLayout.addView(labelTextView)
        }
    }
}

data class DisclosureContent(val label: CharSequence, val description: CharSequence)