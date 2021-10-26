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

sealed class RepresentationType(override val discriminator: String) : EnumLike, Parcelable
{
    @Parcelize
    object AuthenticationStep : RepresentationType("authentication-step")
    @Parcelize
    object RedirectionStep : RepresentationType("redirection-step")
    @Parcelize
    object RegistrationStep : RepresentationType("registration-step")
    @Parcelize
    object PollingStep : RepresentationType("polling-step")
    @Parcelize
    object ContinueSameStep : RepresentationType("continue-same-step")
    @Parcelize
    object ConsentorStep : RepresentationType("consentor-step")
    @Parcelize
    object UserConsentStep : RepresentationType("user-consent-step")
    @Parcelize
    object OauthAuthorizationResponse : RepresentationType("oauth-authorization-response")

    // Problems
    @Parcelize
    object IncorrectCredentialsProblem : RepresentationType("https://curity.se/problems/incorrect-credentials")
    @Parcelize
    object InvalidInputProblem : RepresentationType("https://curity.se/problems/invalid-input")
    @Parcelize
    object UnexpectedProblem : RepresentationType("https://curity.se/problems/unexpected")
    @Parcelize
    object AuthorizationResponseProblem : RepresentationType("https://curity.se/problems/error-authorization-response")

    @Parcelize
    data class Unknown(val value: String) : RepresentationType(value)
}
