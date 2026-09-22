package io.github.shumtugle.ellipse;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

import java.util.Random;

/**
 * The shape every application wears here.
 *
 * One curve does all of it: |x/a|^n + |y/b|^n = 1. With n at two it is an
 * ellipse, and a circle when the two half-axes agree; near four it is a
 * soft square; the higher n goes, the straighter the sides and the tighter
 * the corners, until it is a rectangle. A tile is two such curves, one
 * inside the other. The outer one is the rim's edge; the inner one is the
 * window the icon is seen through.
 *
 * Four things are chosen: the roundness n, the proportion of width to
 * height, what the rim is made of, and how close the icon is brought to
 * the window. Everything else follows from them.
 * The first look was measured from the original, not chosen: a tile a
 * little wider than it is tall, a rim of about a fortieth of its width,
 * and a window a touch squarer at the corners than the edge around it, as
 * happens to any curve drawn a constant distance inside another.
 *
 * An icon made of two layers is laid under the window as it was meant to
 * be: the part its designer promised to keep visible fills the window's
 * height, and the spare margin every such icon carries fills the width.
 * An old icon of one flat picture has no spare margin, so it stands on
 * white paper, as tall as the window, and the paper shows at its sides.
 *
 * Brought closer, the icon is enlarged under the window and whatever
 * falls outside it is cut: at twice, only the middle half of the promised
 * part is seen, and an old icon covers its paper edge to edge.
 *
 * A tile is drawn once and kept as a picture. Nothing here runs while a
 * finger is moving.
 */
final class Tile {

    /** The chosen things, and the name they are kept under. */
    static final class Look {

        static final int METAL = 0;
        static final int GOLD = 1;
        static final int ACCENT = 2;
        static final int BARE = 3;
        static final int WOOD = 4;
        static final int BLING = 5;
        static final int BLACK = 6;
        static final int WHITE = 7;

        /** The window follows the tile's own shape. */
        static final int FOLLOWS = 0;
        /** The window is round, a medallion set in a plate of the rim's material. */
        static final int MEDALLION = 1;
        /** No window at all: the icon as its application draws it, in the system's own shape. */
        static final int RAW = 2;

        /** The look measured from the original tile. */
        static final Look MEASURED = new Look(8f, 1.267f, METAL, 1f);

        /** The closest an icon is brought: twice its own size. */
        static final float CLOSEST = 2f;
        /** The rim, from a thread to a frame, as a share of the tile's width. */
        static final float THINNEST = 0.015f;
        static final float THICKEST = 0.14f;

        final float power;
        final float ratio;
        final int rim;
        /** How close the icon is brought, from one, as designed, to twice. */
        final float zoom;
        /** The rim's width, as a share of the tile's width. */
        final float width;
        final int window;
        /** Whether a glaze of light lies over the top of the tile, as over glass. */
        final boolean gloss;

        Look(float power, float ratio, int rim, float zoom) {
            this(power, ratio, rim, zoom, RIM, FOLLOWS, false);
        }

        Look(float power, float ratio, int rim, float zoom, float width, int window, boolean gloss) {
            this.power = Math.max(2f, power);
            this.ratio = Math.max(1f, ratio);
            this.rim = rim;
            this.zoom = Math.max(1f, Math.min(CLOSEST, zoom));
            this.width = Math.max(THINNEST, Math.min(THICKEST, width));
            this.window = window;
            this.gloss = gloss;
        }

        Look with(float power, float ratio) {
            return new Look(power, ratio, rim, zoom, width, window, gloss);
        }

        Look rim(int kind) {
            return new Look(power, ratio, kind, zoom, width, window, gloss);
        }

        Look zoom(float closer) {
            return new Look(power, ratio, rim, closer, width, window, gloss);
        }

        Look width(float share) {
            return new Look(power, ratio, rim, zoom, share, window, gloss);
        }

        Look window(int kind) {
            return new Look(power, ratio, rim, zoom, width, kind, gloss);
        }

