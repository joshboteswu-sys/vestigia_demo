package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.models.RecentPost;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

// Mixed grid of lost and found posts with a search pill and a filter screen
public class BrowseActivity extends BaseProtectedActivity {

    public static final String EXTRA_TYPE = "type";       // all | found | lost
    public static final String EXTRA_CUSTODY = "custody"; // finder_held (admin shortcut)

    private EditText etSearch;
    private ImageView ivBack, ivFilter;
    private View filterDot, summaryBar;
    private TextView tvSummary, tvClearFilters, tvEmpty;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;

    private RecentPostAdapter adapter;
    private final List<RecentPost> posts = new ArrayList<>();

    private String type = "all";
    private String category = "";
    private String custody = "";
    private boolean ignoreTextChanges = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::loadPosts;
    private ActivityResultLauncher<Intent> filterLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        String t = getIntent().getStringExtra(EXTRA_TYPE);
        if (t != null) type = t;
        String c = getIntent().getStringExtra(EXTRA_CUSTODY);
        if (c != null) custody = c;

        ivBack = findViewById(R.id.ivBack);
        etSearch = findViewById(R.id.etSearch);
        ivFilter = findViewById(R.id.ivFilter);
        filterDot = findViewById(R.id.filterDot);
        summaryBar = findViewById(R.id.summaryBar);
        tvSummary = findViewById(R.id.tvSummary);
        tvClearFilters = findViewById(R.id.tvClearFilters);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        fabAdd = findViewById(R.id.fabAdd);

        adapter = new RecentPostAdapter(posts);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());
        fabAdd.setOnClickListener(v -> ReportChooser.show(this));

        // Results coming back from the filter screen
        filterLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent d = result.getData();
                type = orDefault(d.getStringExtra(FilterActivity.EXTRA_TYPE), "all");
                category = orDefault(d.getStringExtra(FilterActivity.EXTRA_CATEGORY), "");
                ignoreTextChanges = true;
                etSearch.setText(orDefault(d.getStringExtra(FilterActivity.EXTRA_KEYWORDS), ""));
                ignoreTextChanges = false;
                updateFilterIndicators();
                loadPosts();
            }
        });

        ivFilter.setOnClickListener(v -> {
            Intent i = new Intent(this, FilterActivity.class);
            i.putExtra(FilterActivity.EXTRA_KEYWORDS, etSearch.getText().toString().trim());
            i.putExtra(FilterActivity.EXTRA_CATEGORY, category);
            i.putExtra(FilterActivity.EXTRA_TYPE, type);
            filterLauncher.launch(i);
        });

        tvClearFilters.setOnClickListener(v -> {
            type = "all";
            category = "";
            custody = "";
            updateFilterIndicators();
            loadPosts();
        });

        // Search while typing (waits a moment so it doesn't query on every key)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (ignoreTextChanges) return;
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, 350);
            }
        });

        updateFilterIndicators();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) loadPosts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(searchRunnable);
    }

    private String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    // Dot on the filter button + a one-line summary of what is being filtered
    private void updateFilterIndicators() {
        List<String> parts = new ArrayList<>();
        if (type.equals("found")) parts.add("Found items");
        else if (type.equals("lost")) parts.add("Lost reports");
        if (!category.isEmpty()) parts.add("Category: " + category);
        if (custody.equals("finder_held")) parts.add("Held by finders");

        boolean active = !parts.isEmpty();
        filterDot.setVisibility(active ? View.VISIBLE : View.GONE);
        summaryBar.setVisibility(active ? View.VISIBLE : View.GONE);
        tvSummary.setText(active ? "Showing: " + android.text.TextUtils.join("  •  ", parts) : "");
    }

    private String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private void loadPosts() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        String url = ApiConfig.POSTS_LIST
                + "?type=" + type
                + "&search=" + enc(etSearch.getText().toString().trim())
                + "&category=" + enc(category)
                + (custody.isEmpty() ? "" : "&custody=" + enc(custody));

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, url, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
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
                        adapter.updateData(results);
                        tvEmpty.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read posts.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    android.util.Log.e("BrowseActivity", "Error loading posts from " + url, error);
                    Toast.makeText(this, "Could not load lost or found items. Please check your connection.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}