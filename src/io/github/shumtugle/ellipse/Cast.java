package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

/**
 * The settings, made of what the tiles are made of.
 *
 * Everywhere else a setting is a grey row with a switch at its end. Here
 * the things a finger moves are things. The chosen card is set in a rim of
 * the tiles' own material; the button that does the likely thing is a
 * plate of it with its word cut in; a number stands behind glass; a
 * subject is known by a small tile of its own with its sign in the window.
 * Change the material and every one of them is cast again.
 *
 * What is not a thing keeps to the ground: slabs of the ground's own
 * tones, each with a lip of light along its top and a line of shade along
 * its foot, so a card reads as something lying on the table and not as a
 * colour painted on it. The room itself is lit from above, a pool of
 * light in the middle of the ceiling and the corners going dark.
 *
 * Nothing here works out a shape while a finger moves that it could have
 * worked out before: paths and paints are made when the size is known and
 * kept until it changes.
 */
final class Cast {

    /** Dark glass, as on the clock and the weather: smoke at the top, nearly black at the foot. */
    static final int GLASS_TOP = 0xE62A2520;
    static final int GLASS_FOOT = 0xF20C0B0A;
    /** The thin dark line of anything cut. */
    static final int CUT = 0x66000000;

    private Cast() {
    }

    /**
     * Bare tiles have no material of their own, but a plate in the settings
     * has to be made of something: it is made of the seed's colour.
     */
    static int metal(int rim) {
        return rim == Tile.Look.BARE ? Tile.Look.ACCENT : rim;
    }

    /** Whether a material is light enough that words cut into it are dark. */
    static boolean light(int kind) {
        switch (kind) {
            case Tile.Look.METAL:
            case Tile.Look.GOLD:
            case Tile.Look.WHITE:
            case Tile.Look.ACCENT:
                return true;
            default:
                return false;
        }
    }

    /** The ink of words and marks cut into a material. */
    static int ink(int kind) {
        if (kind == Tile.Look.ACCENT) {
            return Tone.of(Tone.ON_PRIMARY);
        }
        return light(kind) ? 0xE6231E17 : Tile.accentOf(kind);
    }

    /** The colour of a sign seen through the glass of a window in the material. */
    static int glow(int kind) {
        return kind == Tile.Look.ACCENT ? Tone.of(Tone.PRIMARY) : Tile.accentOf(kind);
    }

    /**
     * Words cut into a plate. Dark words on a light metal are sunk, and
     * catch a pixel of light on their lower edge; light words on a dark
     * wood are inlaid, and throw a pixel of shade.
     */
    static void engrave(TextView view, int kind) {
        view.setTextColor(ink(kind));
        view.setShadowLayer(0.01f, 0f, Round.px(1f), light(kind) ? 0x80FFFFFF : 0x99000000);
    }

    /** A rounded box as a path, its corners clamped to half its shorter side. */
    static Path box(float left, float top, float right, float bottom, float radius) {
        Path path = new Path();
        float r = Math.max(0f, Math.min(radius, Math.min(right - left, bottom - top) / 2f));
        path.addRoundRect(new RectF(left, top, right, bottom), r, r, Path.Direction.CW);
        return path;
    }

    /** The lit top and shaded foot of a plate, as one gradient over its height. */
    static Shader bevel(float top, float height) {
        return new LinearGradient(0f, top, 0f, top + height,
            new int[] {0x38FFFFFF, 0x00FFFFFF, 0x00000000, 0x42000000},
            new float[] {0f, 0.35f, 0.6f, 1f}, Shader.TileMode.CLAMP);
    }

    /** Glass in a box: a little lighter where the light falls, near black below. */
    static Shader glass(float left, float top, float width, float height) {
        return new RadialGradient(left + width / 2f, top + height * 0.35f,
            Math.max(1f, Math.max(width, height) * 0.8f), GLASS_TOP, GLASS_FOOT, Shader.TileMode.CLAMP);
    }

