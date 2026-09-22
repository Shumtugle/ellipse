package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Small pictures that say what a choice does, drawn in two inks: a strong
 * one for what the choice is about and a soft one for what surrounds it.
 *
 * A shape is its own silhouette. The drawer's two ways are a screen of
 * tiles running on downwards and three screens lying side by side. The
 * two ways of a widget are a screen with a block inside its margins and
 * one with the block running to the glass. A rim is a disc of its metal.
 * Plus and minus are two strokes and one. Screens that stop at the ends
 * have a wall after the last; screens that go round have an arrow over
 * them coming back. An order is what it goes by: letters, a new star, a
 * turning arrow. A home screen is a small screen of tiles.
 */
final class Sketch extends View {

    static final int SHAPE = 0;
    static final int DOWN = 1;
    static final int ACROSS = 2;
    static final int INSIDE = 3;
    static final int EDGE = 4;
    static final int RIM = 5;
    static final int MINUS = 6;
    static final int PLUS = 7;
    static final int ENDS = 8;
    static final int ROUND = 9;
    static final int NAME = 10;
    static final int NEWEST = 11;
    static final int UPDATED = 12;
    static final int SCREEN = 13;
    static final int BARS = 14;
    static final int INFO = 15;
    static final int REMOVE = 16;
    static final int DRAWER = 17;
    static final int WIDGET = 18;
    static final int HOUSE = 19;
    static final int GEAR = 20;
    static final int FOLDER_OPEN = 21;
    static final int SEARCH = 22;
    static final int CLOSE = 23;
    static final int SCREENS = 24;
    static final int WINDOW_FOLLOWS = 25;
    static final int WINDOW_ROUND = 26;
    static final int MATTE = 27;
    static final int GLOSS = 28;
    static final int CLOCK = 29;
    static final int PIN = 30;
    static final int WINDOW_RAW = 31;
    static final int WEATHER = 32;
    static final int CALENDAR = 33;
    static final int BATTERY = 34;
    static final int HEADPHONES = 35;
    static final int BACK = 36;
    static final int LANGUAGE = 37;
    static final int PALETTE = 38;
    static final int FORWARD = 39;
    static final int DOOR = 40;

    private Paint metal;
    private Paint shine;

    private final int kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float power = Tile.Look.MEASURED.power;
    private float ratio = Tile.Look.MEASURED.ratio;
    private int rim;
    private int bars;
    private int face;
    private int ink = 0xFFFFFFFF;
    private int soft = 0x66FFFFFF;

