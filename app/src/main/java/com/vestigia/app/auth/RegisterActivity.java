package com.vestigia.app.auth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.VolleyMultipartRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    // Student ID format: 00-0000-000000
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{2}-\\d{4}-\\d{6}$");

    private EditText etFirstName, etLastName, etStudentId, etEmail, etPhone, etPassword, etConfirmPassword;
    private RadioGroup rgPreferredContact;
    private ImageView ivBack, ivPhoto, ivTogglePassword, ivToggleConfirm;
    private View photoContainer;
    private Button btnRegister;
    private TextView tvError, tvSignIn;
    private ProgressBar progressBar;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private boolean formattingStudentId = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ivBack = findViewById(R.id.ivBack);
        photoContainer = findViewById(R.id.photoContainer);
        ivPhoto = findViewById(R.id.ivPhoto);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etStudentId = findViewById(R.id.etStudentId);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        rgPreferredContact = findViewById(R.id.rgPreferredContact);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirm = findViewById(R.id.ivToggleConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvError = findViewById(R.id.tvError);
        tvSignIn = findViewById(R.id.tvSignIn);
        progressBar = findViewById(R.id.progressBar);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                ivPhoto.setPadding(0, 0, 0, 0); // remove placeholder padding so the photo fills the circle
                Glide.with(this).load(uri).circleCrop().into(ivPhoto);
            }
        });

        photoContainer.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        ivBack.setOnClickListener(v -> finish());
        tvSignIn.setOnClickListener(v -> finish()); // Login screen is right behind this one
        btnRegister.setOnClickListener(v -> attemptRegister());

        setupStudentIdFormatter();
        setupPasswordToggle(etPassword, ivTogglePassword);
        setupPasswordToggle(etConfirmPassword, ivToggleConfirm);
    }

    // Auto-inserts dashes as the user types digits: 12 -> 12-3456 -> 12-3456-789012
    private void setupStudentIdFormatter() {
        etStudentId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (formattingStudentId) return;
                formattingStudentId = true;

                String digits = s.toString().replaceAll("[^0-9]", "");
                if (digits.length() > 12) digits = digits.substring(0, 12);

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i == 2 || i == 6) formatted.append('-');
                    formatted.append(digits.charAt(i));
                }

                if (!formatted.toString().equals(s.toString())) {
                    s.replace(0, s.length(), formatted.toString());
                }
                formattingStudentId = false;
            }
        });
    }

    // Eye icon: dim = hidden, full brightness = visible
    private void setupPasswordToggle(EditText field, ImageView toggle) {
        toggle.setAlpha(0.5f);
        toggle.setOnClickListener(v -> {
            boolean currentlyHidden = field.getTransformationMethod() instanceof PasswordTransformationMethod;
            if (currentlyHidden) {
                field.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggle.setAlpha(1.0f);
            } else {
                field.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggle.setAlpha(0.5f);
            }
            field.setSelection(field.getText().length());
        });
    }

    private void attemptRegister() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phoneRaw = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(studentId) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phoneRaw) || TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirmPassword)) {
            showError("Please fill in all required fields.");
            return;
        }

        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            showError("Student ID must follow the format 00-0000-000000.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.length() > 50) {
            showError("Please enter a valid email address (max 50 characters).");
            return;
        }

        // Phone: accept "9123456789" or "09123456789", save as +639123456789
        String phoneDigits = phoneRaw.replaceAll("[^0-9]", "");
        if (phoneDigits.startsWith("0")) phoneDigits = phoneDigits.substring(1);
        if (phoneDigits.length() != 10 || !phoneDigits.startsWith("9")) {
            showError("Enter a valid mobile number, e.g. 912 345 6789.");
            return;
        }
        String contactNumber = "+63" + phoneDigits;

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        String preferredContact = "Email";
        int checkedId = rgPreferredContact.getCheckedRadioButtonId();
        if (checkedId != -1) {
            preferredContact = ((RadioButton) findViewById(checkedId)).getText().toString();
        }

        showError(null);
        setLoading(true);

        final Map<String, String> params = new HashMap<>();
        params.put("first_name", firstName);
        params.put("last_name", lastName);
        params.put("student_id", studentId);
        params.put("contact_number", contactNumber);
        params.put("email", email);
        params.put("password", password);
        params.put("preferred_contact", preferredContact);

        // Heavy image decode/compress runs off the main thread so the UI never freezes
        new Thread(() -> {
            byte[] imageBytes = null;
            if (selectedImageUri != null) {
                try {
                    imageBytes = readBytesFromUri(selectedImageUri);
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Could not read selected photo, registering without it.", Toast.LENGTH_SHORT).show());
                }
            }

            final byte[] finalImageBytes = imageBytes;

            runOnUiThread(() -> {
                VolleyMultipartRequest request = new VolleyMultipartRequest(
                        ApiConfig.REGISTER, null, params,
                        response -> {
                            setLoading(false);
                            if (response.optBoolean("success", false)) {
                                Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                showError(response.optString("message", "Registration failed."));
                            }
                        },
                        error -> {
                            setLoading(false);
                            showError(parseServerMessage(error));
                        }
                );

                if (finalImageBytes != null) {
                    request.setFile("photo", finalImageBytes, "photo.jpg", "image/jpeg");
                }

                VolleySingleton.getInstance(this).addToRequestQueue(request);
            });
        }).start();
    }

    // Shows the server's real reason (e.g. "account already exists") instead of a generic error
    private String parseServerMessage(VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                JSONObject json = new JSONObject(new String(error.networkResponse.data));
                return json.optString("message", "Registration failed. Please try again.");
            }
        } catch (Exception ignored) { }
        return "Could not reach the server. Check your connection.";
    }

    // Runs on a background thread — must never touch UI here.
    private byte[] readBytesFromUri(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) inputStream.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
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