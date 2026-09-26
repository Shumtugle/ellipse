package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Several widgets in one place, one shown at a time. A sideways swipe that
 * begins on it turns it to the next or the one before, round and round;
 * the screens do not turn under it. Along its lower edge, inside the frame,
 * one short thin line for each widget, the shown one in the accent. It may
 * turn by itself now and then while it is seen.
 */
final class Pile extends FrameLayout {

    interface Hand {
        /** The pile turned: the widget it shows now, by its place in the pile. */
        void turned(int shown);
    }

    /** How long a pile that turns by itself shows each widget. */
    private static final long DWELL = 12000L;

    private final List<View> faces = new ArrayList<>();
    private final Hand hand;
    private final float density;
    private final int slop;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int shown;
    private boolean turns;
    /** How far above the lower edge the lines stand, to be inside a frame. */
    private float inset;

    private float downX;
    private float downY;
    private boolean claimed;
    private float offset;
    private VelocityTracker speed;

    Pile(Context context, Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        line.setStrokeCap(Paint.Cap.ROUND);
        setWillNotDraw(false);
        setClipChildren(true);
    }

    /** A widget laid in the pile, in order. */
    void add(View face) {
        faces.add(face);
        addView(face, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /** Which one is shown, whether it turns by itself, and how far inside the lower edge the lines stand. */
    void set(int first, boolean turnsItself, float linesInset) {
        shown = faces.isEmpty() ? 0 : Math.max(0, Math.min(faces.size() - 1, first));
        turns = turnsItself;
        inset = linesInset;
        lay(0f);
    }

    int count() {
        return faces.size();
    }

    /** Every face placed for the finger's offset: the shown one moved by it, its neighbour beside it. */
    private void lay(float by) {
        offset = by;
        int w = Math.max(1, getWidth());
        int next = wrap(shown + 1);
        int before = wrap(shown - 1);
        for (int i = 0; i < faces.size(); i++) {
            View face = faces.get(i);
            face.animate().cancel();
            if (i == shown) {
                face.setTranslationX(by);
                face.setVisibility(VISIBLE);
            } else if (by < 0f && i == next) {
                face.setTranslationX(by + w);
                face.setVisibility(VISIBLE);
            } else if (by > 0f && i == before) {
                face.setTranslationX(by - w);
                face.setVisibility(VISIBLE);
            } else {
                face.setTranslationX(0f);
                face.setVisibility(INVISIBLE);
            }
        }
        invalidate();
    }

    private int wrap(int at) {
        int n = Math.max(1, faces.size());
        return ((at % n) + n) % n;
    }

    /** Turned by one, the next or the one before, in an even slide. */
    private void turn(final int way) {
        if (faces.size() < 2) {
            lay(0f);
            return;
        }
        final int w = Math.max(1, getWidth());
        final int to = wrap(shown + way);
        View going = faces.get(shown);
        View coming = faces.get(to);
        coming.setVisibility(VISIBLE);
        if (offset == 0f) {
            coming.setTranslationX(way > 0 ? w : -w);
        }
        going.animate().translationX(way > 0 ? -w : w).setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS)
            .start();
        coming.animate().translationX(0f).setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS)
            .withEndAction(new Runnable() {
                public void run() {
                    shown = to;
                    lay(0f);
                    hand.turned(shown);
                }
            }).start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (faces.size() < 2) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                claimed = false;
                if (speed != null) {
                    speed.recycle();
                }
                speed = VelocityTracker.obtain();
                speed.addMovement(event);
                removeCallbacks(dwell);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (speed != null) {
                    speed.addMovement(event);
                }
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                /* Claimed a little before the screens would take it, so a swipe that begins here turns the pile. */
                if (!claimed && dx > slop * 0.5f && dx > dy * 1.2f) {
                    claimed = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                later();
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!claimed) {
            return super.onTouchEvent(event);
        }
        if (speed != null) {
            speed.addMovement(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                lay(event.getX() - downX);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float fling = 0f;
                if (speed != null) {
                    speed.computeCurrentVelocity(1000);
                    fling = speed.getXVelocity();
                    speed.recycle();
                    speed = null;
                }
                claimed = false;
                boolean far = Math.abs(offset) > getWidth() * 0.25f || Math.abs(fling) > 600f * density;
                if (event.getActionMasked() == MotionEvent.ACTION_UP && far && offset != 0f) {
                    turn(offset < 0f ? 1 : -1);
                } else {
                    settle();
                }
                later();
                return true;
            default:
                return true;
        }
    }

    /** Back to where it was, the neighbour sliding away again. */
    private void settle() {
        final int w = Math.max(1, getWidth());
        View face = faces.get(shown);
        View other = offset < 0f ? faces.get(wrap(shown + 1)) : faces.get(wrap(shown - 1));
        if (other != face) {
            other.animate().translationX(offset < 0f ? w : -w).setDuration(Pace.ARRIVE / 2).start();
        }
        face.animate().translationX(0f).setDuration(Pace.ARRIVE / 2).withEndAction(new Runnable() {
            public void run() {
                lay(0f);
            }
        }).start();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int n = faces.size();
        if (n < 2) {
            return;
        }
        float seg = 13f * density;
        float gap = 4f * density;
        float total = n * seg + (n - 1) * gap;
        float left = getWidth() / 2f - total / 2f;
        float y = getHeight() - inset - 8f * density;
        for (int i = 0; i < n; i++) {
            boolean on = i == shown;
            line.setColor(on ? Tone.primary() : (Tone.onSurface() & 0x00FFFFFF) | 0x66000000);
            line.setStrokeWidth((on ? 2.2f : 1.4f) * density);
            float a = left + i * (seg + gap);
            canvas.drawLine(a, y, a + seg, y, line);
        }
    }

    // ------------------------------------------------------------ by itself

    private final Runnable dwell = new Runnable() {
        public void run() {
            if (turns && faces.size() > 1 && isShown() && !claimed) {
                turn(1);
            }
            later();
        }
    };

    private void later() {
        removeCallbacks(dwell);
        if (turns && faces.size() > 1 && getWindowVisibility() == VISIBLE) {
            postDelayed(dwell, DWELL);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            later();
        } else {
            removeCallbacks(dwell);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(dwell);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            lay(0f);
        }
    }
}
