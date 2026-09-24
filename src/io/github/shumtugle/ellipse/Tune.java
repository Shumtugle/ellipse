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
        return room == HIDDEN ? "Hidden apps" : room == ICONS ? "Icons" : "";
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
        fillRoom();
        Style.apply(root);
    }

    private void fillRoom() {
        rows.removeAllViews();
        window.setVisibility(room() == ICONS ? View.VISIBLE : View.GONE);
        if (room() == HIDDEN) {
            fillHidden();
            return;
        }
        if (room() == ICONS) {
            fillIcons();
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

    private static final String[] SHAPES = {"The phone's own", "Circle", "Squircle", "Rounded square",
        "Drop, lower right", "Drop, lower left", "Drop, upper left", "Drop, upper right", "Paper tile"};

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
        value.setText(Keep.number(this, key, 100) + "%");
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        value.setTextColor(Tone.primary());
        top.addView(value);
        made.addView(top);
        TextView said = new TextView(this);
        said.setText(about);
        said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        said.setTextColor(Tone.faint());
        made.addView(said);
        made.addView(new Slide(this, least, most, Keep.number(this, key, 100), new Slide.Moved() {
            public void moved(int at, boolean done) {
                value.setText(at + "%");
                if (Keep.ICON_SIZE.equals(key)) {
                    Style.iconScale = at / 100f;
                } else if (Keep.ICON_FILL.equals(key)) {
                    Style.fill = at / 100f;
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
