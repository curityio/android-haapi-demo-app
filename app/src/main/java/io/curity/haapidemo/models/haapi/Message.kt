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

import kotlin.reflect.KProperty1

sealed class Message
{
    abstract val key: String?
    abstract val message: String?

    class OfKey(
        override val key: String
    ) : Message()
    {
        override val message: String? = null
    }

    class OfLiteral(
        override val message: String
    ) : Message()
    {
        override val key: String? = null
    }

    class OfLiteralAndKey(
        override val message: String,
        override val key: String,
    ) : Message()

    fun value(kproperty: KProperty1<Message, String?> = Message::message): String {
        return if (key != null && message != null) {
            kproperty.get(this) as String
        } else if (key != null) {
            key  as String
        } else if (message != null) {
            message  as String
        } else {
            throw IllegalStateException("A Message cannot have no value for `key` or `message`")
        }
    }
}