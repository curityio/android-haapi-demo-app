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
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import io.curity.haapidemo.R

class HeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val textView: TextView

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.header_view, this, true)

        imageView = root.findViewById(R.id.image_view)
        textView = root.findViewById(R.id.text_view)

        loadAttrs(attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun loadAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.HeaderView,
            0,
            defStyleAttr
        ).apply {
            try {

                val textAppearance = getResourceId(R.styleable.HeaderView_textAppearance, R.style.TextAppearance_TitleHeader)
                textView.setTextAppearance(textAppearance)

                val text = getText(R.styleable.HeaderView_android_text)
                textView.text = text

                val imageResource = getDrawable(R.styleable.HeaderView_srcCompat) ?: AppCompatResources.getDrawable(
                    context,
                    R.drawable.ic_logo
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
    fun setImageDrawable(drawable: Drawable?) {
        imageView.setImageDrawable(drawable)
    }

    @Suppress("Unused")
    fun setImageResource(@DrawableRes resId: Int) {
        imageView.setImageResource(resId)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }
}