/*
 *  Copyright (C) 2022 Curity AB
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
package io.curity.haapidemo.models

import org.json.JSONObject

data class DecodedJwtData(val header: String?, val payload: String?) {
    val error: Exception? = null
    constructor(error: Exception?) : this(header = null, payload = null)

    fun getHeaderObj():JSONObject? {
        return header?.let {
            return JSONObject(it)
        }
    }

    fun getPayloadObj(): JSONObject? {
        return payload?.let {
            return JSONObject(it)
        }
    }
}
