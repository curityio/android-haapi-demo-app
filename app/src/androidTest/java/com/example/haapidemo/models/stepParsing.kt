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


/**
 * Maps an [HaapiRepresentation] to one of the high-level [HaapiStep] concrete classes.
 * It will return:
 * - [UnknownStep] when the representation doesn't match any of the high-level steps
 * - [InvalidStep] when the representation seems to match one of the high-level steps but there are
 * invalid components on it.
 */
fun HaapiRepresentation.toHaapiStep(): HaapiStep =
    when (type)
    {
        is RepresentationType.AUTHENTICATION_STEP -> handleAuthenticationStep(this)
        is RepresentationType.REDIRECTION_STEP -> handleRedirectStep(this)
        is RepresentationType.REGISTRATION_STEP -> handleAuthenticationStep(this)
        is RepresentationType.POLLING_STEP -> handlePollingStep(this)
        is RepresentationType.CONTINUE_SAME_STEP -> ContinueSameStep(this)
        is RepresentationType.CONSENTOR_STEP -> UnknownStep(this)
        is RepresentationType.USER_CONSENT_STEP -> UnknownStep(this)
        is RepresentationType.OAUTH_AUTHORIZATION_RESPONSE -> handleAuthorizationStep(this)
        is RepresentationType.UNKNOWN -> UnknownStep(this)
    }

private fun handleAuthenticatorSelector(
    representation: HaapiRepresentation,
    action: Action.Selector
): HaapiStep
{

    if (action.model.options.any {
            it !is Action.Form
                    || requiresInteraction(it)
                    || it.kind != "select-authenticator"
                    || it.properties !is Action.Form.Properties
        })
    {
        return InvalidStep(representation)
    }

    return AuthenticatorSelector(
        title = action.title ?: return InvalidStep(representation),
        authenticators = action.model.options
            .filterIsInstance<Action.Form>()
            .map {
                if (it.title == null)
                {
                    return InvalidStep(representation)
                }
                if (it.properties !is Action.Form.Properties)
                {
                    return InvalidStep(representation)
                }
                AuthenticatorOption(
                    label = it.title,
                    type = it.properties.authenticatorType,
                    action = it
                )
            }
    )

}

private fun handleAuthenticationStep(representation: HaapiRepresentation): HaapiStep
{
    if (representation.actions.size == 1 && representation.actions[0] is Action.Selector)
    {
        val singleAction = representation.actions[0] as Action.Selector
        if (singleAction.kind == "authenticator-selector")
        {
            return handleAuthenticatorSelector(representation, singleAction)
        }
    }
    val clientOperation = representation.actions
        .filterIsInstance<Action.ClientOperation>()
        .singleOrNull()
    val cancel = representation.actions.findForm { it.kind == "cancel" }
    return if (clientOperation != null)
    {
        when(val operation = clientOperation.model) {
            is ActionModel.ClientOperation.ExternalBrowser -> ExternalBrowserClientOperation(
                actionModel = operation,
                cancel = cancel,
            )
            is ActionModel.ClientOperation.BankID -> BankIdClientOperation(
                actionModel = operation,
                cancel = cancel,
            )
            is ActionModel.ClientOperation.EncapAutoActivation -> EncapClientOperation(
                actionModel = operation,
                cancel = cancel,
            )
            is ActionModel.ClientOperation.Unknown -> UnknownClientOperation(
                actionModel = operation,
                cancel = cancel,
            )
        }
    } else
    {
        val action = representation.actions.findForm {
            it.kind != "cancel" && it.kind != "continue"
        } ?: return UnknownStep(representation)
        InteractiveForm(action, cancel)
    }
}

private fun handleRedirectStep(representation: HaapiRepresentation): HaapiStep
{
    if (representation.type != RepresentationType.REDIRECTION_STEP)
    {
        return UnknownStep(representation)
    }
    if (representation.actions.size != 1)
    {
        return InvalidStep(representation)
    }
    val theAction = representation.actions[0]
    if (theAction !is Action.Form)
    {
        return InvalidStep(representation)
    }
    if (requiresInteraction(theAction))
    {
        return InvalidStep(representation)
    }
    return Redirect(theAction)
}

private fun handlePollingStep(representation: HaapiRepresentation): HaapiStep
{
    if (representation.type != RepresentationType.POLLING_STEP)
    {
        return UnknownStep(representation)
    }
    if (representation.properties !is Properties.Polling)
    {
        return InvalidStep(representation)
    }
    return if (representation.properties.status == PollingStatus.PENDING)
    {
        val pollAction =
            representation.actions.findForm { it.kind == "poll" } ?: return InvalidStep(
                representation
            )

        val cancelAction = representation.actions.findForm { it.kind == "cancel" }
        PollingStep(
            representation.properties,
            pollAction,
            cancelAction
        )
    } else
    {
        val continueAction =
            representation.actions.findForm { it.kind == "continue" } ?: return InvalidStep(
                representation
            )
        PollingStep(
            representation.properties,
            continueAction,
            null
        )
    }
}

private fun handleAuthorizationStep(representation: HaapiRepresentation): HaapiStep
{
    if (representation.type != RepresentationType.OAUTH_AUTHORIZATION_RESPONSE)
    {
        return UnknownStep(representation)
    }
    if (representation.properties == null || representation.properties !is Properties.AuthorizationResponse)
    {
        return InvalidStep(representation)
    }
    return AuthorizationCompleted(
        representation.properties
    )
}

private fun requiresInteraction(action: Action.Form) =
    action.model.fields.any { field -> field !is Field.Hidden }

private fun List<Action>.findForm(predicate: (Action.Form) -> Boolean): Action.Form? =
    this.filterIsInstance<Action.Form>()
        .find { predicate(it) }