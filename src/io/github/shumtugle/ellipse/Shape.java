package io.github.shumtugle.ellipse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/**
 * The shape every icon is cut to, one for all of them, so the grid reads
 * as one set whatever each app brought.
 *
 * An app's icon comes in layers: a ground that covers a larger square than
 * shows, and a picture in its middle. The layers are laid out here as the
 * platform lays them out, and cut by the chosen outline. The phone's own
 * outline is kept as the phone draws it. An icon that comes as one flat
 * picture is left as it came, for now.
 *
 * Outlines differ in how much of their square they fill, and a circle
 * beside a square of the same size looks smaller. Each outline is drawn a
 * little smaller the more it fills, so a grid of any outline weighs the
 * same.
 */
final class Shape {

    static final int SYSTEM = 0;
    static final int CIRCLE = 1;
    static final int SQUIRCLE = 2;
    static final int ROUNDED = 3;
    static final int COUNT = 4;

    /** The outline chosen now; the settings set it, every icon drawn after reads it. */
    static int current = SYSTEM;

    private Shape() {
    }

    /** The outline in a square of the given side, from its top left corner. */
    static Path outline(int shape, float side) {
        Path path = new Path();
        float r = side / 2f;
        switch (shape) {
            case CIRCLE:
                path.addCircle(r, r, r, Path.Direction.CW);
                break;
            case SQUIRCLE:
                /* A superellipse of the fifth power: the sides run straight
                   longer than a circle's and turn the corner more softly
                   than a rounded square's. */
                for (int i = 0; i <= 180; i++) {
                    double t = 2 * Math.PI * i / 180;
                    double c = Math.cos(t);
                    double s = Math.sin(t);
                    float x = (float) (r + r * Math.signum(c) * Math.pow(Math.abs(c), 2.0 / 5.0));
                    float y = (float) (r + r * Math.signum(s) * Math.pow(Math.abs(s), 2.0 / 5.0));
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                path.close();
                break;
            case ROUNDED:
                path.addRoundRect(new RectF(0, 0, side, side), side * 0.22f, side * 0.22f, Path.Direction.CW);
                break;
            default:
                path.addRoundRect(new RectF(0, 0, side, side), side * 0.5f, side * 0.5f, Path.Direction.CW);
                break;
        }
        return path;
    }

    /** How much smaller than its square an outline is drawn, so every outline weighs the same. */
    static float weight(int shape) {
        switch (shape) {
            case SQUIRCLE:
                return 0.93f;
            case ROUNDED:
                return 0.92f;
            default:
                return 1f;
        }
    }

    /** An icon as it is to be drawn: cut to the outline when it comes in layers. */
    static Drawable face(Drawable icon) {
        if (current == SYSTEM || !(icon instanceof AdaptiveIconDrawable)) {
            return icon;
        }
        return new Cut((AdaptiveIconDrawable) icon, current);
    }

    /**
     * An icon in layers, cut to an outline. It is drawn once into a picture
     * of its size and then only copied, so a list of many icons scrolls as
     * lightly as a list of pictures.
     */
    static final class Cut extends Drawable {
        private final AdaptiveIconDrawable layers;
        private final int shape;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap drawn;

        Cut(AdaptiveIconDrawable layers, int shape) {
            this.layers = layers;
            this.shape = shape;
        }

        @Override
        public void draw(Canvas canvas) {
            int side = Math.min(getBounds().width(), getBounds().height());
            if (side <= 0) {
                return;
            }
            if (drawn == null || drawn.getWidth() != side) {
                drawn = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
                Canvas into = new Canvas(drawn);
                float inner = side * weight(shape);
                float edge = (side - inner) / 2f;
                into.save();
                into.translate(edge, edge);
                into.clipPath(outline(shape, inner));
                /* The layers are larger than what shows: a quarter of the
                   visible side more on every edge, as the platform lays them. */
                int spill = Math.round(inner / 4f);
                int all = Math.round(inner) + 2 * spill;
                Drawable ground = layers.getBackground();
                if (ground != null) {
                    ground.setBounds(-spill, -spill, all - spill, all - spill);
                    ground.draw(into);
                }
                Drawable picture = layers.getForeground();
                if (picture != null) {
                    picture.setBounds(-spill, -spill, all - spill, all - spill);
                    picture.draw(into);
                }
                into.restore();
            }
            float left = getBounds().left + (getBounds().width() - side) / 2f;
            float top = getBounds().top + (getBounds().height() - side) / 2f;
            canvas.drawBitmap(drawn, left, top, paint);
        }

        @Override
        public int getIntrinsicWidth() {
            return layers.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return layers.getIntrinsicHeight();
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
}