    /**
     * The curved reflection across the top of a shape, as the tiles wear
     * it: the lower edge of a very wide ellipse, cut to the shape.
     */
    static Path glaze(Path shape, float left, float top, float width, float height) {
        Path glaze = new Path();
        glaze.addOval(new RectF(left - width * 0.45f, top - height * 1.05f, left + width * 1.45f,
            top + height * 0.48f), Path.Direction.CW);
        glaze.op(shape, Path.Op.INTERSECT);
        return glaze;
    }

    static Shader glazeLight(float top, float height) {
        return new LinearGradient(0f, top, 0f, top + height * 0.48f, 0x3DFFFFFF, 0x0AFFFFFF,
            Shader.TileMode.CLAMP);
    }

    /** What every drawing here shares: it is opaque where it draws and ignores tinting. */
    abstract static class Solid extends Drawable {

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
    }

    // ------------------------------------------------------------ plate

    /** A plate of a material in a rounded box, lit from above, with the line of its edge. */
    static final class Plate extends Solid {

        private final int kind;
        private final float radius;
        private final boolean gloss;
        private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path shape;
        private Path edge;
        private Path glaze;

        Plate(int kind, float radius, boolean gloss) {
            this.kind = kind;
            this.radius = radius;
            this.gloss = gloss;
            line.setStyle(Paint.Style.STROKE);
            line.setColor(0x73000000);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            int w = bounds.width();
            int h = bounds.height();
            shape = null;
            if (w <= 0 || h <= 0) {
                return;
            }
            float r = Round.px(radius);
            shape = box(0f, 0f, w, h, r);
            float stroke = Math.max(1f, Round.px(0.8f));
            line.setStrokeWidth(stroke);
            edge = box(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f, r - stroke / 2f);
            body.setShader(null);
            Tile.material(body, kind, w, h);
            shade.setShader(bevel(0f, h));
            if (gloss) {
                glaze = glaze(shape, 0f, 0f, w, Math.min(h, Round.px(220f)));
                light.setShader(glazeLight(0f, Math.min(h, Round.px(220f))));
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (shape == null) {
                return;
            }
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.drawPath(shape, body);
            canvas.drawPath(shape, shade);
            if (gloss && glaze != null) {
                canvas.drawPath(glaze, light);
            }
            canvas.drawPath(edge, line);
            canvas.restore();
        }
    }

    // ------------------------------------------------------------ pane

    /**
     * A window of dark glass sunk into what holds it: the shade of the
     * lip falls across its top, and its lower edge catches the light.
     */
    static final class Pane extends Solid {

        private final float radius;
        private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path shape;
        private Path edge;

