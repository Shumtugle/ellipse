package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.SharedPreferences;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Copies of the home screen: everything kept — the set-out of every
 * screen, the dock, the folders, the clock and its rings, every setting —
 * written as one file of words, and read back whole.
 *
 * Three kinds: a copy the owner makes into a file of his choosing; a copy
 * made by itself of what was there just before a copy was brought back,
 * so a restore can be undone; and a copy made by itself the first time a
 * new version starts, of the set-out the old one left, so a version that
 * breaks something can be stepped back from. The last five of those are
 * kept, and nothing else.
 *
 * With the settings go the few things kept as files of their own: a
 * language module brought from a file, and the pictures icons were given.
 *
 * Widgets are kept as the places they stood in; a widget belongs to the
 * phone's own list of them, so on another phone, or after the home screen
 * was installed anew, its place stays empty until it is added again.
 */
final class Copy {

    static final String KIND = "ellipse-copy";
    private static final int FORM = 1;
    private static final String STORE = "ellipse";
    private static final String DIR = "copies";
    private static final String BEFORE_RESTORE = "before-restore.json";
    private static final String UPDATE = "update-";
    private static final String SEEN = "copy_seen_version";
    private static final int KEPT = 5;

    private Copy() {
    }

    /** Everything kept, as words. */
    /** Everything, with the wallpaper too where it may be read: for a file, a sending, or the copy before a restore. */
    static String whole(Context context) throws JSONException {
        JSONObject copy = new JSONObject(write(context));
        JSONObject pictures = wallpapers(context);
        if (pictures.length() > 0) {
            copy.put("wallpapers", pictures);
        }
        return copy.toString(1);
    }

    /** Whether the wallpaper goes into copies, and whether the phone lets it be read. */
    static boolean wallpaperReadable() {
        return android.os.Build.VERSION.SDK_INT >= 30 && android.os.Environment.isExternalStorageManager();
    }

    /** The wallpapers of the home screen and the lock screen as pictures, where the phone lets them be read. */
    private static JSONObject wallpapers(Context context) throws JSONException {
        JSONObject out = new JSONObject();
        if (!Keep.flag(context, Keep.COPY_WALLPAPER, false) || !wallpaperReadable()) {
            return out;
        }
        android.app.WallpaperManager manager = android.app.WallpaperManager.getInstance(context);
        int[] which = {android.app.WallpaperManager.FLAG_SYSTEM, android.app.WallpaperManager.FLAG_LOCK};
        String[] names = {"home", "lock"};
        for (int i = 0; i < which.length; i++) {
            try (android.os.ParcelFileDescriptor file = manager.getWallpaperFile(which[i])) {
                if (file == null) {
                    continue;
                }
                try (InputStream in = new FileInputStream(file.getFileDescriptor())) {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    byte[] chunk = new byte[65536];
                    int n;
                    while ((n = in.read(chunk)) > 0) {
                        bytes.write(chunk, 0, n);
                    }
                    out.put(names[i], android.util.Base64.encodeToString(bytes.toByteArray(),
                        android.util.Base64.NO_WRAP));
                }
            } catch (IOException | RuntimeException unread) {
                // That one is left out.
            }
        }
        return out;
    }

    /** The wallpapers a copy kept, set again: the home screen's, and the lock screen's if it was its own. */
    private static void rewall(Context context, JSONObject kept) {
        android.app.WallpaperManager manager = android.app.WallpaperManager.getInstance(context);
        String home = kept.optString("home", "");
        String lock = kept.optString("lock", "");
        try {
            if (!home.isEmpty()) {
                manager.setStream(new java.io.ByteArrayInputStream(android.util.Base64.decode(home,
                    android.util.Base64.DEFAULT)), null, true, lock.isEmpty()
                    ? android.app.WallpaperManager.FLAG_SYSTEM | android.app.WallpaperManager.FLAG_LOCK
                    : android.app.WallpaperManager.FLAG_SYSTEM);
            }
            if (!lock.isEmpty()) {
                manager.setStream(new java.io.ByteArrayInputStream(android.util.Base64.decode(lock,
                    android.util.Base64.DEFAULT)), null, true, android.app.WallpaperManager.FLAG_LOCK);
            }
        } catch (IOException | RuntimeException refused) {
            // The wallpaper stays as it is.
        }
    }

