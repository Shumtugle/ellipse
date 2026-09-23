package io.github.shumtugle.ellipse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The home screen's own clock, drawn the design system's way: a card of
 * the surface, and in it three windows, each a container of its own
 * colour. On the left, a scalloped face with the hands of a clock; on the
 * right, the hour in figures with the date under it, and under that the
 * phone's charge.
 *
 * The hands move once a second while the clock is in sight, and not at
 * all when it is not. A press on a window opens what it shows about: the
 * alarms, the calendar, the phone's battery.
 */
final class Almanac extends View {

    interface Hand {
        void pressed(String window, View from, RectF box);
    }

    static final String DIAL = "dial";
    static final String TIME = "time";
    static final String CHARGE = "charge";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dialBox = new RectF();
    private final RectF timeBox = new RectF();
    private final RectF chargeBox = new RectF();
    private final float density;
    private final GestureDetector taps;
    private final Hand hand;
    private int charge = -1;
    private boolean charging;
    private boolean seen;

    private final Runnable tick = new Runnable() {
        public void run() {
            invalidate();
            if (seen) {
                long now = System.currentTimeMillis();
                postDelayed(this, 1000L - now % 1000L);
            }
        }
    };

    private final BroadcastReceiver power = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            read(intent);
        }
    };

    Almanac(Context context, Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
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
        });
    }

    private float px(float dp) {
        return dp * density;
    }

    private void read(Intent state) {
        if (state == null) {
            return;
        }
        int level = state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = state.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        charge = level < 0 || scale <= 0 ? -1 : Math.round(100f * level / scale);
        int plugged = state.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        charging = plugged != 0;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        read(getContext().registerReceiver(power, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            getContext().unregisterReceiver(power);
        } catch (IllegalArgumentException never) {
            // It was never taken on.
        }
        seen = false;
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        boolean now = visibility == VISIBLE;
        if (now != seen) {
            seen = now;
            removeCallbacks(tick);
            if (seen) {
                post(tick);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        taps.onTouchEvent(event);
        return true;
    }

    private void press(float x, float y) {
        RectF box = dialBox.contains(x, y) ? dialBox : timeBox.contains(x, y) ? timeBox
            : chargeBox.contains(x, y) ? chargeBox : null;
        if (box == null) {
            return;
        }
        String which = box == dialBox ? DIAL : box == timeBox ? TIME : CHARGE;
        hand.pressed(which, this, box);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float inset = px(6f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Tone.container());
        float corner = Math.min(px(28f), (h - 2f * inset) / 2f);
        canvas.drawRoundRect(inset, inset, w - inset, h - inset, corner, corner, paint);

        float pad = px(12f);
        float gap = px(10f);
        float column = h - 2f * (inset + pad);
        float side = Math.min(column, (w - 2f * (inset + pad)) * 0.44f);
        float left = inset + pad;
        dialBox.set(left, (h - side) / 2f, left + side, (h + side) / 2f);
        float right = w - inset - pad;
        float from = dialBox.right + gap * 1.4f;
        float top = inset + pad;
        float bottom = h - inset - pad;
        float split = top + (column - gap) * 0.62f;
        timeBox.set(from, top, right, split);
        chargeBox.set(from, split + gap, right, bottom);

        paint.setColor(Tone.primaryContainer());
        canvas.drawPath(cookie(dialBox.centerX(), dialBox.centerY(), side / 2f, 12, 0.07f), paint);
        dial(canvas, dialBox, Tone.onPrimaryContainer());

        float r = Math.min(timeBox.height() / 2f, px(24f));
        paint.setColor(Tone.secondaryContainer());
        canvas.drawRoundRect(timeBox, r, r, paint);
        Date now = new Date();
        int ink = Tone.onSecondaryContainer();
        words.setTextAlign(Paint.Align.CENTER);
        String hour = android.text.format.DateFormat.getTimeFormat(getContext()).format(now);
        words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        words.setColor(ink);
        words.setTextSize(fit(hour, timeBox.width() * 0.84f, timeBox.height() * 0.5f));
        canvas.drawText(hour, timeBox.centerX(), timeBox.top + timeBox.height() * 0.56f, words);
        String day = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(
            Locale.getDefault(), "EEEdMMMM"), Locale.getDefault()).format(now);
        words.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        words.setColor((ink & 0x00FFFFFF) | 0xB3000000);
        words.setTextSize(fit(day, timeBox.width() * 0.84f, timeBox.height() * 0.17f));
        canvas.drawText(day, timeBox.centerX(), timeBox.top + timeBox.height() * 0.84f, words);

        float rc = Math.min(chargeBox.height() / 2f, px(24f));
        paint.setColor(Tone.tertiaryContainer());
        canvas.drawRoundRect(chargeBox, rc, rc, paint);
        pill(canvas, chargeBox, charge >= 0 ? charge + "%" : "\u2013", Tone.onTertiaryContainer());
    }

    /** A round face with shallow scallops at its edge, like a biscuit. */
    private static Path cookie(float cx, float cy, float radius, int lobes, float depth) {
        Path shape = new Path();
        int steps = lobes * 16;
        for (int i = 0; i <= steps; i++) {
            double a = Math.PI * 2 * i / steps;
            float r = radius * (1f - depth + depth * (float) Math.cos(lobes * a));
            float x = cx + (float) Math.sin(a) * r;
            float y = cy - (float) Math.cos(a) * r;
            if (i == 0) {
                shape.moveTo(x, y);
            } else {
                shape.lineTo(x, y);
            }
        }
        shape.close();
        return shape;
    }

    private void pill(Canvas canvas, RectF box, String text, int ink) {
        float s = Math.min(box.height() * 0.5f, box.width() * 0.3f);
        words.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        words.setColor(ink);
        words.setTextAlign(Paint.Align.LEFT);
        words.setTextSize(fit(text, box.width() * 0.5f, box.height() * 0.36f));
        float textW = words.measureText(text);
        float all = s + px(6f) + textW;
        float start = box.centerX() - all / 2f;
        battery(canvas, start + s / 2f, box.centerY(), s, ink);
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + s + px(6f), box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    /** The largest size, up to a height, at which words fit a width. */
    private float fit(String text, float width, float height) {
        words.setTextSize(height);
        float measured = words.measureText(text);
        return measured > width ? height * width / measured : height;
    }

    /** Twelve marks and three hands; the seconds in the accent. */
    private void dial(Canvas canvas, RectF box, int ink) {
        float cx = box.centerX();
        float cy = box.centerY();
        float r = box.width() / 2f;
        int quiet = (ink & 0x00FFFFFF) | 0x99000000;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            boolean main = i % 3 == 0;
            paint.setColor(main ? ink : quiet);
            paint.setStrokeWidth(r * (main ? 0.04f : 0.025f));
            float from = r * (main ? 0.68f : 0.74f);
            float to = r * 0.8f;
            canvas.drawLine(cx + (float) Math.sin(a) * from, cy - (float) Math.cos(a) * from,
                cx + (float) Math.sin(a) * to, cy - (float) Math.cos(a) * to, paint);
        }
        Calendar now = Calendar.getInstance();
        float seconds = now.get(Calendar.SECOND);
        float minutes = now.get(Calendar.MINUTE) + seconds / 60f;
        float hours = now.get(Calendar.HOUR) + minutes / 60f;
        hand(canvas, cx, cy, hours / 12f, r * 0.42f, r * 0.075f, ink);
        hand(canvas, cx, cy, minutes / 60f, r * 0.62f, r * 0.05f, ink);
        hand(canvas, cx, cy, seconds / 60f, r * 0.7f, r * 0.018f, Tone.primary());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Tone.primary());
        canvas.drawCircle(cx, cy, r * 0.045f, paint);
    }

    private void hand(Canvas canvas, float cx, float cy, float turn, float length, float width, int colour) {
        double a = Math.PI * 2 * turn;
        paint.setColor(colour);
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx - (float) Math.sin(a) * length * 0.12f, cy + (float) Math.cos(a) * length * 0.12f,
            cx + (float) Math.sin(a) * length, cy - (float) Math.cos(a) * length, paint);
    }

    private void battery(Canvas canvas, float cx, float cy, float s, int colour) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(colour);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.07f);
        RectF body = new RectF(cx - s * 0.2f, cy - s * 0.34f, cx + s * 0.2f, cy + s * 0.38f);
        canvas.drawRoundRect(body, s * 0.07f, s * 0.07f, p);
        p.setStyle(Paint.Style.FILL);
        canvas.drawRect(cx - s * 0.08f, cy - s * 0.44f, cx + s * 0.08f, cy - s * 0.36f, p);
        float full = Math.max(0f, Math.min(1f, charge / 100f));
        float inner = body.height() - s * 0.16f;
        canvas.drawRect(body.left + s * 0.08f, body.bottom - s * 0.08f - inner * full,
            body.right - s * 0.08f, body.bottom - s * 0.08f, p);
        if (charging) {
            p.setColor(Tone.tertiaryContainer());
            Path bolt = new Path();
            bolt.moveTo(cx + s * 0.04f, cy - s * 0.2f);
            bolt.lineTo(cx - s * 0.08f, cy + s * 0.04f);
            bolt.lineTo(cx + s * 0.01f, cy + s * 0.04f);
            bolt.lineTo(cx - s * 0.04f, cy + s * 0.24f);
            bolt.lineTo(cx + s * 0.09f, cy - s * 0.02f);
            bolt.lineTo(cx, cy - s * 0.02f);
            bolt.close();
            canvas.drawPath(bolt, p);
        }
    }
}
