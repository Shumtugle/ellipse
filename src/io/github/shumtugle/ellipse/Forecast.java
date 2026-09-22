package io.github.shumtugle.ellipse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The weather, whole, on plates of the same material as the tiles.
 *
 * It opens out of the window that was pressed: the pane of glass on the
 * clock grows until it is the screen, and the screens behind step back
 * out of focus. It reads from now outward. The first plate is this
 * moment: the sky in a round window, the warmth large, and under them the
 * next two hours in quarters, a column of water for each. The second is
 * the small facts of the moment, each a pill of glass, and the pollen
 * when something is flying. The third is the next twelve hours, two rows
 * of six; the fourth the days, each with the span between its night and
 * its afternoon drawn against the span of all the days, so a cold day
 * looks cold beside a warm one.
 *
 * A number that asks something of the reader, a strong wind, a sun that
 * burns, air worth staying in from, takes the material's own colour; a
 * quiet day is a quiet screen.
 *
 * Everything is drawn in one piece, so the plates arrive one after
 * another, a beat apart, and the columns of water rise into place.
 */
final class Forecast extends FrameLayout {

    interface Hand {
        /** How far the screen has opened, from nothing to all of it. */
        void depth(float open);

        void closed();
    }

    private static final String[] DAYS = {"sunday", "monday", "tuesday", "wednesday", "thursday",
        "friday", "saturday"};

    private final Hand hand;
    private final Sheet sheet;
    private final ScrollView scroll;
    private boolean closing;
    private float cx;
    private float cy;
    private float from;

