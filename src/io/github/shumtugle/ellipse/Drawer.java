package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Every application on the phone, four to a row, each with its name.
 *
 * The drawer lies over the home screen like a sheet of dark glass and is
 * drawn up from below. It shows its applications one of two ways, as the
 * owner chose: as one long column scrolled downwards, or as pages turned
 * sideways. Either way a cell is the same: a tile and a name under it.
 *
 * At its foot lies a field for finding: a few letters, and only the
 * applications that answer to them stay, the best first; the key that
 * ends typing opens the first of them. Closed, the drawer forgets what
 * was typed.
 *
 * It knows nothing of how it moves; the stage it lies on does that. It
 * only says whether its list is at the top, since a pull downwards means
 * "close" there and "scroll" anywhere else. Pages turned sideways are
 * always at the top.
 */
final class Drawer extends LinearLayout {

    /** What the drawer asks the screen to do with an application. */
    interface Hand {
        void open(App app, View from);

        /** A long press: the application is picked up, to be placed or asked about. */
        void lift(App app, View from);
    }

    private static final int COLUMNS = 4;
    /** How much of a column the tile takes; the rest is air between tiles. */
    private static final float FILL = 0.92f;

    private final Icons icons;
    private final Hand hand;
    private final TextView title;
    private final GridView grid;
    private final Pages pages;
    private final Shelf shelf = new Shelf();
    private List<App> apps = new ArrayList<App>();
    private List<App> all = new ArrayList<App>();
    private String query = "";
    private final android.widget.EditText field;
    private final LinearLayout pill;
    private final Sketch clear;
    private final android.graphics.drawable.GradientDrawable sheet;
    private int tile;

