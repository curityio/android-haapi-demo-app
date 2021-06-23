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

package com.example.haapidemo.models.haapi

sealed class Field(
    val name: String,
)
{
    class Hidden(
        name: String,
        val value: String
    ) : Field(name)

    class Text(
        name: String,
        val label: Message?,
        val placeholder: String?,
        val value: String?,
        val kind: TextKind?
    ) : Field(name)

    sealed class TextKind(override val discriminator: String) : EnumLike
    {
        object Number : TextKind("number")
        object Email : TextKind("email")
        object Url : TextKind("url")
        object Tel : TextKind("tel")
        object Color : TextKind("color")

        data class UNKNOWN(val kind: String) : TextKind(kind)
    }

    class Username(
        name: String,
        val label: Message?,
        val placeholder: String?,
        val value: String?,
    ) : Field(name)

    class Password(
        name: String,
        val label: Message?,
        val placeholder: String?,
    ) : Field(name)

    class Checkbox(
        name: String,
        val label: Message?,
        val value: String?,
        val checked: Boolean,
        val readonly: Boolean,
    ) : Field(name)

    class Select(
        name: String,
        label: Message,
        options: List<Option>
    ) : Field(name)
    {
        class Option(
            val label: Message,
            val value: String,
            val selected: Boolean
        )
    }

    class Context(
        name: String
    ) : Field(name)

}
