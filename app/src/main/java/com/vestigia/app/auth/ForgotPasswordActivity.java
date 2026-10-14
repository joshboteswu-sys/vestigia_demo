package com.vestigia.app.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_IDENTIFIER = "extra_identifier";
    private static final long RESEND_COOLDOWN_MS = 60_000;

    private View stepRequest, stepReset;
    private EditText etIdentifier, etCode, etNewPassword, etConfirmPassword;
    private ImageView ivToggleNew, ivToggleConfirm;
    private TextView tvTopTitle, tvIntro, tvSentInfo, tvError, tvResend;
    private Button btnPrimary;
    private ProgressBar progressBar;

    private boolean codeStep = false;
    private boolean busy = false;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        tvTopTitle = findViewById(R.id.tvTopTitle);
        tvIntro = findViewById(R.id.tvIntro);
        stepRequest = findViewById(R.id.stepRequest);
        stepReset = findViewById(R.id.stepReset);
        etIdentifier = findViewById(R.id.etIdentifier);
        etCode = findViewById(R.id.etCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivToggleNew = findViewById(R.id.ivToggleNew);
        ivToggleConfirm = findViewById(R.id.ivToggleConfirm);
        tvSentInfo = findViewById(R.id.tvSentInfo);
        tvResend = findViewById(R.id.tvResend);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
        btnPrimary = findViewById(R.id.btnPrimary);

        String prefill = getIntent().getStringExtra(EXTRA_IDENTIFIER);
        if (prefill != null) etIdentifier.setText(prefill);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        btnPrimary.setOnClickListener(v -> {
            if (busy) return;
            if (codeStep) resetPassword(); else requestCode(false);
        });
        tvResend.setOnClickListener(v -> {
            if (tvResend.isEnabled() && !busy) requestCode(true);
        });

        setupToggle(etNewPassword, ivToggleNew);
        setupToggle(etConfirmPassword, ivToggleConfirm);

        showStep(false);
    }

    private void showStep(boolean code) {
        codeStep = code;
        stepRequest.setVisibility(code ? View.GONE : View.VISIBLE);
        stepReset.setVisibility(code ? View.VISIBLE : View.GONE);
        tvTopTitle.setText(code ? "Reset password" : "Forgot password");
        tvIntro.setText(code
                ? "Enter the code we emailed you and choose a new password."
                : "Enter your email or Student ID and we'll email you a 6-digit code to reset your password.");
        btnPrimary.setText(code ? "Reset password" : "Send reset code");
        showError(null);
    }

    private void requestCode(boolean isResend) {
        String identifier = etIdentifier.getText().toString().trim();
        if (TextUtils.isEmpty(identifier)) {
            showError("Enter your email or Student ID.");
            return;
        }
        showError(null);
        setBusy(true);

        JSONObject body = new JSONObject();
        try {
            body.put("identifier", identifier);
        } catch (Exception ignored) { }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.FORGOT_PASSWORD, body,
                response -> {
                    setBusy(false);
                    if (!codeStep) showStep(true);
                    tvSentInfo.setText(response.optString("message",
                            "If an account matches, a 6-digit reset code has been sent."));
                    startCooldown(RESEND_COOLDOWN_MS);
                    if (isResend) Toast.makeText(this, "A new code is on its way.", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    setBusy(false);
                    showError(errorMessage(error, "Could not send a code. Please try again."));
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void resetPassword() {
        String identifier = etIdentifier.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String newPass = etNewPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (code.length() != 6) {
            showError("Enter the 6-digit code from your email.");
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
        setBusy(true);

        JSONObject body = new JSONObject();
        try {
            body.put("identifier", identifier);
            body.put("code", code);
            body.put("new_password", newPass);
        } catch (Exception e) {
            setBusy(false);
            showError("Unexpected error building request.");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.RESET_PASSWORD, body,
                response -> {
                    setBusy(false);
                    Toast.makeText(this, "Password updated. Please sign in.", Toast.LENGTH_LONG).show();
                    finish(); // Login is right underneath
                },
                error -> {
                    setBusy(false);
                    showError(errorMessage(error, "Could not reset your password. Please try again."));
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void setupToggle(EditText field, ImageView toggle) {
        toggle.setOnClickListener(v -> {
            boolean hidden = field.getTransformationMethod() instanceof PasswordTransformationMethod;
            field.setTransformationMethod(hidden
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            toggle.setImageResource(hidden ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
            field.setSelection(field.getText().length());
        });
    }

    private void startCooldown(long millis) {
        if (resendTimer != null) resendTimer.cancel();
        tvResend.setEnabled(false);
        tvResend.setAlpha(0.5f);
        resendTimer = new CountDownTimer(millis, 1000) {
            @Override public void onTick(long left) {
                tvResend.setText("Resend code in " + (left / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                tvResend.setText("Resend code");
                tvResend.setEnabled(true);
                tvResend.setAlpha(1f);
            }
        }.start();
    }

    private String errorMessage(VolleyError error, String fallback) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                return new JSONObject(new String(error.networkResponse.data)).optString("message", fallback);
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

    private void setBusy(boolean isBusy) {
        busy = isBusy;
        progressBar.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        btnPrimary.setEnabled(!isBusy);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}