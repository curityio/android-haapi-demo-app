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
}