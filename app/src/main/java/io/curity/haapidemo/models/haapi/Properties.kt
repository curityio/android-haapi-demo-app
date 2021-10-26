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
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.time.Duration

sealed class Properties : Parcelable
{
    abstract val jsonString: String

    @Parcelize
    data class AuthorizationResponse(
        override val jsonString: String,
        val code: String?,
        val state: String?,
        val scope: String?,
        val accessToken: String?,
        val tokenType: String?,
        val expiresIn: Duration?,
        val idToken: String?,
        val sessionState: String?
    ) : Properties()

    @Parcelize
    data class Polling(
        override val jsonString: String,
        val recipientOfCommunication: String?,
        val status: PollingStatus,
    ) : Properties(), Parcelable

    @Parcelize
    data class Unknown(
        override val jsonString: String
    ) : Properties()
}
