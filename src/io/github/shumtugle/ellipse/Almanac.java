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
 * colour. On the left, a scalloped face with the hands of a clock. Given
 * one row of the grid, the hour in figures with the date under it stands
 * beside the face and the phone's charge beside that, all three in a line;
 * given two, the charge goes under the hour.
 *
 * The hands move once a second while the clock is in sight, and not at
 * all when it is not. A press on a window opens what it shows about: the
 * alarms, the calendar, the phone's battery. Headphones near take a window
 * from the hour's, beside it and as tall. A long press takes the clock
 * up, to be set down in another row or on another screen.
 */
final class Almanac extends View implements Timepiece {

    interface Hand {
        void pressed(String window, View from, RectF box);
    }

    static final String DIAL = "dial";
    static final String TIME = "time";
    static final String CHARGE = "charge";
    static final String WEATHER = "weather";
    static final String EARS = "ears";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dialBox = new RectF();
    private final RectF timeBox = new RectF();
    private final RectF chargeBox = new RectF();
    private final RectF weatherBox = new RectF();
    private final RectF earsBox = new RectF();
    /** Whether the weather is shown at all, and the charge of headphones near, or none. */
    private boolean showWeather = true;
    private int ears = -1;
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

    /* The owner's touches: the windows' sizes and the parts' colours, the
       face's own where nothing was chosen (a colour of zero is its own). */
    private final float dialSize;
    private final float hourSize;
    private final float rowSize;
    private final int dialFill;
    private final int hourFill;
    private final int windowFill;
    private final int secondsColour;

