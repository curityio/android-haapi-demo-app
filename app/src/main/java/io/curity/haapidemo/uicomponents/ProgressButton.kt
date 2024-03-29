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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R
import io.curity.haapidemo.utils.setStroke
import io.curity.haapidemo.utils.toDpi

/**
 * `ProgressButton` is an UI component that is similar to a `Button` with a "spinner" `ProgressBar` in the center.
 *
 * To display the spinner, it is mandatory to call [ProgressButton.setLoading] with `true`.
 * To style it, check ProgressButton stylable to see which properties can be styled.
 */
class ProgressButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr), ViewStopLoadable {

    private val progressBar: ProgressBar
    private val textView: TextView
    private val imageView: ImageView
    private val space: Space

    var isLoading: Boolean = false
        private set

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.progress_btn_layout, this, true)
        progressBar = root.findViewById(R.id.progressBar)
        textView = root.findViewById(R.id.text_view)
        imageView = root.findViewById(R.id.image_view)
        space = root.findViewById(R.id.spacer)

        loadAttrs(attrs, defStyleAttr)

        progressBar.visibility = View.GONE
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.ProgressButton,
            0,
            defStyleAttr
        )
            .apply {
                try {
                    val bgDrawable = getDrawable(R.styleable.ProgressButton_background) ?: AppCompatResources.getDrawable(context, R.drawable.primary_progress_btn_shape)
                    background = bgDrawable

                    val foreground = getDrawable(R.styleable.ProgressButton_android_foreground)
                    if (foreground != null) {
                        setForeground(foreground)
                    } else {
                        val typedValue = TypedValue()
                        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                        setForeground(AppCompatResources.getDrawable(context, typedValue.resourceId))
                    }

                    val textAppearance = getResourceId(R.styleable.ProgressButton_textAppearanceButton, R.style.TextAppearance_ProgressButton_Primary)
                    textView.setTextAppearance(textAppearance)

                    val progressBarColor = getColor(R.styleable.ProgressButton_tintColor, Color.parseColor("#FFFFFF"))
                    progressBar.indeterminateDrawable.setTint(progressBarColor)

                    getColor(R.styleable.ProgressButton_strokeColor, Color.BLACK).let {
                        background.setStroke(it, (1).toDpi(resources))
                    }

                    val text = getText(R.styleable.ProgressButton_android_text)
                    textView.text = text

                } finally {
                    recycle()
                }
        }
    }

    /**
     * Set a text to the ProgressButton
     *
     * @param text A String
     */
    @Suppress("Unused")
    fun setText(text: String) {
        textView.text = text
    }

    /**
     * Returns the text of ProgressButton
     */
    @Suppress("Unused")
    fun getText(): String {
        return textView.text.toString()
    }

    /**
     * Sets an image with a @DrawableRes [image]
     */
    fun setImage(@DrawableRes image: Int) {
        imageView.setImageResource(image)
        imageView.visibility = VISIBLE
        space.visibility = VISIBLE
    }

    /**
     * Set the loading state to `ProgressButton`. If loading is `true` then the text is hidden, the spinner is shown and
     * ProgressButton is not clickable unless calling `setLoading(false)`.
     *
     * @param loading A Boolean for the loading state of ProgressButton
     */
    fun setLoading(loading: Boolean) {
        isLoading = loading
        if (isLoading) {
            progressBar.visibility = VISIBLE
            textView.visibility = GONE
            imageView.visibility = GONE
            isClickable = false
        } else {
            progressBar.visibility = GONE
            textView.visibility = VISIBLE
            imageView.visibility = VISIBLE
            isClickable = true
        }
    }

    override fun stopLoading() {
        setLoading(false)
    }
}