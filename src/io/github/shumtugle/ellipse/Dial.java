package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * A number chosen by sliding a cap along a groove, as on a mixing desk.
 *
 * The groove is sunk into the card: shade under its upper lip, a thread of
 * light along its lower one, and under it a scale of twenty notches, every
 * fifth a longer one. A dial of colour shows in its groove the colours the
 * value will give, the whole circle of hues or one hue from grey to its
 * fullest; any other dial fills its groove with light from the start as
 * far as the cap, as a gauge does.
 *
 * The cap is cut from the tiles' own material, lit from above, with a
 * glaze if the tiles have one and ridges for the finger. A dial of colour
 * has a window in its cap instead of an index line, and in the window the
 * very colour the cap stands on: the finger covers the track, never the
 * choice. Held, the cap lifts from the groove and its shadow spreads.
 *
 * Every twentieth of the way the phone ticks under the finger.
 *
 * A dial is moved only by a finger that goes sideways along it, and it
 * moves by as much as the finger does, from where it stood: a touch does
 * not make the value jump to the finger. A finger that sets off up or
 * down belongs to the page, which scrolls, and the dial stays as it was;
 * so a page of dials can be read and scrolled without one of them being
 * knocked on the way.
 */
final class Dial extends View {

    interface Moved {
        void moved(float value, boolean done);
    }

    private static final int NOTCHES = 20;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gauge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint notch = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pane = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint picture = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bar = new RectF();
    private final Moved moved;
    private int[] colours = {0xFF808080, 0xFF808080};
    private int light;
    private boolean loupe;
    private int kind = Tile.Look.METAL;
    private boolean gloss = true;
    private Bitmap cap;
    private String capKey = "";
    private float value;
    private int tick = -1;
    private float lift;
    private ValueAnimator lifting;
    /** Sizes in dp: the groove, the cap, and the room the dial takes. */
    private float grooveTall = 10f;
    private float capWide = 22f;
    private float capTall = 40f;
    private int roomTall = 56;
    private final int slop;
    private float downX;
    private float downY;
    private float from;
    private boolean dragging;

    Dial(Context context, float start, Moved listener) {
        super(context);
        value = clamp(start);
        moved = listener;
        slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        cut.setStyle(Paint.Style.STROKE);
        cut.setColor(Cast.CUT);
        notch.setStrokeCap(Paint.Cap.ROUND);
        notch.setColor(0x4DFFFFFF);
    }

    /** The large dial: a deeper groove and a taller cap, for a screen that speaks up. */
    Dial large() {
        grooveTall = 12f;
        capWide = 28f;
        capTall = 50f;
        roomTall = 72;
        requestLayout();
        return this;
    }

    /** What the groove shows: the colours a value gives, from the start to the end. */
    void colours(int[] stops) {
        colours = stops;
        shade();
        invalidate();
    }

    /**
     * The light a gauge fills its groove with as far as the cap; nothing
     * leaves the groove to its colours alone.
     */
    void ink(int colour) {
        light = colour;
        shade();
        invalidate();
    }

    /** The cap: which material it is cut from, and whether it is glazed. */
    void material(int kind, boolean gloss) {
        this.kind = kind;
        this.gloss = gloss;
        invalidate();
    }

    /** A window in the cap showing the colour under it, for a dial of colour. */
    Dial loupe() {
        loupe = true;
        invalidate();
        return this;
    }

    /** The colour of the notches of the scale. */
    void marks(int colour) {
        notch.setColor(colour);
        invalidate();
    }

    void value(float v) {
        value = clamp(v);
        invalidate();
    }

