package io.github.shumtugle.ellipse;

import android.content.Context;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * What is fresh: the applications last opened from here, and those put on
 * the phone or changed lately, lowered from the top over the home screen.
 *
 * Every tile keeps its name; a fresh one says under it whether it is new
 * or updated, and an application signed with the same key as this home
 * screen says its version too, so a build just put on the phone can be
 * seen to be the one that was meant.
 *
 * It is pushed back up by the finger, from anywhere on it: a throw
 * upwards or a pull past a quarter of its height sends it away; less, and
 * it settles back. A touch on the screen below it sends it away too. A
 * press opens an application; a long press lifts it, to be set down on a
 * screen.
 */
final class Fresh extends LinearLayout {

    interface Hand {
        void open(App app, View from);

        void lift(App app, View from);

        void closed();
    }

    private static final int COLUMNS = 4;

    private final Hand hand;
    private final int slop;
    private final float throwSpeed;
    private VelocityTracker velocity;
    private float downX;
    private float downY;
    private boolean dragging;
    private boolean closing;
    private FrameLayout veil;

    Fresh(Context context, List<App> recent, List<App> fresh, List<String> tags, Icons icons,
          int width, int top, int bottom, Hand hand) {
        super(context);
        this.hand = hand;
        ViewConfiguration config = ViewConfiguration.get(context);
        slop = config.getScaledTouchSlop();
        throwSpeed = config.getScaledMinimumFlingVelocity() * 6f;
        setOrientation(VERTICAL);
        setClickable(true);
        android.graphics.drawable.GradientDrawable ground = new android.graphics.drawable.GradientDrawable();
        ground.setColor(Tone.of(Tone.SURFACE, 0.97f));
        float r = Round.px(Round.XL);
        ground.setCornerRadii(new float[] {0f, 0f, 0f, 0f, r, r, r, r});
        setBackground(ground);
        setPadding(Round.dp(8f), top + Round.dp(24f), Round.dp(8f), Round.dp(12f));
        setElevation(Round.px(4f));

        int tile = Math.round((width - Round.dp(16f)) / (float) COLUMNS * 0.92f);
        if (!recent.isEmpty()) {
            addView(title(Words.s("recent")));
            rows(recent, null, icons, tile);
        }
        if (!fresh.isEmpty()) {
            TextView name = title(Words.s("fresh"));
            addView(name, gap(recent.isEmpty() ? 0 : 20));
            rows(fresh, tags, icons, tile);
        }
        if (recent.isEmpty() && fresh.isEmpty()) {
            TextView none = Letter.set(new TextView(context), Letter.BODY_L);
            none.setText(Words.s("fresh_empty"));
            none.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
            none.setPadding(Round.dp(16f), Round.dp(8f), Round.dp(16f), Round.dp(24f));
            addView(none);
        }

        View grip = new View(context);
        grip.setBackground(Round.box(Tone.of(Tone.ON_SURFACE_VARIANT, 0.5f), Round.FULL));
        LayoutParams gripPlace = new LayoutParams(Round.dp(32f), Round.dp(4f));
        gripPlace.gravity = Gravity.CENTER_HORIZONTAL;
        gripPlace.topMargin = Round.dp(16f);
        addView(grip, gripPlace);
    }

    private TextView title(String text) {
        TextView made = Letter.set(new TextView(getContext()), Letter.HEADLINE_M);
        Letter.serif(made);
        made.setText(text);
        made.setTextColor(Tone.of(Tone.ON_SURFACE));
        made.setPadding(Round.dp(16f), 0, Round.dp(16f), Round.dp(12f));
        return made;
    }

