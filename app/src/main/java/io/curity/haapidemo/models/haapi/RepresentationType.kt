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

sealed class RepresentationType(override val discriminator: String) : EnumLike
{
    object AuthenticationStep : RepresentationType("authentication-step")
    object RedirectionStep : RepresentationType("redirection-step")
    object RegistrationStep : RepresentationType("registration-step")
    object PollingStep : RepresentationType("polling-step")
    object ContinueSameStep : RepresentationType("continue-same-step")
    object ConsentorStep : RepresentationType("consentor-step")
    object UserConsentStep : RepresentationType("user-consent-step")
    object OauthAuthorizationResponse : RepresentationType("oauth-authorization-response")

    // Problems
    object IncorrectCredentialsProblem : RepresentationType("https://curity.se/problems/incorrect-credentials")
    object InvalidInputProblem : RepresentationType("https://curity.se/problems/invalid-input")
    object UnexpectedProblem : RepresentationType("https://curity.se/problems/unexpected")
    object AuthorizationResponseProblem : RepresentationType("https://curity.se/problems/error-authorization-response")

    data class Unknown(val value: String) : RepresentationType(value)
}
