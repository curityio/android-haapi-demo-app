package com.example.haapiwithduo;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import kotlin.Unit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import se.curity.identityserver.haapi.android.sdk.HaapiTokenManager;

import static se.curity.identityserver.haapi.android.sdk.okhttp.OkHttpUtils.addHaapiInterceptor;

public class CheckSth {

    private void doCheck() throws JSONException, URISyntaxException, IOException {
        HaapiTokenManager haapiTokenManager = new HaapiTokenManager.Builder(new URI(""), "").build();

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

        addHaapiInterceptor(httpClientBuilder, haapiTokenManager);
        OkHttpClient httpClient = httpClientBuilder.build();

        String authorizeUrl = new Uri.Builder()
                .scheme("https")
                .authority("curity-instance.example.com")
                .appendPath("oauth")
                .appendPath("v2")
                .appendPath("oauth-authorize")
                .appendQueryParameter("client_id", "client-name")
                .appendQueryParameter("scope", "openid")
                .appendQueryParameter("response_type", "code")
                .build()
                .toString();

        Request request = new Request.Builder()
                .get()
                .url(authorizeUrl)
                .build();

        Response response = httpClient.newCall(request).execute();

        String responseBodyString;

        try {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                responseBodyString = responseBody.string();
            } else {
                responseBodyString = "{}";
            }
        } catch (IOException e) {
            responseBodyString = "{}";
        }

        JSONObject responseObject = new JSONObject(responseBodyString);

        System.out.println("Response type  = " + responseObject.getString("type"));
    }
}