    Almanac(Context context, Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        dialSize = Hues.size(context, Hues.FIRST, Hues.DIAL);
        hourSize = Hues.size(context, Hues.FIRST, Hues.HOUR);
        rowSize = Hues.size(context, Hues.FIRST, Hues.ROW);
        dialFill = Hues.own(context, Hues.FIRST, Hues.DIAL) ? 0
            : Hues.field(Hues.colour(context, Hues.FIRST, Hues.DIAL, 0));
        hourFill = Hues.own(context, Hues.FIRST, Hues.HOUR) ? 0
            : Hues.field(Hues.colour(context, Hues.FIRST, Hues.HOUR, 0));
        windowFill = Hues.own(context, Hues.FIRST, Hues.WINDOWS) ? 0
            : Hues.field(Hues.colour(context, Hues.FIRST, Hues.WINDOWS, 0));
        secondsColour = Hues.own(context, Hues.FIRST, Hues.SECONDS) ? 0
            : Hues.colour(context, Hues.FIRST, Hues.SECONDS, 0);
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

            /* Held long, the clock is taken up to be moved, like anything
               else standing on a screen. */
            @Override
            public void onLongPress(MotionEvent e) {
                performLongClick();
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
            : chargeBox.contains(x, y) ? chargeBox
            : !weatherBox.isEmpty() && weatherBox.contains(x, y) ? weatherBox
            : !earsBox.isEmpty() && earsBox.contains(x, y) ? earsBox : null;
        if (box == null) {
            return;
        }
        String which = box == dialBox ? DIAL : box == timeBox ? TIME : box == chargeBox ? CHARGE
            : box == weatherBox ? WEATHER : EARS;
        hand.pressed(which, this, box);
    }

    /** Whether to show the weather at all; what it is comes from the sky as last asked. */
    public void weather(boolean shown) {
        showWeather = shown;
        invalidate();
    }

    /** The charge of headphones near, or less than nought when none tell one. */
    public void ears(int level) {
        ears = level;
        invalidate();
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

        boolean line = w > h * 2.6f;
        float pad = px(line ? 10f : 12f);
        float gap = px(line ? 8f : 10f);
        float column = h - 2f * (inset + pad);
        float left = inset + pad;
        float right = w - inset - pad;
        float top = inset + pad;
        float bottom = h - inset - pad;
        /* The face on the left; on the right, the hour and the date above,
           and under them a row of small windows: the weather, the charge,
           and the headphones' charge while they tell one. */
        float from;
        if (line) {
            float side = column;
            if (dialSize < 1f) {
                side = column * dialSize;
                dialBox.set(left, (h - side) / 2f, left + side, (h + side) / 2f);
            } else {
                dialBox.set(left, top, left + side, bottom);
            }
            from = dialBox.right + gap;
        } else {
            /* Never more than its share of the width, so a taller clock
               keeps the windows beside it as wide as a low one does. */
            float side = Math.min(column, (w - 2f * (inset + pad)) * 0.38f * dialSize);
            dialBox.set(left, (h - side) / 2f, left + side, (h + side) / 2f);
            from = dialBox.right + gap * 1.4f;
            /* The windows stand in a band as high as the face, so rows
               added below or above only widen the card's margins. */
            top = dialBox.top;
            bottom = dialBox.bottom;
            column = side;
        }
        /* The hour's share of the band against the row's, each as the owner sized it. */
        float share = line ? 0.58f : 0.6f;
        if (hourSize != 1f || rowSize != 1f) {
            share = share * hourSize / (share * hourSize + (1f - share) * rowSize);
            share = Math.max(0.3f, Math.min(0.8f, share));
        }
        float split = top + (column - gap) * share;
        /* Headphones near take their window from the hour's, beside it and
           as tall, so the row under it keeps its two roomy windows. */
        float timeRight = right;
        float room = right - from;
        if (ears >= 0) {
            /* Nearly square, but never more than a quarter of the windows'
               width: the hour and the date keep the rest. */
            float earsWide = Math.min((split - top) * 0.92f, room * 0.25f);
            earsBox.set(right - earsWide, top, right, split);
            timeRight = earsBox.left - gap;
        } else {
            earsBox.setEmpty();
        }
        timeBox.set(from, top, timeRight, split);
        float rowTop = split + gap;
        float rowTall = bottom - rowTop;
        /* The charge a pill of its own proportion, but never wider than
           three eighths of the row: the weather keeps the rest. */
        float small = Math.min(rowTall * 1.9f, room * 0.37f);
        float x = right;
        chargeBox.set(x - small, rowTop, x, bottom);
        x -= small + gap;
        if (showWeather) {
            weatherBox.set(from, rowTop, x, bottom);
        } else {
            weatherBox.setEmpty();
            chargeBox.left = from;
        }
        float side = dialBox.width();

        paint.setColor(dialFill != 0 ? dialFill : Tone.primaryContainer());
        canvas.drawPath(cookie(dialBox.centerX(), dialBox.centerY(), side / 2f, 12, 0.07f), paint);
        dial(canvas, dialBox, dialFill != 0 ? Hues.inkOn(dialFill) : Tone.onPrimaryContainer());

        float r = Math.min(timeBox.height() / 2f, px(24f));
        paint.setColor(hourFill != 0 ? hourFill : Tone.secondaryContainer());
        canvas.drawRoundRect(timeBox, r, r, paint);
        Date now = new Date();
        int ink = hourFill != 0 ? Hues.inkOn(hourFill) : Tone.onSecondaryContainer();
        words.setTextAlign(Paint.Align.CENTER);
        String hour = android.text.format.DateFormat.getTimeFormat(getContext()).format(now);
        words.setTypeface(Style.family == 0 ? Typeface.create("sans-serif-light", Typeface.NORMAL) : Style.face());
        words.setColor(ink);
        words.setTextSize(fit(hour, timeBox.width() * 0.84f, timeBox.height() * 0.5f));
        canvas.drawText(hour, timeBox.centerX(), timeBox.top + timeBox.height() * 0.56f, words);
        String day = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(
            Locale.getDefault(), "EEEdMMMM"), Locale.getDefault()).format(now);
        words.setTypeface(Style.face());
        words.setColor((ink & 0x00FFFFFF) | 0xB3000000);
        words.setTextSize(fit(day, timeBox.width() * 0.84f, timeBox.height() * 0.17f));
        canvas.drawText(day, timeBox.centerX(), timeBox.top + timeBox.height() * 0.84f, words);

        int quiet = windowFill != 0 ? Hues.inkOn(windowFill) : Tone.onTertiaryContainer();
        String level = charge >= 0 ? charge + "%" : "\u2013";
        window(canvas, chargeBox);
        pill(canvas, chargeBox, level, quiet);
        if (!weatherBox.isEmpty()) {
            window(canvas, weatherBox);
            String warmth = Sky.degrees() != Sky.MISSING ? Sky.degrees() + "\u00B0" : "\u2013";
            mark(canvas, weatherBox, warmth, quiet, true);
        }
        if (!earsBox.isEmpty()) {
            window(canvas, earsBox);
            float s = Math.min(earsBox.width() * 0.5f, earsBox.height() * 0.42f);
            headphones(canvas, earsBox.centerX(), earsBox.top + earsBox.height() * 0.36f, s, quiet);
            words.setTypeface(Style.face());
            words.setColor(quiet);
            words.setTextAlign(Paint.Align.CENTER);
            String said = ears + "%";
            words.setTextSize(fit(said, earsBox.width() * 0.74f, earsBox.height() * 0.24f));
            canvas.drawText(said, earsBox.centerX(), earsBox.top + earsBox.height() * 0.82f, words);
        }
    }

