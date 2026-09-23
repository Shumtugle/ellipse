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
 * What is fresh: the applications last opened from here, and those put on
 * the phone or changed lately, lowered from the top over the home screen.
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

    private static final String RECENT = "Recent";
    private static final String NEW = "New";
    private static final String EMPTY = "Nothing opened or installed lately.";
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

    private void grid(List<Apps.Door> doors, List<String> notes) {
        int rows = (doors.size() + COLUMNS - 1) / COLUMNS;
        Grid grid = new Grid(getContext(), COLUMNS, rows);
        for (int i = 0; i < doors.size(); i++) {
            final Cell cell = new Cell(getContext(), doors.get(i), iconSize, true);
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
        sheet.addView(grid, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.round(cell * rows)));
    }

    /** Lowers itself from the top with what was opened and what is fresh. */
    void show(List<Apps.Door> recent, List<Apps.Door> lately, List<String> notes) {
        sheet.removeAllViews();
        GradientDrawable ground = new GradientDrawable();
        float r = dp(28);
        ground.setColor(Tone.containerHigh());
        ground.setCornerRadii(new float[] {0f, 0f, 0f, 0f, r, r, r, r});
        sheet.setBackground(ground);
        sheet.setPadding(dp(8), top + dp(4), dp(8), dp(10));
        if (!recent.isEmpty()) {
            sheet.addView(caption(RECENT));
            grid(recent, null);
        }
        if (!lately.isEmpty()) {
            sheet.addView(caption(NEW));
            grid(lately, notes);
        }
        if (recent.isEmpty() && lately.isEmpty()) {
            TextView none = new TextView(getContext());
            none.setText(EMPTY);
            none.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
            none.setTextColor(Tone.faint());
            none.setPadding(dp(16), dp(24), dp(16), dp(24));
            sheet.addView(none);
        }
        /* A short grip at the foot: the sheet is pushed back up from anywhere. */
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
