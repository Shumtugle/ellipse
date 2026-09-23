package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * The floor everything stands on. While something is carried, the finger
 * belongs to the carrying alone: whatever was under it is told the touch
 * is over, and every later move goes to the carrier.
 */
final class Floor extends FrameLayout {

    interface Carrier {
        boolean carrying();

        void move(float x, float y);

        void drop(float x, float y, boolean kept);
    }

    private Carrier carrier;
    private boolean taken;

    Floor(Context context) {
        super(context);
    }

    void carrier(Carrier carrier) {
        this.carrier = carrier;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (carrier == null || !carrier.carrying()) {
            taken = false;
            return super.dispatchTouchEvent(event);
        }
        if (!taken) {
            taken = true;
            MotionEvent over = MotionEvent.obtain(event);
            over.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(over);
            over.recycle();
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                taken = false;
                carrier.drop(event.getX(), event.getY(), true);
                break;
            case MotionEvent.ACTION_CANCEL:
                taken = false;
                carrier.drop(event.getX(), event.getY(), false);
                break;
            default:
                carrier.move(event.getX(), event.getY());
                break;
        }
        return true;
    }
}
