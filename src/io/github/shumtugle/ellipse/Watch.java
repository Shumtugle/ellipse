package io.github.shumtugle.ellipse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.BatteryManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The clock as a plate: a slab of a material — steel, wood, the accent, a
 * stamped dial, glass — with windows sunk in it, dark as the face of a
 * good watch. On the left a round dial behind a metal bezel, its hands
 * light and its hours marked in bars; on the right the hour and the date
 * in a long window, and under it smaller windows for the weather, the
 * headphones while they are near, and the phone's charge.
 *
 * It stands beside the home screen's first clock, not in its place: the
 * first clock is left as it was, and this is one of the others to choose.
 */
final class Watch extends View implements Timepiece {

    private boolean edgeLeft;
    private boolean edgeRight;

    @Override
    public void edged(boolean left, boolean right) {
        edgeLeft = left;
        edgeRight = right;
        invalidate();
    }

    /**
     * What the dial and the small windows are made of: dark, as the face of
     * a good watch, or any of the materials a plate may be — then the words
     * and hands on them turn dark where the material is light.
     */
    static final int DARK = -1;

    private static final int INK = 0xFFEFE7D6;
    private static final int QUIET = 0xFFB9B1A2;

    private final Almanac.Hand hand;
    private final float density;
    private final GestureDetector taps;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dialBox = new RectF();
    private final RectF timeBox = new RectF();
    private final RectF weatherBox = new RectF();
    private final RectF earsBox = new RectF();
    private final RectF chargeBox = new RectF();
    private final int plate;
    private final int dial;
    private final int fields;
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

    /* The owner's touches: the dial's and the windows' sizes, the hands'
       and the seconds' colours; a colour of zero is the face's own. */
    private final float dialSize;
    private final float hourSize;
    private final float rowSize;
    private final int handsColour;
    private final int secondsColour;

