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
    /** Whether the app has a notification standing: a small point of the accent at the icon's shoulder. */
    private boolean dot;
    private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        words.setTextSize(14f * scaled * Style.nameScale);
        words.setTypeface(Style.face());
        words.setTextAlign(Paint.Align.CENTER);
        words.setColor(Style.nameInk());
        /* The names stand on the wallpaper, whatever it is; a soft dark
           halo keeps them legible on a white sky without a plate behind. */
        words.setShadowLayer(3f * density, 0f, 0.75f * density, Style.nameHalo());
        small.setTextSize(12.5f * scaled);
        small.setTextAlign(Paint.Align.CENTER);
        small.setColor(Tone.primary());
        setClickable(true);
        setContentDescription(this.label);
    }

    void dot(boolean on) {
        if (on != dot) {
            dot = on;
            invalidate();
        }
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
        probe.setTextSize(14f * scaled * Style.nameScale);
        probe.setTypeface(Style.face());
        return 6f * density - probe.ascent() + probe.descent();
    }

    /**
     * Whether the name has room under the icon: a place lower than the icon
     * and its name leaves the name out, rather than lay it over the icon
     * of the row below.
     */
    private boolean nameFits() {
        return named && (getHeight() == 0
            || iconSize + gap - words.ascent() + words.descent() <= getHeight() + 0.5f);
    }

    private float top() {
        float tall = iconSize;
        if (nameFits()) {
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

    /** Names on a surface of the theme, not on the wallpaper: its own words, no halo. */
    Cell onGround() {
        return onGround(Tone.onSurface());
    }

    /** The same, in words of a given colour: for a ground of the owner's own. */
    Cell onGround(int ink) {
        words.setColor(ink);
        words.clearShadowLayer();
        invalidate();
        return this;
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
            /* A tile wider than its square keeps inside its cell: where the
               cell is narrow, as in the dock, the whole icon is drawn smaller
               rather than over its neighbours. */
            float size = iconSize;
            if (icon instanceof Shape.Cut) {
                float need = iconSize * ((Shape.Cut) icon).wideness();
                float room = getWidth() * 0.96f;
                if (need > room && need > 0f) {
                    size = iconSize * room / need;
                }
            }
            float ix = (getWidth() - size) / 2f;
            float iy = y + (iconSize - size) / 2f;
            icon.setBounds(Math.round(ix), Math.round(iy), Math.round(ix + size), Math.round(iy + size));
            icon.draw(canvas);
        }
        if (dot) {
            float r = iconSize * 0.12f;
            float px = x + iconSize * 0.86f;
            float py = y + iconSize * 0.14f;
            point.setStyle(Paint.Style.FILL);
            point.setColor(0xFF15120E);
            canvas.drawCircle(px, py, r * 1.28f, point);
            point.setColor(Tone.primary());
            canvas.drawCircle(px, py, r, point);
        }
        if (nameFits()) {
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
