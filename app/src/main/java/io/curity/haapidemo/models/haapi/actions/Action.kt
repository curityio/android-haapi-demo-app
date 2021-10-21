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

package io.curity.haapidemo.models.haapi.actions

import android.os.Parcelable
import io.curity.haapidemo.models.haapi.Message
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONObject

sealed class Action: Parcelable
{
    abstract val template: ActionTemplate

    @Parcelize
    data class Form(
        override val template: ActionTemplate,
        val kind: String,
        val title: Message?,
        val model: ActionModel.Form,
        val properties: Properties?,
    ) : Action()
    {

        @Parcelize
        data class Properties(
            val jsonString: String,
            val authenticatorType: String?,
        ) : Parcelable
    }

    @Parcelize
    data class Selector(
        override val template: ActionTemplate,
        val kind: String,
        val title: Message?,
        val model: ActionModel.Selector,
        val properties: Properties?,
    ) : Action()
    {

        @Parcelize
        class Properties(
            val jsonString: String
        ) : Parcelable
    }

    @Parcelize
    data class ClientOperation(
        override val template: ActionTemplate,
        val model: ActionModel.ClientOperation
    ) : Action()

}