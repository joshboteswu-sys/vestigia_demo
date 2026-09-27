package com.vestigia.app.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.android.material.textfield.TextInputEditText;
import com.vestigia.app.R;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

public class ChangePasswordActivity extends BaseProtectedActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private TextView tvError;
    private Button btnSave, btnBack;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        tvError = findViewById(R.id.tvError);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> attemptChange());
    }

    private void attemptChange() {
        String current = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirm)) {
            showError("Please fill in all fields.");
            return;
        }
        if (newPass.length() < 6) {
            showError("New password must be at least 6 characters.");
            return;
        }
        if (!newPass.equals(confirm)) {
            showError("New passwords do not match.");
            return;
        }

        showError(null);
        setLoading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("current_password", current);
            body.put("new_password", newPass);
        } catch (Exception e) {
            setLoading(false);
            showError("Unexpected error building request.");
            return;
        }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.CHANGE_PASSWORD, body, sessionManager.getToken(),
                response -> {
                    setLoading(false);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Password changed successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showError(response.optString("message", "Could not change password."));
                    }
                },
                error -> {
                    setLoading(false);
                    showError(parseServerMessage(error));
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private String parseServerMessage(com.android.volley.VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                JSONObject json = new JSONObject(new String(error.networkResponse.data));
                return json.optString("message", "Could not change password.");
            }
        } catch (Exception ignored) { }
        return "Could not reach the server. Check your connection.";
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
        btnSave.setEnabled(!loading);
    }
}