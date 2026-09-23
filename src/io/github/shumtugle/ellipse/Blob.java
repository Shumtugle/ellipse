package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * The round button. In the dock it is the way into every application and
 * carries six marks, two by three, a grid drawn small. On the list of
 * every application it is that list's menu and carries three marks, one
 * above the other; open, it turns an eighth and the marks draw together
 * into a cross. Pressed, it gives a little and comes back when the finger
 * lifts.
 */
final class Blob extends View {

    static final int GRID = 0;
    static final int MENU = 1;

    private static final float SQUEEZE = 0.9f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float size;
    private final int kind;
    private float open;

    Blob(Context context, float sizePx, int kind) {
        super(context);
        size = sizePx;
        this.kind = kind;
        setClickable(true);
        mark.setStrokeCap(Paint.Cap.ROUND);
        mark.setStrokeWidth(sizePx * 0.09f);
        tint();
    }

    /** How far the menu stands open, from shut to open. */
    void open(float value) {
        open = value < 0f ? 0f : (value > 1f ? 1f : value);
        invalidate();
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
            canvas.save();
            canvas.rotate(45f * open, centre, centre);
            if (open < 1f) {
                float spread = 1f - open;
                for (int i = -1; i <= 1; i++) {
                    canvas.drawCircle(centre, centre + step * i * spread, dot, mark);
                }
            }
            if (open > 0f) {
                float arm = step * open;
                canvas.drawLine(centre - arm, centre, centre + arm, centre, mark);
                canvas.drawLine(centre, centre - arm, centre, centre + arm, mark);
            }
            canvas.restore();
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
