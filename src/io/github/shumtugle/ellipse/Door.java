package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/**
 * The door to the drawer, as it stands on a screen: a tile of the look
 * like any other, and in its window either this home screen's own sign or
 * one of a few marks lying behind dark glass, lit in the colour the
 * material gives what is seen through glass.
 *
 * A mark is handed to the tile as an icon of two layers, the glass behind
 * and the mark in front, so it fills a window of any proportion the way an
 * application's own icon does, and is brought closer with the rest when
 * the icons are.
 */
final class Door {

    static final int OWN = 0;
    static final int DOTS = 1;
    static final int FOUR = 2;
    static final int RING = 3;
    static final int ARCH = 4;
    static final int RISE = 5;
    static final int STAR = 6;
    static final int KEYHOLE = 7;
    static final int GLASS = 8;

    /** The dictionary's name of each face, in the order of the numbers above. */
    static final String[] NAMES = {"door_own", "door_dots", "door_four", "door_ring", "door_arch", "door_rise",
        "door_star", "door_keyhole", "door_glass"};

    private Door() {
    }

    /** What stands in the door's window, for a face and the material of the moment. */
    static Drawable face(Context context, int face, int kind) {
        if (face <= OWN || face >= NAMES.length) {
            return context.getApplicationInfo().loadIcon(context.getPackageManager());
        }
        int glow = Cast.glow(kind);
        return new AdaptiveIconDrawable(new Glass(), new Mark(face, glow, (glow & 0x00FFFFFF) | 0x73000000));
    }

