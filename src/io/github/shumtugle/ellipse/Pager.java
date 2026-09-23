package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

/**
 * Pages side by side, turned by a sideways stroke of the finger and always
 * coming to rest square on one of them. Under them, a row of points tells
 * how many there are and which one is in front.
 */
final class Pager extends ViewGroup {

    /** Called when another page comes to the front. */
    interface Turn {
        void turned(int page);
    }

    private final OverScroller scroller;
    private final int slop;
    private final int fling;
    private final float density;
    private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);
    private VelocityTracker speed;
    private float downX;
    private float downY;
    private float lastX;
    private boolean dragging;
    private int page;
    private int home = -1;
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Turn turn;

    Pager(Context context) {
        super(context);
        scroller = new OverScroller(context, Pace.EMPHASIS);
        ViewConfiguration config = ViewConfiguration.get(context);
        slop = config.getScaledTouchSlop();
        fling = config.getScaledMinimumFlingVelocity() * 4;
        density = context.getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setClipChildren(false);
        ring.setStyle(Paint.Style.STROKE);
    }

    /** Which page is the home one, ringed among the points; none by default. */
    void home(int which) {
        home = which;
        invalidate();
    }

    void turn(Turn turn) {
        this.turn = turn;
    }

    /** The height kept at the foot for the row of points. */
    int foot() {
        return Math.round(28f * density);
    }

    int page() {
        return page;
    }

    void show(int which, boolean smoothly) {
        int last = Math.max(0, getChildCount() - 1);
        which = which < 0 ? 0 : (which > last ? last : which);
        int to = which * getWidth();
        if (smoothly) {
            scroller.startScroll(getScrollX(), 0, to - getScrollX(), 0, (int) Pace.ARRIVE);
            postInvalidateOnAnimation();
        } else {
            scroller.forceFinished(true);
            scrollTo(to, 0);
        }
        if (which != page) {
            page = which;
            if (turn != null) {
                turn.turned(page);
            }
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int pageHeight = Math.max(0, height - getPaddingTop() - foot());
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(pageHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(i * width, getPaddingTop(), (i + 1) * width,
                getPaddingTop() + child.getMeasuredHeight());
        }
        if (scroller.isFinished()) {
            scrollTo(page * width, 0);
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), 0);
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                lastX = downX;
                dragging = !scroller.isFinished();
                if (dragging) {
                    scroller.forceFinished(true);
                }
                track(event);
                return dragging;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                track(event);
                if (dx > slop && dx > dy) {
                    dragging = true;
                    lastX = event.getX();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        track(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging && Math.abs(event.getX() - downX) > slop) {
                    dragging = true;
                    lastX = event.getX();
                }
                if (dragging) {
                    float by = lastX - event.getX();
                    lastX = event.getX();
                    int max = Math.max(0, (getChildCount() - 1) * getWidth());
                    float to = getScrollX() + by;
                    /* Past either end the page gives only a little, as if
                       held on an elastic. */
                    if (to < 0 || to > max) {
                        to = getScrollX() + by * 0.35f;
                    }
                    scrollTo(Math.round(to), 0);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    speed.computeCurrentVelocity(1000);
                    float v = speed.getXVelocity();
                    int near = Math.round(getScrollX() / (float) Math.max(1, getWidth()));
                    if (Math.abs(v) > fling) {
                        near = v < 0 ? page + 1 : page - 1;
                    }
                    show(near, true);
                }
                dragging = false;
                if (speed != null) {
                    speed.recycle();
                    speed = null;
                }
                return true;
            default:
                return true;
        }
    }

    private void track(MotionEvent event) {
        if (speed == null) {
            speed = VelocityTracker.obtain();
        }
        speed.addMovement(event);
    }

    /** The row of points, standing still while the pages move over it. */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int count = getChildCount();
        if (count < 2) {
            return;
        }
        float gap = 14f * density;
        float y = getHeight() - foot() / 2f;
        float left = getScrollX() + getWidth() / 2f - gap * (count - 1) / 2f;
        float at = getScrollX() / (float) Math.max(1, getWidth());
        for (int i = 0; i < count; i++) {
            float near = Math.max(0f, 1f - Math.abs(at - i));
            point.setColor(near > 0.5f ? Tone.primary() : Tone.faint());
            float r = (2.5f + 1.5f * near) * density;
            canvas.drawCircle(left + gap * i, y, r, point);
            if (i == home) {
                ring.setColor(point.getColor());
                ring.setStrokeWidth(1.2f * density);
                canvas.drawCircle(left + gap * i, y, r + 2.5f * density, ring);
            }
        }
    }
}
