package io.github.shumtugle.ellipse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Every application, by name, one to a line, on plain black. No title: a
 * list of every application says what it is by being one. At its foot
 * stands the bar the home screen already knows, here with a field to
 * narrow the list and the round button of the list's own menu.
 *
 * Held long, a line gives up its icon to the finger, and the black closes
 * over the list into that fingertip, leaving the home screen underneath
 * for the icon to be set down on.
 */
final class Drawer extends FrameLayout {

    interface Opener {
        /** Opens a door; the icon is given in the coordinates of the view it stands in. */
        void open(View from, Apps.Door door, int[] icon);

        /** A line was held: its icon is to be carried from a point of the screen. */
        void lift(Row row, float rawX, float rawY);

        /** Another order was chosen in the list's menu. */
        void order(int order);
    }

    private static final String SEARCH = "Search";
    private static final String MENU = "Menu";
    private static final String SORT = "Sort";
    /** The orders, in the order the menu offers them. */
    private static final String[] ORDERS = {"A to Z", "Newest first", "Recently updated"};
    private static final int[] ORDER_KEYS = {Keep.BY_NAME, Keep.NEWEST, Keep.UPDATED};

    private final Opener opener;
    private final ListView list;
    private final LinearLayout column;
    private final LinearLayout bar;
    private final EditText field;
    private final Blob blob;
    private final Lines lines = new Lines();
    private final float iconSize;
    private final float density;
    private List<Apps.Door> every = new ArrayList<>();
    private List<Apps.Door> doors = new ArrayList<>();
    private boolean shown;
    /** The menu: a card that grows out of the round button, and the veil that closes it. */
    private final View veil;
    private final LinearLayout card;
    private final TextView caption;
    private final TextView[] choices = new TextView[ORDERS.length];
    private boolean menu;
    private int order;
    private android.animation.ValueAnimator turning;
    private float downX;
    private float downY;

