/*
 * Copyright (C) 2020 Curity AB. All rights reserved.
 *
 * The contents of this file are the property of Curity AB.
 * You may not copy or use this file, in either source code
 * or executable form, except in compliance with terms
 * set by Curity AB.
 *
 * For further information, please contact Curity AB.
 */

package io.curity.haapidemo

import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI

const val EMULATOR_HOST_IP = "10.0.2.2"
val BASE_URI: URI = URI("https://$EMULATOR_HOST_IP:8443")
val HOST_NAME = BASE_URI.host
val SCHEME = BASE_URI.scheme
val PORT = ":${BASE_URI.port}"

val TOKEN_ENDPOINT = "$SCHEME://$HOST_NAME$PORT/dev/oauth/token"
val AUTHORIZATION_ENDPOINT = "$SCHEME://$HOST_NAME$PORT/dev/oauth/authorize"
const val API_MEDIA_TYPE = "application/vnd.auth+json"

@JvmField
val CLIENT_ID: String = "haapi-android-client"

val AUTHORIZATION_REQUEST_URI: HttpUrl = AUTHORIZATION_ENDPOINT.toHttpUrl()
        .newBuilder()
        .addQueryParameter("client_id", CLIENT_ID)
        .addQueryParameter("response_type", "code")
        .addQueryParameter("scope", "read")
        .addQueryParameter("state", "foo")
        .addQueryParameter("redirect_uri", "haapi:start")
        .build()

val AUTHORIZATION_REQUEST_USING_IMPLICIT_URI: HttpUrl = AUTHORIZATION_ENDPOINT.toHttpUrl()
        .newBuilder()
        .addQueryParameter("client_id", CLIENT_ID)
        .addQueryParameter("response_type", "token")
        .addQueryParameter("scope", "read")
        .addQueryParameter("state", "foo")
        .addQueryParameter("redirect_uri", "haapi:start")
        .build()


