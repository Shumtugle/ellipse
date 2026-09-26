package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Looks: how the home screen looks, kept under a name and put on again at
 * a touch. A look holds what is seen — the theme and the accent, the icons'
 * outline, rim and names, the widgets' frames, the clock and every touch to
 * its face, the rings and where they stand, the shape of the places and
 * how the grid meets the edges — and nothing of what is where, what the
 * gestures do, or which apps are hidden.
 *
 * Putting a look on first keeps the look as it was, so one touch takes it
 * off again.
 */
final class Looks {

    private static final String STORE = "ellipse";
    private static final String KEPT = "looks";
    private static final String BEFORE = "look_before";

    /** What a look holds, word by word, and the words that begin what it holds. */
    private static final String[] WORDS = {"hue", "sat", "val", "solid", "ground", "look_from", "icon_shape", "zoom",
        Keep.THEME, Keep.ICON_SIZE, Keep.ICON_FILL, Keep.NAMES_SCREENS, Keep.NAMES_LIST, Keep.NAME_SIZE, Keep.FONT,
        Keep.ICON_TINT, Keep.NAME_COLOUR, Keep.ICON_PACK, Keep.ICON_PACK_NAME, Keep.NAME_LINES_SCREENS, Keep.NAME_LINES_LIST, Keep.FOLDER_FACE, Keep.FOLDER_GROUND, Keep.RIM_KIND, Keep.RIM_WIDTH, Keep.GLAZE,
        Keep.GLASS_TONE, Keep.GLASS_CLEAR, Keep.WINDOW, Keep.TILE_ASPECT, Keep.CLOCK_FACE, Keep.CLOCK_PLATE,
        Keep.CLOCK_DIAL, Keep.CLOCK_FIELDS, Keep.CLOCK_GROUND, Keep.MENO_SECOND, Keep.MENO_LINES, Keep.RINGS_BIG,
        Keep.RINGS_LEVEL, Keep.RINGS_SECONDS, Keep.RINGS_DAY, Keep.RINGS_LIST, Keep.RINGS_PLACES, Keep.WIDGET_FRAME,
        Keep.WIDGET_FRAME_WIDTH, Keep.WIDGET_FRAME_ROUND, Keep.WIDGET_GLAZE, Keep.LIST_OWN, Keep.LIST_HUE,
        Keep.LIST_SAT, Keep.LIST_VAL, Keep.LIST_ALPHA, Keep.CELL_SHAPE, Keep.ROW_HEIGHT,
        Keep.EDGES};
    private static final String[] STARTS = {"hue.", "size.", Keep.RING_KIND};

    private Looks() {
    }

