package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The string table.
 *
 * English lives in the source, keyed by a stable id. Every other tongue lives
 * outside the source, in a module anyone can fill in a text editor and hand
 * to a stranger. A missing key falls back to English silently, so a module is
 * useful from its first line.
 */
public final class Words {

    /** The line a module opens with; it is how a module is told from any other text. */
    public static final String HEAD = "# a language for this home screen";

    public static final String[] IDS = {
        "settings",
        "back",
        "drawer",
        "search",
        "tile",
        "shape_superellipse",
        "shape_squircle",
        "shape_circle",
        "shape_square",
        "roundness",
        "proportion",
        "zoom",
        "rim",
        "rim_metal",
        "rim_gold",
        "rim_wood",
        "rim_bling",
        "rim_black",
        "rim_white",
        "rim_accent",
        "rim_bare",
        "rim_width",
        "window",
        "window_follows",
        "window_round",
        "window_raw",
        "light",
        "light_matte",
        "light_gloss",
        "way_down",
        "way_across",
        "order",
        "order_name",
        "order_installed",
        "order_updated",
        "screens",
        "columns",
        "rows",
        "turning",
        "turn_ends",
        "turn_round",
        "widgets",
        "widget",
        "tap_to_place",
        "no_room",
        "clock",
        "hidden",
        "state_none",
        "row",
        "column",
        "to_middle",
        "middle_cannot",
        "immersion",
        "imm_bars",
        "imm_status",
        "imm_nav",
        "imm_full",
        "edge_inside",
        "edge_glass",
        "column_taken",
        "row_taken",
        "colour",
        "hue",
        "richness",
        "wallpaper",
        "export",
        "import",
        "files",
        "folder",
        "folder_choose",
        "folder_none",
        "saved_in",
        "import_what",
        "foreign_read",
        "folder_empty",
        "not_readable",
        "exported",
        "language",
        "language_what",
        "english",
        "words_of",
        "module_load",
        "module_save",
        "use_english",
        "module_bad",
        "module_saved",
        "unsaved",
        "home_screen",
        "home_screen_what",
        "about",
        "shortcuts_home",
        "screens_of",
        "screens_note",
        "no_entry",
        "remove",
        "put_door",
        "add_screen",
        "remove_screen",
        "main_screen",
        "screen_n",
        "width",
        "height",
        "recent",
        "fresh",
        "tag_new",
        "tag_updated",
        "fresh_empty",
        "fault",
        "fault_send",
        "fault_drop",
        "theme",
        "theme_auto",
        "theme_light",
        "theme_dark",
        "door_disc_blue",
        "door_disc_light",
        "door_disc_dark",
        "door_squircle",
        "door_colours",
        "door_dots",
        "door_four",
        "door_ring",
        "door_arch",
        "door_rise",
        "door_star",
        "door_keyhole",
        "door_glass",
        "look_mine",
        "look_default",
        "dock",
        "dock_on",
    };

    private static final String[] EN = {
        "Settings",
        "Back",
        "Applications",
        "Search",
        "Tile",
        "Superellipse",
        "Squircle",
        "Circle",
        "Square",
        "Roundness",
        "Proportion",
        "Zoom",
        "Rim",
        "Metal",
        "Gold",
        "Wood",
        "Bling",
        "Black",
        "Steel",
        "Accent",
        "Bare",
        "Rim width",
        "Window",
        "Tile",
        "Medallion",
        "No mask",
        "Light",
        "Matte",
        "Gloss",
        "Down",
        "Across",
        "Order",
        "Name",
        "Installed",
        "Updated",
        "Screens",
        "Columns",
        "Rows",
        "Turning",
        "Ends",
        "Round",
        "Widgets",
        "Widget",
        "Tap to place",
        "No room for it on this screen.",
        "Clock",
        "Hidden",
        "None",
        "Row",
        "Column",
        "To the middle",
        "A width of {w} cannot stand exactly in the middle of {n} columns: make it one wider or one narrower",
        "Immersion",
        "Bars",
        "No status",
        "No navigation",
        "Full",
        "Inside the margins",
        "To the edge",
        "Something stands in column {n}; move it first.",
        "Something stands in row {n}; move it first.",
        "Colour",
        "Hue",
        "Richness",
        "From the wallpaper",
        "Export",
        "Import",
        "Files",
        "Folder",
        "Choose a folder",
        "Not chosen: every file asks where it goes",
        "Written to the folder: {n}",
        "A layout, a language module, or another home screen's backup",
        "Read from another home screen: {n} screens.",
        "The folder holds nothing to read.",
        "That file is not something Ellipse reads.",
        "The layout, the settings and the language are written.",
        "Language",
        "English lives inside; every other language is a module",
        "English",
        "{n} of {m} words",
        "Load a module",
        "Save the template",
        "Use English",
        "That file is not a language module.",
        "The template is written.",
        "It could not be written.",
        "Home screen",
        "Which home screen the phone uses",
        "About",
        "Shortcuts come when Ellipse is the phone's home screen",
        "Screens inside",
        "A press opens one; a long press sets it on a screen. Not every screen opens alone.",
        "This screen does not open from outside.",
        "Remove",
        "Door",
        "Add screen",
        "Remove screen",
        "Main screen",
        "Screen {n}",
        "Width",
        "Height",
        "Recent",
        "New",
        "new",
        "updated",
        "Nothing opened or installed lately.",
        "Something broke",
        "Hand the report on",
        "Let it go",
        "Theme",
        "As the phone",
        "Light",
        "Dark",
        "Blue disc",
        "Light disc",
        "Dark disc",
        "Light squircle",
        "Nine colours",
        "Nine dots",
        "Four tiles",
        "Ellipse",
        "Arch",
        "Upward",
        "Star",
        "Keyhole",
        "Bare glass",
        "My own",
        "Default",
        "Dock",
        "Along the foot",
    };

