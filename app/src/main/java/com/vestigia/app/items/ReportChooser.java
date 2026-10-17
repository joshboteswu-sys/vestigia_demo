package com.vestigia.app.items;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vestigia.app.R;

// One place for the "Found or Lost?" choice, used by the home screen and the browse screen
public final class ReportChooser {

    private ReportChooser() { }

    public static void show(Activity activity) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_report_choice, null);

        view.findViewById(R.id.optionFound).setOnClickListener(v -> {
            dialog.dismiss();
            activity.startActivity(new Intent(activity, AddEditItemActivity.class));
        });
        view.findViewById(R.id.optionLost).setOnClickListener(v -> {
            dialog.dismiss();
            activity.startActivity(new Intent(activity, AddEditLostItemActivity.class));
        });

        dialog.setContentView(view);
        dialog.show();
    }
}