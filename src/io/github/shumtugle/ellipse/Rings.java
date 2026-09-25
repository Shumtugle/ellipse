package io.github.shumtugle.ellipse;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
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
 * Rings of glass with no card under them: a big one with the hour, in
 * figures or in hands, and an orange arc of the day gone; beside it small
 * ones, each holding what the settings chose — the place's warmth, the
 * calendar's day, the charge — and, while headphones are connected, one
 * more with theirs. An arc whose length is a level is drawn in the accent.
 *
 * The rings stand on a canvas of their own measured in dp, brought to the
 * box by one multiplier, as the widget clock is: a wide canvas, or, in a
 * narrow box, a tall one. Where the owner has not set them, they stand in
 * a chain; set by hand, each keeps its place as a share of the canvas and
 * its size in the canvas's dp, so a larger box spreads them as it grows.
 *
 * Set by hand: from the clock's menu the rings are taken into the owner's
 * hands. A finger moves a ring, two fingers size it, a double tap sends it
 * back to its place in the chain; a ring never leaves the box.
 */
final class Rings extends View implements Timepiece {

    static final float WIDE_W = 360f;
    static final float WIDE_H = 170f;
    static final float TALL_W = 200f;
    static final float TALL_H = 300f;
    static final float WIDE_FROM = 1.5f;
    static final float LEAST = 0.6f;

    /** What the big ring holds. */
    static final int FIGURES = 0;
    static final int HANDS = 1;
    static final String[] BIG_NAMES = {"Figures", "Hands"};
    /** What a small ring holds. */
    static final int CITY = 0;
    static final int CALENDAR = 1;
    static final int CHARGE = 2;
    static final String[] SMALL_NAMES = {"Weather", "Calendar", "Charge"};

    /** The rings by their places: the big one, the two chosen, the headphones'. */
    private static final int BIG = 0;
    private static final int FIRST = 1;
    private static final int SECOND = 2;
    private static final int EARS = 3;
    private static final int COUNT = 4;

    private static final int INK = 0xFFF5F1E8;
    private static final int QUIET = 0xFFD8D2C6;
    private static final int ORANGE = 0xFFF29A4A;
    private static final int GLASS = 0x0E1014;

    private final Almanac.Hand hand;
    private final float density;
    private final GestureDetector taps;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    private final int big;
    private final int[] kinds = new int[COUNT];
    private final int ground;
    private final int accent;
    /** Where each ring was set by hand: a share across, a share down, a radius in dp; or none. */
    private final float[][] placed = new float[COUNT][];
    /** Where each ring stands as last drawn, in the canvas's dp: x, y, radius; or none. */
    private final float[][] stands = new float[COUNT][];
    private float scale = 1f;
    private float k = 1f;
    private float vw;
    private float vh;
    private boolean showWeather = true;
    private int ears = -1;
    private int charge = -1;
    private boolean seen;

    /* Setting the rings by hand. */
    private boolean arranging;
    private int held = -1;
    private float heldDx;
    private float heldDy;
    private float pinchFrom;
    private float pinchRadius;

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

