package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The settings of the home screen, in the shape settings of home screens
 * have come to take: the rooms of the settings one under another, each
 * with its drawing, its name and a line about what is in it, and at the
 * foot the bar every listing screen here has, a field to find a setting
 * and the round button of a small menu. A room opens sideways, with its
 * name large at its head; Back leaves it, and Back again leaves the
 * settings.
 *
 * Inside a room a line is one of four things: a switch, a choice that
 * opens the application's one menu beside it, a deed, or a door to a
 * further room.
 */
public final class Tune extends Activity {

    private static final int ROOM = 0;
    private static final int SWITCH = 1;
    private static final int CHOICE = 2;
    private static final int DEED = 3;

    /** One line of the settings. */
    private static final class Line {
        final int kind;
        final int glyph;
        final String title;
        final String about;
        /** For a door, the room it opens; for a deed, which deed. */
        final int room;
        final String key;
        final int fallback;
        final String[] names;
        final int[] values;

        Line(int kind, int glyph, String title, String about, int room, String key,
             int fallback, String[] names, int[] values) {
            this.kind = kind;
            this.glyph = glyph;
            this.title = title;
            this.about = about;
            this.room = room;
            this.key = key;
            this.fallback = fallback;
            this.names = names;
            this.values = values;
        }
    }

    private static Line door(int glyph, String title, String about, int room) {
        return new Line(ROOM, glyph, title, about, room, null, 0, null, null);
    }

    private static Line toggle(String title, String about, String key, boolean fallback) {
        return new Line(SWITCH, -1, title, about, 0, key, fallback ? 1 : 0, null, null);
    }

    private static Line choice(String title, String about, String key, int fallback,
                               String[] names, int[] values) {
        return new Line(CHOICE, -1, title, about, 0, key, fallback, names, values);
    }

    private static Line deed(int glyph, String title, String about, int which) {
        return new Line(DEED, glyph, title, about, which, null, 0, null, null);
    }

    private static final int ROOT = 0;
    private static final int DESK = 1;
    private static final int LIST = 2;
    private static final int LOOK = 3;
    private static final int HANDS = 4;
    private static final int BACKUP = 5;
    private static final int LANGUAGE = 6;
    private static final int OTHER = 7;
    private static final int HIDDEN = 8;
    private static final int ICONS = 9;
    private static final int CLOCK = 10;
    private static final int LOOKS = 11;
    private static final int FONTS = 12;
    private static final int LISTGROUND = 13;

    private static final int RESTART = 1;
    private static final int RESET = 2;
    private static final int DEFAULT = 3;
    private static final int COLOUR = 4;
    private static final int PLACE = 5;
    private static final int ABOUT = 6;
    private static final int HELP = 7;
    private static final int LAPSE = 8;
    /** What any gesture may be given, by its name. */
    private static final String[] DEED_NAMES = {"Nothing", "All apps", "Search apps", "Notifications",
        "Quick settings", "Recent and new", "Home screen", "Lock the phone", "Ellipse settings", "Open an app\u2026"};
    private static final int[] DEEDS = {Keep.DO_NOTHING, Keep.DO_LIST, Keep.DO_SEARCH, Keep.DO_NOTICES, Keep.DO_QUICK,
        Keep.DO_FRESH, Keep.DO_HOME, Keep.DO_LOCK, Keep.DO_SETTINGS, Keep.DO_APP};
    /** The room where an app is chosen for a gesture, and the gesture it is chosen for. */
    private static final int APPS = 14;
    /** The ground factory's room, the recipe on it, and the throws of the dice in this sitting. */
    private static final int GROUNDS = 16;
    private static final int GROUND_FINE = 17;
    private Ground ground;
    private android.widget.ImageView groundView;
    private int groundDrawn;
    private static final java.util.List<String> thrown = new java.util.ArrayList<>();
    private static int throwAt = -1;
    private static final java.util.Random dice = new java.util.Random();
    private static String choosingFor;

    private static final String SEARCH = "Search settings";
    private static final String RESTART_LINE = "Restart launcher";
    private static final String AGAIN = "Tap again to reset everything";
    private static final String ALREADY = "Done: the Home button opens Ellipse";

    private static final String[] GRIDS = {"3 \u00D7 4", "4 \u00D7 5", "4 \u00D7 6", "5 \u00D7 5", "5 \u00D7 6",
        "5 \u00D7 8", "5 \u00D7 9", "5 \u00D7 11", "6 \u00D7 7"};
    private static final int[] GRID_VALUES = {34, 45, 46, 55, 56, 58, 59, 511, 67};
    private static final String[] EDGE_NAMES = {"Margins", "Widgets to the edges", "Everything to the edges"};
    private static final int[] EDGE_VALUES = {Keep.EDGES_MARGINS, Keep.EDGES_WIDGETS, Keep.EDGES_ALL};

    private static final Line[] ROOMS = {
        door(Glyph.DESK, "Desktop", "Dock, grid, scrolling, page points, new apps", DESK),
        door(Glyph.LIST, "All apps", "Grid, lines or pages, hidden apps", LIST),
        door(Glyph.LOOK, "Look", "Colour, icons, the clock, notification dots", LOOK),
        door(Glyph.HANDS, "Gestures", "Up, down, Back and Home on the home screen", HANDS),
        door(Glyph.BACKUP, "Backup and restore", "Keep the set-out, bring it back, bring one in", BACKUP),
        door(Glyph.LANGUAGE, "Languages", "Language modules for the words of the home screen", LANGUAGE),
        door(Glyph.OTHER, "Other", "Start the home screen afresh, or set it back as it was", OTHER)
    };

    private static Line[] inside(int room) {
        switch (room) {
            case DESK:
                return new Line[] {
                    toggle("Dock", "The bar of four at the foot, with the way into every app",
                        Keep.DOCK, true),
                    choice("Desktop grid", "Columns and rows of every screen", Keep.DESK_GRID, 45,
                        GRIDS, GRID_VALUES),
                    toggle("Endless scrolling", "Past the last screen comes the first again",
                        Keep.DESK_ENDLESS, false),
                    toggle("Page indicator", "Points under the screens, the home one ringed",
                        Keep.DOTS, true),
                    toggle("Moving wallpaper", "The wallpaper goes a little way along with the screens",
                        Keep.WALL_MOVES, true),
                    toggle("Add new apps", "An app put on the phone is set down on a free place",
                        Keep.AUTO_ADD, false),
                    toggle("Half steps", "Icons and widgets set down half a place along or down, "
                        + "for a closer set-out; off, each goes back to the nearest whole place",
                        Keep.HALF_STEPS, false),
                    toggle("Overlap", "Icons and widgets may be set down over one another, the last "
                        + "set down on top; best with half steps", Keep.OVERLAP, false),
                    choice("Place shape", "The screen's share: the grid's rows fill the screen. Square, wide "
                        + "or tall: each place so shaped, and as many rows as fit", Keep.CELL_SHAPE,
                        Keep.SHAPE_SCREEN, new String[] {"Screen's share", "Square", "Wide", "Tall"},
                        new int[] {Keep.SHAPE_SCREEN, Keep.SHAPE_SQUARE, Keep.SHAPE_WIDE, Keep.SHAPE_TALL}),
                    choice("Row height", "How high each row stands: lower, the rows close up toward the top "
                        + "and the gaps between them narrow", Keep.ROW_HEIGHT, 100,
                        new String[] {"100%", "90%", "80%", "70%", "60%"}, new int[] {100, 90, 80, 70, 60}),
                    choice("Edges", "How the grid meets the screen's edges: within its margins; widgets and "
                        + "the clock past them; or no margins at all, screen against screen", Keep.EDGES,
                        Keep.EDGES_MARGINS, EDGE_NAMES, EDGE_VALUES),
                    toggle("Hide the status bar", "The home screen takes the top of the screen; a swipe down "
                        + "from the edge shows the bar for a moment", Keep.HIDE_STATUS, false),
                    toggle("Hide the navigation bar", "The home screen takes the foot of the screen; a swipe up "
                        + "from the edge shows the bar for a moment", Keep.HIDE_NAVIGATION, false),
                    choice("Widget shelf", "How the widgets to add are laid out", Keep.SHELF_VIEW,
                        Keep.LINES, new String[] {"Lines", "Grid"}, new int[] {Keep.LINES, Keep.PAGES}),
                    choice("Shortcut makers", "How the shortcuts to add are laid out", Keep.MAKERS_VIEW,
                        Keep.LINES, new String[] {"Lines", "Grid"}, new int[] {Keep.LINES, Keep.PAGES})
                };
            case LIST:
                return new Line[] {
                    choice("Layout", "Lines going down, or pages going across", Keep.VIEW_KEY,
                        Keep.LINES, new String[] {"Lines", "Pages"}, new int[] {Keep.LINES, Keep.PAGES}),
                    door(Glyph.LOOK, "Background", "The list's ground: the theme's, or a colour of your own",
                        LISTGROUND),
                    choice("Categories", "Tabs across the list: none; kinds you name and fill by hand; or "
                        + "the kinds apps say they are", Keep.KINDS, Kinds.NONE,
                        new String[] {"None", "By hand", "Automatic"},
                        new int[] {Kinds.NONE, Kinds.BY_HAND, Kinds.BY_THEMSELVES}),
                    choice("Page grid", "Columns and rows of a page", Keep.LIST_GRID, 45,
                        GRIDS, GRID_VALUES),
                    toggle("Endless scrolling", "Past the last page comes the first again",
                        Keep.LIST_ENDLESS, false),
                    toggle("Page indicator", "Points under the pages",
                        Keep.LIST_DOTS, true),
                    door(-1, "Hidden apps", "Left out of the list and its search", HIDDEN)
                };
            case LOOK:
                return new Line[] {
                    door(Glyph.LOOK, "Presets", "Ready looks to put on at a touch, and your own", LOOKS),
                    door(Glyph.LOOK, "Typeface", "The letters of every name and every word here", FONTS),
                    door(Glyph.LOOK, "Wallpaper", "A ground drawn from layers of light, texture and ornament",
                        GROUNDS),
                    choice("Theme", "Dark surfaces, light ones, or as the phone is set", Keep.THEME,
                        Keep.THEME_DARK, new String[] {"Dark", "Light", "As the phone"},
                        new int[] {Keep.THEME_DARK, Keep.THEME_LIGHT, Keep.THEME_PHONE}),
                    deed(Glyph.LOOK, "Colour and text",
                        "The accent, the ground, how solid the cards are, the size of words", COLOUR),
                    door(Glyph.ICONS, "Icons", "The outline every icon is cut to", ICONS),
                    deed(Glyph.DESK, "The weather's place", "Where the clock's weather is for", PLACE),
                    toggle("Clock", "The home screen's own clock across the top of the home screen",
                        Keep.CLOCK, true),
                    door(Glyph.CLOCK, "Clock face", "The first clock, or another", CLOCK),
                    toggle("Weather on the clock", "The warmth and the sky where the phone is",
                        Keep.WEATHER, true),
                    toggle("Headphones on the clock", "Their charge, while they are near and tell it",
                        Keep.EARS, true),
                    toggle("Notification dots", "A point on the icon of an app with something to say",
                        Keep.DOTS_ON, false)
                };
            case HANDS:
                return new Line[] {
                    choice("Swipe up", "On bare screens", Keep.ON_UP, Keep.DO_LIST, DEED_NAMES, DEEDS),
                    choice("Swipe down", "On bare screens", Keep.ON_DOWN, Keep.DO_NOTICES, DEED_NAMES, DEEDS),
                    choice("Two fingers up", "On bare screens", Keep.ON_TWO_UP, Keep.DO_NOTHING, DEED_NAMES, DEEDS),
                    choice("Two fingers down", "On bare screens", Keep.ON_TWO_DOWN, Keep.DO_NOTHING, DEED_NAMES,
                        DEEDS),
                    choice("Pinch", "Two fingers drawn together, on bare screens", Keep.ON_PINCH, Keep.DO_NOTHING,
                        DEED_NAMES, DEEDS),
                    choice("Double tap", "On the empty home screen", Keep.ON_DOUBLE, Keep.DO_LOCK, DEED_NAMES,
                        DEEDS),
                    choice("Back", "On bare screens", Keep.ON_BACK, Keep.DO_FRESH, DEED_NAMES, DEEDS),
                    choice("Home button", "On bare screens", Keep.ON_HOME, Keep.DO_HOME, DEED_NAMES, DEEDS),
                    choice("Lock with", "An accessibility service locks at once; as a keeper of the phone, some "
                        + "banks' apps do not mind it, but the first unlock may ask for the code",
                        Keep.LOCK_WAY, Keep.LOCK_SERVICE, new String[] {"Accessibility service", "Keeper of the phone"},
                        new int[] {Keep.LOCK_SERVICE, Keep.LOCK_KEEPER})
                };
            case BACKUP:
                return new Line[0];
            case LANGUAGE:
                return new Line[0];
            case OTHER:
                return new Line[] {
                    deed(Glyph.DESK, "Set as default home app",
                        "Make the phone's Home button open Ellipse", DEFAULT),
                    deed(Glyph.RESTART, "Restart launcher", "Close the home screen and open it again",
                        RESTART),
                    deed(Glyph.INFO, "Help", "How every part of the home screen works, in English", HELP),
                    deed(Glyph.INFO, "Last error", "Where and why the home screen last stopped, to copy and send",
                        LAPSE),
                    deed(Glyph.INFO, "About", "The version, the project, and where the weather comes from", ABOUT),
                    deed(Glyph.RESET, "Reset launcher",
                        "Forget everything set by hand and lay the screens out as on first start", RESET)
                };
            default:
                return new Line[0];
        }
    }

    private static String nameOf(int room) {
        for (Line line : ROOMS) {
            if (line.room == room) {
                return line.title;
            }
        }
        return room == HIDDEN ? "Hidden apps" : room == ICONS ? "Icons" : room == CLOCK ? "Clock face"
            : room == APPS ? "Open an app" : room == GROUNDS ? "Wallpaper" : room == GROUND_FINE ? "Fine tuning" : room == LOOKS ? "Presets" : room == FONTS ? "Typeface" : room == LISTGROUND ? "Background" : "";
    }

    private float density;
    private float scaled;
    private FrameLayout host;
    private LinearLayout root;
    private FrameLayout head;
    private Foot foot;
    private EditText field;
    private TextView heading;
    private ScrollView scroll;
    private LinearLayout rows;
    private FrameLayout window;
    private Sample sample;
    private Menu menu;
    private final List<Integer> path = new ArrayList<>();
    private long armed;
    /** The choice the menu stands open for, and the words showing its value. */
    private Line asking;
    private TextView askingValue;

    private int dp(float value) {
        return Math.round(value * density);
    }

    private int room() {
        return path.isEmpty() ? ROOT : path.get(path.size() - 1);
    }

    @Override
    protected void onCreate(Bundle saved) {
        Lapse.watch(this);
        super.onCreate(saved);
        Tone.read(this);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        build();
        Tone.dress(getWindow(), true);
        show(0);
    }

    /** The window onto the wallpaper, if a room keeps one open; the rest of the settings is the surface. */
    private View throughTo;
    /** The soft ground of the settings, as the pages wear it. */
    private final Glow glow = new Glow();

