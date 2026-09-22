package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The layout of another home screen, read from its backup.
 *
 * A backup is a zip, and somewhere inside it a database. Nothing here goes
 * by the names of files or of the applications that wrote them: every
 * database in the zip is opened, and the one whose tables have a known
 * shape is read. Two shapes are known.
 *
 * In the first, things on the screens are home items, each pointing to a
 * launchable or to a widget; what a launchable does is its first action,
 * and a folder is a launchable other launchables name as their parent.
 *
 * In the second, the one the platform's own home screen keeps and many
 * grew from, everything is a row of one table of favourites, and a row's
 * container says where it lies: on the screens, in the row along the
 * bottom, or in a folder, by the folder's number.
 *
 * Applications, folders with their applications, shortcuts and the places
 * of widgets are brought over, each at its cell. The home screen's own
 * gadgets and the row along the bottom are left behind. Widgets come as
 * places, to be pressed once to be shown.
 */
final class Foreign {

    private Foreign() {
    }

    /** A backup's layout, or none if nothing in it has a known shape. */
    static Layout read(Context context, byte[] zip) throws IOException {
        File dir = new File(context.getCacheDir(), "foreign");
        wipe(dir);
        if (!dir.mkdirs() && !dir.isDirectory()) {
            return null;
        }
        List<File> bases = new ArrayList<File>();
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip));
        try {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                String base = name.substring(name.lastIndexOf('/') + 1);
                if (entry.isDirectory() || base.length() == 0 || base.contains("..")) {
                    continue;
                }
                boolean keeps = base.endsWith(".db") || base.endsWith("-wal") || base.endsWith("-shm")
                    || base.endsWith("-journal") || !base.contains(".");
                if (!keeps) {
                    continue;
                }
                File out = new File(dir, base);
                OutputStream write = new FileOutputStream(out);
                try {
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        write.write(buffer, 0, n);
                    }
                } finally {
                    write.close();
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
                    return found;
                }
            }
            return null;
        } finally {
            wipe(dir);
        }
    }

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
        Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            new String[] {table});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    private static boolean column(SQLiteDatabase db, String table, String name) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        try {
            int at = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (name.equals(c.getString(at))) {
                    return true;
                }
            }
            return false;
        } finally {
            c.close();
        }
    }

    // ------------------------------------------------------------ home items

    private static final int OPENS_APP = 7;
    private static final int OPENS_SHORTCUT = 13;
    private static final int OPENS_FOLDER = 9;
    private static final int APP_WIDGET = 1;

    /** The first shape: home items, launchables and their actions. */
    private static Layout items(SQLiteDatabase db) {
        long config = 0;
        Cursor most = db.rawQuery("SELECT configId, COUNT(*) AS n FROM HomeItem GROUP BY configId "
            + "ORDER BY n DESC LIMIT 1", null);
        try {
            if (most.moveToFirst()) {
                config = most.getLong(0);
            }
        } finally {
            most.close();
        }
        Layout layout = new Layout();
        int columns = 1;
        int rows = 1;
        Map<Integer, Layout.Screen> screens = new HashMap<Integer, Layout.Screen>();
        int last = -1;
        Cursor c = db.rawQuery("SELECT idLaunchable, idWidget, screen, cellX, cellY, spanX, spanY "
            + "FROM HomeItem WHERE configId=? ORDER BY screen, cellY, cellX",
            new String[] {String.valueOf(config)});
        try {
            while (c.moveToNext()) {
                int screen = c.getInt(2);
                int x = Math.max(0, Math.round(c.getFloat(3)));
                int y = Math.max(0, Math.round(c.getFloat(4)));
                int w = Math.max(1, Math.round(c.getFloat(5)));
                int h = Math.max(1, Math.round(c.getFloat(6)));
                Layout.Item item = null;
                if (!c.isNull(1)) {
                    item = widget(db, c.getLong(1), x, y, w, h);
                } else if (!c.isNull(0)) {
                    item = launchable(db, c.getLong(0), x, y);
                }
                if (item == null) {
                    continue;
                }
                columns = Math.max(columns, item.x + item.w);
                rows = Math.max(rows, item.y + item.h);
                last = Math.max(last, screen);
                Layout.Screen on = screens.get(screen);
                if (on == null) {
                    on = new Layout.Screen();
                    screens.put(screen, on);
                }
                on.items.add(item);
            }
        } finally {
            c.close();
        }
        if (last < 0) {
            return null;
        }
        for (int i = 0; i <= last; i++) {
            Layout.Screen on = screens.get(i);
            layout.screens.add(on == null ? new Layout.Screen() : on);
        }
        layout.columns = columns;
        layout.rows = rows;
        return layout;
    }

    private static Layout.Item widget(SQLiteDatabase db, long id, int x, int y, int w, int h) {
        Cursor c = db.rawQuery("SELECT type, provider FROM Widget WHERE id=?", new String[] {String.valueOf(id)});
        try {
            if (!c.moveToFirst() || c.getInt(0) != APP_WIDGET || c.isNull(1)) {
                return null;
            }
            Layout.Item item = new Layout.Item(Layout.WIDGET, x, y);
            item.w = w;
            item.h = h;
            item.provider = c.getString(1);
            return item;
        } finally {
            c.close();
        }
    }

    private static Layout.Item launchable(SQLiteDatabase db, long id, int x, int y) {
        Cursor c = db.rawQuery("SELECT type, intentUri, label, deepShortcutId FROM Action "
            + "WHERE idLaunchable=? AND actionId=1", new String[] {String.valueOf(id)});
        int type;
        String uri;
        String label;
        String shortcut;
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            type = c.getInt(0);
            uri = c.getString(1);
            label = c.isNull(2) ? "" : c.getString(2);
            shortcut = c.isNull(3) ? null : c.getString(3);
        } finally {
            c.close();
        }
        if (type == OPENS_APP) {
            String door = door(uri);
            if (door == null) {
                return null;
            }
            Layout.Item item = new Layout.Item(Layout.APP, x, y);
            item.component = door;
            return item;
        }
        if (type == OPENS_SHORTCUT) {
            String door = door(uri);
            if (door == null) {
                return null;
            }
            Layout.Item item = new Layout.Item(Layout.SHORTCUT, x, y);
            item.component = door.substring(0, door.indexOf('/'));
            item.shortcut = shortcut;
            item.label = label;
            return item;
        }
        if (type == OPENS_FOLDER) {
            Layout.Item folder = new Layout.Item(Layout.FOLDER, x, y);
            Cursor kids = db.rawQuery("SELECT id FROM Launchable WHERE idParentFolderLaunchable=? "
                + "ORDER BY position", new String[] {String.valueOf(id)});
            try {
                while (kids.moveToNext()) {
                    Layout.Item kid = launchable(db, kids.getLong(0), 0, 0);
                    if (kid != null && Layout.APP.equals(kid.kind)) {
                        folder.apps.add(kid.component);
                    }
                }
            } finally {
                kids.close();
            }
            return folder.apps.isEmpty() ? null : folder;
        }
        return null;
    }

    // ------------------------------------------------------------ favourites

    private static final int CONTAINER_SCREENS = -100;
    private static final int KIND_APP = 0;
    private static final int KIND_SHORTCUT = 1;
    private static final int KIND_FOLDER = 2;
    private static final int KIND_WIDGET = 4;
    private static final int KIND_CUSTOM_WIDGET = 5;
    private static final int KIND_DEEP_SHORTCUT = 6;

    /** The second shape: one table of favourites, with containers. */
    private static Layout favourites(SQLiteDatabase db) {
        boolean ranked = column(db, "favorites", "rank");
        boolean providers = column(db, "favorites", "appWidgetProvider");
        List<Long> order = new ArrayList<Long>();
        if (has(db, "workspaceScreens") && column(db, "workspaceScreens", "screenRank")) {
            Cursor s = db.rawQuery("SELECT _id FROM workspaceScreens ORDER BY screenRank", null);
            try {
                while (s.moveToNext()) {
                    order.add(s.getLong(0));
                }
            } finally {
                s.close();
            }
        }
        Cursor d = db.rawQuery("SELECT DISTINCT screen FROM favorites WHERE container=? ORDER BY screen",
            new String[] {String.valueOf(CONTAINER_SCREENS)});
        try {
            while (d.moveToNext()) {
                if (!order.contains(d.getLong(0))) {
                    order.add(d.getLong(0));
                }
            }
        } finally {
            d.close();
        }
        if (order.isEmpty()) {
            return null;
        }
        Layout layout = new Layout();
        for (int i = 0; i < order.size(); i++) {
            layout.screens.add(new Layout.Screen());
        }
        int columns = 1;
        int rows = 1;
        boolean titled = column(db, "favorites", "title");
        Cursor c = db.rawQuery("SELECT _id, intent, screen, cellX, cellY, spanX, spanY, itemType, "
            + (providers ? "appWidgetProvider" : "NULL") + ", " + (titled ? "title" : "NULL")
            + " FROM favorites WHERE container=?", new String[] {String.valueOf(CONTAINER_SCREENS)});
        try {
            while (c.moveToNext()) {
                int x = Math.max(0, c.getInt(3));
                int y = Math.max(0, c.getInt(4));
                int w = Math.max(1, c.getInt(5));
                int h = Math.max(1, c.getInt(6));
                int kind = c.getInt(7);
                Layout.Item item = null;
                if (kind == KIND_APP) {
                    String door = door(c.getString(1));
                    if (door != null) {
                        item = new Layout.Item(Layout.APP, x, y);
                        item.component = door;
                    }
                } else if (kind == KIND_SHORTCUT) {
                    // A shortcut the old home screen made itself names a screen
                    // inside an application; it stays that screen, not its front door.
                    String door = door(c.getString(1));
                    if (door != null) {
                        item = new Layout.Item(Layout.ACTIVITY, x, y);
                        item.component = ComponentName.unflattenFromString(door).flattenToString();
                        item.label = c.isNull(9) ? "" : c.getString(9);
                    }
                } else if (kind == KIND_DEEP_SHORTCUT) {
                    item = deep(c.getString(1), x, y);
                } else if (kind == KIND_FOLDER) {
                    item = folder(db, c.getLong(0), ranked, x, y);
                } else if ((kind == KIND_WIDGET || kind == KIND_CUSTOM_WIDGET) && providers
                    && !c.isNull(8)) {
                    item = new Layout.Item(Layout.WIDGET, x, y);
                    item.w = w;
                    item.h = h;
                    item.provider = c.getString(8);
                }
                if (item == null) {
                    continue;
                }
                columns = Math.max(columns, item.x + item.w);
                rows = Math.max(rows, item.y + item.h);
                layout.screens.get(order.indexOf(c.getLong(2))).items.add(item);
            }
        } finally {
            c.close();
        }
        layout.columns = columns;
        layout.rows = rows;
        return layout;
    }

    private static Layout.Item folder(SQLiteDatabase db, long id, boolean ranked, int x, int y) {
        Layout.Item folder = new Layout.Item(Layout.FOLDER, x, y);
        Cursor kids = db.rawQuery("SELECT intent FROM favorites WHERE container=? ORDER BY "
            + (ranked ? "rank" : "cellY, cellX"), new String[] {String.valueOf(id)});
        try {
            while (kids.moveToNext()) {
                String door = door(kids.getString(0));
                if (door != null) {
                    folder.apps.add(door);
                }
            }
        } finally {
            kids.close();
        }
        return folder.apps.isEmpty() ? null : folder;
    }

    private static Layout.Item deep(String uri, int x, int y) {
        try {
            Intent intent = Intent.parseUri(uri, 0);
            String id = intent.getStringExtra("shortcut_id");
            String pkg = intent.getPackage();
            if (pkg == null && intent.getComponent() != null) {
                pkg = intent.getComponent().getPackageName();
            }
            if (id == null || pkg == null) {
                return null;
            }
            Layout.Item item = new Layout.Item(Layout.SHORTCUT, x, y);
            item.component = pkg;
            item.shortcut = id;
            return item;
        } catch (Exception broken) {
            return null;
        }
    }

    // ------------------------------------------------------------ parts

    /** The front door an intent written as text opens, in its short form. */
    private static String door(String uri) {
        if (uri == null) {
            return null;
        }
        try {
            ComponentName name = Intent.parseUri(uri, 0).getComponent();
            return name == null ? null : name.flattenToShortString();
        } catch (Exception broken) {
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
}
