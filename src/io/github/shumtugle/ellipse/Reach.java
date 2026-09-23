package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * The frame a widget or a folder is reshaped in: a ring of the accent
 * round the places it takes, and a round handle in the middle of each
 * side it may grow or shrink by. A handle is drawn across whole places,
 * the thing growing with it as it goes, never over what stands beside
 * it; a touch anywhere else ends the reshaping.
 */
final class Reach extends View {

    interface Done {
        void done(int column, int row, int across, int down);
    }

    private static final int NONE = 0;
    private static final int LEFT = 1;
    private static final int TOP = 2;
    private static final int RIGHT = 3;
    private static final int BOTTOM = 4;

    private final Grid grid;
    private final View thing;
    private final int minAcross;
    private final int minDown;
    private final int maxAcross;
    private final int maxDown;
    private final boolean wide;
    private final boolean tall;
    private final Done done;
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private int column;
    private int row;
    private int across;
    private int down;
    private int held = NONE;
    private boolean ended;

    Reach(Context context, Grid grid, View thing, int[] block, int minAcross, int minDown,
          int maxAcross, int maxDown, boolean wide, boolean tall, Done done) {
        super(context);
        this.grid = grid;
        this.thing = thing;
        this.column = block[0];
        this.row = block[1];
        this.across = block[2];
        this.down = block[3];
        this.minAcross = minAcross;
        this.minDown = minDown;
        this.maxAcross = maxAcross;
        this.maxDown = maxDown;
        this.wide = wide;
        this.tall = tall;
        this.done = done;
        density = context.getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2f * density);
        ring.setColor(Tone.primary());
        knob.setColor(Tone.primary());
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(2f * density);
        rim.setColor(Tone.onAccent());
        setAlpha(0f);
        animate().alpha(1f).setDuration(Pace.ARRIVE / 2).start();
    }

    /** The block, in this view's own coordinates. */
    private RectF frame() {
        RectF box = grid.block(column, row, across, down);
        int[] g = new int[2];
        int[] me = new int[2];
        grid.getLocationOnScreen(g);
        getLocationOnScreen(me);
        box.offset(g[0] - me[0], g[1] - me[1]);
        float inset = 2f * density;
        box.inset(inset, inset);
        return box;
    }

    private float[][] handles(RectF f) {
        return new float[][] {
            {LEFT, f.left, f.centerY()}, {TOP, f.centerX(), f.top},
            {RIGHT, f.right, f.centerY()}, {BOTTOM, f.centerX(), f.bottom}
        };
    }

    private boolean movable(int side) {
        return (side == LEFT || side == RIGHT) ? wide : tall;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        RectF f = frame();
        float r = Math.min(24f * density, Math.min(f.width(), f.height()) / 2f);
        canvas.drawRoundRect(f, r, r, ring);
        for (float[] h : handles(f)) {
            if (!movable((int) h[0])) {
                continue;
            }
            float size = (held == (int) h[0] ? 9f : 7f) * density;
            canvas.drawCircle(h[1], h[2], size, knob);
            canvas.drawCircle(h[1], h[2], size, rim);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ended) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                held = NONE;
                float best = 32f * density;
                for (float[] h : handles(frame())) {
                    float d = (float) Math.hypot(h[1] - x, h[2] - y);
                    if (movable((int) h[0]) && d < best) {
                        best = d;
                        held = (int) h[0];
                    }
                }
                if (held == NONE) {
                    end();
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (held != NONE) {
                    drag(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                held = NONE;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    /** The held side follows the finger to the nearest line between places. */
    private void drag(float x, float y) {
        int[] g = new int[2];
        int[] me = new int[2];
        grid.getLocationOnScreen(g);
        getLocationOnScreen(me);
        float gx = x - (g[0] - me[0]) - grid.getPaddingLeft();
        float gy = y - (g[1] - me[1]) - grid.getPaddingTop();
        int line = held == LEFT || held == RIGHT
            ? Math.round(gx / grid.cellWidth()) : Math.round(gy / grid.cellHeight());
        int c = column;
        int r = row;
        int a = across;
        int d = down;
        if (held == LEFT) {
            c = line;
            a = column + across - line;
        } else if (held == RIGHT) {
            a = line - column;
        } else if (held == TOP) {
            r = line;
            d = row + down - line;
        } else {
            d = line - row;
        }
        if (a < minAcross || a > maxAcross || d < minDown || d > maxDown) {
            return;
        }
        if (c == column && r == row && a == across && d == down) {
            return;
        }
        if (!grid.free(c, r, a, d, thing)) {
            return;
        }
        column = c;
        row = r;
        across = a;
        down = d;
        grid.reshape(thing, c, r, a, d);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        invalidate();
    }

    /** Ends the reshaping and hands on the block the thing now takes. */
    void end() {
        if (ended) {
            return;
        }
        ended = true;
        animate().alpha(0f).setDuration(Pace.ARRIVE / 3).withEndAction(new Runnable() {
            public void run() {
                if (getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) getParent()).removeView(Reach.this);
                }
            }
        }).start();
        done.done(column, row, across, down);
    }

    boolean ended() {
        return ended;
    }
}
