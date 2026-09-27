package com.vestigia.app.items;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.models.FoundItem;
import com.vestigia.app.models.LostItem;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MyItemsActivity extends BaseProtectedActivity {

    private RecyclerView recyclerFound, recyclerLost;
    private TextView tvEmptyFound, tvEmptyLost;
    private ProgressBar progressBar;

    private FoundItemAdapter foundAdapter;
    private LostItemAdapter lostAdapter;
    private final List<FoundItem> foundItems = new ArrayList<>();
    private final List<LostItem> lostItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);

        recyclerFound = findViewById(R.id.recyclerFound);
        recyclerLost = findViewById(R.id.recyclerLost);
        tvEmptyFound = findViewById(R.id.tvEmptyFound);
        tvEmptyLost = findViewById(R.id.tvEmptyLost);
        progressBar = findViewById(R.id.progressBar);

        foundAdapter = new FoundItemAdapter(foundItems);
        recyclerFound.setLayoutManager(new LinearLayoutManager(this));
        recyclerFound.setAdapter(foundAdapter);

        lostAdapter = new LostItemAdapter(lostItems);
        recyclerLost.setLayoutManager(new LinearLayoutManager(this));
        recyclerLost.setAdapter(lostAdapter);

        loadMyFoundItems();
        loadMyLostItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadMyFoundItems();
            loadMyLostItems();
        }
    }

    private void loadMyFoundItems() {
        progressBar.setVisibility(View.VISIBLE);
        String url = ApiConfig.ITEMS_LIST + "?filter=mine";

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
                                    o.getInt("id"), o.getString("item_name"), o.optString("description", ""),
                                    o.optString("category", ""), o.getString("location"), o.getString("date_found"),
                                    o.getString("status"), o.isNull("image_url") ? null : o.optString("image_url", null),
                                    o.optString("first_name", "") + " " + o.optString("last_name", "")
                            ));
                        }
                        foundAdapter.updateData(results);
                        tvEmptyFound.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read found items.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load your found items.", Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void loadMyLostItems() {
        String url = ApiConfig.LOST_LIST + "?filter=mine";

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, url, null, sessionManager.getToken(),
                response -> {
                    try {
                        JSONArray arr = response.getJSONArray("items");
                        List<LostItem> results = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            results.add(new LostItem(
                                    o.getInt("id"), o.getString("item_name"), o.optString("description", ""),
                                    o.optString("category", ""), o.getString("last_seen_location"), o.getString("date_lost"),
                                    o.getString("status"), o.isNull("image_url") ? null : o.optString("image_url", null),
                                    o.optInt("reported_by", -1)
                            ));
                        }
                        lostAdapter.updateData(results);
                        tvEmptyLost.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read lost reports.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Could not load your lost reports.", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}