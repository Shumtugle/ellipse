package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.WallpaperManager;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The home screen, and in this version the whole application.
 *
 * Screens side by side: the wallpaper in a rounded window with a grid of
 * four by five inside it, turned sideways, and under it the bar, which is
 * the dock: four places and the round button that opens the list of every
 * application. A long press on a screen opens its menu beside the finger.
 *
 * The hand: a pull upward anywhere on the screens draws the list of every
 * application after the finger, and a pull downward on the list, while it
 * stands at its top, puts it back; let go, a throw decides by its
 * direction and a slow pull by whether it went more than half way. Back
 * on bare screens lowers what is fresh, which a push upward sends away.
 * Home closes whatever lies open, and on bare screens returns to the home
 * one. An icon carried to the edge of the screen and held there turns the
 * screens under it. What stands on the screen is what the
 * phone itself keeps for each everyday role; nothing is moved, nothing is
 * saved, nothing is set. Everything else arrives later, one thing at a time.
 */
public final class Home extends Activity {

    /** The grid, after the platform's own guidance for a first screen. */
    private static final int COLUMNS = 4;
    private static final int ROWS = 5;
    private static final int DOCK = 4;

    /** The words of this version; a dictionary arrives with the second language. */
    private static final String ALL = "All applications";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Cell> cells = new ArrayList<>();

    private float density;
    private float scaled;
    private float iconSize;

    private Floor root;
    /** The icon being carried from the list to the grid, while it is. */
    private Lift lift;
    private Apps.Door carried;
    private int[] landing;
    /** Where the finger is, and how far the icon has grown out of its line toward it. */
    private float fingerX;
    private float fingerY;
    private ValueAnimator growing;
    private Frame frame;
    private Drawer drawer;
    /** The screens, and the grid of each; the one an icon is carried over, while it is. */
    private Pager screens;
    private final List<Grid> pages = new ArrayList<>();
    private Grid grid;
    private Menu menu;
    private Fresh fresh;
    /** What a vertical pull is doing, while one is under way. */
    private int pull;
    private static final int PULL_OPEN = 1;
    private static final int PULL_CLOSE = 2;
    private static final int PULL_FRESH = 3;
    /** How long a thing put on the phone or changed counts as fresh. */
    private static final long FRESH_FOR = 14L * 24L * 60L * 60L * 1000L;
    /** How long a carried icon waits at the edge before the screens turn. */
    private static final long EDGE_WAIT = 550L;
    private int edgeWay;
    private Grid dock;
    private LinearLayout bar;
    private Blob blob;
    private LauncherApps apps;
    private boolean away;

    private final LauncherApps.Callback watch = new LauncherApps.Callback() {
        public void onPackageRemoved(String name, UserHandle user) {
            later();
        }

        public void onPackageAdded(String name, UserHandle user) {
            later();
        }

        public void onPackageChanged(String name, UserHandle user) {
            later();
        }

        public void onPackagesAvailable(String[] names, UserHandle user, boolean replacing) {
            later();
        }

        public void onPackagesUnavailable(String[] names, UserHandle user, boolean replacing) {
            later();
        }
    };

