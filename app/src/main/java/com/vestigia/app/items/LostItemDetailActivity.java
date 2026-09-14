package com.vestigia.app.items;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

public class LostItemDetailActivity extends BaseProtectedActivity {

    private TextView tvItemName, tvStatus, tvLocationDate, tvDescription, tvNotYours;
    private ImageView ivPhoto;
    private Button btnEdit, btnDelete;
    private android.widget.ProgressBar progressBar;

    private int itemId;
    private JSONObject currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_item_detail);

        tvItemName = findViewById(R.id.tvItemName);
        tvStatus = findViewById(R.id.tvStatus);
        tvLocationDate = findViewById(R.id.tvLocationDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvNotYours = findViewById(R.id.tvNotYours);
        ivPhoto = findViewById(R.id.ivPhoto);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);

        itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) {
            Toast.makeText(this, "Report not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnEdit.setOnClickListener(v -> goToEdit());
        btnDelete.setOnClickListener(v -> confirmDelete());

        loadItem();
    }

    private void loadItem() {
        progressBar.setVisibility(View.VISIBLE);
        String url = ApiConfig.LOST_GET + "?id=" + itemId;

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, url, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        currentItem = response.getJSONObject("item");
                        tvItemName.setText(currentItem.getString("item_name"));
                        tvStatus.setText(currentItem.getString("status"));
                        tvLocationDate.setText(currentItem.getString("last_seen_location") + "  •  " + currentItem.getString("date_lost"));
                        tvDescription.setText(currentItem.optString("description", ""));

                        String imageUrl = currentItem.isNull("image_url") ? null : currentItem.optString("image_url", null);
                        if (imageUrl != null) {
                            Glide.with(this).load(imageUrl)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .centerCrop().into(ivPhoto);
                        } else {
                            ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                        }

                        // ---- Permission restriction: only the reporter (or admin) can edit/delete ----
                        int reportedBy = currentItem.optInt("reported_by", -1);
                        boolean canModify = sessionManager.canModify(reportedBy);
                        btnEdit.setVisibility(canModify ? View.VISIBLE : View.GONE);
                        btnDelete.setVisibility(canModify ? View.VISIBLE : View.GONE);
                        tvNotYours.setVisibility(canModify ? View.GONE : View.VISIBLE);

                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read report details.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load report.", Toast.LENGTH_SHORT).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void goToEdit() {
        if (currentItem == null) return;
        Intent intent = new Intent(this, AddEditLostItemActivity.class);
        intent.putExtra("item_id", itemId);
        intent.putExtra("item_name", currentItem.optString("item_name"));
        intent.putExtra("category", currentItem.optString("category"));
        intent.putExtra("last_seen_location", currentItem.optString("last_seen_location"));
        intent.putExtra("date_lost", currentItem.optString("date_lost"));
        intent.putExtra("description", currentItem.optString("description"));
        intent.putExtra("status", currentItem.optString("status"));
        intent.putExtra("image_url", currentItem.isNull("image_url") ? null : currentItem.optString("image_url", null));
        startActivity(intent);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete report")
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
                Request.Method.POST, ApiConfig.LOST_DELETE, body, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Report deleted successfully.", Toast.LENGTH_SHORT).show();
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
}