    static String write(Context context) throws JSONException {
        SharedPreferences kept = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        JSONObject all = new JSONObject();
        for (Map.Entry<String, ?> one : kept.getAll().entrySet()) {
            Object value = one.getValue();
            JSONObject item = new JSONObject();
            if (value instanceof Boolean) {
                item.put("t", "b").put("v", value);
            } else if (value instanceof Integer) {
                item.put("t", "i").put("v", value);
            } else if (value instanceof Long) {
                item.put("t", "l").put("v", value);
            } else if (value instanceof Float) {
                item.put("t", "f").put("v", ((Float) value).doubleValue());
            } else if (value instanceof String) {
                item.put("t", "s").put("v", value);
            } else if (value instanceof Set) {
                JSONArray list = new JSONArray();
                for (Object word : (Set<?>) value) {
                    list.put(String.valueOf(word));
                }
                item.put("t", "set").put("v", list);
            } else {
                continue;
            }
            all.put(one.getKey(), item);
        }
        JSONObject copy = new JSONObject();
        copy.put("kind", KIND);
        copy.put("form", FORM);
        copy.put("made", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ROOT).format(new Date()));
        copy.put("version", version(context));
        copy.put("settings", all);
        copy.put("files", files(context));
        copy.put("widgets", widgets(context));
        return copy.toString(1);
    }

    /**
     * A copy read back whole: what is kept now is let go of and the copy's
     * put in its place, at once. What was there is first copied aside, so
     * it can be brought back. False, and nothing touched, if the words are
     * not a copy made here.
     */
    static boolean read(Context context, String words) {
        JSONObject all;
        try {
            JSONObject copy = new JSONObject(words);
            if (!KIND.equals(copy.optString("kind"))) {
                return false;
            }
            all = copy.getJSONObject("settings");
        } catch (JSONException broken) {
            return false;
        }
        try {
            save(context, new File(dir(context), BEFORE_RESTORE), whole(context));
        } catch (JSONException | IOException unsaved) {
            // The restore goes on; it only cannot be undone.
        }
        SharedPreferences.Editor edit = context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit();
        edit.clear();
        java.util.Iterator<String> keys = all.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject item = all.optJSONObject(key);
            if (item == null) {
                continue;
            }
            String type = item.optString("t");
            if ("b".equals(type)) {
                edit.putBoolean(key, item.optBoolean("v"));
            } else if ("i".equals(type)) {
                edit.putInt(key, item.optInt("v"));
            } else if ("l".equals(type)) {
                edit.putLong(key, item.optLong("v"));
            } else if ("f".equals(type)) {
                edit.putFloat(key, (float) item.optDouble("v"));
            } else if ("s".equals(type)) {
                edit.putString(key, item.optString("v"));
            } else if ("set".equals(type)) {
                JSONArray list = item.optJSONArray("v");
                Set<String> words2 = new HashSet<>();
                for (int i = 0; list != null && i < list.length(); i++) {
                    words2.add(list.optString(i));
                }
                edit.putStringSet(key, words2);
            }
        }
        /* What the updates saw stays as it is now, so bringing back an old
           copy does not look like a new version starting. */
        edit.putInt(SEEN, versionCode(context));
        edit.commit();
        try {
            JSONObject kept = new JSONObject(words).optJSONObject("files");
            if (kept != null) {
                unfile(context, kept);
            }
        } catch (JSONException | IOException broken) {
            // The settings are back; a file that could not be is left as it was.
        }
        Words.forget();
        try {
            JSONObject pictures = new JSONObject(words).optJSONObject("wallpapers");
            if (pictures != null) {
                rewall(context, pictures);
            }
        } catch (JSONException broken) {
            // The wallpaper stays as it is.
        }
        try {
            JSONObject whose = new JSONObject(words).optJSONObject("widgets");
            remake(context, whose == null ? new JSONObject() : whose);
        } catch (JSONException broken) {
            // The widgets stay as the copy left them.
        }
        return true;
    }

    /** What is here now copied aside, as before a restore, so what comes next can be undone. */
    static void aside(Context context) {
        try {
            save(context, new File(dir(context), BEFORE_RESTORE), whole(context));
        } catch (JSONException | IOException unsaved) {
            // What comes next cannot be undone, then.
        }
    }

    /** The widgets the last restore could not make again, by the names of their applications. */
    static final List<String> unmade = new ArrayList<>();

    /** Whose each widget on the screens is, by its number: the application and the widget it offers. */
    private static JSONObject widgets(Context context) throws JSONException {
        JSONObject out = new JSONObject();
        android.appwidget.AppWidgetManager manager = android.appwidget.AppWidgetManager.getInstance(context);
        for (Keep.Spot spot : Keep.placed(context)) {
            int id = widgetOf(spot.token);
            if (id == 0) {
                continue;
            }
            android.appwidget.AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
            if (info != null && info.provider != null) {
                out.put(String.valueOf(id), info.provider.flattenToString());
            }
        }
        return out;
    }

    /** A widget's number from its word in the set-out; nought if the word is not a widget's. */
    private static int widgetOf(String token) {
        if (!token.startsWith("#widget:")) {
            return 0;
        }
        try {
            return Integer.parseInt(token.split(":")[1]);
        } catch (RuntimeException broken) {
            return 0;
        }
    }

    /**
     * Every widget of the set-out restored that the phone no longer holds —
     * as after installing anew, or a copy from another phone — made again
     * from whose it was, where the phone lets it be made without asking;
     * its place keeps its size and frame. What cannot be made is named.
     */
    private static void remake(Context context, JSONObject whose) {
        unmade.clear();
        android.appwidget.AppWidgetManager manager = android.appwidget.AppWidgetManager.getInstance(context);
        android.appwidget.AppWidgetHost host = new android.appwidget.AppWidgetHost(context, Home.WIDGET_HOST);
        SharedPreferences kept = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = kept.edit();
        List<Keep.Spot> spots = Keep.placed(context);
        List<Keep.Spot> after = new ArrayList<>();
        boolean changed = false;
        for (Keep.Spot spot : spots) {
            int id = widgetOf(spot.token);
            if (id == 0 || manager.getAppWidgetInfo(id) != null) {
                after.add(spot);
                continue;
            }
            String said = whose.optString(String.valueOf(id), "");
            android.content.ComponentName provider = said.isEmpty() ? null
                : android.content.ComponentName.unflattenFromString(said);
            int fresh = 0;
            if (provider != null) {
                fresh = host.allocateAppWidgetId();
                boolean made;
                try {
                    made = manager.bindAppWidgetIdIfAllowed(fresh, provider);
                } catch (RuntimeException refused) {
                    made = false;
                }
                if (!made) {
                    host.deleteAppWidgetId(fresh);
                    fresh = 0;
                }
            }
            if (fresh == 0) {
                unmade.add(provider == null ? "?" : label(context, provider));
                after.add(spot);
                continue;
            }
            String[] part = spot.token.split(":");
            StringBuilder token = new StringBuilder("#widget:").append(fresh);
            for (int i = 2; i < part.length; i++) {
                token.append(':').append(part[i]);
            }
            after.add(new Keep.Spot(token.toString(), spot.screen, spot.x, spot.y));
            if (kept.contains(Keep.FRAME_OFF + id)) {
                edit.putBoolean(Keep.FRAME_OFF + fresh, kept.getBoolean(Keep.FRAME_OFF + id, false));
                edit.remove(Keep.FRAME_OFF + id);
            }
            changed = true;
        }
        edit.commit();
        if (changed) {
            Keep.lay(context, after);
        }
    }

    private static String label(Context context, android.content.ComponentName provider) {
        android.content.pm.PackageManager pm = context.getPackageManager();
        try {
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(provider.getPackageName(), 0)));
        } catch (android.content.pm.PackageManager.NameNotFoundException gone) {
            return provider.getPackageName();
        }
    }

    /** The files kept beside the settings, by their place under the application's own, as words. */
    private static JSONObject files(Context context) throws JSONException {
        JSONObject out = new JSONObject();
        File root = context.getFilesDir();
        List<File> kept = new ArrayList<>();
        kept.add(new File(root, Words.BROUGHT_FILE));
        for (String room : new String[] {"faces", "links"}) {
            File[] inside = new File(root, room).listFiles();
            if (inside != null) {
                kept.addAll(Arrays.asList(inside));
            }
        }
        for (File one : kept) {
            if (!one.isFile() || one.length() > 2L * 1024 * 1024) {
                continue;
            }
            try (InputStream in = new FileInputStream(one)) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) > 0) {
                    bytes.write(chunk, 0, n);
                }
                String where = root.toURI().relativize(one.toURI()).getPath();
                out.put(where, android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP));
            } catch (IOException unread) {
                // That one file stays out of the copy.
            }
        }
        return out;
    }

    /** The files a copy kept, written back where they were. */
    private static void unfile(Context context, JSONObject kept) throws IOException {
        File root = context.getFilesDir();
        java.util.Iterator<String> names = kept.keys();
        while (names.hasNext()) {
            String where = names.next();
            if (where.contains("..") || where.startsWith("/")) {
                continue;
            }
            File to = new File(root, where);
            File dir = to.getParentFile();
            if (dir != null && !dir.isDirectory()) {
                dir.mkdirs();
            }
            try (OutputStream out = new FileOutputStream(to)) {
                out.write(android.util.Base64.decode(kept.optString(where), android.util.Base64.DEFAULT));
            }
        }
    }

    /** Whether a restore can be undone: what was there before it is kept. */
    static boolean undoable(Context context) {
        return new File(dir(context), BEFORE_RESTORE).isFile();
    }

    /** The restore undone: what was there before it brought back. */
    static boolean undo(Context context) {
        File before = new File(dir(context), BEFORE_RESTORE);
        String words;
        try {
            words = load(before);
        } catch (IOException gone) {
            return false;
        }
        boolean done = read(context, words);
        return done;
    }

    /**
     * The first start of a new version: the set-out the old one left is
     * copied aside before anything reads it, and the oldest such copies go.
     */
    static void onUpdate(Context context) {
        SharedPreferences kept = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        int now = versionCode(context);
        int seen = kept.getInt(SEEN, -1);
        if (seen == now) {
            return;
        }
        if (!kept.getAll().isEmpty()) {
            try {
                String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date());
                /* Made now, but what it holds is the old version's: it is named for that one. */
                JSONObject copy = new JSONObject(write(context));
                copy.put("version", seen < 0 ? "the version before" : seen / 10000 + "." + (seen / 100) % 100 + "."
                    + seen % 100);
                save(context, new File(dir(context), UPDATE + Math.max(0, seen) + "-" + stamp + ".json"),
                    copy.toString(1));
            } catch (JSONException | IOException unsaved) {
                // Nothing to step back to this time.
            }
            List<File> copies = updates(context);
            for (int i = KEPT; i < copies.size(); i++) {
                copies.get(i).delete();
            }
        }
        kept.edit().putInt(SEEN, now).commit();
    }

    /** The copies made on updates, newest first. */
    static List<File> updates(Context context) {
        File[] found = dir(context).listFiles();
        List<File> copies = new ArrayList<>();
        if (found == null) {
            return copies;
        }
        for (File one : found) {
            if (one.getName().startsWith(UPDATE)) {
                copies.add(one);
            }
        }
        java.util.Collections.sort(copies, new java.util.Comparator<File>() {
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        return copies;
    }

    /** What a copy on disk says of itself: the version it was made under, and when. */
    static String[] about(File copy) {
        try {
            JSONObject read = new JSONObject(load(copy));
            return new String[] {read.optString("version", "?"), read.optString("made", "")};
        } catch (IOException | JSONException broken) {
            return new String[] {"?", ""};
        }
    }

    static String load(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return words(in);
        }
    }

    static String words(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
            out.write(chunk, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    static void put(OutputStream out, String words) throws IOException {
        out.write(words.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void save(Context context, File file, String words) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            put(out, words);
        }
    }

    private static File dir(Context context) {
        File dir = new File(context.getFilesDir(), DIR);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    static String version(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException never) {
            return "?";
        }
    }

    @SuppressWarnings("deprecation")
    static int versionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (android.content.pm.PackageManager.NameNotFoundException never) {
            return 0;
        }
    }

    /** A name for a copy made today. */
    static String name() {
        return "ellipse-" + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date()) + ".json";
    }

}
