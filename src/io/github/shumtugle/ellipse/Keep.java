package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * What the owner has put on the screen by hand, and where. Only that is
 * kept: the places the phone fills by role are found again every time.
 */
final class Keep {

    /** One application set down on the grid. */
    static final class Spot {
        final ComponentName name;
        final int screen;
        final int x;
        final int y;

        Spot(ComponentName name, int screen, int x, int y) {
            this.name = name;
            this.screen = screen;
            this.x = x;
            this.y = y;
        }
    }

    private static final String PLACED = "placed";
    private static final String ORDER = "order";
    private static final String VIEW = "view";
    private static final String SCREENS = "screens";
    private static final String HOME = "home";
    private static final String ROLES = "roles";
    private static final String SHAPE = "shape";
    private static final String RECENT = "recent";
    /** How many applications opened from here are remembered. */
    private static final int RECENT_KEPT = 8;

    /** How the list of every application is laid out: lines down, or pages across. */
    static final int LINES = 0;
    static final int PAGES = 1;

    /** The orders the list of every application can stand in. */
    static final int BY_NAME = 0;
    static final int NEWEST = 1;
    static final int UPDATED = 2;

    private Keep() {
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences("ellipse", Context.MODE_PRIVATE);
    }

    static int order(Context context) {
        int kept = store(context).getInt(ORDER, BY_NAME);
        return kept < BY_NAME || kept > UPDATED ? BY_NAME : kept;
    }

    static void saveOrder(Context context, int order) {
        store(context).edit().putInt(ORDER, order).apply();
    }

    static int view(Context context) {
        return store(context).getInt(VIEW, LINES) == PAGES ? PAGES : LINES;
    }

    static void saveView(Context context, int view) {
        store(context).edit().putInt(VIEW, view).apply();
    }

    /**
     * Brings what an earlier version kept into today's shape, once. A first
     * start, or a phone that never added a screen, gets three screens with
     * the middle one as home, and whatever stood on the single screen moves
     * onto that middle one.
     */
    static void settle(Context context) {
        SharedPreferences kept = store(context);
        if (kept.getInt(SHAPE, 0) >= 1) {
            return;
        }
        SharedPreferences.Editor edit = kept.edit();
        if (!kept.contains(SCREENS)) {
            StringBuilder out = new StringBuilder();
            for (Spot spot : placed(context)) {
                out.append(spot.screen + 1).append('\t').append(spot.x).append('\t')
                    .append(spot.y).append('\t').append(spot.name.flattenToString()).append('\n');
            }
            edit.putString(PLACED, out.toString());
            edit.putInt(SCREENS, 3).putInt(HOME, 1).putInt(ROLES, 1);
        } else {
            edit.putInt(ROLES, 0);
        }
        edit.putInt(SHAPE, 1).commit();
    }

    /** How many screens stand side by side; never fewer than one. */
    static int screens(Context context) {
        return Math.max(1, store(context).getInt(SCREENS, 3));
    }

    /** The screen the everyday roles stand on: where home was when they were first set out. */
    static int roles(Context context) {
        int roles = store(context).getInt(ROLES, 1);
        return roles < 0 || roles >= screens(context) ? 0 : roles;
    }

    /** The applications last opened from here, the latest first. */
    static List<ComponentName> recent(Context context) {
        List<ComponentName> list = new ArrayList<>();
        for (String line : store(context).getString(RECENT, "").split("\n")) {
            ComponentName name = ComponentName.unflattenFromString(line);
            if (name != null) {
                list.add(name);
            }
        }
        return list;
    }

    static void opened(Context context, ComponentName name) {
        StringBuilder out = new StringBuilder(name.flattenToString());
        int count = 1;
        for (ComponentName was : recent(context)) {
            if (count >= RECENT_KEPT) {
                break;
            }
            if (!was.equals(name)) {
                out.append('\n').append(was.flattenToString());
                count++;
            }
        }
        store(context).edit().putString(RECENT, out.toString()).apply();
    }

    static void saveScreens(Context context, int count) {
        store(context).edit().putInt(SCREENS, Math.max(1, count)).apply();
    }

    /** Which screen Home returns to, counted from the left. */
    static int home(Context context) {
        int home = store(context).getInt(HOME, 1);
        return home < 0 || home >= screens(context) ? 0 : home;
    }

    static void saveHome(Context context, int screen) {
        store(context).edit().putInt(HOME, screen).apply();
    }

    /**
     * One line per spot: screen, column, row and the door, apart by tabs.
     * A line of the first versions has no screen and stands on the first.
     */
    static List<Spot> placed(Context context) {
        List<Spot> list = new ArrayList<>();
        String kept = store(context).getString(PLACED, "");
        for (String line : kept.split("\n")) {
            String[] part = line.split("\t");
            if (part.length != 3 && part.length != 4) {
                continue;
            }
            int shift = part.length - 3;
            ComponentName name = ComponentName.unflattenFromString(part[2 + shift]);
            if (name == null) {
                continue;
            }
            try {
                int screen = shift == 1 ? Integer.parseInt(part[0]) : 0;
                list.add(new Spot(name, screen, Integer.parseInt(part[shift]),
                    Integer.parseInt(part[1 + shift])));
            } catch (NumberFormatException broken) {
                // A line that cannot be read is let go.
            }
        }
        return list;
    }

    /** Sets a door down in a place; whatever the owner had put there before gives way. */
    static void place(Context context, ComponentName name, int screen, int x, int y) {
        StringBuilder out = new StringBuilder();
        for (Spot spot : placed(context)) {
            if (spot.screen == screen && spot.x == x && spot.y == y) {
                continue;
            }
            out.append(spot.screen).append('\t').append(spot.x).append('\t').append(spot.y)
                .append('\t').append(spot.name.flattenToString()).append('\n');
        }
        out.append(screen).append('\t').append(x).append('\t').append(y).append('\t')
            .append(name.flattenToString());
        store(context).edit().putString(PLACED, out.toString()).apply();
    }
}
