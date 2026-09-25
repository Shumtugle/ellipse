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
    /** How many lines a name may take: on the screens; in the list and the panels. */
    static int nameLinesScreens = 1;
    static int nameLinesList = 1;
    static float nameScale = 1f;
    /** The names' colour on the screens, and the halo that keeps them read on any wallpaper. */
    private static int nameChoice = Keep.NAME_LIGHT;

    static int nameInk() {
        return nameChoice == Keep.NAME_LIGHT ? Tone.onWall() : nameChoice == Keep.NAME_DARK ? 0xFF1C1A17
            : Rings.colour(nameChoice);
    }

    /** A dark halo under light words, a light one under dark words. */
    static int nameHalo() {
        return android.graphics.Color.luminance(nameInk()) > 0.4f ? 0x99000000 : 0x99FFFFFF;
    }
    static int family;
    /** Nought: the apps' own colours; one: every icon in the accent; two: only those drawn for it. */
    static int tint;
    static final int OWN = 0;
    static final int ALL = 1;
    static final int ABLE = 2;
    /** Icons given a face of their own, by the word they are kept by. */
    private static java.util.Map<String, String[]> faces = new java.util.HashMap<>();

    /** One icon's own face: colour, drawing, inking, picture; less than nought or nought when not its own. */
    static int[] faceOf(String token) {
        String[] own = token == null ? null : faces.get(token);
        if (own == null) {
            return new int[] {-1, -1, 0, 0, 0};
        }
        int[] made = new int[5];
        for (int i = 0; i < 5; i++) {
            try {
                made[i] = Integer.parseInt(own[i]);
            } catch (NumberFormatException broken) {
                made[i] = i < 2 ? -1 : 0;
            }
        }
        return made;
    }

    /** The symbol one icon was given in place of its picture, by name, or an empty word. */
    static String symbolOf(String token) {
        String[] own = token == null ? null : faces.get(token);
        return own == null ? "" : own[5];
    }

    /** Where the picture of the owner's own for one icon is kept. */
    static java.io.File pictureOf(Context context, String token) {
        java.io.File room = new java.io.File(context.getFilesDir(), "faces");
        room.mkdirs();
        return new java.io.File(room, Integer.toHexString(token.hashCode()) + ".png");
    }

    /**
     * An icon as it is to be drawn, with its own face if it was given one: a
     * picture of the owner's own in its place, a symbol in its place, the
     * home screen's drawing, a chosen way of inking, its own colour — and
     * always the outline every icon wears.
     */
    static android.graphics.drawable.Drawable dress(Context context, String token,
                                                    android.graphics.drawable.Drawable raw, String owner) {
        int[] own = faceOf(token);
        if (own[4] == 1) {
            android.graphics.Bitmap picture = android.graphics.BitmapFactory.decodeFile(
                pictureOf(context, token).getPath());
            if (picture != null) {
                return Shape.face(new android.graphics.drawable.BitmapDrawable(context.getResources(), picture),
                    -1, own[1], Marks.NONE, own[3]);
            }
        }
        String symbol = symbolOf(token);
        if (symbol.length() > 0) {
            int id = context.getResources().getIdentifier("sym_" + symbol, "drawable", context.getPackageName());
            if (id != 0) {
                return Shape.faceMark(raw, -1, context.getDrawable(id));
            }
        }
        int drawing = own[2] == 1 && owner != null ? Marks.of(owner) : Marks.NONE;
        return Shape.face(raw, -1, own[1], drawing, own[3]);
    }
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
        nameChoice = Keep.number(context, Keep.NAME_COLOUR, Keep.NAME_LIGHT);
        nameLinesScreens = Keep.number(context, Keep.NAME_LINES_SCREENS, 1) >= 2 ? 2 : 1;
        nameLinesList = Keep.number(context, Keep.NAME_LINES_LIST, 1) >= 2 ? 2 : 1;
        Pack.read(context);
        font(Keep.number(context, Keep.FONT, 0));
        tint = Keep.number(context, Keep.ICON_TINT, OWN);
        faces = Keep.faces(context);
        Stack.layout = Keep.number(context, Keep.FOLDER_FACE, Stack.FOUR);
        Stack.ground = Keep.flag(context, Keep.FOLDER_GROUND, true);
        Rim.materials(context.getResources());
        Rim.kind = Keep.number(context, Keep.RIM_KIND, Rim.NONE);
        Rim.width = Keep.number(context, Keep.RIM_WIDTH, 3) / 100f;
        Rim.glaze = Keep.flag(context, Keep.GLAZE, false);
        Rim.glassTone = Keep.number(context, Keep.GLASS_TONE, 0) / 100f;
        Rim.glassClear = Keep.number(context, Keep.GLASS_CLEAR, 55) / 100f;
        Shape.window = Keep.number(context, Keep.WINDOW, Shape.WINDOW_TILE);
        Shape.aspect = Keep.number(context, Keep.TILE_ASPECT, 100) / 100f;
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
