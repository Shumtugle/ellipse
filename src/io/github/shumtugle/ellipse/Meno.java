package io.github.shumtugle.ellipse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
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
 * The owner's own widget clock, drawn here as it draws itself, from its own
 * pictures and to its own measures: a card of dark glass with a fine light
 * edge, the dial and its hands on the left, and on the right the hour and
 * the date in a card of their own, under it a pill with the weather and a
 * circle with the charge — and, while the headphones are near, a circle for
 * them between the two.
 *
 * Its right side keeps the widget's own sizes; only when the clock stands
 * lower than the widget ever does is it drawn smaller, all of it alike.
 */
final class Meno extends View implements Timepiece {

    /** The seconds hand in the widget's own colours, as it offers them. */
    static final int[] SECONDS = {0xFFF5F1E8, 0xFF8A8A8A, 0xFF7D8BD4, 0xFF9E2B3A, 0xFFA8C0D8, 0xFFC9A86A,
        0xFFD8C9A8};
    private static final int[] SECOND_PICTURES = {R.drawable.meno_second_0, R.drawable.meno_second_1,
        R.drawable.meno_second_2, R.drawable.meno_second_3, R.drawable.meno_second_4, R.drawable.meno_second_5,
        R.drawable.meno_second_6};
    private static final int INK = 0xFFF5F1E8;
    private static final int EDGE = 0x3DFFFFFF;
    private static final int GLASS = 0x0E1014;

    private final Almanac.Hand hand;
    private final float density;
    private final float scaled;
    private final GestureDetector taps;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dialBox = new RectF();
    private final RectF timeBox = new RectF();
    private final RectF weatherBox = new RectF();
    private final RectF earsBox = new RectF();
    private final RectF chargeBox = new RectF();
    private final Bitmap dial;
    private final Bitmap hourHand;
    private final Bitmap minuteHand;
    private final Bitmap secondHand;
    private final int cardAlpha;
    private boolean showWeather = true;
    private int ears = -1;
    private int charge = -1;
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

    Meno(Context context, Almanac.Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;
        BitmapFactory.Options raw = new BitmapFactory.Options();
        raw.inScaled = false;
        dial = BitmapFactory.decodeResource(context.getResources(), R.drawable.meno_dial, raw);
        hourHand = BitmapFactory.decodeResource(context.getResources(), R.drawable.meno_hand_hour, raw);
        minuteHand = BitmapFactory.decodeResource(context.getResources(), R.drawable.meno_hand_minute, raw);
        int second = Math.max(0, Math.min(SECOND_PICTURES.length - 1, Keep.number(context, Keep.MENO_SECOND, 0)));
        secondHand = BitmapFactory.decodeResource(context.getResources(), SECOND_PICTURES[second], raw);
        /* The card's glass: the widget's own tenth by default, or as dark as the settings ask. */
        int dark = Keep.number(context, Keep.CLOCK_GROUND, 10);
        cardAlpha = Math.round(255f * Math.max(0, Math.min(95, dark)) / 100f);
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

    private float dp(float v) {
        return v * density;
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
        RectF[] boxes = {dialBox, timeBox, weatherBox, earsBox, chargeBox};
        String[] names = {Almanac.DIAL, Almanac.TIME, Almanac.WEATHER, Almanac.EARS, Almanac.CHARGE};
        for (int i = 0; i < boxes.length; i++) {
            if (!boxes[i].isEmpty() && boxes[i].contains(x, y)) {
                hand.pressed(names[i], this, boxes[i]);
                return;
            }
        }
    }

    /** A box of the widget's own drawing: a fill of its dark glass, a fine light edge. */
    private void card(Canvas canvas, RectF box, float radius, int alpha) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((alpha << 24) | GLASS);
        canvas.drawRoundRect(box, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, dp(1)));
        paint.setColor(EDGE);
        canvas.drawRoundRect(box, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float edge = dp(4);
        RectF whole = new RectF(edge, edge, w - edge, h - edge);
        card(canvas, whole, dp(30), cardAlpha);

        float pad = dp(10);
        RectF inside = new RectF(whole.left + pad, whole.top + pad, whole.right - pad, whole.bottom - pad);
        float gap = dp(8);
        float leftW = (inside.width() - gap) * 1f / 2.1f;
        float d = Math.min(leftW, inside.height());
        dialBox.set(inside.left + (leftW - d) / 2f, inside.centerY() - d / 2f, inside.left + (leftW + d) / 2f,
            inside.centerY() + d / 2f);
        clock(canvas, dialBox);

        /* The right side at the widget's own sizes, drawn smaller alike
           only where the clock stands too low for them. */
        float rightLeft = inside.left + leftW + gap;
        float rightW = inside.right - rightLeft;
        float timeSize = 38f * scaled;
        float dateSize = 16f * scaled;
        float needed = dp(6) + timeSize * 1.17f + dateSize * 1.17f + dp(6) + dp(10) + dp(46);
        float k = Math.min(1f, inside.height() / needed);
        float cardH = (dp(6) * 2 + timeSize * 1.17f + dateSize * 1.17f) * k;
        float rowH = dp(46) * k;
        float top = inside.centerY() - (cardH + dp(10) * k + rowH) / 2f;
        timeBox.set(rightLeft, top, rightLeft + rightW, top + cardH);
        card(canvas, timeBox, dp(22) * k, 0x14);
        Date now = new Date();
        String time = new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(getContext())
            ? "HH:mm" : "hh:mm", Locale.getDefault()).format(now);
        String date = new SimpleDateFormat("EE, d MMMM", Locale.getDefault()).format(now);
        words.setColor(INK);
        words.setTextAlign(Paint.Align.CENTER);
        words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        words.setTextSize(fit(time, rightW - dp(28) * k, timeSize * k));
        Paint.FontMetrics f = words.getFontMetrics();
        float line1 = timeBox.top + dp(6) * k - f.ascent;
        canvas.drawText(time, timeBox.centerX(), line1, words);
        words.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        words.setTextSize(fit(date, rightW - dp(28) * k, dateSize * k));
        Paint.FontMetrics g = words.getFontMetrics();
        canvas.drawText(date, timeBox.centerX(), line1 + f.descent - g.ascent, words);

