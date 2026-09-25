package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

/**
 * The soft ground the pages wear, drawn for the screens of the settings:
 * the surface, three wide clouds of the accent's light drifting slowly at
 * the top, and a veil that gives the lower screen back to the surface, so
 * the lines below are read on quiet ground. The clouds move as the page's
 * do, a long breath out and back; where the phone asks for no animation,
 * they stand still.
 */
final class Glow extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint veil = new Paint();
    private float phase;
    private float drawnAt = -1f;
    private ValueAnimator drift;
    private android.view.View target;

    /**
     * The drift begun, while the screen it lies under is shown: the view
     * given is drawn again as it moves, or, with none, whatever holds the
     * ground as its background.
     */
    void start(android.view.View on) {
        target = on;
        if (drift != null) {
            return;
        }
        drift = ValueAnimator.ofFloat(0f, 1f);
        drift.setDuration(46000L);
        drift.setRepeatMode(ValueAnimator.REVERSE);
        drift.setRepeatCount(ValueAnimator.INFINITE);
        drift.setInterpolator(new LinearInterpolator());
        drift.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                phase = (Float) a.getAnimatedValue();
                /* The clouds move slowly: drawn again only when they have moved enough to be seen. */
                if (Math.abs(phase - drawnAt) < 0.001f) {
                    return;
                }
                drawnAt = phase;
                if (target != null) {
                    target.invalidate();
                } else {
                    invalidateSelf();
                }
            }
        });
        drift.start();
    }

    void stop() {
        if (drift != null) {
            drift.cancel();
            drift = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect box = getBounds();
        float w = box.width();
        float h = box.height();
        canvas.drawColor(Tone.surface());
        float s = (float) (0.5 - 0.5 * Math.cos(Math.PI * phase));
        cloud(canvas, Tone.lit(0.52f, 0.95f), box.left + w * (-0.18f + 0.15f * s), box.top + h * (-0.18f + 0.08f * s),
            w * 0.96f * (1f + 0.2f * s), h * 0.52f * (1f + 0.2f * s));
        cloud(canvas, Tone.lit(0.42f, 0.8f), box.left + w * (0.28f - 0.19f * s), box.top + h * (-0.06f + 0.11f * s),
            w * 0.80f * (1.06f - 0.18f * s), h * 0.44f * (1.06f - 0.18f * s));
        cloud(canvas, Tone.lit(0.34f, 0.7f), box.left + w * (-0.24f + 0.11f * s), box.top + h * (0.14f - 0.09f * s),
            w * 1.04f * (0.94f + 0.3f * s), h * 0.46f * (0.94f + 0.3f * s));
        int ground = Tone.surface() & 0x00FFFFFF;
        veil.setShader(new LinearGradient(0f, box.top, 0f, box.bottom,
            new int[] {(0x0D << 24) | ground, (0x57 << 24) | ground, (0xC7 << 24) | ground, 0xFF000000 | ground},
            new float[] {0f, 0.42f, 0.72f, 0.92f}, Shader.TileMode.CLAMP));
        canvas.drawRect(box, veil);
    }

    /** A cloud: a blurred oval of a colour, as the pages blur theirs. */
    private void cloud(Canvas canvas, int colour, float left, float top, float wide, float tall) {
        float cx = left + wide / 2f;
        float cy = top + tall / 2f;
        float r = Math.max(1f, Math.max(wide, tall) / 2f);
        int rgb = colour & 0x00FFFFFF;
        paint.setShader(new RadialGradient(cx, cy, r, new int[] {(0xE6 << 24) | rgb, (0x73 << 24) | rgb, rgb},
            new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.save();
        canvas.scale(wide / (2f * r), tall / (2f * r), cx, cy);
        canvas.drawCircle(cx, cy, r, paint);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
