package io.github.shumtugle.ellipse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.Random;

/**
 * A ground drawn by the home screen itself, to stand as the wallpaper:
 * not a picture but a recipe of up to four layers — a light, a texture, an
 * ornament and a darkening towards the edges — each with its colour, its
 * strength and its scale, and one number of chance under them all. The
 * recipe is a few numbers; the picture is drawn from it at any size, the
 * same at a thumbnail's size as at the screen's.
 */
final class Ground {

    static final int LIGHT_NONE = 0;
    static final int CURTAIN = 1;
    static final int HORIZON = 2;
    static final int WASH = 3;
    static final String[] LIGHTS = {"None", "Curtain of light", "Horizon", "Wash"};

    static final int TEXTURE_NONE = 0;
    static final int CLOTH = 1;
    static final int PLASTER = 2;
    static final int GRAVEL = 3;
    static final int GROOVES = 4;
    static final int WOOD = 5;
    static final int CONCRETE = 6;
    static final int SAND = 7;
    static final int METAL = 8;
    static final int BIOME = 9;
    static final int RIPPLE = 10;
    static final String[] TEXTURES = {"None", "Cloth", "Plaster", "Gravel", "Grooves", "Wood", "Aerated concrete",
        "Sand", "Brushed metal", "Biome", "Ripples"};

    static final int ORNAMENT_NONE = 0;
    static final int CIRCLES = 1;
    static final int BRANCHES = 2;
    static final int DROPS = 3;
    static final int STARS = 4;
    static final String[] ORNAMENTS = {"None", "Circles", "Branches", "Drops on glass", "Stars"};

    /** A recipe: which layer of each kind, their colours, strengths and scale, and the number of chance. */
    int light = CURTAIN;
    int lightHue = 40;
    int lightHue2 = 80;
    int lightK = 70;
    int texture = TEXTURE_NONE;
    int hue = 40;
    int sat = 20;
    int val = 12;
    int textureK = 60;
    int scale = 100;
    int ornament = ORNAMENT_NONE;
    int ornamentK = 50;
    int vignette = 50;
    int seed = 1;

    /** The words each part of a recipe is kept under. */
    static final String[] KEYS = {"g_light", "g_light_hue", "g_light_hue2", "g_light_k", "g_texture", "g_hue", "g_sat",
        "g_val", "g_texture_k", "g_scale", "g_ornament", "g_ornament_k", "g_vignette", "g_seed"};

    int[] values() {
        return new int[] {light, lightHue, lightHue2, lightK, texture, hue, sat, val, textureK, scale, ornament,
            ornamentK, vignette, seed};
    }

    void take(int[] v) {
        light = v[0];
        lightHue = v[1];
        lightHue2 = v[2];
        lightK = v[3];
        texture = v[4];
        hue = v[5];
        sat = v[6];
        val = v[7];
        textureK = v[8];
        scale = v[9];
        ornament = v[10];
        ornamentK = v[11];
        vignette = v[12];
        seed = v[13];
    }

    /** The recipe as it is kept in the settings. */
    static Ground kept(android.content.Context context) {
        Ground g = new Ground();
        int[] v = g.values();
        for (int i = 0; i < KEYS.length; i++) {
            v[i] = Keep.number(context, KEYS[i], v[i]);
        }
        g.take(v);
        return g;
    }

    void keep(android.content.Context context) {
        int[] v = values();
        for (int i = 0; i < KEYS.length; i++) {
            Keep.saveNumber(context, KEYS[i], v[i]);
        }
    }

    /** As words, for a recipe kept under a name. */
    String words() {
        StringBuilder b = new StringBuilder();
        for (int one : values()) {
            b.append(one).append(',');
        }
        return b.toString();
    }

    static Ground of(String words) {
        Ground g = new Ground();
        String[] part = words.split(",");
        int[] v = g.values();
        for (int i = 0; i < v.length && i < part.length; i++) {
            try {
                v[i] = Integer.parseInt(part[i].trim());
            } catch (NumberFormatException broken) {
                // That part keeps its first value.
            }
        }
        g.take(v);
        return g;
    }

