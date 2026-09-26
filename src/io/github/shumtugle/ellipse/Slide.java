package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * A slider of the settings, drawn as the colour page draws its own: a full
 * round track, the part behind the thumb in the accent, a round thumb. It
 * tells every step while the finger moves, and once more when it lifts.
 */
final class Slide extends View {

    interface Moved {
        void moved(int value, boolean done);
    }

    private final int least;
    private final int most;
    private int value;
    private final Moved moved;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final float density;

    Slide(Context context, int least, int most, int value, Moved moved) {
        super(context);
        this.least = least;
        this.most = most;
        this.value = Math.max(least, Math.min(most, value));
        this.moved = moved;
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Math.round(44 * density));
    }

    private float edge() {
        return 22 * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = 16 * density;
        float cy = getHeight() / 2f;
        track.set(edge() - h / 2f, cy - h / 2f, getWidth() - edge() + h / 2f, cy + h / 2f);
        paint.setColor(Tone.containerHigh());
        canvas.drawRoundRect(track, h / 2f, h / 2f, paint);
        float at = edge() + (getWidth() - 2 * edge()) * (value - least) / (float) (most - least);
        track.right = at + h / 2f;
        paint.setColor(Tone.primary());
        canvas.drawRoundRect(track, h / 2f, h / 2f, paint);
        paint.setColor(Tone.onSurface());
        canvas.drawCircle(at, cy, 13 * density, paint);
    }

    /*
     * A finger that comes down on the slider is not yet the slider's: the
     * page may be scrolling. It becomes the slider's only when it has gone
     * clearly sideways, or when it lifts where it came down, as a tap; a
     * finger going up or down is handed back to the page, and the value is
     * left as it was.
     */
    private float downX;
    private float downY;
    private boolean holding;
    private boolean given;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float slop = android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            holding = false;
            given = false;
            return true;
        }
        if (given) {
            return false;
        }
        if (!holding) {
            float dx = Math.abs(event.getX() - downX);
            float dy = Math.abs(event.getY() - downY);
            if (action == MotionEvent.ACTION_MOVE) {
                if (dy > slop && dy > dx) {
                    given = true;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                }
                if (dx > slop * 1.5f && dx > dy * 1.5f) {
                    holding = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else {
                    return true;
                }
            } else if (action == MotionEvent.ACTION_CANCEL) {
                return false;
            } else if (action == MotionEvent.ACTION_UP && (dx > slop || dy > slop)) {
                return false;
            }
        }
        float f = (event.getX() - edge()) / Math.max(1f, getWidth() - 2 * edge());
        int now = Math.round(least + Math.max(0f, Math.min(1f, f)) * (most - least));
        boolean done = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
        if (now != value) {
            value = now;
            invalidate();
            if (value % 5 == 0) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
            moved.moved(value, false);
        }
        if (done) {
            moved.moved(value, true);
        }
        return true;
    }
}
