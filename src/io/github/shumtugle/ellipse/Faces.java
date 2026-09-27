package io.github.shumtugle.ellipse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Six clocks of other shapes than the first four, each the time, the
 * weather and the charge: two circles in eclipse; a horizon with the sky
 * above and the time below; a capsule filling like a tank; flip cards over
 * the weather, after a clock loved long ago; the time as a monogram; and
 * three stones in the clock's own material. Each is drawn on a canvas of
 * its own measures, made as large as its box allows and set in its middle,
 * and each answers a touch on its time, its weather and its charge.
 */
final class Faces {

    private Faces() {
    }

    static final String[] NAMES = {"Eclipse", "Horizon", "Reservoir", "Flip", "Monogram", "Three stones"};

    /** A face of the six by its number among them. */
    static View make(Context context, Almanac.Hand hand, int which) {
        switch (which) {
            case 0:
                return new Eclipse(context, hand);
            case 1:
                return new Horizon(context, hand);
            case 2:
                return new Reservoir(context, hand);
            case 3:
                return new Flip(context, hand);
            case 4:
                return new Monogram(context, hand);
            default:
                return new Stones(context, hand);
        }
    }

    /** The least box, in dp, a face is drawn in. */
    static float[] least(int which) {
        switch (which) {
            case 0:
                return new float[] {220f, 110f};
            case 2:
                return new float[] {230f, 88f};
            case 3:
                return new float[] {220f, 118f};
            default:
                return new float[] {230f, 108f};
        }
    }

    /** What every one of the six shares: the hours, the charge, the weather, the touches. */
    abstract static class Face extends View implements Timepiece {
        static final int INK = 0xFFF2EEE6;
        static final int FAINT = 0xFFA8A29A;
        static final int DARK_INK = 0xFF1E1A16;

        final Almanac.Hand hand;
        final float density;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
        final int cardAlpha;
        private final GestureDetector taps;
        private final List<String> names = new ArrayList<>();
        private final List<RectF> boxes = new ArrayList<>();
        /** The canvas's own measures, in dp, and how large it was drawn, and where. */
        final float wide;
        final float tall;
        float scale = 1f;
        float offX;
        float offY;
        boolean showWeather = true;
        int ears = -1;
        /** The ground: dark, glass, or none at all; what shows the charge: the accent, the charge's own, white. */
        final int ground;
        final int mark;
        int charge = -1;
        boolean charging;
        boolean seen;
        private Bitmap skyPicture;
        private int skyKind = -1;

        private final Runnable tick = new Runnable() {
            public void run() {
                ticked();
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

        Face(Context context, Almanac.Hand hand, float wide, float tall) {
            super(context);
            this.hand = hand;
            this.wide = wide;
            this.tall = tall;
            density = context.getResources().getDisplayMetrics().density;
            int dark = Keep.number(context, Keep.CLOCK_GROUND, 10);
            cardAlpha = Math.round(255f * Math.max(25, Math.min(95, dark + 40)) / 100f);
            ground = Keep.number(context, Keep.FACE_GROUND, 0);
            mark = Keep.number(context, Keep.FACE_MARK, 0);
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
                public void onLongPress(MotionEvent e) {
                    performLongClick();
                }
            });
        }

        public void weather(boolean shown) {
            showWeather = shown;
            invalidate();
        }

        public void ears(int level) {
            ears = level;
            invalidate();
        }

