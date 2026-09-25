package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * What is fresh: the applications opened most from here, those last
 * opened, and those put on the phone or changed lately, each under a tab
 * of its own, lowered from the top over the home screen; it opens on the
 * tab it was last left on.
 *
 * Every icon keeps its name; a fresh one says under it whether it is new
 * or updated, and an application signed with the same key as this home
 * screen says its version too, so a build just put on the phone can be
 * seen to be the one that was meant.
 *
 * It is pushed back up by the finger, from anywhere on it: a throw upward
 * or a pull past a quarter of its height sends it away; less, and it
 * settles back. A touch on the screen below it sends it away too. A press
 * opens an application; a long press lifts it, to be set down on a screen.
 */
final class Fresh extends FrameLayout {

    interface Hand {
        void open(View from, Apps.Door door, int[] icon);

        void lift(View from, Apps.Door door, int[] icon);
    }

    private static final String MOST = "Frequent";
    private static final String RECENT = "Recent";
    private static final String NEW = "New and updated";
    private static final int COLUMNS = 4;

    private final View veil;
    private final LinearLayout sheet;
    private final float density;
    private final float scaled;
    private final float iconSize;
    private final Hand hand;
    private int top;
    private boolean shown;

    Fresh(Context context, float iconSize, Hand hand) {
        super(context);
        this.iconSize = iconSize;
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibility(GONE);

        veil = new View(context);
        veil.setBackgroundColor(0x66000000);
        veil.setClickable(true);
        veil.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close(true);
            }
        });
        addView(veil, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        sheet = new LinearLayout(context);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setClickable(true);
        sheet.setElevation(dp(6));
        addView(sheet, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
            Gravity.TOP));
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    /** Keeps the first row clear of the status bar. */
    void inset(int top) {
        this.top = top;
    }

    boolean shown() {
        return shown;
    }

    private TextView caption(String text) {
        TextView made = new TextView(getContext());
        made.setText(text.toUpperCase(Locale.ROOT));
        made.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        made.setLetterSpacing(0.12f);
        made.setTextColor(Tone.faint());
        made.setPadding(dp(16), dp(12), dp(16), dp(4));
        return made;
    }

    private void grid(List<Apps.Door> doors, List<String> notes, LinearLayout into) {
        int rows = (doors.size() + COLUMNS - 1) / COLUMNS;
        Grid grid = new Grid(getContext(), COLUMNS, rows);
        for (int i = 0; i < doors.size(); i++) {
            final Cell cell = new Cell(getContext(), doors.get(i), iconSize, true).onGround();
            if (notes != null) {
                cell.note(notes.get(i));
            }
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
        float cell = iconSize + dp(notes == null ? 44 : 58);
        into.addView(grid, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.round(cell * rows)));
    }

    /** The three lists, as last lowered, for the tabs to turn between. */
    private List<Apps.Door> most = new java.util.ArrayList<>();
    private List<Apps.Door> recent = new java.util.ArrayList<>();
    private List<Apps.Door> lately = new java.util.ArrayList<>();
    private List<String> notes = new java.util.ArrayList<>();
    private LinearLayout body;

    /** Lowers itself from the top: the most opened, the last opened and the fresh, under their tabs. */
    void show(List<Apps.Door> most, List<Apps.Door> recent, List<Apps.Door> lately, List<String> notes) {
        this.most = most;
        this.recent = recent;
        this.lately = lately;
        this.notes = notes;
        sheet.removeAllViews();
        GradientDrawable ground = new GradientDrawable();
        float r = dp(28);
        ground.setColor(Tone.containerHigh());
        ground.setCornerRadii(new float[] {0f, 0f, 0f, 0f, r, r, r, r});
        sheet.setBackground(ground);
        sheet.setPadding(dp(8), top + dp(4), dp(8), dp(10));
        int tab = Keep.number(getContext(), Keep.FRESH_TAB, 2);
        final LinearLayout tabs = new LinearLayout(getContext());
        String[] names = {MOST, RECENT, NEW};
        /* The words made smaller together, if need be, so all three fit whole. */
        float width = getResources().getDisplayMetrics().widthPixels - dp(16);
        float size = Tabs.fit(getContext(), names, 14f * scaled, width);
        for (int i = 0; i < names.length; i++) {
            final int which = i;
            TextView one = Tabs.tab(getContext(), names[i], size, Tone.onSurface(), Tone.faint(), false);
            one.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    Keep.saveNumber(getContext(), Keep.FRESH_TAB, which);
                    choose(tabs, which);
                }
            });
            tabs.addView(one, Tabs.share(one));
        }
        sheet.addView(tabs);
        body = new LinearLayout(getContext());
        body.setOrientation(LinearLayout.VERTICAL);
        sheet.addView(body);
        choose(tabs, Math.max(0, Math.min(2, tab)));
        lower();
    }

    /** A tab chosen: its word in the accent and underlined, its list below. */
    private void choose(LinearLayout tabs, int which) {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            Tabs.mark((TextView) tabs.getChildAt(i), i == which, Tone.onSurface(), Tone.faint());
        }
        body.removeAllViews();
        List<Apps.Door> doors = which == 0 ? most : which == 1 ? recent : lately;
        if (doors.isEmpty()) {
            TextView none = new TextView(getContext());
            none.setText(which == 0 ? "Nothing opened from here yet." : which == 1 ? "Nothing opened lately."
                : "Nothing installed or updated lately.");
            none.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
            none.setTextColor(Tone.faint());
            none.setPadding(dp(16), dp(24), dp(16), dp(24));
            body.addView(none);
            return;
        }
        grid(doors, which == 2 ? notes : null, body);
    }

    /** A short grip at the foot, the sheet being pushed back up from anywhere; then the sheet lowered. */
    private void lower() {
        View grip = new View(getContext());
        grip.setBackground(Tone.box(Tone.faint(), dp(2), 0f));
        LinearLayout.LayoutParams gripParams = new LinearLayout.LayoutParams(dp(32), dp(4));
        gripParams.gravity = Gravity.CENTER_HORIZONTAL;
        gripParams.topMargin = dp(10);
        sheet.addView(grip, gripParams);

        shown = true;
        setVisibility(VISIBLE);
        sheet.measure(MeasureSpec.makeMeasureSpec(Math.max(1, getWidth()), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        float tall = sheet.getMeasuredHeight();
        sheet.animate().cancel();
        sheet.setTranslationY(-tall);
        veil.setAlpha(0f);
        sheet.animate().translationY(0f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).start();
        veil.animate().alpha(1f).setDuration(Pace.ARRIVE).start();
        for (int i = 0; i < sheet.getChildCount(); i++) {
            View part = sheet.getChildAt(i);
            part.setAlpha(0f);
            part.animate().alpha(1f).setStartDelay(Pace.STEP * (i + 1))
                .setDuration(Pace.ARRIVE).start();
        }
    }

    /** How open it stands while pushed, from nought, gone above, to one. */
    void drag(float open) {
        open = open < 0f ? 0f : (open > 1f ? 1f : open);
        sheet.animate().cancel();
        veil.animate().cancel();
        sheet.setTranslationY(-(1f - open) * sheet.getHeight());
        veil.setAlpha(open);
    }

    /** Let go after a push: away past a quarter or on a throw, back otherwise. */
    void let(boolean away) {
        if (away) {
            close(true);
            return;
        }
        sheet.animate().translationY(0f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).start();
        veil.animate().alpha(1f).setDuration(Pace.ARRIVE).start();
    }

    int sheetHeight() {
        return sheet.getHeight();
    }

    /** Back up and away; at once, when nobody is looking. */
    void close(boolean slowly) {
        if (!shown) {
            return;
        }
        shown = false;
        sheet.animate().cancel();
        veil.animate().cancel();
        if (!slowly) {
            setVisibility(GONE);
            return;
        }
        veil.animate().alpha(0f).setDuration(Pace.ARRIVE / 2).start();
        sheet.animate().translationY(-sheet.getHeight()).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                    }
                }
            }).start();
    }
}
