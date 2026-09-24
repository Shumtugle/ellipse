package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * What the owner has put on the screen by hand, and where. Only that is
 * kept: the places the phone fills by role are found again every time.
 */
final class Keep {

    /** One application set down on the grid. */
    /**
     * One thing set down on a screen: an application by its component, or
     * one of the home screen's own things by a word after a hash — the
     * clock, a folder the phone fills, the door to the settings.
     */
    static final class Spot {
        final String token;
        /** The application, when the thing is one; otherwise none. */
        final ComponentName name;
        final int screen;
        final int x;
        final int y;

        Spot(String token, int screen, int x, int y) {
            this.token = token;
            this.name = Apps.nameOf(token);
            this.screen = screen;
            this.x = x;
            this.y = y;
        }

        String line() {
            return screen + "\t" + x + "\t" + y + "\t" + token;
        }
    }

    /** The home screen's own things, as they are kept. */
    static final String CLOCK_THING = "#clock";
    static final String VENDOR_THING = "#vendor";
    static final String SYSTEM_THING = "#system";
    static final String OWN_THING = "#own";
    private static final String LAID = "laid";

    private static final String PLACED = "placed";
    private static final String ORDER = "order";
    private static final String VIEW = "view";
    private static final String SCREENS = "screens";
    private static final String HOME = "home";
    private static final String ROLES = "roles";
    private static final String SHAPE = "shape";
    private static final String RECENT = "recent";
    /** How many applications opened from here are remembered. */
    private static final int RECENT_KEPT = 8;

    /** How the list of every application is laid out: lines down, or pages across. */
    static final int LINES = 0;
    static final int PAGES = 1;

    /** The orders the list of every application can stand in. */
    static final int BY_NAME = 0;
    static final int NEWEST = 1;
    static final int UPDATED = 2;

