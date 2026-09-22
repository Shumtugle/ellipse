package io.github.shumtugle.ellipse;

import android.app.WallpaperManager;
import android.content.Context;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

/**
 * The home screens, side by side, turned like pages.
 *
 * A finger drags them; let go, a throw turns to the next screen in its
 * direction, and a slow drag settles on whichever screen shows most. The
 * wallpaper is told how far along the screens are, so a wallpaper wider
 * than the phone slides with them.
 *
 * A long press on bare ground, where nothing stands, is passed on with
 * the cell it fell on: that is where a thing asked for there will go.
 *
 * Turned round and round, the screens have no ends: past the last comes
 * the first, and before the first the last. The one beyond an end is
 * drawn there while the finger is over it, and when the turn is done the
 * screens are set back where that screen really lies, which to the eye
 * is no move at all.
 */
final class Board extends ViewGroup {

    interface Hand {
        void ground(int screen, int cx, int cy, float x, float y);
    }

    private final OverScroller scroller;
    private final int slop;
    private final float throwSpeed;
    private final GestureDetector press;
    private final WallpaperManager wallpaper;
    private Hand hand;
    private int page;
    private boolean placed;
    private int wished = -1;
    private boolean endless;
    /** A turn past an end: where the screens are set back to when it is done, or none. */
    private int wrapTo = -1;

    private VelocityTracker velocity;
    private long lastTime = -1L;
    private int lastAction = -1;
    private float downX;
    private float downY;
    private float lastX;
    private boolean dragging;

