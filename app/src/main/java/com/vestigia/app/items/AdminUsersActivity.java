package com.vestigia.app.items;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class AdminUsersActivity extends BaseProtectedActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private UserAdapter adapter;
    private final List<JSONObject> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Admins only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new UserAdapter(users);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && sessionManager.isAdmin()) {
            loadUsers();
        }
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.ADMIN_USERS_LIST, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray arr = response.getJSONArray("users");
                        users.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            users.add(arr.getJSONObject(i));
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read server response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load users.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void toggleAdmin(int userId) {
        JSONObject body = new JSONObject();
        try { body.put("user_id", userId); } catch (Exception ignored) { }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.ADMIN_TOGGLE_ADMIN, body, sessionManager.getToken(),
                response -> {
                    Toast.makeText(this, response.optString("message", "Updated."), Toast.LENGTH_SHORT).show();
                    loadUsers(); // refresh so the pill/button label updates
                },
                error -> {
                    String msg = "Could not update user.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            JSONObject json = new JSONObject(new String(error.networkResponse.data));
                            msg = json.optString("message", msg);
                        } catch (Exception ignored) { }
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<JSONObject> data;

        UserAdapter(List<JSONObject> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject u = data.get(position);
            holder.tvName.setText(u.optString("first_name") + " " + u.optString("last_name"));
            holder.tvEmail.setText(u.optString("email") + "  •  " + u.optString("student_id"));

            boolean isAdmin = u.optBoolean("is_admin", false);
            holder.tvRole.setText(isAdmin ? "ADMIN" : "USER");

            int userId = u.optInt("id");
            boolean isSelf = (userId == sessionManager.getUserId());
            holder.btnToggle.setEnabled(!isSelf);
            holder.btnToggle.setText(isSelf ? "This is you" : (isAdmin ? "Remove admin" : "Make admin"));

            holder.btnToggle.setOnClickListener(v -> toggleAdmin(userId));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvRole;
            Button btnToggle;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                tvRole = itemView.findViewById(R.id.tvRole);
                btnToggle = itemView.findViewById(R.id.btnToggle);
            }
        }
    }
}