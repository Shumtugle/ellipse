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
 * further room. A line of what is still to come is shown quietly and does
 * nothing.
 */
public final class Tune extends Activity {

    private static final int ROOM = 0;
    private static final int SWITCH = 1;
    private static final int CHOICE = 2;
    private static final int DEED = 3;
    private static final int SOON = 4;

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

    private static Line soon(String title, String about) {
        return new Line(SOON, -1, title, about, 0, null, 0, null, null);
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

    private static final int RESTART = 1;
    private static final int RESET = 2;
    private static final int DEFAULT = 3;
    private static final int COLOUR = 4;
    private static final int PLACE = 5;

    private static final String SEARCH = "Search settings";
    private static final String RESTART_LINE = "Restart launcher";
    private static final String AGAIN = "Tap again to reset everything";
    private static final String LATER = "Coming in a later version";
    private static final String ALREADY = "Done: the Home button opens Ellipse";

    private static final String[] GRIDS = {"3 \u00D7 4", "4 \u00D7 5", "4 \u00D7 6", "5 \u00D7 5", "5 \u00D7 6", "6 \u00D7 7"};
    private static final int[] GRID_VALUES = {34, 45, 46, 55, 56, 67};

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
                    choice("Widget shelf", "How the widgets to add are laid out", Keep.SHELF_VIEW,
                        Keep.LINES, new String[] {"Lines", "Grid"}, new int[] {Keep.LINES, Keep.PAGES}),
                    choice("Shortcut makers", "How the shortcuts to add are laid out", Keep.MAKERS_VIEW,
                        Keep.LINES, new String[] {"Lines", "Grid"}, new int[] {Keep.LINES, Keep.PAGES})
                };
            case LIST:
                return new Line[] {
                    choice("Layout", "Lines going down, or pages going across", Keep.VIEW_KEY,
                        Keep.LINES, new String[] {"Lines", "Pages"}, new int[] {Keep.LINES, Keep.PAGES}),
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
                    choice("Swipe up", "On bare screens", Keep.ON_UP, Keep.DO_LIST,
                        new String[] {"All apps", "Nothing"}, new int[] {Keep.DO_LIST, Keep.DO_NOTHING}),
                    choice("Swipe down", "On bare screens", Keep.ON_DOWN, Keep.DO_NOTICES,
                        new String[] {"Notifications", "Quick settings", "Nothing"},
                        new int[] {Keep.DO_NOTICES, Keep.DO_QUICK, Keep.DO_NOTHING}),
                    choice("Back", "On bare screens", Keep.ON_BACK, Keep.DO_FRESH,
                        new String[] {"Recent and new", "All apps", "Nothing"},
                        new int[] {Keep.DO_FRESH, Keep.DO_LIST, Keep.DO_NOTHING}),
                    choice("Double tap", "On the empty home screen", Keep.ON_DOUBLE, Keep.DO_LOCK,
                        new String[] {"Lock the phone", "Nothing"}, new int[] {Keep.DO_LOCK, Keep.DO_NOTHING}),
                    choice("Home button", "On bare screens", Keep.ON_HOME, Keep.DO_HOME,
                        new String[] {"Home screen", "All apps", "Nothing"},
                        new int[] {Keep.DO_HOME, Keep.DO_LIST, Keep.DO_NOTHING})
                };
            case BACKUP:
                return new Line[] {
                    soon("Back up", "The whole set-out and the settings, into one file"),
                    soon("Restore", "From a file made here"),
                    soon("Bring in", "The set-out of another home screen, from its backup")
                };
            case LANGUAGE:
                return new Line[] {
                    soon("Language modules", "The words of the home screen in another language")
                };
            case OTHER:
                return new Line[] {
                    deed(Glyph.DESK, "Set as default home app",
                        "Make the phone's Home button open Ellipse", DEFAULT),
                    deed(Glyph.RESTART, "Restart launcher", "Close the home screen and open it again",
                        RESTART),
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
        return room == HIDDEN ? "Hidden apps" : room == ICONS ? "Icons" : room == CLOCK ? "Clock face" : "";
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
        show(0);
    }

