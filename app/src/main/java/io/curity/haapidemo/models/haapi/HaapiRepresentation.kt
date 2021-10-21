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
import io.curity.haapidemo.models.haapi.actions.Action
import kotlinx.android.parcel.Parcelize

/**
 * Representation of non-error HAAPI responses
 */
@Parcelize
data class HaapiRepresentation(val type: RepresentationType,
                               val properties: Properties?,
                               val actions: List<Action>,
                               val links: List<Link>,
                               val metadata: Map<String, String>,
                               val messages: List<UserMessage>) : Parcelable
