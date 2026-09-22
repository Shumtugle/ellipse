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
    /**
     * The dock: one row along the foot of every screen, as many places as
     * the grid has columns and standing under them, holding what every
     * screen shares. It is asked for by a place of its own in place of a
     * screen's number.
     */
    final Screen dock = new Screen();
    static final int DOCK = -2;

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

    /**
     * The first layout of a new phone: the clock across the head of one
     * screen, and in the dock the door, then whatever this phone calls,
     * writes messages and keeps its people with, each found by the part it
     * plays and not by its name. A phone without one of them, as a tablet
     * may be, simply has one place fewer taken.
     */
    static Layout fresh(Context context) {
        Layout layout = new Layout();
        Screen screen = new Screen();
        Item clock = new Item(CLOCK, 0, 0);
        clock.w = layout.columns;
        screen.items.add(clock);
        layout.screens.add(screen);
        layout.dock.items.add(new Item(DOOR, 0, 0));
        List<String> taken = new ArrayList<String>();
        String[] found = {dialer(context), messages(context), people(context)};
        for (String component : found) {
            if (component != null && !taken.contains(component) && taken.size() + 1 < layout.columns) {
                taken.add(component);
                Item app = new Item(APP, taken.size(), 0);
                app.component = component;
                layout.dock.items.add(app);
            }
        }
        return layout;
    }

    /** The phone's own application for calls: the one the system makes its default. */
    private static String dialer(Context context) {
        try {
            android.telecom.TelecomManager calls = context.getSystemService(android.telecom.TelecomManager.class);
            return calls == null ? null : front(context, calls.getDefaultDialerPackage(), null);
        } catch (RuntimeException none) {
            return null;
        }
    }

    /** The phone's default application for messages. */
    private static String messages(Context context) {
        try {
            return front(context, android.provider.Telephony.Sms.getDefaultSmsPackage(context), null);
        } catch (RuntimeException none) {
            return null;
        }
    }

    /** Whatever answers for the phone's people: its contacts. */
    private static String people(Context context) {
        try {
            android.content.pm.ResolveInfo found = context.getPackageManager().resolveActivity(
                android.content.Intent.makeMainSelectorActivity(android.content.Intent.ACTION_MAIN,
                    android.content.Intent.CATEGORY_APP_CONTACTS),
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            if (found == null || found.activityInfo == null) {
                return null;
            }
            return front(context, found.activityInfo.packageName, found.activityInfo.name);
        } catch (RuntimeException none) {
            return null;
        }
    }

    /**
     * The front door of a package, written as the layout writes one: the
     * named activity if it is one of the package's front doors, else the
     * first of them; none for the system's own chooser or a package
     * without one.
     */
    private static String front(Context context, String pkg, String activity) {
        if (pkg == null || "android".equals(pkg) || pkg.equals(context.getPackageName())) {
            return null;
        }
        android.content.pm.LauncherApps doors = context.getSystemService(android.content.pm.LauncherApps.class);
        if (doors == null) {
            return null;
        }
        List<android.content.pm.LauncherActivityInfo> fronts = doors.getActivityList(pkg,
            android.os.Process.myUserHandle());
        if (fronts == null || fronts.isEmpty()) {
            return null;
        }
        for (android.content.pm.LauncherActivityInfo one : fronts) {
            if (one.getComponentName().getClassName().equals(activity)) {
                return one.getComponentName().flattenToShortString();
            }
        }
        return fronts.get(0).getComponentName().flattenToShortString();
    }

    /** Every place things stand: the screens, then the dock. */
    List<Screen> every() {
        List<Screen> all = new ArrayList<Screen>(screens);
        all.add(dock);
        return all;
    }

    /** A screen by its number, or the dock. */
    Screen screen(int page) {
        return page == DOCK ? dock : screens.get(page);
    }

    /** How many rows a place has: the dock has one. */
    int rowsOf(int page) {
        return page == DOCK ? 1 : rows;
    }

    // ------------------------------------------------------------ what was mine

    /** The layout of the moment, set aside whole while the default stands in its place. */
    static void stash(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        File aside = new File(context.getFilesDir(), FILE + ".mine");
        if (file.exists() && !aside.exists()) {
            file.renameTo(aside);
        }
    }

    /** The layout set aside, back in its place; the default it replaced goes. */
    static void unstash(Context context) {
        File aside = new File(context.getFilesDir(), FILE + ".mine");
        if (aside.exists()) {
            aside.renameTo(new File(context.getFilesDir(), FILE));
        }
    }

    /** The layout set aside, if there is one: its widgets are kept alive while it waits. */
    static Layout stashed(Context context) {
        File aside = new File(context.getFilesDir(), FILE + ".mine");
        if (!aside.exists()) {
            return null;
        }
        try {
            InputStream in = new FileInputStream(aside);
            try {
                return parse(read(in));
            } finally {
                in.close();
            }
        } catch (IOException | JSONException broken) {
            return null;
        }
    }

    // ------------------------------------------------------------ the file

    static Layout load(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        if (!file.exists()) {
            return fresh(context);
        }
        try {
            InputStream in = new FileInputStream(file);
            try {
                return parse(read(in));
            } finally {
                in.close();
            }
        } catch (IOException | JSONException broken) {
            return fresh(context);
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
        if (!dock.items.isEmpty()) {
            JSONArray shelf = new JSONArray();
            for (Item item : dock.items) {
                shelf.put(item.json());
            }
            o.put("dock", shelf);
        }
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
        JSONArray shelf = o.optJSONArray("dock");
        if (shelf != null) {
            for (int k = 0; k < shelf.length(); k++) {
                Item item = Item.from(shelf.getJSONObject(k));
                // The dock holds only what is one place large.
                if (CLOCK.equals(item.kind) || WIDGET.equals(item.kind)) {
                    continue;
                }
                item.x = Math.max(0, Math.min(layout.columns - 1, item.x));
                item.y = 0;
                item.w = 1;
                item.h = 1;
                layout.dock.items.add(item);
            }
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
        for (Item item : screen(screen).items) {
            if (item.covers(cx, cy)) {
                return item;
            }
        }
        return null;
    }

    /** Whether a block of cells is empty, not counting one thing that is moving. */
    boolean free(int screen, int x, int y, int w, int h, Item moving) {
        if (x < 0 || y < 0 || x + w > columns || y + h > rowsOf(screen)) {
            return false;
        }
        for (Item item : screen(screen).items) {
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
        for (int cy = 0; cy + h <= rowsOf(screen); cy++) {
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
        dock.items.remove(item);
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
