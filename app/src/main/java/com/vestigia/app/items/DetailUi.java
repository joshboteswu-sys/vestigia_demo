package com.vestigia.app.items;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.vestigia.app.R;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DetailUi {

    private DetailUi() { }

    // Reads a string field, treating a missing key or JSON null as ""
    public static String str(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return "";
        return o.optString(key, "");
    }

    // "2026-09-04 08:12:00" -> "04-09-2026"
    public static String postedDate(String raw) {
        return reformat(raw, "dd-MM-yyyy");
    }

    // "2026-09-04" -> "Sep 4, 2026"
    public static String friendlyDate(String raw) {
        return reformat(raw, "MMM d, yyyy");
    }

    // "2026-09-04 08:12:00" -> "Sep 4, 2026, 8:12 AM"
    public static String friendlyDateTime(String raw) {
        if (raw == null || raw.length() < 19) return friendlyDate(raw);
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw.substring(0, 19));
            return new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US).format(d);
        } catch (Exception e) {
            return raw;
        }
    }

    private static String reformat(String raw, String outPattern) {
        if (raw == null) return "";
        if (raw.length() < 10) return raw;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw.substring(0, 10));
            return new SimpleDateFormat(outPattern, Locale.US).format(d);
        } catch (Exception e) {
            return raw;
        }
    }

    // Fills the shared hero block (view_detail_hero.xml)
    public static void bindHero(Activity a, String title, String badge, int badgeBackground,
                                String postedRaw, String category, String imageUrl) {
        if (a.isFinishing() || a.isDestroyed()) return;

        ((TextView) a.findViewById(R.id.tvTitle)).setText(title);

        TextView tvBadge = a.findViewById(R.id.tvBadge);
        tvBadge.setText(badge);
        tvBadge.setBackgroundResource(badgeBackground);

        ((TextView) a.findViewById(R.id.tvPostedDate)).setText(postedDate(postedRaw));

        boolean hasCategory = category != null && !category.trim().isEmpty();
        a.findViewById(R.id.metaCategory).setVisibility(hasCategory ? View.VISIBLE : View.GONE);
        ((TextView) a.findViewById(R.id.tvCategoryLine)).setText(hasCategory ? category.trim() : "");

        ImageView hero = a.findViewById(R.id.ivHero);
        TextView none = a.findViewById(R.id.tvNoImage);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            none.setVisibility(View.GONE);
            hero.setVisibility(View.VISIBLE);
            Glide.with(a).load(imageUrl).centerCrop().into(hero);
        } else {
            hero.setVisibility(View.GONE);
            none.setVisibility(View.VISIBLE);
        }
    }

    // Shows a row (with its divider) only when it has a value
    public static void setRow(View row, TextView value, String text) {
        boolean has = text != null && !text.trim().isEmpty();
        row.setVisibility(has ? View.VISIBLE : View.GONE);
        if (has) value.setText(text.trim());
    }

    public static void openEmail(Context c, String email) {
        try {
            c.startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email)));
        } catch (Exception e) {
            Toast.makeText(c, "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openDial(Context c, String phone) {
        try {
            c.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
        } catch (Exception e) {
            Toast.makeText(c, "No phone app found.", Toast.LENGTH_SHORT).show();
        }
    }
}