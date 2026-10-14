package com.vestigia.app.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// Two-series line chart: items reported (slate) vs items returned (teal)
public class LineChartView extends View {

    private static final int COLOR_REPORTED = 0xFF64748B;
    private static final int COLOR_RETURNED = 0xFF0F766E;
    private static final int COLOR_GRID = 0xFFE2E8F0;
    private static final int COLOR_TEXT = 0xFF64748B;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;

    private List<String> labels = new ArrayList<>();
    private int[] reported = new int[0];
    private int[] returned = new int[0];

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;

        gridPaint.setColor(COLOR_GRID);
        gridPaint.setStrokeWidth(density);

        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<String> labels, int[] reported, int[] returned) {
        this.labels = labels != null ? labels : new ArrayList<>();
        this.reported = reported != null ? reported : new int[0];
        this.returned = returned != null ? returned : new int[0];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int n = labels.size();
        float left = 30 * density, right = 10 * density, top = 10 * density, bottom = 24 * density;
        float chartW = getWidth() - left - right;
        float chartH = getHeight() - top - bottom;
        if (chartW <= 0 || chartH <= 0) return;

        int max = 0;
        for (int v : reported) max = Math.max(max, v);
        for (int v : returned) max = Math.max(max, v);
        int step = Math.max(1, (int) Math.ceil(max / 4.0)); // whole-number ticks only
        int niceMax = step * 4;

        // Grid lines + y-axis numbers
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            float y = top + chartH - chartH * i / 4f;
            canvas.drawLine(left, y, left + chartW, y, gridPaint);
            canvas.drawText(String.valueOf(i * step), left - 6 * density, y + textPaint.getTextSize() / 3f, textPaint);
        }
        if (n == 0) return;

        // x-axis labels (at most 5 so they never overlap)
        textPaint.setTextAlign(Paint.Align.LEFT);
        int labelCount = Math.min(5, n);
        for (int i = 0; i < labelCount; i++) {
            int idx = labelCount == 1 ? 0 : Math.round(i * (n - 1) / (float) (labelCount - 1));
            float x = xFor(idx, n, left, chartW);
            String text = labels.get(idx);
            float tw = textPaint.measureText(text);
            float tx = Math.max(0, Math.min(x - tw / 2f, getWidth() - tw));
            canvas.drawText(text, tx, getHeight() - 6 * density, textPaint);
        }

        drawSeries(canvas, reported, COLOR_REPORTED, n, left, top, chartW, chartH, niceMax);
        drawSeries(canvas, returned, COLOR_RETURNED, n, left, top, chartW, chartH, niceMax);
    }

    private void drawSeries(Canvas canvas, int[] data, int color, int n,
                            float left, float top, float chartW, float chartH, int niceMax) {
        if (data.length == 0) return;

        linePaint.setColor(color);
        dotPaint.setColor(color);
        int count = Math.min(n, data.length);

        path.reset();
        for (int i = 0; i < count; i++) {
            float x = xFor(i, n, left, chartW);
            float y = top + chartH - chartH * data[i] / (float) niceMax;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);

        if (n <= 16) { // dots only when there are few points
            for (int i = 0; i < count; i++) {
                float x = xFor(i, n, left, chartW);
                float y = top + chartH - chartH * data[i] / (float) niceMax;
                canvas.drawCircle(x, y, 3.5f * density, dotPaint);
            }
        }
    }

    private float xFor(int i, int n, float left, float chartW) {
        return n == 1 ? left + chartW / 2f : left + chartW * i / (float) (n - 1);
    }
}