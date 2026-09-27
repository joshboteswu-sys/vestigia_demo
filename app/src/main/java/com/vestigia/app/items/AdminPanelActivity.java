package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;

public class AdminPanelActivity extends BaseProtectedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Admins only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button btnPendingClaims = findViewById(R.id.btnPendingClaims);
        Button btnHandovers = findViewById(R.id.btnHandovers);
        Button btnManageUsers = findViewById(R.id.btnManageUsers);

        btnPendingClaims.setOnClickListener(v ->
                startActivity(new Intent(this, AdminPendingClaimsActivity.class)));

        btnHandovers.setOnClickListener(v ->
                startActivity(new Intent(this, AdminHandoversActivity.class)));

        btnManageUsers.setOnClickListener(v ->
                startActivity(new Intent(this, AdminUsersActivity.class)));
    }
}