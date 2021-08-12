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
package io.curity.haapidemo.models.haapi.extensions

import android.content.Context
import io.curity.haapidemo.models.haapi.Field
import io.curity.haapidemo.models.haapi.UserMessage
import io.curity.haapidemo.ui.haapiflow.InteractiveFormItem
import io.curity.haapidemo.uicomponents.MessageStyle
import io.curity.haapidemo.uicomponents.MessageView
import java.util.*

fun UserMessage.messageStyle(): MessageStyle {
    val lowerCaseClassList = classList.map { it.toLowerCase(Locale.getDefault()) }
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
        text = userMessage.text.message ?: "",
        style = userMessage.messageStyle()
    )
}

fun Field.Checkbox.toInteractiveFormItemCheckbox(): InteractiveFormItem.Checkbox {
    return InteractiveFormItem.Checkbox(
        key = name,
        label = label?.message ?: "",
        readonly = readonly,
        checked = checked,
        value = value ?: "on"
    )
}