    Drawer(Context context, Icons icons, boolean across, Hand hand) {
        super(context);
        this.icons = icons;
        this.hand = hand;
        setOrientation(VERTICAL);
        sheet = new android.graphics.drawable.GradientDrawable();
        // The drawer lies on the same field as the folders and the widgets:
        // its colour and how solid it stands are set once, for all of them.
        sheet.setColor(Well.colour(Well.of(context)));
        setBackground(sheet);
        corners(1f);
        setClickable(true);

        // No heading: the line at the foot says what is being looked
        // through, and the applications themselves say the rest.
        title = Letter.set(new TextView(context), Letter.HEADLINE_M);
        title.setVisibility(GONE);

        if (across) {
            grid = null;
            pages = new Pages(context, new Pages.Cells() {
                public View make() {
                    return Cell.make(getContext(), tile, Drawer.this.icons.look());
                }

                public void dress(View cell, App app, int position) {
                    Drawer.this.dress(cell, app);
                    final App mine = app;
                    cell.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Drawer.this.hand.open(mine, v);
                        }
                    });
                    cell.setOnLongClickListener(new OnLongClickListener() {
                        public boolean onLongClick(View v) {
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            Drawer.this.hand.lift(mine, v);
                            return true;
                        }
                    });
                }
            });
            addView(pages, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            pages = null;
            grid = new GridView(context);
            grid.setNumColumns(COLUMNS);
            grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            grid.setVerticalSpacing(Round.dp(18f));
            grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
            grid.setVerticalScrollBarEnabled(false);
            grid.setOverScrollMode(OVER_SCROLL_NEVER);
            grid.setClipToPadding(false);
            grid.setAdapter(shelf);
            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Drawer.this.hand.open(apps.get(position), view);
                }
            });
            grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    Drawer.this.hand.lift(apps.get(position), view);
                    return true;
                }
            });
            addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        }
        pill = new LinearLayout(context);
        pill.setOrientation(HORIZONTAL);
        pill.setGravity(android.view.Gravity.CENTER_VERTICAL);
        pill.setPadding(Round.dp(20f), 0, Round.dp(8f), 0);
        pill.setBackground(Round.box(Tone.of(Tone.SURFACE_HIGH), Round.FULL));
        Sketch lens = new Sketch(context, Sketch.SEARCH);
        lens.ink(Tone.of(Tone.ON_SURFACE_VARIANT), 0);
        pill.addView(lens, new LayoutParams(Round.dp(24f), Round.dp(24f)));
        field = new android.widget.EditText(context);
        Letter.set(field, Letter.TITLE_M);
        field.setBackground(null);
        field.setSingleLine(true);
        field.setHint(Words.s("search_apps"));
        field.setTextColor(Tone.of(Tone.ON_SURFACE));
        field.setHintTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            | android.text.InputType.TYPE_TEXT_VARIATION_FILTER);
        field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO
            | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.graphics.drawable.GradientDrawable caret = Round.box(Tone.of(Tone.PRIMARY), 1f);
            caret.setSize(Round.dp(2f), 0);
            field.setTextCursorDrawable(caret);
        }
        field.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(android.text.Editable text) {
                query = text.toString();
                clear.setVisibility(query.length() > 0 ? VISIBLE : INVISIBLE);
                refilter();
            }
        });
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int action, android.view.KeyEvent event) {
                if (!apps.isEmpty() && query.length() > 0) {
                    Drawer.this.hand.open(apps.get(0), v);
                }
                return true;
            }
        });
        LayoutParams fieldPlace = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        fieldPlace.leftMargin = Round.dp(12f);
        pill.addView(field, fieldPlace);
        clear = new Sketch(context, Sketch.CLOSE);
        clear.ink(Tone.of(Tone.ON_SURFACE_VARIANT), 0);
        clear.setVisibility(INVISIBLE);
        clear.setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.FULL));
        clear.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                field.setText("");
            }
        });
        pill.addView(clear, new LayoutParams(Round.dp(40f), Round.dp(40f)));
        addView(pill, new LayoutParams(LayoutParams.MATCH_PARENT, Round.dp(56f)));
        pad(0, 0);
    }

    /** Only what answers to the letters typed, on the first page. */
    private void refilter() {
        apps = Find.rank(all, query);
        if (grid != null) {
            shelf.notifyDataSetChanged();
            grid.setSelection(0);
        } else if (tile > 0) {
            pages.show(apps);
            pages.rewind();
        }
    }

    /**
     * The sheet's upper corners: round while the drawer is on its way, and
     * square once it is up and has become the whole screen.
     */
    void corners(float share) {
        float r = Round.px(Round.XL) * Math.max(0f, Math.min(1f, share));
        sheet.setCornerRadii(new float[] {r, r, r, r, 0f, 0f, 0f, 0f});
    }

    /**
     * The tiles in sight rise into place row after row, a little later each,
     * as the drawer comes up under them.
     */
    void cascade() {
        ViewGroup holder = grid != null ? grid : pages;
        if (holder == null) {
            return;
        }
        int[] base = new int[2];
        holder.getLocationOnScreen(base);
        for (int i = 0; i < holder.getChildCount(); i++) {
            View cell = holder.getChildAt(i);
            float row = Math.max(0f, (cell.getTop() - holder.getScrollY()) / (float) Math.max(1, cell.getHeight()));
            if (pages != null && (cell.getLeft() < pages.getScrollX() - cell.getWidth()
                || cell.getLeft() > pages.getScrollX() + pages.getWidth())) {
                continue;
            }
            cell.animate().cancel();
            cell.setTranslationY(Round.px(36f) + row * Round.px(10f));
            cell.setAlpha(0f);
            cell.animate().translationY(0f).alpha(1f).setStartDelay(Math.round(row * 38f))
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    /** What was typed forgotten, and the keyboard put away. */
    void forget() {
        if (query.length() > 0) {
            field.setText("");
        }
        if (field.hasFocus()) {
            field.clearFocus();
            android.view.inputmethod.InputMethodManager keys = (android.view.inputmethod.InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keys != null) {
                keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
            }
        }
    }

    /** Room for the system's bars above and below. */
    void pad(int top, int bottom) {
        int side = Round.dp(8f);
        title.setPadding(Round.dp(24f), top + Round.dp(28f), Round.dp(24f), Round.dp(20f));
        if (grid != null) {
            grid.setPadding(side, Round.dp(4f), side, Round.dp(12f));
        } else {
            pages.pad(Round.dp(4f), 0);
        }
        if (pill != null) {
            LayoutParams place = (LayoutParams) pill.getLayoutParams();
            place.leftMargin = Round.dp(16f);
            place.rightMargin = Round.dp(16f);
            place.topMargin = Round.dp(4f);
            place.bottomMargin = bottom + Round.dp(12f);
            pill.setLayoutParams(place);
        }
    }

    void show(List<App> list) {
        all = list;
        apps = Find.rank(all, query);
        if (grid != null) {
            shelf.notifyDataSetChanged();
        } else if (tile > 0) {
            pages.show(apps);
        }
    }

    /** Whether the list shows its first row, so a pull down may close the drawer. */
    boolean atTop() {
        if (grid == null) {
            return true;
        }
        return grid.getChildCount() == 0 || !grid.canScrollVertically(-1);
    }

    /** Back to the beginning, for the next time the drawer is opened. */
    void rewind() {
        if (grid != null) {
            grid.setSelection(0);
        } else {
            pages.rewind();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int room = w - 2 * Round.dp(8f);
        int next = Math.round(room / (float) COLUMNS * FILL);
        if (next != tile) {
            tile = next;
            if (grid != null) {
                shelf.notifyDataSetChanged();
            } else {
                // Cells are made to the tile's size, so a new size means new cells.
                post(new Runnable() {
                    public void run() {
                        pages.show(apps);
                    }
                });
            }
        }
    }

    private void dress(View made, App app) {
        Cell.dress(made, app, icons, tile);
    }

    /** The rows of the scrolled column. */
    private final class Shelf extends BaseAdapter {

        public int getCount() {
            return tile > 0 ? apps.size() : 0;
        }

        public Object getItem(int position) {
            return apps.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View reuse, ViewGroup parent) {
            LinearLayout cell = (LinearLayout) reuse;
            if (cell == null || ((ImageView) cell.getChildAt(0)).getLayoutParams().width != tile) {
                cell = Cell.make(parent.getContext(), tile, icons.look());
            }
            dress(cell, apps.get(position));
            return cell;
        }
    }
}
