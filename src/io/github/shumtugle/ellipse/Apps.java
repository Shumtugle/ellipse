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
        final Drawable icon;

        Door(LauncherActivityInfo info, int density) {
            name = info.getComponentName();
            user = info.getUser();
            label = info.getLabel();
            icon = info.getIcon(density);
        }
    }

    private final Context context;
    private final PackageManager manager;
    /** Every package with a front door, by its name, holding the first door. */
    private final Map<String, LauncherActivityInfo> doors = new HashMap<>();

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
            if (!doors.containsKey(owner)) {
                doors.put(owner, info);
            }
        }
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
