package io.github.shumtugle.ellipse;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The clock in outline: nothing filled, everything drawn in fine lines over
 * the wallpaper — a card round it all, a round dial with dots for the hours
 * and bars at the quarters, and beside it windows of the same thin line.
 *
 * It keeps its form at any size. Low, it lays the hour and date in a long
 * window with the weather and the charge under it. Tall, the dial fills the
 * height, the weather stands in a long window with the place it is for, and
 * under it rings: the next alarm, the headphones while they are near, and
 * the charge, each ring filled as far as its measure goes.
 */
final class Contour extends View implements Timepiece {

    private static final int LINE = 0x66E8E0D0;
    private static final int INK = 0xFFEFE7D6;
    private static final int QUIET = 0xFFB9B1A2;
    private static final int MARK = 0xFF9AA3B5;
    private static final int ALARM = 0xFFC3A2D6;
    private static final int POWER = 0xFF7AA7F0;
    private static final int EARS_RING = 0xFF8FC7A0;
    private static final int SECOND = 0xFFB0404A;

    private final Almanac.Hand hand;
    private final float density;
    private final GestureDetector taps;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dialBox = new RectF();
    private final RectF timeBox = new RectF();
    private final RectF weatherBox = new RectF();
    private final RectF alarmBox = new RectF();
    private final RectF earsBox = new RectF();
    private final RectF chargeBox = new RectF();
    private boolean showWeather = true;
    private int ears = -1;
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

