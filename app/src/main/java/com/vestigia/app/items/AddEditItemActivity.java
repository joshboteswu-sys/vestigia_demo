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
import android.widget.RadioGroup;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Launched with an "item_id" extra -> UPDATE mode (pre-filled form).
// Launched without it -> CREATE mode (blank form, with the custody choice).
public class AddEditItemActivity extends BaseProtectedActivity {

    private EditText etItemName, etCategory, etLocation, etDescription, etCustodyNote;
    private TextView tvTopTitle, tvDate, tvError;
    private ImageView ivBack, ivPhoto;
    private View photoSlot, photoPlaceholder, rowDate, custodySection;
    private RadioGroup rgCustody;
    private Button btnSave;
    private ProgressBar progressBar;

    private int editingItemId = -1; // -1 means "create new"
    private Uri selectedImageUri = null;
    private String selectedDateIso = ""; // yyyy-MM-dd, the format the server expects

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        ivBack = findViewById(R.id.ivBack);
        tvTopTitle = findViewById(R.id.tvTopTitle);
        photoSlot = findViewById(R.id.photoSlot);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);
        ivPhoto = findViewById(R.id.ivPhoto);
        etItemName = findViewById(R.id.etItemName);
        etCategory = findViewById(R.id.etCategory);
        rowDate = findViewById(R.id.rowDate);
        tvDate = findViewById(R.id.tvDate);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        custodySection = findViewById(R.id.custodySection);
        rgCustody = findViewById(R.id.rgCustody);
        etCustodyNote = findViewById(R.id.etCustodyNote);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                photoPlaceholder.setVisibility(View.GONE);
                ivPhoto.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).centerCrop().into(ivPhoto);
            }
        });

        photoSlot.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        rowDate.setOnClickListener(v -> showDatePicker());
        ivBack.setOnClickListener(v -> finish());

        editingItemId = getIntent().getIntExtra("item_id", -1);
        if (editingItemId != -1) {
            tvTopTitle.setText("Edit found item");
            btnSave.setText("Save changes");
            prefillFromIntent();
            custodySection.setVisibility(View.GONE); // custody changes only through the admin's "Receive" action
        } else {
            tvTopTitle.setText("Report found item");
        }

        btnSave.setOnClickListener(v ->
                saveItem(editingItemId == -1 ? ApiConfig.ITEMS_CREATE : ApiConfig.ITEMS_UPDATE, editingItemId != -1));
    }

    private void prefillFromIntent() {
        etItemName.setText(getIntent().getStringExtra("item_name"));
        etCategory.setText(getIntent().getStringExtra("category"));
        etLocation.setText(getIntent().getStringExtra("location"));
        etDescription.setText(getIntent().getStringExtra("description"));
        setDateFromIso(getIntent().getStringExtra("date_found"));

        String imageUrl = getIntent().getStringExtra("image_url");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            photoPlaceholder.setVisibility(View.GONE);
            ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUrl).centerCrop().into(ivPhoto);
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        if (selectedDateIso.length() >= 10) {
            c.set(Integer.parseInt(selectedDateIso.substring(0, 4)),
                    Integer.parseInt(selectedDateIso.substring(5, 7)) - 1,
                    Integer.parseInt(selectedDateIso.substring(8, 10)));
        }
        new DatePickerDialog(this, (view, year, month, day) -> setDate(year, month, day),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setDate(int year, int month0, int day) {
        selectedDateIso = String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, day);
        Calendar c = Calendar.getInstance();
        c.set(year, month0, day);
        tvDate.setText(new SimpleDateFormat("MMM d, yyyy", Locale.US).format(c.getTime()));
    }

    private void setDateFromIso(String iso) {
        if (iso == null || !iso.matches("\\d{4}-\\d{2}-\\d{2}.*")) return;
        setDate(Integer.parseInt(iso.substring(0, 4)),
                Integer.parseInt(iso.substring(5, 7)) - 1,
                Integer.parseInt(iso.substring(8, 10)));
    }

    private boolean validate() {
        if (TextUtils.isEmpty(etItemName.getText().toString().trim())
                || TextUtils.isEmpty(etLocation.getText().toString().trim())
                || selectedDateIso.isEmpty()) {
            showError("Item name, location, and date found are required.");
            return false;
        }
        showError(null);
        return true;
    }

    private void saveItem(String url, boolean isUpdate) {
        if (!validate()) return;
        setLoading(true);

        Map<String, String> params = new HashMap<>();
        if (isUpdate) {
            params.put("id", String.valueOf(editingItemId));
            String status = getIntent().getStringExtra("status");
            params.put("status", status != null ? status : "Active");
        }
        params.put("item_name", etItemName.getText().toString().trim());
        params.put("category", etCategory.getText().toString().trim());
        params.put("location", etLocation.getText().toString().trim());
        params.put("date_found", selectedDateIso);
        params.put("description", etDescription.getText().toString().trim());

        if (!isUpdate) {
            params.put("custody_holder",
                    rgCustody.getCheckedRadioButtonId() == R.id.rbAdminOffice ? "Admin" : "Finder");
            params.put("custody_note", etCustodyNote.getText().toString().trim());
        }

        // Heavy image decoding stays off the main thread
        new Thread(() -> {
            byte[] imageBytes = null;
            if (selectedImageUri != null) {
                try {
                    imageBytes = readBytesFromUri(selectedImageUri);
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Could not read selected photo, saving without it.", Toast.LENGTH_SHORT).show());
                }
            }
            byte[] finalImageBytes = imageBytes;

            runOnUiThread(() -> {
                VolleyMultipartRequest request = new VolleyMultipartRequest(
                        url, sessionManager.getToken(), params,
                        response -> {
                            setLoading(false);
                            if (response.optBoolean("success", false)) {
                                Toast.makeText(this, isUpdate ? "Item updated successfully." : "Item added successfully.",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                showError(response.optString("message", "Could not save item."));
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

    // Background thread only — never touch UI here
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