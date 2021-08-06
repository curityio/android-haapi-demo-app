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

package io.curity.haapidemo.models

import java.io.Serializable
import java.time.Duration
import kotlin.collections.HashMap

/**
 * OAuthTokenResponse represents a successful Access Token response
 */
data class OAuthTokenResponse(
    val accessToken: String,
    val tokenType: String?,
    val scope: String?,
    val expiresIn: Duration?,
    val refreshToken: String?,
    val idToken: String?,
    val properties: HashMap<String, String>
) : Serializable