    private void window(Canvas canvas, RectF box) {
        float r = Math.min(box.height() / 2f, px(24f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(windowFill != 0 ? windowFill : Tone.tertiaryContainer());
        canvas.drawRoundRect(box, r, r, paint);
    }

    /** A drawing and its words side by side in a small window: the sky, or headphones. */
    private void mark(Canvas canvas, RectF box, String text, int ink, boolean sky) {
        float s = Math.min(box.height() * 0.62f, box.width() * 0.34f);
        words.setTypeface(Style.face());
        words.setColor(ink);
        words.setTextAlign(Paint.Align.LEFT);
        words.setTextSize(fit(text, box.width() * 0.52f, box.height() * 0.4f));
        float textW = words.measureText(text);
        float all = s + px(8f) + textW;
        float start = box.centerX() - all / 2f;
        if (sky) {
            skyMark(canvas, Sky.sky(), start + s / 2f, box.centerY(), s, ink);
        } else {
            headphones(canvas, start + s / 2f, box.centerY(), s, ink);
        }
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + s + px(8f), box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    private void headphones(Canvas canvas, float cx, float cy, float s, int colour) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(colour);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.08f);
        p.setStrokeCap(Paint.Cap.ROUND);
        RectF arc = new RectF(cx - s * 0.34f, cy - s * 0.36f, cx + s * 0.34f, cy + s * 0.32f);
        canvas.drawArc(arc, 180f, 180f, false, p);
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(cx - s * 0.4f, cy - s * 0.02f, cx - s * 0.2f, cy + s * 0.34f),
            s * 0.06f, s * 0.06f, p);
        canvas.drawRoundRect(new RectF(cx + s * 0.2f, cy - s * 0.02f, cx + s * 0.4f, cy + s * 0.34f),
            s * 0.06f, s * 0.06f, p);
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
        words.setTypeface(Style.face());
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

    /** The mark above its words, for a window taller than it is wide. */
    private void stacked(Canvas canvas, RectF box, String text, int ink) {
        float s = Math.min(box.width() * 0.46f, box.height() * 0.4f);
        float cy = box.top + box.height() * 0.36f;
        battery(canvas, box.centerX(), cy, s, ink);
        words.setTypeface(Style.face());
        words.setColor(ink);
        words.setTextAlign(Paint.Align.CENTER);
        words.setTextSize(fit(text, box.width() * 0.78f, box.height() * 0.2f));
        canvas.drawText(text, box.centerX(), box.top + box.height() * 0.82f, words);
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
        hand(canvas, cx, cy, seconds / 60f, r * 0.7f, r * 0.018f,
            secondsColour != 0 ? secondsColour : Tone.primary());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(secondsColour != 0 ? secondsColour : Tone.primary());
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
            p.setColor(windowFill != 0 ? windowFill : Tone.tertiaryContainer());
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

    /**
     * The sky drawn in one ink, in a square of the given size about a centre,
     * from what the weather says it is doing: clear, cloud, fog, rain, snow
     * or a storm.
     */
    static void skyMark(Canvas canvas, int kind, float cx, float cy, float size, int ink) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ink);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int[] codes = {0, 3, 45, 61, 71, 95};
        int code = kind >= 0 && kind < codes.length ? codes[kind] : 3;
        boolean day = true;
        boolean clear = code == 0 || code == 1;
        boolean part = code == 2 || code == 1;
        boolean fog = code == 45 || code == 48;
        boolean snow = (code >= 71 && code <= 77) || code == 85 || code == 86;
        boolean rain = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
        boolean bolt = code >= 95;
        float s = size;
        if (clear || part) {
            float bx = part ? cx - s * 0.16f : cx;
            float by = part ? cy - s * 0.14f : cy;
            float r = s * (part ? 0.2f : 0.24f);
            if (day) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(s * 0.06f);
                canvas.drawCircle(bx, by, r, paint);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    canvas.drawLine(bx + (float) Math.cos(a) * r * 1.45f, by + (float) Math.sin(a) * r * 1.45f,
                        bx + (float) Math.cos(a) * r * 1.9f, by + (float) Math.sin(a) * r * 1.9f, paint);
                }
            } else {
                Path moon = new Path();
                moon.addCircle(bx, by, r * 1.2f, Path.Direction.CW);
                Path bite = new Path();
                bite.addCircle(bx + r * 0.6f, by - r * 0.45f, r * 1.05f, Path.Direction.CW);
                moon.op(bite, Path.Op.DIFFERENCE);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(moon, paint);
            }
            if (clear && !part) {
                return;
            }
        }
        if (fog) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(s * 0.07f);
            for (int i = -1; i <= 1; i++) {
                float y = cy + i * s * 0.18f;
                canvas.drawLine(cx - s * 0.34f + (i == 0 ? s * 0.06f : 0f), y,
                    cx + s * 0.34f - (i == 1 ? s * 0.08f : 0f), y, paint);
            }
            return;
        }
        float lift = rain || snow || bolt ? s * 0.1f : 0f;
        cloud(canvas, paint, part ? cx + s * 0.08f : cx, cy - lift + (part ? s * 0.08f : 0f), s * (part ? 0.8f : 1f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.055f);
        float base = cy + s * 0.2f;
        if (rain) {
            for (int i = -1; i <= 1; i++) {
                float x = cx + i * s * 0.18f;
                canvas.drawLine(x, base, x - s * 0.06f, base + s * 0.2f, paint);
            }
        } else if (snow) {
            paint.setStyle(Paint.Style.FILL);
            for (int i = -1; i <= 1; i++) {
                canvas.drawCircle(cx + i * s * 0.18f, base + s * 0.1f + (i == 0 ? s * 0.08f : 0f), s * 0.045f, paint);
            }
        } else if (bolt) {
            Path zig = new Path();
            zig.moveTo(cx + s * 0.04f, base - s * 0.02f);
            zig.lineTo(cx - s * 0.08f, base + s * 0.16f);
            zig.lineTo(cx + s * 0.02f, base + s * 0.16f);
            zig.lineTo(cx - s * 0.06f, base + s * 0.32f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(zig, paint);
        }
    }

    private static void cloud(Canvas canvas, Paint paint, float cx, float cy, float s) {
        Path cloud = new Path();
        cloud.addCircle(cx - s * 0.16f, cy + s * 0.02f, s * 0.15f, Path.Direction.CW);
        cloud.addCircle(cx + s * 0.02f, cy - s * 0.08f, s * 0.2f, Path.Direction.CW);
        cloud.addCircle(cx + s * 0.2f, cy + s * 0.03f, s * 0.14f, Path.Direction.CW);
        cloud.addRoundRect(new RectF(cx - s * 0.31f, cy + s * 0.0f, cx + s * 0.34f, cy + s * 0.17f),
            s * 0.08f, s * 0.08f, Path.Direction.CW);
        cloud.setFillType(Path.FillType.WINDING);
        Path solid = new Path();
        solid.op(cloud, Path.Op.UNION);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.055f);
        canvas.drawPath(solid, paint);
    }
}
