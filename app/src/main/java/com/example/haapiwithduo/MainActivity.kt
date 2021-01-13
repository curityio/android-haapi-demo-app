package com.example.haapiwithduo

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.util.Function
import androidx.constraintlayout.widget.ConstraintSet.WRAP_CONTENT
import androidx.core.content.ContextCompat.getSystemService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import se.curity.identityserver.haapi.android.sdk.HaapiTokenManager
import se.curity.identityserver.haapi.android.sdk.okhttp.OkHttpUtils.addHaapiInterceptor
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture


private const val host = "iggbom-curity.ngrok.io"
private const val baseUrl = "https://$host"
private const val clientId = "haapi-public-client"
private const val redirectUri = "https://localhost:7777/client-callback"

private val haapiTokenManager = HaapiTokenManager(
    URI("$baseUrl/oauth/v2/oauth-token"),
    clientId
)

class MainActivity : AppCompatActivity() {

    private val httpClient = OkHttpClient.Builder()
        .addHaapiInterceptor(haapiTokenManager)
        .build()

    private val marginParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    private val fieldsList = HashMap<Int, HashMap<Int, Pair<String, Function<View, String>>>>()

    init {
        marginParams.topMargin = 20
        marginParams.bottomMargin = 20
    }

    private fun logAppInfo()  {
        val packageInfo =
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signatures =  packageInfo.signingInfo.apkContentsSigners.map { SHA256(it.toByteArray()) }
        Log.d("PackageName", packageName)
        Log.d("AppInfo", "APK signatures $signatures")
    }

