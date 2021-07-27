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

package io.curity.haapidemo.utils

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import androidx.annotation.ColorInt

/**
 * Sets stroke by using [colorInt] with a [width]
 *
 * @param colorInt An [Int] value that represents a color
 * @param width An [Int] value that represents the expected width
 */
fun Drawable.setStroke(@ColorInt colorInt: Int, width: Int) {
    when (this) {
        is GradientDrawable -> {
            mutate()
            setStroke(width, colorInt)
        }
    }
}

/**
 * Sets the background color by using [colorInt]
 *
 * @param colorInt An [Int] value that represents a color
 */
fun Drawable?.setBackgroundColor(@ColorInt colorInt: Int) {
    when (this) {
        is ShapeDrawable -> paint.color = colorInt
        is GradientDrawable -> {
            mutate()
            setColor(colorInt)
        }
        is ColorDrawable -> color = colorInt
    }
}