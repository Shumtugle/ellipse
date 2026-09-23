package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Every application, by name, one to a line, on plain black. No title: a
 * list of every application says what it is by being one. It rises from
 * the bar that opened it and sinks back into it.
 */
final class Drawer extends FrameLayout {

    interface Opener {
        void open(Row row);
    }

    private final ListView list;
    private final Lines lines = new Lines();
    private final float iconSize;
    private List<Apps.Door> doors = new ArrayList<>();
    private boolean shown;

    Drawer(Context context, float iconSize, final Opener opener) {
        super(context);
        this.iconSize = iconSize;
        setBackgroundColor(Color.BLACK);
        setVisibility(GONE);
        /* Touches that fall between the lines stay here and never reach the
           screen underneath. */
        setClickable(true);

        list = new ListView(context);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(OVER_SCROLL_NEVER);
        list.setClipToPadding(false);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setAdapter(lines);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int at, long id) {
                opener.open((Row) view);
            }
        });
        addView(list, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /** Keeps the first and last lines clear of the phone's bars; the rest scroll under them. */
    void inset(int top, int bottom) {
        float density = getResources().getDisplayMetrics().density;
        list.setPadding(0, top + Math.round(12f * density), 0, bottom + Math.round(12f * density));
    }

    void fill(List<Apps.Door> doors) {
        this.doors = doors;
        lines.notifyDataSetChanged();
    }

    void tint() {
        for (int i = 0; i < list.getChildCount(); i++) {
            ((Row) list.getChildAt(i)).tint();
        }
    }

    boolean shown() {
        return shown;
    }

    /** Rises from the foot of the screen, always from the top of the alphabet. */
    void rise() {
        if (shown) {
            return;
        }
        shown = true;
        list.setSelection(0);
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(getResources().getDisplayMetrics().density * 72f);
        animate().cancel();
        animate().alpha(1f).translationY(0f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
    }

    /** Sinks back where it came from; at once, when nobody is looking. */
    void sink(boolean slowly) {
        if (!shown) {
            return;
        }
        shown = false;
        animate().cancel();
        if (!slowly) {
            setVisibility(GONE);
            return;
        }
        animate().alpha(0f).translationY(getResources().getDisplayMetrics().density * 48f)
            .setDuration(Pace.ARRIVE / 2).setInterpolator(Pace.EMPHASIS)
            .withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                    }
                }
            }).start();
    }

    private final class Lines extends BaseAdapter {
        public int getCount() {
            return doors.size();
        }

        public Object getItem(int at) {
            return doors.get(at);
        }

        public long getItemId(int at) {
            return at;
        }

        public View getView(int at, View reused, ViewGroup parent) {
            Row row = reused instanceof Row ? (Row) reused : new Row(getContext(), iconSize);
            row.show(doors.get(at));
            return row;
        }
    }
}
