package io.github.shumtugle.ellipse;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Where widgets live: the platform's host, with one change to the views
 * it makes.
 *
 * A widget is a window into another application, and its buttons and
 * lists take every touch for themselves, so a long press on it would never
 * reach the screen. Each widget view here listens over its own shoulder:
 * a finger held still for the time of a long press is taken from the
 * widget and handed to the screen, which lifts the widget as it lifts
 * anything else. A finger that moves or lets go in time leaves the widget
 * to do as it would.
 */
final class Host extends AppWidgetHost {

    /** This home screen's number among the platform's widget hosts. */
    static final int ID = 0x0E11;

    Host(Context context) {
        super(context, ID);
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
                                             AppWidgetProviderInfo appWidget) {
        return new Pane(context);
    }

    /** A widget's view that hears a long press over the widget's own buttons. */
    static final class Pane extends AppWidgetHostView {

        private final int slop;
        private float downX;
        private float downY;
        private boolean fired;

        private final Runnable held = new Runnable() {
            public void run() {
                if (isAttachedToWindow() && getParent() != null) {
                    fired = true;
                    performLongClick();
                }
            }
        };

        Pane(Context context) {
            super(context);
            slop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    fired = false;
                    downX = e.getX();
                    downY = e.getY();
                    postDelayed(held, ViewConfiguration.getLongPressTimeout());
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getX() - downX) > slop || Math.abs(e.getY() - downY) > slop) {
                        removeCallbacks(held);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    removeCallbacks(held);
                    break;
                default:
                    break;
            }
            return fired;
        }

        @Override
        public void cancelLongPress() {
            super.cancelLongPress();
            removeCallbacks(held);
        }
    }
}
