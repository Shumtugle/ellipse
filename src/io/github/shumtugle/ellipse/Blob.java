package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * The round button. In the dock it is the way into every application and
 * carries six marks, two by three, a grid drawn small. On the list of
 * every application it is that list's menu and carries three marks, one
 * above the other. Pressed, it gives a little and comes back when the
 * finger lifts.
 */
final class Blob extends View {

    static final int GRID = 0;
    static final int MENU = 1;

    private static final float SQUEEZE = 0.9f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float size;
    private final int kind;

    Blob(Context context, float sizePx, int kind) {
        super(context);
        size = sizePx;
        this.kind = kind;
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
        if (kind == MENU) {
            float step = size * 0.17f;
            float dot = size * 0.055f;
            for (int i = -1; i <= 1; i++) {
                canvas.drawCircle(centre, centre + step * i, dot, mark);
            }
            return;
        }
        float across = size * 0.085f;
        float down = size * 0.15f;
        float dot = size * 0.048f;
        for (int row = -1; row <= 1; row++) {
            canvas.drawCircle(centre - across, centre + down * row, dot, mark);
            canvas.drawCircle(centre + across, centre + down * row, dot, mark);
        }
    }
}
