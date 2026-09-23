package io.github.shumtugle.ellipse;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/**
 * The door to the drawer, as it stands on a screen: a tile of the look
 * like any other, and in its window either a whole icon of the kind a
 * drawer's door has long worn, a disc or a squircle with dots in it, or
 * one of a few marks lying behind dark glass, lit in the colour the
 * material gives what is seen through glass. With no mask, a whole icon
 * stands as it is, in its own shape.
 *
 * A mark is handed to the tile as an icon of two layers, the glass behind
 * and the mark in front, so it fills a window of any proportion the way an
 * application's own icon does, and is brought closer with the rest when
 * the icons are.
 */
final class Door {

    static final int DOTS = 1;
    static final int FOUR = 2;
    static final int RING = 3;
    static final int ARCH = 4;
    static final int RISE = 5;
    static final int STAR = 6;
    static final int KEYHOLE = 7;
    static final int GLASS = 8;
    static final int DISC_LIGHT = 9;
    static final int DISC_DARK = 10;
    static final int SQUIRCLE = 11;
    static final int COLOURS = 12;
    static final int DISC_BLUE = 13;

    /** The face a door wears until the owner chooses another. */
    static final int DEFAULT = DISC_BLUE;

    /**
     * The faces in the order they are offered: the whole icons first, then
     * the marks. A face keeps its number whatever its place here, because
     * the number is what is written down.
     */
    static final int[] ORDER = {DISC_BLUE, DISC_LIGHT, DISC_DARK, SQUIRCLE, COLOURS, DOTS, FOUR, RING, ARCH, RISE, STAR,
        KEYHOLE, GLASS};

    /** Whether a number written down is a face there still is. */
    static boolean known(int face) {
        return face >= DOTS && face <= DISC_BLUE;
    }

    /** The dictionary's name of a face. */
    static String name(int face) {
        switch (face) {
            case DOTS: return "door_dots";
            case FOUR: return "door_four";
            case RING: return "door_ring";
            case ARCH: return "door_arch";
            case RISE: return "door_rise";
            case STAR: return "door_star";
            case KEYHOLE: return "door_keyhole";
            case GLASS: return "door_glass";
            case DISC_DARK: return "door_disc_dark";
            case SQUIRCLE: return "door_squircle";
            case COLOURS: return "door_colours";
            case DISC_BLUE: return "door_disc_blue";
            default: return "door_disc_light";
        }
    }

    private Door() {
    }

