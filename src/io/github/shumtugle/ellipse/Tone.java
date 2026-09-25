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
 * One ground, one accent, dark or light, as the owner mixes them on the
 * colour page: a hue, how rich it is, how bright the accent, how solid the
 * containers stand over what is behind them, and how much colour the
 * ground itself takes. Everything else is derived from the same hue:
 * surfaces carry a trace of it at very low saturation, so the whole home
 * screen drifts with the slider instead of leaving grey furniture around a
 * coloured button. Depth comes from neighbouring tones of the same surface
 * rather than from shadows.
 *
 * Light, the same roles stand the other way up: the ground near white
 * with a trace of the hue, the cards a step darker, the words dark, the
 * accent deepened so it reads on paper. What stands on the wallpaper —
 * the names under the icons, the frame round the screen — keeps the dark
 * scheme's light words and dark frame, for the wallpaper is not the theme's.
 */
final class Tone {

    private static float hue = 38f;
    private static float bright = 1f;
    private static float rich = 0.58f;
    private static int accent = shade(0.58f, 1f);
    private static int veil = 255;
    /** Whether the surfaces are light. */
    private static boolean light;
    /** How coloured the ground is: nought is the old near black, one a deep colour. */
    private static float earth;
    /** The design system's containers and their inks, first, second and third. */
    private static final int[] held = new int[6];

    private Tone() {
    }

    /** Reads the look again; true when anything visible changed. */
    static boolean read(Context context) {
        int was = accent;
        float wasEarth = earth;
        int wasVeil = veil;
        Shape.current = Keep.shape(context);
        Style.read(context);
        float[] look = Keep.look(context);
        hue = look[0];
        rich = look[1];
        bright = look[2];
        /* Followed from the phone, the accent's hue and richness are read
           from the system's colour or the wallpaper's; the rest stays the
           owner's. */
        int from = Keep.from(context);
        int seed = 0;
        if (from == Keep.FROM_SYSTEM && Build.VERSION.SDK_INT >= 31) {
            seed = context.getColor(android.R.color.system_accent1_200);
        } else if (from == Keep.FROM_WALL) {
            seed = wallpaperColour(context);
        }
        if (seed != 0) {
            float[] hsv = new float[3];
            Color.colorToHSV(seed, hsv);
            hue = hsv[0];
            rich = Math.min(0.72f, Math.max(0.24f, hsv[1]));
            bright = Math.max(0.82f, hsv[2]);
        }
        boolean wasLight = light;
        int theme = Keep.number(context, Keep.THEME, Keep.THEME_DARK);
        light = theme == Keep.THEME_LIGHT || (theme == Keep.THEME_PHONE
            && (context.getResources().getConfiguration().uiMode
            & android.content.res.Configuration.UI_MODE_NIGHT_MASK) != android.content.res.Configuration.UI_MODE_NIGHT_YES);
        accent = lead(rich, bright);
        veil = Math.round(255f * Math.min(100f, Math.max(55f, look[3])) / 100f);
        earth = Keep.ground(context) / 100f;
        hold();
        return was != accent || wasEarth != earth || wasVeil != veil || wasLight != light;
    }

    /**
     * Colours tried under the thumb and not kept: the colour page's window
     * shows them while a slider moves, and reading the look again puts back
     * what is kept. A hue, richness and brightness below nought leave the
     * accent where it is.
     */
    static void mix(float h, float s, float v, int solid, int ground) {
        if (h >= 0f) {
            hue = h;
            rich = s;
            bright = v;
            accent = lead(rich, bright);
        }
        veil = Math.round(255f * Math.min(100f, Math.max(55f, solid)) / 100f);
        earth = Math.max(0, Math.min(100, ground)) / 100f;
        hold();
    }

    /** Whether the surfaces are light now. */
    static boolean light() {
        return light;
    }

    /** The accent: as mixed on a dark ground; on a light one, deepened so it reads on paper. */
    private static int lead(float sat, float val) {
        return light ? shade(Math.min(0.85f, sat + 0.25f), Math.max(0.30f, val * 0.46f)) : shade(sat, val);
    }

    /**
     * The phone's own bars over a surface: dark marks on a light one, light
     * marks on a dark one or on the wallpaper.
     */
    static void dress(android.view.Window window, boolean onSurface) {
        boolean dark = onSurface && light;
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController bars = window.getInsetsController();
            if (bars != null) {
                int both = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                bars.setSystemBarsAppearance(dark ? both : 0, both);
            }
            return;
        }
        android.view.View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility() & ~(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (dark) {
            flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(flags);
    }

    /** The containers of the design system, and their inks, from the hue. */
    private static void hold() {
        float third = (hue + 60f) % 360f;
        if (light) {
            held[0] = shade(Math.min(0.45f, rich * 0.7f + earth * 0.1f), 0.90f);
            held[1] = shade(Math.min(0.8f, rich + 0.2f), 0.24f);
            held[2] = shade(Math.min(0.30f, rich * 0.4f + earth * 0.1f), 0.93f);
            held[3] = shade(Math.min(0.6f, rich * 0.8f), 0.20f);
            held[4] = Color.HSVToColor(new float[] {third, 0.28f, 0.90f});
            held[5] = Color.HSVToColor(new float[] {third, 0.55f, 0.22f});
            return;
        }
        held[0] = shade(Math.min(0.7f, rich + 0.1f), mix(0.34f, 0.42f));
        held[1] = shade(0.18f, 0.96f);
        held[2] = shade(Math.min(0.6f, rich * 0.4f + earth * 0.2f), mix(0.28f, 0.36f));
        held[3] = shade(0.10f, 0.94f);
        held[4] = Color.HSVToColor(new float[] {third, 0.45f, mix(0.32f, 0.40f)});
        held[5] = Color.HSVToColor(new float[] {third, 0.12f, 0.96f});
    }

    /** The wallpaper's leading colour, or nought when it will not say. */
    private static int wallpaperColour(Context context) {
        try {
            android.app.WallpaperColors colours = android.app.WallpaperManager.getInstance(context)
                .getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM);
            return colours == null ? 0 : colours.getPrimaryColor().toArgb() | 0xFF000000;
        } catch (RuntimeException refused) {
            return 0;
        }
    }

