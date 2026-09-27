package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

/**
 * A plain grid of equal places. Each child is told its column and row and
 * how many places it takes across and down, one by one unless it says
 * otherwise; nothing floats.
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
    private int targetAcross = 1;
    private int targetDown = 1;

    Grid(Context context, int columns, int rows) {
        super(context);
        this.columns = columns;
        this.rows = rows;
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(false);
        ring.setStyle(Paint.Style.STROKE);
    }

    /** Whether things may stand over one another. */
    private boolean overlap;

    void overlap(boolean on) {
        overlap = on;
    }

    /**
     * Whether a thing kept here may stand here now: inside the grid and, if
     * things may not stand over one another, on free places.
     */
    boolean fits(int column, int row, int across, int down) {
        if (!overlap) {
            return free(column, row, across, down);
        }
        return column >= 0 && row >= 0 && column + across <= columns && row + down <= rows;
    }

    /** Whether a thing already starts at this very place: two may not share a corner. */
    private boolean taken(int column, int row) {
        for (int i = 0; i < getChildCount(); i++) {
            int[] at = (int[]) getChildAt(i).getTag();
            if (at[0] == column && at[1] == row) {
                return true;
            }
        }
        return false;
    }

    /** How many small places make one place each way: two with half steps. */
    private int unit = 1;

    void unit(int small) {
        unit = Math.max(1, small);
    }

    int unit() {
        return unit;
    }

    /** One thing of one place: with half steps, a block of small places. */
    void put(View child, int column, int row) {
        put(child, column, row, unit, unit);
    }

    /**
     * With everything to the edges, the icons are set out as a justified line
     * of type is: the first column's against the screen's left edge, the last
     * column's against its right, and the space between them shared evenly.
     */
    private boolean justified;

    void justify(boolean on) {
        justified = on;
    }

    void put(View child, int column, int row, int across, int down) {
        if (child instanceof Cell) {
            float lean = 0.5f;
            if (justified && across <= unit() && columns > unit()) {
                lean = column / (float) (columns - unit());
            }
            ((Cell) child).lean(Math.max(0f, Math.min(1f, lean)));
        }
        child.setTag(new int[] {column, row, across, down});
        addView(child);
    }

    /** Sets a thing already standing here into another block, at once. */
    void reshape(View child, int column, int row, int across, int down) {
        child.setTag(new int[] {column, row, across, down});
        requestLayout();
    }

    /** Where a block of places stands, in the grid's own coordinates. */
    android.graphics.RectF block(int column, int row, int across, int down) {
        float w = cellWidth();
        float h = cellHeight();
        float left = getPaddingLeft() + column * w;
        float top = getPaddingTop() + row * h;
        return new android.graphics.RectF(left, top, left + across * w, top + down * h);
    }

    private static int across(int[] at) {
        return at.length > 2 ? at[2] : 1;
    }

    private static int down(int[] at) {
        return at.length > 3 ? at[3] : 1;
    }

    /** How icons stand in their places: their size, and how tall the name under them is. */
    void shape(float iconSize, float below) {
        this.iconSize = iconSize;
        this.below = below;
    }

    float cellWidth() {
        return (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) / (float) columns;
    }

    /** A row's share of its full height: under one, the rows close up toward the top. */
    private float rowShare = 1f;

    void rowShare(float share) {
        rowShare = Math.max(0.5f, Math.min(1f, share));
        requestLayout();
    }

    float cellHeight() {
        return (getMeasuredHeight() - getPaddingTop() - getPaddingBottom()) / (float) rows * rowShare;
    }

    /** The centre of the icon of a place starting here, in the grid's own coordinates. */
    float[] centre(int column, int row) {
        float w = cellWidth() * unit;
        float h = cellHeight() * unit;
        float x = getPaddingLeft() + column * cellWidth() + 0.5f * w;
        float y = getPaddingTop() + row * cellHeight() + (h - iconSize - below) / 2f + iconSize / 2f;
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

    /** Whether one thing of one place could stand here: with half steps, a free block. */
    boolean free(int column, int row) {
        return unit == 1 ? free(column, row, (View) null) : free(column, row, unit, unit, null);
    }

    /** Whether a place is free, not counting one thing that may stand on it. */
    boolean free(int column, int row, View besides) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) == besides) {
                continue;
            }
            int[] at = (int[]) getChildAt(i).getTag();
            if (column >= at[0] && column < at[0] + across(at)
                && row >= at[1] && row < at[1] + down(at)) {
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

    /** Whether a block of places is inside the grid and all free. */
    boolean free(int column, int row, int across, int down) {
        return free(column, row, across, down, null);
    }

    boolean free(int column, int row, int across, int down, View besides) {
        if (column < 0 || row < 0 || column + across > columns || row + down > rows) {
            return false;
        }
        for (int c = column; c < column + across; c++) {
            for (int r = row; r < row + down; r++) {
                if (!free(c, r, besides)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The centre of a block of places, in the grid's own coordinates. */
    float[] middle(int column, int row, int across, int down) {
        if (across == 1 && down == 1) {
            return centre(column, row);
        }
        float w = cellWidth();
        float h = cellHeight();
        return new float[] {getPaddingLeft() + (column + across / 2f) * w,
            getPaddingTop() + (row + down / 2f) * h};
    }

    /**
     * Where a block would land with its centre at a point: the block that
     * point would place, if free, or else the free block nearest to it.
     */
    int[] landing(float x, float y, int across, int down) {
        if (x < 0 || y < 0 || x > getWidth() || y > getHeight()) {
            return null;
        }
        if (overlap) {
            /* Over others too: the block round the finger, or, if something
               starts at that very place, the nearest place nothing starts at. */
            float w = cellWidth();
            float h = cellHeight();
            int column = Math.max(0, Math.min(columns - across, Math.round((x - getPaddingLeft()) / w - across / 2f)));
            int row = Math.max(0, Math.min(rows - down, Math.round((y - getPaddingTop()) / h - down / 2f)));
            if (!taken(column, row)) {
                return new int[] {column, row};
            }
            int[] best = null;
            float nearest = Float.MAX_VALUE;
            for (int r = 0; r + down <= rows; r++) {
                for (int c = 0; c + across <= columns; c++) {
                    if (taken(c, r)) {
                        continue;
                    }
                    float d = (c - column) * (c - column) + (r - row) * (r - row);
                    if (d < nearest) {
                        nearest = d;
                        best = new int[] {c, r};
                    }
                }
            }
            return best;
        }
        if (across == 1 && down == 1) {
            int[] under = cellAt(x, y);
            if (under == null) {
                return null;
            }
            return free(under[0], under[1]) ? under : nearestFree(x, y);
        }
        float w = cellWidth();
        float h = cellHeight();
        int column = Math.round((x - getPaddingLeft()) / w - across / 2f);
        int row = Math.round((y - getPaddingTop()) / h - down / 2f);
        column = Math.max(0, Math.min(columns - across, column));
        row = Math.max(0, Math.min(rows - down, row));
        if (free(column, row, across, down)) {
            return new int[] {column, row};
        }
        int[] best = null;
        float nearest = Float.MAX_VALUE;
        for (int r = 0; r + down <= rows; r++) {
            for (int c = 0; c + across <= columns; c++) {
                if (!free(c, r, across, down)) {
                    continue;
                }
                float[] m = middle(c, r, across, down);
                float d = (m[0] - x) * (m[0] - x) + (m[1] - y) * (m[1] - y);
                if (d < nearest) {
                    nearest = d;
                    best = new int[] {c, r};
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
        target(cell, 1, 1);
    }

    void target(int[] cell, int across, int down) {
        int x = cell == null ? -1 : cell[0];
        int y = cell == null ? -1 : cell[1];
        if (x != targetX || y != targetY || across != targetAcross || down != targetDown) {
            targetX = x;
            targetY = y;
            targetAcross = across;
            targetDown = down;
            invalidate();
        }
    }

    /**
     * Things that reach past the grid's margins to its own edges, where
     * they run the grid's whole width: the clock, a widget, when asked.
     */
    private final java.util.Set<View> edged = new java.util.HashSet<>();

    void edge(View thing, boolean on) {
        if (on) {
            edged.add(thing);
        } else {
            edged.remove(thing);
        }
        requestLayout();
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        edged.remove(child);
    }

    /** Whether a thing runs past the left margin: it may, and it stands in the first column. */
    private boolean reachesLeft(View child, int[] at) {
        return edged.contains(child) && at[0] == 0;
    }

    /** Whether a thing runs past the right margin: it may, and it ends in the last column. */
    private boolean reachesRight(View child, int[] at) {
        return edged.contains(child) && at[0] + across(at) >= columns;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        float w = cellWidth();
        float h = cellHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int[] at = (int[]) child.getTag();
            int wide = Math.round(w * across(at)) + (reachesLeft(child, at) ? getPaddingLeft() : 0)
                + (reachesRight(child, at) ? getPaddingRight() : 0);
            child.measure(MeasureSpec.makeMeasureSpec(wide, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.round(h * down(at)), MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float w = cellWidth();
        float h = cellHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int[] at = (int[]) child.getTag();
            int left = reachesLeft(child, at) ? 0 : Math.round(getPaddingLeft() + at[0] * w);
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
                /* With half steps, a point for every whole place only: the
                   ring shows where the half steps land. */
                if (column % unit != 0 || row % unit != 0) {
                    continue;
                }
                float[] c = centre(column, row);
                boolean inside = targetX >= 0 && column >= targetX && column < targetX + targetAcross
                    && row >= targetY && row < targetY + targetDown;
                if (column == targetX && row == targetY && targetAcross == unit && targetDown == unit) {
                    float r = iconSize / 2f;
                    canvas.drawCircle(c[0], c[1], r, wash);
                    canvas.drawCircle(c[0], c[1], r, ring);
                } else if (!inside) {
                    canvas.drawCircle(c[0], c[1], 2.5f * density * guide, point);
                }
            }
        }
        if (targetX >= 0 && targetAcross == unit && targetDown == unit && unit > 1
            && (targetX % unit != 0 || targetY % unit != 0)) {
            float[] c = centre(targetX, targetY);
            float r = iconSize / 2f;
            canvas.drawCircle(c[0], c[1], r, wash);
            canvas.drawCircle(c[0], c[1], r, ring);
        }
        if (targetX >= 0 && (targetAcross > unit || targetDown > unit)) {
            /* A wide thing lands on a block: the block is washed and ringed whole. */
            float w = cellWidth();
            float h = cellHeight();
            float inset = 4f * density;
            float left = getPaddingLeft() + targetX * w + inset;
            float top = getPaddingTop() + targetY * h + inset;
            float right = left + targetAcross * w - 2f * inset;
            float bottom = top + targetDown * h - 2f * inset;
            float r = Math.min(28f * density, (bottom - top) / 2f);
            canvas.drawRoundRect(left, top, right, bottom, r, r, wash);
            canvas.drawRoundRect(left, top, right, bottom, r, r, ring);
        }
    }
}
