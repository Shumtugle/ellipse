package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings that belong to the owner of the phone, not to the source: the
 * seed of the colours and whether it follows the wallpaper, the shape of
 * the tile, and which way the drawer turns its pages.
 *
 * Every change also moves a stamp. The home screen remembers the stamp it
 * was built with and builds itself again when it finds a newer one, so no
 * setting has to know who is listening.
 */
public final class Keep {

    private Keep() {
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences("keep", Context.MODE_PRIVATE);
    }

    private static void mark(SharedPreferences.Editor edit, Context context) {
        edit.putLong("stamp", stamp(context) + 1L).apply();
    }

    /** Moves the stamp without a setting: for a change the home screen should build itself anew for. */
    static void touch(Context context) {
        mark(store(context).edit(), context);
    }

    /** Moves with every change of a setting. */
    public static long stamp(Context context) {
        return store(context).getLong("stamp", 0L);
    }

    // ---------------------------------------------------------------- colour

    /** Hue in degrees and richness from nought to one. */
    public static float[] look(Context context) {
        SharedPreferences kept = store(context);
        return new float[] {kept.getFloat("hue", Tone.HUE), kept.getFloat("rich", Tone.RICH)};
    }

    /**
     * Whether the seed follows the wallpaper. It does until a hand moves a
     * dial: a colour chosen on purpose outranks one arrived at by accident.
     */
    public static boolean wall(Context context) {
        return store(context).getBoolean("wall", true);
    }

    public static void saveLook(Context context, float hue, float rich, boolean wall) {
        mark(store(context).edit().putFloat("hue", hue).putFloat("rich", rich)
            .putBoolean("wall", wall), context);
    }

    // ------------------------------------------------------------------ tile

    /** The tile as it was last chosen, or the one measured from the original. */
    static Tile.Look tile(Context context) {
        SharedPreferences kept = store(context);
        return new Tile.Look(kept.getFloat("power", Tile.Look.MEASURED.power),
            kept.getFloat("ratio", Tile.Look.MEASURED.ratio),
            kept.getInt("rim", Tile.Look.MEASURED.rim),
            kept.getFloat("zoom", Tile.Look.MEASURED.zoom),
            kept.getFloat("thick", Tile.Look.MEASURED.width),
            kept.getInt("window", Tile.Look.FOLLOWS),
            kept.getBoolean("gloss", false));
    }

    static void saveTile(Context context, Tile.Look look) {
        mark(store(context).edit().putFloat("power", look.power)
            .putFloat("ratio", look.ratio).putInt("rim", look.rim).putFloat("zoom", look.zoom)
            .putFloat("thick", look.width).putInt("window", look.window).putBoolean("gloss", look.gloss),
            context);
    }

    // ---------------------------------------------------------------- drawer

    /** Whether the drawer turns its pages sideways rather than scrolling down. */
    static boolean across(Context context) {
        return store(context).getBoolean("across", true);
    }

    static void saveAcross(Context context, boolean across) {
        mark(store(context).edit().putBoolean("across", across), context);
    }

    // ---------------------------------------------------------------- screens

    /**
     * Whether a widget that reaches the side of the grid goes on to the edge
     * of the glass: across the margin at the sides, and under the system's
     * bars above and below.
     */
    static boolean edge(Context context) {
        return store(context).getBoolean("edge", true);
    }

    static void saveEdge(Context context, boolean edge) {
        mark(store(context).edit().putBoolean("edge", edge), context);
    }

    /** Whether the screens turn round and round: past the last comes the first again. */
    static boolean endless(Context context) {
        return store(context).getBoolean("endless", true);
    }

    static void saveEndless(Context context, boolean endless) {
        mark(store(context).edit().putBoolean("endless", endless), context);
    }

    // ---------------------------------------------------------------- order

    static final int BY_NAME = 0;
    static final int BY_INSTALLED = 1;
    static final int BY_UPDATED = 2;

    /** How the drawer is ordered: by name, by the day of installing, or by the last update. */
    static int order(Context context) {
        return store(context).getInt("order", BY_NAME);
    }

    static void saveOrder(Context context, int order) {
        mark(store(context).edit().putInt("order", order), context);
    }

    // ---------------------------------------------------------------- recent

    /** How many applications opened from here are remembered. */
    private static final int REMEMBERED = 12;

    /**
     * Notes that an application was opened from here. Only what this home
     * screen opened is known to it; it asks the system for nothing.
     */
    static void opened(Context context, String key) {
        List<String> kept = recent(context);
        kept.remove(key);
        kept.add(0, key);
        while (kept.size() > REMEMBERED) {
            kept.remove(kept.size() - 1);
        }
        JSONArray list = new JSONArray();
        for (String one : kept) {
            list.put(one);
        }
        store(context).edit().putString("recent", list.toString()).apply();
    }

    /** The applications last opened from here, the latest first. */
    static List<String> recent(Context context) {
        List<String> kept = new ArrayList<String>();
        try {
            JSONArray list = new JSONArray(store(context).getString("recent", "[]"));
            for (int i = 0; i < list.length(); i++) {
                kept.add(list.getString(i));
            }
        } catch (JSONException broken) {
            kept.clear();
        }
        return kept;
    }

