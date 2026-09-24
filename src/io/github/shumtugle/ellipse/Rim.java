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
    static final String[] NAMES = {"Metal", "Gold", "Accent", "Wood", "Sequins", "Black", "Steel", "Stamped"};

    /** What the icons' rim is made of, how wide it is as a share of the icon, and whether glass lies over it. */
    static int kind = NONE;
    static float width = 0.03f;
    static boolean glaze;

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
                if (sequins != null) {
                    paint.setShader(repeated(sequins, across * 0.55f));
                    return;
                }
                paint.setShader(gold());
                return;
            case STAMPED:
                paint.setShader(stamped(across * 0.055f));
                return;
            case STEEL:
                if (steel != null) {
                    paint.setShader(repeated(steel, across * 0.6f));
                    return;
                }
                paint.setColor(0xFFD4D4D4);
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
        int deep = Color.HSVToColor(new float[] {Tone.hue(), 0.45f, 0.36f});
        int high = Color.HSVToColor(new float[] {Tone.hue(), 0.35f, 0.62f});
        Shader ground = new LinearGradient(0f, 0f, 0f, Math.max(4f, across * 6f), high, deep, Shader.TileMode.CLAMP);
        return new android.graphics.ComposeShader(ground, grain, android.graphics.PorterDuff.Mode.OVERLAY);
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
