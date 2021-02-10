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

package com.example.haapidemo.utils

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
