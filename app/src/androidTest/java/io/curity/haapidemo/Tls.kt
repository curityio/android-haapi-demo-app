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

import okhttp3.OkHttpClient
import se.curity.identityserver.haapi.android.driver.HttpURLConnectionProvider
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

private val trustManager = object : X509TrustManager
{
    override fun checkClientTrusted(p0: Array<out X509Certificate>, p1: String)
    {
    }

    override fun checkServerTrusted(p0: Array<out X509Certificate>, p1: String)
    {
    }

    override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
}

fun OkHttpClient.Builder.disableSslTrustVerification(): OkHttpClient.Builder
{

    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, arrayOf(trustManager), null)
    return sslSocketFactory(sslContext.socketFactory, trustManager)
        .hostnameVerifier { _, _ -> true }
}

val DISABLE_TRUST_VERIFICATION: (OkHttpClient.Builder) -> Unit =
    { it.disableSslTrustVerification() }

fun URLConnection.disableSslTrustVerification(): URLConnection
{
    when (this)
    {
        is HttpsURLConnection ->
        {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(trustManager), null)
            this.setSSLSocketFactory(sslContext.socketFactory)
            this.setHostnameVerifier { _, _ -> true }
        }
    }
    return this
}

val UNCHECKED_CONNECTION_PROVIDER: HttpURLConnectionProvider = {
    it.openConnection().disableSslTrustVerification() as HttpURLConnection
}

val UNCHECKED_CONNECTION_PROVIDER_FUNCTION: java.util.function.Function<URL, HttpURLConnection> =
    java.util.function.Function {
        UNCHECKED_CONNECTION_PROVIDER(it)
    }
