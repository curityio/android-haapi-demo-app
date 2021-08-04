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

import android.app.Activity
import android.view.View
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import io.curity.haapidemo.R

private const val SOURCE = "drawable"

@DrawableRes
@Suppress("Unused")
fun Fragment.getImageResources(name: String, fallbackRes: Int = R.drawable.ic_icon_user): Int {
    val iActivity = activity
    return if (iActivity != null) {
        resources.getIdentifier(name, SOURCE, iActivity.packageName)
    } else {
        fallbackRes
    }
}

@DrawableRes
@Suppress("Unused")
fun Activity.getImageResources(name: String, fallbackRes: Int = R.drawable.ic_icon_user): Int {
    val resID = resources.getIdentifier(name, SOURCE, packageName)
    return if (resID != 0) {
        resID
    } else {
        fallbackRes
    }
}

@DrawableRes
@Suppress("Unused")
fun View.getImageResources(name: String, fallbackRes: Int = R.drawable.ic_icon_user): Int {
    val resID = context.resources.getIdentifier(name, SOURCE, context.packageName)
    return if (resID != 0) {
        resID
    } else {
        fallbackRes
    }
}