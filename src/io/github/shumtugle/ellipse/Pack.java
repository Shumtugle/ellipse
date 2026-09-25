package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A pack of icons, as home screens have long read them: an application of
 * its own whose list names, for each application it knows, a picture to
 * stand for its icon; and, for every other application, a ground to lay
 * the icon on, a mask to cut it with, a gloss to lay over it, and how
 * small the icon is drawn within them.
 *
 * An icon the pack gives, or makes, is final: the home screen's outline
 * and rim do not cut it again. An application the pack neither knows nor
 * masks keeps its own icon, cut as every icon is.
 */
final class Pack {

    /** The calls a pack answers to, as packs have long been found. */
    private static final String[] CALLS = {"org.adw.launcher.THEMES", "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme", "com.fede.launcher.THEME_ICONPACK", "com.anddoes.launcher.THEME"};

    /** An icon a pack gave or made: final, not to be cut again. */
    static final class Given extends BitmapDrawable {
        Given(Resources res, Bitmap bitmap) {
            super(res, bitmap);
        }
    }

    private static String chosen = "";
    private static Resources res;
    private static final Map<String, String> pictures = new HashMap<>();
    private static final List<String> grounds = new ArrayList<>();
    private static String mask;
    private static String gloss;
    private static float scale = 1f;
    private static final Map<String, Drawable> made = new HashMap<>();

    private Pack() {
    }

