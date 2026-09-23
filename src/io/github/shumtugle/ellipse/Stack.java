package io.github.shumtugle.ellipse;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.util.List;

/**
 * The face of a folder: a round container of the surface's tone and in it
 * the first four icons of what the folder holds, small, two by two.
 */
final class Stack extends Drawable {

    private final Paint ground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Apps.Door> doors;

    Stack(List<Apps.Door> doors) {
        this.doors = doors;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float size = Math.min(b.width(), b.height());
        float cx = b.exactCenterX();
        float cy = b.exactCenterY();
        ground.setColor(Tone.containerHigh());
        canvas.drawCircle(cx, cy, size / 2f, ground);
        float small = size * 0.3f;
        float step = size * 0.17f;
        int count = Math.min(4, doors.size());
        for (int i = 0; i < count; i++) {
            Drawable icon = doors.get(i).icon();
            if (icon == null) {
                continue;
            }
            float x = cx + (i % 2 == 0 ? -step : step);
            float y = cy + (i < 2 ? -step : step);
            Rect was = icon.copyBounds();
            icon.setBounds(Math.round(x - small / 2f), Math.round(y - small / 2f),
                Math.round(x + small / 2f), Math.round(y + small / 2f));
            icon.draw(canvas);
            icon.setBounds(was);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        ground.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        ground.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