    /**
     * A face's mark in a square of the given side around a point: its
     * strong parts in one colour, its quiet parts in the other. Bare glass
     * and the door's own sign have no mark.
     */
    static void mark(Canvas canvas, Paint paint, int face, float cx, float cy, float side, int strong, int quiet) {
        float line = Math.max(1f, side * 0.075f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setShader(null);
        RectF box = new RectF();
        switch (face) {
            case DOTS: {
                float step = side * 0.3f;
                float r = side * 0.075f;
                for (int row = -1; row <= 1; row++) {
                    for (int col = -1; col <= 1; col++) {
                        fill(paint, Math.abs(row + col) % 2 == 0 ? strong : quiet);
                        canvas.drawCircle(cx + col * step, cy + row * step, r, paint);
                    }
                }
                break;
            }
            case FOUR: {
                float cell = side * 0.3f;
                float gap = side * 0.11f;
                for (int i = 0; i < 4; i++) {
                    float x = cx - gap / 2f - cell + (i % 2) * (cell + gap);
                    float y = cy - gap / 2f - cell + (i / 2) * (cell + gap);
                    box.set(x, y, x + cell, y + cell);
                    if (i == 0) {
                        fill(paint, strong);
                    } else {
                        stroke(paint, i == 3 ? strong : quiet, line);
                        box.inset(line / 2f, line / 2f);
                    }
                    canvas.drawRoundRect(box, cell * 0.3f, cell * 0.3f, paint);
                }
                break;
            }
            case RING: {
                // The tile's own shape, in outline, with a smaller one inside: the name of the thing.
                float wide = side * 0.84f;
                float tall = wide / Tile.Look.MEASURED.ratio;
                stroke(paint, strong, line);
                canvas.drawPath(Tile.curve(cx - wide / 2f, cy - tall / 2f, wide, tall, Tile.Look.MEASURED.power),
                    paint);
                float inner = wide * 0.46f;
                float innerTall = inner / Tile.Look.MEASURED.ratio;
                fill(paint, quiet);
                canvas.drawPath(Tile.curve(cx - inner / 2f, cy - innerTall / 2f, inner, innerTall,
                    Tile.Look.MEASURED.power), paint);
                break;
            }
            case ARCH: {
                float wide = side * 0.46f;
                float left = cx - wide / 2f;
                float right = cx + wide / 2f;
                float top = cy - side * 0.36f;
                float foot = cy + side * 0.34f;
                Path door = new Path();
                door.moveTo(left, foot);
                door.lineTo(left, top + wide / 2f);
                box.set(left, top, right, top + wide);
                door.arcTo(box, 180f, 180f, false);
                door.lineTo(right, foot);
                stroke(paint, strong, line);
                canvas.drawPath(door, paint);
                stroke(paint, quiet, line);
                canvas.drawLine(cx - side * 0.4f, foot, cx + side * 0.4f, foot, paint);
                fill(paint, strong);
                canvas.drawCircle(right - wide * 0.24f, cy + side * 0.06f, line * 0.8f, paint);
                break;
            }
            case RISE: {
                float half = side * 0.3f;
                float lift = side * 0.2f;
                Path upper = new Path();
                upper.moveTo(cx - half, cy + lift * 0.1f);
                upper.lineTo(cx, cy - lift * 1.2f);
                upper.lineTo(cx + half, cy + lift * 0.1f);
                stroke(paint, strong, line * 1.2f);
                canvas.drawPath(upper, paint);
                Path lower = new Path();
                lower.moveTo(cx - half, cy + lift * 1.3f);
                lower.lineTo(cx, cy);
                lower.lineTo(cx + half, cy + lift * 1.3f);
                stroke(paint, quiet, line * 1.2f);
                canvas.drawPath(lower, paint);
                break;
            }
            case STAR: {
                fill(paint, strong);
                canvas.drawPath(sparkle(cx - side * 0.06f, cy + side * 0.06f, side * 0.38f), paint);
                fill(paint, quiet);
                canvas.drawPath(sparkle(cx + side * 0.28f, cy - side * 0.28f, side * 0.14f), paint);
                break;
            }
            case KEYHOLE: {
                stroke(paint, quiet, line);
                canvas.drawCircle(cx, cy, side * 0.44f, paint);
                fill(paint, strong);
                canvas.drawCircle(cx, cy - side * 0.1f, side * 0.13f, paint);
                Path fall = new Path();
                fall.moveTo(cx - side * 0.055f, cy - side * 0.06f);
                fall.lineTo(cx - side * 0.12f, cy + side * 0.26f);
                fall.lineTo(cx + side * 0.12f, cy + side * 0.26f);
                fall.lineTo(cx + side * 0.055f, cy - side * 0.06f);
                fall.close();
                canvas.drawPath(fall, paint);
                break;
            }
            default:
                break;
        }
    }

    /** A star of four points, its sides drawn in towards the middle. */
    private static Path sparkle(float cx, float cy, float reach) {
        float pull = reach * 0.14f;
        Path star = new Path();
        star.moveTo(cx, cy - reach);
        star.quadTo(cx + pull, cy - pull, cx + reach, cy);
        star.quadTo(cx + pull, cy + pull, cx, cy + reach);
        star.quadTo(cx - pull, cy + pull, cx - reach, cy);
        star.quadTo(cx - pull, cy - pull, cx, cy - reach);
        star.close();
        return star;
    }

    private static void fill(Paint paint, int colour) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colour);
    }

    private static void stroke(Paint paint, int colour, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(colour);
    }

    /** The back layer: the dark glass of the windows, filling whatever it is given. */
    private static final class Glass extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            paint.setShader(Cast.glass(b.left, b.top, b.width(), b.height()));
            canvas.drawRect(b, paint);
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
            return PixelFormat.OPAQUE;
        }
    }

    /**
     * The front layer: the mark. A layer is half as large again as what
     * shows of it, so the mark takes two fifths of the layer and fills
     * about three fifths of the window.
     */
    private static final class Mark extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int face;
        private final int strong;
        private final int quiet;

        Mark(int face, int strong, int quiet) {
            this.face = face;
            this.strong = strong;
            this.quiet = quiet;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float side = Math.min(b.width(), b.height()) * 0.4f;
            mark(canvas, paint, face, b.exactCenterX(), b.exactCenterY(), side, strong, quiet);
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
    }
}
