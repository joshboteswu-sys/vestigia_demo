package com.vestigia.app.items;

import android.app.DatePickerDialog;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddEditLostItemActivity extends BaseProtectedActivity {

    private EditText etItemName, etCategory, etLocation, etDateLost, etDescription;
    private Button btnBack, btnSave, btnSelectPhoto;
    private ImageView ivPhoto;
    private TextView tvTitle, tvError;
    private ProgressBar progressBar;

    private int editingItemId = -1;
    private Uri selectedImageUri = null;
    private String existingImageUrl = null;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_lost_item);

        etItemName = findViewById(R.id.etItemName);
        etCategory = findViewById(R.id.etCategory);
        etLocation = findViewById(R.id.etLocation);
        etDateLost = findViewById(R.id.etDateFound); // shared layout, reused ID
        etDescription = findViewById(R.id.etDescription);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        ivPhoto = findViewById(R.id.ivPhoto);
        tvTitle = findViewById(R.id.tvTitle);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                Glide.with(this).load(uri).centerCrop().into(ivPhoto);
            }
        });

        btnSelectPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        ivPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        etDateLost.setOnClickListener(v -> showDatePicker());
        btnBack.setOnClickListener(v -> finish());

        tvTitle.setText("Report a lost item");

        editingItemId = getIntent().getIntExtra("item_id", -1);
        if (editingItemId != -1) {
            tvTitle.setText("Edit lost report");
            prefillFromIntent();
        }

        btnSave.setOnClickListener(v -> {
            if (editingItemId == -1) {
                saveItem(ApiConfig.LOST_CREATE, false);
            } else {
                saveItem(ApiConfig.LOST_UPDATE, true);
            }
        });
    }

    private void prefillFromIntent() {
        etItemName.setText(getIntent().getStringExtra("item_name"));
        etCategory.setText(getIntent().getStringExtra("category"));
        etLocation.setText(getIntent().getStringExtra("last_seen_location"));
        etDateLost.setText(getIntent().getStringExtra("date_lost"));
        etDescription.setText(getIntent().getStringExtra("description"));
        existingImageUrl = getIntent().getStringExtra("image_url");
        if (existingImageUrl != null) {
            Glide.with(this).load(existingImageUrl).centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery).into(ivPhoto);
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDateLost.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean validate() {
        if (TextUtils.isEmpty(etItemName.getText()) ||
                TextUtils.isEmpty(etLocation.getText()) ||
                TextUtils.isEmpty(etDateLost.getText())) {
            showError("Item name, last seen location, and date lost are required.");
            return false;
        }
        showError(null);
        return true;
    }

    // Same pattern as AddEditItemActivity: image decoding happens off the
    // main thread so the UI never freezes during submit.
    private void saveItem(String url, boolean isUpdate) {
        if (!validate()) return;
        setLoading(true);

        Map<String, String> params = new HashMap<>();
        if (isUpdate) {
            params.put("id", String.valueOf(editingItemId));
            params.put("status", getIntent().getStringExtra("status") != null
                    ? getIntent().getStringExtra("status") : "Active");
        }
        params.put("item_name", etItemName.getText().toString().trim());
        params.put("category", etCategory.getText().toString().trim());
        params.put("last_seen_location", etLocation.getText().toString().trim());
        params.put("date_lost", etDateLost.getText().toString().trim());
        params.put("description", etDescription.getText().toString().trim());

        // ---- Background thread: heavy image decode/compress happens here ----
        new Thread(() -> {
            byte[] imageBytes = null;
            if (selectedImageUri != null) {
                try {
                    imageBytes = readBytesFromUri(selectedImageUri);
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Could not read selected photo, saving without it.", Toast.LENGTH_SHORT).show());
                }
            }

            byte[] finalImageBytes = imageBytes;

            // ---- Back on the UI thread: build and fire the actual request ----
            runOnUiThread(() -> {
                VolleyMultipartRequest request = new VolleyMultipartRequest(
                        url, sessionManager.getToken(), params,
                        response -> {
                            setLoading(false);
                            if (response.optBoolean("success", false)) {
                                Toast.makeText(this, isUpdate ? "Lost report updated." : "Lost report submitted.", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                showError(response.optString("message", "Could not save report."));
                            }
                        },
                        error -> {
                            setLoading(false);
                            showError("Could not reach the server. Check your connection.");
                        }
                );

                if (finalImageBytes != null) {
                    request.setFile("image", finalImageBytes, "photo.jpg", "image/jpeg");
                }

                VolleySingleton.getInstance(this).addToRequestQueue(request);
            });
        }).start();
    }

    // Called from a background thread — must never touch UI here.
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
        btnSave.setEnabled(!loading);
    }
}