package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.auth.LoginActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

public class DashboardActivity extends BaseProtectedActivity {

    private TextView tvWelcome;
    private Button btnReportItem, btnBrowseItems, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // BaseProtectedActivity already redirects if not logged in
        setContentView(R.layout.activity_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnReportItem = findViewById(R.id.btnReportItem);
        btnBrowseItems = findViewById(R.id.btnBrowseItems);
        btnLogout = findViewById(R.id.btnLogout);

        // Display the logged-in user's name
        tvWelcome.setText("Welcome back, " + sessionManager.getFirstName() + "!");

        btnReportItem.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditItemActivity.class)));

        btnBrowseItems.setOnClickListener(v ->
                startActivity(new Intent(this, FoundItemsListActivity.class)));

        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        String token = sessionManager.getToken();

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.LOGOUT, null, token,
                response -> {
                    // clear local session regardless, then go to Login
                    sessionManager.clearSession();
                    Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
                    goToLogin();
                },
                error -> {
                    // Even if the server call fails (e.g. offline), clear the local
                    // session so the device still logs the user out.
                    sessionManager.clearSession();
                    goToLogin();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
