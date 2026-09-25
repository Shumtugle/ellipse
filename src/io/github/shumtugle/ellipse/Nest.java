package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * A folder grown larger than one place: a card of the surface with its
 * name small at the head and what it holds laid out in it, two small
 * icons to every place across and down. A touch on one opens it straight
 * away; when the folder holds more than there is room for, the last
 * place shows how many more, and opens the whole folder. A long press
 * anywhere on it takes the folder up, to be moved or reshaped.
 */
final class Nest extends LinearLayout {

    interface Hand {
        void open(View from, Apps.Door door, int[] icon);

        void more(View from);

        void hold(View nest);
    }

    Nest(Context context, CharSequence name, List<Apps.Door> doors, int across, int down,
         float icon, final Hand hand) {
        super(context);
        float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setOrientation(VERTICAL);
        int pad = Math.round(8 * density);
        setPadding(pad, pad, pad, pad);
        GradientDrawable card = Tone.box(Tone.container(), 28 * density, 0.5f * density);
        card.setAlpha(0xE6);
        setBackground(card);

        TextView caption = new TextView(context);
        caption.setText(name.toString().toUpperCase(Locale.getDefault()));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 13f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setTextColor(Tone.faint());
        caption.setSingleLine(true);
        caption.setPadding(Math.round(10 * density), Math.round(2 * density), 0, Math.round(2 * density));
        addView(caption);

        int columns = across * 2;
        int rows = down * 2;
        Grid grid = new Grid(context, columns, rows);
        addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        int room = columns * rows;
        final View self = this;
        OnLongClickListener held = new OnLongClickListener() {
            public boolean onLongClick(View v) {
                hand.hold(self);
                return true;
            }
        };
        for (int i = 0; i < doors.size() && i < room; i++) {
            boolean last = i == room - 1 && doors.size() > room;
            if (last) {
                TextView more = new TextView(context);
                more.setText("+" + (doors.size() - room + 1));
                more.setGravity(Gravity.CENTER);
                more.setTextColor(Tone.onSurface());
                more.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
                more.setBackground(Tone.touch(Tone.box(Tone.containerHigh(), icon / 2f, 0f), icon / 2f));
                more.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        hand.more(self);
                    }
                });
                more.setOnLongClickListener(held);
                grid.put(more, i % columns, i / columns);
                continue;
            }
            final Cell cell = new Cell(context, doors.get(i), icon, false).onGround();
            cell.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    hand.open(cell, cell.door, cell.localIcon());
                }
            });
            cell.setOnLongClickListener(held);
            grid.put(cell, i % columns, i / columns);
        }
        setOnLongClickListener(held);
    }
}
