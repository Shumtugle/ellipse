package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Pictures to choose from, in an even grid: as many across as the width
 * holds, spread over the whole width, then the next row. What a strip
 * sideways hid past the screen's edge stands here in sight, all of it.
 */
final class Tiles extends ViewGroup {

    private final int gap;
    private int cell;
    private int across;
    private int spread;

    Tiles(Context context, int gap) {
        super(context);
        this.gap = gap;
    }

    private static int spec(int size) {
        return size > 0 ? MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
            : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int room = width - getPaddingLeft() - getPaddingRight();
        cell = 1;
        for (int i = 0; i < getChildCount(); i++) {
            View one = getChildAt(i);
            ViewGroup.LayoutParams at = one.getLayoutParams();
            one.measure(spec(at == null ? 0 : at.width), spec(at == null ? 0 : at.height));
            cell = Math.max(cell, one.getMeasuredWidth());
        }
        across = Math.max(1, (room + gap) / (cell + gap));
        /* What is left over is shared between the gaps, so the rows fill the width. */
        spread = across > 1 ? Math.max(gap, (room - across * cell) / (across - 1)) : 0;
        int tall = 0;
        int line = 0;
        for (int i = 0; i < getChildCount(); i++) {
            line = Math.max(line, getChildAt(i).getMeasuredHeight());
            if ((i + 1) % across == 0 || i == getChildCount() - 1) {
                tall += line + (i == getChildCount() - 1 ? 0 : gap);
                line = 0;
            }
        }
        setMeasuredDimension(width, tall + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int y = getPaddingTop();
        int line = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View one = getChildAt(i);
            int column = i % across;
            if (column == 0 && i > 0) {
                y += line + gap;
                line = 0;
            }
            int x = getPaddingLeft() + column * (cell + spread) + (cell - one.getMeasuredWidth()) / 2;
            one.layout(x, y, x + one.getMeasuredWidth(), y + one.getMeasuredHeight());
            line = Math.max(line, one.getMeasuredHeight());
        }
    }
}
