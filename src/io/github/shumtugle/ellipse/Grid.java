package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * A plain grid of equal places. Each child is told its column and row and
 * gets exactly one place; nothing spans, nothing floats.
 */
final class Grid extends ViewGroup {

    final int columns;
    final int rows;

    Grid(Context context, int columns, int rows) {
        super(context);
        this.columns = columns;
        this.rows = rows;
        setClipChildren(false);
        setClipToPadding(false);
    }

    void put(View child, int column, int row) {
        child.setTag(new int[] {column, row});
        addView(child);
    }

    float cellWidth() {
        return (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) / (float) columns;
    }

    float cellHeight() {
        return (getMeasuredHeight() - getPaddingTop() - getPaddingBottom()) / (float) rows;
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
}
