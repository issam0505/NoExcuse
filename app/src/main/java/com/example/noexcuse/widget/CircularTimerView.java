package com.example.noexcuse.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * CircularTimerView
 *
 * Custom view katbyan:
 *  – cercle background (griw ghamc)
 *  – arc progressif dyal l-crono (orange, stroke-linecap round)
 *  – text dyal lwaqt f l-wst (e.g. "2:00")
 *
 * Kifach tkhdm biha:
 *   CircularTimerView timer = findViewById(R.id.circularTimer);
 *   timer.setTotalSeconds(120);
 *   timer.setRemainingSeconds(75);   ← katupdate kol second
 */
public class CircularTimerView extends View {

    // ── Paints ────────────────────────────────────────────────────────────

    private final Paint trackPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── State ─────────────────────────────────────────────────────────────

    private int totalSeconds     = 120;
    private int remainingSeconds = 120;

    // ── Geometry ──────────────────────────────────────────────────────────

    private final RectF arcRect = new RectF();
    private float strokeWidth;

    // ─────────────────────────────────────────────────────────────────────

    public CircularTimerView(Context context) {
        super(context);
        init();
    }

    public CircularTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularTimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        strokeWidth = 10f * density;   // 10dp → px

        // Track (cercle background gris)
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(0xFF1F1F1F);
        trackPaint.setStrokeWidth(strokeWidth);

        // Arc progressif (orange)
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setColor(0xFFFF6D00);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Chiffres dyal lwaqt f lwst (blanc, kbir)
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(false);
        // size set f onSizeChanged

        // Label optionnel (machi msta3mla hna, gha reserve)
        labelPaint.setColor(0xFF4B5563);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ─── Size ─────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float padding = strokeWidth / 2f + 2f;
        arcRect.set(padding, padding, w - padding, h - padding);

        // Font size = 40% de la hauteur du view
        textPaint.setTextSize(h * 0.38f);
        labelPaint.setTextSize(h * 0.10f);
    }

    // ─── Draw ─────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int cx = getWidth()  / 2;
        int cy = getHeight() / 2;

        // 1. Track complet
        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint);

        // 2. Arc progressif
        //    Ibda men -90° (top) u igi clock-wise
        float ratio   = totalSeconds > 0
                ? (float) remainingSeconds / totalSeconds
                : 0f;
        float sweepDeg = 360f * ratio;
        canvas.drawArc(arcRect, -90f, sweepDeg, false, arcPaint);

        // 3. Chiffres f lwst: "M:SS"
        int mins    = remainingSeconds / 60;
        int secs    = remainingSeconds % 60;
        String text = mins + ":" + String.format(java.util.Locale.getDefault(), "%02d", secs);

        // Center vertically (baseline correction)
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(text, cx, textY, textPaint);
    }

    // ─── Public API ───────────────────────────────────────────────────────

    /**
     * Hdi total seconds (e.g. 120 = 2 min)
     * Atsama mra w7da qbel ma-tbda l-crono
     */
    public void setTotalSeconds(int total) {
        this.totalSeconds = total;
        this.remainingSeconds = total;
        invalidate();
    }

    /**
     * Update remaining seconds kol tick
     * Atsama f onTick dyal CountDownTimer
     */
    public void setRemainingSeconds(int remaining) {
        this.remainingSeconds = Math.max(0, remaining);
        invalidate();   // redraw
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}