    Board(Context context) {
        super(context);
        scroller = new OverScroller(context, Pace.STANDARD);
        ViewConfiguration config = ViewConfiguration.get(context);
        slop = config.getScaledTouchSlop();
        throwSpeed = config.getScaledMinimumFlingVelocity() * 6f;
        wallpaper = WallpaperManager.getInstance(context);
        press = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (dragging || hand == null || getChildCount() == 0) {
                    return;
                }
                Sheet sheet = sheet(page);
                int[] cell = sheet.cellAt(e.getX(), e.getY());
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                hand.ground(page, cell[0], cell[1], e.getX(), e.getY());
            }
        });
    }

    void hold(Hand hand) {
        this.hand = hand;
    }

    void endless(boolean on) {
        endless = on;
    }

    private boolean round() {
        return endless && getChildCount() > 1;
    }

    /**
     * Brings the screens back from beyond an end to where the screen in view
     * really lies. What is seen does not change: beyond the end the same
     * screen was being drawn.
     */
    private void normalize() {
        int width = getWidth();
        int span = getChildCount() * width;
        if (!round() || width == 0) {
            return;
        }
        int at = getScrollX();
        if (at < 0) {
            scrollTo(at + span, 0);
        } else if (at > span - width) {
            scrollTo(at - span, 0);
        }
        wrapTo = -1;
    }

    Sheet sheet(int i) {
        return (Sheet) getChildAt(i);
    }

    /** The screen in view; before the screens are there, the one asked for. */
    int page() {
        return wished >= 0 ? wished : page;
    }

    int count() {
        return getChildCount();
    }

    /** The cell under a point of the screen in view. */
    int[] cellAt(float x, float y) {
        return sheet(page).cellAt(x, y);
    }

    // ------------------------------------------------------------ measure

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int w = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int h = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(w, h);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(i * width, 0, (i + 1) * width, b - t);
        }
        if (wished >= 0 && getChildCount() > 0) {
            page = wished;
            wished = -1;
        }
        page = Math.max(0, Math.min(getChildCount() - 1, page));
        if (scroller.isFinished()) {
            scrollTo(page * width, 0);
        }
        placed = true;
        steps();
    }

    // ------------------------------------------------------------ turning

    /**
     * Turns to a screen; before the first layout, the screen is only
     * remembered. Round and round, one past either end means the screen at
     * the other end, reached across the edge.
     */
    void turnTo(int target, boolean animate) {
        int count = getChildCount();
        if (count == 0) {
            // No screens yet: the one asked for is kept as asked, and found
            // among them once they are there. Clamped to none, it was lost,
            // and every home screen built anew opened on the first.
            wished = Math.max(0, target);
            page = wished;
            return;
        }
        int lowest = round() ? -1 : 0;
        int highest = round() ? count : Math.max(0, count - 1);
        target = Math.max(lowest, Math.min(highest, target));
        int real = count == 0 ? 0 : ((target % count) + count) % count;
        if (!placed || getWidth() == 0) {
            wished = real;
            page = real;
            return;
        }
        page = real;
        wished = -1;
        wrapTo = target == real ? -1 : real;
        int dx = target * getWidth() - getScrollX();
        if (!animate || dx == 0) {
            scroller.abortAnimation();
            scrollTo(real * getWidth(), 0);
            wrapTo = -1;
            return;
        }
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
        } else if (wrapTo >= 0) {
            scrollTo(wrapTo * getWidth(), 0);
            wrapTo = -1;
        }
    }

    /** Past an end, the screen from the other end is drawn where it would come. */
    @Override
    protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);
        int count = getChildCount();
        if (!round() || getWidth() == 0) {
            return;
        }
        int width = getWidth();
        if (getScrollX() > (count - 1) * width) {
            canvas.save();
            canvas.translate(count * width, 0);
            getChildAt(0).draw(canvas);
            canvas.restore();
        } else if (getScrollX() < 0) {
            canvas.save();
            canvas.translate(-width, 0);
            getChildAt(count - 1).draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        steps();
    }

    /** Tells the wallpaper how far along the screens are. */
    private void steps() {
        IBinder token = getWindowToken();
        if (token == null) {
            return;
        }
        int total = getChildCount();
        float along = 0.5f;
        if (total > 1 && getWidth() > 0) {
            wallpaper.setWallpaperOffsetSteps(1f / (total - 1), 1f);
            float at = getScrollX() / (float) getWidth();
            if (at < 0f) {
                at = (total - 1) * -at;
            } else if (at > total - 1) {
                at = (total - 1) * (total - at);
            }
            along = Math.max(0f, Math.min(1f, at / (total - 1)));
        }
        try {
            wallpaper.setWallpaperOffsets(token, along, 0.5f);
        } catch (IllegalArgumentException gone) {
            // The window left between the question and the answer.
        }
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
                dragging = false;
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                    normalize();
                    dragging = true;
                }
                return dragging;
            case MotionEvent.ACTION_MOVE:
                return dragging || decide(e);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                drop();
                return false;
            default:
                return dragging;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        watch(e);
        press.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!dragging) {
                    downX = e.getX();
                    downY = e.getY();
                    lastX = downX;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    decide(e);
                }
                if (dragging) {
                    follow(e.getX());
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    settle();
                }
                drop();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    turnTo(nearest(), true);
                }
                drop();
                return true;
            default:
                return true;
        }
    }

    private boolean decide(MotionEvent e) {
        float dx = e.getX() - downX;
        float dy = e.getY() - downY;
        if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
            dragging = true;
            lastX = e.getX();
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }
        return false;
    }

    /**
     * Follows the finger; past the first or the last screen, only half as
     * far, unless the screens go round, when the next one is simply there.
     */
    private void follow(float x) {
        float moved = lastX - x;
        lastX = x;
        int width = getWidth();
        int limit = Math.max(0, getChildCount() - 1) * width;
        int at = getScrollX();
        if (round()) {
            scrollTo(Math.round(at + moved), 0);
            normalizeFar();
            return;
        }
        if (at + moved < 0 || at + moved > limit) {
            moved /= 2f;
        }
        scrollBy(Math.round(moved), 0);
    }

    /** A finger that goes on past the screen beyond an end brings the screens round with it. */
    private void normalizeFar() {
        int width = getWidth();
        int span = getChildCount() * width;
        int at = getScrollX();
        if (at <= -width) {
            scrollTo(at + span, 0);
        } else if (at >= span) {
            scrollTo(at - span, 0);
        }
    }

    private int nearest() {
        int width = Math.max(1, getWidth());
        int lowest = round() ? -1 : 0;
        int highest = round() ? getChildCount() : getChildCount() - 1;
        return Math.max(lowest, Math.min(highest, Math.round(getScrollX() / (float) width)));
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
        turnTo(target, true);
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
        dragging = false;
        if (velocity != null) {
            velocity.recycle();
            velocity = null;
        }
    }
}