    private static boolean held(String key) {
        for (String word : WORDS) {
            if (word.equals(key)) {
                return true;
            }
        }
        for (String start : STARTS) {
            if (key.startsWith(start)) {
                return true;
            }
        }
        return false;
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    /** The look as it is now, as words. */
    private static JSONObject now(Context context) throws JSONException {
        JSONObject look = new JSONObject();
        for (Map.Entry<String, ?> one : store(context).getAll().entrySet()) {
            if (!held(one.getKey())) {
                continue;
            }
            Object value = one.getValue();
            JSONObject item = new JSONObject();
            if (value instanceof Boolean) {
                item.put("t", "b").put("v", value);
            } else if (value instanceof Integer) {
                item.put("t", "i").put("v", value);
            } else if (value instanceof Float) {
                item.put("t", "f").put("v", ((Float) value).doubleValue());
            } else if (value instanceof String) {
                item.put("t", "s").put("v", value);
            } else {
                continue;
            }
            look.put(one.getKey(), item);
        }
        return look;
    }

    private static JSONObject all(Context context) {
        try {
            return new JSONObject(store(context).getString(KEPT, "{}"));
        } catch (JSONException broken) {
            return new JSONObject();
        }
    }

    /** The names of the looks kept, in the order of the alphabet. */
    static List<String> names(Context context) {
        List<String> names = new ArrayList<>();
        Iterator<String> keys = all(context).keys();
        while (keys.hasNext()) {
            names.add(keys.next());
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** The look as it is now kept under a name; a look of that name is written over. */
    static boolean keep(Context context, String name) {
        try {
            JSONObject looks = all(context);
            looks.put(name, now(context));
            store(context).edit().putString(KEPT, looks.toString()).apply();
            return true;
        } catch (JSONException broken) {
            return false;
        }
    }

    static void forget(Context context, String name) {
        JSONObject looks = all(context);
        looks.remove(name);
        store(context).edit().putString(KEPT, looks.toString()).apply();
    }

    /**
     * A look put on: everything a look holds is let go of and the look's
     * put in its place, so what the look never set is as it first was. The
     * look worn till now is kept aside, to be put back.
     */
    static boolean wear(Context context, String name) {
        JSONObject look = all(context).optJSONObject(name);
        if (look == null) {
            return false;
        }
        try {
            store(context).edit().putString(BEFORE, now(context).toString()).apply();
        } catch (JSONException unsaved) {
            // It is worn all the same; only it cannot be taken off again.
        }
        return put(context, look);
    }

    /**
     * The home screen as it first comes, Material 3: its dress let go of
     * — the icons' outline, rim and gloss, a pack, the folders' face, the
     * typeface, the names' colour, the colours' source, the ground, the
     * first clock in its own colours, the widgets' frames, the list's own
     * ground — so each is as it was at first. Nothing the owner set out is
     * touched: the grids, the apps and their places, whether names stand
     * under the icons and how, the size of the icons, the theme, the dock,
     * the edges, the gestures, the language. The look worn till now is
     * kept aside, to be put back.
     */
    static void material(Context context) {
        dress(context, new Object[0]);
    }

    /** The dress let go of, as a set-out brought in begins: the same as Material 3. */
    static void plain(Context context) {
        dress(context, new Object[0]);
    }

    /** The ready presets, by their names; the first is the home screen as it first comes. */
    static final String[] READY = {"Material 3", "Wood", "Steel", "Meno", "Rings", "Paper", "Walnut", "Gold"};
    static final String[] READY_ABOUT = {
        "The home screen as it first comes: icons in the phone's own shape and colours, the first clock, "
            + "no rims or frames",
        "Tiles with a rim of wood, the plate clock in a case of wood, widgets framed in wood, a warm sand accent",
        "Squircles with a rim of steel, the plate clock in steel, frames of steel, a cool grey-blue accent",
        "The glass card clock, round icons with a fine rim of glass, frames of glass, a dark ground",
        "The rings, and round icons of glass with its gleam, like small rings; no frames, the rings hang free",
        "The paper tile, no rim; with the owner's own pack of masks, if it is on the phone",
        "Wide tiles rimmed in wood and glazed, each app drawn as a light symbol in a round window; the plate "
            + "clock in wood with a black dial, frames of wood, names in sand",
        "Tiles rimmed in gold and glazed, symbols in round windows, a gold accent; the plate clock in gold "
            + "with a black embossed dial, frames of gold"};

    /** The pack of masks the paper preset wears, if the phone has it. */
    private static final String PAPER_PACK = "Stylisha.superellipsy.icon.mask";

    /** A ready preset put on, by its place in the list. */
    static void ready(Context context, int which) {
        switch (which) {
            case 1:
                dress(context, new Object[] {"icon_shape", Shape.PAPER, Keep.RIM_KIND, Rim.WOOD,
                    Keep.CLOCK_FACE, Home.FACE_PLATE, Keep.CLOCK_PLATE, Rim.WOOD,
                    Keep.WIDGET_FRAME, Rim.WOOD, Keep.WIDGET_FRAME_WIDTH, 6,
                    "look_from", Keep.FROM_OWN, "hue", 36f, "sat", 0.34f, "val", 0.86f});
                break;
            case 2:
                dress(context, new Object[] {"icon_shape", Shape.SQUIRCLE, Keep.RIM_KIND, Rim.STEEL,
                    Keep.CLOCK_FACE, Home.FACE_PLATE, Keep.CLOCK_PLATE, Rim.STEEL,
                    Keep.WIDGET_FRAME, Rim.STEEL, Keep.WIDGET_FRAME_WIDTH, 6,
                    "look_from", Keep.FROM_OWN, "hue", 212f, "sat", 0.22f, "val", 0.82f});
                break;
            case 3:
                dress(context, new Object[] {"icon_shape", Shape.CIRCLE, Keep.RIM_KIND, Rim.GLASS, Keep.RIM_WIDTH, 2,
                    Keep.CLOCK_FACE, Home.FACE_MENO, Keep.WIDGET_FRAME, Rim.GLASS, Keep.WIDGET_FRAME_WIDTH, 3,
                    "ground", 0});
                break;
            case 4:
                dress(context, new Object[] {"icon_shape", Shape.CIRCLE, Keep.RIM_KIND, Rim.GLASS, Keep.RIM_WIDTH, 4,
                    Keep.GLAZE, true, Keep.CLOCK_FACE, Home.FACE_RINGS});
                break;
            case 5:
                boolean has = Pack.installed(context).containsKey(PAPER_PACK);
                dress(context, has ? new Object[] {"icon_shape", Shape.PAPER, Keep.ICON_PACK, PAPER_PACK}
                    : new Object[] {"icon_shape", Shape.PAPER});
                break;
            case 6:
                dress(context, new Object[] {"icon_shape", Shape.ROUNDED, Keep.ICON_FILL, 101, Keep.TILE_ASPECT, 135,
                    Keep.WINDOW, Shape.WINDOW_ROUND, Keep.ICON_TINT, 1, Keep.RIM_KIND, Rim.WOOD, Keep.RIM_WIDTH, 14,
                    Keep.GLAZE, true, Keep.NAME_COLOUR, 6, "face.#all", "-1,1,0,3,0,",
                    Keep.CLOCK_FACE, Home.FACE_PLATE, Keep.CLOCK_PLATE, Rim.WOOD, Keep.CLOCK_DIAL, Rim.BLACK,
                    Keep.CLOCK_FIELDS, Rim.GLASS, "size.plate.dial", 140, "size.plate.hour", 140, "size.plate.row", 90,
                    Keep.WIDGET_FRAME, Rim.WOOD, Keep.WIDGET_FRAME_WIDTH, 20, Keep.WIDGET_FRAME_ROUND, 48,
                    Keep.WIDGET_GLAZE, true});
                break;
            case 7:
                dress(context, new Object[] {"icon_shape", Shape.ROUNDED, Keep.TILE_ASPECT, 120,
                    Keep.WINDOW, Shape.WINDOW_ROUND, Keep.ICON_TINT, 1, Keep.RIM_KIND, Rim.GOLD, Keep.RIM_WIDTH, 12,
                    Keep.GLAZE, true, Keep.NAME_COLOUR, 6, "face.#all", "-1,1,0,3,0,",
                    "look_from", Keep.FROM_OWN, "hue", 42f, "sat", 0.5f, "val", 0.88f,
                    Keep.CLOCK_FACE, Home.FACE_PLATE, Keep.CLOCK_PLATE, Rim.GOLD, Keep.CLOCK_DIAL, Watch.EMBOSSED,
                    Keep.CLOCK_FIELDS, Watch.DARK, Keep.WIDGET_FRAME, Rim.GOLD, Keep.WIDGET_FRAME_WIDTH, 14,
                    Keep.WIDGET_FRAME_ROUND, 40, Keep.WIDGET_GLAZE, true});
                break;
            default:
                dress(context, new Object[0]);
                break;
        }
    }

    /**
     * A preset put on: the dress let go of, as the home screen first comes,
     * then the preset's own words written over it — pairs of a word and its
     * value. Nothing the owner set out is touched: the grids, the apps and
     * their places, whether names stand under the icons and how, the size
     * of the icons, the theme, the dock, the edges, the gestures, the
     * language. The look worn till now is kept aside, to be put back.
     */
    private static void dress(Context context, Object[] sets) {
        try {
            store(context).edit().putString(BEFORE, now(context).toString()).apply();
        } catch (JSONException unsaved) {
            // It is put on all the same; only it cannot be taken off again.
        }
        String[] words = {"icon_shape", Keep.ICON_FILL, Keep.TILE_ASPECT, Keep.WINDOW, Keep.ICON_TINT,
            Keep.ICON_PACK, Keep.ICON_PACK_NAME, Keep.RIM_KIND, Keep.RIM_WIDTH, Keep.GLAZE, Keep.GLASS_TONE, Keep.GLASS_CLEAR,
            Keep.FOLDER_FACE, Keep.FOLDER_GROUND, Keep.FONT, Keep.NAME_COLOUR, "look_from", "ground", "solid",
            Keep.CLOCK_FACE, Keep.CLOCK_PLATE, Keep.CLOCK_DIAL, Keep.CLOCK_FIELDS, Keep.WIDGET_FRAME,
            Keep.WIDGET_FRAME_WIDTH, Keep.WIDGET_FRAME_ROUND, Keep.WIDGET_GLAZE, Keep.LIST_OWN, "face.#all"};
        SharedPreferences kept = store(context);
        SharedPreferences.Editor edit = kept.edit();
        for (String key : words) {
            edit.remove(key);
        }
        for (String key : kept.getAll().keySet()) {
            /* The touches of every face go with the dress; an app's own icon, chosen for it alone, stays. */
            if (key.startsWith("hue.") || key.startsWith("size.")) {
                edit.remove(key);
            }
        }
        for (int i = 0; i + 1 < sets.length; i += 2) {
            String key = (String) sets[i];
            Object value = sets[i + 1];
            if (value instanceof Integer) {
                edit.putInt(key, (Integer) value);
            } else if (value instanceof Float) {
                edit.putFloat(key, (Float) value);
            } else if (value instanceof Boolean) {
                edit.putBoolean(key, (Boolean) value);
            } else if (value instanceof String) {
                edit.putString(key, (String) value);
            }
        }
        edit.putInt("stamp", kept.getInt("stamp", 0) + 1);
        edit.commit();
    }

    /** Whether the look worn before the last one put on is kept. */
    static boolean undoable(Context context) {
        return store(context).contains(BEFORE);
    }

    /** The look worn before the last one put on, back again. */
    static boolean undo(Context context) {
        try {
            JSONObject before = new JSONObject(store(context).getString(BEFORE, "{}"));
            store(context).edit().remove(BEFORE).apply();
            return put(context, before);
        } catch (JSONException broken) {
            return false;
        }
    }

    private static boolean put(Context context, JSONObject look) {
        SharedPreferences kept = store(context);
        SharedPreferences.Editor edit = kept.edit();
        for (String key : kept.getAll().keySet()) {
            if (held(key)) {
                edit.remove(key);
            }
        }
        Iterator<String> keys = look.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject item = look.optJSONObject(key);
            if (item == null || !held(key)) {
                continue;
            }
            String type = item.optString("t");
            if ("b".equals(type)) {
                edit.putBoolean(key, item.optBoolean("v"));
            } else if ("i".equals(type)) {
                edit.putInt(key, item.optInt("v"));
            } else if ("f".equals(type)) {
                edit.putFloat(key, (float) item.optDouble("v"));
            } else if ("s".equals(type)) {
                edit.putString(key, item.optString("v"));
            }
        }
        edit.putInt("stamp", kept.getInt("stamp", 0) + 1);
        edit.commit();
        return true;
    }
}
