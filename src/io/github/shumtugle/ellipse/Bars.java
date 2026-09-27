package io.github.shumtugle.ellipse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The home screen's own status strip, standing where the phone's status
 * bar stands while the home screen hides it: a strip of signs — the signal,
 * Wi-Fi, what waits unread, the date, the charge. Each sign may be left out;
 * all are drawn in ink that reads on the wallpaper — dark on a light one, as
 * the phone's own bar does — in the accent, or in a colour of the owner's
 * own. Nothing here asks for any leave: what is shown is what the phone
 * tells any home screen.
 */
final class Bars {

    private Bars() {
    }

    /** What the strips ask of the home screen. */
    interface Hand {
        void open(String what, View from, RectF box);
    }

    static final String SHADE = "shade";
    static final String QUICK = "quick";
    static final String WIFI = "wifi";
    static final String SIGNAL = "signal";
    static final String DATE = "date";
    static final String CHARGE = "charge";
    static final int INK = 0xFFF2EEE6;
    static final int DARK_INK = 0xFF1C1A17;

    /**
     * Whether the wallpaper under the strip is light, as the phone itself
     * judges it for its own bar's icons: the wallpaper's colours say whether
     * dark text reads on them.
     */
    static boolean lightBehind(Context context) {
        try {
            android.app.WallpaperManager walls = android.app.WallpaperManager.getInstance(context);
            android.app.WallpaperColors colours = walls.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM);
            if (colours == null) {
                return false;
            }
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                return (colours.getColorHints() & android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
            }
            return android.graphics.Color.luminance(colours.getPrimaryColor().toArgb()) > 0.6f;
        } catch (RuntimeException unread) {
            return false;
        }
    }

    /** The strip's colour: ink that reads on the wallpaper, the accent, or the owner's own from the palette. */
    static int colour(Context context) {
        int mode = Keep.number(context, Keep.BARS_COLOUR, 0);
        if (mode == 1) {
            return Tone.primary();
        }
        if (mode == 2) {
            return Rings.colour(Math.max(1, Keep.number(context, Keep.BARS_OWN_COLOUR, 7)));
        }
        return lightBehind(context) ? DARK_INK : INK;
    }

    /** The accent as it reads on the wallpaper: deepened on a light one, as the ink darkens. */
    static int accentOn(Context context) {
        int accent = Tone.primary();
        if (!lightBehind(context)) {
            return accent;
        }
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(accent, hsv);
        hsv[1] = Math.min(1f, hsv[1] + 0.25f);
        hsv[2] = Math.min(hsv[2], 0.45f);
        return android.graphics.Color.HSVToColor(hsv);
    }

    abstract static class Strip extends View {
        final Hand hand;
        final float density;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
        final GestureDetector taps;
        int ink;
        int accent;
        private final java.util.List<String> names = new java.util.ArrayList<>();
        private final java.util.List<RectF> boxes = new java.util.ArrayList<>();

        Strip(Context context, Hand hand) {
            super(context);
            this.hand = hand;
            density = context.getResources().getDisplayMetrics().density;
            words.setTypeface(Typeface.DEFAULT);
            taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    press(e.getX(), e.getY());
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                    return flung(a, b, vx, vy);
                }
            });
            read();
        }

        /** The colours and the chosen signs, read again as the settings change. */
        void read() {
            ink = colour(getContext());
            accent = Keep.number(getContext(), Keep.BARS_COLOUR, 0) == 0 ? accentOn(getContext()) : ink;
            invalidate();
        }

        boolean flung(MotionEvent a, MotionEvent b, float vx, float vy) {
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return taps.onTouchEvent(event) || super.onTouchEvent(event);
        }

        private void press(float x, float y) {
            for (int i = boxes.size() - 1; i >= 0; i--) {
                if (boxes.get(i).contains(x, y)) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    hand.open(names.get(i), this, new RectF(boxes.get(i)));
                    return;
                }
            }
        }

        void window(String name, float l, float t, float r, float b) {
            names.add(name);
            boxes.add(new RectF(l, t, r, b));
        }

        @Override
        protected final void onDraw(Canvas canvas) {
            names.clear();
            boxes.clear();
            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            draw(canvas, getWidth(), getHeight());
        }

        abstract void draw(Canvas canvas, float w, float h);

        float dp(float v) {
            return v * density;
        }
    }

    // ------------------------------------------------------------- the top

    /** The status strip: signs on the left, the date in the middle, the charge on the right. */
    static final class Status extends Strip {
        private int charge = -1;
        private boolean charging;
        private boolean seen;

        private final BroadcastReceiver power = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                battery(intent);
            }
        };

        private final Runnable tick = new Runnable() {
            public void run() {
                invalidate();
                if (seen) {
                    postDelayed(this, 30000L);
                }
            }
        };

        Status(Context context, Hand hand) {
            super(context, hand);
        }

        private void battery(Intent state) {
            if (state == null) {
                return;
            }
            int level = state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int of = state.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            charge = level < 0 || of <= 0 ? -1 : Math.round(100f * level / of);
            int status = state.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            invalidate();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            battery(getContext().registerReceiver(power, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        }

        @Override
        protected void onDetachedFromWindow() {
            try {
                getContext().unregisterReceiver(power);
            } catch (IllegalArgumentException never) {
                // Never taken on.
            }
            seen = false;
            removeCallbacks(tick);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            seen = visibility == VISIBLE;
            removeCallbacks(tick);
            if (seen) {
                post(tick);
            }
        }

        /** A swipe down: the notifications from the left half, the quick settings from the right. */
        @Override
        boolean flung(MotionEvent a, MotionEvent b, float vx, float vy) {
            if (a == null || b.getY() - a.getY() < dp(12) || Math.abs(vy) < Math.abs(vx)) {
                return false;
            }
            hand.open(a.getX() < getWidth() / 2f ? SHADE : QUICK, this, new RectF(0, 0, getWidth(), getHeight()));
            return true;
        }

        void draw(Canvas c, float w, float h) {
            float cy = h / 2f;
            float s = Math.min(h * 0.5f, dp(14));
            float pad = dp(16);
            float x = pad;
            Context context = getContext();
            if (Keep.flag(context, Keep.STRIP_SIGNAL, true)) {
                int level = signal(context);
                if (level >= 0) {
                    drawSignal(c, x, cy + s * 0.5f, s, level);
                    window(SIGNAL, x - dp(6), 0, x + s * 1.1f + dp(6), h);
                    x += s * 1.1f + dp(12);
                }
            }
            if (Keep.flag(context, Keep.STRIP_WIFI, true)) {
                int level = wifi(context);
                if (level >= 0) {
                    drawWifi(c, x + s * 0.5f, cy + s * 0.42f, s, level);
                    window(WIFI, x - dp(6), 0, x + s + dp(6), h);
                    x += s + dp(12);
                }
            }
            if (Keep.flag(context, Keep.STRIP_NOTES, true)) {
                int waiting = Notices.marked().size();
                if (waiting > 0) {
                    drawBell(c, x + s * 0.5f, cy, s, accent);
                    words.setColor(accent);
                    words.setTextSize(s * 0.9f);
                    words.setTypeface(Typeface.DEFAULT_BOLD);
                    words.setTextAlign(Paint.Align.LEFT);
                    Paint.FontMetrics f = words.getFontMetrics();
                    c.drawText(String.valueOf(waiting), x + s + dp(3), cy - (f.ascent + f.descent) / 2f, words);
                    window(SHADE, x - dp(6), 0, x + s + dp(20), h);
                }
            }
            if (Keep.flag(context, Keep.STRIP_DATE, true)) {
                String day = new SimpleDateFormat("EE d MMMM", Locale.getDefault()).format(new Date());
                words.setColor((ink & 0x00FFFFFF) | 0xA6000000);
                words.setTextSize(s * 0.9f);
                words.setTypeface(Typeface.DEFAULT);
                words.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics f = words.getFontMetrics();
                c.drawText(day, w / 2f, cy - (f.ascent + f.descent) / 2f, words);
                float half = words.measureText(day) / 2f;
                window(DATE, w / 2f - half - dp(8), 0, w / 2f + half + dp(8), h);
            }
            if (Keep.flag(context, Keep.STRIP_CHARGE, true) && charge >= 0) {
                float bw = s * 1.8f;
                float bh = s * 0.9f;
                float right = w - pad;
                RectF body = new RectF(right - bw - dp(2), cy - bh / 2f, right - dp(2), cy + bh / 2f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1f, dp(1.2f)));
                paint.setColor(ink);
                c.drawRoundRect(body, dp(2.5f), dp(2.5f), paint);
                paint.setStyle(Paint.Style.FILL);
                c.drawRect(right - dp(2), cy - bh * 0.2f, right, cy + bh * 0.2f, paint);
                float in = dp(2);
                paint.setColor(charging ? accent : ink);
                c.drawRoundRect(new RectF(body.left + in, body.top + in,
                    body.left + in + (body.width() - 2 * in) * charge / 100f, body.bottom - in), dp(1.5f), dp(1.5f), paint);
                words.setColor(ink);
                words.setTextSize(s * 0.9f);
                words.setTypeface(Typeface.DEFAULT);
                words.setTextAlign(Paint.Align.RIGHT);
                Paint.FontMetrics f = words.getFontMetrics();
                c.drawText(String.valueOf(charge), body.left - dp(5), cy - (f.ascent + f.descent) / 2f, words);
                window(CHARGE, body.left - dp(34), 0, w, h);
            }
        }

        private void drawSignal(Canvas c, float x, float bottom, float s, int level) {
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 4; i++) {
                float tall = s * (0.3f + 0.23f * i);
                paint.setColor(i < level ? ink : (ink & 0x00FFFFFF) | 0x40000000);
                c.drawRect(x + i * s * 0.28f, bottom - tall, x + i * s * 0.28f + s * 0.18f, bottom, paint);
            }
        }

        private void drawWifi(Canvas c, float x, float y, float s, int level) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.2f, s * 0.13f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 3; i++) {
                float r = s * (0.32f + 0.3f * i);
                paint.setColor(i < level ? ink : (ink & 0x00FFFFFF) | 0x40000000);
                c.drawArc(new RectF(x - r, y - r, x + r, y + r), 225f, 90f, false, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ink);
            c.drawCircle(x, y, s * 0.08f, paint);
        }

        private void drawBell(Canvas c, float x, float y, float s, int colour) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colour);
            android.graphics.Path bell = new android.graphics.Path();
            bell.moveTo(x - s * 0.42f, y + s * 0.28f);
            bell.cubicTo(x - s * 0.36f, y - s * 0.1f, x - s * 0.36f, y - s * 0.46f, x, y - s * 0.46f);
            bell.cubicTo(x + s * 0.36f, y - s * 0.46f, x + s * 0.36f, y - s * 0.1f, x + s * 0.42f, y + s * 0.28f);
            bell.close();
            c.drawPath(bell, paint);
            c.drawCircle(x, y + s * 0.4f, s * 0.1f, paint);
        }

        /** The mobile signal in steps, nought to four; less than nought where the phone does not tell it. */
        private static int signal(Context context) {
            try {
                android.telephony.TelephonyManager phone =
                    (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (phone == null || android.os.Build.VERSION.SDK_INT < 28) {
                    return -1;
                }
                android.telephony.SignalStrength strength = phone.getSignalStrength();
                return strength == null ? -1 : Math.min(4, strength.getLevel());
            } catch (RuntimeException withheld) {
                return -1;
            }
        }

        /** Wi-Fi in steps, one to three, when it carries the phone; less than nought when it does not. */
        private static int wifi(Context context) {
            try {
                android.net.ConnectivityManager net =
                    (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (net == null) {
                    return -1;
                }
                android.net.NetworkCapabilities caps = net.getNetworkCapabilities(net.getActiveNetwork());
                if (caps == null || !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                    return -1;
                }
                int rssi = android.os.Build.VERSION.SDK_INT >= 29 ? caps.getSignalStrength() : -60;
                return rssi > -60 ? 3 : rssi > -72 ? 2 : 1;
            } catch (RuntimeException withheld) {
                return -1;
            }
        }
    }
}
