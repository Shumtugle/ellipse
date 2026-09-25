package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The set-out of another home screen, read from its backup and brought in.
 *
 * Three shapes are known, and none is known by the names of files or of
 * the applications that wrote them. A file of words, the set-out an
 * earlier line of this home screen wrote. A zip with a database of home
 * items, each pointing to a launchable or a widget. And a zip with a
 * database of favourites, the shape the platform's own home screen keeps
 * and many grew from, where a row's container says where it lies.
 *
 * What comes over: applications at their places, folders with their
 * applications, and widgets, where the phone lets them be made at once.
 * What stays behind, and is counted: applications not on this phone,
 * shortcuts into applications, the other home screen's own gadgets, and
 * its dock, which here is a row of its own.
 */
final class Foreign {

    /** A thing of the other set-out. */
    static final class Item {
        static final int APP = 0;
        static final int FOLDER = 1;
        static final int WIDGET = 2;
        static final int OTHER = 3;
        final int kind;
        final int x;
        final int y;
        int w = 1;
        int h = 1;
        String component;
        String provider;
        String name = "";
        final List<String> apps = new ArrayList<>();

        Item(int kind, int x, int y) {
            this.kind = kind;
            this.x = x;
            this.y = y;
        }
    }

    /** The other set-out: its grid, its screens and which is home, and how it looked, where it said. */
    static final class Layout {
        int columns = 1;
        int rows = 1;
        int home = -1;
        /* How the other home screen looked, where its settings tell: names
           under the icons on the screens, its dock shown, the outline of its
           icons as a width to a height; null where they do not tell. */
        Boolean names;
        Boolean dock;
        Float tile;
        /** The pack of icons it read, by its package; none if it read none. */
        String pack;
        /* The screens' points, their endless turning, no margins at all; and
           the list of every app: pages or lines, its grid, endless, names. */
        Boolean dots;
        Boolean endless;
        Boolean edgeless;
        Boolean listPages;
        Integer listColumns;
        Integer listRows;
        Boolean listEndless;
        Boolean listNames;
        Integer listLines;
        final List<List<Item>> screens = new ArrayList<>();

        int count(int kind) {
            int n = 0;
            for (List<Item> screen : screens) {
                for (Item item : screen) {
                    if (item.kind == kind) {
                        n++;
                    }
                }
            }
            return n;
        }

        void add(int screen, Item item) {
            while (screens.size() <= screen) {
                screens.add(new ArrayList<Item>());
            }
            screens.get(screen).add(item);
            columns = Math.max(columns, item.x + item.w);
            rows = Math.max(rows, item.y + item.h);
        }
    }

    private Foreign() {
    }

