package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Words laid in lines that wrap, as text does: each word as wide as it is,
 * the next line begun where the width runs out. Nothing goes off the edge
 * of the screen, and nothing scrolls sideways.
 */
final class Flow extends ViewGroup {

    private final int gap;

    Flow(Context context, int gap) {
        super(context);
        this.gap = gap;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int room = width - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int y = 0;
        int line = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View one = getChildAt(i);
            one.measure(MeasureSpec.makeMeasureSpec(room, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED);
            if (x > 0 && x + one.getMeasuredWidth() > room) {
                x = 0;
                y += line + gap;
                line = 0;
            }
            x += one.getMeasuredWidth() + gap;
            line = Math.max(line, one.getMeasuredHeight());
        }
        setMeasuredDimension(width, y + line + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int room = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int y = 0;
        int line = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View one = getChildAt(i);
            int w = one.getMeasuredWidth();
            int h = one.getMeasuredHeight();
            if (x > 0 && x + w > room) {
                x = 0;
                y += line + gap;
                line = 0;
            }
            one.layout(getPaddingLeft() + x, getPaddingTop() + y, getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + gap;
            line = Math.max(line, h);
        }
    }
}
