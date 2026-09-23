package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The applications the screen shows, found the way a first start finds
 * them: not a list chosen by anyone, but the ones the phone itself keeps
 * for each everyday role.
 */
final class Apps {

    /** One application with a front door, ready to be drawn and opened. */
    static final class Door {
        final ComponentName name;
        final UserHandle user;
        final CharSequence label;
        private final LauncherActivityInfo info;
        private final int density;
        private Drawable icon;

        Door(LauncherActivityInfo info, int density) {
            this.info = info;
            this.density = density;
            name = info.getComponentName();
            user = info.getUser();
            CharSequence named = info.getLabel();
            label = named == null ? "" : named;
        }

        /** Drawn once, when first asked for: a long list is not painted all at once. */
        Drawable icon() {
            if (icon == null) {
                icon = info.getIcon(density);
            }
            return icon;
        }
    }

    private final Context context;
    private final PackageManager manager;
    /** Every package with a front door, by its name, holding the first door. */
    private final Map<String, LauncherActivityInfo> doors = new HashMap<>();
    /** Every front door, one per entry, whatever package it belongs to. */
    private final List<LauncherActivityInfo> every = new ArrayList<>();

    Apps(Context context) {
        this.context = context;
        manager = context.getPackageManager();
        LauncherApps apps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserHandle me = Process.myUserHandle();
        List<LauncherActivityInfo> all = apps.getActivityList(null, me);
        for (LauncherActivityInfo info : all) {
            String owner = info.getComponentName().getPackageName();
            if (owner.equals(context.getPackageName())) {
                continue;
            }
            every.add(info);
            if (!doors.containsKey(owner)) {
                doors.put(owner, info);
            }
        }
    }

    /** The door of one component, or null when it is gone. */
    Door door(ComponentName name) {
        int density = context.getResources().getDisplayMetrics().densityDpi;
        for (LauncherActivityInfo info : every) {
            if (info.getComponentName().equals(name)) {
                return new Door(info, density);
            }
        }
        return null;
    }

    /**
     * Every application with a front door, by name, the way the reader's
     * own language orders its alphabet.
     */
    List<Door> all() {
        int density = context.getResources().getDisplayMetrics().densityDpi;
        List<Door> list = new ArrayList<>();
        for (LauncherActivityInfo info : every) {
            list.add(new Door(info, density));
        }
        final Collator order = Collator.getInstance();
        order.setStrength(Collator.SECONDARY);
        Collections.sort(list, new Comparator<Door>() {
            public int compare(Door a, Door b) {
                return order.compare(a.label.toString().trim(), b.label.toString().trim());
            }
        });
        return list;
    }

    /**
     * The application that answers a role, or null. The phone's own choice
     * wins; with no choice made, a system application that answers is taken
     * before one installed later, and one already standing elsewhere on the
     * screen is never taken twice.
     */
    Door role(Intent probe, Set<String> taken) {
        String owner = null;
        ResolveInfo chosen = manager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (chosen != null && chosen.activityInfo != null) {
            String candidate = chosen.activityInfo.packageName;
            if (doors.containsKey(candidate) && !taken.contains(candidate)) {
                owner = candidate;
            }
        }
        if (owner == null) {
            List<ResolveInfo> answering = manager.queryIntentActivities(probe, 0);
            String fallback = null;
            for (ResolveInfo each : answering) {
                if (each.activityInfo == null) {
                    continue;
                }
                String candidate = each.activityInfo.packageName;
                if (!doors.containsKey(candidate) || taken.contains(candidate)) {
                    continue;
                }
                boolean system = (each.activityInfo.applicationInfo.flags
                    & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (system) {
                    owner = candidate;
                    break;
                }
                if (fallback == null) {
                    fallback = candidate;
                }
            }
            if (owner == null) {
                owner = fallback;
            }
        }
        if (owner == null) {
            return null;
        }
        taken.add(owner);
        return new Door(doors.get(owner), context.getResources().getDisplayMetrics().densityDpi);
    }
}
