package io.github.shumtugle.ellipse;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Another application's widget, standing on a screen. It keeps every
 * touch its own application expects, and listens underneath for one
 * thing only: a finger held still for a long press, which takes the
 * widget up to be moved like anything else on the screens.
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
