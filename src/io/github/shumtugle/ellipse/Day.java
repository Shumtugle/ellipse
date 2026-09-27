package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The day as the home screen sees it, counted by the home screen itself,
 * for the phone keeps no such count to give without leave: how long the
 * screen has been on today, the charge the day began with and has now,
 * and the warmest the battery has been. A new day begins them afresh.
 */
final class Day {

    private static final String STORE = "day";
    private static final String DATE = "date";
    private static final String SCREEN = "screen";
    private static final String ON_AT = "on_at";
    private static final String FIRST = "first";
    private static final String NOW = "now";
    private static final String HOT = "hot";

    private Day() {
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    /** Today's page, begun afresh if the day has turned. */
    private static SharedPreferences today(Context context) {
        SharedPreferences day = store(context);
        String date = new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date());
        if (!date.equals(day.getString(DATE, ""))) {
            long onAt = day.getLong(ON_AT, 0L);
            day.edit().clear().putString(DATE, date)
                .putLong(ON_AT, onAt > 0L ? System.currentTimeMillis() : 0L).apply();
        }
        return day;
    }

    static void screenOn(Context context) {
        SharedPreferences day = today(context);
        if (day.getLong(ON_AT, 0L) == 0L) {
            day.edit().putLong(ON_AT, System.currentTimeMillis()).apply();
        }
    }

    static void screenOff(Context context) {
        SharedPreferences day = today(context);
        long onAt = day.getLong(ON_AT, 0L);
        if (onAt > 0L) {
            long seen = Math.max(0L, System.currentTimeMillis() - onAt);
            day.edit().putLong(SCREEN, day.getLong(SCREEN, 0L) + seen).putLong(ON_AT, 0L).apply();
        }
    }

    /** The battery as it is now: the day's first charge kept, the warmest kept. */
    static void battery(Context context, Intent battery) {
        if (battery == null) {
            return;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int tenths = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        SharedPreferences day = today(context);
        SharedPreferences.Editor edit = day.edit();
        if (level >= 0 && scale > 0) {
            int percent = Math.round(100f * level / scale);
            if (!day.contains(FIRST)) {
                edit.putInt(FIRST, percent);
            }
            edit.putInt(NOW, percent);
        }
        if (tenths != Integer.MIN_VALUE && tenths > day.getInt(HOT, Integer.MIN_VALUE)) {
            edit.putInt(HOT, tenths);
        }
        edit.apply();
    }

    /** How long the screen has been on today, the time it is on now counted in. */
    static long screen(Context context) {
        SharedPreferences day = today(context);
        long onAt = day.getLong(ON_AT, 0L);
        return day.getLong(SCREEN, 0L) + (onAt > 0L ? Math.max(0L, System.currentTimeMillis() - onAt) : 0L);
    }

    static int first(Context context) {
        return today(context).getInt(FIRST, -1);
    }

    /** The warmest the battery has been today, in degrees; or nothing known. */
    static float hottest(Context context) {
        int tenths = today(context).getInt(HOT, Integer.MIN_VALUE);
        return tenths == Integer.MIN_VALUE ? Float.NaN : tenths / 10f;
    }
}
