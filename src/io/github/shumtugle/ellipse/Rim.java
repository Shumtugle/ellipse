package io.github.shumtugle.ellipse;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/**
 * The rim an icon may wear, as the owner's own icon masks of years ago
 * were built: a plate of a material in the icon's outline, lit from above
 * and shaded below so it reads as a bevel and not a flat band; the icon
 * seen through a window cut in it, the window's edge a thin dark line as a
 * cut is; and, if asked for, the curved glaze of glass across the top.
 *
 * The materials are metal and gold in a fine grain, wood, sequins and
 * brushed steel from pictures, black, the accent, and a stamped dial drawn
 * rather than photographed. The same rim and glaze may frame widgets.
 */
final class Rim {

    static final int NONE = -1;
    static final int METAL = 0;
    static final int GOLD = 1;
    static final int ACCENT = 2;
    static final int WOOD = 3;
    static final int SEQUINS = 4;
    static final int BLACK = 5;
    static final int STEEL = 6;
    static final int STAMPED = 7;
    /**
     * Dark glass: the wallpaper shows through it, a light edge runs round
     * it, and the curve of light lies across its top whether asked for or not.
     */
    static final int GLASS = 8;
    /** Silk, in four colours: a soft sheen running across it, a weave too fine to see but felt. */
    static final int SILK_PINK = 9;
    static final int SILK_NUDE = 10;
    static final int SILK_GOLD = 11;
    static final int SILK_SILVER = 12;
    /** Nylon, dense as a stocking's: a lattice of the finest cells over a dark or a light tone. */
    static final int NYLON_DARK = 13;
    static final int NYLON_LIGHT = 14;
    /** A plate of the surface's own raised tone, for a window with no material chosen. */
    static final int GROUND = 99;
    static final String[] NAMES = {"Metal", "Gold", "Accent", "Wood", "Sequins", "Black", "Steel", "Stamped",
        "Glass", "Pink silk", "Nude silk", "Gold silk", "Silver silk", "Dark nylon", "Light nylon"};

