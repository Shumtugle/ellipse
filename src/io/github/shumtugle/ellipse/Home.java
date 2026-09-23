package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The home screens.
 *
 * Screens side by side, each a grid; on them, things set by hand: an
 * application, a folder, a widget's place, a shortcut, the door to the
 * drawer. Over them, the drawer, drawn up from below.
 *
 * Every thing is moved the same way. A long press picks it up, and an
 * offer of what can be done with it stands beside the finger: let go at
 * once, and the offer stays to be pressed; move, and the offer goes and
 * the thing follows the finger, the grid showing where it can land. Held
 * at the edge of a screen, it turns to the next. Let go over an
 * application, and the two become a folder; over a folder, it joins it.
 * An application from the drawer or out of a folder is carried the same
 * way.
 *
 * A long press on bare ground offers what belongs to a screen: the door,
 * a screen more, this screen gone if it is empty, this screen made the
 * one Home returns to, and the settings.
 *
 * Widgets live in their places on the screens. A widget brought in with a
 * layout from elsewhere waits as a quiet outline until pressed; then the
 * system is asked for leave to show it, once, and the widget's own
 * settings open if it has any. A long press on bare ground offers every
 * widget the phone has, on a sheet of pictures, and the one chosen is set
 * down where the finger was, as wide as the screen if it can stretch that
 * far and wants to be wide.
 *
 * The home screen never finishes. Back closes whatever lies open; on
 * bare screens it lowers what is fresh, the applications last opened from
 * here and those put on the phone or changed lately, which a push upwards
 * sends away again. Home closes whatever lies open too, and on bare
 * screens returns to the main one.
 */
public final class Home extends Activity {

    /** How much of a cell a tile takes, across and down. */
    private static final float ACROSS = 0.94f;
    private static final float DOWN = 0.92f;
    /** How long a carried thing waits at the edge before the screen turns. */
    private static final long EDGE_WAIT = 550L;

    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private LauncherApps doors;
    private Icons icons;
    private Tile.Look look;
    private Stage stage;
    private Board board;
    private Drawer drawer;
    private Tray tray;
    private Fresh fresh;
    private Picker picker;
    private Host host;
    private AppWidgetManager widgets;
    /** A widget between asking the system and being placed. */
    private Layout.Item waiting;
    private int waitingId = -1;
    private boolean waitingNew;
    private float cellW;
    private float cellH;
    private static final int BIND = 11;
    private static final int CONFIGURE = 12;
    /** Choosing which application shall make a shortcut, and then making it. */
    private static final int WHICH_LINK = 13;
    private static final int MAKE_LINK = 14;

    /** Where a shortcut being made is to stand. */
    private int linkScreen;
    private int linkX;
    private int linkY;
    private Layout layout;
    private List<App> apps = new ArrayList<App>();
    private final Map<String, App> known = new HashMap<String, App>();
    private final Map<String, App> byKey = new HashMap<String, App>();
    private boolean seen;
    private boolean edge;
    /** Whether the dock stands along the foot of the screens, and the dock itself while it does. */
    private boolean docked;
    private Sheet dock;
    private int dockHigh;
    /** The frame around a widget or the clock while its card is open. */
    private Grip grip;
    /** Whether the card about a fall has been offered in this life of the screen. */
    private boolean told;
    /** Air between the dock's shelf and the bar at the foot. */
    private static final int DOCK_GAP = Round.dp(8f);
    /** How much of the glass the keyboard takes, while it is up. */
    private int keyboard;
    /** How far the drawer was drawn last time it moved. */
    private float drawn;
    private long built;
    private int barTop;
    private int barBottom;
    private int tile;
    private int turning;