    private static final Map<String, String> TABLE = new LinkedHashMap<String, String>();
    private static final Map<String, String> MINE = new LinkedHashMap<String, String>();
    /** For every word of the module, the English it was translated from, as the module said. */
    private static final Map<String, String> FROM = new LinkedHashMap<String, String>();
    private static String moduleName = "";

    static {
        for (int i = 0; i < IDS.length; i++) {
            TABLE.put(IDS[i], EN[i]);
        }
    }

    private Words() {
    }

    public static String en(String id) {
        String s = TABLE.get(id);
        return s == null ? id : s;
    }

    /** The word as the reader should see it: their module first, English behind it. */
    public static String s(String id) {
        String s = MINE.get(id);
        if (s != null && s.length() > 0) {
            return s;
        }
        return en(id);
    }

    public static String name() {
        return moduleName;
    }

    public static boolean active() {
        return MINE.size() > 0;
    }

    public static int filled() {
        return MINE.size();
    }

    public static int total() {
        return IDS.length;
    }

    public static void forget() {
        MINE.clear();
        FROM.clear();
        moduleName = "";
    }

    /**
     * How many words of the module were translated from an English that has
     * changed since: the key is the same, the meaning moved on, and the old
     * translation still stands until a newer module comes. Unknown for a
     * module loaded before the English was kept with it.
     */
    public static int stale() {
        int n = 0;
        for (Map.Entry<String, String> entry : FROM.entrySet()) {
            if (MINE.containsKey(entry.getKey()) && !entry.getValue().equals(en(entry.getKey()))) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- storage

    public static void load(Context context) {
        SharedPreferences p = context.getSharedPreferences("words", Context.MODE_PRIVATE);
        moduleName = p.getString("!name", "");
        MINE.clear();
        FROM.clear();
        for (int i = 0; i < IDS.length; i++) {
            String v = p.getString(IDS[i], "");
            if (v.length() > 0) {
                MINE.put(IDS[i], v);
            }
            String was = p.getString("~" + IDS[i], null);
            if (was != null) {
                FROM.put(IDS[i], was);
            }
        }
    }

    public static void save(Context context) {
        SharedPreferences.Editor e =
            context.getSharedPreferences("words", Context.MODE_PRIVATE).edit();
        e.clear();
        e.putString("!name", moduleName);
        for (Map.Entry<String, String> entry : MINE.entrySet()) {
            e.putString(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : FROM.entrySet()) {
            e.putString("~" + entry.getKey(), entry.getValue());
        }
        e.apply();
    }

    // ----------------------------------------------------------------- module

    /**
     * A module is plain text, and it carries every line whether filled or not:
     * the English above as a comment, the key and the word below. Written out
     * whole, it can be finished in any editor by someone who has never seen
     * the application, and brought back in.
     */
    public static String write() {
        StringBuilder b = new StringBuilder();
        b.append(HEAD).append('\n');
        b.append("# fill the empty lines and load the file back\n\n");
        b.append("module ").append(moduleName.length() > 0 ? moduleName : "untitled")
            .append("\n\n");
        for (int i = 0; i < IDS.length; i++) {
            String mine = MINE.get(IDS[i]);
            b.append("# ").append(en(IDS[i])).append('\n');
            b.append(IDS[i]).append('\t').append(mine == null ? "" : mine).append("\n\n");
        }
        return b.toString();
    }

    /** Whether a text is a module at all, judged by its first line. */
    public static boolean isModule(String text) {
        if (text == null) {
            return false;
        }
        String t = text.startsWith("\uFEFF") ? text.substring(1) : text;
        return t.trim().startsWith(HEAD);
    }

    public static void read(String text) {
        if (text == null) {
            return;
        }
        MINE.clear();
        FROM.clear();
        moduleName = "";
        String[] lines = text.split("\n");
        /* The English a line was made from stands as a comment just above it. */
        String above = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.charAt(0) == '#') {
                above = line.substring(1).trim();
                continue;
            }
            int cut = line.indexOf('\t');
            if (cut < 0) {
                cut = line.indexOf(' ');
            }
            if (cut <= 0) {
                continue;
            }
            String key = line.substring(0, cut).trim();
            String value = line.substring(cut + 1).trim();
            if (key.equals("module")) {
                moduleName = value;
            } else if (TABLE.containsKey(key) && value.length() > 0) {
                MINE.put(key, value);
                if (above != null) {
                    FROM.put(key, above);
                }
            }
            above = null;
        }
    }
}
