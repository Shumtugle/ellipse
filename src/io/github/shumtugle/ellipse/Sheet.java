package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

/**
 * One home screen: a grid, and things standing on it.
 *
 * The grid is only seen while something is being carried. Then every
 * cell shows as a faint dot at its centre, so the hand knows where a
 * thing may go, and the cells where it would land light up in the seed's
 * colour.
 */
final class Sheet extends ViewGroup {

    /** Where a thing stands: a cell, and how many cells it covers. */
    static final class Spot extends ViewGroup.LayoutParams {
        final int x;
        final int y;
        final int w;
        final int h;
        /** Whether it may run on to the edge of the glass where it reaches the grid's side. */
        boolean bleed;

        Spot(int x, int y, int w, int h) {
            super(WRAP_CONTENT, WRAP_CONTENT);
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final int columns;
    private final int rows;
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private int side;
    private int top;
    private int bottom;
    private boolean grid;
    private boolean edge;
    private final int[] box4 = new int[4];
    private int hx = -1;
    private int hy;
    private int hw;
    private int hh;

    Sheet(Context context, int columns, int rows) {
        super(context);
        this.columns = columns;
        this.rows = rows;
        side = Round.dp(4f);
        setWillNotDraw(false);
        setClipChildren(false);
    }

    void pad(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
        requestLayout();
        invalidate();
    }

    /** Whether things that may bleed run on to the edge of the glass. */
    void edge(boolean on) {
        edge = on;
        requestLayout();
    }

    /**
     * The box a thing stands in: its cells, and for a thing that may bleed,
     * the margin or the bar beyond any side where it reaches the grid's end.
     */
    private int[] box(Spot spot, int width, int height) {
        float cw = cellWidth(width);
        float ch = cellHeight(height);
        int left = Math.round(side + spot.x * cw);
        int right = Math.round(side + (spot.x + spot.w) * cw);
        int up = Math.round(top + spot.y * ch);
        int down = Math.round(top + (spot.y + spot.h) * ch);
        if (edge && spot.bleed) {
            if (spot.x == 0) {
                left = 0;
            }
            if (spot.x + spot.w >= columns) {
                right = width;
            }
            if (spot.y == 0) {
                up = 0;
            }
            if (spot.y + spot.h >= rows) {
                down = height;
            }
        }
        box4[0] = left;
        box4[1] = up;
        box4[2] = right;
        box4[3] = down;
        return box4;
    }

    float cellWidth() {
        return (getWidth() - 2f * side) / columns;
    }

    float cellHeight() {
        return (getHeight() - top - bottom) / (float) rows;
    }

    private float cellWidth(int width) {
        return (width - 2f * side) / columns;
    }

    private float cellHeight(int height) {
        return (height - top - bottom) / (float) rows;
    }

    /** The cell under a point of this sheet, held inside the grid. */
    int[] cellAt(float x, float y) {
        int cx = (int) Math.floor((x - side) / Math.max(1f, cellWidth()));
        int cy = (int) Math.floor((y - top) / Math.max(1f, cellHeight()));
        return new int[] {
            Math.max(0, Math.min(columns - 1, cx)),
            Math.max(0, Math.min(rows - 1, cy)),
        };
    }

    /** Shows the grid, and the block where a carried thing would land, or none. */
    void carry(boolean on, int x, int y, int w, int h) {
        grid = on;
        hx = x;
        hy = y;
        hw = w;
        hh = h;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int[] b = box((Spot) child.getLayoutParams(), width, height);
            child.measure(MeasureSpec.makeMeasureSpec(b[2] - b[0], MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(b[3] - b[1], MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int[] box = box((Spot) child.getLayoutParams(), r - l, b - t);
            child.layout(box[0], box[1], box[0] + child.getMeasuredWidth(),
                box[1] + child.getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!grid) {
            return;
        }
        float cw = cellWidth();
        float ch = cellHeight();
        dot.setColor(Tone.of(Tone.ON_SURFACE, 0.28f));
        float r = Round.px(2f);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                canvas.drawCircle(side + (x + 0.5f) * cw, top + (y + 0.5f) * ch, r, dot);
            }
        }
        if (hx >= 0) {
            float inset = Round.px(3f);
            box.set(side + hx * cw + inset, top + hy * ch + inset,
                side + (hx + hw) * cw - inset, top + (hy + hh) * ch - inset);
            glow.setStyle(Paint.Style.FILL);
            glow.setColor(Tone.of(Tone.PRIMARY, 0.18f));
            canvas.drawRoundRect(box, Round.px(Round.L), Round.px(Round.L), glow);
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(Round.px(1.5f));
            glow.setColor(Tone.of(Tone.PRIMARY, 0.9f));
            canvas.drawRoundRect(box, Round.px(Round.L), Round.px(Round.L), glow);
        }
    }
}