    // ------------------------------------------------------------ life

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Tone.read(this);
        Keep.settle(this);
        glass();
        build();
        fill();
        arrive();
        apps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        apps.registerCallback(watch, main);
    }

    @Override
    protected void onDestroy() {
        if (apps != null) {
            apps.unregisterCallback(watch);
        }
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Tone.read(this)) {
            tint();
        }
        if (away) {
            away = false;
            settle();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        away = true;
        /* Back from an application, the home screen is found, not the list
           the application was picked from. */
        drawer.sink(false);
        menu.hide(false);
        fresh.close(false);
    }

    @Override
    public void onConfigurationChanged(Configuration changed) {
        super.onConfigurationChanged(changed);
        Tone.read(this);
        build();
        fill();
    }

    /** Home, pressed on the home screen, closes whatever stands over it. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (menu.shown()) {
            menu.hide(true);
            return;
        }
        if (drawer.shown()) {
            drawer.sink(true);
            return;
        }
        if (fresh.shown()) {
            fresh.close(true);
            return;
        }
        /* Home on the home screen, nothing standing over it: back to the
           screen Home belongs to. */
        screens.show(Keep.home(this), true);
    }

    /**
     * Back closes the menu of the list, then the list; there is nowhere
     * further back than the home screen.
     */
    @Override
    public void onBackPressed() {
        if (menu.shown()) {
            menu.hide(true);
            return;
        }
        if (drawer.menuShown()) {
            drawer.shutMenu(true);
            return;
        }
        if (drawer.shown()) {
            drawer.sink(true);
            return;
        }
        if (fresh.shown()) {
            fresh.close(true);
            return;
        }
        if (lift == null) {
            showFresh();
        }
    }

    // ----------------------------------------------------------- window

    /**
     * The window runs under the phone's bars; the frame keeps its content
     * clear of them and paints the ground they stand on.
     */
    private void glass() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
    }

    private void fitBars(View root) {
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int left;
                int top;
                int right;
                int bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    left = bars.left;
                    top = bars.top;
                    right = bars.right;
                    bottom = bars.bottom;
                } else {
                    left = insets.getSystemWindowInsetLeft();
                    top = insets.getSystemWindowInsetTop();
                    right = insets.getSystemWindowInsetRight();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                /* The keyboard only ever rises over the list, so only the
                   list's bar makes room for it; the home screen stays put. */
                int keys = bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    keys = Math.max(bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
                }
                frame.setPadding(left, top, right, bottom);
                drawer.setPadding(left, 0, right, 0);
                drawer.inset(top, keys);
                fresh.inset(top);
                return insets;
            }
        });
        root.requestApplyInsets();
    }

    /** The wallpaper stands still and centred: there is one screen to slide over. */
    private void still() {
        frame.post(new Runnable() {
            public void run() {
                if (frame.getWindowToken() == null) {
                    return;
                }
                WallpaperManager paper = WallpaperManager.getInstance(Home.this);
                paper.setWallpaperOffsetSteps(0f, 0f);
                paper.setWallpaperOffsets(frame.getWindowToken(), 0.5f, 0.5f);
            }
        });
    }

    // ------------------------------------------------------------ build

    private int dp(float value) {
        return Math.round(value * density);
    }

    private void build() {
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        int wide = getResources().getDisplayMetrics().widthPixels;
        int tall = getResources().getDisplayMetrics().heightPixels;
        /* The icon is sized from the short side of the screen, so a turn of
           the phone does not make it grow; the platform's own range holds it. */
        float column = (Math.min(wide, tall) - dp(16)) / (float) COLUMNS;
        iconSize = Math.max(dp(48), Math.min(dp(64), column * 0.58f));

        boolean was = drawer != null && drawer.shown();
        root = new Floor(this);
        root.carrier(carrier);
        root.hand(pulling);
        frame = new Frame(this, dp(24));
        root.addView(frame, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout stage = new FrameLayout(this);
        stage.setClipChildren(false);
        frame.addView(stage, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        frame.cut(stage);

        screens = new Pager(this);
        stage.addView(screens, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        /* The bar is the dock: four places in the pill at the foot, no
           names, and the round button at its end is the way into every
           application, marked with six dots, as a grid of them would be. */
        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        bar.setPadding(dp(4), dp(8), dp(10), dp(8));
        bar.setClipChildren(false);

        dock = new Grid(this, DOCK, 1);
        bar.addView(dock, new LinearLayout.LayoutParams(0,
            Math.round(iconSize + dp(16)), 1f));

        blob = new Blob(this, iconSize, Blob.GRID);
        blob.setContentDescription(ALL);
        blob.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawer.rise();
            }
        });
        LinearLayout.LayoutParams blobParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blobParams.leftMargin = dp(4);
        bar.addView(blob, blobParams);

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.setMargins(dp(8), dp(6), dp(8), dp(10));
        frame.addView(bar, barParams);

        drawer = new Drawer(this, iconSize, new Drawer.Opener() {
            public void open(View from, Apps.Door door, int[] icon) {
                launch(from, door, icon);
            }

            public void lift(View from, Apps.Door door, int[] icon, float rawX, float rawY) {
                pick(from, door, icon, rawX, rawY);
            }

            public void view(int view) {
                Keep.saveView(Home.this, view);
            }

            public void order(int order) {
                Keep.saveOrder(Home.this, order);
                drawer.reorder(new Apps(Home.this).all(order));
            }
        });
        root.addView(drawer, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        drawer.order(Keep.order(this), Keep.view(this));

        fresh = new Fresh(this, iconSize, new Fresh.Hand() {
            public void open(View from, Apps.Door door, int[] icon) {
                launch(from, door, icon);
            }

            public void lift(View from, Apps.Door door, int[] icon) {
                pick(from, door, icon, root.fingerX() + rootLeft(), root.fingerY() + rootTop());
            }
        });
        root.addView(fresh, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        menu = new Menu(this, root, new Menu.Listener() {
            public void picked(int section, int key) {
                act(key);
            }

            public void closing() {
            }
        });
        if (was) {
            drawer.rise();
        }

        tint();
        setContentView(root);
        fitBars(root);
        still();
    }

    /** Everything that wears a colour, in the colours of the moment. */
    private void tint() {
        frame.tint();
        bar.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        blob.tint();
        drawer.tint();
    }

    // ------------------------------------------------------------- fill

    /** The roles of the dock, in the order they stand. */
    private static Intent[] dockRoles() {
        return new Intent[] {
            new Intent(Intent.ACTION_DIAL),
            category(Intent.CATEGORY_APP_MESSAGING),
            category(Intent.CATEGORY_APP_GALLERY),
            new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        };
    }

    /**
     * The rows of the grid nearest the dock, top to bottom, left to right;
     * a null leaves its place empty.
     */
    private static Intent[][] gridRoles() {
        return new Intent[][] {
            {
                new Intent(Intent.ACTION_VIEW, Uri.parse("https:")),
                null,
                null,
                null
            },
            {
                category(Intent.CATEGORY_APP_CALENDAR),
                new Intent(AlarmClock.ACTION_SHOW_ALARMS),
                category(Intent.CATEGORY_APP_MAPS),
                category(Intent.CATEGORY_APP_FILES)
            },
            {
                category(Intent.CATEGORY_APP_MARKET),
                category(Intent.CATEGORY_APP_EMAIL),
                category(Intent.CATEGORY_APP_MUSIC),
                new Intent(Settings.ACTION_SETTINGS)
            }
        };
    }

    private static Intent category(String name) {
        return new Intent(Intent.ACTION_MAIN).addCategory(name);
    }

    /**
     * Puts on the screen what the owner set down by hand, then what the
     * phone keeps for each role in the places still free. A role with no
     * answer leaves its place empty: nothing is put there in its stead.
     */
    private void fill() {
        int showing = pages.isEmpty() ? Keep.home(this) : screens.page();
        screens.removeAllViews();
        pages.clear();
        dock.removeAllViews();
        cells.clear();
        Apps found = new Apps(this);
        Set<String> taken = new HashSet<>();
        List<Keep.Spot> spots = Keep.placed(this);

        int count = Keep.screens(this);
        for (Keep.Spot spot : spots) {
            count = Math.max(count, spot.screen + 1);
        }
        for (int i = 0; i < count; i++) {
            Grid page = new Grid(this, COLUMNS, ROWS);
            page.setPadding(dp(8), dp(16), dp(8), 0);
            page.shape(iconSize, Cell.below(this));
            page.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    ask(v);
                    return true;
                }
            });
            screens.addView(page);
            pages.add(page);
        }
        grid = pages.get(0);

        int roles = Math.min(Keep.roles(this), pages.size() - 1);
        boolean[][] held = new boolean[COLUMNS][ROWS];
        for (Keep.Spot spot : spots) {
            if (spot.x < 0 || spot.y < 0 || spot.x >= COLUMNS || spot.y >= ROWS) {
                continue;
            }
            Apps.Door door = found.door(spot.name);
            if (door == null) {
                continue;
            }
            place(pages.get(spot.screen), door, spot.x, spot.y, true);
            if (spot.screen == roles) {
                held[spot.x][spot.y] = true;
            }
            taken.add(spot.name.getPackageName());
        }

        Intent[] docked = dockRoles();
        for (int i = 0; i < docked.length; i++) {
            place(dock, found.role(docked[i], taken), i, 0, false);
        }
        Intent[][] rows = gridRoles();
        int first = ROWS - rows.length;
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length; c++) {
                if (rows[r][c] != null && !held[c][first + r]) {
                    place(pages.get(roles), found.role(rows[r][c], taken), c, first + r, true);
                }
            }
        }
        drawer.fill(found.all(Keep.order(this)));
        screens.home(Keep.home(this));
        screens.show(showing, false);
    }

    // ------------------------------------------------------------- menu

    private static final String[] ASKS = {
        "Add screen", "Add shortcut", "Add widget", "Add folder", "Make home screen"
    };
    private static final int ADD_SCREEN = 0;
    private static final int ADD_SHORTCUT = 1;
    private static final int ADD_WIDGET = 2;
    private static final int ADD_FOLDER = 3;
    private static final int MAKE_HOME = 4;

    /**
     * A long press on a screen: its menu grows out of the fingertip. On a
     * screen that is not the home one, it offers to make it so.
     */
    private void ask(View on) {
        boolean home = screens.page() == Keep.home(this);
        int count = home ? 4 : 5;
        String[] lines = new String[count];
        int[] keys = new int[count];
        for (int i = 0; i < count; i++) {
            lines[i] = ASKS[i];
            keys[i] = i;
        }
        on.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        menu.show(new Menu.Section[] {new Menu.Section(null, lines, keys)},
            root.fingerX(), root.fingerY(), dp(20));
    }

    private void act(int key) {
        menu.hide(true);
        switch (key) {
            case ADD_SCREEN:
                /* A new screen is added at the end, and the screens slide
                   over to it: an empty page is shown, not announced. */
                int count = pages.size() + 1;
                Keep.saveScreens(this, count);
                fill();
                screens.show(count - 1, true);
                break;
            case MAKE_HOME:
                Keep.saveHome(this, screens.page());
                screens.home(screens.page());
                screens.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                    ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
                break;
            default:
                /* Shortcuts, widgets and folders arrive one at a time; until
                   then the line answers with the phone's own short no. */
                refuse(screens);
                break;
        }
    }

    private void place(Grid into, Apps.Door door, int column, int row, boolean named) {
        if (door == null) {
            return;
        }
        final Cell cell = new Cell(this, door, iconSize, named);
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                open(cell);
            }
        });
        into.put(cell, column, row);
        cells.add(cell);
    }

    /** Package changes come in bursts; the screen is filled again once they settle. */
    private void later() {
        main.removeCallbacks(refill);
        main.postDelayed(refill, 300L);
    }

    private final Runnable refill = new Runnable() {
        public void run() {
            fill();
        }
    };

    // ------------------------------------------------------------- open

    /** The application grows out of its own icon. */
    private void open(Cell cell) {
        int[] on = cell.iconBounds();
        int[] at = new int[2];
        cell.getLocationOnScreen(at);
        launch(cell, cell.door, new int[] {on[0] - at[0], on[1] - at[1], on[2], on[3]});
    }

    /** Opens a door; the icon is given in the coordinates of the view it stands in. */
    private void launch(View from, Apps.Door door, int[] icon) {
        int[] at = new int[2];
        from.getLocationOnScreen(at);
        Rect bounds = new Rect(at[0] + icon[0], at[1] + icon[1],
            at[0] + icon[0] + icon[2], at[1] + icon[1] + icon[3]);
        Bundle grow = ActivityOptions.makeClipRevealAnimation(from,
            icon[0], icon[1], icon[2], icon[3]).toBundle();
        try {
            ((LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE))
                .startMainActivity(door.name, door.user, bounds, grow);
            Keep.opened(this, door.name);
        } catch (RuntimeException gone) {
            refuse(from);
            later();
        }
    }

    /** A refusal is felt, not read: the phone's own short no. */
    private void refuse(View v) {
        v.performHapticFeedback(Build.VERSION.SDK_INT >= 30
            ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
    }

    // ---------------------------------------------------------- carrying

    private final Floor.Carrier carrier = new Floor.Carrier() {
        public boolean carrying() {
            return lift != null && carried != null;
        }

        public void move(float x, float y) {
            hold(x, y);
        }

        public void drop(float x, float y, boolean kept) {
            set(kept);
        }
    };

    /** How much larger a carried icon is than one set down. */
    private static final float LIFTED = 1.12f;

    /** How far above the fingertip a carried icon rides, so the finger never hides it. */
    private float above() {
        return iconSize * 0.7f;
    }

    /**
     * A held line gives up its icon. It grows from the line into the size
     * it will have on the grid, rides a little above the finger, and the
     * list is swallowed into that fingertip.
     */
    private void pick(View from, Apps.Door door, int[] icon, float rawX, float rawY) {
        if (lift != null) {
            return;
        }
        int[] floorAt = new int[2];
        root.getLocationOnScreen(floorAt);
        float x = rawX - floorAt[0];
        float y = rawY - floorAt[1];

        int[] rowAt = new int[2];
        from.getLocationOnScreen(rowAt);
        float fromX = rowAt[0] - floorAt[0] + icon[0] + icon[2] / 2f;
        float fromY = rowAt[1] - floorAt[1] + icon[1] + icon[3] / 2f;

        carried = door;
        grid = pages.get(screens.page());
        lift = new Lift(this, carried.icon(), Math.round(iconSize));
        root.addView(lift, new FrameLayout.LayoutParams(lift.size(), lift.size()));
        fingerX = x;
        fingerY = y;
        final Lift held = lift;
        final float startX = fromX;
        final float startY = fromY;
        final float start = icon[2] / iconSize;
        held.at(startX, startY);
        held.setScaleX(start);
        held.setScaleY(start);
        /* The icon leaves its line and meets the finger wherever the finger
           has gone by then, growing to its size on the grid on the way. */
        growing = ValueAnimator.ofFloat(0f, 1f);
        growing.setDuration(Pace.ARRIVE / 2);
        growing.setInterpolator(Pace.EMPHASIS);
        growing.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float p = (Float) animation.getAnimatedValue();
                held.at(startX + (fingerX - startX) * p,
                    startY + (fingerY - above() - startY) * p);
                float s = start + (LIFTED - start) * p;
                held.setScaleX(s);
                held.setScaleY(s);
            }
        });
        growing.start();

        from.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (drawer.shown()) {
            drawer.swallow(x, y);
        }
        if (fresh.shown()) {
            fresh.close(true);
        }
        grid.carrying(true);
        hold(x, y);
    }

    /** The icon follows the finger; the grid shows where it would land. */
    private void hold(float x, float y) {
        if (lift == null) {
            return;
        }
        fingerX = x;
        fingerY = y;
        edge(x);
        float cx = x;
        float cy = y - above();
        if (growing == null || !growing.isRunning()) {
            lift.at(cx, cy);
        }
        int[] gridAt = new int[2];
        int[] floorAt = new int[2];
        grid.getLocationOnScreen(gridAt);
        root.getLocationOnScreen(floorAt);
        float gx = cx - (gridAt[0] - floorAt[0]);
        float gy = cy - (gridAt[1] - floorAt[1]);
        int[] under = grid.cellAt(gx, gy);
        int[] spot = null;
        if (under != null) {
            spot = grid.free(under[0], under[1]) ? under : grid.nearestFree(gx, gy);
        }
        if (spot != null && (landing == null || spot[0] != landing[0] || spot[1] != landing[1])) {
            grid.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        landing = spot;
        grid.target(spot);
    }

    /**
     * The finger lifts. Over a free place the icon settles into it and
     * stays; anywhere else it fades where it was let go, and nothing
     * is changed.
     */
    private void set(boolean kept) {
        final Lift going = lift;
        final Apps.Door door = carried;
        final int[] spot = kept ? landing : null;
        lift = null;
        carried = null;
        landing = null;
        edge(-1f);
        if (growing != null) {
            growing.cancel();
            growing = null;
        }
        grid.carrying(false);
        if (going == null) {
            return;
        }
        if (spot == null) {
            going.animate().cancel();
            going.animate().scaleX(0.4f).scaleY(0.4f).alpha(0f)
                .setDuration(Pace.ARRIVE / 2).setInterpolator(Pace.EMPHASIS)
                .withEndAction(new Runnable() {
                    public void run() {
                        root.removeView(going);
                    }
                }).start();
            return;
        }
        int[] gridAt = new int[2];
        int[] floorAt = new int[2];
        grid.getLocationOnScreen(gridAt);
        root.getLocationOnScreen(floorAt);
        float[] c = grid.centre(spot[0], spot[1]);
        float tx = gridAt[0] - floorAt[0] + c[0] - going.size() / 2f;
        float ty = gridAt[1] - floorAt[1] + c[1] - going.size() / 2f;
        Keep.place(this, door.name, screens.page(), spot[0], spot[1]);
        going.animate().cancel();
        going.animate().translationX(tx).translationY(ty).scaleX(1f).scaleY(1f)
            .setDuration(Pace.ARRIVE).setInterpolator(Pace.SPRING)
            .withEndAction(new Runnable() {
                public void run() {
                    fill();
                    root.removeView(going);
                }
            }).start();
        grid.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    // ------------------------------------------------------------- edges

    private float rootLeft() {
        int[] at = new int[2];
        root.getLocationOnScreen(at);
        return at[0];
    }

    private float rootTop() {
        int[] at = new int[2];
        root.getLocationOnScreen(at);
        return at[1];
    }

    /**
     * A carried icon near a side edge waits there a moment, then the
     * screens turn toward that side; still held there, they go on turning.
     */
    private void edge(float x) {
        int way = 0;
        float band = dp(28);
        if (x >= 0f && lift != null) {
            if (x < band) {
                way = -1;
            } else if (x > root.getWidth() - band) {
                way = 1;
            }
        }
        if (way == edgeWay) {
            return;
        }
        edgeWay = way;
        main.removeCallbacks(turnAtEdge);
        if (way != 0) {
            main.postDelayed(turnAtEdge, EDGE_WAIT);
        }
    }

    private final Runnable turnAtEdge = new Runnable() {
        public void run() {
            if (lift == null || edgeWay == 0) {
                return;
            }
            int to = screens.page() + edgeWay;
            if (to < 0 || to >= pages.size()) {
                return;
            }
            grid.carrying(false);
            grid.target(null);
            screens.show(to, true);
            grid = pages.get(to);
            grid.carrying(true);
            landing = null;
            grid.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            hold(fingerX, fingerY);
            main.postDelayed(this, EDGE_WAIT + Pace.ARRIVE);
        }
    };

    // ------------------------------------------------------------ fresh

    /**
     * Lowers what is fresh: up to eight applications last opened from here,
     * and up to twelve put on the phone or changed in the last two weeks,
     * the newest first, each with a word about it.
     */
    private void showFresh() {
        Apps found = new Apps(this);
        List<Apps.Door> recent = new ArrayList<>();
        for (android.content.ComponentName name : Keep.recent(this)) {
            Apps.Door door = found.door(name);
            if (door != null) {
                recent.add(door);
            }
        }
        long since = System.currentTimeMillis() - FRESH_FOR;
        List<Apps.Door> lately = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        android.content.pm.PackageManager manager = getPackageManager();
        for (Apps.Door door : found.all(Keep.UPDATED)) {
            if (door.when < since || lately.size() >= 12) {
                continue;
            }
            String note = door.installed >= since ? "new" : "updated";
            String owner = door.name.getPackageName();
            try {
                if (manager.checkSignatures(getPackageName(), owner)
                    == android.content.pm.PackageManager.SIGNATURE_MATCH) {
                    note = note + "  " + manager.getPackageInfo(owner, 0).versionName;
                }
            } catch (Exception unknown) {
                // A version that cannot be read is simply not said.
            }
            lately.add(door);
            notes.add(note);
        }
        fresh.show(recent, lately, notes);
    }

    // ------------------------------------------------------------- pull

    /** Whom a vertical pull serves, decided once, when it begins. */
    private final Floor.Hand pulling = new Floor.Hand() {
        public boolean pullable(boolean up) {
            pull = 0;
            if (lift != null || menu.shown() || drawer.menuShown()) {
                return false;
            }
            if (fresh.shown()) {
                if (up) {
                    pull = PULL_FRESH;
                }
            } else if (drawer.shown()) {
                if (!up && drawer.atTop()) {
                    pull = PULL_CLOSE;
                }
            } else if (up) {
                pull = PULL_OPEN;
                drawer.begin();
            }
            return pull != 0;
        }

        public void pulled(float by) {
            float tall = Math.max(1f, root.getHeight());
            if (pull == PULL_OPEN) {
                drawer.drag(-by / tall);
            } else if (pull == PULL_CLOSE) {
                drawer.drag(1f - by / tall);
            } else if (pull == PULL_FRESH) {
                fresh.drag(1f + by / Math.max(1f, fresh.sheetHeight()));
            }
        }

        public void released(float by, float speed) {
            float tall = Math.max(1f, root.getHeight());
            float throwing = android.view.ViewConfiguration.get(Home.this)
                .getScaledMinimumFlingVelocity() * 6f;
            if (pull == PULL_OPEN) {
                boolean open = speed < -throwing || (speed <= throwing && -by / tall > 0.5f);
                drawer.let(open);
            } else if (pull == PULL_CLOSE) {
                boolean close = speed > throwing || (speed >= -throwing && by / tall > 0.5f);
                drawer.let(!close);
            } else if (pull == PULL_FRESH) {
                boolean away = speed < -throwing
                    || -by > Math.max(1f, fresh.sheetHeight()) * 0.25f;
                fresh.let(away);
            }
            pull = 0;
        }
    };

    // ----------------------------------------------------------- motion

    /**
     * The first arrival: the icons rise out of the dock one after another,
     * the dock first, then the grid row by row upward, as if the bar had
     * poured them onto the screen.
     */
    private void arrive() {
        List<Cell> order = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.getParent() == dock) {
                order.add(cell);
            }
        }
        for (int row = ROWS - 1; row >= 0; row--) {
            for (Cell cell : cells) {
                if (cell.getParent() != dock && ((int[]) cell.getTag())[1] == row) {
                    order.add(cell);
                }
            }
        }
        float rise = dp(28);
        for (int i = 0; i < order.size(); i++) {
            Cell cell = order.get(i);
            cell.setAlpha(0f);
            cell.setTranslationY(rise);
            cell.setScaleX(0.86f);
            cell.setScaleY(0.86f);
            cell.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(120L + i * Pace.STEP / 2)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    /**
     * Coming back from an application: the grid is found a little far away
     * and comes to its place, while the application shrinks back into it.
     */
    private void settle() {
        screens.setScaleX(0.94f);
        screens.setScaleY(0.94f);
        screens.setAlpha(0.6f);
        screens.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setStartDelay(0L).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).start();
    }
}
