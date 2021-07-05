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
package io.curity.haapidemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import io.curity.haapidemo.R
import io.curity.haapidemo.utils.ViewsCreator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class HaapiResponseProcessor(
    private val context: Context,
    private val baseUrl: String,
    private val apiCaller: (Request, Boolean, (String) -> List<List<View>>) -> Unit,
    private val redraw: (List<View>, Boolean) -> Unit
    ) {
    private val views = ViewsCreator(context)

    private val marginParams = LinearLayout.LayoutParams(
        ConstraintSet.WRAP_CONTENT,
        ConstraintSet.WRAP_CONTENT
    )

    private val paramsForFields = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ConstraintSet.WRAP_CONTENT
    )

    private val fieldsList = HashMap<Int, HashMap<Int, Pair<String, TextView>>>()

    init {
        marginParams.topMargin = 20
        marginParams.bottomMargin = 20

        paramsForFields.setMargins(50, 20, 50, 20)
    }

    fun processHaapiResponse(haapiResponseString: String): List<List<View>> {

        val haapiResponseObject = JSONObject(haapiResponseString)

        val type = haapiResponseObject["type"].toString()
        return when (type) {
            "redirection-step" -> processAuthenticationStep(haapiResponseObject.getJSONArray("actions"))
            "authentication-step" -> processAuthenticationStep(haapiResponseObject.getJSONArray("actions"))
            "polling-status" -> processPollingStatus(haapiResponseObject)
            "oauth-authorization-response" -> processAuthorizationResponse(
                haapiResponseObject.getJSONObject(
                    "properties"
                )
            )
            "polling-step" -> processPollingStatus(haapiResponseObject)
//            "continue-same-step" ->
//            "https://curity.se/problems/incorrect-credentials" -> //TODO: Implement handling of failed authentication
//            "registration-step" //TODO: Implement handling of registration
            else -> {

                if (type.startsWith("https://curity.se/problems")) {
                    return processErrors(haapiResponseObject)
                }

                return emptyList()
            }
        }
    }

    private fun processAuthenticationStep(actions: JSONArray): List<List<View>> {
        val viewsList = mutableListOf<List<View>>()

        for (i in (0 until actions.length())) {
            val action = actions.getJSONObject(i)

            viewsList.add(processAction(action))
        }

        return viewsList
    }

    private fun processAction(action: JSONObject): List<View> {
        return when (action["template"]) {
            "form" -> processHaapiForm(action)
            "selector" -> processSelector(action)
            "client-operation" -> processClientOperation(action)
            else -> emptyList()
        }
    }

    private fun processHaapiForm(form: JSONObject): List<View> {

        return when (form["kind"]) {
            "redirect" -> processHaapiRedirect(form)
            // TODO: Implement continue and cancel UI elements
            "continue" -> emptyList()
            "cancel" -> emptyList()
            else -> processForm(form)
        }
    }

    private fun processForm(form: JSONObject): List<View> {

        val title = form["title"].toString()
        val formViews = mutableListOf<View>()
        formViews.add(views.header(title))
        val model = form.getJSONObject("model")

        val fields = processFormFields(model)
        for (field in fields) {
            formViews.add(field.value)
        }

        val submitButton = views.button(model.optString("actionTitle", context.getText(R.string.submit_button).toString()))
        submitButton.setOnClickListener {

            val href = model["href"].toString()
            val url = if (href.startsWith("http")) href else "$baseUrl$href"
            val request = Request.Builder()
                    .method(model.getString("method"), getBody(fields, model.getString("type")))
                    .url(url)
                    .build()

            apiCaller(request, true) { haapiResponse -> processHaapiResponse(haapiResponse) }
        }

        formViews.add(submitButton)
        return formViews
    }

    private fun processFormFields(model: JSONObject): Map<String, TextView> {

        if (!model.has("fields")) {
            return emptyMap()
        }

        val fieldsArray = model.getJSONArray("fields")
        val fields = HashMap<String, TextView>(fieldsArray.length())

        for (i in (0 until fieldsArray.length())) {
            val fieldModel = fieldsArray.getJSONObject(i)

            val field = when (fieldModel.getString("type")) {
                "hidden" -> views.hiddenField(fieldModel.getString("value"))
                "password" -> views.passwordField(fieldModel.getString("label"))
                "text" -> views.inputField(fieldModel.getString("label"))
                "username" -> views.inputField(fieldModel.getString("label"))
                else -> views.hiddenField(fieldModel.getString("value"))
            }

            fields[fieldModel["name"].toString()] = field
        }

        return fields
    }

    private fun getBody(fields: Map<String, TextView>, mediaType: String): RequestBody {
        val requestString = fields
            .map { entry -> "${entry.key}=${entry.value.text}" }
            .joinToString("&")

        return requestString.toRequestBody(mediaType.toMediaType())
    }

    private fun processHaapiRedirect(haapiRedirectForm: JSONObject): List<View> {

        val haapiResponseModel = haapiRedirectForm.getJSONObject("model")
        val href = haapiResponseModel["href"].toString()
        val url = if (href.startsWith("http")) href else "$baseUrl$href"

        val request = Request.Builder()
            .method(haapiResponseModel["method"].toString(), getRedirectBody(haapiResponseModel))
            .url(url)
            .build()

        apiCaller(request, false) { haapiResponse -> processHaapiResponse(haapiResponse) }
        return emptyList()
    }

    private fun getRedirectBody(haapiResponseModel: JSONObject): RequestBody? {
        if (!haapiResponseModel.has("fields")) {
            return null
        }

        val fields = haapiResponseModel.getJSONArray("fields")
        val requestBodyString = StringBuilder()

        for (i in (0 until fields.length())) {
            val field = fields.getJSONObject(i)
            requestBodyString.append("&")
                .append(field["name"])
                .append("=")
                .append(field["value"])
        }

        return requestBodyString.toString().substring(1).toRequestBody(
            haapiResponseModel["type"].toString().toMediaType()
        )
    }

    private fun processSelector(selectorAction: JSONObject): List<View> {
        return when (selectorAction["kind"]) {
            "authenticator-selector" -> processSelectorAuthenticator(selectorAction)
            "device-selector" -> processSelectorAuthenticator(selectorAction)
            "device-option" -> processDeviceOptions(selectorAction)
            else -> emptyList()
        }
    }

    private fun processClientOperation(action: JSONObject): List<View> {

        val model =  action.getJSONObject("model")
        return when (model["name"]) {
            // TODO: Implement the external browser flow correctly
            "external-browser-flow" -> emptyList()
            "bankid" -> processLaunch(model)
            else -> emptyList()
        }
    }

    private fun processLaunch(model: JSONObject): List<View> {

        val args = model.getJSONObject("arguments")
        val launchUrl = args["href"].toString()

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(launchUrl)
            (context as Activity).startActivity(intent)

            val action = model.getJSONArray("continueActions").getJSONObject(0)
            return processAction(action)

        } catch (e: Throwable) {

            val action = model.getJSONArray("errorActions").getJSONObject(0)
            return processAction(action)
        }
    }

    private fun processSelectorAuthenticator(selectorAction: JSONObject): List<View> {
        val selectorViews = mutableListOf<View>()

        selectorViews.add(views.header(selectorAction.getString("title")))
        val selectorActionModel = selectorAction.getJSONObject("model")
        val options = selectorActionModel.getJSONArray("options")

        for (i in (0 until options.length())) {
            val selectorOptions = options.getJSONObject(i)
            val selector = views.button(selectorOptions.getString("title"))
            selector.setOnClickListener {

                when (selectorAction["kind"]) {
                    "authenticator-selector" -> {
                        val selectorOptionsModel = selectorOptions.getJSONObject("model")
                        selectAuthenticator(selectorOptionsModel["href"].toString())
                    }
                    "device-selector" -> redraw(processSelector(selectorOptions), true)
                }
            }

            selectorViews.add(selector)
        }

        return selectorViews
    }

    private fun selectAuthenticator(url: String) {
        val request = Request.Builder()
            .get()
            .url("$baseUrl$url")
            .build()

        apiCaller(request, true) { haapiResponse -> processHaapiResponse(haapiResponse) }
    }

    private fun processDeviceOptions(deviceOptions: JSONObject): List<View> {
        val deviceOptionsViews = mutableListOf<View>()
        deviceOptionsViews.add(views.header(deviceOptions.getString("title")))

        val form = views.form()
        val deviceOptionsModel = deviceOptions.getJSONObject("model")
        val options = deviceOptionsModel.getJSONArray("options")

        val radioGroup = views.radioGroup()

        val radioButtons = hashMapOf<Int, JSONObject>()

        for (i in (0 until options.length())) {
            val selectorOptions = options.getJSONObject(i)
            val radioButton = views.radioSelector(selectorOptions.getString("title"))
            radioGroup.addView(radioButton)
            radioButtons[radioButton.id] = selectorOptions.getJSONObject("model")

            if (i == 0) {
                radioButton.isChecked = true
                processDeviceOption(form, selectorOptions.getJSONObject("model"))
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedOptionModel = radioButtons[checkedId]!!
            processDeviceOption(form, selectedOptionModel)
        }

        deviceOptionsViews.add(radioGroup)
        deviceOptionsViews.add(form)

        return deviceOptionsViews
    }

    private fun processDeviceOption(form: LinearLayout, model: JSONObject) {
        fieldsList[form.id] = HashMap()
        val formFields = mutableListOf<View>()

        val href = model.getString("href")
        val method = model.getString("method")
        val contentType = model.getString("type")

        val fields = model.getJSONArray("fields")
        for (i in (0 until fields.length())) {
            val field = fields.getJSONObject(i)

            when (field["type"].toString()) {
                "hidden" -> {
                    val hiddenText = views.hiddenField(field.getString("value"))
                    formFields.add(hiddenText)
                    fieldsList[form.id]!![hiddenText.id] = Pair(field["name"].toString(), hiddenText)
                }
                "text" -> {
                    val input = views.inputField(field.getString("label"))
                    formFields.add(input)
                    fieldsList[form.id]!![input.id] = Pair(field.getString("name"), input)
                }
            }
        }

        val submitButton = views.button(context.getText(R.string.submit_button).toString())
        submitButton.setOnClickListener {
            val request: Request
            val requestBody = getBodyForRequest(fieldsList[form.id]!!, contentType)

            request = Request.Builder()
                .url("$baseUrl$href")
                .method(method, requestBody)
                .build()

            if (model.has("continueActions")) {
                // TODO: error from API call should be handled somehow.
                // TODO: maybe layout should be refreshed only after a successful API call?
                apiCaller(request, true) { listOf(processAction(model.getJSONArray("continueActions").getJSONObject(0))) }
            } else {
                apiCaller(request, true) { haapiResponse -> processHaapiResponse(haapiResponse) }
            }
        }

        formFields.add(submitButton)

        form.post {
            form.removeAllViews()
            formFields.forEach { form.addView(it) }
        }
    }

    private fun  getBodyForRequest(
        fieldsList: Map<Int, Pair<String, TextView>>,
        contentType: String
    ): RequestBody {
        val requestBodyString = fieldsList
            .map { entry -> "${entry.value.first}=${entry.value.second.text}" }
            .joinToString("&")

        return requestBodyString.toRequestBody(contentType.toMediaType())
    }

    private fun processPollingStatus(haapiResponseObject: JSONObject): List<List<View>> {

        val properties = haapiResponseObject.getJSONObject("properties")
        val status = properties["status"].toString()
        val actions = haapiResponseObject.getJSONArray("actions")

        return when (status) {
            "done" -> {
                redraw(emptyList(), true)
                return processAuthenticationStep(actions)
            }
            "pending" -> {
                redraw(listOf(views.textField("Waiting, status is ${status} ...")), true)
                return pollStatus(actions)
            }
            else -> emptyList()
        }
    }

    private fun pollStatus(actions: JSONArray): List<List<View>> {

        for (i in (0 until actions.length())) {
            val action = actions.getJSONObject(i)
            when (action["kind"]) {
                "poll" -> poll(action.getJSONObject("model"))
//                "cancel" -> //TODO: Not implemented yet
            }
        }

        return emptyList()
    }

    private fun poll(pollModel: JSONObject) {
        val request = Request.Builder()
            .url(pollModel["href"].toString())
            .get()
            .build()

        Handler(Looper.getMainLooper()).postDelayed({
            apiCaller(request, false) {
                processHaapiResponse(it)
            }
        }, 3000)
    }

    private fun processAuthorizationResponse(properties: JSONObject): List<List<View>> {
        return listOf(listOf(
            views.textField(context.getText(R.string.code_response).toString()),
            views.textField(properties.getString("code"))
        ))
    }

    private fun processErrors(haapiResponse: JSONObject): List<List<View>> {

        val type = haapiResponse["type"].toString()
        return when (type) {
            "https://curity.se/problems/invalid-input" -> processInvalidInput(haapiResponse)
            "https://curity.se/problems/incorrect-credentials" -> processGeneralError(haapiResponse)
            else -> processGeneralError(haapiResponse)
        }
    }

    private fun processInvalidInput(haapiResponse: JSONObject): List<List<View>> {

        val errorViews = mutableListOf<View>()
        errorViews.add(views.errorField(haapiResponse.getString("title")))

        if (haapiResponse.has("invalidFields")) {

            val fields = haapiResponse.getJSONArray("invalidFields")
            for (i in (0 until fields.length())) {
                val field = fields.getJSONObject(i)
                if (field.has("detail")) {
                    errorViews.add(views.errorField(field["detail"].toString()))
                }
            }
        }

        return listOf(errorViews)
    }

    private fun processGeneralError(haapiResponse: JSONObject): List<List<View>> {

        val errorViews = mutableListOf<View>()
        errorViews.add(views.errorField(haapiResponse.getString("title")))

        if (haapiResponse.has("messages")) {

            val fields = haapiResponse.getJSONArray("messages")
            for (i in (0 until fields.length())) {
                val field = fields.getJSONObject(i)
                if (field.has("text")) {
                    errorViews.add(views.errorField(field["text"].toString()))
                }
            }
        }

        return listOf(errorViews)
    }
}
