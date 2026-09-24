package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Typeface;

/**
 * How the home screen's icons and words are drawn, beside their colour:
 * how large the icons stand, how much of their outline the picture fills,
 * whether names stand under them on the screens and in the list, how
 * large the names are, and in which of the phone's own typefaces every
 * word is set. Read with the colour; the settings may try values under the
 * thumb before they are kept.
 */
final class Style {

    /** The phone's own families, from the plain to the typewriter. */
    static final String[] FAMILIES = {"sans-serif", "sans-serif-condensed", "sans-serif-light",
        "sans-serif-medium", "serif", "monospace"};

    static float iconScale = 1f;
    static float fill = 1f;
    static boolean namesOnScreens = true;
    static boolean namesInList = true;
    static float nameScale = 1f;
    static int family;
    /** Nought: the apps' own colours; one: every icon in the accent; two: only those drawn for it. */
    static int tint;
    static final int OWN = 0;
    static final int ALL = 1;
    static final int ABLE = 2;
    private static Typeface face = Typeface.create(FAMILIES[0], Typeface.NORMAL);
    private static Typeface bold = Typeface.create(FAMILIES[0], Typeface.BOLD);

    private Style() {
    }

    static void read(Context context) {
        iconScale = Keep.number(context, Keep.ICON_SIZE, 100) / 100f;
        fill = Keep.number(context, Keep.ICON_FILL, 100) / 100f;
        namesOnScreens = Keep.flag(context, Keep.NAMES_SCREENS, true);
        namesInList = Keep.flag(context, Keep.NAMES_LIST, true);
        nameScale = Keep.number(context, Keep.NAME_SIZE, 100) / 100f;
        font(Keep.number(context, Keep.FONT, 0));
        tint = Keep.number(context, Keep.ICON_TINT, OWN);
    }

    /** Sets the family every word is set in. */
    static void font(int which) {
        family = Math.max(0, Math.min(FAMILIES.length - 1, which));
        face = Typeface.create(FAMILIES[family], Typeface.NORMAL);
        bold = Typeface.create(FAMILIES[family], Typeface.BOLD);
    }

    static Typeface face() {
        return face;
    }

    static Typeface bold() {
        return bold;
    }

    /** Sets every word under a view in the chosen family, keeping bold words bold. */
    static void apply(android.view.View view) {
        if (view instanceof android.widget.TextView) {
            android.widget.TextView words = (android.widget.TextView) view;
            Typeface was = words.getTypeface();
            words.setTypeface(was != null && was.isBold() ? bold : face);
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i));
            }
        }
    }

    static String familyName() {
        return FAMILIES[family];
    }
}