    Drawer(Context context, float iconSize, final Opener opener) {
        super(context);
        this.opener = opener;
        this.iconSize = iconSize;
        density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setBackgroundColor(Color.BLACK);
        setVisibility(GONE);
        /* Touches that fall between the lines stay here and never reach the
           screen underneath. */
        setClickable(true);

        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

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
                Row row = (Row) view;
                opener.open(row, row.door(), row.iconBounds());
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int at, long id) {
                opener.lift((Row) view, downX, downY);
                return true;
            }
        });
        /* Scrolling puts the keyboard away: the list is being read, not written. */
        list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            public void onScrollStateChanged(android.widget.AbsListView view, int state) {
                if (state == SCROLL_STATE_TOUCH_SCROLL) {
                    hideKeys();
                }
            }

            public void onScroll(android.widget.AbsListView view, int first, int count, int total) {
            }
        });
        column.addView(list, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        /* The bar, as the browser this screen borrows its look from wears
           it: one tone, one radius, one hairline, a field and a round button. */
        bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(14), dp(12), dp(14));

        field = new EditText(context);
        field.setBackground(null);
        field.setPadding(dp(6), dp(6), dp(10), dp(6));
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        field.setHint(SEARCH);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void afterTextChanged(Editable t) {
                narrow(t.toString());
            }
        });
        /* Go opens what stands first: typed enough to be alone, a name is
           opened without a second touch. */
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int action, KeyEvent event) {
                if (action == EditorInfo.IME_ACTION_GO && !doors.isEmpty()) {
                    View first = list.getChildAt(0);
                    if (first instanceof Row && ((Row) first).door() == doors.get(0)) {
                        opener.open(first, doors.get(0), ((Row) first).iconBounds());
                    } else {
                        opener.open(list, doors.get(0), new int[] {0, 0, list.getWidth(), 1});
                    }
                    return true;
                }
                return false;
            }
        });
        bar.addView(field, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        blob = new Blob(context, dp(56), Blob.MENU);
        blob.setContentDescription(MENU);
        blob.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (menu) {
                    shutMenu(true);
                } else {
                    openMenu();
                }
            }
        });
        bar.addView(blob);

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.setMargins(dp(8), dp(6), dp(8), dp(10));
        column.addView(bar, barParams);

        veil = new View(context);
        veil.setVisibility(GONE);
        veil.setClickable(true);
        veil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                shutMenu(true);
            }
        });
        addView(veil, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(14), dp(8), dp(8));
        card.setElevation(dp(6));
        card.setVisibility(GONE);
        card.setClickable(true);

        caption = new TextView(context);
        caption.setText(SORT.toUpperCase(java.util.Locale.ROOT));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 12f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setPadding(dp(16), 0, dp(16), dp(8));
        card.addView(caption);

        for (int i = 0; i < ORDERS.length; i++) {
            final int key = ORDER_KEYS[i];
            TextView choice = new TextView(context);
            choice.setText(ORDERS[i]);
            choice.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
            choice.setSingleLine(true);
            choice.setGravity(Gravity.CENTER_VERTICAL);
            choice.setPadding(dp(16), dp(13), dp(16), dp(13));
            choice.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    choose(key);
                }
            });
            LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            choiceParams.topMargin = dp(2);
            card.addView(choice, choiceParams);
            choices[i] = choice;
        }
        addView(card, new LayoutParams(dp(248), LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.END));
        tint();
    }

    /** The order the list stands in now, as the menu marks it. */
    void order(int order) {
        this.order = order;
        mark();
    }

    /** The chosen order wears the accent; the others stay plain. */
    private void mark() {
        for (int i = 0; i < choices.length; i++) {
            boolean on = ORDER_KEYS[i] == order;
            int fill = on ? ((0x2E << 24) | (Tone.primary() & 0x00FFFFFF)) : 0x00000000;
            choices[i].setBackground(Tone.touch(Tone.box(fill, dp(20), 0f), dp(20)));
            choices[i].setTextColor(on ? Tone.primary() : Tone.onSurface());
        }
    }

    boolean menuShown() {
        return menu;
    }

    /**
     * The card grows out of the round button, the three marks of which
     * draw together into a cross as it does; its lines follow one after
     * another, like icons arriving on the screen.
     */
    private void openMenu() {
        if (menu) {
            return;
        }
        menu = true;
        hideKeys();
        mark();
        LayoutParams params = (LayoutParams) card.getLayoutParams();
        params.bottomMargin = getHeight() - bar.getTop() + dp(8);
        params.rightMargin = dp(8);
        card.setLayoutParams(params);
        card.measure(MeasureSpec.makeMeasureSpec(dp(248), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        /* The pivot is the centre of the button, below the card and near
           its right edge, so the card seems to come out of the button. */
        float blobFromRight = getWidth() - getPaddingRight() - dp(8)
            - (column.getLeft() + bar.getLeft() + blob.getLeft() + blob.getWidth() / 2f);
        card.setPivotX(dp(248) - blobFromRight);
        card.setPivotY(card.getMeasuredHeight() + dp(8) + bar.getHeight() / 2f);
        card.setVisibility(VISIBLE);
        veil.setVisibility(VISIBLE);
        card.animate().cancel();
        card.setAlpha(0f);
        card.setScaleX(0.2f);
        card.setScaleY(0.2f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
        for (int i = 0; i < card.getChildCount(); i++) {
            View line = card.getChildAt(i);
            line.animate().cancel();
            line.setAlpha(0f);
            line.setTranslationY(dp(10));
            line.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP * (i + 1))
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
        turn(1f);
    }

    /** Back into the button it came from; at once, when nobody is looking. */
    void shutMenu(boolean slowly) {
        if (!menu) {
            return;
        }
        menu = false;
        veil.setVisibility(GONE);
        card.animate().cancel();
        if (!slowly) {
            card.setVisibility(GONE);
            if (turning != null) {
                turning.cancel();
            }
            blob.open(0f);
            return;
        }
        card.animate().alpha(0f).scaleX(0.2f).scaleY(0.2f).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!menu) {
                        card.setVisibility(GONE);
                    }
                }
            }).start();
        turn(0f);
    }

    private void turn(float to) {
        if (turning != null) {
            turning.cancel();
        }
        final float from = to > 0.5f ? 0f : 1f;
        turning = android.animation.ValueAnimator.ofFloat(from, to);
        turning.setDuration(to > 0.5f ? Pace.ARRIVE : Pace.ARRIVE / 2);
        turning.setInterpolator(Pace.EMPHASIS);
        turning.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                blob.open((Float) animation.getAnimatedValue());
            }
        });
        turning.start();
    }

    /** The mark moves to the chosen line first, so the choice is seen, then the card goes. */
    private void choose(int key) {
        if (key == order) {
            shutMenu(true);
            return;
        }
        order = key;
        mark();
        opener.order(key);
        postDelayed(new Runnable() {
            public void run() {
                shutMenu(true);
            }
        }, Pace.STEP * 3);
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    /** Where the last touch went down, for a held line to be lifted from. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX();
            downY = event.getRawY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            downX = event.getRawX();
            downY = event.getRawY();
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Keeps the first line clear of the status bar and the bar clear of the
     * navigation and, when it is up, the keyboard.
     */
    void inset(int top, int bottom) {
        list.setPadding(0, top + dp(12), 0, dp(8));
        LinearLayout.LayoutParams barParams = (LinearLayout.LayoutParams) bar.getLayoutParams();
        barParams.bottomMargin = bottom + dp(10);
        bar.setLayoutParams(barParams);
    }

    void fill(List<Apps.Door> doors) {
        this.every = doors;
        narrow(field.getText().toString());
    }

    /** The same list in another order: the lines are let in again from the top. */
    void reorder(List<Apps.Door> doors) {
        fill(doors);
        list.setAlpha(0f);
        list.setTranslationY(dp(16));
        list.animate().alpha(1f).translationY(0f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).start();
    }

    /** The list, narrowed to what the field holds: names that start with it first. */
    private void narrow(String typed) {
        String key = Match.norm(typed);
        if (key.length() == 0) {
            doors = every;
        } else {
            List<Apps.Door> start = new ArrayList<>();
            List<Apps.Door> word = new ArrayList<>();
            List<Apps.Door> inside = new ArrayList<>();
            for (Apps.Door door : every) {
                int rank = Match.rank(Match.norm(door.label.toString()), key);
                if (rank == Match.START) {
                    start.add(door);
                } else if (rank == Match.WORD) {
                    word.add(door);
                } else if (rank == Match.INSIDE) {
                    inside.add(door);
                }
            }
            start.addAll(word);
            start.addAll(inside);
            doors = start;
        }
        lines.notifyDataSetChanged();
        list.setSelection(0);
    }

    void tint() {
        bar.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        field.setTextColor(Tone.onSurface());
        field.setHintTextColor(Tone.faint());
        field.setHighlightColor((0x59 << 24) | (Tone.primary() & 0x00FFFFFF));
        if (Build.VERSION.SDK_INT >= 29) {
            GradientDrawable caret = new GradientDrawable();
            caret.setShape(GradientDrawable.RECTANGLE);
            caret.setColor(Tone.primary());
            caret.setCornerRadius(dp(1.5f));
            caret.setSize(dp(2), 0);
            field.setTextCursorDrawable(caret);
        }
        blob.tint();
        card.setBackground(Tone.box(Tone.containerHigh(), dp(28), dp(0.5f)));
        caption.setTextColor(Tone.faint());
        mark();
        for (int i = 0; i < list.getChildCount(); i++) {
            ((Row) list.getChildAt(i)).tint();
        }
    }

    boolean shown() {
        return shown;
    }

    private void hideKeys() {
        InputMethodManager keys = (InputMethodManager)
            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keys != null) {
            keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
        field.clearFocus();
    }

    /** Emptied and put away, ready to be found at the top of the alphabet next time. */
    private void forget() {
        shutMenu(false);
        hideKeys();
        if (field.getText().length() > 0) {
            field.setText("");
        }
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
        setTranslationY(density * 72f);
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
        forget();
        animate().cancel();
        if (!slowly) {
            setVisibility(GONE);
            return;
        }
        animate().alpha(0f).translationY(density * 48f)
            .setDuration(Pace.ARRIVE / 2).setInterpolator(Pace.EMPHASIS)
            .withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                    }
                }
            }).start();
    }

    /**
     * The black closes over the list into one point, the fingertip that
     * holds the icon, and the home screen is left standing where it was.
     */
    void swallow(float x, float y) {
        if (!shown) {
            return;
        }
        shown = false;
        forget();
        animate().cancel();
        setAlpha(1f);
        setTranslationY(0f);
        float far = (float) Math.hypot(Math.max(x, getWidth() - x), Math.max(y, getHeight() - y));
        Animator closing = ViewAnimationUtils.createCircularReveal(this,
            Math.round(x), Math.round(y), far, 0f);
        closing.setDuration(Pace.ARRIVE + Pace.STEP * 2);
        closing.setInterpolator(Pace.EMPHASIS);
        closing.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!shown) {
                    setVisibility(GONE);
                }
            }
        });
        closing.start();
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
