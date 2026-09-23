package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * The one round thing on the screen. It sits at the right end of the field
 * and will be the way into the menu, so it can afford to be large. Three
 * marks stand in it, one above the other; pressed, the button gives a little
 * and comes back when the finger lifts.
 */
final class Blob extends View {

    private static final float SQUEEZE = 0.9f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float size;

    Blob(Context context, float sizePx) {
        super(context);
        size = sizePx;
        setClickable(true);
        tint();
    }

    void tint() {
        fill.setColor(Tone.primary());
        mark.setColor(Tone.onAccent());
        invalidate();
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) {
            return;
        }
        float to = pressed ? SQUEEZE : 1f;
        animate().scaleX(to).scaleY(to).setDuration(Pace.PRESS)
            .setInterpolator(pressed ? Pace.EMPHASIS : Pace.SPRING).start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Math.round(size), Math.round(size));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float centre = size / 2f;
        canvas.drawCircle(centre, centre, centre, fill);
        float step = size * 0.17f;
        float dot = size * 0.055f;
        for (int i = -1; i <= 1; i++) {
            canvas.drawCircle(centre, centre + step * i, dot, mark);
        }
    }
}
