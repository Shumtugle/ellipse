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

    interface Hand {
        void chosen(int key);
    }

    /** One thing to choose: its picture, its name, and the key it is chosen by. */
    static final class Item {
        final Drawable icon;
        final CharSequence name;
        final int key;

        Item(Drawable icon, CharSequence name, int key) {
            this.icon = icon;
            this.name = name == null ? "" : name;
            this.key = key;
        }
    }

    private static final String SEARCH = "Search";
    private static final int COLUMNS = 4;

    private final Hand hand;
    private final float iconSize;
    private final float density;
    private final float scaled;
    private final LinearLayout head;
    private final View pill;
    private final Glyph lens;
    private final EditText field;
    private final ScrollView scroll;
    private final LinearLayout groups;
    private String[] captions = new String[0];
    private List<List<Item>> items = new ArrayList<>();
    private boolean shown;

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

        head = new LinearLayout(context);
        head.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(6), dp(16), dp(6));
        lens = new Glyph(context, Glyph.SEARCH, dp(26));
        bar.addView(lens);
        field = new EditText(context);
        field.setBackground(null);
        field.setHint(SEARCH);
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.setPadding(dp(18), dp(14), dp(8), dp(14));
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void afterTextChanged(Editable t) {
                build();
            }
        });
        bar.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.setMargins(dp(16), dp(12), dp(16), dp(8));
        head.addView(bar, barParams);
        pill = bar;
        column.addView(head);

        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        groups = new LinearLayout(context);
        groups.setOrientation(LinearLayout.VERTICAL);
        groups.setPadding(dp(8), dp(4), dp(8), dp(24));
        scroll.addView(groups);
        column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    void inset(int top, int bottom) {
        head.setPadding(0, top, 0, 0);
        scroll.setPadding(0, 0, 0, bottom);
    }

    boolean shown() {
        return shown;
    }

    /** Rises with things in groups, each group under its caption. */
    void show(String[] captions, List<List<Item>> items) {
        this.captions = captions;
        this.items = items;
        setBackgroundColor(Tone.surface());
        pill.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        lens.tint(Tone.faint());
        field.setTextColor(Tone.onSurface());
        field.setHintTextColor(Tone.faint());
        field.setText("");
        build();
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
        InputMethodManager keys = (InputMethodManager)
            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keys != null) {
            keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
        field.clearFocus();
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

    private void build() {
        groups.removeAllViews();
        String typed = Match.norm(field.getText().toString());
        int count = 0;
        for (int g = 0; g < captions.length && g < items.size(); g++) {
            List<Item> found = new ArrayList<>();
            for (Item item : items.get(g)) {
                if (typed.length() == 0 || Match.rank(Match.norm(item.name.toString()), typed) != Match.NONE) {
                    found.add(item);
                }
            }
            if (found.isEmpty()) {
                continue;
            }
            TextView caption = new TextView(getContext());
            caption.setText(captions[g].toUpperCase(Locale.getDefault()));
            caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
            caption.setLetterSpacing(0.12f);
            caption.setTextColor(Tone.faint());
            caption.setPadding(dp(16), dp(g == 0 ? 8 : 24), dp(16), dp(4));
            groups.addView(caption);
            int rows = (found.size() + COLUMNS - 1) / COLUMNS;
            Grid grid = new Grid(getContext(), COLUMNS, rows);
            for (int i = 0; i < found.size(); i++) {
                final Item item = found.get(i);
                Cell cell = new Cell(getContext(), item.icon, item.name, iconSize);
                cell.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        hand.chosen(item.key);
                    }
                });
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
}