    // ------------------------------------------------------------ the ready ones

    static final String[] READY = {"Void, glow", "Void, blue", "Void, green", "Horizon and cloth", "Plaster",
        "Gravel", "Collage, fog", "Collage, sunset", "Wood", "Light oak", "Aerated concrete", "Sand",
        "Drops on glass", "Brushed aluminium", "Biome", "Ripples", "Cosmos"};

    /** A ready recipe: light, its hues and strength; texture, its colour, strength, scale; ornament; darkening. */
    private static final int[][] READY_VALUES = {
        {CURTAIN, 28, 70, 80, TEXTURE_NONE, 40, 10, 2, 0, 100, ORNAMENT_NONE, 0, 55, 3},
        {CURTAIN, 190, 225, 80, TEXTURE_NONE, 220, 10, 2, 0, 100, ORNAMENT_NONE, 0, 55, 5},
        {CURTAIN, 85, 150, 80, TEXTURE_NONE, 120, 10, 2, 0, 100, ORNAMENT_NONE, 0, 55, 7},
        {HORIZON, 42, 42, 85, CLOTH, 48, 25, 16, 70, 100, ORNAMENT_NONE, 0, 60, 8},
        {LIGHT_NONE, 45, 45, 0, PLASTER, 42, 38, 58, 55, 100, ORNAMENT_NONE, 0, 25, 4},
        {LIGHT_NONE, 30, 30, 0, GRAVEL, 32, 55, 42, 85, 100, ORNAMENT_NONE, 0, 70, 3},
        {WASH, 60, 70, 20, GROOVES, 70, 10, 30, 30, 100, CIRCLES, 45, 50, 2},
        {WASH, 18, 50, 60, GROOVES, 40, 45, 38, 30, 100, CIRCLES, 45, 45, 6},
        {LIGHT_NONE, 25, 25, 0, WOOD, 24, 65, 38, 75, 100, ORNAMENT_NONE, 0, 40, 11},
        {LIGHT_NONE, 36, 36, 0, WOOD, 36, 38, 86, 45, 70, ORNAMENT_NONE, 0, 15, 12},
        {LIGHT_NONE, 40, 40, 0, CONCRETE, 38, 8, 72, 70, 100, ORNAMENT_NONE, 0, 20, 13},
        {LIGHT_NONE, 34, 34, 0, SAND, 34, 42, 82, 60, 100, ORNAMENT_NONE, 0, 25, 14},
        {WASH, 60, 60, 12, TEXTURE_NONE, 60, 6, 42, 0, 100, DROPS, 70, 40, 15},
        {LIGHT_NONE, 210, 210, 0, METAL, 210, 4, 72, 70, 100, ORNAMENT_NONE, 0, 20, 16},
        {CURTAIN, 110, 150, 35, BIOME, 125, 50, 30, 70, 100, ORNAMENT_NONE, 0, 55, 17},
        {CURTAIN, 185, 200, 40, RIPPLE, 195, 70, 34, 60, 100, ORNAMENT_NONE, 0, 50, 18},
        {WASH, 270, 220, 45, TEXTURE_NONE, 250, 30, 4, 0, 100, STARS, 70, 45, 19}};

    static Ground ready(int which) {
        Ground g = new Ground();
        g.take(READY_VALUES[Math.max(0, Math.min(READY_VALUES.length - 1, which))].clone());
        return g;
    }

