package com.vestigia.app.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.vestigia.app.R;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etMiddleInitial, etStudentId,
            etContactNumber, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister, btnBack;
    private TextView tvError;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMiddleInitial = findViewById(R.id.etMiddleInitial);
        etStudentId = findViewById(R.id.etStudentId);
        etContactNumber = findViewById(R.id.etContactNumber);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleInitial = etMiddleInitial.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(studentId) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showError("Please fill in all required fields.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        showError(null);
        setLoading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("first_name", firstName);
            body.put("last_name", lastName);
            body.put("middle_initial", middleInitial);
            body.put("student_id", studentId);
            body.put("contact_number", contactNumber);
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) {
            setLoading(false);
            showError("Unexpected error building request.");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, ApiConfig.REGISTER, body,
                response -> {
                    setLoading(false);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_LONG).show();
                        finish(); // back to Login
                    } else {
                        showError(response.optString("message", "Registration failed."));
                    }
                },
                error -> {
                    setLoading(false);
                    showError(parseServerMessage(error));
                });

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private String parseServerMessage(VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                String body = new String(error.networkResponse.data);
                JSONObject json = new JSONObject(body);
                return json.optString("message", "Registration failed. Please try again.");
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
        btnRegister.setEnabled(!loading);
    }
}
