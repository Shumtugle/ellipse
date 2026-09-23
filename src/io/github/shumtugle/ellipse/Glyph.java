package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * The small line drawings of the settings, drawn by hand on a grid of
 * twenty four, in one stroke weight: a magnifier, a gear, a brush, three
 * dots in a ring, a turning arrow, an arrow turning back, a phone, a grid
 * of dots, a palette, a stroke of a finger, a box with an arrow out of
 * it, a globe, a chevron pointing down, a cross, a small i in a ring, a
 * bin, two corners pulled apart, and a pen.
 */
final class Glyph extends View {

    static final int SEARCH = 0;
    static final int GEAR = 1;
    static final int BRUSH = 2;
    static final int OTHER = 3;
    static final int RESTART = 4;
    static final int RESET = 5;
    static final int DESK = 6;
    static final int LIST = 7;
    static final int LOOK = 8;
    static final int HANDS = 9;
    static final int BACKUP = 10;
    static final int LANGUAGE = 11;
    static final int CHEVRON = 12;
    static final int CROSS = 13;
    static final int INFO = 14;
    static final int TRASH = 15;
    static final int RESIZE = 16;
    static final int PEN = 17;

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private int kind;
    private final float size;

    Glyph(Context context, int kind, float size) {
        super(context);
        this.kind = kind;
        this.size = size;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        tint(Tone.onSurface());
    }

    /** Draws another of the drawings in the same place. */
    void setKind(int kind) {
        this.kind = kind;
        invalidate();
    }

