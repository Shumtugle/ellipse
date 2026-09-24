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
    /** Whether the button wears the icons' outline rather than its own circle. */
    private boolean shaped;
    /** A face of the owner's choosing for the door to every app, drawn as an icon; none keeps the button. */
    private android.graphics.drawable.Drawable face;

    void face(android.graphics.drawable.Drawable made) {
        face = made;
        requestLayout();
        invalidate();
    }

    void shaped(boolean on) {
        shaped = on;
        requestLayout();
        invalidate();
    }

    /** While something is carried from the screens, the button becomes the way off them. */
    private boolean bin;
    private boolean binOver;

    Blob(Context context, float sizePx, int kind) {
        super(context);
        size = sizePx;
        this.kind = kind;
        setClickable(true);
        mark.setStrokeCap(Paint.Cap.ROUND);
        mark.setStrokeWidth(sizePx * 0.08f);
        tint();
    }

    /** Becomes the bin, or stops being it: a cross on the colour of taking away. */
    void bin(boolean on) {
        if (bin == on) {
            return;
        }
        bin = on;
        binOver = false;
        invalidate();
        animate().scaleX(1f).scaleY(1f).setDuration(Pace.PRESS).start();
    }

    boolean binning() {
        return bin;
    }

    /** The finger comes over the bin, or leaves it. */
    void binOver(boolean now) {
        if (!bin || now == binOver) {
            return;
        }
        binOver = now;
        float s = now ? 1.18f : 1f;
        animate().scaleX(s).scaleY(s).setDuration(Pace.PRESS).setInterpolator(Pace.SPRING).start();
        if (now) {
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        }
        invalidate();
    }

    boolean binOver() {
        return bin && binOver;
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
        /* Wearing the paper tile, the door is as wide as the tiles beside it. */
        float wide = !shaped || Shape.current == Shape.SYSTEM ? size
            : Shape.current == Shape.PAPER ? size * 0.86f * Shape.PAPER_WIDE
            : size * Shape.weight(Shape.current) * (float) Math.sqrt(Shape.aspect);
        setMeasuredDimension(Math.round(Math.max(size, wide)), Math.round(size));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float centre = size / 2f;
        if (face != null && shaped && !bin) {
            /* Dressed as an icon: drawn as the icons beside it are. */
            float room = getWidth();
            float drawn = size;
            if (face instanceof Shape.Cut) {
                float need = size * ((Shape.Cut) face).wideness();
                if (need > room) {
                    drawn = size * room / need;
                }
            }
            float x = (getWidth() - drawn) / 2f;
            float y = (getHeight() - drawn) / 2f;
            face.setBounds(Math.round(x), Math.round(y), Math.round(x + drawn), Math.round(y + drawn));
            face.draw(canvas);
            return;
        }
        canvas.translate((getWidth() - size) / 2f, 0f);
        if (bin) {
            fill.setColor(binOver ? 0xFFD9472F : 0xFFE8674A);
            canvas.drawCircle(centre, centre, centre, fill);
            mark.setColor(0xFFFFFFFF);
            float arm = size * 0.16f;
            canvas.drawLine(centre - arm, centre - arm, centre + arm, centre + arm, mark);
            canvas.drawLine(centre + arm, centre - arm, centre - arm, centre + arm, mark);
            fill.setColor(Tone.primary());
            mark.setColor(Tone.onAccent());
            return;
        }
        if (shaped && Shape.current != Shape.SYSTEM) {
            /* In the dock, the door to every app wears the outline of the icons beside it. */
            /* The same measure every icon is drawn to, so the door stands
               as large as the icons beside it. */
            float side = Shape.current == Shape.PAPER ? size * 0.86f * Shape.PAPER_WIDE
                : size * Shape.weight(Shape.current);
            float root = Shape.current == Shape.PAPER ? 1f : (float) Math.sqrt(Shape.aspect);
            float w = side * root;
            float h = side / root;
            canvas.save();
            canvas.translate(centre - w / 2f, centre - h / 2f);
            canvas.drawPath(Shape.outline(Shape.current, w, h), fill);
            canvas.restore();
        } else {
            canvas.drawCircle(centre, centre, centre, fill);
        }
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
