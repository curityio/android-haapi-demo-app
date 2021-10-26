/*
 *  Copyright 2021 Curity AB
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

package io.curity.haapidemo.parsers

import io.curity.haapidemo.models.OAuthTokenResponse
import org.json.JSONException
import org.json.JSONObject
import java.time.Duration
import io.curity.haapidemo.models.haapi.*
import io.curity.haapidemo.models.haapi.actions.Action
import io.curity.haapidemo.models.haapi.actions.ActionModel
import io.curity.haapidemo.models.haapi.actions.ActionTemplate
import io.curity.haapidemo.models.haapi.actions.Arguments
import io.curity.haapidemo.models.haapi.problems.AuthorizationProblem
import io.curity.haapidemo.models.haapi.problems.InvalidField
import io.curity.haapidemo.models.haapi.problems.InvalidInputProblem
import io.curity.haapidemo.models.haapi.problems.Problem

class ModelException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

object RepresentationParser
{
    fun parse(obj: JSONObject) = parsing("Representation") {
        val type = parseType(obj.string("type"))
        HaapiRepresentation(
            type = type,
            properties = parseProperties(type, obj),
            actions = obj.list("actions", ActionParser::parse),
            links = obj.list("links", LinkParser::parse),
            metadata = obj.mapOfStrings("metadata"),
            messages = obj.list("messages", RepresentationParser::parseUserMessage)
        )
    }

    fun parseAccessToken(obj: JSONObject) = parsing("AccessToken") {
        OAuthTokenResponse(
            accessToken = obj.string("access_token"),
            tokenType = obj.stringOpt("token_type"),
            scope = obj.stringOpt("scope"),
            expiresIn = obj.longFromNumberOrStringOpt("expires_in")?.let { Duration.ofSeconds(it) },
            refreshToken = obj.stringOpt("refresh_token"),
            idToken = obj.stringOpt("id_token"),
            properties = obj
        )
    }

    fun parseProblem(obj: JSONObject) = parsing("Problem") {
        val type = parseType(obj.string("type"))
        when (type) {
            RepresentationType.InvalidInputProblem ->
                InvalidInputProblem(
                    title = obj.string("title"),
                    code = obj.stringOpt("code"),
                    messages = obj.list("messages", RepresentationParser::parseUserMessage),
                    links = obj.list("links", LinkParser::parse),
                    invalidFields = obj.list("invalidFields", InvalidFieldParser::parse),
                    errorDescription = obj.stringOpt("error_description")
                )
            RepresentationType.IncorrectCredentialsProblem, RepresentationType.UnexpectedProblem ->
                Problem(
                    title = obj.string("title"),
                    code = obj.stringOpt("code"),
                    messages = obj.list("messages", RepresentationParser::parseUserMessage),
                    links = obj.list("links", LinkParser::parse)
                )
            RepresentationType.AuthorizationResponseProblem ->
                AuthorizationProblem(
                    title = obj.stringOpt("title") ?: obj.string("error"),
                    code = obj.stringOpt("code"),
                    messages = obj.list("messages", RepresentationParser::parseUserMessage),
                    links = obj.list("links", LinkParser::parse),
                    error = obj.string("error"),
                    errorDescription =  obj.string("error_description")
                )
            else -> throw ModelException("Invalid Problem for '$type'")
        }
    }

    private val parseType = createParser<RepresentationType> { RepresentationType.Unknown(it) }

    private fun parseProperties(type: RepresentationType, obj: JSONObject): Properties? =
        when (type)
        {
            RepresentationType.PollingStep -> PropertiesParser.parsePolling(
                (obj.getJSONObject(
                    "properties"
                ))
            )
            RepresentationType.OauthAuthorizationResponse -> PropertiesParser.parseAuthorizationResponse(
                (obj.getJSONObject(
                    "properties"
                ))
            )
            else -> obj.optJSONObject("properties")?.let { Properties.Unknown(it.toString()) }
        }

    private fun parseUserMessage(obj: JSONObject) =
        UserMessage(
            text = obj.message("text"),
            classList = obj.listStrings("classList")
        )
}

private object PropertiesParser
{
    fun parseAuthorizationResponse(obj: JSONObject) = parsing("oauth-authorization-response") {
        Properties.AuthorizationResponse(
            jsonString = obj.toString(),
            code = obj.stringOpt("code"),
            state = obj.stringOpt("state"),
            scope = obj.stringOpt("scope"),
            accessToken = obj.stringOpt("access_token"),
            tokenType = obj.stringOpt("token_type"),
            idToken = obj.stringOpt("id_token"),
            sessionState = obj.stringOpt("session_state"),
            expiresIn = obj.longFromNumberOrStringOpt("expires_in")?.let { Duration.ofSeconds(it) }
        )
    }

    private val parseStatus = createParser<PollingStatus> { PollingStatus.Unknown(it) }

    fun parsePolling(obj: JSONObject) = parsing("polling") {
        Properties.Polling(
            jsonString = obj.toString(),
            recipientOfCommunication = obj.stringOpt("recipientOfCommunication"),
            status = parseStatus(obj.string("status"))
        )
    }

}

private object LinkParser
{
    fun parse(obj: JSONObject) = parsing("Link") {
        Link(
            href = obj.string("href"),
            rel = obj.string("rel"),
            title = obj.messageOpt("title"),
            type = obj.stringOpt("type")
        )
    }
}

private object ActionParser
{
    fun parse(obj: JSONObject): Action = parsing("Action")
    {
        when (val template = obj.string("template"))
        {
            "form" -> parseForm(obj)
            "selector" -> parseSelector(obj)
            "client-operation" -> parseClientOperation(obj)
            else -> throw JSONException("Unsupported action type '$template'")
        }
    }

    fun parseForm(obj: JSONObject) =
        Action.Form(
            template = ActionTemplate.Form,
            kind = obj.string("kind"),
            title = obj.messageOpt("title"),
            model = ActionFormModelParser.parse(obj.getJSONObject("model")),
            properties = obj.optJSONObject("properties")?.run {
                Action.Form.Properties(
                    jsonString = this.toString(),
                    authenticatorType = this.optString("authenticatorType")
                )
            }
        )

    fun parseSelector(obj: JSONObject) =
        Action.Selector(
            template = ActionTemplate.Selector,
            kind = obj.string("kind"),
            title = obj.messageOpt("title"),
            model = ActionSelectorModelParser.parse(obj.getJSONObject("model")),
            properties = obj.optJSONObject("properties")?.run {
                Action.Selector.Properties(
                    jsonString = this.toString(),
                )
            }
        )

    fun parseClientOperation(obj: JSONObject) =
        Action.ClientOperation(
            template = ActionTemplate.ClientOperation,
            model = ActionClientOperationModelParser.parse(obj.getJSONObject("model"))
        )
}

private object ActionFormModelParser
{
    fun parse(obj: JSONObject) = parsing("Form") {
        ActionModel.Form(
            href = obj.string("href"),
            method = obj.string("method"),
            type = obj.stringOpt("type"),
            actionTitle = obj.messageOpt("actionTitle"),
            fields = obj.list("fields", FieldParser::parse)
        )
    }
}

private object ActionSelectorModelParser
{
    fun parse(obj: JSONObject) = parsing("Selector") {
        ActionModel.Selector(
            options = obj.list("options", ActionParser::parse)
        )
    }
}

private object ActionClientOperationModelParser
{
    fun parse(obj: JSONObject) = parsing("client-operation") {
        val name = obj.string("name")
        val argsObj = obj.getJSONObject("arguments")
        when (name)
        {
            "external-browser-flow" -> ActionModel.ClientOperation.ExternalBrowser(
                continueActions = obj.list("continueActions", ActionParser::parse),
                errorActions = obj.list("errorActions", ActionParser::parse),
                arguments = Arguments.ExternalBrowser(
                    href = argsObj.string("href"),
                )
            )
            "bankid" -> ActionModel.ClientOperation.BankID(
                continueActions = obj.list("continueActions", ActionParser::parse),
                errorActions = obj.list("errorActions", ActionParser::parse),
                arguments = Arguments.BankID(
                    href = argsObj.string("href"),
                    autoStartToken = argsObj.string("autoStartToken"),
                    redirect = argsObj.string("redirect"),
                )
            )
            "encap-auto-activation" -> ActionModel.ClientOperation.EncapAutoActivation(
                continueActions = obj.list("continueActions", ActionParser::parse),
                errorActions = obj.list("errorActions", ActionParser::parse),
                arguments = Arguments.EncapAutoActivation(
                    href = argsObj.string("href"),
                    activationCode = argsObj.string("activationCode"),
                )
            )
            else -> ActionModel.ClientOperation.Unknown(
                name = name,
                continueActions = obj.list("continueActions", ActionParser::parse),
                errorActions = obj.list("errorActions", ActionParser::parse),
                arguments = obj.optJSONObject("arguments"),
            )
        }
    }
}

private object FieldParser
{
    fun parse(obj: JSONObject): Field = when (val type = obj.string("type"))
    {
        "hidden" -> parseHidden(obj)
        "text" -> parseText(obj)
        "username" -> parseUsername(obj)
        "password" -> parsePassword(obj)
        "checkbox" -> parseCheckbox(obj)
        "select" -> parseSelect(obj)
        "context" -> parseContext(obj)
        else -> throw ModelException("unknown field type '$type'")
    }

    private val kindParser = createParser<Field.TextKind> { Field.TextKind.UNKNOWN(it) }

    private fun parseText(obj: JSONObject) =
        Field.Text(
            name = obj.string("name"),
            label = obj.messageOpt("label"),
            placeholder = obj.stringOpt("placeholder"),
            value = obj.stringOpt("value"),
            kind = obj.stringOpt("kind")?.let {
                kindParser(it)
            }
        )

    private fun parseHidden(obj: JSONObject) =
        Field.Hidden(
            name = obj.string("name"),
            value = obj.string("value"),
        )

    private fun parseUsername(obj: JSONObject) =
        Field.Username(
            name = obj.string("name"),
            label = obj.message("label"),
            placeholder = obj.stringOpt("placeholder"),
            value = obj.stringOpt("value"),
        )

    private fun parsePassword(obj: JSONObject) =
        Field.Password(
            name = obj.string("name"),
            label = obj.messageOpt("label"),
            placeholder = obj.stringOpt("placeholder"),
            value = obj.stringOpt("value")
        )

    private fun parseCheckbox(obj: JSONObject) =
        Field.Checkbox(
            name = obj.string("name"),
            label = obj.messageOpt("label"),
            value = obj.stringOpt("value"),
            checked = obj.boolean("checked"),
            readonly = obj.boolean("readonly")
        )

    private fun parseSelect(obj: JSONObject) =
        Field.Select(
            name = obj.string("name"),
            label = obj.message("label"),
            options = obj.list("options") {
                Field.Select.Option(
                    label = it.message("label"),
                    value = it.string("value"),
                    selected = it.boolean("selected"),
                )
            },
            value = obj.stringOpt("value")
        )

    private fun parseContext(obj: JSONObject) =
        Field.Context(
            name = obj.string("name")
        )
}

private object InvalidFieldParser
{
    fun parse(obj: JSONObject) = parsing("InvalidField") {
        InvalidField(
            name = obj.string("name"),
            reason = obj.stringOpt("reason"),
            detail = obj.stringOpt("detail")
        )
    }
}

private fun <T> parsing(context: String, block: () -> T) =
    try
    {
        block()
    } catch (ex: Throwable)
    {
        throw ModelException("Error while parsing '$context'", ex)
    }

private fun JSONObject.string(name: String): String = this.getString(name)
private fun JSONObject.stringOpt(name: String): String? = this.optString(name).run {
    if (this.isBlank()) null else this
}

private fun JSONObject.messageOpt(name: String): Message?
{
    val maybeLiteral = this.stringOpt(name)
    val maybeKey = this.stringOpt(keyField(name))

    return if (maybeLiteral != null)
    {
        if (maybeKey != null)
        {
            Message.OfLiteralAndKey(maybeLiteral, maybeKey)
        } else
        {
            Message.OfLiteral(maybeLiteral)
        }
    } else
    {
        if (maybeKey != null)
        {
            Message.OfKey(maybeKey)
        } else
        {
            null
        }
    }
}

private fun JSONObject.message(name: String): Message
{
    return messageOpt(name) ?: messageOpt("name") ?: Message.OfLiteral("Missing label")
}


private fun keyField(name: String) = name + "Key"

private fun JSONObject.boolean(name: String): Boolean = this.optBoolean(name, false)

private fun <T> JSONObject.obj(name: String, parser: (JSONObject) -> T): T =
    this.getJSONObject(name).run {
        parser(this)
    }

private fun <T> JSONObject.list(name: String, parser: (JSONObject) -> T): List<T> =
    if (this.has(name))
    {
        this.getJSONArray(name).run {
            sequence {
                for (index in 0 until length())
                {
                    yield(parser(getJSONObject(index)))
                }
            }.toList()
        }
    } else
    {
        listOf()
    }

private fun JSONObject.listStrings(name: String): List<String> =
    if (this.has(name))
    {
        this.getJSONArray(name).run {
            sequence {
                for (index in 0 until length())
                {
                    yield(getString(index))
                }
            }.toList()
        }
    } else
    {
        listOf()
    }

private fun JSONObject.mapOfStrings(name: String): Map<String, String> =
    if (this.has(name))
    {
        this.getJSONObject(name).run {
            keys().asSequence()
                .filter { get(it) is String }
                .map { it to getString(it) }
                .toMap()
        }
    } else
    {
        mapOf()
    }

/**
 * Function to create a [EnumLike] parser.
 * @param unmapped the function to create the instance when the input doesn't match any discriminator
 * @return a function from string to [T]
 */
private inline fun <reified T : EnumLike>
        createParser(crossinline unmapped: (String) -> T): (String) -> T
{
    val map = T::class.sealedSubclasses
        .mapNotNull {
            it.objectInstance.let { objectInstance ->
                if (objectInstance != null)
                {
                    objectInstance.discriminator to objectInstance
                } else
                {
                    null
                }
            }
        }
        .toMap()
    return { value: String ->
        map[value] ?: unmapped(value)
    }
}

private fun JSONObject.longFromNumberOrStringOpt(field: String): Long? =
    if (this.has(field))
    {
        when (val value = this.get(field))
        {
            is Int -> value.toLong()
            is Long -> value
            is String -> value.toLongOrNull() ?: throw ModelException(
                "Value of field '$field' has unexpected value '$value'"
            )
            else -> throw ModelException(
                "Value of field '$field' has unexpected value '$value'"
            )
        }
    } else
    {
        null
    }