    private void build() {
        /* The settings paint their own ground, all but a window the icons'
           sample may open onto the wallpaper behind them. */
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        host = new FrameLayout(this) {
            private final android.graphics.Path hole = new android.graphics.Path();
            private final int[] at = new int[2];
            private final int[] mine = new int[2];

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                View through = throughTo;
                if (through != null && through.isShown() && through.getWidth() > 0) {
                    through.getLocationInWindow(at);
                    getLocationInWindow(mine);
                    float left = at[0] - mine[0];
                    float top = at[1] - mine[1];
                    hole.reset();
                    hole.addRoundRect(new android.graphics.RectF(left, top, left + through.getWidth(),
                        top + through.getHeight()), dp(30), dp(30), android.graphics.Path.Direction.CW);
                    canvas.save();
                    canvas.clipOutPath(hole);
                    glow.setBounds(0, 0, getWidth(), getHeight());
                    glow.draw(canvas);
                    canvas.restore();
                } else {
                    glow.setBounds(0, 0, getWidth(), getHeight());
                    glow.draw(canvas);
                }
            }
        };
        host.setWillNotDraw(false);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        host.addView(root, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        head = new FrameLayout(this);
        heading = new TextView(this);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_PX, 34f * scaled);
        heading.setTextColor(Tone.onSurface());
        heading.setPadding(dp(24), dp(20), dp(24), dp(12));
        heading.setVisibility(View.GONE);
        head.addView(heading);
        root.addView(head);

