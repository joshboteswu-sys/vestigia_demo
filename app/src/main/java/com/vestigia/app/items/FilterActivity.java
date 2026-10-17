package com.vestigia.app.items;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.vestigia.app.R;
import com.vestigia.app.auth.BaseProtectedActivity;

public class FilterActivity extends BaseProtectedActivity {

    public static final String EXTRA_KEYWORDS = "keywords";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_TYPE = "type"; // all | found | lost

    private EditText etKeywords, etCategory;
    private TextView chipAll, chipFound, chipLost;
    private String selectedType = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        ((TextView) findViewById(R.id.tvTopTitle)).setText("Filter");
        ((ImageView) findViewById(R.id.ivBack)).setOnClickListener(v -> finish());

        etKeywords = findViewById(R.id.etKeywords);
        etCategory = findViewById(R.id.etCategory);
        chipAll = findViewById(R.id.chipAll);
        chipFound = findViewById(R.id.chipFound);
        chipLost = findViewById(R.id.chipLost);

        // Start from whatever the browse screen is currently showing
        etKeywords.setText(orEmpty(getIntent().getStringExtra(EXTRA_KEYWORDS)));
        etCategory.setText(orEmpty(getIntent().getStringExtra(EXTRA_CATEGORY)));
        String t = getIntent().getStringExtra(EXTRA_TYPE);
        selectedType = (t == null || t.isEmpty()) ? "all" : t;
        updateChips();

        chipAll.setOnClickListener(v -> selectType("all"));
        chipFound.setOnClickListener(v -> selectType("found"));
        chipLost.setOnClickListener(v -> selectType("lost"));

        findViewById(R.id.btnReset).setOnClickListener(v -> {
            etKeywords.setText("");
            etCategory.setText("");
            selectType("all");
        });

        findViewById(R.id.btnApply).setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra(EXTRA_KEYWORDS, etKeywords.getText().toString().trim());
            result.putExtra(EXTRA_CATEGORY, etCategory.getText().toString().trim());
            result.putExtra(EXTRA_TYPE, selectedType);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private void selectType(String type) {
        selectedType = type;
        updateChips();
    }

    private void updateChips() {
        styleChip(chipAll, "all".equals(selectedType));
        styleChip(chipFound, "found".equals(selectedType));
        styleChip(chipLost, "lost".equals(selectedType));
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chip.setTextColor(ContextCompat.getColor(this, selected ? R.color.surface : R.color.text_secondary));
    }
}