    Forecast(Context context, Tile.Look look, int top, int bottom, Hand hand) {
        super(context);
        this.hand = hand;
        setBackgroundColor(0xB3000000);
        setClickable(true);
        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        sheet = new Sheet(context, look, top, bottom);
        scroll.addView(sheet, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /** Opens out of a round of the given radius around a point of the screen. */
    void open(final float x, final float y, final float radius) {
        cx = x;
        cy = y;
        from = radius;
        sheet.read();
        post(new Runnable() {
            public void run() {
                float far = (float) Math.hypot(Math.max(cx, getWidth() - cx), Math.max(cy, getHeight() - cy));
                Animator grow = ViewAnimationUtils.createCircularReveal(Forecast.this,
                    Math.round(cx), Math.round(cy), from, far);
                grow.setDuration(Pace.ARRIVE);
                grow.setInterpolator(Pace.EMPHASIS);
                grow.start();
                ValueAnimator depth = ValueAnimator.ofFloat(0f, 1f);
                depth.setDuration(Pace.ARRIVE);
                depth.setInterpolator(Pace.EMPHASIS);
                depth.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator a) {
                        hand.depth((Float) a.getAnimatedValue());
                    }
                });
                depth.start();
                sheet.arrive();
            }
        });
    }

    /** The weather has been asked for again; what is shown is read anew. */
    void refresh() {
        sheet.read();
        sheet.invalidate();
    }

    /** Closes back into the window it came out of. */
    void close() {
        if (closing) {
            return;
        }
        closing = true;
        float far = (float) Math.hypot(Math.max(cx, getWidth() - cx), Math.max(cy, getHeight() - cy));
        Animator shrink = ViewAnimationUtils.createCircularReveal(this, Math.round(cx), Math.round(cy),
            far, Math.max(0f, from));
        shrink.setDuration(Pace.GROW);
        shrink.setInterpolator(Pace.STANDARD);
        shrink.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                drop();
            }
        });
        ValueAnimator depth = ValueAnimator.ofFloat(1f, 0f);
        depth.setDuration(Pace.GROW);
        depth.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                hand.depth((Float) a.getAnimatedValue());
            }
        });
        depth.start();
        shrink.start();
    }

    /** Gone at once, as when the home screen is left. */
    void drop() {
        if (getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) getParent()).removeView(this);
        }
        hand.depth(0f);
        hand.closed();
    }

    // ------------------------------------------------------------ the sheet

    /** The whole screen, laid out and drawn in one piece. */
    private static final class Sheet extends View {

        private final Tile.Look look;
        private final int top;
        private final int bottom;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int ink = 0xFFF4ECDD;
        private final int quiet = 0xB3F4ECDD;
        private final int faint = 0x66F4ECDD;
        private final int accent;
        private Sky.Whole w = new Sky.Whole();
        private float arrived = 1f;
        private final List<RectF> plates = new ArrayList<RectF>();
        private final RectF credit = new RectF();
        private final RectF shut = new RectF();
        private final GestureDetector press;

        // Laid out on measure.
        private float pad;
        private float heroH;
        private float soonH;
        private final List<Pill> pills = new ArrayList<Pill>();
        private float pillsH;
        private float grainsH;
        private float hoursH;
        private float daysH;

        /** A small fact: a value and what it is. */
        private static final class Pill {
            String value;
            String name;
            boolean asks;
            int arrow = -1;
            final RectF box = new RectF();
        }

        Sheet(Context context, Tile.Look look, int top, int bottom) {
            super(context);
            this.look = look;
            this.top = top;
            this.bottom = bottom;
            accent = Tile.accentOf(look.rim);
            edge.setStyle(Paint.Style.STROKE);
            edge.setColor(0x66000000);
            edge.setStrokeWidth(Math.max(1f, Round.px(0.8f)));
            press = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (shut.contains(e.getX(), e.getY())) {
                        View up = (View) getParent().getParent();
                        if (up instanceof Forecast) {
                            ((Forecast) up).close();
                        }
                        return true;
                    }
                    if (credit.contains(e.getX(), e.getY())) {
                        try {
                            getContext().startActivity(new android.content.Intent(
                                android.content.Intent.ACTION_VIEW, android.net.Uri.parse(Sky.WEATHER_SOURCE))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                        } catch (RuntimeException none) {
                            performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                        }
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            return press.onTouchEvent(e) || super.onTouchEvent(e);
        }

        void read() {
            w = Sky.whole(getContext());
            requestLayout();
        }

        void arrive() {
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(Pace.ARRIVE + 5 * Pace.STAGGER * 2);
            a.setInterpolator(new android.view.animation.LinearInterpolator());
            a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator v) {
                    arrived = (Float) v.getAnimatedValue();
                    invalidate();
                }
            });
            arrived = 0f;
            a.start();
        }

        /** How far one plate has arrived: each starts a beat after the one above it. */
        private float at(int rank) {
            float total = Pace.ARRIVE + 5 * Pace.STAGGER * 2;
            float t = (arrived * total - rank * Pace.STAGGER * 2) / Pace.ARRIVE;
            t = Math.max(0f, Math.min(1f, t));
            return Pace.EMPHASIS.getInterpolation(t);
        }

        // -------------------------------------------------------- measuring

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            pad = Round.px(16f);
            float inner = width - 2 * pad - 2 * Round.px(14f);
            heroH = Round.px(196f);
            soonH = Round.px(96f);
            pillsH = layPills(inner);
            grainsH = flying() ? Round.px(118f) : 0f;
            hoursH = Round.px(2 * 92f + 8f);
            daysH = w.dates.length * Round.px(58f) + Math.max(0, w.dates.length - 1) * Round.px(6f);
            float h = top + Round.px(76f);
            if (w.known) {
                h += plateH(heroH + Round.px(10f) + soonH) + Round.px(14f);
                h += plateH(pillsH + (grainsH > 0 ? Round.px(10f) + grainsH : 0f)) + Round.px(14f);
                h += plateH(hoursH) + Round.px(14f);
                h += plateH(daysH) + Round.px(14f);
            } else {
                h += Round.px(120f);
            }
            h += Round.px(64f) + bottom;
            setMeasuredDimension(width, Math.round(h));
        }

        private float plateH(float content) {
            return content + 2 * Round.px(14f);
        }

        private boolean flying() {
            for (int count : w.pollen) {
                if (count > 0) {
                    return true;
                }
            }
            return false;
        }

        /** Lays the pills in rows as wide as their words, the long ones last. */
        private float layPills(float width) {
            pills.clear();
            if (!Float.isNaN(w.wind)) {
                Pill p = pill(Math.round(w.wind) + " " + Words.s("ms"), Words.s("wind"), w.wind >= 11f);
                p.arrow = w.whence;
            }
            if (w.humidity >= 0) {
                pill(w.humidity + "%", Words.s("wet"), false);
            }
            if (!Float.isNaN(w.pressure)) {
                pill(Math.round(w.pressure * 0.750062f) + " \u00b7 " + Math.round(w.pressure),
                    Words.s("press"), false);
            }
            if (!Float.isNaN(w.uv)) {
                int burn = Math.round(w.uv);
                pill(String.valueOf(burn), Words.s("sun") + " \u00b7 "
                    + Words.s(burn <= 2 ? "uv_low" : burn <= 5 ? "uv_mid" : "uv_high"), burn >= 3);
            }
            if (w.air >= 0) {
                pill(String.valueOf(w.air), Words.s("air"), w.air >= 60);
            }
            if (w.dust >= 0) {
                pill(String.valueOf(w.dust), "pm2.5", w.dust >= 25);
            }
            if (w.pollen[0] >= 0 || w.pollen[1] >= 0) {
                int worst = 0;
                for (int i = 1; i < 4; i++) {
                    if (w.pollen[i] > w.pollen[worst]) {
                        worst = i;
                    }
                }
                if (w.pollen[worst] > 0) {
                    pill(String.valueOf(w.pollen[worst]), Words.s(POLLEN[worst]), w.pollen[worst] >= 10);
                } else {
                    pill("", Words.s("no_pollen"), false);
                }
            }
            if (w.dawn.length() > 0) {
                pill(w.dawn + " \u2013 " + w.dusk, Words.s("daylight"), false);
            }
            if (w.kp >= 0f) {
                int kp = Math.round(w.kp);
                pill(String.valueOf(kp), Words.s("geomagnetic") + " \u00b7 "
                    + Words.s(kp >= 5 ? "aurora" : kp >= 4 ? "maybe" : "quiet"), kp >= 4);
            }
            float gap = Round.px(8f);
            float h = Round.px(62f);
            float x = 0f;
            float y = 0f;
            for (Pill p : pills) {
                text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                text.setTextSize(Round.px(20f));
                float valueW = text.measureText(p.value) + (p.arrow >= 0 ? Round.px(20f) : 0f);
                text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                text.setTextSize(Round.px(12f));
                float nameW = text.measureText(p.name);
                float pw = Math.min(width, Math.max(valueW, nameW) + 2 * Round.px(16f));
                if (x > 0f && x + pw > width) {
                    x = 0f;
                    y += h + gap;
                }
                p.box.set(x, y, x + pw, y + h);
                x += pw + gap;
            }
            // Each row is stretched to the full width, so the pills close ranks.
            float rowTop = Float.NaN;
            List<Pill> row = new ArrayList<Pill>();
            for (int i = 0; i <= pills.size(); i++) {
                Pill p = i < pills.size() ? pills.get(i) : null;
                if (p == null || (row.size() > 0 && p.box.top != rowTop)) {
                    stretch(row, width, gap);
                    row.clear();
                }
                if (p != null) {
                    rowTop = p.box.top;
                    row.add(p);
                }
            }
            return pills.isEmpty() ? 0f : y + h;
        }

        private void stretch(List<Pill> row, float width, float gap) {
            if (row.isEmpty()) {
                return;
            }
            float used = 0f;
            for (Pill p : row) {
                used += p.box.width();
            }
            float spare = width - used - gap * (row.size() - 1);
            float each = spare / row.size();
            float x = 0f;
            for (Pill p : row) {
                float pw = p.box.width() + each;
                p.box.set(x, p.box.top, x + pw, p.box.bottom);
                x += pw + gap;
            }
        }

        private static final String[] POLLEN = {"alder", "birch", "grass", "mugwort"};

        private Pill pill(String value, String name, boolean asks) {
            Pill p = new Pill();
            p.value = value;
            p.name = name;
            p.asks = asks;
            pills.add(p);
            return p;
        }

        // -------------------------------------------------------- drawing

        @Override
        protected void onDraw(Canvas canvas) {
            float width = getWidth();
            float y = top + Round.px(12f);

            // The head: the word, the place, and a round key to close.
            float headT = at(0);
            canvas.save();
            canvas.translate(0f, (1f - headT) * Round.px(14f));
            text.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            text.setTextSize(Round.px(30f));
            text.setColor(withAlpha(ink, headT));
            text.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(Words.s("weather"), pad + Round.px(4f), y + Round.px(34f), text);
            if (w.place.length() > 0) {
                float wordW = text.measureText(Words.s("weather"));
                text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                text.setTextSize(Round.px(15f));
                text.setColor(withAlpha(quiet, headT));
                canvas.drawText(w.place, pad + Round.px(4f) + wordW + Round.px(12f), y + Round.px(33f), text);
            }
            float key = Round.px(44f);
            shut.set(width - pad - key, y + Round.px(4f), width - pad, y + Round.px(4f) + key);
            pane(canvas, shut, true, headT);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Round.px(2f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(withAlpha(ink, headT));
            float c = Round.px(7f);
            canvas.drawLine(shut.centerX() - c, shut.centerY() - c, shut.centerX() + c, shut.centerY() + c, paint);
            canvas.drawLine(shut.centerX() + c, shut.centerY() - c, shut.centerX() - c, shut.centerY() + c, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();
            y += Round.px(64f);

            if (!w.known) {
                text.setTextAlign(Paint.Align.LEFT);
                text.setTextSize(Round.px(16f));
                text.setColor(quiet);
                canvas.drawText(Words.s("none"), pad + Round.px(4f), y + Round.px(40f), text);
                return;
            }

            float in = Round.px(14f);

            // The first plate: this moment, and the next two hours.
            float t = at(1);
            RectF plate = new RectF(pad, y, width - pad, y + plateH(heroH + Round.px(10f) + soonH));
            enter(canvas, t, plate);
            plate(canvas, plate);
            hero(canvas, new RectF(plate.left + in, plate.top + in, plate.right - in, plate.top + in + heroH));
            soon(canvas, new RectF(plate.left + in, plate.top + in + heroH + Round.px(10f), plate.right - in,
                plate.top + in + heroH + Round.px(10f) + soonH), t);
            glaze(canvas, plate);
            leave(canvas);
            y = plate.bottom + Round.px(14f);

            // The second: the small facts, and what is flying.
            t = at(2);
            plate = new RectF(pad, y, width - pad, y + plateH(pillsH + (grainsH > 0 ? Round.px(10f) + grainsH : 0f)));
            enter(canvas, t, plate);
            plate(canvas, plate);
            for (Pill p : pills) {
                RectF box = new RectF(p.box);
                box.offset(plate.left + in, plate.top + in);
                pill(canvas, box, p);
            }
            if (grainsH > 0) {
                grains(canvas, new RectF(plate.left + in, plate.top + in + pillsH + Round.px(10f),
                    plate.right - in, plate.top + in + pillsH + Round.px(10f) + grainsH), t);
            }
            glaze(canvas, plate);
            leave(canvas);
            y = plate.bottom + Round.px(14f);

            // The third: twelve hours, two rows of six.
            t = at(3);
            plate = new RectF(pad, y, width - pad, y + plateH(hoursH));
            enter(canvas, t, plate);
            plate(canvas, plate);
            hours(canvas, new RectF(plate.left + in, plate.top + in, plate.right - in, plate.bottom - in));
            glaze(canvas, plate);
            leave(canvas);
            y = plate.bottom + Round.px(14f);

            // The fourth: the days.
            t = at(4);
            plate = new RectF(pad, y, width - pad, y + plateH(daysH));
            enter(canvas, t, plate);
            plate(canvas, plate);
            days(canvas, new RectF(plate.left + in, plate.top + in, plate.right - in, plate.bottom - in), t);
            glaze(canvas, plate);
            leave(canvas);
            y = plate.bottom + Round.px(18f);

            // Where it all comes from.
            t = at(5);
            text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            text.setTextSize(Round.px(12f));
            text.setColor(withAlpha(faint, t));
            text.setTextAlign(Paint.Align.LEFT);
            float lineY = y + Round.px(14f);
            for (String line : wrap(Words.s("sky_sources"), width - 2 * pad - Round.px(8f))) {
                canvas.drawText(line, pad + Round.px(4f), lineY, text);
                lineY += Round.px(17f);
            }
            credit.set(pad, y, width - pad, lineY);
        }

        /**
         * A plate arrives: from a little below, and from nothing. Two layers
         * are opened, and {@link #leave} closes both; the see-through one only
         * while the plate is still arriving, and only as large as the plate.
         */
        private void enter(Canvas canvas, float t, RectF box) {
            canvas.save();
            canvas.translate(0f, (1f - t) * Round.px(22f));
            if (t < 1f) {
                float out = Round.px(4f);
                canvas.saveLayerAlpha(box.left - out, box.top - out, box.right + out, box.bottom + out,
                    Math.round(255 * t));
            } else {
                canvas.save();
            }
        }

        private void leave(Canvas canvas) {
            canvas.restore();
            canvas.restore();
        }

        private void plate(Canvas canvas, RectF box) {
            Path shape = new Path();
            float r = Round.px(30f);
            shape.addRoundRect(box, r, r, Path.Direction.CW);
            if (look.rim != Tile.Look.BARE) {
                canvas.save();
                canvas.translate(box.left, box.top);
                Path local = new Path();
                local.addRoundRect(new RectF(0f, 0f, box.width(), box.height()), r, r, Path.Direction.CW);
                Tile.plate(canvas, local, look.rim, Math.round(box.width()), Math.round(box.height()));
                canvas.restore();
            } else {
                paint.setShader(null);
                paint.setColor(0x8C000000);
                canvas.drawPath(shape, paint);
            }
        }

        private void glaze(Canvas canvas, RectF box) {
            if (look.gloss && look.rim != Tile.Look.BARE) {
                Path shape = new Path();
                float r = Round.px(30f);
                shape.addRoundRect(box, r, r, Path.Direction.CW);
                Tile.glaze(canvas, shape, box.left, box.top, box.width(), Math.min(box.height(), Round.px(220f)));
            }
        }

        /** A pane of dark glass, with the thin line of its cut. */
        private void pane(Canvas canvas, RectF box, boolean round, float alpha) {
            paint.setShader(new RadialGradient(box.centerX(), box.top + box.height() * 0.35f,
                Math.max(box.width(), box.height()) * 0.8f, withAlpha(0xE62A2520, alpha),
                withAlpha(0xF20C0B0A, alpha), Shader.TileMode.CLAMP));
            edge.setAlpha(Math.round(0x66 * alpha));
            if (round) {
                canvas.drawOval(box, paint);
                canvas.drawOval(box, edge);
            } else {
                float r = Math.min(box.height() / 2f, Round.px(20f));
                canvas.drawRoundRect(box, r, r, paint);
                canvas.drawRoundRect(box, r, r, edge);
            }
            paint.setShader(null);
        }

        private void pane(Canvas canvas, RectF box) {
            pane(canvas, box, false, 1f);
        }

        /** The moment: the sky in a round window, the warmth large, what it feels like. */
        private void hero(Canvas canvas, RectF box) {
            float d = box.height();
            RectF round = new RectF(box.left, box.top, box.left + d, box.top + d);
            pane(canvas, round, true, 1f);
            Sky.draw(canvas, new Sky.Now(true, w.degrees, w.code, w.day), round.centerX(), round.centerY(),
                d * 0.56f, ink);
            float left = round.right + Round.px(18f);
            float room = box.right - left;
            String warm = Math.round(w.degrees) + "\u00b0";
            text.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            text.setTextAlign(Paint.Align.LEFT);
            text.setColor(ink);
            text.setTextSize(fit(warm, room, Round.px(76f)));
            canvas.drawText(warm, left, box.top + box.height() * 0.56f, text);
            if (!Float.isNaN(w.feels)) {
                String feels = Words.s("feels") + " " + Math.round(w.feels) + "\u00b0";
                text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                text.setColor(quiet);
                text.setTextSize(fit(feels, room, Round.px(15f)));
                canvas.drawText(feels, left, box.top + box.height() * 0.56f + Round.px(30f), text);
            }
            if (w.high.length > 0) {
                String span = Math.round(w.low[0]) + "\u00b0 \u2013 " + Math.round(w.high[0]) + "\u00b0";
                text.setColor(accent);
                text.setTextSize(fit(span, room, Round.px(15f)));
                canvas.drawText(span, left, box.top + box.height() * 0.56f + Round.px(54f), text);
            }
        }

        /** The next two hours in quarters: a sentence, and a column of water for each. */
        private void soon(Canvas canvas, RectF box, float t) {
            pane(canvas, box);
            int steps = w.water.length;
            float most = 0f;
            int first = -1;
            boolean snow = false;
            for (int i = 0; i < steps; i++) {
                float water = w.water[i] + (i < w.snow.length ? w.snow[i] : 0f);
                most = Math.max(most, water);
                if (first < 0 && water > 0.02f) {
                    first = i;
                    snow = i < w.snow.length && w.snow[i] > w.water[i];
                }
            }
            String says = first < 0 ? Words.s("dry") : first == 0
                ? Words.s(snow ? "snowing" : "raining")
                : Words.s(snow ? "snow_in" : "rain_in") + " " + first * 15 + " " + Words.s("min");
            float inset = Round.px(16f);
            text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            text.setTextSize(Round.px(16f));
            text.setColor(first < 0 ? ink : accent);
            text.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(says, box.left + inset, box.top + Round.px(28f), text);
            if (steps == 0) {
                return;
            }
            String from = w.quarters.length > 0 ? w.quarters[0] : "";
            String to = w.quarters.length > 0 ? w.quarters[w.quarters.length - 1] : "";
            text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            text.setTextSize(Round.px(12f));
            text.setColor(quiet);
            text.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(from + " \u2013 " + to, box.right - inset, box.top + Round.px(27f), text);
            float base = box.bottom - Round.px(16f);
            float tallest = Round.px(38f);
            float barW = (box.width() - 2 * inset) / steps;
            for (int i = 0; i < steps; i++) {
                float water = w.water[i] + (i < w.snow.length ? w.snow[i] : 0f);
                float h = water <= 0.02f || most <= 0f ? Round.px(3f)
                    : Math.max(Round.px(4f), water / most * tallest);
                float rise = Math.max(0f, Math.min(1f, t * 1.4f - i * 0.05f));
                h *= rise;
                paint.setColor(water > 0.02f ? accent : faint);
                RectF bar = new RectF(box.left + inset + i * barW + barW * 0.22f, base - h,
                    box.left + inset + (i + 1) * barW - barW * 0.22f, base);
                canvas.drawRoundRect(bar, Round.px(3f), Round.px(3f), paint);
            }
        }

        /** One small fact, on its own pill of glass. */
        private void pill(Canvas canvas, RectF box, Pill p) {
            pane(canvas, box);
            if (p.asks) {
                Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
                ring.setStyle(Paint.Style.STROKE);
                ring.setStrokeWidth(Round.px(1.5f));
                ring.setColor((accent & 0x00FFFFFF) | 0xAA000000);
                float r = Math.min(box.height() / 2f, Round.px(20f));
                RectF inner = new RectF(box);
                inner.inset(Round.px(1.5f), Round.px(1.5f));
                canvas.drawRoundRect(inner, r, r, ring);
            }
            float x = box.left + Round.px(16f);
            if (p.value.length() > 0) {
                float vy = box.top + Round.px(29f);
                if (p.arrow >= 0) {
                    arrow(canvas, x + Round.px(7f), vy - Round.px(7f), Round.px(7f), (p.arrow + 180) % 360);
                    x += Round.px(20f);
                }
                text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                text.setTextSize(Round.px(20f));
                text.setTextAlign(Paint.Align.LEFT);
                text.setColor(p.asks ? accent : ink);
                canvas.drawText(p.value, x, vy, text);
            }
            text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            text.setTextSize(Round.px(12f));
            text.setColor(quiet);
            text.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(p.name, box.left + Round.px(16f),
                p.value.length() > 0 ? box.top + Round.px(49f) : box.centerY() + Round.px(4f), text);
        }

        /** An arrow turned to where the wind goes. */
        private void arrow(Canvas canvas, float cx, float cy, float r, int toward) {
            canvas.save();
            canvas.rotate(toward, cx, cy);
            Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(Round.px(2f));
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setColor(accent);
            canvas.drawLine(cx, cy + r, cx, cy - r, line);
            canvas.drawLine(cx - r * 0.6f, cy - r * 0.35f, cx, cy - r, line);
            canvas.drawLine(cx + r * 0.6f, cy - r * 0.35f, cx, cy - r, line);
            canvas.restore();
        }

        /** What is flying, kind by kind, a bar for each against fifty grains. */
        private void grains(Canvas canvas, RectF box, float t) {
            pane(canvas, box);
            float inset = Round.px(16f);
            float rowH = (box.height() - 2 * Round.px(10f)) / 4f;
            for (int i = 0; i < 4; i++) {
                float cy = box.top + Round.px(10f) + rowH * (i + 0.5f);
                int count = w.pollen[i];
                text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                text.setTextSize(Round.px(12f));
                text.setColor(quiet);
                text.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(Words.s(POLLEN[i]), box.left + inset, cy + Round.px(4f), text);
                float barL = box.left + Round.px(96f);
                float barR = box.right - Round.px(48f);
                paint.setColor(0x33F4ECDD);
                canvas.drawRoundRect(new RectF(barL, cy - Round.px(3f), barR, cy + Round.px(3f)),
                    Round.px(3f), Round.px(3f), paint);
                float share = count <= 0 ? 0.02f : Math.min(1f, count / 50f);
                paint.setColor(count >= 10 ? accent : quiet);
                canvas.drawRoundRect(new RectF(barL, cy - Round.px(3f), barL + (barR - barL) * share * t,
                    cy + Round.px(3f)), Round.px(3f), Round.px(3f), paint);
                text.setTextAlign(Paint.Align.RIGHT);
                text.setColor(ink);
                canvas.drawText(count < 0 ? "\u2014" : String.valueOf(count), box.right - inset,
                    cy + Round.px(4f), text);
            }
        }

        /** Twelve hours, two rows of six small panes. */
        private void hours(Canvas canvas, RectF box) {
            int n = Math.min(12, w.hours.length);
            float gap = Round.px(8f);
            float cw = (box.width() - 5 * gap) / 6f;
            float ch = Round.px(92f);
            for (int i = 0; i < n; i++) {
                float x = box.left + (i % 6) * (cw + gap);
                float y = box.top + (i / 6) * (ch + gap);
                RectF cell = new RectF(x, y, x + cw, y + ch);
                pane(canvas, cell);
                text.setTextAlign(Paint.Align.CENTER);
                text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                text.setTextSize(Round.px(11f));
                text.setColor(quiet);
                canvas.drawText(i < w.hours.length ? w.hours[i] : "", cell.centerX(), y + Round.px(17f), text);
                int code = i < w.hourCode.length ? w.hourCode[i] : 3;
                boolean day = i >= w.hourDay.length || w.hourDay[i];
                Sky.draw(canvas, new Sky.Now(true, 0f, code, day), cell.centerX(), y + Round.px(40f),
                    Round.px(28f), ink);
                text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                text.setTextSize(Round.px(15f));
                text.setColor(ink);
                canvas.drawText(i < w.hourWarm.length ? Math.round(w.hourWarm[i]) + "\u00b0" : "",
                    cell.centerX(), y + Round.px(68f), text);
                int rain = i < w.hourRain.length ? w.hourRain[i] : -1;
                text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                text.setTextSize(Round.px(11f));
                text.setColor(rain >= 50 ? accent : faint);
                canvas.drawText(rain >= 0 ? rain + "%" : "", cell.centerX(), y + Round.px(84f), text);
            }
        }

        /**
         * The days: each its sky, its name, its chance of rain, and the span
         * from its night to its afternoon drawn against the span of them all.
         */
        private void days(Canvas canvas, RectF box, float t) {
            float coldest = Float.MAX_VALUE;
            float warmest = -Float.MAX_VALUE;
            for (int i = 0; i < w.dates.length && i < w.high.length && i < w.low.length; i++) {
                coldest = Math.min(coldest, w.low[i]);
                warmest = Math.max(warmest, w.high[i]);
            }
            float spread = Math.max(1f, warmest - coldest);
            float rowH = Round.px(58f);
            for (int i = 0; i < w.dates.length; i++) {
                float y = box.top + i * (rowH + Round.px(6f));
                RectF row = new RectF(box.left, y, box.right, y + rowH);
                pane(canvas, row);
                int code = i < w.dayCode.length ? w.dayCode[i] : 3;
                Sky.draw(canvas, new Sky.Now(true, 0f, code, true), row.left + Round.px(30f), row.centerY(),
                    Round.px(30f), ink);
                text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                text.setTextSize(Round.px(15f));
                text.setColor(ink);
                text.setTextAlign(Paint.Align.LEFT);
                // The name, and the chance of rain under it, so the span
                // beside them has room to be read as a span.
                String name = i == 0 ? Words.s("today") : weekday(w.dates[i]);
                float nameRoom = Round.px(100f);
                text.setTextSize(fit(name, nameRoom, Round.px(15f)));
                canvas.drawText(name, row.left + Round.px(56f), row.centerY() - Round.px(1f), text);
                int rain = i < w.dayRain.length ? w.dayRain[i] : -1;
                text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                text.setTextSize(Round.px(11f));
                text.setColor(rain >= 50 ? accent : faint);
                canvas.drawText(rain >= 0 ? rain + "%" : "", row.left + Round.px(56f),
                    row.centerY() + Round.px(15f), text);
                if (i < w.high.length && i < w.low.length) {
                    float lo = w.low[i];
                    float hi = w.high[i];
                    float numW = Round.px(34f);
                    float barL = row.left + Round.px(56f) + nameRoom + numW;
                    float barR = row.right - Round.px(14f) - numW;
                    text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                    text.setTextSize(Round.px(14f));
                    text.setColor(quiet);
                    text.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(Math.round(lo) + "\u00b0", barL - Round.px(6f), row.centerY() + Round.px(5f), text);
                    text.setColor(ink);
                    text.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(Math.round(hi) + "\u00b0", barR + Round.px(6f), row.centerY() + Round.px(5f), text);
                    if (barR > barL) {
                        paint.setColor(0x26F4ECDD);
                        canvas.drawRoundRect(new RectF(barL, row.centerY() - Round.px(3f), barR,
                            row.centerY() + Round.px(3f)), Round.px(3f), Round.px(3f), paint);
                        float a = barL + (barR - barL) * (lo - coldest) / spread;
                        float b = barL + (barR - barL) * (hi - coldest) / spread;
                        float mid = (a + b) / 2f;
                        a = mid + (a - mid) * t;
                        b = mid + (b - mid) * t;
                        paint.setShader(new android.graphics.LinearGradient(a, 0f, Math.max(a + 1f, b), 0f,
                            quiet, accent, Shader.TileMode.CLAMP));
                        canvas.drawRoundRect(new RectF(a, row.centerY() - Round.px(3f), Math.max(a + Round.px(6f), b),
                            row.centerY() + Round.px(3f)), Round.px(3f), Round.px(3f), paint);
                        paint.setShader(null);
                    }
                }
            }
        }

        private String weekday(String date) {
            try {
                String[] parts = date.split("-");
                Calendar when = Calendar.getInstance();
                when.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
                return Words.s(DAYS[when.get(Calendar.DAY_OF_WEEK) - 1]);
            } catch (Exception odd) {
                return date;
            }
        }

        private float fit(String words, float room, float size) {
            text.setTextSize(size);
            float measured = text.measureText(words);
            return measured > room ? size * room / measured : size;
        }

        private List<String> wrap(String words, float room) {
            List<String> lines = new ArrayList<String>();
            StringBuilder line = new StringBuilder();
            for (String word : words.split(" ")) {
                String tried = line.length() == 0 ? word : line + " " + word;
                if (text.measureText(tried) > room && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(tried);
                }
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
            return lines;
        }

        private static int withAlpha(int colour, float share) {
            int a = Math.round(((colour >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, share)));
            return (colour & 0x00FFFFFF) | (a << 24);
        }
    }
}
