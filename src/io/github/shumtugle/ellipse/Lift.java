package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

/** A thing held up under the finger while it is carried to its place: an icon, or a picture of it. */
final class Lift extends View {

    private final Drawable icon;
    private final int wide;
    private final int tall;

    Lift(Context context, Drawable icon, int size) {
        this(context, icon, size, size);
    }

    Lift(Context context, Drawable icon, int wide, int tall) {
        super(context);
        this.icon = icon == null ? null : (icon.getConstantState() == null
            ? icon : icon.getConstantState().newDrawable().mutate());
        this.wide = wide;
        this.tall = tall;
        setPivotX(wide / 2f);
        setPivotY(tall / 2f);
    }

    int size() {
        return wide;
    }

    int wide() {
        return wide;
    }

    int tall() {
        return tall;
    }

    /** Stands the thing's centre at a point of the floor. */
    void at(float x, float y) {
        setTranslationX(x - wide / 2f);
        setTranslationY(y - tall / 2f);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(wide, tall);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (icon != null) {
            icon.setBounds(0, 0, wide, tall);
            icon.draw(canvas);
        }
    }
}