        Look gloss(boolean on) {
            return new Look(power, ratio, rim, zoom, width, window, on);
        }

        /** A name that changes whenever the picture would. */
        String key() {
            return Math.round(power * 100f) + ":" + Math.round(ratio * 1000f) + ":" + rim
                + ":" + Math.round(zoom * 100f) + ":" + Math.round(width * 1000f) + ":" + window
                + ":" + (gloss ? 1 : 0);
        }

        boolean same(Look other) {
            return key().equals(other.key());
        }
    }

    /** The rim, as a share of the tile's width. */
    private static final float RIM = 0.026f;
    /**
     * How much squarer the window is than the edge: nothing for an ellipse,
     * and for the measured look, 9.6 inside an edge of 8.
     */
    private static final float SQUARER = 1.27f;

    /** Metal and gold: a colour, and how much each grain strays from it. */
    private static final int METAL_GREY = 194;
    private static final int GOLD_TONE = 0xFFC99A3E;
    private static final double GRAIN = 7.0;
    /** What a flat old icon stands on. */
    private static final int PAPER = 0xFFF7F7F7;

    /**
     * A two-layer icon is drawn on a square half as large again as the
     * part of it that is sure to be seen: 108 units around a promise of 72.
     */
    private static final float BLEED = 108f / 72f;

    /** Points along one curve: enough that no corner shows a facet. */
    private static final int STEPS = 720;

    private static Shader metal;
    private static Shader gold;

    private Tile() {
    }

    /** The height of a tile of the given width. */
    static int height(int width, Look look) {
        return Math.round(width / look.ratio);
    }

