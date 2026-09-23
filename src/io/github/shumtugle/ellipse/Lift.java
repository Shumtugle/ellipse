package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

/** An icon held up under the finger while it is carried to its place. */
final class Lift extends View {

    private final Drawable icon;
    private final int size;

    Lift(Context context, Drawable icon, int size) {
        super(context);
        this.icon = icon == null ? null : (icon.getConstantState() == null
            ? icon : icon.getConstantState().newDrawable().mutate());
        this.size = size;
        setPivotX(size / 2f);
        setPivotY(size / 2f);
    }

    int size() {
        return size;
    }

    /** Stands the icon's centre at a point of the floor. */
    void at(float x, float y) {
        setTranslationX(x - size / 2f);
        setTranslationY(y - size / 2f);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (icon != null) {
            icon.setBounds(0, 0, size, size);
            icon.draw(canvas);
        }
    }
}