    /**
     * The materials offered, in order. Sequins have left the stage: a rim
     * once made of them is drawn in gold silk. Embossing is for the inside
     * of shapes — icons, a dial — not for frames or the clock's case.
     */
    static int[] offered(boolean frames) {
        java.util.List<Integer> all = new java.util.ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            if (i == SEQUINS || (frames && i == STAMPED)) {
                continue;
            }
            all.add(i);
        }
        int[] out = new int[all.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = all.get(i);
        }
        return out;
    }

    static String[] names(int[] kinds) {
        String[] out = new String[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            out[i] = NAMES[kinds[i]];
        }
        return out;
    }

    /** What the icons' rim is made of, how wide it is as a share of the icon, and whether glass lies over it. */
    static int kind = NONE;
    static float width = 0.03f;
    static boolean glaze;
    /** The glass's tone, from smoked dark to milk white, and how much of what is behind it shows. */
    static float glassTone;
    static float glassClear = 0.55f;

    private static Bitmap wood;
    private static Bitmap sequins;
    private static Bitmap steel;
    private static Bitmap pressed;
    private static Shader metal;
    private static Shader gold;

    private Rim() {
    }

    /** Pictures of materials, handed in once from the application's resources. */
    static synchronized void materials(Resources resources) {
        if (wood != null) {
            return;
        }
        BitmapFactory.Options raw = new BitmapFactory.Options();
        raw.inScaled = false;
        wood = BitmapFactory.decodeResource(resources, R.drawable.plate_wood, raw);
        sequins = BitmapFactory.decodeResource(resources, R.drawable.grain_bling, raw);
        steel = BitmapFactory.decodeResource(resources, R.drawable.grain_brushed, raw);
    }

    /** A plate of a material filling a shape, lit from above and shaded below. */
    static void plate(Canvas canvas, Path shape, int which, float width, float height) {
        Paint material = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        material(material, which, width, height);
        canvas.drawPath(shape, material);
        Paint bevel = new Paint(Paint.ANTI_ALIAS_FLAG);
        bevel.setShader(new LinearGradient(0f, 0f, 0f, height,
            new int[] {0x38FFFFFF, 0x00FFFFFF, 0x00000000, 0x42000000},
            new float[] {0f, 0.35f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawPath(shape, bevel);
        if (which == GLASS) {
            /* Glass shows its edge: a fine light line, brighter where the
               light falls on it from above. */
            Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(Math.max(1.5f, Math.min(width, height) * 0.025f));
            edge.setShader(new LinearGradient(0f, 0f, 0f, height, 0x8CFFFFFF, 0x1AFFFFFF, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, edge);
        }
    }

    /** The thin dark line of a window cut in the plate. */
    static void cut(Canvas canvas, Path window, float width) {
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, width * 0.006f));
        line.setColor(0x59000000);
        canvas.drawPath(window, line);
    }

    /** The curved glaze of glass across the top of a shape standing in a box. */
    static void glaze(Canvas canvas, Path shape, float left, float top, float width, float height) {
        Path glass = new Path();
        glass.addOval(new RectF(left - width * 0.45f, top - height * 1.05f, left + width * 1.45f,
            top + height * 0.48f), Path.Direction.CW);
        glass.op(shape, Path.Op.INTERSECT);
        Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
        light.setShader(new LinearGradient(0f, top, 0f, top + height * 0.48f,
            0x3DFFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP));
        canvas.drawPath(glass, light);
    }

    /** The material as paint over a box: one wood and one gold for everything cut from them. */
    static void material(Paint paint, int which, float width, float height) {
        float across = Math.min(width, height * 1.27f);
        switch (which) {
            case ACCENT:
                paint.setColor(Tone.primary());
                return;
            case GROUND:
                paint.setColor(Tone.containerHigh() | 0xFF000000);
                return;
            case GLASS:
                /* Glass of the accent's hue, from smoked near black to milk
                   white, and as clear as the owner asks. */
                float light = 0.10f + 0.85f * glassTone;
                float sat = 0.40f - 0.30f * glassTone;
                int alpha = Math.round(255f * (1f - glassClear));
                paint.setColor((alpha << 24) | (Color.HSVToColor(new float[] {Tone.hue(), sat, light}) & 0xFFFFFF));
                return;
            case GOLD:
                paint.setShader(gold());
                return;
            case BLACK:
                paint.setShader(new RadialGradient(width * 0.5f, height * 0.35f, Math.max(width, height) * 0.8f,
                    0xFF33312E, 0xFF0B0A09, Shader.TileMode.CLAMP));
                return;
            case WOOD:
                if (wood != null) {
                    BitmapShader shader = new BitmapShader(wood, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    Matrix fit = new Matrix();
                    fit.setScale(width / wood.getWidth(), height / wood.getHeight());
                    shader.setLocalMatrix(fit);
                    paint.setShader(shader);
                    return;
                }
                paint.setColor(0xFF6A4630);
                return;
            case SEQUINS:
            case SILK_GOLD:
                paint.setShader(silk(0xFFD6B46A, width, height));
                return;
            case SILK_PINK:
                paint.setShader(silk(0xFFE3AFBB, width, height));
                return;
            case SILK_NUDE:
                paint.setShader(silk(0xFFD8B7A0, width, height));
                return;
            case SILK_SILVER:
                paint.setShader(silk(0xFFC6CAD1, width, height));
                return;
            case NYLON_DARK:
                paint.setShader(nylon(0xFF22201F, width, height));
                return;
            case NYLON_LIGHT:
                paint.setShader(nylon(0xFFC9B3A3, width, height));
                return;
            case STAMPED:
                paint.setShader(stamped(across * 0.055f));
                return;
            case STEEL:
                paint.setShader(brushed(width, height));
                return;
            default:
                paint.setShader(metal());
        }
    }

    /** A texture laid again and again, mirrored at every seam so no seam shows. */
    private static Shader repeated(Bitmap source, float across) {
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
        Matrix size = new Matrix();
        float scale = Math.max(1f, across) / source.getWidth();
        size.setScale(scale, scale);
        shader.setLocalMatrix(size);
        return shader;
    }

    /**
     * A dial stamped in little pyramids, the light falling from above and to
     * the left: every face catches or loses it, and the grid reads as one
     * stamped surface. It is drawn, so it takes the accent's hue.
     */
    private static Shader stamped(float across) {
        if (pressed == null) {
            int side = 16;
            pressed = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    float ax = (x + 0.5f) / side * 2f - 1f;
                    float ay = (y + 0.5f) / side * 2f - 1f;
                    float lit;
                    if (Math.abs(ax) > Math.abs(ay)) {
                        lit = ax < 0f ? 0.80f : 0.30f;
                    } else {
                        lit = ay < 0f ? 0.92f : 0.18f;
                    }
                    if (Math.abs(Math.abs(ax) - Math.abs(ay)) < 1.4f / side) {
                        lit = Math.min(1f, lit + 0.12f);
                    }
                    float seam = Math.min(Math.abs(ax), Math.abs(ay)) > 1f - 1.2f / side ? 0.55f : 1f;
                    int shade = Math.round(Math.max(0f, Math.min(1f, lit)) * 255f * seam);
                    pressed.setPixel(x, y, 0xFF000000 | (shade << 16) | (shade << 8) | shade);
                }
            }
        }
        BitmapShader grain = new BitmapShader(pressed, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix size = new Matrix();
        float scale = Math.max(2f, across) / pressed.getWidth();
        size.setScale(scale, scale);
        grain.setLocalMatrix(size);
        /* Dark, as embossing looks best: near black, the accent's hue only a breath in it. */
        int deep = Color.HSVToColor(new float[] {Tone.hue(), 0.16f, 0.12f});
        int high = Color.HSVToColor(new float[] {Tone.hue(), 0.12f, 0.34f});
        Shader ground = new LinearGradient(0f, 0f, 0f, Math.max(4f, across * 6f), high, deep, Shader.TileMode.CLAMP);
        return new android.graphics.ComposeShader(ground, grain, android.graphics.PorterDuff.Mode.OVERLAY);
    }

    private static Bitmap brushedSheet;
    private static Bitmap nylonCell;

    /**
     * Brushed steel in the finest grain: long streaks, one pixel high, side
     * by side, never scaled up — steel, not a moon's craters — under a soft
     * light across it.
     */
    private static Shader brushed(float width, float height) {
        if (brushedSheet == null) {
            int wide = 256;
            int tall = 128;
            int[] pixels = new int[wide * tall];
            Random random = new Random(11L);
            for (int y = 0; y < tall; y++) {
                double row = random.nextGaussian() * 10.0;
                double drift = 0;
                for (int x = 0; x < wide; x++) {
                    drift = drift * 0.92 + random.nextGaussian() * 2.2;
                    int v = clamp(196 + row + drift);
                    pixels[y * wide + x] = 0xFF000000 | (v << 16) | (v << 8) | clamp(v + 4);
                }
            }
            brushedSheet = Bitmap.createBitmap(pixels, wide, tall, Bitmap.Config.ARGB_8888);
        }
        Shader grain = new BitmapShader(brushedSheet, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
        Shader light = new LinearGradient(0f, 0f, width, height,
            new int[] {0xFF8C8C8C, 0xFFFFFFFF, 0xFF9A9A9A, 0xFFE6E6E6}, new float[] {0f, 0.35f, 0.62f, 1f},
            Shader.TileMode.CLAMP);
        return new android.graphics.ComposeShader(grain, light, android.graphics.PorterDuff.Mode.MULTIPLY);
    }

    /** Silk of one colour: the sheen in broad soft bands across it, light and shadow as the cloth falls. */
    private static Shader silk(int colour, float width, float height) {
        float[] hsv = new float[3];
        Color.colorToHSV(colour, hsv);
        int shadow = Color.HSVToColor(new float[] {hsv[0], Math.min(1f, hsv[1] * 1.15f), hsv[2] * 0.62f});
        int lit = Color.HSVToColor(new float[] {hsv[0], hsv[1] * 0.55f, Math.min(1f, hsv[2] * 1.18f)});
        Shader sheen = new LinearGradient(0f, 0f, width * 0.9f, height,
            new int[] {shadow, colour, lit, colour, shadow, colour, lit},
            new float[] {0f, 0.18f, 0.32f, 0.5f, 0.66f, 0.84f, 1f}, Shader.TileMode.MIRROR);
        return sheen;
    }

    /**
     * Nylon, dense as a stocking's: the finest lattice of cells, three
     * pixels across, over its tone, the sheen of the stretch across it.
     */
    private static Shader nylon(int tone, float width, float height) {
        if (nylonCell == null) {
            int side = 6;
            nylonCell = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    boolean thread = (x + y) % 3 == 0 || (x - y + side) % 3 == 0;
                    nylonCell.setPixel(x, y, thread ? 0xFFFFFFFF : 0xFF9A9A9A);
                }
            }
        }
        Shader mesh = new BitmapShader(nylonCell, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        float[] hsv = new float[3];
        Color.colorToHSV(tone, hsv);
        int dark = Color.HSVToColor(new float[] {hsv[0], hsv[1], hsv[2] * 0.7f});
        int light = Color.HSVToColor(new float[] {hsv[0], hsv[1] * 0.8f, Math.min(1f, hsv[2] * 1.25f + 0.06f)});
        Shader stretch = new LinearGradient(0f, 0f, width, height * 0.6f, new int[] {dark, light, tone, dark},
            new float[] {0f, 0.4f, 0.7f, 1f}, Shader.TileMode.CLAMP);
        return new android.graphics.ComposeShader(stretch, mesh, android.graphics.PorterDuff.Mode.MULTIPLY);
    }

    private static synchronized Shader metal() {
        if (metal == null) {
            metal = grain(0xFFC2C2C2);
        }
        return metal;
    }

    private static synchronized Shader gold() {
        if (gold == null) {
            gold = grain(0xFFC99A3E);
        }
        return gold;
    }

    /** A sheet of grain around one colour, drawn once and repeated; every rim is cut from the same sheet. */
    private static Shader grain(int base) {
        int side = 96;
        int[] pixels = new int[side * side];
        Random random = new Random(7L);
        int r = (base >> 16) & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = base & 0xFF;
        for (int i = 0; i < pixels.length; i++) {
            double stray = random.nextGaussian() * 9.0;
            pixels[i] = 0xFF000000 | (clamp(r + stray) << 16) | (clamp(g + stray) << 8) | clamp(b + stray);
        }
        Bitmap sheet = Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888);
        return new BitmapShader(sheet, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    private static int clamp(double v) {
        return (int) Math.max(0L, Math.min(255L, Math.round(v)));
    }
}
