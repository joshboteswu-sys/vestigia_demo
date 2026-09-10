package com.vestigia.app.items;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

public class ItemDetailActivity extends BaseProtectedActivity {

    private TextView tvItemName, tvStatus, tvLocationDate, tvDescription;
    private Button btnEdit, btnDelete;
    private android.widget.ProgressBar progressBar;

    private int itemId;
    private JSONObject currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        tvItemName = findViewById(R.id.tvItemName);
        tvStatus = findViewById(R.id.tvStatus);
        tvLocationDate = findViewById(R.id.tvLocationDate);
        tvDescription = findViewById(R.id.tvDescription);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);

        itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) {
            Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnEdit.setOnClickListener(v -> goToEdit());
        btnDelete.setOnClickListener(v -> confirmDelete());

        loadItem();
    }

    private void loadItem() {
        progressBar.setVisibility(View.VISIBLE);
        String url = ApiConfig.ITEMS_GET + "?id=" + itemId;

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, url, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        currentItem = response.getJSONObject("item");
                        tvItemName.setText(currentItem.getString("item_name"));
                        tvStatus.setText(currentItem.getString("status"));
                        tvLocationDate.setText(currentItem.getString("location") + "  •  " + currentItem.getString("date_found"));
                        tvDescription.setText(currentItem.optString("description", ""));
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read item details.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load item.", Toast.LENGTH_SHORT).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void goToEdit() {
        if (currentItem == null) return;
        Intent intent = new Intent(this, AddEditItemActivity.class);
        intent.putExtra("item_id", itemId);
        intent.putExtra("item_name", currentItem.optString("item_name"));
        intent.putExtra("category", currentItem.optString("category"));
        intent.putExtra("location", currentItem.optString("location"));
        intent.putExtra("date_found", currentItem.optString("date_found"));
        intent.putExtra("description", currentItem.optString("description"));
        intent.putExtra("status", currentItem.optString("status"));
        startActivity(intent);
    }

    // "Remove a selected record after showing a confirmation message"
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete item")
                .setMessage("Are you sure you want to delete \"" + tvItemName.getText() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem() {
        progressBar.setVisibility(View.VISIBLE);

        JSONObject body = new JSONObject();
        try {
            body.put("id", itemId);
        } catch (Exception ignored) { }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.ITEMS_DELETE, body, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Item deleted successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, response.optString("message", "Delete failed."), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not reach the server.", Toast.LENGTH_SHORT).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // no-op beyond BaseProtectedActivity's session check
    }
}