    /**
     * A throw of the dice: a layer of each kind, colours that sit together,
     * and every strength within the range where a ground stays a ground —
     * never at its ends, where it is either nothing or a poster.
     */
    static Ground roll(Random dice) {
        Ground g = new Ground();
        g.light = dice.nextInt(10) < 7 ? 1 + dice.nextInt(3) : LIGHT_NONE;
        g.lightHue = dice.nextInt(360);
        g.lightHue2 = (g.lightHue + 20 + dice.nextInt(60)) % 360;
        g.lightK = 35 + dice.nextInt(50);
        g.texture = dice.nextInt(10) < 8 ? 1 + dice.nextInt(TEXTURES.length - 1) : TEXTURE_NONE;
        g.hue = dice.nextBoolean() ? g.lightHue : dice.nextInt(360);
        g.sat = 5 + dice.nextInt(55);
        boolean dark = g.light != LIGHT_NONE || dice.nextInt(3) > 0;
        g.val = dark ? 6 + dice.nextInt(30) : 45 + dice.nextInt(40);
        g.textureK = 30 + dice.nextInt(55);
        g.scale = 60 + dice.nextInt(100);
        g.ornament = dice.nextInt(10) < 3 ? 1 + dice.nextInt(ORNAMENTS.length - 1) : ORNAMENT_NONE;
        g.ornamentK = 25 + dice.nextInt(50);
        g.vignette = 20 + dice.nextInt(55);
        g.seed = 1 + dice.nextInt(1 << 30);
        return g;
    }

    // ------------------------------------------------------------ drawing

