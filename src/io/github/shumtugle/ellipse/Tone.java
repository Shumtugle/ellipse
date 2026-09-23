package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;

/**
 * Colour as roles, not as a palette.
 *
 * One dark ground, containers a step lighter each, one accent. Depth is told
 * by neighbouring tones rather than by shadows. From Android 12 the hue is
 * taken from the system, which grows it from the wallpaper, so the frame
 * around the wallpaper carries a trace of the picture it holds; older
 * systems keep a warm default.
 */
final class Tone {

    private static float hue = 38f;
    private static float rich = 0.58f;
    private static int accent = shade(0.58f, 1f);
    /** The design system's containers and their inks, first, second and third. */
    private static final int[] held = new int[6];

    private Tone() {
    }

    /** Reads the seed again; true when anything visible changed. */
    static boolean read(Context context) {
        int was = accent;
        if (Build.VERSION.SDK_INT >= 31) {
            int seed = context.getColor(android.R.color.system_accent1_200);
            float[] hsv = new float[3];
            Color.colorToHSV(seed, hsv);
            hue = hsv[0];
            rich = Math.min(0.58f, Math.max(0.18f, hsv[1]));
            accent = seed | 0xFF000000;
        } else {
            hue = 38f;
            rich = 0.58f;
            accent = shade(rich, 1f);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            held[0] = context.getColor(android.R.color.system_accent1_700);
            held[1] = context.getColor(android.R.color.system_accent1_100);
            held[2] = context.getColor(android.R.color.system_accent2_700);
            held[3] = context.getColor(android.R.color.system_accent2_100);
            held[4] = context.getColor(android.R.color.system_accent3_700);
            held[5] = context.getColor(android.R.color.system_accent3_100);
        } else {
            float third = (hue + 60f) % 360f;
            held[0] = shade(Math.min(0.7f, rich + 0.1f), 0.34f);
            held[1] = shade(0.18f, 0.96f);
            held[2] = shade(rich * 0.4f, 0.28f);
            held[3] = shade(0.10f, 0.94f);
            held[4] = Color.HSVToColor(new float[] {third, 0.45f, 0.32f});
            held[5] = Color.HSVToColor(new float[] {third, 0.12f, 0.96f});
        }
        return was != accent;
    }

    static int primaryContainer() {
        return held[0];
    }

    static int onPrimaryContainer() {
        return held[1];
    }

    static int secondaryContainer() {
        return held[2];
    }

    static int onSecondaryContainer() {
        return held[3];
    }

    static int tertiaryContainer() {
        return held[4];
    }

    static int onTertiaryContainer() {
        return held[5];
    }

    private static int shade(float sat, float val) {
        return Color.HSVToColor(new float[] {hue, sat, val});
    }

    /** The ground: near black, holding the hue so faintly it reads as dark. */
    static int surface() {
        return shade(Math.min(0.30f, rich * 0.4f), 0.035f);
    }

    static int container() {
        return shade(Math.min(0.26f, rich * 0.34f), 0.095f);
    }

    static int containerHigh() {
        return shade(Math.min(0.24f, rich * 0.30f), 0.14f);
    }

    static int onSurface() {
        return shade(rich * 0.07f, 0.96f);
    }

    /** Quiet words: a hint, not an instruction. */
    static int faint() {
        return shade(rich * 0.14f, 0.52f);
    }

    static int outline() {
        return (0x26 << 24) | (shade(rich * 0.2f, 0.85f) & 0x00FFFFFF);
    }

    static int primary() {
        return accent;
    }

    /** Dark ink on a light accent, light ink on a dark one. */
    static int onAccent() {
        int red = (accent >> 16) & 0xFF;
        int green = (accent >> 8) & 0xFF;
        int blue = accent & 0xFF;
        int light = (red * 299 + green * 587 + blue * 114) / 1000;
        return light > 140 ? shade(0.9f, 0.09f) : shade(rich * 0.05f, 0.97f);
    }

    /** The accent thinned for a press: seen, not shouted. */
    static int wash() {
        return (0x47 << 24) | (accent & 0x00FFFFFF);
    }

    /** A container: one tone, one radius, one hairline. */
    static GradientDrawable box(int fill, float radius, float stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        if (stroke > 0f) {
            shape.setStroke(Math.max(1, Math.round(stroke)), outline());
        }
        return shape;
    }

    /** Press feedback in the accent, clipped to a rounded shape. */
    static RippleDrawable touch(GradientDrawable under, float radius) {
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.RECTANGLE);
        mask.setCornerRadius(radius);
        mask.setColor(0xFFFFFFFF);
        return new RippleDrawable(ColorStateList.valueOf(wash()), under, mask);
    }
}
