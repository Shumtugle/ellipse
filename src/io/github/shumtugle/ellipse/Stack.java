package io.github.shumtugle.ellipse;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.util.List;

/**
 * The face of a folder: a container in the outline every icon wears, of
 * the surface's tone — or none, when the owner wants the icons alone — and
 * in it small icons of what the folder holds, laid out as the owner chose:
 * four, two by two; nine, three by three; five in a ring; three stacked
 * one behind another; three fanned side by side; three one above another.
 */
final class Stack extends Drawable {

    static final int FOUR = 0;
    static final int NINE = 1;
    static final int RING = 2;
    static final int PILE = 3;
    static final int FAN = 4;
    static final int TOWER = 5;
    static final String[] NAMES = {"Four", "Nine", "Ring", "Stack", "Fan", "Tower"};

    /** The layout and whether the container is drawn, for every folder; the settings set them. */
    static int layout = FOUR;
    static boolean ground = true;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        if (ground) {
            paint.setColor(Tone.containerHigh());
            /* The folder's face takes the outline every icon is cut to. */
            if (Shape.current == Shape.SYSTEM) {
                canvas.drawCircle(cx, cy, size / 2f, paint);
            } else {
                float side = Shape.current == Shape.PAPER ? size * 0.86f * Shape.PAPER_WIDE
                    : size * Shape.weight(Shape.current);
                float root = Shape.current == Shape.PAPER ? 1f : (float) Math.sqrt(Shape.aspect);
                float w = side * root;
                float h = side / root;
                canvas.save();
                canvas.translate(cx - w / 2f, cy - h / 2f);
                canvas.drawPath(Shape.outline(Shape.current, w, h), paint);
                canvas.restore();
            }
        }
        /* Without a container the icons may take more of the place. */
        float room = ground ? 1f : 1.18f;
        switch (layout) {
            case NINE:
                for (int i = 0; i < Math.min(9, doors.size()); i++) {
                    one(canvas, i, cx + (i % 3 - 1) * size * 0.25f * room, cy + (i / 3 - 1) * size * 0.25f * room,
                        size * 0.21f * room);
                }
                break;
            case RING:
                for (int i = 0; i < Math.min(5, doors.size()); i++) {
                    double a = -Math.PI / 2 + 2 * Math.PI * i / 5;
                    one(canvas, i, cx + (float) Math.cos(a) * size * 0.27f * room,
                        cy + (float) Math.sin(a) * size * 0.27f * room, size * 0.23f * room);
                }
                break;
            case PILE:
                /* The first in front, the others behind it, up and to the left. */
                for (int i = Math.min(3, doors.size()) - 1; i >= 0; i--) {
                    float at = (1 - i) * size * 0.12f * room;
                    one(canvas, i, cx + at, cy + at, size * (0.46f - 0.04f * i) * room);
                }
                break;
            case FAN:
                int[] fan = {1, 2, 0};
                float[] across = {-0.22f, 0.22f, 0f};
                for (int k = 0; k < 3; k++) {
                    int i = fan[k];
                    if (i < doors.size()) {
                        one(canvas, i, cx + across[k] * size * room, cy, size * (i == 0 ? 0.46f : 0.36f) * room);
                    }
                }
                break;
            case TOWER:
                int[] tower = {1, 2, 0};
                float[] down = {-0.22f, 0.22f, 0f};
                for (int k = 0; k < 3; k++) {
                    int i = tower[k];
                    if (i < doors.size()) {
                        one(canvas, i, cx, cy + down[k] * size * room, size * (i == 0 ? 0.46f : 0.36f) * room);
                    }
                }
                break;
            default:
                for (int i = 0; i < Math.min(4, doors.size()); i++) {
                    one(canvas, i, cx + (i % 2 == 0 ? -1 : 1) * size * 0.17f * room,
                        cy + (i < 2 ? -1 : 1) * size * 0.17f * room, size * 0.3f * room);
                }
                break;
        }
    }

    /** One small icon about a centre. */
    private void one(Canvas canvas, int i, float x, float y, float small) {
        Drawable icon = doors.get(i).icon();
        if (icon == null) {
            return;
        }
        Rect was = icon.copyBounds();
        icon.setBounds(Math.round(x - small / 2f), Math.round(y - small / 2f),
            Math.round(x + small / 2f), Math.round(y + small / 2f));
        icon.draw(canvas);
        icon.setBounds(was);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