        Pane(float radius) {
            this.radius = radius;
            rim.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            int w = bounds.width();
            int h = bounds.height();
            shape = null;
            if (w <= 0 || h <= 0) {
                return;
            }
            float r = Round.px(radius);
            float stroke = Math.max(1f, Round.px(1f));
            shape = box(0f, 0f, w, h, r);
            edge = box(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f, r - stroke / 2f);
            body.setShader(glass(0f, 0f, w, h));
            rim.setStrokeWidth(stroke);
            rim.setShader(new LinearGradient(0f, 0f, 0f, h,
                new int[] {0x99000000, 0x33000000, 0x00000000, 0x2EFFFFFF},
                new float[] {0f, 0.3f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            if (shape == null) {
                return;
            }
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.drawPath(shape, body);
            canvas.drawPath(edge, rim);
            canvas.restore();
        }
    }

    // ------------------------------------------------------------ slab

    /** A card of the ground's own tone, lying on the table: a lip of light above, shade below. */
    static final class Slab extends Solid {

        private final int fill;
        private final float radius;
        private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path shape;
        private Path edge;

        Slab(int fill, float radius) {
            this.fill = fill;
            this.radius = radius;
            body.setColor(fill);
            rim.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            int w = bounds.width();
            int h = bounds.height();
            shape = null;
            if (w <= 0 || h <= 0) {
                return;
            }
            float r = Round.px(radius);
            float stroke = Math.max(1f, Round.px(1f));
            shape = box(0f, 0f, w, h, r);
            edge = box(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f, r - stroke / 2f);
            rim.setStrokeWidth(stroke);
            rim.setShader(lip(h));
        }

        @Override
        public void draw(Canvas canvas) {
            if (shape == null) {
                return;
            }
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.drawPath(shape, body);
            canvas.drawPath(edge, rim);
            canvas.restore();
        }
    }

    /** The edge of anything raised: light along the top, nothing at the sides, shade at the foot. */
    static Shader lip(float height) {
        float soft = Math.min(0.3f, Round.px(18f) / Math.max(1f, height));
        return new LinearGradient(0f, 0f, 0f, height,
            new int[] {0x29FFFFFF, 0x00FFFFFF, 0x00000000, 0x59000000},
            new float[] {0f, soft, 1f - soft, 1f}, Shader.TileMode.CLAMP);
    }

    // ------------------------------------------------------------ setting

    /**
     * A card of a group, which can be set. Unset it is a slab like the
     * others. Being chosen, a rim of the material closes in around it from
     * nothing to its full width, and the slab inside turns to dark glass:
     * the chosen thing is set, as a stone is set in a ring.
     */
    static final class Setting extends Solid {

        private final int flat;
        private final Paint slab = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pane = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final RectF inner = new RectF();
        private int kind;
        private float radius;
        private float set;
        private int w;
        private int h;

        Setting(int flat, int kind, float radius, float set) {
            this.flat = flat;
            this.kind = kind;
            this.radius = radius;
            this.set = set;
            slab.setColor(flat);
            lipPaint.setStyle(Paint.Style.STROKE);
            cut.setStyle(Paint.Style.STROKE);
            cut.setColor(CUT);
        }

        /** The corner, in pixels, as the group's movement has it now. */
        void radius(float px) {
            radius = px;
            invalidateSelf();
        }

        /** How far the rim has closed in: nothing for a card not chosen, one for a card set. */
        void set(float amount) {
            set = Math.max(0f, Math.min(1f, amount));
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            w = bounds.width();
            h = bounds.height();
            if (w <= 0 || h <= 0) {
                return;
            }
            body.setShader(null);
            Tile.material(body, kind, w, h);
            shade.setShader(bevel(0f, h));
            pane.setShader(glass(0f, 0f, w, h));
            lipPaint.setStrokeWidth(Math.max(1f, Round.px(1f)));
            lipPaint.setShader(lip(h));
            cut.setStrokeWidth(Math.max(1f, Round.px(0.8f)));
        }

        @Override
        public void draw(Canvas canvas) {
            if (w <= 0 || h <= 0) {
                return;
            }
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            float r = Math.min(radius, Math.min(w, h) / 2f);
            box.set(0f, 0f, w, h);
            if (set < 1f) {
                int a = Math.round(255f * (1f - set));
                slab.setAlpha(a);
                lipPaint.setAlpha(a);
                canvas.drawRoundRect(box, r, r, slab);
                float s = lipPaint.getStrokeWidth() / 2f;
                inner.set(s, s, w - s, h - s);
                canvas.drawRoundRect(inner, r, r, lipPaint);
            }
            if (set > 0f) {
                int a = Math.round(255f * set);
                float ring = Round.px(4f) * set;
                Path outer = new Path();
                outer.addRoundRect(box, r, r, Path.Direction.CW);
                inner.set(ring, ring, w - ring, h - ring);
                float ri = Math.max(0f, r - ring * 0.8f);
                Path hole = new Path();
                hole.addRoundRect(inner, ri, ri, Path.Direction.CW);
                Path band = new Path();
                band.op(outer, hole, Path.Op.DIFFERENCE);
                body.setAlpha(a);
                shade.setAlpha(a);
                pane.setAlpha(a);
                cut.setAlpha(Math.round(0x66 * set));
                canvas.drawPath(hole, pane);
                canvas.drawPath(band, body);
                canvas.drawPath(band, shade);
                canvas.drawPath(hole, cut);
            }
            canvas.restore();
        }
    }

    // ------------------------------------------------------------ seal

    /**
     * A small tile of the owner's look standing for a subject: the plate,
     * the window and the glaze exactly as the tiles wear them, with the
     * subject's sign in the window where an icon would be. The rim is never
     * thinner than a tenth of the seal, or at this size it would be a line
     * and not a material.
     *
     * A seal is drawn in two layers, so the sign can lie between them: the
     * plate and its glass behind, the glaze in front.
     */
    static final class Seal extends Solid {

        static final int BACK = 0;
        static final int FRONT = 1;

        private final int layer;
        private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pane = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Tile.Look look = Tile.Look.MEASURED;
        private boolean dirty = true;
        private Path outer;
        private Path window;
        private Path glaze;
        private boolean plate;

        Seal(int layer) {
            this.layer = layer;
            cut.setStyle(Paint.Style.STROKE);
            cut.setColor(CUT);
        }

        /**
         * Told the look while a dial moves. Nothing is worked out here: a
         * seal that has changed does it when it is next drawn, and one that
         * is not on the screen, behind an open subject, does not do it at all.
         */
        void cast(Tile.Look look) {
            if (look.same(this.look)) {
                return;
            }
            this.look = look;
            dirty = true;
            invalidateSelf();
        }

        /** Cast again whatever the look: the seed's colour, which an accent plate is made of, has moved. */
        void recast(Tile.Look look) {
            this.look = look;
            dirty = true;
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            dirty = true;
        }

        private void shape() {
            Rect b = getBounds();
            int w = b.width();
            int h = b.height();
            outer = null;
            if (w <= 0 || h <= 0) {
                return;
            }
            float tw = w;
            float th = w / look.ratio;
            if (th > h) {
                th = h;
                tw = h * look.ratio;
            }
            float left = (w - tw) / 2f;
            float top = (h - th) / 2f;
            boolean raw = look.window == Tile.Look.RAW;
            plate = !raw && look.rim != Tile.Look.BARE;
            if (raw) {
                // No mask: the sign on a round of glass, the shape the system gives every icon.
                float d = Math.min(tw, th) * 0.94f;
                outer = new Path();
                outer.addOval(new RectF((w - d) / 2f, (h - d) / 2f, (w + d) / 2f, (h + d) / 2f),
                    Path.Direction.CW);
                window = outer;
            } else {
                outer = Tile.curve(left, top, tw, th, look.power);
                float rim = plate ? Math.max(look.width, 0.1f) * tw : 0f;
                if (look.window == Tile.Look.MEDALLION && plate) {
                    float shorter = Math.min(tw, th);
                    float d = shorter - 2f * Math.max(rim, shorter * 0.13f);
                    window = new Path();
                    window.addOval(new RectF((w - d) / 2f, (h - d) / 2f, (w + d) / 2f, (h + d) / 2f),
                        Path.Direction.CW);
                } else if (plate) {
                    window = Tile.curve(left + rim, top + rim, tw - 2f * rim, th - 2f * rim,
                        2f + (look.power - 2f) * 1.27f);
                } else {
                    window = outer;
                }
            }
            int kind = metal(look.rim);
            body.setShader(null);
            Tile.material(body, kind, w, h);
            shade.setShader(bevel(top, th));
            pane.setShader(glass(left, top, tw, th));
            cut.setStrokeWidth(Math.max(1f, tw * 0.012f));
            if (look.gloss && !raw) {
                glaze = Cast.glaze(outer, left, top, tw, th);
                light.setShader(glazeLight(top, th));
            } else {
                glaze = null;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (dirty) {
                shape();
                dirty = false;
            }
            if (outer == null) {
                return;
            }
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            if (layer == BACK) {
                if (plate) {
                    canvas.drawPath(outer, body);
                    canvas.drawPath(outer, shade);
                }
                canvas.drawPath(window, pane);
                canvas.drawPath(window, cut);
            } else if (glaze != null) {
                canvas.drawPath(glaze, light);
            }
            canvas.restore();
        }
    }

    // ------------------------------------------------------------ glass front

    /**
     * The glass front of a case. At rest it holds one faint reflection in
     * its upper corner; asked to, it lets a band of light run across itself
     * once, from left to right, as a pane does when the case is turned in
     * the light.
     */
    static final class Front extends Solid {

        private final float radius;
        private final Paint still = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix move = new Matrix();
        private LinearGradient sheen;
        private Path shape;
        private float wide;
        private float at = -1f;
        private ValueAnimator running;

        Front(float radius) {
            this.radius = radius;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            int w = bounds.width();
            int h = bounds.height();
            shape = null;
            if (w <= 0 || h <= 0) {
                return;
            }
            shape = box(0f, 0f, w, h, Round.px(radius));
            still.setShader(new LinearGradient(0f, 0f, w * 0.55f, h * 0.9f,
                new int[] {0x17FFFFFF, 0x08FFFFFF, 0x00FFFFFF}, new float[] {0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
            wide = Math.max(Round.px(90f), w * 0.28f);
            sheen = new LinearGradient(-wide / 2f, 0f, wide / 2f, 0f,
                new int[] {0x00FFFFFF, 0x14FFFFFF, 0x47FFFFFF, 0x14FFFFFF, 0x00FFFFFF},
                new float[] {0f, 0.3f, 0.5f, 0.7f, 1f}, Shader.TileMode.CLAMP);
            band.setShader(sheen);
        }

        /** One pass of light across the glass. */
        void sweep() {
            if (running != null) {
                running.cancel();
            }
            running = ValueAnimator.ofFloat(0f, 1f);
            running.setDuration(1100L);
            running.setInterpolator(Pace.STANDARD);
            running.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    at = (Float) a.getAnimatedValue();
                    invalidateSelf();
                }
            });
            running.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator a) {
                    at = -1f;
                    invalidateSelf();
                }
            });
            running.start();
        }

        @Override
        public void draw(Canvas canvas) {
            if (shape == null) {
                return;
            }
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.drawPath(shape, still);
            if (at >= 0f && sheen != null) {
                float w = b.width();
                float h = b.height();
                // The band leans, so at its start and end it must stand
                // clear of the glass by its lean as well as its width.
                float clear = h * 0.25f + wide;
                move.reset();
                move.setRotate(-24f);
                move.postTranslate(-clear + at * (w + 2f * clear), h / 2f);
                sheen.setLocalMatrix(move);
                canvas.drawPath(shape, band);
            }
            canvas.restore();
        }
    }

