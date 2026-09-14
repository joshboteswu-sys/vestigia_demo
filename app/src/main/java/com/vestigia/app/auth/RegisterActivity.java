package com.vestigia.app.auth;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.VolleyMultipartRequest;
import com.vestigia.app.network.VolleySingleton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etMiddleInitial, etStudentId,
            etContactNumber, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister, btnBack, btnSelectPhoto;
    private ImageView ivPhoto;
    private TextView tvError;
    private ProgressBar progressBar;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

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
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        ivPhoto = findViewById(R.id.ivPhoto);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                Glide.with(this).load(uri).circleCrop().into(ivPhoto);
            }
        });

        btnSelectPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        ivPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

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

        Map<String, String> params = new HashMap<>();
        params.put("first_name", firstName);
        params.put("last_name", lastName);
        params.put("middle_initial", middleInitial);
        params.put("student_id", studentId);
        params.put("contact_number", contactNumber);
        params.put("email", email);
        params.put("password", password);

        VolleyMultipartRequest request = new VolleyMultipartRequest(
                ApiConfig.REGISTER, null, params,
                response -> {
                    setLoading(false);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_LONG).show();
                        finish(); // back to Login
                    } else {
                        showError(response.optString("message", "Registration failed."));
                    }
                },
                error -> {
                    setLoading(false);
                    showError("Could not reach the server. Check your connection.");
                }
        );

        if (selectedImageUri != null) {
            try {
                byte[] bytes = readBytesFromUri(selectedImageUri);
                request.setFile("photo", bytes, "photo.jpg", "image/jpeg");
            } catch (Exception e) {
                Toast.makeText(this, "Could not read selected photo, registering without it.", Toast.LENGTH_SHORT).show();
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private byte[] readBytesFromUri(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) inputStream.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
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