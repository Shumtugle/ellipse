package io.github.shumtugle.ellipse;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.StateListAnimator;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * How things give under a finger.
 *
 * A pressed thing sinks a little, quickly; let go, it comes back past
 * where it was and settles, as something with a spring in it does. A
 * tile on a screen sinks further than a button on a card: it is a larger
 * thing, and it is the thing itself.
 */
final class Give {

    private Give() {
    }

    /** For buttons and cards. */
    static StateListAnimator press() {
        return make(0.95f, 2f);
    }

    /** For tiles on the screens and in the drawer. */
    static StateListAnimator tile() {
        return make(0.88f, 3f);
    }

    private static StateListAnimator make(float down, float spring) {
        StateListAnimator give = new StateListAnimator();
        ObjectAnimator in = ObjectAnimator.ofPropertyValuesHolder((Object) null,
            PropertyValuesHolder.ofFloat(View.SCALE_X, down),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, down));
        in.setDuration(Pace.PRESS);
        in.setInterpolator(Pace.STANDARD);
        ObjectAnimator out = ObjectAnimator.ofPropertyValuesHolder((Object) null,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f));
        out.setDuration(Pace.GROW);
        out.setInterpolator(new OvershootInterpolator(spring));
        give.addState(new int[] {android.R.attr.state_pressed}, in);
        give.addState(new int[] {}, out);
        return give;
    }
}
