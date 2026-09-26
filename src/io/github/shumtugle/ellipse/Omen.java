package io.github.shumtugle.ellipse;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A mark thrown by the dice: a pictogram for a thing no symbol shows. It is
 * not random lines but a thing made the way things are made — a slab two
 * or three times over, a body with something on it, a thing within a
 * thing, rays round a middle, rings in a chain, a head on shoulders, a
 * roof on walls, a vessel, a frame and its panes, rails and their ties, a
 * cross, a whirl — so that the eye finds in it what it likes: a switch,
 * books, a home. One number, its seed, throws it the same every time.
 *
 * It is drawn on the grid symbols are drawn on, twenty-four units across,
 * in lines two units wide, in one colour given as a tint.
 */
final class Omen extends Drawable {

    /** The word a mark is kept by among a face's symbols: this, then its seed. */
    static final String WORD = "omen_";

    private static final int RECT = 0;
    private static final int LEAN = 1;
    private static final int RING = 2;
    private static final int DISC = 3;
    private static final int LINE = 4;
    private static final int ARC = 5;
    private static final int PLUS = 6;
    private static final int CROSS = 7;
    private static final int CHECK = 8;
    private static final int POLY = 9;
    private static final int PATH = 10;

    /** One stroke or shape of a mark, in grid units. */
    private static final class Part {
        final int kind;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        final boolean filled;
        /** A rectangle's rounding; an arc's first angle. */
        final float extra;
        /** An arc's sweep. */
        float sweep;
        float[] points;

        Part(int kind, double x0, double y0, double x1, double y1, boolean filled, double extra) {
            this.kind = kind;
            this.x0 = (float) Math.min(x0, x1);
            this.y0 = (float) Math.min(y0, y1);
            this.x1 = (float) Math.max(x0, x1);
            this.y1 = (float) Math.max(y0, y1);
            this.filled = filled;
            this.extra = (float) extra;
        }

        /** A line keeps its direction: its ends as given. */
        static Part line(double x0, double y0, double x1, double y1) {
            Part made = new Part(LINE, 0, 0, 0, 0, false, 0);
            made.points = new float[] {(float) x0, (float) y0, (float) x1, (float) y1};
            return made;
        }

        static Part arc(double x0, double y0, double x1, double y1, double from, double sweep) {
            Part made = new Part(ARC, x0, y0, x1, y1, false, from);
            made.sweep = (float) sweep;
            return made;
        }

        static Part shape(int kind, boolean filled, List<double[]> at) {
            Part made = new Part(kind, 0, 0, 0, 0, filled, 0);
            made.points = new float[at.size() * 2];
            for (int i = 0; i < at.size(); i++) {
                made.points[i * 2] = (float) at.get(i)[0];
                made.points[i * 2 + 1] = (float) at.get(i)[1];
            }
            return made;
        }
    }

    private final List<Part> parts = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int colour = 0xFFFFFFFF;
    /** A being's one outline, a body and what grew on it, in grid units; none for a mark of the first kinds. */
    private Path silhouette;
    private boolean solid;
    /** What is cut from a solid being, or drawn thin within an outlined one: eyes, a beat, facets. */
    private final List<Part> holes = new ArrayList<>();

    Omen(long seed) {
        Random r = new Random(seed & FIRST);
        int kind = pickKind(r, second(seed));
        if (kind == BEING) {
            being(r);
        } else {
            recipe(kind, r, parts);
        }
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    /**
     * Seeds of the second dice carry this bit: they may throw beings too. The
     * seeds kept before it came throw exactly as they always did.
     */
    static final long SECOND = 0x4000000000000000L;
    private static final long FIRST = ~SECOND;

    private static boolean second(long seed) {
        return (seed & SECOND) != 0;
    }

    /** A fresh seed of the dice as it throws now. */
    static long fresh(Random dice) {
        return (dice.nextLong() & 0xFFFFFFFFFFL) | SECOND;
    }

    /** The mark a kept word names; or none if the word is not a mark's. */
    static Omen of(String word) {
        if (word == null || !word.startsWith(WORD)) {
            return null;
        }
        try {
            return new Omen(Long.parseLong(word.substring(WORD.length())));
        } catch (NumberFormatException broken) {
            return null;
        }
    }

    // ------------------------------------------------------------ the dice

    /** Which of the kinds a seed throws, by its first choice. */
    static int kindOf(long seed) {
        return pickKind(new Random(seed & FIRST), second(seed));
    }

    private static final int[] WEIGHTS = {3, 3, 3, 1, 1, 2, 3, 1, 2, 1, 2, 2, 1, 1, 1, 1, 1};
    /** The second dice: the same kinds, and beings as often as a third of all. */
    private static final int[] WEIGHTS_SECOND = {3, 3, 3, 1, 1, 2, 3, 1, 2, 1, 2, 2, 1, 1, 1, 1, 1, 14};
    private static final int BEING = 17;

    private static int pickKind(Random r, boolean second) {
        int[] weights = second ? WEIGHTS_SECOND : WEIGHTS;
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int pick = r.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            if (pick < weights[i]) {
                return i;
            }
            pick -= weights[i];
        }
        return 0;
    }

    /** Marks like a chosen one: the same kind of thing, its numbers thrown otherwise. */
    static List<Long> like(long seed, int count) {
        int kind = kindOf(seed);
        long mark = seed & SECOND;
        List<Long> found = new ArrayList<>();
        long next = (seed & FIRST) * 7919L;
        while (found.size() < count) {
            next++;
            long one = (next & FIRST) | mark;
            if (kindOf(one) == kind) {
                found.add(one);
            }
        }
        return found;
    }

    private static void recipe(int kind, Random r, List<Part> p) {
        switch (kind) {
            case 0: stack(r, p); break;
            case 1: attach(r, p); break;
            case 2: nest(r, p); break;
            case 3: grid(r, p); break;
            case 4: waves(r, p); break;
            case 5: sign(r, p); break;
            case 6: radial(r, p); break;
            case 7: chain(r, p); break;
            case 8: figure(r, p); break;
            case 9: face(r, p); break;
            case 10: house(r, p); break;
            case 11: vessel(r, p); break;
            case 12: pad(r, p); break;
            case 13: frame(r, p); break;
            case 14: rails(r, p); break;
            case 15: crossed(r, p); break;
            default: whirl(r, p); break;
        }
    }

    private static double one(Random r, double... of) {
        return of[r.nextInt(of.length)];
    }

    private static int pick(Random r, int... of) {
        return of[r.nextInt(of.length)];
    }

