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

sealed class Field(
    val name: String,
    val value: String?
)
{
    class Hidden(
        name: String,
        value: String
    ) : Field(name, value)

    class Text(
        name: String,
        value: String?,
        val label: Message?,
        val placeholder: String?,
        val kind: TextKind?
    ) : Field(name, value)

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
        value: String?,
    ) : Field(name, value)

    class Password(
        name: String,
        value: String?,
        val label: Message?,
        val placeholder: String?,
    ) : Field(name, value)

    class Checkbox(
        name: String,
        val label: Message?,
        value: String?,
        val checked: Boolean,
        val readonly: Boolean,
    ) : Field(name, value)

    class Select(
        name: String,
        value: String?,
        val label: Message,
        val options: List<Option>
    ) : Field(name, value)
    {
        class Option(
            val label: Message,
            val value: String,
            val selected: Boolean
        )
    }

    class Context(
        name: String
    ) : Field(name, null)

}
