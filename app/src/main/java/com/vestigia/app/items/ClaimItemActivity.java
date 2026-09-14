package com.vestigia.app.items;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.VolleyMultipartRequest;
import com.vestigia.app.network.VolleySingleton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Matches the Figma "Submit proof of ownership" screen.
// Launched from ItemDetailActivity with item_id, item_name, location, date_found extras.
public class ClaimItemActivity extends BaseProtectedActivity {

    private TextView tvItemName, tvItemMeta, tvError;
    private EditText etProofDescription;
    private ImageView ivProofPhoto;
    private Button btnBack, btnSubmit, btnSelectPhoto;
    private ProgressBar progressBar;

    private int itemId;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim_item);

        tvItemName = findViewById(R.id.tvItemName);
        tvItemMeta = findViewById(R.id.tvItemMeta);
        tvError = findViewById(R.id.tvError);
        etProofDescription = findViewById(R.id.etProofDescription);
        ivProofPhoto = findViewById(R.id.ivProofPhoto);
        btnBack = findViewById(R.id.btnBack);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        progressBar = findViewById(R.id.progressBar);

        itemId = getIntent().getIntExtra("item_id", -1);
        tvItemName.setText(getIntent().getStringExtra("item_name"));
        String location = getIntent().getStringExtra("location");
        String dateFound = getIntent().getStringExtra("date_found");
        tvItemMeta.setText(location + "  •  Found " + dateFound);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                Glide.with(this).load(uri).centerCrop().into(ivProofPhoto);
            }
        });

        btnSelectPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        ivProofPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitClaim());
    }

    private void submitClaim() {
        String proof = etProofDescription.getText().toString().trim();
        if (TextUtils.isEmpty(proof)) {
            showError("Please describe your proof of ownership.");
            return;
        }
        showError(null);
        setLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("item_id", String.valueOf(itemId));
        params.put("proof_description", proof);

        VolleyMultipartRequest request = new VolleyMultipartRequest(
                ApiConfig.CLAIMS_CREATE, sessionManager.getToken(), params,
                response -> {
                    setLoading(false);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, response.optString("message", "Claim submitted."), Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        showError(response.optString("message", "Could not submit claim."));
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
                request.setFile("proof_photo", bytes, "proof.jpg", "image/jpeg");
            } catch (Exception e) {
                Toast.makeText(this, "Could not read selected photo, submitting without it.", Toast.LENGTH_SHORT).show();
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

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
        btnSubmit.setEnabled(!loading);
    }
}