    private void build() {
        host = new FrameLayout(this);
        host.setBackgroundColor(Tone.surface());
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
        window.setVisibility(View.GONE);
        root.addView(window);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(0, dp(4), 0, dp(16));
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
                rows.setPadding(0, dp(4), 0, dp(16));
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
        heading.setText(nameOf(room()));
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
        window.setVisibility(room() == ICONS || room() == CLOCK ? View.VISIBLE : View.GONE);
        if (room() == HIDDEN) {
            fillHidden();
            return;
        }
        if (room() == ICONS) {
            fillIcons();
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
        boolean soon = line.kind == SOON;
        if (!soon) {
            made.setBackground(Tone.touch(null, 0f));
        }
        if (drawn) {
            Glyph drawing = new Glyph(this, line.glyph, dp(28));
            drawing.tint(Tone.primary());
            made.addView(drawing);
        }
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(drawn ? dp(28) : 0, 0, dp(12), 0);
        TextView title = new TextView(this);
        title.setText(line.title);
        title.setTextColor(soon ? Tone.faint() : Tone.onSurface());
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * scaled);
        words.addView(title);
        final TextView about = new TextView(this);
        String said = line.kind == DEED && line.room == DEFAULT && isHome() ? ALREADY
            : line.kind == DEED && line.room == PLACE && Keep.here(this)[0].length() > 0 ? Keep.here(this)[0]
            : line.about;
        about.setText(soon ? line.about + ". " + LATER + "." : said);
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
            final TextView value = new TextView(this);
            value.setText(valueOf(line));
            value.setTextColor(Tone.primary());
            value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
            made.addView(value);
            made.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ask(line, made, value);
                }
            });
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

    private int current(Line line) {
        return Keep.number(this, line.key, line.fallback);
    }

    private String valueOf(Line line) {
        int now = current(line);
        for (int i = 0; i < line.values.length; i++) {
            if (line.values[i] == now) {
                return line.names[i];
            }
        }
        return line.names[0];
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
        menu.choose(0, value);
        Keep.saveNumber(this, asking.key, value);
        /* The lock needs the phone's leave, as an accessibility service. */
        if (Keep.ON_DOUBLE.equals(asking.key) && value == Keep.DO_LOCK && !Latch.ready()) {
            openSafely(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        askingValue.setText(valueOf(asking));
        host.postDelayed(new Runnable() {
            public void run() {
                menu.hide(true);
            }
        }, Pace.STEP * 3);
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
            about.setText(AGAIN);
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
    protected void onResume() {
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
        TextView caption = new TextView(this);
        caption.setText("SHAPE");
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
            tiles.addView(tile(i));
        }
        across.addView(tiles);
        rows.addView(across);
        arrive(across, 1);

        caption("SIZE");
        slider("Icons", "How large the icons stand", Keep.ICON_SIZE, 80, 130);
        slider("Fill", "How much of its outline the picture fills", Keep.ICON_FILL, 85, 125);
        caption("NAMES");
        rows.addView(row(toggle("On the screens", "Names under the icons of the home screen",
            Keep.NAMES_SCREENS, true)));
        rows.addView(row(toggle("In the list", "Names under the icons of every app's pages",
            Keep.NAMES_LIST, true)));
        slider("Size of names", "How large the names are drawn", Keep.NAME_SIZE, 80, 140);
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
        caption("TYPEFACE");
        android.widget.HorizontalScrollView faces = new android.widget.HorizontalScrollView(this);
        faces.setHorizontalScrollBarEnabled(false);
        faces.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(dp(18), 0, dp(18), dp(24));
        for (int i = 0; i < Style.FAMILIES.length; i++) {
            chips.addView(faceChip(i));
        }
        faces.addView(chips);
        rows.addView(faces);
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
        caption.setText(text);
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
        name.setText(title);
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
        said.setText(about);
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
        chip.setText(title);
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
    private View faceChip(final int which) {
        TextView chip = new TextView(this);
        chip.setText("Aa  " + Style.FAMILIES[which].replace("sans-serif", "sans").replace('-', ' '));
        chip.setTypeface(android.graphics.Typeface.create(Style.FAMILIES[which], android.graphics.Typeface.NORMAL));
        chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        boolean on = Style.family == which;
        chip.setTextColor(on ? Tone.onAccent() : Tone.onSurface());
        chip.setPadding(dp(18), dp(12), dp(18), dp(12));
        chip.setBackground(Tone.touch(Tone.box(on ? Tone.primary() : Tone.container(), dp(24), 0f), dp(24)));
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        at.rightMargin = dp(8);
        chip.setLayoutParams(at);
        chip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Keep.saveNumber(Tune.this, Keep.FONT, which);
                Style.font(which);
                int y = scroll.getScrollY();
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
        rowsSlider();
        rows.addView(row(toggle("To the edges", "The clock runs past the grid's margins to the screen's edges",
            Keep.CLOCK_EDGE, false)));
        caption("FACE");
        int face = Keep.number(this, Keep.CLOCK_FACE, Home.FACE_FIRST);
        rows.addView(swatches(Keep.CLOCK_FACE, face,
            new int[] {Home.FACE_FIRST, Home.FACE_PLATE, Home.FACE_MENO}, Home.FACE_NAMES,
            new Painter() {
                public void paint(android.graphics.Canvas c, float w, float h, int value) {
                    paintFace(c, w, h, value);
                }
            }));
        if (face == Home.FACE_MENO) {
            caption("GROUND");
            groundSlider();
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
            int[] made = new int[Rim.NAMES.length + 1];
            String[] madeNames = new String[Rim.NAMES.length + 1];
            made[0] = Watch.DARK;
            madeNames[0] = "Dark";
            for (int i = 0; i < Rim.NAMES.length; i++) {
                made[i + 1] = i;
                madeNames[i + 1] = Rim.NAMES[i];
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
            Rim.material(p, value, w, h);
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
    private void groundSlider() {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        TextView name = new TextView(this);
        name.setText("Darkening");
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(Keep.number(this, Keep.CLOCK_GROUND, 10) + "%");
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

    /** How many rows the clock stands in: its form is kept at any of them. */
    private void rowsSlider() {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(dp(24), dp(6), dp(24), dp(6));
        LinearLayout top = new LinearLayout(this);
        TextView name = new TextView(this);
        name.setText("Rows");
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        name.setTextColor(Tone.onSurface());
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = new TextView(this);
        value.setText(String.valueOf(Keep.number(this, Keep.CLOCK_ROWS, 1)));
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        made.addView(new Slide(this, 1, 2, Math.min(2, Keep.number(this, Keep.CLOCK_ROWS, 1)), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(String.valueOf(at));
                if (done) {
                    Keep.saveNumber(Tune.this, Keep.CLOCK_ROWS, at);
                    showClock();
                }
            }
        }));
        rows.addView(made);
    }

    /** The clock the settings describe, standing in the window at the head of the room. */
    private void showClock() {
        window.removeAllViews();
        View clock = Home.timepiece(this, new Almanac.Hand() {
            public void pressed(String which, View from, android.graphics.RectF box) {
            }
        });
        ((Timepiece) clock).weather(Keep.flag(this, Keep.WEATHER, true));
        /* As tall as it will stand on the screen, up to three rows, so its
           form at that height can be seen. */
        int tall = Math.min(2, Math.max(1, Keep.number(this, Keep.CLOCK_ROWS, 1)));
        window.addView(clock, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            dp(tall == 1 ? 132 : 110 * tall)));
    }

    /** A clock face in small: the first as its round dial and its windows; the plate as a slab with dark windows. */
    private void paintFace(android.graphics.Canvas c, float w, float h, int value) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float r = h * 0.28f;
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
            name.setText(names[i]);
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
        android.graphics.Paint ground = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        ground.setColor(Tone.containerHigh());
        c.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, ground);
        android.graphics.Paint dot = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float s = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        switch (value) {
            case Stack.NINE:
                for (int i = 0; i < 9; i++) {
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + (i % 3 - 1) * s * 0.25f, cy + (i / 3 - 1) * s * 0.25f, s * 0.09f, dot);
                }
                break;
            case Stack.RING:
                for (int i = 0; i < 5; i++) {
                    double a = -Math.PI / 2 + 2 * Math.PI * i / 5;
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + (float) Math.cos(a) * s * 0.27f, cy + (float) Math.sin(a) * s * 0.27f,
                        s * 0.1f, dot);
                }
                break;
            case Stack.PILE:
                for (int i = 2; i >= 0; i--) {
                    float at = (1 - i) * s * 0.12f;
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + at, cy + at, s * (0.23f - 0.02f * i), dot);
                }
                break;
            case Stack.FAN:
            case Stack.TOWER:
                boolean fan = value == Stack.FAN;
                float[] off = {-0.22f, 0.22f, 0f};
                for (int k = 0; k < 3; k++) {
                    dot.setColor(k == 2 ? Tone.primary() : Tone.faint());
                    float r = s * (k == 2 ? 0.23f : 0.18f);
                    c.drawCircle(cx + (fan ? off[k] * s : 0f), cy + (fan ? 0f : off[k] * s), r, dot);
                }
                break;
            default:
                for (int i = 0; i < 4; i++) {
                    dot.setColor(i == 0 ? Tone.primary() : Tone.faint());
                    c.drawCircle(cx + (i % 2 == 0 ? -1 : 1) * s * 0.17f, cy + (i < 2 ? -1 : 1) * s * 0.17f,
                        s * 0.14f, dot);
                }
                break;
        }
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