    private static List<double[]> pts(double... xy) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i + 1 < xy.length; i += 2) {
            out.add(new double[] {xy[i], xy[i + 1]});
        }
        return out;
    }

    private static double[] turn(double cx, double cy, double rad, double a) {
        return new double[] {cx + rad * Math.cos(a), cy + rad * Math.sin(a)};
    }

    /** The same slab two to four times: books, layers, bars; the last may lean on the others. */
    private static void stack(Random r, List<Part> p) {
        int n = pick(r, 2, 3, 3, 4);
        boolean upright = r.nextDouble() < 0.6;
        double thick = one(r, 3, 4, 4, 5);
        double gap = one(r, 1, 2, 2);
        double span = n * thick + (n - 1) * gap;
        double start = 12 - span / 2;
        double tall = one(r, 14, 16, 18);
        boolean uneven = r.nextDouble() < 0.35;
        boolean lean = r.nextDouble() < 0.25;
        for (int i = 0; i < n; i++) {
            double a = start + i * (thick + gap);
            double h = tall - (uneven ? one(r, 0, 2, 4) : 0);
            boolean filled = r.nextDouble() < 0.3;
            if (upright) {
                p.add(new Part(lean && i == n - 1 ? LEAN : RECT, a, 21 - h, a + thick, 21, filled, 1));
            } else {
                p.add(new Part(RECT, 12 - h / 2, a, 12 + h / 2, a + thick, filled, 1));
            }
        }
    }

    /** A body and something on it: a ring with a handle, a box with a knob. */
    private static void attach(Random r, List<Part> p) {
        int body = pick(r, 0, 0, 1, 2);
        double size = one(r, 10, 12, 12, 14);
        int side = r.nextInt(5);
        double[][] shift = {{-2, -2}, {0, -3}, {0, 3}, {-3, 0}, {2, 2}};
        double[][] way = {{1, 1}, {0, 1}, {0, -1}, {1, 0}, {-1, -1}};
        double cx = 12 + shift[side][0];
        double cy = 12 + shift[side][1];
        double half = size / 2;
        if (body == 1) {
            p.add(new Part(RECT, cx - half, cy - half, cx + half, cy + half, false, 2));
        } else {
            p.add(new Part(body == 2 ? DISC : RING, cx - half, cy - half, cx + half, cy + half, body == 2, 0));
        }
        double norm = Math.hypot(way[side][0], way[side][1]);
        double ux = way[side][0] / norm;
        double uy = way[side][1] / norm;
        double ex = cx + ux * half;
        double ey = cy + uy * half;
        double length = one(r, 4, 5, 6);
        if (r.nextInt(3) < 2) {
            p.add(Part.line(ex, ey, ex + ux * length, ey + uy * length));
        } else {
            p.add(Part.line(ex, ey, ex + ux * (length - 2), ey + uy * (length - 2)));
            double kx = ex + ux * length;
            double ky = ey + uy * length;
            p.add(new Part(DISC, kx - 1.6, ky - 1.6, kx + 1.6, ky + 1.6, true, 0));
        }
        if (r.nextDouble() < 0.35) {
            p.add(new Part(DISC, cx - 1.5, cy - 1.5, cx + 1.5, cy + 1.5, true, 0));
        }
    }

    /** A thing within a thing: a ring and a dot, a frame, a screen on its foot. */
    private static void nest(Random r, List<Part> p) {
        boolean ring = r.nextInt(3) == 0;
        double h = one(r, 16, 18, 18, 20) / 2;
        boolean wide = !ring && r.nextDouble() < 0.4;
        double ax = h;
        double ay = wide ? h * 0.7 : h;
        p.add(new Part(ring ? RING : RECT, 12 - ax, 12 - ay, 12 + ax, 12 + ay, false, pick(r, 1, 2, 3)));
        int inner = r.nextInt(5);
        double k = one(r, 0.35, 0.45, 0.55);
        if (inner == 0) {
            p.add(new Part(DISC, 12 - ax * k, 12 - ax * k, 12 + ax * k, 12 + ax * k, true, 0));
        } else if (inner == 1) {
            p.add(new Part(RING, 12 - ax * k, 12 - ax * k, 12 + ax * k, 12 + ax * k, false, 0));
        } else if (inner == 2) {
            p.add(new Part(RECT, 12 - ax * 0.6, 12 - ay * 0.6, 12 + ax * 0.6, 12 + ay * 0.2, true, 1));
        } else if (inner == 3) {
            p.add(Part.line(12 - ax * 0.55, 12 + ay * 0.35, 12 + ax * 0.55, 12 + ay * 0.35));
            p.add(Part.line(12 - ax * 0.55, 12 - ay * 0.05, 12 + ax * 0.2, 12 - ay * 0.05));
        } else {
            for (int i = -1; i <= 1; i++) {
                p.add(new Part(DISC, 12 + i * 4 - 1.4, 10.6, 12 + i * 4 + 1.4, 13.4, true, 0));
            }
        }
        if (!ring && r.nextDouble() < 0.3) {
            p.add(Part.line(9, 12 + ay + 2.5, 15, 12 + ay + 2.5));
        }
    }

    /** Four or nine dots or squares; one may give its place to a plus. */
    private static void grid(Random r, List<Part> p) {
        int n = pick(r, 2, 2, 3);
        int kind = r.nextInt(3);
        double cell = 16.0 / n;
        double size = cell * one(r, 0.6, 0.7, 0.75);
        boolean skip = r.nextDouble() < 0.3;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (skip && i == n - 1 && j == n - 1) {
                    continue;
                }
                double cx = 4 + cell * (i + 0.5);
                double cy = 4 + cell * (j + 0.5);
                boolean filled = kind == 0 || (kind == 1 && r.nextDouble() < 0.5);
                p.add(new Part(kind == 1 ? RECT : kind == 0 ? DISC : RING, cx - size / 2, cy - size / 2,
                    cx + size / 2, cy + size / 2, filled, kind == 1 ? 1 : 0));
            }
        }
        if (skip) {
            double c = 4 + cell * (n - 0.5);
            p.add(new Part(PLUS, c - size / 2, c - size / 2, c + size / 2, c + size / 2, false, 0));
        }
    }

    /** Arcs from a point: sound, a signal, warmth. */
    private static void waves(Random r, List<Part> p) {
        int n = pick(r, 2, 3, 3);
        int corner = r.nextInt(3);
        double[][] at = {{12, 19, 225}, {5, 12, 315}, {5, 19, 270}};
        double ox = at[corner][0];
        double oy = at[corner][1];
        p.add(new Part(DISC, ox - 1.8, oy - 1.8, ox + 1.8, oy + 1.8, true, 0));
        double step = one(r, 4, 4, 5);
        for (int i = 0; i < n; i++) {
            double rad = 5 + i * step;
            p.add(Part.arc(ox - rad, oy - rad, ox + rad, oy + rad, at[corner][2], 90));
        }
    }

    /** A body with a sign on it: a card, a target, a note, a tick. */
    private static void sign(Random r, List<Part> p) {
        boolean box = r.nextInt(3) != 1;
        double ax;
        double ay;
        if (box) {
            boolean wide = r.nextDouble() < 0.5;
            ax = wide ? 9 : 7;
            ay = wide ? 6.5 : 9;
            p.add(new Part(RECT, 12 - ax, 12 - ay, 12 + ax, 12 + ay, false, 2));
        } else {
            ax = ay = 9;
            p.add(new Part(RING, 3, 3, 21, 21, false, 0));
        }
        int what = r.nextInt(5);
        double k = 0.45;
        if (what == 0) {
            p.add(new Part(PLUS, 12 - ax * k, 12 - ax * k, 12 + ax * k, 12 + ax * k, false, 0));
        } else if (what == 1) {
            p.add(new Part(CROSS, 12 - ax * k, 12 - ax * k, 12 + ax * k, 12 + ax * k, false, 0));
        } else if (what == 2) {
            int lines = pick(r, 2, 3);
            for (int i = 0; i < lines; i++) {
                double y = 12 - ay * 0.4 + i * 4;
                p.add(Part.line(12 - ax * 0.55, y, 12 + ax * (i == 0 ? 0.55 : 0.2), y));
            }
        } else if (what == 3) {
            p.add(new Part(CHECK, 12 - ax * 0.5, 12 - ay * 0.3, 12 + ax * 0.5, 12 + ay * 0.35, false, 0));
        } else {
            p.add(new Part(DISC, 10, 10, 14, 14, true, 0));
        }
    }

    /** Things with rays or petals round a middle: a sun, a flower, a snowflake, a web, a star. */
    private static void radial(Random r, List<Part> p) {
        int kind = r.nextInt(6);
        double up = -Math.PI / 2;
        if (kind == 0) {
            int n = pick(r, 8, 8, 12);
            double core = one(r, 3.5, 4, 4.5);
            boolean ring = r.nextDouble() < 0.6;
            p.add(new Part(ring ? RING : DISC, 12 - core, 12 - core, 12 + core, 12 + core, !ring, 0));
            for (int i = 0; i < n; i++) {
                double a = up + 2 * Math.PI * i / n;
                double[] a0 = turn(12, 12, core + 2.5, a);
                double[] a1 = turn(12, 12, i % 2 == 0 || n == 8 ? 9.5 : 8, a);
                p.add(Part.line(a0[0], a0[1], a1[0], a1[1]));
            }
        } else if (kind == 1) {
            int n = pick(r, 5, 6, 8);
            double petal = one(r, 4.5, 5, 5.5);
            double wide = one(r, 2.2, 2.6, 3);
            for (int i = 0; i < n; i++) {
                double a = up + 2 * Math.PI * i / n;
                List<double[]> round = new ArrayList<>();
                for (int k = 0; k < 24; k++) {
                    double t = 2 * Math.PI * k / 24;
                    double px = petal * Math.cos(t) + petal + 1.2;
                    double py = wide * Math.sin(t);
                    round.add(new double[] {12 + px * Math.cos(a) - py * Math.sin(a),
                        12 + px * Math.sin(a) + py * Math.cos(a)});
                }
                p.add(Part.shape(POLY, false, round));
            }
            p.add(new Part(DISC, 10.3, 10.3, 13.7, 13.7, true, 0));
        } else if (kind == 2) {
            int n = pick(r, 6, 6, 8);
            for (int i = 0; i < n; i++) {
                double a = up + 2 * Math.PI * i / n;
                double[] end = turn(12, 12, 9.5, a);
                p.add(Part.line(12, 12, end[0], end[1]));
                double[][] twigs = {{5.5, 2.6}, {8, 1.8}};
                for (double[] twig : twigs) {
                    double[] b = turn(12, 12, twig[0], a);
                    for (int sgn = -1; sgn <= 1; sgn += 2) {
                        double[] e = turn(b[0], b[1], twig[1], a + sgn * 0.8);
                        p.add(Part.line(b[0], b[1], e[0], e[1]));
                    }
                }
            }
        } else if (kind == 3) {
            int n = pick(r, 6, 8);
            for (int i = 0; i < n; i++) {
                double[] end = turn(12, 12, 10, up + 2 * Math.PI * i / n);
                p.add(Part.line(12, 12, end[0], end[1]));
            }
            int rings = pick(r, 2, 3);
            double[] radii = {3.5, 6.5, 9.5};
            for (int k = 0; k < rings; k++) {
                List<double[]> web = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    double a0 = up + 2 * Math.PI * i / n;
                    double a1 = up + 2 * Math.PI * (i + 1) / n;
                    web.add(turn(12, 12, radii[k], a0));
                    web.add(turn(12, 12, radii[k] * 0.86, (a0 + a1) / 2));
                }
                p.add(Part.shape(POLY, false, web));
            }
        } else if (kind == 4) {
            int n = pick(r, 5, 5, 6);
            double inner = one(r, 0.4, 0.45, 0.5);
            List<double[]> star = new ArrayList<>();
            for (int i = 0; i < n * 2; i++) {
                star.add(turn(12, 12.6, i % 2 == 0 ? 9.5 : 9.5 * inner, up + Math.PI * i / n));
            }
            p.add(Part.shape(POLY, r.nextDouble() < 0.3, star));
        } else {
            int n = pick(r, 8, 12, 16);
            for (int i = 0; i < n; i++) {
                double a = up + 2 * Math.PI * i / n;
                double[] a0 = turn(12, 12, i % 2 == 1 ? 3 : 5, a);
                double[] a1 = turn(12, 12, i % 2 == 0 ? 9.5 : 7, a);
                p.add(Part.line(a0[0], a0[1], a1[0], a1[1]));
            }
        }
    }

    /** Rings linked: two, three or four in a row, five in two rows. */
    private static void chain(Random r, List<Part> p) {
        int kind = r.nextInt(4);
        if (kind == 0) {
            double rad = 6;
            p.add(new Part(RING, 12 - 3.6 - rad, 13 - rad, 12 - 3.6 + rad, 13 + rad, false, 0));
            p.add(new Part(RING, 12 + 3.6 - rad, 13 - rad, 12 + 3.6 + rad, 13 + rad, false, 0));
            if (r.nextDouble() < 0.6) {
                p.add(Part.shape(POLY, true, pts(6.6, 5.4, 8.4, 3.6, 10.2, 5.4, 8.4, 7.4)));
            }
        } else if (kind < 3) {
            int n = pick(r, 3, 4);
            double rad = n == 3 ? 4.6 : 3.8;
            double step = rad * 1.35;
            double start = 12 - step * (n - 1) / 2;
            for (int i = 0; i < n; i++) {
                double cx = start + i * step;
                p.add(new Part(RING, cx - rad, 12 - rad, cx + rad, 12 + rad, false, 0));
            }
        } else {
            double rad = 3.5;
            double[][] at = {{5, 10}, {12, 10}, {19, 10}, {8.5, 14.2}, {15.5, 14.2}};
            for (double[] c : at) {
                p.add(new Part(RING, c[0] - rad, c[1] - rad, c[0] + rad, c[1] + rad, false, 0));
            }
        }
    }

    private static void person(List<Part> p, double cx, double top, double scale, boolean filled) {
        double head = 3.4 * scale;
        p.add(new Part(filled ? DISC : RING, cx - head, top, cx + head, top + head * 2, filled, 0));
        double sy = top + head * 2 + 1.6 * scale;
        p.add(Part.arc(cx - 6.4 * scale, sy, cx + 6.4 * scale, sy + 11 * scale, 180, 180));
        p.add(Part.line(cx - 6.4 * scale, sy + 5.5 * scale, cx + 6.4 * scale, sy + 5.5 * scale));
    }

    /** A head on shoulders: one, two, or one with a sign beside it. */
    private static void figure(Random r, List<Part> p) {
        int kind = pick(r, 0, 0, 1, 2);
        boolean filled = r.nextDouble() < 0.3;
        if (kind == 0) {
            person(p, 12, 3.5, 1.15, filled);
        } else if (kind == 1) {
            person(p, 15, 5.5, 0.9, false);
            person(p, 9, 3.8, 1.0, filled);
        } else {
            person(p, 10, 4, 1.0, filled);
            int sign = r.nextInt(3);
            p.add(new Part(sign == 0 ? PLUS : sign == 1 ? CHECK : CROSS, 16, 5, 21, 10, false, 0));
        }
    }

    /** A round face: its eyes, and a mouth that decides the mood. */
    private static void face(Random r, List<Part> p) {
        p.add(new Part(RING, 3, 3, 21, 21, false, 0));
        int eyes = pick(r, 0, 0, 1, 2);
        if (eyes == 1) {
            p.add(Part.line(8, 9.6, 10, 9.6));
            p.add(Part.line(14, 9.6, 16, 9.6));
        } else {
            p.add(new Part(DISC, 7.8, 8.4, 10.2, 10.8, true, 0));
            if (eyes == 0) {
                p.add(new Part(DISC, 13.8, 8.4, 16.2, 10.8, true, 0));
            } else {
                p.add(Part.line(14, 9.6, 16.4, 9.6));
            }
        }
        int mouth = pick(r, 0, 0, 1, 2, 3);
        if (mouth == 0) {
            p.add(Part.arc(7.5, 8.5, 16.5, 16.5, 25, 130));
        } else if (mouth == 1) {
            p.add(Part.line(8.5, 15, 15.5, 15));
        } else if (mouth == 2) {
            p.add(Part.arc(8, 14.5, 16, 20, 205, 130));
        } else {
            p.add(new Part(RING, 10.2, 13.2, 13.8, 16.8, false, 0));
        }
    }

    /** A roof on walls: a house, a shop, a tent. */
    private static void house(Random r, List<Part> p) {
        int kind = pick(r, 0, 0, 1, 2);
        if (kind == 1) {
            p.add(Part.shape(POLY, false, pts(12, 3.5, 21, 20, 3, 20)));
            p.add(Part.shape(PATH, false, pts(9.5, 20, 12, 14, 14.5, 20)));
            return;
        }
        double wall = one(r, 13, 14, 15);
        double top = 10;
        double left = 12 - wall / 2 + 0.5;
        double right = 12 + wall / 2 - 0.5;
        p.add(Part.shape(PATH, false, pts(3, top + 1.5, 12, 3.5, 21, top + 1.5)));
        p.add(Part.shape(PATH, false, pts(left, top, left, 20.5, right, 20.5, right, top)));
        if (kind == 2) {
            p.add(Part.line(left, top + 3, right, top + 3));
        }
        int door = r.nextInt(4);
        if (door == 0) {
            p.add(Part.shape(PATH, false, pts(10.5, 20.5, 10.5, 15, 13.5, 15, 13.5, 20.5)));
        } else if (door == 1) {
            p.add(Part.arc(9.8, 13.6, 14.2, 18, 180, 180));
            p.add(Part.line(9.8, 15.8, 9.8, 20.5));
            p.add(Part.line(14.2, 15.8, 14.2, 20.5));
        } else if (door == 2) {
            p.add(new Part(RECT, 10, 13.5, 14, 17.5, false, 0.5));
        }
        if (r.nextDouble() < 0.35) {
            p.add(Part.line(16.5, 5.8, 16.5, 3.5));
        }
    }

    /** Something to hold things in: a bucket, a cup, a bag, a pot. */
    private static void vessel(Random r, List<Part> p) {
        int kind = r.nextInt(4);
        if (kind == 0) {
            p.add(Part.shape(POLY, false, pts(5, 9, 19, 9, 17, 21, 7, 21)));
            p.add(Part.arc(5, 2.5, 19, 15.5, 200, 140));
        } else if (kind == 1) {
            p.add(Part.shape(PATH, false, pts(4, 8, 4, 17, 6.5, 20.5, 13.5, 20.5, 16, 17, 16, 8, 4, 8)));
            p.add(Part.arc(13.5, 9.5, 20.5, 16.5, 270, 180));
            if (r.nextDouble() < 0.6) {
                p.add(Part.shape(PATH, false, pts(7.5, 5.5, 8.3, 4.3, 7.5, 3)));
                p.add(Part.shape(PATH, false, pts(10.5, 5.5, 11.3, 4.3, 10.5, 3)));
            }
        } else if (kind == 2) {
            p.add(new Part(RECT, 4.5, 8, 19.5, 21, false, 1.5));
            p.add(Part.arc(8.5, 3.5, 15.5, 12.5, 180, 180));
        } else {
            p.add(Part.shape(PATH, false, pts(5, 10, 6, 19, 8, 21, 16, 21, 18, 19, 19, 10)));
            p.add(Part.line(3.5, 10, 20.5, 10));
            p.add(Part.line(10, 7.5, 14, 7.5));
        }
    }

    /** Things held to play: a pad, a stick on its base. */
    private static void pad(Random r, List<Part> p) {
        if (r.nextInt(3) < 2) {
            p.add(new Part(RECT, 2.5, 7.5, 21.5, 17.5, false, 5));
            p.add(new Part(PLUS, 5.5, 10.5, 9.5, 14.5, false, 0));
            p.add(new Part(DISC, 14.8, 9.8, 16.8, 11.8, true, 0));
            p.add(new Part(DISC, 17, 12.4, 19, 14.4, true, 0));
        } else {
            p.add(new Part(RECT, 5, 16, 19, 21, false, 2));
            p.add(Part.line(12, 16, 12, 8.5));
            p.add(new Part(DISC, 9.3, 3.3, 14.7, 8.7, true, 0));
            if (r.nextDouble() < 0.6) {
                p.add(new Part(DISC, 15.5, 17.5, 17.5, 19.5, true, 0));
            }
        }
    }

    /** A window: its frame and its panes, square or arched. */
    private static void frame(Random r, List<Part> p) {
        int kind = pick(r, 0, 0, 1, 2);
        if (kind == 2) {
            p.add(Part.line(5, 21, 5, 10));
            p.add(Part.line(19, 21, 19, 10));
            p.add(Part.arc(5, 3, 19, 17, 180, 180));
            p.add(Part.line(5, 21, 19, 21));
            p.add(Part.line(12, 4, 12, 20));
            p.add(Part.line(6, 13, 18, 13));
            return;
        }
        p.add(new Part(RECT, 3.5, 3.5, 20.5, 20.5, false, 1.5));
        if (kind == 0) {
            p.add(Part.line(12, 4.5, 12, 19.5));
            p.add(Part.line(4.5, 12, 19.5, 12));
        } else {
            p.add(Part.line(9.2, 4.5, 9.2, 19.5));
            p.add(Part.line(14.8, 4.5, 14.8, 19.5));
            p.add(Part.line(4.5, 12, 19.5, 12));
        }
    }

    /** Two lines and the ties across them: a ladder, a track going away. */
    private static void rails(Random r, List<Part> p) {
        if (r.nextInt(3) == 0) {
            p.add(Part.line(7.5, 3, 7.5, 21));
            p.add(Part.line(16.5, 3, 16.5, 21));
            for (double y = 6.5; y < 20; y += 4) {
                p.add(Part.line(7.5, y, 16.5, y));
            }
        } else {
            p.add(Part.line(10.5, 3, 5, 21));
            p.add(Part.line(13.5, 3, 19, 21));
            double[] ts = {0.12, 0.35, 0.6, 0.9};
            for (double t : ts) {
                double y = 3 + 18 * t;
                double half = 2.7 + 7 * t;
                p.add(Part.line(12 - half, y, 12 + half, y));
            }
        }
    }

    /** Crosses: the tall one, the healers', the leaning one in a ring, the one with two bars. */
    private static void crossed(Random r, List<Part> p) {
        int kind = r.nextInt(4);
        if (kind == 0) {
            p.add(Part.line(12, 3, 12, 21));
            p.add(Part.line(6.5, 8.5, 17.5, 8.5));
        } else if (kind == 1) {
            double a = 8.5;
            double b = 15.5;
            p.add(Part.shape(POLY, r.nextDouble() < 0.4, pts(a, 3.5, b, 3.5, b, a, 20.5, a, 20.5, b, b, b,
                b, 20.5, a, 20.5, a, b, 3.5, b, 3.5, a, a, a)));
        } else if (kind == 2) {
            p.add(new Part(CROSS, 6.5, 6.5, 17.5, 17.5, false, 0));
            p.add(new Part(RING, 3, 3, 21, 21, false, 0));
        } else {
            p.add(Part.line(12, 3, 12, 21));
            p.add(Part.line(7.5, 7.5, 16.5, 7.5));
            p.add(Part.line(5.5, 12, 18.5, 12));
        }
    }

    /** A whirl: a spiral from the middle out. */
    private static void whirl(Random r, List<Part> p) {
        double turns = one(r, 2.2, 2.6, 3.0);
        List<double[]> line = new ArrayList<>();
        for (int k = 0; k < 160; k++) {
            double t = k / 159.0;
            line.add(turn(12, 12, 1 + 8.8 * t, t * turns * 2 * Math.PI));
        }
        p.add(Part.shape(PATH, false, line));
    }

    // ------------------------------------------------------------ beings

    private static final String[] BODIES = {"cloud", "circle", "heart", "drop", "gem", "peak", "box"};
    private static final int[] BODY_WEIGHTS = {5, 3, 2, 1, 1, 1, 2};

    /** What may grow on each body, at each point, with how likely; null is nothing there. */
    private static Object[][] rules(String body, String at) {
        switch (body + "." + at) {
            case "cloud.top": return new Object[][] {{"cat_ears", 3}, {"round_ears", 1}, {"long_ears", 1},
                {"horns", 1}, {null, 5}};
            case "cloud.below": return new Object[][] {{"trunk", 4}, {"legs4", 3}, {"legs2", 2}, {"rain", 2},
                {"bolt", 2}, {null, 2}};
            case "cloud.side": return new Object[][] {{"tail", 3}, {"beak", 2}, {"elephant", 1}, {"speech", 2},
                {"head", 3}, {null, 4}};
            case "cloud.within": return new Object[][] {{"eyes", 3}, {"eye", 1}, {null, 5}};
            case "circle.top": return new Object[][] {{"cat_ears", 2}, {"round_ears", 2}, {"feelers", 1},
                {"horns", 1}, {null, 4}};
            case "circle.below": return new Object[][] {{"string", 3}, {"legs2", 1}, {null, 4}};
            case "circle.side": return new Object[][] {{"beak", 2}, {"tail", 1}, {"handle", 1}, {"speech", 1},
                {null, 4}};
            case "circle.within": return new Object[][] {{"eyes", 3}, {"beat", 1}, {"eye", 1}, {null, 3}};
            case "heart.top": return new Object[][] {{"wings", 2}, {null, 5}};
            case "heart.side": return new Object[][] {{"arrow", 2}, {null, 4}};
            case "heart.within": return new Object[][] {{"beat", 3}, {"plus", 1}, {null, 3}};
            case "drop.within": return new Object[][] {{"eyes", 2}, {"shine", 2}, {null, 2}};
            case "gem.top": return new Object[][] {{"spark", 1}, {null, 3}};
            case "gem.within": return new Object[][] {{"facets", 5}, {null, 1}};
            case "peak.top": return new Object[][] {{"flag", 2}, {null, 4}};
            case "peak.below": return new Object[][] {{"trunk", 3}, {null, 3}};
            case "peak.within": return new Object[][] {{"eye", 2}, {"snow", 2}, {null, 3}};
            case "box.top": return new Object[][] {{"steam", 3}, {"lid", 1}, {null, 3}};
            case "box.below": return new Object[][] {{"legs4", 1}, {null, 5}};
            case "box.side": return new Object[][] {{"handle", 4}, {"speech", 1}, {null, 3}};
            case "box.within": return new Object[][] {{"eyes", 1}, {"beat", 1}, {null, 4}};
            default: return new Object[][] {{null, 1}};
        }
    }

    private static String weighted(Random r, Object[][] choices) {
        int total = 0;
        for (Object[] c : choices) {
            total += (Integer) c[1];
        }
        int pick = r.nextInt(total);
        for (Object[] c : choices) {
            int w = (Integer) c[1];
            if (pick < w) {
                return (String) c[0];
            }
            pick -= w;
        }
        return null;
    }

    private static boolean is(String what, String... any) {
        for (String one : any) {
            if (one.equals(what)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A body, and what grows on it at its points: above, below, at a side,
     * within. What grows joins the body's outline, so a cloud with ears and
     * a tail is one creature. The rules keep it from being a mixer: weather
     * is weather; a tree or a balloon wears nothing else; four legs carry no
     * ears on their back and want a head half the time; a beat keeps ears
     * away; two things outside at most, one within.
     */
    private void being(Random r) {
        int pick = r.nextInt(15);
        String body = "cloud";
        for (int i = 0; i < BODIES.length; i++) {
            if (pick < BODY_WEIGHTS[i]) {
                body = BODIES[i];
                break;
            }
            pick -= BODY_WEIGHTS[i];
        }
        String top = weighted(r, rules(body, "top"));
        String below = weighted(r, rules(body, "below"));
        String side = weighted(r, rules(body, "side"));
        String within = weighted(r, rules(body, "within"));
        if (is(below, "rain", "bolt", "trunk", "string")) {
            top = null;
            side = null;
            if (!"string".equals(below)) {
                within = null;
            }
        }
        if (is(below, "legs4", "legs2") && is(top, "cat_ears", "long_ears", "round_ears", "feelers")) {
            top = null;
        }
        if ("beat".equals(within)) {
            top = null;
        }
        if ("legs4".equals(below) && side == null && "cloud".equals(body) && r.nextDouble() < 0.5) {
            side = "head";
        }
        if ("head".equals(side)) {
            top = null;
            within = null;
        }
        if ("speech".equals(side)) {
            top = null;
            below = null;
        }
        if (top != null && below != null && side != null) {
            side = null;
        }
        solid = r.nextDouble() < 0.3;

        /* Room left for what grows on it. */
        double y0 = 3.5 + (is(top, "cat_ears", "long_ears", "horns", "feelers", "wings", "steam", "flag", "spark",
            "round_ears") ? 4.5 : 0) + ("long_ears".equals(top) ? 2 : 0);
        double y1 = 20.5 - (is(below, "trunk", "legs4", "legs2", "rain", "bolt", "string") ? 6.5 : 0);
        double x0 = 3.5 + (is(side, "beak", "elephant") ? 3 : "head".equals(side) ? 5 : 0);
        double x1 = 20.5 - (is(side, "tail", "handle", "arrow", "speech") ? 4 : 0);
        if (is(body, "circle", "drop", "gem")) {
            double sz = Math.min(x1 - x0, y1 - y0);
            double cx = (x0 + x1) / 2;
            double cy = (y0 + y1) / 2;
            double half = "drop".equals(body) ? sz * 0.4 : sz / 2;
            x0 = cx - half;
            x1 = cx + half;
            y0 = cy - sz / 2;
            y1 = cy + sz / 2;
        }
        double w = x1 - x0;
        double h = y1 - y0;
        double cx = (x0 + x1) / 2;
        Path shape = new Path();
        switch (body) {
            case "cloud": {
                double base = y1 - h * 0.28;
                oval(shape, x0 + w * 0.26, base - h * 0.08, h * 0.36, h * 0.36);
                oval(shape, cx, y0 + h * 0.42, h * 0.44, h * 0.44);
                oval(shape, x1 - w * 0.26, base - h * 0.06, h * 0.34, h * 0.34);
                Path floor = new Path();
                floor.addRoundRect(new RectF((float) (x0 + w * 0.08), (float) (base - h * 0.1), (float) (x1 - w * 0.08),
                    (float) y1), (float) (h * 0.26), (float) (h * 0.26), Path.Direction.CW);
                shape.op(floor, Path.Op.UNION);
                break;
            }
            case "circle":
                oval(shape, cx, (y0 + y1) / 2, w / 2, h / 2);
                break;
            case "heart": {
                List<double[]> round = new ArrayList<>();
                for (int k = 0; k < 64; k++) {
                    double t = 2 * Math.PI * k / 64;
                    double x = 16 * Math.pow(Math.sin(t), 3);
                    double y = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));
                    round.add(new double[] {x0 + (x + 17) / 34 * w, y0 + (y + 17.5) / 30 * h});
                }
                add(shape, round);
                break;
            }
            case "drop": {
                double rad = w / 2;
                double cy = y1 - rad;
                oval(shape, cx, cy, rad, rad);
                double a = Math.asin(Math.min(1, rad / Math.max(0.1, cy - y0)));
                add(shape, pts(cx, y0, cx - rad * Math.cos(a), cy - rad * Math.sin(a), cx + rad * Math.cos(a),
                    cy - rad * Math.sin(a)));
                break;
            }
            case "gem": {
                double t = y0 + h * 0.3;
                add(shape, pts(x0 + 3, y0, x1 - 3, y0, x1, t, cx, y1, x0, t));
                break;
            }
            case "peak":
                add(shape, pts(cx, y0, x1, y1, x0, y1));
                break;
            default: {
                Path box = new Path();
                box.addRoundRect(new RectF((float) x0, (float) y0, (float) x1, (float) y1), 2f, 2f,
                    Path.Direction.CW);
                shape.op(box, Path.Op.UNION);
                break;
            }
        }

        /* Above. */
        if ("cat_ears".equals(top)) {
            for (int s = -1; s <= 1; s += 2) {
                double bx = cx + s * w * 0.28;
                add(shape, pts(bx - 3, y0 + 2.5, bx + 3, y0 + 2.5, bx + s * 1.2, y0 - 4));
            }
        } else if ("round_ears".equals(top)) {
            for (int s = -1; s <= 1; s += 2) {
                oval(shape, cx + s * w * 0.34, y0 + 0.6, 2.8, 2.8);
            }
        } else if ("long_ears".equals(top)) {
            for (int s = -1; s <= 1; s += 2) {
                oval(shape, cx + s * 2.6, y0 - 2.25, 1.7, 4.25);
            }
        } else if ("horns".equals(top)) {
            for (int s = -1; s <= 1; s += 2) {
                double bx = cx + s * w * 0.22;
                parts.add(Part.shape(PATH, false, pts(bx, y0 + 1.5, bx + s * 1.5, y0 - 1.5, bx + s * 3.8, y0 - 3)));
            }
        } else if ("feelers".equals(top)) {
            for (int s = -1; s <= 1; s += 2) {
                parts.add(Part.line(cx + s * 1.5, y0 + 0.5, cx + s * 4, y0 - 3));
                parts.add(new Part(DISC, cx + s * 4 - 1.2, y0 - 4.6, cx + s * 4 + 1.2, y0 - 2.2, true, 0));
            }
        } else if ("wings".equals(top)) {
            for (int s = -1; s <= 1; s += 2) {
                double wx = cx + s * w * 0.42;
                parts.add(Part.shape(PATH, false, pts(wx, y0 + 3, wx + s * 3.5, y0 - 1.5, wx + s * 2, y0 + 1,
                    wx + s * 4.2, y0 + 1.5, wx + s * 1.4, y0 + 4.5)));
            }
        } else if ("steam".equals(top)) {
            for (int k = -1; k <= 1; k += 2) {
                double x = cx + k * 2.4;
                parts.add(Part.shape(PATH, false, pts(x, y0 - 1, x + 1, y0 - 2.3, x, y0 - 3.6, x + 1, y0 - 4.9)));
            }
        } else if ("lid".equals(top)) {
            parts.add(Part.line(x0 - 0.5, y0 - 1.5, x1 + 0.5, y0 - 1.5));
            parts.add(Part.line(cx - 1.5, y0 - 3.3, cx + 1.5, y0 - 3.3));
        } else if ("flag".equals(top)) {
            parts.add(Part.line(cx, y0 + 0.5, cx, y0 - 4.2));
            add(shape, pts(cx, y0 - 4.4, cx + 3.6, y0 - 3.4, cx, y0 - 2.3));
        } else if ("spark".equals(top)) {
            List<double[]> star = new ArrayList<>();
            for (int k = 0; k < 8; k++) {
                double a = -Math.PI / 2 + Math.PI * k / 4;
                star.add(turn(x1 + 0.5, y0 - 2, k % 2 == 0 ? 2.2 : 0.8, a));
            }
            parts.add(Part.shape(POLY, true, star));
        }

        /* Below. */
        if ("trunk".equals(below)) {
            parts.add(Part.line(cx, y1 - 0.5, cx, 21));
            if (r.nextDouble() < 0.5) {
                parts.add(Part.line(cx, y1 + 3, cx + 2.2, y1 + 1.2));
            }
        } else if ("legs2".equals(below)) {
            for (int s = -1; s <= 1; s += 2) {
                parts.add(Part.line(cx + s * 2.2, y1 - 0.5, cx + s * 2.6, 21));
            }
        } else if ("legs4".equals(below)) {
            for (double k = 0.2; k < 0.9; k += 0.2) {
                parts.add(Part.line(x0 + w * k, y1 - 0.5, x0 + w * k, 21));
            }
        } else if ("string".equals(below)) {
            parts.add(Part.shape(PATH, false, pts(cx, y1, cx - 1, y1 + 2.2, cx + 0.8, y1 + 4.2, cx, 21)));
        } else if ("rain".equals(below)) {
            double[] xs = {cx - 4, cx, cx + 4};
            for (int k = 0; k < 3; k++) {
                parts.add(Part.line(xs[k] + 0.8, y1 + 1.6 + (k % 2) * 1.2, xs[k] - 0.8, y1 + 4.4 + (k % 2) * 1.2));
            }
        } else if ("bolt".equals(below)) {
            parts.add(Part.shape(PATH, false, pts(cx + 1, y1 - 0.5, cx - 1.6, y1 + 3, cx + 1.4, y1 + 3, cx - 1.2, 21)));
        }

        /* At a side. */
        if ("tail".equals(side)) {
            parts.add(Part.shape(PATH, false, pts(x1 - 0.8, y1 - 2.5, x1 + 2.5, y1 - 3.5, x1 + 3.4, y1 - 6.5,
                x1 + 2.2, y1 - 8.5)));
        } else if ("beak".equals(side)) {
            double by = y0 + h * 0.42;
            add(shape, pts(x0 + 1, by - 2, x0 - 3.2, by, x0 + 1, by + 2));
        } else if ("elephant".equals(side)) {
            double by = y0 + h * 0.45;
            parts.add(Part.shape(PATH, false, pts(x0 + 0.5, by, x0 - 2, by + 2, x0 - 2.5, by + 5, x0 - 1, by + 6.5)));
        } else if ("speech".equals(side)) {
            add(shape, pts(x0 + 3, y1 - 1, x0 + 7, y1 - 1, x0 + 1.5, y1 + 3.2));
        } else if ("head".equals(side)) {
            double hx = x0 - 1.2;
            double hy = y0 + h * 0.3;
            oval(shape, hx - 0.4, hy + 0.1, 3.0, 3.1);
            oval(shape, hx + 1.5, hy - 2.8, 1.1, 1.4);
            holes.add(new Part(DISC, hx - 2.1, hy - 1.1, hx - 0.3, hy + 0.7, true, 0));
        } else if ("handle".equals(side)) {
            double hy = (y0 + y1) / 2;
            parts.add(Part.arc(x1 - 3.5, hy - 3.5, x1 + 3.5, hy + 3.5, 270, 180));
        } else if ("arrow".equals(side)) {
            parts.add(Part.line(x0 - 1.5, y1 - 0.5, x1 + 2.5, y0 + 0.5));
            parts.add(Part.line(x1 + 2.5, y0 + 0.5, x1 - 0.4, y0 + 0.9));
            parts.add(Part.line(x1 + 2.5, y0 + 0.5, x1 + 2.1, y0 + 3.4));
        }

        /* Within. */
        double iy = y0 + h * 0.5;
        if ("eyes".equals(within)) {
            for (int s = -1; s <= 1; s += 2) {
                double ex = cx + s * w * 0.17;
                holes.add(new Part(DISC, ex - 1.25, iy - 1.25, ex + 1.25, iy + 1.25, true, 0));
            }
        } else if ("eye".equals(within)) {
            double ey = iy + ("peak".equals(body) ? 1.2 : 0);
            holes.add(new Part(DISC, cx - 1.7, ey - 1.7, cx + 1.7, ey + 1.7, true, 0));
        } else if ("beat".equals(within)) {
            double a = x0 + w * 0.18;
            double span = w * 0.64;
            holes.add(Part.shape(PATH, false, pts(a, iy, a + span * 0.3, iy, a + span * 0.42, iy - 3,
                a + span * 0.56, iy + 3, a + span * 0.68, iy, a + span, iy)));
        } else if ("plus".equals(within)) {
            holes.add(new Part(PLUS, cx - 2.6, iy - 3.4, cx + 2.6, iy + 1.8, false, 0));
        } else if ("shine".equals(within)) {
            double rad = w / 2 - 2.3;
            double cy = y1 - w / 2;
            holes.add(Part.arc(cx - rad, cy - rad, cx + rad, cy + rad, 110, 60));
        } else if ("facets".equals(within)) {
            double t = y0 + h * 0.3;
            holes.add(Part.line(x0 + 0.5, t, x1 - 0.5, t));
            holes.add(Part.shape(PATH, false, pts(x0 + 3, y0 + 0.4, cx - 1.8, t, cx, y1 - 0.8, cx + 1.8, t,
                x1 - 3, y0 + 0.4)));
        } else if ("snow".equals(within)) {
            holes.add(Part.shape(PATH, false, pts(cx - 2.6, y0 + 5.2, cx - 1, y0 + 6.4, cx + 0.6, y0 + 5.2,
                cx + 2.4, y0 + 6.4)));
        }
        silhouette = shape;
    }

    private static void oval(Path into, double cx, double cy, double rx, double ry) {
        Path one = new Path();
        one.addOval(new RectF((float) (cx - rx), (float) (cy - ry), (float) (cx + rx), (float) (cy + ry)),
            Path.Direction.CW);
        into.op(one, Path.Op.UNION);
    }

    private static void add(Path into, List<double[]> at) {
        Path one = new Path();
        one.moveTo((float) at.get(0)[0], (float) at.get(0)[1]);
        for (int i = 1; i < at.size(); i++) {
            one.lineTo((float) at.get(i)[0], (float) at.get(i)[1]);
        }
        one.close();
        into.op(one, Path.Op.UNION);
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float u = Math.min(b.width(), b.height()) / 24f;
        canvas.save();
        canvas.translate(b.exactCenterX() - 12f * u, b.exactCenterY() - 12f * u);
        paint.setColor(colour);
        paint.setStrokeWidth(2f * u);
        int layer = -1;
        if (silhouette != null) {
            if (solid) {
                /* Cut marks need a layer of their own to cut through. */
                layer = canvas.saveLayer(0, 0, 24f * u, 24f * u, null);
            }
            Path scaled = new Path(silhouette);
            android.graphics.Matrix grow = new android.graphics.Matrix();
            grow.setScale(u, u);
            scaled.transform(grow);
            paint.setStyle(solid ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
            canvas.drawPath(scaled, paint);
        }
        drawParts(canvas, parts, u, paint);
        if (!holes.isEmpty()) {
            Paint cut = new Paint(paint);
            cut.setStrokeWidth(1.6f * u);
            if (solid) {
                cut.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
            }
            drawParts(canvas, holes, u, cut);
        }
        if (layer >= 0) {
            canvas.restoreToCount(layer);
        }
        canvas.restore();
    }

    private static void drawParts(Canvas canvas, List<Part> parts, float u, Paint paint) {
        Path path = new Path();
        RectF box = new RectF();
        for (Part part : parts) {
            box.set(part.x0 * u, part.y0 * u, part.x1 * u, part.y1 * u);
            paint.setStyle(part.filled ? Paint.Style.FILL : Paint.Style.STROKE);
            switch (part.kind) {
                case RECT:
                    canvas.drawRoundRect(box, part.extra * u, part.extra * u, paint);
                    break;
                case LEAN:
                    canvas.save();
                    canvas.rotate(16f, box.centerX(), box.bottom);
                    canvas.drawRoundRect(box, part.extra * u, part.extra * u, paint);
                    canvas.restore();
                    break;
                case RING:
                case DISC:
                    paint.setStyle(part.kind == DISC || part.filled ? Paint.Style.FILL : Paint.Style.STROKE);
                    canvas.drawOval(box, paint);
                    break;
                case LINE:
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(part.points[0] * u, part.points[1] * u, part.points[2] * u, part.points[3] * u,
                        paint);
                    break;
                case ARC:
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawArc(box, part.extra, part.sweep, false, paint);
                    break;
                case PLUS:
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(box.left, box.centerY(), box.right, box.centerY(), paint);
                    canvas.drawLine(box.centerX(), box.top, box.centerX(), box.bottom, paint);
                    break;
                case CROSS:
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(box.left, box.top, box.right, box.bottom, paint);
                    canvas.drawLine(box.left, box.bottom, box.right, box.top, paint);
                    break;
                case CHECK:
                    paint.setStyle(Paint.Style.STROKE);
                    path.reset();
                    path.moveTo(box.left, box.centerY());
                    path.lineTo(box.left + box.width() * 0.38f, box.bottom);
                    path.lineTo(box.right, box.top);
                    canvas.drawPath(path, paint);
                    break;
                default:
                    path.reset();
                    path.moveTo(part.points[0] * u, part.points[1] * u);
                    for (int i = 2; i + 1 < part.points.length; i += 2) {
                        path.lineTo(part.points[i] * u, part.points[i + 1] * u);
                    }
                    if (part.kind == POLY) {
                        path.close();
                    }
                    paint.setStyle(part.kind == POLY && part.filled ? Paint.Style.FILL : Paint.Style.STROKE);
                    canvas.drawPath(path, paint);
                    break;
            }
        }
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (tint != null) {
            colour = tint.getDefaultColor();
            invalidateSelf();
        }
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

    @Override
    public int getIntrinsicWidth() {
        return 96;
    }

    @Override
    public int getIntrinsicHeight() {
        return 96;
    }
}