    private fun SHA256(bytes: ByteArray): String =
        Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes), Base64.DEFAULT)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logAppInfo()
    }

    fun restart(view: View) {
        val intent = intent
        finish()
        startActivity(intent)
        startLogin(view)
    }

    fun startLogin(view: View) {
        val authorizeUrl = Uri.Builder()
            .scheme("https")
            .authority(host)
            .appendPath("oauth")
            .appendPath("v2")
            .appendPath("oauth-authorize")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("state", "1586511942384-OcG")
            .appendQueryParameter("scope", "openid")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge", "ERNHshyzhznDQOKAIEkJl94N048wMAaN4jY-2xlVy_s")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("redirect_uri", redirectUri)
            .build()
            .toString()
        val request = Request.Builder()
            .get()
            .url(authorizeUrl)
            .build()

        view.post {
            view.visibility = GONE
        }

        val authenticatorLayout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        val selectorLayout = findViewById<LinearLayout>(R.id.selectorsLayout)

        authenticatorLayout.post {
            authenticatorLayout.removeAllViews()
        }

        selectorLayout.post {
            selectorLayout.removeAllViews()
        }

        callApi(request)
    }

    private fun processHaapiResponse(haapiResponse: Response) {
        val responseBody = haapiResponse.body?.string() ?: "{}"
        val haapiResponseObject = JSONObject(responseBody)

        //TODO just for now
//        showJSONResponse(responseBody)

        when (haapiResponseObject["type"]) {
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
            else -> return
        }
    }

    private fun processPollingStatus(haapiResponseObject: JSONObject) {
        val properties = haapiResponseObject.getJSONObject("properties")

        when (properties["status"]) {
            "done" -> processAuthenticationStep(haapiResponseObject.getJSONArray("actions"))
            "pending" -> pollStatus(haapiResponseObject)
        }
    }

    private fun pollStatus(haapiResponseObject: JSONObject) {
        val actions = haapiResponseObject.getJSONArray("actions")

        for (i in (0 until actions.length())) {
            val action = actions.getJSONObject(i)

            when (action["kind"]) {
                "poll" -> poll(action.getJSONObject("model"))
//                "cancel" -> //TODO: Not implemented yet
            }
        }
    }

    private fun poll(pollModel: JSONObject) {
        val request = Request.Builder()
                .url(pollModel["href"].toString())
                .get()
                .build()

        Handler(Looper.getMainLooper()).postDelayed({
            callApi(request)
        }, 3000)
    }

    private fun processAuthenticationStep(actions: JSONArray) {

        for (i in (0 until actions.length())) {
            val action = actions.getJSONObject(i)

            when (action["template"]) {
                "form" -> processHaapiForm(action)
                "selector" -> processSelector(action)
                else -> return
            }
        }
    }

    private fun processHaapiForm(form: JSONObject) {
        when (form["kind"]) {
            "login" -> processForm(form)
            "redirect" -> processHaapiRedirect(form.getJSONObject("model"))
            "form" -> processForm(form.getJSONObject("model"))

            else -> return
        }
    }

    private fun processHaapiRedirect(haapiResponseModel: JSONObject) {
        val href = haapiResponseModel["href"].toString()
        val url = if (href.startsWith("http")) href else "$baseUrl$href"

        val request = Request.Builder()
            .method(haapiResponseModel["method"].toString(), getRedirectBody(haapiResponseModel))
            .url(url)
            .build()
        callApi(request)
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

    private fun processSelector(selectorAction: JSONObject) {
        when (selectorAction["kind"]) {
            "authenticator-selector" -> processSelectorAuthenticator(selectorAction)
            "device-selector" -> processSelectorAuthenticator(selectorAction)
            "device-option" -> processDeviceOptions(selectorAction)
            else -> return
        }
    }

    private fun processSelectorAuthenticator(selectorAction: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.selectorsLayout)
        layout.post {
            layout.addView(generateHeader(selectorAction["title"].toString()))
            val selectorActionModel = selectorAction.getJSONObject("model")
            val options = selectorActionModel.getJSONArray("options")

            for (i in (0 until options.length())) {
                val selectorOptions = options.getJSONObject(i)
                val selector = Button(this)
                selector.id = generateViewId()
                selector.text = selectorOptions["title"].toString()
                selector.visibility = VISIBLE
                selector.layoutParams = marginParams
                selector.backgroundTintList = ColorStateList.valueOf(getColor(R.color.button))
                selector.setTextColor(getColor(R.color.button_txt))
                selector.setOnClickListener {
                    layout.post {
                        layout.visibility = GONE
                        layout.removeAllViewsInLayout()
                    }

                    when (selectorAction["kind"]) {
                        "authenticator-selector" -> {
                            val selectorOptionsModel = selectorOptions.getJSONObject("model")
                            selectAuthenticator(selectorOptionsModel["href"].toString())
                        }
                        "device-selector" -> processSelector(selectorOptions)
                    }
                }

                layout.addView(selector)
            }

            layout.visibility = VISIBLE
        }
    }

    private fun processForm(form: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        layout.post {
            layout.addView(generateHeader(form["title"].toString()))
            val model = form.getJSONObject("model")

            val fieldsArray = model.getJSONArray("fields")
            val fields = HashMap<Int, String>(fieldsArray.length())

            val paramsForFields = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            paramsForFields.setMargins(50, 20, 50, 20)

            for (i in (0 until fieldsArray.length())) {
                val fieldModel = fieldsArray.getJSONObject(i)
                val field = EditText(this)
                field.id = generateViewId()
                fields[field.id] = fieldModel["name"].toString()

                field.layoutParams = paramsForFields
                field.hint = fieldModel["label"].toString()
                field.onFocusChangeListener = FocusChangeListener()

                if (fieldModel["type"].toString() == "password") {
                    field.inputType = InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD or InputType.TYPE_CLASS_TEXT
                }

                layout.addView(field)
            }

            val submitButton = Button(this)
            submitButton.text = model["actionTitle"].toString()
            submitButton.layoutParams = marginParams
            submitButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.button))
            submitButton.setTextColor(getColor(R.color.button_txt))
            submitButton.setOnClickListener {
                val request = Request.Builder()
                    .method(model["method"].toString(), getBody(fields, model["type"].toString()))
                    .url("$baseUrl${model["href"]}")
                    .build()

                layout.post {
                    layout.removeAllViews()
                }

                callApi(request)
            }

            layout.addView(submitButton)
            layout.visibility = VISIBLE
        }

    }

    private fun getBody(fields: Map<Int, String>, mediaType: String): RequestBody {
        val requestString = StringBuilder()
        fields.forEach { entry ->
            val field = findViewById<EditText>(entry.key)
            requestString
                .append("&")
                .append(entry.value)
                .append("=")
                .append(field.text)
        }

        return requestString.toString().substring(1).toRequestBody(mediaType.toMediaType())
    }

    private fun selectAuthenticator(url: String) {
        val request = Request.Builder()
            .get()
            .url("$baseUrl$url")
            .build()
        callApi(request)
    }

    private fun callApi(request: Request): CompletableFuture<Void> {
        val loader = findViewById<ProgressBar>(R.id.loader)
        loader.post {
            loader.visibility = VISIBLE
        }

        return doCallApi(request).thenAccept { response ->
            loader.post {
                loader.visibility = GONE
            }
            processHaapiResponse(response)
        }
    }

    private fun doCallApi(request: Request): CompletableFuture<Response> {
        return CompletableFuture.supplyAsync {
            httpClient.newCall(request).execute()
        }
    }

    private fun showJSONResponse(response: String) {
        val view = findViewById<TextView>(R.id.api_response)
        val animation: Animation = AlphaAnimation(1f, 0f)
        animation.duration = 500
        animation.interpolator = LinearInterpolator()
        animation.repeatCount = 1
        animation.repeatMode = Animation.REVERSE

        view.post {
            view.text = response
            view.visibility = VISIBLE
            view.startAnimation(animation)
        }
    }

    private fun generateHeader(text: String): TextView {
        val selectorHeader = TextView(this)
        selectorHeader.id = generateViewId()
        selectorHeader.text = text
        selectorHeader.textAlignment = TEXT_ALIGNMENT_CENTER
        selectorHeader.layoutParams = marginParams

        return selectorHeader
    }

    private fun processDeviceOptions(deviceOptions: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.factorsLayout)
        layout.addView(generateHeader(deviceOptions["title"].toString()))
        layout.post {
            val form = LinearLayout(this)
            form.id = generateViewId()

            fieldsList[form.id] = HashMap()

            val deviceOptionsModel = deviceOptions.getJSONObject("model")
            val options = deviceOptionsModel.getJSONArray("options")

            val paramsForFields = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            paramsForFields.setMargins(370, 20, 210, 20)

            val passwordField = EditText(this)
            passwordField.id = generateViewId()
            passwordField.inputType = InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD or InputType.TYPE_CLASS_TEXT
            passwordField.onFocusChangeListener = FocusChangeListener()
            passwordField.layoutParams = paramsForFields
            passwordField.visibility = GONE


            val smsLink = TextView(this)
            smsLink.id = generateViewId()
            smsLink.layoutParams = paramsForFields
            smsLink.movementMethod = LinkMovementMethod.getInstance()
            smsLink.visibility = GONE

            val radioGroup = RadioGroup(this)
            radioGroup.layoutParams = marginParams
            radioGroup.id = generateViewId()

            for (i in (0 until options.length())) {
                val selectorOptions = options.getJSONObject(i)
                val radioButton = generateSelector(radioGroup, form.id, selectorOptions)

                if (i == 0) {
                    radioButton.isChecked = true
                }
            }

            radioGroup.setOnCheckedChangeListener { group, checkedId ->
                val selectedOption = findViewById<RadioButton>(checkedId)
                val checkedButton = findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                val option = JSONObject(checkedButton.tag.toString())
                val model = option.getJSONObject("model")
                val href = model.getString("href")
                val type = model.getString("type")

                when (selectedOption.text) {
                    "passcode" -> {
                        passwordField.hint =
                            model.getJSONArray("fields").getJSONObject(2).getString(
                                "label"
                            )
                        passwordField.visibility = VISIBLE
                        smsLink.visibility = GONE
                    }
                    "sms" -> {
                        smsLink.text = "Receive passcode via SMS"
                        smsLink.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                        passwordField.hint =
                            model.getJSONArray("continueActions").getJSONObject(0).getJSONObject(
                                "model"
                            ).getJSONArray("fields").getJSONObject(2).getString("label")
                        passwordField.visibility = VISIBLE
                        smsLink.visibility = VISIBLE

                        smsLink.setOnClickListener {

                            val request = Request.Builder()
                                .url("$baseUrl$href")
                                .method(
                                    "POST",
                                    getStringForRequest(model).toRequestBody(type.toMediaType())
                                )
                                .build()

                            callApi(request)

                            showAlertDialog("SMS sent", layout.context);

                        }
                    }
                    else -> {
                        passwordField.visibility = GONE
                        smsLink.visibility = GONE
                    }
                }
            }

            layout.addView(radioGroup)
            layout.addView(passwordField)
            layout.addView(smsLink)

            val submitButton = Button(this)
            submitButton.id = generateViewId()
            submitButton.text = "submit"
            submitButton.layoutParams = marginParams
            submitButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.button))
            submitButton.setTextColor(getColor(R.color.button_txt))
            submitButton.setOnClickListener {
                val checkedRadioButton = findViewById<RadioButton>(radioGroup.checkedRadioButtonId)

                val model = JSONObject(checkedRadioButton.tag.toString()).getJSONObject("model")
                val href = model.getString("href")
                val type = model.getString("type")
                val request :Request
                val requestString = StringBuilder()

                when (checkedRadioButton.text) {
                    "passcode" -> {
                        requestString.append(getStringForRequest(model))
                            .append("&")
                            .append("passcode")
                            .append("=")
                            .append(passwordField.text)
                    }
                    "sms" -> {
                        requestString.append(
                            getStringForRequest(
                                model.getJSONArray("continueActions").getJSONObject(
                                    0
                                ).getJSONObject("model")
                            )
                        )
                            .append("&")
                            .append("passcode")
                            .append("=")
                            .append(passwordField.text)
                    }
                    else -> {
                        requestString.append(getStringForRequest(model))
                    }
                }

                request = Request.Builder()
                    .url("$baseUrl$href")
                    .method("POST", requestString.toString().toRequestBody(type.toMediaType()))
                    .build()

                layout.post {
                    layout.removeAllViews()
                }

                callApi(request)
            }

            layout.addView(submitButton)
            layout.addView(form)
            layout.visibility = VISIBLE
        }
    }


    private fun showAlertDialog(stringToShow: String, context: Context) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage(stringToShow)
        builder.setCancelable(true)
        val alert: AlertDialog = builder.create()
        alert.show()
    }


    private fun generateSelector(radioGroup: RadioGroup, formId: Int, option: JSONObject): RadioButton {
        val radioButton = RadioButton(this)
        radioButton.id = generateViewId()
        radioButton.text = option["title"].toString()
        radioButton.tag = option

        radioGroup.addView(radioButton)

        fieldsList[formId]!![radioGroup.id] = Pair(option.getString("title"),
            Function<View, String> { view ->
                val checkedRadioId = (view as RadioGroup).checkedRadioButtonId

                val checkedRadioButton = findViewById<RadioButton>(checkedRadioId)
                return@Function checkedRadioButton.tag.toString()

            })

        return radioButton
    }

    private fun getStringForRequest(model: JSONObject): String {
        val requestBodyString = StringBuilder()
        val fields = model.getJSONArray("fields")

        for (i in (0 until fields.length())) {

            if(!fields.getJSONObject(i).isNull("value")){
            requestBodyString.append("&")
                .append(fields.getJSONObject(i).getString("name"))
                .append("=")
                .append(fields.getJSONObject(i).getString("value"))
            }
        }

        return requestBodyString.toString().substring(1)
    }

    private fun processAuthorizationResponse(properties: JSONObject) {
        val layout = findViewById<LinearLayout>(R.id.authenticatorLayout)
        layout.post {
            layout.removeAllViews()
            val text = TextView(this)
            text.text = getText(R.string.code_response)
            text.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            text.layoutParams = marginParams
            layout.addView(text)

            val code = TextView(this)
            code.text = properties["code"].toString()
            code.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            text.layoutParams = marginParams
            layout.addView(code)

//            layout.addView(getLogoutButton())

            layout.visibility = VISIBLE
        }
    }


    /** Util **/

    class FocusChangeListener: OnFocusChangeListener {

        private fun hideKeyboard(view: View) {
            val inputMethodManager: InputMethodManager = getSystemService(
                view.context,
                InputMethodManager::class.java
            )!!
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }
    }
}