package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * A number chosen by sliding along a track that shows what it chooses.
 *
 * The track is painted with the colours the value will give — the whole
 * circle of hues, or one hue from grey to its fullest — so the choice is
 * made by eye and not by reading a number. The handle is a narrow upright
 * bar with a gap of ground on either side, and it thins while it is held,
 * so the finger does not hide the very colour it is choosing.
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
    private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final Moved moved;
    private int[] colours = {0xFF808080, 0xFF808080};
    private float value;
    private int notch = -1;
    private boolean held;
    /** Sizes in dp: the track, the handle, and the room the dial takes. */
    private float trackTall = 16f;
    private float handleTall = 44f;
    private int roomTall = 48;
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
        handle.setStyle(Paint.Style.FILL);
        track.setStyle(Paint.Style.FILL);
    }

    /** The large dial: a thicker track and a taller handle, for a screen that speaks up. */
    Dial large() {
        trackTall = 24f;
        handleTall = 56f;
        roomTall = 64;
        requestLayout();
        return this;
    }

    void colours(int[] stops) {
        colours = stops;
        shade();
        invalidate();
    }

    void ink(int colour) {
        handle.setColor(colour);
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

    private float inset() {
        return Round.px(12f);
    }

    private void shade() {
        float from = inset();
        float to = Math.max(from + 1f, getWidth() - inset());
        track.setShader(new LinearGradient(from, 0f, to, 0f, colours, null,
            Shader.TileMode.CLAMP));
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
                        held = true;
                        downX = event.getX();
                        getParent().requestDisallowInterceptTouchEvent(true);
                        invalidate();
                    }
                    return true;
                }
                follow(event.getX(), false);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    held = false;
                    follow(event.getX(), true);
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void follow(float x, boolean done) {
        float span = getWidth() - 2f * inset();
        value = clamp(from + (x - downX) / Math.max(1f, span));
        int now = Math.round(value * NOTCHES);
        if (now != notch) {
            if (notch >= 0 && !done) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
            notch = now;
        }
        if (done) {
            notch = -1;
        }
        invalidate();
        if (moved != null) {
            moved.moved(value, done);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight();
        float from = inset();
        float to = getWidth() - inset();
        float x = from + (to - from) * value;
        float tall = Round.px(trackTall);
        float gap = Round.px(6f);
        float wide = Round.px(held ? 2f : 4f);
        float top = (h - tall) / 2f;

        /* The track in two pieces, with ground between them and the handle. */
        float leftEnd = x - wide / 2f - gap;
        if (leftEnd > from) {
            bar.set(from, top, leftEnd, top + tall);
            canvas.drawRoundRect(bar, tall / 2f, tall / 2f, track);
        }
        float rightStart = x + wide / 2f + gap;
        if (rightStart < to) {
            bar.set(rightStart, top, to, top + tall);
            canvas.drawRoundRect(bar, tall / 2f, tall / 2f, track);
        }

        float standing = Round.px(handleTall);
        float handleTop = (h - standing) / 2f;
        bar.set(x - wide / 2f, handleTop, x + wide / 2f, handleTop + standing);
        canvas.drawRoundRect(bar, wide / 2f, wide / 2f, handle);
    }
}
