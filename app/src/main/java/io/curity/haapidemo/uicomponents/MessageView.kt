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
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R
import io.curity.haapidemo.utils.setBackgroundColor

class MessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val textView: TextView

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.message_view, this, true)

        imageView = root.findViewById(R.id.image_view)
        textView = root.findViewById(R.id.text_view)

        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.MessageView,
            0,
            defStyleAttr
        ).apply {
            try {
                val bgDrawable = getDrawable(R.styleable.MessageView_background) ?: AppCompatResources.getDrawable(
                    context,
                    R.drawable.message_view_shape
                )
                val backgroundColor = getColor(R.styleable.MessageView_backgroundColor, Color.WHITE)
                bgDrawable.setBackgroundColor(backgroundColor)
                background = bgDrawable

                val tintColor = getColor(R.styleable.MessageView_tintColor, Color.BLACK)
                imageView.setColorFilter(tintColor)

                val textAppearance = getResourceId(R.styleable.MessageView_textAppearance, R.style.TextAppearance_Caption)
                textView.setTextAppearance(textAppearance)

                val textColor = getColor(R.styleable.MessageView_android_textColor, Color.BLACK)
                textView.setTextColor(textColor)

                val text = getText(R.styleable.MessageView_android_text)
                textView.text = text

                val imageResource = getDrawable(R.styleable.MessageView_srcCompat) ?: AppCompatResources.getDrawable(
                    context,
                    R.drawable.ic_warning
                )
                imageView.setImageDrawable(imageResource)
            } finally {
                recycle()
            }
        }
    }

    @Suppress("Unused")
    fun setText(text: CharSequence) {
        textView.text = text
    }

    @Suppress("Unused")
    fun applyStyle(style: MessageStyle) {
        background.setBackgroundColor(resources.getColor(style.backgroundColor, null))

        textView.setTextColor(resources.getColor(style.textColor, null))
        textView.setTextAppearance(style.textColor)

        imageView.setColorFilter(resources.getColor(style.tintColor, null))
        imageView.setImageResource(style.imageResource)
    }

    companion object {
        fun newInstance(context: Context, text: String, style: MessageStyle): MessageView {
            val view = MessageView(context)
            val newLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            newLayoutParams.setMargins(0, context.resources.getDimension(R.dimen.padding).toInt(), 0, 0)
            view.layoutParams = newLayoutParams
            view.setText(text)
            view.applyStyle(style)
            return view
        }
    }
}

sealed class MessageStyle {

    abstract val backgroundColor: Int
    abstract val textColor: Int
    abstract val tintColor: Int
    abstract val imageResource: Int
    abstract val textAppearance: Int

    @Suppress("Unused")
    data class Warning(
        override val backgroundColor: Int = R.color.warning_background,
        override val textColor: Int = R.color.warning_text_color,
        override val tintColor: Int = R.color.warning_text_color,
        override val imageResource: Int = R.drawable.ic_warningrounded,
        override val textAppearance: Int = R.style.TextAppearance_Caption
    ) : MessageStyle()

    @Suppress("Unused")
    data class Error(
        override val backgroundColor: Int = R.color.error_background,
        override val textColor: Int = R.color.error_text_color,
        override val tintColor: Int = R.color.error_text_color,
        override val imageResource: Int = R.drawable.ic_warning,
        override val textAppearance: Int = R.style.TextAppearance_Caption
    ) : MessageStyle()

    @Suppress("Unused")
    data class Info(
        override val backgroundColor: Int = R.color.info_background,
        override val textColor: Int = R.color.info_text_color,
        override val tintColor: Int = R.color.info_text_color,
        override val imageResource: Int = R.drawable.ic_warningrounded,
        override val textAppearance: Int = R.style.TextAppearance_Caption
    ) : MessageStyle()
}