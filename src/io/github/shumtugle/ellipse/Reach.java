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
 * round the places it takes, and a short bar on each side it may grow or
 * shrink by. A whole side can be taken, anywhere along it and a finger's
 * breadth either side of it; a corner takes both its sides. It is drawn
 * across whole places, the thing growing with it as it goes, never over
 * what stands beside it, while the side itself follows the finger
 * smoothly so the hand always sees where it is. The phone's own edge
 * gestures are kept off the frame's sides, so a side at the screen's edge
 * can be taken without going Back. A touch inside the frame is let be; a
 * touch outside it ends the reshaping.
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
    /** The sides taken by the finger now, any of them, a corner being two. */
    private boolean takeLeft;
    private boolean takeTop;
    private boolean takeRight;
    private boolean takeBottom;
    /** Where the finger has the taken sides now, between the lines places snap to. */
    private float fingerX;
    private float fingerY;
    private final java.util.List<android.graphics.Rect> kept = new java.util.ArrayList<>();
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

    /** The sides of the frame where the phone's edge gestures are held back. */
    private void guardEdges(RectF f) {
        if (android.os.Build.VERSION.SDK_INT < 29) {
            return;
        }
        int band = Math.round(40f * density);
        kept.clear();
        kept.add(new android.graphics.Rect(Math.round(f.left) - band, Math.round(f.top),
            Math.round(f.left) + band, Math.round(f.bottom)));
        kept.add(new android.graphics.Rect(Math.round(f.right) - band, Math.round(f.top),
            Math.round(f.right) + band, Math.round(f.bottom)));
        setSystemGestureExclusionRects(kept);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        RectF f = frame();
        guardEdges(f);
        if (held != NONE) {
            /* The taken sides are drawn where the finger is, not where they will snap. */
            if (takeLeft) {
                f.left = Math.min(fingerX, f.right - 24f * density);
            }
            if (takeRight) {
                f.right = Math.max(fingerX, f.left + 24f * density);
            }
            if (takeTop) {
                f.top = Math.min(fingerY, f.bottom - 24f * density);
            }
            if (takeBottom) {
                f.bottom = Math.max(fingerY, f.top + 24f * density);
            }
        }
        float r = Math.min(24f * density, Math.min(f.width(), f.height()) / 2f);
        canvas.drawRoundRect(f, r, r, ring);
        for (float[] h : handles(f)) {
            int side = (int) h[0];
            if (!movable(side)) {
                continue;
            }
            boolean taken = (side == LEFT && takeLeft) || (side == TOP && takeTop)
                || (side == RIGHT && takeRight) || (side == BOTTOM && takeBottom);
            float half = (taken ? 20f : 16f) * density;
            float thick = (taken ? 10f : 8f) * density;
            boolean upright = side == LEFT || side == RIGHT;
            RectF bar = upright
                ? new RectF(h[1] - thick / 2f, h[2] - half, h[1] + thick / 2f, h[2] + half)
                : new RectF(h[1] - half, h[2] - thick / 2f, h[1] + half, h[2] + thick / 2f);
            canvas.drawRoundRect(bar, thick / 2f, thick / 2f, knob);
            canvas.drawRoundRect(bar, thick / 2f, thick / 2f, rim);
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
                RectF f = frame();
                float band = 36f * density;
                boolean along = y > f.top - band && y < f.bottom + band;
                boolean across = x > f.left - band && x < f.right + band;
                takeLeft = wide && along && Math.abs(x - f.left) < band;
                takeRight = wide && along && Math.abs(x - f.right) < band && !takeLeft;
                takeTop = tall && across && Math.abs(y - f.top) < band;
                takeBottom = tall && across && Math.abs(y - f.bottom) < band && !takeTop;
                /* A narrow frame: the nearer side wins where both would do. */
                if (takeLeft && Math.abs(x - f.right) < Math.abs(x - f.left)) {
                    takeLeft = false;
                    takeRight = wide;
                }
                if (takeTop && Math.abs(y - f.bottom) < Math.abs(y - f.top)) {
                    takeTop = false;
                    takeBottom = tall;
                }
                held = takeLeft || takeTop || takeRight || takeBottom ? LEFT : NONE;
                fingerX = x;
                fingerY = y;
                if (held == NONE && !f.contains(x, y)) {
                    end();
                }
                if (held != NONE) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (held != NONE) {
                    fingerX = x;
                    fingerY = y;
                    drag(x, y);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                held = NONE;
                takeLeft = false;
                takeTop = false;
                takeRight = false;
                takeBottom = false;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    /** The taken sides follow the finger, each to the nearest line between places. */
    private void drag(float x, float y) {
        int[] g = new int[2];
        int[] me = new int[2];
        grid.getLocationOnScreen(g);
        getLocationOnScreen(me);
        float gx = x - (g[0] - me[0]) - grid.getPaddingLeft();
        float gy = y - (g[1] - me[1]) - grid.getPaddingTop();
        int lineX = Math.round(gx / grid.cellWidth());
        int lineY = Math.round(gy / grid.cellHeight());
        int left = column;
        int right = column + across;
        int top = row;
        int bottom = row + down;
        if (takeLeft) {
            left = lineX;
        }
        if (takeRight) {
            right = lineX;
        }
        if (takeTop) {
            top = lineY;
        }
        if (takeBottom) {
            bottom = lineY;
        }
        /* Each direction is tried on its own, so a corner that cannot go
           one way still goes the other. */
        int a = right - left;
        int d = bottom - top;
        boolean widthFits = a >= minAcross && a <= maxAcross;
        boolean heightFits = d >= minDown && d <= maxDown;
        if (!widthFits) {
            left = column;
            a = across;
        }
        if (!heightFits) {
            top = row;
            d = down;
        }
        if (left == column && top == row && a == across && d == down) {
            return;
        }
        if (!grid.free(left, top, a, d, thing)) {
            if (grid.free(left, row, a, down, thing) && (a != across || left != column)) {
                top = row;
                d = down;
            } else if (grid.free(column, top, across, d, thing) && (d != down || top != row)) {
                left = column;
                a = across;
            } else {
                return;
            }
        }
        column = left;
        row = top;
        across = a;
        down = d;
        grid.reshape(thing, left, top, a, d);
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
