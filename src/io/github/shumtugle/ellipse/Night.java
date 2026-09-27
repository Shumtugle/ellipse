package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Calendar;
import java.util.Random;

/**
 * The night clock: the home screen's clock across the whole screen lying on
 * its side, nothing else, the screen a little darker than by day. Half a
 * minute untouched, the face goes down into full black; a touch brings it
 * back for another half minute. A double touch, or anything done with two
 * fingers, closes it. It keeps the screen on only for the time chosen;
 * after that the phone sleeps as it always does. The face drifts a few
 * points now and then, so the screen does not keep its lines. It may keep
 * the phone quiet while it is open, alarms still ringing, and it closes by
 * itself when its hours are over, if it came by them.
 */
public final class Night extends Activity {

    /** How long the face shows after a touch before it goes down into black. */
    private static final long SHOWN = 30000L;

    /** As dark as asked: the screen's light, and a veil over the face. */
    private static final float[] LIGHT = {-1f, 0.25f, 0.08f, 0.02f};
    private static final float[] VEIL = {0f, 0.12f, 0.3f, 0.5f};

    /** Whether it came by its hours: it then goes when they are over. */
    static final String BY_CLOCK = "by_clock";

    private final Handler later = new Handler(Looper.getMainLooper());
    private final Random drift = new Random();
    private View face;
    private int quietBefore = -1;
    private GestureDetector twice;
    private boolean byClock;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Home.sized(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Style.read(this);
        byClock = getIntent().getBooleanExtra(BY_CLOCK, false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        int dark = Math.max(0, Math.min(LIGHT.length - 1, Keep.number(this, Keep.NIGHT_DIM, 1)));
        if (LIGHT[dark] >= 0f) {
            WindowManager.LayoutParams light = getWindow().getAttributes();
            light.screenBrightness = LIGHT[dark];
            getWindow().setAttributes(light);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        int kind = Keep.number(this, Keep.NIGHT_FACE, -1);
        face = kind < 0 ? Home.timepiece(this, hand) : Home.timepiece(this, hand, kind);
        if (face instanceof Timepiece) {
            ((Timepiece) face).weather(Keep.flag(this, Keep.WEATHER, true));
            ((Timepiece) face).ears(-1);
        }
        root.addView(face, new FrameLayout.LayoutParams(0, 0));
        View veil = new View(this);
        veil.setBackgroundColor(Math.round(VEIL[dark] * 255) << 24);
        veil.setClickable(false);
        root.addView(veil, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        root.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (r - l != or - ol || b - t != ob - ot) {
                    lay(r - l, b - t);
                }
            }
        });
        twice = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                wake();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                finish();
                return true;
            }
        });
        setContentView(root);
        hideBars();
    }

    /** Touches reach the whole screen first, the face's own windows included: none of them opens anything. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        /* Anything with two fingers closes it, as a double touch does. */
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
            finish();
            return true;
        }
        twice.onTouchEvent(event);
        return true;
    }

    /** The face back from the black, for another half minute. */
    private void wake() {
        later.removeCallbacks(sink);
        face.animate().cancel();
        face.animate().alpha(1f).setDuration(600).start();
        if (Keep.flag(this, Keep.NIGHT_HIDE, true)) {
            later.postDelayed(sink, SHOWN);
        }
    }

    /** The face down into full black, slowly: on this screen, black is the light put out. */
    private final Runnable sink = new Runnable() {
        public void run() {
            face.animate().cancel();
            face.animate().alpha(0f).setDuration(2500).start();
        }
    };

    /** Its time over: the screen is no longer kept on, and the phone sleeps as it always does. */
    private final Runnable spent = new Runnable() {
        public void run() {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    };

    private final Almanac.Hand hand = new Almanac.Hand() {
        public void pressed(String window, View from, RectF box) {
            // At night the face only tells; the touch is read above.
        }
    };

    /** The face across the screen, as wide as it goes while keeping its own proportion, in the middle. */
    private void lay(int w, int h) {
        int margin = Math.round(getResources().getDisplayMetrics().density * 24);
        int wide = w - margin * 2;
        int tall = Math.min(h - margin * 2, Math.round(wide / 2.3f));
        wide = Math.min(wide, Math.round(tall * 2.3f));
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(wide, tall);
        at.leftMargin = (w - wide) / 2;
        at.topMargin = (h - tall) / 2;
        face.setLayoutParams(at);
    }

    private final Runnable move = new Runnable() {
        public void run() {
            /* A few points aside every few minutes, so the screen keeps no line burnt in. */
            float reach = getResources().getDisplayMetrics().density * 8;
            face.animate().translationX((drift.nextFloat() * 2 - 1) * reach)
                .translationY((drift.nextFloat() * 2 - 1) * reach).setDuration(4000).start();
            if (byClock && !Night.within(Night.this)) {
                finish();
                return;
            }
            later.postDelayed(this, 180000L);
        }
    };

    private void hideBars() {
        WindowInsetsController bars = getWindow().getInsetsController();
        if (bars != null) {
            bars.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
        quiet(true);
        later.postDelayed(move, 180000L);
        wake();
        later.removeCallbacks(spent);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        later.postDelayed(spent, Keep.number(this, Keep.NIGHT_LASTS, 30) * 60000L);
    }

    @Override
    protected void onPause() {
        later.removeCallbacks(move);
        later.removeCallbacks(sink);
        later.removeCallbacks(spent);
        quiet(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        /* Left for anything else, it does not wait behind it. */
        finish();
    }

    /** Quiet while it is open, if asked and allowed: only alarms get through; as it was again after. */
    private void quiet(boolean on) {
        if (!Keep.flag(this, Keep.NIGHT_QUIET, false)) {
            return;
        }
        NotificationManager notes = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notes == null || !notes.isNotificationPolicyAccessGranted()) {
            return;
        }
        try {
            if (on) {
                quietBefore = notes.getCurrentInterruptionFilter();
                notes.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS);
            } else if (quietBefore > 0) {
                notes.setInterruptionFilter(quietBefore);
                quietBefore = -1;
            }
        } catch (RuntimeException refused) {
            // The phone keeps its own sounds.
        }
    }

    // ------------------------------------------------------------ its hours

    /** Whether it is now within the night clock's hours. */
    static boolean within(Context context) {
        Calendar now = Calendar.getInstance();
        int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int from = Keep.number(context, Keep.NIGHT_FROM, 23 * 60);
        int until = Keep.number(context, Keep.NIGHT_UNTIL, 7 * 60);
        return from <= until ? minute >= from && minute < until : minute >= from || minute < until;
    }

    private static boolean charging(Context context) {
        android.content.Intent power = context.registerReceiver(null,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = power == null ? 0 : power.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        return plugged != 0;
    }

    /**
     * Whether the home screen, coming to the front, is to open the night clock
     * by its hours: they are on, it is within them, the phone is charging if
     * that is asked, and it has not already opened this night — closed by
     * the owner, it stays closed until the next.
     */
    static boolean due(Context context) {
        if (!Keep.flag(context, Keep.NIGHT_AUTO, false) || !within(context)) {
            return false;
        }
        if (Keep.flag(context, Keep.NIGHT_CHARGING, true) && !charging(context)) {
            return false;
        }
        long last = Keep.clock(context, Keep.NIGHT_OPENED);
        return System.currentTimeMillis() - last > 14L * 3600000L;
    }

    /** Opened by its hours: noted, so it opens once a night. */
    static void open(Context context, boolean byClock) {
        if (byClock) {
            Keep.saveClock(context, Keep.NIGHT_OPENED, System.currentTimeMillis());
        }
        Intent night = new Intent(context, Night.class).putExtra(BY_CLOCK, byClock);
        if (!(context instanceof Activity)) {
            night.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(night);
    }
}
