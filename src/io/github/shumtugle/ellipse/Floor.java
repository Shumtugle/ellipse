package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * The floor everything stands on, and the one who settles whom a moving
 * finger belongs to.
 *
 * While something is carried, the finger belongs to the carrying alone:
 * whatever was under it is told the touch is over, and every later move
 * goes to the carrier.
 *
 * Otherwise the first real move decides. Sideways, it is left to what is
 * under the finger: the screens turn, a list scrolls. Up or down, the hand
 * is asked whether it wants a pull that way just now; if it does, the
 * finger is its own from then on, and the hand is told how far it has gone
 * and, at the end, how fast it was going.
 */
final class Floor extends FrameLayout {

    interface Carrier {
        boolean carrying();

        void move(float x, float y);

        void drop(float x, float y, boolean kept);
    }

    interface Hand {
        /** Whether a pull upward, or downward, is wanted now. */
        boolean pullable(boolean up);

        /** How far the finger has gone since the pull was taken, down being positive. */
        void pulled(float by);

        /** Let go, with the speed downward in pixels a second. */
        void released(float by, float speed);
    }

    private final int slop;
    private Carrier carrier;
    /**
     * After a long press has opened a thing's menu, the same finger moving
     * on starts carrying the thing instead; this is how that is begun.
     */
    private Runnable armed;
    private float armedX;
    private float armedY;
    private Hand hand;
    private boolean taken;
    private boolean deciding;
    private boolean pulling;
    private float startX;
    private float startY;
    private VelocityTracker velocity;
    /** Where the finger last was on the floor, for a long press to open beside. */
    private float fingerX;
    private float fingerY;

    Floor(Context context) {
        super(context);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /** Whether the floor leaves every touch to what lies under it: while a clock's rings are set by hand. */
    private boolean still;

    void still(boolean on) {
        still = on;
        deciding = false;
        pulling = false;
    }

    void carrier(Carrier carrier) {
        this.carrier = carrier;
    }

    void hand(Hand hand) {
        this.hand = hand;
    }

    void arm(Runnable begin) {
        armed = begin;
        armedX = fingerX;
        armedY = fingerY;
    }

    float fingerX() {
        return fingerX;
    }

    float fingerY() {
        return fingerY;
    }

    private void cancelBelow(MotionEvent event) {
        MotionEvent over = MotionEvent.obtain(event);
        over.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(over);
        over.recycle();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        fingerX = event.getX();
        fingerY = event.getY();
        if (armed != null) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_DOWN) {
                armed = null;
            } else if (action == MotionEvent.ACTION_MOVE
                && Math.hypot(fingerX - armedX, fingerY - armedY) > slop * 2) {
                Runnable begin = armed;
                armed = null;
                begin.run();
            }
        }
        if (carrier != null && carrier.carrying()) {
            deciding = false;
            pulling = false;
            if (!taken) {
                taken = true;
                cancelBelow(event);
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
        taken = false;
        if (hand == null || still) {
            return super.dispatchTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                deciding = true;
                pulling = false;
                if (velocity != null) {
                    velocity.recycle();
                }
                velocity = VelocityTracker.obtain();
                velocity.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (velocity != null) {
                    velocity.addMovement(event);
                }
                if (pulling) {
                    hand.pulled(event.getY() - startY);
                    return true;
                }
                if (deciding) {
                    float dx = event.getX() - startX;
                    float dy = event.getY() - startY;
                    if (Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx)) {
                        deciding = false;
                        if (event.getPointerCount() == 1 && hand.pullable(dy < 0)) {
                            pulling = true;
                            /* Counted from here, so the pull starts where the
                               finger is and nothing jumps by the slop. */
                            startY = event.getY();
                            cancelBelow(event);
                            hand.pulled(0f);
                            return true;
                        }
                    } else if (Math.abs(dx) > slop) {
                        deciding = false;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pulling) {
                    pulling = false;
                    float speed = 0f;
                    if (velocity != null) {
                        velocity.addMovement(event);
                        velocity.computeCurrentVelocity(1000);
                        speed = velocity.getYVelocity();
                    }
                    hand.released(event.getY() - startY,
                        event.getActionMasked() == MotionEvent.ACTION_CANCEL ? 0f : speed);
                    return true;
                }
                deciding = false;
                break;
            default:
                if (pulling) {
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
