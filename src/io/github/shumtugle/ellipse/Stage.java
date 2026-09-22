package io.github.shumtugle.ellipse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Everything on the screen, and the hand over it.
 *
 * The stage holds the home screens, the drawer over them, and whatever
 * lies over both for a moment: an offer of things to do, an open folder,
 * a thing being carried. It settles who a moving finger belongs to.
 *
 * The drawer follows the finger: drawn up from anywhere on the home
 * screens, pulled down while its list is at the top. Let go, and it goes
 * where the finger was sending it: a throw decides by its direction, a
 * slow pull by whether the drawer is more than half way.
 *
 * A thing picked up is carried by the stage itself, above everything,
 * until the finger lifts; whoever picked it up is told where it is and
 * where it was let go.
 *
 * While an offer stands, a touch anywhere but on the offer only closes it.
 */
final class Stage extends FrameLayout {

    interface Hand {
        /** How far the drawer is drawn, from nought for closed to one for open. */
        void travel(float shown);
    }

    /** Told about a thing being carried. */
    interface Carrier {
        /** The finger has gone far enough to mean a move, not a press. */
        void moved();

        void over(float x, float y);

        /** Let go. Moved says whether the thing ever left the place it was picked up. */
        void drop(float x, float y, boolean moved);
    }

    private final int slop;
    private final float throwSpeed;

    private Drawer drawer;
    private Hand hand;
    private boolean open;
    private boolean dragging;
    private boolean still;
    private float downX;
    private float downY;
    private float from;
    private VelocityTracker velocity;
    private long lastTime = -1L;
    private int lastAction = -1;
    private ValueAnimator motion;

    private float fingerX;
    private float fingerY;

    private ImageView lifted;
    private Carrier carrier;
    private float liftX;
    private float liftY;
    private boolean far;
    /** Whether whatever held the finger before the lift has been told to let go. */
    private boolean handedOver;

    private Offer offer;
    private boolean swallowing;

    Stage(Context context) {
        super(context);
        ViewConfiguration config = ViewConfiguration.get(context);
        slop = config.getScaledTouchSlop();
        throwSpeed = config.getScaledMinimumFlingVelocity() * 8f;
    }

    void hold(Drawer drawer, Hand hand) {
        this.drawer = drawer;
        this.hand = hand;
    }

    boolean isOpen() {
        return open;
    }

    /** While something lies over the screens, the drawer stays where it is. */
    void still(boolean on) {
        still = on;
    }

    /** Where the finger last touched the stage. */
    float fingerX() {
        return fingerX;
    }

    float fingerY() {
        return fingerY;
    }

    void offered(Offer offer) {
        if (this.offer != null && offer != null && this.offer != offer) {
            this.offer.close();
        }
        this.offer = offer;
    }

    boolean offering() {
        return offer != null;
    }

    void closeOffer() {
        if (offer != null) {
            offer.close();
            offer = null;
        }
    }

    private float shut() {
        return getHeight();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (motion == null || !motion.isRunning()) {
            place(open ? 0f : h);
        }
    }

    private void place(float y) {
        if (drawer == null) {
            return;
        }
        drawer.setTranslationY(y);
        float height = shut();
        drawer.setVisibility(height > 0f && y >= height ? View.INVISIBLE : View.VISIBLE);
        if (hand != null) {
            hand.travel(height > 0f ? 1f - y / height : (open ? 1f : 0f));
        }
    }

    // ------------------------------------------------------------ carrying

    /**
     * Picks a thing up: its picture floats under the finger, a little
     * larger than it stood, until the finger lifts.
     */
    void lift(Bitmap face, Carrier carrier) {
        if (lifted != null) {
            removeView(lifted);
        }
        lifted = new ImageView(getContext());
        lifted.setImageBitmap(face);
        lifted.setElevation(Round.px(8f));
        addView(lifted, new LayoutParams(face.getWidth(), face.getHeight()));
        this.carrier = carrier;
        liftX = fingerX;
        liftY = fingerY;
        far = false;
        float half = face.getWidth() / 2f;
        float halfTall = face.getHeight() / 2f;
        lifted.setPivotX(half);
        lifted.setPivotY(halfTall);
        lifted.setTranslationX(fingerX - half);
        lifted.setTranslationY(fingerY - halfTall);
        lifted.animate().scaleX(1.12f).scaleY(1.12f).setDuration(Pace.PRESS)
            .setInterpolator(Pace.STANDARD).start();
        handedOver = false;
    }

    boolean carrying() {
        return carrier != null;
    }

    private void carry(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        lifted.setTranslationX(x - lifted.getWidth() / 2f);
        lifted.setTranslationY(y - lifted.getHeight() / 2f);
        if (!far && Math.hypot(x - liftX, y - liftY) > slop * 2f) {
            far = true;
            carrier.moved();
        }
        if (far) {
            carrier.over(x, y);
        }
    }