    /**
     * The curve inside a box. Walked by angle, which bunches the points
     * where the curve bends and spends few on the straight runs, which is
     * where they are least needed.
     */
    static Path curve(float left, float top, float width, float height, float power) {
        Path path = new Path();
        float a = width / 2f;
        float b = height / 2f;
        float cx = left + a;
        float cy = top + b;
        double bend = 2.0 / power;
        for (int i = 0; i < STEPS; i++) {
            double t = 2.0 * Math.PI * i / STEPS;
            double c = Math.cos(t);
            double s = Math.sin(t);
            float x = (float) (cx + a * Math.signum(c) * Math.pow(Math.abs(c), bend));
            float y = (float) (cy + b * Math.signum(s) * Math.pow(Math.abs(s), bend));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        return path;
    }

    /**
     * A tile with an icon in its window, built in three layers, as the
     * owner's own icon masks of years ago were built: a plate of the rim's
     * material, the icon seen through a window cut in it, and light over
     * both. The plate is lit from above and shaded below, so a rim reads as
     * a bevel and not as a flat band; the window's edge is drawn as a thin
     * dark line, as a cut is; the glaze, if asked for, is the curved
     * reflection of glass across the top.
     */
    static Bitmap render(Drawable icon, int width, Look look) {
        int height = height(width, look);
        Bitmap tile = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tile);
        if (look.window == Look.RAW) {
            // The owner has asked for no mask: the icon is drawn as its own
            // application made it, an adaptive one in the shape the system
            // gives all of them, and nothing is laid over it.
            if (icon != null) {
                int side = Math.round(Math.min(width, height) * 0.94f);
                int left = (width - side) / 2;
                int top = (height - side) / 2;
                icon.setBounds(left, top, left + side, top + side);
                icon.draw(canvas);
            }
            return tile;
        }
        Path outer = curve(0f, 0f, width, height, look.power);

        boolean plate = look.rim != Look.BARE;
        float rim = plate ? look.width * width : 0f;
        if (plate) {
            plate(canvas, outer, look.rim, width, height);
        }

        RectF window = new RectF();
        Path cut;
        if (look.window == Look.MEDALLION && plate) {
            float shorter = Math.min(width, height);
            float d = shorter - 2f * Math.max(rim, shorter * 0.13f);
            window.set((width - d) / 2f, (height - d) / 2f, (width + d) / 2f, (height + d) / 2f);
            cut = new Path();
            cut.addOval(window, Path.Direction.CW);
        } else {
            window.set(rim, rim, width - rim, height - rim);
            float round = plate ? 2f + (look.power - 2f) * SQUARER : look.power;
            cut = curve(rim, rim, width - 2f * rim, height - 2f * rim, round);
        }

        // The picture is painted whole first and then poured through the
        // window, so the window's edge is smoothed like any drawn line. A
        // clip would leave it stepped.
        Bitmap picture = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas under = new Canvas(picture);
        under.drawColor(PAPER);
        if (icon != null) {
            lay(under, icon, window.centerX(), window.centerY(), window.width(), window.height(), look.zoom);
        }
        Paint through = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        through.setShader(new BitmapShader(picture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawPath(cut, through);
        picture.recycle();

        if (plate && rim > width * 0.02f) {
            Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(Math.max(1f, width * 0.006f));
            line.setColor(0x59000000);
            canvas.drawPath(cut, line);
        }

        if (look.gloss) {
            glaze(canvas, outer, 0f, 0f, width, height);
        }
        return tile;
    }

    /** A plate of a material filling a shape, lit from above and shaded below. */
    static void plate(Canvas canvas, Path shape, int kind, int width, int height) {
        Paint material = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        material(material, kind, width, height);
        canvas.drawPath(shape, material);
        Paint bevel = new Paint(Paint.ANTI_ALIAS_FLAG);
        bevel.setShader(new LinearGradient(0f, 0f, 0f, height,
            new int[] {0x38FFFFFF, 0x00FFFFFF, 0x00000000, 0x42000000},
            new float[] {0f, 0.35f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawPath(shape, bevel);
    }

    /** The curved glaze of glass across the top of a shape standing in a box. */
    static void glaze(Canvas canvas, Path shape, float left, float top, float width, float height) {
        Path glaze = new Path();
        glaze.addOval(new RectF(left - width * 0.45f, top - height * 1.05f, left + width * 1.45f,
            top + height * 0.48f), Path.Direction.CW);
        glaze.op(shape, Path.Op.INTERSECT);
        Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
        light.setShader(new LinearGradient(0f, top, 0f, top + height * 0.48f,
            0x3DFFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP));
        canvas.drawPath(glaze, light);
    }

    /** The colour that goes with a material, for hands and marks drawn on it. */
    static int accentOf(int kind) {
        switch (kind) {
            case Look.WOOD: return 0xFFE3AE6E;
            case Look.GOLD: return 0xFFF0CD7A;
            case Look.BLING: return 0xFFEBD49A;
            case Look.BLACK: return 0xFFD9D4CC;
            case Look.WHITE: return 0xFFA9CDE6;
            case Look.METAL: return 0xFFE8E8E8;
            default: return Tone.of(Tone.PRIMARY);
        }
    }

    /** The rim's material, as paint over the tile's own box. */
    private static void material(Paint paint, int kind, int width, int height) {
        switch (kind) {
            case Look.ACCENT:
                paint.setColor(Tone.of(Tone.PRIMARY));
                return;
            case Look.GOLD:
                paint.setShader(gold());
                return;
            case Look.BLACK:
                paint.setShader(new RadialGradient(width * 0.5f, height * 0.35f, Math.max(width, height) * 0.8f,
                    0xFF33312E, 0xFF0B0A09, Shader.TileMode.CLAMP));
                return;
            case Look.WOOD:
                if (wood != null) {
                    paint.setShader(stretched(wood, width, height));
                    return;
                }
                paint.setShader(new RadialGradient(width * 0.5f, height * 0.5f, Math.max(width, height) * 0.7f,
                    0xFF8A5A3A, 0xFF24160D, Shader.TileMode.CLAMP));
                return;
            case Look.BLING:
                if (bling != null) {
                    paint.setShader(repeated(bling, Math.min(width, height * 1.27f) * 0.55f));
                    return;
                }
                paint.setShader(gold());
                return;
            case Look.WHITE:
                if (brushed != null) {
                    paint.setShader(repeated(brushed, Math.min(width, height * 1.27f) * 0.6f));
                    return;
                }
                paint.setColor(0xFFD4D4D4);
                return;
            default:
                paint.setShader(metal());
        }
    }

    /** Pictures of materials, handed in once from the application's resources. */
    private static Bitmap wood;
    private static Bitmap bling;
    private static Bitmap brushed;

    static synchronized void materials(android.content.res.Resources resources) {
        if (wood != null) {
            return;
        }
        android.graphics.BitmapFactory.Options raw = new android.graphics.BitmapFactory.Options();
        raw.inScaled = false;
        wood = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.plate_wood, raw);
        bling = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.grain_bling, raw);
        brushed = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.grain_brushed, raw);
    }

    /** A plate drawn once across the whole tile. */
    private static Shader stretched(Bitmap source, int width, int height) {
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        android.graphics.Matrix fit = new android.graphics.Matrix();
        fit.setScale(width / (float) source.getWidth(), height / (float) source.getHeight());
        shader.setLocalMatrix(fit);
        return shader;
    }

    /** A texture laid again and again, mirrored at every seam so no seam shows. */
    private static Shader repeated(Bitmap source, float across) {
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
        android.graphics.Matrix size = new android.graphics.Matrix();
        float scale = across / source.getWidth();
        size.setScale(scale, scale);
        shader.setLocalMatrix(size);
        return shader;
    }

    /**
     * Lays an icon under the window. The promised part fills the window's
     * shorter side; for a tall window that is its width, for a wide one its
     * height, so a two-layer icon never runs out of margin at either edge.
     */
    private static void lay(Canvas canvas, Drawable icon, float cx, float cy,
                            float inside, float tall, float zoom) {
        float shorter = Math.min(inside, tall);
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable layered = (AdaptiveIconDrawable) icon;
            float longer = Math.max(inside, tall);
            int side = Math.round(Math.max(shorter * BLEED * zoom, longer));
            int left = Math.round(cx - side / 2f);
            int top = Math.round(cy - side / 2f);
            Drawable back = layered.getBackground();
            Drawable front = layered.getForeground();
            if (back != null) {
                back.setBounds(left, top, left + side, top + side);
                back.draw(canvas);
            }
            if (front != null) {
                front.setBounds(left, top, left + side, top + side);
                front.draw(canvas);
            }
            return;
        }
        int side = Math.round(shorter * zoom);
        int left = Math.round(cx - side / 2f);
        int top = Math.round(cy - side / 2f);
        icon.setBounds(left, top, left + side, top + side);
        icon.draw(canvas);
    }

    private static synchronized Shader metal() {
        if (metal == null) {
            metal = grain(0xFF000000 | (METAL_GREY << 16) | (METAL_GREY << 8) | METAL_GREY);
        }
        return metal;
    }

    private static synchronized Shader gold() {
        if (gold == null) {
            gold = grain(GOLD_TONE);
        }
        return gold;
    }

    /**
     * A sheet of grain around one colour, drawn once and repeated. The seed
     * is fixed, so every rim on the screen is cut from the same sheet.
     */
    private static Shader grain(int base) {
        int side = 96;
        int[] pixels = new int[side * side];
        Random random = new Random(7L);
        int r = (base >> 16) & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = base & 0xFF;
        for (int i = 0; i < pixels.length; i++) {
            double stray = random.nextGaussian() * GRAIN;
            pixels[i] = 0xFF000000 | (clamp(r + stray) << 16) | (clamp(g + stray) << 8)
                | clamp(b + stray);
        }
        Bitmap sheet = Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888);
        return new BitmapShader(sheet, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    private static int clamp(double v) {
        return (int) Math.max(0L, Math.min(255L, Math.round(v)));
    }
}