    /** The hue, richness and brightness the accent stands at now, wherever they came from. */
    static float hue() {
        return hue;
    }

    static float rich() {
        return rich;
    }

    static float bright() {
        return bright;
    }

    private static float mix(float from, float to) {
        return from + (to - from) * earth;
    }

    private static int veiled(int colour) {
        return (veil << 24) | (colour & 0x00FFFFFF);
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

    /**
     * The ground. At rest it is near black, carrying the hue: on an unlit
     * screen it is still black. Given colour, it deepens into the accent's
     * own hue but stays dark enough for light words and a lit accent; the
     * containers climb with it, a step lighter each.
     */
    static int surface() {
        if (light) {
            return shade(mix(Math.min(0.06f, rich * 0.1f), Math.min(0.22f, 0.08f + rich * 0.2f)), mix(0.975f, 0.93f));
        }
        float sat = mix(Math.min(0.30f, rich * 0.4f), Math.min(0.80f, 0.30f + rich * 0.6f));
        return shade(sat, mix(0.035f, 0.19f));
    }

    /**
     * The frame around the wallpaper's window, under the status bar and the
     * dock: always the near black of a ground with no colour, whatever colour
     * the ground takes, so the phone's own edges stay dark.
     */
    static int frame() {
        return shade(Math.min(0.30f, rich * 0.4f), 0.035f);
    }

    static int container() {
        if (light) {
            return veiled(shade(mix(Math.min(0.10f, rich * 0.16f), Math.min(0.30f, 0.12f + rich * 0.26f)),
                mix(0.935f, 0.88f)));
        }
        float sat = mix(Math.min(0.26f, rich * 0.34f), Math.min(0.70f, 0.26f + rich * 0.5f));
        return veiled(shade(sat, mix(0.095f, 0.26f)));
    }

    static int containerHigh() {
        if (light) {
            return veiled(shade(mix(Math.min(0.12f, rich * 0.18f), Math.min(0.34f, 0.14f + rich * 0.3f)),
                mix(0.895f, 0.84f)));
        }
        float sat = mix(Math.min(0.24f, rich * 0.30f), Math.min(0.60f, 0.24f + rich * 0.42f));
        return veiled(shade(sat, mix(0.14f, 0.33f)));
    }

    static int onSurface() {
        return light ? shade(Math.min(0.5f, rich * 0.4f), 0.12f) : shade(rich * 0.07f, 0.96f);
    }

    /** Words on the wallpaper, under the icons: light in either scheme, for the wallpaper is its own. */
    static int onWall() {
        return shade(rich * 0.07f, 0.96f);
    }

    static int onVariant() {
        return light ? shade(Math.min(0.4f, rich * 0.35f), 0.34f) : shade(rich * 0.12f, mix(0.66f, 0.80f));
    }

    /** Quiet words; on a coloured ground they need more light to be read at all. */
    static int faint() {
        return light ? shade(Math.min(0.3f, rich * 0.25f), 0.52f) : shade(rich * 0.14f, mix(0.42f, 0.64f));
    }

    static int outline() {
        return light ? (0x33 << 24) | (shade(rich * 0.3f, 0.25f) & 0x00FFFFFF)
            : (0x26 << 24) | (shade(rich * 0.2f, 0.85f) & 0x00FFFFFF);
    }

    /** A lamp of the chosen hue: the light the colour page fills a room with. */
    static int lit(float weight, float richness) {
        return shade(Math.min(1f, rich * richness), Math.min(1f, weight));
    }

    /** A colour with a given opacity, for the pages drawn as pages. */
    static String rgba(int colour, float alpha) {
        return "rgba(" + ((colour >> 16) & 0xFF) + "," + ((colour >> 8) & 0xFF) + ","
            + (colour & 0xFF) + "," + alpha + ")";
    }

    /** For the pages drawn as pages. */
    static String hex(int colour) {
        return String.format("#%06X", colour & 0x00FFFFFF);
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

    /** The same box, outlined in a colour of its own. */
    static GradientDrawable box(int fill, float radius, float stroke, int edge) {
        GradientDrawable made = box(fill, radius, 0f);
        made.setStroke(Math.max(1, Math.round(stroke)), edge);
        return made;
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
