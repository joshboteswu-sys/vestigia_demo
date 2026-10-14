package com.vestigia.app.items;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.charts.DonutChartView;
import com.vestigia.app.charts.LineChartView;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnalyticsActivity extends BaseProtectedActivity {

    private ProgressBar progressBar;
    private TextView tvReturnRate, tvReturnRateSub, tvActiveClaims, tvActiveClaimsSub;
    private TextView tvCustodyFinder, tvCustodyFinderSub, tvExpiring, tvExpiringSub;
    private TextView chip7d, chip30d, chip12m;
    private LineChartView lineChart;
    private DonutChartView donutChart;
    private LinearLayout legendContainer;
    private TextView tvApprovalRate, tvApprovedCount, tvRejectedCount, tvPendingNote;
    private View barApproved, barRejected;

    private String currentRange = "30d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Admins only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.progressBar);
        tvReturnRate = findViewById(R.id.tvReturnRate);
        tvReturnRateSub = findViewById(R.id.tvReturnRateSub);
        tvActiveClaims = findViewById(R.id.tvActiveClaims);
        tvActiveClaimsSub = findViewById(R.id.tvActiveClaimsSub);
        tvCustodyFinder = findViewById(R.id.tvCustodyFinder);
        tvCustodyFinderSub = findViewById(R.id.tvCustodyFinderSub);
        tvExpiring = findViewById(R.id.tvExpiring);
        tvExpiringSub = findViewById(R.id.tvExpiringSub);
        chip7d = findViewById(R.id.chip7d);
        chip30d = findViewById(R.id.chip30d);
        chip12m = findViewById(R.id.chip12m);
        lineChart = findViewById(R.id.lineChart);
        donutChart = findViewById(R.id.donutChart);
        legendContainer = findViewById(R.id.legendContainer);
        tvApprovalRate = findViewById(R.id.tvApprovalRate);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        tvRejectedCount = findViewById(R.id.tvRejectedCount);
        tvPendingNote = findViewById(R.id.tvPendingNote);
        barApproved = findViewById(R.id.barApproved);
        barRejected = findViewById(R.id.barRejected);

        chip7d.setOnClickListener(v -> selectRange("7d"));
        chip30d.setOnClickListener(v -> selectRange("30d"));
        chip12m.setOnClickListener(v -> selectRange("12m"));
        updateChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && sessionManager.isAdmin()) {
            load();
        }
    }

    private void selectRange(String range) {
        if (range.equals(currentRange)) return;
        currentRange = range;
        updateChips();
        load();
    }

    private void updateChips() {
        styleChip(chip7d, "7d".equals(currentRange));
        styleChip(chip30d, "30d".equals(currentRange));
        styleChip(chip12m, "12m".equals(currentRange));
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chip.setTextColor(ContextCompat.getColor(this, selected ? R.color.surface : R.color.text_secondary));
    }

    private void load() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.ANALYTICS + "?range=" + currentRange, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        render(response);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read analytics data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load analytics.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void render(JSONObject r) throws Exception {
        renderCards(r.getJSONObject("cards"));
        renderTimeline(r.getJSONObject("timeline"));
        renderBreakdown(r.getJSONArray("breakdown"));
        renderDecisions(r.getJSONObject("decisions"));
    }

    private void renderCards(JSONObject cards) throws Exception {
        JSONObject rr = cards.getJSONObject("return_rate");
        tvReturnRate.setText(String.format(Locale.US, "%.0f%%", rr.optDouble("percent", 0)));
        tvReturnRateSub.setText(rr.optInt("returned") + " of " + rr.optInt("total") + " items returned");

        JSONObject ac = cards.getJSONObject("active_claims");
        tvActiveClaims.setText(String.valueOf(ac.optInt("count")));
        tvActiveClaimsSub.setText(ac.optInt("pending") + " pending · " + ac.optInt("awaiting_pickup") + " awaiting pickup\n"
                + ac.optInt("active_users") + " active of " + ac.optInt("total_users") + " users");

        JSONObject cf = cards.getJSONObject("custody_finder");
        tvCustodyFinder.setText(String.valueOf(cf.optInt("count")));
        tvCustodyFinderSub.setText("items still held by finders");

        JSONObject ex = cards.getJSONObject("expiring_soon");
        tvExpiring.setText(String.valueOf(ex.optInt("count")));
        String sub = "within " + ex.optInt("warning_days") + " days of the " + ex.optInt("retention_days") + "-day limit";
        if (ex.optInt("overdue") > 0) sub += "\n" + ex.optInt("overdue") + " already overdue";
        tvExpiringSub.setText(sub);
    }

    private void renderTimeline(JSONObject tl) throws Exception {
        JSONArray labelsJson = tl.getJSONArray("labels");
        JSONArray reportedJson = tl.getJSONArray("reported");
        JSONArray returnedJson = tl.getJSONArray("returned");

        int n = labelsJson.length();
        List<String> labels = new ArrayList<>();
        int[] reported = new int[n];
        int[] returned = new int[n];
        for (int i = 0; i < n; i++) {
            labels.add(labelsJson.getString(i));
            reported[i] = reportedJson.getInt(i);
            returned[i] = returnedJson.getInt(i);
        }
        lineChart.setData(labels, reported, returned);
    }

    private void renderBreakdown(JSONArray breakdown) throws Exception {
        int n = breakdown.length();
        int[] values = new int[n];
        int[] colors = new int[n];
        int total = 0;

        for (int i = 0; i < n; i++) {
            JSONObject s = breakdown.getJSONObject(i);
            values[i] = s.getInt("count");
            colors[i] = colorForKey(s.getString("key"));
            total += values[i];
        }
        donutChart.setData(values, colors, String.valueOf(total), total == 1 ? "item" : "items");

        legendContainer.removeAllViews();
        for (int i = 0; i < n; i++) {
            JSONObject s = breakdown.getJSONObject(i);
            addLegendRow(colors[i], s.getString("label"), values[i], total);
        }
    }

    private int colorForKey(String key) {
        switch (key) {
            case "active_finder": return 0xFF14B8A6;
            case "active_admin":  return 0xFF0F766E;
            case "claimed":       return 0xFF64748B;
            case "returned":      return 0xFF5EEAD4;
            case "disposed":      return 0xFFEF4444;
            default:              return 0xFF94A3B8;
        }
    }

    private void addLegendRow(int color, String label, int count, int total) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.setMarginEnd(dp(10));
        dot.setLayoutParams(dotParams);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        dot.setBackground(circle);

        TextView name = new TextView(this);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        name.setText(label);
        name.setTextSize(13);
        name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        TextView value = new TextView(this);
        int pct = total > 0 ? Math.round(100f * count / total) : 0;
        value.setText(count + "  (" + pct + "%)");
        value.setTextSize(13);
        value.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        row.addView(dot);
        row.addView(name);
        row.addView(value);
        legendContainer.addView(row);
    }

    private void renderDecisions(JSONObject d) {
        int approved = d.optInt("approved");
        int rejected = d.optInt("rejected");
        int pending = d.optInt("pending");
        int decided = approved + rejected;

        LinearLayout.LayoutParams approvedParams = (LinearLayout.LayoutParams) barApproved.getLayoutParams();
        LinearLayout.LayoutParams rejectedParams = (LinearLayout.LayoutParams) barRejected.getLayoutParams();
        approvedParams.weight = approved;
        rejectedParams.weight = rejected;
        barApproved.setLayoutParams(approvedParams);
        barRejected.setLayoutParams(rejectedParams);

        if (decided == 0) {
            tvApprovalRate.setText("—");
            tvApprovedCount.setText("Approved  0");
            tvRejectedCount.setText("Rejected  0");
        } else {
            int approvedPct = Math.round(100f * approved / decided);
            tvApprovalRate.setText(approvedPct + "%");
            tvApprovedCount.setText("Approved  " + approved + "  (" + approvedPct + "%)");
            tvRejectedCount.setText("Rejected  " + rejected + "  (" + (100 - approvedPct) + "%)");
        }

        tvPendingNote.setText(pending == 0
                ? "No claims are waiting for a decision."
                : pending + (pending == 1 ? " claim is" : " claims are") + " still pending and not counted yet.");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}