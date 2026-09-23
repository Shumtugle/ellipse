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

    private Keep() {
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences("ellipse", Context.MODE_PRIVATE);
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
