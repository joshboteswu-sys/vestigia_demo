package com.vestigia.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

// Keeps every screen clear of the status/navigation bars and shrinks it above the keyboard.
// The root view is SHRUNK with margins (not padded), so a ScrollView root, or a ScrollView
// inside the root, really gets smaller and scrolls the focused field into view.
public final class SystemBarInsets {

    private SystemBarInsets() { }

    public static void apply(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;

        View root = content.getChildAt(0);
        if (root.getTag(R.id.tag_insets_applied) != null) return;
        root.setTag(R.id.tag_insets_applied, Boolean.TRUE);

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) window.setNavigationBarContrastEnforced(false);

        int mode = window.getAttributes().softInputMode;
        mode = (mode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        window.setSoftInputMode(mode);

        boolean isDrawer = root instanceof DrawerLayout;

        // The strip behind the system bars takes the screen's own background colour
        if (!isDrawer && root.getBackground() instanceof ColorDrawable) {
            content.setBackgroundColor(((ColorDrawable) root.getBackground()).getColor());
        }

        // A dark header at the top of the screen also colours the status bar area
        View tintHost = root;
        if (isDrawer) {
            tintHost = ((ViewGroup) root).getChildCount() > 0 ? ((ViewGroup) root).getChildAt(0) : null;
        }
        StatusBarTint tint = tintHost != null ? createTint(tintHost) : null;
        if (tint != null && Build.VERSION.SDK_INT >= 23) {
            if (isDrawer) tintHost.setForeground(tint);
            else content.setForeground(tint);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(tint == null);
        controller.setAppearanceLightNavigationBars(true);

        if (isDrawer) {
            applyToDrawer(activity, (DrawerLayout) root, tintHost, tint);
        } else {
            applyToRoot(activity, root, tint);
        }
        ViewCompat.requestApplyInsets(root);
    }

    private static void applyToRoot(Activity activity, View root, @Nullable StatusBarTint tint) {
        if (!(root.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;

        ViewGroup.MarginLayoutParams first = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
        final int bl = first.leftMargin, bt = first.topMargin, br = first.rightMargin, bb = first.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.setMargins(bl + bars.left, bt + bars.top, br + bars.right, bb + Math.max(bars.bottom, ime.bottom));
            v.setLayoutParams(lp);

            if (tint != null) tint.setHeight(bars.top);
            keepFocusVisible(activity, v, ime.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // DrawerLayout ignores its own padding, so the content and the drawer panel are padded separately
    private static void applyToDrawer(Activity activity, DrawerLayout drawer, @Nullable View content, @Nullable StatusBarTint tint) {
        ViewCompat.setOnApplyWindowInsetsListener(drawer, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            if (content != null) pad(content, bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            if (tint != null) tint.setHeight(bars.top);

            for (int i = 1; i < drawer.getChildCount(); i++) {
                View panel = drawer.getChildAt(i);
                pad(panel, 0, 0, 0, bars.bottom);
                if (panel instanceof LinearLayout && ((LinearLayout) panel).getChildCount() > 0) {
                    pad(((LinearLayout) panel).getChildAt(0), 0, bars.top, 0, 0);
                }
            }

            keepFocusVisible(activity, drawer, ime.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private static void pad(View v, int l, int t, int r, int b) {
        int[] base = (int[]) v.getTag(R.id.tag_insets_base);
        if (base == null) {
            base = new int[]{v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom()};
            v.setTag(R.id.tag_insets_base, base);
        }
        v.setPadding(base[0] + l, base[1] + t, base[2] + r, base[3] + b);
    }

    // After the screen has shrunk for the keyboard, make sure the field being typed in is on screen
    private static void keepFocusVisible(Activity activity, View root, int imeBottom) {
        if (imeBottom <= 0) return;
        Runnable scroll = () -> {
            View focus = activity.getCurrentFocus();
            if (focus != null && focus.isAttachedToWindow()) {
                focus.requestRectangleOnScreen(new Rect(0, 0, focus.getWidth(), focus.getHeight()), false);
            }
        };
        root.postDelayed(scroll, 120);
        root.postDelayed(scroll, 350);
    }

    @Nullable
    private static StatusBarTint createTint(View host) {
        if (Build.VERSION.SDK_INT < 23 || !(host instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) host;
        if (group.getChildCount() == 0) return null;

        Drawable bg = group.getChildAt(0).getBackground();
        if (!(bg instanceof ColorDrawable)) return null;

        int color = ((ColorDrawable) bg).getColor();
        if (Color.alpha(color) < 255 || ColorUtils.calculateLuminance(color) > 0.4) return null;
        return new StatusBarTint(color);
    }

    private static class StatusBarTint extends Drawable {
        private final Paint paint = new Paint();
        private int height;

        StatusBarTint(int color) {
            paint.setColor(color);
        }

        void setHeight(int h) {
            if (h != height) {
                height = h;
                invalidateSelf();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (height > 0) canvas.drawRect(0, 0, getBounds().width(), height, paint);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter colorFilter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}