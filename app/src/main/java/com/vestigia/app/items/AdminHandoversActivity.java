package com.vestigia.app.items;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminHandoversActivity extends BaseProtectedActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private HandoverAdapter adapter;
    private final List<JSONObject> approvedClaims = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_handovers);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Admins only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new HandoverAdapter(approvedClaims);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadApprovedClaims();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && sessionManager.isAdmin()) {
            loadApprovedClaims();
        }
    }

    private void loadApprovedClaims() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.CLAIMS_APPROVED_PENDING, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray arr = response.getJSONArray("claims");
                        approvedClaims.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            approvedClaims.add(arr.getJSONObject(i));
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(approvedClaims.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read server response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load approved claims.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void confirmHandover(int claimId, String notes) {
        JSONObject body = new JSONObject();
        try {
            body.put("claim_id", claimId);
            body.put("verification_notes", notes);
        } catch (Exception ignored) { }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.HANDOVERS_CREATE, body, sessionManager.getToken(),
                response -> {
                    String receipt = response.optString("receipt_no", "");
                    Toast.makeText(this, "Handover recorded. Receipt: " + receipt, Toast.LENGTH_LONG).show();
                    loadApprovedClaims();
                },
                error -> Toast.makeText(this, "Could not record handover.", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private class HandoverAdapter extends RecyclerView.Adapter<HandoverAdapter.ViewHolder> {
        private final List<JSONObject> data;

        HandoverAdapter(List<JSONObject> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_approved_claim_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject c = data.get(position);
            holder.tvItemName.setText(c.optString("item_name"));
            holder.tvClaimant.setText("Approved for: " + c.optString("first_name") + " " + c.optString("last_name")
                    + " (" + c.optString("email") + ")");
            holder.tvMeta.setText(c.optString("location") + "  •  Approved " + c.optString("decision_date"));

            int claimId = c.optInt("claim_id");
            holder.btnConfirm.setOnClickListener(v -> showConfirmDialog(claimId));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvItemName, tvClaimant, tvMeta;
            Button btnConfirm;

            ViewHolder(View itemView) {
                super(itemView);
                tvItemName = itemView.findViewById(R.id.tvItemName);
                tvClaimant = itemView.findViewById(R.id.tvClaimant);
                tvMeta = itemView.findViewById(R.id.tvMeta);
                btnConfirm = itemView.findViewById(R.id.btnConfirm);
            }
        }
    }

    private void showConfirmDialog(int claimId) {
        EditText input = new EditText(this);
        input.setHint("Verification notes (e.g. ID checked, item inspected)");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm handover")
                .setMessage("Confirm that the claimant has physically received this item.")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> confirmHandover(claimId, input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }
}