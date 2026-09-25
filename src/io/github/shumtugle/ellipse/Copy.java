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
            save(context, new File(dir(context), BEFORE_RESTORE), write(context));
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
        return true;
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
