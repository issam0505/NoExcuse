package com.example.noexcuse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ProfessorAvatarView extends View {

    public static final int STATE_IDLE = 0;
    public static final int STATE_THINKING = 1;
    public static final int STATE_SPEAKING = 2;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private ValueAnimator animator;
    private float phase = 0f;
    private float mouthEnergy = 0f;
    private int state = STATE_IDLE;

    public ProfessorAvatarView(Context context) {
        super(context);
        init();
    }

    public ProfessorAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProfessorAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(1400L);
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setAvatarState(int newState) {
        state = newState;
        if (animator != null) {
            animator.setDuration(state == STATE_SPEAKING ? 640L : state == STATE_THINKING ? 950L : 1800L);
            if (!animator.isStarted()) {
                animator.start();
            }
        }
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isStarted()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float pulse = (float) Math.sin(phase * Math.PI * 2f);
        float lift = pulse * (state == STATE_IDLE ? size * 0.008f : size * 0.012f);

        drawGlow(canvas, cx, cy + lift, size);
        drawBody(canvas, cx, cy + size * 0.27f + lift, size);
        drawNeck(canvas, cx, cy + size * 0.08f + lift, size);
        drawHead(canvas, cx, cy - size * 0.12f + lift, size);
        drawHair(canvas, cx, cy - size * 0.12f + lift, size);
        drawProfessorCap(canvas, cx, cy - size * 0.12f + lift, size);
        drawFace(canvas, cx, cy - size * 0.12f + lift, size);
    }

    private void drawGlow(Canvas canvas, float cx, float cy, float size) {
        paint.setShader(new RadialGradient(
                cx, cy, size * 0.54f,
                new int[]{Color.argb(82, 33, 150, 243), Color.argb(28, 76, 175, 80), Color.TRANSPARENT},
                new float[]{0f, 0.56f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, size * 0.54f, paint);
        paint.setShader(null);
    }

    private void drawBody(Canvas canvas, float cx, float cy, float size) {
        float bodyW = size * 0.66f;
        float bodyH = size * 0.44f;

        rect.set(cx - bodyW / 2f, cy - bodyH * 0.35f, cx + bodyW / 2f, cy + bodyH * 0.55f);
        paint.setShader(new LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                new int[]{Color.rgb(36, 49, 64), Color.rgb(12, 18, 25)},
                null,
                Shader.TileMode.CLAMP
        ));
        paint.setShadowLayer(18f, 0f, 12f, Color.argb(130, 0, 0, 0));
        canvas.drawRoundRect(rect, size * 0.06f, size * 0.06f, paint);
        paint.clearShadowLayer();
        paint.setShader(null);

        paint.setColor(Color.rgb(235, 238, 241));
        path.reset();
        path.moveTo(cx - size * 0.13f, cy - bodyH * 0.3f);
        path.lineTo(cx, cy + bodyH * 0.18f);
        path.lineTo(cx + size * 0.13f, cy - bodyH * 0.3f);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(Color.rgb(33, 150, 243));
        path.reset();
        path.moveTo(cx - size * 0.025f, cy - bodyH * 0.18f);
        path.lineTo(cx + size * 0.025f, cy - bodyH * 0.18f);
        path.lineTo(cx + size * 0.04f, cy + bodyH * 0.34f);
        path.lineTo(cx, cy + bodyH * 0.44f);
        path.lineTo(cx - size * 0.04f, cy + bodyH * 0.34f);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawNeck(Canvas canvas, float cx, float cy, float size) {
        float neckW = size * 0.16f;
        float neckH = size * 0.16f;
        rect.set(cx - neckW / 2f, cy - neckH / 2f, cx + neckW / 2f, cy + neckH / 2f);
        paint.setColor(Color.rgb(218, 159, 116));
        canvas.drawRoundRect(rect, size * 0.03f, size * 0.03f, paint);
    }

    private void drawHead(Canvas canvas, float cx, float cy, float size) {
        float headW = size * 0.34f;
        float headH = size * 0.42f;
        float jawDrop = getSpeechEnergy() * size * 0.004f;
        rect.set(cx - headW / 2f, cy - headH / 2f, cx + headW / 2f, cy + headH / 2f + jawDrop);
        paint.setShader(new LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                new int[]{Color.rgb(255, 223, 188), Color.rgb(218, 143, 96), Color.rgb(135, 75, 55)},
                null,
                Shader.TileMode.CLAMP
        ));
        canvas.drawOval(rect, paint);
        paint.setShader(null);
    }

    private void drawHair(Canvas canvas, float cx, float cy, float size) {
        float headW = size * 0.38f;
        float headH = size * 0.43f;
        paint.setColor(Color.rgb(62, 45, 38));
        path.reset();
        path.moveTo(cx - headW * 0.5f, cy - headH * 0.03f);
        path.cubicTo(cx - headW * 0.56f, cy - headH * 0.5f, cx - headW * 0.18f, cy - headH * 0.64f, cx + headW * 0.2f, cy - headH * 0.54f);
        path.cubicTo(cx + headW * 0.54f, cy - headH * 0.44f, cx + headW * 0.54f, cy - headH * 0.08f, cx + headW * 0.45f, cy + headH * 0.42f);
        path.cubicTo(cx + headW * 0.2f, cy + headH * 0.22f, cx - headW * 0.18f, cy + headH * 0.22f, cx - headW * 0.45f, cy + headH * 0.42f);
        path.cubicTo(cx - headW * 0.52f, cy + headH * 0.12f, cx - headW * 0.5f, cy + headH * 0.02f, cx - headW * 0.5f, cy - headH * 0.03f);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawProfessorCap(Canvas canvas, float cx, float cy, float size) {
        float y = cy - size * 0.27f;
        paint.setColor(Color.rgb(22, 31, 42));
        path.reset();
        path.moveTo(cx - size * 0.22f, y);
        path.lineTo(cx, y - size * 0.085f);
        path.lineTo(cx + size * 0.22f, y);
        path.lineTo(cx, y + size * 0.075f);
        path.close();
        canvas.drawPath(path, paint);

        rect.set(cx - size * 0.105f, y + size * 0.018f, cx + size * 0.105f, y + size * 0.075f);
        canvas.drawRoundRect(rect, size * 0.016f, size * 0.016f, paint);

        paint.setColor(Color.rgb(33, 150, 243));
        paint.setStrokeWidth(size * 0.006f);
        canvas.drawLine(cx + size * 0.1f, y + size * 0.012f, cx + size * 0.16f, y + size * 0.12f, paint);
        canvas.drawCircle(cx + size * 0.16f, y + size * 0.125f, size * 0.012f, paint);
    }

    private void drawFace(Canvas canvas, float cx, float cy, float size) {
        float energy = getSpeechEnergy();
        float eyeY = cy - size * 0.055f;
        float eyeOffset = size * 0.062f;
        float eyeR = size * 0.012f;
        float look = getEyeLook(size);
        float blink = blinkPulse(0.18f, 0.024f) + (state == STATE_SPEAKING ? blinkPulse(0.72f, 0.025f) : 0f);
        float eyeH = Math.max(size * 0.002f, eyeR * (1f - blink * 0.82f));

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.009f);
        paint.setColor(Color.rgb(42, 49, 56));
        canvas.drawCircle(cx - eyeOffset, eyeY, size * 0.034f, paint);
        canvas.drawCircle(cx + eyeOffset, eyeY, size * 0.034f, paint);
        canvas.drawLine(cx - size * 0.028f, eyeY, cx + size * 0.028f, eyeY, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.rgb(33, 25, 20));
        rect.set(cx - eyeOffset - eyeR + look, eyeY - eyeH, cx - eyeOffset + eyeR + look, eyeY + eyeH);
        canvas.drawOval(rect, paint);
        rect.set(cx + eyeOffset - eyeR + look, eyeY - eyeH, cx + eyeOffset + eyeR + look, eyeY + eyeH);
        canvas.drawOval(rect, paint);

        paint.setColor(Color.rgb(90, 90, 86));
        paint.setStrokeWidth(size * 0.008f);
        canvas.drawLine(cx - eyeOffset - size * 0.03f, eyeY - size * 0.043f, cx - eyeOffset + size * 0.026f, eyeY - size * 0.047f, paint);
        canvas.drawLine(cx + eyeOffset - size * 0.026f, eyeY - size * 0.047f, cx + eyeOffset + size * 0.03f, eyeY - size * 0.043f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.007f);
        paint.setColor(Color.argb(105, 100, 62, 44));
        canvas.drawLine(cx, cy - size * 0.018f, cx - size * 0.012f, cy + size * 0.04f, paint);
        paint.setStyle(Paint.Style.FILL);

        drawMouth(canvas, cx, cy + energy * size * 0.004f, size, energy);

        if (state == STATE_THINKING) {
            paint.setColor(Color.argb(220, 33, 150, 243));
            float y = cy + size * 0.23f;
            for (int i = 0; i < 3; i++) {
                float r = size * (0.008f + 0.007f * (float) Math.max(0, Math.sin((phase + i * 0.18f) * Math.PI * 2f)));
                canvas.drawCircle(cx + (i - 1) * size * 0.045f, y, r, paint);
            }
        }
    }

    private void drawMouth(Canvas canvas, float cx, float cy, float size, float energy) {
        float mouthW = size * (0.092f + (state == STATE_SPEAKING ? energy * 0.006f : 0f));
        float open = state == STATE_SPEAKING ? size * (0.014f + energy * 0.04f) : size * 0.004f;
        float y = cy + size * 0.105f;

        paint.setColor(Color.rgb(120, 43, 45));
        path.reset();
        path.moveTo(cx - mouthW * 0.55f, y);
        path.cubicTo(cx - mouthW * 0.25f, y - size * 0.012f, cx + mouthW * 0.25f, y - size * 0.012f, cx + mouthW * 0.55f, y);
        path.cubicTo(cx + mouthW * 0.28f, y + size * 0.012f, cx - mouthW * 0.28f, y + size * 0.012f, cx - mouthW * 0.55f, y);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(Color.rgb(58, 18, 22));
        rect.set(cx - mouthW * 0.34f, y - open * 0.1f, cx + mouthW * 0.34f, y + open);
        canvas.drawOval(rect, paint);

        if (state == STATE_SPEAKING) {
            paint.setColor(Color.rgb(246, 239, 224));
            rect.set(cx - mouthW * 0.24f, y - open * 0.08f, cx + mouthW * 0.24f, y + open * 0.12f);
            canvas.drawArc(rect, 0f, -180f, true, paint);
        }
    }

    private void drawMustache(Canvas canvas, float cx, float cy, float size) {
        float y = cy + size * 0.075f;
        paint.setColor(Color.rgb(82, 82, 78));
        path.reset();
        path.moveTo(cx - size * 0.006f, y);
        path.cubicTo(cx - size * 0.035f, y - size * 0.018f, cx - size * 0.07f, y - size * 0.01f, cx - size * 0.095f, y + size * 0.006f);
        path.cubicTo(cx - size * 0.055f, y + size * 0.02f, cx - size * 0.025f, y + size * 0.014f, cx - size * 0.006f, y);
        path.close();
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(cx + size * 0.006f, y);
        path.cubicTo(cx + size * 0.035f, y - size * 0.018f, cx + size * 0.07f, y - size * 0.01f, cx + size * 0.095f, y + size * 0.006f);
        path.cubicTo(cx + size * 0.055f, y + size * 0.02f, cx + size * 0.025f, y + size * 0.014f, cx + size * 0.006f, y);
        path.close();
        canvas.drawPath(path, paint);
    }

    private float getSpeechEnergy() {
        if (state != STATE_SPEAKING) {
            mouthEnergy *= 0.55f;
            return mouthEnergy;
        }
        float fast = (float) Math.abs(Math.sin(phase * Math.PI * 10f));
        float medium = (float) Math.abs(Math.sin((phase + 0.21f) * Math.PI * 6f));
        mouthEnergy = Math.min(1f, 0.14f + fast * 0.52f + medium * 0.22f);
        return mouthEnergy;
    }

    private float getEyeLook(float size) {
        if (state == STATE_SPEAKING) {
            return (float) Math.sin(phase * Math.PI * 2.4f) * size * 0.005f;
        }
        return (lookPulse(0.28f, 0.055f, -1f) + lookPulse(0.64f, 0.06f, 1f)) * size * 0.01f;
    }

    private float lookPulse(float center, float width, float direction) {
        float distance = Math.abs(phase - center);
        distance = Math.min(distance, 1f - distance);
        if (distance > width) return 0f;
        return direction * (1f - distance / width);
    }

    private float blinkPulse(float center, float width) {
        float distance = Math.abs(phase - center);
        distance = Math.min(distance, 1f - distance);
        if (distance > width) return 0f;
        return Math.min(1f, 1f - distance / width);
    }
}
