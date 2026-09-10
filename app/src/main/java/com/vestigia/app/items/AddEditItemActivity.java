package com.vestigia.app.items;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

import java.util.Calendar;

// If launched with an "item_id" extra -> UPDATE mode (pre-fills the form).
// If launched without it -> CREATE mode (blank form).
public class AddEditItemActivity extends BaseProtectedActivity {

    private EditText etItemName, etCategory, etLocation, etDateFound, etDescription;
    private Button btnBack, btnSave;
    private TextView tvTitle, tvError;
    private ProgressBar progressBar;

    private int editingItemId = -1; // -1 means "create new"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        etItemName = findViewById(R.id.etItemName);
        etCategory = findViewById(R.id.etCategory);
        etLocation = findViewById(R.id.etLocation);
        etDateFound = findViewById(R.id.etDateFound);
        etDescription = findViewById(R.id.etDescription);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        tvTitle = findViewById(R.id.tvTitle);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);

        etDateFound.setOnClickListener(v -> showDatePicker());
        btnBack.setOnClickListener(v -> finish());

        editingItemId = getIntent().getIntExtra("item_id", -1);
        if (editingItemId != -1) {
            tvTitle.setText("Edit found item");
            prefillFromIntent();
        }

        btnSave.setOnClickListener(v -> {
            if (editingItemId == -1) {
                createItem();
            } else {
                updateItem();
            }
        });
    }

    // Simple approach: the previous screen (ItemDetailActivity) passes the
    // existing values as extras so we don't need an extra network call here.
    private void prefillFromIntent() {
        etItemName.setText(getIntent().getStringExtra("item_name"));
        etCategory.setText(getIntent().getStringExtra("category"));
        etLocation.setText(getIntent().getStringExtra("location"));
        etDateFound.setText(getIntent().getStringExtra("date_found"));
        etDescription.setText(getIntent().getStringExtra("description"));
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDateFound.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean validate() {
        if (TextUtils.isEmpty(etItemName.getText()) ||
                TextUtils.isEmpty(etLocation.getText()) ||
                TextUtils.isEmpty(etDateFound.getText())) {
            showError("Item name, location, and date found are required.");
            return false;
        }
        showError(null);
        return true;
    }

    private void createItem() {
        if (!validate()) return;
        setLoading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("item_name", etItemName.getText().toString().trim());
            body.put("category", etCategory.getText().toString().trim());
            body.put("location", etLocation.getText().toString().trim());
            body.put("date_found", etDateFound.getText().toString().trim());
            body.put("description", etDescription.getText().toString().trim());
        } catch (Exception e) {
            setLoading(false);
            showError("Unexpected error building request.");
            return;
        }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.ITEMS_CREATE, body, sessionManager.getToken(),
                response -> {
                    setLoading(false);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Item added successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showError(response.optString("message", "Could not add item."));
                    }
                },
                error -> {
                    setLoading(false);
                    showError("Could not reach the server. Check your connection.");
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void updateItem() {
        if (!validate()) return;
        setLoading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("id", editingItemId);
            body.put("item_name", etItemName.getText().toString().trim());
            body.put("category", etCategory.getText().toString().trim());
            body.put("location", etLocation.getText().toString().trim());
            body.put("date_found", etDateFound.getText().toString().trim());
            body.put("description", etDescription.getText().toString().trim());
            body.put("status", getIntent().getStringExtra("status") != null
                    ? getIntent().getStringExtra("status") : "Unclaimed");
        } catch (Exception e) {
            setLoading(false);
            showError("Unexpected error building request.");
            return;
        }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.ITEMS_UPDATE, body, sessionManager.getToken(),
                response -> {
                    setLoading(false);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Item updated successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showError(response.optString("message", "Could not update item."));
                    }
                },
                error -> {
                    setLoading(false);
                    showError("Could not reach the server. Check your connection.");
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
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