    // ---------------------------------------------------------------- whole

    /** Every setting as one object, to travel with the layout in an exported file. */
    static JSONObject export(Context context) throws JSONException {
        SharedPreferences kept = store(context);
        Tile.Look look = tile(context);
        float[] seed = look(context);
        JSONObject o = new JSONObject();
        o.put("hue", (double) seed[0]);
        o.put("rich", (double) seed[1]);
        o.put("wall", kept.getBoolean("wall", true));
        o.put("power", (double) look.power);
        o.put("ratio", (double) look.ratio);
        o.put("rim", look.rim);
        o.put("zoom", (double) look.zoom);
        o.put("thick", (double) look.width);
        o.put("window", look.window);
        o.put("gloss", look.gloss);
        o.put("across", across(context));
        o.put("edge", edge(context));
        o.put("endless", endless(context));
        o.put("order", order(context));
        o.put("immersion", immersion(context));
        o.put("door", door(context));
        o.put("dock", dock(context));
        o.put("flat", flat(context));
        o.put("well_shape", well(context).shape);
        o.put("well_hue", well(context).hue);
        o.put("well_dense", (double) well(context).dense);
        o.put("night", night(context));
        return o;
    }

    /** Settings brought back from a file; what the file does not name stays as it is. */
    static void restore(Context context, JSONObject o) {
        SharedPreferences.Editor edit = store(context).edit();
        for (String key : new String[] {"hue", "rich", "power", "ratio", "zoom", "thick", "well_dense"}) {
            if (o.has(key)) {
                edit.putFloat(key, (float) o.optDouble(key));
            }
        }
        for (String key : new String[] {"wall", "across", "edge", "endless", "gloss", "dock", "flat"}) {
            if (o.has(key)) {
                edit.putBoolean(key, o.optBoolean(key));
            }
        }
        for (String key : new String[] {"rim", "order", "immersion", "window", "door", "night", "well_shape", "well_hue"}) {
            if (o.has(key)) {
                edit.putInt(key, o.optInt(key));
            }
        }
        mark(edit, context);
    }

    // ---------------------------------------------------------------- default

    /** The grid of the default: four across and five down. */
    static final int DEFAULT_COLUMNS = 4;
    static final int DEFAULT_ROWS = 5;

