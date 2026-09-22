package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.View;

import java.util.List;

/**
 * The drawer as pages turned sideways.
 *
 * Each page holds as many rows as the screen has room for, four tiles to
 * a row. Every cell is made once, when the list arrives: a hundred and
 * fifty tiles are a small matter, and a page turned for the first time
 * should already be there.
 */
final class Pages extends Leaves {

    /** Makes a cell and dresses it for an application. */
    interface Cells {
        View make();

        void dress(View cell, App app, int position);
    }

    private static final int COLUMNS = 4;

    private final Cells cells;
    private int count;
    private int rows = 1;
    private int slot;
    private int cellWidth;
    private final int side;
    private int top;
    private int bottom;

    Pages(Context context, Cells cells) {
        super(context);
        this.cells = cells;
        side = Round.dp(8f);
    }

    void pad(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
        requestLayout();
    }

    void show(List<App> apps) {
        removeAllViews();
        count = apps.size();
        for (int i = 0; i < count; i++) {
            View cell = cells.make();
            cells.dress(cell, apps.get(i), i);
            addView(cell);
        }
        requestLayout();
    }

    private int perPage() {
        return rows * COLUMNS;
    }

    @Override
    int leaves() {
        return count == 0 ? 1 : (count + perPage() - 1) / perPage();
    }

    private int dotsRoom() {
        return Round.dp(28f);
    }

    @Override
    float dotsAt() {
        return getHeight() - bottom - dotsRoom() / 2f;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        cellWidth = (width - 2 * side) / COLUMNS;
        int cellSpec = MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY);
        int cellHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(cellSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            cellHeight = Math.max(cellHeight, child.getMeasuredHeight());
        }
        int room = height - top - bottom - dotsRoom();
        int least = cellHeight + Round.dp(12f);
        rows = cellHeight > 0 ? Math.max(1, room / least) : 1;
        slot = room / rows;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int each = perPage();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int p = i / each;
            int k = i % each;
            int row = k / COLUMNS;
            int column = k % COLUMNS;
            int x = p * width + side + column * cellWidth;
            int y = top + row * slot + (slot - child.getMeasuredHeight()) / 2;
            child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
        }
        settleAfterLayout();
    }
}
