package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Where everything on the home screens stands.
 *
 * Screens side by side, each a grid of columns and rows; on a screen,
 * things, each at a cell and covering one or more. A thing is an
 * application, a folder of applications, a widget's place, a shortcut, or
 * the door to the drawer.
 *
 * The whole of it is one small text file, readable by eye: what is kept
 * inside the application is exactly what is exported, and a file written
 * by hand, or by some other tool, is imported the same way. Applications
 * are named by their front door, the package and the activity, and
 * nothing else; an application that is gone leaves an empty place, not a
 * broken file.
 */
final class Layout {

    static final String APP = "app";
    static final String FOLDER = "folder";
    static final String WIDGET = "widget";
    static final String SHORTCUT = "shortcut";
    static final String DOOR = "door";
    /** One screen inside an application, opened by its own name rather than the front door. */
    static final String ACTIVITY = "activity";
    /** This home screen's own clock, with the date, the weather and the charge. */
    static final String CLOCK = "clock";

    private static final String FILE = "layout.json";
    private static final int VERSION = 1;

    /** One thing on a screen. */
    static final class Item {
        String kind;
        int x;
        int y;
        int w = 1;
        int h = 1;
        /** An application's front door; for a shortcut, its application's package. */
        String component;
        /** A shortcut's own name inside its application. */
        String shortcut;
        /** A folder's name, a shortcut's words, a widget's application. */
        String label = "";
        /** A widget's maker. */
        String provider;
        /**
         * The number the platform gave a placed widget. It belongs to this
         * phone and this home screen only; a layout brought from elsewhere
         * carries none, and its widgets wait to be placed again.
         */
        int id = -1;
        /** A folder's applications, in order. */
        final List<String> apps = new ArrayList<String>();
        /** A thing's own settings, by name: what the clock shows in its windows, say. */
        final java.util.Map<String, String> options = new java.util.TreeMap<String, String>();

        Item(String kind, int x, int y) {
            this.kind = kind;
            this.x = x;
            this.y = y;
        }

        boolean single() {
            return w == 1 && h == 1;
        }

        boolean covers(int cx, int cy) {
            return cx >= x && cx < x + w && cy >= y && cy < y + h;
        }

        JSONObject json() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind", kind);
            o.put("x", x);
            o.put("y", y);
            if (!single()) {
                o.put("w", w);
                o.put("h", h);
            }
            if (component != null) {
                o.put("component", component);
            }
            if (shortcut != null) {
                o.put("shortcut", shortcut);
            }
            if (label != null && label.length() > 0) {
                o.put("label", label);
            }
            if (provider != null) {
                o.put("provider", provider);
            }
            if (id >= 0) {
                o.put("id", id);
            }
            if (!options.isEmpty()) {
                JSONObject own = new JSONObject();
                for (java.util.Map.Entry<String, String> one : options.entrySet()) {
                    own.put(one.getKey(), one.getValue());
                }
                o.put("options", own);
            }
            if (FOLDER.equals(kind)) {
                JSONArray list = new JSONArray();
                for (String app : apps) {
                    list.put(app);
                }
                o.put("apps", list);
            }
            return o;
        }

