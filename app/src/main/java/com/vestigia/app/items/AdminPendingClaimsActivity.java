package com.vestigia.app.items;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminPendingClaimsActivity extends BaseProtectedActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ClaimAdapter adapter;
    private final List<JSONObject> claims = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_pending_claims);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Extra guard: non-admins should never even reach this screen, but if
        // they somehow do (e.g. deep link), bounce them back immediately.
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Admins only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ClaimAdapter(claims);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadClaims();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && sessionManager.isAdmin()) {
            loadClaims();
        }
    }

    private void loadClaims() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.CLAIMS_PENDING, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray arr = response.getJSONArray("claims");
                        claims.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            claims.add(arr.getJSONObject(i));
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(claims.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read server response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load claims.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void approveClaim(int claimId) {
        JSONObject body = new JSONObject();
        try { body.put("claim_id", claimId); } catch (Exception ignored) { }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.CLAIMS_APPROVE, body, sessionManager.getToken(),
                response -> {
                    Toast.makeText(this, response.optString("message", "Claim approved."), Toast.LENGTH_SHORT).show();
                    loadClaims();
                },
                error -> Toast.makeText(this, "Could not approve claim.", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void rejectClaim(int claimId) {
        JSONObject body = new JSONObject();
        try { body.put("claim_id", claimId); } catch (Exception ignored) { }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.CLAIMS_REJECT, body, sessionManager.getToken(),
                response -> {
                    Toast.makeText(this, response.optString("message", "Claim rejected."), Toast.LENGTH_SHORT).show();
                    loadClaims();
                },
                error -> Toast.makeText(this, "Could not reject claim.", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    // ---- inner adapter ----
    private class ClaimAdapter extends RecyclerView.Adapter<ClaimAdapter.ViewHolder> {
        private final List<JSONObject> data;

        ClaimAdapter(List<JSONObject> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_claim_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject c = data.get(position);
            holder.tvItemName.setText(c.optString("item_name"));
            holder.tvClaimant.setText("Claimed by: " + c.optString("first_name") + " " + c.optString("last_name")
                    + " (" + c.optString("email") + ")");
            holder.tvProof.setText(c.optString("proof_description"));
            holder.tvMeta.setText(c.optString("location") + "  •  Found " + c.optString("date_found"));

            String photoPath = c.optString("proof_photo_path", null);
            if (photoPath != null && !photoPath.equals("null") && !photoPath.isEmpty()) {
                // Build the same base URL pattern used elsewhere
                String imageUrl = ApiConfig.BASE_URL + photoPath;
                Glide.with(holder.itemView.getContext()).load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .centerCrop().into(holder.ivProof);
                holder.ivProof.setVisibility(View.VISIBLE);
            } else {
                holder.ivProof.setVisibility(View.GONE);
            }

            int claimId = c.optInt("claim_id");
            holder.btnApprove.setOnClickListener(v -> approveClaim(claimId));
            holder.btnReject.setOnClickListener(v -> rejectClaim(claimId));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvItemName, tvClaimant, tvProof, tvMeta;
            ImageView ivProof;
            Button btnApprove, btnReject;

            ViewHolder(View itemView) {
                super(itemView);
                tvItemName = itemView.findViewById(R.id.tvItemName);
                tvClaimant = itemView.findViewById(R.id.tvClaimant);
                tvProof = itemView.findViewById(R.id.tvProof);
                tvMeta = itemView.findViewById(R.id.tvMeta);
                ivProof = itemView.findViewById(R.id.ivProof);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}