    // ------------------------------------------------------------ the case

    /**
     * The case the owner's tiles stand in: a frame of their material
     * around a floor of dark cloth, lit from above like a vitrine, the
     * light falling in a pool in the upper middle and the corners in
     * shade. The frame's inner edge throws a shadow on the cloth.
     */
    static final class Case extends Solid {

        private final int kind;
        private final int cloth;
        private final int lamp;
        private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint floor = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pool = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint inset = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path frame;
        private Path inside;
        private Path outer;

        Case(int kind, int cloth, int lamp) {
            this.kind = kind;
            this.cloth = cloth;
            this.lamp = lamp;
            floor.setColor(cloth);
            inset.setStyle(Paint.Style.STROKE);
            cut.setStyle(Paint.Style.STROKE);
            cut.setColor(0x80000000);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            int w = bounds.width();
            int h = bounds.height();
            outer = null;
            if (w <= 0 || h <= 0) {
                return;
            }
            float r = Round.px(34f);
            float ring = Round.px(6f);
            outer = box(0f, 0f, w, h, r);
            inside = box(ring, ring, w - ring, h - ring, r - ring * 0.8f);
            frame = new Path();
            frame.op(outer, inside, Path.Op.DIFFERENCE);
            body.setShader(null);
            Tile.material(body, kind, w, h);
            shade.setShader(bevel(0f, h));
            pool.setShader(new RadialGradient(w / 2f, h * 0.18f, Math.max(w, h) * 0.75f,
                new int[] {lamp, (lamp & 0x00FFFFFF) | 0x66000000, cloth & 0x00FFFFFF},
                new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            float deep = Round.px(14f);
            inset.setStrokeWidth(deep);
            inset.setShader(new LinearGradient(0f, ring, 0f, ring + deep * 2f,
                0x8C000000, 0x00000000, Shader.TileMode.CLAMP));
            cut.setStrokeWidth(Math.max(1f, Round.px(1f)));
        }

        @Override
        public void draw(Canvas canvas) {
            if (outer == null) {
                return;
            }
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.drawPath(inside, floor);
            canvas.drawPath(inside, pool);
            // The frame's shadow on the cloth: a wide soft stroke along the
            // inner edge, kept inside by the clip, darkest under the top.
            canvas.save();
            canvas.clipPath(inside);
            canvas.drawPath(inside, inset);
            canvas.restore();
            canvas.drawPath(frame, body);
            canvas.drawPath(frame, shade);
            canvas.drawPath(inside, cut);
            canvas.restore();
        }
    }

    // ------------------------------------------------------------ the room

    /**
     * The room the settings stand in: the ground, a pool of light from a
     * lamp in the middle of the ceiling, and the floor going dark towards
     * the foot of the screen. The lamp can be turned down to nothing and
     * up again, which is how the room is entered.
     */
    static final class Room extends Solid {

        private final Paint ground = new Paint();
        private final Paint pool = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint foot = new Paint();
        private float light = 1f;
        private float lift;
        private float span;
        private int glow;
        private int deep;
        private int w;
        private int h;

        Room() {
            tint();
        }

        /** Colours grown again from the seed. */
        void tint() {
            ground.setColor(Tone.of(Tone.SURFACE));
            glow = Tone.at(17f, 4.0 + 10.0 * Tone.rich(), Tone.hue());
            deep = Tone.of(Tone.SURFACE_LOWEST);
            shade();
            invalidateSelf();
        }

        /**
         * Where this piece of the room stands in the window: how far below
         * its top it begins, and how tall the window is. Two pieces placed
         * so meet without a seam, the one behind the bars and the one a
         * subject slides in on.
         */
        void place(float top, float window) {
            if (top != lift || window != span) {
                lift = top;
                span = window;
                shade();
                invalidateSelf();
            }
        }

        /** The lamp, from off to full. */
        void light(float amount) {
            light = Math.max(0f, Math.min(1f, amount));
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            w = bounds.width();
            h = bounds.height();
            shade();
        }

        private void shade() {
            if (w <= 0 || h <= 0) {
                return;
            }
            float reach = Math.max(w * 1.05f, Round.px(420f));
            pool.setShader(new RadialGradient(w / 2f, -lift - reach * 0.12f, reach,
                new int[] {glow, (glow & 0x00FFFFFF) | 0x80000000, glow & 0x00FFFFFF},
                new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            float whole = span > 0f ? span : h;
            foot.setShader(new LinearGradient(0f, whole * 0.55f - lift, 0f, whole - lift,
                deep & 0x00FFFFFF, deep, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.drawRect(0f, 0f, w, h, ground);
            if (light > 0f) {
                pool.setAlpha(Math.round(255f * light));
                canvas.drawRect(0f, 0f, w, h, pool);
            }
            canvas.drawRect(0f, 0f, w, h, foot);
            canvas.restore();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}
