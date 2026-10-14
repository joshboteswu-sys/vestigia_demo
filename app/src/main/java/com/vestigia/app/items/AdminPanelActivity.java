package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

public class AdminPanelActivity extends BaseProtectedActivity {

    private TextView tvAdminSummary, tvStatPending, tvStatAwaiting, tvStatFinders;
    private TextView tvBadgePending, tvBadgeAwaiting, tvBadgeFinders, tvUsersSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Admins only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTopTitle)).setText("Admin Panel");
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.tvAdminHello)).setText("Hello, " + sessionManager.getFirstName() + " 👋");
        tvAdminSummary = findViewById(R.id.tvAdminSummary);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatAwaiting = findViewById(R.id.tvStatAwaiting);
        tvStatFinders = findViewById(R.id.tvStatFinders);
        tvBadgePending = findViewById(R.id.tvBadgePending);
        tvBadgeAwaiting = findViewById(R.id.tvBadgeAwaiting);
        tvBadgeFinders = findViewById(R.id.tvBadgeFinders);
        tvUsersSubtitle = findViewById(R.id.tvUsersSubtitle);

        View.OnClickListener openPending = v -> startActivity(new Intent(this, AdminPendingClaimsActivity.class));
        View.OnClickListener openHandovers = v -> startActivity(new Intent(this, AdminHandoversActivity.class));
        View.OnClickListener openFinderHeld = v -> {
            Intent intent = new Intent(this, BrowseActivity.class);
            intent.putExtra(BrowseActivity.EXTRA_TYPE, "found");
            intent.putExtra(BrowseActivity.EXTRA_CUSTODY, "finder_held");
            startActivity(intent);
        };

        findViewById(R.id.rowAnalytics).setOnClickListener(v -> startActivity(new Intent(this, AnalyticsActivity.class)));
        findViewById(R.id.rowUsers).setOnClickListener(v -> startActivity(new Intent(this, AdminUsersActivity.class)));

        findViewById(R.id.rowPending).setOnClickListener(openPending);
        findViewById(R.id.statPending).setOnClickListener(openPending);
        findViewById(R.id.rowHandovers).setOnClickListener(openHandovers);
        findViewById(R.id.statAwaiting).setOnClickListener(openHandovers);
        findViewById(R.id.rowFinderHeld).setOnClickListener(openFinderHeld);
        findViewById(R.id.statFinders).setOnClickListener(openFinderHeld);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && sessionManager.isAdmin()) loadSummary();
    }

    private void loadSummary() {
        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.ADMIN_SUMMARY, null, sessionManager.getToken(),
                response -> {
                    int pending = response.optInt("pending_claims");
                    int awaiting = response.optInt("awaiting_pickup");
                    int finders = response.optInt("with_finders");
                    int users = response.optInt("total_users");

                    tvStatPending.setText(String.valueOf(pending));
                    tvStatAwaiting.setText(String.valueOf(awaiting));
                    tvStatFinders.setText(String.valueOf(finders));

                    setBadge(tvBadgePending, pending);
                    setBadge(tvBadgeAwaiting, awaiting);
                    setBadge(tvBadgeFinders, finders);
                    tvUsersSubtitle.setText(users + (users == 1 ? " registered user" : " registered users")
                            + " · promote or remove admin access");

                    String message;
                    if (pending > 0) {
                        message = pending + (pending == 1 ? " claim needs" : " claims need") + " your review.";
                    } else if (awaiting > 0) {
                        message = awaiting + (awaiting == 1 ? " item is" : " items are") + " waiting to be picked up.";
                    } else {
                        message = "You're all caught up.";
                    }
                    tvAdminSummary.setText(message);
                },
                error -> tvAdminSummary.setText("Could not load the latest numbers.")
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void setBadge(TextView badge, int count) {
        badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        badge.setText(String.valueOf(count));
    }
}