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

    /** Every setting whose word begins so let go of; the home screen is set out again. */
    static void forgetStarting(Context context, String... starts) {
        SharedPreferences kept = store(context);
        SharedPreferences.Editor edit = kept.edit();
        for (String key : kept.getAll().keySet()) {
            for (String start : starts) {
                if (key.startsWith(start)) {
                    edit.remove(key);
                }
            }
        }
        edit.apply();
        touch(context);
    }

    /** A number noted for the settings to read, not a setting: the home screen is not set out again for it. */
    static void note(Context context, String key, int value) {
        store(context).edit().putInt(key, value).apply();
    }

    static void saveNumber(Context context, String key, int value) {
        store(context).edit().putInt(key, value).apply();
        touch(context);
    }

    /** A grid kept as one number: columns times ten, plus rows. */
    /* A grid is kept as its columns and rows side by side: 45 is four by
       five; from ten rows on, the rows take two figures, so 511 is five by
       eleven. */
    static int columns(int grid) {
        return Math.max(3, Math.min(7, grid >= 100 ? grid / 100 : grid / 10));
    }

    static int rows(int grid) {
        return Math.max(3, Math.min(12, grid >= 100 ? grid % 100 : grid % 10));
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
    /** The pack of icons read, by its package; none, the phone's own icons. */
    static final String ICON_PACK = "icon_pack";

    /** A word kept under a key, or none. */
    static String word(Context context, String key) {
        return store(context).getString(key, null);
    }

    static void saveWord(Context context, String key, String value) {
        if (value == null || value.isEmpty()) {
            store(context).edit().remove(key).apply();
        } else {
            store(context).edit().putString(key, value).apply();
        }
        touch(context);
    }
    /** How a folder's face lays out what it holds, and whether its container is drawn. */
    static final String FOLDER_FACE = "folder_face";
    static final String FOLDER_GROUND = "folder_ground";
    /** The icons' rim: its material (less than nought for none), its width in percent, and glass over it. */
    static final String RIM_KIND = "rim_kind";
    static final String RIM_WIDTH = "rim_width";
    static final String GLAZE = "glaze";
    static final String GLASS_TONE = "glass_tone";
    static final String GLASS_CLEAR = "glass_clear";
    /** Which clock stands across the top — the first, or another — and what the plate and its dial are made of. */
    static final String CLOCK_FACE = "clock_face";
    static final String CLOCK_PLATE = "clock_plate";
    static final String CLOCK_DIAL = "clock_dial_of";
    static final String CLOCK_FIELDS = "clock_fields";
    /** Whether the clock reached the screen's edges: the old word, read once into the grid's. */
    static final String CLOCK_EDGE = "clock_edge";
    /** Whether widgets on the grid's first or last column ran past its margins: the old word. */
    static final String GRID_EDGES = "grid_edges";
    /**
     * How the grid meets the screen's edges: within its margins; its
     * widgets past them; or with no margins at all, icons and widgets to
     * the very edge and screen against screen with nothing between.
     */
    static final String EDGES = "edges";
    static final int EDGES_MARGINS = 0;
    static final int EDGES_WIDGETS = 1;
    static final int EDGES_ALL = 2;
    /** The clock's size as it last stood on a screen, in dp: what the settings show it at. */
    static final String CLOCK_WIDE = "clock_wide";
    static final String CLOCK_TALL = "clock_tall";
    /** Whether the phone's bars are hidden over the home screen: the status bar, the navigation bar. */
    static final String HIDE_STATUS = "hide_status";
    static final String HIDE_NAVIGATION = "hide_navigation";
    /** How dark the outline clock's ground is, in percent: nought is clear. */
    static final String CLOCK_GROUND = "clock_ground";
    /** Which of the widget clock's own seconds hands is drawn. */
    static final String MENO_SECOND = "meno_second";
    /** The rings: what the big one holds, what each small one holds, and where each stands. */
    static final String RINGS_BIG = "rings_big";
    static final String RINGS_FIRST = "rings_first";
    static final String RINGS_SECOND = "rings_second";
    static final String RINGS_PLACES = "rings_places";

    /** The rings' colours, by their place in the rings' list: levels, the seconds hand, the day. */
    static final String RINGS_LEVEL = "rings_level";
    static final String RINGS_SECONDS = "rings_seconds";
    static final String RINGS_DAY = "rings_day";

    /** The small rings in their order, each by a number of its own, and what each holds. */
    static final String RINGS_LIST = "rings_list";
    static final String RING_KIND = "ring_kind.";
    /** The most small rings there may be: seven, and the big one makes eight. */
    static final int RINGS_MOST = 7;

    /**
     * The small rings' numbers, in order. The first time, the two the
     * settings named before become the first two, and the headphones'
     * place, kept then under the number three, is kept under its own word.
     */
    static int[] ringIds(Context context) {
        SharedPreferences kept = store(context);
        if (!kept.contains(RINGS_LIST)) {
            StringBuilder places = new StringBuilder();
            for (String line : kept.getString(RINGS_PLACES, "").split(";")) {
                if (line.isEmpty()) {
                    continue;
                }
                if (places.length() > 0) {
                    places.append(';');
                }
                places.append(line.startsWith("3:") ? "e" + line.substring(1) : line);
            }
            kept.edit().putString(RINGS_LIST, "1,2")
                .putInt(RING_KIND + 1, kept.getInt(RINGS_FIRST, 0))
                .putInt(RING_KIND + 2, kept.getInt(RINGS_SECOND, 2))
                .putString(RINGS_PLACES, places.toString()).apply();
            return new int[] {1, 2};
        }
        String said = kept.getString(RINGS_LIST, "");
        if (said.isEmpty()) {
            return new int[0];
        }
        String[] part = said.split(",");
        int[] ids = new int[part.length];
        int n = 0;
        for (String one : part) {
            try {
                ids[n++] = Integer.parseInt(one.trim());
            } catch (NumberFormatException broken) {
                n--;
            }
        }
        return java.util.Arrays.copyOf(ids, Math.max(0, n));
    }

    static void saveRingIds(Context context, int[] ids) {
        StringBuilder out = new StringBuilder();
        for (int id : ids) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(id);
        }
        store(context).edit().putString(RINGS_LIST, out.toString()).apply();
        touch(context);
    }

    /** Where the rings were set by hand, as the rings wrote it; empty if never. */
    static String rings(Context context) {
        return store(context).getString(RINGS_PLACES, "");
    }

    /** Where the rings stand now, set by hand; the home screen is set out again for it. */
    static void saveRings(Context context, String places) {
        store(context).edit().putString(RINGS_PLACES, places).apply();
        touch(context);
    }

    /**
     * Half steps: every place of the screens split in two each way, so a
     * thing may stand half a place along or down. An icon then takes two
     * by two of the small places. The set-out is kept in the places it was
     * written in; when the steps change, it is written again in the new.
     */
    static final String HALF_STEPS = "half_steps";
    /**
     * The shape of a place: the screen's share (as many rows as the grid
     * says, each as high as the screen gives), or square, wide, or tall —
     * then as many rows as fit.
     */
    static final String CELL_SHAPE = "cell_shape";
    static final int SHAPE_SCREEN = 0;
    static final int SHAPE_SQUARE = 1;
    static final int SHAPE_WIDE = 2;
    static final int SHAPE_TALL = 3;

    /** A place's height to its width in a shape; nought for the screen's share. */
    static float cellRatio(int shape) {
        return shape == SHAPE_SQUARE ? 1f : shape == SHAPE_WIDE ? 0.8f : shape == SHAPE_TALL ? 1.25f : 0f;
    }

    /**
     * Widgets' frames: the material, as the icons' rims have it (none, or a
     * material of the rim's), the frame's width and its corners in dp, and
     * whether the curve of glass lies over it; one widget may go without.
     */
    static final String WIDGET_FRAME = "widget_frame";
    static final String WIDGET_FRAME_WIDTH = "widget_frame_width";
    static final String WIDGET_FRAME_ROUND = "widget_frame_round";
    static final String WIDGET_GLAZE = "widget_glaze";
    static final String FRAME_OFF = "frame_off.";

    /** How many lines a name under an icon may take: on the screens, and in the list and the panels. */
    static final String NAME_LINES_SCREENS = "name_lines_screens";
    static final String NAME_LINES_LIST = "name_lines_list";

    /** The colour of the names under the icons on the screens: light, dark, or one of the rings' colours. */
    static final String NAME_COLOUR = "name_colour";
    static final int NAME_LIGHT = -1;
    static final int NAME_DARK = -2;

    /** Whether things may be set down over one another, the last set down on top. */
    static final String OVERLAP = "overlap";
    private static final String LAYOUT_FINE = "layout_fine";

    /** How many small places make one place each way: two with half steps, else one. */
    static int fine(Context context) {
        return flag(context, HALF_STEPS, false) ? 2 : 1;
    }

    /**
     * The set-out brought to the steps now chosen: each place and each
     * block's size multiplied by two, or halved, rounding a half place
     * down and a half block up. Done once, when the steps change.
     */
    static void settleFine(Context context) {
        SharedPreferences kept = store(context);
        int now = fine(context);
        int was = kept.getInt(LAYOUT_FINE, 1);
        if (was == now) {
            return;
        }
        if (laid(context)) {
            List<Spot> spots = placed(context);
            List<Spot> moved = new ArrayList<>();
            for (Spot spot : spots) {
                moved.add(new Spot(rescaled(spot.token, was, now), spot.screen, spot.x * now / was,
                    spot.y * now / was));
            }
            write(context, moved);
        }
        kept.edit().putInt(LAYOUT_FINE, now).apply();
    }

    /** A thing's word with the size of its block, if it has one, in the new steps. */
    private static String rescaled(String token, int was, int now) {
        String[] part = token.split(":");
        int first;
        if (token.startsWith(FOLDER_THING) || token.startsWith("#widget:")) {
            first = 2;
        } else if (token.startsWith("#")) {
            first = 1;
        } else {
            return token;
        }
        if (part.length != first + 2) {
            return token;
        }
        try {
            int a = Integer.parseInt(part[first]);
            int d = Integer.parseInt(part[first + 1]);
            a = Math.max(1, (a * now + was - 1) / was);
            d = Math.max(1, (d * now + was - 1) / was);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < first; i++) {
                out.append(part[i]).append(':');
            }
            return out.append(a).append(':').append(d).toString();
        } catch (NumberFormatException broken) {
            return token;
        }
    }

    /** The set-out written in whole places, with half steps off: as a set-out brought in is written. */
    static void whole(Context context) {
        store(context).edit().putBoolean(HALF_STEPS, false).putInt(LAYOUT_FINE, 1).apply();
        touch(context);
    }

    /** Dark surfaces, light ones, or as the phone is set: the ground of lists, cards and settings. */
    static final String THEME = "theme";
    static final int THEME_DARK = 0;
    static final int THEME_LIGHT = 1;
    static final int THEME_PHONE = 2;

    /**
     * The list of every app on a colour of the owner's own: whether it is,
     * and the colour's hue in degrees, its saturation, brightness and
     * opacity in percent.
     */
    static final String LIST_OWN = "list_own";
    static final String LIST_HUE = "list_hue";
    static final String LIST_SAT = "list_sat";
    static final String LIST_VAL = "list_val";
    static final String LIST_ALPHA = "list_alpha";
    /** Whether the list of every app is sorted into kinds under tabs, and how; and the tab it was left on. */
    static final String KINDS = "kinds";
    static final String KIND_TAB = "kind_tab";
    /** The language module chosen, by its code; none for English. */
    static final String LANGUAGE = "language";
    /** Which of its three the panel of fresh apps opens on. */
    static final String FRESH_TAB = "fresh_tab";

    /** How high a row of the grid stands, in percent of its full share of the screen. */
    static final String ROW_HEIGHT = "row_height";

    /** How strong the widget clock's fine lines are, in percent of their own. */
    static final String MENO_LINES = "meno_lines";
    /** The window cut in the icons' plate, and the tile's width to its height in percent. */
    static final String WINDOW = "icon_window";
    static final String TILE_ASPECT = "tile_aspect";

    private static final String FACE = "face.";

    /**
     * One icon's own face, kept by the word the thing is kept by: its
     * colour (less than nought: as every icon), the home screen's drawing
     * for it (one: yes), the way it is inked, a picture of the owner's own
     * put in its place (one: yes), and a symbol put in its place, by name.
     * All at rest forgets it.
     */
    static void saveFace(Context context, String token, int tint, int drawing, int method, int image,
                         String symbol) {
        SharedPreferences.Editor edit = store(context).edit();
        String sym = symbol == null ? "" : symbol;
        if (tint < 0 && drawing <= 0 && method <= 0 && image <= 0 && sym.length() == 0) {
            edit.remove(FACE + token);
        } else {
            edit.putString(FACE + token, "-1," + tint + "," + drawing + "," + method + "," + image + "," + sym);
        }
        edit.apply();
        touch(context);
    }

    /** Every icon given a face of its own: word, and the parts of its face as kept. */
    static java.util.Map<String, String[]> faces(Context context) {
        java.util.Map<String, String[]> all = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, ?> each : store(context).getAll().entrySet()) {
            if (each.getKey().startsWith(FACE) && each.getValue() instanceof String) {
                String[] part = ((String) each.getValue()).split(",", -1);
                String[] six = {"-1", "-1", "0", "0", "0", ""};
                System.arraycopy(part, 0, six, 0, Math.min(6, part.length));
                all.put(each.getKey().substring(FACE.length()), six);
            }
        }
        return all;
    }

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
     * A link: an older kind of shortcut, a call to open something — a page,
     * a contact, a place in an app — with its own name and picture, kept by
     * the home screen itself rather than by the app it opens.
     */
    static final String LINK_THING = "#link:";
    private static final String LINK = "link.";
    private static final String LINK_NEXT = "link.next";

    /** A link kept: its call, its name and its picture; its number returned. */
    static int newLink(Context context, String call, String name, byte[] picture) {
        int id = store(context).getInt(LINK_NEXT, 1);
        store(context).edit().putInt(LINK_NEXT, id + 1).putString(LINK + id + ".call", call)
            .putString(LINK + id + ".name", name == null ? "" : name).apply();
        if (picture != null && picture.length > 0) {
            java.io.File file = linkPicture(context, id);
            try (java.io.OutputStream out = new java.io.FileOutputStream(file)) {
                out.write(picture);
            } catch (java.io.IOException unsaved) {
                // The link keeps a plain picture.
            }
        }
        return id;
    }

    static String linkCall(Context context, int id) {
        return store(context).getString(LINK + id + ".call", null);
    }

    static String linkName(Context context, int id) {
        return store(context).getString(LINK + id + ".name", "");
    }

    static java.io.File linkPicture(Context context, int id) {
        java.io.File room = new java.io.File(context.getFilesDir(), "links");
        room.mkdirs();
        return new java.io.File(room, id + ".png");
    }

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
    /**
     * Whether widgets run to the screen's edges: the grid's own setting,
     * taken over the first time from the clock's, which was its first form.
     */
    static boolean edges(Context context) {
        return edgeMode(context) != EDGES_MARGINS;
    }

    /** Whether the grid has no margins at all. */
    static boolean edgeless(Context context) {
        return edgeMode(context) == EDGES_ALL;
    }

    /** How the grid meets the edges; the first time, taken from the words that said it before. */
    static int edgeMode(Context context) {
        SharedPreferences kept = store(context);
        if (!kept.contains(EDGES)) {
            boolean was = kept.contains(GRID_EDGES) ? kept.getBoolean(GRID_EDGES, false)
                : kept.getBoolean(CLOCK_EDGE, false);
            int mode = was ? EDGES_WIDGETS : EDGES_MARGINS;
            kept.edit().putInt(EDGES, mode).apply();
            return mode;
        }
        return Math.max(EDGES_MARGINS, Math.min(EDGES_ALL, kept.getInt(EDGES, EDGES_MARGINS)));
    }

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

    private static final String USES = "uses";
    private static final int USES_KEPT = 200;

    /** How many times each application was opened from here, the most first. */
    static List<String> frequent(Context context) {
        final java.util.Map<String, Integer> counts = uses(context);
        List<String> list = new ArrayList<>();
        for (String token : counts.keySet()) {
            if (Apps.nameOf(token) != null) {
                list.add(token);
            }
        }
        java.util.Collections.sort(list, new java.util.Comparator<String>() {
            public int compare(String a, String b) {
                return counts.get(b) - counts.get(a);
            }
        });
        return list;
    }

    private static java.util.Map<String, Integer> uses(Context context) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String line : store(context).getString(USES, "").split("\n")) {
            int cut = line.lastIndexOf('\t');
            if (cut > 0) {
                try {
                    counts.put(line.substring(0, cut), Integer.parseInt(line.substring(cut + 1)));
                } catch (NumberFormatException broken) {
                    // A line that says nothing is left out.
                }
            }
        }
        return counts;
    }

    static void opened(Context context, String token) {
        /* Counted too, for the most opened; the least counted go when there are too many. */
        java.util.Map<String, Integer> counts = uses(context);
        Integer times = counts.get(token);
        counts.put(token, times == null ? 1 : times + 1);
        List<java.util.Map.Entry<String, Integer>> all = new ArrayList<>(counts.entrySet());
        java.util.Collections.sort(all, new java.util.Comparator<java.util.Map.Entry<String, Integer>>() {
            public int compare(java.util.Map.Entry<String, Integer> a, java.util.Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < Math.min(USES_KEPT, all.size()); i++) {
            kept.append(all.get(i).getKey()).append('\t').append(all.get(i).getValue()).append('\n');
        }
        store(context).edit().putString(USES, kept.toString()).apply();
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

    /**
     * Keeps one thing of a kind in one place: whatever of that kind was
     * kept anywhere else is let go, and the thing is kept here under the
     * word given.
     */
    static void keepOnly(Context context, String kind, String token, int screen, int x, int y) {
        List<Spot> kept = new ArrayList<>();
        for (Spot spot : placed(context)) {
            boolean same = spot.token.equals(kind) || spot.token.startsWith(kind + ":");
            if (same || (spot.screen == screen && spot.x == x && spot.y == y)) {
                continue;
            }
            kept.add(spot);
        }
        kept.add(new Spot(token, screen, x, y));
        write(context, kept);
    }

    /** Moves the thing standing in one place to another. */
    static void shift(Context context, int screen, int x, int y, int toScreen, int toX, int toY) {
        /* The thing moved is written last: over others, it stands on top. */
        List<Spot> kept = new ArrayList<>();
        Spot moved = null;
        for (Spot spot : placed(context)) {
            if (spot.screen == screen && spot.x == x && spot.y == y) {
                moved = new Spot(spot.token, toScreen, toX, toY);
            } else {
                kept.add(spot);
            }
        }
        if (moved != null) {
            kept.add(moved);
        }
        write(context, kept);
    }

    /**
     * A thing brought to the top of those over one another, or sent under
     * them all: it is written last, or first, for the last written stands
     * on top.
     */
    static void stack(Context context, int screen, int x, int y, boolean top) {
        List<Spot> kept = new ArrayList<>();
        Spot moved = null;
        for (Spot spot : placed(context)) {
            if (spot.screen == screen && spot.x == x && spot.y == y) {
                moved = spot;
            } else {
                kept.add(spot);
            }
        }
        if (moved == null) {
            return;
        }
        if (top) {
            kept.add(moved);
        } else {
            kept.add(0, moved);
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
