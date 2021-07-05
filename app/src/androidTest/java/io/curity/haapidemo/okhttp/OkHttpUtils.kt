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

package io.curity.haapidemo.okhttp

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import io.curity.haapidemo.models.haapi.actions.Action
import io.curity.haapidemo.models.haapi.Field
import io.curity.haapidemo.API_MEDIA_TYPE
import io.curity.haapidemo.HOST_NAME
import io.curity.haapidemo.PORT
import io.curity.haapidemo.SCHEME
import io.curity.haapidemo.models.HaapiStep
import io.curity.haapidemo.parsers.ModelException
import io.curity.haapidemo.parsers.RepresentationParser
import io.curity.haapidemo.parsers.toHaapiStep

/*
 * OkHttp utilities
 */

fun Response.toHaapiStep(): HaapiStep =
    when (this.code)
    {
        200 -> when (val mediaType = this.header("Content-Type"))
        {
            API_MEDIA_TYPE ->
                RepresentationParser.parse(
                    JSONObject(
                        this.body?.string()
                            ?: throw ModelException("Response body is empty")
                    )
                ).toHaapiStep()
            else -> throw ModelException("Invalid response media type '$mediaType'")
        }
        else -> throw ModelException(
            "Response has status '${this.code}', " +
                    "body='${this.body?.string()}'"
        )
    }

fun OkHttpClient.submit(action: Action.Form, vararg pairs: Pair<String, String>): Response
{
    val map = pairs.toMap()
    val request = if (action.model.method == "POST")
    {
        val requestBody = FormBody.Builder().also { formBuilder ->
            action.model.fields.forEach { field ->
                if (field is Field.Hidden)
                {
                    formBuilder.add(field.name, field.value ?: "")
                } else {
                    val value = map[field.name] ?: throw Exception("Unable to find value for field ${field.name}")
                    formBuilder.add(field.name, value)
                }
            }
        }.build()

        Request.Builder()
            .url(absolute(action.model.href))
            .post(requestBody)
            .build()
    } else
    {
        val urlBuilder = absolute(action.model.href).toHttpUrlOrNull()?.newBuilder()
            ?: throw Exception("Invalid url '${action.model.href}'")
        action.model.fields.forEach { field ->
            if (field is Field.Hidden)
            {
                urlBuilder.addQueryParameter(field.name, field.value)
            } else {
                val value = map[field.name] ?: throw Exception("Unable to find value for field ${field.name}")
                urlBuilder.addQueryParameter(field.name, value)
            }
        }
        Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()
    }

    return this.newCall(request).execute()
}

private fun absolute(url: String) =
    if (url.startsWith("http"))
    {
        url
    } else
    {
        "$SCHEME://$HOST_NAME$PORT$url"
    }