    private static LayoutParams gap(int above) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = Round.dp(above);
        return params;
    }

    /** The tiles four to a row, each with its name and, if given, a word about it. */
    private void rows(List<App> apps, List<String> tags, Icons icons, int tile) {
        LinearLayout row = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(HORIZONTAL);
                addView(row, gap(i == 0 ? 0 : 8));
            }
            final App app = apps.get(i);
            LinearLayout cell = Cell.make(getContext(), tile, icons.look());
            Cell.dress(cell, app, icons, tile);
            if (tags != null && i < tags.size()) {
                TextView tag = Letter.set(new TextView(getContext()), Letter.LABEL_S);
                tag.setText(tags.get(i));
                tag.setTextColor(Tone.of(Tone.PRIMARY));
                tag.setGravity(Gravity.CENTER_HORIZONTAL);
                tag.setSingleLine(true);
                cell.addView(tag, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
            cell.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    hand.open(app, v);
                }
            });
            cell.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    hand.lift(app, v);
                    return true;
                }
            });
            row.addView(cell, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        }
        for (int i = apps.size(); i % COLUMNS != 0; i++) {
            row.addView(new View(getContext()), new LayoutParams(0, 1, 1f));
        }
    }

    /**
     * Lowered from above the glass, over a faint veil: a touch on the veil,
     * below the panel, sends the panel away.
     */
    void show(final FrameLayout stage) {
        veil = new FrameLayout(getContext());
        veil.setBackgroundColor(0x33000000);
        veil.setClickable(true);
        veil.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
            }
        });
        veil.addView(this, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP));
        stage.addView(veil, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        veil.setAlpha(0f);
        veil.animate().alpha(1f).setDuration(Pace.SHEET).setInterpolator(Pace.STANDARD).start();
        post(new Runnable() {
            public void run() {
                setTranslationY(-getHeight());
                animate().translationY(0f).setDuration(Pace.ARRIVE).setInterpolator(Pace.STANDARD).start();
            }
        });
    }

    void close() {
        if (closing || veil == null || veil.getParent() == null) {
            return;
        }
        closing = true;
        final FrameLayout gone = veil;
        animate().cancel();
        animate().translationY(-getHeight()).setDuration(Pace.LEAVE).setInterpolator(Pace.AWAY).start();
        gone.animate().alpha(0f).setDuration(Pace.LEAVE).setInterpolator(Pace.AWAY)
            .withEndAction(new Runnable() {
                public void run() {
                    if (gone.getParent() != null) {
                        ((FrameLayout) gone.getParent()).removeView(gone);
                    }
                }
            }).start();
        hand.closed();
    }

    /** Gone at once, for when the screen is not in sight or a tile has been lifted out. */
    void drop() {
        if (closing || veil == null || veil.getParent() == null) {
            return;
        }
        closing = true;
        ((FrameLayout) veil.getParent()).removeView(veil);
        hand.closed();
    }

    // ------------------------------------------------------------ touch

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        watch(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getRawX();
                downY = e.getRawY();
                dragging = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                return dragging || decide(e);
            default:
                return dragging;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        watch(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getRawX();
                downY = e.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    decide(e);
                }
                if (dragging) {
                    setTranslationY(Math.min(0f, e.getRawY() - downY));
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    settle();
                }
                dragging = false;
                if (velocity != null) {
                    velocity.recycle();
                    velocity = null;
                }
                return true;
            default:
                return true;
        }
    }

    private boolean decide(MotionEvent e) {
        float dx = e.getRawX() - downX;
        float dy = e.getRawY() - downY;
        if (dy < -slop && Math.abs(dy) > Math.abs(dx)) {
            dragging = true;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }
        return false;
    }

    private void settle() {
        float speed = 0f;
        if (velocity != null) {
            velocity.computeCurrentVelocity(1000);
            speed = velocity.getYVelocity();
        }
        if (speed < -throwSpeed || -getTranslationY() > getHeight() * 0.25f) {
            close();
        } else {
            animate().translationY(0f).setDuration(Pace.GROW).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    /** Movement is measured on the glass, not on the panel, since the panel itself moves. */
    private void watch(MotionEvent e) {
        if (velocity == null) {
            velocity = VelocityTracker.obtain();
        }
        MotionEvent raw = MotionEvent.obtain(e);
        raw.setLocation(e.getRawX(), e.getRawY());
        velocity.addMovement(raw);
        raw.recycle();
    }
}
