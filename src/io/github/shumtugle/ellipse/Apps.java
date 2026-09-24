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
        /** Which of the phone's profiles it lives in: nought for the owner's own, a number for a work profile. */
        final long serial;
        private final LauncherActivityInfo info;
        private final int density;
        private Drawable icon;
        /** A time to sort by, when the order asks for one. */
        long when;
        /** When it came to the phone. */
        final long installed;

        Door(LauncherActivityInfo info, int density) {
            this.info = info;
            this.density = density;
            name = info.getComponentName();
            user = info.getUser();
            serial = Apps.serialOf(user);
            installed = info.getFirstInstallTime();
            CharSequence named = info.getLabel();
            label = named == null ? "" : named;
        }

        /** Drawn once, when first asked for: a long list is not painted all at once. A work app wears its badge. */
        Drawable icon() {
            if (icon == null) {
                icon = serial == 0 ? Shape.face(info.getIcon(density)) : info.getBadgedIcon(density);
            }
            return icon;
        }

        /** The icon as the app gives it, before any cutting: for a preview of other outlines. */
        Drawable plain() {
            return info.getIcon(density);
        }

        /** How it is kept: its component, and after an at sign its profile when that is not the owner's own. */
        String token() {
            return Apps.token(name, serial);
        }
    }

    private final Context context;
    private final PackageManager manager;
    /** Every package with a front door, by its name, holding the first door. */
    private final Map<String, LauncherActivityInfo> doors = new HashMap<>();
    /** Every front door, one per entry, whatever package it belongs to. */
    private final List<LauncherActivityInfo> every = new ArrayList<>();

    private static android.os.UserManager users;

    /** The number a profile is known by: nought for the owner's own. */
    static long serialOf(UserHandle user) {
        if (user == null || user.equals(Process.myUserHandle()) || users == null) {
            return 0L;
        }
        return users.getSerialNumberForUser(user);
    }

    /** The profile a number stands for; the owner's own for nought or a number no longer known. */
    static UserHandle userOf(long serial) {
        if (serial == 0L || users == null) {
            return Process.myUserHandle();
        }
        UserHandle user = users.getUserForSerialNumber(serial);
        return user == null ? Process.myUserHandle() : user;
    }

    static String token(ComponentName name, long serial) {
        return serial == 0L ? name.flattenToString() : name.flattenToString() + "@" + serial;
    }

    /** The component a kept word names, or none if it names something else. */
    static ComponentName nameOf(String token) {
        if (token == null || token.startsWith("#")) {
            return null;
        }
        int at = token.indexOf('@');
        return ComponentName.unflattenFromString(at < 0 ? token : token.substring(0, at));
    }

    static long serialOf(String token) {
        int at = token == null ? -1 : token.indexOf('@');
        if (at < 0) {
            return 0L;
        }
        try {
            return Long.parseLong(token.substring(at + 1));
        } catch (NumberFormatException broken) {
            return 0L;
        }
    }

    Apps(Context context) {
        this.context = context;
        manager = context.getPackageManager();
        users = (android.os.UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps apps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserHandle me = Process.myUserHandle();
        /* Every profile of the phone: the owner's own first, then work. */
        List<UserHandle> profiles = new ArrayList<>();
        profiles.add(me);
        for (UserHandle other : apps.getProfiles()) {
            if (!other.equals(me)) {
                profiles.add(other);
            }
        }
        List<LauncherActivityInfo> all = new ArrayList<>();
        for (UserHandle profile : profiles) {
            try {
                all.addAll(apps.getActivityList(null, profile));
            } catch (SecurityException closed) {
                // A profile that is switched off keeps its apps to itself.
            }
        }
        for (LauncherActivityInfo info : all) {
            String owner = info.getComponentName().getPackageName();
            if (owner.equals(context.getPackageName()) && info.getUser().equals(me)) {
                continue;
            }
            if (!info.getUser().equals(me)) {
                every.add(info);
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
        return door(name, 0L);
    }

    Door door(ComponentName name, long serial) {
        int density = context.getResources().getDisplayMetrics().densityDpi;
        for (LauncherActivityInfo info : every) {
            if (info.getComponentName().equals(name) && serialOf(info.getUser()) == serial) {
                return new Door(info, density);
            }
        }
        return null;
    }

    /** The door a kept word names, or none if it is gone or names something else. */
    Door door(String token) {
        ComponentName name = nameOf(token);
        return name == null ? null : door(name, serialOf(token));
    }

    /**
     * Every application with a front door, in the order asked for: by name,
     * the way the reader's own language orders its alphabet; by when it came
     * to the phone, newest first; or by when it last changed, freshest
     * first. Equal times fall back on the name.
     */
    List<Door> all(final int order) {
        int density = context.getResources().getDisplayMetrics().densityDpi;
        List<Door> list = new ArrayList<>();
        for (LauncherActivityInfo info : every) {
            Door door = new Door(info, density);
            if (order == Keep.NEWEST) {
                door.when = info.getFirstInstallTime();
            } else if (order == Keep.UPDATED) {
                try {
                    door.when = manager.getPackageInfo(
                        info.getComponentName().getPackageName(), 0).lastUpdateTime;
                } catch (PackageManager.NameNotFoundException gone) {
                    door.when = 0L;
                }
            }
            list.add(door);
        }
        final Collator names = Collator.getInstance();
        names.setStrength(Collator.SECONDARY);
        Collections.sort(list, new Comparator<Door>() {
            public int compare(Door a, Door b) {
                if (order != Keep.BY_NAME && a.when != b.when) {
                    return a.when > b.when ? -1 : 1;
                }
                return names.compare(a.label.toString().trim(), b.label.toString().trim());
            }
        });
        return list;
    }

    /**
     * The applications signed by the same hand as the phone's store, by
     * name, leaving out those already standing somewhere. Empty when the
     * phone has no store.
     */
    List<Door> vendor(Set<String> taken) {
        List<Door> list = new ArrayList<>();
        ResolveInfo store = manager.resolveActivity(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET), 0);
        if (store == null || store.activityInfo == null) {
            return list;
        }
        String hand = store.activityInfo.packageName;
        for (Door door : all(Keep.BY_NAME)) {
            String owner = door.name.getPackageName();
            if (taken.contains(owner) || door.serial != 0L) {
                continue;
            }
            if (owner.equals(hand)
                || manager.checkSignatures(hand, owner) == PackageManager.SIGNATURE_MATCH) {
                list.add(door);
            }
        }
        return list;
    }

    /**
     * A name for such a folder, read from the applications themselves: the
     * part of their package names most of them share after the first,
     * with a capital.
     */
    static String vendorName(List<Door> doors) {
        Map<String, Integer> counted = new HashMap<>();
        String best = null;
        int most = 0;
        for (Door door : doors) {
            String[] parts = door.name.getPackageName().split("\\.");
            if (parts.length < 2) {
                continue;
            }
            String part = parts[1];
            int n = (counted.containsKey(part) ? counted.get(part) : 0) + 1;
            counted.put(part, n);
            if (n > most) {
                most = n;
                best = part;
            }
        }
        if (best == null || best.length() == 0) {
            return "Apps";
        }
        return best.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + best.substring(1);
    }

    /** The phone's own applications, by name, leaving out those already standing somewhere. */
    List<Door> system(Set<String> taken) {
        List<Door> list = new ArrayList<>();
        for (Door door : all(Keep.BY_NAME)) {
            String owner = door.name.getPackageName();
            if (taken.contains(owner) || door.serial != 0L) {
                continue;
            }
            try {
                ApplicationInfo info = manager.getApplicationInfo(owner, 0);
                if ((info.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
                    list.add(door);
                }
            } catch (PackageManager.NameNotFoundException gone) {
                // Gone since the list was read.
            }
        }
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
