package io.github.shumtugle.ellipse;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Another application's widget, standing on a screen. It keeps every
 * touch its own application expects, and listens underneath for one
 * thing only: a finger held still for a long press, which takes the
 * widget up to be moved like anything else on the screens.
 *
 * A widget that does not fit the places it was given, because its own
 * layout will not come down to that size, is not cut off: it is laid out
 * as large as it needs and then drawn smaller, the same in both
 * directions, so nothing is lost and nothing is bent out of shape. A
 * widget that fits is left exactly as it is.
 */
final class Piece extends AppWidgetHostView {

    /** The host that makes widgets into pieces of this kind. */
    static final class Host extends AppWidgetHost {
        Host(Context context, int id) {
            super(context, id);
        }

        @Override
        protected AppWidgetHostView onCreateView(Context context, int id, AppWidgetProviderInfo info) {
            return new Piece(context);
        }
    }

    /** How much smaller than itself the widget is drawn; one when it fits. */
    private float fit = 1f;
    private static final float LEAST = 0.6f;
    private static final float STEP = 0.04f;
    private final int slop;
    private float downX;
    private float downY;
    private boolean held;

    private final Runnable longPress = new Runnable() {
        public void run() {
            if (getParent() != null && isAttachedToWindow()) {
                held = performLongClick();
            }
        }
    };

    Piece(Context context) {
        super(context);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                held = false;
                downX = event.getX();
                downY = event.getY();
                removeCallbacks(longPress);
                postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - downX) > slop || Math.abs(event.getY() - downY) > slop) {
                    removeCallbacks(longPress);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                break;
            default:
                break;
        }
        /* Once taken up, the rest of the touch is no longer the widget's. */
        return held;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int wide = MeasureSpec.getSize(widthSpec);
        int tall = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(wide, tall);
        int innerWide = wide - getPaddingLeft() - getPaddingRight();
        int innerTall = tall - getPaddingTop() - getPaddingBottom();
        fit = 1f;
        for (int i = 1; i < getChildCount(); i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(Math.max(0, innerWide), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.max(0, innerTall), MeasureSpec.EXACTLY));
        }
        if (getChildCount() == 0 || innerWide <= 0 || innerTall <= 0) {
            return;
        }
        View content = getChildAt(0);
        /* The largest scale at which the widget's own layout holds together:
           tried from full size down, each time laid out in a box as much
           larger as it will be drawn smaller. */
        for (float s = 1f; s >= LEAST - 0.001f; s -= STEP) {
            int boxWide = Math.round(innerWide / s);
            int boxTall = Math.round(innerTall / s);
            content.measure(MeasureSpec.makeMeasureSpec(boxWide, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(boxTall, MeasureSpec.EXACTLY));
            content.layout(0, 0, boxWide, boxTall);
            fit = s;
            if (holds(content, 0, 0, boxWide, boxTall, true)) {
                break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() == 0) {
            return;
        }
        int left = getPaddingLeft();
        int top = getPaddingTop();
        for (int i = 1; i < getChildCount(); i++) {
            View other = getChildAt(i);
            other.layout(left, top, left + other.getMeasuredWidth(), top + other.getMeasuredHeight());
        }
        View content = getChildAt(0);
        content.setPivotX(0f);
        content.setPivotY(0f);
        content.setScaleX(fit);
        content.setScaleY(fit);
        content.layout(left, top, left + content.getMeasuredWidth(), top + content.getMeasuredHeight());
    }

    /**
     * Whether everything that shows lies within the box and got the height
     * it asked for. What scrolls is let be: its inside is meant to run on.
     */
    private static boolean holds(View view, int x, int y, int wide, int tall, boolean root) {
        if (view.getVisibility() != VISIBLE) {
            return true;
        }
        int left = x + (root ? 0 : view.getLeft());
        int top = y + (root ? 0 : view.getTop());
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        if (left < -1 || top < -1 || right > wide + 1 || bottom > tall + 1) {
            return false;
        }
        if (view.getMeasuredHeight() > view.getHeight() + 1 && !(view instanceof android.view.ViewGroup)) {
            return false;
        }
        if (view instanceof android.widget.AbsListView || view instanceof android.widget.ScrollView
            || view instanceof android.widget.HorizontalScrollView) {
            return true;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (!holds(group.getChildAt(i), left, top, wide, tall, false)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The widget is told the size it really has, in the platform's units, whenever it changes. */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;
        int wide = Math.round(w / density);
        int tall = Math.round(h / density);
        if (wide > 0 && tall > 0) {
            updateAppWidgetSize(null, wide, tall, wide, tall);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        removeCallbacks(longPress);
    }
}
