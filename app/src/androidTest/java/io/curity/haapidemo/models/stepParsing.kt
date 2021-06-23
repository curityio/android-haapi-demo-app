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

package io.curity.haapidemo.models

import io.curity.haapidemo.models.haapi.*

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
        is RepresentationType.AuthenticationStep -> handleAuthenticationStep(this)
        is RepresentationType.RedirectionStep -> handleRedirectStep(this)
        is RepresentationType.RegistrationStep -> handleAuthenticationStep(this)
        is RepresentationType.PollingStep -> handlePollingStep(this)
        is RepresentationType.ContinueSameStep -> ContinueSameStep(this)
        is RepresentationType.ConsentorStep -> UnknownStep(this)
        is RepresentationType.UserConsentStep -> UnknownStep(this)
        is RepresentationType.OauthAuthorizationResponse -> handleAuthorizationStep(this)
        is RepresentationType.Unknown -> UnknownStep(this)
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
                val actionFormTitle = it.title
                if (actionFormTitle == null)
                {
                    return InvalidStep(representation)
                }
                if (it.properties !is Action.Form.Properties)
                {
                    return InvalidStep(representation)
                }
                AuthenticatorOption(
                    label = actionFormTitle,
                    type = it.properties?.authenticatorType,
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
    if (representation.type != RepresentationType.RedirectionStep)
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
    if (representation.type != RepresentationType.PollingStep)
    {
        return UnknownStep(representation)
    }
    val properties = representation.properties?.let { it }
    if (properties !is Properties.Polling)
    {
        return InvalidStep(representation)
    }

    val polling = representation.properties as? Properties.Polling
    return if (polling?.status == PollingStatus.Pending)
    {
        val pollAction =
            representation.actions.findForm { it.kind == "poll" } ?: return InvalidStep(
                representation
            )

        val cancelAction = representation.actions.findForm { it.kind == "cancel" }
        PollingStep(
            properties,
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
            properties,
            continueAction,
            null
        )
    }
}

private fun handleAuthorizationStep(representation: HaapiRepresentation): HaapiStep
{
    if (representation.type != RepresentationType.OauthAuthorizationResponse)
    {
        return UnknownStep(representation)
    }
    val properties = representation.properties
    if (properties == null || properties !is Properties.AuthorizationResponse)
    {
        return InvalidStep(representation)
    }
    return AuthorizationCompleted(
        properties
    )
}

private fun requiresInteraction(action: Action.Form) =
    action.model.fields.any { field -> field !is Field.Hidden }

private fun List<Action>.findForm(predicate: (Action.Form) -> Boolean): Action.Form? =
    this.filterIsInstance<Action.Form>()
        .find { predicate(it) }