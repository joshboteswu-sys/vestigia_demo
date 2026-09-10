package com.vestigia.app.network;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

// Use this for any call to a protected endpoint (needs the session token).
public class AuthJsonObjectRequest extends JsonObjectRequest {

    private final String token;

    public AuthJsonObjectRequest(int method, String url, JSONObject body, String token,
                                  Response.Listener<JSONObject> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, body, listener, errorListener);
        this.token = token;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
