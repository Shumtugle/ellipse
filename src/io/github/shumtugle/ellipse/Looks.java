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
        Keep.ICON_TINT, Keep.FOLDER_FACE, Keep.FOLDER_GROUND, Keep.RIM_KIND, Keep.RIM_WIDTH, Keep.GLAZE,
        Keep.GLASS_TONE, Keep.GLASS_CLEAR, Keep.WINDOW, Keep.TILE_ASPECT, Keep.CLOCK_FACE, Keep.CLOCK_PLATE,
        Keep.CLOCK_DIAL, Keep.CLOCK_FIELDS, Keep.CLOCK_GROUND, Keep.MENO_SECOND, Keep.MENO_LINES, Keep.RINGS_BIG,
        Keep.RINGS_LEVEL, Keep.RINGS_SECONDS, Keep.RINGS_DAY, Keep.RINGS_LIST, Keep.RINGS_PLACES, Keep.WIDGET_FRAME,
        Keep.WIDGET_FRAME_WIDTH, Keep.WIDGET_FRAME_ROUND, Keep.WIDGET_GLAZE, Keep.CELL_SHAPE, Keep.ROW_HEIGHT,
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
