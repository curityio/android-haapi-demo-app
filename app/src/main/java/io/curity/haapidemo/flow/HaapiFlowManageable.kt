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

package io.curity.haapidemo.flow

import androidx.lifecycle.LiveData
import io.curity.haapidemo.models.HaapiStep
import io.curity.haapidemo.models.haapi.Link
import io.curity.haapidemo.models.haapi.actions.ActionModel

interface HaapiFlowManageable: HaapiFlowable, HaapiSubmitable

interface HaapiFlowable {

    /**
     * Returns a LiveData of an optional HaapiStep. The initial value of HaapiStep is `null`.
     */
    val liveStep: LiveData<HaapiStep?>

    /**
     * Starts the Haapi Flow and returns a new HaapiStep
     * @return HaapiStep
     */
    suspend fun start(): HaapiStep

    /**
     * Fetches the Access Token (OAuthToken) with the authorization code and returns a new HaapiStep
     *
     * @param authorizationCode A String that represents an authorization code
     * @return A new HaapiStep - If it succeeds, then it returns an AccessTokenStep.
     */
    suspend fun fetchAccessToken(authorizationCode: String): HaapiStep

    /**
     * Refresh the Access Token (OAuthToken) with a refresh token and returns a new HaapiStep
     *
     * @param refreshToken A String that represents a refresh token
     * @return A new HaapiStep - If the refreshToken is valid, then a TokensStep is expected. Otherwise, a SystemError
     * would be returned
     */
    suspend fun refreshAccessToken(refreshToken: String): HaapiStep

    /**
     * Resets the Haapi Flow
     */
    fun reset()

}

interface HaapiSubmitable {

    /**
     * Submits an ActionModel.Form from HaapiRepresentation with some parameters to override and returns a new HaapiStep
     *
     * @param form An ActionModel.Form
     * @param parameters A Map<String, String> that will be sent and overrides any fields in the form
     * @return A new HaapiStep
     */
    suspend fun submitForm(form: ActionModel.Form, parameters: Map<String, String>): HaapiStep

    /**
     * Follows the link from HaapiRepresentation and returns a new HaapiStep
     *
     * @param link A Link
     * @return A new HaapiStep
     */
    suspend fun followLink(link: Link): HaapiStep

}

