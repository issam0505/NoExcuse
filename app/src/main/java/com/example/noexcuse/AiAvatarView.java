package com.example.noexcuse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class AiAvatarView extends View {

    public static final int STATE_IDLE = 0;
    public static final int STATE_THINKING = 1;
    public static final int STATE_SPEAKING = 2;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private Bitmap idleBitmap;
    private Bitmap thinkingBitmap;
    private Bitmap[] talkingBitmaps;
    private boolean hasRealAvatarFrames;
    private ValueAnimator animator;
    private float phase = 0f;
    private int state = STATE_IDLE;
    private float lastMouthOpen = 0f;

    public AiAvatarView(Context context) {
        super(context);
        init();
    }

    public AiAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AiAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        loadRealAvatarFrames();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(1000L);
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setAvatarState(int newState) {
        state = newState;
        if (animator != null) {
            animator.setDuration(state == STATE_SPEAKING ? 620L : state == STATE_THINKING ? 900L : 1800L);
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

        if (hasRealAvatarFrames) {
            drawRealAvatarFrame(canvas);
            return;
        }

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float pulse = (float) Math.sin(phase * Math.PI * 2f);
        float lift = pulse * (state == STATE_IDLE ? 5f : 9f);
        float glow = state == STATE_THINKING ? 0.35f + (pulse + 1f) * 0.2f : 0.28f;
        float size = Math.min(w, h);

        drawGlow(canvas, cx, cy + lift, size * (0.52f + glow * 0.08f));
        drawTorso(canvas, cx, cy + size * 0.22f + lift, size);
        drawNeck(canvas, cx, cy + size * 0.04f + lift, size);
        drawEars(canvas, cx, cy - size * 0.18f + lift, size);
        drawHead(canvas, cx, cy - size * 0.18f + lift, size, pulse);
        drawHair(canvas, cx, cy - size * 0.18f + lift, size);
        drawFace(canvas, cx, cy - size * 0.18f + lift, size, pulse);
    }

    private void loadRealAvatarFrames() {
        idleBitmap = decodeOptionalDrawable("ai_avatar_idle");
        thinkingBitmap = decodeOptionalDrawable("ai_avatar_thinking");
        talkingBitmaps = new Bitmap[]{
                decodeOptionalDrawable("ai_avatar_talk_1"),
                decodeOptionalDrawable("ai_avatar_talk_2"),
                decodeOptionalDrawable("ai_avatar_talk_3")
        };

        hasRealAvatarFrames = idleBitmap != null
                && thinkingBitmap != null
                && talkingBitmaps[0] != null
                && talkingBitmaps[1] != null
                && talkingBitmaps[2] != null;
    }

    private Bitmap decodeOptionalDrawable(String name) {
        int resId = getResources().getIdentifier(name, "drawable", getContext().getPackageName());
        if (resId == 0) {
            resId = getResources().getIdentifier(name, "drawable-nodpi", getContext().getPackageName());
        }
        return resId != 0 ? BitmapFactory.decodeResource(getResources(), resId) : null;
    }

    private void drawRealAvatarFrame(Canvas canvas) {
        Bitmap frame = idleBitmap;
        if (state == STATE_THINKING) {
            frame = thinkingBitmap;
        } else if (state == STATE_SPEAKING) {
            int index = Math.min(talkingBitmaps.length - 1, (int) (phase * talkingBitmaps.length));
            frame = talkingBitmaps[index];
        }

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float pulse = (float) Math.sin(phase * Math.PI * 2f);
        float lift = pulse * (state == STATE_IDLE ? 2f : 4f);
        float size = Math.min(w, h);

        drawGlow(canvas, cx, cy + lift, size * 0.5f);

        float frameW = w * 0.78f;
        float frameH = frameW * ((float) frame.getHeight() / (float) frame.getWidth());
        if (frameH > h * 0.96f) {
            frameH = h * 0.96f;
            frameW = frameH * ((float) frame.getWidth() / (float) frame.getHeight());
        }

        rect.set(cx - frameW / 2f, cy - frameH / 2f + lift, cx + frameW / 2f, cy + frameH / 2f + lift);
        paint.setShadowLayer(20f, 0f, 12f, Color.argb(140, 0, 0, 0));
        canvas.drawBitmap(frame, null, rect, paint);
        paint.clearShadowLayer();
    }

    private void drawGlow(Canvas canvas, float cx, float cy, float radius) {
        paint.setShader(new RadialGradient(
                cx, cy, radius,
                new int[]{Color.argb(95, 76, 175, 80), Color.argb(28, 33, 150, 243), Color.TRANSPARENT},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
    }

    private void drawTorso(Canvas canvas, float cx, float cy, float size) {
        float shoulderW = size * 0.82f;
        float torsoH = size * 0.5f;
        path.reset();
        path.moveTo(cx - shoulderW * 0.47f, cy - torsoH * 0.08f);
        path.cubicTo(cx - shoulderW * 0.35f, cy - torsoH * 0.34f, cx - shoulderW * 0.16f, cy - torsoH * 0.38f, cx, cy - torsoH * 0.34f);
        path.cubicTo(cx + shoulderW * 0.16f, cy - torsoH * 0.38f, cx + shoulderW * 0.35f, cy - torsoH * 0.34f, cx + shoulderW * 0.47f, cy - torsoH * 0.08f);
        path.lineTo(cx + shoulderW * 0.36f, cy + torsoH * 0.48f);
        path.lineTo(cx - shoulderW * 0.36f, cy + torsoH * 0.48f);
        path.close();

        paint.setShader(new LinearGradient(
                cx - shoulderW * 0.42f, cy - torsoH * 0.34f,
                cx + shoulderW * 0.36f, cy + torsoH * 0.5f,
                new int[]{Color.rgb(42, 42, 42), Color.rgb(8, 10, 12), Color.rgb(0, 0, 0)},
                null,
                Shader.TileMode.CLAMP
        ));
        paint.setShadowLayer(24f, 0f, 16f, Color.argb(155, 0, 0, 0));
        canvas.drawPath(path, paint);
        paint.clearShadowLayer();
        paint.setShader(null);

        paint.setColor(Color.argb(50, 255, 255, 255));
        paint.setStrokeWidth(size * 0.012f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx - shoulderW * 0.24f, cy - torsoH * 0.16f, cx - shoulderW * 0.32f, cy + torsoH * 0.38f, paint);
        canvas.drawLine(cx + shoulderW * 0.24f, cy - torsoH * 0.16f, cx + shoulderW * 0.32f, cy + torsoH * 0.38f, paint);
        paint.setStyle(Paint.Style.FILL);

        drawShirtLogo(canvas, cx, cy + torsoH * 0.08f, size);
    }

    private void drawShirtLogo(Canvas canvas, float cx, float cy, float size) {
        float markW = size * 0.18f;
        float markH = size * 0.21f;
        float top = cy - markH * 0.62f;
        float bottom = cy + markH * 0.34f;
        float mid = (top + bottom) / 2f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(size * 0.018f);
        paint.setColor(Color.WHITE);

        path.reset();
        path.moveTo(cx - markW * 0.45f, top);
        path.cubicTo(cx - markW * 0.22f, top - markH * 0.08f, cx + markW * 0.22f, top - markH * 0.08f, cx + markW * 0.45f, top);
        path.cubicTo(cx + markW * 0.42f, top + markH * 0.24f, cx + markW * 0.24f, mid - markH * 0.08f, cx, mid);
        path.cubicTo(cx - markW * 0.24f, mid - markH * 0.08f, cx - markW * 0.42f, top + markH * 0.24f, cx - markW * 0.45f, top);
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(cx - markW * 0.45f, bottom);
        path.cubicTo(cx - markW * 0.22f, bottom + markH * 0.08f, cx + markW * 0.22f, bottom + markH * 0.08f, cx + markW * 0.45f, bottom);
        path.cubicTo(cx + markW * 0.42f, bottom - markH * 0.24f, cx + markW * 0.24f, mid + markH * 0.08f, cx, mid);
        path.cubicTo(cx - markW * 0.24f, mid + markH * 0.08f, cx - markW * 0.42f, bottom - markH * 0.24f, cx - markW * 0.45f, bottom);
        canvas.drawPath(path, paint);

        paint.setStrokeWidth(size * 0.014f);
        canvas.drawLine(cx, top + markH * 0.18f, cx, bottom - markH * 0.18f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(size * 0.055f);
        canvas.drawText("NoExcuse", cx, cy + markH * 0.78f, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawNeck(Canvas canvas, float cx, float cy, float size) {
        float neckW = size * 0.18f;
        float neckH = size * 0.17f;
        rect.set(cx - neckW / 2f, cy - neckH / 2f, cx + neckW / 2f, cy + neckH / 2f);
        paint.setShader(new LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                new int[]{Color.rgb(226, 170, 126), Color.rgb(145, 82, 55)},
                null,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, neckW * 0.25f, neckW * 0.25f, paint);
        paint.setShader(null);
    }

    private void drawEars(Canvas canvas, float cx, float cy, float size) {
        float earW = size * 0.055f;
        float earH = size * 0.105f;
        float headW = size * 0.34f;
        paint.setColor(Color.rgb(202, 126, 86));
        rect.set(cx - headW * 0.55f, cy - earH * 0.08f, cx - headW * 0.55f + earW, cy + earH);
        canvas.drawOval(rect, paint);
        rect.set(cx + headW * 0.55f - earW, cy - earH * 0.08f, cx + headW * 0.55f, cy + earH);
        canvas.drawOval(rect, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.006f);
        paint.setColor(Color.argb(105, 93, 47, 34));
        canvas.drawArc(cx - headW * 0.535f, cy + earH * 0.16f, cx - headW * 0.50f + earW, cy + earH * 0.74f, -105f, 185f, false, paint);
        canvas.drawArc(cx + headW * 0.50f - earW, cy + earH * 0.16f, cx + headW * 0.535f, cy + earH * 0.74f, -80f, 185f, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHead(Canvas canvas, float cx, float cy, float size, float pulse) {
        float headW = size * 0.34f;
        float headH = size * 0.42f;
        float jawDrop = getSpeechEnergy() * size * 0.004f;
        rect.set(cx - headW / 2f, cy - headH / 2f, cx + headW / 2f, cy + headH / 2f + jawDrop);
        paint.setShader(new LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                new int[]{Color.rgb(255, 221, 185), Color.rgb(224, 148, 96), Color.rgb(121, 63, 46)},
                null,
                Shader.TileMode.CLAMP
        ));
        paint.setShadowLayer(22f, 0f, 12f, Color.argb(150, 0, 0, 0));
        canvas.drawOval(rect, paint);
        paint.clearShadowLayer();
        paint.setShader(null);

        paint.setColor(Color.argb(72, 255, 255, 255));
        canvas.drawOval(cx - headW * 0.28f, cy - headH * 0.25f, cx + headW * 0.08f, cy + headH * 0.08f, paint);

        paint.setColor(Color.argb(50, 82, 39, 29));
        canvas.drawOval(cx + headW * 0.11f, cy - headH * 0.16f, cx + headW * 0.39f, cy + headH * 0.23f, paint);
    }

    private void drawHair(Canvas canvas, float cx, float cy, float size) {
        float headW = size * 0.36f;
        float headH = size * 0.42f;
        path.reset();
        path.moveTo(cx - headW * 0.48f, cy - headH * 0.12f);
        path.cubicTo(cx - headW * 0.55f, cy - headH * 0.48f, cx - headW * 0.22f, cy - headH * 0.63f, cx + headW * 0.16f, cy - headH * 0.55f);
        path.cubicTo(cx + headW * 0.48f, cy - headH * 0.48f, cx + headW * 0.52f, cy - headH * 0.16f, cx + headW * 0.38f, cy + headH * 0.04f);
        path.cubicTo(cx + headW * 0.18f, cy - headH * 0.06f, cx - headW * 0.08f, cy - headH * 0.08f, cx - headW * 0.48f, cy - headH * 0.12f);
        path.close();

        paint.setShader(new LinearGradient(
                cx - headW * 0.4f, cy - headH * 0.58f,
                cx + headW * 0.35f, cy + headH * 0.06f,
                new int[]{Color.rgb(42, 28, 20), Color.rgb(16, 11, 10)},
                null,
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(path, paint);
        paint.setShader(null);
    }

    private void drawFace(Canvas canvas, float cx, float cy, float size, float pulse) {
        float speechEnergy = getSpeechEnergy();
        float blink = getBlinkAmount();
        float eyeLook = getEyeLookOffset(size);
        float jawDrop = speechEnergy * size * 0.004f;
        float eyeY = cy - size * 0.05f;
        float eyeOffset = size * 0.065f;
        float eyeR = size * (state == STATE_THINKING ? 0.014f + (pulse + 1f) * 0.003f : 0.015f);
        float visibleEyeH = Math.max(size * 0.002f, eyeR * (1f - blink * 0.86f));

        paint.setColor(Color.rgb(35, 20, 14));
        rect.set(cx - eyeOffset - eyeR, eyeY - visibleEyeH, cx - eyeOffset + eyeR, eyeY + visibleEyeH);
        canvas.drawOval(rect, paint);
        rect.set(cx + eyeOffset - eyeR, eyeY - visibleEyeH, cx + eyeOffset + eyeR, eyeY + visibleEyeH);
        canvas.drawOval(rect, paint);

        paint.setColor(Color.argb(190, 255, 255, 255));
        if (blink < 0.72f) {
            canvas.drawCircle(cx - eyeOffset + eyeLook + eyeR * 0.2f, eyeY - eyeR * 0.25f, eyeR * 0.28f, paint);
            canvas.drawCircle(cx + eyeOffset + eyeLook + eyeR * 0.2f, eyeY - eyeR * 0.25f, eyeR * 0.28f, paint);
        }

        paint.setColor(Color.argb(105, 95, 49, 35));
        paint.setStrokeWidth(size * 0.008f);
        float browLift = state == STATE_SPEAKING ? speechEnergy * size * 0.006f : 0f;
        canvas.drawLine(cx - eyeOffset - size * 0.03f, eyeY - size * 0.035f - browLift, cx - eyeOffset + size * 0.026f, eyeY - size * 0.043f - browLift * 0.4f, paint);
        canvas.drawLine(cx + eyeOffset - size * 0.026f, eyeY - size * 0.043f - browLift * 0.4f, cx + eyeOffset + size * 0.03f, eyeY - size * 0.035f - browLift, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.007f);
        paint.setColor(Color.argb(90, 100, 52, 38));
        canvas.drawLine(cx, cy - size * 0.015f, cx - size * 0.014f, cy + size * 0.042f, paint);
        paint.setStyle(Paint.Style.FILL);

        drawMouth(canvas, cx, cy + jawDrop, size, speechEnergy);

        if (state == STATE_THINKING) {
            paint.setColor(Color.argb(210, 76, 175, 80));
            float dotY = cy + size * 0.24f;
            for (int i = 0; i < 3; i++) {
                float offset = (i - 1) * size * 0.05f;
                float r = size * (0.01f + 0.008f * (float) Math.max(0, Math.sin((phase + i * 0.18f) * Math.PI * 2f)));
                canvas.drawCircle(cx + offset, dotY, r, paint);
            }
        }
    }

    private void drawMouth(Canvas canvas, float cx, float cy, float size, float speechEnergy) {
        float mouthW = size * (state == STATE_SPEAKING ? 0.104f + speechEnergy * 0.006f : 0.104f);
        float open = state == STATE_SPEAKING ? size * (0.014f + speechEnergy * 0.042f) : size * 0.004f;
        float mouthY = cy + size * 0.112f;

        paint.setColor(Color.rgb(116, 44, 45));
        path.reset();
        path.moveTo(cx - mouthW * 0.56f, mouthY);
        path.cubicTo(cx - mouthW * 0.28f, mouthY - size * 0.018f, cx + mouthW * 0.28f, mouthY - size * 0.018f, cx + mouthW * 0.56f, mouthY);
        path.cubicTo(cx + mouthW * 0.26f, mouthY + size * 0.014f, cx - mouthW * 0.26f, mouthY + size * 0.014f, cx - mouthW * 0.56f, mouthY);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(Color.rgb(65, 18, 22));
        rect.set(cx - mouthW * 0.32f, mouthY - open * 0.12f, cx + mouthW * 0.32f, mouthY + open);
        canvas.drawOval(rect, paint);

        if (state == STATE_SPEAKING) {
            paint.setColor(Color.rgb(245, 238, 224));
            rect.set(cx - mouthW * 0.24f, mouthY - open * 0.08f, cx + mouthW * 0.24f, mouthY + open * 0.12f);
            canvas.drawArc(rect, 0f, -180f, true, paint);

            paint.setColor(Color.rgb(170, 75, 76));
            rect.set(cx - mouthW * 0.18f, mouthY + open * 0.46f, cx + mouthW * 0.18f, mouthY + open * 0.92f);
            canvas.drawOval(rect, paint);
        } else {
            paint.setColor(Color.rgb(226, 119, 112));
            rect.set(cx - mouthW * 0.38f, mouthY - size * 0.003f, cx + mouthW * 0.38f, mouthY + size * 0.006f);
            canvas.drawRoundRect(rect, size * 0.006f, size * 0.006f, paint);
        }
    }

    private float getSpeechEnergy() {
        if (state != STATE_SPEAKING) {
            lastMouthOpen *= 0.55f;
            return lastMouthOpen;
        }
        float fast = (float) Math.abs(Math.sin(phase * Math.PI * 10f));
        float medium = (float) Math.abs(Math.sin((phase + 0.19f) * Math.PI * 6f));
        float slow = (float) Math.abs(Math.sin((phase + 0.33f) * Math.PI * 3f));
        lastMouthOpen = Math.min(1f, 0.12f + fast * 0.50f + medium * 0.22f + slow * 0.10f);
        return lastMouthOpen;
    }

    private float getBlinkAmount() {
        if (state == STATE_SPEAKING) {
            return blinkPulse(0.12f, 0.035f) + blinkPulse(0.68f, 0.028f);
        }
        return blinkPulse(0.18f, 0.025f);
    }

    private float getEyeLookOffset(float size) {
        if (state == STATE_SPEAKING) {
            return (float) Math.sin(phase * Math.PI * 2.6f) * size * 0.006f;
        }

        float look = 0f;
        look += lookPulse(0.28f, 0.055f, -1f);
        look += lookPulse(0.62f, 0.06f, 1f);
        return look * size * 0.012f;
    }

    private float lookPulse(float center, float width, float direction) {
        float distance = Math.abs(phase - center);
        distance = Math.min(distance, 1f - distance);
        if (distance > width) {
            return 0f;
        }
        return direction * (1f - (distance / width));
    }

    private float blinkPulse(float center, float width) {
        float distance = Math.abs(phase - center);
        distance = Math.min(distance, 1f - distance);
        if (distance > width) {
            return 0f;
        }
        return Math.min(1f, 1f - (distance / width));
    }
}
