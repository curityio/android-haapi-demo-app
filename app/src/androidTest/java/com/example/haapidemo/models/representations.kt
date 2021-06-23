/*
 * Copyright (C) 2021 Curity AB. All rights reserved.
 *
 * The contents of this file are the property of Curity AB.
 * You may not copy or use this file, in either source code
 * or executable form, except in compliance with terms
 * set by Curity AB.
 *
 * For further information, please contact Curity AB.
 */

package com.example.haapidemo.models

import org.json.JSONObject
import java.time.Duration

/*
 * Model classes for the `application/vnd.auth+json` representations.
 * They provide a type-safe view for the JSON representation of authentication steps.
 *
 * TODO the goal is to make these classes public in a SDK.
 */

/**
 * Representation of non-error HAAPI responses
 */
data class HaapiRepresentation(
    val type: RepresentationType,
    val properties: Properties?,
    val actions: List<Action>,
    val links: List<Link>,
    val metadata: Map<String, String>,
    val messages: List<UserMessage>,
)

data class UserMessage(
    val text: Message,
    val classList: List<String>
)

/**
 * Base interface for enum-like sealed hierarchies
 */
interface EnumLike
{
    val discriminator: String
}

sealed class RepresentationType(override val discriminator: String) : EnumLike
{
    object AUTHENTICATION_STEP : RepresentationType("authentication-step")
    object REDIRECTION_STEP : RepresentationType("redirection-step")
    object REGISTRATION_STEP : RepresentationType("registration-step")
    object POLLING_STEP : RepresentationType("polling-step")
    object CONTINUE_SAME_STEP : RepresentationType("continue-same-step")
    object CONSENTOR_STEP : RepresentationType("consentor-step")
    object USER_CONSENT_STEP : RepresentationType("user-consent-step")
    object OAUTH_AUTHORIZATION_RESPONSE : RepresentationType("oauth-authorization-response")

    data class UNKNOWN(val value: String) : RepresentationType(value)
}

sealed class Properties
{
    abstract val json: JSONObject

    data class AuthorizationResponse(
        override val json: JSONObject,
        val code: String?,
        val state: String?,
        val scope: String?,
        val accessToken: String?,
        val tokenType: String?,
        val expiresIn: Duration?,
        val idToken: String?,
        val sessionState: String?,
    ) : Properties()

    data class Polling(
        override val json: JSONObject,
        val recipientOfCommunication: String?,
        val status: PollingStatus,
    ) : Properties()

    data class Unknown(
        override val json: JSONObject,
    ) : Properties()
}

sealed class PollingStatus(override val discriminator: String) : EnumLike
{
    object PENDING : PollingStatus("pending")
    object DONE : PollingStatus("done")
    object FAILED : PollingStatus("failed")

    data class UNKNOWN(val value: String) : PollingStatus(value)
}

data class Link(
    val href: String,
    val rel: String,
    val title: Message?,
    val type: String?
)

sealed class ActionTemplate(override val discriminator: String) : EnumLike
{
    object FORM : ActionTemplate("form")
    object SELECTOR : ActionTemplate("selector")
    object CLIENT_OPERATION : ActionTemplate("client-operation")

    data class UNKNOWN(val value: String) : ActionTemplate(value)
}

sealed class Action
{
    abstract val template: ActionTemplate

    data class Form(
        override val template: ActionTemplate,
        val kind: String,
        val title: Message?,
        val model: ActionModel.Form,
        val properties: Properties?,
    ) : Action()
    {

        data class Properties(
            val json: JSONObject,
            val authenticatorType: String?,
        )
    }

    data class Selector(
        override val template: ActionTemplate,
        val kind: String,
        val title: Message?,
        val model: ActionModel.Selector,
        val properties: Selector.Properties?,
    ) : Action()
    {

        class Properties(
            val json: JSONObject,
        )
    }

    data class ClientOperation(
        override val template: ActionTemplate,
        val model: ActionModel.ClientOperation
    ) : Action()

}

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

sealed class Arguments
{
    class ExternalBrowser(
        val href: String
    ) : Arguments()

    class BankID(
        val href: String,
        val autoStartToken: String,
        val redirect: String,
    ) : Arguments()

    class EncapAutoActivation(
        val href: String,
        val activationCode: String,
    ) : Arguments()
}

sealed class Field(
    val name: String,
)
{
    class Hidden(
        name: String,
        val value: String
    ) : Field(name)

    class Text(
        name: String,
        val label: Message?,
        val placeholder: String?,
        val value: String?,
        val kind: TextKind?
    ) : Field(name)

    sealed class TextKind(override val discriminator: String) : EnumLike
    {
        object NUMBER : TextKind("number")
        object EMAIL : TextKind("email")
        object URL : TextKind("url")
        object TEL : TextKind("tel")
        object COLOR : TextKind("color")

        data class UNKNOWN(val kind: String) : TextKind(kind)
    }

    class Username(
        name: String,
        val label: Message?,
        val placeholder: String?,
        val value: String?,
    ) : Field(name)

    class Password(
        name: String,
        val label: Message?,
        val placeholder: String?,
    ) : Field(name)

    class Checkbox(
        name: String,
        val label: Message?,
        val value: String?,
        val checked: Boolean,
        val readonly: Boolean,
    ) : Field(name)

    class Select(
        name: String,
        label: Message,
        options: List<Option>
    ) : Field(name)
    {
        class Option(
            val label: Message,
            val value: String,
            val selected: Boolean
        )
    }

    class Context(
        name: String
    ) : Field(name)

}

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