        float rowTop = timeBox.bottom + dp(10) * k;
        chargeBox.set(timeBox.right - rowH, rowTop, timeBox.right, rowTop + rowH);
        float x = chargeBox.left - dp(8) * k;
        if (ears >= 0) {
            earsBox.set(x - rowH, rowTop, x, rowTop + rowH);
            x = earsBox.left - dp(8) * k;
        } else {
            earsBox.setEmpty();
        }
        if (showWeather) {
            weatherBox.set(timeBox.left, rowTop, x, rowTop + rowH);
            card(canvas, weatherBox, dp(23) * k, 0x14);
            weather(canvas, weatherBox, k);
        } else {
            weatherBox.setEmpty();
        }
        circle(canvas, chargeBox, charge >= 0 ? charge + "%" : "\u2026", k);
        if (!earsBox.isEmpty()) {
            circle(canvas, earsBox, ears + "%", k);
        }
    }

    /** The dial and its hands, each a picture the size of the dial turned about its middle. */
    private void clock(Canvas canvas, RectF box) {
        if (dial == null) {
            return;
        }
        canvas.drawBitmap(dial, null, box, paint);
        Calendar now = Calendar.getInstance();
        float sec = now.get(Calendar.SECOND);
        float min = now.get(Calendar.MINUTE) + sec / 60f;
        float hour = now.get(Calendar.HOUR) + min / 60f;
        turned(canvas, hourHand, box, hour * 30f);
        turned(canvas, minuteHand, box, min * 6f);
        turned(canvas, secondHand, box, sec * 6f);
    }

    private void turned(Canvas canvas, Bitmap picture, RectF box, float degrees) {
        if (picture == null) {
            return;
        }
        canvas.save();
        canvas.rotate(degrees, box.centerX(), box.centerY());
        canvas.drawBitmap(picture, null, box, paint);
        canvas.restore();
    }

    private void weather(Canvas canvas, RectF box, float k) {
        String text = Sky.degrees() != Sky.MISSING ? Sky.degrees() + "\u00B0" : "\u2026";
        words.setTypeface(Typeface.DEFAULT);
        words.setTextAlign(Paint.Align.LEFT);
        words.setColor(INK);
        words.setTextSize(fit(text, box.width() * 0.5f, 16f * scaled * k));
        float icon = dp(26) * k;
        float all = icon + dp(7) * k + words.measureText(text);
        float start = box.centerX() - all / 2f;
        Bitmap picture = sky(Sky.sky());
        if (picture != null) {
            canvas.drawBitmap(picture, null, new RectF(start, box.centerY() - icon / 2f, start + icon,
                box.centerY() + icon / 2f), paint);
        }
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + icon + dp(7) * k, box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    private Bitmap skyPicture;
    private int skyKind = -1;

    /** The widget's own drawing of the sky. */
    private Bitmap sky(int kind) {
        if (kind != skyKind) {
            int id;
            switch (kind) {
                case Sky.CLEAR:
                    id = R.drawable.meno_wx_sun;
                    break;
                case Sky.FOG:
                    id = R.drawable.meno_wx_fog;
                    break;
                case Sky.RAIN:
                    id = R.drawable.meno_wx_rain;
                    break;
                case Sky.SNOW:
                    id = R.drawable.meno_wx_snow;
                    break;
                case Sky.STORM:
                    id = R.drawable.meno_wx_storm;
                    break;
                default:
                    id = R.drawable.meno_wx_cloud;
                    break;
            }
            BitmapFactory.Options raw = new BitmapFactory.Options();
            raw.inScaled = false;
            skyPicture = BitmapFactory.decodeResource(getResources(), id, raw);
            skyKind = kind;
        }
        return skyPicture;
    }

    private void circle(Canvas canvas, RectF box, String text, float k) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((0x14 << 24) | GLASS);
        canvas.drawOval(box, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, dp(1)));
        paint.setColor(EDGE);
        canvas.drawOval(box, paint);
        paint.setStyle(Paint.Style.FILL);
        words.setTypeface(Typeface.DEFAULT);
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(INK);
        words.setTextSize(fit(text, box.width() * 0.78f, 15f * scaled * k));
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, box.centerX(), box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    private float fit(String text, float width, float size) {
        words.setTextSize(size);
        float measured = words.measureText(text);
        return measured > width && measured > 0f ? size * width / measured : size;
    }
}
