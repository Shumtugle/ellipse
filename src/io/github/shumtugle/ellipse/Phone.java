package io.github.shumtugle.ellipse;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

/**
 * The phone's own state, as far as it tells without being asked for leave:
 * its battery — how full, whether charging, its health, warmth, volts and
 * how many times it has been filled; its working memory and its storage;
 * when it was last made safe, and its system's version; how long it has
 * run since it was started.
 */
final class Phone {

    int level = -1;
    boolean charging;
    int health = -1;
    float warmth = Float.NaN;
    float volts = Float.NaN;
    int cycles = -1;
    long memoryFree;
    long memoryAll;
    long storageFree;
    long storageAll;
    String safe = "";
    String update = "";
    String version = "";
    String model = "";
    String build = "";
    long awake;

    static final int GOOD = 0;
    static final int WARM = 1;
    static final int WORN = 2;
    static final int COLD = 3;
    static final int UNKNOWN = 4;

    static Phone read(Context context) {
        Phone p = new Phone();
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            p.level = level < 0 || scale <= 0 ? -1 : Math.round(100f * level / scale);
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            p.charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            switch (battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    p.health = GOOD;
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    p.health = WARM;
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    p.health = WORN;
                    break;
                case BatteryManager.BATTERY_HEALTH_COLD:
                    p.health = COLD;
                    break;
                default:
                    p.health = UNKNOWN;
            }
            int tenths = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            p.warmth = tenths == Integer.MIN_VALUE ? Float.NaN : tenths / 10f;
            int millis = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            p.volts = millis <= 0 ? Float.NaN : (millis > 1000 ? millis / 1000f : millis);
            /* How many times it has been filled, told from the fourteenth version on. */
            p.cycles = battery.getIntExtra("android.os.extra.CYCLE_COUNT", -1);
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(memory);
            p.memoryFree = memory.availMem;
            p.memoryAll = memory.totalMem;
        }
        try {
            StatFs data = new StatFs(Environment.getDataDirectory().getPath());
            p.storageFree = data.getAvailableBytes();
            p.storageAll = data.getTotalBytes();
        } catch (RuntimeException unread) {
            // Not told.
        }
        p.safe = Build.VERSION.SECURITY_PATCH == null ? "" : Build.VERSION.SECURITY_PATCH;
        try {
            /* The system's own modules carry the date of its last update as their version. */
            p.update = context.getPackageManager().getPackageInfo(SYSTEM_MODULES, 0).versionName;
        } catch (Exception none) {
            p.update = "";
        }
        p.version = Build.VERSION.RELEASE;
        p.model = Build.MODEL;
        p.build = Build.DISPLAY;
        p.awake = SystemClock.elapsedRealtime();
        return p;
    }

    private static final String SYSTEM_MODULES = "com.google.android.modulemetadata";
}