    float value() {
        return value;
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** The groove begins and ends half a cap in, so the cap never leaves the card. */
    private float inset() {
        return Round.px(capWide / 2f + 4f);
    }

    private void shade() {
        float from = inset();
        float to = Math.max(from + 1f, getWidth() - inset());
        track.setShader(new LinearGradient(from, 0f, to, 0f, colours, null, Shader.TileMode.CLAMP));
        float h = getHeight();
        float tall = Round.px(grooveTall);
        float top = (h - tall) / 2f;
        sunk.setShader(new LinearGradient(0f, top, 0f, top + tall,
            new int[] {0x8C000000, 0x1A000000, 0x00000000, 0x33FFFFFF},
            new float[] {0f, 0.45f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        if (light != 0) {
            gauge.setShader(new LinearGradient(from, 0f, to, 0f,
                (light & 0x00FFFFFF) | 0x59000000, light, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        shade();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, Round.dp(roomTall));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                from = value;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
                        dragging = true;
                        downX = event.getX();
                        getParent().requestDisallowInterceptTouchEvent(true);
                        rise(true);
                    }
                    return true;
                }
                follow(event.getX(), false);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    follow(event.getX(), true);
                    getParent().requestDisallowInterceptTouchEvent(false);
                    rise(false);
                }
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /** The cap lifts from the groove when taken, and settles back when let go. */
    private void rise(boolean up) {
        if (lifting != null) {
            lifting.cancel();
        }
        lifting = ValueAnimator.ofFloat(lift, up ? 1f : 0f);
        lifting.setDuration(up ? Pace.PRESS : Pace.GROW);
        lifting.setInterpolator(up ? Pace.STANDARD : Pace.EMPHASIS);
        lifting.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                lift = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        lifting.start();
    }

    private void follow(float x, boolean done) {
        float span = getWidth() - 2f * inset();
        value = clamp(from + (x - downX) / Math.max(1f, span));
        int now = Math.round(value * NOTCHES);
        if (now != tick) {
            if (tick >= 0 && !done) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
            tick = now;
        }
        if (done) {
            tick = -1;
        }
        invalidate();
        if (moved != null) {
            moved.moved(value, done);
        }
    }

    /** The colour the groove shows at a point of it, as the gradient mixes it. */
    private int colourAt(float at) {
        if (colours.length == 1) {
            return colours[0];
        }
        float place = at * (colours.length - 1);
        int i = Math.min(colours.length - 2, (int) Math.floor(place));
        float t = place - i;
        int a = colours[i];
        int b = colours[i + 1];
        int out = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int ca = (a >> shift) & 0xFF;
            int cb = (b >> shift) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return out;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight();
        float from = inset();
        float to = getWidth() - inset();
        float x = from + (to - from) * value;
        float tall = Round.px(grooveTall);
        float top = (h - tall) / 2f;
        float r = tall / 2f;

        // The groove: its colours, or the dark of a gauge with its light
        // as far as the cap; then the shade of its lips over either.
        bar.set(from - r, top, to + r, top + tall);
        canvas.drawRoundRect(bar, r, r, track);
        if (light != 0) {
            bar.set(from - r, top, x + r, top + tall);
            canvas.drawRoundRect(bar, r, r, gauge);
        }
        bar.set(from - r, top, to + r, top + tall);
        canvas.drawRoundRect(bar, r, r, sunk);

        // The scale under the groove.
        float line = Math.max(1f, Round.px(1f));
        notch.setStrokeWidth(line);
        float below = top + tall + Round.px(7f);
        for (int i = 0; i <= NOTCHES; i++) {
            float nx = from + (to - from) * i / NOTCHES;
            float length = Round.px(i % 5 == 0 ? 6f : 3f);
            canvas.drawLine(nx, below, nx, below + length, notch);
        }

        // The cap, and the shadow it throws, spreading as it lifts.
        Bitmap face = cap();
        float cw = face.getWidth();
        float ch = face.getHeight();
        float cy = h / 2f;
        float grow = 1f + 0.08f * lift;
        float fall = Round.px(3f + 5f * lift);
        float spread = cw * (0.75f + 0.25f * lift);
        shadow.setShader(new RadialGradient(x, cy + fall, Math.max(1f, spread),
            new int[] {0x8C000000, 0x33000000, 0x00000000}, new float[] {0f, 0.55f, 1f},
            Shader.TileMode.CLAMP));
        canvas.save();
        canvas.scale(1f, ch / Math.max(1f, spread * 2f) * 1.15f, x, cy + fall);
        canvas.drawCircle(x, cy + fall, spread, shadow);
        canvas.restore();

        canvas.save();
        canvas.scale(grow, grow, x, cy);
        canvas.drawBitmap(face, x - cw / 2f, cy - ch / 2f, picture);
        if (loupe) {
            float side = cw - Round.px(10f);
            bar.set(x - side / 2f, cy - side / 2f, x + side / 2f, cy + side / 2f);
            float round = Round.px(4f);
            pane.setShader(null);
            pane.setColor(colourAt(value));
            canvas.drawRoundRect(bar, round, round, pane);
            glint.setShader(new LinearGradient(0f, bar.top, 0f, bar.centerY(), 0x40FFFFFF, 0x00FFFFFF,
                Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bar, round, round, glint);
            cut.setStrokeWidth(Math.max(1f, Round.px(1f)));
            canvas.drawRoundRect(bar, round, round, cut);
        }
        canvas.restore();
    }

    /**
     * The cap, cut once for its material and size and kept as a picture:
     * a rounded block lit from above, its edge a thin dark line, ridges to
     * hold it by, and, on a gauge, an index line down its middle in the
     * ink words take on that material.
     */
    private Bitmap cap() {
        int cw = Round.dp(capWide);
        int ch = Round.dp(capTall);
        String key = cw + ":" + ch + ":" + kind + ":" + gloss + ":" + loupe + ":" + Cast.ink(kind);
        if (cap != null && key.equals(capKey)) {
            return cap;
        }
        Bitmap made = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(made);
        Path shape = Cast.box(0f, 0f, cw, ch, Round.px(7f));
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Tile.material(p, kind, cw, ch);
        c.drawPath(shape, p);
        Paint bevel = new Paint(Paint.ANTI_ALIAS_FLAG);
        bevel.setShader(Cast.bevel(0f, ch));
        c.drawPath(shape, bevel);

        Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
        dark.setStrokeWidth(Math.max(1f, Round.px(1f)));
        dark.setStrokeCap(Paint.Cap.ROUND);
        dark.setColor(0x66000000);
        Paint bright = new Paint(dark);
        bright.setColor(Cast.light(kind) ? 0x73FFFFFF : 0x33FFFFFF);
        float mid = cw / 2f;
        float step = Round.px(5f);
        float one = Math.max(1f, Round.px(1f));
        if (loupe) {
            float side = cw - Round.px(10f);
            float edge = Round.px(5f);
            float upTo = (ch - side) / 2f - Round.px(3f);
            float downFrom = (ch + side) / 2f + Round.px(3f);
            for (int i = -1; i <= 1; i++) {
                float gx = mid + i * step;
                c.drawLine(gx, edge, gx, upTo, dark);
                c.drawLine(gx + one, edge, gx + one, upTo, bright);
                c.drawLine(gx, downFrom, gx, ch - edge, dark);
                c.drawLine(gx + one, downFrom, gx + one, ch - edge, bright);
            }
        } else {
            float edge = Round.px(9f);
            for (int i = -1; i <= 1; i += 2) {
                float gx = mid + i * Round.px(7f);
                c.drawLine(gx, edge + step, gx, ch - edge - step, dark);
                c.drawLine(gx + one, edge + step, gx + one, ch - edge - step, bright);
            }
            Paint index = new Paint(Paint.ANTI_ALIAS_FLAG);
            index.setStrokeCap(Paint.Cap.ROUND);
            index.setStrokeWidth(Round.px(2.5f));
            index.setColor(Cast.ink(kind));
            c.drawLine(mid, edge, mid, ch - edge, index);
        }
        if (gloss) {
            Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
            light.setShader(Cast.glazeLight(0f, ch));
            c.drawPath(Cast.glaze(shape, 0f, 0f, cw, ch), light);
        }
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        float stroke = Math.max(1f, Round.px(0.8f));
        rim.setStrokeWidth(stroke);
        rim.setColor(0x8C000000);
        c.drawPath(Cast.box(stroke / 2f, stroke / 2f, cw - stroke / 2f, ch - stroke / 2f,
            Round.px(7f) - stroke / 2f), rim);
        cap = made;
        capKey = key;
        return cap;
    }
}