    private Keep() {
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences("ellipse", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------ settings

    /** The settings, each by the key it is kept under. */
    static final String DOCK = "dock";
    /** Lines or pages: the same key the list's own menu keeps its choice under. */
    static final String VIEW_KEY = "view";
    static final String DESK_GRID = "desk_grid";
    static final String DESK_ENDLESS = "desk_endless";
    static final String DOTS = "dots";
    static final String AUTO_ADD = "auto_add";
    static final String LIST_GRID = "list_grid";
    static final String LIST_ENDLESS = "list_endless";
    static final String LIST_DOTS = "list_dots";
    /** Lines or a grid for the shelf of widgets and for the makers of shortcuts. */
    static final String SHELF_VIEW = "shelf_view";
    static final String MAKERS_VIEW = "makers_view";
    static final String CLOCK = "clock";
    /** What the clock shows beside the hour and the charge, and whether leave for them was once asked. */
    static final String WEATHER = "clock_weather";
    static final String EARS = "clock_ears";
    static final String ASKED_WORLD = "asked_world";
    static final String ON_BACK = "on_back";
    static final String ON_UP = "on_up";
    static final String ON_DOWN = "on_down";
    static final String ON_HOME = "on_home";
    static final String ON_DOUBLE = "on_double";
    /** Points on icons of apps with notifications, and the wallpaper following the screens. */
    static final String DOTS_ON = "notice_dots";
    static final String WALL_MOVES = "wall_moves";
    private static final String HIDDEN = "hidden";
    private static final String STAMP = "stamp";

    /** What a gesture may do. */
    static final int DO_NOTHING = 0;
    static final int DO_FRESH = 1;
    static final int DO_LIST = 2;
    static final int DO_NOTICES = 3;
    static final int DO_QUICK = 4;
    static final int DO_HOME = 5;
    static final int DO_LOCK = 6;

    static boolean flag(Context context, String key, boolean fallback) {
        return store(context).getBoolean(key, fallback);
    }

    static void saveFlag(Context context, String key, boolean on) {
        store(context).edit().putBoolean(key, on).apply();
        touch(context);
    }

    static int number(Context context, String key, int fallback) {
        return store(context).getInt(key, fallback);
    }

    static void saveNumber(Context context, String key, int value) {
        store(context).edit().putInt(key, value).apply();
        touch(context);
    }

    /** A grid kept as one number: columns times ten, plus rows. */
    static int columns(int grid) {
        return Math.max(3, Math.min(7, grid / 10));
    }

    static int rows(int grid) {
        return Math.max(3, Math.min(9, grid % 10));
    }

    /** What is left out of the list of every application, by component. */
    static java.util.Set<String> hidden(Context context) {
        return new java.util.HashSet<>(store(context).getStringSet(HIDDEN,
            new java.util.HashSet<String>()));
    }

    static void hide(Context context, String token, boolean hide) {
        java.util.Set<String> set = hidden(context);
        if (hide) {
            set.add(token);
        } else {
            set.remove(token);
        }
        store(context).edit().putStringSet(HIDDEN, set).apply();
        touch(context);
    }

    /** A mark changed with every setting, so the home screen knows to set itself out again. */
    static int stamp(Context context) {
        return store(context).getInt(STAMP, 0);
    }

    private static void touch(Context context) {
        store(context).edit().putInt(STAMP, stamp(context) + 1).apply();
    }

    // ---------------------------------------------------------------- look

    /** Hue, saturation, brightness of the accent, and how solid the containers stand. */
    static float[] look(Context context) {
        SharedPreferences kept = store(context);
        return new float[] {
            kept.getFloat("hue", 38f),
            kept.getFloat("sat", 0.58f),
            kept.getFloat("val", 1f),
            kept.getInt("solid", 100)
        };
    }

    static void saveLook(Context context, float hue, float sat, float val, int solid) {
        store(context).edit()
            .putFloat("hue", hue < 0f ? 0f : (hue > 360f ? 360f : hue))
            .putFloat("sat", sat < 0f ? 0f : (sat > 1f ? 1f : sat))
            .putFloat("val", val < 0.4f ? 0.4f : (val > 1f ? 1f : val))
            .putInt("solid", solid < 55 ? 55 : (solid > 100 ? 100 : solid))
            .apply();
        touch(context);
    }

    /** Where the accent comes from: the owner's own mixing, the system's colour, or the wallpaper's. */
    static final int FROM_OWN = 0;
    static final int FROM_SYSTEM = 1;
    static final int FROM_WALL = 2;

    /** At first the colour follows the phone: its system colour where it has one, else the wallpaper. */
    static int from(Context context) {
        int fallback = android.os.Build.VERSION.SDK_INT >= 31 ? FROM_SYSTEM : FROM_WALL;
        return store(context).getInt("look_from", fallback);
    }

    static void saveFrom(Context context, int from) {
        store(context).edit().putInt("look_from", from).apply();
        touch(context);
    }

    /** How icons and names are drawn: sizes in percent, names on or off, the family of words. */
    static final String ICON_SIZE = "icon_size";
    static final String ICON_FILL = "icon_fill";
    static final String NAMES_SCREENS = "names_screens";
    static final String NAMES_LIST = "names_list";
    static final String NAME_SIZE = "name_size";
    static final String FONT = "font";
    /** Icons in their own colours, all in the accent, or in the accent only where they can be. */
    static final String ICON_TINT = "icon_tint";

    /** The outline every icon is cut to; the phone's own at first. */
    static int shape(Context context) {
        return store(context).getInt("icon_shape", Shape.SYSTEM);
    }

    static void saveShape(Context context, int shape) {
        store(context).edit().putInt("icon_shape", shape).apply();
        touch(context);
    }

    /** How much of the accent's colour the ground takes, in percent; none is near black. */
    static int ground(Context context) {
        return store(context).getInt("ground", 0);
    }

    static void saveGround(Context context, int depth) {
        store(context).edit().putInt("ground", depth < 0 ? 0 : (depth > 100 ? 100 : depth)).apply();
        touch(context);
    }

    /** How large the home screen's own words are drawn, in percent. */
    static int zoom(Context context) {
        return store(context).getInt("zoom", 100);
    }

    static void saveZoom(Context context, int size) {
        store(context).edit().putInt("zoom", size < 70 ? 70 : (size > 200 ? 200 : size)).apply();
        touch(context);
    }

    /** The place the weather is for: its name, and its latitude and longitude as written. */
    static String[] here(Context context) {
        SharedPreferences kept = store(context);
        return new String[] {kept.getString("place", ""), kept.getString("lat", ""), kept.getString("lon", "")};
    }

    static void saveHere(Context context, String name, String lat, String lon) {
        store(context).edit().putString("place", name).putString("lat", lat).putString("lon", lon).apply();
    }

    // ---------------------------------------------------------------- dock

    private static final String DOCK_SLOT = "dock.";
    private static final String FOLDER = "folder.";
    private static final String FOLDER_NEXT = "folder.next";
    private static final String PENDING = "pending";
    static final String FOLDER_THING = "#folder:";
    static final String SHORTCUT_THING = "#shortcut:";

    /**
     * What stands in a place of the dock: an app as it is kept, an empty
     * word for a place emptied by hand, or none while the place still
     * belongs to the everyday role that fills it by default.
     */
    static String dockSlot(Context context, int slot) {
        String key = DOCK_SLOT + slot;
        return store(context).contains(key) ? store(context).getString(key, "") : null;
    }

    static void saveDockSlot(Context context, int slot, String token) {
        store(context).edit().putString(DOCK_SLOT + slot, token == null ? "" : token).apply();
    }

    // ------------------------------------------------------------- folders

    /** A new folder of one's own, empty, with a name; its number. */
    static int newFolder(Context context, String name) {
        int id = store(context).getInt(FOLDER_NEXT, 1);
        store(context).edit().putInt(FOLDER_NEXT, id + 1)
            .putString(FOLDER + id + ".name", name).putString(FOLDER + id + ".items", "").apply();
        return id;
    }

    static String folderName(Context context, int id) {
        return store(context).getString(FOLDER + id + ".name", "Folder");
    }

    static void renameFolder(Context context, int id, String name) {
        store(context).edit().putString(FOLDER + id + ".name", name).apply();
    }

    /** What a folder of one's own holds, as it is kept, in the order it was put in. */
    static List<String> folderItems(Context context, int id) {
        List<String> list = new ArrayList<>();
        for (String line : store(context).getString(FOLDER + id + ".items", "").split("\n")) {
            if (line.length() > 0) {
                list.add(line);
            }
        }
        return list;
    }

    private static void saveFolderItems(Context context, int id, List<String> items) {
        StringBuilder out = new StringBuilder();
        for (String item : items) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(item);
        }
        store(context).edit().putString(FOLDER + id + ".items", out.toString()).apply();
    }

    static void folderAdd(Context context, int id, String token) {
        List<String> items = folderItems(context, id);
        items.remove(token);
        items.add(token);
        saveFolderItems(context, id, items);
    }

    static void folderRemove(Context context, int id, String token) {
        List<String> items = folderItems(context, id);
        items.remove(token);
        saveFolderItems(context, id, items);
    }

    // -------------------------------------------------------------- pending

    /**
     * Things another application asked to put on the home screen while it
     * was not in front: the home screen sets them down the next time it is.
     */
    static void queue(Context context, String token) {
        String was = store(context).getString(PENDING, "");
        store(context).edit().putString(PENDING, was.length() == 0 ? token : was + "\n" + token).apply();
        touch(context);
    }

    static List<String> takeQueue(Context context) {
        List<String> list = new ArrayList<>();
        for (String line : store(context).getString(PENDING, "").split("\n")) {
            if (line.length() > 0) {
                list.add(line);
            }
        }
        store(context).edit().remove(PENDING).apply();
        return list;
    }

    // -------------------------------------------------------------- screens

    /**
     * Takes a screen away: what stood on the screens after it moves one to
     * the left, and home and the everyday roles follow their screens.
     */
    static void dropScreen(Context context, int screen) {
        List<Spot> kept = new ArrayList<>();
        for (Spot spot : placed(context)) {
            if (spot.screen == screen) {
                continue;
            }
            kept.add(spot.screen > screen ? new Spot(spot.token, spot.screen - 1, spot.x, spot.y) : spot);
        }
        write(context, kept);
        int count = screens(context);
        int home = home(context);
        int roles = roles(context);
        SharedPreferences.Editor edit = store(context).edit();
        edit.putInt(SCREENS, Math.max(1, count - 1));
        edit.putInt(HOME, home > screen ? home - 1 : (home == screen ? Math.max(0, home - 1) : home));
        edit.putInt(ROLES, roles > screen ? roles - 1 : (roles == screen ? Math.max(0, roles - 1) : roles));
        edit.apply();
    }

    // --------------------------------------------------------------- purge

    /**
     * An application has left the phone: every trace of it here goes too,
     * from the screens, the dock, folders, the recent and the hidden. It
     * returns the words of pinned shortcuts taken away, so they can be let
     * go of as well.
     */
    static void purge(Context context, String owner, long serial) {
        List<Spot> kept = new ArrayList<>();
        for (Spot spot : placed(context)) {
            if (!belongs(spot.token, owner, serial)) {
                kept.add(spot);
            }
        }
        write(context, kept);
        SharedPreferences.Editor edit = store(context).edit();
        for (int slot = 0; slot < 8; slot++) {
            String token = dockSlot(context, slot);
            if (token != null && belongs(token, owner, serial)) {
                edit.putString(DOCK_SLOT + slot, "");
            }
        }
        edit.apply();
        int next = store(context).getInt(FOLDER_NEXT, 1);
        for (int id = 1; id < next; id++) {
            List<String> items = folderItems(context, id);
            List<String> stay = new ArrayList<>();
            for (String item : items) {
                if (!belongs(item, owner, serial)) {
                    stay.add(item);
                }
            }
            if (stay.size() != items.size()) {
                saveFolderItems(context, id, stay);
            }
        }
        List<String> recent = recent(context);
        StringBuilder out = new StringBuilder();
        for (String token : recent) {
            if (!belongs(token, owner, serial)) {
                out.append(out.length() > 0 ? "\n" : "").append(token);
            }
        }
        java.util.Set<String> hidden = hidden(context);
        java.util.Iterator<String> each = hidden.iterator();
        while (each.hasNext()) {
            if (belongs(each.next(), owner, serial)) {
                each.remove();
            }
        }
        store(context).edit().putString(RECENT, out.toString()).putStringSet(HIDDEN, hidden).apply();
        touch(context);
    }

    /** Whether a kept word is an app, or a pinned shortcut, of a package in a profile. */
    static boolean belongs(String token, String owner, long serial) {
        if (token.startsWith(SHORTCUT_THING)) {
            String rest = token.substring(SHORTCUT_THING.length());
            return rest.startsWith(owner + "/") && Apps.serialOf(token) == serial;
        }
        ComponentName name = Apps.nameOf(token);
        return name != null && name.getPackageName().equals(owner) && Apps.serialOf(token) == serial;
    }

    /** Lets the home screen know, on its return, that it must set itself out again. */
    static void nudge(Context context) {
        touch(context);
    }

    /** Everything kept is forgotten: the next start is a first start. */
    static void reset(Context context) {
        store(context).edit().clear().commit();
    }

    static int order(Context context) {
        int kept = store(context).getInt(ORDER, BY_NAME);
        return kept < BY_NAME || kept > UPDATED ? BY_NAME : kept;
    }

    static void saveOrder(Context context, int order) {
        store(context).edit().putInt(ORDER, order).apply();
    }

    static int view(Context context) {
        return store(context).getInt(VIEW, LINES) == PAGES ? PAGES : LINES;
    }

    static void saveView(Context context, int view) {
        store(context).edit().putInt(VIEW, view).apply();
    }

    /**
     * Brings what an earlier version kept into today's shape, once. A first
     * start, or a phone that never added a screen, gets three screens with
     * the middle one as home, and whatever stood on the single screen moves
     * onto that middle one.
     */
    static void settle(Context context) {
        SharedPreferences kept = store(context);
        if (kept.getInt(SHAPE, 0) >= 1) {
            return;
        }
        SharedPreferences.Editor edit = kept.edit();
        if (!kept.contains(SCREENS)) {
            StringBuilder out = new StringBuilder();
            for (Spot spot : placed(context)) {
                out.append(new Spot(spot.token, spot.screen + 1, spot.x, spot.y).line()).append('\n');
            }
            edit.putString(PLACED, out.toString());
            edit.putInt(SCREENS, 3).putInt(HOME, 1).putInt(ROLES, 1);
        } else {
            edit.putInt(ROLES, 0);
        }
        edit.putInt(SHAPE, 1).commit();
    }

    /** How many screens stand side by side; never fewer than one. */
    static int screens(Context context) {
        return Math.max(1, store(context).getInt(SCREENS, 3));
    }

    /** The screen the everyday roles stand on: where home was when they were first set out. */
    static int roles(Context context) {
        int roles = store(context).getInt(ROLES, 1);
        return roles < 0 || roles >= screens(context) ? 0 : roles;
    }

    /** The applications last opened from here, the latest first, as they are kept. */
    static List<String> recent(Context context) {
        List<String> list = new ArrayList<>();
        for (String line : store(context).getString(RECENT, "").split("\n")) {
            if (line.length() > 0 && Apps.nameOf(line) != null) {
                list.add(line);
            }
        }
        return list;
    }

    static void opened(Context context, String token) {
        StringBuilder out = new StringBuilder(token);
        int count = 1;
        for (String was : recent(context)) {
            if (count >= RECENT_KEPT) {
                break;
            }
            if (!was.equals(token)) {
                out.append('\n').append(was);
                count++;
            }
        }
        store(context).edit().putString(RECENT, out.toString()).apply();
    }

    static void saveScreens(Context context, int count) {
        store(context).edit().putInt(SCREENS, Math.max(1, count)).apply();
    }

    /** Which screen Home returns to, counted from the left. */
    static int home(Context context) {
        int home = store(context).getInt(HOME, 1);
        return home < 0 || home >= screens(context) ? 0 : home;
    }

    static void saveHome(Context context, int screen) {
        store(context).edit().putInt(HOME, screen).apply();
    }

    /**
     * One line per spot: screen, column, row and the door, apart by tabs.
     * A line of the first versions has no screen and stands on the first.
     */
    static List<Spot> placed(Context context) {
        List<Spot> list = new ArrayList<>();
        String kept = store(context).getString(PLACED, "");
        for (String line : kept.split("\n")) {
            String[] part = line.split("\t");
            if (part.length != 3 && part.length != 4) {
                continue;
            }
            int shift = part.length - 3;
            String token = part[2 + shift];
            if (!token.startsWith("#") && Apps.nameOf(token) == null) {
                continue;
            }
            try {
                int screen = shift == 1 ? Integer.parseInt(part[0]) : 0;
                list.add(new Spot(token, screen, Integer.parseInt(part[shift]),
                    Integer.parseInt(part[1 + shift])));
            } catch (NumberFormatException broken) {
                // A line that cannot be read is let go.
            }
        }
        return list;
    }

    private static void write(Context context, List<Spot> spots) {
        StringBuilder out = new StringBuilder();
        for (Spot spot : spots) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(spot.line());
        }
        store(context).edit().putString(PLACED, out.toString()).apply();
    }

