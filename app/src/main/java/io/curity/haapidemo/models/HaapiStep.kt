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

import android.os.Parcelable
import io.curity.haapidemo.models.haapi.*
import io.curity.haapidemo.models.haapi.actions.Action
import io.curity.haapidemo.models.haapi.actions.ActionModel
import io.curity.haapidemo.models.haapi.problems.HaapiProblem
import kotlinx.android.parcel.Parcelize

/**
 * Sealed class hierarchy to represent the high-level steps described by the [HaapiRepresentation] class.
 *   - These classes only represent a subset of what is representable by [HaapiRepresentation].
 *   - The classes [UnknownStep] and [InvalidStep] are used for low-level [HaapiRepresentation] instances
 *   that don't map to a well-known high-level step. That can happen if the [HaapiRepresentation.type]
 *   is not known or when the [HaapiRepresentation.type] is known but it structure doesn't match what is expected
 *   (e.g. a `redirect-step` with more than one action).
 *
 *  Use [HaapiRepresentation.toHaapiStep] to create a [HaapiStep] from an [HaapiRepresentation]
 */
sealed class HaapiStep

data class Redirect(
    val action: Action.Form,
) : HaapiStep()

@Parcelize
data class AuthenticatorOption(
    val label: Message,
    val type: String?,
    val action: Action.Form,
) : Parcelable

@Parcelize
data class AuthenticatorSelector(
    val title: Message,
    val authenticators: List<AuthenticatorOption>,
) : HaapiStep(), Parcelable

@Parcelize
data class InteractiveForm(
    val actions: List<Action.Form>,
    val type: RepresentationType,
    val cancel: Action.Form?,
    val links: List<Link>,
    val messages: List<UserMessage>
) : HaapiStep(), Parcelable

data class ExternalBrowserClientOperation(
    val actionModel: ActionModel.ClientOperation.ExternalBrowser,
    val cancel: Action.Form?
) : HaapiStep()

data class BankIdClientOperation(
    val actionModel: ActionModel.ClientOperation.BankID,
    val cancel: Action.Form?
) : HaapiStep()

data class EncapClientOperation(
    val actionModel: ActionModel.ClientOperation.EncapAutoActivation,
    val cancel: Action.Form?
) : HaapiStep()

data class UnknownClientOperation(
    val actionModel: ActionModel.ClientOperation.Unknown,
    val cancel: Action.Form?
) : HaapiStep()

@Parcelize
data class PollingStep(
    val type: RepresentationType,
    val properties: Properties.Polling,
    val main: Action.Form,
    val cancel: Action.Form?,
) : HaapiStep(), Parcelable

@Parcelize
data class AuthorizationCompleted(
    val type: RepresentationType,
    val links: List<Link>,
    val responseParameters: Properties.AuthorizationResponse,
) : HaapiStep(), Parcelable

data class ContinueSameStep(
    val representation: HaapiRepresentation
) : HaapiStep()

data class UnknownStep(
    val representation: HaapiRepresentation,
) : HaapiStep()

data class InvalidStep(
    val representation: HaapiRepresentation,
) : HaapiStep()

data class ProblemStep(
    val problem: HaapiProblem
) : HaapiStep()

data class SystemErrorStep(
    val title: String,
    val description: String
) : HaapiStep()

data class TokensStep(
    val oAuthTokenResponse: OAuthTokenResponse
): HaapiStep()

@Parcelize
data class UserConsentStep(
    val messages: List<UserMessage>,
    val type: RepresentationType,
    val actions: List<Action.Form>
): HaapiStep(), Parcelable