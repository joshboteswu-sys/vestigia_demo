package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends BaseProtectedActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvMarkAll;
    private NotificationAdapter adapter;
    private final List<JSONObject> notifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvMarkAll = findViewById(R.id.tvMarkAll);

        adapter = new NotificationAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        tvMarkAll.setOnClickListener(v -> markAllRead());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.NOTIFICATIONS_LIST + "?limit=100", null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray arr = response.getJSONArray("notifications");
                        notifications.clear();
                        for (int i = 0; i < arr.length(); i++) notifications.add(arr.getJSONObject(i));
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read notifications.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load notifications.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void markRead(int id) {
        JSONObject body = new JSONObject();
        try { body.put("id", id); } catch (Exception ignored) { }
        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.NOTIFICATIONS_READ, body, sessionManager.getToken(),
                response -> { }, error -> { });
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void markAllRead() {
        JSONObject body = new JSONObject();
        try { body.put("all", true); } catch (Exception ignored) { }
        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.NOTIFICATIONS_READ, body, sessionManager.getToken(),
                response -> loadNotifications(),
                error -> Toast.makeText(this, "Could not update notifications.", Toast.LENGTH_SHORT).show());
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    // Tapping a notification marks it read and opens the most relevant screen.
    // Tapping a notification marks it read and opens the most relevant screen.
    private void openTarget(JSONObject n, int position) {
        if (!n.optBoolean("is_read", false)) {
            markRead(n.optInt("id"));
            try { n.put("is_read", true); } catch (Exception ignored) { }
            adapter.notifyItemChanged(position);
        }

        String type = n.optString("type", "");
        boolean hasClaim = !n.isNull("claim_id");
        Intent intent;

        if (hasClaim && (!"claim_submitted".equals(type) || sessionManager.isAdmin())) {
            intent = new Intent(this, ClaimDetailActivity.class);
            intent.putExtra("claim_id", n.optInt("claim_id"));
        } else if (!n.isNull("item_id")) {
            intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra("item_id", n.optInt("item_id"));
        } else {
            return;
        }
        startActivity(intent);
    }

    private String formatTime(String raw) {
        try {
            String base = raw.length() >= 19 ? raw.substring(0, 19) : raw;
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(base);
            return new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(d);
        } catch (Exception e) {
            return raw;
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject n = notifications.get(position);
            boolean read = n.optBoolean("is_read", false);

            holder.tvTitle.setText(n.optString("title"));
            holder.tvMessage.setText(n.optString("message"));
            holder.tvTime.setText(formatTime(n.optString("created_at")));
            holder.itemView.setBackgroundResource(read ? R.drawable.bg_card : R.drawable.bg_card_unread);

            holder.itemView.setOnClickListener(v -> openTarget(n, holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}