    /** Sets a thing down in a place; whatever stood exactly there before gives way. */
    static void place(Context context, String token, int screen, int x, int y) {
        List<Spot> kept = new ArrayList<>();
        for (Spot spot : placed(context)) {
            if (spot.screen == screen && spot.x == x && spot.y == y) {
                continue;
            }
            kept.add(spot);
        }
        kept.add(new Spot(token, screen, x, y));
        write(context, kept);
    }

    /** Moves the thing standing in one place to another. */
    static void shift(Context context, int screen, int x, int y, int toScreen, int toX, int toY) {
        List<Spot> kept = new ArrayList<>();
        for (Spot spot : placed(context)) {
            if (spot.screen == screen && spot.x == x && spot.y == y) {
                kept.add(new Spot(spot.token, toScreen, toX, toY));
            } else {
                kept.add(spot);
            }
        }
        write(context, kept);
    }

    /** Takes away whatever stands in a place. */
    static void remove(Context context, int screen, int x, int y) {
        List<Spot> kept = new ArrayList<>();
        for (Spot spot : placed(context)) {
            if (!(spot.screen == screen && spot.x == x && spot.y == y)) {
                kept.add(spot);
            }
        }
        write(context, kept);
    }

    /** The thing in one place is kept again under a new word and in a new place: a new size, as a rule. */
    static void reshape(Context context, int screen, int x, int y, String token, int toX, int toY) {
        List<Spot> kept = new ArrayList<>();
        for (Spot spot : placed(context)) {
            if (spot.screen == screen && spot.x == x && spot.y == y) {
                kept.add(new Spot(token, screen, toX, toY));
            } else {
                kept.add(spot);
            }
        }
        write(context, kept);
    }

    /**
     * Whether the screens are set out by hand now. Until something that
     * came with the default set-out is first moved, the default is laid
     * anew every time; from then on, only what is kept here stands.
     */
    static boolean laid(Context context) {
        return store(context).getBoolean(LAID, false);
    }

    /** Keeps the whole set-out as it stands, and from now on only it. */
    static void lay(Context context, List<Spot> spots) {
        write(context, spots);
        store(context).edit().putBoolean(LAID, true).apply();
    }
}
