package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.WallpaperManager;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The home screen, and in this version the whole application.
 *
 * One screen: the wallpaper in a rounded window, a grid of four by five
 * inside it, a dock of five under it, and the bar at the foot with the
 * search field and the round button. What stands on the screen is what the
 * phone itself keeps for each everyday role; nothing is moved, nothing is
 * saved, nothing is set. Everything else arrives later, one thing at a time.
 */
public final class Home extends Activity {

    /** The grid, after the platform's own guidance for a first screen. */
    private static final int COLUMNS = 4;
    private static final int ROWS = 5;
    private static final int DOCK = 5;

    /** The words of this version; a dictionary arrives with the second language. */
    private static final String SEARCH = "Search";
    private static final String MENU = "Menu";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Cell> cells = new ArrayList<>();

    private float density;
    private float scaled;
    private float iconSize;

    private Frame frame;
    private Grid grid;
    private Grid dock;
    private FrameLayout shelf;
    private LinearLayout bar;
    private TextView field;
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
    }

    @Override
    public void onConfigurationChanged(Configuration changed) {
        super.onConfigurationChanged(changed);
        Tone.read(this);
        build();
        fill();
    }

    /** Home is already here: pressing it again changes nothing. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    /** There is nowhere further back than the home screen. */
    @Override
    public void onBackPressed() {
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
                view.setPadding(left, top, right, bottom);
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

        frame = new Frame(this, dp(24));

        FrameLayout stage = new FrameLayout(this);
        stage.setClipChildren(false);
        frame.addView(stage, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        frame.cut(stage);

        grid = new Grid(this, COLUMNS, ROWS);
        grid.setPadding(dp(8), dp(16), dp(8), dp(8));
        stage.addView(grid, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        /* The dock is a strip of the same stuff as the bar, risen just over
           it: five places, no names. */
        shelf = new FrameLayout(this);
        shelf.setBackground(Tone.box(Tone.container(), dp(30), dp(0.5f)));
        shelf.setClipChildren(false);
        dock = new Grid(this, DOCK, 1);
        dock.setPadding(dp(6), 0, dp(6), 0);
        shelf.addView(dock, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams shelfParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.round(iconSize + dp(26)));
        shelfParams.setMargins(dp(8), dp(8), dp(8), 0);
        frame.addView(shelf, shelfParams);

        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        bar.setPadding(dp(8), dp(14), dp(12), dp(14));

        /* The field will be the door to every application; the search is
           where one would start looking for one anyway. */
        field = new TextView(this);
        field.setText("");
        field.setHint(SEARCH);
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        field.setSingleLine(true);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(14), dp(10), dp(10), dp(10));
        field.setClickable(true);
        field.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refuse(v);
            }
        });
        bar.addView(field, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        blob = new Blob(this, dp(56));
        blob.setContentDescription(MENU);
        blob.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refuse(v);
            }
        });
        bar.addView(blob);

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.setMargins(dp(8), dp(6), dp(8), dp(10));
        frame.addView(bar, barParams);

        tint();
        setContentView(frame);
        fitBars(frame);
        still();
    }

    /** Everything that wears a colour, in the colours of the moment. */
    private void tint() {
        frame.tint();
        shelf.setBackground(Tone.box(Tone.container(), dp(30), dp(0.5f)));
        bar.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        field.setTextColor(Tone.onSurface());
        field.setHintTextColor(Tone.faint());
        field.setBackground(Tone.touch(null, dp(26)));
        blob.tint();
    }

    // ------------------------------------------------------------- fill

    /** The roles of the dock, in the order they stand. */
    private static Intent[] dockRoles() {
        return new Intent[] {
            new Intent(Intent.ACTION_DIAL),
            category(Intent.CATEGORY_APP_MESSAGING),
            new Intent(Intent.ACTION_VIEW, Uri.parse("https:")),
            new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            category(Intent.CATEGORY_APP_GALLERY)
        };
    }

    /** The two rows of the grid nearest the dock, left to right. */
    private static Intent[][] gridRoles() {
        return new Intent[][] {
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
     * Puts on the screen what the phone keeps for each role. A role with no
     * answer leaves its place empty: nothing is put there in its stead.
     */
    private void fill() {
        grid.removeAllViews();
        dock.removeAllViews();
        cells.clear();
        Apps found = new Apps(this);
        Set<String> taken = new HashSet<>();

        Intent[] docked = dockRoles();
        for (int i = 0; i < docked.length; i++) {
            place(dock, found.role(docked[i], taken), i, 0, false);
        }
        Intent[][] rows = gridRoles();
        int first = ROWS - rows.length;
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length; c++) {
                place(grid, found.role(rows[r][c], taken), c, first + r, true);
            }
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
        Rect from = new Rect(on[0], on[1], on[0] + on[2], on[1] + on[3]);
        Bundle grow = ActivityOptions.makeClipRevealAnimation(cell,
            on[0] - at[0], on[1] - at[1], on[2], on[3]).toBundle();
        try {
            ((LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE))
                .startMainActivity(cell.door.name, cell.door.user, from, grow);
        } catch (RuntimeException gone) {
            refuse(cell);
            later();
        }
    }

    /** A refusal is felt, not read: the phone's own short no. */
    private void refuse(View v) {
        v.performHapticFeedback(Build.VERSION.SDK_INT >= 30
            ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
    }

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
                if (cell.getParent() == grid && ((int[]) cell.getTag())[1] == row) {
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
        grid.setScaleX(0.94f);
        grid.setScaleY(0.94f);
        grid.setAlpha(0.6f);
        grid.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setStartDelay(0L).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).start();
    }
}
