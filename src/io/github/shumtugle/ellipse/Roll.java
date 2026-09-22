package io.github.shumtugle.ellipse;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A list on a sheet drawn up from below: a title, and rows of a name with
 * a smaller line under it. A press on a row does its deed and closes the
 * sheet; a long press does its other deed, when it has one; a touch above
 * the sheet closes it.
 */
final class Roll extends FrameLayout {

    interface Hand {
        void closed();
    }

    private final LinearLayout sheet;
    private final LinearLayout list;
    private final Hand hand;
    private boolean closing;

    Roll(Context context, String title, String note, int top, int bottom, Hand hand) {
        super(context);
        this.hand = hand;
        setBackgroundColor(0x66000000);
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
            }
        });
        sheet = new LinearLayout(context);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setClickable(true);
        sheet.setBackground(Round.sheet(Tone.of(Tone.SURFACE_CONTAINER), Round.XL));
        sheet.setPadding(Round.dp(16f), Round.dp(12f), Round.dp(16f), 0);

        View grip = new View(context);
        grip.setBackground(Round.box(Tone.of(Tone.ON_SURFACE_VARIANT, 0.5f), Round.FULL));
        LinearLayout.LayoutParams gripPlace = new LinearLayout.LayoutParams(Round.dp(32f), Round.dp(4f));
        gripPlace.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(grip, gripPlace);

        TextView name = Letter.set(new TextView(context), Letter.HEADLINE_M);
        Letter.serif(name);
        name.setText(title);
        name.setTextColor(Tone.of(Tone.ON_SURFACE));
        name.setPadding(Round.dp(8f), Round.dp(16f), Round.dp(8f), Round.dp(4f));
        sheet.addView(name);
        if (note != null) {
            TextView under = Letter.set(new TextView(context), Letter.BODY_M);
            under.setText(note);
            under.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
            under.setPadding(Round.dp(8f), 0, Round.dp(8f), Round.dp(12f));
            sheet.addView(under);
        }

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, bottom + Round.dp(24f));
        scroll.addView(list);
        sheet.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LayoutParams place = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM);
        place.topMargin = top + Round.dp(48f);
        addView(sheet, place);
    }

    Roll row(String title, String small, final Runnable deed, final Runnable held) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(Round.dp(16f), Round.dp(12f), Round.dp(16f), Round.dp(12f));
        row.setBackground(Round.touch(Round.box(Tone.of(Tone.SURFACE_HIGH), 20f),
            Tone.of(Tone.ON_SURFACE), 20f));
        TextView big = Letter.set(new TextView(getContext()), Letter.TITLE_M);
        big.setText(title);
        big.setTextColor(Tone.of(Tone.ON_SURFACE));
        big.setSingleLine(true);
        big.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(big);
        if (small != null) {
            TextView under = Letter.set(new TextView(getContext()), Letter.BODY_S);
            under.setText(small);
            under.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
            under.setSingleLine(true);
            under.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            row.addView(under);
        }
        row.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
                deed.run();
            }
        });
        if (held != null) {
            row.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    close();
                    held.run();
                    return true;
                }
            });
        }
        LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        place.topMargin = list.getChildCount() == 0 ? 0 : Round.dp(8f);
        list.addView(row, place);
        return this;
    }

    void show() {
        setAlpha(0f);
        animate().alpha(1f).setDuration(Pace.SHEET).setInterpolator(Pace.STANDARD).start();
        sheet.post(new Runnable() {
            public void run() {
                sheet.setTranslationY(sheet.getHeight());
                sheet.animate().translationY(0f).setDuration(Pace.ARRIVE)
                    .setInterpolator(Pace.STANDARD).start();
            }
        });
    }

    void close() {
        if (closing || getParent() == null) {
            return;
        }
        closing = true;
        ((FrameLayout) getParent()).removeView(this);
        hand.closed();
    }
}
