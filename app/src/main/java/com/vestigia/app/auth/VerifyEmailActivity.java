package com.vestigia.app.auth;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

public class VerifyEmailActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_AUTO_RESEND = "extra_auto_resend";
    public static final String EXTRA_SEND_FAILED = "extra_send_failed";

    private static final long RESEND_COOLDOWN_MS = 60_000;

    private EditText etCode;
    private TextView tvEmailAddress, tvError, tvResend, tvBackToLogin;
    private Button btnVerify;
    private ProgressBar progressBar;
    private ImageView ivBack;

    private String email;
    private boolean busy = false;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        getWindow().setStatusBarColor(getResources().getColor(R.color.surface));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null || email.isEmpty()) {
            finish();
            return;
        }

        etCode = findViewById(R.id.etCode);
        tvEmailAddress = findViewById(R.id.tvEmailAddress);
        tvError = findViewById(R.id.tvError);
        tvResend = findViewById(R.id.tvResend);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        btnVerify = findViewById(R.id.btnVerify);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);

        tvEmailAddress.setText(email);

        ivBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());
        btnVerify.setOnClickListener(v -> submitCode());
        tvResend.setOnClickListener(v -> {
            if (tvResend.isEnabled()) resendCode();
        });

        // Submit automatically once all 6 digits are typed
        etCode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 6) submitCode();
            }
        });

        if (getIntent().getBooleanExtra(EXTRA_AUTO_RESEND, false)) {
            resendCode();                         // opened from Login: send a fresh code
        } else if (getIntent().getBooleanExtra(EXTRA_SEND_FAILED, false)) {
            showError("We couldn't send the email yet. Tap \"Resend code\" to try again.");
            setResendEnabled(true);
        } else {
            startCooldown(RESEND_COOLDOWN_MS);    // a code was just sent at registration
        }
    }

    private void submitCode() {
        if (busy) return;

        String code = etCode.getText().toString().trim();
        if (code.length() != 6) {
            showError("Enter the 6-digit code from your email.");
            return;
        }

        showError(null);
        setBusy(true);
        hideKeyboard();

        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("code", code);
        } catch (Exception e) {
            setBusy(false);
            showError("Unexpected error building request.");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.VERIFY_EMAIL, body,
                response -> {
                    setBusy(false);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Email verified! You can sign in now.", Toast.LENGTH_LONG).show();
                        finish(); // Login is right underneath
                    } else {
                        showError(response.optString("message", "Verification failed."));
                    }
                },
                error -> {
                    setBusy(false);
                    etCode.setText("");
                    showError(errorMessage(error, "Verification failed. Please try again."));
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void resendCode() {
        showError(null);
        setResendEnabled(false);

        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
        } catch (Exception ignored) { }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.RESEND_VERIFICATION, body,
                response -> {
                    Toast.makeText(this, "A new code is on its way.", Toast.LENGTH_SHORT).show();
                    startCooldown(RESEND_COOLDOWN_MS);
                },
                error -> {
                    JSONObject json = errorJson(error);
                    if (json != null && json.has("retry_after")) {
                        // Server says a code was sent very recently: just wait it out
                        startCooldown(json.optInt("retry_after", 60) * 1000L);
                    } else {
                        setResendEnabled(true);
                    }
                    showError(errorMessage(error, "Could not send a new code. Please try again."));
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void startCooldown(long millis) {
        if (resendTimer != null) resendTimer.cancel();
        setResendEnabled(false);
        resendTimer = new CountDownTimer(millis, 1000) {
            @Override public void onTick(long left) {
                tvResend.setText("Resend code in " + (left / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                tvResend.setText("Resend code");
                setResendEnabled(true);
            }
        }.start();
    }

    private void setResendEnabled(boolean enabled) {
        if (enabled) tvResend.setText("Resend code");
        tvResend.setEnabled(enabled);
        tvResend.setAlpha(enabled ? 1f : 0.5f);
    }

    private JSONObject errorJson(VolleyError error) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                return new JSONObject(new String(error.networkResponse.data));
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String errorMessage(VolleyError error, String fallback) {
        JSONObject json = errorJson(error);
        if (json != null) return json.optString("message", fallback);
        return "Could not reach the server. Check your connection.";
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etCode.getWindowToken(), 0);
    }

    private void showError(String message) {
        if (message == null) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void setBusy(boolean isBusy) {
        busy = isBusy;
        progressBar.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!isBusy);
        etCode.setEnabled(!isBusy);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}