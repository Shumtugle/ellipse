package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * An open folder: its name and its applications on a card over the home
 * screen, the screen dimmed behind it.
 *
 * A press on an application opens it. A long press lifts it out of the
 * folder, to be set down anywhere on the screens; a folder left with one
 * application becomes that application. A touch outside the card closes
 * the folder.
 */
final class Tray extends FrameLayout {

    interface Hand {
        void open(App app, View from);

        void lift(App app, View from);

        void closed();
    }

    private static final int COLUMNS = 4;

    private final LinearLayout card;
    private final Hand hand;
    private boolean closing;

    Tray(Context context, String name, List<App> apps, Icons icons, int width, Hand hand) {
        super(context);
        this.hand = hand;
        setBackgroundColor(0x66000000);
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
            }
        });

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        card.setPadding(Round.dp(12f), Round.dp(20f), Round.dp(12f), Round.dp(16f));
        // A folder lies on a field: the same ground everything of its kind
        // lies on, so one setting dresses them all.
        card.setBackground(Well.back(Well.of(context), Keep.tile(context), Round.XL));
        card.setElevation(Round.px(6f));

        TextView title = Letter.set(new TextView(context), Letter.TITLE_L);
        Letter.serif(title);
        title.setText(name);
        title.setTextColor(Tone.of(Tone.ON_SURFACE));
        title.setPadding(Round.dp(12f), 0, Round.dp(12f), Round.dp(12f));
        if (name.length() > 0) {
            card.addView(title);
        }

        int room = width - Round.dp(32f) - Round.dp(24f);
        int tile = Math.round(room / (float) COLUMNS * 0.92f);
        LinearLayout row = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = i == 0 ? 0 : Round.dp(12f);
                card.addView(row, rowParams);
            }
            final App app = apps.get(i);
            View cell = Cell.make(context, tile, icons.look());
            Cell.dress(cell, app, icons, tile);
            cell.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Tray.this.hand.open(app, v);
                }
            });
            cell.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    Tray.this.hand.lift(app, v);
                    return true;
                }
            });
            row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        for (int i = apps.size(); i % COLUMNS != 0; i++) {
            row.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1f));
        }

        LayoutParams place = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
            Gravity.CENTER);
        place.leftMargin = Round.dp(16f);
        place.rightMargin = Round.dp(16f);
        addView(card, place);

        setAlpha(0f);
        card.setScaleX(0.94f);
        card.setScaleY(0.94f);
        animate().alpha(1f).setDuration(Pace.SHEET).setInterpolator(Pace.STANDARD).start();
        card.animate().scaleX(1f).scaleY(1f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.STANDARD).start();
    }

    void close() {
        if (getParent() == null || closing) {
            return;
        }
        closing = true;
        final FrameLayout stage = (FrameLayout) getParent();
        animate().alpha(0f).setDuration(Pace.LEAVE).setInterpolator(Pace.AWAY)
            .withEndAction(new Runnable() {
                public void run() {
                    stage.removeView(Tray.this);
                }
            }).start();
        hand.closed();
    }

    /** Gone at once, for when the screen is not in sight. */
    void drop() {
        if (getParent() != null && !closing) {
            closing = true;
            ((FrameLayout) getParent()).removeView(this);
            hand.closed();
        }
    }
}
