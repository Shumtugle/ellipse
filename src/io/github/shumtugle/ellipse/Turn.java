package io.github.shumtugle.ellipse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import java.util.List;
import java.util.Random;

/**
 * A new ground while the phone sleeps. When the screen goes dark, a wake
 * is set for the time chosen; if the screen comes on before it, the wake is
 * taken back. If it comes, a ground is drawn — thrown by the dice, one of
 * the ready ones, or one of the owner's own — and set on the home screen,
 * and on the lock screen too unless the owner keeps it as it is. The phone
 * wakes to it without seeing it change.
 */
public final class Turn extends BroadcastReceiver {

    static final int DICE = 0;
    static final int READY = 1;
    static final int MINE = 2;

    private static PendingIntent wake(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, Turn.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** The screen gone dark: a wake set for the time chosen, if the grounds are to turn. */
    static void slept(Context context) {
        if (!Keep.flag(context, Keep.TURN, false)) {
            return;
        }
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) {
            return;
        }
        long after = Keep.number(context, Keep.TURN_AFTER, 60) * 60000L;
        alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + after,
            wake(context));
    }

    /** The screen on again before the time: the wake taken back. */
    static void woke(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) {
            alarms.cancel(wake(context));
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Keep.flag(context, Keep.TURN, false)) {
            return;
        }
        final PendingResult later = goAsync();
        new Thread(new Runnable() {
            public void run() {
                try {
                    turn(context.getApplicationContext());
                } catch (Exception | OutOfMemoryError failed) {
                    // The ground stays as it was until the next sleep.
                } finally {
                    later.finish();
                }
            }
        }).start();
    }

    /** A ground chosen the way the owner asked, drawn at the screen's size and set. */
    static void turn(Context context) throws java.io.IOException {
        Random dice = new Random();
        int from = Keep.number(context, Keep.TURN_FROM, DICE);
        Ground ground = null;
        if (from == MINE) {
            String kept = Keep.word(context, "grounds_mine");
            if (kept != null && !kept.trim().isEmpty()) {
                String[] lines = kept.trim().split("\n");
                String line = lines[dice.nextInt(lines.length)];
                int cut = line.indexOf('\t');
                if (cut > 0) {
                    ground = Ground.of(line.substring(cut + 1));
                    ground.seed = 1 + dice.nextInt(1 << 30);
                }
            }
        } else if (from == READY) {
            ground = Ground.ready(dice.nextInt(Ground.READY.length));
            ground.seed = 1 + dice.nextInt(1 << 30);
        }
        if (ground == null) {
            ground = Ground.roll(dice);
        }
        set(context, ground, true);
    }

    /** A ground drawn at the screen's own size and set: on the home screen, and on the lock screen unless kept. */
    static void set(Context context, Ground ground, boolean home) throws java.io.IOException {
        DisplayMetrics real = new DisplayMetrics();
        ((android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
            .getRealMetrics(real);
        boolean moves = Keep.flag(context, Keep.WALL_MOVES, true);
        int w = moves ? Math.round(real.widthPixels * 1.5f) : real.widthPixels;
        Bitmap made = ground.draw(w, real.heightPixels);
        int where = WallpaperManager.FLAG_SYSTEM;
        if (Keep.flag(context, Keep.GROUND_LOCK, true)) {
            where |= WallpaperManager.FLAG_LOCK;
        }
        int id = WallpaperManager.getInstance(context).setBitmap(made, null, true, where);
        made.recycle();
        Keep.saveNumber(context, Keep.GROUND_WALL, id);
        ground.keep(context);
        Keep.saveFlag(context, Keep.GROUND_WORN, true);
    }
}
