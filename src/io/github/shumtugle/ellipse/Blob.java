package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * The round button. In the dock it is the way into every application and
 * carries six marks, two by three, a grid drawn small. On the list of
 * every application it is the way back, and says so in one gesture: an
 * arrow whose shaft runs on into a tick, written as the list arrives.
 * Pressed, it gives a little; leaving, it goes off to the left, as if the
 * list had taken the work and let you go.
 */
final class Blob extends View {

    static final int GRID = 0;
    static final int LEAVE = 1;

    private static final float SQUEEZE = 0.9f;
    private static final long GO = 200L;
    private static final PathInterpolator AWAY = new PathInterpolator(0.3f, 0f, 1f, 1f);

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path gesture = new Path();
    private final Path written = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final float gestureLength;
    private final float size;
    private final int kind;
    private float ink = 1f;
    private ValueAnimator writing;

    Blob(Context context, float sizePx, int kind) {
        super(context);
        size = sizePx;
        this.kind = kind;
        setClickable(true);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeWidth(sizePx * 0.09f);

        /* On the grid of twenty four, centred. The pen goes down the head,
           back up to its point, along the shaft and on into the tick, so the
           whole mark is written in one stroke and fits inside the circle. */
        float u = sizePx / 24f;
        float dy = 0.5f;
        gesture.moveTo(8.5f * u, (7.5f + dy) * u);
        gesture.lineTo(4.5f * u, (11.5f + dy) * u);
        gesture.lineTo(8.5f * u, (15.5f + dy) * u);
        gesture.lineTo(4.5f * u, (11.5f + dy) * u);
        gesture.lineTo(12f * u, (11.5f + dy) * u);
        gesture.lineTo(14.8f * u, (14.3f + dy) * u);
        gesture.lineTo(19.8f * u, (8.8f + dy) * u);
        measure.setPath(gesture, false);
        gestureLength = measure.getLength();
        tint();
    }

    void tint() {
        fill.setColor(Tone.primary());
        mark.setColor(Tone.onAccent());
        line.setColor(Tone.onAccent());
        invalidate();
    }

    /** The gesture writes itself, on the curve and time of any arrival. */
    void write() {
        if (writing != null) {
            writing.cancel();
        }
        animate().cancel();
        setTranslationX(0f);
        setAlpha(1f);
        setScaleX(1f);
        setScaleY(1f);
        ink = 0f;
        writing = ValueAnimator.ofFloat(0f, 1f);
        writing.setDuration(Pace.ARRIVE);
        writing.setStartDelay(Pace.STEP * 2);
        writing.setInterpolator(Pace.EMPHASIS);
        writing.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ink = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        writing.start();
    }

    /** Off to the left, and gone. */
    void depart() {
        animate().cancel();
        animate().translationX(-size * 0.8f).scaleX(0.84f).scaleY(0.84f).alpha(0f)
            .setDuration(GO).setInterpolator(AWAY).start();
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
        if (kind == LEAVE) {
            if (ink >= 1f) {
                canvas.drawPath(gesture, line);
            } else if (ink > 0f) {
                written.reset();
                measure.getSegment(0f, gestureLength * ink, written, true);
                canvas.drawPath(written, line);
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
