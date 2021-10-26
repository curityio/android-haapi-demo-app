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

package io.curity.haapidemo.models.haapi

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class Field : Parcelable
{
    abstract val name: String
    abstract val value: String?

    @Parcelize
    data class Hidden(
        override val name: String,
        override val value: String
    ) : Field()

    @Parcelize
    class Text(
        override val name: String,
        override val value: String?,
        val label: Message?,
        val placeholder: String?,
        val kind: TextKind?
    ) : Field()

    sealed class TextKind(override val discriminator: String) : EnumLike, Parcelable
    {
        @Parcelize
        object Number : TextKind("number")
        @Parcelize
        object Email : TextKind("email")
        @Parcelize
        object Url : TextKind("url")
        @Parcelize
        object Tel : TextKind("tel")
        @Parcelize
        object Color : TextKind("color")

        @Parcelize
        data class UNKNOWN(val kind: String) : TextKind(kind)
    }

    @Parcelize
    class Username(
        override val name: String,
        val label: Message?,
        val placeholder: String?,
        override val value: String?,
    ) : Field()

    @Parcelize
    class Password(
        override val name: String,
        override val value: String?,
        val label: Message?,
        val placeholder: String?,
    ) : Field()

    @Parcelize
    class Checkbox(
        override val name: String,
        val label: Message?,
        override val value: String?,
        val checked: Boolean,
        val readonly: Boolean,
    ) : Field()

    @Parcelize
    class Select(
        override val name: String,
        override val value: String?,
        val label: Message,
        val options: List<Option>
    ) : Field()
    {
        @Parcelize
        class Option(
            val label: Message,
            val value: String,
            val selected: Boolean
        ): Parcelable
    }

    @Parcelize
    class Context(
        override val name: String
    ) : Field() {
        override val value: String? = null
    }
}
