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

import android.content.Context
import io.curity.haapidemo.ui.haapiflow.InteractiveFormItem
import io.curity.haapidemo.uicomponents.MessageStyle
import io.curity.haapidemo.uicomponents.MessageView
import se.curity.haapi.models.android.sdk.models.UserMessage
import se.curity.haapi.models.android.sdk.models.actions.FormField

fun UserMessage.messageStyle(): MessageStyle {
    val lowerCaseClassList = classList.map { it.lowercase() }
    return when {
        lowerCaseClassList.contains("error") -> {
            MessageStyle.Error()
        }
        lowerCaseClassList.contains("warning") -> {
            MessageStyle.Warning()
        }
        else -> {
            MessageStyle.Info()
        }
    }
}

fun List<UserMessage>.toMessageViews(context: Context): List<MessageView> = map { userMessage ->
    MessageView.newInstance(
        context = context,
        text = userMessage.text.literal,
        style = userMessage.messageStyle()
    )
}

fun FormField.Checkbox.toInteractiveFormItemCheckbox(): InteractiveFormItem.Checkbox {
    return InteractiveFormItem.Checkbox(
        key = name,
        label = label?.literal ?: "",
        readonly = readonly,
        checked = checked,
        value = value ?: "on"
    )
}