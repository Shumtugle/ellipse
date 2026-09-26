package io.github.shumtugle.ellipse;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * An open folder: a card of the menu's own make, grown out of the folder's
 * icon, with the folder's name small at its head and what it holds four
 * to a row under it; a long folder scrolls. A press opens an application
 * and closes the folder; a long press lifts it out, to be set down on a
 * screen. A touch anywhere else, or Back, sends the card back into the
 * icon it came from.
 */
final class Tray extends FrameLayout {

    interface Hand {
        void open(View from, Apps.Door door, int[] icon);

        void lift(View from, Apps.Door door, int[] icon);

        /** The widgets the folder holds, made and sized, above its apps; none if it holds none. */
        List<View> widgets();

        /** A widget in the folder held: its menu, and the same finger may carry it out. */
        void hold(View widget);
    }

    private static final int COLUMNS = 4;

    private final View veil;
    private final LinearLayout card;
    private final float density;
    private final float scaled;
    private final float iconSize;
    private final Hand hand;
    private boolean shown;

    Tray(Context context, float iconSize, Hand hand) {
        super(context);
        this.iconSize = iconSize;
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibility(GONE);

        veil = new View(context);
        veil.setBackgroundColor(0x4D000000);
        veil.setClickable(true);
        veil.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close(true);
            }
        });
        addView(veil, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        card.setElevation(dp(6));
        addView(card, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.START));
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    boolean shown() {
        return shown;
    }

    /** Opens, grown out of a point of the floor: the centre of the folder's icon. */
    void show(CharSequence name, List<Apps.Door> doors, float x, float y) {
        card.removeAllViews();
        card.setBackground(Tone.box(Tone.containerHigh(), dp(28), dp(0.5f)));
        card.setPadding(dp(8), dp(14), dp(8), dp(12));

        TextView caption = new TextView(getContext());
        caption.setText(name.toString().toUpperCase(Locale.getDefault()));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setTextColor(Tone.faint());
        caption.setPadding(dp(16), 0, dp(16), dp(6));
        card.addView(caption);

        /* Widgets first, as wide as the card, each at its own height; a thin line under them. */
        LinearLayout inside = new LinearLayout(getContext());
        inside.setOrientation(LinearLayout.VERTICAL);
        float widgetsTall = 0f;
        for (final View widget : hand.widgets()) {
            if (widget.getParent() instanceof ViewGroup) {
                ((ViewGroup) widget.getParent()).removeView(widget);
            }
            ViewGroup.LayoutParams wanted = widget.getLayoutParams();
            int tall = wanted != null && wanted.height > 0 ? wanted.height : dp(160);
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tall);
            at.setMargins(dp(8), dp(4), dp(8), dp(8));
            widget.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    hand.hold(widget);
                    return true;
                }
            });
            inside.addView(widget, at);
            widgetsTall += tall + dp(12);
        }
        if (widgetsTall > 0f && !doors.isEmpty()) {
            View line = new View(getContext());
            line.setBackgroundColor(Tone.faint() & 0x55FFFFFF);
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(0.5f)));
            at.setMargins(dp(24), 0, dp(24), dp(10));
            inside.addView(line, at);
            widgetsTall += dp(10) + Math.max(1, dp(0.5f));
        }
        int rows = doors.isEmpty() ? 0 : Math.max(1, (doors.size() + COLUMNS - 1) / COLUMNS);
        Grid grid = new Grid(getContext(), COLUMNS, Math.max(1, rows));
        for (int i = 0; i < doors.size(); i++) {
            final Cell cell = new Cell(getContext(), doors.get(i), iconSize, true).onGround();
            cell.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    hand.open(cell, cell.door, cell.localIcon());
                }
            });
            cell.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    hand.lift(cell, cell.door, cell.localIcon());
                    return true;
                }
            });
            grid.put(cell, i % COLUMNS, i / COLUMNS);
        }
        float cellTall = iconSize + dp(44);
        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        if (rows > 0) {
            inside.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(cellTall * rows)));
        }
        float full = widgetsTall + cellTall * rows;
        scroll.addView(inside, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        float room = getHeight() * 0.62f;
        card.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            Math.round(Math.min(room, full))));

        shown = true;
        setVisibility(VISIBLE);
        float edge = dp(12);
        float w = getWidth() - getPaddingLeft() - getPaddingRight() - 2f * edge;
        LayoutParams params = (LayoutParams) card.getLayoutParams();
        params.width = Math.round(w);
        card.setLayoutParams(params);
        card.measure(MeasureSpec.makeMeasureSpec(Math.round(w), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        float h = card.getMeasuredHeight();
        float left = getPaddingLeft() + edge;
        /* Above the icon in the lower half of the screen, below it in the upper. */
        float top = y > getHeight() / 2f ? y - iconSize / 2f - dp(12) - h : y + iconSize / 2f + dp(12);
        top = Math.max(getPaddingTop() + edge, Math.min(getHeight() - getPaddingBottom() - edge - h, top));
        card.setTranslationX(left - getPaddingLeft());
        card.setTranslationY(top - getPaddingTop());
        card.setPivotX(x - left);
        card.setPivotY(y - top);
        card.animate().cancel();
        card.setAlpha(0f);
        card.setScaleX(0.15f);
        card.setScaleY(0.15f);
        veil.setAlpha(0f);
        veil.animate().alpha(1f).setDuration(Pace.ARRIVE).start();
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
    }

    /** Back into the icon it came from; at once, when nobody is looking. */
    void close(boolean slowly) {
        if (!shown) {
            return;
        }
        shown = false;
        card.animate().cancel();
        veil.animate().cancel();
        if (!slowly) {
            setVisibility(GONE);
            return;
        }
        veil.animate().alpha(0f).setDuration(Pace.ARRIVE / 2).start();
        card.animate().alpha(0f).scaleX(0.15f).scaleY(0.15f).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                    }
                }
            }).start();
    }
}
