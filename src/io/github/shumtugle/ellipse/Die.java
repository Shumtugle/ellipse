package io.github.shumtugle.ellipse;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import java.util.Random;

/**
 * The face of the home screen's dice: a die showing a thrown number of pips,
 * one to six as a die has; now and then seven, which no die has; and once in
 * a long while no pips at all, but the sign of the endless. Drawn on the grid
 * symbols are drawn on, in one colour given as a tint.
 */
final class Die extends Drawable {

    /** The face showing the endless instead of pips. */
    static final int ENDLESS = 0;

    private static final Random DICE = new Random();

    /** A throw: one to six, seven one time in fifty, the endless one time in five hundred. */
    static int roll() {
        int r = DICE.nextInt(500);
        if (r == 0) {
            return ENDLESS;
        }
        if (r <= 10) {
            return 7;
        }
        return 1 + DICE.nextInt(6);
    }

    /** Where the pips of each number stand, on a grid of three by three: column, row. */
    private static final int[][][] PIPS = {
        {},
        {{1, 1}},
        {{0, 0}, {2, 2}},
        {{0, 0}, {1, 1}, {2, 2}},
        {{0, 0}, {2, 0}, {0, 2}, {2, 2}},
        {{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}},
        {{0, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {2, 2}},
        {{0, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}, {0, 2}, {2, 2}},
    };

    private final int shown;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int colour = 0xFFFFFFFF;

    Die(int shown) {
        this.shown = shown;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float u = Math.min(b.width(), b.height()) / 24f;
        canvas.save();
        canvas.translate(b.exactCenterX() - 12f * u, b.exactCenterY() - 12f * u);
        paint.setColor(colour);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * u);
        canvas.drawRoundRect(new RectF(3.5f * u, 3.5f * u, 20.5f * u, 20.5f * u), 4f * u, 4f * u, paint);
        if (shown == ENDLESS) {
            Path endless = new Path();
            for (int k = 0; k <= 96; k++) {
                double t = 2 * Math.PI * k / 96;
                double d = 1 + Math.sin(t) * Math.sin(t);
                float x = (float) (12 + 6.2 * Math.cos(t) / d);
                float y = (float) (12 + 6.2 * Math.sin(t) * Math.cos(t) / d);
                if (k == 0) {
                    endless.moveTo(x * u, y * u);
                } else {
                    endless.lineTo(x * u, y * u);
                }
            }
            paint.setStrokeWidth(1.8f * u);
            canvas.drawPath(endless, paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            int n = Math.max(1, Math.min(7, shown));
            for (int[] at : PIPS[n]) {
                canvas.drawCircle((7.5f + at[0] * 4.5f) * u, (7.5f + at[1] * 4.5f) * u, 1.55f * u, paint);
            }
        }
        canvas.restore();
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (tint != null) {
            colour = tint.getDefaultColor();
            invalidateSelf();
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 96;
    }

    @Override
    public int getIntrinsicHeight() {
        return 96;
    }
}
