package com.vestigia.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.vestigia.app.R;
import com.vestigia.app.items.DashboardActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier, etPassword;
    private Button btnLogin;
    private TextView tvError, tvGoRegister;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // If already logged in, skip straight to dashboard (keeps user logged in
        // across app restarts).
        if (sessionManager.isLoggedIn()) {
            goToDashboard();
            return;
        }

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvError);
        tvGoRegister = findViewById(R.id.tvGoRegister);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // ---- Empty field validation ----
        if (TextUtils.isEmpty(identifier) || TextUtils.isEmpty(password)) {
            showError("Please fill in both fields.");
            return;
        }

        showError(null);
        setLoading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("identifier", identifier);
            body.put("password", password);
        } catch (JSONException e) {
            showError("Unexpected error building request.");
            setLoading(false);
            return;
        }

        com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                Request.Method.POST, ApiConfig.LOGIN, body,
                response -> {
                    setLoading(false);
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            String token = response.getString("token");
                            JSONObject user = response.getJSONObject("user");
                            sessionManager.saveSession(
                                    token,
                                    user.getInt("id"),
                                    user.getString("first_name"),
                                    user.getString("last_name"),
                                    user.getString("email")
                            );
                            Toast.makeText(this, "Welcome back, " + user.getString("first_name") + "!", Toast.LENGTH_SHORT).show();
                            goToDashboard();
                        } else {
                            showError(response.optString("message", "Login failed."));
                        }
                    } catch (JSONException e) {
                        showError("Unexpected response from server.");
                    }
                },
                error -> {
                    setLoading(false);
                    showError(parseServerMessage(error));
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    // Pulls the "message" field out of the API's JSON error body (e.g.
    // "Incorrect email/ID or password.") so the user sees the real reason,
    // not just a generic network error.
    private String parseServerMessage(VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                String body = new String(error.networkResponse.data);
                JSONObject json = new JSONObject(body);
                return json.optString("message", "Login failed. Please try again.");
            }
        } catch (Exception ignored) { }
        return "Could not reach the server. Check your connection.";
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        if (message == null) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
