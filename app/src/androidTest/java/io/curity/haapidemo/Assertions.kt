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

import okhttp3.Response
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/*
 * Helper assertions for testing purposes.
 */

fun assertApiRedirectToAuthenticationService(authorizationResponse: Response)
{
    assertEquals(200, authorizationResponse.code)

    val bodyString = authorizationResponse.body?.string()
        ?: throw AssertionError("Response body must exist")
    val json = JSONObject(bodyString)
    assertEquals("redirection-step", json.getString("type"))
    val action = json.getJSONArray("actions").getJSONObject(0)
    assertEquals("redirect", action.getString("kind"))
}

fun assertUnauthorized(authorizationResponse: Response)
{
    assertEquals(401, authorizationResponse.code)
}

fun <T> T.assertIs(expected: T)
{
    assertEquals(expected, this)
}

fun <T> T?.assertNotNull()
{
    assertTrue(this != null)
}

fun String?.assertNonBlank()
{
    assertFalse(this?.isBlank() ?: true)
}

fun <T> List<T>.assertSize(expected: Int)
{
    assertEquals(expected, this.size)
}

inline fun <reified T, R> Any.assertOfType(body: T.() -> R): R
{
    assertEquals(T::class.java, this.javaClass)
    return body(this as T)
}

inline fun <reified T> Any.assertOfType(body: T.() -> Unit): Unit
{
    assertEquals(T::class.java, this.javaClass)
    return body(this as T)
}

inline fun <reified T> Any.assertOfType(): T
{
    assertEquals(T::class.java, this.javaClass)
    return this as T
}