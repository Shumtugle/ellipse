package io.github.shumtugle.ellipse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/**
 * The shape every icon is cut to, one for all of them, so the grid reads
 * as one set whatever each app brought.
 *
 * An app's icon comes in layers: a ground that covers a larger square than
 * shows, and a picture in its middle. The layers are laid out here as the
 * platform lays them out, and cut by the chosen outline. The phone's own
 * outline is kept as the phone draws it. An icon that comes as one flat
 * picture is set into the outline: on a ground of the colour at its own
 * edges, a little smaller than the outline; or, if it is solid to its
 * edges already, spread to fill the outline and cut.
 *
 * Outlines differ in how much of their square they fill, and a circle
 * beside a square of the same size looks smaller. Each outline is drawn a
 * little smaller the more it fills, so a grid of any outline weighs the
 * same.
 */
final class Shape {

    static final int SYSTEM = 0;
    static final int CIRCLE = 1;
    static final int SQUIRCLE = 2;
    static final int ROUNDED = 3;
    /** A drop: round on three corners, square on one, in four turns. */
    static final int TEAR_LOWER_RIGHT = 4;
    static final int TEAR_LOWER_LEFT = 5;
    static final int TEAR_UPPER_LEFT = 6;
    static final int TEAR_UPPER_RIGHT = 7;
    /**
     * A wide tile of paper in a thin grey rim, wider than it is tall by the
     * owner's own old masks, its corners turned by a seventh of its height.
     */
    static final int PAPER = 8;
    static final int COUNT = 9;

    /** How much wider than tall the paper tile stands. */
    static final float PAPER_WIDE = 1.29f;
    private static final int PAPER_INK = 0xFFF7F7F7;
    private static final int RIM_INK = 0xFFBDBDBD;

    /** The outline chosen now; the settings set it, every icon drawn after reads it. */
    static int current = SYSTEM;

    private Shape() {
    }

