package com.vestigia.app.items;

import static com.vestigia.app.items.DetailUi.str;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class ItemDetailActivity extends BaseProtectedActivity {

    private TextView tvAddress, tvPostedBy, tvCustody, tvCustodyNote, tvEmail, tvPhone;
    private TextView tvCategory, tvDate, tvStatus, tvDescription;
    private View rowCustodyNote, rowEmail, rowPhone, rowManage;
    private Button btnReceiveCustody, btnDispose, btnClaim, btnBack, btnEdit, btnDelete;
    private ProgressBar progressBar;

    private int itemId;
    private int pendingClaimId = -1; // set when the viewer already has a pending claim on this item
    private JSONObject currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        ((TextView) findViewById(R.id.tvTopTitle)).setText("Item Detail");
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        tvAddress = findViewById(R.id.tvAddress);
        tvPostedBy = findViewById(R.id.tvPostedBy);
        tvCustody = findViewById(R.id.tvCustody);
        tvCustodyNote = findViewById(R.id.tvCustodyNote);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvCategory = findViewById(R.id.tvCategory);
        tvDate = findViewById(R.id.tvDate);
        tvStatus = findViewById(R.id.tvStatus);
        tvDescription = findViewById(R.id.tvDescription);
        rowCustodyNote = findViewById(R.id.rowCustodyNote);
        rowEmail = findViewById(R.id.rowEmail);
        rowPhone = findViewById(R.id.rowPhone);
        rowManage = findViewById(R.id.rowManage);
        btnReceiveCustody = findViewById(R.id.btnReceiveCustody);
        btnDispose = findViewById(R.id.btnDispose);
        btnClaim = findViewById(R.id.btnClaim);
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);

        itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) {
            Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnClaim.setOnClickListener(v -> {
            if (pendingClaimId != -1) openClaimDetail(pendingClaimId); else goToClaim();
        });
        btnEdit.setOnClickListener(v -> goToEdit());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnReceiveCustody.setOnClickListener(v -> showReceiveDialog());
        btnDispose.setOnClickListener(v -> showDisposeDialog());
        rowEmail.setOnClickListener(v -> DetailUi.openEmail(this, tvEmail.getText().toString()));
        rowPhone.setOnClickListener(v -> DetailUi.openDial(this, tvPhone.getText().toString()));

        loadItem();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && itemId != -1) {
            loadItem(); // refresh in case a claim or custody change happened elsewhere
        }
    }

    private void loadItem() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.ITEMS_GET + "?id=" + itemId, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        currentItem = response.getJSONObject("item");
                        bind(currentItem);
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

    private void bind(JSONObject item) {
        String status = str(item, "status");
        String holder = str(item, "custody_holder");
        String poster = (str(item, "first_name") + " " + str(item, "last_name")).trim();
        String custodian = (str(item, "custodian_first_name") + " " + str(item, "custodian_last_name")).trim();

        DetailUi.bindHero(this, str(item, "item_name"), "FOUND", R.drawable.bg_badge_found,
                str(item, "created_at"), str(item, "category"), str(item, "image_url"));

        tvAddress.setText(str(item, "location"));
        tvPostedBy.setText(poster.isEmpty() ? "Unknown" : poster);

        // ---- Custody ----
        String custodyText;
        if ("Admin".equals(holder)) {
            custodyText = "Admin Office" + (custodian.isEmpty() ? "" : " (received by " + custodian + ")");
        } else {
            String who = !custodian.isEmpty() ? custodian : poster;
            custodyText = who.isEmpty() ? "The finder" : who + " (the finder)";
        }
        tvCustody.setText(custodyText);
        DetailUi.setRow(rowCustodyNote, tvCustodyNote, str(item, "custody_note"));

        // ---- Contact (the server only sends it to people allowed to see it) ----
        JSONObject contact = item.optJSONObject("contact");
        DetailUi.setRow(rowEmail, tvEmail, contact == null ? "" : str(contact, "email"));
        DetailUi.setRow(rowPhone, tvPhone, contact == null ? "" : str(contact, "phone"));

        // ---- Item details ----
        String category = str(item, "category");
        tvCategory.setText(category.isEmpty() ? "—" : category);
        tvDate.setText(DetailUi.friendlyDate(str(item, "date_found")));
        tvStatus.setText(status);
        String description = str(item, "description");
        tvDescription.setText(description.isEmpty() ? "No description provided." : description);

        // ---- Buttons ----
        boolean canManage = sessionManager.canModify(item.optInt("reported_by", -1)); // reporter or admin
        rowManage.setVisibility(canManage ? View.VISIBLE : View.GONE);

        pendingClaimId = -1;
        boolean canClaim = status.equals("Active") && !canManage;
        btnClaim.setVisibility(canClaim ? View.VISIBLE : View.GONE);
        if (canClaim) {
            btnClaim.setText("Claim Item");
            btnClaim.setEnabled(true);
            checkExistingClaim();
        }

        boolean withFinder = "Finder".equals(holder);
        boolean closed = status.equals("Returned") || status.equals("Disposed");
        btnReceiveCustody.setVisibility(sessionManager.isAdmin() && withFinder && !closed ? View.VISIBLE : View.GONE);
        btnDispose.setVisibility(sessionManager.isAdmin() && status.equals("Active") ? View.VISIBLE : View.GONE);
    }

    private void checkExistingClaim() {
        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.CLAIMS_STATUS + "?item_id=" + itemId, null, sessionManager.getToken(),
                response -> {
                    JSONObject claim = response.optJSONObject("claim");
                    if (claim == null) return;
                    // A pending claim opens its detail page (where it can be withdrawn).
                    // A rejected or withdrawn claim leaves the button on "Claim Item" so the user can try again.
                    if (claim.optString("status", "").equals("Pending Verification")) {
                        pendingClaimId = claim.optInt("id", -1);
                        btnClaim.setText("View my pending claim");
                        btnClaim.setEnabled(true);
                    }
                },
                error -> { /* the button just keeps its default state */ }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void openClaimDetail(int claimId) {
        Intent intent = new Intent(this, ClaimDetailActivity.class);
        intent.putExtra("claim_id", claimId);
        startActivity(intent);
    }

    // ---------------- Custody ----------------

    private void showReceiveDialog() {
        EditText input = new EditText(this);
        input.setHint("Where is it kept now? (optional)");

        new AlertDialog.Builder(this)
                .setTitle("Receive into Admin custody")
                .setMessage("Confirm that the Admin Office now has this item. The finder will be notified.")
                .setView(input)
                .setPositiveButton("Confirm", (d, w) -> receiveIntoCustody(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void receiveIntoCustody(String note) {
        post(ApiConfig.CUSTODY_RECEIVE, json("item_id", itemId, "note", note), "Item received.");
    }

    // ---------------- Disposal (admin) ----------------

    private void showDisposeDialog() {
        EditText input = new EditText(this);
        input.setHint("Reason (optional)");

        new AlertDialog.Builder(this)
                .setTitle("Mark as disposed")
                .setMessage("Use this for an item that was never claimed. Any pending claims will be rejected and the finder will be told. This can't be undone.")
                .setView(input)
                .setPositiveButton("Mark as disposed", (d, w) ->
                        post(ApiConfig.ITEMS_DISPOSE, json("item_id", itemId, "note", input.getText().toString().trim()),
                                "Item marked as disposed."))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Shared POST: shows the server's message, then reloads the screen
    private void post(String url, JSONObject body, String fallback) {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, url, body, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, response.optString("message", fallback), Toast.LENGTH_SHORT).show();
                    loadItem();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, serverMessage(error, "That action failed."), Toast.LENGTH_LONG).show();
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

    // ---------------- Edit / claim / delete ----------------

    private void goToEdit() {
        if (currentItem == null) return;
        Intent intent = new Intent(this, AddEditItemActivity.class);
        intent.putExtra("item_id", itemId);
        intent.putExtra("item_name", str(currentItem, "item_name"));
        intent.putExtra("category", str(currentItem, "category"));
        intent.putExtra("location", str(currentItem, "location"));
        intent.putExtra("date_found", str(currentItem, "date_found"));
        intent.putExtra("description", str(currentItem, "description"));
        intent.putExtra("status", str(currentItem, "status"));
        String imageUrl = str(currentItem, "image_url");
        intent.putExtra("image_url", imageUrl.isEmpty() ? null : imageUrl);
        startActivity(intent);
    }

    private void goToClaim() {
        if (currentItem == null) return;
        Intent intent = new Intent(this, ClaimItemActivity.class);
        intent.putExtra("item_id", itemId);
        intent.putExtra("item_name", str(currentItem, "item_name"));
        intent.putExtra("location", str(currentItem, "location"));
        intent.putExtra("date_found", str(currentItem, "date_found"));
        startActivity(intent);
    }

    private void confirmDelete() {
        String name = currentItem != null ? str(currentItem, "item_name") : "this item";
        new AlertDialog.Builder(this)
                .setTitle("Delete item")
                .setMessage("Are you sure you want to delete \"" + name + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.ITEMS_DELETE, json("id", itemId), sessionManager.getToken(),
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
                    Toast.makeText(this, serverMessage(error, "Could not delete the item."), Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}