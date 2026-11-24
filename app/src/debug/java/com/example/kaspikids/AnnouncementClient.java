package com.example.kaspikids;

import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;

final class AnnouncementClient implements AnnouncementService.Api {
    private final OkHttpClient http;
    private final HttpUrl endpoint;

    AnnouncementClient(OkHttpClient http, HttpUrl endpoint) {
        this.http = http;
        this.endpoint = endpoint;
    }

    @Override public String fetch() throws IOException {
        Request request = new Request.Builder().url(endpoint).get().build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            ResponseBody body = response.body();
            if (body == null) throw new IOException("Empty announcement response");
            try {
                Object message = new JSONObject(body.string()).opt("message");
                if (message instanceof String && !((String) message).trim().isEmpty()) {
                    return (String) message;
                }
                throw new IOException("Missing announcement message");
            } catch (JSONException e) {
                throw new IOException("Invalid announcement JSON", e);
            }
        }
    }
}
