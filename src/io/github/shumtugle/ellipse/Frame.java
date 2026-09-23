package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.widget.LinearLayout;

/**
 * The ground of the screen with a window cut in it. Everything around the
 * window is the dark surface the dock and the bar stand on; inside it the
 * wallpaper shows through, with rounded corners, the way a page shows in
 * its frame in the browser this screen borrows its look from.
 */
final class Frame extends LinearLayout {

    private final Paint ground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final RectF hole = new RectF();
    private final float radius;
    private View window;

    Frame(Context context, float radius) {
        super(context);
        this.radius = radius;
        setOrientation(VERTICAL);
        setWillNotDraw(false);
        tint();
    }

    /** The child the window is cut around. */
    void cut(View window) {
        this.window = window;
    }

    void tint() {
        ground.setColor(Tone.surface());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        shape.reset();
        shape.setFillType(Path.FillType.EVEN_ODD);
        shape.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        if (window != null && window.getWidth() > 0) {
            hole.set(window.getLeft(), window.getTop(), window.getRight(), window.getBottom());
            shape.addRoundRect(hole, radius, radius, Path.Direction.CW);
        }
        canvas.drawPath(shape, ground);
    }
}
