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

import java.time.Duration

/*
 * Model classes for the `application/vnd.auth+json` representations.
 * "problem+json"
 * They provide a type-safe view for the JSON representation of authentication steps.
 *
 * TODO the goal is to make these classes public in a SDK.
 */

/**
 * Representation of non-error HAAPI responses
 */
data class HaapiRepresentation(
    val type: StepType,
    val properties: Properties?,
    val actions: List<Action>,
    val links: List<Link>,
    val metadata: Map<String, String>,
    val messages: List<UserMessage>,
)

data class UserMessage(
    val text: String,
    val classList: List<String>
)

/**
 * Base interface for enum-like sealed hierarchies
 */
interface DiscriminatedUnion
{
    val discriminator: String
}

sealed class StepType(override val discriminator: String) : DiscriminatedUnion
{
    object AUTHENTICATION_STEP : StepType("authentication-step")
    object REDIRECTION_STEP : StepType("redirection-step")
    object REGISTRATION_STEP : StepType("registration-step")
    object POLLING_STEP : StepType("polling-step")
    object CONTINUE_SAME_STEP : StepType("continue-same-step")
    object CONSENTOR_STEP : StepType("consentor-step")
    object USER_CONSENT_STEP : StepType("user-consent-step")
    object OAUTH_AUTHORIZATION_RESPONSE : StepType("oauth-authorization-response")

    data class UNKNOWN(val value: String) : StepType(value)
}

sealed class Properties
{
    data class AuthorizationResponse(
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
        val recipientOfCommunication: String?,
        val status: PollingStatus,
    ) : Properties()
}

sealed class PollingStatus(override val discriminator: String) : DiscriminatedUnion
{
    object PENDING: PollingStatus("pending")
    object DONE: PollingStatus("done")
    object FAILED: PollingStatus("failed")

    data class UNKNOWN(val value: String): PollingStatus(value)
}

data class Link(
    val href: String,
    val rel: String,
    val title: String?,
    val type: String?
)

sealed class Action
{
    abstract val template: String

    data class Form(
        override val template: String,
        val kind: String,
        val title: String?,
        val model: ActionModel.Form,
        val properties: Properties?,
    ) : Action()
    {

        data class Properties(
            val authenticatorType: String?,
        )
    }

    data class Selector(
        override val template: String,
        val kind: String,
        val title: String?,
        val model: ActionModel.Selector
    ) : Action() {

        class Properties
    }

    data class ClientOperation(
        override val template: String,
        val model: ActionModel.ClientOperation
    ) : Action()

}

sealed class ActionModel
{
    data class Form(
        val href: String,
        val method: String,
        val type: String?,
        val actionTitle: String?,
        val fields: List<Field>
    ) : ActionModel()

    data class Selector(
        val options: List<Action>
    ) : ActionModel()

    data class ClientOperation(
        val name: String,
        val arguments: Arguments,
        val continueActions: List<Action>,
        val errorActions: List<Action>,
    ) : ActionModel()
    {
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

            class Encap(
                val href: String,
                val activationCode: String,
            ) : Arguments()
        }
    }
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
        val label: String?,
        val placeholder: String?,
        val value: String?,
        val kind: TextKind?
    ) : Field(name)

    sealed class TextKind(override val discriminator: String) : DiscriminatedUnion
    {
        object NUMBER: TextKind("number")
        object EMAIL: TextKind("email")
        object URL: TextKind("url")
        object TEL: TextKind("tel")
        object COLOR: TextKind("color")

        data class UNKNOWN(val kind: String) : TextKind(kind)
    }

    class Username(
        name: String,
        val label: String?,
        val placeholder: String?,
        val value: String?,
    ) : Field(name)

    class Password(
        name: String,
        val label: String?,
        val placeholder: String?,
    ) : Field(name)

    class Checkbox(
        name: String,
        val label: String?,
        val value: String?,
        val checked: Boolean,
        val readonly: Boolean,
    ) : Field(name)

    class Select(
        name: String,
        label: String,
        options: List<Option>
    ) : Field(name)
    {
        class Option(
            val label: String,
            val value: String,
            val selected: Boolean
        )
    }

    class Context(
        name: String
    ) : Field(name)

}



