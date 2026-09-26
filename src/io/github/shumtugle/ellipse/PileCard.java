package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A pile's own card, from a widget's menu: the widgets it holds in order,
 * the first one shown first; each carried by its lines to another place,
 * or taken out by its cross; one more added from the widget shelf; and
 * whether the pile turns by itself. Nothing changes until Done; a touch
 * outside the card lets it go as it was.
 */
final class PileCard extends FrameLayout {

    /** One widget in the card: its number, its name, its picture. */
    static final class Entry {
        final int id;
        final CharSequence name;
        final Drawable picture;

        Entry(int id, CharSequence name, Drawable picture) {
            this.id = id;
            this.name = name;
            this.picture = picture;
        }
    }

    interface Hand {
        /** Done: the widgets kept, in order; those taken out; whether it turns by itself. */
        void done(List<Integer> kept, List<Integer> out, boolean turns);

        /** One more wanted: the same, then the shelf. */
        void add(List<Integer> kept, List<Integer> out, boolean turns);
    }

    private final float density;
    private final float scaled;
    private final Hand hand;
    private final View veil;
    private final LinearLayout card;
    private final LinearLayout list;
    private final List<Entry> order = new ArrayList<>();
    private final List<Integer> out = new ArrayList<>();
    private Toggle turning;

