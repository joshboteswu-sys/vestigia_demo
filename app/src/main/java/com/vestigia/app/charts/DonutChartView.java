package com.vestigia.app.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

// Donut chart with a total in the middle
public class DonutChartView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bigTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float density;

    private int[] values = new int[0];
    private int[] colors = new int[0];
    private String centerTop = "";
    private String centerBottom = "";

    public DonutChartView(Context context) {
        this(context, null);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.BUTT);

        bigTextPaint.setColor(0xFF0F172A);
        bigTextPaint.setTextAlign(Paint.Align.CENTER);
        bigTextPaint.setFakeBoldText(true);
        bigTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 28, getResources().getDisplayMetrics()));

        smallTextPaint.setColor(0xFF64748B);
        smallTextPaint.setTextAlign(Paint.Align.CENTER);
        smallTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
    }

    public void setData(int[] values, int[] colors, String centerTop, String centerBottom) {
        this.values = values != null ? values : new int[0];
        this.colors = colors != null ? colors : new int[0];
        this.centerTop = centerTop != null ? centerTop : "";
        this.centerBottom = centerBottom != null ? centerBottom : "";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;

        float stroke = size * 0.17f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = size / 2f - stroke / 2f - 2 * density;
        oval.set(cx - r, cy - r, cx + r, cy + r);
        ringPaint.setStrokeWidth(stroke);

        int total = 0;
        int nonZero = 0;
        for (int v : values) {
            total += v;
            if (v > 0) nonZero++;
        }

        if (total == 0) {
            ringPaint.setColor(0xFFE2E8F0);
            canvas.drawArc(oval, 0, 360, false, ringPaint);
        } else {
            float gap = nonZero > 1 ? 2f : 0f;
            float start = -90f;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float sweep = 360f * values[i] / total;
                ringPaint.setColor(i < colors.length ? colors[i] : 0xFF64748B);
                canvas.drawArc(oval, start + gap / 2f, Math.max(sweep - gap, 0.5f), false, ringPaint);
                start += sweep;
            }
        }

        // Centre text
        Paint.FontMetrics bigFm = bigTextPaint.getFontMetrics();
        float bigY = cy - (bigFm.ascent + bigFm.descent) / 2f - 6 * density;
        canvas.drawText(centerTop, cx, bigY, bigTextPaint);
        canvas.drawText(centerBottom, cx, bigY + 20 * density, smallTextPaint);
    }
}