package com.vestigia.app.items;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vestigia.app.R;

// One Activity reused for both About Us and Privacy Policy — the caller
// passes the title and body text via Intent extras.
public class StaticPageActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_BODY = "extra_body";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_static_page);

        TextView tvTitle = findViewById(R.id.tvPageTitle);
        TextView tvBody = findViewById(R.id.tvPageBody);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String body = getIntent().getStringExtra(EXTRA_BODY);

        tvTitle.setText(title != null ? title : "");
        tvBody.setText(body != null ? body : "");
    }
}