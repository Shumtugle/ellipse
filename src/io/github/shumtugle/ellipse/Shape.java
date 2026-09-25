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
 * picture is set into the outline: on a ground of the colour at its own
 * edges, a little smaller than the outline; or, if it is solid to its
 * edges already, spread to fill the outline and cut.
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
    /** A drop: round on three corners, square on one, in four turns. */
    static final int TEAR_LOWER_RIGHT = 4;
    static final int TEAR_LOWER_LEFT = 5;
    static final int TEAR_UPPER_LEFT = 6;
    static final int TEAR_UPPER_RIGHT = 7;
    /**
     * A wide tile of paper in a thin grey rim, wider than it is tall by the
     * owner's own old masks, its corners turned by a seventh of its height.
     */
    static final int PAPER = 8;
    static final int COUNT = 9;
    static final String[] NAMES = {"The phone's own", "Circle", "Squircle", "Rounded square",
        "Drop, lower right", "Drop, lower left", "Drop, upper left", "Drop, upper right", "Paper tile"};

    /** How much wider than tall the paper tile stands. */
    static final float PAPER_WIDE = 1.29f;
    private static final int PAPER_INK = 0xFFF7F7F7;
    private static final int RIM_INK = 0xFFBDBDBD;

    /** The outline chosen now; the settings set it, every icon drawn after reads it. */
    static int current = SYSTEM;

    private Shape() {
    }

    /** The outline in a square of the given side, from its top left corner. */
    static Path outline(int shape, float side) {
        return outline(shape, side, side);
    }

    /**
     * The outline in a box of the given width and height, from its top left
     * corner: the shapes stretch to a tile wider or taller than it is square
     * without their corners being drawn out of true — a circle becomes a
     * stadium, a squircle keeps its power, a rounded box its radius.
     */
    static Path outline(int shape, float w, float h) {
        Path path = new Path();
        float least = Math.min(w, h);
        switch (shape) {
            case CIRCLE:
                path.addRoundRect(new RectF(0, 0, w, h), least / 2f, least / 2f, Path.Direction.CW);
                break;
            case SQUIRCLE:
                /* A superellipse of the fifth power: the sides run straight
                   longer than a circle's and turn the corner more softly
                   than a rounded square's. */
                superellipse(path, 0f, 0f, w, h, 5.0);
                break;
            case ROUNDED:
                path.addRoundRect(new RectF(0, 0, w, h), least * 0.22f, least * 0.22f, Path.Direction.CW);
                break;
            case TEAR_LOWER_RIGHT:
            case TEAR_LOWER_LEFT:
            case TEAR_UPPER_LEFT:
            case TEAR_UPPER_RIGHT:
                float[] corners = new float[8];
                java.util.Arrays.fill(corners, least / 2f);
                /* Radii run from the upper left clockwise, two numbers a corner. */
                int square = shape == TEAR_UPPER_LEFT ? 0 : shape == TEAR_UPPER_RIGHT ? 1
                    : shape == TEAR_LOWER_RIGHT ? 2 : 3;
                corners[square * 2] = least * 0.12f;
                corners[square * 2 + 1] = least * 0.12f;
                path.addRoundRect(new RectF(0, 0, w, h), corners, Path.Direction.CW);
                break;
            case PAPER:
                float tall = w / PAPER_WIDE;
                float top = (h - tall) / 2f;
                path.addRoundRect(new RectF(0, top, w, top + tall), tall * 0.14f, tall * 0.14f,
                    Path.Direction.CW);
                break;
            default:
                path.addRoundRect(new RectF(0, 0, w, h), least * 0.5f, least * 0.5f, Path.Direction.CW);
                break;
        }
        return path;
    }

    /** A superellipse of a given power filling a box. */
    static void superellipse(Path path, float left, float top, float w, float h, double power) {
        float a = w / 2f;
        float b = h / 2f;
        for (int i = 0; i <= 240; i++) {
            double t = 2 * Math.PI * i / 240;
            double c = Math.cos(t);
            double s = Math.sin(t);
            float x = (float) (left + a + a * Math.signum(c) * Math.pow(Math.abs(c), 2.0 / power));
            float y = (float) (top + b + b * Math.signum(s) * Math.pow(Math.abs(s), 2.0 / power));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    /** The window a plate may have cut in it: the tile's own outline, round, a squircle, or a scallop. */
    static final int WINDOW_TILE = 0;
    static final int WINDOW_ROUND = 1;
    static final int WINDOW_SQUIRCLE = 2;
    static final int WINDOW_SCALLOP = 3;
    static final String[] WINDOW_NAMES = {"As the tile", "Round", "Squircle", "Scallop"};

    /** The window chosen, and the tile's width to its height, for every icon. */
    static int window = WINDOW_TILE;
    static float aspect = 1f;

    /** A round window with a scalloped edge, a dozen soft waves round it. */
    private static Path scallop(float cx, float cy, float d) {
        Path path = new Path();
        float r = d / 2f / 1.05f;
        for (int i = 0; i <= 360; i++) {
            double t = 2 * Math.PI * i / 360;
            float rr = (float) (r * (1 + 0.05 * Math.cos(12 * t)));
            float x = (float) (cx + rr * Math.cos(t));
            float y = (float) (cy + rr * Math.sin(t));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        return path;
    }

    /** How much smaller than its square an outline is drawn, so every outline weighs the same. */
    static float weight(int shape) {
        switch (shape) {
            case SQUIRCLE:
                return 0.93f;
            case ROUNDED:
                return 0.92f;
            case TEAR_LOWER_RIGHT:
            case TEAR_LOWER_LEFT:
            case TEAR_UPPER_LEFT:
            case TEAR_UPPER_RIGHT:
                return 0.97f;
            default:
                return 1f;
        }
    }

    /** An icon as it is to be drawn: cut to the outline when it comes in layers. */
    static Drawable face(Drawable icon) {
        return face(icon, -1, -1);
    }

    /**
     * An icon in a given outline and colour, where one icon was given its
     * own; less than nought for either keeps what every icon wears. The
     * paper takes every icon, flat ones too: they lie on it as the old masks
     * laid them, at seven tenths of its height.
     */
    static Drawable face(Drawable icon, int shape, int tint) {
        return face(icon, shape, tint, Marks.NONE);
    }

    /**
     * The same, with one of the home screen's own drawings standing in for
     * the app's picture: it is laid in the accent like any one-colour
     * picture, whatever colour every icon wears.
     */
    static Drawable face(Drawable icon, int shape, int tint, int drawing) {
        return face(icon, shape, tint, drawing, AUTO);
    }

    /**
     * An icon whose picture is replaced by a one-colour symbol: the symbol
     * is laid in the accent on the deep ground, at the size the platform's
     * own one-colour pictures take, and cut to the outline.
     */
    static Drawable faceMark(Drawable icon, int shape, Drawable symbol) {
        int outline = shape >= 0 ? shape : current;
        boolean paper = outline == PAPER;
        Drawable mark = new android.graphics.drawable.InsetDrawable(symbol.mutate(), 0.28f);
        mark.setTint(paper ? deep(Tone.primary()) : Tone.primary());
        Drawable made = new AdaptiveIconDrawable(new android.graphics.drawable.ColorDrawable(
            paper ? PAPER_INK : Tone.primaryContainer()), mark);
        return outline == SYSTEM ? made : new Cut(made, outline);
    }

    /** How a coloured picture becomes one colour: left to the picture, or as the owner chose for it. */
    static final int AUTO = 0;
    static final int OUTLINE = 1;
    static final int LIGHT = 2;
    static final int DARK = 3;
    static final int APART = 4;
    static final int METHODS = 5;
    static final String[] METHOD_NAMES = {"By itself", "Its outline", "Its light parts", "Its dark parts",
        "What stands apart"};

    static Drawable face(Drawable icon, int shape, int tint, int drawing, int method) {
        /* An icon a pack gave or made is final: not cut, rimmed or tinted again. */
        if (icon instanceof Pack.Given && drawing == Marks.NONE) {
            return icon;
        }
        int outline = shape >= 0 ? shape : current;
        int colour = tint >= 0 ? tint : Style.tint;
        if (icon != null && drawing != Marks.NONE) {
            boolean paper = outline == PAPER;
            Drawable mark = Marks.picture(drawing);
            mark.setTint(paper ? deep(Tone.primary()) : Tone.primary());
            icon = new AdaptiveIconDrawable(new android.graphics.drawable.ColorDrawable(
                paper ? PAPER_INK : Tone.primaryContainer()), mark);
        } else if (icon != null && method != AUTO) {
            icon = inked(icon, outline, Style.ALL, method);
        } else if (icon != null && colour != Style.OWN) {
            icon = inked(icon, outline, colour, AUTO);
        }
        if (icon == null || outline == SYSTEM) {
            return icon;
        }
        return new Cut(icon, outline);
    }

    /**
     * An icon in the accent. An app that drew a one-colour version of its
     * picture for this gives it, and it is laid in the accent on a ground of
     * the same hue, deep: the way the phone themes its own icons, in the
     * home screen's colour. An app that did not is given one, if every icon
     * is to be in the accent: from the picture over its ground, when it
     * comes in layers; from whatever differs from its edge colour, when it
     * is flat. On the paper the ink is the accent darkened, as ink on paper
     * is. The phone's own outline cuts the result as it cuts any icon.
     */
    static Drawable inked(Drawable icon, int outline, int colour, int method) {
        boolean paper = outline == PAPER;
        int ink = paper ? deep(Tone.primary()) : Tone.primary();
        int ground = paper ? PAPER_INK : Tone.primaryContainer();
        if (method != AUTO) {
            /* Chosen for this icon: it wins over the app's own one-colour picture. */
            return new AdaptiveIconDrawable(new android.graphics.drawable.ColorDrawable(ground),
                stencilBy(icon, ink, method));
        }
        Drawable mark = null;
        if (icon instanceof AdaptiveIconDrawable && android.os.Build.VERSION.SDK_INT >= 33) {
            Drawable mono = ((AdaptiveIconDrawable) icon).getMonochrome();
            if (mono != null) {
                mark = mono.mutate();
                mark.setTint(ink);
            }
        }
        if (mark == null) {
            if (colour != Style.ALL) {
                return icon;
            }
            mark = stencil(icon, ink);
            if (mark == null) {
                /* Before giving up, what stands apart from the picture's main
                   colour is tried: a white plate with a small picture on it
                   still has a picture. */
                mark = stencilBy(icon, ink, APART);
                if (!fair(mark)) {
                    mark = null;
                }
            }
            if (mark == null) {
                /* A photograph, a face: nothing in it stands apart from a
                   ground, and a stencil of it would be a blank. It keeps its
                   own colours. */
                return icon;
            }
        }
        return new AdaptiveIconDrawable(new android.graphics.drawable.ColorDrawable(ground), mark);
    }

    /** The accent brought down to ink, dark enough to stand on white paper. */
    private static int deep(int colour) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(colour, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.25f + 0.1f);
        hsv[2] = 0.42f;
        return android.graphics.Color.HSVToColor(hsv);
    }

    /**
     * A one-colour picture made from a coloured icon, in the layers' own
     * square of one hundred and eight with the picture in its middle
     * seventy two: what stands out from the icon's ground becomes ink.
     */
    /**
     * A one-colour picture made the way the owner chose for this icon: its
     * outline alone; its light parts; its dark parts; or whatever stands
     * apart from the colour most of it is made of. The picture is looked at
     * whole, its ground and its picture together, as it shows.
     */
    private static Drawable stencilBy(Drawable icon, int ink, int method) {
        int n = 216;
        Bitmap out = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas into = new Canvas(out);
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable layers = (AdaptiveIconDrawable) icon;
            if (method != OUTLINE && layers.getBackground() != null) {
                layers.getBackground().setBounds(0, 0, n, n);
                layers.getBackground().draw(into);
            }
            if (layers.getForeground() != null) {
                layers.getForeground().setBounds(0, 0, n, n);
                layers.getForeground().draw(into);
            }
        } else {
            int side = Math.round(n * 72f / 108f * 0.8f);
            int at = (n - side) / 2;
            icon.setBounds(at, at, at + side, at + side);
            icon.draw(into);
        }
        int[] px = new int[n * n];
        out.getPixels(px, 0, n, 0, 0, n, n);
        int main = method == APART ? mainColour(px) : 0;
        int inkRgb = ink & 0x00FFFFFF;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int a = c >>> 24;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            float light = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
            float keep = 1f;
            if (method == LIGHT) {
                keep = (light - 0.55f) / 0.2f;
            } else if (method == DARK) {
                keep = (0.45f - light) / 0.2f;
            } else if (method == APART && main != 0) {
                int dr = r - ((main >> 16) & 0xFF);
                int dg = g - ((main >> 8) & 0xFF);
                int db = b - (main & 0xFF);
                keep = (float) Math.sqrt(dr * dr + dg * dg + db * db) / 90f;
            }
            a = Math.round(a * Math.max(0f, Math.min(1f, keep)));
            px[i] = (a << 24) | inkRgb;
        }
        out.setPixels(px, 0, n, 0, 0, n, n);
        return new android.graphics.drawable.BitmapDrawable((android.content.res.Resources) null, out);
    }

    /** Whether a made one-colour picture holds a picture: neither nearly empty nor nearly full. */
    private static boolean fair(Drawable mark) {
        if (!(mark instanceof android.graphics.drawable.BitmapDrawable)) {
            return mark != null;
        }
        Bitmap drawn = ((android.graphics.drawable.BitmapDrawable) mark).getBitmap();
        int n = drawn.getWidth();
        int[] px = new int[n * drawn.getHeight()];
        drawn.getPixels(px, 0, n, 0, 0, n, drawn.getHeight());
        long covered = 0;
        /* Only what shows counts: the middle seventy two of the layers' hundred and eight. */
        int from = n / 6;
        int to = n - n / 6;
        long area = (long) (to - from) * (to - from);
        for (int y = from; y < to; y++) {
            for (int x = from; x < to; x++) {
                covered += px[y * n + x] >>> 24;
            }
        }
        float share = covered / (255f * area);
        return share > 0.03f && share < 0.62f;
    }

    private static Drawable stencil(Drawable icon, int ink) {
        int n = 216;
        Bitmap out = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas into = new Canvas(out);
        if (icon instanceof AdaptiveIconDrawable) {
            /* The picture layer alone: its shape is what the ground shows. */
            Drawable picture = ((AdaptiveIconDrawable) icon).getForeground();
            if (picture != null) {
                picture.setBounds(0, 0, n, n);
                picture.draw(into);
            }
        } else {
            int side = Math.round(n * 72f / 108f * 0.8f);
            int at = (n - side) / 2;
            icon.setBounds(at, at, at + side, at + side);
            icon.draw(into);
        }
        int[] px = new int[n * n];
        out.getPixels(px, 0, n, 0, 0, n, n);
        boolean flat = !(icon instanceof AdaptiveIconDrawable);
        int edge = 0;
        if (flat) {
            /* The flat icon's own ground: the colour most of its border carries. */
            long r = 0;
            long g = 0;
            long b = 0;
            int seen = 0;
            int side = Math.round(n * 72f / 108f * 0.8f);
            int at = (n - side) / 2;
            for (int i = at; i < at + side; i += 2) {
                int[] ring = {px[at * n + i], px[(at + side - 1) * n + i], px[i * n + at], px[i * n + at + side - 1]};
                for (int c : ring) {
                    if ((c >>> 24) > 200) {
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        seen++;
                    }
                }
            }
            edge = seen == 0 ? 0 : 0xFF000000 | (int) (r / seen) << 16 | (int) (g / seen) << 8 | (int) (b / seen);
            if (edge == 0) {
                /* Clear edges: the icon is a disc, a coin, a shape on
                   nothing. Its own ground is then the colour most of it is
                   made of, and only what differs from that becomes ink. */
                edge = mainColour(px);
            }
        }
        int inkRgb = ink & 0x00FFFFFF;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int a = c >>> 24;
            if (flat && edge != 0 && a > 0) {
                int dr = ((c >> 16) & 0xFF) - ((edge >> 16) & 0xFF);
                int dg = ((c >> 8) & 0xFF) - ((edge >> 8) & 0xFF);
                int db = (c & 0xFF) - (edge & 0xFF);
                float far = (float) Math.sqrt(dr * dr + dg * dg + db * db) / 90f;
                a = Math.round(a * Math.min(1f, far));
            }
            px[i] = (a << 24) | inkRgb;
        }
        /* Ink over nearly the whole square is no picture but a blank. */
        long covered = 0;
        for (int c : px) {
            covered += c >>> 24;
        }
        int flatSide = Math.round(n * 72f / 108f * 0.8f);
        long area = flat ? (long) flatSide * flatSide : (long) px.length;
        if (covered > 255L * area * 0.62f) {
            return null;
        }
        out.setPixels(px, 0, n, 0, 0, n, n);
        return new android.graphics.drawable.BitmapDrawable((android.content.res.Resources) null, out);
    }

    /**
     * The colour most of a picture is made of, among its solid pixels:
     * colours are counted in coarse bins, and the fullest bin's average is
     * taken. Nought when the picture has almost nothing solid in it.
     */
    private static int mainColour(int[] px) {
        int[] count = new int[4096];
        long[] sumR = new long[4096];
        long[] sumG = new long[4096];
        long[] sumB = new long[4096];
        int solid = 0;
        for (int c : px) {
            if ((c >>> 24) < 200) {
                continue;
            }
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            int bin = (r >> 4) << 8 | (g >> 4) << 4 | (b >> 4);
            count[bin]++;
            sumR[bin] += r;
            sumG[bin] += g;
            sumB[bin] += b;
            solid++;
        }
        if (solid < 50) {
            return 0;
        }
        int best = 0;
        for (int i = 1; i < count.length; i++) {
            if (count[i] > count[best]) {
                best = i;
            }
        }
        /* Neighbouring bins belong to the same colour, a little shaded. */
        long r = 0;
        long g = 0;
        long b = 0;
        int all = 0;
        int br = best >> 8;
        int bg = (best >> 4) & 0xF;
        int bb = best & 0xF;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dg = -1; dg <= 1; dg++) {
                for (int db = -1; db <= 1; db++) {
                    int rr = br + dr;
                    int gg = bg + dg;
                    int bbb = bb + db;
                    if (rr < 0 || gg < 0 || bbb < 0 || rr > 15 || gg > 15 || bbb > 15) {
                        continue;
                    }
                    int bin = rr << 8 | gg << 4 | bbb;
                    r += sumR[bin];
                    g += sumG[bin];
                    b += sumB[bin];
                    all += count[bin];
                }
            }
        }
        /* A picture that is mostly one colour on nothing, and little else:
           its shape itself is the ink, not what differs from it. */
        if (all > solid * 0.97f) {
            return 0;
        }
        return 0xFF000000 | (int) (r / all) << 16 | (int) (g / all) << 8 | (int) (b / all);
    }

    /** A fine grain for the paper and its rim, made once. */
    private static android.graphics.BitmapShader grain;

    private static android.graphics.BitmapShader grain() {
        if (grain == null) {
            java.util.Random dice = new java.util.Random(7);
            int n = 96;
            int[] dots = new int[n * n];
            for (int i = 0; i < dots.length; i++) {
                int v = dice.nextInt(256);
                dots[i] = ((dice.nextInt(22)) << 24) | (v << 16) | (v << 8) | v;
            }
            Bitmap tile = Bitmap.createBitmap(dots, n, n, Bitmap.Config.ARGB_8888);
            grain = new android.graphics.BitmapShader(tile, android.graphics.Shader.TileMode.REPEAT,
                android.graphics.Shader.TileMode.REPEAT);
        }
        return grain;
    }

    /**
     * An icon in layers, cut to an outline. It is drawn once into a picture
     * of its size and then only copied, so a list of many icons scrolls as
     * lightly as a list of pictures.
     */
    static final class Cut extends Drawable {
        private final Drawable icon;
        private final int shape;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap drawn;

        Cut(Drawable icon, int shape) {
            this.icon = icon;
            this.shape = shape;
        }

        /** How wide the tile is drawn against the square it is given. */
        float wideness() {
            if (shape == PAPER) {
                return 0.86f * PAPER_WIDE;
            }
            return weight(shape) * (float) Math.sqrt(aspect);
        }

        @Override
        public void draw(Canvas canvas) {
            int side = Math.min(getBounds().width(), getBounds().height());
            if (side <= 0) {
                return;
            }
            if (shape == PAPER) {
                paper(canvas, side);
                return;
            }
            /* The tile keeps the area of its square whatever its proportion:
               wider is lower, taller is narrower. */
            float inside = side * weight(shape);
            float root = (float) Math.sqrt(aspect);
            int tileW = Math.max(1, Math.round(inside * root));
            int tileH = Math.max(1, Math.round(inside / root));
            if (drawn == null || drawn.getWidth() != tileW || drawn.getHeight() != tileH) {
                drawn = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.ARGB_8888);
                Canvas into = new Canvas(drawn);
                float least = Math.min(tileW, tileH);
                /* With a rim, or a window of its own, the outline is a plate
                   and the icon is seen through a window cut in it. */
                boolean plate = Rim.kind != Rim.NONE || window != WINDOW_TILE;
                float rim = plate ? Math.max(1f, Rim.width * least) : 0f;
                Path outer = outline(shape, tileW, tileH);
                if (plate) {
                    Rim.plate(into, outer, Rim.kind == Rim.NONE ? Rim.GROUND : Rim.kind, tileW, tileH);
                }
                Path cut;
                RectF hole;
                if (window == WINDOW_TILE) {
                    hole = new RectF(rim, rim, tileW - rim, tileH - rim);
                    cut = outline(shape, hole.width(), hole.height());
                    cut.offset(rim, rim);
                } else {
                    float d = least - 2f * Math.max(rim, least * 0.13f);
                    hole = new RectF((tileW - d) / 2f, (tileH - d) / 2f, (tileW + d) / 2f, (tileH + d) / 2f);
                    cut = new Path();
                    if (window == WINDOW_ROUND) {
                        cut.addOval(hole, Path.Direction.CW);
                    } else if (window == WINDOW_SQUIRCLE) {
                        superellipse(cut, hole.left, hole.top, d, d, 5.0);
                    } else {
                        cut = scallop(hole.centerX(), hole.centerY(), d);
                    }
                }
                into.save();
                into.clipPath(cut);
                if (!(icon instanceof AdaptiveIconDrawable)) {
                    flat(into, hole);
                } else {
                    layered(into, (AdaptiveIconDrawable) icon, hole);
                }
                into.restore();
                if (plate) {
                    Rim.cut(into, cut, least);
                }
                if (Rim.glaze || Rim.kind == Rim.GLASS) {
                    Rim.glaze(into, outer, 0f, 0f, tileW, tileH);
                }
            }
            float left = getBounds().exactCenterX() - tileW / 2f;
            float top = getBounds().exactCenterY() - tileH / 2f;
            canvas.drawBitmap(drawn, left, top, paint);
        }

        /**
         * An icon in layers laid under a window of the given side, as the
         * platform lays them: a quarter of the visible side more on every
         * edge; the owner's fill draws them larger or smaller about the middle.
         */
        private void layered(Canvas into, AdaptiveIconDrawable layers, RectF hole) {
            /* The promised middle fills the window's shorter side; the spare
               margin every such icon carries covers the longer. */
            float shorter = Math.min(hole.width(), hole.height());
            float longer = Math.max(hole.width(), hole.height());
            float all = Math.max(shorter * 1.5f * Style.fill, longer);
            int left = Math.round(hole.centerX() - all / 2f);
            int top = Math.round(hole.centerY() - all / 2f);
            int side = Math.round(all);
            Drawable ground = layers.getBackground();
            if (ground != null) {
                ground.setBounds(left, top, left + side, top + side);
                ground.draw(into);
            }
            Drawable picture = layers.getForeground();
            if (picture != null) {
                picture.setBounds(left, top, left + side, top + side);
                picture.draw(into);
            }
        }

        /**
         * A flat icon set into the outline. It is looked at first, small: if
         * its edges are solid all round, it is a picture meant to fill, and
         * it is spread to the outline and cut; otherwise it stands on a
         * ground of the colour its edges carry, or on pale paper when its
         * edges carry nothing, at seven tenths of the outline.
         */
        private void flat(Canvas into, RectF hole) {
            int look = 48;
            Bitmap small = Bitmap.createBitmap(look, look, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, look, look);
            icon.draw(new Canvas(small));
            long r = 0;
            long g = 0;
            long b = 0;
            int solid = 0;
            int ring = 0;
            for (int i = 0; i < look; i++) {
                int[][] at = {{i, 0}, {i, look - 1}, {0, i}, {look - 1, i}};
                for (int[] p : at) {
                    int c = small.getPixel(p[0], p[1]);
                    ring++;
                    if ((c >>> 24) > 200) {
                        solid++;
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                    }
                }
            }
            /* A little inside the edge too: many flat icons are a disc or a
               rounded square with a thin margin round it. */
            int inner = 0;
            int innerSolid = 0;
            int d = look / 8;
            for (int i = d; i < look - d; i++) {
                int[][] at = {{i, d}, {i, look - 1 - d}, {d, i}, {look - 1 - d, i}};
                for (int[] p : at) {
                    int c = small.getPixel(p[0], p[1]);
                    inner++;
                    if ((c >>> 24) > 200) {
                        innerSolid++;
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                    }
                }
            }
            small.recycle();
            boolean full = solid > ring * 0.9f;
            int seen = solid + innerSolid;
            int ground = seen > (ring + inner) * 0.25f
                ? 0xFF000000 | (int) (r / seen) << 16 | (int) (g / seen) << 8 | (int) (b / seen)
                : PAPER_INK;
            into.drawColor(ground);
            float wide = full ? Math.max(hole.width(), hole.height()) * 1.02f
                : Math.min(hole.width(), hole.height()) * 0.72f * Style.fill;
            float x = hole.centerX() - wide / 2f;
            float y = hole.centerY() - wide / 2f;
            icon.setBounds(Math.round(x), Math.round(y), Math.round(x + wide), Math.round(y + wide));
            icon.draw(into);
        }

        /**
         * The paper tile, a little wider than the square it is given and a
         * little lower: a rim of grey, the paper inside it, and on the paper
         * the icon. An icon in layers spreads its ground across the whole
         * paper and keeps its picture whole within the paper's height; a
         * flat icon lies on the paper at seven tenths of its height.
         */
        private void paper(Canvas canvas, int side) {
            float tall = side * 0.86f;
            float wide = tall * PAPER_WIDE;
            int w = Math.round(wide);
            int h = Math.round(tall);
            if (drawn == null || drawn.getHeight() != h) {
                drawn = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas into = new Canvas(drawn);
                Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                Paint grit = new Paint(Paint.ANTI_ALIAS_FLAG);
                grit.setShader(grain());
                RectF outer = new RectF(0, 0, w, h);
                float round = h * 0.14f;
                fill.setColor(RIM_INK);
                into.drawRoundRect(outer, round, round, fill);
                into.drawRoundRect(outer, round, round, grit);
                float rim = Math.max(1f, h * 0.033f);
                RectF inner = new RectF(rim, rim, w - rim, h - rim);
                float innerRound = Math.max(0f, round - rim);
                fill.setColor(PAPER_INK);
                into.drawRoundRect(inner, innerRound, innerRound, fill);
                into.save();
                Path clip = new Path();
                clip.addRoundRect(inner, innerRound, innerRound, Path.Direction.CW);
                into.clipPath(clip);
                float cx = inner.centerX();
                float cy = inner.centerY();
                if (icon instanceof AdaptiveIconDrawable) {
                    AdaptiveIconDrawable layers = (AdaptiveIconDrawable) icon;
                    /* The ground covers the paper's width; the picture keeps
                       to its height, so no part of it is cut. */
                    float groundSide = inner.width() * 1.5f;
                    Drawable ground = layers.getBackground();
                    if (ground != null) {
                        ground.setBounds(Math.round(cx - groundSide / 2f), Math.round(cy - groundSide / 2f),
                            Math.round(cx + groundSide / 2f), Math.round(cy + groundSide / 2f));
                        ground.draw(into);
                    }
                    float pictureSide = inner.height() * 1.5f * Style.fill;
                    Drawable picture = layers.getForeground();
                    if (picture != null) {
                        picture.setBounds(Math.round(cx - pictureSide / 2f), Math.round(cy - pictureSide / 2f),
                            Math.round(cx + pictureSide / 2f), Math.round(cy + pictureSide / 2f));
                        picture.draw(into);
                    }
                } else {
                    float flat = h * 0.7f * Style.fill;
                    icon.setBounds(Math.round(cx - flat / 2f), Math.round(cy - flat / 2f),
                        Math.round(cx + flat / 2f), Math.round(cy + flat / 2f));
                    icon.draw(into);
                }
                into.drawRoundRect(inner, innerRound, innerRound, grit);
                into.restore();
            }
            float left = getBounds().centerX() - w / 2f;
            float top = getBounds().centerY() - h / 2f;
            canvas.drawBitmap(drawn, left, top, paint);
        }

        @Override
        public int getIntrinsicWidth() {
            return icon.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return icon.getIntrinsicHeight();
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