    Watch(Context context, int plate, int dial, int fields, Almanac.Hand hand) {
        super(context);
        this.hand = hand;
        this.plate = plate;
        this.dial = dial;
        this.fields = fields;
        density = context.getResources().getDisplayMetrics().density;
        dialSize = Hues.size(context, Hues.PLATE, Hues.DIAL);
        hourSize = Hues.size(context, Hues.PLATE, Hues.HOUR);
        rowSize = Hues.size(context, Hues.PLATE, Hues.ROW);
        handsColour = Hues.own(context, Hues.PLATE, Hues.HANDS) ? 0
            : Hues.colour(context, Hues.PLATE, Hues.HANDS, 0);
        secondsColour = Hues.own(context, Hues.PLATE, Hues.SECONDS) ? 0
            : Hues.colour(context, Hues.PLATE, Hues.SECONDS, 0);
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
        String which = dialBox.contains(x, y) ? Almanac.DIAL : timeBox.contains(x, y) ? Almanac.TIME
            : !weatherBox.isEmpty() && weatherBox.contains(x, y) ? Almanac.WEATHER
            : !earsBox.isEmpty() && earsBox.contains(x, y) ? Almanac.EARS
            : chargeBox.contains(x, y) ? Almanac.CHARGE : null;
        if (which == null) {
            return;
        }
        RectF box = Almanac.DIAL.equals(which) ? dialBox : Almanac.TIME.equals(which) ? timeBox
            : Almanac.WEATHER.equals(which) ? weatherBox : Almanac.EARS.equals(which) ? earsBox : chargeBox;
        hand.pressed(which, this, box);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float edge = px(4);
        /* Reaching the screen's edge, the plate comes to touch it: its rim whole, its corners round. */
        float touch = px(1);
        RectF slab = new RectF(edgeLeft ? touch : edge, edge, w - (edgeRight ? touch : edge), h - edge);
        float round = Math.min(slab.height() * 0.16f, px(28));
        Path body = new Path();
        body.addRoundRect(slab, round, round, Path.Direction.CW);
        canvas.save();
        canvas.translate(slab.left, slab.top);
        body.offset(-slab.left, -slab.top);
        Rim.plate(canvas, body, plate, slab.width(), slab.height());
        if (plate == Rim.GLASS) {
            Rim.glaze(canvas, body, 0f, 0f, slab.width(), slab.height());
        }
        canvas.restore();

        /* The dial on the left and the windows beside it, all in a band as
           high as the dial, in any box: rows added only widen the plate's
           margins. The dial is never more than its share of the width, so
           nothing on the right is ever pushed over it. */
        float pad = Math.min(slab.height(), slab.width() / 2.6f) * 0.08f;
        float gap = pad * 0.7f;
        float right = slab.right - pad;
        float d = Math.min(slab.height() - 2 * pad, (slab.width() - 2 * pad) * 0.42f * dialSize);
        dialBox.set(slab.left + pad, slab.centerY() - d / 2f, slab.left + pad + d, slab.centerY() + d / 2f);
        float left = dialBox.right + pad * 1.2f;
        float top = dialBox.top;
        float band = d;
        face(canvas, dialBox);

        float share = 0.58f;
        if (hourSize != 1f || rowSize != 1f) {
            share = Math.max(0.3f, Math.min(0.8f, share * hourSize / (share * hourSize + (1f - share) * rowSize)));
        }
        float split = top + (band - gap) * share;
        timeBox.set(left, top, right, split);
        float rowTop = split + gap;
        float rowBottom = top + band;
        float rowTall = rowBottom - rowTop;
        float room = right - left;
        /* Shares of the row, so the weather always keeps its window: the
           charge and the headphones at their own proportions where the row
           is long, and at no more than their shares where it is short. */
        float small = Math.min(rowTall * 1.9f, room * (ears >= 0 && showWeather ? 0.3f : 0.4f));
        chargeBox.set(right - small, rowTop, right, rowBottom);
        float x = chargeBox.left - gap;
        if (ears >= 0) {
            float earsWide = Math.min(rowTall * 1.6f, room * (showWeather ? 0.28f : 0.4f));
            earsBox.set(x - earsWide, rowTop, x, rowBottom);
            x = earsBox.left - gap;
        } else {
            earsBox.setEmpty();
        }
        if (showWeather && x - left >= room * 0.2f) {
            weatherBox.set(left, rowTop, x, rowBottom);
        } else {
            weatherBox.setEmpty();
            if (ears < 0) {
                chargeBox.left = left;
            } else {
                earsBox.left = left;
                earsBox.right = left + (right - left - gap) / 2f;
                chargeBox.left = earsBox.right + gap;
            }
        }

        sunk(canvas, timeBox);
        Date now = new Date();
        String time = new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(getContext())
            ? "H:mm" : "h:mm", Locale.getDefault()).format(now);
        String date = new SimpleDateFormat("EE, d MMMM", Locale.getDefault()).format(now);
        words.setTypeface(Style.family == 0 ? android.graphics.Typeface.create("sans-serif-light",
            android.graphics.Typeface.NORMAL) : Style.face());
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(ink(fields));
        words.setTextSize(fit(time, timeBox.width() * 0.8f, timeBox.height() * 0.55f));
        canvas.drawText(time, timeBox.centerX(), timeBox.top + timeBox.height() * 0.6f, words);
        words.setTypeface(Style.face());
        words.setColor(quiet(fields));
        words.setTextSize(fit(date, timeBox.width() * 0.8f, timeBox.height() * 0.2f));
        canvas.drawText(date, timeBox.centerX(), timeBox.top + timeBox.height() * 0.86f, words);