    PileCard(Context context, Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibility(GONE);
        veil = new View(context);
        veil.setBackgroundColor(0x80000000);
        veil.setClickable(true);
        veil.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
            }
        });
        addView(veil, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        card.setElevation(dp(8));
        LayoutParams at = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        at.setMargins(dp(12), dp(24), dp(12), dp(24));
        addView(card, at);
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    boolean shown() {
        return getVisibility() == VISIBLE;
    }

    void show(List<Entry> widgets, boolean turns) {
        order.clear();
        order.addAll(widgets);
        out.clear();
        card.removeAllViews();
        list.removeAllViews();
        card.setBackground(Tone.box(Tone.containerHigh(), dp(28), dp(0.5f)));
        card.setPadding(dp(12), dp(18), dp(12), dp(10));

        TextView caption = new TextView(getContext());
        caption.setText(Words.t("Stack").toUpperCase(Locale.getDefault()));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setTextColor(Tone.faint());
        caption.setPadding(dp(12), 0, dp(12), dp(10));
        card.addView(caption);

        for (Entry entry : order) {
            list.addView(row(entry));
        }
        mark();
        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(list);
        card.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView more = new TextView(getContext());
        more.setText("+  " + Words.t("Add a widget to the stack"));
        more.setGravity(Gravity.CENTER);
        more.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f * scaled);
        more.setTextColor(Tone.onPrimaryContainer());
        more.setTypeface(Style.bold());
        more.setBackground(Tone.touch(Tone.box(Tone.primaryContainer(), dp(28), 0f), dp(28)));
        more.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setVisibility(GONE);
                hand.add(ids(), new ArrayList<>(out), turning.on());
            }
        });
        LinearLayout.LayoutParams moreAt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        moreAt.setMargins(dp(8), dp(12), dp(8), dp(8));
        card.addView(more, moreAt);

        LinearLayout turn = new LinearLayout(getContext());
        turn.setGravity(Gravity.CENTER_VERTICAL);
        turn.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout words = new LinearLayout(getContext());
        words.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(getContext());
        name.setText(Words.t("Turns by itself"));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
        name.setTextColor(Tone.onSurface());
        words.addView(name);
        TextView about = new TextView(getContext());
        about.setText(Words.t("Off, only a swipe turns it"));
        about.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        about.setTextColor(Tone.faint());
        words.addView(about);
        turn.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        turning = new Toggle(getContext(), turns);
        turn.addView(turning);
        turn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                turning.set(!turning.on());
            }
        });
        card.addView(turn);

        TextView done = new TextView(getContext());
        done.setText(Words.t("Done"));
        done.setGravity(Gravity.CENTER);
        done.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f * scaled);
        done.setTextColor(Tone.primary());
        done.setTypeface(Style.bold());
        done.setBackground(Tone.touch(null, dp(24)));
        done.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setVisibility(GONE);
                hand.done(ids(), new ArrayList<>(out), turning.on());
            }
        });
        card.addView(done, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        setVisibility(VISIBLE);
        card.setAlpha(0f);
        card.setTranslationY(dp(40));
        veil.setAlpha(0f);
        veil.animate().alpha(1f).setDuration(Pace.ARRIVE).start();
        card.animate().alpha(1f).translationY(0f).setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
    }

    void close() {
        veil.animate().alpha(0f).setDuration(Pace.ARRIVE / 2).start();
        card.animate().alpha(0f).translationY(dp(40)).setDuration(Pace.ARRIVE / 2).withEndAction(new Runnable() {
            public void run() {
                setVisibility(GONE);
            }
        }).start();
    }

    private List<Integer> ids() {
        List<Integer> all = new ArrayList<>();
        for (Entry entry : order) {
            all.add(entry.id);
        }
        return all;
    }

    /** One widget's line: its picture, its name, the lines it is carried by, and its cross. */
    private View row(final Entry entry) {
        final LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(6), dp(10));
        row.setTag(entry);
        ImageView picture = new ImageView(getContext());
        picture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        picture.setImageDrawable(entry.picture);
        picture.setBackground(Tone.box(Tone.container(), dp(12), 0f));
        picture.setPadding(dp(6), dp(6), dp(6), dp(6));
        row.addView(picture, new LinearLayout.LayoutParams(dp(84), dp(56)));
        LinearLayout words = new LinearLayout(getContext());
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(14), 0, dp(6), 0);
        TextView name = new TextView(getContext());
        name.setText(entry.name);
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
        name.setTextColor(Tone.onSurface());
        name.setTypeface(Style.bold());
        name.setMaxLines(2);
        words.addView(name);
        TextView first = new TextView(getContext());
        first.setText(Words.t("Shown first"));
        first.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        first.setTextColor(Tone.primary());
        words.addView(first);
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Mark grip = new Mark(getContext(), false);
        row.addView(grip, new LinearLayout.LayoutParams(dp(44), dp(44)));
        Mark cross = new Mark(getContext(), true);
        cross.setBackground(Tone.touch(null, dp(22)));
        cross.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                order.remove(entry);
                out.add(entry.id);
                list.removeView(row);
                mark();
            }
        });
        row.addView(cross, new LinearLayout.LayoutParams(dp(44), dp(44)));
        grip.setOnTouchListener(new OnTouchListener() {
            float lastY;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastY = event.getRawY();
                        row.setElevation(dp(4));
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        row.setTranslationY(row.getTranslationY() + event.getRawY() - lastY);
                        lastY = event.getRawY();
                        carry(row);
                        return true;
                    default:
                        row.animate().translationY(0f).setDuration(Pace.PRESS).start();
                        row.setElevation(0f);
                        mark();
                        return true;
                }
            }
        });
        return row;
    }

    /** A carried line changes places with its neighbour once it is half over it. */
    private void carry(View row) {
        int at = list.indexOfChild(row);
        float moved = row.getTranslationY();
        int tall = row.getHeight();
        if (moved > tall / 2f && at < list.getChildCount() - 1) {
            View below = list.getChildAt(at + 1);
            list.removeView(below);
            list.addView(below, at);
            row.setTranslationY(moved - tall);
            swap(at, at + 1);
        } else if (moved < -tall / 2f && at > 0) {
            View above = list.getChildAt(at - 1);
            list.removeView(above);
            list.addView(above, at);
            row.setTranslationY(moved + tall);
            swap(at, at - 1);
        }
    }

    private void swap(int a, int b) {
        Entry one = order.get(a);
        order.set(a, order.get(b));
        order.set(b, one);
    }

    /** The first line outlined in the accent and told it is shown first; the rest plain. */
    private void mark() {
        for (int i = 0; i < list.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) list.getChildAt(i);
            boolean first = i == 0;
            row.setBackground(first ? Tone.box(Tone.container(), dp(20), dp(1.5f), Tone.primary())
                : Tone.box(Tone.container(), dp(20), 0f));
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            at.setMargins(dp(4), dp(4), dp(4), dp(6));
            row.setLayoutParams(at);
            View words = row.getChildAt(1);
            if (words instanceof LinearLayout) {
                ((LinearLayout) words).getChildAt(1).setVisibility(first ? VISIBLE : GONE);
            }
        }
    }

    /** Three thin lines to carry a line by, or a thin cross in a ring to take it out. */
    private static final class Mark extends View {
        private final boolean cross;
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;

        Mark(Context context, boolean cross) {
            super(context);
            this.cross = cross;
            density = context.getResources().getDisplayMetrics().density;
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            ink.setColor(Tone.faint());
            if (cross) {
                ink.setStrokeWidth(1f * density);
                canvas.drawCircle(cx, cy, 13f * density, ink);
                ink.setStrokeWidth(1.6f * density);
                float r = 5f * density;
                canvas.drawLine(cx - r, cy - r, cx + r, cy + r, ink);
                canvas.drawLine(cx + r, cy - r, cx - r, cy + r, ink);
            } else {
                ink.setStrokeWidth(1.6f * density);
                float half = 9f * density;
                for (int i = -1; i <= 1; i++) {
                    float y = cy + i * 6f * density;
                    canvas.drawLine(cx - half, y, cx + half, y, ink);
                }
            }
        }
    }
}
