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
 * dots in a ring, a turning arrow, and an arrow turning back.
 */
final class Glyph extends View {

    static final int SEARCH = 0;
    static final int GEAR = 1;
    static final int BRUSH = 2;
    static final int OTHER = 3;
    static final int RESTART = 4;
    static final int RESET = 5;

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final int kind;
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
            default:
                break;
        }
    }
}