    /** The ground drawn at a size; the same recipe gives the same picture at every size. */
    Bitmap draw(int w, int h) {
        float unit = w / 1080f;
        float[] tex = texture == TEXTURE_NONE ? null : texture(w, h, unit);
        int base = Color.HSVToColor(new float[] {hue, sat / 100f, val / 100f});
        float br = Color.red(base);
        float bg = Color.green(base);
        float bb = Color.blue(base);
        float k = textureK / 100f;
        float[] l1 = rgb(Color.HSVToColor(new float[] {lightHue, 0.85f, 0.62f}));
        float[] l2 = rgb(Color.HSVToColor(new float[] {lightHue2, 0.85f, 0.62f}));
        float[] lm = rgb(Color.HSVToColor(new float[] {(lightHue + lightHue2) / 2f, 0.85f, 0.62f}));
        float lk = lightK / 100f;
        float vk = vignette / 100f;
        Random r = new Random(seed);
        float[] cx = {w * (0.12f + r.nextFloat() * 0.1f), w * (0.45f + r.nextFloat() * 0.1f),
            w * (0.78f + r.nextFloat() * 0.1f)};
        float[] freq = {1.5f + r.nextFloat(), 1.5f + r.nextFloat(), 1.5f + r.nextFloat()};
        float[] phase = {r.nextFloat() * 6f, r.nextFloat() * 6f, r.nextFloat() * 6f};
        float horizon = h * (0.4f + r.nextFloat() * 0.1f);
        float from = r.nextBoolean() ? 0.05f : 0.95f;
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            float fy = y / (float) h;
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                float fx = x / (float) w;
                float t = tex == null ? 0.5f : tex[i];
                float strength = k;
                float shade = 1f;
                if (light == HORIZON) {
                    /* Above the horizon the cloth fades into the dark, softly. */
                    float m = smooth((y - horizon + h * 0.02f) / (h * 0.08f));
                    strength = k * m;
                    shade = 0.35f + 0.65f * m;
                }
                float f = shade * (1f - strength + strength * t * 1.6f);
                float rr = br * f;
                float gg = bg * f;
                float bl = bb * f;
                if (light == CURTAIN) {
                    for (int c = 0; c < 3; c++) {
                        float edge = h * 0.55f + (float) Math.sin(fx * Math.PI * freq[c] + phase[c]) * h * 0.06f;
                        float v = Math.max(0f, 1f - y / edge);
                        v = v * (float) Math.sqrt(v);
                        float side = (float) Math.exp(-sq((x - cx[c]) / (w * 0.45f)));
                        float[] col = c == 0 ? l1 : c == 1 ? lm : l2;
                        float a = v * side * lk;
                        rr += col[0] * a;
                        gg += col[1] * a;
                        bl += col[2] * a;
                    }
                } else if (light == HORIZON) {
                    float band = (float) (Math.exp(-sq((y - horizon) / (h * 0.09f)))
                        * Math.exp(-sq((fx - from) / 0.9f)));
                    if (y > horizon) {
                        band *= Math.max(0f, 1f - (y - horizon) / (h * 0.05f));
                    }
                    rr += l1[0] * band * lk;
                    gg += l1[1] * band * lk;
                    bl += l1[2] * band * lk;
                } else if (light == WASH) {
                    float top = (float) Math.pow(Math.max(0f, 1f - fy * 1.3f), 1.3f);
                    float mixed = fx;
                    rr += (l1[0] * (1 - mixed) + l2[0] * mixed) * top * lk;
                    gg += (l1[1] * (1 - mixed) + l2[1] * mixed) * top * lk;
                    bl += (l1[2] * (1 - mixed) + l2[2] * mixed) * top * lk;
                }
                float d = (float) Math.sqrt(sq(fx - 0.5f) + sq(fy - 0.45f));
                float dim = 1f - vk * Math.min(1f, d * 1.5f);
                out[i] = 0xFF000000 | (clamp(rr * dim) << 16) | (clamp(gg * dim) << 8) | clamp(bl * dim);
            }
        }
        Bitmap made = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        made.setPixels(out, 0, w, 0, 0, w, h);
        if (ornament != ORNAMENT_NONE) {
            ornament(new Canvas(made), w, h, unit, new Random(seed * 31L + 7));
        }
        return made;
    }

    private static float[] rgb(int c) {
        return new float[] {Color.red(c), Color.green(c), Color.blue(c)};
    }

    private static float sq(float v) {
        return v * v;
    }

    private static float smooth(float v) {
        float t = Math.max(0f, Math.min(1f, v));
        return t * t * (3f - 2f * t);
    }

    private static int clamp(float v) {
        return v < 0 ? 0 : v > 255 ? 255 : (int) v;
    }

    // ------------------------------------------------------------ textures

    /** The texture as a field of brightness, nought to one, over the whole picture. */
    private float[] texture(int w, int h, float unit) {
        float s = scale / 100f * unit;
        Random r = new Random(seed * 17L + texture);
        float[] t = new float[w * h];
        switch (texture) {
            case CLOTH: {
                float[] fine = noise(w, h, Math.max(1f, 1.5f * s), r);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        float a = (float) (Math.sin((x + y) * 1.6f / s) * 0.5 + 0.5);
                        float b = (float) (Math.sin((x - y * 0.35f) * 3.1f / s) * 0.5 + 0.5);
                        t[y * w + x] = a * b * 0.6f + fine[y * w + x] * 0.4f;
                    }
                }
                break;
            }
            case PLASTER: {
                float[] hgt = fbm(w, h, 5f * s, 3, r);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int i = y * w + x;
                        float gx = hgt[y * w + Math.min(w - 1, x + 1)] - hgt[i];
                        float gy = hgt[Math.min(h - 1, y + 1) * w + x] - hgt[i];
                        t[i] = Math.max(0f, Math.min(1f, 0.55f + (-gx - gy) * 5f / Math.max(0.5f, s)));
                    }
                }
                break;
            }
            case GRAVEL:
            case CONCRETE:
                stones(t, w, h, s, scale / 100f, r, texture == CONCRETE);
                break;
            case GROOVES: {
                float[] warp = fbm(w, h, 80f * s, 3, r);
                float cx = w * 0.5f;
                float cy = -h * 0.9f;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        float d = (float) Math.hypot(x - cx, y - cy) + warp[y * w + x] * 30f * s;
                        t[y * w + x] = (float) (Math.sin(d * 0.9f / s) * 0.5 + 0.5) * 0.5f + 0.35f;
                    }
                }
                break;
            }
            case WOOD: {
                float[] warp = fbm(w, h, 90f * s, 3, r);
                float[] slow = fbm(w, h, 300f * s, 2, r);
                float[] fibre = noise(w, h, Math.max(1f, 3f * s), r);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int i = y * w + x;
                        float ring = (float) Math.sin((x + warp[i] * 54f * s + slow[i] * 60f * s) * 0.18f / s
                            + Math.sin(y * 0.012f / s) * 2);
                        float v = (float) Math.pow(ring * 0.5f + 0.5f, 1.6f) * 0.6f + fibre[i] * 0.4f;
                        t[i] = Math.max(0f, Math.min(1f, v));
                    }
                }
                break;
            }
            case SAND: {
                float[] warp = fbm(w, h, 120f * s, 3, r);
                float[] body = fbm(w, h, 50f * s, 3, r);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int i = y * w + x;
                        float rip = (float) Math.sin((y + warp[i] * 40f * s + x * 0.25f) * 0.25f / s);
                        t[i] = Math.max(0f, Math.min(1f, rip * 0.25f + body[i] * 0.5f + 0.25f));
                    }
                }
                break;
            }
            case METAL: {
                float[] rows = new float[h];
                float run = 0.5f;
                for (int y = 0; y < h; y++) {
                    run = run * 0.7f + r.nextFloat() * 0.3f;
                    rows[y] = run;
                }
                float[] slow = fbm(w, h, 400f * s, 2, r);
                for (int y = 0; y < h; y++) {
                    float sheen = (float) (Math.exp(-sq((y - h * 0.35f) / (h * 0.25f))) * 0.35
                        + Math.exp(-sq((y - h * 0.8f) / (h * 0.12f))) * 0.15);
                    for (int x = 0; x < w; x++) {
                        t[y * w + x] = Math.min(1f, 0.35f + rows[y] * 0.25f + slow[y * w + x] * 0.1f + sheen);
                    }
                }
                break;
            }
            case BIOME:
                cells(t, w, h, 90f * s, r);
                break;
            case RIPPLE: {
                float[][] spot = new float[5][2];
                for (float[] one : spot) {
                    one[0] = r.nextFloat() * w;
                    one[1] = r.nextFloat() * h;
                }
                float lo = Float.MAX_VALUE;
                float hi = -Float.MAX_VALUE;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        float v = 0f;
                        for (int k = 0; k < spot.length; k++) {
                            float d = (float) Math.hypot(x - spot[k][0], y - spot[k][1]) / s;
                            v += (float) Math.sin(d * 0.22f - k) / (1f + d * 0.01f);
                        }
                        t[y * w + x] = v;
                        lo = Math.min(lo, v);
                        hi = Math.max(hi, v);
                    }
                }
                for (int i = 0; i < t.length; i++) {
                    t[i] = (t[i] - lo) / Math.max(0.0001f, hi - lo);
                }
                break;
            }
            default:
                java.util.Arrays.fill(t, 0.5f);
                break;
        }
        return t;
    }

    /** Smooth noise: a small grid of chance, drawn up to the picture's size and softened. */
    private static float[] noise(int w, int h, float cell, Random r) {
        int gw = Math.max(2, (int) (w / cell) + 2);
        int gh = Math.max(2, (int) (h / cell) + 2);
        int[] grid = new int[gw * gh];
        for (int i = 0; i < grid.length; i++) {
            int v = r.nextInt(256);
            grid[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }
        Bitmap small = Bitmap.createBitmap(grid, gw, gh, Bitmap.Config.ARGB_8888);
        Bitmap big = Bitmap.createScaledBitmap(small, w, h, true);
        int[] px = new int[w * h];
        big.getPixels(px, 0, w, 0, 0, w, h);
        small.recycle();
        big.recycle();
        float[] out = new float[w * h];
        for (int i = 0; i < px.length; i++) {
            out[i] = (px[i] & 255) / 255f;
        }
        return out;
    }

    private static float[] fbm(int w, int h, float cell, int octaves, Random r) {
        float[] out = new float[w * h];
        float amp = 1f;
        float total = 0f;
        for (int o = 0; o < octaves; o++) {
            float[] one = noise(w, h, Math.max(1f, cell / (1 << o)), r);
            for (int i = 0; i < out.length; i++) {
                out[i] += one[i] * amp;
            }
            total += amp;
            amp *= 0.5f;
        }
        for (int i = 0; i < out.length; i++) {
            out[i] /= total;
        }
        return out;
    }

    /** Stones pressed together, or the pores of aerated concrete in a grey paste. */
    private static void stones(float[] t, int w, int h, float s, float size, Random r, boolean pores) {
        /* As many stones or pores at every size of the picture: fewer as they are drawn larger. */
        float tall = h / (float) w / 2.17f;
        Bitmap field = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(field);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (pores) {
            float[] paste = fbm(w, h, 40f * s, 3, r);
            int[] px = new int[w * h];
            for (int i = 0; i < px.length; i++) {
                int v = (int) (150 + paste[i] * 70);
                px[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
            field.setPixels(px, 0, w, 0, 0, w, h);
            int count = (int) (900f * tall / (size * size));
            for (int k = 0; k < count; k++) {
                float x = r.nextFloat() * w;
                float y = r.nextFloat() * h;
                float rad = (float) (Math.pow(r.nextFloat(), 3) * 20 + 2.4) * s;
                p.setColor(0xFF505050);
                c.drawCircle(x, y, rad, p);
                p.setColor(0xFFD2D2D2);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, rad * 0.25f));
                c.drawArc(x - rad, y - rad, x + rad, y + rad, 200, 180, false, p);
                p.setStyle(Paint.Style.FILL);
            }
        } else {
            c.drawColor(0xFF1E1E1E);
            int count = (int) (9000f * tall / (size * size));
            for (int k = 0; k < count; k++) {
                float x = r.nextFloat() * w;
                float y = r.nextFloat() * h;
                float rad = (4f + r.nextFloat() * 9f) * s;
                int v = 60 + r.nextInt(90);
                p.setColor(0xFF000000 | (v << 16) | (v << 8) | v);
                c.drawOval(x - rad, y - rad * 0.8f, x + rad, y + rad * 0.8f, p);
                p.setColor(0xFFBEBEBE);
                c.drawCircle(x - rad * 0.4f, y - rad * 0.4f, Math.max(0.5f, rad * 0.2f), p);
            }
        }
        int[] px = new int[w * h];
        field.getPixels(px, 0, w, 0, 0, w, h);
        field.recycle();
        for (int i = 0; i < px.length; i++) {
            t[i] = (px[i] & 255) / 255f;
        }
    }

    /** Cells as of living tissue: the edge between the two nearest seeds of a jittered grid. */
    private static void cells(float[] t, int w, int h, float cell, Random r) {
        int gw = (int) (w / cell) + 3;
        int gh = (int) (h / cell) + 3;
        float[] px = new float[gw * gh];
        float[] py = new float[gw * gh];
        for (int j = 0; j < gh; j++) {
            for (int i = 0; i < gw; i++) {
                px[j * gw + i] = (i - 1 + r.nextFloat()) * cell;
                py[j * gw + i] = (j - 1 + r.nextFloat()) * cell;
            }
        }
        for (int y = 0; y < h; y++) {
            int cj = (int) (y / cell) + 1;
            for (int x = 0; x < w; x++) {
                int ci = (int) (x / cell) + 1;
                float d1 = Float.MAX_VALUE;
                float d2 = Float.MAX_VALUE;
                for (int j = cj - 1; j <= cj + 1; j++) {
                    for (int i = ci - 1; i <= ci + 1; i++) {
                        if (i < 0 || j < 0 || i >= gw || j >= gh) {
                            continue;
                        }
                        float d = (float) Math.hypot(x - px[j * gw + i], y - py[j * gw + i]);
                        if (d < d1) {
                            d2 = d1;
                            d1 = d;
                        } else if (d < d2) {
                            d2 = d;
                        }
                    }
                }
                t[y * w + x] = Math.min(1f, (d2 - d1) / (cell * 0.12f));
            }
        }
    }

    // ------------------------------------------------------------ ornaments

    private void ornament(Canvas c, int w, int h, float unit, Random r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float k = ornamentK / 100f;
        float s = scale / 100f * unit;
        switch (ornament) {
            case CIRCLES:
                for (int n = 0; n < 40; n++) {
                    float x = r.nextFloat() * w;
                    float y = h * (0.4f + r.nextFloat() * 0.55f);
                    float rad = (18f + (float) Math.pow(r.nextFloat(), 2) * 120f) * s;
                    int a = (int) ((18 + r.nextInt(34)) * k * 2f);
                    p.setShader(new RadialGradient(x, y, rad, new int[] {(Math.min(255, a) << 24) | 0xE6DCC8,
                        (Math.min(255, a) << 24) | 0xE6DCC8, 0x00E6DCC8}, new float[] {0f, 0.8f, 1f},
                        Shader.TileMode.CLAMP));
                    c.drawCircle(x, y, rad, p);
                    p.setShader(null);
                    if (r.nextInt(10) < 3) {
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(Math.max(1f, 8f * s));
                        p.setColor((Math.min(255, (int) (60 * k * 1.6f)) << 24) | 0xE6DCC8);
                        c.drawCircle(x, y, rad * 1.4f, p);
                        p.setStyle(Paint.Style.FILL);
                    }
                }
                break;
            case BRANCHES:
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeCap(Paint.Cap.ROUND);
                for (int n = 0; n < 5; n++) {
                    branch(c, p, w * (0.2f + r.nextFloat() * 0.8f), -5f, (float) (Math.PI / 2 + r.nextGaussian() * 0.2),
                        42f * s, 7f * s, 22, r, k);
                }
                break;
            case DROPS:
                for (int n = 0; n < (int) (260 * k + 40); n++) {
                    float x = r.nextFloat() * w;
                    float y = r.nextFloat() * h;
                    float rad = ((float) Math.pow(r.nextFloat(), 2) * 16f + 3f) * s;
                    float ry = rad * (1f + r.nextFloat() * 0.5f);
                    p.setColor(0x6E1E201E);
                    c.drawOval(x - rad, y - ry, x + rad, y + ry, p);
                    p.setColor(0x82FFFFFF);
                    c.drawOval(x - rad * 0.7f, y - ry * 0.8f, x + rad * 0.3f, y, p);
                    if (r.nextInt(100) < 15) {
                        p.setColor(0x5A282A28);
                        p.setStrokeWidth(Math.max(1f, 2f * s));
                        c.drawLine(x, y + ry, x + r.nextFloat() * 4f - 2f, y + ry + (60 + r.nextFloat() * 120) * s, p);
                    }
                }
                break;
            case STARS:
                for (int n = 0; n < (int) (1200 * k + 200); n++) {
                    float x = r.nextFloat() * w;
                    float y = r.nextFloat() * h;
                    int v = 120 + r.nextInt(136);
                    p.setColor(0xFF000000 | (v << 16) | (v << 8) | Math.min(255, v + 20));
                    c.drawCircle(x, y, (r.nextInt(10) < 9 ? 1.2f : 3.5f) * unit, p);
                }
                break;
            default:
                break;
        }
    }

    private static void branch(Canvas c, Paint p, float x, float y, float angle, float length, float width, int depth,
                               Random r, float k) {
        if (depth == 0 || length < 3f) {
            return;
        }
        float x2 = x + (float) Math.cos(angle) * length;
        float y2 = y + (float) Math.sin(angle) * length;
        p.setColor((Math.min(255, (int) (190 * k + 40)) << 24) | 0x14120E);
        p.setStrokeWidth(Math.max(1f, width));
        c.drawLine(x, y, x2, y2, p);
        branch(c, p, x2, y2, angle + (float) r.nextGaussian() * 0.25f, length * 0.9f, width * 0.85f, depth - 1, r, k);
        if (r.nextInt(100) < 35) {
            branch(c, p, x2, y2, angle + (r.nextBoolean() ? 1 : -1) * (0.4f + r.nextFloat() * 0.5f), length * 0.6f,
                width * 0.6f, depth - 1, r, k);
        }
    }
}
