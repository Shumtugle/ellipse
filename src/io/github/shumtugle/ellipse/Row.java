package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/**
 * One line of the list of every application: the icon at the left, the
 * name beside it, and the whole line under the finger. The same view is
 * handed from door to door as the list scrolls.
 */
final class Row extends View {

    private final TextPaint words = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final float iconSize;
    private final float inset;
    private final float gap;
    private final int tall;
    private Apps.Door door;
    private CharSequence shown = "";

    Row(Context context, float iconSize) {
        super(context);
        this.iconSize = iconSize;
        float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        inset = 24f * density;
        gap = 18f * density;
        tall = Math.round(iconSize + 28f * density);
        words.setTextSize(20f * scaled * Style.nameScale);
        words.setTypeface(Style.face());
        tint();
    }

    void tint() {
        words.setColor(Tone.onSurface());
        setBackground(Tone.touch(null, 0f));
        invalidate();
    }

    void show(Apps.Door door) {
        this.door = door;
        setContentDescription(door.label);
        cut();
        invalidate();
    }

    Apps.Door door() {
        return door;
    }

    /** Where the icon stands, in the row's own coordinates, for an opening to grow from. */
    int[] iconBounds() {
        return new int[] {Math.round(inset), Math.round((getHeight() - iconSize) / 2f),
            Math.round(iconSize), Math.round(iconSize)};
    }

    private void cut() {
        if (door == null || getWidth() == 0) {
            return;
        }
        float room = getWidth() - inset * 2f - iconSize - gap;
        shown = TextUtils.ellipsize(door.label, words, Math.max(0f, room),
            TextUtils.TruncateAt.END);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), tall);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cut();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (door == null) {
            return;
        }
        float top = (getHeight() - iconSize) / 2f;
        Drawable icon = door.icon();
        if (icon != null) {
            icon.setBounds(Math.round(inset), Math.round(top),
                Math.round(inset + iconSize), Math.round(top + iconSize));
            icon.draw(canvas);
        }
        float base = getHeight() / 2f - (words.ascent() + words.descent()) / 2f;
        canvas.drawText(shown, 0, shown.length(), inset + iconSize + gap, base, words);
    }
}
