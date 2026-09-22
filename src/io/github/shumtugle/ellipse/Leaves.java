package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

/**
 * Leaves laid side by side and turned by a finger.
 *
 * The finger drags them; let go, a throw turns to the next leaf in its
 * direction, and a slow drag settles on whichever leaf shows most. Past
 * the first or the last leaf the drag gives only half as much, so the
 * edge is felt rather than hit. The leaves are counted by dots, the leaf
 * in view marked in the seed's own colour.
 *
 * What lies on the leaves is for whoever extends this to say.
 */
abstract class Leaves extends ViewGroup {

    private final OverScroller scroller;
    private final int slop;
    private final float throwSpeed;
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int page;
    private VelocityTracker velocity;
    private long lastTime = -1L;
    private int lastAction = -1;
    private float downX;
    private float downY;
    private float lastX;
    private boolean turning;

    Leaves(Context context) {
        super(context);
        scroller = new OverScroller(context, Pace.STANDARD);
        ViewConfiguration config = ViewConfiguration.get(context);
        slop = config.getScaledTouchSlop();
        throwSpeed = config.getScaledMinimumFlingVelocity() * 6f;
        setWillNotDraw(false);
    }

    /** How many leaves there are. */
    abstract int leaves();

    /** Where the dots stand, from the top of the view. */
    abstract float dotsAt();

    int page() {
        return page;
    }

    int slop() {
        return slop;
    }

    /** Keeps the leaf in view when the leaves are laid out again. */
    void settleAfterLayout() {
        page = Math.max(0, Math.min(page, leaves() - 1));
        if (scroller.isFinished()) {
            scrollTo(page * getWidth(), 0);
        }
    }

    void rewind() {
        scroller.abortAnimation();
        page = 0;
        scrollTo(0, 0);
        invalidate();
    }

    // ------------------------------------------------------------ dots

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int total = leaves();
        if (total < 2) {
            return;
        }
        float radius = Round.px(3f);
        float gap = Round.px(14f);
        float y = dotsAt();
        float span = (total - 1) * gap;
        float x = getScrollX() + getWidth() / 2f - span / 2f;
        float at = getWidth() > 0 ? getScrollX() / (float) getWidth() : 0f;
        for (int i = 0; i < total; i++) {
            float near = Math.max(0f, 1f - Math.abs(at - i));
            dot.setColor(mix(Tone.of(Tone.OUTLINE_VARIANT), Tone.of(Tone.PRIMARY), near));
            canvas.drawCircle(x + i * gap, y, radius + Round.px(1f) * near, dot);
        }
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int r = Math.round(ar + (((b >> 16) & 0xFF) - ar) * t);
        int g = Math.round(ag + (((b >> 8) & 0xFF) - ag) * t);
        int bl = Math.round(ab + ((b & 0xFF) - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------ touch

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        watch(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                lastX = downX;
                turning = false;
                if (!scroller.isFinished()) {
                    // Caught while turning: the leaves stop under the finger.
                    scroller.abortAnimation();
                    turning = true;
                }
                return turning;
            case MotionEvent.ACTION_MOVE:
                return turning || decide(e);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                drop();
                return false;
            default:
                return turning;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        watch(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!turning) {
                    downX = e.getX();
                    downY = e.getY();
                    lastX = downX;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!turning) {
                    decide(e);
                }
                if (turning) {
                    drag(e.getX());
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (turning) {
                    settle();
                }
                drop();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (turning) {
                    turnTo(nearest());
                }
                drop();
                return true;
            default:
                return true;
        }
    }

    /** Whether the finger has moved far enough sideways to be turning leaves. */
    boolean moved(MotionEvent e) {
        return Math.abs(e.getX() - downX) > slop || Math.abs(e.getY() - downY) > slop;
    }

    private boolean decide(MotionEvent e) {
        float dx = e.getX() - downX;
        float dy = e.getY() - downY;
        if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
            turning = true;
            lastX = e.getX();
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            began();
            return true;
        }
        return false;
    }

    /** Called when a finger starts turning the leaves. */
    void began() {
    }

    private void drag(float x) {
        float moved = lastX - x;
        lastX = x;
        int limit = (leaves() - 1) * getWidth();
        int at = getScrollX();
        if (at + moved < 0 || at + moved > limit) {
            moved /= 2f;
        }
        scrollBy(Math.round(moved), 0);
        invalidate();
    }

    private int nearest() {
        int width = Math.max(1, getWidth());
        return Math.max(0, Math.min(leaves() - 1, Math.round(getScrollX() / (float) width)));
    }

    private void settle() {
        float speed = 0f;
        if (velocity != null) {
            velocity.computeCurrentVelocity(1000);
            speed = velocity.getXVelocity();
        }
        int width = Math.max(1, getWidth());
        int target;
        if (Math.abs(speed) > throwSpeed) {
            int from = (int) Math.floor(getScrollX() / (float) width);
            target = speed < 0f ? from + 1 : from;
        } else {
            target = nearest();
        }
        turnTo(target);
    }

    /** Turns to a leaf, taking the time a whole turn takes for the share still to go. */
    void turnTo(int target) {
        target = Math.max(0, Math.min(leaves() - 1, target));
        page = target;
        int dx = target * getWidth() - getScrollX();
        float share = Math.min(1f, Math.abs(dx) / (float) Math.max(1, getWidth()));
        int time = (int) Math.max(Pace.PRESS, Math.round(Pace.ARRIVE * share));
        scroller.startScroll(getScrollX(), 0, dx, 0, time);
        postInvalidateOnAnimation();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), 0);
            postInvalidateOnAnimation();
        }
    }

    /** The same event can come twice, once to look at and once to handle; it is counted once. */
    private void watch(MotionEvent e) {
        if (e.getEventTime() == lastTime && e.getActionMasked() == lastAction) {
            return;
        }
        lastTime = e.getEventTime();
        lastAction = e.getActionMasked();
        if (velocity == null) {
            velocity = VelocityTracker.obtain();
        }
        velocity.addMovement(e);
    }

    private void drop() {
        turning = false;
        if (velocity != null) {
            velocity.recycle();
            velocity = null;
        }
    }
}
