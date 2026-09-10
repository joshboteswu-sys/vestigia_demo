package com.vestigia.app.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// Extend this instead of AppCompatActivity for every screen that requires
// the user to be logged in (Dashboard, Found Items list, Add/Edit item, etc).
// It redirects to LoginActivity immediately if there's no valid local session,
// which satisfies "prevent access to protected pages after logout".
public abstract class BaseProtectedActivity extends AppCompatActivity {

    protected SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check every time the screen becomes visible (e.g. user logged out
        // in another tab, or the back button was pressed after logout).
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, com.vestigia.app.auth.LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
