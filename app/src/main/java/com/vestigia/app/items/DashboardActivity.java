package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.auth.ChangePasswordActivity;
import com.vestigia.app.auth.LoginActivity;
import com.vestigia.app.models.LostItem;
import com.vestigia.app.models.RecentPost;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends BaseProtectedActivity {

    private DrawerLayout drawerLayout;
    private TextView tvWelcome, tvViewAll, tvEmptyPendingLost, tvBellBadge;
    private Button btnPost;
    private ProgressBar progressBarPosts;
    private RecyclerView recyclerRecentPosts, recyclerPendingLost;

    private RecentPostAdapter recentPostAdapter;
    private LostItemAdapter pendingLostAdapter;
    private final List<RecentPost> recentPosts = new ArrayList<>();
    private final List<LostItem> pendingLostItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Notification bell -> notifications screen
        tvBellBadge = findViewById(R.id.tvBellBadge);
        findViewById(R.id.bellContainer).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        tvWelcome = findViewById(R.id.tvWelcome);
        tvViewAll = findViewById(R.id.tvViewAll);
        tvEmptyPendingLost = findViewById(R.id.tvEmptyPendingLost);
        btnPost = findViewById(R.id.btnPost);
        progressBarPosts = findViewById(R.id.progressBarPosts);
        recyclerRecentPosts = findViewById(R.id.recyclerRecentPosts);
        recyclerPendingLost = findViewById(R.id.recyclerPendingLost);

        tvWelcome.setText("Hello, " + sessionManager.getFirstName() + "! 👋");

        recentPostAdapter = new RecentPostAdapter(recentPosts);
        recyclerRecentPosts.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerRecentPosts.setAdapter(recentPostAdapter);

        pendingLostAdapter = new LostItemAdapter(pendingLostItems);
        recyclerPendingLost.setLayoutManager(new LinearLayoutManager(this));
        recyclerPendingLost.setAdapter(pendingLostAdapter);

        btnPost.setOnClickListener(v -> ReportChooser.show(this));
        tvViewAll.setOnClickListener(v -> openBrowse("all"));

        setupDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadRecentPosts();
            loadPendingLostReports();
            loadUnreadCount();
        }
    }

    private void openBrowse(String type) {
        Intent intent = new Intent(this, BrowseActivity.class);
        intent.putExtra(BrowseActivity.EXTRA_TYPE, type);
        startActivity(intent);
    }

    private void loadUnreadCount() {
        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.NOTIFICATIONS_LIST + "?limit=1", null, sessionManager.getToken(),
                response -> {
                    int unread = response.optInt("unread_count", 0);
                    tvBellBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                    tvBellBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                },
                error -> { /* the badge simply stays as it was */ }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void loadRecentPosts() {
        progressBarPosts.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.RECENT_POSTS, null, sessionManager.getToken(),
                response -> {
                    progressBarPosts.setVisibility(View.GONE);
                    try {
                        JSONArray arr = response.getJSONArray("posts");
                        List<RecentPost> results = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            results.add(new RecentPost(
                                    o.getInt("id"),
                                    o.getString("item_type"),
                                    o.getString("item_name"),
                                    o.optString("location", ""),
                                    o.isNull("image_url") ? null : o.optString("image_url", null),
                                    o.optString("reporter_name", "")
                            ));
                        }
                        recentPostAdapter.updateData(results);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read recent posts.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBarPosts.setVisibility(View.GONE);
                    android.util.Log.e("DashboardActivity", "Error loading recent posts", error);
                    Toast.makeText(this, "Could not load recent posts. Please check your connection.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void loadPendingLostReports() {
        String url = ApiConfig.LOST_LIST + "?filter=mine";

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, url, null, sessionManager.getToken(),
                response -> {
                    try {
                        JSONArray arr = response.getJSONArray("items");
                        List<LostItem> results = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            if (!o.getString("status").equals("Active")) continue;
                            results.add(new LostItem(
                                    o.getInt("id"), o.getString("item_name"), o.optString("description", ""),
                                    o.optString("category", ""), o.getString("last_seen_location"), o.getString("date_lost"),
                                    o.getString("status"), o.isNull("image_url") ? null : o.optString("image_url", null),
                                    o.optInt("reported_by", -1)
                            ));
                        }
                        pendingLostAdapter.updateData(results);
                        tvEmptyPendingLost.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read lost reports.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    android.util.Log.e("DashboardActivity", "Error loading pending lost reports", error);
                    Toast.makeText(this, "Could not load your lost reports. Please check your connection.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void setupDrawer() {
        View drawerContent = findViewById(R.id.drawerContent);

        TextView tvDrawerUserName = drawerContent.findViewById(R.id.tvDrawerUserName);
        tvDrawerUserName.setText(sessionManager.getFullName());

        bindRow(drawerContent, R.id.rowFoundItems, android.R.drawable.ic_menu_search, "Found Items",
                () -> openBrowse("found"));

        bindRow(drawerContent, R.id.rowLostItems, android.R.drawable.ic_dialog_alert, "Lost Items",
                () -> openBrowse("lost"));

        bindRow(drawerContent, R.id.rowAccount, android.R.drawable.ic_menu_myplaces, "Account",
                () -> startActivity(new Intent(this, ProfileActivity.class)));

        bindRow(drawerContent, R.id.rowMyItems, android.R.drawable.ic_menu_agenda, "My Items",
                () -> startActivity(new Intent(this, MyItemsActivity.class)));

        bindRow(drawerContent, R.id.rowHandovers, android.R.drawable.ic_menu_send, "Pending handovers",
                () -> startActivity(new Intent(this, AdminHandoversActivity.class)));

        bindRow(drawerContent, R.id.rowChangePassword, android.R.drawable.ic_lock_lock, "Change Password",
                () -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        View rowAdminPanel = drawerContent.findViewById(R.id.rowAdminPanel);
        if (sessionManager.isAdmin()) {
            rowAdminPanel.setVisibility(View.VISIBLE);
            bindRow(drawerContent, R.id.rowAdminPanel, android.R.drawable.ic_menu_manage, "Admin Panel",
                    () -> startActivity(new Intent(this, AdminPanelActivity.class)));
        } else {
            rowAdminPanel.setVisibility(View.GONE);
        }

        bindRow(drawerContent, R.id.rowAboutUs, android.R.drawable.ic_menu_info_details, "About Us", () -> {
            Intent intent = new Intent(this, StaticPageActivity.class);
            intent.putExtra(StaticPageActivity.EXTRA_TITLE, "About Us");
            intent.putExtra(StaticPageActivity.EXTRA_BODY,
                    "Vestigia is a lost and found management system built to help our campus community " +
                            "report, find, and reclaim misplaced belongings.");
            startActivity(intent);
        });

        bindRow(drawerContent, R.id.rowPrivacyPolicy, android.R.drawable.ic_menu_help, "Privacy Policy", () -> {
            Intent intent = new Intent(this, StaticPageActivity.class);
            intent.putExtra(StaticPageActivity.EXTRA_TITLE, "Privacy Policy");
            intent.putExtra(StaticPageActivity.EXTRA_BODY,
                    "We collect your name, student ID, contact number, and email to verify your identity. " +
                            "Passwords are stored as one-way hashes and are never visible to anyone, including administrators.");
            startActivity(intent);
        });

        Button btnSignOut = drawerContent.findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(v -> logout());
    }

    private void bindRow(View drawerContent, int rowIncludeId, int iconRes, String label, Runnable onClick) {
        View row = drawerContent.findViewById(rowIncludeId);
        ImageView icon = row.findViewById(R.id.ivRowIcon);
        TextView labelView = row.findViewById(R.id.tvRowLabel);
        icon.setImageResource(iconRes);
        labelView.setText(label);
        row.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            onClick.run();
        });
    }

    private void logout() {
        String token = sessionManager.getToken();

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST, ApiConfig.LOGOUT, null, token,
                response -> {
                    sessionManager.clearSession();
                    Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
                    goToLogin();
                },
                error -> {
                    sessionManager.clearSession();
                    goToLogin();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}