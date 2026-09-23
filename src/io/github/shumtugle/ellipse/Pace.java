package io.github.shumtugle.ellipse;

import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

/** The time everything takes, and the curves it takes it on. */
final class Pace {

    /** A press: felt at once, never waited for. */
    static final long PRESS = 140L;
    /** An arrival. */
    static final long ARRIVE = 460L;
    /** The step between neighbours arriving one after another. */
    static final long STEP = 60L;

    /** Fast out, long settle: the curve every arrival rides. */
    static final Interpolator EMPHASIS = new PathInterpolator(0.2f, 0f, 0f, 1f);
    /** A spring that goes a little past home and comes back. */
    static final Interpolator SPRING = new OvershootInterpolator(2.6f);

    /** How far a pressed icon sinks. */
    static final float SINK = 0.88f;

    private Pace() {
    }
}
