package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A whole screen to choose one thing from many, with room to breathe: a
 * field for finding at the top, and under it the things in groups, each
 * with a small name, laid out four to a row as icons with their names,
 * the size they have on the screens. A touch chooses; Back leaves.
 */
final class Chooser extends FrameLayout {

    private Glow glow;

    interface Hand {
        void chosen(int key);

        /** The launcher's settings were asked for from the screen's menu. */
        void settings();
    }

    /** One thing to choose: its picture, its name, and the key it is chosen by. */
    static final class Item {
        final Drawable icon;
        final CharSequence name;
        final int key;
        /** A faint second line under the name, found by the search too; or none. */
        CharSequence note;

        Item(Drawable icon, CharSequence name, int key) {
            this.icon = icon;
            this.name = name == null ? "" : name;
            this.key = key;
        }

        Item noted(CharSequence said) {
            note = said;
            return this;
        }
    }

    private static final String SEARCH = "Search shortcuts";
    private static final String SETTINGS = "Settings";
    private static final int COLUMNS = 4;

    private final Hand hand;

    /** What holding a thing does, where holding means something; none elsewhere. */
    interface Hold {
        void held(int key);
    }

    private Hold hold;

    /** Holding a thing does this until the chooser is shown again with nothing held. */
    void hold(Hold what) {
        hold = what;
    }
    private final float iconSize;
    private final float density;
    private final float scaled;
    private final Foot foot;
    private final EditText field;
    private final ScrollView scroll;
    private final LinearLayout groups;
    private String[] captions = new String[0];
    private List<List<Item>> items = new ArrayList<>();
    private boolean shown;
    /** Lines, one thing to a line, or a grid of four to a row. */
    private boolean grid;

    Chooser(Context context, float iconSize, Hand hand) {
        super(context);
        this.hand = hand;
        this.iconSize = iconSize;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibility(GONE);
        setClickable(true);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        groups = new LinearLayout(context);
        groups.setOrientation(LinearLayout.VERTICAL);
        groups.setPadding(dp(8), dp(4), dp(8), dp(24));
        scroll.addView(groups);
        column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        foot = new Foot(context, this, SEARCH, new Foot.Owner() {
            public Menu.Section[] sections() {
                return new Menu.Section[] {Home.settingsLine(SETTINGS, 0)};
            }

            public void picked(int section, int key) {
                Chooser.this.hand.settings();
            }

            public void leave() {
                close(true);
            }

            public void typed(String text) {
                build();
            }
        });
        column.addView(foot, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        field = foot.field();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    void inset(int top, int bottom) {
        scroll.setPadding(0, top, 0, 0);
        foot.lift(bottom);
    }

    boolean shown() {
        return shown;
    }

    /** What the field at the foot says it finds, for what is chosen now. */
    void hint(String what) {
        field.setHint(Words.t(what));
    }

    /** Rises with things in groups, each group under its caption; none for a group with no name. */
    void show(String[] captions, List<List<Item>> items, boolean grid) {
        this.grid = grid;
        this.captions = captions;
        this.items = items;
        /* The soft ground of the settings, drifting while the list is shown. */
        glow = new Glow();
        setBackground(glow);
        foot.tint();
        field.setText("");
        build();
        Style.apply(this);
        scroll.scrollTo(0, 0);
        shown = true;
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(dp(72));
        animate().cancel();
        animate().alpha(1f).translationY(0f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
    }

    void close(boolean slowly) {
        if (!shown) {
            return;
        }
        shown = false;
        foot.shutMenu(false);
        foot.hideKeys();
        animate().cancel();
        if (!slowly) {
            setVisibility(GONE);
            return;
        }
        animate().alpha(0f).translationY(dp(48)).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                    }
                }
            }).start();
    }

    /** One thing to a line: its picture and its name, as the list of every app has them. */
    private View line(final Item item) {
        LinearLayout made = new LinearLayout(getContext());
        made.setOrientation(LinearLayout.HORIZONTAL);
        made.setGravity(Gravity.CENTER_VERTICAL);
        made.setPadding(dp(24), dp(12), dp(24), dp(12));
        made.setBackground(Tone.touch(null, 0f));
        android.widget.ImageView picture = new android.widget.ImageView(getContext());
        picture.setImageDrawable(item.icon);
        int size = Math.round(iconSize * 0.78f);
        made.addView(picture, new LinearLayout.LayoutParams(size, size));
        TextView name = new TextView(getContext());
        name.setText(item.name);
        name.setTextColor(Tone.onSurface());
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(dp(18), 0, 0, 0);
        if (item.note == null) {
            made.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            LinearLayout words = new LinearLayout(getContext());
            words.setOrientation(LinearLayout.VERTICAL);
            words.addView(name);
            TextView note = new TextView(getContext());
            note.setText(item.note);
            note.setTextColor(Tone.faint());
            note.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
            note.setSingleLine(true);
            note.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            note.setPadding(dp(18), dp(1), 0, 0);
            words.addView(note);
            made.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        made.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                hand.chosen(item.key);
            }
        });
        return made;
    }

    private void build() {
        groups.removeAllViews();
        String typed = Match.norm(field.getText().toString());
        int count = 0;
        for (int g = 0; g < captions.length && g < items.size(); g++) {
            List<Item> found = new ArrayList<>();
            for (Item item : items.get(g)) {
                if (typed.length() == 0 || Match.rank(Match.norm(item.name.toString()), typed) != Match.NONE
                    || (item.note != null && Match.norm(item.note.toString()).contains(typed))) {
                    found.add(item);
                }
            }
            if (found.isEmpty()) {
                continue;
            }
            if (captions[g] != null) {
                TextView caption = new TextView(getContext());
                caption.setText(Words.t(captions[g]).toUpperCase(Locale.getDefault()));
                caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
                caption.setLetterSpacing(0.12f);
                caption.setTextColor(Tone.faint());
                caption.setPadding(dp(16), dp(g == 0 ? 8 : 24), dp(16), dp(4));
                groups.addView(caption);
            }
            if (!grid) {
                for (int i = 0; i < found.size(); i++) {
                    View line = line(found.get(i));
                    groups.addView(line);
                    if (i < 12) {
                        line.setAlpha(0f);
                        line.setTranslationY(dp(10));
                        line.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP / 2 * i)
                            .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
                    }
                }
                count++;
                continue;
            }
            int rows = (found.size() + COLUMNS - 1) / COLUMNS;
            Grid grid = new Grid(getContext(), COLUMNS, rows);
            for (int i = 0; i < found.size(); i++) {
                final Item item = found.get(i);
                Cell cell = new Cell(getContext(), item.icon, item.name, iconSize).onGround();
                cell.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        hand.chosen(item.key);
                    }
                });
                if (hold != null) {
                    cell.setOnLongClickListener(new OnLongClickListener() {
                        public boolean onLongClick(View v) {
                            hold.held(item.key);
                            return true;
                        }
                    });
                }
                grid.put(cell, i % COLUMNS, i / COLUMNS);
            }
            float tall = iconSize + dp(60);
            groups.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(tall * rows)));
            if (count < 6) {
                grid.setAlpha(0f);
                grid.setTranslationY(dp(12));
                grid.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP * count)
                    .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
            }
            count++;
        }
    }

    @Override
    protected void onVisibilityChanged(View changed, int visibility) {
        super.onVisibilityChanged(changed, visibility);
        if (glow != null) {
            if (isShown()) {
                glow.start(null);
            } else {
                glow.stop();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (glow != null) {
            glow.stop();
        }
        super.onDetachedFromWindow();
    }
}
