package com.vestigia.app.items;

import static com.vestigia.app.items.DetailUi.str;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

public class LostItemDetailActivity extends BaseProtectedActivity {

    private TextView tvAddress, tvPostedBy, tvEmail, tvPhone, tvCategory, tvDate, tvStatus, tvDescription;
    private View rowEmail, rowPhone, rowManage;
    private Button btnBack, btnEdit, btnDelete, btnRecovered;
    private ProgressBar progressBar;

    private int itemId;
    private JSONObject currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_item_detail);

        ((TextView) findViewById(R.id.tvTopTitle)).setText("Lost Report");
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        tvAddress = findViewById(R.id.tvAddress);
        tvPostedBy = findViewById(R.id.tvPostedBy);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvCategory = findViewById(R.id.tvCategory);
        tvDate = findViewById(R.id.tvDate);
        tvStatus = findViewById(R.id.tvStatus);
        tvDescription = findViewById(R.id.tvDescription);
        rowEmail = findViewById(R.id.rowEmail);
        rowPhone = findViewById(R.id.rowPhone);
        rowManage = findViewById(R.id.rowManage);
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnRecovered = findViewById(R.id.btnRecovered);
        progressBar = findViewById(R.id.progressBar);

        itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) {
            Toast.makeText(this, "Report not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> goToEdit());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnRecovered.setOnClickListener(v -> confirmRecovered());
        rowEmail.setOnClickListener(v -> DetailUi.openEmail(this, tvEmail.getText().toString()));
        rowPhone.setOnClickListener(v -> DetailUi.openDial(this, tvPhone.getText().toString()));

        loadItem();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && itemId != -1) loadItem();
    }

    private void loadItem() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.LOST_GET + "?id=" + itemId, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        currentItem = response.getJSONObject("item");
                        bind(currentItem);
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

    private void bind(JSONObject item) {
        String poster = (str(item, "first_name") + " " + str(item, "last_name")).trim();
        String status = str(item, "status");

        DetailUi.bindHero(this, str(item, "item_name"), "LOST", R.drawable.bg_badge_lost,
                str(item, "created_at"), str(item, "category"), str(item, "image_url"));

        tvAddress.setText(str(item, "last_seen_location"));
        tvPostedBy.setText(poster.isEmpty() ? "Unknown" : poster);

        JSONObject contact = item.optJSONObject("contact"); // admins only
        DetailUi.setRow(rowEmail, tvEmail, contact == null ? "" : str(contact, "email"));
        DetailUi.setRow(rowPhone, tvPhone, contact == null ? "" : str(contact, "phone"));

        String category = str(item, "category");
        tvCategory.setText(category.isEmpty() ? "—" : category);
        tvDate.setText(DetailUi.friendlyDate(str(item, "date_lost")));
        tvStatus.setText(status);
        String description = str(item, "description");
        tvDescription.setText(description.isEmpty() ? "No description provided." : description);

        boolean canManage = sessionManager.canModify(item.optInt("reported_by", -1)); // reporter or admin
        rowManage.setVisibility(canManage ? View.VISIBLE : View.GONE);
        btnRecovered.setVisibility(canManage && status.equals("Active") ? View.VISIBLE : View.GONE);
    }

    private void confirmRecovered() {
        String name = currentItem != null ? str(currentItem, "item_name") : "this item";
        new AlertDialog.Builder(this)
                .setTitle("Mark as recovered")
                .setMessage("Did you get \"" + name + "\" back? The report will be closed and removed from the public lists.")
                .setPositiveButton("Yes, recovered", (d, w) -> markRecovered())
                .setNegativeButton("Not yet", null)
                .show();
    }

    private void markRecovered() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.LOST_RESOLVE, json("id", itemId), sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, response.optString("message", "Marked as recovered."), Toast.LENGTH_LONG).show();
                    loadItem();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, serverMessage(error, "Could not update the report."), Toast.LENGTH_LONG).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void goToEdit() {
        if (currentItem == null) return;
        Intent intent = new Intent(this, AddEditLostItemActivity.class);
        intent.putExtra("item_id", itemId);
        intent.putExtra("item_name", str(currentItem, "item_name"));
        intent.putExtra("category", str(currentItem, "category"));
        intent.putExtra("last_seen_location", str(currentItem, "last_seen_location"));
        intent.putExtra("date_lost", str(currentItem, "date_lost"));
        intent.putExtra("description", str(currentItem, "description"));
        intent.putExtra("status", str(currentItem, "status"));
        String imageUrl = str(currentItem, "image_url");
        intent.putExtra("image_url", imageUrl.isEmpty() ? null : imageUrl);
        startActivity(intent);
    }

    private void confirmDelete() {
        String name = currentItem != null ? str(currentItem, "item_name") : "this report";
        new AlertDialog.Builder(this)
                .setTitle("Delete report")
                .setMessage("Are you sure you want to delete \"" + name + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.LOST_DELETE, json("id", itemId), sessionManager.getToken(),
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
                    Toast.makeText(this, serverMessage(error, "Could not delete the report."), Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private JSONObject json(Object... keyValues) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i + 1 < keyValues.length; i += 2) o.put((String) keyValues[i], keyValues[i + 1]);
        } catch (Exception ignored) { }
        return o;
    }

    private String serverMessage(VolleyError error, String fallback) {
        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                return new JSONObject(new String(error.networkResponse.data)).optString("message", fallback);
            }
        } catch (Exception ignored) { }
        return "Could not reach the server.";
    }
}