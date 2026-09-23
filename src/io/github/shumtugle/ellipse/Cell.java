package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/**
 * One application on the screen: its own icon, exactly as the system
 * draws it, and its name under it on the grid. In the dock the name is
 * left out; five places at the foot are learnt by shape, not by reading.
 */
final class Cell extends View {

    final Apps.Door door;

    private final Drawable icon;
    private final TextPaint words = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final boolean named;
    private final float iconSize;
    private final float gap;
    private CharSequence shown = "";

    Cell(Context context, Apps.Door door, float iconSize, boolean named) {
        super(context);
        this.door = door;
        Drawable drawn = door.icon();
        this.icon = drawn == null ? null : drawn.getConstantState() == null
            ? drawn : drawn.getConstantState().newDrawable().mutate();
        this.iconSize = iconSize;
        this.named = named;
        float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        gap = 6f * density;
        words.setTextSize(12.5f * scaled);
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(Tone.onSurface());
        /* The names stand on the wallpaper, whatever it is; a soft dark
           halo keeps them legible on a white sky without a plate behind. */
        words.setShadowLayer(3f * density, 0f, 0.75f * density, 0x99000000);
        setClickable(true);
        setContentDescription(door.label);
    }

    private float top() {
        float tall = iconSize;
        if (named) {
            tall += gap - words.ascent() + words.descent();
        }
        return (getHeight() - tall) / 2f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setPivotX(w / 2f);
        setPivotY(top() + iconSize / 2f);
        if (named) {
            shown = TextUtils.ellipsize(door.label == null ? "" : door.label, words,
                w - gap * 1.5f, TextUtils.TruncateAt.END);
        }
    }

    /** Sinks under the finger, and springs back past its place when it lifts. */
    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) {
            return;
        }
        float to = pressed ? Pace.SINK : 1f;
        animate().scaleX(to).scaleY(to)
            .setDuration(pressed ? Pace.PRESS : Pace.ARRIVE)
            .setInterpolator(pressed ? Pace.EMPHASIS : Pace.SPRING).start();
    }

    /** Where the icon itself stands, in the screen's coordinates, for the opening to grow from. */
    int[] iconBounds() {
        int[] at = new int[2];
        getLocationOnScreen(at);
        int left = Math.round(at[0] + (getWidth() - iconSize) / 2f);
        int top = Math.round(at[1] + top());
        return new int[] {left, top, Math.round(iconSize), Math.round(iconSize)};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float y = top();
        float x = (getWidth() - iconSize) / 2f;
        if (icon != null) {
            icon.setBounds(Math.round(x), Math.round(y),
                Math.round(x + iconSize), Math.round(y + iconSize));
            icon.draw(canvas);
        }
        if (named) {
            canvas.drawText(shown, 0, shown.length(), getWidth() / 2f,
                y + iconSize + gap - words.ascent(), words);
        }
    }
}
