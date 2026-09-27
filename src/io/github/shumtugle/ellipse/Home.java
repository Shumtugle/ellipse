package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.WallpaperManager;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import android.view.MotionEvent;
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
    /* The screens' small places across and down: the grid's own, or, with
       half steps, twice as many each way; and how many make one place. */
    private int columns = 4;
    private int rows = 5;
    private int fine = 1;
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
    /** The pile's own card, and the place whose pile it edits: screen, column, row. */
    private PileCard pileCard;
    private int[] pileAt;
    /** The place a widget chosen from the shelf now is to be added to, as one more in its pile. */
    private int[] pileShelf;
    private int[] pendingPileAt;
    /** Widgets already made, kept across settings-out so they do not blink. */
    private final java.util.Map<Integer, android.appwidget.AppWidgetHostView> widgetViews = new java.util.HashMap<>();
    private LauncherApps launcher;
    /** The clock on the screens now, the charge its headphones tell, and whether they are listened to. */
    private Timepiece clockView;
    private int earsLevel = -1;
    private boolean listening;
    private static final int ASK_WORLD = 31;
    private static final int ASK_SHORTCUT = 13;
    private static final int KEY_INFO = 1;
    private static final int KEY_UNINSTALL = 2;
    private static final int KEY_REMOVE = 3;
    private static final int KEY_RESIZE = 4;
    private static final int KEY_RENAME = 5;
    private static final int KEY_PILE = 40;
    private static final int KEY_SHORTCUT = 100;
    private static final int KEY_FACE = 7;
    private static final int KEY_ARRANGE = 8;
    private static final int KEY_FRONT = 9;
    private static final int KEY_BEHIND = 10;
    private static final int KEY_FRAME = 11;
    private static final int KEY_KIND = 12;
    private static final int KEY_KIND_PICK = 9000;
    private static final String KIND = "Category";
    private static final String NEW_KIND = "New category\u2026";
    /** Where the last menu of a thing stood, for a second one to stand in its place. */
    private float menuX;
    private float menuY;
    private float menuGap;
    private List<String> kindChoices = new ArrayList<>();
    private static final String FRAME_ON = "Frame";
    private static final String FRAME_OFF = "No frame";
    private static final String FRONT = "Bring to front";
    private static final String BEHIND = "Send behind";
    private static final String ARRANGE = "Arrange rings";
    private static final String FACE_LINE = "Change icon";
    /** What the whole screen of choices is choosing now: a shortcut's maker, or one icon's face. */
    private boolean choosingFace;
    private String faceToken;
    private static final String APP_INFO = "App info";
    private static final String UNINSTALL = "Uninstall";
    private static final String REMOVE = "Remove";
    private static final String RESIZE = "Resize";
    private static final String RENAME = "Rename";
    private static final String PILE_LINE = "Stack";
    private static final String NEW_FOLDER = "Folder";
    private static final String OWN_SETTINGS = "Ellipse settings";
    private static final String CLOCK_NAME = "Clock";
    private static final int OWN_MAKER = 1000;
    private static final String NEW_GROUND = "A new ground";
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
    /** A pull that, let go far enough or fast enough, does what the gesture is given. */
    private static final int PULL_DEED = 5;
    private String pullKey;
    private int pullDeed;
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
            Marks.learn(Home.this, true);
            Keep.purge(Home.this, name, Apps.serialOf(user));
            dropDeadWidgets();
            later();
        }

        public void onPackageAdded(String name, UserHandle user) {
            Marks.learn(Home.this, true);
            /* The pack of icons chosen may be the one just installed. */
            if (name.equals(Keep.word(Home.this, Keep.ICON_PACK))) {
                Style.read(Home.this);
            }
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
        /* The screen's sleep and waking, for a ground to turn while it sleeps. */
        android.content.IntentFilter night = new android.content.IntentFilter(Intent.ACTION_SCREEN_OFF);
        night.addAction(Intent.ACTION_SCREEN_ON);
        night.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(sleep, night);
        Lapse.watch(this);
        super.onCreate(saved);
        /* The pinch, left to nothing, goes to the grey once; chosen otherwise, it stays as chosen. */
        if (!Keep.flag(this, Keep.GREY_OFFERED, false)) {
            Keep.saveFlag(this, Keep.GREY_OFFERED, true);
            if (Keep.number(this, Keep.ON_PINCH, Keep.DO_NOTHING) == Keep.DO_NOTHING) {
                Keep.saveNumber(this, Keep.ON_PINCH, Keep.DO_GREY);
            }
        }
        /* A new version's first start: the old set-out is copied aside
           before anything here reads or changes it. */
        Copy.onUpdate(this);
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
        try {
            unregisterReceiver(sleep);
        } catch (RuntimeException never) {
            // It was not listening.
        }
        if (apps != null) {
            apps.unregisterCallback(watch);
        }
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        /* The day's copy, if the home screen has stayed open since before the day began. */
        Copy.onUpdate(this);
        super.onResume();
        greyNow();
        /* Back on the home screen: a step aside at once, then one every three minutes. */
        step(false);
        drifting.removeCallbacks(driftOn);
        drifting.postDelayed(driftOn, 180000L);
        /* By its hours, the night clock opens instead of the home screen, once a night. */
        if (Night.due(this)) {
            Night.open(this, true);
        }
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
        bars();
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
        drifting.removeCallbacks(driftOn);
        unbare();
        super.onStop();
        giveBack();
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
        if (pileCard.shown()) {
            pileCard.close();
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
        perform(Keep.ON_HOME, Keep.DO_HOME);
    }

    /** What a gesture is given, done: its word in the settings, and what it does if none was chosen. */
    private void perform(String gesture, int otherwise) {
        int deed = Keep.number(this, gesture, otherwise);
        switch (deed) {
            case Keep.DO_FRESH:
                showFresh();
                break;
            case Keep.DO_LIST:
                drawer.rise();
                break;
            case Keep.DO_SEARCH:
                drawer.rise();
                drawer.seek();
                break;
            case Keep.DO_NOTICES:
                shade(false);
                break;
            case Keep.DO_QUICK:
                shade(true);
                break;
            case Keep.DO_HOME:
                screens.show(Keep.home(this), true);
                break;
            case Keep.DO_LOCK:
                lock();
                break;
            case Keep.DO_SETTINGS:
                startActivity(new Intent(this, Tune.class));
                break;
            case Keep.DO_NIGHT:
                Night.open(this, false);
                break;
            case Keep.DO_GREY:
                grey(!Keep.flag(this, Keep.GREY, false));
                break;
            case Keep.DO_APP:
                Apps.Door door = new Apps(this).door(Keep.word(this, "app." + gesture) == null ? ""
                    : Keep.word(this, "app." + gesture));
                if (door != null) {
                    launch(screens, door, new int[] {screens.getWidth() / 2, screens.getHeight() / 2, 1, 1});
                } else {
                    refuse(screens);
                }
                break;
            default:
                break;
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
        if (pileCard.shown()) {
            pileCard.close();
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
            perform(Keep.ON_BACK, Keep.DO_FRESH);
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

    /**
     * The phone's bars over the home screen: shown, or hidden as the
     * settings ask — the status bar, the navigation bar, or both. Hidden,
     * a swipe in from the edge brings them back for a moment. Other
     * windows, the settings and every app, keep their bars.
     */
    private void bars() {
        boolean status = Keep.flag(this, Keep.HIDE_STATUS, false);
        boolean navigation = Keep.flag(this, Keep.HIDE_NAVIGATION, false);
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController bars = getWindow().getInsetsController();
            if (bars == null) {
                return;
            }
            bars.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            if (status) {
                bars.hide(WindowInsets.Type.statusBars());
            } else {
                bars.show(WindowInsets.Type.statusBars());
            }
            if (navigation) {
                bars.hide(WindowInsets.Type.navigationBars());
            } else {
                bars.show(WindowInsets.Type.navigationBars());
            }
            return;
        }
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (status) {
            flags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if (navigation) {
            flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        if (status || navigation) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) {
            bars();
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
    /** Whether the phone lies on its side: the screens laid again in a grid of their own, nothing written. */
    private boolean lying;
    /** The grid's width upright, in places: lying down, the clock keeps it rather than the whole long side. */
    private int uprightColumns;
    /** The phone just turned: the screens open on the home one, in the grid of the way it now lies. */
    private boolean turned;
    /** Lying down, the screen each upright screen begins on. */
    private int[] lyingFirst = new int[0];

    private void build() {
        boolean wasLying = lying;
        /* The window's own shape, not the resources': with the owner's size of words the resources are
           made once, upright, and never learn that the phone has turned. */
        int[] shape = windowShape();
        lying = shape[0] > shape[1];
        /* Lying down is a view of the upright set-out, not a set-out of its own. */
        Keep.noLay = lying;
        if (screens != null && !pages.isEmpty() && wasLying == lying) {
            restore = screens.page();
        } else {
            restore = -1;
            turned = true;
        }
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        int wide = shape[0];
        int tall = shape[1];
        /* The icon is sized from the short side of the screen, so a turn of
           the phone does not make it grow; the platform's own range holds it. */
        int grid = Keep.number(this, Keep.DESK_GRID, 45);
        fine = Keep.fine(this);
        Keep.settleFine(this);
        columns = Keep.columns(grid) * fine;
        rows = Keep.rows(grid) * fine;
        uprightColumns = columns;
        stamp = Keep.stamp(this);
        float column = (Math.min(wide, tall) - dp(16)) / (float) Keep.columns(grid);
        if (lying) {
            /* Lying down, three parts across, one, one and a half: the upright screen's width twice and
               half of it again. Places keep their upright size where the screen is wide enough for that;
               on a narrower one everything is drawn a little smaller, so the three parts fit whole. */
            lyingCols = Keep.columns(grid);
            lyingHalf = Math.max(1, lyingCols / 2);
            /* At least as many rows as upright columns, so an upright row turned into a column fits whole. */
            float cell = Math.min(column, Math.min((Math.max(wide, tall) - dp(8)) / (float) (2 * lyingCols + lyingHalf),
                (Math.min(wide, tall) - dp(24)) / (float) lyingCols));
            lyingRows = Math.max(lyingCols, (int) Math.floor((Math.min(wide, tall) - dp(24)) / cell));
            columns = (2 * lyingCols + lyingHalf) * fine;
            rows = lyingRows * fine;
            /* The dock stands as a column in the half part when no screen needs it for its rows; else as
               a row at the foot of the first part. */
            lyingDockRow = false;
            if (Keep.flag(this, Keep.DOCK, true)) {
                project(Keep.placed(this), Keep.screens(this), true);
                lyingDockRow = lyingHalfUsed;
            }
        }
        iconSize = Math.max(dp(48), Math.min(dp(64), column * 0.58f));
        /* The owner's own size, within the cell. */
        /* With everything to the edges, an icon may fill its place nearly whole. */
        iconSize = Math.min(column * (Keep.edgeless(this) ? 0.98f : 0.86f), iconSize * Style.iconScale);

        boolean was = drawer != null && drawer.shown();
        root = new Floor(this);
        root.carrier(carrier);
        root.hand(pulling);
        root.fingers(new Floor.Fingers() {
            public boolean free() {
                return lift == null && !menu.shown() && !drawer.shown() && !drawer.menuShown() && !tray.shown()
                    && !shelf.shown() && !chooser.shown() && !fresh.shown() && (reach == null || reach.ended());
            }

            public void pinched() {
                perform(Keep.ON_PINCH, Keep.DO_NOTHING);
            }

            public void swept(boolean up) {
                perform(up ? Keep.ON_TWO_UP : Keep.ON_TWO_DOWN, Keep.DO_NOTHING);
            }
        });
        frame = new Frame(this, dp(24));
        root.addView(frame, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout stage = new FrameLayout(this);
        stage.setClipChildren(false);
        /* Lying down, the dock stands as a column at the right edge. */
        boolean beside = lying && !lyingDockRow;
        frame.setOrientation(beside ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        frame.addView(stage, beside ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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
        /* If things were set out by a guess at the screens' size, they are
           set out again once the screens have one. */
        screens.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (r - l <= 0 || b - t <= 0 || (r - l == or - ol && b - t == ob - ot)) {
                    return;
                }
                if (guessed) {
                    guessed = false;
                    screens.post(new Runnable() {
                        public void run() {
                            fill();
                        }
                    });
                }
            }
        });

        /* The bar is the dock: four places in the pill at the foot, no
           names, and the round button at its end is the way into every
           application, marked with six dots, as a grid of them would be. */
        bar = new LinearLayout(this);
        boolean barColumn = lying && !lyingDockRow;
        dockUpright = !barColumn;
        bar.setOrientation(barColumn ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        bar.setGravity(barColumn ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);
        bar.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        if (barColumn) {
            bar.setPadding(dp(8), dp(4), dp(8), dp(10));
        } else {
            bar.setPadding(dp(4), dp(8), dp(10), dp(8));
        }
        bar.setClipChildren(false);

        dock = barColumn ? new Grid(this, 1, DOCK) : new Grid(this, DOCK, 1);
        dock.shape(iconSize, 0f);
        bar.addView(dock, barColumn ? new LinearLayout.LayoutParams(Math.round(iconSize + dp(16)), 0, 1f)
            : new LinearLayout.LayoutParams(0, Math.round(iconSize + dp(16)), 1f));

        blob = new Blob(this, iconSize, Blob.GRID);
        blob.shaped(true);
        blob.setContentDescription(ALL);
        blob.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawer.rise();
            }
        });
        /* The door to every app can be given a face like any icon: held,
           it opens the same screen of faces. */
        blob.face(allFace());
        blob.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (lift != null) {
                    return false;
                }
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                chooseFace(ALL_THING, null);
                return true;
            }
        });
        LinearLayout.LayoutParams blobParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (barColumn) {
            blobParams.topMargin = dp(4);
        } else {
            blobParams.leftMargin = dp(4);
        }
        bar.addView(blob, blobParams);

        LinearLayout.LayoutParams barParams = barColumn
            ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (barColumn) {
            barParams.setMargins(dp(6), dp(12), dp(10), dp(12));
        } else {
            barParams.setMargins(dp(8), dp(6), dp(8), dp(10));
        }
        if (lying && lyingDockRow) {
            /* A row at the foot of the first part, over the screens: each screen keeps that row free. */
            int partOne = Math.round(wide * lyingCols / (float) (2 * lyingCols + lyingHalf));
            FrameLayout.LayoutParams under = new FrameLayout.LayoutParams(partOne - dp(16),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
            under.setMargins(dp(8), 0, dp(8), dp(8));
            stage.addView(bar, under);
        } else {
            frame.addView(bar, barParams);
        }
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

            public List<View> widgets() {
                return folderWidgets(trayFolder);
            }

            public void hold(View widget) {
                holdFolderWidget(widget);
            }
        });
        root.addView(tray, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pileCard = new PileCard(this, new PileCard.Hand() {
            public void done(List<Integer> kept, List<Integer> out, boolean turns) {
                pileKeep(pileAt, kept, out, turns);
            }

            public void add(List<Integer> kept, List<Integer> out, boolean turns) {
                int[] at = pileAt;
                pileKeep(at, kept, out, turns);
                pileShelf = at;
                pendingPage = screens.page();
                shelf.show(Keep.number(Home.this, Keep.SHELF_VIEW, Keep.LINES) == Keep.PAGES);
            }
        });
        root.addView(pileCard, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        shelf = new Shelf(this, new Shelf.Hand() {
            public int[] span(android.appwidget.AppWidgetProviderInfo info) {
                return widgetSpan(info);
            }

            public void chosen(android.appwidget.AppWidgetProviderInfo info) {
                shelf.close(true);
                pendingPileAt = pileShelf;
                pileShelf = null;
                takeWidget(info);
            }

            public boolean clockStands() {
                return clockView != null;
            }

            public void clock() {
                shelf.close(true);
                takeClock();
            }

            public android.graphics.drawable.Drawable diceFace() {
                /* On the shelf, as the shelf's icons are: the die in its plain look. */
                return diceRaw(Die.roll());
            }

            public void dice() {
                shelf.close(true);
                setAnywhere(Keep.DICE_THING, pendingPage);
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
                if (choosingFace) {
                    face(key);
                } else {
                    make(key);
                }
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
        /* The door to every app is built once with the dock, but its face
           may change at any time: it is dressed again with every filling. */
        if (blob != null) {
            blob.face(allFace());
        }
        int showing = restore >= 0 ? restore : (pages.isEmpty() || turned ? -1 : screens.page());
        restore = -1;
        turned = false;
        screens.removeAllViews();
        pages.clear();
        dock.removeAllViews();
        cells.clear();
        clockView = null;
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
        if (lying) {
            spots = project(spots, count, lyingDockRow);
            count = Math.max(1, lyingCount);
        }
        if (showing < 0) {
            showing = lying ? uprightToLying(Keep.home(this)) : Keep.home(this);
        }
        /* Lying down, the rows are the three parts' own: the upright shaping of rows stays upright. */
        rowShare = lying ? 1f : shapeRows();
        for (int i = 0; i < count; i++) {
            Grid page = new Grid(this, columns, rows);
            page.unit(fine);
            page.overlap(Keep.flag(this, Keep.OVERLAP, false));
            page.setPadding(side(), dp(16), side(), 0);
            page.rowShare(rowShare);
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
            if (door == null) {
                /* An app brought in that is not on the phone yet waits in its place. */
                String waited = Keep.waitName(this, spot.token);
                if (waited != null && page.fits(spot.x, spot.y, page.unit(), page.unit())) {
                    awaited(page, spot, waited);
                }
                continue;
            }
            if (!page.fits(spot.x, spot.y, page.unit(), page.unit())) {
                continue;
            }
            if (Keep.waitName(this, spot.token) != null) {
                Keep.unwait(this, spot.token);
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
            place(dock, door, dockUpright ? i : 0, dockUpright ? 0 : i, false);
        }

        int home = Math.min(lying ? uprightToLying(Keep.roles(this)) : Keep.roles(this), count - 1);
        if (laid) {
            boolean clocked = false;
            List<Keep.Spot> folders = new ArrayList<>();
            for (Keep.Spot spot : own) {
                Grid page = pages.get(spot.screen);
                if (Keep.CLOCK_THING.equals(base(spot.token))) {
                    clocked = clock(page, spot.x, spot.y, spot.token) || clocked;
                } else if (Keep.DICE_THING.equals(spot.token)) {
                    if (page.free(spot.x, spot.y)) {
                        Cell dice = diceCell();
                        page.put(dice, spot.x, spot.y);
                        cells.add(dice);
                        stand(page, dice, Keep.DICE_THING);
                    }
                } else if (Keep.OWN_THING.equals(spot.token)) {
                    if (page.free(spot.x, spot.y)) {
                        ownDoor(page, spot.x, spot.y);
                    }
                } else if (spot.token.startsWith(WIDGET)) {
                    widget(page, spot);
                } else if (spot.token.startsWith(Keep.PILE_THING)) {
                    pile(page, spot);
                } else if (spot.token.startsWith(Keep.SHORTCUT_THING)) {
                    pinned(page, spot);
                } else if (spot.token.startsWith(Keep.LINK_THING)) {
                    linked(page, spot);
                } else if (spot.token.startsWith(Keep.RUN_THING)) {
                    run(page, spot);
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
                /* No kept clock stood: it was never kept, or its place is
                   taken now. It stands at the head of the home screen, in
                   the size it was last given, and is kept there at once —
                   a clock that stands where nothing keeps it could be
                   neither moved nor reshaped, every change to it looking
                   for a place that was never written down. */
                String last = Keep.CLOCK_THING;
                for (Keep.Spot spot : own) {
                    if (Keep.CLOCK_THING.equals(base(spot.token))) {
                        last = spot.token;
                    }
                }
                if (clock(pages.get(home), 0, 0, last)) {
                    if (!lying) {
                        Keep.keepOnly(this, Keep.CLOCK_THING, last, home, 0, 0);
                    }
                }
            }
            /* Over one another, things stand in the order they were set
               down, apps and widgets alike: the last on top. */
            if (Keep.flag(this, Keep.OVERLAP, false) && !lying) {
                for (Keep.Spot spot : Keep.placed(this)) {
                    if (spot.screen < 0 || spot.screen >= pages.size()) {
                        continue;
                    }
                    Grid page = pages.get(spot.screen);
                    for (int i = 0; i < page.getChildCount(); i++) {
                        View child = page.getChildAt(i);
                        int[] at = (int[]) child.getTag();
                        if (at[0] == spot.x && at[1] == spot.y) {
                            child.bringToFront();
                            break;
                        }
                    }
                }
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
            clock(middle, 0, 0, Keep.CLOCK_THING);
            Intent[] everyday = {
                new Intent(Intent.ACTION_VIEW, Uri.parse("https:")),
                category(Intent.CATEGORY_APP_MARKET),
                category(Intent.CATEGORY_APP_CALENDAR),
                category(Intent.CATEGORY_APP_MAPS)
            };
            int last = rows - fine;
            for (int c = 0; c < Math.min(everyday.length, columns / fine); c++) {
                if (middle.free(c * fine, last)) {
                    place(middle, found.role(everyday[c], taken), c * fine, last, true);
                }
            }
            if (home + 1 < count) {
                Grid after = pages.get(home + 1);
                Apps.Door settings = found.role(new Intent(Settings.ACTION_SETTINGS), taken);
                if (after.free(fine, last)) {
                    place(after, settings, fine, last, true);
                }
                if (after.free(2 * fine, last)) {
                    ownDoor(after, 2 * fine, last);
                }
            }
            List<Apps.Door> vendor = found.vendor(taken);
            if (home - 1 >= 0 && !vendor.isEmpty() && pages.get(home - 1).free(0, last)) {
                folder(pages.get(home - 1), Apps.vendorName(vendor), vendor, 0, last,
                    Keep.VENDOR_THING);
            }
            for (Apps.Door door : vendor) {
                taken.add(door.name.getPackageName());
            }
            List<Apps.Door> system = found.system(taken);
            if (home + 1 < count && !system.isEmpty() && pages.get(home + 1).free(0, rows - fine)) {
                folder(pages.get(home + 1), SYSTEM, system, 0, rows - fine, Keep.SYSTEM_THING);
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
        screens.home(lying ? uprightToLying(Keep.home(this)) : Keep.home(this));
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
        perform(Keep.ON_DOUBLE, Keep.DO_LOCK);
    }

    private static final String KEEPER_TEXT = "To lock the phone, Ellipse is to be allowed to lock it, and nothing "
        + "more, as a keeper of the phone. The first unlock after may ask for the code rather than a finger.";

    /**
     * The phone locked, the way the settings choose: as a keeper of the
     * phone, asked for the power once; or through the accessibility service.
     */
    private void lock() {
        if (Keep.number(this, Keep.LOCK_WAY, Keep.LOCK_SERVICE) == Keep.LOCK_KEEPER) {
            android.app.admin.DevicePolicyManager keeper = (android.app.admin.DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
            android.content.ComponentName me = new android.content.ComponentName(this, Warden.class);
            if (keeper != null && keeper.isAdminActive(me)) {
                keeper.lockNow();
                return;
            }
            try {
                startActivity(new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, me)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, Words.t(KEEPER_TEXT)));
            } catch (RuntimeException none) {
                refuse(screens);
            }
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

    /** A row's share of its full height on the screens now. */
    private float rowShare = 1f;

    /**
     * The rows the screens hold, and the share of the height each takes.
     * With the screen's share, the grid's own rows, as high as the row
     * height says. With a shape, each place as high as its width asks —
     * square, wide or tall — and as many rows as fit; what is left over
     * stays free at the foot.
     */
    private float shapeRows() {
        int grid = Keep.number(this, Keep.DESK_GRID, 45);
        float ratio = Keep.cellRatio(Keep.number(this, Keep.CELL_SHAPE, Keep.SHAPE_SCREEN));
        if (ratio <= 0f) {
            rows = Keep.rows(grid) * fine;
            return Keep.number(this, Keep.ROW_HEIGHT, 100) / 100f;
        }
        float wide;
        float tall;
        if (screens != null && screens.getWidth() > 0 && screens.getHeight() > 0) {
            wide = screens.getWidth() - 2 * side();
            tall = screens.getHeight() - dp(16);
        } else {
            guessed = true;
            android.util.DisplayMetrics m = getResources().getDisplayMetrics();
            wide = m.widthPixels - 2 * side();
            tall = m.heightPixels * 0.72f;
        }
        float high = wide / Keep.columns(grid) * ratio;
        int count = Math.max(3, Math.min(16, (int) Math.floor(tall / high + 0.02f)));
        /* Never fewer rows than the things stand in: where the dock takes height, the rows grow shorter
           and the icons in them smaller, rather than the lowest rows going out of sight. */
        int used = (usedRows() + fine - 1) / fine;
        if (used > count) {
            count = Math.min(16, used);
        }
        rows = count * fine;
        /* The rows share the whole height: what would be left under the last row is spread between them. */
        return 1f;
    }

    /** How far down the things on any screen reach, in places of the grid's finest step. */
    private int usedRows() {
        int most = 0;
        for (Keep.Spot spot : Keep.placed(this)) {
            int tall = fine;
            if (spot.name == null) {
                String[] part = spot.token.split(":");
                try {
                    if ((spot.token.startsWith(WIDGET) || spot.token.startsWith(Keep.PILE_THING)
                        || spot.token.startsWith(Keep.FOLDER_THING)) && part.length >= 4) {
                        tall = Integer.parseInt(part[3]);
                    } else if (Keep.CLOCK_THING.equals(base(spot.token)) && part.length >= 3) {
                        tall = Integer.parseInt(part[2]);
                    }
                } catch (NumberFormatException broken) {
                    tall = fine;
                }
            }
            most = Math.max(most, spot.y + tall);
        }
        return most;
    }

    /** A screen's margin at each side: none if the grid runs to the edges whole. */
    private int side() {
        return Keep.edgeless(this) ? 0 : dp(8);
    }

    /** Whether the places were measured by a guess, before the screens had a size. */
    private boolean guessed;

    /**
     * A place's width and height in pixels: as the screens are laid out,
     * or, before they are, a guess at it, to be set right once they are.
     */
    private float[] place() {
        if (screens != null && screens.getWidth() > 0 && screens.getHeight() > 0) {
            return new float[] {(screens.getWidth() - 2 * side()) / (float) columns,
                (screens.getHeight() - dp(16)) / (float) rows * rowShare};
        }
        guessed = true;
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        return new float[] {(m.widthPixels - 2 * side()) / (float) columns,
            m.heightPixels * 0.72f / rows * rowShare};
    }

    /**
     * The least block the clock takes: its face's least box in dp, counted
     * in places of this grid. A grid of many small places gives it more of
     * them, so the clock is never drawn smaller than its face allows.
     */
    private int lyingCount;
    /** Lying down: the upright columns, the half part's columns, the rows, where the dock stands. */
    private int lyingCols;
    private int lyingHalf;
    private int lyingRows;
    private boolean lyingDockRow;
    /** Whether the dock's places run across, as upright, or down, as a column lying down. */
    private boolean dockUpright = true;
    private boolean lyingHalfUsed;
    /** Where each place lying down stands upright: "page:x:y" to screen, column, row. */
    private final java.util.Map<String, int[]> lyingToUpright = new java.util.HashMap<>();

    /**
     * The upright screens seen lying down, each on a screen of its own, in
     * three parts: one, one and a half.
     *
     * Part one is the upright screen's top and foot put together: its rows
     * that hold widgets, as they stand, icons beside a widget going with it,
     * and, if the dock stands there, the dock's row free under them. Part two
     * takes its rows of icons alone, still rows, as many as the height holds.
     * The half part takes the rows left over, each turned into a column. A
     * screen with no widgets gives its first part to rows of icons as well.
     * What does not fit runs on to one screen more. Nothing of it is kept:
     * it is worked out from the upright set-out each time.
     */
    private List<Keep.Spot> project(List<Keep.Spot> upright, int screens, boolean dockRow) {
        lyingToUpright.clear();
        lyingHalfUsed = false;
        List<Keep.Spot> out = new ArrayList<>();
        lyingFirst = new int[Math.max(1, screens)];
        boolean clocked = false;
        for (Keep.Spot spot : upright) {
            if (Keep.CLOCK_THING.equals(base(spot.token))) {
                clocked = true;
            }
        }
        if (!clocked && Keep.flag(this, Keep.CLOCK, true)) {
            upright = new ArrayList<>(upright);
            upright.add(new Keep.Spot(Keep.CLOCK_THING, Keep.home(this), 0, 0));
        }
        int partTwo = lyingCols * fine;
        int partHalf = 2 * lyingCols * fine;
        int partOneRows = rows - (dockRow ? fine : 0);
        int page = -1;
        for (int s = 0; s < screens; s++) {
            page++;
            lyingFirst[s] = page;
            List<Keep.Spot> mine = new ArrayList<>();
            for (Keep.Spot spot : upright) {
                if (spot.screen == s) {
                    mine.add(spot);
                }
            }
            /* The rows that hold anything larger than one place. */
            java.util.TreeSet<Integer> zone = new java.util.TreeSet<>();
            for (Keep.Spot spot : mine) {
                int[] span = lyingSpan(spot);
                if (span[0] > fine || span[1] > fine) {
                    for (int y = spot.y; y < spot.y + span[1]; y++) {
                        zone.add(y);
                    }
                }
            }
            java.util.Map<Integer, Integer> squeezed = new java.util.HashMap<>();
            int at = 0;
            for (int y : zone) {
                squeezed.put(y, at++);
            }
            /* Part one: the zone, as it stands; what does not fit goes to the top of part two. */
            int twoY = 0;
            List<Keep.Spot> inZone = new ArrayList<>();
            java.util.TreeMap<Integer, List<Keep.Spot>> iconRows = new java.util.TreeMap<>();
            for (Keep.Spot spot : mine) {
                if (zone.contains(spot.y)) {
                    inZone.add(spot);
                } else {
                    List<Keep.Spot> row = iconRows.get(spot.y);
                    if (row == null) {
                        row = new ArrayList<>();
                        iconRows.put(spot.y, row);
                    }
                    row.add(spot);
                }
            }
            java.util.Collections.sort(inZone, new java.util.Comparator<Keep.Spot>() {
                public int compare(Keep.Spot a, Keep.Spot b) {
                    return a.y != b.y ? Integer.compare(a.y, b.y) : Integer.compare(a.x, b.x);
                }
            });
            for (Keep.Spot spot : inZone) {
                int[] span = lyingSpan(spot);
                int y = squeezed.get(spot.y);
                if (y + span[1] <= partOneRows) {
                    keepLying(out, spot, page, Math.min(spot.x, partTwo - span[0]), y);
                } else if (twoY + span[1] <= rows) {
                    keepLying(out, spot, page, partTwo + Math.min(spot.x, partTwo - span[0]), twoY);
                    twoY += span[1];
                }
            }
            /* The rows of icons: part one too if it holds no widget, then part two, then the half part. */
            int oneY = zone.isEmpty() ? 0 : partOneRows;
            int halfColumn = 0;
            for (List<Keep.Spot> row : iconRows.values()) {
                if (oneY + fine <= partOneRows) {
                    for (Keep.Spot spot : row) {
                        keepLying(out, spot, page, Math.min(spot.x, partTwo - fine), oneY);
                    }
                    oneY += fine;
                } else if (twoY + fine <= rows) {
                    for (Keep.Spot spot : row) {
                        keepLying(out, spot, page, partTwo + Math.min(spot.x, partTwo - fine), twoY);
                    }
                    twoY += fine;
                } else if (halfColumn < lyingHalf * fine) {
                    lyingHalfUsed = true;
                    for (Keep.Spot spot : row) {
                        if (spot.x + fine <= rows) {
                            keepLying(out, spot, page, partHalf + halfColumn, spot.x);
                        }
                    }
                    halfColumn += fine;
                } else {
                    /* Run on to one screen more, its first part given to rows as well. */
                    page++;
                    oneY = 0;
                    twoY = 0;
                    halfColumn = 0;
                    for (Keep.Spot spot : row) {
                        keepLying(out, spot, page, Math.min(spot.x, partTwo - fine), oneY);
                    }
                    oneY += fine;
                }
            }
        }
        lyingCount = page + 1;
        return out;
    }

    private void keepLying(List<Keep.Spot> out, Keep.Spot spot, int page, int x, int y) {
        out.add(new Keep.Spot(spot.token, page, Math.max(0, x), Math.max(0, y)));
        lyingToUpright.put(page + ":" + Math.max(0, x) + ":" + Math.max(0, y), new int[] {spot.screen, spot.x, spot.y});
    }

    /** The window's width and height as it stands now, whichever way the phone is turned. */
    private int[] windowShape() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.graphics.Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
            return new int[] {bounds.width(), bounds.height()};
        }
        android.util.DisplayMetrics real = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(real);
        return new int[] {real.widthPixels, real.heightPixels};
    }

    /** A screen taken away, and what stood on it: the widgets there let go of. */
    private void dropScreen(int gone) {
        if (!Keep.laid(this)) {
            Keep.lay(this, standing);
        }
        for (Keep.Spot spot : Keep.placed(this)) {
            if (spot.screen != gone) {
                continue;
            }
            if (spot.token.startsWith(WIDGET)) {
                try {
                    drop(Integer.parseInt(spot.token.substring(WIDGET.length()).split(":")[0]));
                } catch (NumberFormatException broken) {
                    // Nothing to let go.
                }
            } else if (spot.token.startsWith(Keep.PILE_THING)) {
                int pile = pileId(spot.token);
                for (int id : Keep.pileItems(this, pile)) {
                    drop(id);
                }
                Keep.forgetPile(this, pile);
            } else if (spot.token.startsWith(Keep.SHORTCUT_THING)) {
                unpin(spot.token);
            }
        }
        Keep.dropScreen(this, gone);
        fill();
        screens.show(Math.max(0, gone - 1), true);
    }

    /** A veil over nothing, while the desk is away: the first touch brings it back. */
    private View bareVeil;

    /**
     * The wallpaper alone: everything on the screens and the dock goes away
     * softly, and the first touch anywhere brings it back.
     */
    private void bare() {
        if (bareVeil != null || frame == null) {
            return;
        }
        frame.animate().cancel();
        frame.animate().alpha(0f).setDuration(Pace.ARRIVE).start();
        bareVeil = new View(this);
        bareVeil.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    unbare();
                }
                return true;
            }
        });
        root.addView(bareVeil, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void unbare() {
        if (bareVeil == null) {
            return;
        }
        root.removeView(bareVeil);
        bareVeil = null;
        if (frame != null) {
            frame.animate().cancel();
            frame.animate().alpha(1f).setDuration(Pace.ARRIVE).start();
        }
    }

    // ------------------------------------------------------------- drift

    private final android.os.Handler drifting = new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.Random drift = new java.util.Random();

    /**
     * Every three minutes, and each time the home screen comes back, all of
     * it steps a few points aside, too slowly to be seen: an OLED screen then
     * keeps no line of it burnt in.
     */
    private final Runnable driftOn = new Runnable() {
        public void run() {
            step(true);
            drifting.postDelayed(this, 180000L);
        }
    };

    private void step(boolean slowly) {
        if (root == null) {
            return;
        }
        float reach = dp(3);
        float x = Keep.flag(this, Keep.DRIFT, true) ? (drift.nextFloat() * 2 - 1) * reach : 0f;
        float y = Keep.flag(this, Keep.DRIFT, true) ? (drift.nextFloat() * 2 - 1) * reach : 0f;
        if (slowly) {
            root.animate().translationX(x).translationY(y).setDuration(6000).start();
        } else {
            root.setTranslationX(x);
            root.setTranslationY(y);
        }
    }

    private int uprightToLying(int screen) {
        if (lyingFirst.length == 0) {
            return 0;
        }
        return lyingFirst[Math.max(0, Math.min(lyingFirst.length - 1, screen))];
    }

    /** A thing's size in places, as it stands upright, held within the grid lying down. */
    private int[] lyingSpan(Keep.Spot spot) {
        String token = spot.token;
        int[] span = {fine, fine};
        if (spot.name == null) {
            String[] part = token.split(":");
            try {
                if (token.startsWith(WIDGET) || token.startsWith(Keep.PILE_THING)) {
                    if (part.length >= 4) {
                        span = new int[] {Integer.parseInt(part[2]), Integer.parseInt(part[3])};
                    }
                } else if (token.startsWith(Keep.FOLDER_THING)) {
                    if (part.length >= 4) {
                        span = new int[] {Integer.parseInt(part[2]), Integer.parseInt(part[3])};
                    }
                } else if (Keep.CLOCK_THING.equals(base(token))) {
                    int[] least = clockLeast(place());
                    span = token.indexOf(':') > 0 ? folderSpan(token) : new int[] {uprightColumns, least[1]};
                    span[1] = Math.max(span[1], least[1]);
                }
            } catch (NumberFormatException broken) {
                span = new int[] {fine, fine};
            }
        }
        return new int[] {Math.max(1, Math.min(columns, span[0])), Math.max(1, Math.min(rows, span[1]))};
    }

    /** The first place, row by row, where a thing of this size fits whole, on the grid's whole steps. */
    private int[] firstFree(boolean[][] used, int[] span) {
        for (int r = 0; r + span[1] <= rows; r += fine) {
            for (int c = 0; c + span[0] <= columns; c += fine) {
                boolean free = true;
                for (int y = r; y < r + span[1] && free; y++) {
                    for (int x = c; x < c + span[0]; x++) {
                        if (used[y][x]) {
                            free = false;
                            break;
                        }
                    }
                }
                if (free) {
                    return new int[] {c, r};
                }
            }
        }
        return null;
    }

    private int[] clockLeast(float[] cell) {
        float d = getResources().getDisplayMetrics().density;
        float[] box = leastBox(this);
        return new int[] {Math.max(1, Math.min(columns, (int) Math.ceil(box[0] * d / cell[0] - 0.05f))),
            Math.max(1, Math.min(rows, (int) Math.ceil(box[1] * d / cell[1] - 0.05f)))};
    }

    /** The least box of the chosen face, in dp. */
    static float[] leastBox(Context context) {
        if (Keep.number(context, Keep.CLOCK_FACE, FACE_FIRST) == FACE_MENO) {
            return Meno.least();
        }
        if (Keep.number(context, Keep.CLOCK_FACE, FACE_FIRST) == FACE_RINGS) {
            return Rings.least();
        }
        return new float[] {240f, 96f};
    }

    /**
     * The clock, a thing of the grid like a widget: in the block it was
     * given, or, if never given one, across the whole width and as many
     * rows down as its face needs. A block too small for the face on this
     * grid grows to fit where the places are free; what is kept is the
     * block as given, so another grid measures it afresh.
     */
    private boolean clock(Grid page, int column, int row, String token) {
        if (!Keep.flag(this, Keep.CLOCK, true)) {
            return false;
        }
        int[] least = clockLeast(place());
        boolean given = token.indexOf(':') > 0;
        int[] span = given ? folderSpan(token) : new int[] {lying ? uprightColumns : columns, least[1]};
        int across = Math.min(columns - column, Math.max(span[0], least[0]));
        int down = -1;
        for (int d = Math.max(span[1], least[1]); d >= 1; d--) {
            if (row + d <= rows && page.fits(column, row, across, d)) {
                down = d;
                break;
            }
        }
        if (down < 0 || across < 1) {
            return false;
        }
        View clock = timepiece(this, new Almanac.Hand() {
            public void pressed(String window, View from, android.graphics.RectF box) {
                look(window, from, box);
            }
        });
        page.put(clock, column, row, across, down);
        page.edge(clock, Keep.edges(this));
        if (clock instanceof Timepiece) {
            boolean edges = Keep.edges(this);
            ((Timepiece) clock).edged(edges && column == 0, edges && column + across >= columns);
        }
        /* Its size as it stands, kept for the settings to show it at. */
        clock.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (r - l <= 0 || b - t <= 0 || (r - l == or - ol && b - t == ob - ot)) {
                    return;
                }
                float d = getResources().getDisplayMetrics().density;
                Keep.note(Home.this, Keep.CLOCK_WIDE, Math.round((r - l) / d));
                Keep.note(Home.this, Keep.CLOCK_TALL, Math.round((b - t) / d));
            }
        });
        cells.add(clock);
        Timepiece piece = (Timepiece) clock;
        piece.weather(Keep.flag(this, Keep.WEATHER, true));
        piece.ears(Keep.flag(this, Keep.EARS, true) ? earsLevel : -1);
        clockView = piece;
        stand(page, clock, token);
        return true;
    }

    /** The first clock, left as it was, or another the settings chose. */
    static final int FACE_FIRST = 0;
    static final int FACE_PLATE = 1;
    static final int FACE_MENO = 2;
    static final int FACE_RINGS = 3;
    static final String[] FACE_NAMES = {"First", "Plate", "Meno", "Rings"};

    static View timepiece(Context context, Almanac.Hand hand) {
        return timepiece(context, hand, Keep.number(context, Keep.CLOCK_FACE, FACE_FIRST));
    }

    /** A face of the kind asked for, whatever the home screen wears. */
    static View timepiece(Context context, Almanac.Hand hand, int kind) {
        if (kind == FACE_RINGS) {
            return new Rings(context, hand);
        }
        if (kind == FACE_MENO) {
            return new Meno(context, hand);
        }
        if (kind == FACE_PLATE) {
            return new Watch(context, Keep.number(context, Keep.CLOCK_PLATE, Rim.STEEL),
                Keep.number(context, Keep.CLOCK_DIAL, Watch.DARK), Keep.number(context, Keep.CLOCK_FIELDS, Watch.DARK),
                hand);
        }
        return new Almanac(context, hand);
    }

    /** The door to this home screen's own settings. */
    /** The door to this home screen's settings: not its own icon, but a face of their own. */
    private Cell ownCell(boolean named) {
        Cell own = new Cell(this, Style.dress(this, Keep.OWN_THING, getDrawable(R.mipmap.door), null),
            OWN, iconSize,
            named && Style.namesOnScreens);
        own.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tune(v);
            }
        });
        return own;
    }

    // ------------------------------------------------------------- grey

    /**
     * The grey, on or off: the home screen drawn without colour — the screens,
     * the dock, the list of every app. The whole phone goes grey only by the
     * phone's own settings; the home screen does not reach into them.
     */
    private void grey(boolean on) {
        Keep.saveFlag(this, Keep.GREY, on);
        greyNow();
    }

    /** The home screen drawn without colour while the grey is on. */
    private void greyNow() {
        if (root == null) {
            return;
        }
        if (Keep.flag(this, Keep.GREY, false)) {
            android.graphics.ColorMatrix none = new android.graphics.ColorMatrix();
            none.setSaturation(0f);
            android.graphics.Paint grey = new android.graphics.Paint();
            grey.setColorFilter(new android.graphics.ColorMatrixColorFilter(none));
            root.setLayerType(View.LAYER_TYPE_HARDWARE, grey);
        } else {
            root.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }


    /** The dice's face: a die on the plate every icon of the home screen's own wears. */
    private android.graphics.drawable.Drawable diceFace() {
        return Style.dress(this, Keep.DICE_THING, diceRaw(Die.roll()), null);
    }

    /**
     * The die as an app's own icon is made: a plate, the die on it, and the die
     * again as the one-colour layer a look inks icons from — so under any look,
     * inked or not, the die stands out as clearly as an app's own sign.
     */
    private android.graphics.drawable.Drawable diceRaw(int shown) {
        Die front = new Die(shown);
        front.setTint(Tone.primary());
        android.graphics.drawable.Drawable plate = new android.graphics.drawable.ColorDrawable(Tone.primaryContainer());
        android.graphics.drawable.Drawable sign = new android.graphics.drawable.InsetDrawable(front, 0.26f);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            Die mono = new Die(shown);
            return new android.graphics.drawable.AdaptiveIconDrawable(plate, sign,
                new android.graphics.drawable.InsetDrawable(mono, 0.26f));
        }
        return new android.graphics.drawable.AdaptiveIconDrawable(plate, sign);
    }

    /**
     * The dice on a screen: a touch throws a new ground and sets it as the
     * wallpaper, from where the grounds come from as the settings say — the
     * dice, the ready ones, or the owner's own.
     */
    private Cell diceCell() {
        final Cell dice = new Cell(this, diceFace(), Words.t(NEW_GROUND), iconSize, Style.namesOnScreens);
        dice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!Picture.replaceable(Home.this)) {
                    Ask.tell(root, "Replace the wallpaper?", DICE_WARNING, "Replace", new Runnable() {
                        public void run() {
                            Keep.saveFlag(Home.this, Keep.WALL_WARNED, true);
                            throwGround(dice);
                        }
                    });
                    return;
                }
                throwGround(dice);
            }
        });
        return dice;
    }

    private static final String DICE_WARNING = "The wallpaper the phone has now is replaced, and Ellipse "
        + "cannot keep it for you, for the phone does not let it be read. If it is a picture of your "
        + "own, bring it in first with Wallpaper from a picture: Ellipse keeps it, sets it again at a "
        + "touch and carries it in copies.";

    private void throwGround(final View from) {
        /* The die turns over and comes up with another face, as a die does. */
        from.animate().rotationBy(360f).setDuration(700).setInterpolator(Pace.EMPHASIS).start();
        if (from instanceof Cell) {
            from.postDelayed(new Runnable() {
                public void run() {
                    ((Cell) from).picture(diceFace());
                }
            }, 350);
        }
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            public void run() {
                try {
                    Turn.turn(app);
                } catch (Exception | OutOfMemoryError failed) {
                    // The ground stays as it was.
                }
            }
        }).start();
    }

    private void ownDoor(Grid page, int column, int row) {
        Cell own = ownCell(true);
        page.put(own, column, row);
        cells.add(own);
        stand(page, own, Keep.OWN_THING);
    }

    /** Whether a block of places on a grid is all free. */
    private static boolean free(Grid grid, int x, int y, int across, int down) {
        if (!grid.free(x, y, across, down)) {
            return false;
        }
        return true;
    }

    /** A folder on a screen: its face, and the card it opens into. */
    private void folder(Grid into, final CharSequence name, final List<Apps.Door> doors,
                        int column, int row, String token) {
        int[] span = folderSpan(token);
        int across = Math.min(columns - column, span[0]);
        int down = Math.min(rows - row, span[1]);
        if (across * down > fine * fine && into.fits(column, row, across, down)) {
            float cell = (windowShape()[0] - 2 * side()) / (float) columns;
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
        final Cell cell = new Cell(this, folderFace(token, doors), name, iconSize, Style.namesOnScreens);
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
    private void look(String window, final View from, android.graphics.RectF box) {
        if (Almanac.PLAYER.equals(window)) {
            /* The player's ring pauses or goes on with whatever plays, and says so at once. */
            Playing.toggle(this);
            from.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            from.postDelayed(new Runnable() {
                public void run() {
                    from.invalidate();
                }
            }, 300);
            return;
        }
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

    /**
     * A picture of the home screen to show others: the screen as it stands,
     * on its wallpaper, set in a card beside the day's date, a swatch of the
     * wallpaper and the home screen's name — saved among the phone's
     * pictures and offered to be sent.
     */
    private void portrait() {
        final int w = root.getWidth();
        final int h = root.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        final Bitmap front = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(front));
        /* The wallpaper under the screen: the factory's own, drawn again, when the phone's wallpaper is the
           one it set last; else the phone's own picture, read from its file where that is allowed. */
        android.app.WallpaperManager walls = android.app.WallpaperManager.getInstance(this);
        /* The factory's word that the wallpaper is its own: taken back whenever another sets one. */
        final boolean ours = Keep.flag(this, Keep.GROUND_WORN, false);
        android.graphics.drawable.Drawable seen = null;
        if (!ours && Keep.flag(this, Keep.PICTURE_WORN, false) && Picture.kept(this)) {
            /* The owner's own picture, set from here: read from the home screen's own keeping. */
            Bitmap picture = android.graphics.BitmapFactory.decodeFile(Picture.file(this).getPath());
            if (picture != null) {
                seen = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
            }
        }
        if (seen == null && !ours && Copy.wallpaperReadable()) {
            try (android.os.ParcelFileDescriptor file = walls.getWallpaperFile(android.app.WallpaperManager.FLAG_SYSTEM)) {
                if (file != null) {
                    Bitmap picture = android.graphics.BitmapFactory.decodeFileDescriptor(file.getFileDescriptor());
                    if (picture != null) {
                        seen = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
                    }
                }
            } catch (java.io.IOException | RuntimeException unseen) {
                seen = null;
            }
        }
        final android.graphics.drawable.Drawable wall = seen;
        final Ground ground = Ground.kept(this);
        final String day = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG,
            java.text.DateFormat.SHORT, Words.locale()).format(new java.util.Date());
        /* The data under the picture: the phone, its Android, the grid of the screens. */
        int grid = Keep.number(this, Keep.DESK_GRID, 45);
        final String data = android.os.Build.MANUFACTURER.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
            + android.os.Build.MANUFACTURER.substring(1) + " " + android.os.Build.MODEL + "  \u00B7  Android "
            + android.os.Build.VERSION.RELEASE + "  \u00B7  " + Keep.columns(grid) + " \u00D7 " + Keep.rows(grid);
        final int accent = Tone.primary();
        final String version = Copy.version(this);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Bitmap back = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(back);
                    if (wall != null) {
                        float scale = Math.max(w / (float) wall.getIntrinsicWidth(), h / (float) wall.getIntrinsicHeight());
                        int dw = Math.round(wall.getIntrinsicWidth() * scale);
                        int dh = Math.round(wall.getIntrinsicHeight() * scale);
                        wall.setBounds((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2);
                        wall.draw(c);
                    } else if (ours) {
                        Bitmap g = ground.draw(w, h);
                        c.drawBitmap(g, 0, 0, null);
                        g.recycle();
                    } else {
                        c.drawColor(0xFF15130F);
                    }
                    Bitmap shot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas s = new Canvas(shot);
                    s.drawBitmap(back, 0, 0, null);
                    s.drawBitmap(front, 0, 0, null);
                    front.recycle();
                    Bitmap made = Portrait.compose(shot, back, day, version, data, accent);
                    shot.recycle();
                    back.recycle();
                    final android.net.Uri saved = Portrait.save(Home.this, made);
                    made.recycle();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (saved == null) {
                                refuse(screens);
                                return;
                            }
                            Intent send = new Intent(Intent.ACTION_SEND).setType("image/png")
                                .putExtra(Intent.EXTRA_STREAM, saved).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivity(Intent.createChooser(send, null));
                            } catch (RuntimeException none) {
                                refuse(screens);
                            }
                        }
                    });
                } catch (Exception | OutOfMemoryError failed) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            refuse(screens);
                        }
                    });
                }
            }
        }).start();
    }

    private final android.content.BroadcastReceiver sleep = new android.content.BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_WALLPAPER_CHANGED.equals(intent.getAction())) {
                /* Another wallpaper, not set by the factory a moment ago: the factory's word is taken back. */
                if (System.currentTimeMillis() - Keep.clock(context, Keep.GROUND_SET_AT) > 15000L) {
                    Keep.saveFlag(context, Keep.GROUND_WORN, false);
                    Keep.saveFlag(context, Keep.PICTURE_WORN, false);
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                Turn.slept(context);
            } else {
                Turn.woke(context);
            }
        }
    };

    // ------------------------------------------------------------- menu

    private static final String[] ASKS = {
        "Add screen", "Add shortcut", "Add widget", "Add folder", "Make home screen", "Settings",
        "Remove screen", "Picture of the home screen", "Night clock", "Show the wallpaper"
    };
    private static final int BARE = 9;
    private static final int REMOVE_SCREEN = 6;
    private static final int PORTRAIT = 7;
    private static final int NIGHT = 8;
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
        /* Two parts: what adds to the screens; and, under a hairline, always in sight, what is wanted at hand. */
        List<Integer> offered = new ArrayList<>();
        /* Lying down, the screens are a view of the upright ones: things are added and moved upright. */
        if (!lying) {
            for (int i = ADD_SCREEN; i <= ADD_FOLDER; i++) {
                offered.add(i);
            }
            if (!home) {
                offered.add(MAKE_HOME);
            }
            /* A screen can be taken away with what stands on it too; it asks first, then. */
            if (pages.size() > 1) {
                offered.add(REMOVE_SCREEN);
            }
        }
        List<String> handLines = new ArrayList<>();
        List<Integer> handKeys = new ArrayList<>();
        List<Integer> handGlyphs = new ArrayList<>();
        if (!lying) {
            handLines.add(ASKS[PORTRAIT]);
            handKeys.add(PORTRAIT);
            handGlyphs.add(Glyph.DESK);
        }
        handLines.add(ASKS[NIGHT]);
        handKeys.add(NIGHT);
        handGlyphs.add(Glyph.CLOCK);
        handLines.add(ASKS[BARE]);
        handKeys.add(BARE);
        handGlyphs.add(Glyph.LOOK);
        handLines.add(ASKS[SETTINGS]);
        handKeys.add(SETTINGS);
        handGlyphs.add(Glyph.SETTINGS);

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
        Menu.Section hand = pictured(handLines, handKeys, handGlyphs);
        menu.show(offered.isEmpty() ? new Menu.Section[] {hand}
                : new Menu.Section[] {new Menu.Section(null, lines, keys), hand},
            root.fingerX(), root.fingerY(), dp(20));
    }

    private void act(int key) {
        menu.hide(true);
        switch (key) {
            case NIGHT:
                Night.open(this, false);
                break;
            case BARE:
                bare();
                break;
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
                pileShelf = null;
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
                final int gone = screens.page();
                int held = 0;
                for (Keep.Spot spot : Keep.placed(this)) {
                    if (spot.screen == gone) {
                        held++;
                    }
                }
                if (held == 0) {
                    dropScreen(gone);
                } else {
                    Ask.tell(root, "Remove this screen?", Words.n("It holds %1 thing; it goes with it. | It holds %1 "
                        + "things; they go with it.", held), "Remove", new Runnable() {
                            public void run() {
                                dropScreen(gone);
                            }
                        });
                }
                break;
            case PORTRAIT:
                /* After the menu has gone, so it is not in the picture. */
                root.postDelayed(new Runnable() {
                    public void run() {
                        portrait();
                    }
                }, Pace.ARRIVE);
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
        final Cell cell = new Cell(this, folderFace(token, doors), name, iconSize, false);
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
        if (lift != null || lying) {
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

        /* A thing of one place, from the dock or a list, takes a whole
           place on the screens: with half steps, two by two small ones. */
        if (across <= 1 && down <= 1) {
            across = fine;
            down = fine;
        }
        carried = token;
        carriedAcross = across;
        carriedDown = down;
        origin = whence;
        wideLift = across * down > fine * fine;
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
        boolean small = carriedAcross == fine && carriedDown == fine;
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
        if (!binned && slot < 0 && (app || carried.startsWith(WIDGET))) {
            /* An app, or a widget, may go into a folder. */
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
            /* With things over one another, only the folder's middle takes an
               app in; its edges are for setting the app down over it. */
            float grace = Keep.flag(this, Keep.OVERLAP, false) ? -Math.min(view.getWidth(), view.getHeight()) / 4f : 0f;
            if (over(view, x, y, grace)) {
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
            if (whence != null && whence[0] >= 0) {
                /* Put in a folder, a thing leaves the place it stood in: a widget cannot stand in two. */
                if (!Keep.laid(this)) {
                    Keep.lay(this, standing);
                }
                Keep.remove(this, whence[0], whence[1], whence[2]);
            }
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
        if (isFolder(token) && whence[0] != -2) {
            /* A folder taken off lets go of the widgets it held. */
            for (String item : Keep.folderItems(this, folderId(token))) {
                if (item.startsWith(WIDGET)) {
                    try {
                        drop(Integer.parseInt(item.substring(WIDGET.length()).split(":")[0]));
                    } catch (NumberFormatException broken) {
                        // Nothing to let go.
                    }
                    Keep.folderRemove(this, folderId(token), item);
                }
            }
        }
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
        } else if (token.startsWith(Keep.PILE_THING)) {
            /* A pile taken off lets go of every widget it held. */
            int pile = pileId(token);
            for (int id : Keep.pileItems(this, pile)) {
                drop(id);
            }
            Keep.forgetPile(this, pile);
        } else if (Keep.CLOCK_THING.equals(base(token))) {
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
        if (token.startsWith(Keep.PILE_THING)) {
            String[] part = token.split(":");
            return Keep.PILE_THING + (part.length > 1 ? part[1] : "0");
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
            || word.startsWith(Keep.FOLDER_THING) || Keep.CLOCK_THING.equals(word)
            || word.startsWith(Keep.PILE_THING)) {
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
        int minA = fine;
        int minD = fine;
        int maxA = columns;
        int maxD = rows;
        boolean wide = true;
        boolean tall = true;
        if (Keep.CLOCK_THING.equals(base(token))) {
            int[] least = clockLeast(new float[] {page.cellWidth(), page.cellHeight()});
            minA = Math.min(least[0], block[2]);
            minD = Math.min(least[1], block[3]);
        }
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
                    kept = a == fine && d == fine ? word : word + ":" + a + ":" + d;
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
        int page = pages.indexOf((Grid) thing.getParent());
        /* Lying down, a thing's menu works on where it stands upright. */
        int[] whence = lying ? lyingToUpright.get(page + ":" + at[0] + ":" + at[1])
            : new int[] {page, at[0], at[1]};
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
        if (door != null || token.startsWith(Keep.SHORTCUT_THING) || Keep.OWN_THING.equals(token)
            || isFolder(token) || token.startsWith(Keep.RUN_THING)) {
            lines.add(FACE_LINE);
            keys.add(KEY_FACE);
            glyphs.add(Glyph.ICONS);
        }
        if (door != null && Kinds.mode(this) != Kinds.NONE) {
            lines.add(Words.t(KIND) + ": " + Words.t(Kinds.of(this, door)));
            keys.add(KEY_KIND);
            glyphs.add(Glyph.LIST);
        }
        if (door != null && !systemApp(door)) {
            lines.add(UNINSTALL);
            keys.add(KEY_UNINSTALL);
            glyphs.add(Glyph.TRASH);
        }
        if (token.startsWith(Keep.FOLDER_THING) || door != null || token.startsWith(Keep.SHORTCUT_THING)
            || token.startsWith(Keep.LINK_THING) || token.startsWith(Keep.RUN_THING)) {
            lines.add(RENAME);
            keys.add(KEY_RENAME);
            glyphs.add(Glyph.PEN);
        }
        if (whence != null && whence[0] >= 0 && (token.startsWith(WIDGET) || token.startsWith(Keep.PILE_THING))) {
            lines.add(PILE_LINE);
            keys.add(KEY_PILE);
            glyphs.add(Glyph.LIST);
        }
        if (whence != null && whence[0] >= 0 && resizable(token)) {
            lines.add(RESIZE);
            keys.add(KEY_RESIZE);
            glyphs.add(Glyph.RESIZE);
        }
        if (token.startsWith(WIDGET) && Keep.number(this, Keep.WIDGET_FRAME, Rim.NONE) != Rim.NONE) {
            boolean off = Keep.flag(this, Keep.FRAME_OFF + widgetId(token), false);
            lines.add(off ? FRAME_ON : FRAME_OFF);
            keys.add(KEY_FRAME);
            glyphs.add(Glyph.RESIZE);
        }
        if (whence != null && whence[0] >= 0 && Keep.flag(this, Keep.OVERLAP, false)) {
            lines.add(FRONT);
            keys.add(KEY_FRONT);
            glyphs.add(Glyph.FRONT);
            lines.add(BEHIND);
            keys.add(KEY_BEHIND);
            glyphs.add(Glyph.BEHIND);
        }
        if (thing instanceof Rings) {
            lines.add(ARRANGE);
            keys.add(KEY_ARRANGE);
            glyphs.add(Glyph.HANDS);
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
        menuX = x;
        menuY = y;
        menuGap = gap;
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
        if (Keep.CLOCK_THING.equals(base(token))) {
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
            } else if (key == KEY_KIND && offerDoor != null) {
                /* A second menu in the first's place: every kind, and a new one. */
                kindChoices = Kinds.choices(this);
                final List<String> shown = new ArrayList<>(kindChoices);
                shown.add(NEW_KIND);
                final int[] picks = new int[shown.size()];
                for (int i = 0; i < picks.length; i++) {
                    picks[i] = KEY_KIND_PICK + i;
                }
                root.postDelayed(new Runnable() {
                    public void run() {
                        menu.show(new Menu.Section[] {new Menu.Section(KIND, shown.toArray(new String[0]), picks)},
                            menuX, menuY, menuGap);
                    }
                }, Pace.PRESS);
            } else if (key >= KEY_KIND_PICK && key < KEY_KIND_PICK + 100 && offerDoor != null) {
                final Apps.Door door = offerDoor;
                int which = key - KEY_KIND_PICK;
                if (which < kindChoices.size()) {
                    Kinds.put(this, door, kindChoices.get(which));
                    fill();
                } else {
                    Ask.show(root, NEW_KIND.replace("\u2026", ""), "", new Ask.Answer() {
                        public void answered(String text) {
                            String name = text == null ? "" : text.trim();
                            if (!name.isEmpty()) {
                                Kinds.put(Home.this, door, name);
                                fill();
                            }
                        }
                    });
                }
            } else if (key == KEY_FRAME) {
                String off = Keep.FRAME_OFF + widgetId(offerToken);
                Keep.saveFlag(this, off, !Keep.flag(this, off, false));
                fill();
            } else if ((key == KEY_FRONT || key == KEY_BEHIND) && offerWhence != null) {
                if (!Keep.laid(this)) {
                    Keep.lay(this, standing);
                }
                Keep.stack(this, offerWhence[0], offerWhence[1], offerWhence[2], key == KEY_FRONT);
                fill();
            } else if (key == KEY_ARRANGE && offerView instanceof Rings) {
                arrange((Rings) offerView);
            } else if (key == KEY_PILE && offerWhence != null && offerWhence[0] >= 0) {
                openPile(new int[] {offerWhence[0], offerWhence[1], offerWhence[2]});
            } else if (key == KEY_RENAME) {
                if (offerToken.startsWith(Keep.FOLDER_THING)) {
                    rename(folderId(offerToken));
                } else {
                    renameThing(offerToken, nameOfThing(offerView, offerToken, offerDoor));
                }
            } else if (key == KEY_FACE) {
                /* A folder's face is kept by the folder, whatever size it stands at. */
                chooseFace(isFolder(offerToken) ? base(offerToken) : offerToken, offerDoor);

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
            icon = Style.dress(this, token,
                launcher.getShortcutIconDrawable(info, getResources().getDisplayMetrics().densityDpi), null);
        } catch (RuntimeException none) {
            icon = null;
        }
        CharSequence label = Style.nameOf(token) != null ? Style.nameOf(token) : info.getShortLabel();
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

    /**
     * An app waited for, standing grey in its place with the name it was
     * brought in by; a touch looks for it in the store. Once installed, the
     * app itself stands there.
     */
    private void awaited(Grid page, Keep.Spot spot, String name) {
        final String pkg = spot.name.getPackageName();
        android.graphics.drawable.GradientDrawable ground = new android.graphics.drawable.GradientDrawable();
        ground.setColor(Tone.container());
        ground.setStroke(Math.max(1, dp(1)), Tone.outline());
        ground.setSize(dp(48), dp(48));
        final Cell cell = new Cell(this, Style.dress(this, spot.token, ground, null),
            name.isEmpty() ? pkg : name, iconSize, Style.namesOnScreens);
        cell.setAlpha(0.55f);
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (RuntimeException none) {
                    refuse(cell);
                }
            }
        });
        page.put(cell, spot.x, spot.y);
        cells.add(cell);
        stand(page, cell, spot.token);
    }

    /** A link standing on a screen: its own picture and name, and its call made at a touch. */
    private void linked(Grid page, Keep.Spot spot) {
        if (!page.free(spot.x, spot.y)) {
            return;
        }
        final int id;
        try {
            id = Integer.parseInt(spot.token.substring(Keep.LINK_THING.length()));
        } catch (NumberFormatException broken) {
            return;
        }
        final String call = Keep.linkCall(this, id);
        if (call == null) {
            return;
        }
        android.graphics.drawable.Drawable picture = null;
        java.io.File file = Keep.linkPicture(this, id);
        if (file.isFile()) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getPath());
            if (bitmap != null) {
                picture = new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
            }
        }
        if (picture == null) {
            picture = new android.graphics.drawable.ColorDrawable(Tone.primaryContainer());
        }
        String given = Style.nameOf(spot.token);
        final Cell cell = new Cell(this, Style.dress(this, spot.token, picture, null),
            given != null ? given : Keep.linkName(this, id),
            iconSize, Style.namesOnScreens);
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent open = Intent.parseUri(call, 0);
                    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    open.setSourceBounds(boundsOf(cell));
                    startActivity(open, ActivityOptions.makeClipRevealAnimation(cell, 0, 0, cell.getWidth(),
                        cell.getHeight()).toBundle());
                } catch (java.net.URISyntaxException | RuntimeException gone) {
                    refuse(cell);
                }
            }
        });
        page.put(cell, spot.x, spot.y);
        cells.add(cell);
        stand(page, cell, spot.token);
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
            /* Set down by itself, a thing takes whole places, never half ones. */
            for (int r = rows - fine; r >= 0; r -= fine) {
                for (int c = 0; c < columns; c += fine) {
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
        /* In the list, the door wears the list's own look, as every other maker's icon does; on a screen,
           the look of the home screen's icons. */
        all.add(new Chooser.Item(getDrawable(R.mipmap.door), Words.t(OWN_SETTINGS), OWN_MAKER));
        int dpi = getResources().getDisplayMetrics().densityDpi;
        for (int i = 0; i < makers.size(); i++) {
            all.add(new Chooser.Item(makers.get(i).getIcon(dpi), makers.get(i).getLabel(), i));
        }
        groups.add(all);
        /* The apps themselves, their icons set down as from the list of every app. */
        choiceApps = new Apps(this).all(Keep.BY_NAME);
        List<Chooser.Item> apps = new ArrayList<>();
        List<Chooser.Item> ways = new ArrayList<>();
        for (int i = 0; i < choiceApps.size(); i++) {
            Apps.Door door = choiceApps.get(i);
            if (door.serial != 0) {
                continue;
            }
            apps.add(new Chooser.Item(door.icon(), door.label, CHOOSE_APP + i));
            /* One picture cannot stand in two places: the second line gets its own copy. */
            android.graphics.drawable.Drawable.ConstantState again = door.icon().getConstantState();
            ways.add(new Chooser.Item(again != null ? again.newDrawable(getResources()) : door.plain(), door.label,
                CHOOSE_RUNS + i));
        }
        groups.add(apps);
        /* Runs: an app's other ways in, chosen from the app. */
        groups.add(ways);
        choosingFace = false;
        chooser.hold(null);
        chooser.hint(SEARCH_SHORTCUTS);
        chooser.show(new String[] {null, "Apps", "Runs"}, groups,
            Keep.number(this, Keep.MAKERS_VIEW, Keep.LINES) == Keep.PAGES);
    }

    private static final int CHOOSE_APP = 100000;
    private static final int CHOOSE_RUNS = 200000;
    private static final int CHOOSE_RUN = 300000;
    private List<Apps.Door> choiceApps = new ArrayList<>();
    private final List<android.content.pm.ActivityInfo> choiceRuns = new ArrayList<>();

    /**
     * An app's runs: the screens it lets any other app open by name, open
     * to all and switched on, asking no leave — every other is left out, for
     * it would not open from here. Its front door is not among them.
     */
    private void showRuns(Apps.Door door) {
        choiceRuns.clear();
        android.content.pm.PackageManager pm = getPackageManager();
        try {
            android.content.pm.PackageInfo info = pm.getPackageInfo(door.name.getPackageName(),
                android.content.pm.PackageManager.GET_ACTIVITIES);
            if (info.activities != null) {
                for (android.content.pm.ActivityInfo one : info.activities) {
                    if (!one.exported || !one.enabled || one.permission != null
                        || one.name.equals(door.name.getClassName())) {
                        continue;
                    }
                    choiceRuns.add(one);
                }
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException gone) {
            choiceRuns.clear();
        }
        if (choiceRuns.isEmpty()) {
            said("This app has no other ways in");
            return;
        }
        final java.text.Collator order = java.text.Collator.getInstance();
        final android.content.pm.PackageManager names = pm;
        java.util.Collections.sort(choiceRuns, new java.util.Comparator<android.content.pm.ActivityInfo>() {
            public int compare(android.content.pm.ActivityInfo a, android.content.pm.ActivityInfo b) {
                int by = order.compare(String.valueOf(a.loadLabel(names)), String.valueOf(b.loadLabel(names)));
                return by != 0 ? by : a.name.compareTo(b.name);
            }
        });
        List<Chooser.Item> runs = new ArrayList<>();
        for (int i = 0; i < choiceRuns.size(); i++) {
            android.content.pm.ActivityInfo one = choiceRuns.get(i);
            Chooser.Item item = new Chooser.Item(one.loadIcon(pm), one.loadLabel(pm), CHOOSE_RUN + i);
            runs.add(item.noted(shortClass(one.packageName, one.name)));
        }
        List<List<Chooser.Item>> groups = new ArrayList<>();
        groups.add(runs);
        choosingFace = false;
        chooser.hold(null);
        chooser.hint(SEARCH_SHORTCUTS);
        chooser.show(new String[] {door.label.toString()}, groups, false);
    }

    /** A screen's class as short as it reads: without its package, where it begins with it. */
    private static String shortClass(String owner, String name) {
        return name.startsWith(owner + ".") ? name.substring(owner.length()) : name;
    }

    /** A short word on the screen, for a moment. */
    private void said(String words) {
        android.widget.Toast.makeText(this, Words.t(words), android.widget.Toast.LENGTH_LONG).show();
    }

    /** A maker was chosen: its own window makes the shortcut, and the answer comes back as a pin request. */
    private void make(int which) {
        if (which >= CHOOSE_RUN) {
            int at = which - CHOOSE_RUN;
            if (at >= 0 && at < choiceRuns.size()) {
                android.content.pm.ActivityInfo one = choiceRuns.get(at);
                String token = Keep.RUN_THING + new android.content.ComponentName(one.packageName, one.name)
                    .flattenToString();
                Keep.saveRunName(this, token, String.valueOf(one.loadLabel(getPackageManager())));
                setAnywhere(token, pendingPage);
            }
            return;
        }
        if (which >= CHOOSE_RUNS) {
            int at = which - CHOOSE_RUNS;
            if (at >= 0 && at < choiceApps.size()) {
                showRuns(choiceApps.get(at));
            }
            return;
        }
        if (which >= CHOOSE_APP) {
            int at = which - CHOOSE_APP;
            if (at >= 0 && at < choiceApps.size()) {
                setAnywhere(choiceApps.get(at).token(), pendingPage);
            }
            return;
        }
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

    // ---------------------------------------------------------- one icon's face

    /** An icon as its app or its maker gives it, before any outline or colour. */
    /** The word the door to every app is kept by, for a face of its own. */
    static final String ALL_THING = "#all";

    /** The door to every app as an icon: its dots on the accent. */
    private android.graphics.drawable.Drawable allRaw() {
        android.graphics.drawable.Drawable dots = getDrawable(R.drawable.sym_apps).mutate();
        dots.setTint(Tone.onAccent());
        return new android.graphics.drawable.AdaptiveIconDrawable(
            new android.graphics.drawable.ColorDrawable(Tone.primary()),
            new android.graphics.drawable.InsetDrawable(dots, 0.3f));
    }

    /** The door's own face, if the owner gave it one; none keeps the round button. */
    private android.graphics.drawable.Drawable allFace() {
        int[] mine = Style.faceOf(ALL_THING);
        boolean given = mine[1] >= 0 || mine[3] > 0 || mine[4] == 1 || Style.symbolOf(ALL_THING).length() > 0;
        return given ? Style.dress(this, ALL_THING, allRaw(), null) : null;
    }

    private android.graphics.drawable.Drawable rawIcon(String token, Apps.Door door) {
        if (ALL_THING.equals(token)) {
            return allRaw();
        }
        if (door != null) {
            return door.plain();
        }
        if (Keep.OWN_THING.equals(token)) {
            return getDrawable(R.mipmap.door);
        }
        if (token.startsWith(Keep.SHORTCUT_THING)) {
            android.content.pm.ShortcutInfo info = pinnedInfo(token);
            if (info != null) {
                try {
                    return launcher.getShortcutIconDrawable(info, getResources().getDisplayMetrics().densityDpi);
                } catch (RuntimeException none) {
                    return null;
                }
            }
        }
        if (token.startsWith(Keep.RUN_THING)) {
            android.content.pm.ActivityInfo info = runInfo(token);
            return info == null ? null : info.loadIcon(getPackageManager());
        }
        if (isFolder(token)) {
            return new Stack(faceDoors(token, folderDoors(token, new Apps(this), new java.util.HashSet<String>())),
                Keep.folderLook(this, base(token)));
        }
        return null;
    }

    /**
     * What a folder's face shows: its apps, and for each widget it holds, the
     * icon of the app the widget comes from — a folder of widgets alone is
     * not a blank. A folder given a picture or a symbol of its own wears it.
     */
    private android.graphics.drawable.Drawable folderFace(String token, List<Apps.Door> doors) {
        String kept = base(token);
        Stack face = new Stack(faceDoors(token, doors), Keep.folderLook(this, kept), widgetPictures(token));
        int[] own = Style.faceOf(kept);
        if (own[4] == 1 || Style.symbolOf(kept).length() > 0) {
            return Style.dress(this, kept, face, null);
        }
        return face;
    }

    /** The pictures of a folder's widgets whose apps have no front door to show instead. */
    private List<android.graphics.drawable.Drawable> widgetPictures(String token) {
        List<android.graphics.drawable.Drawable> out = new ArrayList<>();
        Apps found = null;
        int dpi = getResources().getDisplayMetrics().densityDpi;
        for (String item : Keep.folderItems(this, folderId(token))) {
            if (!item.startsWith(WIDGET)) {
                continue;
            }
            android.appwidget.AppWidgetProviderInfo info = widgetOf(item);
            if (info == null || info.provider == null) {
                continue;
            }
            if (found == null) {
                found = new Apps(this);
            }
            if (found.ofPackage(info.provider.getPackageName()) == null) {
                try {
                    android.graphics.drawable.Drawable own = info.loadIcon(this, dpi);
                    if (own != null) {
                        out.add(Shape.face(own));
                    }
                } catch (RuntimeException unseen) {
                    // Left out.
                }
            }
        }
        return out;
    }

    private List<Apps.Door> faceDoors(String token, List<Apps.Door> doors) {
        List<Apps.Door> face = new ArrayList<>(doors);
        Apps found = null;
        for (String item : Keep.folderItems(this, folderId(token))) {
            if (!item.startsWith(WIDGET)) {
                continue;
            }
            android.appwidget.AppWidgetProviderInfo info = widgetOf(item);
            if (info == null || info.provider == null) {
                continue;
            }
            if (found == null) {
                found = new Apps(this);
            }
            Apps.Door owner = found.ofPackage(info.provider.getPackageName());
            if (owner != null) {
                face.add(0, owner);
            }
        }
        return face;
    }

    /**
     * One icon's own face, chosen on the whole screen of choices: the icon
     * itself drawn in every outline, and then in its own colours or in the
     * accent; "as all the others" hands it back to what every icon wears.
     */
    private void chooseFace(String token, Apps.Door door) {
        android.graphics.drawable.Drawable raw = rawIcon(token, door);
        if (raw == null) {
            refuse(screens);
            return;
        }
        /* The outline is one for all icons, chosen in the settings; one icon
           is given only what makes it read: colour, inking, a drawing, a
           picture or a symbol in its place. */
        int[] mine = Style.faceOf(token);
        List<List<Chooser.Item>> groups = new ArrayList<>();
        List<String> captions = new ArrayList<>();
        if (ALL_THING.equals(token)) {
            /* The door to every app: the plain button, or an icon among icons. */
            List<Chooser.Item> doors = new ArrayList<>();
            android.graphics.drawable.Drawable plain = getDrawable(R.drawable.sym_apps).mutate();
            plain.setTint(Tone.primary());
            doors.add(new Chooser.Item(plain, Words.t(PLAIN_BUTTON), 8000));
            doors.add(new Chooser.Item(Shape.face(allRaw(), -1, Style.OWN), Words.t(AS_AN_ICON), 8001));
            groups.add(doors);
            captions.add("Door");
        }
        /* A folder's face is its apps in small: only a picture or a symbol is put in its place. */
        boolean folder = isFolder(token);
        List<Chooser.Item> colours = new ArrayList<>();
        colours.add(new Chooser.Item(Shape.face(rawIcon(token, door), -1, -1), Words.t(AS_OTHERS), 1999));
        colours.add(new Chooser.Item(Shape.face(rawIcon(token, door), -1, Style.OWN), Words.t(THEIR_OWN), 2000));
        colours.add(new Chooser.Item(Shape.face(rawIcon(token, door), -1, Style.ALL), Words.t(IN_ACCENT), 2001));
        if (!folder) {
            groups.add(colours);
            captions.add("Colour");
        }
        List<Chooser.Item> inkings = new ArrayList<>();
        for (int m = 0; m < Shape.METHODS; m++) {
            inkings.add(new Chooser.Item(Shape.face(rawIcon(token, door), -1, Style.ALL, Marks.NONE, m),
                Words.t(Shape.METHOD_NAMES[m]), 4000 + m));
        }
        if (!folder) {
            groups.add(inkings);
            captions.add("Inking");
        }
        int drawing = door != null ? Marks.of(door.name.getPackageName()) : Marks.NONE;
        if (drawing != Marks.NONE) {
            List<Chooser.Item> drawings = new ArrayList<>();
            drawings.add(new Chooser.Item(Shape.face(rawIcon(token, door), -1, mine[1]), Words.t(APPS_OWN), 3000));
            drawings.add(new Chooser.Item(Shape.face(rawIcon(token, door), -1, mine[1], drawing), Words.t(HOME_OWN), 3001));
            groups.add(drawings);
            captions.add("Drawing");
        }
        if (folder) {
            /* A folder's small icons laid out its own way, or as every folder's are. */
            /* Drawn as the settings draw them, in dots: a folder with little in it shows every layout too. */
            List<Chooser.Item> looks = new ArrayList<>();
            looks.add(new Chooser.Item(Stack.sketch(Stack.layout), Words.t(AS_FOLDERS), 8099));
            for (int l = 0; l < Stack.NAMES.length; l++) {
                looks.add(new Chooser.Item(Stack.sketch(l), Words.t(Stack.NAMES[l]), 8100 + l));
            }
            groups.add(looks);
            captions.add("Folder");
        }
        /* A picture of the owner's own, from the phone's pictures. */
        List<Chooser.Item> pictures = new ArrayList<>();
        pictures.add(new Chooser.Item(getDrawable(R.drawable.sym_photo_library), Words.t(FROM_PICTURE), 5000));
        /* The dice of marks: a thing no symbol shows, found among thrown pictograms. */
        pictures.add(new Chooser.Item(getDrawable(R.drawable.sym_casino), Words.t(FROM_DICE), 5002));
        if (mine[4] == 1) {
            pictures.add(new Chooser.Item(Style.dress(this, token, rawIcon(token, door), null), Words.t(DROP_PICTURE), 5001));
        }
        groups.add(pictures);
        captions.add("Picture");
        /* The marks put on icons before, to be taken again without throwing. */
        List<Long> kept = Keep.omens(this);
        if (!kept.isEmpty()) {
            List<Chooser.Item> marks = new ArrayList<>();
            for (int i = 0; i < kept.size(); i++) {
                marks.add(new Chooser.Item(Shape.faceMark(rawIcon(token, door), -1, new Omen(kept.get(i))), "",
                    9500 + i));
            }
            groups.add(marks);
            captions.add("My marks");
        }
        /* Symbols to put in the picture's place, found by their names. */
        List<Chooser.Item> symbols = new ArrayList<>();
        symbolNames = Folio.asset(this, "symbols.txt").trim().split("\\s+");
        if (Style.symbolOf(token).length() > 0) {
            symbols.add(new Chooser.Item(Shape.face(rawIcon(token, door), -1, mine[1]), Words.t(NO_SYMBOL), 6999));
        }
        for (int i = 0; i < symbolNames.length; i++) {
            int id = getResources().getIdentifier("sym_" + symbolNames[i], "drawable", getPackageName());
            if (id != 0) {
                symbols.add(new Chooser.Item(Shape.faceMark(rawIcon(token, door), -1, getDrawable(id)),
                    symbolNames[i].replace('_', ' '), 7000 + i));
            }
        }
        groups.add(symbols);
        captions.add("Symbols");
        choosingFace = true;
        faceToken = token;
        faceDoor = door;
        chooser.hold(omenHold);
        chooser.hint(SEARCH_SYMBOLS);
        chooser.show(captions.toArray(new String[0]), groups, true);
    }

    private String[] symbolNames = new String[0];
    private Apps.Door faceDoor;
    private static final String FROM_DICE = "From the dice";
    private static final String AS_FOLDERS = "As every folder";
    /** The marks shown on the dice's page now, by their seeds. */
    private final List<Long> omenPage = new ArrayList<>();

    /** Holding a mark: more marks like it. */
    private final Chooser.Hold omenHold = new Chooser.Hold() {
        public void held(int key) {
            long seed;
            if (key >= 9500) {
                List<Long> kept = Keep.omens(Home.this);
                if (key - 9500 >= kept.size()) {
                    return;
                }
                final long mine = kept.get(key - 9500);
                /* One of the owner's own: more like it, or let go of; where it stands already, it stays. */
                Ask.tell(root, MY_MARK, FORGET_OR_MORE, MORE_LIKE, new Runnable() {
                    public void run() {
                        chooser.close(false);
                        showOmens(Omen.like(mine, 24), MORE_LIKE);
                    }
                }, FORGET, new Runnable() {
                    public void run() {
                        Keep.forgetOmen(Home.this, mine);
                        chooser.close(false);
                        chooseFace(faceToken, faceDoor);
                    }
                });
                return;
            } else if (key >= 9000 && key - 9000 < omenPage.size()) {
                seed = omenPage.get(key - 9000);
            } else {
                return;
            }
            chooser.close(false);
            showOmens(Omen.like(seed, 24), MORE_LIKE);
        }
    };
    private static final String MORE_LIKE = "More like it";
    private static final String MY_MARK = "My mark";
    private static final String FORGET = "Forget";
    private static final String FORGET_OR_MORE = "More marks like this one, or forget it? Where it is on an icon, it stays.";
    private static final String THROWN = "Thrown";
    private static final java.util.Random dice = new java.util.Random();

    /** A fresh page of marks from the dice. */
    private List<Long> thrown() {
        List<Long> seeds = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            seeds.add(Omen.fresh(dice));
        }
        return seeds;
    }

    /**
     * The dice's page: marks, each on this icon's own plate, as it would stand;
     * a touch puts one on, holding one throws its kin, and the dice below
     * throws a new page.
     */
    private void showOmens(List<Long> seeds, String caption) {
        if (faceToken == null) {
            return;
        }
        omenPage.clear();
        omenPage.addAll(seeds);
        android.graphics.drawable.Drawable raw = rawIcon(faceToken, faceDoor);
        if (raw == null) {
            raw = new android.graphics.drawable.ColorDrawable(Tone.primaryContainer());
        }
        List<Chooser.Item> marks = new ArrayList<>();
        for (int i = 0; i < seeds.size(); i++) {
            marks.add(new Chooser.Item(Shape.faceMark(raw, -1, new Omen(seeds.get(i))), "", 9000 + i));
        }
        List<Chooser.Item> again = new ArrayList<>();
        again.add(new Chooser.Item(getDrawable(R.drawable.sym_casino), Words.t(THROW_AGAIN), 8998));
        List<List<Chooser.Item>> groups = new ArrayList<>();
        groups.add(marks);
        groups.add(again);
        choosingFace = true;
        chooser.hold(omenHold);
        chooser.hint(SEARCH_SYMBOLS);
        chooser.show(new String[] {caption, null}, groups, true);
    }

    private static final String THROW_AGAIN = "Throw again";
    private static final String FROM_PICTURE = "From a picture";
    private static final String SEARCH_SYMBOLS = "Search symbols";
    private static final String SEARCH_SHORTCUTS = "Search shortcuts";
    private static final String DROP_PICTURE = "Without the picture";
    private static final String NO_SYMBOL = "Without a symbol";
    private static final int ASK_PICTURE = 14;

    private static final String AS_OTHERS = "As all the others";
    private static final String PLAIN_BUTTON = "The plain button";
    private static final String AS_AN_ICON = "As an icon";
    private static final String APPS_OWN = "The app's own";
    private static final String HOME_OWN = "The home screen's";
    private static final String THEIR_OWN = "Its own colours";
    private static final String IN_ACCENT = "In the accent";

    /** The face chosen for the one icon is kept, and the screens set out again. */
    private void face(int key) {
        if (faceToken == null) {
            return;
        }
        int[] mine = Style.faceOf(faceToken);
        int tint = mine[1];
        int drawing = mine[2];
        int method = mine[3];
        int image = mine[4];
        String symbol = Style.symbolOf(faceToken);
        if (key == 8000) {
            Keep.saveFace(this, faceToken, -1, 0, 0, 0, "");
            Style.read(this);
            stamp = Keep.stamp(this);
            fill();
            return;
        } else if (key == 8001) {
            tint = Style.OWN;
        } else if (key >= 1999 && key < 3000) {
            tint = key - 2000;
            if (tint == Style.OWN) {
                method = Shape.AUTO;
            }
        } else if (key >= 3000 && key < 4000) {
            drawing = key - 3000;
            symbol = "";
            image = 0;
        } else if (key >= 4000 && key < 5000) {
            method = key - 4000;
            tint = Style.ALL;
            symbol = "";
            image = 0;
        } else if (key == 5000) {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            try {
                startActivityForResult(pick, ASK_PICTURE);
            } catch (RuntimeException none) {
                refuse(screens);
            }
            return;
        } else if (key == 5002 || key == 8998) {
            showOmens(thrown(), THROWN);
            return;
        } else if (key >= 8099 && key < 8110) {
            Keep.saveFolderLook(this, faceToken, key == 8099 ? -1 : key - 8100);
            Style.read(this);
            stamp = Keep.stamp(this);
            fill();
            return;
        } else if (key >= 9000 && key < 9500 && key - 9000 < omenPage.size()) {
            long seed = omenPage.get(key - 9000);
            symbol = Omen.WORD + seed;
            image = 0;
            Keep.keepOmen(this, seed);
        } else if (key >= 9500) {
            List<Long> kept = Keep.omens(this);
            if (key - 9500 < kept.size()) {
                symbol = Omen.WORD + kept.get(key - 9500);
                image = 0;
                Keep.keepOmen(this, kept.get(key - 9500));
            }
        } else if (key == 5001) {
            image = 0;
        } else if (key == 6999) {
            symbol = "";
        } else if (key >= 7000 && key - 7000 < symbolNames.length) {
            symbol = symbolNames[key - 7000];
            image = 0;
        }
        Keep.saveFace(this, faceToken, tint, drawing, method, image, symbol);
        Style.read(this);
        stamp = Keep.stamp(this);
        fill();
    }

    /**
     * A picture chosen from the phone's pictures becomes the icon: it is
     * brought down to a size an icon needs, kept with the home screen's own
     * files, and set in the outline like any flat icon.
     */
    private void takePicture(Intent data) {
        if (faceToken == null || data == null || data.getData() == null) {
            return;
        }
        try {
            java.io.InputStream in = getContentResolver().openInputStream(data.getData());
            android.graphics.Bitmap whole = android.graphics.BitmapFactory.decodeStream(in);
            if (in != null) {
                in.close();
            }
            if (whole == null) {
                refuse(screens);
                return;
            }
            int most = 288;
            float scale = Math.min(1f, most / (float) Math.max(whole.getWidth(), whole.getHeight()));
            android.graphics.Bitmap kept = android.graphics.Bitmap.createScaledBitmap(whole,
                Math.max(1, Math.round(whole.getWidth() * scale)), Math.max(1, Math.round(whole.getHeight() * scale)),
                true);
            java.io.FileOutputStream out = new java.io.FileOutputStream(Style.pictureOf(this, faceToken));
            kept.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            int[] mine = Style.faceOf(faceToken);
            Keep.saveFace(this, faceToken, mine[1], 0, Shape.AUTO, 1, "");
            Style.read(this);
            stamp = Keep.stamp(this);
            fill();
        } catch (Exception broken) {
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

    /**
     * An app, a shortcut or a link given a name of the owner's own, shown
     * wherever it stands; left empty, it is shown under its own name again.
     */
    private void renameThing(final String token, String now) {
        Ask.show(root, RENAME, now, new Ask.Answer() {
            public void answered(String text) {
                Style.name(Home.this, token, text);
                fill();
            }
        });
    }

    /** Which widget each view shown in an open folder is, by the word the folder keeps it by. */
    private final java.util.Map<View, String> folderWidgetTokens = new java.util.HashMap<>();

    /**
     * The widgets a folder holds, made and sized to be shown above its apps:
     * as tall as the places they were given, as wide as the folder's card.
     * One whose app is gone is left out.
     */
    private List<View> folderWidgets(int folder) {
        List<View> made = new ArrayList<>();
        folderWidgetTokens.clear();
        if (folder <= 0) {
            return made;
        }
        float[] place = place();
        for (String item : Keep.folderItems(this, folder)) {
            if (!item.startsWith(WIDGET)) {
                continue;
            }
            String[] part = item.substring(WIDGET.length()).split(":");
            int id;
            int down;
            try {
                id = Integer.parseInt(part[0]);
                down = part.length > 2 ? Math.max(1, Integer.parseInt(part[2])) : 2;
            } catch (NumberFormatException broken) {
                continue;
            }
            android.appwidget.AppWidgetProviderInfo info = widgets.getAppWidgetInfo(id);
            if (info == null) {
                continue;
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
            if (view instanceof Piece) {
                ((Piece) view).frame(Rim.NONE, 0, dp(22), false);
            }
            android.graphics.Rect own = android.appwidget.AppWidgetHostView.getDefaultPaddingForWidget(
                this, info.provider, null);
            view.setPadding(own.left, own.top, own.right, own.bottom);
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(place[1] * down)));
            folderWidgetTokens.put(view, item);
            made.add(view);
        }
        return made;
    }

    /** A widget held in an open folder: its menu, and the finger moving on carries it out of the folder. */
    private void holdFolderWidget(final View widget) {
        final String token = folderWidgetTokens.get(widget);
        if (token == null || trayFolder <= 0) {
            return;
        }
        final int[] whence = {-2, trayFolder};
        offerThing(widget, token, whence, null);
        root.arm(new Runnable() {
            public void run() {
                menu.hide(false);
                android.graphics.Bitmap picture = android.graphics.Bitmap.createBitmap(
                    Math.max(1, widget.getWidth()), Math.max(1, widget.getHeight()),
                    android.graphics.Bitmap.Config.ARGB_8888);
                widget.draw(new android.graphics.Canvas(picture));
                android.graphics.drawable.Drawable face = new android.graphics.drawable.BitmapDrawable(
                    getResources(), picture);
                int[] icon = {0, 0, widget.getWidth(), widget.getHeight()};
                String[] part = token.substring(WIDGET.length()).split(":");
                int across = 2;
                int down = 2;
                try {
                    across = Math.max(1, Integer.parseInt(part[1]));
                    down = Math.max(1, Integer.parseInt(part[2]));
                } catch (RuntimeException broken) {
                    // Carried at a size of two by two.
                }
                carry(widget, token, face, icon, across, down, whence, root.fingerX() + rootLeft(),
                    root.fingerY() + rootTop());
                if (widget.getParent() instanceof ViewGroup) {
                    ((ViewGroup) widget.getParent()).removeView(widget);
                }
                tray.close(false);
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
            ? (windowShape()[0] - 2 * side()) / (float) columns : page.cellWidth();
        float tall = page == null || page.cellHeight() <= 0 ? wide * 1.2f : page.cellHeight();
        /* Its least size is in pixels of this phone; counted in places of
           this grid, not of any other. */
        int across = (int) Math.ceil(info.minWidth / wide - 0.05f);
        int down = (int) Math.ceil(info.minHeight / tall - 0.05f);
        if (Build.VERSION.SDK_INT >= 31 && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
            /* The places it asks for are places of a common grid of four or
               five across: taken as they are across, but down they are read
               as the height that many common places give, seventy dp each
               less thirty, and counted in this grid's rows, which on a grid
               of many rows are far lower. */
            across = Math.max(across, info.targetCellWidth * fine);
            down = Math.max(down, (int) Math.ceil(dp(70f * info.targetCellHeight - 30f) / tall - 0.05f));
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
        if (asked == ASK_PICTURE) {
            if (answer == RESULT_OK) {
                takePicture(data);
            }
            return;
        }
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
            pendingPileAt = null;
            return;
        }
        if (pendingPileAt != null) {
            /* One more for a pile: laid behind the others, and the pile's card shown again. */
            int[] at = pendingPileAt;
            pendingPileAt = null;
            List<Integer> now = pileWidgets(at);
            if (now != null) {
                now.add(id);
                pileKeep(at, now, new ArrayList<Integer>(), pileTurnsAt(at));
                openPile(at);
                return;
            }
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
            for (int r = 0; r + span[1] <= rows; r += fine) {
                for (int c = 0; c + span[0] <= columns; c += fine) {
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

    /**
     * The launcher's own clock, from the widget shelf: if it stands, its
     * screen is shown; if not, it is set on the first free block, whole
     * width and as many rows as its face needs, from the screen the shelf
     * was opened on — kept there, the one clock there is.
     */
    private void takeClock() {
        if (clockView != null) {
            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).indexOfChild((View) clockView) >= 0) {
                    screens.show(i, true);
                    return;
                }
            }
            return;
        }
        int[] least = clockLeast(place());
        List<Integer> order = new ArrayList<>();
        order.add(Math.max(0, Math.min(pendingPage, pages.size() - 1)));
        for (int i = 0; i < pages.size(); i++) {
            if (!order.contains(i)) {
                order.add(i);
            }
        }
        for (int screen : order) {
            Grid page = pages.get(screen);
            for (int r = 0; r + least[1] <= rows; r += fine) {
                if (page.free(0, r, columns, least[1])) {
                    if (!Keep.laid(this)) {
                        Keep.lay(this, standing);
                    }
                    Keep.saveFlag(this, Keep.CLOCK, true);
                    Keep.keepOnly(this, Keep.CLOCK_THING, Keep.CLOCK_THING, screen, 0, r);
                    fill();
                    screens.show(screen, true);
                    screens.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                        ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
                    return;
                }
            }
        }
        refuse(screens);
    }

    /** A widget's own number from its word on the screen, or nought. */
    private static int widgetId(String token) {
        if (token == null || !token.startsWith(WIDGET)) {
            return 0;
        }
        String[] part = token.substring(WIDGET.length()).split(":");
        try {
            return Integer.parseInt(part[0]);
        } catch (NumberFormatException broken) {
            return 0;
        }
    }

    /** The rings being set by hand now, and the veil over the rest of the screen meanwhile. */
    private Rings arranged;
    private View veil;

    /**
     * The rings taken into the owner's hands: the screen dims round the
     * clock and leaves every touch on it to the rings, a word under it
     * says what the fingers do; a touch anywhere else gives them back.
     */
    private void arrange(final Rings rings) {
        giveBack();
        arranged = rings;
        rings.arrange(true);
        root.still(true);
        final Rect clock = new Rect();
        int[] at = new int[2];
        int[] floor = new int[2];
        rings.getLocationOnScreen(at);
        root.getLocationOnScreen(floor);
        clock.set(at[0] - floor[0], at[1] - floor[1], at[0] - floor[0] + rings.getWidth(),
            at[1] - floor[1] + rings.getHeight());
        final android.graphics.Paint dim = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        final android.graphics.Paint said = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        said.setTextAlign(android.graphics.Paint.Align.CENTER);
        said.setTextSize(15f * getResources().getDisplayMetrics().scaledDensity);
        said.setColor(0xE6FFFFFF);
        veil = new View(this) {
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                canvas.save();
                if (Build.VERSION.SDK_INT >= 26) {
                    canvas.clipOutRect(clock);
                }
                dim.setColor(0x99000000);
                canvas.drawRect(0, 0, getWidth(), getHeight(), dim);
                canvas.restore();
                float y = clock.bottom + dp(28);
                canvas.drawText(Words.t("Drag a ring \u00B7 pinch to size it \u00B7 double-tap to put it back"),
                    getWidth() / 2f, y, said);
                canvas.drawText(Words.t("Tap outside the clock when done"), getWidth() / 2f, y + dp(22), said);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    && clock.contains((int) event.getX(), (int) event.getY())) {
                    return false;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    giveBack();
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
                return true;
            }
        };
        veil.setAlpha(0f);
        root.addView(veil, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        veil.animate().alpha(1f).setDuration(Pace.ARRIVE).start();
    }

    /** The rings given back, if they were in hand: where they stand is kept. */
    private void giveBack() {
        if (arranged == null) {
            return;
        }
        Rings rings = arranged;
        arranged = null;
        root.still(false);
        if (veil != null) {
            final View gone = veil;
            veil = null;
            gone.animate().alpha(0f).setDuration(Pace.ARRIVE / 2).withEndAction(new Runnable() {
                public void run() {
                    root.removeView(gone);
                }
            }).start();
        }
        rings.arrange(false);
        stamp = Keep.stamp(this);
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
        if (info == null || !page.fits(spot.x, spot.y, across, down)) {
            return;
        }
        /* A block kept on another grid may be lower than the widget can
           live in on this one: it grows to its least size here, where the
           places are free. What is kept stays as it was. */
        float[] cell = place();
        int leastWide = info.minResizeWidth > 0 ? info.minResizeWidth : info.minWidth;
        int leastTall = info.minResizeHeight > 0 ? info.minResizeHeight : info.minHeight;
        int needA = Math.min(columns - spot.x, (int) Math.ceil(leastWide / cell[0] - 0.05f));
        int needD = Math.min(rows - spot.y, (int) Math.ceil(leastTall / cell[1] - 0.05f));
        if (needA > across && page.free(spot.x, spot.y, needA, down)) {
            across = needA;
        }
        if (needD > down && page.free(spot.x, spot.y, across, needD)) {
            down = needD;
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
        /* A widget's host leaves a margin of its own round it; with no
           margins on the grid, it leaves none either. */
        /* The frame, if widgets wear one and this one was not let off it:
           the widget drawn within it, the frame's width added to its margin. */
        int kind = Keep.number(this, Keep.WIDGET_FRAME, Rim.NONE);
        boolean framed = kind != Rim.NONE && !Keep.flag(this, Keep.FRAME_OFF + id, false);
        int band = framed ? dp(Keep.number(this, Keep.WIDGET_FRAME_WIDTH, 6)) : 0;
        if (view instanceof Piece) {
            ((Piece) view).frame(framed ? kind : Rim.NONE, band, dp(Keep.number(this, Keep.WIDGET_FRAME_ROUND, 24)),
                Keep.flag(this, Keep.WIDGET_GLAZE, false));
        }
        if (Keep.edgeless(this)) {
            view.setPadding(band, band, band, band);
        } else {
            android.graphics.Rect own = android.appwidget.AppWidgetHostView.getDefaultPaddingForWidget(
                this, info.provider, null);
            view.setPadding(own.left + band, own.top + band, own.right + band, own.bottom + band);
        }
        page.put(view, spot.x, spot.y, across, down);
        page.edge(view, Keep.edges(this));
        cells.add(view);
        stand(page, view, spot.token);
    }

    // ------------------------------------------------------------- runs

    /** The screen a run opens, as the phone knows it now; or none if its app or its screen is gone. */
    private android.content.pm.ActivityInfo runInfo(String token) {
        android.content.ComponentName name = android.content.ComponentName.unflattenFromString(
            token.substring(Keep.RUN_THING.length()));
        if (name == null) {
            return null;
        }
        try {
            return getPackageManager().getActivityInfo(name, 0);
        } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException gone) {
            return null;
        }
    }

    /**
     * A run on a screen: the screen's own icon and name, or the name given
     * by hand. A run whose app or screen is gone stays in its place, faint,
     * under the name it was kept with, until the app comes back.
     */
    private void run(Grid page, final Keep.Spot spot) {
        if (!page.free(spot.x, spot.y)) {
            return;
        }
        android.content.pm.ActivityInfo info = runInfo(spot.token);
        android.graphics.drawable.Drawable raw = info == null
            ? new android.graphics.drawable.ColorDrawable(Tone.container()) : info.loadIcon(getPackageManager());
        String given = Style.nameOf(spot.token);
        CharSequence name = given != null ? given
            : info != null ? info.loadLabel(getPackageManager()) : Keep.runName(this, spot.token);
        final Cell cell = new Cell(this, Style.dress(this, spot.token, raw, info == null ? null : info.packageName),
            name, iconSize, Style.namesOnScreens);
        if (info == null) {
            cell.setAlpha(0.45f);
        }
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                runNow(cell, spot.token);
            }
        });
        page.put(cell, spot.x, spot.y);
        cells.add(cell);
        stand(page, cell, spot.token);
    }

    /** A run opened: its screen, by name, grown out of its icon; if the phone will not, a short word why. */
    private void runNow(View from, String token) {
        android.content.ComponentName name = android.content.ComponentName.unflattenFromString(
            token.substring(Keep.RUN_THING.length()));
        if (name == null) {
            return;
        }
        Intent open = new Intent().setComponent(name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            android.app.ActivityOptions grow = android.app.ActivityOptions.makeClipRevealAnimation(from, 0, 0,
                from.getWidth(), from.getHeight());
            startActivity(open, grow.toBundle());
        } catch (android.content.ActivityNotFoundException gone) {
            refuse(from);
            said("This way in is no longer in its app");
        } catch (SecurityException closed) {
            refuse(from);
            said("Its app does not let it be opened from here");
        } catch (RuntimeException other) {
            refuse(from);
            said("It could not be opened");
        }
    }

    // ------------------------------------------------------------- piles

    /** The number of a pile, from the word it is kept by; nought for any other. */
    private static int pileId(String token) {
        if (token == null || !token.startsWith(Keep.PILE_THING)) {
            return 0;
        }
        try {
            return Integer.parseInt(token.split(":")[1]);
        } catch (RuntimeException broken) {
            return 0;
        }
    }

    /** The word of what is kept at a place, screen, column and row; or none. */
    private String keptAt(int[] at) {
        if (at == null) {
            return null;
        }
        for (Keep.Spot spot : Keep.placed(this)) {
            if (spot.screen == at[0] && spot.x == at[1] && spot.y == at[2]) {
                return spot.token;
            }
        }
        return null;
    }

    /** The widgets at a place, in order: a pile's, or a single widget's own; none if neither stands there. */
    private List<Integer> pileWidgets(int[] at) {
        String token = keptAt(at);
        List<Integer> ids = new ArrayList<>();
        if (token == null) {
            return null;
        }
        if (token.startsWith(Keep.PILE_THING)) {
            ids.addAll(Keep.pileItems(this, pileId(token)));
        } else if (token.startsWith(WIDGET)) {
            try {
                ids.add(Integer.parseInt(token.substring(WIDGET.length()).split(":")[0]));
            } catch (NumberFormatException broken) {
                return null;
            }
        } else {
            return null;
        }
        return ids;
    }

    private boolean pileTurnsAt(int[] at) {
        String token = keptAt(at);
        return token != null && token.startsWith(Keep.PILE_THING) && Keep.pileTurns(this, pileId(token));
    }

    /** The pile's own card for the widget or pile at a place. */
    private void openPile(int[] at) {
        List<Integer> ids = pileWidgets(at);
        if (ids == null) {
            return;
        }
        pileAt = at;
        List<PileCard.Entry> entries = new ArrayList<>();
        int dpi = getResources().getDisplayMetrics().densityDpi;
        for (int id : ids) {
            android.appwidget.AppWidgetProviderInfo info = widgets.getAppWidgetInfo(id);
            if (info == null) {
                continue;
            }
            android.graphics.drawable.Drawable picture = null;
            try {
                picture = info.loadPreviewImage(this, dpi);
                if (picture == null) {
                    picture = info.loadIcon(this, dpi);
                }
            } catch (RuntimeException unseen) {
                picture = null;
            }
            entries.add(new PileCard.Entry(id, info.loadLabel(getPackageManager()), picture));
        }
        pileCard.show(entries, pileTurnsAt(at));
    }

    /**
     * What the pile's card left, kept at its place: widgets taken out let
     * go of; none left, the place is freed; one left, it stands as a plain
     * widget again; more, they are a pile, the first shown first. Its size
     * on the screen stays as it was.
     */
    private void pileKeep(int[] at, List<Integer> kept, List<Integer> out, boolean turns) {
        String token = keptAt(at);
        if (token == null) {
            return;
        }
        for (int id : out) {
            drop(id);
        }
        String[] part = token.split(":");
        String size = part.length >= 4 ? ":" + part[2] + ":" + part[3] : "";
        int pile = pileId(token);
        if (!Keep.laid(this)) {
            Keep.lay(this, standing);
        }
        if (kept.isEmpty()) {
            Keep.remove(this, at[0], at[1], at[2]);
            if (pile > 0) {
                Keep.forgetPile(this, pile);
            }
        } else if (kept.size() == 1) {
            Keep.reshape(this, at[0], at[1], at[2], WIDGET + kept.get(0) + size, at[1], at[2]);
            if (pile > 0) {
                Keep.forgetPile(this, pile);
            }
        } else {
            if (pile > 0) {
                Keep.savePile(this, pile, kept);
            } else {
                pile = Keep.newPile(this, kept);
            }
            Keep.savePileTurns(this, pile, turns);
            Keep.savePileShown(this, pile, 0);
            Keep.reshape(this, at[0], at[1], at[2], Keep.PILE_THING + pile + size, at[1], at[2]);
        }
        fill();
    }

    /** A pile on a screen: its widgets made, each framed as a widget is, one shown. */
    private void pile(Grid page, final Keep.Spot spot) {
        String[] part = spot.token.split(":");
        if (part.length < 4) {
            return;
        }
        final int pile;
        int across;
        int down;
        try {
            pile = Integer.parseInt(part[1]);
            across = Math.min(columns, Integer.parseInt(part[2]));
            down = Math.min(rows, Integer.parseInt(part[3]));
        } catch (NumberFormatException broken) {
            return;
        }
        if (!page.fits(spot.x, spot.y, across, down)) {
            return;
        }
        Pile made = new Pile(this, new Pile.Hand() {
            public void turned(int shown) {
                Keep.savePileShown(Home.this, pile, shown);
            }
        });
        int kind = Keep.number(this, Keep.WIDGET_FRAME, Rim.NONE);
        int band = 0;
        for (int id : Keep.pileItems(this, pile)) {
            android.appwidget.AppWidgetProviderInfo info = widgets.getAppWidgetInfo(id);
            if (info == null) {
                continue;
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
            boolean framed = kind != Rim.NONE && !Keep.flag(this, Keep.FRAME_OFF + id, false);
            band = framed ? dp(Keep.number(this, Keep.WIDGET_FRAME_WIDTH, 6)) : 0;
            if (view instanceof Piece) {
                ((Piece) view).frame(framed ? kind : Rim.NONE, band, dp(Keep.number(this, Keep.WIDGET_FRAME_ROUND, 24)),
                    Keep.flag(this, Keep.WIDGET_GLAZE, false));
            }
            if (Keep.edgeless(this)) {
                view.setPadding(band, band, band, band);
            } else {
                android.graphics.Rect own = android.appwidget.AppWidgetHostView.getDefaultPaddingForWidget(
                    this, info.provider, null);
                view.setPadding(own.left + band, own.top + band, own.right + band, own.bottom + band);
            }
            /* A widget in a pile is held as the pile: its menu is the pile's. */
            final Pile whole = made;
            view.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    return whole.performLongClick();
                }
            });
            made.add(view);
        }
        if (made.count() == 0) {
            return;
        }
        made.set(Keep.pileShown(this, pile), Keep.pileTurns(this, pile), band + dp(2));
        page.put(made, spot.x, spot.y, across, down);
        page.edge(made, Keep.edges(this));
        cells.add(made);
        stand(page, made, spot.token);
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
            for (int r = 0; r < rows; r += fine) {
                for (int c = 0; c < columns; c += fine) {
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
        List<Apps.Door> most = new ArrayList<>();
        for (String name : Keep.frequent(this)) {
            Apps.Door door = found.door(name);
            if (door != null && most.size() < 12) {
                most.add(door);
            }
        }
        fresh.show(most, recent, lately, notes);
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
                int deed = Keep.number(Home.this, Keep.ON_UP, Keep.DO_LIST);
                if (deed == Keep.DO_LIST) {
                    pull = PULL_OPEN;
                    drawer.begin();
                } else if (deed != Keep.DO_NOTHING) {
                    pull = PULL_DEED;
                    pullKey = Keep.ON_UP;
                    pullDeed = Keep.DO_LIST;
                }
            } else {
                int deed = Keep.number(Home.this, Keep.ON_DOWN, Keep.DO_NOTICES);
                if (deed == Keep.DO_NOTICES || deed == Keep.DO_QUICK) {
                    pull = PULL_BAR;
                    barAsked = false;
                } else if (deed != Keep.DO_NOTHING) {
                    pull = PULL_DEED;
                    pullKey = Keep.ON_DOWN;
                    pullDeed = Keep.DO_NOTICES;
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
            } else if (pull == PULL_DEED) {
                boolean up = Keep.ON_UP.equals(pullKey);
                boolean far = up ? (by < -dp(64) || speed < -throwing) : (by > dp(64) || speed > throwing);
                if (far) {
                    perform(pullKey, pullDeed);
                }
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
