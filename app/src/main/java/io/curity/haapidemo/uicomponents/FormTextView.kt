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
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updatePadding
import com.google.android.material.textfield.TextInputEditText
import io.curity.haapidemo.R
import io.curity.haapidemo.utils.setStroke
import io.curity.haapidemo.utils.toDpi

class FormTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val labelTextView: TextView
    private val textInputEditText: TextInputEditText
    private val imageView: ImageView
    private val visibilityToggleButton: ImageButton
    private val linearLayout: LinearLayout

    private var isTextVisible = false

    @StyleRes
    private var labelAppearance: Int = 0
    @StyleRes
    private var textAppearance: Int = 0

    /**
     * Returns an [inputText] for the editText element.
     * Sets a [CharSequence] to the editText element.
     */
    @Suppress("Unused")
    var inputText: CharSequence
        get() = textInputEditText.text.toString()
        set(value) = textInputEditText.setText(value)

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.form_text_view, this, true)

        labelTextView = root.findViewById(R.id.label_text_view)
        textInputEditText = root.findViewById(R.id.text_input_edit_text)
        imageView = root.findViewById(R.id.image_view)
        visibilityToggleButton = root.findViewById(R.id.image_button)
        linearLayout = root.findViewById(R.id.linear_layout)

        loadAttrs(attrs, defStyleAttr)
        adjustPadding()

        visibilityToggleButton.setOnClickListener {
            togglePassword()
        }
        imageView.setColorFilter(resources.getColor(R.color.error, null))
        visibilityToggleButton.setColorFilter(resources.getColor(android.R.color.black, null))
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.FormTextView,
            0,
            defStyleAttr
        ).apply {
            try {
                getResourceId(R.styleable.FormTextView_labelStyle, R.style.TextAppearance_Body).let {
                    labelTextView.setTextAppearance(it)
                    labelAppearance = it
                }

                getResourceId(R.styleable.FormTextView_valueStyle, R.style.TextAppearance_Body_Black).let {
                    textInputEditText.setTextAppearance(it)
                    textAppearance = it
                }

                getString(R.styleable.FormTextView_labelText).let {
                    labelTextView.text = it
                }

                getString(R.styleable.FormTextView_valueText).let {
                    textInputEditText.setText(it)
                }

                getString(R.styleable.FormTextView_android_hint).let {
                    textInputEditText.hint = it
                }

                getBoolean(R.styleable.FormTextView_passwordToggleEnabled, false).let {
                    if (it) {
                        textInputEditText.transformationMethod = PasswordTransformationMethod()
                        visibilityToggleButton.visibility = View.VISIBLE
                    } else {
                        visibilityToggleButton.visibility = View.GONE
                    }
                }

                enableError(getBoolean(R.styleable.FormTextView_errorEnabled, false))
            } finally {
                recycle()
            }
        }
    }

    /**
     * Sets the label with a [text]. The text is above of the editText.
     *
     * @param text A CharSequence
     */
    @Suppress("Unused")
    fun setLabelText(text: CharSequence) {
        labelTextView.text = text
    }

    /**
     * Enables error design.
     * If [isErrorEnabled] is True then the stroke line will turn to [R.color.error] and an error warning icon will be shown.
     * If [isErrorEnabled] is False then the strole line will turn to [R.color.grey] and there is no error warning.
     *
     * @param isErrorEnabled A boolean
     */
    @SuppressWarnings("WeakerAccess")
    fun enableError(isErrorEnabled: Boolean) {
        val widthDp = (2).toDpi(resources)

        if (isErrorEnabled) {
            imageView.visibility = View.VISIBLE
            val errorColor = resources.getColor(R.color.error, null)
            linearLayout.background.setStroke(errorColor, widthDp)
            textInputEditText.setTextColor(errorColor)
            labelTextView.setTextColor(errorColor)
        } else {
            imageView.visibility = View.GONE
            linearLayout.background.setStroke(resources.getColor(R.color.grey, null), widthDp)
            textInputEditText.setTextAppearance(textAppearance)
            labelTextView.setTextAppearance(labelAppearance)
        }

        adjustPadding()
    }

    /**
     * Sets the hint text for the editText. When there is no text then the hint will be shown.
     *
     * @param text A CharSequence
     */
    @Suppress("Unused")
    fun setHint(text: CharSequence) {
        textInputEditText.hint = text
    }

    /**
     * Adds a TextWatcher whenever the TextInput's text changes.
     *
     * @param textWatcher A TextWatcher
     */
    fun addTextChangedListener(textWatcher: TextWatcher) {
        textInputEditText.addTextChangedListener(textWatcher)
    }

    fun setInputType(inputType: Int) {
        textInputEditText.inputType = inputType
    }

    /**
     * Sets PasswordToggle for the FormTextView.
     * If [isEnabled] is True then Password toggle is enabled with a button toggle. The edited text can be visible or hidden with a button.
     * If [isEnabled] is False then Password toggle is disabled
     *
     * @param isEnabled A Boolean
     */
    @Suppress("Unused")
    fun setPasswordToggleEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            textInputEditText.transformationMethod = PasswordTransformationMethod()
            visibilityToggleButton.visibility = View.VISIBLE
        } else {
            visibilityToggleButton.visibility = View.GONE
        }
    }

    private fun togglePassword() {
        isTextVisible = !isTextVisible
        if (isTextVisible) {
            textInputEditText.transformationMethod = HideReturnsTransformationMethod()
            visibilityToggleButton.setImageResource(R.drawable.ic_outline_visibility_24)
        } else {
            textInputEditText.transformationMethod = PasswordTransformationMethod()
            visibilityToggleButton.setImageResource(R.drawable.ic_outline_visibility_off_24)
        }
    }

    private fun adjustPadding() {
        if (imageView.visibility == View.VISIBLE && visibilityToggleButton.visibility == View.VISIBLE) {
            imageView.updatePadding(right = 0)
        } else {
            imageView.updatePadding(right = (14).toDpi(resources))
        }
    }
}