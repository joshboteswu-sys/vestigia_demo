package com.vestigia.app.network;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class VolleySingleton {
    private static VolleySingleton instance;
    private RequestQueue requestQueue;

    private VolleySingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public <T> void addToRequestQueue(com.android.volley.Request<T> req) {
        // 30s timeout, no automatic retry: a silent retry would submit the same request twice.
        req.setRetryPolicy(new DefaultRetryPolicy(30000, 0, 1.0f));
        getRequestQueue().add(req);
    }
}