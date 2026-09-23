package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * A switch in the design system's shape: a track as round as it is tall,
 * and in it a thumb that is small and hollow-toned when off, large and
 * lit when on, sliding and growing between the two.
 */
final class Toggle extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private float at;
    private boolean on;
    private ValueAnimator moving;

    Toggle(Context context, boolean on) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        this.on = on;
        at = on ? 1f : 0f;
        edge.setStyle(Paint.Style.STROKE);
    }

    boolean on() {
        return on;
    }

    void set(boolean value) {
        if (value == on) {
            return;
        }
        on = value;
        if (moving != null) {
            moving.cancel();
        }
        moving = ValueAnimator.ofFloat(at, on ? 1f : 0f);
        moving.setDuration(Pace.ARRIVE / 2);
        moving.setInterpolator(Pace.EMPHASIS);
        moving.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                at = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        moving.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Math.round(52f * density), Math.round(32f * density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float r = h / 2f;
        int offTrack = Tone.containerHigh();
        int onTrack = Tone.primary();
        track.setColor(blend(offTrack, onTrack, at));
        canvas.drawRoundRect(0f, 0f, w, h, r, r, track);
        if (at < 1f) {
            edge.setStrokeWidth(2f * density);
            edge.setColor(Tone.faint());
            edge.setAlpha(Math.round(255 * (1f - at)));
            float s = density;
            canvas.drawRoundRect(s, s, w - s, h - s, r - s, r - s, edge);
        }
        float small = 8f * density;
        float large = 12f * density;
        float radius = small + (large - small) * at;
        float cx = r + (w - 2f * r) * at;
        thumb.setColor(blend(Tone.faint(), Tone.onAccent(), at));
        canvas.drawCircle(cx, h / 2f, radius, thumb);
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return 0xFF000000 | (Math.round(ar + (br - ar) * t) << 16)
            | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }
}