    void tint(int colour) {
        line.setColor(colour);
        fill.setColor(colour);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Math.round(size), Math.round(size));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float u = size / 24f;
        line.setStrokeWidth(2f * u);
        path.reset();
        switch (kind) {
            case SEARCH:
                canvas.drawCircle(10f * u, 10f * u, 6f * u, line);
                canvas.drawLine(14.5f * u, 14.5f * u, 20f * u, 20f * u, line);
                break;
            case GEAR:
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float x1 = 12f * u + (float) Math.cos(a) * 6.5f * u;
                    float y1 = 12f * u + (float) Math.sin(a) * 6.5f * u;
                    float x2 = 12f * u + (float) Math.cos(a) * 9f * u;
                    float y2 = 12f * u + (float) Math.sin(a) * 9f * u;
                    line.setStrokeWidth(3f * u);
                    canvas.drawLine(x1, y1, x2, y2, line);
                }
                line.setStrokeWidth(2f * u);
                canvas.drawCircle(12f * u, 12f * u, 6f * u, line);
                canvas.drawCircle(12f * u, 12f * u, 2.5f * u, line);
                break;
            case BRUSH:
                path.moveTo(19.5f * u, 4.5f * u);
                path.lineTo(10.5f * u, 13.5f * u);
                canvas.drawPath(path, line);
                path.reset();
                path.moveTo(10f * u, 14f * u);
                path.cubicTo(6f * u, 14f * u, 7f * u, 19f * u, 4f * u, 20f * u);
                path.cubicTo(8f * u, 21f * u, 12f * u, 19.5f * u, 10f * u, 14f * u);
                canvas.drawPath(path, line);
                break;
            case OTHER:
                canvas.drawCircle(12f * u, 12f * u, 9f * u, line);
                canvas.drawCircle(8f * u, 12f * u, 1.3f * u, fill);
                canvas.drawCircle(12f * u, 12f * u, 1.3f * u, fill);
                canvas.drawCircle(16f * u, 12f * u, 1.3f * u, fill);
                break;
            case RESTART:
            case RESET:
                oval.set(4.5f * u, 4.5f * u, 19.5f * u, 19.5f * u);
                boolean back = kind == RESET;
                canvas.drawArc(oval, back ? 200f : -20f, back ? -300f : 300f, false, line);
                /* The head of the arrow, at the open end of the ring. */
                double end = Math.toRadians(back ? 200f - 300f : -20f + 300f);
                float ex = 12f * u + (float) Math.cos(end) * 7.5f * u;
                float ey = 12f * u + (float) Math.sin(end) * 7.5f * u;
                double along = end + (back ? -Math.PI / 2 : Math.PI / 2);
                float tx = (float) Math.cos(along);
                float ty = (float) Math.sin(along);
                float nx = (float) Math.cos(end);
                float ny = (float) Math.sin(end);
                path.moveTo(ex + (nx * 3f - tx * 3f) * u, ey + (ny * 3f - ty * 3f) * u);
                path.lineTo(ex, ey);
                path.lineTo(ex + (-nx * 3f - tx * 3f) * u, ey + (-ny * 3f - ty * 3f) * u);
                canvas.drawPath(path, line);
                if (back) {
                    canvas.drawLine(12f * u, 8.5f * u, 12f * u, 12f * u, line);
                    canvas.drawLine(12f * u, 12f * u, 14.5f * u, 13.5f * u, line);
                }
                break;
            case DESK:
                canvas.drawRoundRect(6.5f * u, 3f * u, 17.5f * u, 21f * u, 2.5f * u, 2.5f * u, line);
                canvas.drawLine(10.5f * u, 18f * u, 13.5f * u, 18f * u, line);
                canvas.drawLine(3f * u, 8f * u, 3f * u, 16f * u, line);
                canvas.drawLine(21f * u, 8f * u, 21f * u, 16f * u, line);
                break;
            case LIST:
                canvas.drawCircle(12f * u, 12f * u, 9.5f * u, line);
                for (int r = -1; r <= 1; r++) {
                    for (int c = -1; c <= 1; c++) {
                        canvas.drawCircle(12f * u + c * 3.6f * u, 12f * u + r * 3.6f * u, 1.1f * u, fill);
                    }
                }
                break;
            case LOOK:
                path.moveTo(12f * u, 3f * u);
                path.cubicTo(5f * u, 3f * u, 3f * u, 9f * u, 3f * u, 12f * u);
                path.cubicTo(3f * u, 17f * u, 7f * u, 21f * u, 12f * u, 21f * u);
                path.cubicTo(14f * u, 21f * u, 14f * u, 18.5f * u, 13f * u, 17.5f * u);
                path.cubicTo(12f * u, 16f * u, 13f * u, 14.5f * u, 15f * u, 14.5f * u);
                path.lineTo(17f * u, 14.5f * u);
                path.cubicTo(19.5f * u, 14.5f * u, 21f * u, 13f * u, 21f * u, 10.5f * u);
                path.cubicTo(21f * u, 6f * u, 17f * u, 3f * u, 12f * u, 3f * u);
                canvas.drawPath(path, line);
                canvas.drawCircle(7.5f * u, 11.5f * u, 1.3f * u, fill);
                canvas.drawCircle(10f * u, 7.5f * u, 1.3f * u, fill);
                canvas.drawCircle(14.5f * u, 7.5f * u, 1.3f * u, fill);
                canvas.drawCircle(17f * u, 11f * u, 1.3f * u, fill);
                break;
            case HANDS:
                path.moveTo(5f * u, 19f * u);
                path.cubicTo(9f * u, 17f * u, 12f * u, 12f * u, 12f * u, 5f * u);
                canvas.drawPath(path, line);
                path.reset();
                path.moveTo(8.5f * u, 8f * u);
                path.lineTo(12f * u, 4.5f * u);
                path.lineTo(15.5f * u, 8f * u);
                canvas.drawPath(path, line);
                canvas.drawCircle(18f * u, 18f * u, 2.5f * u, line);
                break;
            case BACKUP:
                path.moveTo(4f * u, 13f * u);
                path.lineTo(4f * u, 19f * u);
                path.lineTo(20f * u, 19f * u);
                path.lineTo(20f * u, 13f * u);
                canvas.drawPath(path, line);
                path.reset();
                canvas.drawLine(12f * u, 15f * u, 12f * u, 4f * u, line);
                path.moveTo(8f * u, 8f * u);
                path.lineTo(12f * u, 4f * u);
                path.lineTo(16f * u, 8f * u);
                canvas.drawPath(path, line);
                break;
            case LANGUAGE:
                canvas.drawCircle(12f * u, 12f * u, 9f * u, line);
                oval.set(8f * u, 3f * u, 16f * u, 21f * u);
                canvas.drawOval(oval, line);
                canvas.drawLine(3f * u, 12f * u, 21f * u, 12f * u, line);
                break;
            case INFO:
                canvas.drawCircle(12f * u, 12f * u, 9f * u, line);
                canvas.drawLine(12f * u, 11f * u, 12f * u, 16.5f * u, line);
                canvas.drawCircle(12f * u, 7.8f * u, 1.3f * u, fill);
                break;
            case TRASH:
                canvas.drawLine(4.5f * u, 6.5f * u, 19.5f * u, 6.5f * u, line);
                path.moveTo(9.5f * u, 6.5f * u);
                path.lineTo(9.5f * u, 4f * u);
                path.lineTo(14.5f * u, 4f * u);
                path.lineTo(14.5f * u, 6.5f * u);
                canvas.drawPath(path, line);
                path.reset();
                path.moveTo(6.5f * u, 6.5f * u);
                path.lineTo(7.5f * u, 20f * u);
                path.lineTo(16.5f * u, 20f * u);
                path.lineTo(17.5f * u, 6.5f * u);
                canvas.drawPath(path, line);
                canvas.drawLine(10.5f * u, 10f * u, 10.5f * u, 16.5f * u, line);
                canvas.drawLine(13.5f * u, 10f * u, 13.5f * u, 16.5f * u, line);
                break;
            case RESIZE:
                canvas.drawLine(5f * u, 19f * u, 19f * u, 5f * u, line);
                path.moveTo(12f * u, 5f * u);
                path.lineTo(19f * u, 5f * u);
                path.lineTo(19f * u, 12f * u);
                path.moveTo(5f * u, 12f * u);
                path.lineTo(5f * u, 19f * u);
                path.lineTo(12f * u, 19f * u);
                canvas.drawPath(path, line);
                break;
            case PEN:
                path.moveTo(4.5f * u, 19.5f * u);
                path.lineTo(5.5f * u, 15.5f * u);
                path.lineTo(16f * u, 5f * u);
                path.lineTo(19f * u, 8f * u);
                path.lineTo(8.5f * u, 18.5f * u);
                path.close();
                canvas.drawPath(path, line);
                canvas.drawLine(13.5f * u, 7.5f * u, 16.5f * u, 10.5f * u, line);
                break;
            case CROSS:
                canvas.drawLine(6.5f * u, 6.5f * u, 17.5f * u, 17.5f * u, line);
                canvas.drawLine(17.5f * u, 6.5f * u, 6.5f * u, 17.5f * u, line);
                break;
            case CHEVRON:
                path.moveTo(6f * u, 9f * u);
                path.lineTo(12f * u, 15f * u);
                path.lineTo(18f * u, 9f * u);
                canvas.drawPath(path, line);
                break;
            default:
                break;
        }
    }
}
