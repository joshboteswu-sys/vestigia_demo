package com.vestigia.app.items;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.android.volley.Request;
import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;
import com.vestigia.app.network.ApiConfig;
import com.vestigia.app.network.AuthJsonObjectRequest;
import com.vestigia.app.network.VolleyMultipartRequest;
import com.vestigia.app.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class ProfileActivity extends BaseProtectedActivity {

    private TextView tvName, tvEmail, tvContact;
    private TextView tvLostCount, tvFoundCount, tvClaimedCount;
    private LinearLayout containerLostReports, containerFoundReports, containerClaims;
    private TextView tvEmptyLost, tvEmptyFound, tvEmptyClaims;
    private ProgressBar progressBar;
    private ImageView ivPhoto;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvContact = findViewById(R.id.tvContact);
        tvLostCount = findViewById(R.id.tvLostCount);
        tvFoundCount = findViewById(R.id.tvFoundCount);
        tvClaimedCount = findViewById(R.id.tvClaimedCount);
        containerLostReports = findViewById(R.id.containerLostReports);
        containerFoundReports = findViewById(R.id.containerFoundReports);
        containerClaims = findViewById(R.id.containerClaims);
        tvEmptyLost = findViewById(R.id.tvEmptyLost);
        tvEmptyFound = findViewById(R.id.tvEmptyFound);
        tvEmptyClaims = findViewById(R.id.tvEmptyClaims);
        progressBar = findViewById(R.id.progressBar);
        ivPhoto = findViewById(R.id.ivPhoto);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadNewPhoto(uri);
            }
        });

        ivPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadProfile(); // refresh in case something changed (e.g. a claim got approved)
        }
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET, ApiConfig.PROFILE, null, sessionManager.getToken(),
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject user = response.getJSONObject("user");
                        tvName.setText(user.getString("first_name") + " " + user.getString("last_name"));
                        tvEmail.setText(user.getString("email"));
                        String contact = user.optString("contact_number", "");
                        tvContact.setText(contact.isEmpty() ? "No contact number set" : contact);

                        String photoUrl = user.isNull("photo_url") ? null : user.optString("photo_url", null);
                        if (photoUrl != null) {
                            Glide.with(this).load(photoUrl).circleCrop()
                                    .placeholder(android.R.drawable.ic_menu_gallery).into(ivPhoto);
                        } else {
                            ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                        }

                        JSONObject stats = response.getJSONObject("stats");
                        tvLostCount.setText(String.valueOf(stats.getInt("lost_count")));
                        tvFoundCount.setText(String.valueOf(stats.getInt("found_count")));
                        tvClaimedCount.setText(String.valueOf(stats.getInt("claimed_count")));

                        renderList(response.getJSONArray("lost_reports"), containerLostReports, tvEmptyLost, "date_lost");
                        renderList(response.getJSONArray("found_reports"), containerFoundReports, tvEmptyFound, "date_found");
                        renderClaimsList(response.getJSONArray("claims"), containerClaims, tvEmptyClaims);

                    } catch (Exception e) {
                        Toast.makeText(this, "Could not read profile data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load profile. Check your connection.", Toast.LENGTH_SHORT).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void uploadNewPhoto(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();

            VolleyMultipartRequest request = new VolleyMultipartRequest(
                    ApiConfig.PROFILE_PHOTO_UPDATE, sessionManager.getToken(), new HashMap<>(),
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        if (response.optBoolean("success", false)) {
                            Toast.makeText(this, "Profile photo updated.", Toast.LENGTH_SHORT).show();
                            loadProfile(); // refresh to show the new photo
                        } else {
                            Toast.makeText(this, response.optString("message", "Could not update photo."), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Could not reach the server.", Toast.LENGTH_SHORT).show();
                    }
            );
            request.setFile("photo", bytes, "photo.jpg", "image/jpeg");
            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Could not read selected photo.", Toast.LENGTH_SHORT).show();
        }
    }

    // Builds simple rows for lost/found report lists.
    private void renderList(JSONArray arr, LinearLayout container, TextView emptyView, String dateField) throws Exception {
        container.removeAllViews();
        if (arr.length() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);

        boolean isLost = dateField.equals("date_lost");

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            View row = getLayoutInflater().inflate(R.layout.item_profile_activity_row, container, false);
            TextView tvTitle = row.findViewById(R.id.tvTitle);
            TextView tvMeta = row.findViewById(R.id.tvMeta);
            TextView tvStatus = row.findViewById(R.id.tvStatus);

            String location = isLost ? o.optString("last_seen_location", "") : o.optString("location", "");
            String verb = isLost ? "Reported" : "Turned in";

            tvTitle.setText(o.getString("item_name"));
            tvMeta.setText(verb + ": " + o.optString(dateField, "") + "  •  " + location + "  •  Ref #IT-" + o.getInt("id"));
            tvStatus.setText(o.getString("status").toUpperCase());
            container.addView(row);
        }
    }

    // Claims need a slightly different label ("Claimed on") — separate method keeps it simple.
    // Claims: "Claim Approved  •  Ref #4"
    private void renderClaimsList(JSONArray arr, LinearLayout container, TextView emptyView) throws Exception {
        container.removeAllViews();
        if (arr.length() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            View row = getLayoutInflater().inflate(R.layout.item_profile_activity_row, container, false);
            TextView tvTitle = row.findViewById(R.id.tvTitle);
            TextView tvMeta = row.findViewById(R.id.tvMeta);
            TextView tvStatus = row.findViewById(R.id.tvStatus);

            String status = o.getString("status");
            String friendlyStatus = status.equals("Approved") ? "Verified / Ready for pickup"
                    : status.equals("Pending") ? "Pending review"
                      : "Rejected";

            tvTitle.setText(o.getString("item_name"));
            tvMeta.setText("Claim " + status + "  •  " + o.optString("location", "") + "  •  Ref #IT-" + o.getInt("id"));
            tvStatus.setText(friendlyStatus.toUpperCase());
            container.addView(row);
        }
    }
}