    /** The packs on the phone: their package and their name, by name. */
    static Map<String, String> installed(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, String> found = new java.util.TreeMap<>();
        for (String call : CALLS) {
            List<ResolveInfo> answers;
            try {
                answers = pm.queryIntentActivities(new Intent(call), 0);
            } catch (RuntimeException none) {
                continue;
            }
            for (ResolveInfo one : answers) {
                String pkg = one.activityInfo.packageName;
                found.put(pkg, String.valueOf(one.loadLabel(pm)));
            }
        }
        Map<String, String> byName = new LinkedHashMap<>();
        List<Map.Entry<String, String>> all = new ArrayList<>(found.entrySet());
        java.util.Collections.sort(all, new java.util.Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) {
                return a.getValue().compareToIgnoreCase(b.getValue());
            }
        });
        for (Map.Entry<String, String> one : all) {
            byName.put(one.getKey(), one.getValue());
        }
        return byName;
    }

    /** The pack chosen read, once, whenever the choice changes; none chosen, none read. */
    static void read(Context context) {
        String now = Keep.word(context, Keep.ICON_PACK);
        now = now == null ? "" : now;
        if (now.equals(chosen)) {
            return;
        }
        chosen = now;
        res = null;
        pictures.clear();
        grounds.clear();
        mask = null;
        gloss = null;
        scale = 1f;
        made.clear();
        if (now.isEmpty()) {
            return;
        }
        try {
            res = context.getPackageManager().getResourcesForApplication(now);
        } catch (PackageManager.NameNotFoundException gone) {
            res = null;
            return;
        }
        try {
            XmlPullParser list = null;
            int id = res.getIdentifier("appfilter", "xml", now);
            InputStream words = null;
            if (id != 0) {
                list = res.getXml(id);
            } else {
                words = res.getAssets().open("appfilter.xml");
                list = Xml.newPullParser();
                list.setInput(words, "UTF-8");
            }
            try {
                parse(list);
            } finally {
                if (words != null) {
                    words.close();
                }
            }
        } catch (Exception unread) {
            pictures.clear();
        }
    }

    private static void parse(XmlPullParser list) throws Exception {
        int at = list.getEventType();
        while (at != XmlPullParser.END_DOCUMENT) {
            if (at == XmlPullParser.START_TAG) {
                String tag = list.getName();
                if ("item".equals(tag)) {
                    String component = list.getAttributeValue(null, "component");
                    String picture = list.getAttributeValue(null, "drawable");
                    ComponentName name = component(component);
                    if (name != null && picture != null && !pictures.containsKey(name.flattenToString())) {
                        pictures.put(name.flattenToString(), picture);
                    }
                } else if ("iconback".equals(tag)) {
                    for (int i = 0; i < list.getAttributeCount(); i++) {
                        grounds.add(list.getAttributeValue(i));
                    }
                } else if ("iconmask".equals(tag)) {
                    mask = list.getAttributeValue(null, "img1");
                } else if ("iconupon".equals(tag)) {
                    gloss = list.getAttributeValue(null, "img1");
                } else if ("scale".equals(tag)) {
                    try {
                        scale = Float.parseFloat(list.getAttributeValue(null, "factor"));
                    } catch (RuntimeException broken) {
                        scale = 1f;
                    }
                }
            }
            at = list.next();
        }
    }

    /** "ComponentInfo{package/class}" as a component; or none. */
    private static ComponentName component(String said) {
        if (said == null) {
            return null;
        }
        int open = said.indexOf('{');
        int close = said.indexOf('}');
        String inside = open >= 0 && close > open ? said.substring(open + 1, close) : said;
        return ComponentName.unflattenFromString(inside);
    }

    /** Whether a pack is chosen and read. */
    static boolean active() {
        return res != null;
    }

    /**
     * The icon of an application as the pack has it: its own picture, if
     * the pack knows the application; else its icon laid on the pack's
     * ground, cut by its mask and glossed, if the pack has those; else
     * the icon as it was.
     */
    static Drawable icon(ComponentName name, Drawable own) {
        if (res == null || name == null) {
            return own;
        }
        String key = name.flattenToString();
        Drawable done = made.get(key);
        if (done != null) {
            return done;
        }
        String picture = pictures.get(key);
        Drawable given = picture == null ? null : picture(picture);
        if (given != null) {
            done = new Given(res, bitmap(given, 0));
        } else if (!grounds.isEmpty() || mask != null || gloss != null) {
            done = masked(key, own);
        } else {
            return own;
        }
        made.put(key, done);
        return done;
    }

    private static Drawable picture(String named) {
        int id = res.getIdentifier(named, "drawable", chosen);
        if (id == 0) {
            id = res.getIdentifier(named, "mipmap", chosen);
        }
        if (id == 0) {
            return null;
        }
        try {
            return res.getDrawableForDensity(id, android.util.DisplayMetrics.DENSITY_XXXHIGH, null);
        } catch (RuntimeException gone) {
            return null;
        }
    }

    private static Bitmap bitmap(Drawable drawable, int side) {
        int w = side > 0 ? side : Math.max(1, Math.min(512, drawable.getIntrinsicWidth()));
        int h = side > 0 ? side : Math.max(1, Math.min(512, drawable.getIntrinsicHeight()));
        if (w <= 1 || h <= 1) {
            w = 192;
            h = 192;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return out;
    }

    /** An icon laid on the pack's ground, cut by its mask and glossed over, as the pack draws its own. */
    private static Drawable masked(String key, Drawable own) {
        Drawable ground = grounds.isEmpty() ? null
            : picture(grounds.get(Math.abs(key.hashCode()) % grounds.size()));
        Drawable cut = mask == null ? null : picture(mask);
        Drawable over = gloss == null ? null : picture(gloss);
        int side = 192;
        if (ground != null && ground.getIntrinsicWidth() > 1) {
            side = Math.min(512, ground.getIntrinsicWidth());
        }
        Bitmap out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        if (ground != null) {
            ground.setBounds(0, 0, side, side);
            ground.draw(canvas);
        }
        Bitmap picture = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        Canvas into = new Canvas(picture);
        int inset = Math.round(side * (1f - scale) / 2f);
        own.setBounds(inset, inset, side - inset, side - inset);
        own.draw(into);
        if (cut != null) {
            /* The mask's solid parts take the picture away, as packs mean it. */
            Paint away = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            away.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            into.drawBitmap(bitmap(cut, side), 0f, 0f, away);
        }
        canvas.drawBitmap(picture, 0f, 0f, new Paint(Paint.FILTER_BITMAP_FLAG));
        if (over != null) {
            over.setBounds(0, 0, side, side);
            over.draw(canvas);
        }
        return new Given(res, out);
    }
}