    /** The outline in a square of the given side, from its top left corner. */
    static Path outline(int shape, float side) {
        Path path = new Path();
        float r = side / 2f;
        switch (shape) {
            case CIRCLE:
                path.addCircle(r, r, r, Path.Direction.CW);
                break;
            case SQUIRCLE:
                /* A superellipse of the fifth power: the sides run straight
                   longer than a circle's and turn the corner more softly
                   than a rounded square's. */
                for (int i = 0; i <= 180; i++) {
                    double t = 2 * Math.PI * i / 180;
                    double c = Math.cos(t);
                    double s = Math.sin(t);
                    float x = (float) (r + r * Math.signum(c) * Math.pow(Math.abs(c), 2.0 / 5.0));
                    float y = (float) (r + r * Math.signum(s) * Math.pow(Math.abs(s), 2.0 / 5.0));
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                path.close();
                break;
            case ROUNDED:
                path.addRoundRect(new RectF(0, 0, side, side), side * 0.22f, side * 0.22f, Path.Direction.CW);
                break;
            case TEAR_LOWER_RIGHT:
            case TEAR_LOWER_LEFT:
            case TEAR_UPPER_LEFT:
            case TEAR_UPPER_RIGHT:
                float[] corners = new float[8];
                java.util.Arrays.fill(corners, r);
                /* Radii run from the upper left clockwise, two numbers a corner. */
                int square = shape == TEAR_UPPER_LEFT ? 0 : shape == TEAR_UPPER_RIGHT ? 1
                    : shape == TEAR_LOWER_RIGHT ? 2 : 3;
                corners[square * 2] = side * 0.12f;
                corners[square * 2 + 1] = side * 0.12f;
                path.addRoundRect(new RectF(0, 0, side, side), corners, Path.Direction.CW);
                break;
            case PAPER:
                float tall = side / PAPER_WIDE;
                float top = (side - tall) / 2f;
                path.addRoundRect(new RectF(0, top, side, top + tall), tall * 0.14f, tall * 0.14f,
                    Path.Direction.CW);
                break;
            default:
                path.addRoundRect(new RectF(0, 0, side, side), side * 0.5f, side * 0.5f, Path.Direction.CW);
                break;
        }
        return path;
    }

    /** How much smaller than its square an outline is drawn, so every outline weighs the same. */
    static float weight(int shape) {
        switch (shape) {
            case SQUIRCLE:
                return 0.93f;
            case ROUNDED:
                return 0.92f;
            case TEAR_LOWER_RIGHT:
            case TEAR_LOWER_LEFT:
            case TEAR_UPPER_LEFT:
            case TEAR_UPPER_RIGHT:
                return 0.97f;
            default:
                return 1f;
        }
    }

    /** An icon as it is to be drawn: cut to the outline when it comes in layers. */
    static Drawable face(Drawable icon) {
        if (icon == null || current == SYSTEM) {
            return icon;
        }
        if (current == PAPER) {
            /* The paper takes every icon, flat ones too: they lie on it as
               the old masks laid them, at seven tenths of its height. */
            return new Cut(icon, current);
        }
        return new Cut(icon, current);
    }

    /** A fine grain for the paper and its rim, made once. */
    private static android.graphics.BitmapShader grain;

    private static android.graphics.BitmapShader grain() {
        if (grain == null) {
            java.util.Random dice = new java.util.Random(7);
            int n = 96;
            int[] dots = new int[n * n];
            for (int i = 0; i < dots.length; i++) {
                int v = dice.nextInt(256);
                dots[i] = ((dice.nextInt(22)) << 24) | (v << 16) | (v << 8) | v;
            }
            Bitmap tile = Bitmap.createBitmap(dots, n, n, Bitmap.Config.ARGB_8888);
            grain = new android.graphics.BitmapShader(tile, android.graphics.Shader.TileMode.REPEAT,
                android.graphics.Shader.TileMode.REPEAT);
        }
        return grain;
    }

    /**
     * An icon in layers, cut to an outline. It is drawn once into a picture
     * of its size and then only copied, so a list of many icons scrolls as
     * lightly as a list of pictures.
     */
    static final class Cut extends Drawable {
        private final Drawable icon;
        private final int shape;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap drawn;

        Cut(Drawable icon, int shape) {
            this.icon = icon;
            this.shape = shape;
        }

        @Override
        public void draw(Canvas canvas) {
            int side = Math.min(getBounds().width(), getBounds().height());
            if (side <= 0) {
                return;
            }
            if (shape == PAPER) {
                paper(canvas, side);
                return;
            }
            if (drawn == null || drawn.getWidth() != side) {
                drawn = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
                Canvas into = new Canvas(drawn);
                if (!(icon instanceof AdaptiveIconDrawable)) {
                    flat(into, side);
                    float left = getBounds().left + (getBounds().width() - side) / 2f;
                    float top = getBounds().top + (getBounds().height() - side) / 2f;
                    canvas.drawBitmap(drawn, left, top, paint);
                    return;
                }
                AdaptiveIconDrawable layers = (AdaptiveIconDrawable) icon;
                float inner = side * weight(shape);
                float edge = (side - inner) / 2f;
                into.save();
                into.translate(edge, edge);
                into.clipPath(outline(shape, inner));
                /* The layers are larger than what shows: a quarter of the
                   visible side more on every edge, as the platform lays them. */
                /* The owner's fill draws the layers larger or smaller about
                   the middle: more of the picture, less margin, or the reverse. */
                float grown = inner * Style.fill;
                int spill = Math.round(grown / 4f + (grown - inner) / 2f);
                int all = Math.round(grown) + 2 * Math.round(grown / 4f);
                Drawable ground = layers.getBackground();
                if (ground != null) {
                    ground.setBounds(-spill, -spill, all - spill, all - spill);
                    ground.draw(into);
                }
                Drawable picture = layers.getForeground();
                if (picture != null) {
                    picture.setBounds(-spill, -spill, all - spill, all - spill);
                    picture.draw(into);
                }
                into.restore();
            }
            float left = getBounds().left + (getBounds().width() - side) / 2f;
            float top = getBounds().top + (getBounds().height() - side) / 2f;
            canvas.drawBitmap(drawn, left, top, paint);
        }

        /**
         * A flat icon set into the outline. It is looked at first, small: if
         * its edges are solid all round, it is a picture meant to fill, and
         * it is spread to the outline and cut; otherwise it stands on a
         * ground of the colour its edges carry, or on pale paper when its
         * edges carry nothing, at seven tenths of the outline.
         */
        private void flat(Canvas into, int side) {
            int look = 48;
            Bitmap small = Bitmap.createBitmap(look, look, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, look, look);
            icon.draw(new Canvas(small));
            long r = 0;
            long g = 0;
            long b = 0;
            int solid = 0;
            int ring = 0;
            for (int i = 0; i < look; i++) {
                int[][] at = {{i, 0}, {i, look - 1}, {0, i}, {look - 1, i}};
                for (int[] p : at) {
                    int c = small.getPixel(p[0], p[1]);
                    ring++;
                    if ((c >>> 24) > 200) {
                        solid++;
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                    }
                }
            }
            /* A little inside the edge too: many flat icons are a disc or a
               rounded square with a thin margin round it. */
            int inner = 0;
            int innerSolid = 0;
            int d = look / 8;
            for (int i = d; i < look - d; i++) {
                int[][] at = {{i, d}, {i, look - 1 - d}, {d, i}, {look - 1 - d, i}};
                for (int[] p : at) {
                    int c = small.getPixel(p[0], p[1]);
                    inner++;
                    if ((c >>> 24) > 200) {
                        innerSolid++;
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                    }
                }
            }
            small.recycle();
            float inside = side * weight(shape);
            float edge = (side - inside) / 2f;
            into.save();
            into.translate(edge, edge);
            into.clipPath(outline(shape, inside));
            boolean full = solid > ring * 0.9f;
            int seen = solid + innerSolid;
            int ground = seen > (ring + inner) * 0.25f
                ? 0xFF000000 | (int) (r / seen) << 16 | (int) (g / seen) << 8 | (int) (b / seen)
                : PAPER_INK;
            into.drawColor(ground);
            float wide = full ? inside * 1.02f : inside * 0.72f * Style.fill;
            float at = (inside - wide) / 2f;
            icon.setBounds(Math.round(at), Math.round(at), Math.round(at + wide), Math.round(at + wide));
            icon.draw(into);
            into.restore();
        }

        /**
         * The paper tile, a little wider than the square it is given and a
         * little lower: a rim of grey, the paper inside it, and on the paper
         * the icon. An icon in layers spreads its ground across the whole
         * paper and keeps its picture whole within the paper's height; a
         * flat icon lies on the paper at seven tenths of its height.
         */
        private void paper(Canvas canvas, int side) {
            float tall = side * 0.86f;
            float wide = tall * PAPER_WIDE;
            int w = Math.round(wide);
            int h = Math.round(tall);
            if (drawn == null || drawn.getHeight() != h) {
                drawn = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas into = new Canvas(drawn);
                Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                Paint grit = new Paint(Paint.ANTI_ALIAS_FLAG);
                grit.setShader(grain());
                RectF outer = new RectF(0, 0, w, h);
                float round = h * 0.14f;
                fill.setColor(RIM_INK);
                into.drawRoundRect(outer, round, round, fill);
                into.drawRoundRect(outer, round, round, grit);
                float rim = Math.max(1f, h * 0.033f);
                RectF inner = new RectF(rim, rim, w - rim, h - rim);
                float innerRound = Math.max(0f, round - rim);
                fill.setColor(PAPER_INK);
                into.drawRoundRect(inner, innerRound, innerRound, fill);
                into.save();
                Path clip = new Path();
                clip.addRoundRect(inner, innerRound, innerRound, Path.Direction.CW);
                into.clipPath(clip);
                float cx = inner.centerX();
                float cy = inner.centerY();
                if (icon instanceof AdaptiveIconDrawable) {
                    AdaptiveIconDrawable layers = (AdaptiveIconDrawable) icon;
                    /* The ground covers the paper's width; the picture keeps
                       to its height, so no part of it is cut. */
                    float groundSide = inner.width() * 1.5f;
                    Drawable ground = layers.getBackground();
                    if (ground != null) {
                        ground.setBounds(Math.round(cx - groundSide / 2f), Math.round(cy - groundSide / 2f),
                            Math.round(cx + groundSide / 2f), Math.round(cy + groundSide / 2f));
                        ground.draw(into);
                    }
                    float pictureSide = inner.height() * 1.5f * Style.fill;
                    Drawable picture = layers.getForeground();
                    if (picture != null) {
                        picture.setBounds(Math.round(cx - pictureSide / 2f), Math.round(cy - pictureSide / 2f),
                            Math.round(cx + pictureSide / 2f), Math.round(cy + pictureSide / 2f));
                        picture.draw(into);
                    }
                } else {
                    float flat = h * 0.7f * Style.fill;
                    icon.setBounds(Math.round(cx - flat / 2f), Math.round(cy - flat / 2f),
                        Math.round(cx + flat / 2f), Math.round(cy + flat / 2f));
                    icon.draw(into);
                }
                into.drawRoundRect(inner, innerRound, innerRound, grit);
                into.restore();
            }
            float left = getBounds().centerX() - w / 2f;
            float top = getBounds().centerY() - h / 2f;
            canvas.drawBitmap(drawn, left, top, paint);
        }

        @Override
        public int getIntrinsicWidth() {
            return icon.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return icon.getIntrinsicHeight();
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