    Contour(Context context, Almanac.Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
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
            public void onLongPress(MotionEvent e) {
                performLongClick();
            }
        });
    }

    private float px(float dp) {
        return dp * density;
    }

    public void weather(boolean shown) {
        showWeather = shown;
        invalidate();
    }

    public void ears(int level) {
        ears = level;
        invalidate();
    }

    private void read(Intent state) {
        if (state == null) {
            return;
        }
        int level = state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = state.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        charge = level < 0 || scale <= 0 ? -1 : Math.round(100f * level / scale);
        charging = state.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
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
        seen = visibility == VISIBLE;
        removeCallbacks(tick);
        if (seen) {
            post(tick);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return taps.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void press(float x, float y) {
        RectF[] boxes = {dialBox, timeBox, weatherBox, alarmBox, earsBox, chargeBox};
        String[] names = {Almanac.DIAL, Almanac.TIME, Almanac.WEATHER, Almanac.DIAL, Almanac.EARS, Almanac.CHARGE};
        for (int i = 0; i < boxes.length; i++) {
            if (!boxes[i].isEmpty() && boxes[i].contains(x, y)) {
                hand.pressed(names[i], this, boxes[i]);
                return;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float edge = px(4);
        RectF card = new RectF(edge, edge, w - edge, h - edge);
        float thin = Math.max(1f, px(1.2f));
        line.setStrokeWidth(thin);
        line.setColor(LINE);
        float round = Math.min(card.height() * 0.14f, px(34));
        canvas.drawRoundRect(card, round, round, line);

        float pad = Math.max(px(8), card.height() * 0.07f);
        float d = card.height() - 2 * pad;
        dialBox.set(card.left + pad, card.top + pad, card.left + pad + d, card.top + pad + d);
        dial(canvas, dialBox);

        float left = dialBox.right + pad * 1.4f;
        float right = card.right - pad;
        float gap = pad * 0.8f;
        boolean tall = card.height() > card.width() * 0.42f;
        timeBox.setEmpty();
        alarmBox.setEmpty();
        earsBox.setEmpty();
        weatherBox.setEmpty();
        if (tall) {
            /* Tall: the weather and its place above, rings of measure under it. */
            float split = card.top + pad + (d - gap) * 0.42f;
            if (showWeather) {
                weatherBox.set(left, card.top + pad, right, split);
                pill(canvas, weatherBox);
                weatherTall(canvas, weatherBox);
            } else {
                timeBox.set(left, card.top + pad, right, split);
                pill(canvas, timeBox);
                time(canvas, timeBox);
            }
            float ringsTop = split + gap;
            float ringsBottom = card.bottom - pad;
            float size = Math.min(ringsBottom - ringsTop, (right - left - 2 * gap) / 3f);
            int count = ears >= 0 ? 3 : 2;
            float span = count * size + (count - 1) * gap * 1.6f;
            float x = left + (right - left - span) / 2f;
            float cy = (ringsTop + ringsBottom) / 2f;
            alarmBox.set(x, cy - size / 2f, x + size, cy + size / 2f);
            ring(canvas, alarmBox, ALARM, 1f, 0, nextAlarm());
            x += size + gap * 1.6f;
            if (ears >= 0) {
                earsBox.set(x, cy - size / 2f, x + size, cy + size / 2f);
                ring(canvas, earsBox, EARS_RING, ears / 100f, 1, ears + "%");
                x += size + gap * 1.6f;
            }
            chargeBox.set(x, cy - size / 2f, x + size, cy + size / 2f);
            ring(canvas, chargeBox, POWER, charge < 0 ? 0f : charge / 100f, 2,
                charge >= 0 ? charge + "%" : "\u2013");
        } else {
            /* Low: the hour and date above, the weather and the charge under. */
            float split = card.top + pad + (d - gap) * 0.56f;
            timeBox.set(left, card.top + pad, right, split);
            pill(canvas, timeBox);
            time(canvas, timeBox);
            float rowTop = split + gap;
            float rowBottom = card.bottom - pad;
            float tallRow = rowBottom - rowTop;
            chargeBox.set(right - tallRow, rowTop, right, rowBottom);
            float x = chargeBox.left - gap;
            if (ears >= 0) {
                earsBox.set(x - tallRow, rowTop, x, rowBottom);
                x = earsBox.left - gap;
            }
            line.setColor(LINE);
            line.setStrokeWidth(thin);
            canvas.drawOval(chargeBox, line);
            small(canvas, chargeBox, charge >= 0 ? charge + "%" : "\u2013");
            if (!earsBox.isEmpty()) {
                canvas.drawOval(earsBox, line);
                small(canvas, earsBox, ears + "%");
            }
            if (showWeather) {
                weatherBox.set(left, rowTop, x, rowBottom);
                pill(canvas, weatherBox);
                weatherLow(canvas, weatherBox);
            }
        }
    }

    private void pill(Canvas canvas, RectF box) {
        line.setColor(LINE);
        line.setStrokeWidth(Math.max(1f, px(1.2f)));
        float r = Math.min(box.height() / 2f, px(40));
        canvas.drawRoundRect(box, r, r, line);
    }

    /** The dial in outline: a fine circle, dots for the hours, bars at the quarters, three hands. */
    private void dial(Canvas canvas, RectF box) {
        float cx = box.centerX();
        float cy = box.centerY();
        float r = box.width() / 2f;
        line.setColor(LINE);
        line.setStrokeWidth(Math.max(1f, px(1.2f)));
        canvas.drawCircle(cx, cy, r, line);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            float sx = (float) Math.sin(a);
            float sy = (float) -Math.cos(a);
            fill.setColor(MARK);
            if (i % 3 == 0) {
                line.setColor(MARK);
                line.setStrokeWidth(r * 0.06f);
                canvas.drawLine(cx + sx * r * 0.74f, cy + sy * r * 0.74f, cx + sx * r * 0.86f, cy + sy * r * 0.86f, line);
            } else {
                canvas.drawCircle(cx + sx * r * 0.8f, cy + sy * r * 0.8f, r * 0.035f, fill);
            }
        }
        Calendar now = Calendar.getInstance();
        float sec = now.get(Calendar.SECOND);
        float min = now.get(Calendar.MINUTE) + sec / 60f;
        float hour = now.get(Calendar.HOUR) + min / 60f;
        stroke(canvas, cx, cy, hour / 12f, r * 0.45f, r * 0.11f, 0xFFA0937E);
        stroke(canvas, cx, cy, min / 60f, r * 0.72f, r * 0.07f, 0xFFD9CCB4);
        stroke(canvas, cx, cy, sec / 60f, r * 0.82f, r * 0.02f, SECOND);
        fill.setColor(SECOND);
        canvas.drawCircle(cx, cy, r * 0.055f, fill);
    }

    private void stroke(Canvas canvas, float cx, float cy, float turn, float length, float width, int colour) {
        double a = Math.PI * 2 * turn;
        line.setStrokeWidth(width);
        line.setColor(colour);
        canvas.drawLine(cx - (float) Math.sin(a) * length * 0.15f, cy + (float) Math.cos(a) * length * 0.15f,
            cx + (float) Math.sin(a) * length, cy - (float) Math.cos(a) * length, line);
    }

    private void time(Canvas canvas, RectF box) {
        Date now = new Date();
        String time = new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(getContext())
            ? "H:mm" : "h:mm", Locale.getDefault()).format(now);
        String date = new SimpleDateFormat("EE, d MMMM", Locale.getDefault()).format(now);
        words.setTextAlign(Paint.Align.CENTER);
        words.setTypeface(Style.family == 0 ? android.graphics.Typeface.create("sans-serif-light",
            android.graphics.Typeface.NORMAL) : Style.face());
        words.setColor(INK);
        words.setTextSize(fit(time, box.width() * 0.75f, box.height() * 0.5f));
        canvas.drawText(time, box.centerX(), box.top + box.height() * 0.56f, words);
        words.setTypeface(Style.bold());
        words.setTextSize(fit(date, box.width() * 0.75f, box.height() * 0.2f));
        canvas.drawText(date, box.centerX(), box.top + box.height() * 0.84f, words);
    }

    private String warmth() {
        return Sky.degrees() != Sky.MISSING ? Sky.degrees() + "\u00B0" : "\u2013";
    }

    private void weatherLow(Canvas canvas, RectF box) {
        String text = warmth();
        words.setTypeface(Style.face());
        words.setTextAlign(Paint.Align.LEFT);
        words.setColor(INK);
        float s = box.height() * 0.46f;
        words.setTextSize(fit(text, box.width() * 0.45f, box.height() * 0.4f));
        float all = s + px(10) + words.measureText(text);
        float start = box.centerX() - all / 2f;
        Almanac.skyMark(canvas, Sky.sky(), start + s / 2f, box.centerY(), s, INK);
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + s + px(10), box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    private void weatherTall(Canvas canvas, RectF box) {
        String text = warmth();
        String place = Keep.here(getContext())[0];
        words.setTypeface(Style.face());
        words.setTextAlign(Paint.Align.LEFT);
        words.setColor(INK);
        float s = box.height() * 0.4f;
        words.setTextSize(fit(text, box.width() * 0.4f, box.height() * 0.42f));
        float all = s + px(10) + words.measureText(text);
        float start = box.centerX() - all / 2f;
        float line1 = box.top + box.height() * (place.length() > 0 ? 0.4f : 0.5f);
        Almanac.skyMark(canvas, Sky.sky(), start + s / 2f, line1, s, 0xFFF2A96B);
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + s + px(10), line1 - (f.ascent + f.descent) / 2f, words);
        if (place.length() > 0) {
            words.setTextAlign(Paint.Align.CENTER);
            words.setTextSize(fit(place, box.width() * 0.8f, box.height() * 0.2f));
            canvas.drawText(place, box.centerX(), box.top + box.height() * 0.8f, words);
        }
    }

    /**
     * A ring of measure: a faint full circle and, over it, the part the
     * measure fills, in its colour; in the middle a small drawing and the
     * words.
     */
    private void ring(Canvas canvas, RectF box, int colour, float filled, int what, String text) {
        float stroke = box.width() * 0.08f;
        RectF inner = new RectF(box.left + stroke / 2f, box.top + stroke / 2f, box.right - stroke / 2f,
            box.bottom - stroke / 2f);
        line.setStrokeWidth(stroke);
        line.setColor((colour & 0x00FFFFFF) | 0x33000000);
        canvas.drawOval(inner, line);
        line.setColor(colour);
        canvas.drawArc(inner, -90f, 360f * Math.max(0f, Math.min(1f, filled)), false, line);
        float cx = box.centerX();
        float s = box.width() * 0.22f;
        float iconY = box.top + box.height() * 0.4f;
        fill.setColor(INK);
        line.setColor(INK);
        line.setStrokeWidth(s * 0.14f);
        if (what == 0) {
            canvas.drawCircle(cx, iconY, s * 0.45f, line);
            canvas.drawLine(cx, iconY, cx, iconY - s * 0.25f, line);
            canvas.drawLine(cx, iconY, cx + s * 0.18f, iconY + s * 0.1f, line);
            canvas.drawLine(cx - s * 0.5f, iconY - s * 0.45f, cx - s * 0.3f, iconY - s * 0.62f, line);
            canvas.drawLine(cx + s * 0.5f, iconY - s * 0.45f, cx + s * 0.3f, iconY - s * 0.62f, line);
        } else if (what == 1) {
            canvas.drawArc(new RectF(cx - s * 0.4f, iconY - s * 0.45f, cx + s * 0.4f, iconY + s * 0.35f),
                180f, 180f, false, line);
            canvas.drawRoundRect(new RectF(cx - s * 0.46f, iconY, cx - s * 0.24f, iconY + s * 0.38f),
                s * 0.06f, s * 0.06f, fill);
            canvas.drawRoundRect(new RectF(cx + s * 0.24f, iconY, cx + s * 0.46f, iconY + s * 0.38f),
                s * 0.06f, s * 0.06f, fill);
        } else {
            android.graphics.Path bolt = new android.graphics.Path();
            bolt.moveTo(cx + s * 0.12f, iconY - s * 0.5f);
            bolt.lineTo(cx - s * 0.3f, iconY + s * 0.08f);
            bolt.lineTo(cx - s * 0.02f, iconY + s * 0.08f);
            bolt.lineTo(cx - s * 0.12f, iconY + s * 0.5f);
            bolt.lineTo(cx + s * 0.3f, iconY - s * 0.08f);
            bolt.lineTo(cx + s * 0.02f, iconY - s * 0.08f);
            bolt.close();
            if (charging || what == 2) {
                canvas.drawPath(bolt, fill);
            }
        }
        words.setTypeface(Style.face());
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(QUIET);
        words.setTextSize(fit(text, box.width() * 0.6f, box.height() * 0.2f));
        canvas.drawText(text, cx, box.top + box.height() * 0.74f, words);
    }

    private void small(Canvas canvas, RectF box, String text) {
        words.setTypeface(Style.face());
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(INK);
        words.setTextSize(fit(text, box.width() * 0.66f, box.height() * 0.32f));
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, box.centerX(), box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    /** The next alarm the phone will ring, as its hour, or two dashes. */
    private String nextAlarm() {
        try {
            AlarmManager alarms = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo next = alarms == null ? null : alarms.getNextAlarmClock();
            if (next != null) {
                return new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(getContext())
                    ? "H:mm" : "h:mm", Locale.getDefault()).format(new Date(next.getTriggerTime()));
            }
        } catch (RuntimeException refused) {
            // No alarms to tell.
        }
        return "- -";
    }

    private float fit(String text, float width, float height) {
        float size = height;
        words.setTextSize(size);
        float measured = words.measureText(text);
        if (measured > width && measured > 0f) {
            size = size * width / measured;
        }
        return size;
    }
}