        /** The headphones: a band and two cups, and their charge beside them. */
        void drawEars(Canvas c, float x, float y, float size, int colour, Paint.Align align) {
            if (ears < 0) {
                return;
            }
            String said = ears + "%";
            words.setTextSize(size);
            float wide = words.measureText(said) + size * 1.3f;
            float left = align == Paint.Align.RIGHT ? x - wide : align == Paint.Align.CENTER ? x - wide / 2f : x;
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.13f);
            paint.setColor(colour);
            float r = size * 0.42f;
            c.drawArc(new RectF(left, y - r, left + 2 * r, y + r), 180f, 180f, false, paint);
            paint.setStyle(Paint.Style.FILL);
            c.drawRoundRect(new RectF(left - size * 0.05f, y, left + size * 0.22f, y + r * 0.95f), 2f, 2f, paint);
            c.drawRoundRect(new RectF(left + 2 * r - size * 0.22f, y, left + 2 * r + size * 0.05f, y + r * 0.95f), 2f,
                2f, paint);
            text(c, said, left + size * 1.3f, y + r * 0.3f, size, 0f, colour, false, Paint.Align.LEFT);
            window(Almanac.EARS, left - 4f, y - r - 4f, left + wide + 4f, y + r + 4f);
        }

        /** Once a second, before drawing: for a face that moves between minutes. */
        void ticked() {
        }

