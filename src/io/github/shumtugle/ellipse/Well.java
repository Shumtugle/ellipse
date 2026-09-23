package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * A field: the ground a piece of content lies on, described once and used
 * wherever such a ground is wanted — behind what a folder holds, and, when
 * the clock comes back, behind its dial, its weather and its charge.
 *
 * A field has three things about it and no more: a shape, a colour, and
 * how solid it stands. The colour is the scheme's own unless the owner
 * asks for one; how solid it stands runs from clear glass to opaque. One
 * setting therefore changes every field at once, and anything drawn on a
 * field looks like everything else drawn on one.
 */
final class Well {

    /** The shapes a field can be cut in. */
    static final int ROUNDED = 0;
    static final int SQUIRCLE = 1;
    static final int LIKE_TILE = 2;
    static final int CIRCLE = 3;

    /** How a field is described: the same few words wherever one is drawn. */
    static final class Look {

        final int shape;
        /** A hue of the owner's own, or below zero for the scheme's. */
        final int hue;
        /** From clear to opaque. */
        final float dense;

        Look(int shape, int hue, float dense) {
            this.shape = shape;
            this.hue = hue;
            this.dense = Math.max(0f, Math.min(1f, dense));
        }
    }

    private Well() {
    }

    /** What the owner has asked for, read once and handed round. */
    static Look of(Context context) {
        return Keep.well(context);
    }

    /** The colour of a field: its own hue if it has one, else the scheme's container. */
    static int colour(Look look) {
        int base = look.hue >= 0
            ? Tone.at(Tone.night() ? 20f : 90f, 26.0, look.hue)
            : Tone.of(Tone.SURFACE_CONTAINER);
        return (base & 0x00FFFFFF) | (Math.round(255f * look.dense) << 24);
    }

    /** The ink that reads on a field of that colour. */
    static int ink(Look look) {
        if (look.hue < 0) {
            return Tone.of(Tone.ON_SURFACE);
        }
        return Tone.night() ? 0xFFF4ECDD : 0xFF20201E;
    }

    /**
     * The outline of a field in a box: rounded as the shape scale has it,
     * a squircle, the tile's own shape, or a circle in the box's middle.
     */
    static Path shape(RectF box, Look look, Tile.Look tile, float radius) {
        if (look.shape == CIRCLE) {
            Path round = new Path();
            round.addCircle(box.centerX(), box.centerY(), Math.min(box.width(), box.height()) / 2f,
                Path.Direction.CW);
            return round;
        }
        if (look.shape == SQUIRCLE) {
            return Tile.curve(box.left, box.top, box.width(), box.height(), 4f);
        }
        if (look.shape == LIKE_TILE) {
            return Tile.curve(box.left, box.top, box.width(), box.height(), tile.power);
        }
        Path made = new Path();
        float r = Math.min(radius, Math.min(box.width(), box.height()) / 2f);
        made.addRoundRect(box, r, r, Path.Direction.CW);
        return made;
    }

    /** A field as a background for a view: the same ground, whatever stands on it. */
    static Drawable back(final Look look, final Tile.Look tile, final float radius) {
        return new Drawable() {

            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            public void draw(Canvas canvas) {
                Rect b = getBounds();
                paint.setColor(colour(look));
                canvas.drawPath(shape(new RectF(b), look, tile, Round.px(radius)), paint);
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
        };
    }
}
