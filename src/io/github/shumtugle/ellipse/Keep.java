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
        final int x;
        final int y;

        Spot(ComponentName name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    private static final String PLACED = "placed";
    private static final String ORDER = "order";
    private static final String VIEW = "view";

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

    /** One line per spot: column, row and the door, apart by tabs. */
    static List<Spot> placed(Context context) {
        List<Spot> list = new ArrayList<>();
        String kept = store(context).getString(PLACED, "");
        for (String line : kept.split("\n")) {
            String[] part = line.split("\t");
            if (part.length != 3) {
                continue;
            }
            ComponentName name = ComponentName.unflattenFromString(part[2]);
            if (name == null) {
                continue;
            }
            try {
                list.add(new Spot(name, Integer.parseInt(part[0]), Integer.parseInt(part[1])));
            } catch (NumberFormatException broken) {
                // A line that cannot be read is let go.
            }
        }
        return list;
    }

    /** Sets a door down in a place; whatever the owner had put there before gives way. */
    static void place(Context context, ComponentName name, int x, int y) {
        StringBuilder out = new StringBuilder();
        for (Spot spot : placed(context)) {
            if (spot.x == x && spot.y == y) {
                continue;
            }
            out.append(spot.x).append('\t').append(spot.y).append('\t')
                .append(spot.name.flattenToString()).append('\n');
        }
        out.append(x).append('\t').append(y).append('\t').append(name.flattenToString());
        store(context).edit().putString(PLACED, out.toString()).apply();
    }
}
