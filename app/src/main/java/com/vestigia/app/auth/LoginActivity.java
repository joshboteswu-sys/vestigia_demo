package com.vestigia.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.vestigia.app.R;
import com.vestigia.app.items.DashboardActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier, etPassword;
    private ImageView ivTogglePassword;
    private Button btnLogin;
    private TextView tvError, tvGoRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Already logged in? Skip straight to the home screen.
        if (sessionManager.isLoggedIn()) {
            goToDashboard();
            return;
        }

        // White status bar with dark icons so it blends with the white login screen
        getWindow().setStatusBarColor(getResources().getColor(R.color.surface));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvError);
        tvGoRegister = findViewById(R.id.tvGoRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            intent.putExtra(ForgotPasswordActivity.EXTRA_IDENTIFIER, etIdentifier.getText().toString().trim());
            startActivity(intent);
        });

        // Keyboard "Done" key submits the form
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivTogglePassword.setImageResource(R.drawable.ic_visibility);
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        }
        etPassword.setSelection(etPassword.getText().length());
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

        JsonObjectRequest request = new JsonObjectRequest(
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
                                    user.getString("email"),
                                    user.optBoolean("is_admin", false)
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
                    handleLoginError(error, identifier);
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    // Pulls the "message" field out of the API's JSON error body so the user
    // sees the real reason (e.g. "Incorrect email/ID or password.").
    private String parseServerMessage(VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                JSONObject json = new JSONObject(new String(error.networkResponse.data));
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
    // An unverified account sends the user to the verification screen instead of showing an error.
    private void handleLoginError(VolleyError error, String typedIdentifier) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                JSONObject json = new JSONObject(new String(error.networkResponse.data));
                if ("email_not_verified".equals(json.optString("error"))) {
                    Intent intent = new Intent(this, VerifyEmailActivity.class);
                    intent.putExtra(VerifyEmailActivity.EXTRA_EMAIL, json.optString("email", typedIdentifier));
                    intent.putExtra(VerifyEmailActivity.EXTRA_AUTO_RESEND, true); // send a fresh code on open
                    startActivity(intent);
                    return;
                }
            }
        } catch (Exception ignored) { }
        showError(parseServerMessage(error));
    }
}