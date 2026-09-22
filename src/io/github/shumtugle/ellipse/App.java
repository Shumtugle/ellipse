package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.DisplayMetrics;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * One thing that can be opened: an application's front door, in one of
 * the profiles of the phone's owner.
 */
final class App {

    final LauncherActivityInfo info;
    final String label;
    /** The profile's number as the system keeps it: steady across restarts, unlike the handle. */
    final long serial;
    /** Which door, and whose: the same application in a work profile is another door. */
    final String key;
    /** When the application came onto the phone, and when it last changed. */
    long installed;
    long updated;

    private App(LauncherActivityInfo info, long serial) {
        this.info = info;
        this.installed = info.getFirstInstallTime();
        this.updated = installed;
        this.label = String.valueOf(info.getLabel());
        this.serial = serial;
        this.key = info.getComponentName().flattenToShortString() + "#" + serial;
    }

    /** The key a written-down door is looked up by: the short form of its name, and the profile. */
    static String keyOf(String component, long serial) {
        ComponentName name = ComponentName.unflattenFromString(component == null ? "" : component);
        return (name == null ? String.valueOf(component) : name.flattenToShortString()) + "#" + serial;
    }

    ComponentName component() {
        return info.getComponentName();
    }

    UserHandle user() {
        return info.getUser();
    }

    String pkg() {
        return info.getComponentName().getPackageName();
    }

    /**
     * The icon as its application draws it. In the owner's own profile it
     * comes bare, so its layers survive; in another profile it comes with
     * that profile's badge, which the system paints over a flat copy.
     */
    Drawable icon(int dpi) {
        if (Process.myUserHandle().equals(info.getUser())) {
            return info.getIcon(dpi);
        }
        return info.getBadgedIcon(dpi);
    }

    /** The density to ask icons in: never coarser than the screen, never coarser than very fine. */
    static int dpi(Context context) {
        return Math.max(context.getResources().getDisplayMetrics().densityDpi,
            DisplayMetrics.DENSITY_XXHIGH);
    }

    /**
     * Every front door on the phone, in every profile, except this one's
     * own: the home screen does not list itself among the things it opens.
     *
     * The order is the reading order of the phone's language for now. It
     * is a starting point, not a rule; places set by hand come later.
     */
    static List<App> all(Context context) {
        LauncherApps doors = context.getSystemService(LauncherApps.class);
        UserManager users = context.getSystemService(UserManager.class);
        String self = context.getPackageName();
        List<App> apps = new ArrayList<App>();
        for (UserHandle user : users.getUserProfiles()) {
            long serial = users.getSerialNumberForUser(user);
            for (LauncherActivityInfo info : doors.getActivityList(null, user)) {
                if (!self.equals(info.getComponentName().getPackageName())) {
                    apps.add(new App(info, serial));
                }
            }
        }
        android.content.pm.PackageManager pm = context.getPackageManager();
        java.util.Map<String, Long> changed = new java.util.HashMap<String, Long>();
        UserHandle me = Process.myUserHandle();
        for (App app : apps) {
            if (!me.equals(app.user())) {
                continue;
            }
            Long when = changed.get(app.pkg());
            if (when == null) {
                try {
                    when = pm.getPackageInfo(app.pkg(), 0).lastUpdateTime;
                } catch (android.content.pm.PackageManager.NameNotFoundException gone) {
                    when = app.installed;
                }
                changed.put(app.pkg(), when);
            }
            app.updated = Math.max(app.installed, when);
        }
        sort(apps, Keep.BY_NAME);
        return apps;
    }

    /**
     * Orders a list by name in the phone's own alphabet, or by the day of
     * installing or of the last update, the newest first, and by name
     * where the times agree.
     */
    static void sort(List<App> apps, final int by) {
        final Collator order = Collator.getInstance();
        order.setStrength(Collator.PRIMARY);
        Collections.sort(apps, new Comparator<App>() {
            public int compare(App one, App two) {
                if (by == Keep.BY_INSTALLED && one.installed != two.installed) {
                    return one.installed > two.installed ? -1 : 1;
                }
                if (by == Keep.BY_UPDATED && one.updated != two.updated) {
                    return one.updated > two.updated ? -1 : 1;
                }
                return order.compare(one.label, two.label);
            }
        });
    }
}
