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
package io.curity.haapidemo.utils

import io.curity.haapidemo.models.DecodedJwtData
import java.util.Base64

class JWTUtils {
    companion object {
        fun extractJwt(jwt: String?): DecodedJwtData? {
           return jwt?.let {
                val parts = jwt.split(".")
                if (parts.count() < 2) return DecodedJwtData(
                    error = java.lang.Exception("Invalid JWT input")
                )
                return try {
                    val charset = charset("UTF-8")
                    val header = String(
                        Base64.getUrlDecoder().decode(parts[0].toByteArray(charset)),
                        charset
                    )
                    val payload = String(
                        Base64.getUrlDecoder().decode(parts[1].toByteArray(charset)),
                        charset
                    )
                    return DecodedJwtData(header = header, payload = payload)
                } catch (e: Exception) {
                    return DecodedJwtData(error = java.lang.Exception("Error parsing JWT: $e"))
                }
            }
        }
    }
}