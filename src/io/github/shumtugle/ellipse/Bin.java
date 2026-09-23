package io.github.shumtugle.ellipse;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Where things are taken off the screens: while something from a screen
 * is carried away from its place, the dock at the foot gives way to a
 * pill of its own size that says so, and warms to the colour of taking
 * away when the finger is over it. It lies where nothing can be set down
 * anyway, well away from the places a thing is carried between.
 */
final class Bin extends LinearLayout {

    private static final String REMOVE = "Remove";

    private final Glyph cross;
    private final TextView word;
    private final float density;
    private boolean over;
    private boolean shown;

    Bin(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(18), dp(12), dp(22), dp(12));
        cross = new Glyph(context, Glyph.CROSS, dp(22));
        addView(cross);
        word = new TextView(context);
        word.setText(REMOVE);
        word.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
        word.setPadding(dp(10), 0, 0, 0);
        addView(word);
        setElevation(dp(6));
        setVisibility(GONE);
        paint();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    private void paint() {
        int fill = over ? 0xFFE8674A : Tone.containerHigh();
        int ink = over ? 0xFFFFFFFF : Tone.onSurface();
        setBackground(Tone.box(fill, dp(28), over ? 0f : dp(0.5f)));
        cross.tint(ink);
        word.setTextColor(ink);
    }

    /** Comes up in the dock's place, as tall as the dock is. */
    void show(int tall) {
        if (shown) {
            return;
        }
        shown = true;
        over = false;
        paint();
        android.view.ViewGroup.LayoutParams params = getLayoutParams();
        params.height = tall > 0 ? tall : dp(80);
        setLayoutParams(params);
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(dp(24));
        animate().cancel();
        animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
    }

    void hide() {
        if (!shown) {
            return;
        }
        shown = false;
        animate().cancel();
        animate().alpha(0f).translationY(dp(24)).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                    }
                }
            }).start();
    }

    /** Whether a point of the floor is over it; a little above its top edge counts too. */
    boolean holds(float x, float y) {
        if (!shown || getWidth() == 0) {
            return false;
        }
        return x > getLeft() && x < getRight() && y > getTop() - dp(12);
    }

    /** The finger comes over it, or leaves it. */
    void over(boolean now) {
        if (now == over) {
            return;
        }
        over = now;
        paint();
        float s = now ? 1.03f : 1f;
        animate().scaleX(s).scaleY(s).setDuration(Pace.PRESS).setInterpolator(Pace.SPRING).start();
        if (now) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    boolean over() {
        return over;
    }
}