    Rings(Context context, Almanac.Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        big = Keep.number(context, Keep.RINGS_BIG, FIGURES) == HANDS ? HANDS : FIGURES;
        kinds[FIRST] = Math.max(0, Math.min(CHARGE, Keep.number(context, Keep.RINGS_FIRST, CITY)));
        kinds[SECOND] = Math.max(0, Math.min(CHARGE, Keep.number(context, Keep.RINGS_SECOND, CHARGE)));
        ground = Math.round(255f * Math.max(0, Math.min(95, Keep.number(context, Keep.CLOCK_GROUND, 30))) / 100f);
        accent = Tone.primary();
        read(Keep.rings(context));
        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!arranging) {
                    press(e.getX(), e.getY());
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (arranging) {
                    int ring = ringAt(e.getX() / scale, e.getY() / scale);
                    if (ring >= 0) {
                        placed[ring] = null;
                        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                        invalidate();
                    }
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (!arranging) {
                    performLongClick();
                }
            }
        });
    }

    /** The least box, in dp, the rings are drawn in at their smallest multiplier. */
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

    /** The rings taken into the owner's hands, or given back; given back, where they stand is kept. */
    void arrange(boolean on) {
        if (arranging && !on) {
            Keep.saveRings(getContext(), write());
        }
        arranging = on;
        held = -1;
        invalidate();
    }

    boolean arranging() {
        return arranging;
    }

    /** Places as written: one ring a line, its number, its share across, its share down, its radius. */
    private void read(String text) {
        for (String line : text.split(";")) {
            String[] part = line.split(":");
            if (part.length != 4) {
                continue;
            }
            try {
                int ring = Integer.parseInt(part[0]);
                if (ring >= 0 && ring < COUNT) {
                    placed[ring] = new float[] {Float.parseFloat(part[1]), Float.parseFloat(part[2]),
                        Float.parseFloat(part[3])};
                }
            } catch (NumberFormatException broken) {
                // That ring stands in the chain, then.
            }
        }
    }

    private String write() {
        StringBuilder out = new StringBuilder();
        for (int ring = 0; ring < COUNT; ring++) {
            if (placed[ring] != null) {
                if (out.length() > 0) {
                    out.append(';');
                }
                out.append(ring).append(':').append(placed[ring][0]).append(':').append(placed[ring][1])
                    .append(':').append(placed[ring][2]);
            }
        }
        return out.toString();
    }

    private void read(Intent state) {
        if (state == null) {
            return;
        }
        int level = state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int of = state.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        charge = level < 0 || of <= 0 ? -1 : Math.round(100f * level / of);
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

    // ------------------------------------------------------------ placing

    /** The canvas for the box and the rings on it: set by hand, or in the chain. */
    private void lay(float w, float h) {
        boolean wide = w >= h * WIDE_FROM;
        float w0 = wide ? WIDE_W : TALL_W;
        float h0 = wide ? WIDE_H : TALL_H;
        k = Math.min(w / (w0 * density), h / (h0 * density));
        scale = k * density;
        vw = w / scale;
        vh = h / scale;
        float[][] chain = chain(wide);
        for (int ring = 0; ring < COUNT; ring++) {
            if (ring == EARS && ears < 0) {
                stands[ring] = null;
                continue;
            }
            float[] at = placed[ring] != null
                ? new float[] {placed[ring][0] * vw, placed[ring][1] * vh, placed[ring][2]} : chain[ring];
            stands[ring] = keepIn(at);
        }
    }

    /** A ring kept whole within the box: no larger than it, and pushed back in from its edges. */
    private float[] keepIn(float[] at) {
        float r = Math.min(at[2], Math.min(vw, vh) / 2f);
        return new float[] {Math.max(r, Math.min(vw - r, at[0])), Math.max(r, Math.min(vh - r, at[1])), r};
    }

    /**
     * The rings in the chain: the big one, then the small ones zig-zag
     * beside it, each over the one before; beside it in a wide box, under
     * it in a narrow one. Three small ones stand closer and a little less.
     */
    private float[][] chain(boolean wide) {
        int n = ears >= 0 ? 3 : 2;
        float[][] at = new float[COUNT][];
        int[] order = {FIRST, SECOND, EARS};
        if (wide) {
            float bigD = 160f;
            float d = n <= 2 ? 100f : 86f;
            float lap = n <= 2 ? 24f : 20f;
            float step = d - (n <= 2 ? 14f : 24f);
            float rise = n <= 2 ? 26f : 22f;
            float span = bigD + (d - lap) + step * (n - 1);
            float x = (vw - span) / 2f + bigD / 2f;
            float cy = vh / 2f;
            at[BIG] = new float[] {x, cy, bigD / 2f};
            float cx = x + bigD / 2f + d / 2f - lap;
            for (int i = 0; i < n; i++) {
                at[order[i]] = new float[] {cx, cy + (i % 2 == 0 ? -rise : rise * 0.85f), d / 2f};
                cx += step;
            }
        } else {
            float bigD = 170f;
            float d = n <= 2 ? 96f : 74f;
            float step = d - (n <= 2 ? 20f : 12f);
            float span = d + step * (n - 1);
            float top = (vh - (bigD + d - 18f)) / 2f;
            at[BIG] = new float[] {vw / 2f, top + bigD / 2f, bigD / 2f};
            float cx = (vw - span) / 2f + d / 2f;
            for (int i = 0; i < n; i++) {
                at[order[i]] = new float[] {cx, top + bigD - 18f + d / 2f + (i % 2 == 0 ? 0f : 8f), d / 2f};
                cx += step;
            }
        }
        if (at[EARS] == null) {
            at[EARS] = new float[] {vw / 2f, vh / 2f, 40f};
        }
        return at;
    }

    /** The ring on top under a point of the canvas, or none. */
    private int ringAt(float x, float y) {
        int[] order = drawOrder();
        for (int i = order.length - 1; i >= 0; i--) {
            float[] at = stands[order[i]];
            if (at != null && Math.hypot(x - at[0], y - at[1]) <= at[2]) {
                return order[i];
            }
        }
        return -1;
    }

    /** The rings bottom to top: the one held last of all. */
    private int[] drawOrder() {
        int[] order = {BIG, FIRST, SECOND, EARS};
        if (held >= 0) {
            int at = 0;
            for (int ring : new int[] {BIG, FIRST, SECOND, EARS}) {
                if (ring != held) {
                    order[at++] = ring;
                }
            }
            order[COUNT - 1] = held;
        }
        return order;
    }

    // ------------------------------------------------------------ touch

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!arranging) {
            return taps.onTouchEvent(event) || super.onTouchEvent(event);
        }
        taps.onTouchEvent(event);
        float x = event.getX() / scale;
        float y = event.getY() / scale;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                held = ringAt(x, y);
                if (held >= 0) {
                    heldDx = stands[held][0] - x;
                    heldDy = stands[held][1] - y;
                    performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (held < 0 && event.getPointerCount() == 2) {
                    held = ringAt((event.getX(0) + event.getX(1)) / 2f / scale,
                        (event.getY(0) + event.getY(1)) / 2f / scale);
                }
                if (held >= 0 && event.getPointerCount() == 2) {
                    pinchFrom = spread(event);
                    pinchRadius = stands[held][2];
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (held < 0) {
                    return true;
                }
                float[] at = stands[held].clone();
                if (event.getPointerCount() >= 2 && pinchFrom > 0f) {
                    float least = held == BIG ? 56f : 28f;
                    at[2] = Math.max(least, Math.min(Math.min(vw, vh) / 2f, pinchRadius * spread(event) / pinchFrom));
                } else if (event.getPointerCount() == 1) {
                    at[0] = x + heldDx;
                    at[1] = y + heldDy;
                }
                at = keepIn(at);
                stands[held] = at;
                placed[held] = new float[] {at[0] / vw, at[1] / vh, at[2]};
                invalidate();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                pinchFrom = 0f;
                if (held >= 0) {
                    /* The finger left on the ring carries on from where it is. */
                    int stay = event.getActionIndex() == 0 ? 1 : 0;
                    heldDx = stands[held][0] - event.getX(stay) / scale;
                    heldDy = stands[held][1] - event.getY(stay) / scale;
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                held = -1;
                pinchFrom = 0f;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private float spread(MotionEvent event) {
        return (float) Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1)) / scale;
    }

    /** A tap on a ring opens what it tells of. */
    private void press(float x, float y) {
        int ring = ringAt(x / scale, y / scale);
        if (ring < 0) {
            return;
        }
        String window;
        if (ring == BIG) {
            window = big == HANDS ? Almanac.DIAL : Almanac.TIME;
        } else if (ring == EARS) {
            window = Almanac.EARS;
        } else if (kinds[ring] == CITY) {
            window = Almanac.WEATHER;
        } else if (kinds[ring] == CALENDAR) {
            window = Almanac.TIME;
        } else {
            window = Almanac.CHARGE;
        }
        float[] at = stands[ring];
        hand.pressed(window, this, new RectF((at[0] - at[2]) * scale, (at[1] - at[2]) * scale,
            (at[0] + at[2]) * scale, (at[1] + at[2]) * scale));
    }

    // ------------------------------------------------------------ drawing

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
        if (arranging) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f / k);
            paint.setColor(0x80FFFFFF);
            paint.setPathEffect(new DashPathEffect(new float[] {6f / k, 5f / k}, 0f));
            float in = 1f / k;
            canvas.drawRoundRect(in, in, vw - in, vh - in, 18f, 18f, paint);
            paint.setPathEffect(null);
        }
        for (int ring : drawOrder()) {
            float[] at = stands[ring];
            if (at == null) {
                continue;
            }
            glass(canvas, at, ring == held);
            if (ring == BIG) {
                arc(canvas, at, dayGone(), ORANGE, 5f, 4f);
                if (big == HANDS) {
                    hands(canvas, at);
                } else {
                    figures(canvas, at);
                }
            } else if (ring == EARS) {
                arc(canvas, at, ears / 100f, accent, 5f, 3.5f);
                phones(canvas, at[0], at[1] - at[2] * 0.38f, at[2] * 0.34f);
                text(canvas, ears + "%", at[0], at[1] + at[2] * 0.34f, at[2] * 0.32f, INK, at[2] * 1.5f);
            } else {
                small(canvas, at, kinds[ring]);
            }
        }
        canvas.restore();
    }

    private float dayGone() {
        Calendar now = Calendar.getInstance();
        return (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)) / 1440f;
    }

    /** A ring of glass: dark ground, a light that falls from the upper left, a fine rim, a gleam. */
    private void glass(Canvas canvas, float[] at, boolean lifted) {
        float x = at[0];
        float y = at[1];
        float r = at[2];
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((ground << 24) | GLASS);
        canvas.drawCircle(x, y, r, paint);
        paint.setShader(new RadialGradient(x - r * 0.3f, y - r * 0.5f, r * 1.8f,
            new int[] {0x33FFFFFF, 0x0DFFFFFF}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, r, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.2f, 1.5f / k) * (lifted ? 1.6f : 1f));
        paint.setColor(lifted ? 0xE6FFFFFF : 0x8CFFFFFF);
        canvas.drawCircle(x, y, r, paint);
        paint.setStrokeWidth(Math.max(2.5f, 1.5f / k));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0x59FFFFFF);
        oval.set(x - r * 0.9f, y - r * 0.9f, x + r * 0.9f, y + r * 0.9f);
        canvas.drawArc(oval, 212f, 58f, false, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    /** An arc along the rim, from the top clockwise, as long as the share told, over a faint track. */
    private void arc(Canvas canvas, float[] at, float share, int colour, float inset, float width) {
        float r = at[2] - inset;
        oval.set(at[0] - r, at[1] - r, at[0] + r, at[1] + r);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(width, 1.5f / k));
        paint.setColor(0x1FFFFFFF);
        canvas.drawCircle(at[0], at[1], r, paint);
        if (share > 0f) {
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(colour);
            canvas.drawArc(oval, -90f, 360f * Math.min(1f, share), false, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** The big ring in figures: the weather above, the hour, the date, the next alarm in orange. */
    private void figures(Canvas canvas, float[] at) {
        float x = at[0];
        float y = at[1];
        float r = at[2];
        float room = r * 1.5f;
        if (showWeather && Sky.degrees() != Sky.MISSING) {
            String warm = Sky.degrees() + "\u00B0";
            float size = r * 0.15f;
            words.setTypeface(Typeface.DEFAULT);
            words.setTextSize(size);
            float icon = size * 1.3f;
            float all = icon + size * 0.4f + words.measureText(warm);
            float start = x - all / 2f;
            Bitmap sky = sky(Sky.sky());
            if (sky != null) {
                oval.set(start, y - r * 0.42f - icon * 0.78f, start + icon, y - r * 0.42f + icon * 0.22f);
                canvas.drawBitmap(sky, null, oval, paint);
            }
            words.setTextAlign(Paint.Align.LEFT);
            words.setColor(QUIET);
            canvas.drawText(warm, start + icon + size * 0.4f, y - r * 0.42f, words);
        }
        String hour = android.text.format.DateFormat.getTimeFormat(getContext()).format(new Date());
        words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        text(canvas, hour, x, y + r * 0.14f, r * 0.42f, INK, room);
        words.setTypeface(Typeface.DEFAULT);
        String day = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(
            Locale.getDefault(), "EEEdMMMM"), Locale.getDefault()).format(new Date());
        text(canvas, day, x, y + r * 0.36f, r * 0.14f, QUIET, room);
        long next = nextAlarm();
        if (next > 0L) {
            String when = android.text.format.DateFormat.getTimeFormat(getContext()).format(new Date(next));
            float size = r * 0.14f;
            words.setTextSize(size);
            float icon = size;
            float all = icon + size * 0.45f + words.measureText(when);
            float start = x - all / 2f;
            bell(canvas, start + icon / 2f, y + r * 0.63f - size * 0.36f, icon);
            words.setTextAlign(Paint.Align.LEFT);
            words.setColor(ORANGE);
            canvas.drawText(when, start + icon + size * 0.45f, y + r * 0.63f, words);
        }
    }

    private long nextAlarm() {
        AlarmManager alarms = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo next = alarms == null ? null : alarms.getNextAlarmClock();
        return next == null ? 0L : next.getTriggerTime();
    }

    /** A small alarm clock: a round face with its hands, in orange. */
    private void bell(Canvas canvas, float x, float y, float size) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(ORANGE);
        paint.setStrokeWidth(size * 0.14f);
        canvas.drawCircle(x, y, size * 0.5f, paint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(size * 0.12f);
        path.reset();
        path.moveTo(x, y - size * 0.3f);
        path.lineTo(x, y);
        path.lineTo(x + size * 0.22f, y + size * 0.12f);
        canvas.drawPath(path, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    /** The big ring in hands: marks at the hours, the hour and minute in ink, the seconds in orange. */
    private void hands(Canvas canvas, float[] at) {
        float x = at[0];
        float y = at[1];
        float r = at[2];
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(QUIET);
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            float sin = (float) Math.sin(a);
            float cos = (float) Math.cos(a);
            float from = r * (i % 3 == 0 ? 0.70f : 0.76f);
            paint.setStrokeWidth(Math.max(r * (i % 3 == 0 ? 0.035f : 0.02f), 1.5f / k));
            canvas.drawLine(x + from * sin, y - from * cos, x + r * 0.82f * sin, y - r * 0.82f * cos, paint);
        }
        Calendar now = Calendar.getInstance();
        float sec = now.get(Calendar.SECOND);
        float min = now.get(Calendar.MINUTE) + sec / 60f;
        float hour = now.get(Calendar.HOUR) + min / 60f;
        hand(canvas, x, y, hour * 30f, r * 0.45f, r * 0.07f, INK);
        hand(canvas, x, y, min * 6f, r * 0.66f, r * 0.045f, INK);
        hand(canvas, x, y, sec * 6f, r * 0.70f, Math.max(r * 0.018f, 1.5f / k), ORANGE);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ORANGE);
        canvas.drawCircle(x, y, r * 0.04f, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void hand(Canvas canvas, float x, float y, float degrees, float length, float width, int colour) {
        double a = Math.toRadians(degrees);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(colour);
        canvas.drawLine(x, y, x + length * (float) Math.sin(a), y - length * (float) Math.cos(a), paint);
    }

    /** A small ring by what it holds. */
    private void small(Canvas canvas, float[] at, int kind) {
        float x = at[0];
        float y = at[1];
        float r = at[2];
        float room = r * 1.55f;
        words.setTypeface(Typeface.DEFAULT);
        if (kind == CITY) {
            String place = Sky.place();
            text(canvas, place == null || place.isEmpty() ? "\u2026" : place, x, y - r * 0.08f, r * 0.22f, QUIET,
                room);
            String span = Sky.days() > 0 ? Sky.high(0) + "\u00B0 \u00B7 " + Sky.low(0) + "\u00B0"
                : Sky.degrees() != Sky.MISSING ? Sky.degrees() + "\u00B0" : "\u2026";
            text(canvas, span, x, y + r * 0.3f, r * 0.3f, INK, room);
        } else if (kind == CALENDAR) {
            Date now = new Date();
            text(canvas, new SimpleDateFormat("EE", Locale.getDefault()).format(now), x, y - r * 0.12f,
                r * 0.24f, ORANGE, room);
            text(canvas, new SimpleDateFormat("d", Locale.getDefault()).format(now), x, y + r * 0.36f,
                r * 0.5f, INK, room);
        } else {
            arc(canvas, at, charge < 0 ? 0f : charge / 100f, accent, 5f, 3.5f);
            text(canvas, charge >= 0 ? charge + "%" : "\u2026", x, y + r * 0.13f, r * 0.36f, INK, room);
        }
    }

    /** Headphones: a band and two cups. */
    private void phones(Canvas canvas, float x, float y, float size) {
        paint.setColor(QUIET);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(size * 0.12f);
        path.reset();
        path.moveTo(x - size * 0.45f, y + size * 0.2f);
        path.lineTo(x - size * 0.45f, y);
        oval.set(x - size * 0.45f, y - size * 0.45f, x + size * 0.45f, y + size * 0.45f);
        path.arcTo(oval, 180f, 180f, false);
        path.lineTo(x + size * 0.45f, y + size * 0.2f);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
        oval.set(x - size * 0.52f, y + size * 0.05f, x - size * 0.28f, y + size * 0.41f);
        canvas.drawRoundRect(oval, size * 0.08f, size * 0.08f, paint);
        oval.set(x + size * 0.28f, y + size * 0.05f, x + size * 0.52f, y + size * 0.41f);
        canvas.drawRoundRect(oval, size * 0.08f, size * 0.08f, paint);
    }

    /** Words centred at a baseline, at a size, made smaller only if they would not fit the room. */
    private void text(Canvas canvas, String said, float x, float y, float size, int colour, float room) {
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(colour);
        words.setTextSize(size);
        float measured = words.measureText(said);
        if (measured > room && measured > 0f) {
            words.setTextSize(size * room / measured);
        }
        canvas.drawText(said, x, y, words);
    }

    private Bitmap skyPicture;
    private int skyKind = -1;

    /** The sky's drawing, the widget clock's own. */
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
}