        static Item from(JSONObject o) {
            Item item = new Item(o.optString("kind", APP), o.optInt("x"), o.optInt("y"));
            item.w = Math.max(1, o.optInt("w", 1));
            item.h = Math.max(1, o.optInt("h", 1));
            item.component = door(o.optString("component", null));
            item.shortcut = o.optString("shortcut", null);
            item.label = o.optString("label", "");
            item.provider = o.optString("provider", null);
            item.id = o.optInt("id", -1);
            JSONObject own = o.optJSONObject("options");
            if (own != null) {
                java.util.Iterator<String> names = own.keys();
                while (names.hasNext()) {
                    String name = names.next();
                    item.options.put(name, own.optString(name, ""));
                }
            }
            JSONArray list = o.optJSONArray("apps");
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    String app = door(list.optString(i, null));
                    if (app != null) {
                        item.apps.add(app);
                    }
                }
            }
            return item;
        }
    }

    /** One screen: its things, in no particular order. */
    static final class Screen {
        final List<Item> items = new ArrayList<Item>();
    }

    int columns = Keep.DEFAULT_COLUMNS;
    int rows = Keep.DEFAULT_ROWS;
    /** The screen Home returns to. */
    int home;
    final List<Screen> screens = new ArrayList<Screen>();

    /** A front door written the one way the platform writes it short, so two spellings match. */
    static String door(String component) {
        if (component == null) {
            return null;
        }
        ComponentName name = ComponentName.unflattenFromString(component);
        return name == null ? component : name.flattenToShortString();
    }

    /** Whether this phone has a layout written down yet. */
    static boolean kept(Context context) {
        return new File(context.getFilesDir(), FILE).exists();
    }

    /** The first layout of a new phone: one screen, and the door in the middle of its lowest row. */
    static Layout fresh() {
        Layout layout = new Layout();
        Screen screen = new Screen();
        screen.items.add(new Item(DOOR, layout.columns / 2, layout.rows - 1));
        layout.screens.add(screen);
        return layout;
    }

    // ------------------------------------------------------------ the file

    static Layout load(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        if (!file.exists()) {
            return fresh();
        }
        try {
            InputStream in = new FileInputStream(file);
            try {
                return parse(read(in));
            } finally {
                in.close();
            }
        } catch (IOException | JSONException broken) {
            return fresh();
        }
    }

    /** Written beside the old file and moved over it, so a cut in the middle leaves the old one whole. */
    void save(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        File next = new File(context.getFilesDir(), FILE + ".new");
        try {
            OutputStream out = new FileOutputStream(next);
            try {
                out.write(text().getBytes(StandardCharsets.UTF_8));
            } finally {
                out.close();
            }
            if (!next.renameTo(file)) {
                next.delete();
            }
        } catch (IOException | JSONException lost) {
            next.delete();
        }
    }

    String text() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("ellipse", VERSION);
        o.put("columns", columns);
        o.put("rows", rows);
        o.put("home", home);
        JSONArray list = new JSONArray();
        for (Screen screen : screens) {
            JSONArray items = new JSONArray();
            for (Item item : screen.items) {
                items.put(item.json());
            }
            JSONObject s = new JSONObject();
            s.put("items", items);
            list.put(s);
        }
        o.put("screens", list);
        return o.toString(1);
    }

    static Layout parse(String text) throws JSONException {
        JSONObject o = new JSONObject(text);
        if (!o.has("ellipse") || !o.has("screens")) {
            throw new JSONException("not a layout");
        }
        Layout layout = new Layout();
        layout.columns = Math.max(1, o.optInt("columns", 5));
        layout.rows = Math.max(1, o.optInt("rows", 11));
        JSONArray list = o.getJSONArray("screens");
        for (int i = 0; i < list.length(); i++) {
            Screen screen = new Screen();
            JSONArray items = list.getJSONObject(i).optJSONArray("items");
            if (items != null) {
                for (int k = 0; k < items.length(); k++) {
                    Item item = Item.from(items.getJSONObject(k));
                    item.x = Math.max(0, Math.min(layout.columns - 1, item.x));
                    item.y = Math.max(0, Math.min(layout.rows - 1, item.y));
                    item.w = Math.min(item.w, layout.columns - item.x);
                    item.h = Math.min(item.h, layout.rows - item.y);
                    screen.items.add(item);
                }
            }
            layout.screens.add(screen);
        }
        if (layout.screens.isEmpty()) {
            layout.screens.add(new Screen());
        }
        layout.home = Math.max(0, Math.min(layout.screens.size() - 1, o.optInt("home", 0)));
        return layout;
    }

    static String read(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
        int n;
        while ((n = in.read(buffer)) > 0) {
            all.write(buffer, 0, n);
        }
        return new String(all.toByteArray(), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------ places

    /** The thing covering a cell, or none. */
    Item at(int screen, int cx, int cy) {
        for (Item item : screens.get(screen).items) {
            if (item.covers(cx, cy)) {
                return item;
            }
        }
        return null;
    }

    /** Whether a block of cells is empty, not counting one thing that is moving. */
    boolean free(int screen, int x, int y, int w, int h, Item moving) {
        if (x < 0 || y < 0 || x + w > columns || y + h > rows) {
            return false;
        }
        for (Item item : screens.get(screen).items) {
            if (item == moving) {
                continue;
            }
            if (x < item.x + item.w && item.x < x + w && y < item.y + item.h && item.y < y + h) {
                return false;
            }
        }
        return true;
    }

    /** The empty block nearest to a wished-for cell, or none if the screen is full. */
    int[] nearest(int screen, int x, int y, int w, int h, Item moving) {
        int[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int cy = 0; cy + h <= rows; cy++) {
            for (int cx = 0; cx + w <= columns; cx++) {
                if (!free(screen, cx, cy, w, h, moving)) {
                    continue;
                }
                int d = (cx - x) * (cx - x) + (cy - y) * (cy - y);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = new int[] {cx, cy};
                }
            }
        }
        return best;
    }

    /** Takes a thing off whatever screen holds it. */
    void remove(Item item) {
        for (Screen screen : screens) {
            screen.items.remove(item);
        }
    }

    int screenOf(Item item) {
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).items.contains(item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A folder that lost an application: with one left it becomes that
     * application, with none it goes.
     */
    void settle(Item folder) {
        if (!FOLDER.equals(folder.kind) || folder.apps.size() > 1) {
            return;
        }
        if (folder.apps.isEmpty()) {
            remove(folder);
            return;
        }
        folder.kind = APP;
        folder.component = folder.apps.get(0);
        folder.apps.clear();
        folder.label = "";
    }
}
