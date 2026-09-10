package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.models.FoundItem;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class FoundItemsListActivity extends BaseProtectedActivity {

    private EditText etSearch;
    private Button btnFilterToday, btnFilterWeek, btnFilterOlder;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;

    private FoundItemAdapter adapter;
    private final List<FoundItem> itemList = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_items_list);

        etSearch = findViewById(R.id.etSearch);
        btnFilterToday = findViewById(R.id.btnFilterToday);
        btnFilterWeek = findViewById(R.id.btnFilterWeek);
        btnFilterOlder = findViewById(R.id.btnFilterOlder);
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        fabAdd = findViewById(R.id.fabAdd);

        adapter = new FoundItemAdapter(itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnFilterToday.setOnClickListener(v -> { currentFilter = "today"; loadItems(); });
        btnFilterWeek.setOnClickListener(v -> { currentFilter = "week"; loadItems(); });
        btnFilterOlder.setOnClickListener(v -> { currentFilter = "older"; loadItems(); });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { loadItems(); }
        });

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditItemActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadItems(); // refresh so new/edited/deleted items show up immediately
        }
    }

    private void loadItems() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        String search = etSearch.getText().toString().trim();
        String url = ApiConfig.ITEMS_LIST + "?filter=" + currentFilter;
        try {
            url += "&search=" + URLEncoder.encode(search, "UTF-8");
        } catch (Exception ignored) { }

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, url, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray arr = response.getJSONArray("items");
                        List<FoundItem> results = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            results.add(new FoundItem(
                                    o.getInt("id"),
                                    o.getString("item_name"),
                                    o.optString("description", ""),
                                    o.optString("category", ""),
                                    o.getString("location"),
                                    o.getString("date_found"),
                                    o.getString("status"),
                                    o.optString("image_path", null),
                                    o.optString("first_name", "") + " " + o.optString("last_name", "")
                            ));
                        }
                        adapter.updateData(results);
                        tvEmpty.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read server response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load items. Check your connection.", Toast.LENGTH_SHORT).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}
