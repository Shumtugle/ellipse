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
 * Every application, on plain black: by name one to a line going down, or
 * as icons with their names on pages of four by five going across. No title: a
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

        /** A line or an icon was held: its icon is to be carried from a point of the screen. */
        void lift(View from, Apps.Door door, int[] icon, float rawX, float rawY);

        /** Another order was chosen in the list's menu. */
        void order(int order);

        /** The launcher's settings were asked for from the list's menu. */
        void settings(View from);
    }

    private static final String SEARCH = "Search";
    private static final String MENU = "Menu";
    /**
     * The menu: the order, which is changed often and so stays at hand,
     * and the way to the settings, where lines or pages and the rest are
     * chosen once and left.
     */
    private static final String SORT = "Sort";
    private static final String[] ORDERS = {"A to Z", "Newest first", "Recently updated"};
    private static final int[] ORDER_KEYS = {Keep.BY_NAME, Keep.NEWEST, Keep.UPDATED};
    private static final String[] AWAY = {"Settings"};
    private static final int[] AWAY_KEYS = {0};
    /** The grid of a page, when the list is laid out across. */
    private int columns = 4;
    private int rows = 5;

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
    /** The list's menu, grown out of the round button. */
    private final Menu menu;
    private int order;
    private int view;
    /** The pages, when the list is laid out across, and the icon size on them. */
    private final Pager pager;
    private final float gridIcon;
    private android.animation.ValueAnimator turning;
    private float downX;
    private float downY;

    Drawer(Context context, float gridIcon, final Opener opener) {
        super(context);
        this.opener = opener;
        this.gridIcon = gridIcon;
        this.iconSize = Math.round(gridIcon * 0.78f);
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
                Row row = (Row) view;
                opener.lift(row, row.door(), row.iconBounds(), downX, downY);
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
        FrameLayout content = new FrameLayout(context);
        column.addView(content, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        content.addView(list, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        pager = new Pager(context);
        pager.setVisibility(GONE);
        pager.turn(new Pager.Turn() {
            public void turned(int page) {
                hideKeys();
            }
        });
        content.addView(pager, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        /* The bar, as the browser this screen borrows its look from wears
           it: one tone, one radius, one hairline, a field and a round button. */
        bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(14), dp(12), dp(14));

        field = new EditText(context);
        field.setBackground(null);
        field.setPadding(dp(6), dp(6), dp(10), dp(6));
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
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
                if (menu.shown()) {
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

        menu = new Menu(context, this, new Menu.Listener() {
            public void picked(int section, int key) {
                choose(section, key);
            }

            public void closing() {
                turn(0f);
            }
        });
        tint();
    }

    /** The grid of the pages, and whether they go round; the pages are laid out again. */
    void grid(int columns, int rows, boolean endless) {
        this.columns = columns;
        this.rows = rows;
        pager.endless(endless);
        paginate();
    }

    /** The order and the view the list stands in now, as the menu marks them. */
    void order(int order, int view) {
        this.order = order;
        this.view = view;
        list.setVisibility(view == Keep.PAGES ? GONE : VISIBLE);
        pager.setVisibility(view == Keep.PAGES ? VISIBLE : GONE);
    }

    boolean menuShown() {
        return menu.shown();
    }

    /**
     * The card grows out of the round button, the three marks of which
     * draw together into a cross as it does.
     */
    private void openMenu() {
        hideKeys();
        Menu.Section sort = new Menu.Section(SORT, ORDERS, ORDER_KEYS);
        sort.chosen = order;
        Menu.Section away = new Menu.Section(null, AWAY, AWAY_KEYS);
        float x = column.getLeft() + bar.getLeft() + blob.getLeft() + blob.getWidth() / 2f;
        float y = column.getTop() + bar.getTop() + blob.getTop() + blob.getHeight() / 2f;
        menu.show(new Menu.Section[] {sort, away}, x, y, blob.getHeight() / 2f + dp(22));
        turn(1f);
    }

    void shutMenu(boolean slowly) {
        menu.hide(slowly);
        if (!slowly) {
            if (turning != null) {
                turning.cancel();
            }
            blob.open(0f);
        }
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
    private void choose(int section, int key) {
        if (section == 1) {
            shutMenu(true);
            opener.settings(blob);
            return;
        }
        if (key == order) {
            shutMenu(true);
            return;
        }
        menu.choose(section, key);
        order = key;
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
        pager.setPadding(0, top + dp(12), 0, 0);
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
        enter(view == Keep.PAGES ? pager : list);
    }

    /** The content is let in again, from a little below. */
    private void enter(View content) {
        content.setAlpha(0f);
        content.setTranslationY(dp(16));
        content.animate().alpha(1f).translationY(0f).setDuration(Pace.ARRIVE)
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
        paginate();
    }

    /**
     * The same doors, twenty to a page, filled row by row. The pages are
     * built again whenever the doors change; their icons are only painted
     * once a page is turned to.
     */
    private void paginate() {
        pager.removeAllViews();
        Grid page = null;
        for (int i = 0; i < doors.size(); i++) {
            int at = i % (columns * rows);
            if (at == 0) {
                page = new Grid(getContext(), columns, rows);
                page.setPadding(dp(8), 0, dp(8), 0);
                pager.addView(page);
            }
            final Cell cell = new Cell(getContext(), doors.get(i), gridIcon, true);
            cell.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    opener.open(cell, cell.door, cell.localIcon());
                }
            });
            cell.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    opener.lift(cell, cell.door, cell.localIcon(), downX, downY);
                    return true;
                }
            });
            page.put(cell, at % columns, at / columns);
        }
        pager.show(0, false);
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

    /** The height it travels when drawn by the finger: the whole floor. */
    private float travel() {
        View floor = (View) getParent();
        return floor == null ? getHeight() : floor.getHeight();
    }

    /**
     * Whether a pull downward should close it rather than scroll: pages
     * always stand at the top; lines only when the first one is in full view.
     */
    boolean atTop() {
        if (view == Keep.PAGES || list.getChildCount() == 0) {
            return true;
        }
        return list.getFirstVisiblePosition() == 0
            && list.getChildAt(0).getTop() >= list.getPaddingTop();
    }

    /** Readies to be drawn up by the finger, from below the screen, at the top of the alphabet. */
    void begin() {
        shown = true;
        animate().cancel();
        list.setSelection(0);
        pager.show(0, false);
        setAlpha(1f);
        setTranslationY(travel());
        setVisibility(VISIBLE);
    }

    /** How far it stands drawn, from nought, below the screen, to one, open. */
    void drag(float drawn) {
        drawn = drawn < 0f ? 0f : (drawn > 1f ? 1f : drawn);
        setTranslationY((1f - drawn) * travel());
    }

    /** Let go: it goes on to open, or back down and away. */
    void let(boolean open) {
        animate().cancel();
        if (open) {
            shown = true;
            animate().translationY(0f).alpha(1f).setDuration(Pace.ARRIVE)
                .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
            return;
        }
        shown = false;
        forget();
        animate().translationY(travel()).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                        setTranslationY(0f);
                    }
                }
            }).start();
    }

    /** Rises from the foot of the screen, always from the top of the alphabet. */
    void rise() {
        if (shown) {
            return;
        }
        shown = true;
        list.setSelection(0);
        pager.show(0, false);
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
