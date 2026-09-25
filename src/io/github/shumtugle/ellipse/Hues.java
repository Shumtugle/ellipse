package io.github.shumtugle.ellipse;

import android.content.Context;

/**
 * The owner's touches to a clock face: the colour of its parts and the
 * size of its windows, each kept by the face and the part. Nothing is
 * kept until something is chosen, and nothing chosen means the face as
 * it was drawn: its own colours, its own measures. A face is put back as
 * it was by letting go of everything kept for it.
 */
final class Hues {

    /** A face's own colour for a part: the first choice, kept as nothing. */
    static final int ORIGINAL = -1;
    static final int[] VALUES = {ORIGINAL, 0, 1, 2, 3, 4, 5, 6, 7};
    static final String[] NAMES = {"Original", "Accent", "Orange", "Red", "Lilac", "Blue", "Green", "Sand",
        "White"};

    /** The faces, by the word their touches are kept under. */
    static final String FIRST = "first";
    static final String PLATE = "plate";
    static final String MENO = "meno";

    /** The parts a face may colour, and the windows it may size. */
    static final String DIAL = "dial";
    static final String HOUR = "hour";
    static final String WINDOWS = "windows";
    static final String SECONDS = "seconds";
    static final String HANDS = "hands";
    static final String MARKS = "marks";
    static final String ROW = "row";

    private static final String HUE = "hue.";
    private static final String SIZE = "size.";

    private Hues() {
    }

    static String hueKey(String face, String part) {
        return HUE + face + "." + part;
    }

    static String sizeKey(String face, String part) {
        return SIZE + face + "." + part;
    }

    /** A part's colour: the one chosen for it, or the face's own. */
    static int colour(Context context, String face, String part, int own) {
        int chosen = Keep.number(context, hueKey(face, part), ORIGINAL);
        return chosen == ORIGINAL ? own : Rings.colour(chosen);
    }

    /** Whether a part keeps the face's own colour. */
    static boolean own(Context context, String face, String part) {
        return Keep.number(context, hueKey(face, part), ORIGINAL) == ORIGINAL;
    }

    /** A window's size as a share of its own: one, unless chosen. */
    static float size(Context context, String face, String part) {
        return Math.max(50, Math.min(150, Keep.number(context, sizeKey(face, part), 100))) / 100f;
    }

    /**
     * A chosen colour made fit to fill a window of a dark face: the colour
     * taken half into the dark, so a bright choice glows and does not shout.
     */
    static int field(int colour) {
        return mix(colour, 0xFF1C1B19, 0.5f);
    }

    /** Words to stand on a fill: dark on a light one, light on a dark one. */
    static int inkOn(int fill) {
        return android.graphics.Color.luminance(fill) > 0.45f ? 0xFF1C1A17 : 0xFFF2EDE4;
    }

    static int mix(int a, int b, float toward) {
        float keep = 1f - toward;
        int r = Math.round(((a >> 16) & 0xFF) * keep + ((b >> 16) & 0xFF) * toward);
        int g = Math.round(((a >> 8) & 0xFF) * keep + ((b >> 8) & 0xFF) * toward);
        int bl = Math.round((a & 0xFF) * keep + (b & 0xFF) * toward);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Everything kept for a face let go of: its colours and its sizes, as it was drawn. */
    static void forget(Context context, String face) {
        Keep.forgetStarting(context, HUE + face + ".", SIZE + face + ".");
    }
}
