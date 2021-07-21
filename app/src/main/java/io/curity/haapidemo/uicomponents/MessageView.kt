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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R

class MessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val textView: TextView

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.message_view, this, true)

        imageView = root.findViewById(R.id.imageView)
        textView = root.findViewById(R.id.textView)

        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.MessageView,
            defStyleAttr,
            0
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

                val textColor = getColor(R.styleable.MessageView_android_textColor, Color.BLACK)
                textView.setTextColor(textColor)

                val textAppearance = getResourceId(R.styleable.MessageView_textAppearance, R.style.TextAppearance_Caption)
                textView.setTextAppearance(textAppearance)

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
        background.setBackgroundColor(style.backgroundColor)

        textView.setTextColor(style.textColor)
        textView.setTextAppearance(style.textAppearance)

        imageView.setColorFilter(style.tintColor)
        imageView.setImageResource(style.imageResource)
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
        override val textColor: Int = R.color.warning,
        override val tintColor: Int = R.color.warning,
        override val imageResource: Int = R.drawable.ic_warningrounded,
        override val textAppearance: Int = R.style.TextAppearance_Caption
    ) : MessageStyle()

    @Suppress("Unused")
    data class Error(
        override val backgroundColor: Int = R.color.error_background,
        override val textColor: Int = R.color.error,
        override val tintColor: Int = R.color.error,
        override val imageResource: Int = R.drawable.ic_warning,
        override val textAppearance: Int = R.style.TextAppearance_Caption
    ) : MessageStyle()

    @Suppress("Unused")
    data class Info(
        override val backgroundColor: Int = R.color.info_background,
        override val textColor: Int = R.color.info,
        override val tintColor: Int = R.color.info,
        override val imageResource: Int = R.drawable.ic_warningrounded,
        override val textAppearance: Int = R.style.TextAppearance_Caption
    ) : MessageStyle()
}

//region Private extension
private fun Drawable?.setBackgroundColor(@ColorInt colorInt: Int) {
    when (this) {
        is ShapeDrawable -> paint.color = colorInt
        is GradientDrawable -> setColor(colorInt)
        is ColorDrawable -> color = colorInt
    }
}
//endregion