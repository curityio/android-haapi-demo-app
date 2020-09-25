/*
 * Copyright (C) 2020 Curity AB. All rights reserved.
 * The contents of this file are the property of Curity AB.
 * You may not copy or use this file, in either source code
 * or executable form, except in compliance with terms
 * set by Curity AB.
 * For further information, please contact Curity AB.
 */

package com.example.haapiwithduo.utils

import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun OkHttpClient.Builder.disableSslTrustVerification(): OkHttpClient.Builder
{
    val trustManager = object : X509TrustManager
    {
        override fun checkClientTrusted(p0: Array<out X509Certificate>, p1: String)
        {
        }

        override fun checkServerTrusted(p0: Array<out X509Certificate>, p1: String)
        {
        }

        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    }
    val sslContext = SSLContext.getInstance("SSL")

    sslContext.init(null, arrayOf(trustManager), null)

    return sslSocketFactory(sslContext.socketFactory, trustManager)
        .hostnameVerifier { _, _ -> true }
}