    private void letGo(float x, float y, boolean cancelled) {
        Carrier was = carrier;
        carrier = null;
        if (lifted != null) {
            final ImageView gone = lifted;
            lifted = null;
            gone.animate().alpha(0f).scaleX(1f).scaleY(1f).setDuration(Pace.LEAVE)
                .setInterpolator(Pace.AWAY).withEndAction(new Runnable() {
                    public void run() {
                        removeView(gone);
                    }
                }).start();
        }
        if (was != null) {
            was.drop(x, y, far && !cancelled);
        }
    }

    // ------------------------------------------------------------ touch

    /**
     * A thing picked up takes the finger from whatever held it: that view is
     * told the touch is cancelled, once, and from then on every movement
     * goes to the thing, the lifting of the finger included.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        fingerX = e.getX();
        fingerY = e.getY();
        if (carrier == null) {
            return super.dispatchTouchEvent(e);
        }
        if (!handedOver) {
            handedOver = true;
            MotionEvent cancel = MotionEvent.obtain(e);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();
        }
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_MOVE) {
            carry(e);
        } else if (action == MotionEvent.ACTION_UP) {
            letGo(e.getX(), e.getY(), false);
        } else if (action == MotionEvent.ACTION_CANCEL) {
            letGo(e.getX(), e.getY(), true);
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && offer != null && !offer.holds(e.getX(), e.getY())) {
            closeOffer();
            swallowing = true;
            return true;
        }
        if (swallowing) {
            return true;
        }
        watch(e);
        if (action == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
            dragging = false;
            if (motion != null && motion.isRunning()) {
                // Caught in flight: the drawer stops under the finger.
                motion.cancel();
                begin(e);
                return true;
            }
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE && !dragging) {
            return decide(e);
        }
        return dragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (swallowing) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                swallowing = false;
            }
            return true;
        }
        watch(e);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!dragging) {
                    downX = e.getX();
                    downY = e.getY();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    decide(e);
                }
                if (dragging) {
                    follow(e.getY() - downY);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    settle();
                }
                dragging = false;
                if (velocity != null) {
                    velocity.recycle();
                    velocity = null;
                }
                return true;
            default:
                return true;
        }
    }

    /** Whether a moving finger means the drawer, and if so, takes it. */
    private boolean decide(MotionEvent e) {
        if (drawer == null || still) {
            return false;
        }
        float dx = e.getX() - downX;
        float dy = e.getY() - downY;
        if (Math.abs(dy) < slop || Math.abs(dy) < Math.abs(dx)) {
            return false;
        }
        if (!open && dy < 0f) {
            return begin(e);
        }
        if (open && dy > 0f && drawer.atTop()) {
            return begin(e);
        }
        return false;
    }

    private boolean begin(MotionEvent e) {
        dragging = true;
        from = drawer.getTranslationY();
        downY = e.getY();
        drawer.setVisibility(View.VISIBLE);
        return true;
    }

    private void follow(float dy) {
        place(Math.max(0f, Math.min(shut(), from + dy)));
    }

    private void settle() {
        float speed = 0f;
        if (velocity != null) {
            velocity.computeCurrentVelocity(1000);
            speed = velocity.getYVelocity();
        }
        boolean up;
        if (Math.abs(speed) > throwSpeed) {
            up = speed < 0f;
        } else {
            up = drawer.getTranslationY() < shut() * 0.5f;
        }
        if (up) {
            show(true);
        } else {
            open = false;
            go(shut(), true, Pace.LEAVE, Pace.STANDARD);
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

    // ------------------------------------------------------------ motion

    void show(boolean animate) {
        open = true;
        go(0f, animate, Pace.ARRIVE, Pace.STANDARD);
    }

    /** Closes the drawer: moving if the screen is being looked at, at once if not. */
    void close(boolean animate) {
        open = false;
        go(shut(), animate, Pace.LEAVE, Pace.AWAY);
    }

    /**
     * Moves the drawer to where it is going. A drawer already half way
     * takes half the time: the times in Pace are for the whole journey,
     * and nothing takes less than a press.
     */
    private void go(float to, boolean animate, long time, TimeInterpolator curve) {
        if (motion != null) {
            motion.cancel();
            motion = null;
        }
        float at = drawer.getTranslationY();
        if (!animate || shut() <= 0f || at == to) {
            place(to);
            return;
        }
        float share = Math.abs(to - at) / shut();
        motion = ValueAnimator.ofFloat(at, to);
        motion.setDuration(Math.max(Pace.PRESS, Math.round(time * share)));
        motion.setInterpolator(curve);
        motion.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                place((Float) animation.getAnimatedValue());
            }
        });
        final ValueAnimator mine = motion;
        motion.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (motion == mine) {
                    motion = null;
                }
            }
        });
        motion.start();
    }
}
