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
 * The owner's own widget clock: a card of dark glass with a fine light
 * edge, a dial with its hands, the hour and the date in a card of their
 * own, and under it a pill with the weather and circles for the charge
 * and, while they are near, the headphones.
 *
 * It is drawn in lines, not pictures, on a canvas of its own measured in
 * dp, and brought to the size of its box by one multiplier, so its form is
 * the same on any grid. It keeps the widget's own arrangement in any box,
 * the dial on the left and the windows beside it: height to spare goes to
 * the card's margins and the dial, never to a new order. The dial
 * keeps the widget's geometry, a square of six hundred units: the ring,
 * bars at the quarters, dots between, and hands of the widget's widths.
 * No line is ever thinner on the screen than a dp and a half, and every
 * mark is drawn in a solid colour.
 */
final class Meno extends View implements Timepiece {

    /** The seconds hand in the widget's own colours, as it offers them. */
    static final int[] SECONDS = {0xFFF5F1E8, 0xFF8A8A8A, 0xFF7D8BD4, 0xFF9E2B3A, 0xFFA8C0D8, 0xFFC9A86A,
        0xFFD8C9A8};

    /** The canvas, in dp: the widget's own. */
    static final float WIDE_W = 360f;
    static final float WIDE_H = 160f;
    /** The smallest multiplier the clock is drawn at; a box that small takes more places. */
    static final float LEAST = 0.65f;

    private static final int INK = 0xFFF5F1E8;
    private static final int MARK = 0xFFAEB8C8;
    private static final int HOUR = 0xFFC9BFAF;
    private static final int MINUTE = 0xFFE8E2D8;
    private static final int GLASS = 0x0E1014;
    private static final int EDGE_ALPHA = 0x66;
    private static final int FIELD_ALPHA = 0x14;

    /* The widget's measures, in dp of the canvas. */
    private static final float BORDER = 4f;
    private static final float TIME_SIZE = 38f;
    private static final float DATE_SIZE = 16f;
    private static final float LINE = 1.17f;
    private static final float TIME_PAD = 6f;
    private static final float ROW = 46f;
    private static final float GAP = 8f;
    private static final float UNDER = 10f;
    private static final float THINNEST = 1.5f;

    private final Almanac.Hand hand;
    private final float density;
    private final GestureDetector taps;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();
    private final RectF dialBox = new RectF();
    private final RectF timeBox = new RectF();
    private final RectF weatherBox = new RectF();
    private final RectF earsBox = new RectF();
    private final RectF chargeBox = new RectF();
    private final RectF spare = new RectF();
    private final int second;
    private final int cardAlpha;
    private final float strength;
    /** The canvas's dp to the screen's pixels, as last drawn. */
    private float scale = 1f;
    private float k = 1f;
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
        int which = Math.max(0, Math.min(SECONDS.length - 1, Keep.number(context, Keep.MENO_SECOND, 0)));
        second = SECONDS[which];
        /* The card's glass: the widget's own tenth by default, or as dark as the settings ask. */
        int dark = Keep.number(context, Keep.CLOCK_GROUND, 10);
        cardAlpha = Math.round(255f * Math.max(0, Math.min(95, dark)) / 100f);
        strength = Math.max(50, Math.min(200, Keep.number(context, Keep.MENO_LINES, 100))) / 100f;
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