    Sketch(Context context, int kind) {
        super(context);
        this.kind = kind;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    Sketch shape(float power, float ratio) {
        this.power = power;
        this.ratio = ratio;
        invalidate();
        return this;
    }

    /** Which of the system's bars the screen keeps: none put away, the top, the bottom, or both. */
    Sketch bars(int mode) {
        bars = mode;
        invalidate();
        return this;
    }

    /** Which face of the door to draw. */
    Sketch door(int face) {
        this.face = face;
        invalidate();
        return this;
    }

    Sketch rim(int kind) {
        rim = kind;
        invalidate();
        return this;
    }

    void ink(int strong, int quiet) {
        ink = strong;
        soft = quiet;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float line = Round.px(2f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        switch (kind) {
            case DOOR: {
                float side = Math.min(w, h);
                if (Door.whole(face)) {
                    Door.whole(canvas, paint, face, w / 2f, h / 2f, side);
                } else if (face == Door.GLASS) {
                    float wide = side * 0.9f;
                    float tall = wide / Tile.Look.MEASURED.ratio;
                    stroke(soft, line);
                    canvas.drawPath(Tile.curve((w - wide) / 2f, (h - tall) / 2f, wide, tall,
                        Tile.Look.MEASURED.power), paint);
                } else {
                    Door.mark(canvas, paint, face, w / 2f, h / 2f, side * 0.92f, ink, soft);
                }
                break;
            }
            case SHAPE: {
                float wide = Math.min(w * 0.86f, h * 0.86f * ratio);
                float tall = wide / ratio;
                fill(ink);
                canvas.drawPath(Tile.curve((w - wide) / 2f, (h - tall) / 2f, wide, tall, power), paint);
                break;
            }
            case DOWN: {
                screen(canvas, w / 2f, h / 2f, h * 0.9f);
                float sw = h * 0.9f * 0.56f;
                float cell = sw / 4.2f;
                float left = w / 2f - cell * 1.5f - cell * 0.2f;
                float top = h / 2f - h * 0.9f / 2f + cell * 0.7f;
                for (int r = 0; r < 5; r++) {
                    for (int c = 0; c < 3; c++) {
                        fill(r < 3 ? ink : soft);
                        float x = left + c * cell * 1.2f;
                        float y = top + r * cell * 1.2f;
                        box.set(x, y, x + cell, y + cell * 0.8f);
                        canvas.drawRoundRect(box, cell * 0.25f, cell * 0.25f, paint);
                    }
                }
                break;
            }
            case ACROSS: {
                float tall = h * 0.8f;
                float sw = tall * 0.56f;
                for (int i = -1; i <= 1; i++) {
                    float cx = w / 2f + i * (sw + Round.px(6f));
                    stroke(i == 0 ? ink : soft, line);
                    box.set(cx - sw / 2f, h / 2f - tall / 2f, cx + sw / 2f, h / 2f + tall / 2f);
                    canvas.drawRoundRect(box, sw * 0.18f, sw * 0.18f, paint);
                    fill(i == 0 ? ink : soft);
                    float cell = sw / 4f;
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 2; c++) {
                            float x = cx - cell * 1.1f + c * cell * 1.2f;
                            float y = h / 2f - tall / 2f + cell * 0.9f + r * cell * 1.15f;
                            box.set(x, y, x + cell, y + cell * 0.8f);
                            canvas.drawRoundRect(box, cell * 0.25f, cell * 0.25f, paint);
                        }
                    }
                }
                break;
            }
            case INSIDE:
            case EDGE: {
                float tall = h * 0.9f;
                float sw = tall * 0.56f;
                float left = w / 2f - sw / 2f;
                float top = h / 2f - tall / 2f;
                float margin = kind == INSIDE ? sw * 0.14f : 0f;
                fill(ink);
                box.set(left + margin, top + margin, left + sw - margin, top + tall * 0.4f);
                float r = kind == INSIDE ? sw * 0.1f : sw * 0.18f;
                canvas.drawRoundRect(box, r, r, paint);
                screen(canvas, w / 2f, h / 2f, tall);
                fill(soft);
                float cell = sw / 5f;
                for (int row = 0; row < 3; row++) {
                    for (int c = 0; c < 3; c++) {
                        float x = left + sw * 0.16f + c * cell * 1.35f;
                        float y = top + tall * 0.52f + row * cell * 1.2f;
                        box.set(x, y, x + cell, y + cell * 0.8f);
                        canvas.drawRoundRect(box, cell * 0.25f, cell * 0.25f, paint);
                    }
                }
                break;
            }
            case RIM: {
                float radius = Math.min(w, h) * 0.42f;
                if (rim == Tile.Look.BARE) {
                    stroke(soft, line);
                    canvas.drawCircle(w / 2f, h / 2f, radius - line, paint);
                    stroke(soft, line);
                    canvas.drawLine(w / 2f - radius * 0.5f, h / 2f + radius * 0.5f,
                        w / 2f + radius * 0.5f, h / 2f - radius * 0.5f, paint);
                } else {
                    coin(canvas, w / 2f, h / 2f, radius);
                }
                break;
            }
            case ENDS:
            case ROUND: {
                float tall = h * 0.56f;
                float sw = tall * 0.56f;
                float gap = Round.px(4f);
                float cy = h * 0.62f;
                for (int i = -1; i <= 1; i++) {
                    float cx = w / 2f + i * (sw + gap);
                    fill(i == 0 ? ink : soft);
                    box.set(cx - sw / 2f, cy - tall / 2f, cx + sw / 2f, cy + tall / 2f);
                    canvas.drawRoundRect(box, sw * 0.2f, sw * 0.2f, paint);
                }
                float right = w / 2f + 1.5f * sw + gap;
                float left = w / 2f - 1.5f * sw - gap;
                if (kind == ENDS) {
                    stroke(ink, Round.px(3f));
                    canvas.drawLine(right + gap * 1.5f, cy - tall / 2f - gap, right + gap * 1.5f,
                        cy + tall / 2f + gap, paint);
                } else {
                    stroke(ink, Round.px(2.5f));
                    float top = cy - tall / 2f - Round.px(6f);
                    box.set(left + sw * 0.3f, top - h * 0.18f, right - sw * 0.3f, top + h * 0.18f);
                    canvas.drawArc(box, 200f, 140f, false, paint);
                    float ax = box.left + box.width() * 0.08f;
                    float ay = box.centerY() - box.height() * 0.2f;
                    canvas.drawLine(ax, ay, ax + Round.px(7f), ay - Round.px(2f), paint);
                    canvas.drawLine(ax, ay, ax + Round.px(2f), ay - Round.px(7f), paint);
                }
                break;
            }
            case NAME: {
                fill(ink);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(h * 0.5f);
                paint.setTypeface(android.graphics.Typeface.SERIF);
                Paint.FontMetrics m = paint.getFontMetrics();
                canvas.drawText("Aa", w / 2f, h / 2f - (m.ascent + m.descent) / 2f, paint);
                break;
            }
            case NEWEST: {
                float r = Math.min(w, h) * 0.4f;
                float cx = w / 2f;
                float cy = h / 2f;
                android.graphics.Path star = new android.graphics.Path();
                star.moveTo(cx, cy - r);
                star.quadTo(cx, cy, cx + r, cy);
                star.quadTo(cx, cy, cx, cy + r);
                star.quadTo(cx, cy, cx - r, cy);
                star.quadTo(cx, cy, cx, cy - r);
                star.close();
                fill(ink);
                canvas.drawPath(star, paint);
                break;
            }
            case UPDATED: {
                float r = Math.min(w, h) * 0.32f;
                stroke(ink, Round.px(3f));
                box.set(w / 2f - r, h / 2f - r, w / 2f + r, h / 2f + r);
                canvas.drawArc(box, -60f, 290f, false, paint);
                double end = Math.toRadians(-60f);
                float ex = (float) (w / 2f + r * Math.cos(end));
                float ey = (float) (h / 2f + r * Math.sin(end));
                canvas.drawLine(ex, ey, ex - Round.px(8f), ey - Round.px(1f), paint);
                canvas.drawLine(ex, ey, ex + Round.px(1f), ey + Round.px(8f), paint);
                break;
            }
            case SCREEN: {
                float tall = h * 0.9f;
                screen(canvas, w / 2f, h / 2f, tall);
                float sw = tall * 0.56f;
                float left = w / 2f - sw / 2f;
                float top = h / 2f - tall / 2f;
                float cell = sw / 5f;
                for (int row = 0; row < 4; row++) {
                    for (int c = 0; c < 3; c++) {
                        fill(ink);
                        float x = left + sw * 0.16f + c * cell * 1.35f;
                        float y = top + tall * 0.2f + row * cell * 1.3f;
                        box.set(x, y, x + cell, y + cell * 0.8f);
                        canvas.drawRoundRect(box, cell * 0.25f, cell * 0.25f, paint);
                    }
                }
                break;
            }
            case BARS: {
                float tall = h * 0.9f;
                float sw = tall * 0.56f;
                float left = w / 2f - sw / 2f;
                float top = h / 2f - tall / 2f;
                boolean status = bars == Keep.BARS || bars == Keep.NO_NAVIGATION;
                boolean navigation = bars == Keep.BARS || bars == Keep.NO_STATUS;
                fill(soft);
                float cell = sw / 5f;
                float from = top + (status ? tall * 0.14f : tall * 0.07f);
                float to = top + tall - (navigation ? tall * 0.12f : tall * 0.05f);
                for (float y = from; y + cell * 0.8f <= to; y += cell * 1.2f) {
                    for (int c = 0; c < 3; c++) {
                        float x = left + sw * 0.16f + c * cell * 1.35f;
                        box.set(x, y, x + cell, y + cell * 0.8f);
                        canvas.drawRoundRect(box, cell * 0.25f, cell * 0.25f, paint);
                    }
                }
                fill(ink);
                if (status) {
                    box.set(left + sw * 0.08f, top + tall * 0.03f, left + sw * 0.92f, top + tall * 0.09f);
                    canvas.drawRoundRect(box, sw * 0.03f, sw * 0.03f, paint);
                }
                if (navigation) {
                    box.set(left + sw * 0.3f, top + tall * 0.93f, left + sw * 0.7f, top + tall * 0.955f);
                    canvas.drawRoundRect(box, sw * 0.02f, sw * 0.02f, paint);
                }
                screen(canvas, w / 2f, h / 2f, tall);
                break;
            }
            case INFO: {
                float r = Math.min(w, h) * 0.42f;
                stroke(ink, Round.px(2f));
                canvas.drawCircle(w / 2f, h / 2f, r, paint);
                fill(ink);
                canvas.drawCircle(w / 2f, h / 2f - r * 0.45f, Round.px(1.6f), paint);
                stroke(ink, Round.px(2.2f));
                canvas.drawLine(w / 2f, h / 2f - r * 0.1f, w / 2f, h / 2f + r * 0.5f, paint);
                break;
            }
            case REMOVE: {
                float r = Math.min(w, h) * 0.42f;
                stroke(ink, Round.px(2f));
                canvas.drawCircle(w / 2f, h / 2f, r, paint);
                stroke(ink, Round.px(2.2f));
                canvas.drawLine(w / 2f - r * 0.45f, h / 2f, w / 2f + r * 0.45f, h / 2f, paint);
                break;
            }
            case DRAWER: {
                fill(ink);
                float step = Math.min(w, h) * 0.3f;
                float dot = Math.min(w, h) * 0.09f;
                for (int r = -1; r <= 1; r++) {
                    for (int c = -1; c <= 1; c++) {
                        canvas.drawCircle(w / 2f + c * step, h / 2f + r * step, dot, paint);
                    }
                }
                break;
            }
            case WIDGET: {
                float s = Math.min(w, h);
                fill(ink);
                box.set(w / 2f - s * 0.42f, h / 2f - s * 0.38f, w / 2f + s * 0.42f, h / 2f + s * 0.02f);
                canvas.drawRoundRect(box, s * 0.1f, s * 0.1f, paint);
                box.set(w / 2f - s * 0.42f, h / 2f + s * 0.12f, w / 2f - s * 0.04f, h / 2f + s * 0.4f);
                canvas.drawRoundRect(box, s * 0.08f, s * 0.08f, paint);
                box.set(w / 2f + s * 0.04f, h / 2f + s * 0.12f, w / 2f + s * 0.42f, h / 2f + s * 0.4f);
                canvas.drawRoundRect(box, s * 0.08f, s * 0.08f, paint);
                break;
            }
            case HOUSE: {
                float s = Math.min(w, h);
                android.graphics.Path roof = new android.graphics.Path();
                roof.moveTo(w / 2f - s * 0.38f, h / 2f - s * 0.02f);
                roof.lineTo(w / 2f, h / 2f - s * 0.38f);
                roof.lineTo(w / 2f + s * 0.38f, h / 2f - s * 0.02f);
                roof.lineTo(w / 2f + s * 0.38f, h / 2f + s * 0.38f);
                roof.lineTo(w / 2f - s * 0.38f, h / 2f + s * 0.38f);
                roof.close();
                stroke(ink, Round.px(2f));
                canvas.drawPath(roof, paint);
                fill(ink);
                box.set(w / 2f - s * 0.1f, h / 2f + s * 0.1f, w / 2f + s * 0.1f, h / 2f + s * 0.38f);
                canvas.drawRect(box, paint);
                break;
            }
            case GEAR: {
                float s = Math.min(w, h);
                float outer = s * 0.44f;
                float inner = s * 0.32f;
                android.graphics.Path gear = new android.graphics.Path();
                int teeth = 8;
                for (int i = 0; i < teeth * 2; i++) {
                    double a0 = Math.PI * 2 * (i - 0.35) / (teeth * 2);
                    double a1 = Math.PI * 2 * (i + 0.35) / (teeth * 2);
                    float r = i % 2 == 0 ? outer : inner;
                    float x0 = (float) (w / 2f + r * Math.cos(a0));
                    float y0 = (float) (h / 2f + r * Math.sin(a0));
                    float x1 = (float) (w / 2f + r * Math.cos(a1));
                    float y1 = (float) (h / 2f + r * Math.sin(a1));
                    if (i == 0) {
                        gear.moveTo(x0, y0);
                    } else {
                        gear.lineTo(x0, y0);
                    }
                    gear.lineTo(x1, y1);
                }
                gear.close();
                gear.addCircle(w / 2f, h / 2f, s * 0.14f, android.graphics.Path.Direction.CCW);
                gear.setFillType(android.graphics.Path.FillType.EVEN_ODD);
                fill(ink);
                canvas.drawPath(gear, paint);
                break;
            }
            case FOLDER_OPEN: {
                float s = Math.min(w, h);
                fill(ink);
                float cell = s * 0.34f;
                float gap = s * 0.08f;
                for (int r = 0; r < 2; r++) {
                    for (int c = 0; c < 2; c++) {
                        float x = w / 2f - cell - gap / 2f + c * (cell + gap);
                        float y = h / 2f - cell - gap / 2f + r * (cell + gap);
                        box.set(x, y, x + cell, y + cell);
                        canvas.drawRoundRect(box, cell * 0.28f, cell * 0.28f, paint);
                    }
                }
                break;
            }
            case SEARCH: {
                float s = Math.min(w, h);
                stroke(ink, Round.px(2.2f));
                canvas.drawCircle(w / 2f - s * 0.08f, h / 2f - s * 0.08f, s * 0.28f, paint);
                canvas.drawLine(w / 2f + s * 0.13f, h / 2f + s * 0.13f, w / 2f + s * 0.38f,
                    h / 2f + s * 0.38f, paint);
                break;
            }
            case CLOSE: {
                float arm = Math.min(w, h) * 0.18f;
                stroke(ink, Round.px(2.2f));
                canvas.drawLine(w / 2f - arm, h / 2f - arm, w / 2f + arm, h / 2f + arm, paint);
                canvas.drawLine(w / 2f - arm, h / 2f + arm, w / 2f + arm, h / 2f - arm, paint);
                break;
            }
            case SCREENS: {
                float s = Math.min(w, h);
                stroke(ink, Round.px(2f));
                for (int i = 0; i < 3; i++) {
                    float y = h / 2f - s * 0.3f + i * s * 0.3f;
                    box.set(w / 2f - s * 0.38f, y - s * 0.1f, w / 2f + s * 0.38f, y + s * 0.1f);
                    canvas.drawRoundRect(box, s * 0.06f, s * 0.06f, paint);
                }
                break;
            }
            case WINDOW_FOLLOWS:
            case WINDOW_ROUND:
            case MATTE:
            case GLOSS: {
                float wide = Math.min(w * 0.86f, h * 0.86f * 1.267f);
                float tall = wide / 1.267f;
                float left = (w - wide) / 2f;
                float top = (h - tall) / 2f;
                fill(soft);
                android.graphics.Path plate = Tile.curve(left, top, wide, tall, 8f);
                canvas.drawPath(plate, paint);
                fill(ink);
                if (kind == WINDOW_ROUND) {
                    float d = tall * 0.7f;
                    canvas.drawCircle(w / 2f, h / 2f, d / 2f, paint);
                } else {
                    float inset = wide * 0.08f;
                    canvas.drawPath(Tile.curve(left + inset, top + inset, wide - 2 * inset, tall - 2 * inset, 9.6f), paint);
                }
                if (kind == GLOSS) {
                    android.graphics.Path glaze = new android.graphics.Path();
                    glaze.addOval(new RectF(left - wide * 0.45f, top - tall * 1.05f, left + wide * 1.45f,
                        top + tall * 0.48f), android.graphics.Path.Direction.CW);
                    glaze.op(plate, android.graphics.Path.Op.INTERSECT);
                    fill(0x66FFFFFF);
                    canvas.drawPath(glaze, paint);
                }
                break;
            }
            case WINDOW_RAW: {
                // An icon as its application draws it: no plate, no window, only itself.
                float r = Math.min(w, h) * 0.34f;
                fill(ink);
                canvas.drawCircle(w / 2f, h / 2f, r, paint);
                fill(soft);
                canvas.drawCircle(w / 2f + r * 0.22f, h / 2f - r * 0.22f, r * 0.42f, paint);
                break;
            }
            case WEATHER: {
                Sky.draw(canvas, new Sky.Now(true, 0f, 2, true), w / 2f, h / 2f, Math.min(w, h) * 0.95f, ink);
                break;
            }
            case CALENDAR: {
                float s = Math.min(w, h);
                stroke(ink, Round.px(2f));
                box.set(w / 2f - s * 0.36f, h / 2f - s * 0.3f, w / 2f + s * 0.36f, h / 2f + s * 0.36f);
                canvas.drawRoundRect(box, s * 0.08f, s * 0.08f, paint);
                canvas.drawLine(box.left, box.top + s * 0.18f, box.right, box.top + s * 0.18f, paint);
                canvas.drawLine(w / 2f - s * 0.18f, box.top - s * 0.1f, w / 2f - s * 0.18f, box.top + s * 0.06f, paint);
                canvas.drawLine(w / 2f + s * 0.18f, box.top - s * 0.1f, w / 2f + s * 0.18f, box.top + s * 0.06f, paint);
                fill(ink);
                canvas.drawCircle(w / 2f, box.top + s * 0.4f, s * 0.06f, paint);
                break;
            }
            case BATTERY: {
                float s = Math.min(w, h);
                stroke(ink, Round.px(2f));
                box.set(w / 2f - s * 0.2f, h / 2f - s * 0.32f, w / 2f + s * 0.2f, h / 2f + s * 0.4f);
                canvas.drawRoundRect(box, s * 0.07f, s * 0.07f, paint);
                fill(ink);
                canvas.drawRect(w / 2f - s * 0.08f, box.top - s * 0.08f, w / 2f + s * 0.08f, box.top, paint);
                canvas.drawRect(box.left + s * 0.08f, box.top + s * 0.3f, box.right - s * 0.08f, box.bottom - s * 0.08f, paint);
                break;
            }
            case HEADPHONES: {
                float s = Math.min(w, h);
                stroke(ink, Round.px(2.2f));
                box.set(w / 2f - s * 0.34f, h / 2f - s * 0.36f, w / 2f + s * 0.34f, h / 2f + s * 0.32f);
                canvas.drawArc(box, 180f, 180f, false, paint);
                fill(ink);
                box.set(w / 2f - s * 0.4f, h / 2f, w / 2f - s * 0.2f, h / 2f + s * 0.34f);
                canvas.drawRoundRect(box, s * 0.06f, s * 0.06f, paint);
                box.set(w / 2f + s * 0.2f, h / 2f, w / 2f + s * 0.4f, h / 2f + s * 0.34f);
                canvas.drawRoundRect(box, s * 0.06f, s * 0.06f, paint);
                break;
            }
            case BACK:
            case FORWARD: {
                float s = Math.min(w, h);
                float dir = kind == BACK ? 1f : -1f;
                stroke(ink, Round.px(2.2f));
                canvas.drawLine(w / 2f - dir * s * 0.1f, h / 2f, w / 2f + dir * s * 0.14f, h / 2f - s * 0.24f, paint);
                canvas.drawLine(w / 2f - dir * s * 0.1f, h / 2f, w / 2f + dir * s * 0.14f, h / 2f + s * 0.24f, paint);
                if (kind == BACK) {
                    canvas.drawLine(w / 2f - s * 0.1f, h / 2f, w / 2f + s * 0.34f, h / 2f, paint);
                }
                break;
            }
            case LANGUAGE: {
                float s = Math.min(w, h);
                fill(ink);
                paint.setTextAlign(android.graphics.Paint.Align.CENTER);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SERIF,
                    android.graphics.Typeface.BOLD));
                paint.setTextSize(s * 0.62f);
                canvas.drawText("Aa", w / 2f, h / 2f + s * 0.22f, paint);
                paint.setTypeface(null);
                break;
            }
            case PALETTE: {
                float s = Math.min(w, h);
                float r = s * 0.17f;
                int[] spots = {0xFFE3AE6E, 0xFFB86A5A, 0xFF6F8FB0, 0xFF7FA27A};
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 4 + i * Math.PI / 2;
                    fill(spots[i]);
                    canvas.drawCircle(w / 2f + (float) Math.cos(a) * s * 0.2f,
                        h / 2f + (float) Math.sin(a) * s * 0.2f, r, paint);
                }
                break;
            }
            case PIN: {
                float s = Math.min(w, h);
                android.graphics.Path pin = new android.graphics.Path();
                float cx = w / 2f;
                float top = h / 2f - s * 0.4f;
                float r = s * 0.24f;
                pin.addCircle(cx, top + r, r, android.graphics.Path.Direction.CW);
                android.graphics.Path point = new android.graphics.Path();
                point.moveTo(cx - r * 0.86f, top + r * 1.5f);
                point.lineTo(cx, h / 2f + s * 0.42f);
                point.lineTo(cx + r * 0.86f, top + r * 1.5f);
                point.close();
                pin.op(point, android.graphics.Path.Op.UNION);
                android.graphics.Path hole = new android.graphics.Path();
                hole.addCircle(cx, top + r, r * 0.4f, android.graphics.Path.Direction.CW);
                pin.op(hole, android.graphics.Path.Op.DIFFERENCE);
                fill(ink);
                canvas.drawPath(pin, paint);
                break;
            }
            case CLOCK: {
                float r = Math.min(w, h) * 0.42f;
                stroke(ink, Round.px(2f));
                canvas.drawCircle(w / 2f, h / 2f, r, paint);
                stroke(ink, Round.px(2.2f));
                canvas.drawLine(w / 2f, h / 2f, w / 2f, h / 2f - r * 0.6f, paint);
                canvas.drawLine(w / 2f, h / 2f, w / 2f + r * 0.45f, h / 2f + r * 0.2f, paint);
                break;
            }
            case MINUS:
            case PLUS: {
                float arm = Math.min(w, h) * 0.22f;
                stroke(ink, Round.px(2.5f));
                canvas.drawLine(w / 2f - arm, h / 2f, w / 2f + arm, h / 2f, paint);
                if (kind == PLUS) {
                    canvas.drawLine(w / 2f, h / 2f - arm, w / 2f, h / 2f + arm, paint);
                }
                break;
            }
            default:
                break;
        }
    }

    /** The outline of a phone standing upright, centred on a point. */
    /**
     * A rim is shown as a coin of its own material, cut from the same
     * stuff the tiles are and lit the same way: the plate, the bevel, the
     * line of its edge and the glaze across its top. Bling and steel show
     * their grain; black, its thin bright ring.
     */
    private void coin(Canvas canvas, float cx, float cy, float radius) {
        if (metal == null) {
            metal = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        int side = Math.max(1, Math.round(radius * 2f));
        android.graphics.Path disc = new android.graphics.Path();
        disc.addCircle(radius, radius, radius, android.graphics.Path.Direction.CW);
        canvas.save();
        canvas.translate(cx - radius, cy - radius);
        metal.setShader(null);
        Tile.material(metal, rim, side, side);
        canvas.drawPath(disc, metal);
        shine.setShader(Cast.bevel(0f, side));
        canvas.drawPath(disc, shine);
        if (rim == Tile.Look.BLACK) {
            stroke(0x55FFFFFF, Round.px(1.2f));
            canvas.drawCircle(radius, radius, radius - Round.px(1.5f), paint);
        }
        shine.setShader(Cast.glazeLight(0f, side));
        canvas.drawPath(Cast.glaze(disc, 0f, 0f, side, side), shine);
        stroke(0x8C000000, Math.max(1f, Round.px(0.8f)));
        canvas.drawCircle(radius, radius, radius - Round.px(0.4f), paint);
        canvas.restore();
    }

    private void screen(Canvas canvas, float cx, float cy, float tall) {
        float sw = tall * 0.56f;
        stroke(soft, Round.px(2f));
        box.set(cx - sw / 2f, cy - tall / 2f, cx + sw / 2f, cy + tall / 2f);
        canvas.drawRoundRect(box, sw * 0.18f, sw * 0.18f, paint);
    }

    private void fill(int colour) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colour);
    }

    private void stroke(int colour, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(colour);
    }
}
