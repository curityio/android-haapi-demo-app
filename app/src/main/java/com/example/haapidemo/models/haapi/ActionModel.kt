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

package com.example.haapidemo.models.haapi

import org.json.JSONObject

sealed class ActionModel
{
    data class Form(
        val href: String,
        val method: String,
        val type: String?,
        val actionTitle: Message?,
        val fields: List<Field>
    ) : ActionModel()

    data class Selector(
        val options: List<Action>
    ) : ActionModel()

    sealed class ClientOperation
    {
        abstract val name: String
        abstract val continueActions: List<Action>
        abstract val errorActions: List<Action>

        data class ExternalBrowser(
            val arguments: Arguments.ExternalBrowser,
            override val continueActions: List<Action>,
            override val errorActions: List<Action>,
        ) : ClientOperation()
        {
            override val name = "external-browser"
        }

        data class BankID(
            val arguments: Arguments.BankID,
            override val continueActions: List<Action>,
            override val errorActions: List<Action>,
        ) : ClientOperation()
        {
            override val name = "bankid"
        }

        data class EncapAutoActivation(
            val arguments: Arguments.EncapAutoActivation,
            override val continueActions: List<Action>,
            override val errorActions: List<Action>,
        ) : ClientOperation()
        {
            override val name = "encap-auto-activation"
        }

        data class Unknown(
            override val name: String,
            val arguments: JSONObject?,
            override val continueActions: List<Action>,
            override val errorActions: List<Action>,
        ) : ClientOperation()
    }
}
