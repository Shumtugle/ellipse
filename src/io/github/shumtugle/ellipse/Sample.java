package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The owner's own icons at the head of the icons' room: one large, so the
 * cut of the outline and the fill of the picture inside it can be seen,
 * and six small, as they stand in a grid. They are drawn from the apps'
 * own layers each time an outline is chosen, so the window shows the new
 * outline at once.
 */
final class Sample extends LinearLayout {

    private final List<Apps.Door> doors = new ArrayList<>();
    private final float density;

    Sample(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        setPadding(dp(20), dp(16), dp(20), dp(16));
        Apps found = new Apps(context);
        for (String token : Keep.recent(context)) {
            Apps.Door door = found.door(token);
            if (door != null && door.serial == 0L && doors.size() < 7) {
                doors.add(door);
            }
        }
        for (Apps.Door door : found.all(Keep.BY_NAME)) {
            if (doors.size() >= 7) {
                break;
            }
            boolean had = false;
            for (Apps.Door one : doors) {
                had |= one.name.equals(door.name);
            }
            if (!had && door.serial == 0L) {
                doors.add(door);
            }
        }
        show();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    /** Draws the icons again in the outline chosen now. */
    void show() {
        removeAllViews();
        setBackground(Tone.box(Tone.container(), dp(30), dp(0.5f)));
        if (doors.isEmpty()) {
            return;
        }
        addView(icon(doors.get(0)), new LayoutParams(dp(112), dp(112)));
        LinearLayout small = new LinearLayout(getContext());
        small.setOrientation(VERTICAL);
        LayoutParams smallAt = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        smallAt.leftMargin = dp(16);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int c = 0; c < 3; c++) {
                int at = 1 + r * 3 + c;
                FrameLayout slot = new FrameLayout(getContext());
                if (at < doors.size()) {
                    slot.addView(icon(doors.get(at)), new FrameLayout.LayoutParams(dp(48), dp(48),
                        android.view.Gravity.CENTER));
                }
                row.addView(slot, new LayoutParams(0, dp(56), 1f));
            }
            small.addView(row);
        }
        addView(small, smallAt);
    }

    private ImageView icon(Apps.Door door) {
        ImageView view = new ImageView(getContext());
        Drawable face = Shape.face(door.plain());
        view.setImageDrawable(face);
        return view;
    }
}