    /**
     * What stands in the door's window, for a face and the material of the
     * moment. A whole icon with no mask over it is itself; set in a window,
     * it lies behind the glass like the marks.
     */
    static Drawable face(int face, int kind, boolean bare) {
        if (!known(face)) {
            face = DEFAULT;
        }
        if (whole(face)) {
            return bare ? new Whole(face, 1f) : new AdaptiveIconDrawable(new Glass(), new Whole(face, 0.42f));
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

    // ------------------------------------------------------------ whole icons

    /** Whether a face is a whole icon with a shape of its own, rather than a mark behind glass. */
    static boolean whole(int face) {
        return face >= DISC_LIGHT && face <= DISC_BLUE;
    }

    /**
     * A whole icon in a square of the given side around a point: its own
     * shape, lit from above, and the shadow it throws, all inside the square.
     */
    static void whole(Canvas canvas, Paint paint, int face, float cx, float cy, float side) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        switch (face) {
            case DISC_LIGHT:
                disc(canvas, paint, cx, cy, side, false);
                break;
            case DISC_DARK:
                disc(canvas, paint, cx, cy, side, true);
                break;
            case DISC_BLUE:
                blue(canvas, paint, cx, cy, side);
                break;
            case SQUIRCLE:
                squircle(canvas, paint, cx, cy, side);
                break;
            case COLOURS:
                colours(canvas, paint, cx, cy, side);
                break;
            default:
                break;
        }
        paint.clearShadowLayer();
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    /** A disc with six dots in two rows: light with the dots sunk into it, or dark with them raised in white. */
    private static void disc(Canvas canvas, Paint paint, float cx, float cy, float side, boolean dark) {
        // As wide as the round icons beside it, with room left for no more than a breath of shadow.
        float r = side * 0.48f;
        paint.setColor(dark ? 0xFF16181B : 0xFFF2F2F2);
        paint.setShadowLayer(side * 0.016f, 0f, side * 0.01f, dark ? 0x73000000 : 0x40000000);
        canvas.drawCircle(cx, cy, r, paint);
        paint.clearShadowLayer();
        paint.setShader(new RadialGradient(cx, cy - r * 0.6f, r * 1.7f,
            dark ? new int[] {0xFF4E525A, 0xFF25282D, 0xFF111316} : new int[] {0xFFFFFFFF, 0xFFF4F4F4, 0xFFE0E0E0},
            new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        rim(canvas, paint, cx, cy, r, side, dark);
        float across = r * 0.38f;
        float down = r * 0.2f;
        float dot = r * 0.125f;
        for (int row = -1; row <= 1; row += 2) {
            for (int col = -1; col <= 1; col++) {
                float x = cx + col * across;
                float y = cy + row * down;
                if (dark) {
                    paint.setShadowLayer(dot * 0.5f, 0f, dot * 0.25f, 0x80000000);
                    paint.setShader(new RadialGradient(x, y - dot * 0.4f, dot * 1.4f, 0xFFFFFFFF, 0xFFDCE0E5,
                        Shader.TileMode.CLAMP));
                } else {
                    // Sunk into the plate: dark at the top, where the lip
                    // hides the light, and caught by it along the lower edge.
                    // Shallower than this and the holes are not there at all.
                    paint.setShader(new LinearGradient(0f, y - dot, 0f, y + dot, 0xFF44474C, 0xFF8B8F95,
                        Shader.TileMode.CLAMP));
                }
                canvas.drawCircle(x, y, dot, paint);
                paint.clearShadowLayer();
                if (!dark) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(1f, dot * 0.22f));
                    paint.setShader(new LinearGradient(0f, y - dot * 1.1f, 0f, y + dot * 1.1f, 0x00FFFFFF,
                        0xFFFFFFFF, Shader.TileMode.CLAMP));
                    canvas.drawCircle(x, y, dot * 1.02f, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
        }
    }

    /** A disc of clear sky blue, its six dots raised in white. */
    private static void blue(Canvas canvas, Paint paint, float cx, float cy, float side) {
        float r = side * 0.48f;
        paint.setColor(0xFF3F86E6);
        paint.setShadowLayer(side * 0.016f, 0f, side * 0.01f, 0x59000000);
        canvas.drawCircle(cx, cy, r, paint);
        paint.clearShadowLayer();
        paint.setShader(new RadialGradient(cx, cy - r * 0.6f, r * 1.7f,
            new int[] {0xFF9CCBFF, 0xFF4C93EE, 0xFF2A66C8}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        rim(canvas, paint, cx, cy, r, side, true);
        float across = r * 0.38f;
        float down = r * 0.2f;
        float dot = r * 0.11f;
        for (int row = -1; row <= 1; row += 2) {
            for (int col = -1; col <= 1; col++) {
                float x = cx + col * across;
                float y = cy + row * down;
                paint.setShadowLayer(dot * 0.5f, 0f, dot * 0.25f, 0x59102850);
                paint.setShader(new RadialGradient(x, y - dot * 0.4f, dot * 1.4f, 0xFFFFFFFF, 0xFFE3EEFB,
                    Shader.TileMode.CLAMP));
                canvas.drawCircle(x, y, dot, paint);
                paint.clearShadowLayer();
            }
        }
    }

    /** The light along the upper edge of a disc, and the fine line of its edge. */
    private static void rim(Canvas canvas, Paint paint, float cx, float cy, float r, float side, boolean dark) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, side * 0.01f));
        paint.setShader(new LinearGradient(0f, cy - r, 0f, cy + r, dark ? 0x40FFFFFF : 0xB3FFFFFF, 0x00FFFFFF,
            Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r - side * 0.006f, paint);
        paint.setShader(null);
        paint.setStrokeWidth(Math.max(1f, side * 0.006f));
        paint.setColor(dark ? 0x80000000 : 0x1F000000);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /** A soft white squircle, the dots in it dark and each lit round by a halo of the white. */
    private static void squircle(Canvas canvas, Paint paint, float cx, float cy, float side) {
        float wide = side * 0.94f;
        float l = cx - wide / 2f;
        float t = cy - wide / 2f;
        Path shape = Tile.curve(l, t, wide, wide, 4.5f);
        paint.setColor(0xFFF1F2F5);
        paint.setShadowLayer(side * 0.016f, 0f, side * 0.01f, 0x40000000);
        canvas.drawPath(shape, paint);
        paint.clearShadowLayer();
        paint.setShader(new LinearGradient(0f, t, 0f, t + wide, 0xFFFFFFFF, 0xFFE2E4E9, Shader.TileMode.CLAMP));
        canvas.drawPath(shape, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, side * 0.014f));
        paint.setShader(new LinearGradient(0f, t, 0f, t + wide, 0xE6FFFFFF, 0x59FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawPath(Tile.curve(l + side * 0.007f, t + side * 0.007f, wide - side * 0.014f, wide - side * 0.014f,
            4.5f), paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        float across = wide * 0.235f;
        float down = wide * 0.12f;
        float dot = wide * 0.077f;
        for (int row = -1; row <= 1; row += 2) {
            for (int col = -1; col <= 1; col++) {
                float x = cx + col * across;
                float y = cy + row * down;
                paint.setShadowLayer(dot * 0.55f, 0f, 0f, 0xF2FFFFFF);
                paint.setShader(new RadialGradient(x - dot * 0.25f, y - dot * 0.35f, dot * 1.3f, 0xFF6C6D71,
                    0xFF2C2D30, Shader.TileMode.CLAMP));
                canvas.drawCircle(x, y, dot, paint);
                paint.clearShadowLayer();
            }
        }
    }

    /**
     * A pale plate with nine dots, each of another colour: the owner's own
     * hue first, then round the circle of hues, each throwing a small shadow.
     */
    private static void colours(Canvas canvas, Paint paint, float cx, float cy, float side) {
        float wide = side * 0.94f;
        float l = cx - wide / 2f;
        float t = cy - wide / 2f;
        Path plate = Tile.curve(l, t, wide, wide, 5f);
        paint.setColor(0xFFF5F5F5);
        paint.setShadowLayer(side * 0.016f, 0f, side * 0.01f, 0x40000000);
        canvas.drawPath(plate, paint);
        paint.clearShadowLayer();
        paint.setShader(new LinearGradient(0f, t, 0f, t + wide, 0xFFFBFBFB, 0xFFEBEBEB, Shader.TileMode.CLAMP));
        canvas.drawPath(plate, paint);
        paint.setShader(null);
        float step = wide * 0.22f;
        float dot = wide * 0.075f;
        float hue = Tone.hue();
        for (int i = 0; i < 9; i++) {
            float x = cx + (i % 3 - 1) * step;
            float y = cy + (i / 3 - 1) * step;
            paint.setColor(Tone.at(62f, 52.0, (hue + i * 40f) % 360f));
            paint.setShadowLayer(dot * 0.3f, dot * 0.12f, dot * 0.2f, 0x66000000);
            canvas.drawCircle(x, y, dot, paint);
            paint.clearShadowLayer();
        }
    }

    /** The front layer that sets a whole icon behind the glass of a tile's window. */
    private static final class Whole extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int face;
        /** How much of the shorter side the icon takes. */
        private final float share;

        Whole(int face, float share) {
            this.face = face;
            this.share = share;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            whole(canvas, paint, face, b.exactCenterX(), b.exactCenterY(), Math.min(b.width(), b.height()) * share);
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
