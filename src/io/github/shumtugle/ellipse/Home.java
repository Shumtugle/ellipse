package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.WallpaperManager;
import android.animation.ValueAnimator;
import android.content.Context;
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
    /** The grid of the screens, as the settings have it. */
    private int columns = 4;
    private int rows = 5;
    /** The settings as last read, so a change made in them is noticed on return. */
    private int stamp = -1;
    private static final int DOCK = 4;

    /** The words of this version; a dictionary arrives with the second language. */
    private static final String ALL = "All applications";

    private final Handler main = new Handler(Looper.getMainLooper());
    /** Everything set out on the screens and in the dock, in the order it was set out. */
    private final List<View> cells = new ArrayList<>();

    private float density;
    private float scaled;
    private float iconSize;

    private Floor root;
    /** The icon being carried from the list to the grid, while it is. */
    private Lift lift;
    /** What is carried, as it will be kept; how many places it takes; where it came from, if a screen. */
    private String carried;
    private int carriedAcross = 1;
    private int carriedDown = 1;
    private int[] origin;
    private boolean wideLift;
    /** What each thing on the screens is kept as, and the set-out as it stands, for its first move. */
    private final java.util.Map<View, String> things = new java.util.HashMap<>();
    /** What each folder on the screens and in the dock holds, for its dot. */
    private final java.util.Map<View, List<Apps.Door>> insides = new java.util.HashMap<>();
    private final List<Keep.Spot> standing = new ArrayList<>();
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
    private Tray tray;
    /** Other applications' widgets: the shelf they are chosen from, and the host they live in. */
    private Shelf shelf;
    /** The whole screen that shortcuts are chosen from. */
    private Chooser chooser;
    /** The frame things are reshaped in. */
    private Reach reach;
    /** What the one menu is showing now: the screen's own, a thing's, or the list of shortcut makers. */
    private int menuFor;
    private static final int MENU_SCREEN = 0;
    private static final int MENU_THING = 1;
    private static final int MENU_MAKERS = 2;
    /** The thing whose menu is open, and where it stands. */
    private String offerToken;
    private int[] offerWhence;
    private View offerView;
    private Apps.Door offerDoor;
    private List<android.content.pm.ShortcutInfo> offerShortcuts = new ArrayList<>();
    private List<android.content.pm.LauncherActivityInfo> makers = new ArrayList<>();
    /** Where the dock or a folder of one's own would take what is carried. */
    private int dockSlot = -1;
    /** What each dock place holds now, as it is kept; empty for nothing. */
    private final String[] dockHeld = new String[DOCK];
    private View folderHover;
    /** The folder of one's own that stands open, if one does. */
    private int trayFolder = -1;
    /** Widgets already made, kept across settings-out so they do not blink. */
    private final java.util.Map<Integer, android.appwidget.AppWidgetHostView> widgetViews = new java.util.HashMap<>();
    private LauncherApps launcher;
    /** The clock on the screens now, the charge its headphones tell, and whether they are listened to. */
    private Almanac clockView;
    private int earsLevel = -1;
    private boolean listening;
    private static final int ASK_WORLD = 31;
    private static final int ASK_SHORTCUT = 13;
    private static final int KEY_INFO = 1;
    private static final int KEY_UNINSTALL = 2;
    private static final int KEY_REMOVE = 3;
    private static final int KEY_RESIZE = 4;
    private static final int KEY_RENAME = 5;
    private static final int KEY_SHORTCUT = 100;
    private static final String APP_INFO = "App info";
    private static final String UNINSTALL = "Uninstall";
    private static final String REMOVE = "Remove";
    private static final String RESIZE = "Resize";
    private static final String RENAME = "Rename";
    private static final String NEW_FOLDER = "Folder";
    private static final String OWN_SETTINGS = "Ellipse settings";
    private static final String CLOCK_NAME = "Clock";
    private static final int OWN_MAKER = 1000;
    private float carryStartX;
    private float carryStartY;
    private boolean carryMoved;
    private Piece.Host host;
    private android.appwidget.AppWidgetManager widgets;
    /** A widget on its way to a screen, while the phone asks leave for it or it is set up. */
    private int pendingWidget = -1;
    private int pendingPage;
    static final int WIDGET_HOST = 0x454C;
    private static final int ASK_BIND = 11;
    private static final int ASK_SETUP = 12;
    private static final String WIDGET = "#widget:";
    /** What a vertical pull is doing, while one is under way. */
    private int pull;
    private static final int PULL_OPEN = 1;
    private static final int PULL_CLOSE = 2;
    private static final int PULL_FRESH = 3;
    private static final int PULL_BAR = 4;
    /** Whether the phone's own shade was already asked for during this pull. */
    private boolean barAsked;
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
            Keep.purge(Home.this, name, Apps.serialOf(user));
            dropDeadWidgets();
            later();
        }

        public void onPackageAdded(String name, UserHandle user) {
            if (Keep.flag(Home.this, Keep.AUTO_ADD, false)) {
                setDown(name, Apps.serialOf(user));
            }
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
        widgets = android.appwidget.AppWidgetManager.getInstance(this);
        launcher = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        host = new Piece.Host(getApplicationContext(), WIDGET_HOST);
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
        if (Keep.zoom(this) != sizedAt) {
            recreate();
            return;
        }
        if (Keep.stamp(this) != stamp) {
            Tone.read(this);
            build();
            fill();
        } else if (Tone.read(this)) {
            tint();
        }
        listen();
        Notices.tell(new Runnable() {
            public void run() {
                dots();
            }
        });
        dots();
        /* What other applications asked to set on the home screen meanwhile. */
        List<String> asked = Keep.takeQueue(this);
        stamp = Keep.stamp(this);
        for (String token : asked) {
            if (token.startsWith(WIDGET)) {
                try {
                    pendingPage = Keep.home(this);
                    setWidget(Integer.parseInt(token.substring(WIDGET.length()).split(":")[0]));
                } catch (NumberFormatException broken) {
                    // Nothing to set.
                }
            } else {
                setAnywhere(token, Keep.home(this));
            }
        }
        if (away) {
            away = false;
            settle();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            host.startListening();
        } catch (RuntimeException busy) {
            // The widgets will draw themselves once the service answers.
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unlisten();
        try {
            host.stopListening();
        } catch (RuntimeException busy) {
            // Nothing was listening.
        }
        shelf.close(false);
        chooser.close(false);
        if (reach != null && !reach.ended()) {
            reach.end();
        }
        away = true;
        /* Back from an application, the home screen is found, not the list
           the application was picked from. */
        drawer.sink(false);
        menu.hide(false);
        fresh.close(false);
        tray.close(false);
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
        if (reach != null && !reach.ended()) {
            reach.end();
            return;
        }
        if (menu.shown()) {
            menu.hide(true);
            return;
        }
        if (shelf.shown()) {
            shelf.close(true);
            return;
        }
        if (chooser.shown()) {
            chooser.close(true);
            return;
        }
        if (tray.shown()) {
            tray.close(true);
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
        /* Home on the home screen, nothing standing over it: what the
           settings ask for, by default back to the screen Home belongs to. */
        int deed = Keep.number(this, Keep.ON_HOME, Keep.DO_HOME);
        if (deed == Keep.DO_HOME) {
            screens.show(Keep.home(this), true);
        } else if (deed == Keep.DO_LIST) {
            drawer.rise();
        }
    }

    /**
     * Back closes the menu of the list, then the list; there is nowhere
     * further back than the home screen.
     */
    @Override
    public void onBackPressed() {
        if (reach != null && !reach.ended()) {
            reach.end();
            return;
        }
        if (menu.shown()) {
            menu.hide(true);
            return;
        }
        if (shelf.shown()) {
            shelf.close(true);
            return;
        }
        if (chooser.shown()) {
            chooser.close(true);
            return;
        }
        if (drawer.menuShown()) {
            drawer.shutMenu(true);
            return;
        }
        if (tray.shown()) {
            tray.close(true);
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
            int deed = Keep.number(this, Keep.ON_BACK, Keep.DO_FRESH);
            if (deed == Keep.DO_FRESH) {
                showFresh();
            } else if (deed == Keep.DO_LIST) {
                drawer.rise();
            }
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
                tray.setPadding(left, top, right, bottom);
                shelf.setPadding(left, 0, right, 0);
                shelf.inset(top, keys);
                chooser.setPadding(left, 0, right, 0);
                chooser.inset(top, keys);
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
                boolean moves = Keep.flag(Home.this, Keep.WALL_MOVES, true) && pages.size() > 1;
                paper.setWallpaperOffsetSteps(moves ? 1f / (pages.size() - 1) : 0f, 0f);
                float at = moves ? screens.page() / (float) (pages.size() - 1) : 0.5f;
                paper.setWallpaperOffsets(frame.getWindowToken(), at, 0.5f);
            }
        });
    }

    // ------------------------------------------------------------ build

    private int dp(float value) {
        return Math.round(value * density);
    }

    /**
     * The screen that was in front before the screens were built anew: a
     * change of settings, of the phone's shape or of the colour builds them
     * again, and the owner comes back to the screen they were on, not to the
     * first one.
     */
    private int restore = -1;

    private void build() {
        if (screens != null && !pages.isEmpty()) {
            restore = screens.page();
        }
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        int wide = getResources().getDisplayMetrics().widthPixels;
        int tall = getResources().getDisplayMetrics().heightPixels;
        /* The icon is sized from the short side of the screen, so a turn of
           the phone does not make it grow; the platform's own range holds it. */
        int grid = Keep.number(this, Keep.DESK_GRID, 45);
        columns = Keep.columns(grid);
        rows = Keep.rows(grid);
        stamp = Keep.stamp(this);
        float column = (Math.min(wide, tall) - dp(16)) / (float) columns;
        iconSize = Math.max(dp(48), Math.min(dp(64), column * 0.58f));
        /* The owner's own size, within the cell. */
        iconSize = Math.min(column * 0.86f, iconSize * Style.iconScale);

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
        /* The wallpaper goes along with the screens, a little, as through a window. */
        screens.across(new Pager.Across() {
            public void across(float fraction) {
                if (frame == null || frame.getWindowToken() == null
                    || !Keep.flag(Home.this, Keep.WALL_MOVES, true) || pages.size() < 2) {
                    return;
                }
                try {
                    WallpaperManager.getInstance(Home.this)
                        .setWallpaperOffsets(frame.getWindowToken(), fraction, 0.5f);
                } catch (RuntimeException none) {
                    // The wallpaper stays where it was.
                }
            }
        });
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
        dock.shape(iconSize, 0f);
        bar.addView(dock, new LinearLayout.LayoutParams(0,
            Math.round(iconSize + dp(16)), 1f));

        blob = new Blob(this, iconSize, Blob.GRID);
        blob.shaped(true);
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
        /* Without the dock the screens reach down to the foot; the list of
           every application is still a pull upward away. */
        bar.setVisibility(Keep.flag(this, Keep.DOCK, true) ? View.VISIBLE : View.GONE);
        screens.dots(Keep.flag(this, Keep.DOTS, true));
        screens.endless(Keep.flag(this, Keep.DESK_ENDLESS, false));

        drawer = new Drawer(this, iconSize, new Drawer.Opener() {
            public void open(View from, Apps.Door door, int[] icon) {
                launch(from, door, icon);
            }

            public void lift(final View from, final Apps.Door door, final int[] icon, float rawX, float rawY) {
                offerNew(from, door, icon, null);
            }

            public void settings(View from) {
                tune(from);
            }

            public void home() {
                drawer.sink(true);
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
                offerNew(from, door, icon, null);
            }
        });
        root.addView(fresh, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        tray = new Tray(this, iconSize, new Tray.Hand() {
            public void open(View from, Apps.Door door, int[] icon) {
                launch(from, door, icon);
            }

            public void lift(View from, Apps.Door door, int[] icon) {
                offerNew(from, door, icon, trayFolder > 0 ? new int[] {-2, trayFolder} : null);
            }
        });
        root.addView(tray, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        shelf = new Shelf(this, new Shelf.Hand() {
            public int[] span(android.appwidget.AppWidgetProviderInfo info) {
                return widgetSpan(info);
            }

            public void chosen(android.appwidget.AppWidgetProviderInfo info) {
                shelf.close(true);
                takeWidget(info);
            }

            public void settings() {
                tune(screens);
            }
        });
        root.addView(shelf, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        chooser = new Chooser(this, iconSize, new Chooser.Hand() {
            public void chosen(int key) {
                chooser.close(true);
                make(key);
            }

            public void settings() {
                tune(screens);
            }
        });
        root.addView(chooser, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        reach = null;

        menu = new Menu(this, root, new Menu.Listener() {
            public void picked(int section, int key) {
                if (menuFor == MENU_THING) {
                    offered(section == Menu.HEAD ? KEY_INFO : key);
                } else if (menuFor == MENU_MAKERS) {
                    menu.hide(true);
                    make(key);
                } else {
                    act(key);
                }
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

    private static Intent category(String name) {
        return new Intent(Intent.ACTION_MAIN).addCategory(name);
    }

    /** The words of the default set-out. */
    private static final String SYSTEM = "System";
    private static final String OWN = "Ellipse";

    /**
     * Puts on the screens what the owner set down by hand, then the
     * default set-out in the places still free. On the home screen, the
     * clock across the top and four everyday applications at the foot; on
     * the screen before it, a folder of the applications signed by the same
     * hand as the phone's store; on the screen after it, a folder of the
     * phone's own applications, the phone's settings and this home screen's
     * own. A role with no answer leaves its place empty: nothing is put
     * there in its stead.
     */
    private void fill() {
        int showing = restore >= 0 ? restore : (pages.isEmpty() ? Keep.home(this) : screens.page());
        restore = -1;
        screens.removeAllViews();
        pages.clear();
        dock.removeAllViews();
        cells.clear();
        things.clear();
        insides.clear();
        standing.clear();
        Apps found = new Apps(this);
        Set<String> taken = new HashSet<>();
        List<Keep.Spot> spots = Keep.placed(this);
        boolean laid = Keep.laid(this);

        int count = Keep.screens(this);
        for (Keep.Spot spot : spots) {
            count = Math.max(count, spot.screen + 1);
        }
        for (int i = 0; i < count; i++) {
            Grid page = new Grid(this, columns, rows);
            page.setPadding(dp(8), dp(16), dp(8), 0);
            page.shape(iconSize, Cell.below(this));
            final android.view.GestureDetector twice = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(android.view.MotionEvent e) {
                        doubleTap();
                        return true;
                    }
                });
            page.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    twice.onTouchEvent(event);
                    return false;
                }
            });
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

        /* Applications first, since folders the phone fills leave out
           whatever already stands somewhere. */
        List<Keep.Spot> own = new ArrayList<>();
        for (Keep.Spot spot : spots) {
            if (spot.x < 0 || spot.y < 0 || spot.x >= columns || spot.y >= rows) {
                continue;
            }
            if (spot.name == null) {
                own.add(spot);
                continue;
            }
            Apps.Door door = found.door(spot.token);
            Grid page = pages.get(spot.screen);
            if (door == null || !page.free(spot.x, spot.y)) {
                continue;
            }
            place(page, door, spot.x, spot.y, true);
            taken.add(spot.name.getPackageName());
        }
        /* Apps in folders of one's own count as standing somewhere too. */
        for (Keep.Spot spot : own) {
            if (spot.token.startsWith(Keep.FOLDER_THING)) {
                for (String item : Keep.folderItems(this, folderId(spot.token))) {
                    android.content.ComponentName inside = Apps.nameOf(item);
                    if (inside != null) {
                        taken.add(inside.getPackageName());
                    }
                }
            }
        }

        /* The dock: each place holds what was put there by hand, stays empty
           if it was emptied, or else is filled by its everyday role. */
        Intent[] docked = dockRoles();
        for (int i = 0; i < docked.length; i++) {
            String slot = Keep.dockSlot(this, i);
            dockHeld[i] = "";
            if (slot != null && (slot.equals(Keep.OWN_THING) || slot.startsWith(Keep.SHORTCUT_THING))) {
                Cell cell = slot.equals(Keep.OWN_THING) ? ownCell(false) : pinnedCell(slot, false);
                if (cell != null) {
                    dockThing(cell, slot, i);
                }
                continue;
            }
            if (slot != null && isFolder(slot)) {
                dockThing(dockFolder(slot, found, taken), slot, i);
                continue;
            }
            if (slot != null && slot.startsWith(WIDGET)) {
                View piece = dockWidget(slot);
                if (piece != null) {
                    dockThing(piece, slot, i);
                }
                continue;
            }
            Apps.Door door;
            if (slot == null) {
                door = found.role(docked[i], taken);
            } else if (slot.length() == 0) {
                door = null;
            } else {
                door = found.door(slot);
                if (door != null) {
                    taken.add(door.name.getPackageName());
                }
            }
            place(dock, door, i, 0, false);
        }

        int home = Math.min(Keep.roles(this), count - 1);
        if (laid) {
            boolean clocked = false;
            List<Keep.Spot> folders = new ArrayList<>();
            for (Keep.Spot spot : own) {
                Grid page = pages.get(spot.screen);
                if (Keep.CLOCK_THING.equals(spot.token)) {
                    clocked = clock(page, spot.y) || clocked;
                } else if (Keep.OWN_THING.equals(spot.token)) {
                    if (page.free(spot.x, spot.y)) {
                        ownDoor(page, spot.x, spot.y);
                    }
                } else if (spot.token.startsWith(WIDGET)) {
                    widget(page, spot);
                } else if (spot.token.startsWith(Keep.SHORTCUT_THING)) {
                    pinned(page, spot);
                } else if (spot.token.startsWith(Keep.FOLDER_THING)) {
                    int id = folderId(spot.token);
                    List<Apps.Door> inside = new ArrayList<>();
                    for (String item : Keep.folderItems(this, id)) {
                        Apps.Door door = found.door(item);
                        if (door != null) {
                            inside.add(door);
                        }
                    }
                    if (page.free(spot.x, spot.y)) {
                        folder(page, Keep.folderName(this, id), inside, spot.x, spot.y, spot.token);
                    }
                } else {
                    folders.add(spot);
                }
            }
            if (!clocked) {
                clock(pages.get(home), 0);
            }
            List<Apps.Door> vendor = found.vendor(taken);
            for (Keep.Spot spot : folders) {
                if (Keep.VENDOR_THING.equals(base(spot.token)) && !vendor.isEmpty()
                    && pages.get(spot.screen).free(spot.x, spot.y)) {
                    folder(pages.get(spot.screen), Apps.vendorName(vendor), vendor, spot.x, spot.y,
                        spot.token);
                }
            }
            for (Apps.Door door : vendor) {
                taken.add(door.name.getPackageName());
            }
            List<Apps.Door> system = found.system(taken);
            for (Keep.Spot spot : folders) {
                if (Keep.SYSTEM_THING.equals(base(spot.token)) && !system.isEmpty()
                    && pages.get(spot.screen).free(spot.x, spot.y)) {
                    folder(pages.get(spot.screen), SYSTEM, system, spot.x, spot.y, spot.token);
                }
            }
        } else {
            Grid middle = pages.get(home);
            clock(middle, 0);
            Intent[] everyday = {
                new Intent(Intent.ACTION_VIEW, Uri.parse("https:")),
                category(Intent.CATEGORY_APP_MARKET),
                category(Intent.CATEGORY_APP_CALENDAR),
                category(Intent.CATEGORY_APP_MAPS)
            };
            for (int c = 0; c < Math.min(everyday.length, columns); c++) {
                if (middle.free(c, rows - 1)) {
                    place(middle, found.role(everyday[c], taken), c, rows - 1, true);
                }
            }
            if (home + 1 < count) {
                Grid after = pages.get(home + 1);
                Apps.Door settings = found.role(new Intent(Settings.ACTION_SETTINGS), taken);
                if (after.free(1, rows - 1)) {
                    place(after, settings, 1, rows - 1, true);
                }
                if (after.free(2, rows - 1)) {
                    ownDoor(after, 2, rows - 1);
                }
            }
            List<Apps.Door> vendor = found.vendor(taken);
            if (home - 1 >= 0 && !vendor.isEmpty() && pages.get(home - 1).free(0, rows - 1)) {
                folder(pages.get(home - 1), Apps.vendorName(vendor), vendor, 0, rows - 1,
                    Keep.VENDOR_THING);
            }
            for (Apps.Door door : vendor) {
                taken.add(door.name.getPackageName());
            }
            List<Apps.Door> system = found.system(taken);
            if (home + 1 < count && !system.isEmpty() && pages.get(home + 1).free(0, rows - 1)) {
                folder(pages.get(home + 1), SYSTEM, system, 0, rows - 1, Keep.SYSTEM_THING);
            }
        }

        java.util.Set<String> hidden = Keep.hidden(this);
        List<Apps.Door> listed = new ArrayList<>();
        for (Apps.Door door : found.all(Keep.order(this))) {
            if (!hidden.contains(door.token())) {
                listed.add(door);
            }
        }
        int listGrid = Keep.number(this, Keep.LIST_GRID, 45);
        drawer.grid(Keep.columns(listGrid), Keep.rows(listGrid),
            Keep.flag(this, Keep.LIST_ENDLESS, false), Keep.flag(this, Keep.LIST_DOTS, true));
        drawer.fill(listed);
        screens.home(Keep.home(this));
        screens.show(Math.max(0, Math.min(pages.size() - 1, showing)), false);
        dots();
    }

    /** Puts a point on every icon whose app has a notification standing, and on its folder. */
    private void dots() {
        boolean on = Keep.flag(this, Keep.DOTS_ON, false) && Notices.on();
        Set<String> marked = on ? Notices.marked() : new HashSet<String>();
        for (View view : cells) {
            if (!(view instanceof Cell)) {
                continue;
            }
            Cell cell = (Cell) view;
            boolean lit = false;
            if (cell.door != null) {
                lit = marked.contains(Notices.mark(cell.door.name.getPackageName(), cell.door.serial));
            } else if (insides.containsKey(cell)) {
                for (Apps.Door door : insides.get(cell)) {
                    if (marked.contains(Notices.mark(door.name.getPackageName(), door.serial))) {
                        lit = true;
                        break;
                    }
                }
            }
            cell.dot(lit);
        }
    }

    private static final String LOCK_CAPTION = "Lock on double tap";
    private static final String LOCK_TEXT = "To lock the phone, Ellipse has to be turned on among the phone's "
        + "accessibility services. It reads nothing on the screen.";
    private static final String OPEN_SETTINGS = "Open settings";

    /** A double tap on the empty home screen: the phone is locked, if the settings ask for it. */
    private void doubleTap() {
        if (Keep.number(this, Keep.ON_DOUBLE, Keep.DO_LOCK) != Keep.DO_LOCK) {
            return;
        }
        if (Latch.lock()) {
            return;
        }
        Ask.tell(root, LOCK_CAPTION, LOCK_TEXT, OPEN_SETTINGS, new Runnable() {
            public void run() {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (RuntimeException none) {
                    refuse(screens);
                }
            }
        });
    }

    /**
     * Something set out on a screen: remembered as what it will be kept as,
     * counted in the set-out as it stands, and made movable by a long press.
     */
    private void stand(final Grid page, final View thing, String token) {
        things.put(thing, token);
        int[] at = (int[]) thing.getTag();
        standing.add(new Keep.Spot(token, pages.indexOf(page), at[0], at[1]));
        thing.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                offerStanding(v);
                return true;
            }
        });
    }

    /** The clock across a whole row, if the settings want it and the row is free. */
    private boolean clock(Grid page, int row) {
        if (!Keep.flag(this, Keep.CLOCK, true) || !page.free(0, row, columns, 1)) {
            return false;
        }
        Almanac clock = new Almanac(this, new Almanac.Hand() {
            public void pressed(String window, View from, android.graphics.RectF box) {
                look(window, from, box);
            }
        });
        page.put(clock, 0, row, columns, 1);
        cells.add(clock);
        clock.weather(Keep.flag(this, Keep.WEATHER, true));
        clock.ears(Keep.flag(this, Keep.EARS, true) ? earsLevel : -1);
        clockView = clock;
        stand(page, clock, Keep.CLOCK_THING);
        return true;
    }

    /** The door to this home screen's own settings. */
    /** The door to this home screen's settings: not its own icon, but a face of their own. */
    private Cell ownCell(boolean named) {
        Cell own = new Cell(this, Shape.face(getDrawable(R.mipmap.door)), OWN, iconSize,
            named && Style.namesOnScreens);
        own.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tune(v);
            }
        });
        return own;
    }

    private void ownDoor(Grid page, int column, int row) {
        Cell own = ownCell(true);
        page.put(own, column, row);
        cells.add(own);
        stand(page, own, Keep.OWN_THING);
    }

    /** Whether a block of places on a grid is all free. */
    private static boolean free(Grid grid, int x, int y, int across, int down) {
        for (int c = x; c < x + across; c++) {
            for (int r = y; r < y + down; r++) {
                if (!grid.free(c, r)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A folder on a screen: its face, and the card it opens into. */
    private void folder(Grid into, final CharSequence name, final List<Apps.Door> doors,
                        int column, int row, String token) {
        int[] span = folderSpan(token);
        int across = Math.min(columns - column, span[0]);
        int down = Math.min(rows - row, span[1]);
        if (across * down > 1 && into.free(column, row, across, down)) {
            float cell = (getResources().getDisplayMetrics().widthPixels - dp(16)) / (float) columns;
            float small = Math.min(iconSize * 0.62f, (cell / 2f - dp(8)) * 0.86f);
            final Nest nest = new Nest(this, name, doors, across, down, small, new Nest.Hand() {
                public void open(View from, Apps.Door door, int[] icon) {
                    launch(from, door, icon);
                }

                public void more(View from) {
                    trayFolder = folderId(things.get(from));
                    int[] at = new int[2];
                    int[] floorAt = new int[2];
                    from.getLocationOnScreen(at);
                    root.getLocationOnScreen(floorAt);
                    tray.show(name, doors, at[0] - floorAt[0] + from.getWidth() / 2f,
                        at[1] - floorAt[1] + from.getHeight() / 2f);
                }

                public void hold(View which) {
                    offerStanding(which);
                }
            });
            into.put(nest, column, row, across, down);
            cells.add(nest);
            stand(into, nest, token);
            nest.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    offerStanding(v);
                    return true;
                }
            });
            return;
        }
        final Cell cell = new Cell(this, new Stack(doors), name, iconSize, Style.namesOnScreens);
        insides.put(cell, doors);
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int[] at = new int[2];
                int[] floorAt = new int[2];
                cell.getLocationOnScreen(at);
                root.getLocationOnScreen(floorAt);
                int[] icon = cell.localIcon();
                trayFolder = folderId(things.get(cell));
                tray.show(name, doors, at[0] - floorAt[0] + icon[0] + icon[2] / 2f,
                    at[1] - floorAt[1] + icon[1] + icon[3] / 2f);
            }
        });
        into.put(cell, column, row);
        cells.add(cell);
        stand(into, cell, token);
    }

    /** The settings, grown out of the door that leads to them. */
    private void tune(View from) {
        Intent open = new Intent(this, Tune.class);
        startActivity(open, ActivityOptions.makeClipRevealAnimation(from, 0, 0,
            from.getWidth(), from.getHeight()).toBundle());
    }

    /** A window of the clock was pressed: what it shows about is opened out of it. */
    private void look(String window, View from, android.graphics.RectF box) {
        if (Almanac.WEATHER.equals(window)) {
            /* The weather, whole, on its own page; the first time, its place. */
            startActivity(new Intent(this, Folio.class).putExtra(Folio.PAGE, Folio.WEATHER));
            return;
        }
        Intent open;
        if (Almanac.DIAL.equals(window)) {
            open = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        } else if (Almanac.TIME.equals(window)) {
            open = category(Intent.CATEGORY_APP_CALENDAR);
        } else if (Almanac.EARS.equals(window)) {
            open = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        } else {
            open = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        }
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(open, ActivityOptions.makeClipRevealAnimation(from,
                Math.round(box.left), Math.round(box.top),
                Math.round(box.width()), Math.round(box.height())).toBundle());
        } catch (RuntimeException none) {
            refuse(from);
        }
    }

    // ------------------------------------------------------------- menu

    private static final String[] ASKS = {
        "Add screen", "Add shortcut", "Add widget", "Add folder", "Make home screen", "Settings",
        "Remove screen"
    };
    private static final int REMOVE_SCREEN = 6;
    /** Where the screen's menu was asked for. */
    private float askX;
    private float askY;
    private static final int ADD_SCREEN = 0;
    private static final int ADD_SHORTCUT = 1;
    private static final int ADD_WIDGET = 2;
    private static final int ADD_FOLDER = 3;
    private static final int MAKE_HOME = 4;
    private static final int SETTINGS = 5;

    /**
     * A long press on a screen: its menu grows out of the fingertip. On a
     * screen that is not the home one, it offers to make it so.
     */
    private void ask(View on) {
        boolean home = screens.page() == Keep.home(this);
        List<Integer> offered = new ArrayList<>();
        for (int i = ADD_SCREEN; i <= ADD_FOLDER; i++) {
            offered.add(i);
        }
        if (!home) {
            offered.add(MAKE_HOME);
        }
        if (pages.size() > 1 && pages.get(screens.page()).getChildCount() == 0) {
            offered.add(REMOVE_SCREEN);
        }
        menuFor = MENU_SCREEN;
        askX = root.fingerX();
        askY = root.fingerY();
        String[] lines = new String[offered.size()];
        int[] keys = new int[offered.size()];
        for (int i = 0; i < offered.size(); i++) {
            lines[i] = ASKS[offered.get(i)];
            keys[i] = offered.get(i);
        }
        on.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        /* The settings stand apart, under a hairline: they lead away. */
        menu.show(new Menu.Section[] {new Menu.Section(null, lines, keys),
                settingsLine(ASKS[SETTINGS], SETTINGS)},
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
            case SETTINGS:
                tune(screens);
                break;
            case ADD_WIDGET:
                pendingPage = screens.page();
                shelf.show(Keep.number(this, Keep.SHELF_VIEW, Keep.LINES) == Keep.PAGES);
                break;
            case ADD_SHORTCUT:
                pendingPage = screens.page();
                showMakers();
                break;
            case ADD_FOLDER:
                newFolder();
                break;
            case REMOVE_SCREEN:
                if (!Keep.laid(this)) {
                    Keep.lay(this, standing);
                }
                int gone = screens.page();
                Keep.dropScreen(this, gone);
                fill();
                screens.show(Math.max(0, gone - 1), true);
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

    /** A thing of the home screen's own, or a pinned shortcut, standing in a dock place. */
    private void dockThing(final View cell, final String token, final int slot) {
        dock.put(cell, slot, 0);
        cells.add(cell);
        dockHeld[slot] = token;
        cell.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                offerThing(cell, token, new int[] {-1, slot}, null);
                return true;
            }
        });
    }

    /** Whether a kept word names a folder, the phone's or one's own. */
    private static boolean isFolder(String token) {
        String word = base(token);
        return Keep.VENDOR_THING.equals(word) || Keep.SYSTEM_THING.equals(word)
            || token.startsWith(Keep.FOLDER_THING);
    }

    /** What a folder holds, found afresh. */
    private List<Apps.Door> folderDoors(String token, Apps found, Set<String> taken) {
        String word = base(token);
        if (Keep.VENDOR_THING.equals(word)) {
            return found.vendor(taken);
        }
        if (Keep.SYSTEM_THING.equals(word)) {
            return found.system(taken);
        }
        List<Apps.Door> inside = new ArrayList<>();
        for (String item : Keep.folderItems(this, folderId(token))) {
            Apps.Door door = found.door(item);
            if (door != null) {
                inside.add(door);
            }
        }
        return inside;
    }

    private String folderName(String token, List<Apps.Door> doors) {
        String word = base(token);
        if (Keep.VENDOR_THING.equals(word)) {
            return Apps.vendorName(doors);
        }
        if (Keep.SYSTEM_THING.equals(word)) {
            return SYSTEM;
        }
        return Keep.folderName(this, folderId(token));
    }

    /** A folder standing in a dock place: its face, without a name, and the card it opens into. */
    private Cell dockFolder(final String token, Apps found, Set<String> taken) {
        final List<Apps.Door> doors = folderDoors(token, found, taken);
        final String name = folderName(token, doors);
        final Cell cell = new Cell(this, new Stack(doors), name, iconSize, false);
        insides.put(cell, doors);
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int[] at = new int[2];
                int[] floorAt = new int[2];
                cell.getLocationOnScreen(at);
                root.getLocationOnScreen(floorAt);
                int[] icon = cell.localIcon();
                trayFolder = folderId(token);
                tray.show(name, doors, at[0] - floorAt[0] + icon[0] + icon[2] / 2f,
                    at[1] - floorAt[1] + icon[1] + icon[3] / 2f);
            }
        });
        return cell;
    }

    /** A widget of one place standing in the dock. */
    private View dockWidget(String token) {
        int id;
        try {
            id = Integer.parseInt(token.substring(WIDGET.length()).split(":")[0]);
        } catch (RuntimeException broken) {
            return null;
        }
        android.appwidget.AppWidgetProviderInfo info = widgets.getAppWidgetInfo(id);
        if (info == null) {
            return null;
        }
        android.appwidget.AppWidgetHostView view = widgetViews.get(id);
        if (view == null) {
            view = host.createView(this, id, info);
            widgetViews.put(id, view);
        } else if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        return view;
    }

    private void place(Grid into, Apps.Door door, int column, int row, boolean named) {
        if (door == null) {
            return;
        }
        if (into == dock) {
            dockHeld[column] = door.token();
        }
        final Cell cell = new Cell(this, door, iconSize, named && (into == dock || Style.namesOnScreens));
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                open(cell);
            }
        });
        into.put(cell, column, row);
        cells.add(cell);
        if (into != dock) {
            stand(into, cell, door.token());
        } else {
            final int slot = column;
            final String token = door.token();
            cell.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    offerThing(cell, token, new int[] {-1, slot}, door);
                    return true;
                }
            });
        }
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
            Keep.opened(this, door.token());
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
        return wideLift ? 0f : iconSize * 0.7f;
    }

    /**
     * A held line gives up its icon. It grows from the line into the size
     * it will have on the grid, rides a little above the finger, and the
     * list is swallowed into that fingertip.
     */
    private int[] whenceNew;

    private void pick(View from, Apps.Door door, int[] icon, float rawX, float rawY) {
        carry(from, door.token(), door.icon(), icon, 1, 1, whenceNew, rawX, rawY);
    }

    /**
     * Something standing on a screen, held long, is taken up to be set
     * down elsewhere: an icon rides above the finger as any carried icon
     * does, the clock is held up whole under it. The first such move keeps
     * the default set-out as it stands, from then on as the owner's own.
     */
    private void move(View thing) {
        String token = things.get(thing);
        if (token == null || lift != null || !(thing.getParent() instanceof Grid)) {
            return;
        }
        Grid page = (Grid) thing.getParent();
        int[] at = (int[]) thing.getTag();
        int across = at.length > 2 ? at[2] : 1;
        int down = at.length > 3 ? at[3] : 1;
        if (!Keep.laid(this)) {
            Keep.lay(this, standing);
        }
        android.graphics.drawable.Drawable face;
        int[] icon;
        if (thing instanceof Cell) {
            face = ((Cell) thing).drawable();
            icon = ((Cell) thing).localIcon();
        } else {
            android.graphics.Bitmap picture = android.graphics.Bitmap.createBitmap(
                Math.max(1, thing.getWidth()), Math.max(1, thing.getHeight()),
                android.graphics.Bitmap.Config.ARGB_8888);
            thing.draw(new android.graphics.Canvas(picture));
            face = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
            icon = new int[] {0, 0, thing.getWidth(), thing.getHeight()};
        }
        int[] screen = new int[2];
        thing.getLocationOnScreen(screen);
        float rawX = root.fingerX() + rootLeft();
        float rawY = root.fingerY() + rootTop();
        int[] from = {pages.indexOf(page), at[0], at[1]};
        carry(thing, token, face, icon, across, down, from, rawX, rawY);
        page.removeView(thing);
    }

    private void carry(View from, String token, android.graphics.drawable.Drawable face, int[] icon,
                       int across, int down, int[] whence, float rawX, float rawY) {
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

        carried = token;
        carriedAcross = across;
        carriedDown = down;
        origin = whence;
        wideLift = across * down > 1;
        grid = pages.get(screens.page());
        lift = wideLift ? new Lift(this, face, icon[2], icon[3])
            : new Lift(this, face, Math.round(iconSize));
        root.addView(lift, new FrameLayout.LayoutParams(lift.wide(), lift.tall()));
        fingerX = x;
        fingerY = y;
        final Lift held = lift;
        final float startX = fromX;
        final float startY = fromY;
        final float start = wideLift ? 1f : icon[2] / iconSize;
        final float lifted = wideLift ? 1.04f : LIFTED;
        held.at(startX, startY);
        held.setScaleX(start);
        held.setScaleY(start);
        /* The thing leaves its place and meets the finger wherever the
           finger has gone by then, growing to its carried size on the way. */
        growing = ValueAnimator.ofFloat(0f, 1f);
        growing.setDuration(Pace.ARRIVE / 2);
        growing.setInterpolator(Pace.EMPHASIS);
        growing.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float p = (Float) animation.getAnimatedValue();
                held.at(startX + (fingerX - startX) * p,
                    startY + (fingerY - above() - startY) * p);
                float s = start + (lifted - start) * p;
                held.setScaleX(s);
                held.setScaleY(s);
            }
        });
        growing.start();

        screens.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (drawer.shown()) {
            drawer.swallow(x, y);
        }
        if (fresh.shown()) {
            fresh.close(true);
        }
        if (tray.shown()) {
            tray.close(true);
        }
        grid.carrying(true);
        carryStartX = x;
        carryStartY = y;
        carryMoved = false;
        hold(x, y);
    }

    /** The thing follows the finger; the grid, the dock or a folder shows where it would go. */
    private void hold(float x, float y) {
        if (lift == null) {
            return;
        }
        fingerX = x;
        fingerY = y;
        /* Only once the thing is truly carried away does the bin come up:
           the round button at the dock's end turns into it. */
        if (!carryMoved && Math.hypot(x - carryStartX, y - carryStartY) > dp(32)) {
            carryMoved = true;
            if (origin != null && bar.getVisibility() == View.VISIBLE) {
                blob.bin(true);
            }
        }
        edge(x);
        float cx = x;
        float cy = y - above();
        if (growing == null || !growing.isRunning()) {
            lift.at(cx, cy);
        }
        boolean app = Apps.nameOf(carried) != null;
        boolean small = carriedAcross == 1 && carriedDown == 1;
        boolean dockable = app || Keep.OWN_THING.equals(carried) || carried.startsWith(Keep.SHORTCUT_THING)
            || (small && isFolder(carried)) || (small && carried.startsWith(WIDGET));
        /* Over the bin, nothing else is offered: the thing is about to go. */
        boolean binned = blob.binning() && over(blob, x, y, dp(14));
        blob.binOver(binned);
        lift.setAlpha(binned ? 0.55f : 1f);
        int slot = -1;
        if (!binned && dockable && bar.getVisibility() == View.VISIBLE && over(bar, x, y, dp(10))) {
            int[] at = new int[2];
            int[] floorAt = new int[2];
            dock.getLocationOnScreen(at);
            root.getLocationOnScreen(floorAt);
            float dx = x - (at[0] - floorAt[0]) - dock.getPaddingLeft();
            slot = Math.max(0, Math.min(DOCK - 1, (int) (dx / Math.max(1f, dock.cellWidth()))));
        }
        if (slot != dockSlot) {
            dockSlot = slot;
            dock.carrying(slot >= 0);
            dock.target(slot >= 0 ? new int[] {slot, 0} : null);
            if (slot >= 0) {
                dock.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
        }
        View hover = null;
        if (!binned && slot < 0 && app) {
            hover = folderUnder(cx, cy);
        }
        if (hover != folderHover) {
            if (folderHover != null) {
                folderHover.animate().scaleX(1f).scaleY(1f).setDuration(Pace.PRESS).start();
            }
            folderHover = hover;
            if (hover != null) {
                hover.animate().scaleX(1.14f).scaleY(1.14f).setDuration(Pace.PRESS)
                    .setInterpolator(Pace.SPRING).start();
                hover.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
        }
        if (binned || slot >= 0 || hover != null) {
            landing = null;
            grid.target(null);
            return;
        }
        int[] gridAt = new int[2];
        int[] floorAt = new int[2];
        grid.getLocationOnScreen(gridAt);
        root.getLocationOnScreen(floorAt);
        float gx = cx - (gridAt[0] - floorAt[0]);
        float gy = cy - (gridAt[1] - floorAt[1]);
        int[] spot = grid.landing(gx, gy, carriedAcross, carriedDown);
        if (spot != null && (landing == null || spot[0] != landing[0] || spot[1] != landing[1])) {
            grid.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        landing = spot;
        grid.target(spot, carriedAcross, carriedDown);
    }

    /** Whether a point of the floor is over a view, with some room to spare. */
    private boolean over(View view, float x, float y, float grace) {
        int[] at = new int[2];
        int[] floorAt = new int[2];
        view.getLocationOnScreen(at);
        root.getLocationOnScreen(floorAt);
        float left = at[0] - floorAt[0];
        float top = at[1] - floorAt[1];
        return x > left - grace && x < left + view.getWidth() + grace
            && y > top - grace && y < top + view.getHeight() + grace;
    }

    /** A folder of one's own under a point of the floor, on the screen in front. */
    private View folderUnder(float x, float y) {
        for (java.util.Map.Entry<View, String> each : things.entrySet()) {
            View view = each.getKey();
            if (view.getParent() != grid || !each.getValue().startsWith(Keep.FOLDER_THING)) {
                continue;
            }
            if (origin != null && origin[0] == -2 && origin[1] == folderId(each.getValue())) {
                continue;
            }
            if (over(view, x, y, 0)) {
                return view;
            }
        }
        return null;
    }

    /**
     * The finger lifts. Over the bin the thing goes; over the dock an app
     * takes that place, and what stood there goes where the app came from;
     * over a folder of one's own the app goes into it; over a free place on
     * a screen the thing settles there. Anywhere else a thing brought from
     * a list fades where it was let go, and a thing taken from a place goes
     * back to it.
     */
    private void set(boolean kept) {
        final Lift going = lift;
        final String token = carried;
        final int[] whence = origin;
        final boolean binned = kept && whence != null && blob.binOver();
        final int slot = kept ? dockSlot : -1;
        final View into = kept ? folderHover : null;
        final int[] spot = kept && !binned && slot < 0 && into == null ? landing : null;
        final int across = carriedAcross;
        final int down = carriedDown;
        lift = null;
        carried = null;
        origin = null;
        landing = null;
        dockSlot = -1;
        if (folderHover != null) {
            folderHover.animate().scaleX(1f).scaleY(1f).setDuration(Pace.PRESS).start();
            folderHover = null;
        }
        dock.carrying(false);
        dock.target(null);
        blob.bin(false);
        edge(-1f);
        if (growing != null) {
            growing.cancel();
            growing = null;
        }
        grid.carrying(false);
        if (going == null) {
            return;
        }
        if (binned) {
            removeThing(token, whence);
            flyInto(going, blob, 0.15f, false);
            screens.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            return;
        }
        if (slot >= 0) {
            String was = dockToken(slot);
            Keep.saveDockSlot(this, slot, token);
            if (whence != null && whence[0] >= 0) {
                if (!Keep.laid(this)) {
                    Keep.lay(this, standing);
                }
                Keep.remove(this, whence[0], whence[1], whence[2]);
                if (was != null && was.length() > 0) {
                    Keep.place(this, was, whence[0], whence[1], whence[2]);
                }
            } else if (whence != null && whence[0] == -1 && whence[1] != slot) {
                Keep.saveDockSlot(this, whence[1], was == null ? "" : was);
            } else if (whence != null && whence[0] == -2) {
                Keep.folderRemove(this, whence[1], token);
            }
            flyInto(going, dock.getChildCount() > 0 ? dock : bar, 1f, true);
            grid.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            return;
        }
        if (into != null) {
            Keep.folderAdd(this, folderId(things.get(into)), token);
            leave(token, whence);
            flyInto(going, into, 0.3f, true);
            grid.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
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
            if (whence != null) {
                fill();
            }
            return;
        }
        int[] gridAt = new int[2];
        int[] floorAt = new int[2];
        grid.getLocationOnScreen(gridAt);
        root.getLocationOnScreen(floorAt);
        float[] c = grid.middle(spot[0], spot[1], across, down);
        float tx = gridAt[0] - floorAt[0] + c[0] - going.wide() / 2f;
        float ty = gridAt[1] - floorAt[1] + c[1] - going.tall() / 2f;
        if (whence != null && whence[0] >= 0) {
            Keep.shift(this, whence[0], whence[1], whence[2], screens.page(), spot[0], spot[1]);
        } else {
            Keep.place(this, token, screens.page(), spot[0], spot[1]);
            leave(token, whence);
        }
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

    /** A thing set down elsewhere leaves the dock place or the folder it was taken from. */
    private void leave(String token, int[] whence) {
        if (whence == null) {
            return;
        }
        if (whence[0] == -1) {
            Keep.saveDockSlot(this, whence[1], "");
        } else if (whence[0] == -2) {
            Keep.folderRemove(this, whence[1], token);
        }
    }

    /** The carried picture flies into a view and is gone; the screens are set out again. */
    private void flyInto(final Lift going, View target, float scale, final boolean refill) {
        int[] at = new int[2];
        int[] floorAt = new int[2];
        target.getLocationOnScreen(at);
        root.getLocationOnScreen(floorAt);
        float tx = at[0] - floorAt[0] + target.getWidth() / 2f - going.wide() / 2f;
        float ty = at[1] - floorAt[1] + target.getHeight() / 2f - going.tall() / 2f;
        going.animate().cancel();
        going.animate().translationX(tx).translationY(ty).scaleX(scale).scaleY(scale)
            .alpha(refill ? 0.9f : 0f).setDuration(Pace.ARRIVE / 2).setInterpolator(Pace.EMPHASIS)
            .withEndAction(new Runnable() {
                public void run() {
                    root.removeView(going);
                    fill();
                }
            }).start();
    }

    /** What stands in a dock place now, by hand or by role; empty when nothing does. */
    private String dockToken(int slot) {
        String held = slot >= 0 && slot < DOCK ? dockHeld[slot] : null;
        return held == null ? "" : held;
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

    // --------------------------------------------------- taking off, reshaping

    /**
     * A thing dropped on the bin is taken off its screen. A widget lets go
     * of its place with its application; the clock is switched off in the
     * settings, where it can be switched on again.
     */
    private void removeThing(String token, int[] whence) {
        if (whence[0] == -1) {
            Keep.saveDockSlot(this, whence[1], "");
            if (token.startsWith(WIDGET)) {
                try {
                    drop(Integer.parseInt(token.substring(WIDGET.length()).split(":")[0]));
                } catch (NumberFormatException broken) {
                    // Nothing to let go.
                }
            }
            fill();
            return;
        }
        if (whence[0] == -2) {
            Keep.folderRemove(this, whence[1], token);
            fill();
            return;
        }
        if (!Keep.laid(this)) {
            Keep.lay(this, standing);
        }
        Keep.remove(this, whence[0], whence[1], whence[2]);
        if (token.startsWith(Keep.SHORTCUT_THING)) {
            unpin(token);
        }
        if (token.startsWith(WIDGET)) {
            try {
                drop(Integer.parseInt(token.substring(WIDGET.length()).split(":")[0]));
            } catch (NumberFormatException broken) {
                // Nothing to let go.
            }
        } else if (Keep.CLOCK_THING.equals(token)) {
            Keep.saveFlag(this, Keep.CLOCK, false);
            stamp = Keep.stamp(this);
        }
        fill();
    }

    /** The word before the size, for things kept with one. */
    private static String base(String token) {
        if (token.startsWith(WIDGET)) {
            return WIDGET;
        }
        if (token.startsWith(Keep.FOLDER_THING)) {
            String[] part = token.split(":");
            return Keep.FOLDER_THING + (part.length > 1 ? part[1] : "0");
        }
        int colon = token.indexOf(':');
        return colon < 0 ? token : token.substring(0, colon);
    }

    /** The size a folder is kept with: one place, unless it says more. */
    private static int[] folderSpan(String token) {
        String[] part = token.split(":");
        int first = token.startsWith(Keep.FOLDER_THING) ? 2 : 1;
        if (part.length == first + 2) {
            try {
                return new int[] {Math.max(1, Integer.parseInt(part[first])),
                    Math.max(1, Integer.parseInt(part[first + 1]))};
            } catch (NumberFormatException broken) {
                // One place, then.
            }
        }
        return new int[] {1, 1};
    }

    private boolean resizable(String token) {
        if (token == null) {
            return false;
        }
        String word = base(token);
        if (Keep.VENDOR_THING.equals(word) || Keep.SYSTEM_THING.equals(word)
            || word.startsWith(Keep.FOLDER_THING)) {
            return true;
        }
        if (WIDGET.equals(word)) {
            android.appwidget.AppWidgetProviderInfo info = widgetOf(token);
            return info != null && info.resizeMode != android.appwidget.AppWidgetProviderInfo.RESIZE_NONE;
        }
        return false;
    }

    private android.appwidget.AppWidgetProviderInfo widgetOf(String token) {
        try {
            return widgets.getAppWidgetInfo(Integer.parseInt(token.substring(WIDGET.length()).split(":")[0]));
        } catch (RuntimeException broken) {
            return null;
        }
    }

    /**
     * The frame for reshaping what stands in a place: a folder to any size
     * from one place to the whole screen; a widget within the bounds and
     * the directions its application allows.
     */
    private void reshapeAt(final String token, final int screen, final int column, final int row) {
        if (screen >= pages.size()) {
            return;
        }
        final Grid page = pages.get(screen);
        View thing = null;
        for (java.util.Map.Entry<View, String> each : things.entrySet()) {
            if (token.equals(each.getValue()) && each.getKey().getParent() == page) {
                thing = each.getKey();
            }
        }
        if (thing == null) {
            return;
        }
        int[] at = (int[]) thing.getTag();
        int[] block = {at[0], at[1], at.length > 2 ? at[2] : 1, at.length > 3 ? at[3] : 1};
        int minA = 1;
        int minD = 1;
        int maxA = columns;
        int maxD = rows;
        boolean wide = true;
        boolean tall = true;
        if (token.startsWith(WIDGET)) {
            android.appwidget.AppWidgetProviderInfo info = widgetOf(token);
            if (info == null) {
                return;
            }
            float cw = page.cellWidth();
            float ch = page.cellHeight();
            wide = (info.resizeMode & android.appwidget.AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0;
            tall = (info.resizeMode & android.appwidget.AppWidgetProviderInfo.RESIZE_VERTICAL) != 0;
            int least = info.minResizeWidth > 0 ? info.minResizeWidth : info.minWidth;
            int leastTall = info.minResizeHeight > 0 ? info.minResizeHeight : info.minHeight;
            minA = wide ? Math.max(1, (int) Math.ceil(least / cw)) : block[2];
            minD = tall ? Math.max(1, (int) Math.ceil(leastTall / ch)) : block[3];
            maxA = wide ? columns : block[2];
            maxD = tall ? rows : block[3];
            if (Build.VERSION.SDK_INT >= 31) {
                /* The largest size is read generously: rounded up to whole
                   places, and never smaller than the size the widget asks
                   for at first. A widget whose largest size is less than
                   one of our places, or less than the size it asked for,
                   must still be able to grow back to where it began. */
                int[] asked = widgetSpan(info);
                if (wide && info.maxResizeWidth > 0) {
                    maxA = Math.max(Math.max(minA, asked[0]),
                        Math.min(columns, (int) Math.ceil(info.maxResizeWidth / cw)));
                }
                if (tall && info.maxResizeHeight > 0) {
                    maxD = Math.max(Math.max(minD, asked[1]),
                        Math.min(rows, (int) Math.ceil(info.maxResizeHeight / ch)));
                }
            }
            minA = Math.min(minA, block[2]);
            minD = Math.min(minD, block[3]);
        }
        reach = new Reach(this, page, thing, block, minA, minD, maxA, maxD, wide, tall, new Reach.Done() {
            public void done(int c, int r, int a, int d) {
                String word = base(token);
                String kept;
                if (WIDGET.equals(word)) {
                    kept = WIDGET + token.substring(WIDGET.length()).split(":")[0] + ":" + a + ":" + d;
                } else {
                    kept = a == 1 && d == 1 ? word : word + ":" + a + ":" + d;
                }
                Keep.reshape(Home.this, screen, column, row, kept, c, r);
                fill();
            }
        });
        root.addView(reach, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screens.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    // --------------------------------------------------------- the offer

    /** The number of a folder of one's own, from the word it is kept by; nought for any other. */
    private static int folderId(String token) {
        if (token == null || !token.startsWith(Keep.FOLDER_THING)) {
            return 0;
        }
        try {
            return Integer.parseInt(token.split(":")[1]);
        } catch (RuntimeException broken) {
            return 0;
        }
    }

    /** A long press on something standing on a screen. */
    private void offerStanding(View thing) {
        String token = things.get(thing);
        if (token == null || !(thing.getParent() instanceof Grid)) {
            return;
        }
        int[] at = (int[]) thing.getTag();
        int[] whence = {pages.indexOf((Grid) thing.getParent()), at[0], at[1]};
        Apps.Door door = thing instanceof Cell ? ((Cell) thing).door : null;
        offerThing(thing, token, whence, door);
    }

    /** A long press on an app in a list, in what is fresh, or in an open folder. */
    private void offerNew(View from, Apps.Door door, int[] icon, int[] whence) {
        offerThing(from, door.token(), whence, door);
        final View view = from;
        final Apps.Door which = door;
        final int[] where = icon;
        final int[] origin = whence;
        root.arm(new Runnable() {
            public void run() {
                menu.hide(false);
                whenceNew = origin;
                pick(view, which, where, root.fingerX() + rootLeft(), root.fingerY() + rootTop());
                whenceNew = null;
            }
        });
    }

    /**
     * The menu of a thing, beside it: an app's own shortcuts first, with
     * their pictures; then what can be done to the thing; and, set apart,
     * taking it off. The same finger moving on closes the menu and carries
     * the thing instead.
     */
    private void offerThing(final View thing, String token, final int[] whence, Apps.Door door) {
        offerToken = token;
        offerWhence = whence;
        offerView = thing;
        offerDoor = door;
        offerShortcuts = new ArrayList<>();
        /* The first face: what the app itself offers. */
        List<Menu.Section> own = new ArrayList<>();
        if (door != null) {
            offerShortcuts = shortcutsOf(door);
            if (!offerShortcuts.isEmpty()) {
                String[] lines = new String[offerShortcuts.size()];
                int[] keys = new int[lines.length];
                android.graphics.drawable.Drawable[] icons = new android.graphics.drawable.Drawable[lines.length];
                int dpi = getResources().getDisplayMetrics().densityDpi;
                for (int i = 0; i < lines.length; i++) {
                    android.content.pm.ShortcutInfo info = offerShortcuts.get(i);
                    CharSequence label = info.getShortLabel();
                    lines[i] = label == null ? "" : label.toString();
                    keys[i] = KEY_SHORTCUT + i;
                    try {
                        icons[i] = launcher.getShortcutIconDrawable(info, dpi);
                    } catch (RuntimeException none) {
                        icons[i] = null;
                    }
                }
                Menu.Section section = new Menu.Section(null, lines, keys);
                section.icons = icons;
                own.add(section);
            }
        }
        /* The second face: what the home screen can do to the thing. */
        List<Menu.Section> ours = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        List<Integer> keys = new ArrayList<>();
        List<Integer> glyphs = new ArrayList<>();
        if (door != null && !systemApp(door)) {
            lines.add(UNINSTALL);
            keys.add(KEY_UNINSTALL);
            glyphs.add(Glyph.TRASH);
        }
        if (token.startsWith(Keep.FOLDER_THING)) {
            lines.add(RENAME);
            keys.add(KEY_RENAME);
            glyphs.add(Glyph.PEN);
        }
        if (whence != null && whence[0] >= 0 && resizable(token)) {
            lines.add(RESIZE);
            keys.add(KEY_RESIZE);
            glyphs.add(Glyph.RESIZE);
        }
        if (!lines.isEmpty()) {
            ours.add(pictured(lines, keys, glyphs));
        }
        if (whence != null) {
            List<String> away = new ArrayList<>();
            away.add(REMOVE);
            List<Integer> awayKeys = new ArrayList<>();
            awayKeys.add(KEY_REMOVE);
            List<Integer> awayGlyphs = new ArrayList<>();
            awayGlyphs.add(Glyph.CROSS);
            Menu.Section gone = pictured(away, awayKeys, awayGlyphs);
            gone.danger = true;
            ours.add(gone);
        }
        String name = nameOfThing(thing, token, door);
        menuFor = MENU_THING;
        int[] at = new int[2];
        int[] floorAt = new int[2];
        thing.getLocationOnScreen(at);
        root.getLocationOnScreen(floorAt);
        float x;
        float y;
        float gap;
        if (thing instanceof Cell) {
            int[] icon = ((Cell) thing).localIcon();
            x = at[0] - floorAt[0] + icon[0] + icon[2] / 2f;
            y = at[1] - floorAt[1] + icon[1] + icon[3] / 2f;
            gap = icon[3] / 2f + dp(10);
        } else if (thing instanceof Row) {
            int[] icon = ((Row) thing).iconBounds();
            x = at[0] - floorAt[0] + icon[0] + icon[2] / 2f;
            y = at[1] - floorAt[1] + icon[1] + icon[3] / 2f;
            gap = icon[3] / 2f + dp(10);
        } else {
            x = at[0] - floorAt[0] + thing.getWidth() / 2f;
            y = at[1] - floorAt[1] + thing.getHeight() / 2f;
            gap = thing.getHeight() / 2f + dp(8);
        }
        thing.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        menu.showFaces(name, own.toArray(new Menu.Section[0]), Glyph.SETTINGS,
            ours.toArray(new Menu.Section[0]), door != null ? Glyph.INFO : -1, x, y, gap);
        if (whence != null && whence[0] != -2) {
            root.arm(new Runnable() {
                public void run() {
                    menu.hide(false);
                    if (whence[0] == -1) {
                        moveDock(thing, whence[1]);
                    } else {
                        move(thing);
                    }
                }
            });
        }
    }

    /** The line that leads to the settings, with the settings' own mark before it. */
    static Menu.Section settingsLine(String word, int key) {
        Menu.Section line = new Menu.Section(null, new String[] {word}, new int[] {key});
        line.glyphs = new int[] {Glyph.SETTINGS};
        return line;
    }

    /** Lines of the home screen's own, each with its drawing. */
    private static Menu.Section pictured(List<String> lines, List<Integer> keys, List<Integer> glyphs) {
        Menu.Section made = section(lines, keys);
        made.glyphs = new int[glyphs.size()];
        for (int i = 0; i < made.glyphs.length; i++) {
            made.glyphs[i] = glyphs.get(i);
        }
        return made;
    }

    /** The name a thing's menu is headed with. */
    private String nameOfThing(View thing, String token, Apps.Door door) {
        if (door != null) {
            return door.label.toString();
        }
        if (token.startsWith(Keep.FOLDER_THING)) {
            return Keep.folderName(this, folderId(token));
        }
        if (token.startsWith(WIDGET)) {
            android.appwidget.AppWidgetProviderInfo info = widgetOf(token);
            return info == null ? "" : info.loadLabel(getPackageManager());
        }
        if (Keep.CLOCK_THING.equals(token)) {
            return CLOCK_NAME;
        }
        if (Keep.OWN_THING.equals(token)) {
            return OWN_SETTINGS;
        }
        if (thing instanceof Cell) {
            CharSequence said = thing.getContentDescription();
            return said == null ? "" : said.toString();
        }
        return "";
    }

    private static Menu.Section section(List<String> lines, List<Integer> keys) {
        int[] k = new int[keys.size()];
        for (int i = 0; i < k.length; i++) {
            k[i] = keys.get(i);
        }
        return new Menu.Section(null, lines.toArray(new String[0]), k);
    }

    private boolean systemApp(Apps.Door door) {
        try {
            android.content.pm.ApplicationInfo info = getPackageManager()
                .getApplicationInfo(door.name.getPackageName(), 0);
            return (info.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                && (info.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
        } catch (android.content.pm.PackageManager.NameNotFoundException gone) {
            return true;
        }
    }

    /** An app's own shortcuts, as its maker ranked them, four at most; none unless this is the home screen in charge. */
    private List<android.content.pm.ShortcutInfo> shortcutsOf(Apps.Door door) {
        List<android.content.pm.ShortcutInfo> list = new ArrayList<>();
        try {
            if (!launcher.hasShortcutHostPermission()) {
                return list;
            }
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(door.name.getPackageName());
            query.setActivity(door.name);
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
            List<android.content.pm.ShortcutInfo> found = launcher.getShortcuts(query, door.user);
            if (found != null) {
                list.addAll(found);
            }
        } catch (RuntimeException none) {
            return list;
        }
        java.util.Collections.sort(list, new java.util.Comparator<android.content.pm.ShortcutInfo>() {
            public int compare(android.content.pm.ShortcutInfo a, android.content.pm.ShortcutInfo b) {
                if (a.isDeclaredInManifest() != b.isDeclaredInManifest()) {
                    return a.isDeclaredInManifest() ? -1 : 1;
                }
                return a.getRank() - b.getRank();
            }
        });
        return list.size() > 4 ? new ArrayList<>(list.subList(0, 4)) : list;
    }

    /** A line of a thing's menu was chosen. */
    private void offered(int key) {
        menu.hide(true);
        View from = offerView;
        Rect bounds = boundsOf(from);
        Bundle grow = from == null ? null : ActivityOptions.makeClipRevealAnimation(from, 0, 0,
            from.getWidth(), from.getHeight()).toBundle();
        try {
            if (key >= KEY_SHORTCUT) {
                android.content.pm.ShortcutInfo info = offerShortcuts.get(key - KEY_SHORTCUT);
                launcher.startShortcut(info, bounds, grow);
            } else if (key == KEY_INFO && offerDoor != null) {
                launcher.startAppDetailsActivity(offerDoor.name, offerDoor.user, bounds, grow);
            } else if (key == KEY_UNINSTALL && offerDoor != null) {
                Intent away = new Intent(Intent.ACTION_DELETE,
                    Uri.fromParts("package", offerDoor.name.getPackageName(), null));
                away.putExtra(Intent.EXTRA_USER, offerDoor.user);
                startActivity(away);
            } else if (key == KEY_REMOVE && offerWhence != null) {
                removeThing(offerToken, offerWhence);
            } else if (key == KEY_RESIZE && offerWhence != null) {
                if (!Keep.laid(this)) {
                    Keep.lay(this, standing);
                    fill();
                }
                reshapeAt(offerToken, offerWhence[0], offerWhence[1], offerWhence[2]);
            } else if (key == KEY_RENAME) {
                rename(folderId(offerToken));

            }
        } catch (RuntimeException refused) {
            refuse(screens);
        }
    }

    private Rect boundsOf(View view) {
        if (view == null) {
            return null;
        }
        int[] at = new int[2];
        view.getLocationOnScreen(at);
        return new Rect(at[0], at[1], at[0] + view.getWidth(), at[1] + view.getHeight());
    }

    /** An app is taken out of the dock to be carried. */
    private void moveDock(View thing, int slot) {
        String token = dockToken(slot);
        if (lift != null || token.length() == 0) {
            return;
        }
        android.graphics.drawable.Drawable face;
        int[] icon;
        if (thing instanceof Cell) {
            face = ((Cell) thing).drawable();
            icon = ((Cell) thing).localIcon();
        } else {
            android.graphics.Bitmap picture = android.graphics.Bitmap.createBitmap(
                Math.max(1, thing.getWidth()), Math.max(1, thing.getHeight()),
                android.graphics.Bitmap.Config.ARGB_8888);
            thing.draw(new android.graphics.Canvas(picture));
            face = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
            icon = new int[] {0, 0, thing.getWidth(), thing.getHeight()};
        }
        carry(thing, token, face, icon, 1, 1, new int[] {-1, slot},
            root.fingerX() + rootLeft(), root.fingerY() + rootTop());
        thing.setVisibility(View.INVISIBLE);
    }

    // ------------------------------------------------------ pinned shortcuts

    /** The parts of a pinned shortcut's word: its package, its id, and its profile. */
    private static String[] shortcutParts(String token) {
        String rest = token.substring(Keep.SHORTCUT_THING.length());
        long serial = 0L;
        int at = rest.lastIndexOf('@');
        if (at > 0 && rest.substring(at + 1).matches("\\d+")) {
            serial = Long.parseLong(rest.substring(at + 1));
            rest = rest.substring(0, at);
        }
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return null;
        }
        return new String[] {rest.substring(0, slash), rest.substring(slash + 1), Long.toString(serial)};
    }

    static String shortcutToken(android.content.pm.ShortcutInfo info) {
        long serial = Apps.serialOf(info.getUserHandle());
        return Keep.SHORTCUT_THING + info.getPackage() + "/" + info.getId() + (serial == 0L ? "" : "@" + serial);
    }

    private android.content.pm.ShortcutInfo pinnedInfo(String token) {
        String[] part = shortcutParts(token);
        if (part == null) {
            return null;
        }
        try {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(part[0]);
            query.setShortcutIds(java.util.Collections.singletonList(part[1]));
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<android.content.pm.ShortcutInfo> found = launcher.getShortcuts(query,
                Apps.userOf(Long.parseLong(part[2])));
            return found == null || found.isEmpty() ? null : found.get(0);
        } catch (RuntimeException none) {
            return null;
        }
    }

    /** A pinned shortcut as a cell with its own picture and name; none if it is no longer pinned. */
    private Cell pinnedCell(String token, boolean named) {
        final android.content.pm.ShortcutInfo info = pinnedInfo(token);
        if (info == null) {
            return null;
        }
        android.graphics.drawable.Drawable icon = null;
        try {
            icon = Shape.face(launcher.getShortcutIconDrawable(info, getResources().getDisplayMetrics().densityDpi));
        } catch (RuntimeException none) {
            icon = null;
        }
        CharSequence label = info.getShortLabel();
        final Cell cell = new Cell(this, icon, label == null ? "" : label, iconSize,
            named && Style.namesOnScreens);
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    launcher.startShortcut(info, boundsOf(cell), ActivityOptions.makeClipRevealAnimation(cell,
                        0, 0, cell.getWidth(), cell.getHeight()).toBundle());
                } catch (RuntimeException gone) {
                    refuse(cell);
                }
            }
        });
        return cell;
    }

    /** A shortcut another app asked to pin, standing on a screen. */
    private void pinned(Grid page, Keep.Spot spot) {
        if (!page.free(spot.x, spot.y)) {
            return;
        }
        Cell cell = pinnedCell(spot.token, true);
        if (cell == null) {
            return;
        }
        page.put(cell, spot.x, spot.y);
        cells.add(cell);
        stand(page, cell, spot.token);
    }

    /** A pinned shortcut taken off every screen is let go of, so its app may forget it too. */
    private void unpin(String token) {
        String[] part = shortcutParts(token);
        if (part == null) {
            return;
        }
        List<String> still = new ArrayList<>();
        for (Keep.Spot spot : Keep.placed(this)) {
            String[] other = spot.token.startsWith(Keep.SHORTCUT_THING) ? shortcutParts(spot.token) : null;
            if (other != null && other[0].equals(part[0]) && other[2].equals(part[2])) {
                still.add(other[1]);
            }
        }
        try {
            launcher.pinShortcuts(part[0], still, Apps.userOf(Long.parseLong(part[2])));
        } catch (RuntimeException none) {
            // Not the home screen in charge; nothing to let go of.
        }
    }

    /**
     * Something asked to be put on the home screen is set down in the
     * first free place: the screen given first, then those after it, then
     * those before.
     */
    private void setAnywhere(String token, int first) {
        if (pages.isEmpty()) {
            return;
        }
        List<Integer> order = new ArrayList<>();
        first = Math.max(0, Math.min(pages.size() - 1, first));
        for (int i = first; i < pages.size(); i++) {
            order.add(i);
        }
        for (int i = first - 1; i >= 0; i--) {
            order.add(i);
        }
        for (int screen : order) {
            Grid page = pages.get(screen);
            for (int r = rows - 1; r >= 0; r--) {
                for (int c = 0; c < columns; c++) {
                    if (page.free(c, r)) {
                        if (!Keep.laid(this)) {
                            Keep.lay(this, standing);
                        }
                        Keep.place(this, token, screen, c, r);
                        fill();
                        screens.show(screen, true);
                        screens.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                            ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
                        return;
                    }
                }
            }
        }
        refuse(screens);
    }

    /** The makers of shortcuts on a whole screen: the home screen's own first, then every app's. */
    private void showMakers() {
        makers = new ArrayList<>();
        try {
            makers.addAll(launcher.getShortcutConfigActivityList(null, android.os.Process.myUserHandle()));
        } catch (RuntimeException none) {
            makers.clear();
        }
        final android.content.pm.PackageManager manager = getPackageManager();
        java.util.Collections.sort(makers, new java.util.Comparator<android.content.pm.LauncherActivityInfo>() {
            final java.text.Collator order = java.text.Collator.getInstance();

            public int compare(android.content.pm.LauncherActivityInfo a, android.content.pm.LauncherActivityInfo b) {
                return order.compare(String.valueOf(a.getLabel()), String.valueOf(b.getLabel()));
            }
        });
        /* One list: the door to these settings first, then every app's makers. */
        List<List<Chooser.Item>> groups = new ArrayList<>();
        List<Chooser.Item> all = new ArrayList<>();
        all.add(new Chooser.Item(Shape.face(getDrawable(R.mipmap.door)), OWN_SETTINGS, OWN_MAKER));
        int dpi = getResources().getDisplayMetrics().densityDpi;
        for (int i = 0; i < makers.size(); i++) {
            all.add(new Chooser.Item(makers.get(i).getIcon(dpi), makers.get(i).getLabel(), i));
        }
        groups.add(all);
        chooser.show(new String[] {null}, groups, Keep.number(this, Keep.MAKERS_VIEW, Keep.LINES) == Keep.PAGES);
    }

    /** A maker was chosen: its own window makes the shortcut, and the answer comes back as a pin request. */
    private void make(int which) {
        if (which == OWN_MAKER) {
            setAnywhere(Keep.OWN_THING, pendingPage);
            return;
        }
        if (which < 0 || which >= makers.size()) {
            return;
        }
        try {
            android.content.IntentSender ask = launcher.getShortcutConfigActivityIntent(makers.get(which));
            startIntentSenderForResult(ask, ASK_SHORTCUT, null, 0, 0, 0);
        } catch (Exception refused) {
            refuse(screens);
        }
    }

    // --------------------------------------------------- folders of one's own

    /** A new, empty folder where the screen's menu was asked for, and at once a name for it. */
    private void newFolder() {
        Grid page = pages.get(screens.page());
        int[] gridAt = new int[2];
        int[] floorAt = new int[2];
        page.getLocationOnScreen(gridAt);
        root.getLocationOnScreen(floorAt);
        float gx = askX - (gridAt[0] - floorAt[0]);
        float gy = askY - (gridAt[1] - floorAt[1]);
        int[] cell = page.landing(gx, gy, 1, 1);
        if (cell == null) {
            refuse(screens);
            return;
        }
        if (!Keep.laid(this)) {
            Keep.lay(this, standing);
        }
        int id = Keep.newFolder(this, NEW_FOLDER);
        Keep.place(this, Keep.FOLDER_THING + id, screens.page(), cell[0], cell[1]);
        fill();
        rename(id);
    }

    /** A card with the folder's name ready to be written over. */
    private void rename(final int id) {
        if (id <= 0) {
            return;
        }
        Ask.show(root, NEW_FOLDER, Keep.folderName(this, id), new Ask.Answer() {
            public void answered(String text) {
                String name = text.trim();
                if (name.length() > 0) {
                    Keep.renameFolder(Home.this, id, name);
                    fill();
                }
            }
        });
    }

    /** Widgets whose applications are gone are let go of and taken off. */
    private void dropDeadWidgets() {
        for (Keep.Spot spot : Keep.placed(this)) {
            if (!spot.token.startsWith(WIDGET)) {
                continue;
            }
            try {
                int id = Integer.parseInt(spot.token.substring(WIDGET.length()).split(":")[0]);
                if (widgets.getAppWidgetInfo(id) == null) {
                    Keep.remove(this, spot.screen, spot.x, spot.y);
                    drop(id);
                }
            } catch (RuntimeException broken) {
                // Left as it is.
            }
        }
    }

    // ------------------------------------------------------------- words' size

    /**
     * A context whose words are drawn at the size chosen on the colour page,
     * over whatever size the phone itself sets.
     */
    static Context sized(Context base) {
        int zoom = Keep.zoom(base);
        if (zoom == 100) {
            return base;
        }
        android.content.res.Configuration shape =
            new android.content.res.Configuration(base.getResources().getConfiguration());
        shape.fontScale = shape.fontScale * zoom / 100f;
        return base.createConfigurationContext(shape);
    }

    private int sizedAt;

    @Override
    protected void attachBaseContext(Context base) {
        sizedAt = Keep.zoom(base);
        super.attachBaseContext(sized(base));
    }

    // ------------------------------------------------------ weather, headphones

    /** The sky has answered, on whatever thread it answered on: the clock is drawn again. */
    private final Runnable weatherCame = new Runnable() {
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (clockView != null) {
                        clockView.weather(Keep.flag(Home.this, Keep.WEATHER, true));
                    }
                }
            });
        }
    };

    private static final String EARS_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    private static final String EARS_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL";

    /** Headphones tell their charge when they change it; gone, they tell nothing. */
    private final android.content.BroadcastReceiver ears = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            earsLevel = EARS_CHANGED.equals(intent.getAction()) ? intent.getIntExtra(EARS_LEVEL, -1) : -1;
            if (clockView != null) {
                clockView.ears(Keep.flag(Home.this, Keep.EARS, true) ? earsLevel : -1);
            }
        }
    };

    /** Whether this home screen may hear headphones: from Android 12 only with the owner's leave. */
    private boolean hearsEars() {
        return Build.VERSION.SDK_INT < 31
            || checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /**
     * While the screens are in front: headphones are listened to, and the
     * weather is asked for when what is known is old. The first time the
     * clock is shown, leave is asked for both, once.
     */
    private void listen() {
        if (Keep.flag(this, Keep.CLOCK, true) && !Keep.flag(this, Keep.ASKED_WORLD, false)) {
            Keep.saveFlag(this, Keep.ASKED_WORLD, true);
            stamp = Keep.stamp(this);
            List<String> asks = new ArrayList<>();
            if (Keep.flag(this, Keep.EARS, true) && !hearsEars()) {
                asks.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!asks.isEmpty()) {
                requestPermissions(asks.toArray(new String[0]), ASK_WORLD);
            }
        }
        if (!listening && hearsEars()) {
            android.content.IntentFilter heard = new android.content.IntentFilter(EARS_CHANGED);
            heard.addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED);
            try {
                registerReceiver(ears, heard);
                listening = true;
                earsLevel = earsNow();
            } catch (RuntimeException refused) {
                earsLevel = -1;
            }
            if (clockView != null) {
                clockView.ears(Keep.flag(this, Keep.EARS, true) ? earsLevel : -1);
            }
        }
        if (Keep.flag(this, Keep.WEATHER, true)) {
            Folio.freshen(this, weatherCame);
        }
    }

    private void unlisten() {
        if (!listening) {
            return;
        }
        listening = false;
        try {
            unregisterReceiver(ears);
        } catch (RuntimeException already) {
            // Not registered.
        }
    }

    /**
     * The charge of headphones already near. The system tells it only to
     * those who ask by a name it does not publish, so it is asked for
     * carefully, and a refusal means nothing is shown.
     */
    @SuppressWarnings("deprecation")
    private int earsNow() {
        try {
            android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                return -1;
            }
            for (android.bluetooth.BluetoothDevice device : adapter.getBondedDevices()) {
                Object near = device.getClass().getMethod("isConnected").invoke(device);
                if (Boolean.TRUE.equals(near)) {
                    Object level = device.getClass().getMethod("getBatteryLevel").invoke(device);
                    if (level instanceof Integer && (Integer) level >= 0) {
                        return (Integer) level;
                    }
                }
            }
        } catch (Exception refused) {
            return -1;
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int asked, String[] what, int[] answers) {
        super.onRequestPermissionsResult(asked, what, answers);
        if (asked == ASK_WORLD) {
            listen();
        }
    }

    // ----------------------------------------------------------- widgets

    /**
     * How many places across and down a widget takes: what it asks for, if
     * it says so in places; otherwise its least size in places of this
     * grid, rounded up, and never more than the grid has.
     */
    private int[] widgetSpan(android.appwidget.AppWidgetProviderInfo info) {
        Grid page = pages.isEmpty() ? null : pages.get(Math.min(screens.page(), pages.size() - 1));
        float wide = page == null || page.cellWidth() <= 0
            ? (getResources().getDisplayMetrics().widthPixels - dp(16)) / (float) columns : page.cellWidth();
        float tall = page == null || page.cellHeight() <= 0 ? wide * 1.2f : page.cellHeight();
        int across;
        int down;
        if (Build.VERSION.SDK_INT >= 31 && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
            across = info.targetCellWidth;
            down = info.targetCellHeight;
        } else {
            across = (int) Math.ceil(info.minWidth / wide);
            down = (int) Math.ceil(info.minHeight / tall);
        }
        return new int[] {Math.max(1, Math.min(columns, across)), Math.max(1, Math.min(rows, down))};
    }

    /** A widget chosen from the shelf: leave is asked for it if the phone wants that, then it is set up. */
    private void takeWidget(android.appwidget.AppWidgetProviderInfo info) {
        int id = host.allocateAppWidgetId();
        pendingWidget = id;
        boolean bound = widgets.bindAppWidgetIdIfAllowed(id, info.getProfile(), info.provider, null);
        if (bound) {
            setUp(id);
            return;
        }
        Intent ask = new Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND);
        ask.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        ask.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
        ask.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.getProfile());
        try {
            startActivityForResult(ask, ASK_BIND);
        } catch (RuntimeException none) {
            drop(id);
            refuse(screens);
        }
    }

    /** A widget that wants to be set up first opens its own window for that. */
    private void setUp(int id) {
        android.appwidget.AppWidgetProviderInfo info = widgets.getAppWidgetInfo(id);
        if (info == null) {
            drop(id);
            refuse(screens);
            return;
        }
        if (info.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(this, id, 0, ASK_SETUP, null);
                return;
            } catch (RuntimeException none) {
                // It cannot be set up from here; it is set down as it is.
            }
        }
        setWidget(id);
    }

    @Override
    protected void onActivityResult(int asked, int answer, Intent data) {
        super.onActivityResult(asked, answer, data);
        if (asked == ASK_SHORTCUT) {
            if (answer == RESULT_OK && data != null) {
                try {
                    LauncherApps.PinItemRequest request = launcher.getPinItemRequest(data);
                    if (request != null && request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
                        && request.isValid() && request.accept()) {
                        setAnywhere(shortcutToken(request.getShortcutInfo()), pendingPage);
                    }
                } catch (RuntimeException refused) {
                    refuse(screens);
                }
            }
            return;
        }
        int id = pendingWidget;
        if (id < 0 || (asked != ASK_BIND && asked != ASK_SETUP)) {
            return;
        }
        if (answer != RESULT_OK) {
            drop(id);
            return;
        }
        if (asked == ASK_BIND) {
            setUp(id);
        } else {
            setWidget(id);
        }
    }

    private void drop(int id) {
        widgetViews.remove(id);
        try {
            host.deleteAppWidgetId(id);
        } catch (RuntimeException gone) {
            // Already let go.
        }
        pendingWidget = -1;
    }

    /**
     * Sets a widget down in the first free block of its size: on the
     * screen the shelf was opened from, else on any other. With no room
     * anywhere, it is let go and the phone says no.
     */
    private void setWidget(int id) {
        pendingWidget = -1;
        android.appwidget.AppWidgetProviderInfo info = widgets.getAppWidgetInfo(id);
        if (info == null) {
            drop(id);
            return;
        }
        int[] span = widgetSpan(info);
        List<Integer> order = new ArrayList<>();
        order.add(Math.min(pendingPage, pages.size() - 1));
        for (int i = 0; i < pages.size(); i++) {
            if (!order.contains(i)) {
                order.add(i);
            }
        }
        for (int screen : order) {
            Grid page = pages.get(screen);
            for (int r = 0; r + span[1] <= rows; r++) {
                for (int c = 0; c + span[0] <= columns; c++) {
                    if (page.free(c, r, span[0], span[1])) {
                        if (!Keep.laid(this)) {
                            Keep.lay(this, standing);
                        }
                        Keep.place(this, WIDGET + id + ":" + span[0] + ":" + span[1], screen, c, r);
                        fill();
                        screens.show(screen, true);
                        screens.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                            ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
                        return;
                    }
                }
            }
        }
        drop(id);
        refuse(screens);
    }

    /** A widget kept on a screen, made anew from its host; gone if its application is. */
    private void widget(Grid page, Keep.Spot spot) {
        String[] part = spot.token.substring(WIDGET.length()).split(":");
        if (part.length != 3) {
            return;
        }
        int id;
        int across;
        int down;
        try {
            id = Integer.parseInt(part[0]);
            across = Math.min(columns, Integer.parseInt(part[1]));
            down = Math.min(rows, Integer.parseInt(part[2]));
        } catch (NumberFormatException broken) {
            return;
        }
        android.appwidget.AppWidgetProviderInfo info = widgets.getAppWidgetInfo(id);
        if (info == null || !page.free(spot.x, spot.y, across, down)) {
            return;
        }
        android.appwidget.AppWidgetHostView view = widgetViews.get(id);
        if (view == null) {
            view = host.createView(this, id, info);
            widgetViews.put(id, view);
        } else if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setVisibility(View.VISIBLE);
        page.put(view, spot.x, spot.y, across, down);
        cells.add(view);
        stand(page, view, spot.token);
    }

    // ------------------------------------------------------------- shade

    /**
     * Lowers the phone's own shade, the notices or the quick settings. The
     * platform keeps the way to it for itself and for home screens that
     * hold the leave to ask; if it will not answer, nothing happens.
     */
    private void shade(boolean quick) {
        try {
            Object bar = getSystemService("statusbar");
            Class<?> kind = Class.forName("android.app.StatusBarManager");
            kind.getMethod(quick ? "expandSettingsPanel" : "expandNotificationsPanel").invoke(bar);
        } catch (Exception refused) {
            refuse(screens);
        }
    }

    // -------------------------------------------------------- new arrivals

    /**
     * An application just put on the phone is set down in the first free
     * place: the home screen first, then the screens after it, then those
     * before.
     */
    private void setDown(String owner, long serial) {
        Apps found = new Apps(this);
        Apps.Door door = null;
        for (Apps.Door each : found.all(Keep.BY_NAME)) {
            if (each.name.getPackageName().equals(owner) && each.serial == serial) {
                door = each;
                break;
            }
        }
        if (door == null || pages.isEmpty()) {
            return;
        }
        int home = Keep.home(this);
        List<Integer> order = new ArrayList<>();
        for (int i = home; i < pages.size(); i++) {
            order.add(i);
        }
        for (int i = home - 1; i >= 0; i--) {
            order.add(i);
        }
        for (int screen : order) {
            Grid page = pages.get(screen);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    if (page.free(c, r)) {
                        Keep.place(this, door.token(), screen, c, r);
                        return;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ fresh

    /**
     * Lowers what is fresh: up to eight applications last opened from here,
     * and up to twelve put on the phone or changed in the last two weeks,
     * the newest first, each with a word about it.
     */
    private void showFresh() {
        Apps found = new Apps(this);
        List<Apps.Door> recent = new ArrayList<>();
        for (String name : Keep.recent(this)) {
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
            if (lift != null || menu.shown() || drawer.menuShown() || tray.shown() || shelf.shown() || chooser.shown()
                || (reach != null && !reach.ended())) {
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
                if (Keep.number(Home.this, Keep.ON_UP, Keep.DO_LIST) == Keep.DO_LIST) {
                    pull = PULL_OPEN;
                    drawer.begin();
                }
            } else {
                int deed = Keep.number(Home.this, Keep.ON_DOWN, Keep.DO_NOTICES);
                if (deed == Keep.DO_NOTICES || deed == Keep.DO_QUICK) {
                    pull = PULL_BAR;
                    barAsked = false;
                }
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
            } else if (pull == PULL_BAR && !barAsked && by > dp(28)) {
                barAsked = true;
                shade(Keep.number(Home.this, Keep.ON_DOWN, Keep.DO_NOTICES) == Keep.DO_QUICK);
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
        List<View> order = new ArrayList<>();
        for (View cell : cells) {
            if (cell.getParent() == dock) {
                order.add(cell);
            }
        }
        for (int row = rows - 1; row >= 0; row--) {
            for (View cell : cells) {
                if (cell.getParent() != dock && ((int[]) cell.getTag())[1] == row) {
                    order.add(cell);
                }
            }
        }
        float rise = dp(28);
        for (int i = 0; i < order.size(); i++) {
            View cell = order.get(i);
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