    private final LauncherApps.Callback watch = new LauncherApps.Callback() {
        @Override
        public void onPackageRemoved(String pkg, UserHandle user) {
            icons.forget(pkg);
            read();
        }

        @Override
        public void onPackageAdded(String pkg, UserHandle user) {
            read();
        }

        @Override
        public void onPackageChanged(String pkg, UserHandle user) {
            icons.forget(pkg);
            read();
        }

        @Override
        public void onPackagesAvailable(String[] pkgs, UserHandle user, boolean replacing) {
            read();
        }

        @Override
        public void onPackagesUnavailable(String[] pkgs, UserHandle user, boolean replacing) {
            read();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Fault.watch(this);
        Keep.settle(this);
        Keep.catchUp(this);
        Cast.flat = Keep.flat(this);
        Round.measure(this);
        Tone.read(this);
        Words.load(this);
        Tile.materials(getResources());
        glass();
        // From the version on which the keyboard's room is told like any bar's,
        // the drawer makes room itself; before it, the window is moved.
        getWindow().setSoftInputMode(Build.VERSION.SDK_INT >= 30
            ? android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            : android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        built = Keep.stamp(this);
        look = Keep.tile(this);
        edge = Keep.edge(this);
        docked = Keep.dock(this);
        layout = Layout.load(this);

        doors = getSystemService(LauncherApps.class);
        icons = new Icons(this, look);
        widgets = AppWidgetManager.getInstance(this);
        host = new Host(this);
        sweep();

        stage = new Stage(this);
        board = new Board(this);
        board.endless(Keep.endless(this));
        board.hold(new Board.Hand() {
            public void ground(int screen, int cx, int cy, float x, float y) {
                offerGround(screen, cx, cy);
            }
        });
        stage.addView(board, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        drawer = new Drawer(this, icons, Keep.across(this), new Drawer.Hand() {
            public void open(App app, View from) {
                launch(app, from);
            }

            public void lift(App app, View from) {
                liftFromDrawer(app, from);
            }
        });
        stage.addView(drawer, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        stage.hold(drawer, new Stage.Hand() {
            public void travel(float shown) {
                // The screens step back as the drawer comes up: smaller,
                // darker and, where the system can, out of focus.
                float back = 1f - 0.07f * shown;
                board.setScaleX(back);
                board.setScaleY(back);
                board.setAlpha(1f - 0.55f * shown);
                if (Build.VERSION.SDK_INT >= 31) {
                    float blur = Round.px(22f) * shown;
                    board.setRenderEffect(blur < 0.5f ? null : android.graphics.RenderEffect
                        .createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP));
                }
                echo(back, 1f - 0.55f * shown, Round.px(22f) * shown);
                drawer.corners(1f - shown);
                if (shown > 0f && drawn == 0f) {
                    drawer.cascade();
                }
                drawn = shown;
                if (shown == 0f) {
                    drawer.forget();
                }
            }
        });
        stage.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top;
                int bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    // Only the bars that are there count: one put away gives its room to the screen.
                    top = insets.getInsets(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.displayCutout()).top;
                    bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                } else {
                    top = insets.getSystemWindowInsetTop();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                int keys = Build.VERSION.SDK_INT >= 30
                    ? insets.getInsets(WindowInsets.Type.ime()).bottom : 0;
                if (top != barTop || bottom != barBottom) {
                    barTop = top;
                    barBottom = bottom;
                    drawer.pad(barTop, Math.max(barBottom, keys));
                    build();
                } else if (keys != keyboard) {
                    drawer.pad(barTop, Math.max(barBottom, keys));
                }
                keyboard = keys;
                return insets;
            }
        });
        stage.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int ol, int ot, int or, int ob) {
                if (r - l != or - ol || b - t != ob - ot) {
                    build();
                }
            }
        });
        if (Keep.takeDoor(this)) {
            placeDoor();
        }
        setContentView(stage);
        // Built anew, after the settings or a turn of the phone, the home
        // screen opens where it was left; started for the first time, on
        // the main screen.
        board.turnTo(state != null ? state.getInt("page", layout.home) : layout.home, false);

        read();
        doors.registerCallback(watch, new Handler(Looper.getMainLooper()));
    }

    /**
     * The window as clear glass: the wallpaper behind everything, the
     * system's bars drawn over the screen instead of beside it.
     */
    @SuppressWarnings("deprecation")
    private void glass() {
        Window window = getWindow();
        int mode = Keep.immersion(this);
        boolean status = mode == Keep.NO_STATUS || mode == Keep.FULL;
        boolean navigation = mode == Keep.NO_NAVIGATION || mode == Keep.FULL;
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT < 30) {
            // Before the bars could be put away one by one, a screen that
            // keeps them measures by them; one that puts them away does not.
            if (mode == Keep.BARS) {
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            }
            if (status) {
                flags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
            if (navigation) {
                flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            if (status || navigation) {
                flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        } else if (mode == Keep.BARS) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        }
        window.getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController bars = window.getInsetsController();
            if (bars != null) {
                bars.setSystemBarsBehavior(
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
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
            }
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    /** Reads the list of applications off the main thread, then lays everything out again. */
    private void read() {
        reader.execute(new Runnable() {
            public void run() {
                final List<App> list = App.all(Home.this);
                runOnUiThread(new Runnable() {
                    public void run() {
                        apps = list;
                        App.sort(list, Keep.order(Home.this));
                        known.clear();
                        byKey.clear();
                        for (App app : list) {
                            byKey.put(app.key, app);
                        }
                        UserHandle me = Process.myUserHandle();
                        for (App app : list) {
                            if (me.equals(app.user())) {
                                known.put(app.component().flattenToShortString(), app);
                            }
                        }
                        drawer.show(list);
                        heal();
                        pinAll();
                        build();
                    }
                });
            }
        });
    }

    // ------------------------------------------------------------ building

    /**
     * Lays every screen out again from the layout. Tiles come from the
     * kept pictures, so this is cheap enough to do after every change.
     */
    private void build() {
        int width = stage.getWidth();
        int height = stage.getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        float cellWidth = (width - 2f * Round.dp(4f)) / layout.columns;
        float room = height - barTop - barBottom;
        dockHigh = 0;
        if (docked) {
            // As tall as a tile and the air around it; never taller than a row of the grid.
            float first = Math.min(cellWidth * ACROSS, room / layout.rows * DOWN * look.ratio);
            dockHigh = Math.round(Math.min(room / (layout.rows + 1f),
                Tile.height(Math.round(first), look) + Round.dp(28f)));
        }
        float cellHeight = (room - dockHigh) / (float) layout.rows;
        cellW = cellWidth;
        cellH = cellHeight;
        tile = Math.round(Math.min(cellWidth * ACROSS, cellHeight * DOWN * look.ratio));

        int page = board.page();
        board.removeAllViews();
        for (int s = 0; s < layout.screens.size(); s++) {
            Sheet sheet = new Sheet(this, layout.columns, layout.rows);
            sheet.pad(barTop, barBottom + dockHigh + (docked ? DOCK_GAP : 0));
            sheet.edge(edge);
            for (Layout.Item item : layout.screens.get(s).items) {
                View view = make(item);
                if (view != null) {
                    Sheet.Spot spot = new Sheet.Spot(item.x, item.y, item.w, item.h);
                    spot.bleed = Layout.WIDGET.equals(item.kind);
                    sheet.addView(view, spot);
                }
            }
            board.addView(sheet);
        }
        board.turnTo(Math.min(page, layout.screens.size() - 1), false);
        shelve();
        if (!told) {
            told = true;
            fallen();
        }
    }

    /**
     * After a fall: a card offering to hand the report on. What was written
     * is kept until it is sent or let go, so a report is not lost when no
     * folder can be written to.
     */
    private void fallen() {
        final String report = Fault.last(this);
        if (report == null || stage.getWidth() == 0) {
            return;
        }
        Offer offer = new Offer(this, stage).title(Words.s("fault"));
        offer.link(Sketch.INFO, Words.s("fault_send"), new Runnable() {
            public void run() {
                Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                    .putExtra(Intent.EXTRA_TEXT, report);
                try {
                    startActivity(Intent.createChooser(send, null));
                    Fault.forget(Home.this);
                } catch (ActivityNotFoundException none) {
                    stage.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                }
            }
        });
        offer.link(Sketch.CLOSE, Words.s("fault_drop"), new Runnable() {
            public void run() {
                Fault.forget(Home.this);
            }
        });
        stage.offered(offer);
        int cx = stage.getWidth() / 2;
        int cy = stage.getHeight() / 3;
        offer.show(new Rect(cx - Round.dp(24f), cy, cx + Round.dp(24f), cy + Round.dp(24f)));
    }

    /**
     * The dock: one row of the grid's own columns, standing under them
     * above the bar at the foot, the same whichever screen is in view. It
     * lies over the screens and under the drawer, and steps back with the
     * screens when anything opens over them.
     */
    private void shelve() {
        if (dock != null) {
            stage.removeView(dock);
            dock = null;
        }
        if (!docked) {
            return;
        }
        dock = new Sheet(this, layout.columns, 1);
        dock.pad(0, 0);
        // A shelf apart from the ground: glass in a look of material, a pale
        // tone of the surface with no mask, so the dock reads as its own.
        float round = Math.min(Round.dp(32f), dockHigh / 2f);
        android.graphics.drawable.Drawable shelf = look.window == Tile.Look.RAW
            ? new Cast.Slab((Tone.of(Tone.SURFACE_CONTAINER) & 0x00FFFFFF) | 0xD9000000, round / Round.dp(1f))
            : new Cast.Pane(round / Round.dp(1f));
        int inset = Round.dp(10f);
        dock.setBackground(new android.graphics.drawable.InsetDrawable(shelf, inset, Round.dp(3f), inset,
            Round.dp(3f)));
        for (Layout.Item item : layout.dock.items) {
            View view = make(item);
            if (view != null) {
                dock.addView(view, new Sheet.Spot(item.x, 0, 1, 1));
            }
        }
        FrameLayout.LayoutParams foot = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dockHigh, android.view.Gravity.BOTTOM);
        foot.bottomMargin = barBottom + DOCK_GAP;
        stage.addView(dock, 1, foot);
        echo(board.getScaleX(), board.getAlpha(), 0f);
    }

    /** The dock steps back as the screens do: as small, as dim, and as far out of focus. */
    private void echo(float back, float alpha, float blur) {
        if (dock == null) {
            return;
        }
        dock.setScaleX(back);
        dock.setScaleY(back);
        dock.setAlpha(alpha);
        // Drawn in towards the middle of the screens, not of itself, so it stays under them.
        float apart = stage.getHeight() - barBottom - DOCK_GAP - dockHigh / 2f - stage.getHeight() / 2f;
        dock.setTranslationY(apart * (back - 1f));
        if (Build.VERSION.SDK_INT >= 31) {
            dock.setRenderEffect(blur < 0.5f ? null : android.graphics.RenderEffect
                .createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP));
        }
    }

    private View make(final Layout.Item item) {
        final View view;
        if (Layout.FOLDER.equals(item.kind)) {
            view = Things.folder(this, icons, holding(item), tile, item.label);
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openFolder(item);
                }
            });
        } else if (Layout.WIDGET.equals(item.kind)) {
            view = widget(item);
        } else if (Layout.DOOR.equals(item.kind)) {
            int face = Keep.door(this);
            view = Things.tile(this, icons, null, "door:" + face, tile,
                Door.face(face, Cast.metal(look.rim), look.window == Tile.Look.RAW));
            view.setContentDescription(Words.s("drawer"));
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    stage.show(true);
                }
            });
        } else if (Layout.ACTIVITY.equals(item.kind)) {
            view = screenTile(item);
        } else if (Layout.LINK.equals(item.kind)) {
            view = linkTile(item);
        } else if (Layout.SHORTCUT.equals(item.kind)) {
            view = shortcutTile(item);
            view.setContentDescription(item.label);
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    shortcut(item, v);
                }
            });
        } else {
            final App app = known.get(item.component);
            view = Things.tile(this, icons, app, "missing:" + item.component, tile, null);
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (app != null) {
                        launch(app, v);
                    }
                }
            });
        }
        view.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                liftFromHome(item, v);
                return true;
            }
        });
        if (!Layout.WIDGET.equals(item.kind)) {
            view.setStateListAnimator(Give.tile());
        }
        return view;
    }

    /**
     * Mends front doors that moved. An application may rename the activity
     * it opens with in an update, and a layout written before that names a
     * door that is no longer there; if the package still has a door, the
     * layout is set to it and written down, once.
     */
    private void heal() {
        boolean mended = false;
        for (Layout.Screen screen : layout.every()) {
            for (Layout.Item item : screen.items) {
                if (Layout.APP.equals(item.kind)) {
                    String now = mend(item.component);
                    if (now != null) {
                        item.component = now;
                        mended = true;
                    }
                } else if (Layout.FOLDER.equals(item.kind)) {
                    for (int i = 0; i < item.apps.size(); i++) {
                        String now = mend(item.apps.get(i));
                        if (now != null) {
                            item.apps.set(i, now);
                            mended = true;
                        }
                    }
                }
            }
        }
        if (mended) {
            layout.save(this);
        }
    }

    /** The door a package opens with now, if the one written down is gone; otherwise nothing. */
    private String mend(String component) {
        if (component == null || known.containsKey(component)) {
            return null;
        }
        int slash = component.indexOf('/');
        App owner = ownerOf(slash > 0 ? component.substring(0, slash) : component);
        return owner == null ? null : owner.component().flattenToShortString();
    }

    /** The first front door of a package, for a shortcut whose own way in is closed. */
    private App ownerOf(String pkg) {
        for (App app : apps) {
            if (app.pkg().equals(pkg)) {
                return app;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ carrying

    /** What is being carried, and from where. */
    private static final class Load {
        /** A thing already on a screen, or none for a new one. */
        Layout.Item item;
        /** The application carried, for anything that can join a folder. */
        String component;
        /** The folder it is being taken out of, or none. */
        Layout.Item folder;
        View origin;
        /** A shortcut lifted out of an application's card: its package, its name inside it, its words. */
        String shortcutPkg;
        String shortcutId;
        String shortcutLabel;
        /** A screen inside an application, lifted out of its list: its full name, and what it is called. */
        String activity;
        String activityLabel;

        boolean joins() {
            return component != null;
        }

        int w() {
            return item == null ? 1 : item.w;
        }

        int h() {
            return item == null ? 1 : item.h;
        }
    }

    private void liftFromHome(final Layout.Item item, View view) {
        Load load = new Load();
        load.item = item;
        load.origin = view;
        if (Layout.APP.equals(item.kind)) {
            load.component = item.component;
        }
        final App app = Layout.APP.equals(item.kind) ? known.get(item.component) : null;
        Bitmap picture = face(view);
        Offer offer = new Offer(this, stage).tint(picture).title(nameOf(item, app));
        if (app != null) {
            offer.tool(Sketch.INFO, Words.s("about"), new Runnable() {
                public void run() {
                    about(app);
                }
            });
        }
        offer.tool(Sketch.REMOVE, Words.s("remove"), new Runnable() {
            public void run() {
                if (Layout.WIDGET.equals(item.kind) && item.id >= 0) {
                    forget(item.id);
                }
                layout.remove(item);
                layout.save(Home.this);
                if (Layout.SHORTCUT.equals(item.kind)) {
                    pinFor(item.component);
                }
                build();
            }
        });
        if (app != null) {
            shortcuts(offer, app);
        }
        if (Layout.DOOR.equals(item.kind)) {
            // What the thing shows is chosen in the settings; its card keeps
            // where it stands and how large it is, which is seen moving here.
            final String subject = "drawer";
            offer.tool(Sketch.GEAR, Words.s("settings"), new Runnable() {
                public void run() {
                    closeCard();
                    startActivity(new Intent(Home.this, Tune.class).putExtra(Tune.SUBJECT, subject));
                }
            });
        }
        if (Layout.WIDGET.equals(item.kind)) {
            sizes(offer, item);
        }
        lift(load, picture, offer, onStage(view));
        view.setAlpha(0.25f);
    }

    /** What a thing on a screen is called, for the head of its card. */
    private String nameOf(Layout.Item item, App app) {
        if (app != null) {
            return app.label;
        }
        if (Layout.FOLDER.equals(item.kind)) {
            return item.label != null && item.label.length() > 0 ? item.label : Words.s("folder");
        }
        if (Layout.DOOR.equals(item.kind)) {
            return Words.s("put_door");
        }
        if (Layout.SHORTCUT.equals(item.kind) || Layout.ACTIVITY.equals(item.kind)) {
            return item.label;
        }
        if (Layout.WIDGET.equals(item.kind) && item.provider != null) {
            int slash = item.provider.indexOf('/');
            App owner = ownerOf(slash > 0 ? item.provider.substring(0, slash) : item.provider);
            return owner != null ? owner.label : Words.s("widget");
        }
        return "";
    }

    private void liftFromDrawer(final App app, View cell) {
        Load load = new Load();
        load.component = app.component().flattenToShortString();
        load.origin = null;
        Bitmap picture = face(Cell.face(cell));
        Offer offer = new Offer(this, stage).tint(picture).title(app.label);
        offer.tool(Sketch.INFO, Words.s("about"), new Runnable() {
            public void run() {
                about(app);
            }
        });
        shortcuts(offer, app);
        lift(load, picture, offer, onStage(Cell.face(cell)));
    }

    private void liftFromFolder(final App app, View cell, Layout.Item folder) {
        Load load = new Load();
        load.component = app.component().flattenToShortString();
        load.folder = folder;
        Bitmap picture = face(Cell.face(cell));
        Rect at = onStage(Cell.face(cell));
        if (tray != null) {
            tray.drop();
        }
        Offer offer = new Offer(this, stage).tint(picture).title(app.label);
        offer.tool(Sketch.INFO, Words.s("about"), new Runnable() {
            public void run() {
                about(app);
            }
        });
        shortcuts(offer, app);
        lift(load, picture, offer, at);
    }

    /** Where a view stands on the stage. */
    private Rect onStage(View view) {
        int[] at = new int[2];
        int[] base = new int[2];
        view.getLocationInWindow(at);
        stage.getLocationInWindow(base);
        int x = at[0] - base[0];
        int y = at[1] - base[1];
        return new Rect(x, y, x + view.getWidth(), y + view.getHeight());
    }

    // ------------------------------------------------------------ shortcuts

    /** A circle, for the pictures of shortcuts on a card. */
    private static final Tile.Look DISC = new Tile.Look(2f, 1f, Tile.Look.BARE, 1f);

    /**
     * The little ground a mark sits on in a card. A card belongs to the
     * thing it opened over, and says so by shape rather than by colour:
     * the marks stand on the tiles' own outline, at the tiles' own
     * proportion, so a card of a home screen of squircles is a card of
     * squircles. Where the tiles wear no shape of ours — no mask over the
     * icons — the marks keep to a disc, as the icons do.
     */
    private Tile.Look mould() {
        if (look.window == Tile.Look.RAW) {
            return DISC;
        }
        return new Tile.Look(look.power, look.ratio, Tile.Look.BARE, 1f);
    }

    /**
     * The ways into an application it offers itself, up to five: those it
     * declares first, then those it made lately, each by its rank. Only a
     * home screen chosen as the phone's own may ask for them.
     */
    private void shortcuts(Offer offer, final App app) {
        offerShortcuts(offer, app);
        offer.link(Sketch.SCREENS, Words.s("screens_of"), new Runnable() {
            public void run() {
                screensOf(app);
            }
        });
    }

    private void offerShortcuts(Offer offer, final App app) {
        if (!doors.hasShortcutHostPermission()) {
            offer.note(Words.s("shortcuts_home"));
            return;
        }
        List<android.content.pm.ShortcutInfo> found;
        try {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(app.pkg());
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
            found = doors.getShortcuts(query, app.user());
        } catch (RuntimeException refused) {
            return;
        }
        if (found == null || found.isEmpty()) {
            return;
        }
        // Shortcuts made for another of the package's front doors belong to that door's card.
        List<android.content.pm.ShortcutInfo> own = new ArrayList<android.content.pm.ShortcutInfo>();
        for (android.content.pm.ShortcutInfo one : found) {
            if (one.getActivity() == null || one.getActivity().equals(app.component())
                || !isDoor(one.getActivity())) {
                own.add(one);
            }
        }
        found = own;
        List<android.content.pm.ShortcutInfo> sorted = new ArrayList<android.content.pm.ShortcutInfo>(found);
        java.util.Collections.sort(sorted, new java.util.Comparator<android.content.pm.ShortcutInfo>() {
            public int compare(android.content.pm.ShortcutInfo a, android.content.pm.ShortcutInfo b) {
                if (a.isDeclaredInManifest() != b.isDeclaredInManifest()) {
                    return a.isDeclaredInManifest() ? -1 : 1;
                }
                return a.getRank() - b.getRank();
            }
        });
        int dpi = App.dpi(this);
        int side = Round.dp(40f);
        for (int i = 0; i < sorted.size() && i < 5; i++) {
            final android.content.pm.ShortcutInfo info = sorted.get(i);
            Bitmap picture = null;
            try {
                Drawable icon = doors.getShortcutIconDrawable(info, dpi);
                if (icon != null) {
                    picture = glyph(icon, side, offer.accent(), offer.onAccent());
                }
            } catch (RuntimeException unreadable) {
                picture = null;
            }
            final CharSequence said = info.getShortLabel() != null ? info.getShortLabel() : info.getLongLabel();
            final Bitmap shown = picture;
            offer.row(picture, said == null ? "" : said.toString(), new Runnable() {
                public void run() {
                    Keep.opened(Home.this, app.key);
                    try {
                        doors.startShortcut(info.getPackage(), info.getId(), null, null, info.getUserHandle());
                    } catch (RuntimeException closed) {
                        launch(app, board);
                    }
                }
            }, new Runnable() {
                public void run() {
                    Load load = new Load();
                    load.shortcutPkg = info.getPackage();
                    load.shortcutId = info.getId();
                    load.shortcutLabel = said == null ? "" : said.toString();
                    Bitmap tilePicture = Tile.render(shown == null ? null
                        : doors.getShortcutIconDrawable(info, App.dpi(Home.this)), tile, look);
                    stage.lift(tilePicture, carrier(load));
                }
            });
        }
    }

    /**
     * A shortcut's picture drawn to match its card: its mark in the card's
     * ink, on a disc of the card's accent. Every mark is inked, whatever
     * colours it came in — a card of rows should read as one thing, not as
     * a handful of red, yellow and green badges — and a mark drawn in a
     * colour only its own application knows is seen here like the rest.
     */
    private Bitmap glyph(Drawable icon, int side, int fill, int ink) {
        Drawable mark = null;
        if (icon instanceof android.graphics.drawable.AdaptiveIconDrawable) {
            android.graphics.drawable.AdaptiveIconDrawable layered =
                (android.graphics.drawable.AdaptiveIconDrawable) icon;
            if (Build.VERSION.SDK_INT >= 33) {
                mark = layered.getMonochrome();
            }
            if (mark == null) {
                mark = layered.getForeground();
            }
        }
        if (mark == null) {
            mark = icon;
        }
        mark = mark.mutate();
        Bitmap made = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(made);
        android.graphics.Paint disc = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        disc.setColor(fill);
        // The ground of a mark is the tiles' own outline: a card says
        // which home screen it belongs to by its shapes, not by its colour.
        android.graphics.Path ground = mould() == DISC ? null
            : Tile.curve(0f, 0f, side, side, look.power);
        if (ground == null) {
            canvas.drawCircle(side / 2f, side / 2f, side / 2f, disc);
        } else {
            canvas.drawPath(ground, disc);
        }
        // A layer of an icon is drawn larger than what shows of it; a
        // picture that is a picture already fits the disc as it is.
        int bleed = icon instanceof android.graphics.drawable.AdaptiveIconDrawable
            ? Math.round(side * 0.25f) : -Math.round(side * 0.16f);
        mark.setBounds(-bleed, -bleed, side + bleed, side + bleed);
        mark.setColorFilter(new android.graphics.PorterDuffColorFilter(ink,
            android.graphics.PorterDuff.Mode.SRC_IN));
        android.graphics.Path round = ground;
        if (round == null) {
            round = new android.graphics.Path();
            round.addCircle(side / 2f, side / 2f, side / 2f, android.graphics.Path.Direction.CW);
        }
        canvas.save();
        canvas.clipPath(round);
        mark.draw(canvas);
        canvas.restore();
        return made;
    }

    /**
     * Pins, for a package, exactly the shortcuts its places on the screens
     * name: a shortcut set down is kept by the system for this home screen,
     * and one taken away is let go.
     */
    private void pinFor(String pkg) {
        if (pkg == null || !doors.hasShortcutHostPermission()) {
            return;
        }
        List<String> ids = new ArrayList<String>();
        for (Layout.Screen screen : layout.every()) {
            for (Layout.Item item : screen.items) {
                if (Layout.SHORTCUT.equals(item.kind) && pkg.equals(item.component) && item.shortcut != null
                    && !ids.contains(item.shortcut)) {
                    ids.add(item.shortcut);
                }
            }
        }
        try {
            doors.pinShortcuts(pkg, ids, Process.myUserHandle());
        } catch (RuntimeException refused) {
            // Not a shortcut this home screen may keep; the tile opens its application instead.
        }
    }

    /** Every package with shortcuts on the screens, pinned as the screens say. */
    private void pinAll() {
        java.util.Set<String> pkgs = new java.util.HashSet<String>();
        for (Layout.Screen screen : layout.every()) {
            for (Layout.Item item : screen.items) {
                if (Layout.SHORTCUT.equals(item.kind) && item.component != null) {
                    pkgs.add(item.component);
                }
            }
        }
        for (String pkg : pkgs) {
            pinFor(pkg);
        }
    }

    /** A shortcut's tile: its own picture if this home screen may have it, else its application's. */
    private View shortcutTile(final Layout.Item item) {
        final App owner = ownerOf(item.component);
        ImageView face = new ImageView(this);
        face.setScaleType(ImageView.ScaleType.CENTER);
        face.setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.L));
        final int dpi = App.dpi(this);
        icons.put(face, "shortcut:" + item.component + ":" + item.shortcut, tile, new Icons.Source() {
            public Drawable icon() {
                if (doors.hasShortcutHostPermission() && item.shortcut != null) {
                    LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                    query.setPackage(item.component);
                    query.setShortcutIds(java.util.Collections.singletonList(item.shortcut));
                    query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                        | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                        | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
                    List<android.content.pm.ShortcutInfo> found = doors.getShortcuts(query, Process.myUserHandle());
                    if (found != null && !found.isEmpty()) {
                        Drawable own = doors.getShortcutIconDrawable(found.get(0), dpi);
                        if (own != null) {
                            return own;
                        }
                    }
                }
                return owner == null ? null : owner.icon(dpi);
            }
        });
        return face;
    }

    /** Whether a component is one of the phone's front doors. */
    private boolean isDoor(ComponentName name) {
        return known.containsKey(name.flattenToShortString());
    }

    // ------------------------------------------------------------ screens inside

    private Roll roll;

    /**
     * The screens inside an application that it lets anything open: each by
     * its name and, under it, the name it is known by inside the package, so
     * that two alike can be told apart. A press opens one; a long press
     * lifts it, to be set on a screen as a tile of its own. Not every screen
     * opens alone: some wait for the screen before them, or for something to
     * show, and say so by closing.
     */
    private void screensOf(final App app) {
        android.content.pm.PackageManager pm = getPackageManager();
        android.content.pm.ActivityInfo[] found;
        try {
            found = pm.getPackageInfo(app.pkg(), android.content.pm.PackageManager.GET_ACTIVITIES).activities;
        } catch (android.content.pm.PackageManager.NameNotFoundException gone) {
            return;
        }
        final List<android.content.pm.ActivityInfo> open = new ArrayList<android.content.pm.ActivityInfo>();
        if (found != null) {
            for (android.content.pm.ActivityInfo one : found) {
                if (one.exported && one.enabled && (one.permission == null
                    || checkSelfPermission(one.permission) == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
                    open.add(one);
                }
            }
        }
        final List<String> names = new ArrayList<String>();
        for (android.content.pm.ActivityInfo one : open) {
            names.add(String.valueOf(one.loadLabel(pm)));
        }
        final java.text.Collator order = java.text.Collator.getInstance();
        order.setStrength(java.text.Collator.PRIMARY);
        List<Integer> index = new ArrayList<Integer>();
        for (int i = 0; i < open.size(); i++) {
            index.add(i);
        }
        java.util.Collections.sort(index, new java.util.Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                int by = order.compare(names.get(a), names.get(b));
                return by != 0 ? by : open.get(a).name.compareTo(open.get(b).name);
            }
        });
        stage.still(true);
        roll = new Roll(this, app.label, Words.s("screens_note"), barTop, barBottom, new Roll.Hand() {
            public void closed() {
                stage.still(false);
                roll = null;
            }
        });
        if (open.size() > 1) {
            // All of them at once, in a folder of their own: a list of
            // seventy screens is not carried out one at a time.
            final List<String> every = new ArrayList<String>();
            for (int i : index) {
                every.add(new ComponentName(open.get(i).packageName, open.get(i).name).flattenToShortString());
            }
            roll.row(Words.s("screens_folder"), String.valueOf(open.size()), new Runnable() {
                public void run() {
                    if (roll != null) {
                        roll.close();
                    }
                    gather(app.label, every);
                }
            }, null);
        }
        for (int i : index) {
            final android.content.pm.ActivityInfo one = open.get(i);
            final String name = names.get(i);
            final ComponentName target = new ComponentName(one.packageName, one.name);
            String inner = target.flattenToShortString();
            inner = inner.substring(inner.indexOf('/') + 1);
            roll.row(name, inner, new Runnable() {
                public void run() {
                    openScreen(target, board);
                }
            }, new Runnable() {
                public void run() {
                    Load load = new Load();
                    load.activity = target.flattenToString();
                    load.activityLabel = name;
                    Drawable icon;
                    try {
                        icon = getPackageManager().getActivityIcon(target);
                    } catch (Exception unknown) {
                        icon = app.icon(App.dpi(Home.this));
                    }
                    stage.lift(Tile.render(icon, tile, look), carrier(load));
                }
            });
        }
        stage.addView(roll, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        roll.show();
    }

    /** Opens one screen inside an application by its name; a screen that will not open says so. */
    private void openScreen(ComponentName target, View from) {
        Intent open = new Intent(Intent.ACTION_MAIN);
        open.setComponent(target);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(open, ActivityOptions.makeScaleUpAnimation(from, 0, 0,
                from.getWidth(), from.getHeight()).toBundle());
        } catch (RuntimeException closed) {
            from.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            android.widget.Toast.makeText(this, Words.s("no_entry"), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /** A tile for one screen inside an application: that screen's own picture, or its application's. */
    private View screenTile(final Layout.Item item) {
        final ComponentName target = ComponentName.unflattenFromString(item.component);
        final App owner = target == null ? null : ownerOf(target.getPackageName());
        ImageView face = new ImageView(this);
        face.setScaleType(ImageView.ScaleType.CENTER);
        face.setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.L));
        face.setContentDescription(item.label);
        icons.put(face, "activity:" + item.component, tile, new Icons.Source() {
            public Drawable icon() {
                try {
                    return getPackageManager().getActivityIcon(target);
                } catch (Exception unknown) {
                    return owner == null ? null : owner.icon(App.dpi(Home.this));
                }
            }
        });
        face.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (target != null) {
                    openScreen(target, v);
                }
            }
        });
        return face;
    }










    /**
     * The charge of headphones already near when the screen is shown. The
     * system tells it only to those who ask by a name it does not publish,
     * so it is asked for carefully, and a refusal means nothing is shown.
     */
    @SuppressWarnings("deprecation")

    // ------------------------------------------------------------ widget sizes

    /** A widget's width and height on its card, each between a minus and a plus. */
    private void sizes(final Offer offer, final Layout.Item item) {
        final Offer.Count[] place = new Offer.Count[2];
        offer.link(Sketch.ACROSS, Words.s("to_middle"), new Runnable() {
            public void run() {
                middle(item);
            }
        });
        place[0] = offer.count(Words.s("row"), String.valueOf(item.y + 1), new Runnable() {
            public void run() {
                shift(item, 0, -1, place);
            }
        }, new Runnable() {
            public void run() {
                shift(item, 0, 1, place);
            }
        });
        place[1] = offer.count(Words.s("column"), String.valueOf(item.x + 1), new Runnable() {
            public void run() {
                shift(item, -1, 0, place);
            }
        }, new Runnable() {
            public void run() {
                shift(item, 1, 0, place);
            }
        });
        gripFor(item);
        final Offer.Count[] counts = new Offer.Count[2];
        counts[0] = offer.count(Words.s("width"), String.valueOf(item.w), new Runnable() {
            public void run() {
                resize(item, item.w - 1, item.h, counts);
            }
        }, new Runnable() {
            public void run() {
                resize(item, item.w + 1, item.h, counts);
            }
        });
        counts[1] = offer.count(Words.s("height"), String.valueOf(item.h), new Runnable() {
            public void run() {
                resize(item, item.w, item.h - 1, counts);
            }
        }, new Runnable() {
            public void run() {
                resize(item, item.w, item.h + 1, counts);
            }
        });
    }

    /**
     * A widget or the clock moved a row up or down, if the row it would
     * take is free; refused, the phone says so and nothing moves.
     */
    private void shift(Layout.Item item, int dx, int dy, Offer.Count[] counts) {
        int screen = layout.screenOf(item);
        int x = item.x + dx;
        int y = item.y + dy;
        if (screen < 0 || x < 0 || y < 0 || x + item.w > layout.columns || y + item.h > layout.rows
            || !layout.free(screen, x, y, item.w, item.h, item)) {
            board.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            return;
        }
        item.x = x;
        item.y = y;
        layout.save(this);
        build();
        counts[0].show(String.valueOf(y + 1));
        counts[1].show(String.valueOf(x + 1));
    }

    /**
     * A widget or the clock set across the middle of its row. A width can
     * stand exactly in the middle only if what is left over splits evenly;
     * when it does not, the phone says what width would.
     */
    private void middle(Layout.Item item) {
        int screen = layout.screenOf(item);
        int spare = layout.columns - item.w;
        if (screen < 0 || spare < 0) {
            return;
        }
        if (spare % 2 != 0) {
            board.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            android.widget.Toast.makeText(this, Words.s("middle_cannot")
                .replace("{w}", String.valueOf(item.w)).replace("{n}", String.valueOf(layout.columns)),
                android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        int x = spare / 2;
        if (!layout.free(screen, x, item.y, item.w, item.h, item)) {
            board.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            return;
        }
        item.x = x;
        layout.save(this);
        build();
    }

    /**
     * A widget given a new size, if there is room for it where it stands;
     * wider than the space to its right, it moves left as far as it must.
     * Refused, the phone says so under the finger and nothing moves.
     */
    private void resize(Layout.Item item, int w, int h, Offer.Count[] counts) {
        int screen = layout.screenOf(item);
        if (screen < 0 || w < 1 || h < 1 || w > layout.columns || h > layout.rows) {
            board.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            return;
        }
        int x = Math.min(item.x, layout.columns - w);
        int y = Math.min(item.y, layout.rows - h);
        if (!layout.free(screen, x, y, w, h, item)) {
            board.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            return;
        }
        item.x = x;
        item.y = y;
        item.w = w;
        item.h = h;
        layout.save(this);
        build();
        counts[0].show(String.valueOf(w));
        counts[1].show(String.valueOf(h));
    }

    private void lift(final Load load, Bitmap picture, Offer offer, Rect at) {
        stage.lift(picture, carrier(load));
        stage.offered(offer);
        offer.show(at);
    }

    /** What happens to a lifted thing as the finger moves and lets go. */
    private Stage.Carrier carrier(final Load load) {
        return new Stage.Carrier() {
            public void moved() {
                closeCard();
                if (stage.isOpen()) {
                    stage.close(true);
                }
                if (fresh != null) {
                    fresh.drop();
                }
                grid(true);
            }

            public void over(float fx, float fy) {
                hover(load, fx, fy);
            }

            public void drop(float fx, float fy, boolean moved) {
                grid(false);
                main.removeCallbacks(turn);
                turning = 0;
                if (moved) {
                    land(load, fx, fy);
                } else if (load.origin != null) {
                    load.origin.setAlpha(1f);
                }
            }
        };
    }

    /** The card closed, and with it the frame around whatever it was about. */
    private void closeCard() {
        stage.closeOffer();
        ungrip();
    }

    private void ungrip() {
        if (grip != null) {
            stage.removeView(grip);
            grip = null;
        }
    }

    /**
     * The frame around a widget or the clock: its four handles take and
     * give back rows and columns under the finger, and the thing is laid
     * out again at once. What the card counts in numbers, this does by hand.
     */
    private void gripFor(final Layout.Item item) {
        ungrip();
        final int page = layout.screenOf(item);
        if (page < 0) {
            return;
        }
        grip = new Grip(this, new Grip.Hand() {
            public boolean set(int x, int y, int w, int h) {
                if (x < 0 || y < 0 || x + w > layout.columns || y + h > layout.rows
                    || !layout.free(page, x, y, w, h, item)) {
                    return false;
                }
                item.x = x;
                item.y = y;
                item.w = w;
                item.h = h;
                build();
                return true;
            }

            public void done() {
                layout.save(Home.this);
            }

            public void away() {
                ungrip();
            }
        });
        stage.addView(grip, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        fitGrip(item);
    }

    /** The frame told where the grid lies and which of its cells the thing takes. */
    private void fitGrip(Layout.Item item) {
        if (grip == null) {
            return;
        }
        float bottom = barBottom + dockHigh + (docked ? DOCK_GAP : 0);
        float high = (stage.getHeight() - barTop - bottom) / (float) layout.rows;
        grip.fit(Round.dp(4f), barTop, cellW, high, layout.columns, layout.rows,
            item.x, item.y, item.w, item.h);
    }

    /** The picture a thing is carried as: its tile if it is one, else the view as it stands. */
    private Bitmap face(View view) {
        if (view instanceof ImageView) {
            Drawable d = ((ImageView) view).getDrawable();
            if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
                return ((BitmapDrawable) d).getBitmap();
            }
        }
        Bitmap picture = Bitmap.createBitmap(Math.max(1, view.getWidth()), Math.max(1, view.getHeight()),
            Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(picture));
        return picture;
    }

    private void grid(boolean on) {
        for (int i = 0; i < board.count(); i++) {
            board.sheet(i).carry(on, -1, 0, 0, 0);
        }
        if (dock != null) {
            dock.carry(on, -1, 0, 0, 0);
        }
    }

    /** Where a carried thing would land if let go here: a block of cells, or a thing it would join. */
    private int[] target(Load load, float x, float y) {
        if (dock != null && y >= dock.getTop()) {
            return docking(load, x, y - dock.getTop());
        }
        int page = board.page();
        int[] cell = board.cellAt(x, y);
        if (load.joins()) {
            Layout.Item under = layout.at(page, cell[0], cell[1]);
            if (under != null && under != load.item && joinable(under, load)) {
                return new int[] {under.x, under.y, 1, 1, 1, page};
            }
        }
        int w = load.w();
        int h = load.h();
        int ax = Math.max(0, Math.min(layout.columns - w, cell[0] - (w - 1) / 2));
        int ay = Math.max(0, Math.min(layout.rows - h, cell[1] - (h - 1) / 2));
        int[] spot = layout.nearest(page, ax, ay, w, h, load.item);
        if (spot == null) {
            return null;
        }
        return new int[] {spot[0], spot[1], w, h, 0, page};
    }

    /** Where a carried thing would land in the dock: only what is one place large stands there. */
    private int[] docking(Load load, float x, float y) {
        int[] cell = dock.cellAt(x, y);
        if (load.joins()) {
            Layout.Item under = layout.at(Layout.DOCK, cell[0], 0);
            if (under != null && under != load.item && joinable(under, load)) {
                return new int[] {under.x, 0, 1, 1, 1, Layout.DOCK};
            }
        }
        if (load.w() != 1 || load.h() != 1) {
            return null;
        }
        int[] spot = layout.nearest(Layout.DOCK, cell[0], 0, 1, 1, load.item);
        return spot == null ? null : new int[] {spot[0], 0, 1, 1, 0, Layout.DOCK};
    }

    private boolean joinable(Layout.Item under, Load load) {
        if (Layout.FOLDER.equals(under.kind)) {
            return under != load.folder;
        }
        return Layout.APP.equals(under.kind) && under.component != null
            && !under.component.equals(load.component);
    }

    private void hover(Load load, float x, float y) {
        int page = board.page();
        int[] t = target(load, x, y);
        boolean docking = t != null && t[5] == Layout.DOCK;
        for (int i = 0; i < board.count(); i++) {
            if (i == page && t != null && !docking) {
                board.sheet(i).carry(true, t[0], t[1], t[2], t[3]);
            } else {
                board.sheet(i).carry(true, -1, 0, 0, 0);
            }
        }
        if (dock != null) {
            if (docking) {
                dock.carry(true, t[0], t[1], t[2], t[3]);
            } else {
                dock.carry(true, -1, 0, 0, 0);
            }
        }
        float edge = stage.getWidth() * 0.07f;
        int wish = x < edge ? -1 : (x > stage.getWidth() - edge ? 1 : 0);
        if (wish != turning) {
            turning = wish;
            main.removeCallbacks(turn);
            if (wish != 0) {
                main.postDelayed(turn, EDGE_WAIT);
            }
        }
    }

    private final Runnable turn = new Runnable() {
        public void run() {
            if (turning == 0 || !stage.carrying()) {
                return;
            }
            int next = board.page() + turning;
            if (Keep.endless(Home.this) || (next >= 0 && next < board.count())) {
                board.turnTo(next, true);
                main.postDelayed(this, EDGE_WAIT + Pace.ARRIVE);
            }
        }
    };

    /** Sets a carried thing down, and writes the layout. */
    private void land(Load load, float x, float y) {
        int page = board.page();
        int[] t = target(load, x, y);
        if (t == null) {
            build();
            return;
        }
        page = t[5];
        if (t[4] == 1) {
            Layout.Item under = layout.at(page, t[0], t[1]);
            if (Layout.FOLDER.equals(under.kind)) {
                if (!under.apps.contains(load.component)) {
                    under.apps.add(load.component);
                }
            } else {
                under.apps.clear();
                under.apps.add(under.component);
                under.apps.add(load.component);
                under.kind = Layout.FOLDER;
                under.component = null;
                under.label = "";
            }
            leave(load);
        } else if (load.item != null) {
            layout.remove(load.item);
            load.item.x = t[0];
            load.item.y = t[1];
            layout.screen(page).items.add(load.item);
        } else if (load.activity != null) {
            Layout.Item made = new Layout.Item(Layout.ACTIVITY, t[0], t[1]);
            made.component = load.activity;
            made.label = load.activityLabel;
            layout.screen(page).items.add(made);
        } else if (load.shortcutId != null) {
            Layout.Item made = new Layout.Item(Layout.SHORTCUT, t[0], t[1]);
            made.component = load.shortcutPkg;
            made.shortcut = load.shortcutId;
            made.label = load.shortcutLabel;
            layout.screen(page).items.add(made);
            pinFor(load.shortcutPkg);
        } else {
            Layout.Item made = new Layout.Item(Layout.APP, t[0], t[1]);
            made.component = load.component;
            layout.screen(page).items.add(made);
            if (load.folder != null) {
                load.folder.apps.remove(load.component);
                layout.settle(load.folder);
            }
        }
        layout.save(this);
        build();
    }

    /** A thing that joined a folder leaves where it came from. */
    private void leave(Load load) {
        if (load.item != null) {
            layout.remove(load.item);
        }
        if (load.folder != null) {
            load.folder.apps.remove(load.component);
            layout.settle(load.folder);
        }
    }

    // ------------------------------------------------------------ folders

    /**
     * What a folder holds: its applications, and the screens inside
     * applications that have been put in beside them. A name that answers
     * to nothing any more is simply left out.
     */
    private List<Held> holding(Layout.Item folder) {
        List<Held> inside = new ArrayList<Held>();
        for (String component : folder.apps) {
            App app = known.get(component);
            if (app != null) {
                inside.add(Held.of(component, app));
                continue;
            }
            Held screen = Held.screen(this, component);
            if (screen != null) {
                inside.add(screen);
            }
        }
        return inside;
    }

    /**
     * A folder made on the screen in sight, holding what it is given: the
     * screens inside an application, say. It goes wherever there is room,
     * and the home screen is laid out again with it.
     */
    private void gather(String name, List<String> inside) {
        int page = board.page();
        int[] spot = layout.nearest(page, layout.columns / 2, 0, 1, 1, null);
        if (spot == null) {
            stage.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            return;
        }
        Layout.Item made = new Layout.Item(Layout.FOLDER, spot[0], spot[1]);
        made.label = name;
        made.apps.addAll(inside);
        layout.screen(page).items.add(made);
        layout.save(this);
        build();
    }

    private void openFolder(final Layout.Item folder) {
        List<Held> inside = holding(folder);
        if (inside.isEmpty()) {
            return;
        }
        stage.still(true);
        tray = new Tray(this, folder.label == null ? "" : folder.label, inside, icons, stage.getWidth(),
            new Tray.Hand() {
                public void open(Held app, View from) {
                    if (app.app != null) {
                        launch(app.app, from);
                        return;
                    }
                    try {
                        startActivity(app.opening());
                    } catch (RuntimeException gone) {
                        from.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                    }
                }

                public void lift(Held app, View from) {
                    if (app.app != null) {
                        liftFromFolder(app.app, from, folder);
                    }
                }

                public void closed() {
                    stage.still(false);
                    tray = null;
                }
            });
        stage.addView(tray, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    // ------------------------------------------------------------ links

    /**
     * A shortcut is made in two turns: the owner chooses which application
     * shall make one, and the application then makes it and hands back a
     * way in, a name and a picture.
     */
    private void askLink(int screen, int cx, int cy) {
        linkScreen = screen;
        linkX = cx;
        linkY = cy;
        Intent making = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        Intent choosing = new Intent(Intent.ACTION_PICK_ACTIVITY)
            .putExtra(Intent.EXTRA_INTENT, making)
            .putExtra(Intent.EXTRA_TITLE, Words.s("put_link"));
        try {
            startActivityForResult(choosing, WHICH_LINK);
        } catch (RuntimeException refused) {
            stage.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
        }
    }

    /** What the application handed back, written down and set on the screen. */
    private void keepLink(Intent said) {
        Intent way = said.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (way == null) {
            return;
        }
        String name = said.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Layout.Item made = new Layout.Item(Layout.LINK, linkX, linkY);
        made.label = name == null ? Words.s("put_link") : name;
        made.component = way.toUri(Intent.URI_INTENT_SCHEME);
        Bitmap drawn = said.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (drawn == null) {
            Intent.ShortcutIconResource from =
                said.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (from != null) {
                try {
                    android.content.res.Resources theirs =
                        getPackageManager().getResourcesForApplication(from.packageName);
                    int at = theirs.getIdentifier(from.resourceName, null, null);
                    Drawable icon = at == 0 ? null : theirs.getDrawable(at, null);
                    if (icon != null) {
                        int side = Math.max(1, Math.max(icon.getIntrinsicWidth(), Round.dp(48f)));
                        drawn = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
                        icon.setBounds(0, 0, side, side);
                        icon.draw(new android.graphics.Canvas(drawn));
                    }
                } catch (Exception gone) {
                    drawn = null;
                }
            }
        }
        String kept = drawn == null ? null : keepPicture(drawn);
        if (kept != null) {
            made.options.put("icon", kept);
        }
        if (linkScreen < 0 || linkScreen >= layout.screens.size()
            || !layout.free(linkScreen, linkX, linkY, 1, 1, null)) {
            int[] spot = layout.nearest(board.page(), layout.columns / 2, layout.rows - 1, 1, 1, null);
            if (spot == null) {
                return;
            }
            made.x = spot[0];
            made.y = spot[1];
            layout.screen(board.page()).items.add(made);
        } else {
            layout.screens.get(linkScreen).items.add(made);
        }
        layout.save(this);
        build();
    }

    /** A shortcut's picture, kept among the home screen's own files. */
    private String keepPicture(Bitmap picture) {
        java.io.File room = new java.io.File(getFilesDir(), "links");
        room.mkdirs();
        String name = "link-" + System.currentTimeMillis() + ".png";
        java.io.OutputStream out = null;
        try {
            out = new java.io.FileOutputStream(new java.io.File(room, name));
            picture.compress(Bitmap.CompressFormat.PNG, 100, out);
            return name;
        } catch (java.io.IOException unwritten) {
            return null;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (java.io.IOException never) {
                // Nothing to be done about a file that will not close.
            }
        }
    }

    /** A shortcut on a screen: its own picture, and its own way in. */
    private View linkTile(final Layout.Item item) {
        ImageView face = new ImageView(this);
        face.setScaleType(ImageView.ScaleType.CENTER);
        face.setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.L));
        face.setContentDescription(item.label);
        final String kept = item.options.get("icon");
        icons.put(face, "link:" + item.component, tile, new Icons.Source() {
            public Drawable icon() {
                if (kept == null) {
                    return null;
                }
                Bitmap picture = android.graphics.BitmapFactory.decodeFile(
                    new java.io.File(new java.io.File(getFilesDir(), "links"), kept).getPath());
                return picture == null ? null : new android.graphics.drawable.BitmapDrawable(
                    getResources(), picture);
            }
        });
        face.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(Intent.parseUri(item.component, Intent.URI_INTENT_SCHEME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Exception refused) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                }
            }
        });
        face.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                liftFromHome(item, v);
                return true;
            }
        });
        return face;
    }

    // ------------------------------------------------------------ ground

    private void offerGround(final int screen, final int cx, final int cy) {
        final boolean empty = layout.screens.get(screen).items.isEmpty();
        // The screen has a number, but nobody came here to read it.
        Offer offer = new Offer(this, stage).title("");
        offer.tool(Sketch.GEAR, Words.s("settings"), new Runnable() {
            public void run() {
                startActivity(new Intent(Home.this, Tune.class));
            }
        });
        if (layout.free(screen, cx, cy, 1, 1, null)) {
            offer.row(Sketch.FOLDER_OPEN, Words.s("put_folder"), new Runnable() {
                public void run() {
                    Layout.Item made = new Layout.Item(Layout.FOLDER, cx, cy);
                    made.label = Words.s("put_folder");
                    layout.screens.get(screen).items.add(made);
                    layout.save(Home.this);
                    build();
                }
            });
        }
        offer.row(Sketch.WIDGET, Words.s("widget"), new Runnable() {
            public void run() {
                openPicker(screen, cx, cy);
            }
        });
        offer.row(Sketch.PIN, Words.s("put_link"), new Runnable() {
            public void run() {
                askLink(screen, cx, cy);
            }
        });
        offer.row(Sketch.PLUS, Words.s("add_screen"), new Runnable() {
            public void run() {
                layout.screens.add(screen + 1, new Layout.Screen());
                if (layout.home > screen) {
                    layout.home++;
                }
                layout.save(Home.this);
                build();
                board.turnTo(screen + 1, true);
            }
        });
        if (empty && layout.screens.size() > 1) {
            offer.row(Sketch.REMOVE, Words.s("remove_screen"), new Runnable() {
                public void run() {
                    layout.screens.remove(screen);
                    if (layout.home >= screen && layout.home > 0) {
                        layout.home--;
                    }
                    layout.save(Home.this);
                    build();
                }
            });
        }
        if (screen != layout.home) {
            offer.row(Sketch.HOUSE, Words.s("main_screen"), new Runnable() {
                public void run() {
                    layout.home = screen;
                    layout.save(Home.this);
                    build();
                }
            });
        }
        stage.offered(offer);
        int x = Math.round(stage.fingerX());
        int y = Math.round(stage.fingerY());
        int reach = Round.dp(24f);
        offer.show(new Rect(x - reach, y - reach, x + reach, y + reach));
    }

    // ------------------------------------------------------------ doors

    private void launch(App app, View from) {
        Keep.opened(this, app.key);
        Rect bounds = new Rect();
        from.getGlobalVisibleRect(bounds);
        Bundle grow = ActivityOptions.makeScaleUpAnimation(from, 0, 0,
            from.getWidth(), from.getHeight()).toBundle();
        try {
            doors.startMainActivity(app.component(), app.user(), bounds, grow);
        } catch (RuntimeException gone) {
            read();
        }
    }

    /**
     * A shortcut opens by its own way in if this home screen may use it,
     * and otherwise by its application's front door.
     */
    private void shortcut(Layout.Item item, View from) {
        Rect bounds = new Rect();
        from.getGlobalVisibleRect(bounds);
        try {
            doors.startShortcut(item.component, item.shortcut, bounds, null, Process.myUserHandle());
            return;
        } catch (RuntimeException closed) {
            // Not a shortcut this home screen was given; the application itself, then.
        }
        App owner = ownerOf(item.component);
        if (owner != null) {
            launch(owner, from);
        }
    }

    private void about(App app) {
        try {
            doors.startAppDetailsActivity(app.component(), app.user(), null, null);
        } catch (RuntimeException gone) {
            read();
        }
    }

    // ------------------------------------------------------------ life

    /** A bar called back for a moment is put away again when the screen has the finger once more. */
    @Override
    public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus && Keep.immersion(this) != Keep.BARS) {
            glass();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        seen = true;
        if (Keep.stamp(this) != built) {
            // A changed setting was throwing the whole screen away and
            // building it from nothing. Only a new look or a new colour
            // needs that much; everything else is put on where it stands.
            Tile.Look now = Keep.tile(this);
            float hue = Tone.hue();
            float rich = Tone.rich();
            Tone.read(this);
            if (!now.same(look) || hue != Tone.hue() || rich != Tone.rich() || Cast.flat != Keep.flat(this)) {
                recreate();
                return;
            }
            refresh();
        }
    }


    /** The button for the applications, asked for in the settings, set down where there is room. */
    private void placeDoor() {
        int page = board.page();
        int[] spot = layout.nearest(page, layout.columns / 2, layout.rows - 1, 1, 1, null);
        if (spot == null) {
            return;
        }
        layout.screen(page).items.add(new Layout.Item(Layout.DOOR, spot[0], spot[1]));
        layout.save(this);
    }

    /** What a setting can change without the screen being made anew, read again and put on. */
    private void refresh() {
        built = Keep.stamp(this);
        if (Keep.takeDoor(this)) {
            placeDoor();
        }
        Words.load(this);
        glass();
        edge = Keep.edge(this);
        docked = Keep.dock(this);
        layout = Layout.load(this);
        board.endless(Keep.endless(this));
        sweep();
        build();
    }

    /** The day turning into the night under the home screen, while it stands. */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration now) {
        super.onConfigurationChanged(now);
        boolean was = Tone.night();
        Tone.read(this);
        if (Tone.night() != was) {
            recreate();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", board.page());
    }

    @Override
    protected void onPause() {
        super.onPause();
        seen = false;
    }

    /** Home: whatever lies open closes; on bare screens, back to the main one. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean closed = false;
        if (tray != null) {
            tray.drop();
            closed = true;
        }
        if (picker != null) {
            picker.close();
            closed = true;
        }
        if (roll != null) {
            roll.close();
            closed = true;
        }
        if (fresh != null) {
            if (seen) {
                fresh.close();
            } else {
                fresh.drop();
            }
            closed = true;
        }
        closeCard();
        if (stage.isOpen()) {
            stage.close(seen);
            if (!seen) {
                drawer.rewind();
            }
            closed = true;
        }
        if (!closed && seen && board.page() != layout.home) {
            board.turnTo(layout.home, true);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (roll != null) {
            roll.close();
        } else if (picker != null) {
            picker.close();
        } else if (tray != null) {
            tray.close();
        } else if (fresh != null) {
            fresh.close();
        } else if (stage.isOpen()) {
            stage.close(true);
        } else if (stage.offering()) {
            closeCard();
        } else if (!stage.carrying()) {
            openFresh();
        }
    }

    // ------------------------------------------------------------ widgets

    @Override
    protected void onStart() {
        super.onStart();
        try {
            host.startListening();
        } catch (RuntimeException busy) {
            // The system could not take the host just now; the widgets stand still until next time.
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            host.stopListening();
        } catch (RuntimeException gone) {
            // Nothing to stop.
        }
    }

    /** A placed widget, live; or its place, waiting to be pressed. */
    private View widget(final Layout.Item item) {
        AppWidgetProviderInfo info = item.id >= 0 ? widgets.getAppWidgetInfo(item.id) : null;
        if (info != null) {
            final AppWidgetHostView pane = host.createView(getApplicationContext(), item.id, info);
            if (edge) {
                pane.setPadding(0, 0, 0, 0);
            }
            pane.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                public void onLayoutChange(View v, int l, int t, int r, int b,
                                           int ol, int ot, int or, int ob) {
                    if (r - l != or - ol || b - t != ob - ot) {
                        size(pane);
                    }
                }
            });
            return pane;
        }
        View place = Things.place(this, item.provider);
        place.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                place(item, false);
            }
        });
        return place;
    }

    /** Tells a widget the size it has been given, in the units widgets are measured in. */
    @SuppressWarnings("deprecation")
    private void size(AppWidgetHostView pane) {
        float d = getResources().getDisplayMetrics().density;
        int w = Math.round(pane.getWidth() / d);
        int h = Math.round(pane.getHeight() / d);
        if (w > 0 && h > 0) {
            pane.updateAppWidgetSize(new Bundle(), w, h, w, h);
        }
    }

    /** The size a widget will have, for the system to pass on before it is first drawn. */
    private Bundle options(Layout.Item item) {
        float d = getResources().getDisplayMetrics().density;
        int w = Math.round(item.w * cellW / d);
        int h = Math.round(item.h * cellH / d);
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, w);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, w);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, h);
        return options;
    }

    /**
     * How many cells a widget takes: as many as it asks for, or as its
     * least size needs; and the whole width if it can stretch and is
     * already more than half as wide as the screen.
     */
    private int[] span(AppWidgetProviderInfo info) {
        int w = 0;
        int h = 0;
        if (Build.VERSION.SDK_INT >= 31) {
            w = info.targetCellWidth;
            h = info.targetCellHeight;
        }
        if (w <= 0) {
            w = (int) Math.ceil(info.minWidth / Math.max(1f, cellW));
        }
        if (h <= 0) {
            h = (int) Math.ceil(info.minHeight / Math.max(1f, cellH));
        }
        if ((info.resizeMode & AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0 && w * 2 > layout.columns) {
            w = layout.columns;
        }
        return new int[] {Math.max(1, Math.min(layout.columns, w)), Math.max(1, Math.min(layout.rows, h))};
    }

    /** Every widget the phone offers, on a sheet; the one chosen goes where the finger was. */
    private void openPicker(final int screen, final int cx, final int cy) {
        List<AppWidgetProviderInfo> offered = widgets.getInstalledProvidersForProfile(Process.myUserHandle());
        stage.still(true);
        picker = new Picker(this, offered, barTop, barBottom, new Picker.Hand() {
            public void picked(AppWidgetProviderInfo info) {
                int[] span = span(info);
                int[] at = layout.nearest(screen, cx - (span[0] - 1) / 2, cy, span[0], span[1], null);
                if (at == null) {
                    android.widget.Toast.makeText(Home.this, Words.s("no_room"),
                        android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                Layout.Item made = new Layout.Item(Layout.WIDGET, at[0], at[1]);
                made.w = span[0];
                made.h = span[1];
                made.provider = info.provider.flattenToString();
                layout.screens.get(screen).items.add(made);
                layout.save(Home.this);
                build();
                place(made, true);
            }

            public void closed() {
                stage.still(false);
                picker = null;
            }

            public int[] span(AppWidgetProviderInfo info) {
                return Home.this.span(info);
            }
        });
        stage.addView(picker, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        picker.show();
    }

    /**
     * Places a widget: a number from the host, and the system's leave to
     * bind it to its maker, asked for only if not already given for good.
     */
    private void place(Layout.Item item, boolean fresh) {
        ComponentName provider = item.provider == null ? null
            : ComponentName.unflattenFromString(item.provider);
        if (provider == null || waiting != null) {
            return;
        }
        int id = host.allocateAppWidgetId();
        Bundle options = options(item);
        waiting = item;
        waitingId = id;
        waitingNew = fresh;
        boolean bound;
        try {
            bound = widgets.bindAppWidgetIdIfAllowed(id, Process.myUserHandle(), provider, options);
        } catch (RuntimeException gone) {
            giveUp();
            return;
        }
        if (bound) {
            configure();
            return;
        }
        Intent ask = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
        ask.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        ask.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
        ask.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, Process.myUserHandle());
        ask.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
        try {
            startActivityForResult(ask, BIND);
        } catch (ActivityNotFoundException none) {
            giveUp();
        }
    }

    /** A bound widget's own settings, if it has any it cannot do without; then its place. */
    private void configure() {
        AppWidgetProviderInfo info = widgets.getAppWidgetInfo(waitingId);
        if (info == null) {
            giveUp();
            return;
        }
        boolean optional = Build.VERSION.SDK_INT >= 31
            && (info.widgetFeatures & AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0;
        if (info.configure != null && !optional) {
            try {
                host.startAppWidgetConfigureActivityForResult(this, waitingId, 0, CONFIGURE, null);
                return;
            } catch (RuntimeException closed) {
                // Its settings would not open; it is placed as it is.
            }
        }
        attach();
    }

    private void attach() {
        Layout.Item item = waiting;
        int id = waitingId;
        waiting = null;
        waitingId = -1;
        if (item.id >= 0 && item.id != id) {
            forget(item.id);
        }
        item.id = id;
        layout.save(this);
        build();
    }

    /** The number is handed back; a widget that was new leaves its place too. */
    private void giveUp() {
        if (waitingId >= 0) {
            forget(waitingId);
        }
        if (waitingNew && waiting != null) {
            layout.remove(waiting);
            layout.save(this);
            build();
        }
        waiting = null;
        waitingId = -1;
    }

    private void forget(int id) {
        try {
            host.deleteAppWidgetId(id);
        } catch (RuntimeException notOurs) {
            // A number from another phone, or already gone.
        }
    }

    /** Numbers the host holds that no place on the screens names any more are handed back. */
    private void sweep() {
        java.util.Set<Integer> used = new java.util.HashSet<Integer>();
        for (Layout.Screen screen : layout.screens) {
            for (Layout.Item item : screen.items) {
                if (Layout.WIDGET.equals(item.kind) && item.id >= 0) {
                    used.add(item.id);
                }
            }
        }
        // The owner's own layout, set aside while the default stands, keeps its widgets alive.
        Layout aside = Layout.stashed(this);
        if (aside != null) {
            for (Layout.Screen screen : aside.screens) {
                for (Layout.Item item : screen.items) {
                    if (Layout.WIDGET.equals(item.kind) && item.id >= 0) {
                        used.add(item.id);
                    }
                }
            }
        }
        int[] held;
        try {
            held = host.getAppWidgetIds();
        } catch (RuntimeException unknown) {
            return;
        }
        for (int id : held) {
            if (!used.contains(id)) {
                forget(id);
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == WHICH_LINK) {
            if (result == RESULT_OK && data != null && data.getComponent() != null) {
                try {
                    startActivityForResult(new Intent(Intent.ACTION_CREATE_SHORTCUT)
                        .setComponent(data.getComponent()), MAKE_LINK);
                } catch (RuntimeException refused) {
                    stage.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                }
            }
            return;
        }
        if (request == MAKE_LINK) {
            if (result == RESULT_OK && data != null) {
                keepLink(data);
            }
            return;
        }
        if (waiting == null) {
            return;
        }
        if (request == BIND) {
            if (result == RESULT_OK) {
                configure();
            } else {
                giveUp();
            }
        } else if (request == CONFIGURE) {
            if (result == RESULT_OK) {
                attach();
            } else {
                giveUp();
            }
        }
    }

    // ------------------------------------------------------------ fresh

    /** How long a thing put on the phone or changed counts as fresh. */
    private static final long FRESH_FOR = 14L * 24L * 60L * 60L * 1000L;

    /**
     * Lowers what is fresh: up to eight applications last opened from here,
     * and up to twelve put on the phone or changed in the last two weeks,
     * the newest first, each with a word about it.
     */
    private void openFresh() {
        List<App> recent = new ArrayList<App>();
        for (String key : Keep.recent(this)) {
            App app = byKey.get(key);
            if (app != null && recent.size() < 8) {
                recent.add(app);
            }
        }
        long since = System.currentTimeMillis() - FRESH_FOR;
        List<App> lately = new ArrayList<App>();
        for (App app : apps) {
            if (app.updated >= since) {
                lately.add(app);
            }
        }
        App.sort(lately, Keep.BY_UPDATED);
        while (lately.size() > 12) {
            lately.remove(lately.size() - 1);
        }
        List<String> tags = new ArrayList<String>();
        android.content.pm.PackageManager pm = getPackageManager();
        for (App app : lately) {
            String tag = Words.s(app.installed >= since ? "tag_new" : "tag_updated");
            try {
                if (pm.checkSignatures(getPackageName(), app.pkg())
                    == android.content.pm.PackageManager.SIGNATURE_MATCH) {
                    tag = tag + "  " + pm.getPackageInfo(app.pkg(), 0).versionName;
                }
            } catch (Exception unknown) {
                // A version that cannot be read is simply not said.
            }
            tags.add(tag);
        }
        stage.still(true);
        fresh = new Fresh(this, recent, lately, tags, icons, stage.getWidth(), barTop, barBottom,
            new Fresh.Hand() {
                public void open(App app, View from) {
                    launch(app, from);
                }

                public void lift(App app, View from) {
                    liftFromDrawer(app, from);
                }

                public void closed() {
                    stage.still(false);
                    fresh = null;
                }
            });
        fresh.show(stage);
    }

    @Override
    protected void onDestroy() {
        doors.unregisterCallback(watch);
        icons.close();
        reader.shutdownNow();
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
