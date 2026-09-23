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

    private Drawable icon;
    private final TextPaint words = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final boolean named;
    private final float iconSize;
    private final float gap;
    private CharSequence shown = "";
    /** A quiet second line under the name, when there is one. */
    private String note;
    private final TextPaint small = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    /** The label shown under the icon: the door's, or one given. */
    private final CharSequence label;

    Cell(Context context, Apps.Door door, float iconSize, boolean named) {
        this(context, door, null, door.label, iconSize, named);
    }

    /** A place that is not one application: a folder, or a door of our own. */
    Cell(Context context, Drawable icon, CharSequence label, float iconSize) {
        this(context, null, icon, label, iconSize, true);
    }

    /** The same, named under its icon or not: in the dock names are left out. */
    Cell(Context context, Drawable icon, CharSequence label, float iconSize, boolean named) {
        this(context, null, icon, label, iconSize, named);
    }

    private Cell(Context context, Apps.Door door, Drawable icon, CharSequence label,
                 float iconSize, boolean named) {
        super(context);
        this.door = door;
        this.icon = icon;
        this.label = label == null ? "" : label;
        this.iconSize = iconSize;
        this.named = named;
        float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        gap = 6f * density;
        words.setTextSize(14f * scaled);
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(Tone.onSurface());
        /* The names stand on the wallpaper, whatever it is; a soft dark
           halo keeps them legible on a white sky without a plate behind. */
        words.setShadowLayer(3f * density, 0f, 0.75f * density, 0x99000000);
        small.setTextSize(12.5f * scaled);
        small.setTextAlign(Paint.Align.CENTER);
        small.setColor(Tone.primary());
        setClickable(true);
        setContentDescription(this.label);
    }

    /** Sets a quiet line under the name, in the accent: a word about the application. */
    Cell note(String note) {
        this.note = note;
        invalidate();
        return this;
    }

    /** How tall the name under an icon stands, with the air above it. */
    static float below(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        TextPaint probe = new TextPaint();
        probe.setTextSize(14f * scaled);
        return 6f * density - probe.ascent() + probe.descent();
    }

    private float top() {
        float tall = iconSize;
        if (named) {
            tall += gap - words.ascent() + words.descent();
        }
        if (note != null) {
            tall += -small.ascent() + small.descent();
        }
        return (getHeight() - tall) / 2f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setPivotX(w / 2f);
        setPivotY(top() + iconSize / 2f);
        if (named) {
            shown = TextUtils.ellipsize(label, words,
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

    /** What the cell shows as its icon, for a hand to hold up. */
    Drawable drawable() {
        if (icon == null && door != null) {
            icon = door.icon();
        }
        return icon;
    }

    /** Where the icon stands, in the cell's own coordinates. */
    int[] localIcon() {
        return new int[] {Math.round((getWidth() - iconSize) / 2f), Math.round(top()),
            Math.round(iconSize), Math.round(iconSize)};
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
        /* The icon is asked for when first seen: a page nobody turns to
           never paints its icons at all. */
        if (icon == null && door != null) {
            icon = door.icon();
        }
        if (icon != null) {
            icon.setBounds(Math.round(x), Math.round(y),
                Math.round(x + iconSize), Math.round(y + iconSize));
            icon.draw(canvas);
        }
        if (named) {
            float base = y + iconSize + gap - words.ascent();
            canvas.drawText(shown, 0, shown.length(), getWidth() / 2f, base, words);
            if (note != null) {
                CharSequence cut = TextUtils.ellipsize(note, small, getWidth() - gap,
                    TextUtils.TruncateAt.END);
                canvas.drawText(cut, 0, cut.length(), getWidth() / 2f,
                    base + words.descent() - small.ascent(), small);
            }
        }
    }
}
