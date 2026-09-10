package com.vestigia.app.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "vestigia_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, int userId, String firstName, String lastName, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, "");
    }

    public String getLastName() {
        return prefs.getString(KEY_LAST_NAME, "");
    }

    public String getFullName() {
        return (getFirstName() + " " + getLastName()).trim();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // Called after a successful /logout.php call (or if the server says the
    // session is invalid/expired) to clear everything stored locally.
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