        private void read(Intent state) {
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
            read(getContext().registerReceiver(power, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
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

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return taps.onTouchEvent(event) || super.onTouchEvent(event);
        }

        private void press(float x, float y) {
            float cx = (x - offX) / scale;
            float cy = (y - offY) / scale;
            for (int i = boxes.size() - 1; i >= 0; i--) {
                RectF box = boxes.get(i);
                if (box.contains(cx, cy)) {
                    hand.pressed(names.get(i), this, new RectF(offX + box.left * scale, offY + box.top * scale,
                        offX + box.right * scale, offY + box.bottom * scale));
                    return;
                }
            }
        }

        /** A window of the face, on its own canvas: the time, the weather or the charge. */
        void window(String name, float l, float t, float r, float b) {
            names.add(name);
            boxes.add(new RectF(l, t, r, b));
        }

        @Override
        protected final void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            scale = Math.min(w / (wide * density), h / (tall * density)) * density;
            offX = (w - wide * scale) / 2f;
            offY = (h - tall * scale) / 2f;
            names.clear();
            boxes.clear();
            canvas.save();
            canvas.translate(offX, offY);
            canvas.scale(scale, scale);
            draw(canvas, new Date());
            canvas.restore();
        }

        abstract void draw(Canvas canvas, Date now);

        String hours(Date now) {
            return new SimpleDateFormat(android.text.format.DateFormat.is24HourFormat(getContext()) ? "HH" : "hh",
                Locale.getDefault()).format(now);
        }

        String minutes(Date now) {
            return new SimpleDateFormat("mm", Locale.getDefault()).format(now);
        }

        String time(Date now) {
            return hours(now) + ":" + minutes(now);
        }

        String day(Date now, String pattern) {
            return new SimpleDateFormat(pattern, Locale.getDefault()).format(now);
        }

        boolean known() {
            return showWeather && Sky.degrees() != Sky.MISSING;
        }

        String degrees() {
            return Sky.degrees() != Sky.MISSING ? Sky.degrees() + "\u00B0" : "\u2026";
        }

        String percent() {
            return charge < 0 ? "\u2026" : charge + "%";
        }

        /** The widget's own drawing of the sky, as the card clock draws it. */
        Bitmap sky() {
            int kind = Sky.sky();
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

        void drawSky(Canvas canvas, float cx, float cy, float size) {
            Bitmap picture = sky();
            if (picture != null) {
                canvas.drawBitmap(picture, null, new RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f,
                    cy + size / 2f), paint);
            }
        }

        /** Words, their middle at a point, as large as asked but no wider than the room. */
        void text(Canvas canvas, String said, float x, float y, float size, float room, int colour, boolean bold,
                  Paint.Align align) {
            words.setTypeface(bold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
            words.setTextAlign(align);
            words.setColor(colour);
            words.setTextSize(size);
            float measured = words.measureText(said);
            if (room > 0f && measured > room) {
                words.setTextSize(size * room / measured);
            }
            Paint.FontMetrics f = words.getFontMetrics();
            canvas.drawText(said, x, y - (f.ascent + f.descent) / 2f, words);
        }

        int card() {
            if (ground == 2) {
                return 0;
            }
            if (ground == 1) {
                return ((cardAlpha * 2 / 3) << 24) | 0x5A5A62;
            }
            return (cardAlpha << 24) | 0x16130F;
        }

        /** What shows the charge: the accent; or the charge's own colour, green to amber to red; or white. */
        int accent() {
            if (mark == 1) {
                return charging || charge > 50 ? 0xFF8FBF8A : charge > 20 ? 0xFFE0B060 : 0xFFE07A6A;
            }
            if (mark == 2) {
                return INK;
            }
            return Tone.primary();
        }
    }

    // ---------------------------------------------------------------- eclipse

    /** Two circles in eclipse: the time in one, the weather in the other, the charge in the lens between. */
    static final class Eclipse extends Face {
        Eclipse(Context context, Almanac.Hand hand) {
            super(context, hand, 300f, 150f);
        }

        void draw(Canvas c, Date now) {
            float r = 68f;
            float ax = 98f;
            float bx = 202f;
            float cy = 75f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(card());
            c.drawCircle(ax, cy, r, paint);
            paint.setColor(((cardAlpha * 3 / 4) << 24) | 0x3A342D);
            c.drawCircle(bx, cy, r, paint);
            Path a = new Path();
            a.addCircle(ax, cy, r, Path.Direction.CW);
            Path b = new Path();
            b.addCircle(bx, cy, r, Path.Direction.CW);
            Path lens = new Path();
            lens.op(a, b, Path.Op.INTERSECT);
            paint.setColor(accent());
            c.drawPath(lens, paint);
            text(c, time(now), ax - 20f, cy - 6f, 32f, 92f, INK, true, Paint.Align.CENTER);
            text(c, day(now, "EE d"), ax - 20f, cy + 22f, 13f, 84f, FAINT, false, Paint.Align.CENTER);
            window(Almanac.TIME, ax - r, cy - r, 150f - 22f, cy + 10f);
            window(Almanac.DATE, ax - r, cy + 10f, 150f - 22f, cy + r);
            if (known()) {
                drawSky(c, bx + 20f, cy - 20f, 32f);
                text(c, degrees(), bx + 20f, cy + 16f, 22f, 74f, INK, true, Paint.Align.CENTER);
            }
            drawEars(c, bx + 20f, cy + 40f, 11f, FAINT, Paint.Align.CENTER);
            window(Almanac.WEATHER, 150f + 22f, cy - r, bx + r, cy + 30f);
            boolean light = android.graphics.Color.luminance(accent()) > 0.45f;
            text(c, charge < 0 ? "\u2026" : String.valueOf(charge), 150f, cy, 18f, 30f, light ? DARK_INK : INK, true,
                Paint.Align.CENTER);
            window(Almanac.CHARGE, 150f - 22f, cy - r * 0.7f, 150f + 22f, cy + r * 0.7f);
        }
    }

    // ---------------------------------------------------------------- horizon

    /** The sky above with the weather in it, the charge as the line of the horizon, the time below. */
    static final class Horizon extends Face {
        Horizon(Context context, Almanac.Hand hand) {
            super(context, hand, 320f, 150f);
        }

        void draw(Canvas c, Date now) {
            RectF whole = new RectF(4f, 4f, 316f, 146f);
            float line = 66f;
            Path card = new Path();
            card.addRoundRect(whole, 26f, 26f, Path.Direction.CW);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(card());
            c.drawPath(card, paint);
            /* The sky: its colour from the hour and the weather, day blue, night deep, grey in rain. */
            int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.ROOT).format(now));
            boolean night = hour < 6 || hour >= 21;
            boolean grey = Sky.sky() == Sky.RAIN || Sky.sky() == Sky.FOG || Sky.sky() == Sky.STORM;
            int top = night ? 0xFF141A30 : grey ? 0xFF4A4E56 : 0xFF3E5A86;
            int low = night ? 0xFF2A2440 : grey ? 0xFF6A6A66 : 0xFF9A8672;
            c.save();
            c.clipPath(card);
            paint.setShader(new LinearGradient(0f, 4f, 0f, line, top, low, Shader.TileMode.CLAMP));
            paint.setAlpha(Math.min(255, cardAlpha + 40));
            c.drawRect(4f, 4f, 316f, line, paint);
            paint.setShader(null);
            paint.setAlpha(255);
            c.restore();
            if (known()) {
                drawSky(c, 34f, 35f, 30f);
                text(c, degrees(), 56f, 35f, 22f, 70f, INK, false, Paint.Align.LEFT);
                String place = Sky.place();
                if (place != null && !place.isEmpty()) {
                    text(c, place, 298f, 35f, 14f, 150f, 0xFFDCD8E4, false, Paint.Align.RIGHT);
                }
            }
            window(Almanac.WEATHER, 4f, 4f, 316f, line - 4f);
            /* The horizon: the charge as its lit length. */
            paint.setColor(0x55FFFFFF);
            c.drawRect(4f, line - 1.5f, 316f, line + 1.5f, paint);
            paint.setColor(charging ? 0xFF8FBF8A : accent());
            c.drawRect(4f, line - 1.5f, 4f + 312f * Math.max(0, charge) / 100f, line + 1.5f, paint);
            text(c, percent(), 298f, line + 12f, 11f, 60f, FAINT, false, Paint.Align.RIGHT);
            window(Almanac.CHARGE, 240f, line - 8f, 316f, line + 20f);
            drawEars(c, 234f, line + 12f, 11f, FAINT, Paint.Align.RIGHT);
            text(c, time(now), 22f, 108f, 50f, 190f, INK, true, Paint.Align.LEFT);
            text(c, day(now, "EEEE"), 298f, 116f, 14f, 100f, FAINT, false, Paint.Align.RIGHT);
            window(Almanac.TIME, 4f, line + 20f, 200f, 146f);
            window(Almanac.DATE, 200f, line + 20f, 316f, 146f);
        }
    }

    // ---------------------------------------------------------------- reservoir

    /** A capsule filling at its right like a tank: the charge its level; the time and the weather beside it. */
    static final class Reservoir extends Face {
        Reservoir(Context context, Almanac.Hand hand) {
            super(context, hand, 320f, 120f);
        }

        void draw(Canvas c, Date now) {
            RectF whole = new RectF(4f, 6f, 316f, 114f);
            Path capsule = new Path();
            capsule.addRoundRect(whole, 54f, 54f, Path.Direction.CW);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(card());
            c.drawPath(capsule, paint);
            float tank = 214f;
            c.save();
            c.clipPath(capsule);
            float level = 114f - 108f * Math.max(0, charge) / 100f;
            paint.setColor(charging ? 0xFF8FBF8A : accent());
            c.drawRect(tank, level, 316f, 114f, paint);
            paint.setColor(0x33FFFFFF);
            c.drawRect(tank, level, 316f, level + 1.5f, paint);
            c.restore();
            paint.setColor(0x66000000);
            c.drawRect(tank - 1f, 14f, tank + 1f, 106f, paint);
            boolean light = android.graphics.Color.luminance(accent()) > 0.45f && charge > 45;
            text(c, percent(), (tank + 316f) / 2f, 60f, 20f, 80f, light ? DARK_INK : INK, true, Paint.Align.CENTER);
            window(Almanac.CHARGE, tank, 6f, 316f, 114f);
            drawEars(c, (tank + 316f) / 2f, 86f, 11f, light ? DARK_INK : INK, Paint.Align.CENTER);
            text(c, time(now), 34f, 50f, 40f, 170f, INK, true, Paint.Align.LEFT);
            text(c, day(now, "EE, d MMM"), 34f, 84f, 13f, 100f, FAINT, false, Paint.Align.LEFT);
            window(Almanac.TIME, 4f, 6f, 150f, 70f);
            window(Almanac.DATE, 4f, 70f, 146f, 114f);
            if (known()) {
                drawSky(c, 158f, 84f, 18f);
                text(c, degrees(), 170f, 84f, 13f, 40f, INK, false, Paint.Align.LEFT);
                window(Almanac.WEATHER, 146f, 66f, tank - 2f, 104f);
            }
        }
    }

    // ---------------------------------------------------------------- flip

    /**
     * Flip cards over the weather, after the clock a well-loved phone once
     * wore: two dark cards for the hours and the minutes, split at their
     * middle with a hinge at each side, the top leaf falling as the minute
     * turns; under them the town, the sky, the temperature with the day's
     * high and low, and the charge.
     */
    static final class Flip extends Face {
        private String shownHours;
        private String shownMinutes;
        private String wasHours;
        private String wasMinutes;
        private long flipAt;
        private static final long FLIP = 420L;

        /** The cards: dark, light, or cut from the clock's own material. */
        private final int cards;
        private final int material;

        Flip(Context context, Almanac.Hand hand) {
            super(context, hand, 300f, 160f);
            cards = Keep.number(context, Keep.FLIP_CARDS, 0);
            material = Keep.number(context, Keep.CLOCK_PLATE, Rim.BLACK);
        }

        @Override
        void ticked() {
            Date now = new Date();
            String h = hours(now);
            String m = minutes(now);
            if (shownHours != null && (!h.equals(shownHours) || !m.equals(shownMinutes))) {
                wasHours = shownHours;
                wasMinutes = shownMinutes;
                flipAt = System.currentTimeMillis();
            }
            shownHours = h;
            shownMinutes = m;
        }

        void draw(Canvas c, Date now) {
            if (shownHours == null) {
                ticked();
            }
            float phase = Math.min(1f, (System.currentTimeMillis() - flipAt) / (float) FLIP);
            if (phase < 1f) {
                postInvalidateOnAnimation();
            }
            card(c, new RectF(16f, 6f, 142f, 104f), shownHours, wasHours != null && !wasHours.equals(shownHours)
                ? wasHours : null, phase);
            card(c, new RectF(158f, 6f, 284f, 104f), shownMinutes, wasMinutes != null && !wasMinutes.equals(shownMinutes)
                ? wasMinutes : null, phase);
            window(Almanac.TIME, 16f, 6f, 284f, 104f);
            float y = 132f;
            if (known()) {
                drawSky(c, 34f, y, 30f);
                text(c, degrees(), 56f, y - 6f, 22f, 60f, INK, true, Paint.Align.LEFT);
                String place = Sky.place();
                String span = Sky.days() > 0 && Sky.high(0) != Sky.MISSING
                    ? Sky.high(0) + "\u00B0 / " + Sky.low(0) + "\u00B0" : "";
                text(c, (place == null ? "" : place) + (span.isEmpty() ? "" : "  " + span), 56f, y + 14f, 11f, 150f,
                    FAINT, false, Paint.Align.LEFT);
                window(Almanac.WEATHER, 12f, y - 22f, 210f, y + 24f);
            }
            text(c, day(now, "EE d MMM"), 284f, y - 6f, 12f, 90f, FAINT, false, Paint.Align.RIGHT);
            text(c, percent(), 284f, y + 14f, 12f, 60f, charging ? 0xFF8FBF8A : mark == 0 ? INK : accent(), false,
                Paint.Align.RIGHT);
            window(Almanac.CHARGE, 244f, y + 2f, 290f, y + 26f);
            window(Almanac.DATE, 214f, y - 20f, 290f, y + 2f);
            drawEars(c, 240f, y + 14f, 12f, FAINT, Paint.Align.RIGHT);
        }

        /** One flip card: its two halves, the old top leaf falling onto the new bottom as it turns. */
        private void card(Canvas c, RectF box, String now, String before, float phase) {
            float mid = box.centerY();
            float round = 12f;
            RectF top = new RectF(box.left, box.top, box.right, mid - 1f);
            RectF bottom = new RectF(box.left, mid + 1f, box.right, box.bottom);
            String upper = now;
            String lower = before != null && phase < 1f ? (phase < 0.5f ? before : now) : now;
            half(c, box, top, upper, true, round);
            half(c, box, bottom, lower, false, round);
            if (before != null && phase < 1f) {
                /* The leaf: the old top falling to the hinge, then the new bottom falling from it. */
                c.save();
                if (phase < 0.5f) {
                    float s = 1f - phase * 2f;
                    c.scale(1f, s, box.centerX(), mid);
                    half(c, box, top, before, true, round);
                } else {
                    float s = (phase - 0.5f) * 2f;
                    c.scale(1f, s, box.centerX(), mid);
                    half(c, box, bottom, now, false, round);
                }
                c.restore();
            }
            /* The hinges at each side, and the fine split. */
            paint.setShader(null);
            paint.setColor(0xFF0A0908);
            c.drawRect(box.left, mid - 1f, box.right, mid + 1f, paint);
            paint.setColor(0xFF5A544C);
            c.drawRoundRect(new RectF(box.left - 3f, mid - 6f, box.left + 3f, mid + 6f), 2f, 2f, paint);
            c.drawRoundRect(new RectF(box.right - 3f, mid - 6f, box.right + 3f, mid + 6f), 2f, 2f, paint);
        }

        private void half(Canvas c, RectF box, RectF part, String digits, boolean upper, float round) {
            c.save();
            c.clipRect(part);
            int ink = INK;
            if (cards == 2) {
                /* Cut from the clock's material, each half its own plate. */
                Path plate = new Path();
                plate.addRoundRect(new RectF(0f, 0f, box.width(), box.height()), round, round, Path.Direction.CW);
                c.save();
                c.translate(box.left, box.top);
                Rim.plate(c, plate, material, box.width(), box.height());
                c.restore();
                ink = Watch.light(material) ? DARK_INK : INK;
            } else {
                /* Solid, as a card is, whatever lies behind it: the lower half a shade deeper than the upper. */
                boolean light = cards == 1;
                paint.setShader(new LinearGradient(0f, box.top, 0f, box.bottom, light
                    ? new int[] {0xFFF4F1EB, 0xFFE2DED6, 0xFFD2CEC6, 0xFFE0DCD4}
                    : new int[] {0xFF34302C, 0xFF211F1C, 0xFF151311, 0xFF1F1D1A},
                    new float[] {0f, 0.49f, 0.51f, 1f}, Shader.TileMode.CLAMP));
                c.drawRoundRect(box, round, round, paint);
                paint.setShader(null);
                ink = light ? DARK_INK : INK;
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(cards == 1 ? 0x22000000 : 0x26FFFFFF);
            c.drawRoundRect(new RectF(box.left + 0.5f, box.top + 0.5f, box.right - 0.5f, box.bottom - 0.5f), round, round,
                paint);
            paint.setStyle(Paint.Style.FILL);
            text(c, digits, box.centerX(), box.centerY() + 2f, 76f, box.width() - 14f, ink, true, Paint.Align.CENTER);
            if (upper) {
                paint.setColor(cards == 1 ? 0x0A000000 : 0x0CFFFFFF);
                c.drawRect(box.left, box.top, box.right, box.top + (box.height() / 2f) * 0.35f, paint);
            }
            c.restore();
        }
    }

    // ---------------------------------------------------------------- monogram

    /** The time as a monogram across the whole face, the minutes in the accent, the rest a small line. */
    static final class Monogram extends Face {
        Monogram(Context context, Almanac.Hand hand) {
            super(context, hand, 320f, 150f);
        }

        void draw(Canvas c, Date now) {
            words.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            words.setTextSize(118f);
            String h = hours(now);
            float gap = 10f;
            float hw = words.measureText(h);
            float colon = 22f;
            text(c, h, 6f, 62f, 118f, 140f, INK, true, Paint.Align.LEFT);
            /* A colon, thin and faint, so the hours and the minutes never read as a year. */
            float at = 6f + Math.min(140f, hw) + gap / 2f;
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(FAINT);
            c.drawCircle(at + colon / 2f - 4f, 40f, 5f, paint);
            c.drawCircle(at + colon / 2f - 4f, 84f, 5f, paint);
            text(c, minutes(now), at + colon, 62f, 118f, 140f, accent(), true, Paint.Align.LEFT);
            window(Almanac.TIME, 4f, 4f, 316f, 122f);
            String line = day(now, "EE d") + (known() ? "  \u00B7  " + degrees() : "") + "  \u00B7  " + percent()
                + (ears >= 0 ? "  \u00B7  \u266B " + ears + "%" : "");
            text(c, line, 14f, 136f, 14f, 290f, FAINT, false, Paint.Align.LEFT);
            window(Almanac.DATE, 4f, 124f, 70f, 148f);
            if (known()) {
                window(Almanac.WEATHER, 70f, 124f, 150f, 148f);
            }
            window(Almanac.CHARGE, 150f, 124f, 316f, 148f);
        }
    }

    // ---------------------------------------------------------------- three stones

    /**
     * Three stones with no ground between them, each a little uneven, cut
     * from the clock's own material: the large one the time, the middle one
     * the weather, the small one the charge.
     */
    static final class Stones extends Face {
        private final int material;

        Stones(Context context, Almanac.Hand hand) {
            super(context, hand, 320f, 150f);
            material = Keep.number(context, Keep.CLOCK_PLATE, Rim.BLACK);
        }

        void draw(Canvas c, Date now) {
            boolean lightStone = Watch.light(material);
            int ink = lightStone ? DARK_INK : INK;
            int faint = lightStone ? 0xFF4A443C : FAINT;
            stone(c, 106f, 76f, 98f, 68f, 0.045f, 1.3f);
            text(c, time(now), 106f, 68f, 42f, 160f, ink, true, Paint.Align.CENTER);
            text(c, day(now, "EE d"), 106f, 104f, 14f, 120f, faint, false, Paint.Align.CENTER);
            window(Almanac.TIME, 8f, 8f, 204f, 88f);
            window(Almanac.DATE, 8f, 88f, 204f, 144f);
            stone(c, 252f, 54f, 50f, 44f, 0.06f, 2.1f);
            if (known()) {
                drawSky(c, 252f, 42f, 26f);
                text(c, degrees(), 252f, 72f, 17f, 70f, ink, true, Paint.Align.CENTER);
            }
            window(Almanac.WEATHER, 202f, 10f, 302f, 98f);
            stone(c, 282f, 124f, 30f, 24f, 0.08f, 3.7f);
            text(c, charge < 0 ? "\u2026" : String.valueOf(charge), 282f, ears >= 0 ? 118f : 124f, 14f, 44f,
                charging ? 0xFF8FBF8A : mark == 0 ? ink : accent(), true, Paint.Align.CENTER);
            if (ears >= 0) {
                text(c, "\u266B " + ears, 282f, 134f, 9f, 44f, faint, false, Paint.Align.CENTER);
            }
            window(Almanac.CHARGE, 252f, 100f, 312f, 148f);
        }

        private void stone(Canvas c, float cx, float cy, float rx, float ry, float wobble, float seed) {
            Path p = new Path();
            for (int k = 0; k <= 72; k++) {
                double a = 2 * Math.PI * k / 72;
                double r = 1 + wobble * (Math.sin(3 * a + seed) + 0.6 * Math.cos(2 * a + seed * 1.7));
                float x = cx + (float) (Math.cos(a) * rx * r);
                float y = cy + (float) (Math.sin(a) * ry * r);
                if (k == 0) {
                    p.moveTo(x, y);
                } else {
                    p.lineTo(x, y);
                }
            }
            p.close();
            c.save();
            c.translate(cx - rx * 1.1f, cy - ry * 1.1f);
            p.offset(-(cx - rx * 1.1f), -(cy - ry * 1.1f));
            Rim.plate(c, p, material, rx * 2.2f, ry * 2.2f);
            c.restore();
        }
    }
}
