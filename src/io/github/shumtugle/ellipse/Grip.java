package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * The frame around a widget or the clock while it is being set: a line
 * about it and a handle on each of its four sides. A handle drawn outwards
 * takes another row or column of the grid, drawn inwards gives one back,
 * and the thing follows at once under the finger. A handle that cannot be
 * moved, because the cells beyond it are taken or the grid ends there,
 * simply does not move.
 *
 * The frame lies over the whole glass but answers only near its handles;
 * a touch anywhere else falls through to what is under it.
 */
final class Grip extends View {

    interface Hand {
        /** Asked for a place and a size in cells; answers whether they were taken. */
        boolean set(int x, int y, int w, int h);

        /** The finger has let go. */
        void done();

        /** A touch outside the handles: the frame is not wanted any more. */
        void away();
    }

    private static final int NONE = -1;
    private static final int LEFT = 0;
    private static final int TOP = 1;
    private static final int RIGHT = 2;
    private static final int BOTTOM = 3;

    private final Hand hand;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    private float left;
    private float top;
    private float cellW = 1f;
    private float cellH = 1f;
    private int columns = 1;
    private int rows = 1;
    private int x;
    private int y;
    private int w = 1;
    private int h = 1;
    private int held = NONE;

    Grip(Context context, Hand hand) {
        super(context);
        this.hand = hand;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, Round.px(2f)));
        shade.setStyle(Paint.Style.STROKE);
        shade.setStrokeWidth(Math.max(1f, Round.px(4f)));
        shade.setColor(0x59000000);
        tint();
    }

    /** Colours from the scheme, so the frame is seen on any table. */
    void tint() {
        line.setColor(Tone.of(Tone.PRIMARY));
        knob.setColor(Tone.of(Tone.PRIMARY));
        invalidate();
    }

    /**
     * Where the grid lies on the glass and where the thing stands in it:
     * the first cell's corner, the size of a cell, how many cells there
     * are, and the block the thing takes.
     */
    void fit(float left, float top, float cellW, float cellH, int columns, int rows,
        int x, int y, int w, int h) {
        this.left = left;
        this.top = top;
        this.cellW = Math.max(1f, cellW);
        this.cellH = Math.max(1f, cellH);
        this.columns = Math.max(1, columns);
        this.rows = Math.max(1, rows);
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        invalidate();
    }

    private void shape() {
        box.set(left + x * cellW, top + y * cellH, left + (x + w) * cellW, top + (y + h) * cellH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        shape();
        float r = Round.px(18f);
        float in = Round.px(3f);
        RectF inner = new RectF(box.left + in, box.top + in, box.right - in, box.bottom - in);
        canvas.drawRoundRect(inner, r, r, shade);
        canvas.drawRoundRect(inner, r, r, line);
        float knobR = Round.px(9f);
        canvas.drawCircle(inner.left, inner.centerY(), knobR, shade);
        canvas.drawCircle(inner.right, inner.centerY(), knobR, shade);
        canvas.drawCircle(inner.centerX(), inner.top, knobR, shade);
        canvas.drawCircle(inner.centerX(), inner.bottom, knobR, shade);
        canvas.drawCircle(inner.left, inner.centerY(), knobR, knob);
        canvas.drawCircle(inner.right, inner.centerY(), knobR, knob);
        canvas.drawCircle(inner.centerX(), inner.top, knobR, knob);
        canvas.drawCircle(inner.centerX(), inner.bottom, knobR, knob);
    }

    /** Which handle a finger came down on, or none. */
    private int handleAt(float fx, float fy) {
        shape();
        float reach = Round.px(28f);
        if (near(fx, fy, box.left, box.centerY(), reach)) {
            return LEFT;
        }
        if (near(fx, fy, box.right, box.centerY(), reach)) {
            return RIGHT;
        }
        if (near(fx, fy, box.centerX(), box.top, reach)) {
            return TOP;
        }
        if (near(fx, fy, box.centerX(), box.bottom, reach)) {
            return BOTTOM;
        }
        return NONE;
    }

    private static boolean near(float fx, float fy, float px, float py, float reach) {
        return Math.abs(fx - px) <= reach && Math.abs(fy - py) <= reach;
    }

    private int columnAt(float fx) {
        return Math.max(0, Math.min(columns - 1, (int) Math.floor((fx - left) / cellW)));
    }

    private int rowAt(float fy) {
        return Math.max(0, Math.min(rows - 1, (int) Math.floor((fy - top) / cellH)));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                held = handleAt(event.getX(), event.getY());
                if (held == NONE) {
                    // Let the touch through to whatever is under, and go.
                    post(new Runnable() {
                        public void run() {
                            hand.away();
                        }
                    });
                    return false;
                }
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (held != NONE) {
                    drag(event.getX(), event.getY());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (held != NONE) {
                    held = NONE;
                    hand.done();
                }
                return true;
            default:
                return held != NONE;
        }
    }

    /** A handle drawn to a cell: the block asked for, if it can be had. */
    private void drag(float fx, float fy) {
        int nx = x;
        int ny = y;
        int nw = w;
        int nh = h;
        if (held == RIGHT) {
            nw = columnAt(fx) - x + 1;
        } else if (held == LEFT) {
            nx = columnAt(fx);
            nw = x + w - nx;
        } else if (held == BOTTOM) {
            nh = rowAt(fy) - y + 1;
        } else {
            ny = rowAt(fy);
            nh = y + h - ny;
        }
        if (nw < 1 || nh < 1 || (nx == x && ny == y && nw == w && nh == h)) {
            return;
        }
        if (hand.set(nx, ny, nw, nh)) {
            x = nx;
            y = ny;
            w = nw;
            h = nh;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            invalidate();
        }
    }
}
