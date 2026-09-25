package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A window onto the home screen, at the head of a settings page: the
 * wallpaper itself shows through a rounded hole in the ground, and on it
 * stand the same things the home screen is made of, drawn by the same
 * classes — the clock, a row of the owner's own apps with their names,
 * the dock, and a menu open over them. Whatever the page changes, the
 * window shows at once, before anything is kept.
 */
final class Glimpse extends FrameLayout {

    private final Paint ground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final RectF hole = new RectF();
    private final float density;
    private final List<Apps.Door> doors = new ArrayList<>();
    private int zoom = 100;

    Glimpse(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        Apps found = new Apps(context);
        for (String token : Keep.recent(context)) {
            Apps.Door door = found.door(token);
            if (door != null && doors.size() < 8) {
                doors.add(door);
            }
        }
        for (Apps.Door door : found.all(Keep.BY_NAME)) {
            if (doors.size() >= 8) {
                break;
            }
            boolean had = false;
            for (Apps.Door one : doors) {
                had |= one.name.equals(door.name);
            }
            if (!had) {
                doors.add(door);
            }
        }
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    /** Stands everything anew in the colours and the size of words as they are now. */
    void show(int zoom) {
        this.zoom = zoom;
        removeAllViews();
        ground.setColor(Tone.frame());
        Context base = getContext();
        Configuration shape = new Configuration(base.getResources().getConfiguration());
        shape.fontScale = shape.fontScale * zoom / 100f;
        Context sized = base.createConfigurationContext(shape);

        LinearLayout column = new LinearLayout(sized);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(14), dp(14), dp(14), dp(10));

        View clock = Home.timepiece(sized, new Almanac.Hand() {
            public void pressed(String window, View from, RectF box) {
            }
        });
        ((Timepiece) clock).weather(Keep.flag(base, Keep.WEATHER, true));
        column.addView(clock, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(96)));

        float icon = dp(44);
        Grid row = new Grid(sized, 4, 1);
        for (int i = 0; i < 4 && i < doors.size(); i++) {
            /* Drawn afresh from the app's own layers each time: an icon in the
               accent is to follow the accent under the thumb. */
            row.put(new Cell(sized, Shape.face(doors.get(i).plain()), doors.get(i).label, icon,
                Style.namesOnScreens), i, 0);
        }
        LinearLayout.LayoutParams rowAt = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            Math.round(icon + dp(40) * zoom / 100f));
        rowAt.topMargin = dp(10);
        column.addView(row, rowAt);

        LinearLayout bar = new LinearLayout(sized);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(8), dp(8));
        bar.setBackground(Tone.box(Tone.container(), dp(34), dp(0.5f)));
        Grid dock = new Grid(sized, 4, 1);
        for (int i = 4; i < 8 && i < doors.size(); i++) {
            dock.put(new Cell(sized, Shape.face(doors.get(i).plain()), doors.get(i).label, icon, false),
                i - 4, 0);
        }
        bar.addView(dock, new LinearLayout.LayoutParams(0, Math.round(icon + dp(8)), 1f));
        Blob blob = new Blob(sized, dp(48), Blob.GRID);
        blob.shaped(true);
        bar.addView(blob, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams barAt = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        barAt.topMargin = dp(8);
        column.addView(bar, barAt);
        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        /* A menu open over the row, as a long press leaves it. */
        LinearLayout card = new LinearLayout(sized);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(6), dp(6), dp(6), dp(6));
        card.setBackground(Tone.box(Tone.containerHigh(), dp(20), 0f));
        card.setElevation(dp(6));
        String[] lines = {"Add widget", "Add folder", "Settings"};
        float scaled = sized.getResources().getDisplayMetrics().scaledDensity;
        for (int i = 0; i < lines.length; i++) {
            TextView line = new TextView(sized);
            line.setText(Words.t(lines[i]));
            line.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
            line.setTextColor(i == 0 ? Tone.primary() : Tone.onSurface());
            line.setPadding(dp(12), dp(7), dp(12), dp(7));
            if (i == 0) {
                line.setBackground(Tone.box((0x2E << 24) | (Tone.primary() & 0x00FFFFFF), dp(16), 0f));
            }
            card.addView(line);
        }
        LayoutParams cardAt = new LayoutParams(dp(150), LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.TOP);
        cardAt.setMargins(0, dp(96), dp(22), 0);
        addView(card, cardAt);
        invalidate();
    }

    int zoom() {
        return zoom;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        /* The ground first, with the window cut in it; then what stands there. */
        shape.reset();
        shape.setFillType(Path.FillType.EVEN_ODD);
        shape.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        hole.set(dp(6), dp(4), getWidth() - dp(6), getHeight() - dp(4));
        shape.addRoundRect(hole, dp(30), dp(30), Path.Direction.CW);
        canvas.drawPath(shape, ground);
        super.dispatchDraw(canvas);
    }
}