    /** The least box, in dp, the clock is drawn in at its smallest multiplier. */
    static float[] least() {
        return new float[] {WIDE_W * LEAST, WIDE_H * LEAST};
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
        int scaleOf = state.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        charge = level < 0 || scaleOf <= 0 ? -1 : Math.round(100f * level / scaleOf);
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

    /** A touch, in the view's pixels, is looked for among the windows, which are kept in the canvas's dp. */
    private void press(float x, float y) {
        float cx = x / scale;
        float cy = y / scale;
        RectF[] boxes = {dialBox, timeBox, weatherBox, earsBox, chargeBox};
        String[] names = {Almanac.DIAL, Almanac.TIME, Almanac.WEATHER, Almanac.EARS, Almanac.CHARGE};
        for (int i = 0; i < boxes.length; i++) {
            if (!boxes[i].isEmpty() && boxes[i].contains(cx, cy)) {
                RectF out = new RectF(boxes[i].left * scale, boxes[i].top * scale, boxes[i].right * scale,
                    boxes[i].bottom * scale);
                hand.pressed(names[i], this, out);
                return;
            }
        }
    }

    /**
     * The windows set out on the canvas: the multiplier is the smaller of
     * the box's width and height to the canvas's, and the canvas is then
     * made as large as the box at that multiplier, so one side is the
     * canvas's own and the other has room to spare.
     */
    private void lay(float w, float h) {
        k = Math.min(w / (WIDE_W * density), h / (WIDE_H * density));
        scale = k * density;
        float vw = w / scale;
        float vh = h / scale;
        card.set(BORDER, BORDER, vw - BORDER, vh - BORDER);
        float pad = 10f;
        float left = BORDER + pad;
        float top = BORDER + pad;
        float right = vw - BORDER - pad;
        float bottom = vh - BORDER - pad;
        float timeH = TIME_PAD * 2f + (TIME_SIZE + DATE_SIZE) * LINE;
        float stack = timeH + UNDER + ROW;
        float rowTop;
        /* The dial in a slot of the widget's own share, never much wider
           than it is high, so a long box gives its length to the words. */
        float high = bottom - top;
        float slot = Math.min((right - left - GAP) / 2.1f, high * 1.12f);
        float d = Math.min(slot, high);
        dialBox.set(left + (slot - d) / 2f, top + (high - d) / 2f, left + (slot + d) / 2f, top + (high + d) / 2f);
        float from = left + slot + GAP;
        float at = top + (high - stack) / 2f;
        timeBox.set(from, at, right, at + timeH);
        rowTop = timeBox.bottom + UNDER;
        chargeBox.set(right - ROW, rowTop, right, rowTop + ROW);
        float x = chargeBox.left - GAP;
        if (ears >= 0) {
            earsBox.set(x - ROW, rowTop, x, rowTop + ROW);
            x = earsBox.left - GAP;
        } else {
            earsBox.setEmpty();
        }
        if (showWeather && x - from > ROW) {
            weatherBox.set(from, rowTop, x, rowTop + ROW);
        } else {
            weatherBox.setEmpty();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        lay(w, h);
        canvas.save();
        canvas.scale(scale, scale);
        float edge = Math.max(1f, 1f / k) * strength;
        field(canvas, card, 30f, cardAlpha, edge);
        dial(canvas, dialBox);

        field(canvas, timeBox, 22f, FIELD_ALPHA, edge);
        Date now = new Date();
        String time = new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(getContext())
            ? "HH:mm" : "hh:mm", Locale.getDefault()).format(now);
        String date = new SimpleDateFormat("EE, d MMMM", Locale.getDefault()).format(now);
        words.setColor(INK);
        words.setTextAlign(Paint.Align.CENTER);
        words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        words.setTextSize(fit(time, timeBox.width() - 28f, TIME_SIZE));
        Paint.FontMetrics f = words.getFontMetrics();
        float line1 = timeBox.top + TIME_PAD - f.ascent;
        canvas.drawText(time, timeBox.centerX(), line1, words);
        words.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        words.setTextSize(fit(date, timeBox.width() - 28f, DATE_SIZE));
        Paint.FontMetrics g = words.getFontMetrics();
        canvas.drawText(date, timeBox.centerX(), line1 + f.descent - g.ascent, words);

        if (!weatherBox.isEmpty()) {
            field(canvas, weatherBox, ROW / 2f, FIELD_ALPHA, edge);
            sky(canvas, weatherBox);
        }
        if (!earsBox.isEmpty()) {
            circle(canvas, earsBox, ears + "%", edge);
        }
        circle(canvas, chargeBox, charge >= 0 ? charge + "%" : "\u2026", edge);
        canvas.restore();
    }

    /** A box of the widget's own drawing: a fill of its dark glass, a light edge. */
    private void field(Canvas canvas, RectF box, float radius, int alpha, float edge) {
        float r = Math.min(radius, Math.min(box.width(), box.height()) / 2f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((alpha << 24) | GLASS);
        canvas.drawRoundRect(box, r, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(edge);
        paint.setColor((Math.min(255, Math.round(EDGE_ALPHA * strength)) << 24) | 0xFFFFFF);
        canvas.drawRoundRect(box, r, r, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * The dial in the widget's own geometry, a square of six hundred units
     * about its middle: the ring, bars at the quarters, dots at the other
     * hours, and the hands — none thinner than the thinnest line.
     */
    private void dial(Canvas canvas, RectF box) {
        float d = box.width();
        if (d <= 0f) {
            return;
        }
        float u = d / 600f;
        float cx = box.centerX();
        float cy = box.centerY();
        float thin = THINNEST / k;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(Math.max(3f * u, thin) * strength);
        paint.setColor((Math.min(255, Math.round(EDGE_ALPHA * strength)) << 24) | 0xFFFFFF);
        canvas.drawCircle(cx, cy, 268.5f * u, paint);

        paint.setColor(MARK);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(15f * u, thin));
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            float sin = (float) Math.sin(a);
            float cos = (float) Math.cos(a);
            if (i % 3 == 0) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(cx + 221.5f * u * sin, cy - 221.5f * u * cos,
                    cx + 242.5f * u * sin, cy - 242.5f * u * cos, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx + 234f * u * sin, cy - 234f * u * cos, Math.max(9.5f * u, thin * 0.75f), paint);
            }
        }

        Calendar now = Calendar.getInstance();
        float sec = now.get(Calendar.SECOND);
        float min = now.get(Calendar.MINUTE) + sec / 60f;
        float hour = now.get(Calendar.HOUR) + min / 60f;
        paint.setStyle(Paint.Style.FILL);
        bar(canvas, cx, cy, hour * 30f, Math.max(43f * u, thin), 150f * u, 16f * u, 21.5f * u, HOUR);
        bar(canvas, cx, cy, min * 6f, Math.max(27f * u, thin), 215f * u, 16f * u, 10f * u, MINUTE);

        canvas.save();
        canvas.rotate(sec * 6f, cx, cy);
        paint.setColor(second);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(9f * u, thin));
        canvas.drawLine(cx, cy - 250f * u, cx, cy + 55f * u, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, Math.max(16.5f * u, thin * 1.4f), paint);
        canvas.restore();
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    /** A hand: a bar of a width from its tip past the middle to its tail, its ends rounded. */
    private void bar(Canvas canvas, float cx, float cy, float degrees, float width, float tip, float tail,
                     float round, int colour) {
        canvas.save();
        canvas.rotate(degrees, cx, cy);
        paint.setColor(colour);
        spare.set(cx - width / 2f, cy - tip, cx + width / 2f, cy + tail);
        float r = Math.min(round, width / 2f);
        canvas.drawRoundRect(spare, r, r, paint);
        canvas.restore();
    }

    private void sky(Canvas canvas, RectF box) {
        String text = Sky.degrees() != Sky.MISSING ? Sky.degrees() + "\u00B0" : "\u2026";
        words.setTypeface(Typeface.DEFAULT);
        words.setTextAlign(Paint.Align.LEFT);
        words.setColor(INK);
        words.setTextSize(fit(text, box.width() * 0.5f, 16f));
        /* The sky's drawing shrinks before it goes: only where not even a
        half-size one fits beside the warmth is it left out. */
        float icon = Math.min(26f, box.width() - 24f - 7f - words.measureText(text));
        if (icon < 13f) {
            icon = 0f;
        }
        float all = icon > 0f ? icon + 7f + words.measureText(text) : words.measureText(text);
        float start = box.centerX() - all / 2f;
        Bitmap picture = icon > 0f ? sky(Sky.sky()) : null;
        if (picture != null) {
            spare.set(start, box.centerY() - icon / 2f, start + icon, box.centerY() + icon / 2f);
            canvas.drawBitmap(picture, null, spare, paint);
        }
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + (icon > 0f ? icon + 7f : 0f), box.centerY() - (f.ascent + f.descent) / 2f, words);
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

    private void circle(Canvas canvas, RectF box, String text, float edge) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((FIELD_ALPHA << 24) | GLASS);
        canvas.drawOval(box, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(edge);
        paint.setColor((Math.min(255, Math.round(EDGE_ALPHA * strength)) << 24) | 0xFFFFFF);
        canvas.drawOval(box, paint);
        paint.setStyle(Paint.Style.FILL);
        words.setTypeface(Typeface.DEFAULT);
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(INK);
        words.setTextSize(fit(text, box.width() * 0.78f, 15f));
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, box.centerX(), box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    private float fit(String text, float width, float size) {
        words.setTextSize(size);
        float measured = words.measureText(text);
        return measured > width && measured > 0f ? size * width / measured : size;
    }
}
