package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

/**
 * A plain grid of equal places. Each child is told its column and row and
 * gets exactly one place; nothing spans, nothing floats.
 *
 * While an icon is carried over it, the grid shows where it could land: a
 * faint point in every free place, and a ring of the accent round the one
 * the icon would drop into.
 */
final class Grid extends ViewGroup {

    final int columns;
    final int rows;

    private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wash = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float iconSize;
    private float below;
    /** How much of the landing guide shows, from nothing to all of it. */
    private float guide;
    private ValueAnimator guiding;
    private int targetX = -1;
    private int targetY = -1;

    Grid(Context context, int columns, int rows) {
        super(context);
        this.columns = columns;
        this.rows = rows;
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(false);
        ring.setStyle(Paint.Style.STROKE);
    }

    void put(View child, int column, int row) {
        child.setTag(new int[] {column, row});
        addView(child);
    }

    /** How icons stand in their places: their size, and how tall the name under them is. */
    void shape(float iconSize, float below) {
        this.iconSize = iconSize;
        this.below = below;
    }

    float cellWidth() {
        return (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) / (float) columns;
    }

    float cellHeight() {
        return (getMeasuredHeight() - getPaddingTop() - getPaddingBottom()) / (float) rows;
    }

    /** The centre of the icon in a place, in the grid's own coordinates. */
    float[] centre(int column, int row) {
        float w = cellWidth();
        float h = cellHeight();
        float x = getPaddingLeft() + (column + 0.5f) * w;
        float y = getPaddingTop() + row * h + (h - iconSize - below) / 2f + iconSize / 2f;
        return new float[] {x, y};
    }

    /** The place under a point of the grid, or null outside it. */
    int[] cellAt(float x, float y) {
        float w = cellWidth();
        float h = cellHeight();
        int column = (int) Math.floor((x - getPaddingLeft()) / w);
        int row = (int) Math.floor((y - getPaddingTop()) / h);
        if (x < 0 || y < 0 || x > getWidth() || y > getHeight()
            || column < 0 || row < 0 || column >= columns || row >= rows) {
            return null;
        }
        return new int[] {column, row};
    }

    boolean free(int column, int row) {
        for (int i = 0; i < getChildCount(); i++) {
            int[] at = (int[]) getChildAt(i).getTag();
            if (at[0] == column && at[1] == row) {
                return false;
            }
        }
        return true;
    }

    /** The free place nearest to a point of the grid, or null when none is left. */
    int[] nearestFree(float x, float y) {
        int[] best = null;
        float bestDistance = Float.MAX_VALUE;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (!free(column, row)) {
                    continue;
                }
                float[] c = centre(column, row);
                float d = (c[0] - x) * (c[0] - x) + (c[1] - y) * (c[1] - y);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = new int[] {column, row};
                }
            }
        }
        return best;
    }

    /** Shows or hides the landing guide, fading it in and out. */
    void carrying(boolean on) {
        if (guiding != null) {
            guiding.cancel();
        }
        if (!on) {
            targetX = -1;
            targetY = -1;
        }
        guiding = ValueAnimator.ofFloat(guide, on ? 1f : 0f);
        guiding.setDuration(on ? Pace.ARRIVE : Pace.ARRIVE / 2);
        guiding.setInterpolator(Pace.EMPHASIS);
        guiding.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                guide = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        guiding.start();
    }

    void target(int[] cell) {
        int x = cell == null ? -1 : cell[0];
        int y = cell == null ? -1 : cell[1];
        if (x != targetX || y != targetY) {
            targetX = x;
            targetY = y;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int w = Math.round(cellWidth());
        int h = Math.round(cellHeight());
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float w = cellWidth();
        float h = cellHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int[] at = (int[]) child.getTag();
            int left = Math.round(getPaddingLeft() + at[0] * w);
            int top = Math.round(getPaddingTop() + at[1] * h);
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (guide <= 0f || iconSize <= 0f) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        point.setColor(Tone.onSurface());
        point.setAlpha(Math.round(90 * guide));
        ring.setColor(Tone.primary());
        ring.setStrokeWidth(2f * density);
        ring.setAlpha(Math.round(255 * guide));
        wash.setColor(Tone.primary());
        wash.setAlpha(Math.round(60 * guide));
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (!free(column, row)) {
                    continue;
                }
                float[] c = centre(column, row);
                if (column == targetX && row == targetY) {
                    float r = iconSize / 2f;
                    canvas.drawCircle(c[0], c[1], r, wash);
                    canvas.drawCircle(c[0], c[1], r, ring);
                } else {
                    canvas.drawCircle(c[0], c[1], 2.5f * density * guide, point);
                }
            }
        }
    }
}