        if (!weatherBox.isEmpty()) {
            sunk(canvas, weatherBox);
            String warmth = Sky.degrees() != Sky.MISSING ? Sky.degrees() + "\u00B0" : "\u2013";
            pair(canvas, weatherBox, warmth, 0);
        }
        if (!earsBox.isEmpty()) {
            sunk(canvas, earsBox);
            pair(canvas, earsBox, ears + "%", 1);
        }
        sunk(canvas, chargeBox);
        pair(canvas, chargeBox, charge >= 0 ? charge + "%" : "\u2013", 2);
    }

    /** Whether a material reads light, so what is written on it must be dark. */
    /** Black stamped with a fine grid of small pyramids, as the dial of a dress watch is. */
    static final int EMBOSSED = -2;
    /** A plain colour of the palette, from this value up: the accent, orange, red, lilac, blue, green, sand, white. */
    static final int COLOUR = 100;
    static final String[] COLOUR_NAMES = {"Accent", "Orange", "Red", "Lilac", "Blue", "Green", "Sand", "White"};

    /**
     * What a dial or a window is filled with: a material of the rims, black
     * stamped with pyramids, or a colour lit a little from above.
     */
    static void ground(Paint paint, int which, float w, float h) {
        if (which >= COLOUR) {
            int c = Rings.colour(which - COLOUR);
            paint.setShader(new RadialGradient(w * 0.5f, h * 0.3f, Math.max(w, h) * 0.8f,
                mix(c, 0xFFFFFFFF, 0.14f), mix(c, 0xFF000000, 0.28f), Shader.TileMode.CLAMP));
        } else if (which == EMBOSSED) {
            paint.setShader(stamp(Math.max(6f, Math.min(w, h) / 16f)));
        } else {
            Rim.material(paint, which, w, h);
        }
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** The pyramids' tile: four facets, lit from the upper left, repeated. */
    private static Shader stamp(float size) {
        int s = Math.max(4, Math.round(size));
        android.graphics.Bitmap tile = android.graphics.Bitmap.createBitmap(s, s, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tile);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float m = s / 2f;
        float in = s * 0.08f;
        int[] facets = {0xFF2C2926, 0xFF1C1A18, 0xFF0B0A09, 0xFF211F1C};
        float[][] tri = {{in, in, s - in, in}, {s - in, in, s - in, s - in}, {s - in, s - in, in, s - in},
            {in, s - in, in, in}};
        c.drawColor(0xFF0E0D0C);
        for (int i = 0; i < 4; i++) {
            android.graphics.Path f = new android.graphics.Path();
            f.moveTo(tri[i][0], tri[i][1]);
            f.lineTo(tri[i][2], tri[i][3]);
            f.lineTo(m, m);
            f.close();
            p.setColor(facets[i]);
            c.drawPath(f, p);
        }
        return new android.graphics.BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    static boolean light(int which) {
        if (which >= COLOUR) {
            return android.graphics.Color.luminance(Rings.colour(which - COLOUR)) > 0.45f;
        }
        if (which == EMBOSSED) {
            return false;
        }
        switch (which) {
            case Rim.METAL:
            case Rim.GOLD:
            case Rim.SEQUINS:
            case Rim.STEEL:
            case Rim.SILK_PINK:
            case Rim.SILK_NUDE:
            case Rim.SILK_GOLD:
            case Rim.SILK_SILVER:
            case Rim.MESH_LIGHT:
                return true;
            case Rim.ACCENT:
                return android.graphics.Color.luminance(Tone.primary()) > 0.4f;
            case Rim.GLASS:
                return Rim.glassTone > 0.55f;
            default:
                return false;
        }
    }

    private static int ink(int which) {
        return which != DARK && light(which) ? 0xFF1C1A17 : INK;
    }

    private static int quiet(int which) {
        return which != DARK && light(which) ? 0xFF4A453E : QUIET;
    }

    /** A window sunk in the plate: of its material, a shadow under its upper edge, a fine light line round it. */
    private void sunk(Canvas canvas, RectF box) {
        float r = Math.min(box.height() / 2f, px(26));
        paint.setStyle(Paint.Style.FILL);
        if (fields == DARK) {
            paint.setShader(new LinearGradient(0, box.top, 0, box.bottom, 0xFF252321, 0xFF131211,
                Shader.TileMode.CLAMP));
            canvas.drawRoundRect(box, r, r, paint);
        } else {
            canvas.save();
            canvas.translate(box.left, box.top);
            Paint made = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            ground(made, fields, box.width(), box.height());
            canvas.drawRoundRect(new RectF(0, 0, box.width(), box.height()), r, r, made);
            canvas.restore();
        }
        paint.setShader(new LinearGradient(0, box.top, 0, box.top + box.height() * 0.25f, 0x66000000, 0x00000000,
            Shader.TileMode.CLAMP));
        canvas.drawRoundRect(box, r, r, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, px(0.8f)));
        paint.setColor(0x40FFFFFF);
        canvas.drawRoundRect(box, r, r, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /** The dial: behind a bezel of metal, a dark or stamped face, bars for the hours, light hands. */
    private void face(Canvas canvas, RectF box) {
        float cx = box.centerX();
        float cy = box.centerY();
        float r = box.width() / 2f;
        paint.setShader(new LinearGradient(0, box.top, 0, box.bottom, 0xFFE2E2E2, 0xFF6E6E6E,
            Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        float inner = r * 0.93f;
        if (dial != DARK) {
            canvas.save();
            canvas.translate(cx - inner, cy - inner);
            Paint made = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            ground(made, dial, inner * 2f, inner * 2f);
            canvas.drawCircle(inner, inner, inner, made);
            canvas.restore();
            paint.setShader(new RadialGradient(cx, cy, inner, 0x00000000, 0x55000000, Shader.TileMode.CLAMP));
        } else {
            paint.setShader(new RadialGradient(cx, cy - inner * 0.2f, inner, 0xFF2E2B28, 0xFF0D0C0B,
                Shader.TileMode.CLAMP));
        }
        canvas.drawCircle(cx, cy, inner, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, r * 0.012f));
        paint.setColor(0x80000000);
        canvas.drawCircle(cx, cy, inner, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            boolean major = i % 3 == 0;
            float from = inner * (major ? 0.70f : 0.76f);
            float to = inner * 0.88f;
            paint.setStrokeWidth(inner * (major ? 0.05f : 0.028f));
            paint.setColor(major ? ink(dial) : (dial != DARK && light(dial) ? 0xFF5E574D : 0xFF8F887C));
            canvas.drawLine(cx + (float) Math.sin(a) * from, cy - (float) Math.cos(a) * from,
                cx + (float) Math.sin(a) * to, cy - (float) Math.cos(a) * to, paint);
        }
        Calendar now = Calendar.getInstance();
        float sec = now.get(Calendar.SECOND);
        float min = now.get(Calendar.MINUTE) + sec / 60f;
        float hour = now.get(Calendar.HOUR) + min / 60f;
        int hands = handsColour != 0 ? handsColour : ink(dial);
        hand(canvas, cx, cy, hour / 12f, inner * 0.5f, inner * 0.07f, hands);
        hand(canvas, cx, cy, min / 60f, inner * 0.78f, inner * 0.05f, hands);
        hand(canvas, cx, cy, sec / 60f, inner * 0.85f, inner * 0.015f, secondsColour != 0 ? secondsColour
            : dial != DARK && light(dial) ? 0xFF8A3A2E : 0xFFD8D2C6);
        paint.setColor(hands);
        canvas.drawCircle(cx, cy, inner * 0.05f, paint);
    }

    private void hand(Canvas canvas, float cx, float cy, float turn, float length, float width, int colour) {
        double a = Math.PI * 2 * turn;
        paint.setStrokeWidth(width);
        paint.setColor(colour);
        canvas.drawLine(cx - (float) Math.sin(a) * length * 0.12f, cy + (float) Math.cos(a) * length * 0.12f,
            cx + (float) Math.sin(a) * length, cy - (float) Math.cos(a) * length, paint);
    }

    /** A drawing and its words side by side in a small window: the sky, headphones, or the charge. */
    private void pair(Canvas canvas, RectF box, String text, int what) {
        float s = Math.min(box.height() * 0.5f, box.width() * 0.32f);
        words.setTypeface(Style.face());
        int mark = ink(fields);
        words.setColor(mark);
        words.setTextAlign(Paint.Align.LEFT);
        words.setTextSize(fit(text, box.width() * 0.5f, box.height() * 0.4f));
        float textW = words.measureText(text);
        float all = s + px(8) + textW;
        float start = box.centerX() - all / 2f;
        float mx = start + s / 2f;
        float my = box.centerY();
        if (what == 0) {
            Almanac.skyMark(canvas, Sky.sky(), mx, my, s, mark);
        } else if (what == 1) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(mark);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.09f);
            canvas.drawArc(new RectF(mx - s * 0.34f, my - s * 0.36f, mx + s * 0.34f, my + s * 0.32f), 180f, 180f,
                false, p);
            p.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(mx - s * 0.4f, my, mx - s * 0.2f, my + s * 0.34f), s * 0.06f, s * 0.06f, p);
            canvas.drawRoundRect(new RectF(mx + s * 0.2f, my, mx + s * 0.4f, my + s * 0.34f), s * 0.06f, s * 0.06f, p);
        } else {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(mark);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.08f);
            RectF cell = new RectF(mx - s * 0.2f, my - s * 0.38f, mx + s * 0.2f, my + s * 0.42f);
            canvas.drawRoundRect(cell, s * 0.06f, s * 0.06f, p);
            p.setStyle(Paint.Style.FILL);
            canvas.drawRect(mx - s * 0.08f, my - s * 0.48f, mx + s * 0.08f, my - s * 0.38f, p);
            float full = charge < 0 ? 0f : charge / 100f;
            canvas.drawRect(cell.left + s * 0.08f, cell.bottom - s * 0.08f - (cell.height() - s * 0.16f) * full,
                cell.right - s * 0.08f, cell.bottom - s * 0.08f, p);
        }
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + s + px(8), box.centerY() - (f.ascent + f.descent) / 2f, words);
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