        /* A room that changes how things look keeps a window onto them at
           its head, which stays while the room's lines scroll under it. */
        window = new FrameLayout(this);
        window.setPadding(dp(16), dp(4), dp(16), dp(10));
        window.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                host.invalidate();
            }
        });
        window.setVisibility(View.GONE);
        root.addView(window);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(0, dp(4), 0, dp(120));
        scroll.addView(rows);
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        foot = new Foot(this, host, SEARCH, new Foot.Owner() {
            public Menu.Section[] sections() {
                return new Menu.Section[] {new Menu.Section(null,
                    new String[] {RESTART_LINE}, new int[] {RESTART})};
            }

            public void picked(int section, int key) {
                if (key == RESTART) {
                    restart();
                }
            }

            public void leave() {
                finish();
            }

            public void typed(String text) {
                /* Whatever room stands open, finding starts from all of them. */
                if (text.length() > 0 && !path.isEmpty()) {
                    path.clear();
                    show(0);
                    return;
                }
                if (room() == ROOT) {
                    fill();
                }
            }
        });
        root.addView(foot, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        field = foot.field();

        menu = new Menu(this, host, new Menu.Listener() {
            public void picked(int section, int key) {
                pick(key);
            }

            public void closing() {
            }
        });

        host.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int top;
                int bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                    top = bars.top;
                    bottom = Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
                } else {
                    top = insets.getSystemWindowInsetTop();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                head.setPadding(0, top, 0, 0);
                rows.setPadding(0, dp(4), 0, bottom + dp(112));
                foot.lift(bottom);
                return insets;
            }
        });
        setContentView(host);
        host.requestApplyInsets();
    }

    /** Shows the room at the top of the path; a room entered comes in from the side it lies on. */
    private void show(int from) {
        armed = 0L;
        menu.hide(false);
        boolean inRoom = room() != ROOT;
        heading.setVisibility(inRoom ? View.VISIBLE : View.GONE);
        /* In a room, the bar at the foot carries the way back out of it. */
        foot.back(inRoom ? new Runnable() {
            public void run() {
                onBackPressed();
            }
        } : null);
        heading.setText(Words.t(nameOf(room())));
        if (inRoom) {
            hideKeys();
        }
        fill();
        scroll.scrollTo(0, 0);
        if (from != 0) {
            scroll.setTranslationX(from * dp(48));
            scroll.setAlpha(0f);
            scroll.animate().translationX(0f).alpha(1f).setDuration(Pace.ARRIVE)
                .setInterpolator(Pace.EMPHASIS).start();
        }
    }

    /** The room's lines anew, every word in the chosen family. */
    private void fill() {
        /* The room is built anew, but every row that scrolls sideways, and
           the room itself, stay where the finger left them. */
        final java.util.List<Integer> across = new java.util.ArrayList<>();
        for (int i = 0; i < rows.getChildCount(); i++) {
            if (rows.getChildAt(i) instanceof android.widget.HorizontalScrollView) {
                across.add(rows.getChildAt(i).getScrollX());
            }
        }
        final int down = scroll.getScrollY();
        final boolean same = lastRoom == room();
        lastRoom = room();
        fillRoom();
        Style.apply(root);
        if (same) {
            scroll.post(new Runnable() {
                public void run() {
                    int k = 0;
                    for (int i = 0; i < rows.getChildCount() && k < across.size(); i++) {
                        if (rows.getChildAt(i) instanceof android.widget.HorizontalScrollView) {
                            rows.getChildAt(i).scrollTo(across.get(k++), 0);
                        }
                    }
                    scroll.scrollTo(0, down);
                }
            });
        }
    }

    private int lastRoom = -1;

    private void fillRoom() {
        rows.removeAllViews();
        window.setVisibility(room() == ICONS || room() == CLOCK || room() == LISTGROUND || room() == FONTS
            || room() == GROUNDS || room() == GROUND_FINE ? View.VISIBLE : View.GONE);
        window.setPadding(dp(16), dp(4), dp(16), dp(10));
        /* The wallpaper's rooms end with Done: the ground set on both screens. */
        foot.done(room() == GROUNDS || room() == GROUND_FINE ? new Runnable() {
            public void run() {
                setGround(0);
            }
        } : null);
        if (room() == HIDDEN) {
            fillHidden();
            return;
        }
        if (room() == ICONS) {
            fillIcons();
            return;
        }
        if (room() == BACKUP) {
            fillBackup();
            return;
        }
        if (room() == APPS) {
            fillApps();
            return;
        }
        if (room() == GROUNDS) {
            fillGrounds();
            return;
        }
        if (room() == GROUND_FINE) {
            fillGroundsFine();
            return;
        }
        if (room() == LOOKS) {
            fillLooks();
            return;
        }
        if (room() == LANGUAGE) {
            fillLanguages();
            return;
        }
        if (room() == FONTS) {
            fillFonts();
            return;
        }
        if (room() == LISTGROUND) {
            fillListGround();
            return;
        }
        if (room() == CLOCK) {
            fillClock();
            return;
        }
        List<Line> lines = new ArrayList<>();
        if (room() != ROOT) {
            for (Line line : inside(room())) {
                lines.add(line);
            }
        } else {
            String typed = Match.norm(field.getText().toString());
            if (typed.length() == 0) {
                for (Line line : ROOMS) {
                    lines.add(line);
                }
            } else {
                /* Found, a line is shown wherever it lives, rooms and what is in them alike. */
                List<Line> every = new ArrayList<>();
                for (Line line : ROOMS) {
                    every.add(line);
                    for (Line in : inside(line.room)) {
                        every.add(in);
                    }
                }
                for (Line line : every) {
                    if (Match.rank(Match.norm(line.title), typed) != Match.NONE
                        || Match.norm(line.about).contains(typed)) {
                        lines.add(line);
                    }
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            View row = row(lines.get(i));
            rows.addView(row);
            arrive(row, i);
        }
    }

    private void arrive(View row, int i) {
        row.setAlpha(0f);
        row.setTranslationY(dp(10));
        row.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP * Math.min(i, 8))
            .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
    }

    private View row(final Line line) {
        final LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.HORIZONTAL);
        made.setGravity(Gravity.CENTER_VERTICAL);
        boolean drawn = line.glyph >= 0;
        made.setPadding(dp(drawn ? 28 : 24), dp(22), dp(24), dp(22));
        made.setBackground(Tone.touch(null, 0f));
        if (drawn) {
            Glyph drawing = new Glyph(this, line.glyph, dp(28));
            drawing.tint(Tone.primary());
            made.addView(drawing);
        }
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(drawn ? dp(28) : 0, 0, dp(12), 0);
        TextView title = new TextView(this);
        title.setText(Words.t(line.title));
        title.setTextColor(Tone.onSurface());
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * scaled);
        words.addView(title);
        final TextView about = new TextView(this);
        String said = line.kind == DEED && line.room == DEFAULT && isHome() ? ALREADY
            : line.kind == DEED && line.room == PLACE && Keep.here(this)[0].length() > 0 ? Keep.here(this)[0]
            : line.about;
        about.setText(Words.t(said));
        about.setTextColor(Tone.faint());
        about.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        about.setPadding(0, dp(2), 0, 0);
        words.addView(about);
        made.addView(words, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (line.kind == SWITCH) {
            final Toggle toggle = new Toggle(this, Keep.flag(this, line.key, line.fallback == 1));
            made.addView(toggle);
            made.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggle.set(!toggle.on());
                    Keep.saveFlag(Tune.this, line.key, toggle.on());
                    /* A new ground every night replaces a wallpaper not set here: asked once, before. */
                    if (Keep.TURN.equals(line.key) && toggle.on() && !Picture.replaceable(Tune.this)) {
                        Ask.tell(host, "Replace the wallpaper?", WARN_REPLACE, "Replace", new Runnable() {
                            public void run() {
                                Keep.saveFlag(Tune.this, Keep.WALL_WARNED, true);
                            }
                        });
                    }
                    if (sample != null && room() == ICONS) {
                        Style.read(Tune.this);
                        sample.show();
                    }
                    /* Dots need the phone's leave to know of notifications. */
                    if (Keep.DOTS_ON.equals(line.key) && toggle.on() && !Notices.on()) {
                        openSafely(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                }
            });
        } else if (line.kind == CHOICE) {
            /* The value stands under the title, never beside it: the row keeps its whole width for words.
               Two or three short things to choose from are all seen at once, one pill across. */
            if (segmented(line)) {
                words.addView(segments(line), 1);
            } else {
                LinearLayout pill = new LinearLayout(this);
                pill.setGravity(Gravity.CENTER_VERTICAL);
                pill.setPadding(dp(14), dp(7), dp(8), dp(7));
                pill.setBackground(Tone.box(Tone.primaryContainer(), dp(18), 0f));
                final TextView value = new TextView(this);
                value.setText(valueOf(line));
                value.setTextColor(Tone.onPrimaryContainer());
                value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
                value.setSingleLine(true);
                value.setEllipsize(android.text.TextUtils.TruncateAt.END);
                pill.addView(value, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                Glyph down = new Glyph(this, Glyph.CHEVRON, dp(18));
                down.tint(Tone.onPrimaryContainer());
                LinearLayout.LayoutParams downAt = new LinearLayout.LayoutParams(dp(18), dp(18));
                downAt.setMargins(dp(4), 0, 0, 0);
                pill.addView(down, downAt);
                LinearLayout.LayoutParams pillAt = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                pillAt.setMargins(0, dp(8), 0, dp(4));
                words.addView(pill, 1, pillAt);
                made.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ask(line, made, value);
                    }
                });
            }
        } else if (line.kind == ROOM) {
            made.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    field.setText("");
                    path.add(line.room);
                    show(1);
                }
            });
        } else if (line.kind == DEED) {
            made.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act(line, about);
                }
            });
        }
        return made;
    }

    /** Whether a choice is shown as one pill of all its values: two or three, each short, none leading on. */
    private boolean segmented(Line line) {
        if (line.names == null || line.names.length < 2 || line.names.length > 3) {
            return false;
        }
        for (int i = 0; i < line.names.length; i++) {
            if (line.values[i] == Keep.DO_APP || Words.t(line.names[i]).length() > 26) {
                return false;
            }
        }
        return true;
    }

    /** A choice's values side by side in one outlined pill; the one chosen filled and ticked. */
    private View segments(final Line line) {
        final LinearLayout band = new LinearLayout(this);
        band.setPadding(dp(3), dp(3), dp(3), dp(3));
        band.setBackground(Tone.box(0x00000000, dp(24), dp(1f)));
        final int now = current(line);
        for (int i = 0; i < line.names.length; i++) {
            final int value = line.values[i];
            boolean on = value == now;
            TextView one = new TextView(this);
            one.setText(on ? "\u2713  " + Words.t(line.names[i]) : Words.t(line.names[i]));
            one.setGravity(Gravity.CENTER);
            one.setMaxLines(2);
            one.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
            one.setTextColor(on ? Tone.onPrimaryContainer() : Tone.faint());
            if (on) {
                one.setTypeface(Style.bold());
                one.setBackground(Tone.box(Tone.primaryContainer(), dp(21), 0f));
            } else {
                one.setBackground(Tone.touch(null, dp(21)));
            }
            one.setPadding(dp(8), dp(9), dp(8), dp(9));
            one.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (value == current(line)) {
                        return;
                    }
                    asking = line;
                    askingValue = null;
                    pick(value);
                    ViewGroup holder = (ViewGroup) band.getParent();
                    int at = holder.indexOfChild(band);
                    holder.removeViewAt(at);
                    holder.addView(segments(line), at);
                }
            });
            band.addView(one, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        at.setMargins(0, dp(10), 0, dp(6));
        band.setLayoutParams(at);
        return band;
    }

    private int current(Line line) {
        return Keep.number(this, line.key, line.fallback);
    }

    private String valueOf(Line line) {
        if (line.key != null && line.key.startsWith("on_") && Keep.number(this, line.key, line.fallback) == Keep.DO_APP) {
            Apps.Door door = new Apps(this).door(Keep.word(this, "app." + line.key) == null ? ""
                : Keep.word(this, "app." + line.key));
            if (door != null) {
                return String.valueOf(door.label);
            }
        }
        int now = current(line);
        for (int i = 0; i < line.values.length; i++) {
            if (line.values[i] == now) {
                return Words.t(line.names[i]);
            }
        }
        return Words.t(line.names[0]);
    }

    /** A choice opens the one menu beside the value it shows, the value it holds marked. */
    private void ask(Line line, View row, TextView value) {
        asking = line;
        askingValue = value;
        Menu.Section section = new Menu.Section(null, line.names, line.values);
        section.chosen = current(line);
        int[] at = new int[2];
        int[] hostAt = new int[2];
        value.getLocationOnScreen(at);
        host.getLocationOnScreen(hostAt);
        float x = at[0] - hostAt[0] + value.getWidth() / 2f;
        float y = at[1] - hostAt[1] + value.getHeight() / 2f;
        menu.show(new Menu.Section[] {section}, x, y, dp(20));
    }

    private void pick(int value) {
        if (asking == null) {
            return;
        }
        if (menu.shown()) {
            menu.choose(0, value);
        }
        Keep.saveNumber(this, asking.key, value);
        /* The lock needs the phone's leave, as an accessibility service, when it is to lock that way. */
        if (value == Keep.DO_LOCK && Keep.number(this, Keep.LOCK_WAY, Keep.LOCK_SERVICE) == Keep.LOCK_SERVICE
            && !Latch.ready()) {
            openSafely(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        /* An app for a gesture: the apps are shown to choose it from. */
        if (value == Keep.DO_APP && asking.key.startsWith("on_")) {
            choosingFor = asking.key;
            menu.hide(true);
            path.add(APPS);
            show(1);
            return;
        }
        if (askingValue != null) {
            askingValue.setText(valueOf(asking));
        }
        if (Keep.THEME.equals(asking.key)) {
            /* A new ground for everything: the settings are made again in it. */
            Tone.read(this);
            host.postDelayed(new Runnable() {
                public void run() {
                    recreate();
                }
            }, Pace.ARRIVE);
            return;
        }
        if (room() == CLOCK) {
            showClock();
        }
        if (room() == ICONS && sample != null) {
            Style.read(this);
            sample.show();
        }
        host.postDelayed(new Runnable() {
            public void run() {
                menu.hide(true);
            }
        }, Pace.STEP * 3);
    }

    // ------------------------------------------------------------- the ground factory

    /**
     * The ground factory: the ground drawn small at the head of the room;
     * the dice, back and forth through what they threw; the ready grounds;
     * each layer's kind as a row of words and its colours and strengths as
     * sliders, seen at once; another draw of the same; and the ground set
     * as the wallpaper, or kept among the owner's own.
     */
    private void fillGrounds() {
        groundWindow(true);
        caption("READY");
        rows.addView(flow(Ground.READY, -1, new Chosen() {
            public void chosen(int which) {
                ground = Ground.ready(which);
                groundChanged(true);
            }
        }));
        caption("MINE");
        final java.util.List<String[]> mine = mineGrounds();
        String[] names = new String[mine.size() + 1];
        for (int i = 0; i < mine.size(); i++) {
            names[i] = mine.get(i)[0];
        }
        names[mine.size()] = "+ " + Words.t("Keep this ground");
        Flow kept = flow(names, -1, new Chosen() {
            public void chosen(int which) {
                if (which < mine.size()) {
                    ground = Ground.of(mine.get(which)[1]);
                    groundChanged(true);
                    return;
                }
                Ask.show(host, "Name this ground", Words.f("Ground %1", mine.size() + 1), new Ask.Answer() {
                    public void answered(String text) {
                        String name = text == null ? "" : text.trim().replace("\t", " ").replace("\n", " ");
                        if (!name.isEmpty()) {
                            mine.add(new String[] {name, ground.words()});
                            keepMineGrounds(mine);
                            fill();
                        }
                    }
                });
            }
        });
        /* A kept ground is forgotten by holding it. */
        for (int i = 0; i < mine.size(); i++) {
            final int which = i;
            kept.getChildAt(i).setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    Ask.tell(host, mine.get(which)[0], Words.t("Forget this ground?"), "Forget", new Runnable() {
                        public void run() {
                            mine.remove(which);
                            keepMineGrounds(mine);
                            fill();
                        }
                    });
                    return true;
                }
            });
        }
        rows.addView(kept);
        caption("YOUR PICTURE");
        rows.addView(deed("Wallpaper from a picture", new Runnable() {
            public void run() {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                try {
                    startActivityForResult(pick, READ_PICTURE);
                } catch (android.content.ActivityNotFoundException none) {
                    said("No way to choose a picture was found on the phone");
                }
            }
        }));
        if (Picture.kept(this)) {
            rows.addView(deed("Your picture again", new Runnable() {
                public void run() {
                    wearPicture();
                }
            }));
        }
        note("A picture of your own is kept by Ellipse and carried in copies, so a ground set later never "
            + "takes it away for good.");
        caption("WALLPAPER");
        rows.addView(row(toggle("On the lock screen too", "Off, the lock screen keeps the wallpaper it has",
            Keep.GROUND_LOCK, true)));
        rows.addView(row(toggle("A new one while the phone sleeps", "When the screen has been dark a while, a new "
            + "ground is drawn and set, to wake to", Keep.TURN, false)));
        rows.addView(row(choice("After", "How long the screen is to be dark first", Keep.TURN_AFTER, 60,
            new String[] {"15 minutes", "30 minutes", "An hour", "Three hours", "Eight hours"},
            new int[] {15, 30, 60, 180, 480})));
        rows.addView(row(choice("From", "Where the new ground comes from", Keep.TURN_FROM, Turn.DICE,
            new String[] {"The dice", "The ready ones", "My own"}, new int[] {Turn.DICE, Turn.READY, Turn.MINE})));
        if (Keep.flag(this, Keep.TURN, false)) {
            note(nightWitness());
        }
        rows.addView(row(door(Glyph.BRUSH, "Fine tuning", "Each layer's kind, colour, strength and scale",
            GROUND_FINE)));
    }

    /** What the last night did: when the wake was set, whether it came, and whether a ground was set. */
    private String nightWitness() {
        long asked = Keep.clock(this, Turn.ASKED);
        long came = Keep.clock(this, Turn.CAME);
        java.text.DateFormat at = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,
            java.text.DateFormat.SHORT);
        if (came > 0 && came >= asked) {
            return Words.f(Keep.flag(this, Turn.DONE, false) ? "The last new ground came %1."
                : "The last wake came %1, but no ground could be set.", at.format(new java.util.Date(came)));
        }
        if (asked > 0) {
            return Words.f("A wake was set %1 and has not come yet: the screen came on first, or the phone "
                + "held it back.", at.format(new java.util.Date(asked)));
        }
        return Words.t("No night has asked for a new ground yet.");
    }

    /** The ground's layers one by one, for whoever wants more than the dice. */
    private void fillGroundsFine() {
        if (ground == null) {
            ground = Ground.kept(this);
        }
        groundWindow(false);
        caption("LIGHT");
        rows.addView(flow(Ground.LIGHTS, ground.light, new Chosen() {
            public void chosen(int which) {
                ground.light = which;
                groundChanged(true);
            }
        }));
        if (ground.light != Ground.LIGHT_NONE) {
            groundSlider("Hue", 0, 360, ground.lightHue, "\u00B0", 0);
            groundSlider("Second hue", 0, 360, ground.lightHue2, "\u00B0", 1);
            groundSlider("Strength", 0, 100, ground.lightK, "%", 2);
        }
        caption("TEXTURE");
        rows.addView(flow(Ground.TEXTURES, ground.texture, new Chosen() {
            public void chosen(int which) {
                ground.texture = which;
                groundChanged(true);
            }
        }));
        groundSlider("Hue", 0, 360, ground.hue, "\u00B0", 3);
        groundSlider("Saturation", 0, 100, ground.sat, "%", 4);
        groundSlider("Brightness", 0, 100, ground.val, "%", 5);
        if (ground.texture != Ground.TEXTURE_NONE) {
            groundSlider("Strength", 0, 100, ground.textureK, "%", 6);
            groundSlider("Scale", 40, 250, ground.scale, "%", 7);
        }
        caption("ORNAMENT");
        rows.addView(flow(Ground.ORNAMENTS, ground.ornament, new Chosen() {
            public void chosen(int which) {
                ground.ornament = which;
                groundChanged(true);
            }
        }));
        if (ground.ornament != Ground.ORNAMENT_NONE) {
            groundSlider("Strength", 0, 100, ground.ornamentK, "%", 8);
        }
        caption("EDGES");
        groundSlider("Darkening", 0, 100, ground.vignette, "%", 9);
        rows.addView(deed("Another draw of the same", new Runnable() {
            public void run() {
                ground.seed = 1 + dice.nextInt(1 << 30);
                groundChanged(false);
            }
        }));
        caption("WALLPAPER");
        note("Done sets it on the home screen, and on the lock screen too if so chosen; here, on one only.");
        rows.addView(deed("Set on the home screen", new Runnable() {
            public void run() {
                setGround(android.app.WallpaperManager.FLAG_SYSTEM);
            }
        }));
        rows.addView(deed("Set on the lock screen", new Runnable() {
            public void run() {
                setGround(android.app.WallpaperManager.FLAG_LOCK);
            }
        }));
    }

    /**
     * The ground at the head of the room as a card, as wide as the settings
     * allow, with the owner's own icons standing on it as they will on the
     * screens; and, in the first of the two rooms, round buttons back and
     * forth through the throws, and the dice, larger, in the accent, half
     * over the card's foot.
     */
    private void groundWindow(boolean dice) {
        if (ground == null) {
            ground = Ground.kept(this);
        }
        window.removeAllViews();
        window.setPadding(dp(12), dp(4), dp(12), dp(4));
        FrameLayout stage = new FrameLayout(this);
        int tall = dp(dice ? 300 : 250);
        FrameLayout card = new FrameLayout(this);
        card.setClipToOutline(true);
        card.setOutlineProvider(new android.view.ViewOutlineProvider() {
            public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(30));
            }
        });
        groundView = new android.widget.ImageView(this);
        groundView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        card.addView(groundView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        /* The icons as they will stand on this ground. */
        sample = new Sample(this);
        sample.setBackground(null);
        card.addView(sample, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, android.view.Gravity.CENTER));
        stage.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tall));
        int under = 0;
        if (dice) {
            under = dp(62);
            LinearLayout steps = new LinearLayout(this);
            steps.addView(round(false), new LinearLayout.LayoutParams(dp(52), dp(52)));
            View gap = new View(this);
            steps.addView(gap, new LinearLayout.LayoutParams(dp(12), 1));
            steps.addView(round(true), new LinearLayout.LayoutParams(dp(52), dp(52)));
            FrameLayout.LayoutParams stepsAt = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.BOTTOM | android.view.Gravity.START);
            stepsAt.leftMargin = dp(16);
            stepsAt.bottomMargin = 0;
            stage.addView(steps, stepsAt);
            View roll = die();
            FrameLayout.LayoutParams rollAt = new FrameLayout.LayoutParams(dp(76), dp(76),
                android.view.Gravity.BOTTOM | android.view.Gravity.END);
            rollAt.rightMargin = dp(20);
            stage.addView(roll, rollAt);
        }
        window.addView(stage, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tall + under));
        drawGround();
    }

    /** A round tonal button with a fine chevron, back or forth through the throws of the dice. */
    private View round(final boolean forth) {
        View made = new View(this) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            private final android.graphics.Path chevron = new android.graphics.Path();

            @Override
            protected void onDraw(android.graphics.Canvas c) {
                float w = getWidth();
                float h = getHeight();
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(Tone.containerHigh());
                c.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, paint);
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
                paint.setColor(Tone.onSurface());
                float s = Math.min(w, h) * 0.14f;
                float dir = forth ? 1f : -1f;
                chevron.reset();
                chevron.moveTo(w / 2f - dir * s * 0.6f, h / 2f - s * 1.2f);
                chevron.lineTo(w / 2f + dir * s * 0.6f, h / 2f);
                chevron.lineTo(w / 2f - dir * s * 0.6f, h / 2f + s * 1.2f);
                c.drawPath(chevron, paint);
            }
        };
        made.setElevation(dp(2));
        made.setContentDescription(Words.t(forth ? "Forward" : "Back"));
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                if (!forth) {
                    if (throwAt > 0) {
                        throwAt--;
                        ground = Ground.of(thrown.get(throwAt));
                        groundChanged(false);
                    }
                } else if (throwAt >= 0 && throwAt < thrown.size() - 1) {
                    throwAt++;
                    ground = Ground.of(thrown.get(throwAt));
                    groundChanged(false);
                } else {
                    throwDice();
                }
            }
        });
        return made;
    }

    /** The dice: a round in the accent with a die drawn in its ink, five pips, turned a little. */
    private View die() {
        View made = new View(this) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(android.graphics.Canvas c) {
                float w = getWidth();
                float h = getHeight();
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(Tone.primary());
                c.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, paint);
                int ink = android.graphics.Color.luminance(Tone.primary()) > 0.4f ? 0xFF1C1A17 : 0xFFF5F1E8;
                float s = Math.min(w, h) * 0.2f;
                c.save();
                c.rotate(-12f, w / 2f, h / 2f);
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(ink);
                c.drawRoundRect(w / 2f - s, h / 2f - s, w / 2f + s, h / 2f + s, s * 0.35f, s * 0.35f, paint);
                paint.setStyle(android.graphics.Paint.Style.FILL);
                float p = s * 0.16f;
                float o = s * 0.52f;
                c.drawCircle(w / 2f, h / 2f, p, paint);
                c.drawCircle(w / 2f - o, h / 2f - o, p, paint);
                c.drawCircle(w / 2f + o, h / 2f - o, p, paint);
                c.drawCircle(w / 2f - o, h / 2f + o, p, paint);
                c.drawCircle(w / 2f + o, h / 2f + o, p, paint);
                c.restore();
            }
        };
        made.setElevation(dp(6));
        made.setContentDescription(Words.t("Random"));
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                v.animate().rotationBy(90f).setDuration(Pace.PRESS).start();
                throwDice();
            }
        });
        return made;
    }

    /** Words to choose one from, in lines that wrap; the chosen one in the accent. */
    private Flow flow(String[] names, int chosen, final Chosen then) {
        Flow made = new Flow(this, dp(8));
        made.setPadding(dp(20), dp(4), dp(20), dp(10));
        for (int i = 0; i < names.length; i++) {
            final int which = i;
            TextView one = chip(Words.t(names[i]), i == chosen, new Runnable() {
                public void run() {
                    then.chosen(which);
                }
            });
            one.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
            one.setPadding(dp(16), dp(9), dp(16), dp(9));
            one.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
            made.addView(one);
        }
        return made;
    }

    interface Chosen {
        void chosen(int which);
    }

    /** A row of words to choose one from, sideways; the chosen one in the accent. */
    private View chips(String[] names, int chosen, final Chosen then) {
        android.widget.HorizontalScrollView across = new android.widget.HorizontalScrollView(this);
        across.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(18), dp(4), dp(18), dp(8));
        for (int i = 0; i < names.length; i++) {
            final int which = i;
            row.addView(chip(Words.t(names[i]), i == chosen, new Runnable() {
                public void run() {
                    then.chosen(which);
                }
            }));
        }
        across.addView(row);
        return across;
    }

    private TextView chip(String word, boolean on, final Runnable does) {
        TextView made = new TextView(this);
        made.setText(word);
        made.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        made.setTextColor(on ? (android.graphics.Color.luminance(Tone.primary()) > 0.4f ? 0xFF1C1A17 : 0xFFF5F1E8)
            : Tone.onSurface());
        made.setPadding(dp(18), dp(10), dp(18), dp(10));
        made.setBackground(Tone.touch(Tone.box(on ? Tone.primary() : Tone.container(), dp(22), 0f), dp(22)));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        gap.setMargins(dp(4), 0, dp(4), 0);
        made.setLayoutParams(gap);
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                does.run();
            }
        });
        return made;
    }

    /** A slider of the recipe, by the place of its part: the ground drawn again as it moves. */
    private void groundSlider(String title, int least, int most, int now, final String unit, final int part) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout top = new LinearLayout(this);
        top.setPadding(dp(12), 0, dp(12), 0);
        TextView name = new TextView(this);
        name.setText(Words.t(title));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(now + unit);
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        made.addView(new Slide(this, least, most, now, new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + unit);
                int[] v = ground.values();
                int[] map = {1, 2, 3, 5, 6, 7, 8, 9, 11, 12};
                v[map[part]] = at;
                ground.take(v);
                ground.keep(Tune.this);
                drawGround();
            }
        }));
        rows.addView(made);
    }

    /** A new throw of the dice, after the last; the sitting remembers every throw. */
    private void throwDice() {
        Ground g = Ground.roll(dice);
        while (thrown.size() > throwAt + 1) {
            thrown.remove(thrown.size() - 1);
        }
        if (thrown.isEmpty() && ground != null) {
            thrown.add(ground.words());
        }
        thrown.add(g.words());
        throwAt = thrown.size() - 1;
        ground = g;
        groundChanged(true);
    }

    /** The recipe changed: kept, drawn again, and, where its rows change, the room built again. */
    private void groundChanged(boolean rebuild) {
        ground.keep(this);
        if (rebuild) {
            fill();
        } else {
            drawGround();
        }
    }

    /** The ground drawn small, away from the hand, and shown when it is ready if it is still the last asked. */
    private void drawGround() {
        final int asked = ++groundDrawn;
        final Ground g = Ground.of(ground.words());
        android.util.DisplayMetrics real = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(real);
        /* Drawn large enough to fill the screen's width without softening much. */
        final int w = 720;
        final int h = Math.round(w * real.heightPixels / (float) real.widthPixels);
        new Thread(new Runnable() {
            public void run() {
                final android.graphics.Bitmap made = g.draw(w, h);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (asked == groundDrawn && groundView != null) {
                            groundView.setImageBitmap(made);
                        }
                    }
                });
            }
        }).start();
    }

    /** The ground drawn at the screen's own size and set as the wallpaper where asked. */
    private static final String WARN_REPLACE = "The wallpaper the phone has now is replaced, and Ellipse "
        + "cannot keep it for you, for the phone does not let it be read. If it is a picture of your "
        + "own, bring it in first with Wallpaper from a picture: Ellipse keeps it, sets it again at a "
        + "touch and carries it in copies.";

    private void setGround(final int where) {
        if (!Picture.replaceable(this)) {
            /* The wallpaper the phone has may be the owner's own picture, and the phone does not let it
               be read to be kept: before it is replaced the first time, the owner is asked. */
            Ask.tell(host, "Replace the wallpaper?", WARN_REPLACE, "Replace", new Runnable() {
                    public void run() {
                        Keep.saveFlag(Tune.this, Keep.WALL_WARNED, true);
                        setGround(where);
                    }
                });
            return;
        }
        final Ground g = Ground.of(ground.words());
        if (where == 0) {
            /* Done: on the home screen, and on the lock screen unless the owner keeps it. */
            said("Drawing the wallpaper");
            new Thread(new Runnable() {
                public void run() {
                    boolean set;
                    try {
                        Turn.set(Tune.this, g, true);
                        set = true;
                    } catch (Exception | OutOfMemoryError failed) {
                        set = false;
                    }
                    final boolean done = set;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            said(done ? "The wallpaper is set" : "The wallpaper could not be set");
                        }
                    });
                }
            }).start();
            return;
        }
        android.util.DisplayMetrics real = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(real);
        /* With the wallpaper moving along with the screens, it is drawn wider than the screen, for there to
           be somewhere to move; however many screens there are, it goes the same width across all of them. */
        final boolean moves = Keep.flag(this, Keep.WALL_MOVES, true) && (where
            & android.app.WallpaperManager.FLAG_SYSTEM) != 0;
        final int w = moves ? Math.round(real.widthPixels * 1.5f) : real.widthPixels;
        final int h = real.heightPixels;
        said("Drawing the wallpaper");
        new Thread(new Runnable() {
            public void run() {
                boolean set;
                try {
                    android.graphics.Bitmap made = g.draw(w, h);
                    Keep.saveClock(Tune.this, Keep.GROUND_SET_AT, System.currentTimeMillis());
                    int id = android.app.WallpaperManager.getInstance(Tune.this).setBitmap(made, null, true, where);
                    Keep.saveFlag(Tune.this, Keep.GROUND_WORN, true);
                    if ((where & android.app.WallpaperManager.FLAG_SYSTEM) != 0) {
                        Keep.saveNumber(Tune.this, Keep.GROUND_WALL, id);
                        Keep.saveFlag(Tune.this, Keep.PICTURE_WORN, false);
                    }
                    set = true;
                } catch (Exception | OutOfMemoryError failed) {
                    set = false;
                }
                final boolean done = set;
                runOnUiThread(new Runnable() {
                    public void run() {
                        said(done ? "The wallpaper is set" : "The wallpaper could not be set");
                    }
                });
            }
        }).start();
    }

    private java.util.List<String[]> mineGrounds() {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        String kept = Keep.word(this, "grounds_mine");
        if (kept != null) {
            for (String line : kept.split("\n")) {
                int cut = line.indexOf('\t');
                if (cut > 0) {
                    out.add(new String[] {line.substring(0, cut), line.substring(cut + 1)});
                }
            }
        }
        return out;
    }

    private void keepMineGrounds(java.util.List<String[]> mine) {
        StringBuilder b = new StringBuilder();
        for (String[] one : mine) {
            b.append(one[0]).append('\t').append(one[1]).append('\n');
        }
        Keep.saveWord(this, "grounds_mine", b.toString());
    }

    /** Every app, to choose the one a gesture opens. */
    private void fillApps() {
        final String gesture = choosingFor;
        if (gesture == null) {
            return;
        }
        for (final Apps.Door door : new Apps(this).all(Keep.BY_NAME)) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(android.view.Gravity.CENTER_VERTICAL);
            line.setPadding(dp(24), dp(8), dp(24), dp(8));
            line.setBackground(Tone.touch(null, dp(16)));
            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setImageDrawable(door.icon());
            line.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
            TextView name = new TextView(this);
            name.setText(door.label);
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
            name.setTextColor(Tone.onSurface());
            name.setPadding(dp(16), 0, 0, 0);
            line.addView(name);
            line.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    Keep.saveWord(Tune.this, "app." + gesture, door.token());
                    Keep.saveNumber(Tune.this, gesture, Keep.DO_APP);
                    choosingFor = null;
                    onBackPressed();
                }
            });
            rows.addView(line);
        }
    }

    /** Every app with a switch beside it: on, and it is left out of the list. */
    private void fillHidden() {
        Set<String> hidden = Keep.hidden(this);
        List<Apps.Door> doors = new Apps(this).all(Keep.BY_NAME);
        for (int i = 0; i < doors.size(); i++) {
            final Apps.Door door = doors.get(i);
            LinearLayout made = new LinearLayout(this);
            made.setOrientation(LinearLayout.HORIZONTAL);
            made.setGravity(Gravity.CENTER_VERTICAL);
            made.setPadding(dp(24), dp(10), dp(24), dp(10));
            made.setBackground(Tone.touch(null, 0f));
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(door.icon());
            made.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
            TextView name = new TextView(this);
            name.setText(door.label);
            name.setTextColor(Tone.onSurface());
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
            name.setSingleLine(true);
            name.setPadding(dp(20), 0, dp(12), 0);
            made.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            final Toggle toggle = new Toggle(this, hidden.contains(door.token()));
            made.addView(toggle);
            made.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggle.set(!toggle.on());
                    Keep.hide(Tune.this, door.token(), toggle.on());
                }
            });
            rows.addView(made);
            if (i < 12) {
                arrive(made, i);
            }
        }
    }

    private void act(Line line, TextView about) {
        if (line.room == RESTART) {
            restart();
            return;
        }
        if (line.room == DEFAULT) {
            askToBeHome();
            return;
        }
        if (line.room == LAPSE) {
            String kept = Lapse.last(this);
            if (kept == null) {
                said("No error is kept");
                return;
            }
            /* The first lines shown; the whole of it copied, to paste into a message. */
            String[] lines = kept.split("\n");
            StringBuilder head = new StringBuilder();
            for (int i = 0; i < Math.min(6, lines.length); i++) {
                head.append(i > 0 ? "\n" : "").append(lines[i].trim());
            }
            final String all = kept;
            Ask.tell(host, "Last error", head.toString(), "Copy", new Runnable() {
                public void run() {
                    android.content.ClipboardManager clip = (android.content.ClipboardManager)
                        getSystemService(CLIPBOARD_SERVICE);
                    clip.setPrimaryClip(android.content.ClipData.newPlainText("Ellipse", all));
                    said("Copied: paste it into a message");
                }
            });
            return;
        }
        if (line.room == ABOUT || line.room == HELP) {
            startActivity(new Intent(this, Folio.class).putExtra(Folio.PAGE, line.room == ABOUT ? Folio.ABOUT
                : Folio.HELP));
            return;
        }
        if (line.room == COLOUR || line.room == PLACE) {
            startActivity(new Intent(this, Folio.class)
                .putExtra(Folio.PAGE, line.room == COLOUR ? Folio.LOOK : Folio.PLACE));
            return;
        }
        /* Setting everything back is asked twice: the first tap says what
           the second will do, and is forgotten after a few seconds. */
        long now = System.currentTimeMillis();
        if (now - armed > 4000L) {
            armed = now;
            about.setText(Words.t(AGAIN));
            about.setTextColor(Tone.primary());
            about.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
            return;
        }
        /* The widgets set on the screens are let go along with everything else. */
        try {
            new android.appwidget.AppWidgetHost(getApplicationContext(), Home.WIDGET_HOST).deleteHost();
        } catch (RuntimeException gone) {
            // There was nothing to let go.
        }
        Keep.reset(this);
        restart();
    }

    /**
     * Asks Android to make this the home screen: its own question where it
     * has one, and otherwise the page of the phone's settings where the
     * home screen is chosen.
     */
    private void askToBeHome() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.app.role.RoleManager roles = getSystemService(android.app.role.RoleManager.class);
                if (roles != null && roles.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)
                    && !roles.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    startActivityForResult(roles.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), 7);
                    return;
                }
            }
            startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS));
        } catch (RuntimeException none) {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        }
    }

    private void openSafely(Intent open) {
        try {
            startActivity(open);
        } catch (RuntimeException none) {
            // The page is not there on this phone.
        }
    }

    /** Whether Android opens this for Home now. */
    private boolean isHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo found = getPackageManager().resolveActivity(home,
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        return found != null && found.activityInfo != null
            && getPackageName().equals(found.activityInfo.packageName);
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(Home.sized(base));
    }

    @Override
    protected void onPause() {
        super.onPause();
        glow.stop();
    }

    @Override
    protected void onResume() {
        glow.start(host);
        super.onResume();
        if (Tone.read(this)) {
            recreate();
            return;
        }
        if (rows != null && path.size() > 0 && room() == OTHER) {
            fill();
        }
    }

    // ------------------------------------------------------------- icons

    private static final String[] SHAPES = Shape.NAMES;

    /**
     * The icons' room: the owner's own icons in the window at its head, and
     * under it every outline as a small tile of its own shape; the chosen
     * one wears the accent. A touch cuts the icons in the window at once.
     */
    private void fillIcons() {
        window.removeAllViews();
        sample = new Sample(this);
        window.addView(sample);
        throughTo = sample;
        sample.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                host.invalidate();
            }
        });
        TextView caption = new TextView(this);
        caption.setText(Words.t("SHAPE"));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setTextColor(Tone.faint());
        caption.setPadding(dp(24), dp(12), dp(24), dp(8));
        rows.addView(caption);
        android.widget.HorizontalScrollView across = new android.widget.HorizontalScrollView(this);
        across.setHorizontalScrollBarEnabled(false);
        across.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.setPadding(dp(18), 0, dp(18), 0);
        for (int i = 0; i < Shape.COUNT; i++) {
            if (i == Shape.RETIRED) {
                continue;
            }
            tiles.addView(tile(i));
        }
        across.addView(tiles);
        rows.addView(across);
        arrive(across, 1);

        caption("SIZE");
        slider("Icons", "How large the icons stand", Keep.ICON_SIZE, 80, 130);
        slider("Fill", "How much of its outline the picture fills", Keep.ICON_FILL, 85, 175);
        caption("NAMES");
        rows.addView(row(toggle("On the screens", "Names under the icons of the home screen",
            Keep.NAMES_SCREENS, true)));
        rows.addView(row(toggle("In the list", "Names under the icons of every app's pages",
            Keep.NAMES_LIST, true)));
        rows.addView(row(choice("Lines on the screens", "One line, or two for a long name", Keep.NAME_LINES_SCREENS, 1,
            new String[] {"One", "Two"}, new int[] {1, 2})));
        rows.addView(row(choice("Lines in the list", "One line, or two for a long name, in the list and the panels",
            Keep.NAME_LINES_LIST, 1, new String[] {"One", "Two"}, new int[] {1, 2})));
        rows.addView(swatches(Keep.NAME_COLOUR, Keep.number(this, Keep.NAME_COLOUR, Keep.NAME_LIGHT),
            new int[] {Keep.NAME_LIGHT, Keep.NAME_DARK, 0, 1, 2, 3, 4, 5, 6, 7},
            new String[] {"Light", "Dark", "Accent", "Orange", "Red", "Lilac", "Blue", "Green", "Sand", "White"},
            new Painter() {
                public void paint(android.graphics.Canvas c, float w, float h, int value) {
                    paintName(c, w, h, value);
                }
            }));
        caption("COLOUR");
        android.widget.HorizontalScrollView tints = new android.widget.HorizontalScrollView(this);
        tints.setHorizontalScrollBarEnabled(false);
        tints.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout tintChips = new LinearLayout(this);
        tintChips.setPadding(dp(18), 0, dp(18), dp(8));
        String[] tintNames = {"Their own", "In the accent", "Accent where drawn for it"};
        for (int i = 0; i < tintNames.length; i++) {
            tintChips.addView(chip(tintNames[i], Style.tint == i, Keep.ICON_TINT, i));
        }
        tints.addView(tintChips);
        rows.addView(tints);
        caption("WINDOW");
        rows.addView(swatches(Keep.WINDOW, Shape.window, windowValues(), Shape.WINDOW_NAMES, new Painter() {
            public void paint(android.graphics.Canvas c, float w, float h, int value) {
                paintWindow(c, w, h, value);
            }
        }));
        slider("Proportion", "Wider than tall, square, or taller than wide", Keep.TILE_ASPECT, 70, 135);
        caption("RIM");
        String[] rimNames = new String[Rim.NAMES.length + 1];
        int[] rimValues = new int[Rim.NAMES.length + 1];
        rimNames[0] = "None";
        rimValues[0] = Rim.NONE;
        for (int i = 0; i < Rim.NAMES.length; i++) {
            rimNames[i + 1] = Rim.NAMES[i];
            rimValues[i + 1] = i;
        }
        rows.addView(swatches(Keep.RIM_KIND, Rim.kind, rimValues, rimNames, new Painter() {
            public void paint(android.graphics.Canvas c, float w, float h, int value) {
                paintMaterial(c, w, h, value);
            }
        }));
        slider("Rim width", "From a thread to a frame", Keep.RIM_WIDTH, 1, 14);
        if (Rim.kind == Rim.GLASS) {
            slider("Glass tone", "From smoked dark to milk white", Keep.GLASS_TONE, 0, 100);
            slider("Clear", "How much of what is behind the glass shows", Keep.GLASS_CLEAR, 10, 90);
        }
        rows.addView(row(toggle("Glaze", "The curved light of glass across the top of every icon",
            Keep.GLAZE, false)));
        caption("ICON PACK");
        note("A pack's own picture stands for each app it knows; every other app is laid on its ground, "
            + "cut by its mask and glossed, if it has those. What a pack gives is not cut again.");
        String pack = Keep.word(this, Keep.ICON_PACK);
        java.util.Map<String, String> packs = Pack.installed(this);
        java.util.List<String> keys = new java.util.ArrayList<>();
        java.util.List<String> names = new java.util.ArrayList<>();
        keys.add("");
        names.add("None, the apps' own");
        for (java.util.Map.Entry<String, String> one : packs.entrySet()) {
            keys.add(one.getKey());
            names.add(one.getValue());
        }
        for (int i = 0; i < keys.size(); i++) {
            final String key = keys.get(i);
            boolean on = key.equals(pack == null ? "" : pack);
            TextView line = (TextView) deed((on ? "\u25CF  " : "\u25CB  ") + names.get(i), new Runnable() {
                public void run() {
                    Keep.saveWord(Tune.this, Keep.ICON_PACK, key);
                    Keep.saveWord(Tune.this, Keep.ICON_PACK_NAME, "");
                    Style.read(Tune.this);
                    fill();
                }
            });
            line.setTextColor(on ? Tone.primary() : Tone.onSurface());
            rows.addView(line);
        }
        if (pack != null && !pack.isEmpty() && !packs.containsKey(pack)) {
            /* Chosen, but not on the phone: it waits, and can be found in the store. */
            String named = Keep.word(this, Keep.ICON_PACK_NAME);
            final String wanted = pack;
            TextView waits = (TextView) deed("\u25CF  " + (named == null ? pack : named), new Runnable() {
                public void run() {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("market://details?id=" + wanted)));
                    } catch (RuntimeException none) {
                        said("There is no store on this phone");
                    }
                }
            });
            waits.setTextColor(Tone.primary());
            rows.addView(waits);
            note("Not on the phone. The icons take this pack once it is installed; a touch looks for it in the store.");
        } else if (packs.isEmpty()) {
            note("No pack of icons is on the phone.");
        }
        caption("WIDGET FRAMES");
        final View framed = new FramedSample(this);
        LinearLayout.LayoutParams sampleAt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120));
        sampleAt.setMargins(dp(24), dp(4), dp(24), dp(8));
        rows.addView(framed, sampleAt);
        rows.addView(swatches(Keep.WIDGET_FRAME, Keep.number(this, Keep.WIDGET_FRAME, Rim.NONE), rimValues, rimNames,
            new Painter() {
                public void paint(android.graphics.Canvas c, float w, float h, int value) {
                    paintMaterial(c, w, h, value);
                }
            }));
        frameSlider("Frame width", Keep.WIDGET_FRAME_WIDTH, 2, 20, 6, framed);
        frameSlider("Corners", Keep.WIDGET_FRAME_ROUND, 0, 48, 24, framed);
        rows.addView(row(toggle("Frame glaze", "The curved light of glass across every framed widget",
            Keep.WIDGET_GLAZE, false)));
        note("A widget may go without: hold it and choose Frame.");
        caption("FOLDERS");
        int[] layouts = new int[Stack.NAMES.length];
        for (int i = 0; i < layouts.length; i++) {
            layouts[i] = i;
        }
        rows.addView(swatches(Keep.FOLDER_FACE, Stack.layout, layouts, Stack.NAMES, new Painter() {
            public void paint(android.graphics.Canvas c, float w, float h, int value) {
                paintFolder(c, w, h, value);
            }
        }));
        rows.addView(row(toggle("Folder ground", "A container behind the small icons of a folder",
            Keep.FOLDER_GROUND, true)));
        final android.widget.HorizontalScrollView row = across;
        final LinearLayout all = tiles;
        across.post(new Runnable() {
            public void run() {
                View chosen = all.getChildAt(Math.max(0, Math.min(all.getChildCount() - 1, Shape.current)));
                int x = chosen.getLeft() - (row.getWidth() - chosen.getWidth()) / 2;
                row.scrollTo(Math.max(0, x), 0);
            }
        });
    }

    private void caption(String text) {
        TextView caption = new TextView(this);
        caption.setText(Words.t(text));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setTextColor(Tone.faint());
        caption.setPadding(dp(24), dp(22), dp(24), dp(8));
        rows.addView(caption);
    }

    /**
     * A named slider: the window at the head of the room follows it while
     * the finger moves, and what it shows is kept when the finger lifts.
     */
    private void slider(String title, String about, final String key, int least, int most) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        final TextView name = new TextView(this);
        name.setText(Words.t(title));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        final int fallback = Keep.RIM_WIDTH.equals(key) ? 3 : Keep.GLASS_TONE.equals(key) ? 0
            : Keep.GLASS_CLEAR.equals(key) ? 55 : 100;
        value.setText(Keep.number(this, key, fallback) + "%");
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        TextView said = new TextView(this);
        said.setText(Words.t(about));
        said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        said.setTextColor(Tone.faint());
        made.addView(said);
        made.addView(new Slide(this, least, most, Keep.number(this, key, fallback), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + "%");
                if (Keep.ICON_SIZE.equals(key)) {
                    Style.iconScale = at / 100f;
                } else if (Keep.ICON_FILL.equals(key)) {
                    Style.fill = at / 100f;
                } else if (Keep.RIM_WIDTH.equals(key)) {
                    Rim.width = at / 100f;
                } else if (Keep.TILE_ASPECT.equals(key)) {
                    Shape.aspect = at / 100f;
                } else if (Keep.GLASS_TONE.equals(key)) {
                    Rim.glassTone = at / 100f;
                } else if (Keep.GLASS_CLEAR.equals(key)) {
                    Rim.glassClear = at / 100f;
                } else {
                    Style.nameScale = at / 100f;
                }
                if (sample != null) {
                    sample.show();
                }
                if (done) {
                    Keep.saveNumber(Tune.this, key, at);
                }
            }
        }));
        rows.addView(made);
    }

    /** A choice among a few, as a pill; the chosen one wears the accent. */
    private View chip(String title, boolean on, final String key, final int value) {
        TextView chip = new TextView(this);
        chip.setText(Words.t(title));
        chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        chip.setTextColor(on ? Tone.onAccent() : Tone.onSurface());
        chip.setPadding(dp(18), dp(12), dp(18), dp(12));
        chip.setBackground(Tone.touch(Tone.box(on ? Tone.primary() : Tone.container(), dp(24), 0f), dp(24)));
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        at.rightMargin = dp(8);
        chip.setLayoutParams(at);
        chip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Keep.saveNumber(Tune.this, key, value);
                Style.read(Tune.this);
                final int y = scroll.getScrollY();
                fill();
                scroll.post(new Runnable() {
                    public void run() {
                        scroll.scrollTo(0, y);
                    }
                });
            }
        });
        return chip;
    }

    /** A family of words, written in itself; the chosen one wears the accent. */
    // ------------------------------------------------------------- the clock

    /**
     * The clock's room: the clock itself in the window at its head, as the
     * home screen will carry it, and under it the faces to choose from —
     * the first, left as it was, and the others — and for a plate, what it
     * is made of and what its dial is.
     */
    private void fillClock() {
        showClock();
        caption("SIZE");
        note("The clock is sized on the home screen: hold it and choose Resize. Whether it runs to the "
            + "screen's edges is the grid's to say, with every widget: Desktop, Edges.");
        caption("FACE");
        int face = Keep.number(this, Keep.CLOCK_FACE, Home.FACE_FIRST);
        rows.addView(swatches(Keep.CLOCK_FACE, face,
            new int[] {Home.FACE_FIRST, Home.FACE_PLATE, Home.FACE_MENO, Home.FACE_RINGS}, Home.FACE_NAMES,
            new Painter() {
                public void paint(android.graphics.Canvas c, float w, float h, int value) {
                    paintFace(c, w, h, value);
                }
            }));
        if (face == Home.FACE_FIRST) {
            touches(Hues.FIRST, new String[] {Hues.DIAL, Hues.HOUR, Hues.WINDOWS, Hues.SECONDS},
                new String[] {"DIAL", "HOUR WINDOW", "SMALL WINDOWS", "SECOND HAND"});
        }
        if (face == Home.FACE_PLATE) {
            touches(Hues.PLATE, new String[] {Hues.HANDS, Hues.SECONDS}, new String[] {"HANDS", "SECOND HAND"});
        }
        if (face == Home.FACE_MENO) {
            touches(Hues.MENO, new String[] {Hues.HANDS, Hues.MARKS}, new String[] {"HANDS", "MARKS"});
        }
        if (face == Home.FACE_RINGS) {
            note("To set the rings by hand, hold the clock on the home screen and choose Arrange rings.");
            caption("BIG RING");
            rows.addView(swatches(Keep.RINGS_BIG, Keep.number(this, Keep.RINGS_BIG, Rings.FIGURES),
                new int[] {Rings.FIGURES, Rings.HANDS}, Rings.BIG_NAMES, new Painter() {
                    public void paint(android.graphics.Canvas c, float w, float h, int value) {
                        paintRing(c, w, h, value == Rings.HANDS ? -1 : -2);
                    }
                }));
            caption("SMALL RINGS");
            final int[] ids = Keep.ringIds(this);
            for (int i = 0; i < ids.length; i++) {
                rows.addView(row(choice(Words.f("Ring %1", i + 1), "What it holds", Keep.RING_KIND + ids[i], Rings.CITY,
                    Rings.SMALL_NAMES, Rings.SMALL_KINDS)));
            }
            if (ids.length < Keep.RINGS_MOST) {
                rows.addView(deed("Add a ring", new Runnable() {
                    public void run() {
                        int next = 1;
                        for (int id : ids) {
                            next = Math.max(next, id + 1);
                        }
                        int[] more = java.util.Arrays.copyOf(ids, ids.length + 1);
                        more[ids.length] = next;
                        Keep.saveNumber(Tune.this, Keep.RING_KIND + next, Rings.FEELS);
                        Keep.saveRingIds(Tune.this, more);
                        fill();
                    }
                }));
            }
            if (ids.length > 0) {
                rows.addView(deed("Take away the last ring", new Runnable() {
                    public void run() {
                        Keep.saveRingIds(Tune.this, java.util.Arrays.copyOf(ids, ids.length - 1));
                        fill();
                    }
                }));
            }
            int[] hues = new int[Rings.COLOURS.length];
            for (int i = 0; i < hues.length; i++) {
                hues[i] = i;
            }
            Painter hue = new Painter() {
                public void paint(android.graphics.Canvas c, float w, float h, int value) {
                    paintHue(c, w, h, value);
                }
            };
            caption("LEVEL ARCS");
            rows.addView(swatches(Keep.RINGS_LEVEL, Keep.number(this, Keep.RINGS_LEVEL, Rings.ACCENT), hues,
                Rings.COLOUR_NAMES, hue));
            caption("SECOND HAND");
            rows.addView(swatches(Keep.RINGS_SECONDS, Keep.number(this, Keep.RINGS_SECONDS, Rings.ORANGE_ONE), hues,
                Rings.COLOUR_NAMES, hue));
            caption("DAY ARC AND ALARM");
            rows.addView(swatches(Keep.RINGS_DAY, Keep.number(this, Keep.RINGS_DAY, Rings.ORANGE_ONE), hues,
                Rings.COLOUR_NAMES, hue));
            caption("GROUND");
            groundSlider(30);
            caption("PLACES");
            TextView back = new TextView(this);
            back.setText(Words.t("Put every ring back in the chain"));
            back.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
            back.setTextColor(Tone.primary());
            back.setPadding(dp(24), dp(12), dp(24), dp(12));
            back.setBackground(Tone.touch(null, dp(16)));
            back.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Keep.saveRings(Tune.this, "");
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    showClock();
                }
            });
            rows.addView(back);
        }
        if (face == Home.FACE_MENO) {
            caption("GROUND");
            groundSlider(10);
            caption("LINES");
            linesSlider();
            caption("SECOND HAND");
            rows.addView(seconds());
        }
        if (face == Home.FACE_PLATE) {
            caption("PLATE");
            int[] kinds = new int[Rim.NAMES.length];
            for (int i = 0; i < kinds.length; i++) {
                kinds[i] = i;
            }
            rows.addView(swatches(Keep.CLOCK_PLATE, Keep.number(this, Keep.CLOCK_PLATE, Rim.STEEL), kinds, Rim.NAMES,
                new Painter() {
                    public void paint(android.graphics.Canvas c, float w, float h, int value) {
                        paintMaterial(c, w, h, value);
                    }
                }));
            /* Dark, black stamped, the rims' materials, and the palette's colours. */
            int count = 2 + Rim.NAMES.length + Watch.COLOUR_NAMES.length;
            int[] made = new int[count];
            String[] madeNames = new String[count];
            made[0] = Watch.DARK;
            madeNames[0] = "Dark";
            made[1] = Watch.EMBOSSED;
            madeNames[1] = "Black, embossed";
            for (int i = 0; i < Rim.NAMES.length; i++) {
                made[i + 2] = i;
                madeNames[i + 2] = Rim.NAMES[i];
            }
            for (int i = 0; i < Watch.COLOUR_NAMES.length; i++) {
                made[2 + Rim.NAMES.length + i] = Watch.COLOUR + i;
                madeNames[2 + Rim.NAMES.length + i] = Watch.COLOUR_NAMES[i];
            }
            caption("DIAL");
            rows.addView(swatches(Keep.CLOCK_DIAL, Keep.number(this, Keep.CLOCK_DIAL, Watch.DARK), made, madeNames,
                new Painter() {
                    public void paint(android.graphics.Canvas c, float w, float h, int value) {
                        paintField(c, w, h, value, true);
                    }
                }));
            caption("WINDOWS");
            rows.addView(swatches(Keep.CLOCK_FIELDS, Keep.number(this, Keep.CLOCK_FIELDS, Watch.DARK), made, madeNames,
                new Painter() {
                    public void paint(android.graphics.Canvas c, float w, float h, int value) {
                        paintField(c, w, h, value, false);
                    }
                }));
        }
    }

    /** A dial or a window of the plate clock in small, of its material, with its ink on it. */
    private void paintField(android.graphics.Canvas c, float w, float h, int value, boolean round) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG
            | android.graphics.Paint.FILTER_BITMAP_FLAG);
        if (value == Watch.DARK) {
            p.setColor(0xFF1A1817);
        } else {
            Watch.ground(p, value, w, h);
        }
        boolean light = value != Watch.DARK && Watch.light(value);
        int ink = light ? 0xFF1C1A17 : 0xFFEFE7D6;
        android.graphics.Paint mark = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        mark.setColor(ink);
        mark.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        if (round) {
            float r = Math.min(w, h) / 2f;
            c.drawCircle(w / 2f, h / 2f, r, p);
            mark.setStrokeWidth(r * 0.08f);
            c.drawLine(w / 2f, h / 2f, w / 2f, h / 2f - r * 0.6f, mark);
            c.drawLine(w / 2f, h / 2f, w / 2f + r * 0.4f, h / 2f + r * 0.2f, mark);
        } else {
            android.graphics.RectF box = new android.graphics.RectF(0, h * 0.22f, w, h * 0.78f);
            c.drawRoundRect(box, box.height() / 2f, box.height() / 2f, p);
            mark.setTextSize(h * 0.24f);
            mark.setTextAlign(android.graphics.Paint.Align.CENTER);
            c.drawText("93%", w / 2f, h * 0.58f, mark);
        }
    }

    /** How dark the outline clock's ground is: clear to nearly black, evenly. */
    private void groundSlider(final int fallback) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        TextView name = new TextView(this);
        name.setText(Words.t("Darkening"));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(Keep.number(this, Keep.CLOCK_GROUND, fallback) + "%");
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        made.addView(new Slide(this, 0, 95, Keep.number(this, Keep.CLOCK_GROUND, 10), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + "%");
                Keep.saveNumber(Tune.this, Keep.CLOCK_GROUND, at);
                showClock();
            }
        }));
        rows.addView(made);
    }

    /** The widget clock's own seconds hands, as round samples of their colours. */
    private View seconds() {
        android.widget.HorizontalScrollView across = new android.widget.HorizontalScrollView(this);
        across.setHorizontalScrollBarEnabled(false);
        across.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout line = new LinearLayout(this);
        line.setPadding(dp(18), 0, dp(18), dp(6));
        final int[] chosen = {Keep.number(this, Keep.MENO_SECOND, 0)};
        final List<View> dots = new ArrayList<>();
        for (int i = 0; i < Meno.SECONDS.length; i++) {
            final int value = i;
            View dot = new View(this) {
                @Override
                protected void onDraw(android.graphics.Canvas canvas) {
                    android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Meno.SECONDS[value]);
                    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f - dp(5), p);
                    if (chosen[0] == value) {
                        p.setStyle(android.graphics.Paint.Style.STROKE);
                        p.setStrokeWidth(dp(2.5f));
                        p.setColor(Tone.onSurface());
                        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f - dp(1.5f), p);
                    }
                }
            };
            dots.add(dot);
            dot.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chosen[0] = value;
                    Keep.saveNumber(Tune.this, Keep.MENO_SECOND, value);
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    for (View d : dots) {
                        d.invalidate();
                    }
                    showClock();
                }
            });
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(dp(48), dp(48));
            at.rightMargin = dp(4);
            line.addView(dot, at);
        }
        across.addView(line);
        return across;
    }

    /** How strong the widget clock's fine lines are: its ring, its edges. */
    private void linesSlider() {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        TextView name = new TextView(this);
        name.setText(Words.t("Line strength"));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(Keep.number(this, Keep.MENO_LINES, 100) + "%");
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        made.addView(new Slide(this, 50, 200, Keep.number(this, Keep.MENO_LINES, 100), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + "%");
                Keep.saveNumber(Tune.this, Keep.MENO_LINES, at);
                showClock();
            }
        }));
        rows.addView(made);
    }

    /**
     * The owner's touches to a face: its windows' sizes, seen at once in the
     * window above, the colours of its parts, and a way back to the face as
     * it was drawn, whatever was chosen.
     */
    private void touches(final String face, String[] parts, String[] captions) {
        caption("WINDOWS");
        sizeSlider("Dial", Hues.sizeKey(face, Hues.DIAL));
        sizeSlider("Hour and date", Hues.sizeKey(face, Hues.HOUR));
        sizeSlider("Row of small windows", Hues.sizeKey(face, Hues.ROW));
        Painter hue = new Painter() {
            public void paint(android.graphics.Canvas c, float w, float h, int value) {
                paintHue(c, w, h, value);
            }
        };
        for (int i = 0; i < parts.length; i++) {
            caption(captions[i]);
            String key = Hues.hueKey(face, parts[i]);
            rows.addView(swatches(key, Keep.number(this, key, Hues.ORIGINAL), Hues.VALUES, Hues.NAMES, hue));
        }
        caption("AS IT WAS");
        rows.addView(deed("Put this face back as it was drawn", new Runnable() {
            public void run() {
                Hues.forget(Tune.this, face);
                fill();
            }
        }));
    }

    /** A window's size in percent of its own, seen at once in the clock above. */
    private void sizeSlider(String title, final String key) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        TextView name = new TextView(this);
        name.setText(Words.t(title));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(Keep.number(this, key, 100) + "%");
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        made.addView(new Slide(this, 60, 140, Keep.number(this, key, 100), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + "%");
                Keep.saveNumber(Tune.this, key, at);
                showClock();
            }
        }));
        rows.addView(made);
    }

    private static final int WRITE_COPY = 21;
    private static final int READ_COPY = 22;
    private static final int READ_FOREIGN = 23;
    private static final int READ_LANGUAGE = 24;
    private static final int WRITE_TEMPLATE = 25;
    private static final int READ_PICTURE = 26;

    /** The owner's own picture set, away from the hand; the room shows what is kept now. */
    private void wearPicture() {
        said("Setting the wallpaper");
        new Thread(new Runnable() {
            public void run() {
                boolean set;
                try {
                    Picture.set(Tune.this);
                    set = true;
                } catch (Exception | OutOfMemoryError failed) {
                    set = false;
                }
                final boolean done = set;
                runOnUiThread(new Runnable() {
                    public void run() {
                        said(done ? "The wallpaper is set" : "The wallpaper could not be set");
                        fill();
                    }
                });
            }
        }).start();
    }
    /** A copy armed by a first tap, waiting for the second. */
    private java.io.File armedCopy;
    private long armedCopyAt;

    /**
     * Copies: into a file and back from one; the last restore undone; and
     * the copies made by themselves when a new version first started, each
     * brought back by two taps, the first saying what the second will do.
     */
    private void fillBackup() {
        note("A copy holds every screen's set-out, the dock, the folders, the clock and every setting. "
            + "Widgets keep their places; after installing anew they are added again. Restore takes any copy: "
            + "this home screen's own, or the backup of another home screen, whose screens, apps, folders, "
            + "widgets, dock, wallpaper and look it brings in.");
        rows.addView(row(toggle("Keep the wallpaper in the copy", "The pictures of the home screen and the lock "
            + "screen go into every copy, and come back with it", Keep.COPY_WALLPAPER, false)));
        if (Keep.flag(this, Keep.COPY_WALLPAPER, false) && !Copy.wallpaperReadable()) {
            /* The phone shows the wallpaper only to an app that may read every file. */
            note("The phone lets an app read the wallpaper only if it may read all files. Ellipse reads nothing "
                + "but the wallpaper.");
            rows.addView(deed("Allow reading the wallpaper", new Runnable() {
                public void run() {
                    openSafely(new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:" + getPackageName())));
                }
            }));
        }
        rows.addView(deed("Back up into a file", new Runnable() {
            public void run() {
                Intent make = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json").putExtra(Intent.EXTRA_TITLE, Copy.name());
                try {
                    startActivityForResult(make, WRITE_COPY);
                } catch (RuntimeException none) {
                    said("The phone has no place to keep files");
                }
            }
        }));
        rows.addView(deed("Send a copy", new Runnable() {
            public void run() {
                /* The same copy, handed to whatever the phone can send it with: a cloud, a mail, a chat. */
                try {
                    startActivity(Handed.send(Tune.this, Copy.name(), Copy.whole(Tune.this), "application/json"));
                } catch (Exception failed) {
                    said("The copy could not be written");
                }
            }
        }));
        rows.addView(deed("Restore from a file", new Runnable() {
            public void run() {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*");
                try {
                    startActivityForResult(pick, READ_COPY);
                } catch (RuntimeException none) {
                    said("The phone has no place to keep files");
                }
            }
        }));
        if (Copy.undoable(this)) {
            rows.addView(deed("Undo the last restore", new Runnable() {
                public void run() {
                    if (Copy.undo(Tune.this)) {
                        restart();
                    } else {
                        said("There is nothing to undo");
                    }
                }
            }));
        }
        java.util.List<java.io.File> kept = Copy.updates(this);
        if (!kept.isEmpty()) {
            caption("KEPT AUTOMATICALLY, ONCE A DAY");
            note("The set-out as it was at the first start of the day. Tap twice to bring it back.");
            for (final java.io.File one : kept) {
                String[] what = Copy.about(one);
                final String title = Words.t("As it was") + (what[1].isEmpty() ? "" : ", " + what[1].replace('T', ' '));
                final TextView line = (TextView) deed(title, null);
                line.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        long now = System.currentTimeMillis();
                        if (!one.equals(armedCopy) || now - armedCopyAt > 4000L) {
                            armedCopy = one;
                            armedCopyAt = now;
                            line.setText(Words.t("Tap again to bring this set-out back"));
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            v.postDelayed(new Runnable() {
                                public void run() {
                                    line.setText(title);
                                }
                            }, 4000L);
                            return;
                        }
                        try {
                            if (Copy.read(Tune.this, Copy.load(one))) {
                                restored();
                                return;
                            }
                        } catch (java.io.IOException gone) {
                            // Said below.
                        }
                        said("That copy could not be read");
                    }
                });
                rows.addView(line);
            }
        }
    }

    /**
     * Looks: the look as it is now kept under a name; each kept look put on
     * at a touch, kept again as it is now, or forgotten; and the look worn
     * before the last put on, back at a touch.
     */
    private void fillLooks() {
        note("A preset dresses the home screen: icons, colours, the clock, the frames. What you set out stays "
            + "as it is: grids, apps and their places, the names under the icons, the theme.");
        caption("READY");
        for (int i = 0; i < Looks.READY.length; i++) {
            final int which = i;
            LinearLayout one = new LinearLayout(this);
            one.setOrientation(LinearLayout.VERTICAL);
            one.setPadding(dp(24), dp(12), dp(24), dp(12));
            one.setBackground(Tone.touch(null, dp(16)));
            TextView ready = new TextView(this);
            ready.setText(Words.t(Looks.READY[i]));
            ready.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * scaled);
            ready.setTextColor(Tone.onSurface());
            one.addView(ready);
            TextView about = new TextView(this);
            about.setText(Words.t(Looks.READY_ABOUT[i]));
            about.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
            about.setTextColor(Tone.faint());
            one.addView(about);
            one.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    Looks.ready(Tune.this, which);
                    worn();
                }
            });
            rows.addView(one);
        }
        caption("MINE");
        rows.addView(deed("Keep the look as it is now", new Runnable() {
            public void run() {
                Ask.show(host, "Name this look", Words.f("Look %1", Looks.names(Tune.this).size() + 1), new Ask.Answer() {
                    public void answered(String text) {
                        String name = text == null ? "" : text.trim();
                        if (name.isEmpty()) {
                            return;
                        }
                        Looks.keep(Tune.this, name);
                        fill();
                    }
                });
            }
        }));
        if (Looks.undoable(this)) {
            rows.addView(deed("Back to the look before", new Runnable() {
                public void run() {
                    if (Looks.undo(Tune.this)) {
                        worn();
                    }
                }
            }));
        }
        java.util.List<String> names = Looks.names(this);
        if (names.isEmpty()) {
            return;
        }
        for (final String name : names) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView wear = (TextView) deed(name, new Runnable() {
                public void run() {
                    if (Looks.wear(Tune.this, name)) {
                        worn();
                    }
                }
            });
            wear.setTextColor(Tone.onSurface());
            line.addView(wear, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView again = (TextView) deed("Keep now", new Runnable() {
                public void run() {
                    Looks.keep(Tune.this, name);
                    said(Words.f("\u201C%1\u201D now holds the look as it is", name));
                }
            });
            again.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
            again.setPadding(dp(12), dp(12), dp(12), dp(12));
            line.addView(again);
            final TextView gone = (TextView) deed("\u00D7", null);
            gone.setPadding(dp(16), dp(12), dp(24), dp(12));
            gone.setOnClickListener(new View.OnClickListener() {
                private long armed;

                public void onClick(View v) {
                    long now = System.currentTimeMillis();
                    if (now - armed > 3000L) {
                        armed = now;
                        gone.setText(Words.t("Forget?"));
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        return;
                    }
                    Looks.forget(Tune.this, name);
                    fill();
                }
            });
            line.addView(gone);
            rows.addView(line);
        }
    }

    /**
     * The list of every app on a ground of the owner's own: a colour by its
     * hue, saturation, brightness and opacity, seen at once on a piece of
     * the list above; its words turn dark or light as the colour asks.
     */
    private void fillListGround() {
        final View piece = new View(this) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(android.graphics.Canvas c) {
                float w = getWidth();
                float h = getHeight();
                float r = dp(24);
                paint.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h, 0xFF3A2E1C, 0xFF15140F,
                    android.graphics.Shader.TileMode.CLAMP));
                c.drawRoundRect(0f, 0f, w, h, r, r, paint);
                paint.setShader(null);
                paint.setColor(Tone.listGround());
                c.drawRoundRect(0f, 0f, w, h, r, r, paint);
                String[] names = {"Calendar", "Camera", "Maps"};
                int[] dots = {0xFF4A7BE0, 0xFFE0703A, 0xFF4CAF6A};
                paint.setTextSize(19f * scaled);
                for (int i = 0; i < names.length; i++) {
                    float y = dp(34) + i * dp(46);
                    paint.setColor(dots[i]);
                    c.drawCircle(dp(40), y, dp(15), paint);
                    paint.setColor(Tone.listInk());
                    c.drawText(names[i], dp(72), y + dp(7), paint);
                }
            }
        };
        window.removeAllViews();
        window.setVisibility(View.VISIBLE);
        window.addView(piece, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)));
        rows.addView(row(toggle("A colour of my own", "Off, the list stands on the theme's own ground",
            Keep.LIST_OWN, false)));
        groundSlider("Hue", Keep.LIST_HUE, 0, 360, 38, "\u00B0", piece);
        groundSlider("Saturation", Keep.LIST_SAT, 0, 100, 30, "%", piece);
        groundSlider("Brightness", Keep.LIST_VAL, 0, 100, 45, "%", piece);
        groundSlider("Opacity", Keep.LIST_ALPHA, 20, 100, 100, "%", piece);
    }

    private void groundSlider(String title, final String key, int least, int most, int fallback, final String unit,
                              final View piece) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        TextView name = new TextView(this);
        name.setText(Words.t(title));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(Keep.number(this, key, fallback) + unit);
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        made.addView(new Slide(this, least, most, Keep.number(this, key, fallback), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + unit);
                Keep.saveNumber(Tune.this, key, at);
                if (!Keep.flag(Tune.this, Keep.LIST_OWN, false)) {
                    Keep.saveFlag(Tune.this, Keep.LIST_OWN, true);
                }
                Tone.read(Tune.this);
                piece.invalidate();
            }
        }));
        rows.addView(made);
    }

    /** The typefaces by the names they are shown under. */
    private static final String[] FONT_NAMES = {"Sans", "Sans condensed", "Sans light", "Sans medium", "Serif",
        "Monospace"};

    /**
     * The typeface of every name and every word here: the icons on the
     * wallpaper at the head of the room, their names in the typeface and
     * size chosen; every typeface as a line of its own, its name and a
     * sentence written in it; and the size of the names, seen at once.
     */
    private void fillFonts() {
        window.removeAllViews();
        sample = new Sample(this);
        window.addView(sample);
        throughTo = sample;
        sample.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                host.invalidate();
            }
        });
        caption("LETTERS");
        for (int i = 0; i < Style.FAMILIES.length; i++) {
            rows.addView(fontLine(i));
        }
        caption("SIZE");
        slider("Size of names", "How large the names are drawn", Keep.NAME_SIZE, 80, 140);
    }

    /** A typeface as a line: its name in itself, and a sentence in it; the one chosen in the accent. */
    private View fontLine(final int which) {
        android.graphics.Typeface face = android.graphics.Typeface.create(Style.FAMILIES[which],
            android.graphics.Typeface.NORMAL);
        boolean on = Style.family == which;
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(12), dp(24), dp(12));
        made.setBackground(Tone.touch(null, dp(16)));
        TextView name = new TextView(this);
        name.setText((on ? "\u25CF  " : "\u25CB  ") + Words.t(FONT_NAMES[Math.min(which, FONT_NAMES.length - 1)]));
        name.setTypeface(face);
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * scaled);
        name.setTextColor(on ? Tone.primary() : Tone.onSurface());
        made.addView(name);
        TextView said = new TextView(this);
        said.setText(Words.s("sample"));
        said.setTypeface(face);
        said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
        said.setTextColor(Tone.faint());
        said.setPadding(dp(28), dp(2), 0, 0);
        made.addView(said);
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                Keep.saveNumber(Tune.this, Keep.FONT, which);
                Style.font(which);
                final int y = scroll.getScrollY();
                fill();
                scroll.post(new Runnable() {
                    public void run() {
                        scroll.scrollTo(0, y);
                    }
                });
            }
        });
        return made;
    }

    /**
     * Languages: English, the two modules that come with the home screen,
     * and one brought from a file; a module brought from a file; and the
     * English template given away, for a module of any language to be made
     * from it.
     */
    private void fillLanguages() {
        note("The words of the home screen in another language. A module is a file of words, one phrase a "
            + "line: the English, then the same in its language. A phrase a module lacks is said in English.");
        String now = Keep.word(this, Keep.LANGUAGE);
        now = now == null ? "" : now;
        java.util.List<String> codes = new java.util.ArrayList<>(java.util.Arrays.asList(Words.CODES));
        java.util.List<String> names = new java.util.ArrayList<>(java.util.Arrays.asList(Words.NAMES));
        String brought = Words.broughtName(this);
        if (brought != null) {
            codes.add(Words.BROUGHT);
            names.add(brought);
        }
        for (int i = 0; i < codes.size(); i++) {
            final String code = codes.get(i);
            boolean on = code.equals(now);
            TextView line = new TextView(this);
            line.setText((on ? "\u25CF  " : "\u25CB  ") + names.get(i));
            line.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
            line.setTextColor(on ? Tone.primary() : Tone.onSurface());
            line.setPadding(dp(24), dp(12), dp(24), dp(12));
            line.setBackground(Tone.touch(null, dp(16)));
            line.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    Keep.saveWord(Tune.this, Keep.LANGUAGE, code);
                    worn();
                }
            });
            rows.addView(line);
        }
        caption("MODULES");
        rows.addView(deed("Bring in a module from a file", new Runnable() {
            public void run() {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*");
                try {
                    startActivityForResult(pick, READ_LANGUAGE);
                } catch (RuntimeException none) {
                    said("The phone has no place to keep files");
                }
            }
        }));
        rows.addView(deed("Save the English template", new Runnable() {
            public void run() {
                Intent make = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("text/plain").putExtra(Intent.EXTRA_TITLE, "ellipse-words-en.txt");
                try {
                    startActivityForResult(make, WRITE_TEMPLATE);
                } catch (RuntimeException none) {
                    said("The phone has no place to keep files");
                }
            }
        }));
        note("The template holds every phrase in English on both sides of \" = \". Put the right side into "
            + "another language, by hand or by an assistant, and bring the file in here.");
    }

    /**
     * A copy brought back: the home screen made again; first, if some of its
     * widgets could not be made again without the phone asking, they are
     * named, to be added again from the shelf.
     */
    private void restored() {
        if (Copy.unmade.isEmpty()) {
            restart();
            return;
        }
        StringBuilder told = new StringBuilder(Words.f("These widgets are to be added again, for the phone "
            + "asks first: %1.", joined(Copy.unmade)));
        told.append(' ').append(Words.t("Allow Ellipse to make widgets always, and a restore makes them itself."));
        Ask.tell(host, "Restored", told.toString(), "Done", new Runnable() {
            public void run() {
                restart();
            }
        });
    }

    /** A look put on: the colours read again and the settings made again in them. */
    private void worn() {
        Tone.read(this);
        Style.read(this);
        recreate();
    }

    private void said(String words) {
        android.widget.Toast.makeText(this, Words.t(words), android.widget.Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int asked, int result, Intent answer) {
        super.onActivityResult(asked, result, answer);
        if (result != RESULT_OK || answer == null || answer.getData() == null) {
            return;
        }
        android.net.Uri where = answer.getData();
        if (asked == WRITE_TEMPLATE) {
            try (java.io.OutputStream out = getContentResolver().openOutputStream(where, "wt");
                 java.io.InputStream in = getAssets().open("lang/template.txt")) {
                if (out == null) {
                    throw new java.io.IOException();
                }
                Copy.put(out, Copy.words(in));
                said("The template is saved");
            } catch (java.io.IOException | RuntimeException failed) {
                said("The template could not be written");
            }
            return;
        }
        if (asked == READ_PICTURE) {
            final android.net.Uri chosen = where;
            said("Reading the picture");
            new Thread(new Runnable() {
                public void run() {
                    final boolean kept = Picture.keep(Tune.this, chosen);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (kept) {
                                wearPicture();
                            } else {
                                said("That picture could not be read");
                            }
                        }
                    });
                }
            }).start();
            return;
        }
        if (asked == READ_LANGUAGE) {
            try (java.io.InputStream in = getContentResolver().openInputStream(where)) {
                if (in == null) {
                    throw new java.io.IOException();
                }
                String text = Copy.words(in);
                java.util.Map<String, String> probe = new java.util.HashMap<>();
                Words.parse(text, probe);
                if (probe.isEmpty()) {
                    said("That file holds no phrases");
                    return;
                }
                try (java.io.OutputStream out = new java.io.FileOutputStream(
                    new java.io.File(getFilesDir(), Words.BROUGHT_FILE))) {
                    Copy.put(out, text);
                }
                Keep.saveWord(this, Keep.LANGUAGE, Words.BROUGHT);
                Words.forget();
                worn();
            } catch (java.io.IOException | RuntimeException failed) {
                said("That file could not be read");
            }
            return;
        }
        if (asked == WRITE_COPY) {
            try (java.io.OutputStream out = getContentResolver().openOutputStream(where, "wt")) {
                if (out == null) {
                    throw new java.io.IOException();
                }
                Copy.put(out, Copy.whole(this));
                said("The copy is made");
            } catch (java.io.IOException | org.json.JSONException | RuntimeException failed) {
                said("The copy could not be written");
            }
        } else if (asked == READ_FOREIGN || asked == READ_COPY) {
            /* One file, whichever line it was picked from: a copy made here is
               restored; the backup of another home screen is brought in. */
            byte[] bytes;
            try (java.io.InputStream in = getContentResolver().openInputStream(where)) {
                if (in == null) {
                    throw new java.io.IOException();
                }
                java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[65536];
                int n;
                while ((n = in.read(chunk)) > 0) {
                    all.write(chunk, 0, n);
                }
                bytes = all.toByteArray();
            } catch (java.io.IOException | RuntimeException failed) {
                said("That file could not be read");
                return;
            }
            String words = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (words.contains("\"" + Copy.KIND + "\"")) {
                if (Copy.read(this, words)) {
                    restored();
                } else {
                    said("That copy could not be read");
                }
                return;
            }
            final Foreign.Layout found;
            try {
                found = Foreign.read(this, bytes);
            } catch (java.io.IOException | RuntimeException failed) {
                said("That file could not be read");
                return;
            }
            if (found == null) {
                said("No set-out of a known shape was found in that file");
                return;
            }
            int columns = Math.max(3, Math.min(7, found.columns));
            int rowsOf = Math.max(3, Math.min(12, found.rows));
            String what = Words.n("%1 screen of %2 \u00D7 %3 | %1 screens of %2 \u00D7 %3", found.screens.size(), columns, rowsOf)
                + ": " + Words.f("apps %1, folders %2, widgets %3.", found.count(Foreign.Item.APP),
                    found.count(Foreign.Item.FOLDER), found.count(Foreign.Item.WIDGET))
                + " " + Words.t("It takes the place of the set-out here, which is copied aside first: "
                    + "Undo the last restore brings it back.");
            Ask.tell(host, "Bring in this set-out?", what, "Bring in", new Runnable() {
                public void run() {
                    android.appwidget.AppWidgetHost widgets = new android.appwidget.AppWidgetHost(Tune.this,
                        Home.WIDGET_HOST);
                    Foreign.Report done = Foreign.bringIn(Tune.this, found, widgets);
                    /* Each part put into words whole, so a language may order its own. */
                    StringBuilder told = new StringBuilder();
                    told.append(Words.f("In their places: apps %1, folders %2, widgets %3.",
                        done.apps, done.folders, done.widgets));
                    if (done.look > 0) {
                        told.append(' ').append(Words.t("Taken over too, as it looked there, where its settings "
                            + "told: names, dock, the icons' outline and pack, the screens' points, endless "
                            + "turning and edges, and the list of every app."));
                    }
                    told.append(' ').append(Words.t(done.clock
                        ? "The widget clock stands as this home screen's own, in its face."
                        : "This home screen's own clock is put away; the widget shelf brings it back."));
                    if (done.packMissing != null) {
                        told.append(' ').append(Words.f("Its pack of icons, %1, is not on this phone: it is "
                            + "chosen, and the icons take it once it is installed.", done.packMissing));
                    }
                    if (done.hidden > 0) {
                        told.append(' ').append(Words.f("Hidden as there: %1.", done.hidden));
                    }
                    if (done.kinds > 0) {
                        told.append(' ').append(Words.f("Its kinds of apps, as categories: %1.", done.kinds));
                    }
                    if (done.wallpaper) {
                        told.append(' ').append(Words.t("Its wallpaper is set."));
                    }
                    if (done.links > 0) {
                        told.append(' ').append(Words.f("Shortcuts and links: %1.", done.links));
                    }
                    if (done.named > 0) {
                        told.append(' ').append(Words.f("Names given by hand: %1.", done.named));
                    }
                    if (done.drawn > 0) {
                        told.append(' ').append(Words.f("Icons chosen by hand: %1.", done.drawn));
                    }
                    if (!done.lost.isEmpty()) {
                        told.append(' ').append(Words.f("Shortcuts their apps no longer hold, to be made "
                            + "again: %1.", joined(done.lost)));
                    }
                    if (done.missing > 0) {
                        told.append(' ').append(Words.f("Not on this phone, waiting grey in their places "
                            + "until installed: %1.", done.missing));
                    }
                    if (done.others > 0) {
                        told.append(' ').append(Words.f("Left behind, as shortcuts and the other home "
                            + "screen's own things: %1.", done.others));
                    }
                    if (!done.unmade.isEmpty()) {
                        told.append(' ').append(Words.f("Widgets to add again, for the phone asks first: %1.",
                            joined(done.unmade)));
                    }
                    Ask.tell(host, "Brought in", told.toString(), "Done", new Runnable() {
                        public void run() {
                            restart();
                        }
                    });
                }
            });

        }
    }

    /** Names one after another, apart by commas. */
    private static String joined(java.util.List<String> names) {
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            all.append(i > 0 ? ", " : "").append(names.get(i));
        }
        return all.toString();
    }

    /** A frame's measure in dp, seen at once on the sample widget above it. */
    private void frameSlider(String title, final String key, int least, int most, int fallback, final View sample) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        TextView name = new TextView(this);
        name.setText(Words.t(title));
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(Keep.number(this, key, fallback) + " dp");
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        made.addView(new Slide(this, least, most, Keep.number(this, key, fallback), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + " dp");
                Keep.saveNumber(Tune.this, key, at);
                sample.invalidate();
            }
        }));
        rows.addView(made);
    }

    /**
     * A widget in small, as widgets will wear their frame: a card of the
     * surface with a few lines on it, in the frame as set now.
     */
    private static final class FramedSample extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        FramedSample(android.content.Context context) {
            super(context);
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            android.content.Context c = getContext();
            float d = getResources().getDisplayMetrics().density;
            float w = getWidth();
            float h = getHeight();
            int kind = Keep.number(c, Keep.WIDGET_FRAME, Rim.NONE);
            float f = kind == Rim.NONE ? 0f : Keep.number(c, Keep.WIDGET_FRAME_WIDTH, 6) * d;
            float round = Keep.number(c, Keep.WIDGET_FRAME_ROUND, 24) * d;
            android.graphics.RectF all = new android.graphics.RectF(0f, 0f, w, h);
            android.graphics.RectF in = new android.graphics.RectF(f, f, w - f, h - f);
            float innerRound = Math.max(0f, round - f * 0.6f);
            paint.setColor(Tone.containerHigh());
            canvas.drawRoundRect(in, innerRound, innerRound, paint);
            paint.setColor(Tone.onVariant());
            canvas.drawRoundRect(f + 18f * d, f + 20f * d, w * 0.55f, f + 34f * d, 7f * d, 7f * d, paint);
            paint.setColor(Tone.faint());
            canvas.drawRoundRect(f + 18f * d, f + 46f * d, w * 0.75f, f + 56f * d, 5f * d, 5f * d, paint);
            canvas.drawRoundRect(f + 18f * d, f + 66f * d, w * 0.4f, f + 76f * d, 5f * d, 5f * d, paint);
            if (kind == Rim.NONE) {
                return;
            }
            android.graphics.Path outer = new android.graphics.Path();
            outer.addRoundRect(all, round, round, android.graphics.Path.Direction.CW);
            android.graphics.Path inner = new android.graphics.Path();
            inner.addRoundRect(in, innerRound, innerRound, android.graphics.Path.Direction.CW);
            android.graphics.Path ring = new android.graphics.Path();
            ring.op(outer, inner, android.graphics.Path.Op.DIFFERENCE);
            Rim.plate(canvas, ring, kind, w, h);
            Rim.cut(canvas, inner, w);
            if (Keep.flag(c, Keep.WIDGET_GLAZE, false) || kind == Rim.GLASS) {
                Rim.glaze(canvas, outer, 0f, 0f, w, h);
            }
        }
    }

    /** A line of words in the accent that does something when touched. */
    private View deed(String said, final Runnable does) {
        TextView made = new TextView(this);
        made.setText(Words.t(said));
        made.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
        made.setTextColor(Tone.primary());
        made.setPadding(dp(24), dp(12), dp(24), dp(12));
        made.setBackground(Tone.touch(null, dp(16)));
        if (does != null) {
            made.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    does.run();
                }
            });
        }
        return made;
    }

    /** A quiet line of words under a caption, saying how something is done. */
    private void note(String text) {
        TextView said = new TextView(this);
        said.setText(Words.t(text));
        said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        said.setTextColor(Tone.onVariant());
        said.setPadding(dp(24), dp(2), dp(24), dp(8));
        rows.addView(said);
    }

    /**
     * The clock the settings describe, in the window at the head of the
     * room, at the size it stands at on the home screen — made smaller
     * only to fit the window.
     */
    private void showClock() {
        window.removeAllViews();
        View clock = Home.timepiece(this, new Almanac.Hand() {
            public void pressed(String which, View from, android.graphics.RectF box) {
            }
        });
        ((Timepiece) clock).weather(Keep.flag(this, Keep.WEATHER, true));
        ((Timepiece) clock).ears(80);
        float wide = Keep.number(this, Keep.CLOCK_WIDE, 0);
        float tall = Keep.number(this, Keep.CLOCK_TALL, 0);
        if (wide <= 0 || tall <= 0) {
            android.util.DisplayMetrics m = getResources().getDisplayMetrics();
            wide = m.widthPixels / m.density - 16f;
            tall = 132f;
        }
        window.addView(new Proof(this, clock, Math.round(wide * density()), Math.round(tall * density()), dp(220)),
            new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    /**
     * A thing drawn at a size of its own and shown smaller if the room is
     * less: laid out whole at its true size, then scaled about its corner
     * and centred across.
     */
    private static final class Proof extends FrameLayout {
        private final View thing;
        private final int wide;
        private final int tall;
        private final int most;
        private float s = 1f;

        Proof(android.content.Context context, View thing, int wide, int tall, int most) {
            super(context);
            this.thing = thing;
            this.wide = Math.max(1, wide);
            this.tall = Math.max(1, tall);
            this.most = most;
            setClipChildren(false);
            addView(thing, new FrameLayout.LayoutParams(this.wide, this.tall));
            thing.setPivotX(0f);
            thing.setPivotY(0f);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int room = MeasureSpec.getSize(widthSpec);
            s = Math.min(1f, Math.min(room / (float) wide, most / (float) tall));
            thing.measure(MeasureSpec.makeMeasureSpec(wide, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(tall, MeasureSpec.EXACTLY));
            setMeasuredDimension(room, Math.round(tall * s));
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            thing.layout(0, 0, wide, tall);
            thing.setScaleX(s);
            thing.setScaleY(s);
            thing.setTranslationX((r - l - wide * s) / 2f);
        }
    }

    /** A clock face in small: the first as its round dial and its windows; the plate as a slab with dark windows. */
    /**
     * A ring in small: the big one in figures (-2) or in hands (-1), or a
     * small one by what it holds.
     */
    private void paintRing(android.graphics.Canvas c, float w, float h, int kind) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float r = h * 0.36f;
        float x = w / 2f;
        float y = h / 2f;
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, h * 0.02f));
        p.setColor(Tone.onSurface());
        c.drawCircle(x, y, r, p);
        p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        p.setStrokeWidth(Math.max(1.5f, h * 0.035f));
        android.graphics.RectF o = new android.graphics.RectF(x - r * 0.86f, y - r * 0.86f, x + r * 0.86f,
            y + r * 0.86f);
        if (kind == -1) {
            p.setColor(Tone.onSurface());
            c.drawLine(x, y, x - r * 0.4f, y - r * 0.1f, p);
            c.drawLine(x, y, x + r * 0.15f, y - r * 0.6f, p);
            p.setColor(0xFFF29A4A);
            c.drawArc(o, -90f, 250f, false, p);
            return;
        }
        android.graphics.Paint t = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        t.setTextAlign(android.graphics.Paint.Align.CENTER);
        t.setColor(Tone.onSurface());
        if (kind == -2) {
            p.setColor(0xFFF29A4A);
            c.drawArc(o, -90f, 250f, false, p);
            t.setTextSize(r * 0.5f);
            c.drawText("9:06", x, y + r * 0.18f, t);
        } else if (kind == Rings.CITY) {
            t.setTextSize(r * 0.42f);
            c.drawText("12\u00B0", x, y + r * 0.15f, t);
        } else if (kind == Rings.CALENDAR) {
            t.setTextSize(r * 0.6f);
            c.drawText("25", x, y + r * 0.22f, t);
        } else {
            p.setColor(Tone.primary());
            c.drawArc(o, -90f, 220f, false, p);
            t.setTextSize(r * 0.36f);
            c.drawText("62%", x, y + r * 0.13f, t);
        }
    }

    /** A name's colour in small: a word in it, with its halo, on a ground of dusk. */
    private void paintName(android.graphics.Canvas c, float w, float h, int which) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        int ink = which == Keep.NAME_LIGHT ? Tone.onWall() : which == Keep.NAME_DARK ? 0xFF1C1A17
            : Rings.colour(which);
        float r = h * 0.2f;
        p.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h, 0xFF4A3F33, 0xFFB9A98F,
            android.graphics.Shader.TileMode.CLAMP));
        c.drawRoundRect(w * 0.12f, h * 0.18f, w * 0.88f, h * 0.82f, r, r, p);
        p.setShader(null);
        p.setTextAlign(android.graphics.Paint.Align.CENTER);
        p.setTextSize(h * 0.3f);
        p.setColor(ink);
        p.setShadowLayer(h * 0.04f, 0f, h * 0.01f,
            android.graphics.Color.luminance(ink) > 0.4f ? 0x99000000 : 0x99FFFFFF);
        c.drawText("Aa", w / 2f, h * 0.61f, p);
    }

    /** A colour for the rings in small: an arc of it round a faint ring. */
    private void paintHue(android.graphics.Canvas c, float w, float h, int which) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float r = h * 0.34f;
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, h * 0.02f));
        p.setColor(Tone.outline());
        c.drawCircle(w / 2f, h / 2f, r, p);
        p.setStrokeWidth(Math.max(2f, h * 0.07f));
        p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        p.setColor(which == Hues.ORIGINAL ? Tone.onVariant() : Rings.colour(which));
        if (which == Hues.ORIGINAL) {
            p.setPathEffect(new android.graphics.DashPathEffect(new float[] {h * 0.06f, h * 0.06f}, 0f));
        }
        c.drawArc(new android.graphics.RectF(w / 2f - r, h / 2f - r, w / 2f + r, h / 2f + r), -90f, 250f, false, p);
    }

    private void paintFace(android.graphics.Canvas c, float w, float h, int value) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float r = h * 0.28f;
        if (value == Home.FACE_RINGS) {
            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, h * 0.02f));
            p.setColor(Tone.onSurface());
            c.drawCircle(w * 0.3f, h * 0.5f, h * 0.3f, p);
            c.drawCircle(w * 0.62f, h * 0.38f, h * 0.18f, p);
            c.drawCircle(w * 0.82f, h * 0.62f, h * 0.18f, p);
            p.setColor(Tone.primary());
            p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            p.setStrokeWidth(Math.max(1.5f, h * 0.03f));
            c.drawArc(new android.graphics.RectF(w * 0.82f - h * 0.14f, h * 0.48f, w * 0.82f + h * 0.14f, h * 0.76f),
                -90f, 220f, false, p);
            return;
        }
        if (value == Home.FACE_MENO) {
            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, h * 0.02f));
            p.setColor(Tone.onSurface());
            c.drawRoundRect(new android.graphics.RectF(1, h * 0.15f, w - 1, h * 0.85f), h * 0.1f, h * 0.1f, p);
            c.drawCircle(w * 0.3f, h * 0.5f, h * 0.26f, p);
            c.drawRoundRect(new android.graphics.RectF(w * 0.6f, h * 0.24f, w * 0.92f, h * 0.44f), h * 0.1f, h * 0.1f, p);
            p.setColor(0xFFC3A2D6);
            c.drawCircle(w * 0.68f, h * 0.64f, h * 0.09f, p);
            p.setColor(0xFF7AA7F0);
            c.drawCircle(w * 0.85f, h * 0.64f, h * 0.09f, p);
            return;
        }
        if (value == Home.FACE_PLATE) {
            android.graphics.Path slab = Shape.outline(Shape.ROUNDED, w, h * 0.7f);
            c.save();
            c.translate(0f, h * 0.15f);
            Rim.plate(c, slab, Keep.number(this, Keep.CLOCK_PLATE, Rim.STEEL), w, h * 0.7f);
            p.setColor(0xFF1A1817);
            c.drawCircle(w * 0.28f, h * 0.35f, r, p);
            c.drawRoundRect(new android.graphics.RectF(w * 0.56f, h * 0.1f, w * 0.94f, h * 0.36f), h * 0.1f, h * 0.1f, p);
            c.drawRoundRect(new android.graphics.RectF(w * 0.56f, h * 0.42f, w * 0.94f, h * 0.6f), h * 0.08f, h * 0.08f, p);
            c.restore();
        } else {
            p.setColor(Tone.containerHigh());
            c.drawCircle(w * 0.28f, h * 0.5f, r, p);
            c.drawRoundRect(new android.graphics.RectF(w * 0.56f, h * 0.25f, w * 0.94f, h * 0.5f), h * 0.1f, h * 0.1f, p);
            c.drawRoundRect(new android.graphics.RectF(w * 0.56f, h * 0.56f, w * 0.94f, h * 0.75f), h * 0.08f, h * 0.08f, p);
            p.setColor(Tone.primary());
            p.setStrokeWidth(h * 0.03f);
            p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            c.drawLine(w * 0.28f, h * 0.5f, w * 0.28f, h * 0.5f - r * 0.7f, p);
        }
    }

    // ------------------------------------------------------------- swatches

    /** Draws one choice as what it looks like, in a box of the given size. */
    interface Painter {
        void paint(android.graphics.Canvas canvas, float w, float h, int value);
    }

    private int[] windowValues() {
        int[] values = new int[Shape.WINDOW_NAMES.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        return values;
    }

    /**
     * A row of choices shown as themselves: each a small picture of what it
     * does, with its name under it; the chosen one ringed in the accent. A
     * touch chooses in place — the window at the head follows, and the row
     * stays where it is.
     */
    private View swatches(final String key, int now, final int[] values, String[] names, final Painter painter) {
        android.widget.HorizontalScrollView across = new android.widget.HorizontalScrollView(this);
        across.setHorizontalScrollBarEnabled(false);
        across.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout line = new LinearLayout(this);
        line.setPadding(dp(18), 0, dp(18), dp(8));
        final int[] chosen = {now};
        final List<View> faces = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            LinearLayout one = new LinearLayout(this);
            one.setOrientation(LinearLayout.VERTICAL);
            one.setGravity(Gravity.CENTER_HORIZONTAL);
            one.setPadding(dp(5), 0, dp(5), 0);
            final View face = new View(this) {
                @Override
                protected void onDraw(android.graphics.Canvas canvas) {
                    boolean on = chosen[0] == value;
                    float inset = dp(10);
                    canvas.save();
                    canvas.translate(inset, inset);
                    painter.paint(canvas, getWidth() - 2 * inset, getHeight() - 2 * inset, value);
                    canvas.restore();
                    if (on) {
                        android.graphics.Paint ring = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                        ring.setStyle(android.graphics.Paint.Style.STROKE);
                        ring.setStrokeWidth(dp(2.5f));
                        ring.setColor(Tone.primary());
                        canvas.drawRoundRect(new android.graphics.RectF(dp(1.5f), dp(1.5f), getWidth() - dp(1.5f),
                            getHeight() - dp(1.5f)), dp(20), dp(20), ring);
                    }
                }
            };
            face.setBackground(Tone.box(Tone.container(), dp(20), 0f));
            faces.add(face);
            one.addView(face, new LinearLayout.LayoutParams(dp(84), dp(84)));
            TextView name = new TextView(this);
            name.setText(Words.t(names[i]));
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 13f * scaled);
            name.setTextColor(Tone.faint());
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setPadding(0, dp(6), 0, 0);
            one.addView(name, new LinearLayout.LayoutParams(dp(88), LinearLayout.LayoutParams.WRAP_CONTENT));
            one.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int was = chosen[0];
                    chosen[0] = value;
                    Keep.saveNumber(Tune.this, key, value);
                    Style.read(Tune.this);
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    for (View f : faces) {
                        f.invalidate();
                    }
                    if (room() == CLOCK) {
                        if (Keep.CLOCK_FACE.equals(key)) {
                            fill();
                        } else {
                            showClock();
                        }
                    } else if (sample != null) {
                        sample.show();
                    }
                    /* Glass has sliders of its own: they come and go with it. */
                    if (Keep.RIM_KIND.equals(key) && (was == Rim.GLASS) != (value == Rim.GLASS)) {
                        fill();
                    }
                    /* The framed widget in small is drawn afresh in the new frame. */
                    if (Keep.WIDGET_FRAME.equals(key)) {
                        fill();
                    }
                }
            });
            line.addView(one);
        }
        across.addView(line);
        /* On entering, the chosen one is in sight. */
        final android.widget.HorizontalScrollView row = across;
        final LinearLayout all = line;
        final int at = indexOf(values, now);
        across.post(new Runnable() {
            public void run() {
                if (at >= 0 && at < all.getChildCount()) {
                    View chosenView = all.getChildAt(at);
                    row.scrollTo(Math.max(0, chosenView.getLeft() - (row.getWidth() - chosenView.getWidth()) / 2), 0);
                }
            }
        });
        return across;
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    /** A small plate of a material, a round window cut in it, as the icons wear it. */
    private void paintMaterial(android.graphics.Canvas c, float w, float h, int value) {
        android.graphics.Path plate = Shape.outline(Shape.ROUNDED, w, h);
        if (value == Rim.NONE) {
            android.graphics.Paint flat = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            flat.setColor(Tone.faint());
            c.drawPath(plate, flat);
            return;
        }
        Rim.plate(c, plate, value, w, h);
        android.graphics.Path hole = new android.graphics.Path();
        hole.addOval(new android.graphics.RectF(w * 0.24f, h * 0.24f, w * 0.76f, h * 0.76f),
            android.graphics.Path.Direction.CW);
        android.graphics.Paint dark = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        dark.setColor(Tone.surface());
        c.drawPath(hole, dark);
        Rim.cut(c, hole, w);
        if (value == Rim.GLASS || Rim.glaze) {
            Rim.glaze(c, plate, 0f, 0f, w, h);
        }
    }

    /** The window a plate may have, cut in the chosen material, the accent seen through it. */
    private void paintWindow(android.graphics.Canvas c, float w, float h, int value) {
        android.graphics.Path plate = Shape.outline(Shape.ROUNDED, w, h);
        Rim.plate(c, plate, Rim.kind == Rim.NONE ? Rim.GROUND : Rim.kind, w, h);
        android.graphics.Path hole = new android.graphics.Path();
        float d = Math.min(w, h) * 0.66f;
        float l = (w - d) / 2f;
        float t = (h - d) / 2f;
        if (value == Shape.WINDOW_TILE) {
            hole = Shape.outline(Shape.ROUNDED, w * 0.8f, h * 0.8f);
            hole.offset(w * 0.1f, h * 0.1f);
        } else if (value == Shape.WINDOW_ROUND) {
            hole.addOval(new android.graphics.RectF(l, t, l + d, t + d), android.graphics.Path.Direction.CW);
        } else if (value == Shape.WINDOW_SQUIRCLE) {
            Shape.superellipse(hole, l, t, d, d, 5.0);
        } else {
            for (int i = 0; i <= 360; i++) {
                double a = 2 * Math.PI * i / 360;
                float r = (float) (d / 2f / 1.05f * (1 + 0.05 * Math.cos(12 * a)));
                float x = (float) (w / 2f + r * Math.cos(a));
                float y = (float) (h / 2f + r * Math.sin(a));
                if (i == 0) {
                    hole.moveTo(x, y);
                } else {
                    hole.lineTo(x, y);
                }
            }
            hole.close();
        }
        android.graphics.Paint seen = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        seen.setColor(Tone.primary());
        c.drawPath(hole, seen);
        Rim.cut(c, hole, w);
    }

    /** How a folder lays out what it holds, drawn as small round icons in the accent. */
    private void paintFolder(android.graphics.Canvas c, float w, float h, int value) {
        Stack.sketch(c, w, h, value);
    }

    private View tile(final int shape) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setGravity(Gravity.CENTER_HORIZONTAL);
        made.setPadding(dp(6), 0, dp(6), 0);
        final boolean on = Shape.current == shape;
        View face = new View(this) {
            private final android.graphics.Paint ink = new android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                float side = Math.min(getWidth(), getHeight()) * 0.62f;
                float left = (getWidth() - side) / 2f;
                float top = (getHeight() - side) / 2f;
                canvas.save();
                canvas.translate(left, top);
                ink.setColor(on ? Tone.primary() : Tone.faint());
                if (shape == Shape.SYSTEM) {
                    /* The phone's own outline, as the phone gives it. */
                    android.graphics.Path mine = new android.graphics.Path();
                    try {
                        android.graphics.Path given = new android.graphics.drawable.AdaptiveIconDrawable(
                            null, null).getIconMask();
                        android.graphics.Matrix fit = new android.graphics.Matrix();
                        fit.setScale(side / 100f, side / 100f);
                        given.transform(fit, mine);
                    } catch (RuntimeException none) {
                        mine = Shape.outline(Shape.CIRCLE, side);
                    }
                    canvas.drawPath(mine, ink);
                } else {
                    canvas.drawPath(Shape.outline(shape, side), ink);
                }
                canvas.restore();
            }
        };
        face.setBackground(Tone.box(on ? Tone.containerHigh() : Tone.container(), dp(22), 0f));
        if (on) {
            face.setBackground(Tone.box(Tone.containerHigh(), dp(22), dp(2), Tone.primary()));
        }
        made.addView(face, new LinearLayout.LayoutParams(dp(76), dp(76)));
        TextView name = new TextView(this);
        name.setText(SHAPES[shape]);
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 13f * scaled);
        name.setTextColor(on ? Tone.onSurface() : Tone.faint());
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setPadding(0, dp(6), 0, 0);
        made.addView(name, new LinearLayout.LayoutParams(dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Keep.saveShape(Tune.this, shape);
                Shape.current = shape;
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                int x = rows.getChildCount() > 1 ? rows.getChildAt(1).getScrollX() : 0;
                fill();
                if (rows.getChildCount() > 1) {
                    rows.getChildAt(1).scrollTo(x, 0);
                }
            }
        });
        return made;
    }

    /** The home screen is closed and opened again, from nothing. */
    private void restart() {
        Intent again = Intent.makeRestartActivityTask(new ComponentName(this, Home.class));
        startActivity(again);
        Runtime.getRuntime().exit(0);
    }

    private void hideKeys() {
        InputMethodManager keys = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keys != null && field != null) {
            keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
        if (field != null) {
            field.clearFocus();
        }
    }

    @Override
    public void onBackPressed() {
        if (menu.shown()) {
            menu.hide(true);
            return;
        }
        if (foot.menuShown()) {
            foot.shutMenu(true);
            return;
        }
        if (!path.isEmpty()) {
            path.remove(path.size() - 1);
            show(-1);
            return;
        }
        super.onBackPressed();
    }
}
