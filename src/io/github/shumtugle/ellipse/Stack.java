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

    /** This folder's own layout, or less than nought for the one every folder has. */
    private final int own;

    Stack(List<Apps.Door> doors) {
        this(doors, -1);
    }

    Stack(List<Apps.Door> doors, int own) {
        this(doors, own, null);
    }

    /** Pictures shown after the apps: widgets whose apps have no front door show their own. */
    private final List<Drawable> more;

    Stack(List<Apps.Door> doors, int own, List<Drawable> more) {
        this.doors = doors;
        this.own = own;
        this.more = more;
    }

    private int count() {
        return doors.size() + (more == null ? 0 : more.size());
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
        switch (own >= 0 ? own : layout) {
            case NINE:
                for (int i = 0; i < Math.min(9, count()); i++) {
                    one(canvas, i, cx + (i % 3 - 1) * size * 0.25f * room, cy + (i / 3 - 1) * size * 0.25f * room,
                        size * 0.21f * room);
                }
                break;
            case RING:
                for (int i = 0; i < Math.min(5, count()); i++) {
                    double a = -Math.PI / 2 + 2 * Math.PI * i / 5;
                    one(canvas, i, cx + (float) Math.cos(a) * size * 0.27f * room,
                        cy + (float) Math.sin(a) * size * 0.27f * room, size * 0.23f * room);
                }
                break;
            case PILE:
                /* The first in front, the others behind it, up and to the left. */
                for (int i = Math.min(3, count()) - 1; i >= 0; i--) {
                    float at = (1 - i) * size * 0.12f * room;
                    one(canvas, i, cx + at, cy + at, size * (0.46f - 0.04f * i) * room);
                }
                break;
            case FAN:
                int[] fan = {1, 2, 0};
                float[] across = {-0.22f, 0.22f, 0f};
                for (int k = 0; k < 3; k++) {
                    int i = fan[k];
                    if (i < count()) {
                        one(canvas, i, cx + across[k] * size * room, cy, size * (i == 0 ? 0.46f : 0.36f) * room);
                    }
                }
                break;
            case TOWER:
                int[] tower = {1, 2, 0};
                float[] down = {-0.22f, 0.22f, 0f};
                for (int k = 0; k < 3; k++) {
                    int i = tower[k];
                    if (i < count()) {
                        one(canvas, i, cx, cy + down[k] * size * room, size * (i == 0 ? 0.46f : 0.36f) * room);
                    }
                }
                break;
            default:
                for (int i = 0; i < Math.min(4, count()); i++) {
                    one(canvas, i, cx + (i % 2 == 0 ? -1 : 1) * size * 0.17f * room,
                        cy + (i < 2 ? -1 : 1) * size * 0.17f * room, size * 0.3f * room);
                }
                break;
        }
    }

    /** One small icon about a centre. */
    private void one(Canvas canvas, int i, float x, float y, float small) {
        Drawable icon = i < doors.size() ? doors.get(i).icon() : more.get(i - doors.size());
        if (icon == null) {
            return;
        }
        Rect was = icon.copyBounds();
        icon.setBounds(Math.round(x - small / 2f), Math.round(y - small / 2f),
            Math.round(x + small / 2f), Math.round(y + small / 2f));
        icon.draw(canvas);
        icon.setBounds(was);
    }

    /**
     * A layout drawn in dots on a round ground, the first dot in the accent:
     * how the small icons of a folder lie, whatever the folder holds.
     */
    static void sketch(Canvas c, float w, float h, int layout) {
        Paint ground = new Paint(Paint.ANTI_ALIAS_FLAG);
        ground.setColor(Tone.containerHigh());
        c.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, ground);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        float s = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        switch (layout) {
            case NINE:
                for (int i = 0; i < 9; i++) {
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + (i % 3 - 1) * s * 0.25f, cy + (i / 3 - 1) * s * 0.25f, s * 0.09f, dot);
                }
                break;
            case RING:
                for (int i = 0; i < 5; i++) {
                    double a = -Math.PI / 2 + 2 * Math.PI * i / 5;
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + (float) Math.cos(a) * s * 0.27f, cy + (float) Math.sin(a) * s * 0.27f,
                        s * 0.1f, dot);
                }
                break;
            case PILE:
                for (int i = 2; i >= 0; i--) {
                    float at = (1 - i) * s * 0.12f;
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + at, cy + at, s * (0.23f - 0.02f * i), dot);
                }
                break;
            case FAN:
            case TOWER:
                boolean fan = layout == FAN;
                float[] off = {-0.22f, 0.22f, 0f};
                for (int k = 0; k < 3; k++) {
                    dot.setColor(k == 2 ? Tone.primary() : Tone.faint());
                    float r = s * (k == 2 ? 0.23f : 0.18f);
                    c.drawCircle(cx + (fan ? off[k] * s : 0f), cy + (fan ? 0f : off[k] * s), r, dot);
                }
                break;
            default:
                for (int i = 0; i < 4; i++) {
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + (i % 2 == 0 ? -1 : 1) * s * 0.17f, cy + (i < 2 ? -1 : 1) * s * 0.17f,
                        s * 0.14f, dot);
                }
                break;
        }
    }

    /** The sketch as a picture, for a list to choose a layout from. */
    static Drawable sketch(final int layout) {
        return new Drawable() {
            @Override
            public void draw(Canvas canvas) {
                Rect b = getBounds();
                canvas.save();
                canvas.translate(b.left, b.top);
                sketch(canvas, b.width(), b.height(), layout);
                canvas.restore();
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(ColorFilter filter) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
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