    /**
     * The home screen as the design system's own guidelines have it, and
     * as a new phone first sees it: icons in the system's own shape with
     * no plate over them, colour taken from the wallpaper, the drawer
     * scrolling down in the order of names, screens that stop at their
     * ends, both of the system's bars kept, and the door wearing this home
     * screen's own sign.
     */
    static JSONObject defaults() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("wall", true);
        o.put("window", Tile.Look.RAW);
        o.put("rim", Tile.Look.ACCENT);
        o.put("gloss", false);
        o.put("zoom", 1.0);
        o.put("across", false);
        o.put("edge", false);
        o.put("endless", false);
        o.put("order", BY_NAME);
        o.put("immersion", BARS);
        o.put("door", Door.DEFAULT);
        o.put("dock", true);
        o.put("flat", true);
        o.put("night", PHONE);
        return o;
    }

    /** Whether the default stands now; what was the owner's own is kept beside it. */
    static boolean onDefault(Context context) {
        return store(context).getString("mine", null) != null;
    }

    /** The owner's own settings, as they were when the default was put in their place. */
    static JSONObject mine(Context context) {
        String kept = store(context).getString("mine", null);
        try {
            return kept == null ? null : new JSONObject(kept);
        } catch (JSONException broken) {
            return null;
        }
    }

    /**
     * What the switch between mine and the default touches: how things
     * look, and nothing else. The grid, the screens, what stands on them,
     * the widgets, the shortcuts, the drawer's order and the language are
     * the owner's work, and a change of look is not a reason to undo it.
     */
    private static final String[] LOOK = {"wall", "hue", "rich", "night", "flat", "window", "rim", "gloss",
        "power", "ratio", "thick", "zoom", "door", "well_shape", "well_hue", "well_dense"};

    static void toDefault(Context context, JSONObject mine) throws JSONException {
        store(context).edit().putString("mine", mine.toString()).apply();
        restore(context, only(defaults()));
    }

    static void toMine(Context context) {
        JSONObject mine = mine(context);
        store(context).edit().remove("mine").apply();
        if (mine != null) {
            restore(context, only(mine));
        }
    }

    /** The same settings with everything that is not a matter of looks left out. */
    private static JSONObject only(JSONObject all) {
        JSONObject few = new JSONObject();
        for (String key : LOOK) {
            if (all.has(key)) {
                try {
                    few.put(key, all.get(key));
                } catch (JSONException never) {
                    // A value that will not be copied is a value not changed.
                }
            }
        }
        return few;
    }

    /**
     * Everything forgotten: the settings, the screens as they were laid
     * out, the language that was loaded. The home screen then starts as it
     * would on a phone that had never seen it.
     */
    static void wipe(Context context) {
        store(context).edit().clear().apply();
        new java.io.File(context.getFilesDir(), "layout.json").delete();
        new java.io.File(context.getFilesDir(), "layout.json.mine").delete();
        Words.forget();
    }


    /**
     * A phone that has never seen this home screen starts from the
     * default; one that has keeps whatever it has, told or not.
     */
    static void settle(Context context) {
        SharedPreferences kept = store(context);
        if (kept.contains("settled")) {
            return;
        }
        boolean fresh = kept.getAll().isEmpty() && !Layout.kept(context);
        kept.edit().putBoolean("settled", true).apply();
        if (fresh) {
            try {
                restore(context, defaults());
            } catch (JSONException none) {
                // The code's own defaults stand.
            }
        }
    }

    // ---------------------------------------------------------------- day and night

    /** The scheme follows the phone's own day and night. */
    static final int PHONE = 0;
    /** The scheme of the day stands whatever the phone does. */
    static final int DAY = 1;
    /** The scheme of the night stands whatever the phone does. */
    static final int NIGHT = 2;

    /** Which scheme the owner asks for: the phone's own, the day's, or the night's. */
    static int night(Context context) {
        return store(context).getInt("night", PHONE);
    }

    static void saveNight(Context context, int wish) {
        mark(store(context).edit().putInt("night", wish), context);
    }

    // ---------------------------------------------------------------- fields

    /**
     * The ground a piece of content lies on: its shape, a hue of the
     * owner's own or none, and how solid it stands. One field is kept for
     * everything that has one.
     */
    static Well.Look well(Context context) {
        SharedPreferences kept = store(context);
        return new Well.Look(kept.getInt("well_shape", Well.ROUNDED), kept.getInt("well_hue", -1),
            kept.getFloat("well_dense", 1f));
    }

    static void saveWell(Context context, Well.Look look) {
        mark(store(context).edit().putInt("well_shape", look.shape).putInt("well_hue", look.hue)
            .putFloat("well_dense", look.dense), context);
    }

    // ---------------------------------------------------------------- flat

    /**
     * Whether the settings, the clock and the shelves are drawn the design
     * system's own way, flat in the colours of its roles, rather than cut
     * from the tiles' material. The default is drawn so.
     */
    static boolean flat(Context context) {
        return store(context).getBoolean("flat", false);
    }

    /**
     * A phone that stood on the default before the default was drawn flat,
     * or before its door was blue, takes both up, once.
     */
    static void catchUp(Context context) {
        SharedPreferences kept = store(context);
        if (kept.getInt("caught", 0) >= 1) {
            return;
        }
        SharedPreferences.Editor edit = kept.edit().putInt("caught", 1);
        if (onDefault(context)) {
            edit.putBoolean("flat", true);
            if (kept.getInt("door", Door.DEFAULT) == Door.DISC_LIGHT) {
                edit.putInt("door", Door.DEFAULT);
            }
        }
        edit.apply();
    }

    // ---------------------------------------------------------------- dock

    /** Whether a dock stands along the foot of the screens. */
    static boolean dock(Context context) {
        return store(context).getBoolean("dock", false);
    }

    static void saveDock(Context context, boolean dock) {
        mark(store(context).edit().putBoolean("dock", dock), context);
    }

    // ---------------------------------------------------------------- door

    /** Asked for from the settings: the home screen sets the button down when it is next seen. */
    static void wantDoor(Context context) {
        mark(store(context).edit().putBoolean("want_door", true), context);
    }

    static boolean takeDoor(Context context) {
        SharedPreferences kept = store(context);
        boolean asked = kept.getBoolean("want_door", false);
        if (asked) {
            kept.edit().remove("want_door").apply();
        }
        return asked;
    }

    /** Which face the door to the drawer wears: a whole icon of its own, or a mark behind glass. */
    static int door(Context context) {
        int face = store(context).getInt("door", Door.DEFAULT);
        return Door.known(face) ? face : Door.DEFAULT;
    }

    static void saveDoor(Context context, int face) {
        mark(store(context).edit().putInt("door", face), context);
    }

    // ---------------------------------------------------------------- immersion

    static final int BARS = 0;
    static final int NO_STATUS = 1;
    static final int NO_NAVIGATION = 2;
    static final int FULL = 3;

    /**
     * How far the home screen goes under the glass: with both of the
     * system's bars, without the one at the top, without the one at the
     * bottom, or without either. A bar put away comes back for a moment
     * when a finger draws it in from its edge.
     */
    static int immersion(Context context) {
        return store(context).getInt("immersion", BARS);
    }

    static void saveImmersion(Context context, int mode) {
        mark(store(context).edit().putInt("immersion", mode), context);
    }

    // ---------------------------------------------------------------- folder

    /** The folder the owner gave for this home screen's files, or none. */
    static String folder(Context context) {
        return store(context).getString("folder", null);
    }

    static void saveFolder(Context context, String tree) {
        store(context).edit().putString("folder", tree).apply();
    }
}
