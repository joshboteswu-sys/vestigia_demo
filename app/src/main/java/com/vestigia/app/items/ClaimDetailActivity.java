package com.vestigia.app.items;

import static com.vestigia.app.items.DetailUi.str;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONObject;

// Opened with a "claim_id" extra. What the viewer may see and do is decided by the server.
public class ClaimDetailActivity extends BaseProtectedActivity {

    private int claimId;
    private ProgressBar progressBar;

    private TextView tvClaimant, tvClaimEmail, tvClaimPhone, tvProof, tvClaimStatus, tvReviewer, tvDecisionDate, tvFeedback;
    private TextView tvReceipt, tvPickup, tvHandedBy, tvHandoverNotes;
    private TextView tvItemLocation, tvCustody, tvCustodyNote, tvHolderEmail, tvHolderPhone;
    private View rowClaimEmail, rowClaimPhone, proofPhotoWrap, rowReviewer, rowDecisionDate, rowFeedback;
    private View cardHandover, rowHandoverNotes, rowCustodyNote, rowHolderEmail, rowHolderPhone, rowDecisionButtons;
    private ImageView ivProof;
    private Button btnApprove, btnReject, btnConfirmHandover, btnPrintReceipt, btnWithdraw, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim_detail);

        ((TextView) findViewById(R.id.tvTopTitle)).setText("Claim Detail");
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        tvClaimant = findViewById(R.id.tvClaimant);
        tvClaimEmail = findViewById(R.id.tvClaimEmail);
        tvClaimPhone = findViewById(R.id.tvClaimPhone);
        tvProof = findViewById(R.id.tvProof);
        tvClaimStatus = findViewById(R.id.tvClaimStatus);
        tvReviewer = findViewById(R.id.tvReviewer);
        tvDecisionDate = findViewById(R.id.tvDecisionDate);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvReceipt = findViewById(R.id.tvReceipt);
        tvPickup = findViewById(R.id.tvPickup);
        tvHandedBy = findViewById(R.id.tvHandedBy);
        tvHandoverNotes = findViewById(R.id.tvHandoverNotes);
        tvItemLocation = findViewById(R.id.tvItemLocation);
        tvCustody = findViewById(R.id.tvCustody);
        tvCustodyNote = findViewById(R.id.tvCustodyNote);
        tvHolderEmail = findViewById(R.id.tvHolderEmail);
        tvHolderPhone = findViewById(R.id.tvHolderPhone);
        rowClaimEmail = findViewById(R.id.rowClaimEmail);
        rowClaimPhone = findViewById(R.id.rowClaimPhone);
        proofPhotoWrap = findViewById(R.id.proofPhotoWrap);
        rowReviewer = findViewById(R.id.rowReviewer);
        rowDecisionDate = findViewById(R.id.rowDecisionDate);
        rowFeedback = findViewById(R.id.rowFeedback);
        cardHandover = findViewById(R.id.cardHandover);
        rowHandoverNotes = findViewById(R.id.rowHandoverNotes);
        rowCustodyNote = findViewById(R.id.rowCustodyNote);
        rowHolderEmail = findViewById(R.id.rowHolderEmail);
        rowHolderPhone = findViewById(R.id.rowHolderPhone);
        rowDecisionButtons = findViewById(R.id.rowDecisionButtons);
        ivProof = findViewById(R.id.ivProof);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnConfirmHandover = findViewById(R.id.btnConfirmHandover);
        btnPrintReceipt = findViewById(R.id.btnPrintReceipt);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnBack = findViewById(R.id.btnBack);

        claimId = getIntent().getIntExtra("claim_id", -1);
        if (claimId == -1) {
            Toast.makeText(this, "Claim not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnApprove.setOnClickListener(v -> confirmApprove());
        btnReject.setOnClickListener(v -> showRejectDialog());
        btnConfirmHandover.setOnClickListener(v -> showHandoverDialog());
        btnWithdraw.setOnClickListener(v -> confirmWithdraw());
        btnPrintReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReceiptActivity.class);
            intent.putExtra("claim_id", claimId);
            startActivity(intent);
        });
        rowClaimEmail.setOnClickListener(v -> DetailUi.openEmail(this, tvClaimEmail.getText().toString()));
        rowClaimPhone.setOnClickListener(v -> DetailUi.openDial(this, tvClaimPhone.getText().toString()));
        rowHolderEmail.setOnClickListener(v -> DetailUi.openEmail(this, tvHolderEmail.getText().toString()));
        rowHolderPhone.setOnClickListener(v -> DetailUi.openDial(this, tvHolderPhone.getText().toString()));

        load();
    }

    private void load() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.CLAIMS_GET + "?id=" + claimId, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        bind(response);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read claim details.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, serverMessage(error, "Could not load the claim."), Toast.LENGTH_LONG).show();
                    if (error.networkResponse != null && error.networkResponse.statusCode == 403) finish();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void bind(JSONObject r) throws Exception {
        JSONObject claim = r.getJSONObject("claim");
        JSONObject item = r.getJSONObject("item");
        JSONObject claimant = r.getJSONObject("claimant");
        JSONObject custody = r.getJSONObject("custody");
        JSONObject viewer = r.getJSONObject("viewer");
        JSONObject handover = r.optJSONObject("handover");

        String status = str(claim, "status");

        // ---- Hero ----
        DetailUi.bindHero(this, str(item, "item_name"), badgeText(status), badgeBackground(status),
                str(claim, "claimed_at"), str(item, "category"), str(item, "image_url"));

        // ---- Claimant ----
        tvClaimant.setText(str(claimant, "name"));
        DetailUi.setRow(rowClaimEmail, tvClaimEmail, str(claimant, "email"));
        DetailUi.setRow(rowClaimPhone, tvClaimPhone, str(claimant, "phone"));

        // ---- Proof ----
        String proof = str(claim, "proof_description");
        tvProof.setText(proof.isEmpty() ? "No description given." : proof);
        String proofUrl = str(claim, "proof_photo_url");
        if (!proofUrl.isEmpty() && !isFinishing() && !isDestroyed()) {
            proofPhotoWrap.setVisibility(View.VISIBLE);
            Glide.with(this).load(proofUrl).centerCrop().into(ivProof);
        } else {
            proofPhotoWrap.setVisibility(View.GONE);
        }

        // ---- Review ----
        tvClaimStatus.setText(status);
        String reviewer = str(claim, "reviewer_name");
        rowReviewer.setVisibility(reviewer.isEmpty() ? View.GONE : View.VISIBLE);
        tvReviewer.setText(reviewer);
        String decided = str(claim, "decision_date");
        rowDecisionDate.setVisibility(decided.isEmpty() ? View.GONE : View.VISIBLE);
        tvDecisionDate.setText(DetailUi.friendlyDateTime(decided));
        DetailUi.setRow(rowFeedback, tvFeedback, str(claim, "rejection_feedback"));

        // ---- Handover ----
        if (handover != null) {
            cardHandover.setVisibility(View.VISIBLE);
            tvReceipt.setText(str(handover, "receipt_no"));
            tvPickup.setText(DetailUi.friendlyDateTime(str(handover, "pickup_datetime")));
            tvHandedBy.setText(str(handover, "handed_by"));
            DetailUi.setRow(rowHandoverNotes, tvHandoverNotes, str(handover, "notes"));
        } else {
            cardHandover.setVisibility(View.GONE);
        }

        // ---- Item & custody ----
        tvItemLocation.setText(str(item, "location"));
        String holderName = str(custody, "name");
        if ("Admin".equals(str(custody, "holder"))) {
            tvCustody.setText("Admin Office" + (holderName.isEmpty() ? "" : " (received by " + holderName + ")"));
        } else {
            tvCustody.setText(holderName.isEmpty() ? "The finder" : holderName + " (the finder)");
        }
        DetailUi.setRow(rowCustodyNote, tvCustodyNote, str(custody, "note"));
        DetailUi.setRow(rowHolderEmail, tvHolderEmail, str(custody, "email"));
        DetailUi.setRow(rowHolderPhone, tvHolderPhone, str(custody, "phone"));

        // ---- Buttons ----
        boolean admin = sessionManager.isAdmin();
        boolean pending = status.equals("Pending Verification");
        boolean approved = status.equals("Approved");
        rowDecisionButtons.setVisibility(admin && pending ? View.VISIBLE : View.GONE);
        btnConfirmHandover.setVisibility(approved && (admin || viewer.optBoolean("is_custodian")) ? View.VISIBLE : View.GONE);
        btnWithdraw.setVisibility(pending && viewer.optBoolean("is_claimant") ? View.VISIBLE : View.GONE);
        btnPrintReceipt.setVisibility(handover != null ? View.VISIBLE : View.GONE);
    }

    private String badgeText(String status) {
        switch (status) {
            case "Pending Verification": return "PENDING";
            case "Approved": return "APPROVED";
            case "Rejected": return "REJECTED";
            case "Completed": return "COMPLETED";
            case "Withdrawn": return "WITHDRAWN";
            default: return status.toUpperCase();
        }
    }

    private int badgeBackground(String status) {
        switch (status) {
            case "Pending Verification": return R.drawable.bg_badge_pending;
            case "Rejected": return R.drawable.bg_badge_lost;
            case "Withdrawn": return R.drawable.bg_badge_gray;
            default: return R.drawable.bg_badge_found;
        }
    }

    // ---------------- Actions ----------------

    private void confirmApprove() {
        new AlertDialog.Builder(this)
                .setTitle("Approve claim")
                .setMessage("The claimant will be notified, and any other pending claims on this item will be rejected.")
                .setPositiveButton("Approve", (d, w) -> post(ApiConfig.CLAIMS_APPROVE, json("claim_id", claimId), "Claim approved."))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRejectDialog() {
        EditText input = new EditText(this);
        input.setHint("Reason for rejection (shown to the claimant)");

        new AlertDialog.Builder(this)
                .setTitle("Reject claim")
                .setView(input)
                .setPositiveButton("Reject", (d, w) -> post(ApiConfig.CLAIMS_REJECT,
                        json("claim_id", claimId, "rejection_feedback", input.getText().toString().trim()), "Claim rejected."))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHandoverDialog() {
        EditText input = new EditText(this);
        input.setHint("Verification notes (e.g. ID checked)");

        new AlertDialog.Builder(this)
                .setTitle("Confirm handover")
                .setMessage("Confirm that the claimant has physically received this item.")
                .setView(input)
                .setPositiveButton("Confirm", (d, w) -> post(ApiConfig.HANDOVERS_CREATE,
                        json("claim_id", claimId, "verification_notes", input.getText().toString().trim()), "Handover recorded."))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmWithdraw() {
        new AlertDialog.Builder(this)
                .setTitle("Withdraw claim")
                .setMessage("Your claim will be cancelled. You can submit a new one later if the item is still available.")
                .setPositiveButton("Withdraw", (d, w) -> post(ApiConfig.CLAIMS_WITHDRAW, json("claim_id", claimId), "Your claim was withdrawn."))
                .setNegativeButton("Keep claim", null)
                .show();
    }

    private void post(String url, JSONObject body, String fallback) {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, url, body, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, response.optString("message", fallback), Toast.LENGTH_LONG).show();
                    load(); // show the new status, reviewer, receipt, etc.
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
}