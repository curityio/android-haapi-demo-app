package io.curity.haapidemo.utils

import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import io.curity.haapidemo.R

class ViewsCreator(private val context: Context) {
    private val marginParams = LinearLayout.LayoutParams(
        ConstraintSet.WRAP_CONTENT,
        ConstraintSet.WRAP_CONTENT
    )

    private val buttonMarginParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ConstraintSet.WRAP_CONTENT
    )

    private val paramsForFields = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ConstraintSet.WRAP_CONTENT
    )

    private val formParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ConstraintSet.WRAP_CONTENT
    )

    init {
        marginParams.topMargin = 20
        marginParams.bottomMargin = 20

        paramsForFields.setMargins(50, 20, 50, 20)

        formParams.gravity = Gravity.CENTER
    }

    fun header(text: String): TextView {
        val selectorHeader = TextView(context)
        selectorHeader.id = View.generateViewId()
        selectorHeader.text = text
        selectorHeader.textAlignment = View.TEXT_ALIGNMENT_CENTER
        selectorHeader.layoutParams = marginParams
        selectorHeader.setTextAppearance(R.style.LabelTextViewStyle)

        return selectorHeader
    }

    fun hiddenField(text: String): TextView {
        val field = TextView(context)
        field.id = View.generateViewId()
        field.text = text
        field.visibility = View.GONE

        return field
    }

    fun passwordField(label: String): EditText {
        val field = inputField(label)
        field.inputType = InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD or InputType.TYPE_CLASS_TEXT

        return field
    }

    fun inputField(label: String): EditText {
        val field = EditText(context)
        field.id = View.generateViewId()
        field.layoutParams = paramsForFields
        field.hint = label
        field.onFocusChangeListener = FocusChangeListener()

        return field
    }

    fun button(label: String): Button {
        val button = Button(context)
        button.id = View.generateViewId()
        button.text = label
        button.visibility = View.VISIBLE
        button.layoutParams = buttonMarginParams
        button.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.primary))
        button.setTextColor(context.getColor(R.color.button_text))

        return button
    }

    fun form(): LinearLayout {
        val form = LinearLayout(context)
        form.gravity = Gravity.CENTER
        form.orientation = LinearLayout.VERTICAL
        form.layoutParams = formParams
        form.id = View.generateViewId()

        return form
    }

    fun radioGroup(): RadioGroup {
        val radioGroup = RadioGroup(context)
        radioGroup.layoutParams = marginParams
        radioGroup.id = View.generateViewId()

        return radioGroup
    }

    fun radioSelector(label: String): RadioButton {
        val radioButton = RadioButton(context)
        radioButton.id = View.generateViewId()
        radioButton.text = label

        return radioButton
    }

    fun textField(value: String): TextView {
        val text = TextView(context)
        text.text = value
        text.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        text.layoutParams = marginParams
        text.setTextAppearance(R.style.LabelTextViewStyle)

        return text
    }

    fun errorField(value: String): TextView {
        val text = TextView(context)
        text.text = value
        text.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        text.layoutParams = marginParams
        text.setTextAppearance(R.style.ErrorTextViewStyle)

        return text
    }
}
