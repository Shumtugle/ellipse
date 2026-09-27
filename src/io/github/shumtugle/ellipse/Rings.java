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
 * figures or in hands, and an arc of the day gone; beside it up to
 * seven small ones, each holding what the settings chose — the place's
 * warmth, the calendar's day, the charge, how warm it feels, the damp, the
 * wind, the daylight, the rain, the next alarm — and, while headphones are
 * connected, one more with theirs. The arcs that are levels, the seconds
 * hand, and the day's arc with the daylight and the alarm each take a
 * colour of their own: the accent, orange and orange, unless chosen.
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
    static final int FEELS = 3;
    static final int DAMP = 4;
    static final int WIND = 5;
    static final int DAYLIGHT = 6;
    static final int RAIN = 7;
    static final int ALARM = 8;
    static final int TIME = 9;
    static final int MEMORY = 10;
    static final int SUN_BURN = 11;
    static final int PRESSURE = 12;
    static final int PLAYER = 13;
    static final String[] SMALL_NAMES = {"Time", "Weather", "Calendar", "Charge", "Feels like", "Humidity",
        "Wind", "Daylight", "Rain", "Alarm", "Free memory", "Ultraviolet", "Pressure", "Player"};
    static final int[] SMALL_KINDS = {TIME, CITY, CALENDAR, CHARGE, FEELS, DAMP, WIND, DAYLIGHT, RAIN, ALARM, MEMORY,
        SUN_BURN, PRESSURE, PLAYER};

    /** The big ring is the first; the small ones follow in order; the headphones' is the last. */
    private static final int BIG = 0;

    private static final int INK = 0xFFF5F1E8;
    private static final int QUIET = 0xFFD8D2C6;
    /**
     * The colours the rings' marks may take: the accent of the launcher,
     * then orange, red, lilac, blue, green, sand and white.
     */
    static final int[] COLOURS = {0, 0xFFF29A4A, 0xFFD2464E, 0xFFC3A2D6, 0xFF7AA7F0, 0xFF8FBF8A, 0xFFD4BFA3,
        0xFFF5F1E8};
    static final String[] COLOUR_NAMES = {"Accent", "Orange", "Red", "Lilac", "Blue", "Green", "Sand", "White"};
    static final int ACCENT = 0;
    static final int ORANGE_ONE = 1;

    /** A colour by its place in the list; the first is the launcher's accent. */
    static int colour(int which) {
        return which <= 0 || which >= COLOURS.length ? Tone.primary() : COLOURS[which];
    }
    private static final int GLASS = 0x0E1014;

    private final Almanac.Hand hand;
    private final float density;
    private final GestureDetector taps;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    private final int big;
    /** The small rings' own numbers and what each holds, in order. */
    private final int[] ids;
    private final int[] kinds;
    /** The headphones' ring's place among all. */
    private final int earsRing;
    private final int count;
    private final int ground;
    /** The arcs that are levels; the seconds hand; the day's arc, the daylight, the alarm. */
    private final int accent;
    private final int seconds;
    private final int daily;
    /** Where each ring was set by hand: a share across, a share down, a radius in dp; or none. */
    private final float[][] placed;
    /** Where each ring stands as last drawn, in the canvas's dp: x, y, radius; or none. */
    private final float[][] stands;
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
        ids = Keep.ringIds(context);
        count = ids.length + 2;
        earsRing = count - 1;
        kinds = new int[count];
        for (int i = 0; i < ids.length; i++) {
            kinds[i + 1] = Math.max(CITY, Math.min(PLAYER, Keep.number(context, Keep.RING_KIND + ids[i], CITY)));
        }
        placed = new float[count][];
        stands = new float[count][];
        ground = Math.round(255f * Math.max(0, Math.min(95, Keep.number(context, Keep.CLOCK_GROUND, 30))) / 100f);
        accent = colour(Keep.number(context, Keep.RINGS_LEVEL, ACCENT));
        seconds = colour(Keep.number(context, Keep.RINGS_SECONDS, ORANGE_ONE));
        daily = colour(Keep.number(context, Keep.RINGS_DAY, ORANGE_ONE));
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

    /** A ring's word in what is kept: the big one's, a small one's own number, the headphones'. */
    private String word(int ring) {
        return ring == BIG ? "0" : ring == earsRing ? "e" : String.valueOf(ids[ring - 1]);
    }

    /**
     * Places as written: one ring a line, its word, its share across, its
     * share down, its radius. A ring no longer there keeps its line, so it
     * comes back to its place if it is added again.
     */
    private String others = "";

    private void read(String text) {
        StringBuilder rest = new StringBuilder();
        for (String line : text.split(";")) {
            String[] part = line.split(":");
            if (part.length != 4) {
                continue;
            }
            int ring = -1;
            for (int r = 0; r < count; r++) {
                if (word(r).equals(part[0])) {
                    ring = r;
                }
            }
            try {
                float[] at = {Float.parseFloat(part[1]), Float.parseFloat(part[2]), Float.parseFloat(part[3])};
                if (ring >= 0) {
                    placed[ring] = at;
                } else {
                    rest.append(rest.length() > 0 ? ";" : "").append(line);
                }
            } catch (NumberFormatException broken) {
                // That ring stands in the chain, then.
            }
        }
        others = rest.toString();
    }

    private String write() {
        StringBuilder out = new StringBuilder(others);
        for (int ring = 0; ring < count; ring++) {
            if (placed[ring] != null) {
                if (out.length() > 0) {
                    out.append(';');
                }
                out.append(word(ring)).append(':').append(placed[ring][0]).append(':').append(placed[ring][1])
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
        for (int ring = 0; ring < count; ring++) {
            if (ring == earsRing && ears < 0) {
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
     * it in a narrow one. The more there are, the closer and smaller they
     * stand, so the chain keeps within the box.
     */
    private float[][] chain(boolean wide) {
        int n = count - 2 + (ears >= 0 ? 1 : 0);
        float[][] at = new float[count][];
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i + 1 < earsRing ? i + 1 : earsRing;
        }
        if (n >= 4) {
            /* Many: the small ones in two rows, each over its neighbours,
               the second row half a step along, like cells of a comb. */
            int columns = (n + 1) / 2;
            if (wide) {
                float bigD = 160f;
                float room = vw - 8f - bigD * 0.9f;
                float d = Math.min(86f, Math.min(vh * 0.56f, room / (0.72f * columns + 0.64f)));
                float step = d * 0.72f;
                float span = bigD * 0.9f + step * columns + d * 0.64f;
                float x = Math.max(bigD / 2f, (vw - span) / 2f + bigD / 2f);
                float cy = vh / 2f;
                at[BIG] = new float[] {x, cy, bigD / 2f};
                float start = x + bigD * 0.4f + d / 2f;
                for (int i = 0; i < n; i++) {
                    boolean upper = i % 2 == 0;
                    at[order[i]] = new float[] {start + (i / 2) * step + (upper ? 0f : step / 2f),
                        cy + (upper ? -d * 0.38f : d * 0.38f), d / 2f};
                }
            } else {
                float bigD = 170f;
                float d = Math.min(74f, (vw - 8f) / (0.72f * columns + 0.64f));
                float step = d * 0.72f;
                float span = step * columns + d * 0.64f;
                float top = (vh - (bigD + d * 1.6f - 18f)) / 2f;
                at[BIG] = new float[] {vw / 2f, top + bigD / 2f, bigD / 2f};
                float start = (vw - span) / 2f + d / 2f;
                for (int i = 0; i < n; i++) {
                    boolean upper = i % 2 == 0;
                    at[order[i]] = new float[] {start + (i / 2) * step + (upper ? 0f : step / 2f),
                        top + bigD - 18f + d / 2f + (upper ? 0f : d * 0.6f), d / 2f};
                }
            }
        } else if (wide) {
            float bigD = 160f;
            float d = n <= 2 ? 100f : 86f;
            float lap = n <= 2 ? 24f : 20f;
            float step = n <= 2 ? d - 14f : d - 24f;
            float rise = n <= 2 ? 26f : 22f;
            float span = bigD + (d - lap) + step * (n - 1);
            float x = Math.max(bigD / 2f, (vw - span) / 2f + bigD / 2f);
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
            float step = n <= 2 ? d - 20f : d - 12f;
            float span = d + step * (n - 1);
            float top = (vh - (bigD + d - 18f)) / 2f;
            at[BIG] = new float[] {vw / 2f, top + bigD / 2f, bigD / 2f};
            float cx = (vw - span) / 2f + d / 2f;
            for (int i = 0; i < n; i++) {
                at[order[i]] = new float[] {cx, top + bigD - 18f + d / 2f + (i % 2 == 0 ? 0f : 8f), d / 2f};
                cx += step;
            }
        }
        for (int ring = 0; ring < count; ring++) {
            if (at[ring] == null) {
                at[ring] = new float[] {vw / 2f, vh / 2f, 40f};
            }
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
        int[] order = new int[count];
        int at = 0;
        for (int ring = 0; ring < count; ring++) {
            if (ring != held) {
                order[at++] = ring;
            }
        }
        if (held >= 0) {
            order[count - 1] = held;
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
        } else if (ring == earsRing) {
            window = Almanac.EARS;
        } else if (kinds[ring] == CALENDAR || kinds[ring] == ALARM || kinds[ring] == TIME) {
            window = Almanac.TIME;
        } else if (kinds[ring] == CHARGE || kinds[ring] == MEMORY) {
            window = Almanac.CHARGE;
        } else if (kinds[ring] == PLAYER) {
            window = Almanac.PLAYER;
        } else {
            window = Almanac.WEATHER;
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
                arc(canvas, at, dayGone(), daily, 5f, 4f);
                if (big == HANDS) {
                    hands(canvas, at);
                } else {
                    figures(canvas, at);
                }
            } else if (ring == earsRing) {
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
            words.setColor(daily);
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
        paint.setColor(daily);
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
        hand(canvas, x, y, sec * 6f, r * 0.70f, Math.max(r * 0.018f, 1.5f / k), seconds);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(seconds);
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
                r * 0.24f, daily, room);
            text(canvas, new SimpleDateFormat("d", Locale.getDefault()).format(now), x, y + r * 0.36f,
                r * 0.5f, INK, room);
        } else if (kind == CHARGE) {
            arc(canvas, at, charge < 0 ? 0f : charge / 100f, accent, 5f, 3.5f);
            text(canvas, charge >= 0 ? charge + "%" : "\u2026", x, y + r * 0.13f, r * 0.36f, INK, room);
        } else if (kind == TIME) {
            /* The hour in figures, as the big ring has it, and the day under it. */
            Date now = new Date();
            words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            text(canvas, android.text.format.DateFormat.getTimeFormat(getContext()).format(now), x, y + r * 0.12f,
                r * 0.44f, INK, room);
            words.setTypeface(Typeface.DEFAULT);
            text(canvas, new SimpleDateFormat("EE d", Locale.getDefault()).format(now), x, y + r * 0.48f,
                r * 0.2f, QUIET, room);
        } else if (kind == FEELS) {
            text(canvas, Words.s("feels like"), x, y - r * 0.12f, r * 0.2f, QUIET, room);
            text(canvas, Sky.feels() != Sky.MISSING ? Sky.feels() + "\u00B0" : "\u2026", x, y + r * 0.34f,
                r * 0.42f, INK, room);
        } else if (kind == DAMP) {
            int damp = Sky.wet();
            arc(canvas, at, damp == Sky.MISSING ? 0f : damp / 100f, accent, 5f, 3.5f);
            text(canvas, Words.s("humidity"), x, y - r * 0.12f, r * 0.2f, QUIET, room);
            text(canvas, damp != Sky.MISSING ? damp + "%" : "\u2026", x, y + r * 0.3f, r * 0.34f, INK, room);
        } else if (kind == WIND) {
            if (Sky.whence() != Sky.MISSING) {
                /* An arrow the way the wind goes: it comes from its degrees. */
                arrow(canvas, x, y - r * 0.3f, r * 0.26f, Sky.whence() + 180f);
            }
            text(canvas, Sky.wind() != Sky.MISSING ? Sky.wind() + " " + Words.s("m/s") : "\u2026", x,
                y + r * 0.42f, r * 0.28f, INK, room);
        } else if (kind == DAYLIGHT) {
            daylight(canvas, at);
        } else if (kind == MEMORY) {
            /* How much of the working memory is free now: the ring filled by it. */
            android.app.ActivityManager manager = (android.app.ActivityManager)
                getContext().getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo memory = new android.app.ActivityManager.MemoryInfo();
            int free = -1;
            if (manager != null) {
                manager.getMemoryInfo(memory);
                free = memory.totalMem > 0 ? Math.round(100f * memory.availMem / memory.totalMem) : -1;
            }
            arc(canvas, at, free < 0 ? 0f : free / 100f, accent, 5f, 3.5f);
            text(canvas, "RAM", x, y - r * 0.12f, r * 0.2f, QUIET, room);
            text(canvas, free >= 0 ? free + "%" : "\u2026", x, y + r * 0.3f, r * 0.34f, INK, room);
        } else if (kind == SUN_BURN) {
            /* The sun's burning, on its scale of nought to eleven and more. */
            int burn = Sky.burn();
            arc(canvas, at, burn == Sky.MISSING ? 0f : Math.min(1f, burn / 11f), accent, 5f, 3.5f);
            text(canvas, "UV", x, y - r * 0.12f, r * 0.2f, QUIET, room);
            text(canvas, burn != Sky.MISSING ? String.valueOf(burn) : "\u2026", x, y + r * 0.3f, r * 0.34f, INK,
                room);
        } else if (kind == PRESSURE) {
            /* The air's weight, the ring filled from a deep low to a strong high. */
            int press = Sky.press();
            float part = press == Sky.MISSING ? 0f : Math.max(0f, Math.min(1f, (press - 960f) / 90f));
            arc(canvas, at, part, accent, 5f, 3.5f);
            text(canvas, Words.t("hPa"), x, y - r * 0.12f, r * 0.2f, QUIET, room);
            text(canvas, press != Sky.MISSING ? String.valueOf(press) : "\u2026", x, y + r * 0.3f, r * 0.3f, INK,
                room);
        } else if (kind == PLAYER) {
            player(canvas, at);
        } else if (kind == RAIN) {
            int chance = Sky.rain(0);
            arc(canvas, at, chance == Sky.MISSING ? 0f : chance / 100f, accent, 5f, 3.5f);
            text(canvas, Words.s("rain"), x, y - r * 0.12f, r * 0.2f, QUIET, room);
            text(canvas, chance != Sky.MISSING ? chance + "%" : "\u2026", x, y + r * 0.3f, r * 0.34f, INK, room);
        } else {
            long next = nextAlarm();
            if (next <= 0L) {
                bell(canvas, x, y - r * 0.2f, r * 0.3f);
                text(canvas, "\u2014", x, y + r * 0.42f, r * 0.3f, QUIET, room);
            } else {
                long left = Math.max(0L, next - System.currentTimeMillis());
                arc(canvas, at, Math.min(1f, left / 86400000f), accent, 5f, 3.5f);
                bell(canvas, x, y - r * 0.28f, r * 0.26f);
                long minutes = left / 60000L;
                String said = minutes >= 60 ? (minutes / 60) + " " + Words.s("h") + " " + (minutes % 60) + " "
                    + Words.s("min") : minutes + " " + Words.s("min");
                text(canvas, said, x, y + r * 0.3f, r * 0.24f, INK, room);
            }
        }
    }

    /**
     * The player's ring: what plays now, if the phone lets the home screen
     * see it, the ring filled as far as it has played; a sign to pause or to
     * go on in the middle.
     */
    private void player(Canvas canvas, float[] at) {
        float x = at[0];
        float y = at[1];
        float r = at[2];
        Playing now = Playing.now(getContext());
        if (now != null && now.length > 0) {
            arc(canvas, at, Math.min(1f, now.position / (float) now.length), accent, 5f, 3.5f);
        }
        boolean playing = now != null && now.playing;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        float s = r * 0.22f;
        float cy = y - r * 0.08f;
        if (playing) {
            canvas.drawRoundRect(x - s * 0.9f, cy - s, x - s * 0.25f, cy + s, s * 0.15f, s * 0.15f, paint);
            canvas.drawRoundRect(x + s * 0.25f, cy - s, x + s * 0.9f, cy + s, s * 0.15f, s * 0.15f, paint);
        } else {
            android.graphics.Path play = new android.graphics.Path();
            play.moveTo(x - s * 0.7f, cy - s);
            play.lineTo(x + s, cy);
            play.lineTo(x - s * 0.7f, cy + s);
            play.close();
            canvas.drawPath(play, paint);
        }
        String title = now == null || now.title == null ? "" : now.title;
        if (!title.isEmpty()) {
            text(canvas, title, x, y + r * 0.46f, r * 0.17f, QUIET, r * 1.4f);
        }
    }

    /** An arrow of the wind, pointing the way it blows. */
    private void arrow(Canvas canvas, float x, float y, float size, float degrees) {
        canvas.save();
        canvas.rotate(degrees, x, y);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(QUIET);
        path.reset();
        path.moveTo(x, y - size);
        path.lineTo(x + size * 0.55f, y + size * 0.7f);
        path.lineTo(x, y + size * 0.35f);
        path.lineTo(x - size * 0.55f, y + size * 0.7f);
        path.close();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    /**
     * Daylight: the ring as the whole day from the top at midnight, the
     * sun's hours an orange arc from dawn to dusk, a dot where now is; the
     * dawn and the dusk in words inside.
     */
    private void daylight(Canvas canvas, float[] at) {
        float x = at[0];
        float y = at[1];
        float r = at[2];
        float from = minutes(Sky.dawn());
        float to = minutes(Sky.dusk());
        float ri = r - 5f;
        oval.set(x - ri, y - ri, x + ri, y + ri);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3.5f, 1.5f / k));
        paint.setColor(0x1FFFFFFF);
        canvas.drawCircle(x, y, ri, paint);
        if (from >= 0f && to > from) {
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(daily);
            canvas.drawArc(oval, -90f + 360f * from / 1440f, 360f * (to - from) / 1440f, false, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        double now = Math.toRadians(360f * dayGone());
        canvas.drawCircle(x + ri * (float) Math.sin(now), y - ri * (float) Math.cos(now), Math.max(3f, 2.5f / k),
            paint);
        float room = r * 1.5f;
        text(canvas, Sky.dawn().isEmpty() ? "\u2026" : "\u2191 " + Sky.dawn(), x, y - r * 0.06f, r * 0.24f, INK,
            room);
        text(canvas, Sky.dusk().isEmpty() ? "\u2026" : "\u2193 " + Sky.dusk(), x, y + r * 0.34f, r * 0.24f, QUIET,
            room);
    }

    /** "HH:mm" as minutes from midnight; or less than none if it is not a time. */
    private static float minutes(String time) {
        if (time == null) {
            return -1f;
        }
        int cut = time.indexOf(':');
        if (cut < 1) {
            return -1f;
        }
        try {
            return Integer.parseInt(time.substring(0, cut).trim()) * 60f
                + Integer.parseInt(time.substring(cut + 1).trim());
        } catch (NumberFormatException broken) {
            return -1f;
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