    /** A backup's set-out, or none if nothing in it has a known shape. */
    static Layout read(Context context, byte[] file) throws IOException {
        int first = 0;
        while (first < file.length && Character.isWhitespace(file[first])) {
            first++;
        }
        if (first < file.length && file[first] == '{') {
            return words(new String(file, StandardCharsets.UTF_8));
        }
        File dir = new File(context.getCacheDir(), "foreign");
        wipe(dir);
        if (!dir.mkdirs() && !dir.isDirectory()) {
            return null;
        }
        List<File> bases = new ArrayList<>();
        int home = -1;
        Layout seen = new Layout();
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(file));
        try {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                String base = name.substring(name.lastIndexOf('/') + 1);
                if (entry.isDirectory() || base.isEmpty() || base.contains("..")) {
                    continue;
                }
                if (base.endsWith(".xml")) {
                    /* The settings kept beside the set-out may name the home page, and tell how it looked. */
                    java.io.ByteArrayOutputStream kept = new java.io.ByteArrayOutputStream();
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        kept.write(buffer, 0, n);
                    }
                    String said = new String(kept.toByteArray(), StandardCharsets.UTF_8);
                    Matcher page = Pattern.compile("name=\"desktop_default_page\" value=\"(\\d+)\"").matcher(said);
                    if (page.find()) {
                        home = Integer.parseInt(page.group(1));
                    }
                    look(said, seen);
                    continue;
                }
                boolean keeps = base.endsWith(".db") || base.endsWith("-wal") || base.endsWith("-shm")
                    || base.endsWith("-journal") || !base.contains(".");
                if (!keeps) {
                    continue;
                }
                File out = new File(dir, base);
                try (OutputStream write = new FileOutputStream(out)) {
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        write.write(buffer, 0, n);
                    }
                }
                if (!base.endsWith("-wal") && !base.endsWith("-shm") && !base.endsWith("-journal")) {
                    bases.add(out);
                }
            }
        } finally {
            in.close();
        }
        try {
            for (File base : bases) {
                Layout found = open(base);
                if (found != null) {
                    if (home >= 0 && home < found.screens.size()) {
                        found.home = home;
                    }
                    if (seen.home >= 0 && found.home < 0 && seen.home < found.screens.size()) {
                        found.home = seen.home;
                    }
                    found.pack = seen.pack;
                    found.dots = seen.dots;
                    found.endless = seen.endless;
                    found.edgeless = seen.edgeless;
                    found.listPages = seen.listPages;
                    found.listColumns = seen.listColumns;
                    found.listRows = seen.listRows;
                    found.listEndless = seen.listEndless;
                    found.listNames = seen.listNames;
                    found.listLines = seen.listLines;
                    found.names = seen.names;
                    found.dock = seen.dock;
                    found.tile = seen.tile;
                    return found;
                }
            }
            return null;
        } finally {
            wipe(dir);
        }
    }

    /**
     * How the other home screen looked, from the words of its settings:
     * whether names stood under the icons, whether its dock was shown, its
     * home page, and the outline of its icons, where drawn as a path — a
     * width to a height, for a tile.
     */
    private static void look(String said, Layout into) {
        String plain = said.replace("&quot;", "\"");
        Matcher label = Pattern.compile("\"gridUserSettings\":\\{[^}]*?\"hasLabel\":(true|false)").matcher(plain);
        if (label.find()) {
            into.names = Boolean.valueOf(label.group(1));
        }
        Matcher dock = Pattern.compile("\"dockSettings\":\\{\"show\":(true|false)").matcher(plain);
        if (dock.find()) {
            into.dock = Boolean.valueOf(dock.group(1));
        }
        Matcher page = Pattern.compile("\"defaultHomePage\":(\\d+)").matcher(plain);
        if (page.find()) {
            into.home = Integer.parseInt(page.group(1));
        }
        /* The other shape keeps its cells as a scale, then whether names are shown. */
        Matcher cells = Pattern.compile("name=\"desktop_cellspecs\">[^:<]*:(true|false)").matcher(said);
        if (cells.find() && into.names == null) {
            into.names = Boolean.valueOf(cells.group(1));
        }
        Matcher shown = Pattern.compile("name=\"dock_enable\" value=\"(true|false)\"").matcher(said);
        if (shown.find() && into.dock == null) {
            into.dock = Boolean.valueOf(shown.group(1));
        }
        /* The screens, and the list of every app, as the settings of either shape keep them. */
        into.dots = flag(said, plain, into.dots, "name=\"showIndicator\" value=\"(true|false)\"", false);
        Matcher indicator = Pattern.compile("name=\"desktop_scroll_indicator\">([A-Z_]+)<").matcher(said);
        if (indicator.find() && into.dots == null) {
            into.dots = !"NONE".equals(indicator.group(1));
        }
        into.endless = flag(said, plain, into.endless, "name=\"homeInfiniteScroll\" value=\"(true|false)\"", false);
        into.endless = flag(said, plain, into.endless, "name=\"desktop_infinite_scroll\" value=\"(true|false)\"",
            false);
        Matcher margin = Pattern.compile("name=\"desktop_width_margin\">([A-Z_]+)<").matcher(said);
        if (margin.find()) {
            into.edgeless = "NONE".equals(margin.group(1));
        }
        Matcher style = Pattern.compile("name=\"drawer_style\">([A-Z_]+)<").matcher(said);
        if (style.find()) {
            into.listPages = style.group(1).startsWith("HORIZONTAL");
        }
        Matcher across = Pattern.compile("name=\"drawer_app_grid_cols\" value=\"(\\d+)\"").matcher(said);
        if (across.find()) {
            into.listColumns = Integer.parseInt(across.group(1));
        }
        Matcher down = Pattern.compile("name=\"drawer_app_grid_rows\" value=\"(\\d+)\"").matcher(said);
        if (down.find()) {
            into.listRows = Integer.parseInt(down.group(1));
        }
        Matcher portrait = Pattern.compile("\"drawerSettings\":\\{\"portraitColumns\":(\\d+)").matcher(plain);
        if (portrait.find() && into.listColumns == null) {
            into.listColumns = Integer.parseInt(portrait.group(1));
        }
        into.listEndless = flag(said, plain, into.listEndless,
            "name=\"drawer_infinite_scroll\" value=\"(true|false)\"", false);
        Matcher listed = Pattern.compile("name=\"drawer_cellspecs\">[^:<]*:(true|false)").matcher(said);
        if (listed.find()) {
            into.listNames = Boolean.valueOf(listed.group(1));
        }
        Matcher drawn = Pattern.compile("\"drawerSettings\":\\{[^}]*?\"hasLabel\":(true|false)").matcher(plain);
        if (drawn.find() && into.listNames == null) {
            into.listNames = Boolean.valueOf(drawn.group(1));
        }
        Matcher labelLines = Pattern.compile("\"drawerSettings\":\\{[^}]*?\"labelLines\":(\\d+)").matcher(plain);
        if (labelLines.find()) {
            into.listLines = Integer.parseInt(labelLines.group(1));
        }
        Matcher pack = Pattern.compile("name=\"homeIconAppearanceKey\">[^<]*?icPk:([^;<]+)").matcher(said);
        if (pack.find()) {
            into.pack = pack.group(1).trim();
        }
        /* The other shape keeps its pack as its name, its kind, its package, and a flag, by colons. */
        Matcher theme = Pattern.compile("name=\"theme_icon_pack\">([^<]+)<").matcher(said);
        if (theme.find() && into.pack == null) {
            for (String part : theme.group(1).split(":")) {
                String one = part.trim();
                if (one.contains(".") && !one.contains(" ")) {
                    into.pack = one;
                }
            }
        }
        Matcher shape = Pattern.compile("name=\"homeIconAppearanceKey\">[^<]*?path:([^;<]+)").matcher(said);
        if (shape.find()) {
            float left = Float.MAX_VALUE;
            float right = -Float.MAX_VALUE;
            float top = Float.MAX_VALUE;
            float bottom = -Float.MAX_VALUE;
            Matcher point = Pattern.compile("(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)").matcher(shape.group(1));
            while (point.find()) {
                float x = Float.parseFloat(point.group(1));
                float y = Float.parseFloat(point.group(2));
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
            }
            if (right > left && bottom > top) {
                into.tile = (right - left) / (bottom - top);
            }
        }
    }

    /** A yes or no from the settings' words, if found and not known yet. */
    private static Boolean flag(String said, String plain, Boolean was, String pattern, boolean fromPlain) {
        if (was != null) {
            return was;
        }
        Matcher found = Pattern.compile(pattern).matcher(fromPlain ? plain : said);
        return found.find() ? Boolean.valueOf(found.group(1)) : null;
    }

    // ------------------------------------------------------------ words

    /** The set-out an earlier line of this home screen wrote, as words. */
    private static Layout words(String text) {
        try {
            JSONObject all = new JSONObject(text);
            if (!all.has("screens")) {
                return null;
            }
            Layout layout = new Layout();
            layout.home = all.optInt("home", -1);
            JSONArray screens = all.getJSONArray("screens");
            for (int s = 0; s < screens.length(); s++) {
                JSONArray items = screens.getJSONObject(s).optJSONArray("items");
                while (layout.screens.size() <= s) {
                    layout.screens.add(new ArrayList<Item>());
                }
                for (int i = 0; items != null && i < items.length(); i++) {
                    JSONObject one = items.getJSONObject(i);
                    String kind = one.optString("kind");
                    int x = Math.max(0, one.optInt("x"));
                    int y = Math.max(0, one.optInt("y"));
                    Item item;
                    if ("app".equals(kind)) {
                        item = new Item(Item.APP, x, y);
                        item.component = one.optString("component");
                    } else if ("folder".equals(kind)) {
                        item = new Item(Item.FOLDER, x, y);
                        item.name = one.optString("name", "");
                        JSONArray apps = one.optJSONArray("apps");
                        for (int a = 0; apps != null && a < apps.length(); a++) {
                            item.apps.add(apps.getString(a));
                        }
                    } else if ("widget".equals(kind)) {
                        item = new Item(Item.WIDGET, x, y);
                        item.w = Math.max(1, one.optInt("w", 1));
                        item.h = Math.max(1, one.optInt("h", 1));
                        item.provider = one.optString("provider");
                    } else {
                        item = new Item(Item.OTHER, x, y);
                    }
                    layout.add(s, item);
                }
            }
            layout.columns = Math.max(layout.columns, all.optInt("columns", 0));
            layout.rows = Math.max(layout.rows, all.optInt("rows", 0));
            return layout;
        } catch (JSONException broken) {
            return null;
        }
    }

    // ------------------------------------------------------------ databases

    private static Layout open(File file) {
        SQLiteDatabase db;
        try {
            db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (RuntimeException notOne) {
            return null;
        }
        try {
            if (has(db, "HomeItem") && has(db, "Launchable") && has(db, "Action")) {
                return items(db);
            }
            if (has(db, "favorites")) {
                return favourites(db);
            }
            return null;
        } catch (RuntimeException misread) {
            return null;
        } finally {
            db.close();
        }
    }

    private static boolean has(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            new String[] {table})) {
            return c.moveToFirst();
        }
    }

    private static boolean column(SQLiteDatabase db, String table, String name) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int at = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (name.equals(c.getString(at))) {
                    return true;
                }
            }
            return false;
        }
    }

    /** A place from a stored number that may carry a fraction: a place rounds, a span never shrinks. */
    private static int place(double value) {
        return Math.max(0, (int) Math.round(value));
    }

    private static int span(double value) {
        return Math.max(1, (int) Math.ceil(value - 0.05));
    }

    private static final int OPENS_APP = 7;
    private static final int OPENS_FOLDER = 9;
    private static final int APP_WIDGET = 1;

    /** Home items, launchables and their actions. */
    private static Layout items(SQLiteDatabase db) {
        long config = 0;
        try (Cursor most = db.rawQuery("SELECT configId, COUNT(*) AS n FROM HomeItem GROUP BY configId "
            + "ORDER BY n DESC LIMIT 1", null)) {
            if (most.moveToFirst()) {
                config = most.getLong(0);
            }
        }
        Layout layout = new Layout();
        try (Cursor c = db.rawQuery("SELECT idLaunchable, idWidget, screen, cellX, cellY, spanX, spanY "
            + "FROM HomeItem WHERE configId=? ORDER BY screen, cellY, cellX", new String[] {String.valueOf(config)})) {
            while (c.moveToNext()) {
                int screen = Math.max(0, c.getInt(2));
                int x = place(c.getDouble(3));
                int y = place(c.getDouble(4));
                Item item;
                if (!c.isNull(1)) {
                    item = widget(db, c.getLong(1), x, y);
                    if (item != null) {
                        item.w = span(c.getDouble(5));
                        item.h = span(c.getDouble(6));
                    }
                } else if (!c.isNull(0)) {
                    item = launchable(db, c.getLong(0), x, y);
                } else {
                    item = null;
                }
                if (item != null) {
                    layout.add(screen, item);
                }
            }
        }
        return layout.screens.isEmpty() ? null : layout;
    }

    private static Item widget(SQLiteDatabase db, long id, int x, int y) {
        try (Cursor c = db.rawQuery("SELECT type, provider FROM Widget WHERE id=?", new String[] {String.valueOf(id)})) {
            if (!c.moveToFirst() || c.getInt(0) != APP_WIDGET || c.isNull(1)) {
                return new Item(Item.OTHER, x, y);
            }
            Item item = new Item(Item.WIDGET, x, y);
            item.provider = c.getString(1);
            return item;
        }
    }

    private static Item launchable(SQLiteDatabase db, long id, int x, int y) {
        int type;
        String uri;
        String label;
        try (Cursor c = db.rawQuery("SELECT type, intentUri, label FROM Action WHERE idLaunchable=? AND actionId=1",
            new String[] {String.valueOf(id)})) {
            if (!c.moveToFirst()) {
                return null;
            }
            type = c.getInt(0);
            uri = c.getString(1);
            label = c.isNull(2) ? "" : c.getString(2);
        }
        if (type == OPENS_APP) {
            String door = door(uri);
            if (door == null) {
                return new Item(Item.OTHER, x, y);
            }
            Item item = new Item(Item.APP, x, y);
            item.component = door;
            return item;
        }
        if (type == OPENS_FOLDER) {
            Item folder = new Item(Item.FOLDER, x, y);
            folder.name = label;
            try (Cursor kids = db.rawQuery("SELECT id FROM Launchable WHERE idParentFolderLaunchable=? "
                + "ORDER BY position", new String[] {String.valueOf(id)})) {
                while (kids.moveToNext()) {
                    Item kid = launchable(db, kids.getLong(0), 0, 0);
                    if (kid != null && kid.kind == Item.APP) {
                        folder.apps.add(kid.component);
                    }
                }
            }
            return folder;
        }
        return new Item(Item.OTHER, x, y);
    }

    private static final int CONTAINER_SCREENS = -100;
    private static final int KIND_APP = 0;
    private static final int KIND_FOLDER = 2;
    private static final int KIND_WIDGET = 4;
    private static final int KIND_CUSTOM_WIDGET = 5;

    /** One table of favourites, with containers. */
    private static Layout favourites(SQLiteDatabase db) {
        boolean ranked = column(db, "favorites", "rank");
        boolean providers = column(db, "favorites", "appWidgetProvider");
        boolean titled = column(db, "favorites", "title");
        List<Long> order = new ArrayList<>();
        try (Cursor d = db.rawQuery("SELECT DISTINCT screen FROM favorites WHERE container=? ORDER BY screen",
            new String[] {String.valueOf(CONTAINER_SCREENS)})) {
            while (d.moveToNext()) {
                order.add(d.getLong(0));
            }
        }
        if (order.isEmpty()) {
            return null;
        }
        Layout layout = new Layout();
        for (int i = 0; i < order.size(); i++) {
            layout.screens.add(new ArrayList<Item>());
        }
        try (Cursor c = db.rawQuery("SELECT _id, intent, screen, cellX, cellY, spanX, spanY, itemType, "
            + (providers ? "appWidgetProvider" : "NULL") + ", " + (titled ? "title" : "NULL")
            + " FROM favorites WHERE container=?", new String[] {String.valueOf(CONTAINER_SCREENS)})) {
            while (c.moveToNext()) {
                int x = place(c.getDouble(3));
                int y = place(c.getDouble(4));
                int kind = c.getInt(7);
                Item item;
                if (kind == KIND_APP) {
                    String door = door(c.getString(1));
                    item = new Item(door == null ? Item.OTHER : Item.APP, x, y);
                    item.component = door;
                } else if (kind == KIND_FOLDER) {
                    item = new Item(Item.FOLDER, x, y);
                    item.name = c.isNull(9) ? "" : c.getString(9);
                    try (Cursor kids = db.rawQuery("SELECT intent FROM favorites WHERE container=? ORDER BY "
                        + (ranked ? "rank" : "cellY, cellX"), new String[] {String.valueOf(c.getLong(0))})) {
                        while (kids.moveToNext()) {
                            String door = door(kids.getString(0));
                            if (door != null) {
                                item.apps.add(door);
                            }
                        }
                    }
                } else if ((kind == KIND_WIDGET || kind == KIND_CUSTOM_WIDGET) && providers && !c.isNull(8)) {
                    item = new Item(Item.WIDGET, x, y);
                    item.w = span(c.getDouble(5));
                    item.h = span(c.getDouble(6));
                    item.provider = c.getString(8);
                } else {
                    item = new Item(Item.OTHER, x, y);
                }
                layout.add(order.indexOf(c.getLong(2)), item);
            }
        }
        return layout;
    }

    /** The application an intent opens, as a component; or none if it opens something else. */
    private static String door(String uri) {
        if (uri == null) {
            return null;
        }
        try {
            Intent intent = Intent.parseUri(uri, 0);
            ComponentName name = intent.getComponent();
            return name == null ? null : name.flattenToString();
        } catch (java.net.URISyntaxException | RuntimeException broken) {
            return null;
        }
    }

    private static void wipe(File dir) {
        File[] inside = dir.listFiles();
        if (inside != null) {
            for (File one : inside) {
                one.delete();
            }
        }
        dir.delete();
    }

    // ------------------------------------------------------------ bringing in

    /** What was brought in, and what stayed behind. */
    static final class Report {
        int apps;
        int folders;
        int widgets;
        int missing;
        int others;
        /** How many of the other home screen's looks were taken over: names, dock, outline. */
        int look;
        /** Whether the owner's widget clock was stood in for by this home screen's own. */
        boolean clock;
        /** The pack of icons the other home screen read, not on this phone; or none. */
        String packMissing;
        final List<String> unmade = new ArrayList<>();
    }

    /**
     * The other set-out made this home screen's own: its grid, its screens
     * and home, every application at its place and every folder with what
     * it holds. A widget is made at once if the phone allows it without
     * asking; otherwise its place stays free and its name is told. What was
     * here before is first copied aside, so the restore can be undone.
     */
    static Report bringIn(Context context, Layout layout, android.appwidget.AppWidgetHost host) {
        Copy.aside(context);
        Report report = new Report();
        android.content.pm.PackageManager pm = context.getPackageManager();
        android.appwidget.AppWidgetManager widgets = android.appwidget.AppWidgetManager.getInstance(context);
        int columns = Math.max(3, Math.min(7, layout.columns));
        int rows = Math.max(3, Math.min(12, layout.rows));
        List<Keep.Spot> spots = new ArrayList<>();
        Map<String, Boolean> known = new HashMap<>();
        boolean clocked = false;
        for (int s = 0; s < layout.screens.size(); s++) {
            for (Item item : layout.screens.get(s)) {
                if (item.x >= columns || item.y >= rows) {
                    report.others++;
                    continue;
                }
                if (item.kind == Item.APP) {
                    if (installed(context, item.component, known)) {
                        spots.add(new Keep.Spot(ComponentName.unflattenFromString(item.component).flattenToString(), s, item.x, item.y));
                        report.apps++;
                    } else {
                        report.missing++;
                    }
                } else if (item.kind == Item.FOLDER) {
                    List<String> inside = new ArrayList<>();
                    for (String app : item.apps) {
                        if (installed(context, app, known)) {
                            inside.add(app);
                        } else {
                            report.missing++;
                        }
                    }
                    if (inside.isEmpty()) {
                        continue;
                    }
                    int id = Keep.newFolder(context, item.name.isEmpty() ? "Folder" : item.name);
                    for (String app : inside) {
                        Keep.folderAdd(context, id, ComponentName.unflattenFromString(app).flattenToString());
                    }
                    spots.add(new Keep.Spot(Keep.FOLDER_THING + id, s, item.x, item.y));
                    report.folders++;
                } else if (item.kind == Item.WIDGET && !clocked && item.provider != null
                    && item.provider.startsWith(Meno.WIDGET_PACKAGE + "/")) {
                    /* The owner's own widget clock: this home screen's clock in its
                       face stands in its place and size, and needs no leave. */
                    int w = Math.min(item.w, columns - item.x);
                    int h = Math.min(item.h, rows - item.y);
                    spots.add(new Keep.Spot(Keep.CLOCK_THING + ":" + w + ":" + h, s, item.x, item.y));
                    clocked = true;
                    report.clock = true;
                } else if (item.kind == Item.WIDGET) {
                    ComponentName provider = ComponentName.unflattenFromString(item.provider);
                    int id = provider == null ? 0 : host.allocateAppWidgetId();
                    boolean made = false;
                    try {
                        made = provider != null && widgets.bindAppWidgetIdIfAllowed(id, provider);
                    } catch (RuntimeException refused) {
                        made = false;
                    }
                    if (made) {
                        int w = Math.min(item.w, columns - item.x);
                        int h = Math.min(item.h, rows - item.y);
                        spots.add(new Keep.Spot("#widget:" + id + ":" + w + ":" + h, s, item.x, item.y));
                        report.widgets++;
                    } else {
                        if (id != 0) {
                            host.deleteAppWidgetId(id);
                        }
                        report.unmade.add(label(pm, provider));
                    }
                } else {
                    report.others++;
                }
            }
        }
        Keep.lay(context, spots);
        Keep.saveNumber(context, Keep.DESK_GRID, rows >= 10 ? columns * 100 + rows : columns * 10 + rows);
        Keep.whole(context);
        Keep.saveNumber(context, Keep.CELL_SHAPE, Keep.SHAPE_SCREEN);
        int count = Math.max(1, layout.screens.size());
        Keep.saveScreens(context, count);
        Keep.saveHome(context, layout.home >= 0 && layout.home < count ? layout.home : Math.min(1, count - 1));
        /* How it looked, where the other home screen said. */
        if (layout.names != null) {
            Keep.saveFlag(context, Keep.NAMES_SCREENS, layout.names);
            report.look++;
        }
        if (layout.dock != null) {
            Keep.saveFlag(context, Keep.DOCK, layout.dock);
            report.look++;
        }
        if (layout.tile != null && Math.abs(layout.tile - 1f) > 0.04f) {
            /* An outline wider or taller than square: the tile, in its proportion. */
            Keep.saveShape(context, Shape.PAPER);
            Keep.saveNumber(context, Keep.TILE_ASPECT, Math.max(70, Math.min(135, Math.round(layout.tile * 100f))));
            report.look++;
        }
        if (layout.dots != null) {
            Keep.saveFlag(context, Keep.DOTS, layout.dots);
            report.look++;
        }
        if (layout.endless != null) {
            Keep.saveFlag(context, Keep.DESK_ENDLESS, layout.endless);
            report.look++;
        }
        if (layout.edgeless != null) {
            Keep.saveNumber(context, Keep.EDGES, layout.edgeless ? Keep.EDGES_ALL : Keep.EDGES_MARGINS);
            report.look++;
        }
        if (layout.listPages != null) {
            Keep.saveView(context, layout.listPages ? Keep.PAGES : Keep.LINES);
            report.look++;
        }
        if (layout.listColumns != null || layout.listRows != null) {
            int was = Keep.number(context, Keep.LIST_GRID, 45);
            int c = Math.max(3, Math.min(7, layout.listColumns != null ? layout.listColumns : Keep.columns(was)));
            int r = Math.max(3, Math.min(12, layout.listRows != null ? layout.listRows : Keep.rows(was)));
            Keep.saveNumber(context, Keep.LIST_GRID, r >= 10 ? c * 100 + r : c * 10 + r);
            report.look++;
        }
        if (layout.listEndless != null) {
            Keep.saveFlag(context, Keep.LIST_ENDLESS, layout.listEndless);
            report.look++;
        }
        if (layout.listLines != null) {
            Keep.saveNumber(context, Keep.NAME_LINES_LIST, layout.listLines >= 2 ? 2 : 1);
            report.look++;
        }
        if (layout.listNames != null) {
            Keep.saveFlag(context, Keep.NAMES_LIST, layout.listNames);
            report.look++;
        }
        /* The other home screen's set-out has its own clock, if any, among
           its widgets: this one's own clock stands only where the owner's
           widget clock stood, in that face; otherwise it is put away, and
           the shelf brings it back. */
        Keep.saveFlag(context, Keep.CLOCK, clocked);
        if (clocked) {
            Keep.saveNumber(context, Keep.CLOCK_FACE, Home.FACE_MENO);
        }
        /* Its pack of icons, if it read one and the pack is on this phone. */
        if (layout.pack != null && !layout.pack.isEmpty()) {
            if (Pack.installed(context).containsKey(layout.pack)) {
                Keep.saveWord(context, Keep.ICON_PACK, layout.pack);
                report.look++;
            } else {
                report.packMissing = layout.pack;
            }
        }
        return report;
    }

    /** Whether an application's front door is on this phone, as the home screen sees the phone's apps. */
    private static boolean installed(Context context, String component, Map<String, Boolean> known) {
        ComponentName name = component == null ? null : ComponentName.unflattenFromString(component);
        if (name == null) {
            return false;
        }
        Boolean was = known.get(name.flattenToString());
        if (was != null) {
            return was;
        }
        boolean is = false;
        try {
            android.content.pm.LauncherApps apps = (android.content.pm.LauncherApps)
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            for (android.content.pm.LauncherActivityInfo door : apps.getActivityList(name.getPackageName(),
                android.os.Process.myUserHandle())) {
                if (door.getComponentName().equals(name)) {
                    is = true;
                }
            }
        } catch (RuntimeException gone) {
            is = false;
        }
        known.put(name.flattenToString(), is);
        return is;
    }

    private static String label(android.content.pm.PackageManager pm, ComponentName provider) {
        if (provider == null) {
            return "?";
        }
        try {
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(provider.getPackageName(), 0)));
        } catch (android.content.pm.PackageManager.NameNotFoundException gone) {
            return provider.getPackageName();
        }
    }
}
