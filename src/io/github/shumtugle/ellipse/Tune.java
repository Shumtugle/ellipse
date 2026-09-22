package io.github.shumtugle.ellipse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The settings, as a room the tiles are kept in.
 *
 * The room is dark and lit from above, and it is entered with the lamp
 * off: the light comes up, the cards rise into it one after another, the
 * seals of the subjects are struck into their places, and a band of light
 * runs once across the glass of the case at the top.
 *
 * The case holds four of the owner's own tiles, the first four of the
 * main home screen, standing on dark cloth inside a frame of their own
 * material, each with its shadow on the floor under it, and a plate below
 * them that names the look they wear. Every move of a dial redraws them,
 * so a tile is chosen by eye and not by number, though the number stands
 * beside every dial too.
 *
 * Under the case are the subjects, each a line with a seal: a small tile
 * of the owner's look with the subject's sign in its window, so the list
 * is made of the very thing it sets. A press on a line opens its subject
 * out of the seal: the glass of the new screen grows round from it, the
 * seal flies up to stand beside the subject's name, and the contents step
 * back out of focus. Back folds the screen into the seal it came from.
 *
 * Inside a subject, a choice between a few things is a group of cards
 * drawn with what they do. The chosen card is set, as a stone is set: a
 * rim of the material closes in around it and its face turns to glass. A
 * number is a cap on a groove; a count stands behind glass between two
 * coins, and rolls as it changes. The button most likely wanted is a plate
 * of the material with its word cut in; the others are glass.
 *
 * Change the material and all of it is cast again, and the light runs
 * across the case to show it.
 *
 * Nothing here reaches into the home screen. A setting is written down;
 * the home screen finds it changed when it is shown again, and builds
 * itself anew. The layout of the home screens and a language module go
 * out to a file and come back from one; at the bottom stands the way out,
 * the system's own choice of home screen, on a plate of its own.
 */
public final class Tune extends Activity {

    /** Named shapes: points on the two dials worth a name of their own. */
    private static final float[][] SHAPES = {
        {Tile.Look.MEASURED.power, Tile.Look.MEASURED.ratio},
        {4f, 1f},
        {2f, 1f},
        {16f, 1f},
    };

    /** The roundness dial runs from an ellipse at two to a near rectangle at twenty-four. */
    private static final float LEAST = 2f;
    private static final float MOST = 24f;
    /** The proportion dial runs from square to half as wide again as tall. */
    private static final float WIDEST = 1.5f;

    private static final int COLUMNS_LEAST = 4;
    private static final int COLUMNS_MOST = 7;
    private static final int ROWS_LEAST = 6;
    private static final int ROWS_MOST = 14;

    private static final int EXPORT = 1;
    private static final int IMPORT = 2;
    private static final int MODULE_LOAD = 3;
    private static final int MODULE_SAVE = 4;
    private static final int FOLDER = 5;

    /** A card's corners at rest, and chosen. */
    private static final float REST = 28f;
    private static final float CHOSEN = 16f;

    /** The size of a seal on a line of the contents, and at the head of a subject. */
    private static final float SEAL = 60f;
    private static final float SEAL_HEAD = 72f;

    /** Asked for by name, a subject opens at once, from a thing on a screen that belongs to it. */
    static final String SUBJECT = "subject";

    private final List<Runnable> painters = new ArrayList<Runnable>();
    private final List<Cards> groups = new ArrayList<Cards>();
    private final List<View> cards = new ArrayList<View>();
    /** Every seal on the screen, cast again whenever the look changes. */
    private final List<Cast.Seal> seals = new ArrayList<Cast.Seal>();
    /** The cases of tiles on show: the one in the contents, and the tile subject's own while it is open. */
    private final List<Showcase> cases = new ArrayList<Showcase>();
    private final Drawable[] faces = new Drawable[4];

    private Tile.Look look;
    private Layout grid;
    private FrameLayout root;
    private View sheet;
    private Cast.Room room;
    private Cast.Room pageRoom;
    private Showcase showcase;
    private Showcase pageCase;
    private Cards shapes;
    private Dial round;
    private Dial wide;
    private Dial close;
    private Dial thick;
    private TextView thickValue;
    private Dial hue;
    private Dial rich;
    private TextView roundValue;
    private TextView wideValue;
    private TextView closeValue;
    private TextView gridSaid;
    private TextView said;
    private TextView tongueSaid;
    /** Opened straight into a subject from a screen: going back from it goes back there. */
    private boolean direct;
    /** Told when a sheet is let go without a choice. */
    private Runnable sheetDropped;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Fault.watch(this);
        Round.measure(this);
        Tone.read(this);
        Words.load(this);
        Tile.materials(getResources());
        look = Keep.tile(this);
        grid = Layout.load(this);
        if (state == null) {
            // The room does its own arriving: the lamp comes up and the
            // cards rise into its light. The system's slide would only
            // bring in a room already lit.
            overridePendingTransition(0, 0);
        }

        // The room runs under the bars as well, so the light from the
        // ceiling is not cut off by a band of flat colour at the top.
        room = new Cast.Room();
        getWindow().setBackgroundDrawable(room);
        getWindow().setStatusBarColor(0x00000000);
        getWindow().setNavigationBarColor(0x00000000);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Round.dp(16f), Round.dp(36f), Round.dp(16f), Round.dp(48f));

        TextView title = words(Letter.DISPLAY_M, Words.s("settings"), Tone.ON_SURFACE);
        Letter.serif(title);
        title.setPadding(Round.dp(8f), 0, Round.dp(8f), 0);
        column.addView(title);
        cards.add(title);

        said = words(Letter.BODY_M, "", Tone.PRIMARY);
        tongueSaid = words(Letter.BODY_M, "", Tone.PRIMARY);
        showcase = new Showcase();
        cases.add(showcase);
        column.addView(showcase.view, spaced(20));
        cards.add(showcase.view);
        View choice = defaultSwitch();
        column.addView(choice, spaced(20));
        cards.add(choice);
        contents = new LinearLayout(this);
        contents.setOrientation(LinearLayout.VERTICAL);
        column.addView(contents, spaced(20));
        listSections();
        column.addView(way(), spaced(20));

        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception unknown) {
            version = "";
        }
        // The maker's mark, as small and as spaced as a hallmark.
        TextView made = words(Letter.LABEL_M, (getString(R.string.app_name) + "  \u00b7  " + version)
            .toUpperCase(java.util.Locale.ROOT), Tone.OUTLINE);
        Letter.serif(made);
        made.setLetterSpacing(0.3f);
        made.setGravity(Gravity.CENTER);
        column.addView(made, spaced(32));
        cards.add(made);

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(column);
        index = scroll;
        indexColumn = column;
        root = new FrameLayout(this);
        root.addView(scroll, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        paint();
        int reopen = state == null ? -1 : state.getInt("section", -1);
        direct = state != null && state.getBoolean("direct", false);
        if (state == null && getIntent().getStringExtra(SUBJECT) != null) {
            // Come straight from a thing on a screen: its subject opens at
            // once and rises into the room, and going back goes back there.
            reopen = subjectOf(getIntent().getStringExtra(SUBJECT));
            direct = reopen >= 0;
        }
        if (reopen >= 0) {
            openSection(reopen, false);
            if (direct && state == null) {
                page.setAlpha(0f);
                page.setTranslationY(Round.px(28f));
                page.animate().alpha(1f).translationY(0f).setDuration(Pace.ARRIVE)
                    .setInterpolator(Pace.EMPHASIS).start();
            }
        } else {
            arrive();
        }
        sample();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("section", section);
        out.putBoolean("direct", direct);
    }

    private static int subjectOf(String name) {
        for (int i = 0; i < SUBJECTS.length; i++) {
            if (SUBJECTS[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------ the contents

    /**
     * The settings are split by what they are about, each subject a screen
     * of its own; the first screen is their contents. Each line of the
     * contents says, under the subject's name, how it stands now, so most
     * questions are answered without going in.
     */
    private static final int[] GLYPHS = {Sketch.WINDOW_FOLLOWS, Sketch.SCREENS, Sketch.CLOCK, Sketch.DRAWER,
        Sketch.PALETTE, Sketch.WEATHER, Sketch.FOLDER_OPEN, Sketch.LANGUAGE};
    private static final String[] SUBJECTS = {"tile", "screens", "clock", "drawer", "colour", "weather", "files",
        "language"};

    private ScrollView index;
    private LinearLayout indexColumn;
    private LinearLayout contents;
    private ScrollView page;
    private FrameLayout headSeal;
    private int section = -1;
    /** The seal of each line of the contents, which its subject opens out of and folds back into. */
    private final View[] lineSeals = new View[SUBJECTS.length];
    private final TextView[] standings = new TextView[SUBJECTS.length];
    /** How many painters, groups and seals the contents own; a subject's are those after. */
    private int ownPainters;
    private int ownGroups;
    private int ownSeals;

    private void listSections() {
        contents.removeAllViews();
        for (int i = 0; i < SUBJECTS.length; i++) {
            final int which = i;
            final LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(Round.dp(12f), Round.dp(12f), Round.dp(18f), Round.dp(12f));
            FrameLayout seal = seal(GLYPHS[i], SEAL);
            lineSeals[i] = seal;
            line.addView(seal, new LinearLayout.LayoutParams(Round.dp(SEAL), Round.dp(SEAL)));
            LinearLayout both = new LinearLayout(this);
            both.setOrientation(LinearLayout.VERTICAL);
            TextView name = words(Letter.TITLE_L, Words.s(SUBJECTS[i]), Tone.ON_SURFACE);
            Letter.serif(name);
            both.addView(name);
            TextView now = words(Letter.BODY_M, standing(i), Tone.ON_SURFACE_VARIANT);
            now.setSingleLine(true);
            now.setEllipsize(TextUtils.TruncateAt.END);
            standings[i] = now;
            LinearLayout.LayoutParams nowPlace = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nowPlace.topMargin = Round.dp(2f);
            both.addView(now, nowPlace);
            LinearLayout.LayoutParams bothPlace = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            bothPlace.leftMargin = Round.dp(16f);
            line.addView(both, bothPlace);
            final Sketch on = new Sketch(this, Sketch.FORWARD);
            line.addView(on, new LinearLayout.LayoutParams(Round.dp(20f), Round.dp(20f)));
            painters.add(new Runnable() {
                public void run() {
                    line.setBackground(touch(new Cast.Slab(Tone.of(Tone.SURFACE_CONTAINER), 26f),
                        Tone.of(Tone.ON_SURFACE), 26f));
                    on.ink(Tone.of(Tone.ON_SURFACE_VARIANT), 0);
                }
            });
            line.setStateListAnimator(give());
            line.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openSection(which, true);
                }
            });
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            place.topMargin = i == 0 ? 0 : Round.dp(8f);
            contents.addView(line, place);
            cards.add(line);
        }
    }

    /**
     * My own, or the default. The default is the home screen as the
     * design system's own guidelines have it, and as a new phone first
     * sees it. Going to it keeps what was mine, whole, to come back to.
     */
    private View defaultSwitch() {
        boolean standard = Keep.onDefault(this);
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        Cards two = new Cards(new String[] {Words.s("look_mine"), Words.s("look_default")},
            new Sketch[] {new Sketch(this, Sketch.SHAPE).shape(Tile.Look.MEASURED.power, Tile.Look.MEASURED.ratio),
                new Sketch(this, Sketch.WINDOW_RAW)},
            standard ? 1 : 0, 112, new Picked() {
                public void picked(int which) {
                    if ((which == 1) != Keep.onDefault(Tune.this)) {
                        turnDefault(which == 1);
                    }
                }
            });
        made.addView(two.view());
        if (standard && (grid.columns != Keep.DEFAULT_COLUMNS || grid.rows != Keep.DEFAULT_ROWS)) {
            TextView kept = words(Letter.BODY_M, Words.s("grid_kept").replace("{c}", String.valueOf(grid.columns))
                .replace("{r}", String.valueOf(grid.rows)), Tone.ON_SURFACE_VARIANT);
            kept.setPadding(Round.dp(8f), 0, Round.dp(8f), 0);
            made.addView(kept, spaced(10));
        }
        return made;
    }

    /**
     * To the default, keeping mine; or back to mine. The grid follows only
     * if everything on the screens still fits in it. Then the settings are
     * read again from what is kept, and the room lights anew in its look.
     */
    private void turnDefault(boolean standard) {
        try {
            if (standard) {
                org.json.JSONObject mine = Keep.export(this);
                mine.put("columns", grid.columns);
                mine.put("rows", grid.rows);
                Keep.toDefault(this, mine);
                fitGrid(Keep.DEFAULT_COLUMNS, Keep.DEFAULT_ROWS);
            } else {
                org.json.JSONObject mine = Keep.mine(this);
                Keep.toMine(this);
                if (mine != null) {
                    fitGrid(mine.optInt("columns", grid.columns), mine.optInt("rows", grid.rows));
                }
            }
        } catch (org.json.JSONException broken) {
            root.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
            return;
        }
        root.postDelayed(new Runnable() {
            public void run() {
                recreate();
            }
        }, Pace.ARRIVE);
    }

    /** The grid made so large, if everything on the screens still fits in it. */
    private void fitGrid(int columns, int rows) {
        int reach = 0;
        int depth = 0;
        for (Layout.Screen screen : grid.screens) {
            for (Layout.Item item : screen.items) {
                reach = Math.max(reach, item.x + item.w);
                depth = Math.max(depth, item.y + item.h);
            }
        }
        if (columns >= reach && rows >= depth && columns > 0 && rows > 0) {
            grid.columns = columns;
            grid.rows = rows;
            grid.save(this);
            Keep.touch(this);
        }
    }

    /** The lines of the contents told again how their subjects stand. */
    private void restate() {
        for (int i = 0; i < standings.length; i++) {
            if (standings[i] != null) {
                standings[i].setText(standing(i));
            }
        }
    }

    /**
     * A seal: a small tile of the owner's look, with a subject's sign in
     * its window, in the colour the material gives what shows through
     * glass.
     */
    private FrameLayout seal(int glyph, float size) {
        final FrameLayout made = new FrameLayout(this);
        final Cast.Seal back = new Cast.Seal(Cast.Seal.BACK);
        final Cast.Seal front = new Cast.Seal(Cast.Seal.FRONT);
        seals.add(back);
        seals.add(front);
        made.setBackground(back);
        made.setForeground(front);
        final Sketch mark = new Sketch(this, glyph);
        int side = Round.dp(size * 0.42f);
        made.addView(mark, new FrameLayout.LayoutParams(side, side, Gravity.CENTER));
        painters.add(new Runnable() {
            public void run() {
                int kind = Cast.metal(look.rim);
                int glow = Cast.glow(kind);
                back.recast(look);
                front.recast(look);
                mark.ink(glow, (glow & 0x00FFFFFF) | 0x73000000);
            }
        });
        return made;
    }

    /** How a subject stands now, in a few words. */
    private String standing(int which) {
        switch (which) {
            case 0: {
                int shape = shapeOf(look);
                String[] rims = {"rim_metal", "rim_gold", "rim_accent", "rim_bare", "rim_wood", "rim_bling",
                    "rim_black", "rim_white"};
                String name = shape >= 0 ? shapeNames()[shape] : Words.s("tile");
                if (look.window == Tile.Look.RAW) {
                    return name + "  \u00b7  " + Words.s("window_raw");
                }
                return name + "  \u00b7  " + Words.s(rims[Math.max(0, Math.min(rims.length - 1, look.rim))])
                    + (look.window == Tile.Look.MEDALLION ? "  \u00b7  " + Words.s("window_round") : "");
            }
            case 1:
                return grid.columns + " \u00d7 " + grid.rows + "  \u00b7  "
                    + Words.s(Keep.endless(this) ? "turn_round" : "turn_ends");
            case 2:
                return clockStanding();
            case 3: {
                String[] orders = {"order_name", "order_installed", "order_updated"};
                return Words.s(Keep.across(this) ? "way_across" : "way_down") + "  \u00b7  "
                    + Words.s(orders[Math.max(0, Math.min(2, Keep.order(this)))]);
            }
            case 4:
                return Keep.wall(this) ? Words.s("wallpaper") : Words.s("hue");
            case 5: {
                if (Sky.here(this) && Sky.mayLocate(this)) {
                    return Words.s("here") + (Sky.city(this) != null ? "  \u00b7  " + Sky.city(this) : "");
                }
                return Sky.city(this) != null ? Sky.city(this) : Words.s("city_none");
            }
            case 6: {
                String place = folderName();
                return place != null ? place : Words.s("folder_none");
            }
            default:
                return Words.active() ? Words.name() : Words.s("english");
        }
    }

    private View build(int which) {
        switch (which) {
            case 0: return tileCard();
            case 1: return screensCard();
            case 2: return clockCard();
            case 3: return drawerCard();
            case 4: return colourCard();
            case 5: return weatherCard();
            case 6: return filesCard();
            default: return languageCard();
        }
    }

    /**
     * A subject opens as a screen of its own, out of the seal that was
     * pressed. Its name stands at its head beside the same seal, larger,
     * and a key back; the tile subject has a case of tiles of its own, so
     * what is changed can be seen changing.
     *
     * Whatever the subject adds to the painters, the groups and the seals
     * is its own, and is let go when it closes; the contents keep only
     * what they had.
     */
    private void openSection(int which, boolean animate) {
        if (page != null) {
            return;
        }
        section = which;
        ownPainters = painters.size();
        ownGroups = groups.size();
        ownSeals = seals.size();
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Round.dp(16f), Round.dp(28f), Round.dp(16f), Round.dp(48f));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        final FrameLayout back = new FrameLayout(this);
        final Sketch arrow = new Sketch(this, Sketch.BACK);
        back.addView(arrow, new FrameLayout.LayoutParams(Round.dp(24f), Round.dp(24f), Gravity.CENTER));
        back.setContentDescription(Words.s("back"));
        back.setStateListAnimator(give());
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeSection();
            }
        });
        painters.add(new Runnable() {
            public void run() {
                back.setBackground(touch(new Cast.Pane(Round.FULL), Tone.of(Tone.ON_SURFACE), Round.FULL));
                arrow.ink(Tone.of(Tone.ON_SURFACE), 0);
            }
        });
        head.addView(back, new LinearLayout.LayoutParams(Round.dp(48f), Round.dp(48f)));
        headSeal = seal(GLYPHS[which], SEAL_HEAD);
        LinearLayout.LayoutParams sealPlace = new LinearLayout.LayoutParams(Round.dp(SEAL_HEAD),
            Round.dp(SEAL_HEAD));
        sealPlace.leftMargin = Round.dp(12f);
        head.addView(headSeal, sealPlace);
        TextView title = words(Letter.DISPLAY_S, Words.s(SUBJECTS[which]), Tone.ON_SURFACE);
        Letter.fit(title, new int[] {Letter.DISPLAY_S, Letter.HEADLINE_L, Letter.HEADLINE_M}, true);
        LinearLayout.LayoutParams titlePlace = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titlePlace.leftMargin = Round.dp(16f);
        head.addView(title, titlePlace);
        column.addView(head);

        List<View> arriving = new ArrayList<View>();
        if (which == 0) {
            pageCase = new Showcase();
            cases.add(pageCase);
            column.addView(pageCase.view, spaced(20));
            arriving.add(pageCase.view);
        }
        View body = build(which);
        if (body instanceof ViewGroup && ((ViewGroup) body).getChildCount() > 0) {
            // The card's own heading says what the head of the screen already says.
            ((ViewGroup) body).removeViewAt(0);
        }
        column.addView(body, spaced(16));
        // The card lies there at once; what is on it rises into place.
        if (body instanceof ViewGroup) {
            ViewGroup parts = (ViewGroup) body;
            for (int i = 0; i < parts.getChildCount(); i++) {
                arriving.add(parts.getChildAt(i));
            }
        }

        page = new ScrollView(this);
        page.setOverScrollMode(View.OVER_SCROLL_NEVER);
        page.setVerticalScrollBarEnabled(false);
        page.addView(column);
        page.setClickable(true);
        pageRoom = new Cast.Room();
        page.setBackground(pageRoom);
        final ScrollView placed = page;
        page.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                // The piece of the room under the subject meets the piece behind the bars.
                int[] at = new int[2];
                v.getLocationInWindow(at);
                if (placed == page && pageRoom != null) {
                    pageRoom.place(at[1], getWindow().getDecorView().getHeight());
                }
            }
        });
        root.addView(page, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        paint();
        if (pageCase != null) {
            pageCase.borrow(showcase);
        }
        if (animate) {
            grow(lineSeals[which], arriving);
        } else {
            index.setScaleX(0.92f);
            index.setScaleY(0.92f);
            index.setAlpha(0f);
        }
    }

    /**
     * The subject grows out of its seal. The glass of the new screen opens
     * round from the seal's middle, starting no larger than the seal, so
     * for an instant the seal is the screen; the seal itself goes up to its
     * place beside the name; the contents behind step back, darken and,
     * where the system can, go out of focus. What the subject holds rises
     * into place a moment after, one piece after another.
     */
    private void grow(final View pressed, final List<View> arriving) {
        final ScrollView opening = page;
        opening.setAlpha(0f);
        for (int i = 0; i < arriving.size(); i++) {
            View part = arriving.get(i);
            part.setAlpha(0f);
            part.setTranslationY(Round.px(28f));
        }
        opening.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                opening.getViewTreeObserver().removeOnPreDrawListener(this);
                int[] from = new int[2];
                int[] to = new int[2];
                int[] base = new int[2];
                pressed.getLocationInWindow(from);
                headSeal.getLocationInWindow(to);
                opening.getLocationInWindow(base);
                float fromSide = Math.max(1f, pressed.getWidth());
                float scale = fromSide / Math.max(1f, headSeal.getWidth());
                headSeal.setPivotX(0f);
                headSeal.setPivotY(0f);
                headSeal.setTranslationX(from[0] - to[0]);
                headSeal.setTranslationY(from[1] - to[1]);
                headSeal.setScaleX(scale);
                headSeal.setScaleY(scale);
                headSeal.animate().translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();

                float cx = from[0] - base[0] + fromSide / 2f;
                float cy = from[1] - base[1] + pressed.getHeight() / 2f;
                float far = (float) Math.hypot(Math.max(cx, opening.getWidth() - cx),
                    Math.max(cy, opening.getHeight() - cy));
                opening.setAlpha(1f);
                Animator reveal = ViewAnimationUtils.createCircularReveal(opening, Math.round(cx),
                    Math.round(cy), fromSide * 0.45f, far);
                reveal.setDuration(Pace.ARRIVE + 80L);
                reveal.setInterpolator(Pace.EMPHASIS);
                reveal.start();
                recede(true);
                for (int i = 0; i < arriving.size(); i++) {
                    arriving.get(i).animate().alpha(1f).translationY(0f)
                        .setStartDelay(140L + Math.min(i, 12) * 40L).setDuration(Pace.ARRIVE)
                        .setInterpolator(Pace.EMPHASIS).start();
                }
                if (pageCase != null) {
                    opening.postDelayed(new Runnable() {
                        public void run() {
                            if (pageCase != null) {
                                pageCase.front.sweep();
                            }
                        }
                    }, 420L);
                }
                return true;
            }
        });
    }

    private ValueAnimator depth;

    /** The contents step back behind an open subject, or come forward again; one movement at a time. */
    private void recede(final boolean away) {
        if (depth != null) {
            depth.cancel();
        }
        depth = ValueAnimator.ofFloat(away ? 0f : 1f, away ? 1f : 0f);
        depth.setDuration(away ? Pace.ARRIVE : Pace.GROW);
        depth.setInterpolator(Pace.EMPHASIS);
        depth.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                float d = (Float) a.getAnimatedValue();
                index.setScaleX(1f - 0.08f * d);
                index.setScaleY(1f - 0.08f * d);
                index.setAlpha(1f - 0.65f * d);
                if (Build.VERSION.SDK_INT >= 31) {
                    float blur = Round.px(18f) * d;
                    index.setRenderEffect(blur < 0.5f ? null : android.graphics.RenderEffect
                        .createBlurEffect(blur, blur, Shader.TileMode.CLAMP));
                }
            }
        });
        depth.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (away && page != null) {
                    // Hidden whole behind the subject: nothing to draw, nothing to blur.
                    index.setAlpha(0f);
                    if (Build.VERSION.SDK_INT >= 31) {
                        index.setRenderEffect(null);
                    }
                }
            }
        });
        depth.start();
    }

    /**
     * Back to the contents: the subject folds into the seal it came from.
     * The seal at its head goes down to where the line's seal stands, the
     * glass closes round onto it, and the contents come forward and into
     * focus behind.
     */
    private void closeSection() {
        if (page == null) {
            return;
        }
        if (direct) {
            finish();
            return;
        }
        final ScrollView leaving = page;
        final int was = section;
        final FrameLayout flying = headSeal;
        page = null;
        section = -1;
        headSeal = null;

        // What the subject brought is let go now; its screen is only a picture from here on.
        painters.subList(ownPainters, painters.size()).clear();
        groups.subList(ownGroups, groups.size()).clear();
        seals.subList(ownSeals, seals.size()).clear();
        if (pageCase != null) {
            cases.remove(pageCase);
            pageCase = null;
        }
        pageRoom = null;
        round = null;
        wide = null;
        close = null;
        thick = null;
        hue = null;
        rich = null;
        roundValue = null;
        wideValue = null;
        closeValue = null;
        thickValue = null;
        shapes = null;

        restate();
        paint();

        // Where the line's seal will stand once the contents are at full size again.
        View target = lineSeals[was];
        index.setScaleX(1f);
        index.setScaleY(1f);
        int[] to = new int[2];
        target.getLocationInWindow(to);
        index.setScaleX(0.92f);
        index.setScaleY(0.92f);
        int[] from = new int[2];
        int[] base = new int[2];
        flying.getLocationInWindow(from);
        leaving.getLocationInWindow(base);
        float side = Math.max(1f, target.getWidth());
        float scale = side / Math.max(1f, flying.getWidth() * flying.getScaleX());
        flying.animate().cancel();
        flying.setPivotX(0f);
        flying.setPivotY(0f);
        flying.animate().translationX(flying.getTranslationX() + to[0] - from[0])
            .translationY(flying.getTranslationY() + to[1] - from[1])
            .scaleX(flying.getScaleX() * scale).scaleY(flying.getScaleY() * scale)
            .setDuration(Pace.GROW).setInterpolator(Pace.STANDARD).start();

        float cx = to[0] - base[0] + side / 2f;
        float cy = to[1] - base[1] + target.getHeight() / 2f;
        float far = (float) Math.hypot(Math.max(cx, leaving.getWidth() - cx),
            Math.max(cy, leaving.getHeight() - cy));
        if (leaving.isAttachedToWindow()) {
            Animator fold = ViewAnimationUtils.createCircularReveal(leaving, Math.round(cx), Math.round(cy),
                far, side * 0.45f);
            fold.setDuration(Pace.GROW);
            fold.setInterpolator(Pace.STANDARD);
            fold.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator a) {
                    root.removeView(leaving);
                }
            });
            fold.start();
        } else {
            root.removeView(leaving);
        }
        index.setAlpha(0.35f);
        recede(false);
        // The line's own seal answers the one coming home, a beat after it lands.
        target.animate().cancel();
        target.setScaleX(1f);
        target.setScaleY(1f);
        target.animate().scaleX(1.12f).scaleY(1.12f).setStartDelay(Pace.GROW - 60L).setDuration(Pace.PRESS)
            .setInterpolator(Pace.STANDARD).withEndAction(new Runnable() {
                public void run() {
                    lineSeals[was].animate().scaleX(1f).scaleY(1f).setStartDelay(0L).setDuration(Pace.GROW)
                        .setInterpolator(new OvershootInterpolator(3f)).start();
                }
            }).start();
    }

    /**
     * The room is entered with the lamp off. The light comes up; the cards
     * rise into it one after another; each seal is struck into its place,
     * coming down large and settling with a little give; the tiles in the
     * case rise last, and a band of light runs once across its glass.
     */
    private void arrive() {
        room.light(0f);
        ValueAnimator lamp = ValueAnimator.ofFloat(0f, 1f);
        lamp.setDuration(900L);
        lamp.setInterpolator(Pace.STANDARD);
        lamp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                room.light((Float) a.getAnimatedValue());
            }
        });
        lamp.start();
        long begin = 120L;
        for (int i = 0; i < cards.size(); i++) {
            View card = cards.get(i);
            card.setAlpha(0f);
            card.setTranslationY(Round.px(28f));
            card.animate().alpha(1f).translationY(0f).setStartDelay(begin + i * Pace.STAGGER)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
        OvershootInterpolator strike = new OvershootInterpolator(2.2f);
        int first = cards.indexOf(contents.getChildAt(0));
        for (int i = 0; i < lineSeals.length; i++) {
            View seal = lineSeals[i];
            seal.setAlpha(0f);
            seal.setScaleX(1.6f);
            seal.setScaleY(1.6f);
            seal.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(begin + (Math.max(0, first) + i) * Pace.STAGGER + 160L)
                .setDuration(Pace.GROW).setInterpolator(strike).start();
        }
        for (int i = 0; i < showcase.tiles.length; i++) {
            ImageView tile = showcase.tiles[i];
            tile.setAlpha(0f);
            tile.setTranslationY(Round.px(18f));
            tile.animate().alpha(1f).translationY(0f).setStartDelay(begin + 220L + i * 70L)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
        root.postDelayed(new Runnable() {
            public void run() {
                showcase.front.sweep();
            }
        }, 760L);
    }

    // ------------------------------------------------------------ the tiles

    /**
     * A case of four tiles under glass, two by two, and the plate below
     * them with the name of the look they wear. The case, its plate and
     * its frame are cast in the material with everything else.
     */
    private final class Showcase {

        final FrameLayout view;
        final ImageView[] tiles = new ImageView[4];
        final TextView label;
        final Cast.Front front;

        Showcase() {
            view = new FrameLayout(Tune.this);
            LinearLayout inside = new LinearLayout(Tune.this);
            inside.setOrientation(LinearLayout.VERTICAL);
            inside.setPadding(Round.dp(22f), Round.dp(30f), Round.dp(22f), Round.dp(24f));
            for (int r = 0; r < 2; r++) {
                LinearLayout row = new LinearLayout(Tune.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                for (int c = 0; c < 2; c++) {
                    ImageView tile = new ImageView(Tune.this);
                    tile.setScaleType(ImageView.ScaleType.CENTER);
                    // As tall as a tile will be before there is one, so the
                    // case does not grow under the reader when the icons come.
                    tile.setMinimumHeight(stoodHeight());
                    tiles[r * 2 + c] = tile;
                    row.addView(tile, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                }
                inside.addView(row, spaced(r == 0 ? 0 : 4));
            }
            label = Letter.set(new TextView(Tune.this), Letter.LABEL_L);
            Letter.serif(label);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(Round.dp(20f), Round.dp(8f), Round.dp(20f), Round.dp(8f));
            LinearLayout.LayoutParams plate = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plate.gravity = Gravity.CENTER_HORIZONTAL;
            plate.topMargin = Round.dp(14f);
            inside.addView(label, plate);
            view.addView(inside, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            front = new Cast.Front(34f);
            view.setForeground(front);
            painters.add(new Runnable() {
                public void run() {
                    int kind = Cast.metal(look.rim);
                    view.setBackground(new Cast.Case(kind,
                        Tone.at(7f, 3.0 + 6.0 * Tone.rich(), Tone.hue()),
                        Tone.at(24f, 6.0 + 12.0 * Tone.rich(), Tone.hue())));
                    label.setBackground(new Cast.Plate(kind, Round.FULL, look.gloss));
                    Cast.engrave(label, kind);
                }
            });
        }

        /** The tiles and the plate of another case, as they stand. */
        void borrow(Showcase other) {
            for (int i = 0; i < tiles.length; i++) {
                tiles[i].setImageDrawable(other.tiles[i].getDrawable());
                tiles[i].setMinimumHeight(other.tiles[i].getMinimumHeight());
            }
            label.setText(other.label.getText());
        }
    }

    /** Light runs across the glass of every case: the look has just been cast anew. */
    private void sheen() {
        for (Showcase shown : cases) {
            shown.front.sweep();
        }
    }

    /**
     * The four tiles: the first four applications of the main home screen,
     * so the look is judged on the owner's own; the first of the drawer if
     * the screen has fewer.
     */
    private void sample() {
        new Thread(new Runnable() {
            public void run() {
                List<App> apps = App.all(Tune.this);
                Map<String, App> known = new HashMap<String, App>();
                for (App app : apps) {
                    known.put(app.component().flattenToShortString(), app);
                }
                List<App> chosen = new ArrayList<App>();
                if (!grid.screens.isEmpty()) {
                    List<Layout.Item> items = new ArrayList<Layout.Item>(
                        grid.screens.get(Math.min(grid.home, grid.screens.size() - 1)).items);
                    java.util.Collections.sort(items, new java.util.Comparator<Layout.Item>() {
                        public int compare(Layout.Item a, Layout.Item b) {
                            return a.y != b.y ? a.y - b.y : a.x - b.x;
                        }
                    });
                    for (Layout.Item item : items) {
                        App app = Layout.APP.equals(item.kind) ? known.get(item.component) : null;
                        if (app != null && chosen.size() < faces.length) {
                            chosen.add(app);
                        }
                    }
                }
                for (int i = 0; chosen.size() < faces.length && i < apps.size(); i++) {
                    if (!chosen.contains(apps.get(i))) {
                        chosen.add(apps.get(i));
                    }
                }
                int dpi = App.dpi(Tune.this);
                for (int i = 0; i < faces.length && i < chosen.size(); i++) {
                    faces[i] = chosen.get(i).icon(dpi);
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        preview();
                    }
                });
            }
        }).start();
    }

    /**
     * The tiles drawn again, once, and handed to every case on show: four
     * tiles are a few milliseconds, so this is done while the finger moves.
     * The seals are told the look as well; one that is not on the screen
     * works out its new shape only when it is next drawn.
     */
    private void preview() {
        int tile = tileSide();
        Bitmap[] stood = new Bitmap[faces.length];
        for (int i = 0; i < faces.length; i++) {
            stood[i] = stand(Tile.render(faces[i], tile, look));
        }
        int shape = shapeOf(look);
        String name = shape >= 0 ? shapeNames()[shape] : Words.s("tile");
        String text = name + "  \u00b7  " + number(look.ratio, 2) + "  \u00b7  \u00d7" + number(look.zoom, 2);
        for (Showcase shown : cases) {
            for (int i = 0; i < shown.tiles.length; i++) {
                shown.tiles[i].setImageBitmap(stood[i]);
                shown.tiles[i].setMinimumHeight(stood[i].getHeight());
            }
            shown.label.setText(text);
        }
        if (roundValue != null) {
            roundValue.setText(number(look.power, 1));
            wideValue.setText(number(look.ratio, 2));
            closeValue.setText("\u00d7" + number(look.zoom, 2));
            if (thickValue != null) {
                thickValue.setText(Math.round(look.width * 100f) + "%");
            }
        }
        for (Cast.Seal seal : seals) {
            seal.cast(look);
        }
    }

    /** The width of a tile in the case: two to a row, with air around them. */
    private int tileSide() {
        int width = getResources().getDisplayMetrics().widthPixels;
        return Math.round((width - Round.dp(76f)) / 2f * 0.84f);
    }

    /** How tall a tile stands in the case in the present look, its shadow included. */
    private int stoodHeight() {
        int h = Tile.height(tileSide(), look);
        return h + Math.round(h * 0.18f);
    }

    /**
     * A tile stood on the cloth of the case: under its foot, a soft pool of
     * the shadow it throws, as a thing lit from above throws it.
     */
    private static Bitmap stand(Bitmap tile) {
        int w = tile.getWidth();
        int h = tile.getHeight();
        int foot = Math.round(h * 0.18f);
        Bitmap stood = Bitmap.createBitmap(w, h + foot, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(stood);
        float cx = w / 2f;
        float cy = h + foot * 0.05f;
        float reach = w * 0.44f;
        Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        shade.setShader(new RadialGradient(cx, cy, reach, new int[] {0xA6000000, 0x40000000, 0x00000000},
            new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.save();
        canvas.scale(1f, 0.2f, cx, cy);
        canvas.drawCircle(cx, cy, reach, shade);
        canvas.restore();
        canvas.drawBitmap(tile, 0f, 0f, null);
        tile.recycle();
        return stood;
    }

    private static String number(float value, int places) {
        return String.format(java.util.Locale.ROOT, "%." + places + "f", value);
    }

    private String[] shapeNames() {
        return new String[] {Words.s("shape_superellipse"), Words.s("shape_squircle"),
            Words.s("shape_circle"), Words.s("shape_square")};
    }

    // ------------------------------------------------------------ the tile card

    private View tileCard() {
        LinearLayout card = card(Words.s("tile"));

        Sketch[] silhouettes = new Sketch[SHAPES.length];
        for (int i = 0; i < SHAPES.length; i++) {
            silhouettes[i] = new Sketch(this, Sketch.SHAPE).shape(SHAPES[i][0], SHAPES[i][1]);
        }
        shapes = new Cards(shapeNames(), silhouettes, shapeOf(look), 112, new Picked() {
            public void picked(int which) {
                look = look.with(SHAPES[which][0], SHAPES[which][1]);
                round.value(roundOf(look.power));
                wide.value(wideOf(look.ratio));
                keepTile();
            }
        });
        card.addView(shapes.view(), spaced(16));

        roundValue = value();
        card.addView(labelled(Words.s("roundness"), roundValue), spaced(24));
        round = new Dial(this, roundOf(look.power), new Dial.Moved() {
            public void moved(float value, boolean done) {
                look = look.with(powerOf(value), look.ratio);
                shapes.select(shapeOf(look));
                preview();
                if (done) {
                    keepTile();
                }
            }
        }).large();
        card.addView(round, wideRow());

        wideValue = value();
        card.addView(labelled(Words.s("proportion"), wideValue), spaced(12));
        wide = new Dial(this, wideOf(look.ratio), new Dial.Moved() {
            public void moved(float value, boolean done) {
                look = look.with(look.power, ratioOf(value));
                shapes.select(shapeOf(look));
                preview();
                if (done) {
                    keepTile();
                }
            }
        }).large();
        card.addView(wide, wideRow());

        closeValue = value();
        card.addView(labelled(Words.s("zoom"), closeValue), spaced(12));
        close = new Dial(this, zoomOf(look.zoom), new Dial.Moved() {
            public void moved(float value, boolean done) {
                look = look.zoom(closerOf(value));
                preview();
                if (done) {
                    keepTile();
                }
            }
        }).large();
        card.addView(close, wideRow());

        card.addView(words(Letter.TITLE_S, Words.s("rim"), Tone.ON_SURFACE_VARIANT), spaced(24));
        final int[] order = {Tile.Look.METAL, Tile.Look.GOLD, Tile.Look.WOOD, Tile.Look.BLING,
            Tile.Look.BLACK, Tile.Look.WHITE, Tile.Look.ACCENT, Tile.Look.BARE};
        String[] rimNames = {Words.s("rim_metal"), Words.s("rim_gold"), Words.s("rim_wood"),
            Words.s("rim_bling"), Words.s("rim_black"), Words.s("rim_white"), Words.s("rim_accent"),
            Words.s("rim_bare")};
        Sketch[] discs = new Sketch[order.length];
        int at = 0;
        for (int i = 0; i < order.length; i++) {
            discs[i] = new Sketch(this, Sketch.RIM).rim(order[i]);
            if (order[i] == look.rim) {
                at = i;
            }
        }
        Cards rims = new Cards(rimNames, discs, at, 104, 4, new Picked() {
            public void picked(int which) {
                look = look.rim(order[which]);
                keepTile();
                recast();
            }
        });
        card.addView(rims.view(), spaced(12));

        thickValue = value();
        card.addView(labelled(Words.s("rim_width"), thickValue), spaced(20));
        thick = new Dial(this, (look.width - Tile.Look.THINNEST)
            / (Tile.Look.THICKEST - Tile.Look.THINNEST), new Dial.Moved() {
                public void moved(float value, boolean done) {
                    look = look.width(Tile.Look.THINNEST + value * (Tile.Look.THICKEST - Tile.Look.THINNEST));
                    preview();
                    if (done) {
                        keepTile();
                    }
                }
            }).large();
        card.addView(thick, wideRow());

        card.addView(words(Letter.TITLE_S, Words.s("window"), Tone.ON_SURFACE_VARIANT), spaced(20));
        Cards windows = new Cards(new String[] {Words.s("window_follows"), Words.s("window_round"),
                Words.s("window_raw")},
            new Sketch[] {new Sketch(this, Sketch.WINDOW_FOLLOWS), new Sketch(this, Sketch.WINDOW_ROUND),
                new Sketch(this, Sketch.WINDOW_RAW)},
            look.window, 120, new Picked() {
                public void picked(int which) {
                    look = look.window(which);
                    keepTile();
                    recast();
                }
            });
        card.addView(windows.view(), spaced(12));

        card.addView(words(Letter.TITLE_S, Words.s("light"), Tone.ON_SURFACE_VARIANT), spaced(20));
        Cards lights = new Cards(new String[] {Words.s("light_matte"), Words.s("light_gloss")},
            new Sketch[] {new Sketch(this, Sketch.MATTE), new Sketch(this, Sketch.GLOSS)},
            look.gloss ? 1 : 0, 120, new Picked() {
                public void picked(int which) {
                    look = look.gloss(which == 1);
                    keepTile();
                    recast();
                }
            });
        card.addView(lights.view(), spaced(12));
        return card;
    }

    private void keepTile() {
        Keep.saveTile(this, look);
        preview();
    }

    private static float roundOf(float power) {
        return (float) (Math.log(power / LEAST) / Math.log(MOST / LEAST));
    }

    private static float powerOf(float value) {
        return (float) (LEAST * Math.pow(MOST / LEAST, value));
    }

    private static float wideOf(float ratio) {
        return (ratio - 1f) / (WIDEST - 1f);
    }

    private static float ratioOf(float value) {
        return 1f + value * (WIDEST - 1f);
    }

    private static float zoomOf(float zoom) {
        return (zoom - 1f) / (Tile.Look.CLOSEST - 1f);
    }

    private static float closerOf(float value) {
        return 1f + value * (Tile.Look.CLOSEST - 1f);
    }

    /** Which named shape the look is, or none. */
    private static int shapeOf(Tile.Look look) {
        for (int i = 0; i < SHAPES.length; i++) {
            if (Math.abs(look.power - SHAPES[i][0]) < 0.15f
                && Math.abs(look.ratio - SHAPES[i][1]) < 0.01f) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------ the drawer card

    private View drawerCard() {
        LinearLayout card = card(Words.s("drawer"));
        Cards ways = new Cards(new String[] {Words.s("way_down"), Words.s("way_across")},
            new Sketch[] {new Sketch(this, Sketch.DOWN), new Sketch(this, Sketch.ACROSS)},
            Keep.across(this) ? 1 : 0, 148, new Picked() {
                public void picked(int which) {
                    Keep.saveAcross(Tune.this, which == 1);
                }
            });
        card.addView(ways.view(), spaced(16));

        card.addView(words(Letter.TITLE_S, Words.s("order"), Tone.ON_SURFACE_VARIANT), spaced(24));
        Cards order = new Cards(new String[] {Words.s("order_name"), Words.s("order_installed"),
            Words.s("order_updated")}, new Sketch[] {new Sketch(this, Sketch.NAME),
            new Sketch(this, Sketch.NEWEST), new Sketch(this, Sketch.UPDATED)},
            Keep.order(this), 120, new Picked() {
                public void picked(int which) {
                    Keep.saveOrder(Tune.this, which);
                }
            });
        card.addView(order.view(), spaced(12));

        card.addView(words(Letter.TITLE_S, Words.s("put_door"), Tone.ON_SURFACE_VARIANT), spaced(24));
        String[] faces = new String[Door.NAMES.length];
        Sketch[] fronts = new Sketch[Door.NAMES.length];
        for (int i = 0; i < faces.length; i++) {
            faces[i] = Words.s(Door.NAMES[i]);
            fronts[i] = new Sketch(this, Sketch.DOOR).door(i);
        }
        Cards doors = new Cards(faces, fronts, Keep.door(this), 112, 3, new Picked() {
            public void picked(int which) {
                Keep.saveDoor(Tune.this, which);
            }
        });
        card.addView(doors.view(), spaced(12));
        return card;
    }

    // ------------------------------------------------------------ the clock card

    /**
     * The clock on the screens: what its circle shows, what its small
     * window shows and opens, and which of its parts are there at all.
     * Where it stands and how large it is stays on its own card on the
     * screen, where it can be seen moving. A clock on each of several
     * screens is set each on its own.
     */
    private View clockCard() {
        LinearLayout card = card(Words.s("clock"));
        List<Layout.Item> clocks = clocks();
        if (clocks.isEmpty()) {
            card.addView(words(Letter.BODY_L, Words.s("clock_none"), Tone.ON_SURFACE_VARIANT), spaced(16));
            return card;
        }
        for (int i = 0; i < clocks.size(); i++) {
            clockParts(card, clocks.get(i), clocks.size() > 1, i == 0);
        }
        return card;
    }

    private void clockParts(LinearLayout card, final Layout.Item item, boolean named, boolean first) {
        if (named) {
            TextView where = words(Letter.TITLE_L, Words.s("screen_n").replace("{n}",
                String.valueOf(screenOf(item) + 1)), Tone.ON_SURFACE);
            Letter.serif(where);
            card.addView(where, spaced(first ? 16 : 36));
        }
        card.addView(words(Letter.TITLE_S, Words.s("circle"), Tone.ON_SURFACE_VARIANT), spaced(named ? 12 : 16));
        final String[] bigs = {Almanac.DIAL, Almanac.WEATHER, Almanac.NONE};
        Cards big = new Cards(new String[] {Words.s("state_dial"), Words.s("weather"), Words.s("state_none")},
            new Sketch[] {new Sketch(this, Sketch.CLOCK), new Sketch(this, Sketch.WEATHER),
                new Sketch(this, Sketch.CLOSE)},
            bigOf(item), 112, new Picked() {
                public void picked(int which) {
                    item.options.put("big", bigs[which]);
                    keepClocks();
                }
            });
        card.addView(big.view(), spaced(12));

        card.addView(words(Letter.TITLE_S, Words.s("small_part"), Tone.ON_SURFACE_VARIANT), spaced(20));
        final Cards[] small = new Cards[1];
        final TextView[] chooser = new TextView[1];
        final Runnable chosen = new Runnable() {
            public void run() {
                small[0].select(1);
                chooser[0].setText(appSaid(item));
                keepClocks();
            }
        };
        small[0] = new Cards(new String[] {Words.s("weather"), Words.s("state_app"), Words.s("state_none")},
            new Sketch[] {new Sketch(this, Sketch.WEATHER), new Sketch(this, Sketch.DRAWER),
                new Sketch(this, Sketch.CLOSE)},
            smallOf(item), 112, new Picked() {
                public void picked(int which) {
                    if (which == 1 && appOf(item) == null) {
                        // A window for an application, and none chosen yet:
                        // choose one now, or the window stays as it was.
                        final int was = smallOf(item);
                        pickApp(item, chosen, new Runnable() {
                            public void run() {
                                small[0].select(was);
                            }
                        });
                        return;
                    }
                    item.options.put("small", which == 0 ? Almanac.WEATHER
                        : which == 1 ? Almanac.APP : Almanac.NONE);
                    keepClocks();
                }
            });
        card.addView(small[0].view(), spaced(12));
        chooser[0] = button(appSaid(item), false, new View.OnClickListener() {
            public void onClick(View v) {
                pickApp(item, chosen, null);
            }
        });
        LinearLayout.LayoutParams chooserPlace = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, Round.dp(48f));
        chooserPlace.topMargin = Round.dp(12f);
        card.addView(chooser[0], chooserPlace);

        card.addView(words(Letter.TITLE_S, Words.s("parts"), Tone.ON_SURFACE_VARIANT), spaced(20));
        final String[] parts = {"time", "date", "charge", "ears"};
        boolean[] on = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            on[i] = !"off".equals(item.options.get(parts[i]));
        }
        Cards shown = new Cards(new String[] {Words.s("time_part"), Words.s("date_part"), Words.s("charge_part"),
                Words.s("ears_part")},
            new Sketch[] {new Sketch(this, Sketch.CLOCK), new Sketch(this, Sketch.CALENDAR),
                new Sketch(this, Sketch.BATTERY), new Sketch(this, Sketch.HEADPHONES)},
            -1, 104, 4, new Picked() {
                public void picked(int which) {
                    String part = parts[which];
                    if ("off".equals(item.options.get(part))) {
                        item.options.remove(part);
                    } else {
                        item.options.put(part, "off");
                    }
                    keepClocks();
                }
            }).each(on);
        card.addView(shown.view(), spaced(12));
    }

    /** Every clock on the screens, in the order of the screens. */
    private List<Layout.Item> clocks() {
        List<Layout.Item> found = new ArrayList<Layout.Item>();
        for (Layout.Screen screen : grid.screens) {
            for (Layout.Item item : screen.items) {
                if (Layout.CLOCK.equals(item.kind)) {
                    found.add(item);
                }
            }
        }
        return found;
    }

    private int screenOf(Layout.Item item) {
        for (int i = 0; i < grid.screens.size(); i++) {
            if (grid.screens.get(i).items.contains(item)) {
                return i;
            }
        }
        return 0;
    }

    private static int bigOf(Layout.Item item) {
        String now = item.options.get("big");
        return Almanac.WEATHER.equals(now) ? 1 : Almanac.NONE.equals(now) ? 2 : 0;
    }

    private static int smallOf(Layout.Item item) {
        String now = item.options.get("small");
        if (now == null || Almanac.WEATHER.equals(now)) {
            return 0;
        }
        return Almanac.NONE.equals(now) ? 2 : 1;
    }

    /** The application the clock's small window opens, as written down; an older clock kept it in the window's own place. */
    private static android.content.ComponentName appOf(Layout.Item item) {
        String name = item.options.get("app");
        String small = item.options.get("small");
        if (name == null && small != null && small.indexOf('/') > 0) {
            name = small;
        }
        return name == null ? null : android.content.ComponentName.unflattenFromString(name);
    }

    /** The name of that application, or the question of which it is to be. */
    private String appSaid(Layout.Item item) {
        android.content.ComponentName target = appOf(item);
        if (target == null) {
            return Words.s("choose_app");
        }
        try {
            return getPackageManager().getActivityInfo(target, 0).loadLabel(getPackageManager()).toString();
        } catch (Exception gone) {
            return Words.s("choose_app");
        }
    }

    /** How the first clock stands, in a few words, for its line in the contents. */
    private String clockStanding() {
        List<Layout.Item> clocks = clocks();
        if (clocks.isEmpty()) {
            return Words.s("clock_absent");
        }
        Layout.Item first = clocks.get(0);
        String[] bigs = {"state_dial", "weather", "state_none"};
        int small = smallOf(first);
        String window = small == 0 ? Words.s("weather") : small == 2 ? Words.s("state_none") : appSaid(first);
        return Words.s(bigs[bigOf(first)]) + "  \u00b7  " + window;
    }

    /** The clocks written down with the screens; the home screen builds itself anew when it is next seen. */
    private void keepClocks() {
        grid.save(this);
        Keep.touch(this);
        restate();
    }

    // ------------------------------------------------------------ the screens card

    private View screensCard() {
        LinearLayout card = card(Words.s("screens"));
        LinearLayout counts = new LinearLayout(this);
        counts.setOrientation(LinearLayout.VERTICAL);
        final Stepper across = new Stepper(Words.s("columns"), grid.columns, COLUMNS_LEAST, COLUMNS_MOST);
        final Stepper down = new Stepper(Words.s("rows"), grid.rows, ROWS_LEAST, ROWS_MOST);
        across.changed = new Changed() {
            public boolean to(int value) {
                return regrid(value, grid.rows);
            }
        };
        down.changed = new Changed() {
            public boolean to(int value) {
                return regrid(grid.columns, value);
            }
        };
        counts.addView(across.view, spaced(0));
        counts.addView(down.view, spaced(8));
        card.addView(counts, spaced(16));
        gridSaid = words(Letter.BODY_M, "", Tone.ON_SURFACE_VARIANT);
        gridSaid.setVisibility(View.GONE);
        card.addView(gridSaid, spaced(8));

        int count = Math.min(7, grid.screens.size());
        if (count > 1) {
            card.addView(words(Letter.TITLE_S, Words.s("main_screen"), Tone.ON_SURFACE_VARIANT),
                spaced(24));
            String[] numbers = new String[count];
            Sketch[] faces = new Sketch[count];
            for (int i = 0; i < count; i++) {
                numbers[i] = String.valueOf(i + 1);
                faces[i] = new Sketch(this, Sketch.SCREEN);
            }
            Cards main = new Cards(numbers, faces, Math.min(grid.home, count - 1), 120, new Picked() {
                public void picked(int which) {
                    grid.home = which;
                    grid.save(Tune.this);
                    Keep.touch(Tune.this);
                }
            });
            card.addView(main.view(), spaced(12));
        }

        card.addView(words(Letter.TITLE_S, Words.s("turning"), Tone.ON_SURFACE_VARIANT), spaced(24));
        Cards turning = new Cards(new String[] {Words.s("turn_ends"), Words.s("turn_round")},
            new Sketch[] {new Sketch(this, Sketch.ENDS), new Sketch(this, Sketch.ROUND)},
            Keep.endless(this) ? 1 : 0, 132, new Picked() {
                public void picked(int which) {
                    Keep.saveEndless(Tune.this, which == 1);
                }
            });
        card.addView(turning.view(), spaced(12));

        card.addView(words(Letter.TITLE_S, Words.s("widgets"), Tone.ON_SURFACE_VARIANT), spaced(24));
        Cards edges = new Cards(new String[] {Words.s("edge_inside"), Words.s("edge_glass")},
            new Sketch[] {new Sketch(this, Sketch.INSIDE), new Sketch(this, Sketch.EDGE)},
            Keep.edge(this) ? 1 : 0, 148, new Picked() {
                public void picked(int which) {
                    Keep.saveEdge(Tune.this, which == 1);
                }
            });
        card.addView(edges.view(), spaced(12));

        card.addView(words(Letter.TITLE_S, Words.s("immersion"), Tone.ON_SURFACE_VARIANT), spaced(24));
        Sketch[] modes = new Sketch[4];
        for (int i = 0; i < modes.length; i++) {
            modes[i] = new Sketch(this, Sketch.BARS).bars(i);
        }
        Cards immersion = new Cards(new String[] {Words.s("imm_bars"), Words.s("imm_status"),
            Words.s("imm_nav"), Words.s("imm_full")}, modes, Keep.immersion(this), 132, new Picked() {
                public void picked(int which) {
                    Keep.saveImmersion(Tune.this, which);
                }
            });
        card.addView(immersion.view(), spaced(12));
        return card;
    }

    /**
     * A new grid for the screens, if everything on them still fits in it.
     * Nothing is moved or cut to make room: a grid too small for what
     * stands on the screens is refused, and the row or column in the way
     * is named.
     */
    private boolean regrid(int columns, int rows) {
        int reach = 0;
        int depth = 0;
        for (Layout.Screen screen : grid.screens) {
            for (Layout.Item item : screen.items) {
                reach = Math.max(reach, item.x + item.w);
                depth = Math.max(depth, item.y + item.h);
            }
        }
        if (columns < reach) {
            gridSaid.setText(Words.s("column_taken").replace("{n}", String.valueOf(reach)));
            gridSaid.setVisibility(View.VISIBLE);
            return false;
        }
        if (rows < depth) {
            gridSaid.setText(Words.s("row_taken").replace("{n}", String.valueOf(depth)));
            gridSaid.setVisibility(View.VISIBLE);
            return false;
        }
        grid.columns = columns;
        grid.rows = rows;
        grid.save(this);
        Keep.touch(this);
        gridSaid.setVisibility(View.GONE);
        return true;
    }

    // ------------------------------------------------------------ the colour card

    private View colourCard() {
        LinearLayout card = card(Words.s("colour"));
        LinearLayout roles = new LinearLayout(this);
        roles.setOrientation(LinearLayout.HORIZONTAL);
        roles.addView(swatch(Tone.PRIMARY, Tone.ON_PRIMARY), weighted(2f, 0));
        roles.addView(swatch(Tone.SECONDARY_CONTAINER, Tone.ON_SECONDARY_CONTAINER), weighted(1f, 8));
        roles.addView(swatch(Tone.TERTIARY_CONTAINER, Tone.ON_TERTIARY_CONTAINER), weighted(1f, 8));
        card.addView(roles, spaced(16));

        card.addView(labelled(Words.s("hue"), null), spaced(20));
        hue = new Dial(this, Tone.hue() / 360f, new Dial.Moved() {
            public void moved(float value, boolean done) {
                seed(value * 360f, Tone.rich(), false);
            }
        }).large().loupe();
        card.addView(hue, wideRow());
        card.addView(labelled(Words.s("richness"), null), spaced(12));
        rich = new Dial(this, Tone.rich(), new Dial.Moved() {
            public void moved(float value, boolean done) {
                seed(Tone.hue(), value, false);
            }
        }).large().loupe();
        card.addView(rich, wideRow());

        if (Build.VERSION.SDK_INT >= 31) {
            final TextView wall = button(Words.s("wallpaper"), false, new View.OnClickListener() {
                public void onClick(View v) {
                    seed(Tone.hue(), Tone.rich(), true);
                    hue.value(Tone.hue() / 360f);
                    rich.value(Tone.rich());
                }
            });
            painters.add(new Runnable() {
                public void run() {
                    // Following the wallpaper, the chip is a plate; choosing by hand, glass.
                    boolean on = Keep.wall(Tune.this);
                    int kind = Cast.metal(look.rim);
                    if (on) {
                        wall.setBackground(touch(new Cast.Plate(kind, Round.FULL, look.gloss), Cast.ink(kind),
                            Round.FULL));
                        Cast.engrave(wall, kind);
                    } else {
                        wall.setBackground(touch(new Cast.Pane(Round.FULL), Tone.of(Tone.ON_SURFACE),
                            Round.FULL));
                        plain(wall, Tone.of(Tone.ON_SURFACE));
                    }
                }
            });
            LinearLayout.LayoutParams chip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Round.dp(48f));
            chip.topMargin = Round.dp(20f);
            card.addView(wall, chip);
        }
        return card;
    }

    /** A colour role as a block of itself, a chip of enamel with its ink on it. */
    private TextView swatch(final int fill, final int ink) {
        final TextView made = Letter.set(new TextView(this), Letter.HEADLINE_S);
        made.setText("Aa");
        made.setGravity(Gravity.BOTTOM | Gravity.START);
        made.setPadding(Round.dp(16f), 0, Round.dp(16f), Round.dp(12f));
        painters.add(new Runnable() {
            public void run() {
                made.setBackground(new Cast.Slab(Tone.of(fill), 24f));
                made.setTextColor(Tone.of(ink));
            }
        });
        return made;
    }

    /** The seed, chosen by hand or handed back to the wallpaper, and the screen regrown from it. */
    private void seed(float h, float r, boolean fromWall) {
        Keep.saveLook(this, h, r, fromWall);
        Tone.read(this);
        paint();
        if (look.rim == Tile.Look.ACCENT) {
            preview();
        }
    }

    // ------------------------------------------------------------ the weather card

    private static final int EARS = 6;
    private static final int HERE = 7;
    private TextView cityName;
    private LinearLayout found;

    /**
     * The city the clock's weather is for, found by its name; and the
     * leave, from Android 12, to hear the charge of headphones.
     */
    private View weatherCard() {
        LinearLayout card = card(Words.s("weather"));
        boolean following = Sky.here(this) && Sky.mayLocate(this);
        Cards where = new Cards(new String[] {Words.s("here"), Words.s("city_mode")},
            new Sketch[] {new Sketch(this, Sketch.PIN), new Sketch(this, Sketch.SEARCH)},
            following ? 0 : 1, 120, new Picked() {
                public void picked(int which) {
                    if (which == 0) {
                        if (Sky.mayLocate(Tune.this)) {
                            Sky.follow(Tune.this, true);
                            Keep.touch(Tune.this);
                            recreate();
                        } else {
                            requestPermissions(new String[] {
                                android.Manifest.permission.ACCESS_COARSE_LOCATION}, HERE);
                        }
                    } else {
                        Sky.follow(Tune.this, false);
                        Keep.touch(Tune.this);
                    }
                }
            });
        card.addView(where.view(), spaced(16));
        card.addView(words(Letter.BODY_M, Words.s(following ? "here_what" : "city_what"),
            Tone.ON_SURFACE_VARIANT), spaced(8));
        String city = Sky.city(this);
        if (following && city == null) {
            city = Words.s("here_none");
        }
        cityName = words(city != null ? Letter.HEADLINE_S : Letter.BODY_M,
            city != null ? city : Words.s("city_none"), city != null ? Tone.ON_SURFACE : Tone.ON_SURFACE_VARIANT);
        if (city != null) {
            Letter.serif(cityName);
        }
        card.addView(cityName, spaced(12));

        LinearLayout ask = new LinearLayout(this);
        ask.setOrientation(LinearLayout.HORIZONTAL);
        ask.setGravity(Gravity.CENTER_VERTICAL);
        final android.widget.EditText name = new android.widget.EditText(this);
        Letter.set(name, Letter.TITLE_M);
        name.setSingleLine(true);
        name.setHint(Words.s("city"));
        name.setPadding(Round.dp(20f), 0, Round.dp(20f), 0);
        name.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        painters.add(new Runnable() {
            public void run() {
                name.setBackground(new Cast.Pane(Round.FULL));
                name.setTextColor(Tone.of(Tone.ON_SURFACE));
                name.setHintTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
            }
        });
        ask.addView(name, new LinearLayout.LayoutParams(0, Round.dp(56f), 1f));
        final Runnable search = new Runnable() {
            public void run() {
                findCity(name.getText().toString().trim());
            }
        };
        name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int action, android.view.KeyEvent event) {
                search.run();
                return true;
            }
        });
        ask.addView(button(Words.s("find"), true, new View.OnClickListener() {
            public void onClick(View v) {
                search.run();
            }
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Round.dp(56f)) {
            {
                leftMargin = Round.dp(8f);
            }
        });
        card.addView(ask, spaced(12));
        found = new LinearLayout(this);
        found.setOrientation(LinearLayout.VERTICAL);
        card.addView(found, spaced(8));
        card.addView(credit(Words.s("weather_by"), Sky.WEATHER_SOURCE), spaced(16));
        card.addView(credit(Words.s("places_by"), Sky.PLACES_SOURCE), spaced(4));
        card.addView(credit(Words.s("licence_by"), Sky.LICENCE), spaced(4));

        if (Build.VERSION.SDK_INT >= 31) {
            card.addView(words(Letter.TITLE_S, Words.s("ears"), Tone.ON_SURFACE_VARIANT), spaced(20));
            card.addView(words(Letter.BODY_M, Words.s("ears_what"), Tone.ON_SURFACE_VARIANT), spaced(2));
            final boolean allowed = checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
            TextView allow = button(Words.s(allowed ? "allowed" : "allow"), !allowed, new View.OnClickListener() {
                public void onClick(View v) {
                    if (!allowed) {
                        requestPermissions(new String[] {android.Manifest.permission.BLUETOOTH_CONNECT}, EARS);
                    }
                }
            });
            card.addView(allow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Round.dp(48f)) {
                {
                    topMargin = Round.dp(12f);
                }
            });
        }
        return card;
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == EARS) {
            Keep.touch(this);
            recreate();
        } else if (request == HERE) {
            boolean granted = results.length > 0
                && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            Sky.follow(this, granted);
            Keep.touch(this);
            recreate();
        }
    }

    /** A line saying where something comes from; a press opens its page. */
    private TextView credit(String text, final String page) {
        TextView made = words(Letter.LABEL_M, text, Tone.PRIMARY);
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page)));
                } catch (ActivityNotFoundException none) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                }
            }
        });
        return made;
    }

    /** Places that answer to a name, as buttons; a press chooses one. */
    private void findCity(final String name) {
        if (name.length() == 0) {
            return;
        }
        found.removeAllViews();
        new Thread(new Runnable() {
            public void run() {
                List<Sky.Place> places;
                try {
                    places = Sky.find(name);
                } catch (Exception unreachable) {
                    places = null;
                }
                final List<Sky.Place> shown = places;
                runOnUiThread(new Runnable() {
                    public void run() {
                        showPlaces(shown);
                    }
                });
            }
        }).start();
    }

    private void showPlaces(List<Sky.Place> places) {
        found.removeAllViews();
        if (places == null || places.isEmpty()) {
            TextView none = words(Letter.BODY_M, Words.s("found_none"), Tone.ON_SURFACE_VARIANT);
            none.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
            found.addView(none, spaced(4));
            return;
        }
        for (final Sky.Place place : places) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Round.dp(16f), Round.dp(10f), Round.dp(16f), Round.dp(10f));
            row.setBackground(touch(new Cast.Slab(Tone.of(Tone.SURFACE_HIGH), 20f),
                Tone.of(Tone.ON_SURFACE), 20f));
            row.setStateListAnimator(Give.press());
            TextView big = Letter.set(new TextView(this), Letter.TITLE_M);
            big.setText(place.name);
            big.setTextColor(Tone.of(Tone.ON_SURFACE));
            row.addView(big);
            if (place.region.length() > 0) {
                TextView small = Letter.set(new TextView(this), Letter.BODY_S);
                small.setText(place.region);
                small.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
                row.addView(small);
            }
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Sky.choose(Tune.this, place);
                    Keep.touch(Tune.this);
                    found.removeAllViews();
                    cityName.setText(place.name);
                    Letter.set(cityName, Letter.HEADLINE_S);
                    Letter.serif(cityName);
                    cityName.setTextColor(Tone.of(Tone.ON_SURFACE));
                }
            });
            found.addView(row, spaced(found.getChildCount() == 0 ? 0 : 6));
        }
    }

    // ------------------------------------------------------------ the files card

    private View filesCard() {
        LinearLayout card = card(Words.s("files"));

        card.addView(words(Letter.TITLE_S, Words.s("folder"), Tone.ON_SURFACE_VARIANT), spaced(16));
        String place = folderName();
        TextView where = words(place != null ? Letter.HEADLINE_S : Letter.BODY_M,
            place != null ? place : Words.s("folder_none"),
            place != null ? Tone.ON_SURFACE : Tone.ON_SURFACE_VARIANT);
        if (place != null) {
            Letter.serif(where);
        }
        card.addView(where, spaced(4));
        TextView choose = button(Words.s("folder_choose"), false, new View.OnClickListener() {
            public void onClick(View v) {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(pick, FOLDER);
            }
        });
        card.addView(choose, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, Round.dp(48f)) {
            {
                topMargin = Round.dp(12f);
            }
        });

        LinearLayout pair = new LinearLayout(this);
        pair.setOrientation(LinearLayout.HORIZONTAL);
        pair.addView(button(Words.s("export"), false, new View.OnClickListener() {
            public void onClick(View v) {
                exportAll();
            }
        }), tall(56, 0));
        pair.addView(button(Words.s("import"), true, new View.OnClickListener() {
            public void onClick(View v) {
                importAny();
            }
        }), tall(56, 8));
        card.addView(pair, spaced(24));
        card.addView(words(Letter.BODY_M, Words.s("import_what"), Tone.ON_SURFACE_VARIANT), spaced(8));
        said = words(Letter.BODY_M, "", Tone.PRIMARY);
        card.addView(said, spaced(8));
        return card;
    }

    /** The language of this home screen: which module speaks, and how to bring another. */
    private View languageCard() {
        LinearLayout card = card(Words.s("language"));
        TextView tongue = words(Letter.HEADLINE_S,
            Words.active() ? Words.name() : Words.s("english"), Tone.ON_SURFACE);
        Letter.serif(tongue);
        card.addView(tongue, spaced(12));
        card.addView(words(Letter.BODY_M, Words.active()
            ? Words.s("words_of").replace("{n}", String.valueOf(Words.filled()))
                .replace("{m}", String.valueOf(Words.total()))
            : Words.s("language_what"), Tone.ON_SURFACE_VARIANT), spaced(2));
        LinearLayout both = new LinearLayout(this);
        both.setOrientation(LinearLayout.HORIZONTAL);
        both.addView(button(Words.s("module_load"), true, new View.OnClickListener() {
            public void onClick(View v) {
                importAny();
            }
        }), tall(56, 0));
        both.addView(button(Words.s("module_save"), false, new View.OnClickListener() {
            public void onClick(View v) {
                saveTemplate();
            }
        }), tall(56, 8));
        card.addView(both, spaced(16));
        if (Words.active()) {
            TextView english = button(Words.s("use_english"), false, new View.OnClickListener() {
                public void onClick(View v) {
                    Words.forget();
                    Words.save(Tune.this);
                    Keep.touch(Tune.this);
                    recreate();
                }
            });
            LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Round.dp(56f));
            under.topMargin = Round.dp(8f);
            card.addView(english, under);
        }
        tongueSaid = words(Letter.BODY_M, "", Tone.PRIMARY);
        card.addView(tongueSaid, spaced(8));
        return card;
    }

    // ------------------------------------------------------------ the folder

    /** The folder given for this home screen's files, if the leave to use it still holds. */
    private Uri folder() {
        String kept = Keep.folder(this);
        if (kept == null) {
            return null;
        }
        Uri tree = Uri.parse(kept);
        for (android.content.UriPermission held : getContentResolver().getPersistedUriPermissions()) {
            if (held.getUri().equals(tree) && held.isReadPermission() && held.isWritePermission()) {
                return tree;
            }
        }
        return null;
    }

    private String folderName() {
        Uri tree = folder();
        if (tree == null) {
            return null;
        }
        try {
            Uri self = DocumentsContract.buildDocumentUriUsingTree(tree,
                DocumentsContract.getTreeDocumentId(tree));
            android.database.Cursor c = getContentResolver().query(self,
                new String[] {DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        return c.getString(0);
                    }
                } finally {
                    c.close();
                }
            }
        } catch (RuntimeException gone) {
            return null;
        }
        return null;
    }

    /** A new file in the folder, named as given; the folder adds a number if the name is taken. */
    private Uri make(Uri tree, String mime, String name) throws java.io.FileNotFoundException {
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree,
            DocumentsContract.getTreeDocumentId(tree));
        return DocumentsContract.createDocument(getContentResolver(), parent, mime, name);
    }

    private static String stamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.ROOT)
            .format(new java.util.Date());
    }

    /** Everything, into the folder under the day and the hour, or wherever the owner says. */
    private void exportAll() {
        Uri tree = folder();
        if (tree == null) {
            Intent out = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            out.addCategory(Intent.CATEGORY_OPENABLE);
            out.setType("application/json");
            out.putExtra(Intent.EXTRA_TITLE, "ellipse-" + stamp() + ".json");
            startActivityForResult(out, EXPORT);
            return;
        }
        String name = "ellipse-" + stamp() + ".json";
        try {
            Uri made = make(tree, "application/json", name);
            if (made == null) {
                said.setText(Words.s("unsaved"));
                return;
            }
            writeAll(made);
            said.setText(Words.s("saved_in").replace("{n}", name));
        } catch (Exception failed) {
            said.setText(Words.s("unsaved"));
        }
    }

    private void writeAll(Uri uri) throws Exception {
        // One file for all of it: the layout as the home screen keeps it,
        // the settings beside it, and the language in use, whole.
        org.json.JSONObject whole = new org.json.JSONObject(Layout.load(this).text());
        whole.put("settings", Keep.export(this));
        if (Words.active()) {
            whole.put("language", Words.write());
        }
        OutputStream out = getContentResolver().openOutputStream(uri, "wt");
        try {
            out.write(whole.toString(1).getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
    }

    private void saveTemplate() {
        Uri tree = folder();
        if (tree == null) {
            Intent out = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            out.addCategory(Intent.CATEGORY_OPENABLE);
            out.setType("text/plain");
            out.putExtra(Intent.EXTRA_TITLE, "ellipse-language.txt");
            startActivityForResult(out, MODULE_SAVE);
            return;
        }
        try {
            Uri made = make(tree, "text/plain", "ellipse-language.txt");
            if (made != null) {
                module(MODULE_SAVE, made);
            }
        } catch (Exception failed) {
            tongueSaid.setText(Words.s("unsaved"));
        }
    }

    /**
     * Anything to read: from the folder, as a list of what lies in it, the
     * newest first; without a folder, from wherever the owner points.
     */
    private void importAny() {
        Uri tree = folder();
        if (tree == null) {
            Intent in = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            in.addCategory(Intent.CATEGORY_OPENABLE);
            in.setType("*/*");
            startActivityForResult(in, IMPORT);
            return;
        }
        final List<Uri> uris = new ArrayList<Uri>();
        final List<String> names = new ArrayList<String>();
        final List<Long> times = new ArrayList<Long>();
        try {
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree,
                DocumentsContract.getTreeDocumentId(tree));
            android.database.Cursor c = getContentResolver().query(children, new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED}, null, null, null);
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2))) {
                            continue;
                        }
                        long when = c.isNull(3) ? 0L : c.getLong(3);
                        int at = 0;
                        while (at < times.size() && times.get(at) >= when) {
                            at++;
                        }
                        uris.add(at, DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)));
                        names.add(at, c.getString(1));
                        times.add(at, when);
                    }
                } finally {
                    c.close();
                }
            }
        } catch (RuntimeException gone) {
            said.setText(Words.s("folder_empty"));
            return;
        }
        if (uris.isEmpty()) {
            said.setText(Words.s("folder_empty"));
            return;
        }
        showFiles(uris, names, times);
    }

    /** The folder's files on a sheet from below; a press reads one. */
    private void showFiles(final List<Uri> uris, List<String> names, List<Long> times) {
        final FrameLayout veil = new FrameLayout(this);
        veil.setBackgroundColor(0x66000000);
        veil.setClickable(true);
        veil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeSheet();
            }
        });
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClickable(true);
        panel.setBackground(Round.sheet(Tone.of(Tone.SURFACE_CONTAINER), Round.XL));
        panel.setPadding(Round.dp(16f), Round.dp(20f), Round.dp(16f), Round.dp(24f));
        TextView title = words(Letter.HEADLINE_S, folderName() == null ? "" : folderName(), Tone.ON_SURFACE);
        Letter.serif(title);
        title.setTextColor(Tone.of(Tone.ON_SURFACE));
        title.setPadding(Round.dp(8f), 0, Round.dp(8f), Round.dp(12f));
        panel.addView(title);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        java.text.DateFormat day = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
        for (int i = 0; i < uris.size(); i++) {
            final Uri uri = uris.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Round.dp(16f), Round.dp(14f), Round.dp(16f), Round.dp(14f));
            row.setBackground(touch(new Cast.Slab(Tone.of(Tone.SURFACE_HIGH), 20f),
                Tone.of(Tone.ON_SURFACE), 20f));
            row.setStateListAnimator(give());
            TextView name = Letter.set(new TextView(this), Letter.TITLE_M);
            name.setText(names.get(i));
            name.setTextColor(Tone.of(Tone.ON_SURFACE));
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            row.addView(name);
            TextView when = Letter.set(new TextView(this), Letter.BODY_M);
            when.setText(times.get(i) > 0 ? day.format(new java.util.Date(times.get(i))) : "");
            when.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
            row.addView(when);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    closeSheet();
                    importFrom(uri);
                }
            });
            list.addView(row, spaced(i == 0 ? 0 : 8));
        }
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        at.topMargin = Round.dp(120f);
        veil.addView(panel, at);
        root.addView(veil, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        veil.setAlpha(0f);
        veil.animate().alpha(1f).setDuration(Pace.SHEET).setInterpolator(Pace.STANDARD).start();
        sheet = veil;
    }

    /**
     * Every application, to choose the one the clock's small window opens.
     * Chosen, the clock is told; let go without a choice, whoever asked is
     * told that instead.
     */
    private void pickApp(final Layout.Item item, final Runnable chosen, final Runnable dropped) {
        if (sheet != null) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                final List<App> apps = App.all(Tune.this);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (!isFinishing() && sheet == null) {
                            showApps(apps, item, chosen, dropped);
                        }
                    }
                });
            }
        }).start();
    }

    private void showApps(List<App> apps, final Layout.Item item, final Runnable chosen, Runnable dropped) {
        final FrameLayout veil = new FrameLayout(this);
        veil.setBackgroundColor(0x66000000);
        veil.setClickable(true);
        veil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeSheet();
            }
        });
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClickable(true);
        panel.setBackground(Round.sheet(Tone.of(Tone.SURFACE_CONTAINER), Round.XL));
        panel.setPadding(Round.dp(16f), Round.dp(20f), Round.dp(16f), Round.dp(24f));
        TextView title = words(Letter.HEADLINE_S, Words.s("choose_app"), Tone.ON_SURFACE);
        Letter.serif(title);
        title.setTextColor(Tone.of(Tone.ON_SURFACE));
        title.setPadding(Round.dp(8f), 0, Round.dp(8f), Round.dp(12f));
        panel.addView(title);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < apps.size(); i++) {
            final App app = apps.get(i);
            TextView row = Letter.set(new TextView(this), Letter.TITLE_M);
            row.setText(app.label);
            row.setTextColor(Tone.of(Tone.ON_SURFACE));
            row.setSingleLine(true);
            row.setEllipsize(TextUtils.TruncateAt.END);
            row.setPadding(Round.dp(16f), Round.dp(14f), Round.dp(16f), Round.dp(14f));
            row.setBackground(touch(new Cast.Slab(Tone.of(Tone.SURFACE_HIGH), 20f),
                Tone.of(Tone.ON_SURFACE), 20f));
            row.setStateListAnimator(give());
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sheetDropped = null;
                    closeSheet();
                    item.options.put("app", app.component().flattenToString());
                    item.options.put("small", Almanac.APP);
                    chosen.run();
                }
            });
            list.addView(row, spaced(i == 0 ? 0 : 6));
        }
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        at.topMargin = Round.dp(120f);
        veil.addView(panel, at);
        root.addView(veil, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        veil.setAlpha(0f);
        veil.animate().alpha(1f).setDuration(Pace.SHEET).setInterpolator(Pace.STANDARD).start();
        sheet = veil;
        sheetDropped = dropped;
    }

    private void closeSheet() {
        if (sheet != null) {
            root.removeView(sheet);
            sheet = null;
        }
        if (sheetDropped != null) {
            Runnable told = sheetDropped;
            sheetDropped = null;
            told.run();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (sheet != null) {
            closeSheet();
        } else if (page != null) {
            closeSection();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Reads whatever a file is: another home screen's backup, a language
     * module, or this home screen's own file of layout, settings and
     * language. What it is, is told by what is inside, not by its name.
     */
    private void importFrom(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            byte[] data;
            try {
                java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[16384];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    all.write(buffer, 0, n);
                    if (all.size() > 64 * 1024 * 1024) {
                        throw new java.io.IOException("too large");
                    }
                }
                data = all.toByteArray();
            } finally {
                in.close();
            }
            if (data.length > 1 && data[0] == 'P' && data[1] == 'K') {
                Layout found = Foreign.read(this, data);
                if (found == null) {
                    said.setText(Words.s("not_readable"));
                    return;
                }
                found.save(this);
                grid = found;
                Keep.touch(this);
                said.setText(Words.s("foreign_read").replace("{n}", String.valueOf(found.screens.size())));
                return;
            }
            String text = new String(data, StandardCharsets.UTF_8);
            if (Words.isModule(text)) {
                Words.read(text.startsWith("\uFEFF") ? text.substring(1) : text);
                Words.save(this);
                Keep.touch(this);
                recreate();
                return;
            }
            Layout read = Layout.parse(text);
            read.save(this);
            grid = read;
            org.json.JSONObject whole = new org.json.JSONObject(text);
            org.json.JSONObject settings = whole.optJSONObject("settings");
            if (settings != null) {
                Keep.restore(this, settings);
            }
            String language = whole.optString("language", "");
            if (Words.isModule(language)) {
                Words.read(language);
                Words.save(this);
            }
            Keep.touch(this);
            recreate();
        } catch (Exception failed) {
            said.setText(Words.s("not_readable"));
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (request == FOLDER) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                Keep.saveFolder(this, uri.toString());
            } catch (RuntimeException refused) {
                Keep.saveFolder(this, null);
            }
            recreate();
            return;
        }
        if (request == MODULE_LOAD || request == IMPORT) {
            importFrom(uri);
            return;
        }
        if (request == MODULE_SAVE) {
            module(request, uri);
            return;
        }
        if (request == EXPORT) {
            try {
                writeAll(uri);
                said.setText(Words.s("exported"));
            } catch (Exception failed) {
                said.setText(Words.s("unsaved"));
            }
        }
    }

    /**
     * A language module in or out. One read in is kept, and both this screen
     * and the home screen are built again in it; the template goes out with
     * every line, filled or not, the English above each as a comment.
     */
    private void module(int request, Uri uri) {
        try {
            if (request == MODULE_SAVE) {
                OutputStream out = getContentResolver().openOutputStream(uri, "wt");
                try {
                    out.write(Words.write().getBytes(StandardCharsets.UTF_8));
                } finally {
                    out.close();
                }
                tongueSaid.setText(Words.s("module_saved"));
                return;
            }
            InputStream in = getContentResolver().openInputStream(uri);
            String text;
            try {
                text = Layout.read(in);
            } finally {
                in.close();
            }
            if (!Words.isModule(text)) {
                tongueSaid.setText(Words.s("module_bad"));
                return;
            }
            Words.read(text);
            Words.save(this);
            Keep.touch(this);
            recreate();
        } catch (Exception failed) {
            tongueSaid.setText(Words.s(request == MODULE_SAVE ? "unsaved" : "module_bad"));
        }
    }

    // ------------------------------------------------------------ the way out

    /**
     * The system's choice of home screen: the one door out, and so a door
     * plate, cut from the material, its words cut into it.
     */
    private View way() {
        final LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(Round.dp(24f), Round.dp(22f), Round.dp(24f), Round.dp(22f));
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chooseHome();
            }
        });
        made.setStateListAnimator(give());
        final TextView name = Letter.serif(Letter.set(new TextView(this), Letter.HEADLINE_S));
        name.setText(Words.s("home_screen"));
        made.addView(name);
        final TextView what = Letter.set(new TextView(this), Letter.BODY_M);
        what.setText(Words.s("home_screen_what"));
        made.addView(what, spaced(4));
        painters.add(new Runnable() {
            public void run() {
                int kind = Cast.metal(look.rim);
                made.setBackground(touch(new Cast.Plate(kind, REST, look.gloss), Cast.ink(kind), REST));
                Cast.engrave(name, kind);
                Cast.engrave(what, kind);
            }
        });
        cards.add(made);
        return made;
    }

    private void chooseHome() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (ActivityNotFoundException none) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    // ------------------------------------------------------------ colour of it all

    /**
     * Every colour and every material on the screen, from the seed and the
     * look as they are now. The room is lit again, every painter paints,
     * every cap is cut again from the material and every groove shows what
     * it chooses.
     */
    private void paint() {
        room.tint();
        if (pageRoom != null) {
            pageRoom.tint();
        }
        for (Runnable painter : painters) {
            painter.run();
        }
        int kind = Cast.metal(look.rim);
        int scale = Tone.of(Tone.ON_SURFACE_VARIANT, 0.3f);
        // The dials exist only once their subject's screen has been opened.
        int[] dark = {Tone.of(Tone.SURFACE_LOWEST), Tone.of(Tone.SURFACE_LOW)};
        for (Dial dial : new Dial[] {round, wide, close, thick}) {
            if (dial != null) {
                dial.colours(dark);
                dial.ink(Cast.glow(kind));
                dial.material(kind, look.gloss);
                dial.marks(scale);
            }
        }
        if (hue != null) {
            int[] circle = new int[25];
            for (int i = 0; i < circle.length; i++) {
                circle[i] = Tone.at(72f, 44.0, i * 15f);
            }
            hue.colours(circle);
            hue.ink(0);
            hue.material(kind, look.gloss);
            hue.marks(scale);
        }
        if (rich != null) {
            int[] way = new int[9];
            for (int i = 0; i < way.length; i++) {
                way[i] = Tone.at(72f, Tone.chromaOf(i / 8f), Tone.hue());
            }
            rich.colours(way);
            rich.ink(0);
            rich.material(kind, look.gloss);
            rich.marks(scale);
        }
        for (Cards group : groups) {
            group.paint(false);
        }
    }

    /** The look has changed what everything is made of: cast it all again, and let the light show it. */
    private void recast() {
        paint();
        sheen();
    }

    // ------------------------------------------------------------ parts

    private TextView words(int rung, String text, final int role) {
        final TextView made = Letter.set(new TextView(this), rung);
        made.setText(text);
        painters.add(new Runnable() {
            public void run() {
                made.setTextColor(Tone.of(role));
            }
        });
        return made;
    }

    /** Words in the ground's own ink, with no shadow of a cut about them. */
    private static void plain(TextView view, int ink) {
        view.setTextColor(ink);
        view.setShadowLayer(0f, 0f, 0f, 0);
    }

    /**
     * A press over anything drawn here, in its own ink, and in the shape
     * of its box: the mask is given, so the press never reaches past a
     * corner, whatever the thing under it draws.
     */
    private static RippleDrawable touch(Drawable under, int ink, float radius) {
        int wash = (Math.round(255f * 0.12f) << 24) | (ink & 0x00FFFFFF);
        return new RippleDrawable(ColorStateList.valueOf(wash), under, Round.box(0xFFFFFFFF, radius));
    }

    /** A card for one subject, its name large at the top, lying on the table. */
    private LinearLayout card(String name) {
        final LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(Round.dp(20f), Round.dp(22f), Round.dp(20f), Round.dp(22f));
        made.addView(words(Letter.HEADLINE_S, name, Tone.ON_SURFACE));
        painters.add(new Runnable() {
            public void run() {
                made.setBackground(new Cast.Slab(Tone.of(Tone.SURFACE_CONTAINER), REST));
            }
        });
        return made;
    }

    /** A label, and on the far side the number the dial under it stands at. */
    private View labelled(String name, TextView value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(words(Letter.TITLE_S, name, Tone.ON_SURFACE_VARIANT),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (value != null) {
            row.addView(value);
        }
        return row;
    }

    /** The number a gauge stands at, in the light its groove is filled with. */
    private TextView value() {
        final TextView made = Letter.serif(Letter.set(new TextView(this), Letter.TITLE_L));
        painters.add(new Runnable() {
            public void run() {
                made.setTextColor(Cast.glow(Cast.metal(look.rim)));
            }
        });
        return made;
    }

    /**
     * A button. The one thing most likely wanted is a plate of the
     * material with its word cut in; the rest are glass, the word lit
     * behind it.
     */
    private TextView button(String text, final boolean filled, View.OnClickListener click) {
        final TextView made = Letter.set(new TextView(this), Letter.LABEL_L);
        made.setText(text);
        made.setGravity(Gravity.CENTER);
        made.setPadding(Round.dp(22f), 0, Round.dp(22f), 0);
        made.setSingleLine(true);
        made.setEllipsize(TextUtils.TruncateAt.END);
        made.setOnClickListener(click);
        made.setStateListAnimator(give());
        painters.add(new Runnable() {
            public void run() {
                int kind = Cast.metal(look.rim);
                if (filled) {
                    made.setBackground(touch(new Cast.Plate(kind, Round.FULL, look.gloss), Cast.ink(kind),
                        Round.FULL));
                    Cast.engrave(made, kind);
                } else {
                    made.setBackground(touch(new Cast.Pane(Round.FULL), Tone.of(Tone.ON_SURFACE), Round.FULL));
                    plain(made, Tone.of(Tone.ON_SURFACE));
                }
            }
        });
        return made;
    }

    /** Anything pressable gives a little under the finger and comes back when let go. */
    private static StateListAnimator give() {
        StateListAnimator give = new StateListAnimator();
        ObjectAnimator in = ObjectAnimator.ofPropertyValuesHolder((Object) null,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f));
        in.setDuration(Pace.PRESS);
        in.setInterpolator(Pace.STANDARD);
        ObjectAnimator out = ObjectAnimator.ofPropertyValuesHolder((Object) null,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f));
        out.setDuration(Pace.GROW);
        out.setInterpolator(Pace.EMPHASIS);
        give.addState(new int[] {android.R.attr.state_pressed}, in);
        give.addState(new int[] {}, out);
        return give;
    }

    private static LinearLayout.LayoutParams spaced(int above) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Round.dp(above);
        return params;
    }

    private static LinearLayout.LayoutParams wideRow() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams weighted(float weight, int before) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Round.dp(80f), weight);
        params.leftMargin = Round.dp(before);
        return params;
    }

    private static LinearLayout.LayoutParams tall(int height, int before) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Round.dp(height), 1f);
        params.leftMargin = Round.dp(before);
        return params;
    }

    /** What a group of cards says when one of them is pressed. */
    private interface Picked {
        void picked(int which);
    }

    /**
     * A group of cards of which one is chosen, or none when the value lies
     * between the named ones. Each card is a picture of what it does and its
     * name. The chosen one is set: a rim of the material closes in around
     * it, its face turns to glass and its sign lights in the colour the
     * material gives what is seen through glass, while its corners square a
     * little, as if pressed home. The one given up lets go of its rim and
     * rounds its corners back. Both move together.
     */
    private final class Cards {

        private final LinearLayout row;
        private final LinearLayout[] items;
        private final Sketch[] marks;
        private final TextView[] names;
        private final Cast.Setting[] settings;
        private final float[] corners;
        private final float[] sets;
        private final ValueAnimator[] moving;
        private final Picked picked;
        private int chosen;
        /** For cards set each on its own, which are; for a group of one choice, nothing. */
        private boolean[] ons;

        Cards(String[] labels, Sketch[] marks, int chosen, int height, Picked picked) {
            this(labels, marks, chosen, height, labels.length, picked);
        }

        Cards(String[] labels, Sketch[] marks, int chosen, int height, int perRow, Picked picked) {
            this.marks = marks;
            this.chosen = chosen;
            this.picked = picked;
            row = new LinearLayout(Tune.this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout line = null;
            items = new LinearLayout[labels.length];
            names = new TextView[labels.length];
            settings = new Cast.Setting[labels.length];
            corners = new float[labels.length];
            sets = new float[labels.length];
            moving = new ValueAnimator[labels.length];
            for (int i = 0; i < labels.length; i++) {
                final int which = i;
                LinearLayout item = new LinearLayout(Tune.this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(Round.dp(8f), Round.dp(12f), Round.dp(8f), Round.dp(12f));
                item.setContentDescription(labels[i]);
                item.setStateListAnimator(give());
                item.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                        select(which);
                        Cards.this.picked.picked(which);
                    }
                });
                int mark = Math.round(height * 0.46f);
                item.addView(marks[i], new LinearLayout.LayoutParams(Round.dp(mark), Round.dp(mark)));
                // One line always: a long name is set smaller rather than broken.
                TextView name = Letter.set(new TextView(Tune.this), Letter.LABEL_L);
                name.setText(labels[i]);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(1);
                name.setAutoSizeTextTypeUniformWithConfiguration(9,
                    Math.round(Letter.size(Letter.LABEL_L)), 1, android.util.TypedValue.COMPLEX_UNIT_SP);
                LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Round.dp(22f));
                under.topMargin = Round.dp(8f);
                item.addView(name, under);
                corners[i] = i == chosen ? CHOSEN : REST;
                sets[i] = i == chosen ? 1f : 0f;
                if (i % perRow == 0) {
                    line = new LinearLayout(Tune.this);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams linePlace = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    linePlace.topMargin = i == 0 ? 0 : Round.dp(8f);
                    row.addView(line, linePlace);
                }
                LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0, Round.dp(height), 1f);
                place.leftMargin = i % perRow == 0 ? 0 : Round.dp(8f);
                line.addView(item, place);
                items[i] = item;
                names[i] = name;
            }
            groups.add(this);
        }

        View view() {
            return row;
        }

        /** The cards set each on its own, as parts shown or hidden: a press turns one over. */
        Cards each(boolean[] on) {
            ons = on.clone();
            for (int i = 0; i < items.length; i++) {
                corners[i] = ons[i] ? CHOSEN : REST;
                sets[i] = ons[i] ? 1f : 0f;
            }
            return this;
        }

        /**
         * A card chosen, or, for cards set each on its own, one turned over.
         * Told none, as a dial is when its value has no name of its own, no
         * card is chosen. The tick belongs to the finger, not to the choice:
         * a choice made by a dial moving is felt through the dial.
         */
        void select(int which) {
            if (ons != null) {
                if (which >= 0 && which < ons.length) {
                    ons[which] = !ons[which];
                    paint(true);
                }
                return;
            }
            if (which != chosen) {
                chosen = which < 0 || which >= items.length ? -1 : which;
                paint(true);
            }
        }

        /**
         * The cards in the colours and material of the moment. Called still,
         * each card is cast anew; called moving, the cards keep what they
         * are and only their rims and corners travel.
         */
        void paint(boolean move) {
            int kind = Cast.metal(look.rim);
            int glow = Cast.glow(kind);
            for (int i = 0; i < items.length; i++) {
                boolean on = ons != null ? ons[i] : i == chosen;
                if (!move || settings[i] == null) {
                    settings[i] = new Cast.Setting(Tone.of(Tone.SURFACE_HIGH), kind, Round.px(corners[i]), sets[i]);
                    items[i].setBackground(touch(settings[i], Tone.of(Tone.ON_SURFACE), REST));
                }
                plain(names[i], Tone.of(on ? Tone.ON_SURFACE : Tone.ON_SURFACE_VARIANT));
                int quiet = on ? (glow & 0x00FFFFFF) | 0x73000000 : Tone.of(Tone.ON_SURFACE_VARIANT, 0.45f);
                marks[i].ink(on ? glow : Tone.of(Tone.ON_SURFACE_VARIANT), quiet);
                items[i].setSelected(on);
                travel(i, on, move);
            }
        }

        /**
         * A card to where it goes: a card set squares its corners and closes
         * its rim in, one given up rounds them and lets it go. The movement
         * speaks to the card by its place, not to one drawing, so a card
         * cast anew half way through carries on from where the last left off.
         */
        private void travel(final int i, boolean on, boolean move) {
            final float cornerTo = on ? CHOSEN : REST;
            final float setTo = on ? 1f : 0f;
            if (!move) {
                if (moving[i] == null || !moving[i].isRunning()) {
                    corners[i] = cornerTo;
                    sets[i] = setTo;
                }
                settings[i].radius(Round.px(corners[i]));
                settings[i].set(sets[i]);
                return;
            }
            if (moving[i] != null) {
                moving[i].cancel();
            }
            final float cornerFrom = corners[i];
            final float setFrom = sets[i];
            if (cornerFrom == cornerTo && setFrom == setTo) {
                return;
            }
            ValueAnimator shift = ValueAnimator.ofFloat(0f, 1f);
            shift.setDuration(on ? Pace.ARRIVE : Pace.GROW);
            shift.setInterpolator(Pace.EMPHASIS);
            shift.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    float t = (Float) a.getAnimatedValue();
                    corners[i] = cornerFrom + (cornerTo - cornerFrom) * t;
                    sets[i] = setFrom + (setTo - setFrom) * t;
                    settings[i].radius(Round.px(corners[i]));
                    settings[i].set(sets[i]);
                }
            });
            moving[i] = shift;
            shift.start();
        }
    }

    /** What a count says when a hand moves it: whether the new count may stand. */
    private interface Changed {
        boolean to(int value);
    }

    /**
     * A count: the numeral large behind a window of glass, between two
     * coins of the material with a minus and a plus cut into them. A new
     * count rolls into the window as on a counter, up for more, down for
     * fewer; a count refused does not move, and the phone says no.
     */
    private final class Stepper {

        final LinearLayout view;
        private final FrameLayout window;
        private final TextView numeral;
        private final int least;
        private final int most;
        private int value;
        Changed changed;

        Stepper(String name, int start, int least, int most) {
            this.value = start;
            this.least = least;
            this.most = most;
            view = new LinearLayout(Tune.this);
            view.setOrientation(LinearLayout.HORIZONTAL);
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setPadding(Round.dp(20f), Round.dp(12f), Round.dp(12f), Round.dp(12f));
            TextView label = words(Letter.TITLE_L, name, Tone.ON_SURFACE_VARIANT);
            Letter.serif(label);
            view.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            view.addView(key(Sketch.MINUS, -1), new LinearLayout.LayoutParams(Round.dp(52f), Round.dp(52f)));
            window = new FrameLayout(Tune.this);
            window.setClipToOutline(true);
            window.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline outline) {
                    outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Round.px(16f));
                }
            });
            numeral = Letter.set(new TextView(Tune.this), Letter.DISPLAY_M);
            numeral.setText(String.valueOf(start));
            numeral.setGravity(Gravity.CENTER);
            window.addView(numeral, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            LinearLayout.LayoutParams glass = new LinearLayout.LayoutParams(Round.dp(84f), Round.dp(64f));
            glass.leftMargin = Round.dp(8f);
            glass.rightMargin = Round.dp(8f);
            view.addView(window, glass);
            view.addView(key(Sketch.PLUS, 1), new LinearLayout.LayoutParams(Round.dp(52f), Round.dp(52f)));
            painters.add(new Runnable() {
                public void run() {
                    view.setBackground(new Cast.Slab(Tone.of(Tone.SURFACE_HIGH), 24f));
                    window.setBackground(new Cast.Pane(16f));
                    numeral.setTextColor(Cast.glow(Cast.metal(look.rim)));
                }
            });
        }

        private View key(int kind, final int step) {
            final FrameLayout key = new FrameLayout(Tune.this);
            final Sketch mark = new Sketch(Tune.this, kind);
            key.addView(mark, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            key.setStateListAnimator(Give.press());
            key.setContentDescription(step < 0 ? "\u2212" : "+");
            key.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int next = Math.max(least, Math.min(most, value + step));
                    if (next == value || changed == null || !changed.to(next)) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                        refuse();
                        return;
                    }
                    value = next;
                    roll(step);
                }
            });
            painters.add(new Runnable() {
                public void run() {
                    int metal = Cast.metal(look.rim);
                    key.setBackground(touch(new Cast.Plate(metal, Round.FULL, look.gloss), Cast.ink(metal),
                        Round.FULL));
                    mark.ink(Cast.ink(metal), 0);
                }
            });
            return key;
        }

        /** The old numeral leaves the window the way the count went, and the new one comes in behind it. */
        private void roll(final int step) {
            final String next = String.valueOf(value);
            final float travel = Math.max(1f, window.getHeight() * 0.75f);
            final float way = step > 0 ? 1f : -1f;
            numeral.animate().cancel();
            numeral.animate().translationY(-way * travel).alpha(0f).setStartDelay(0L).setDuration(90L)
                .setInterpolator(Pace.AWAY).withEndAction(new Runnable() {
                    public void run() {
                        numeral.setText(next);
                        numeral.setTranslationY(way * travel);
                        numeral.animate().translationY(0f).alpha(1f).setDuration(Pace.GROW)
                            .setInterpolator(new OvershootInterpolator(1.6f)).withEndAction(null).start();
                    }
                }).start();
        }

        /** A count that may not stand shakes its head in the window. */
        private void refuse() {
            numeral.animate().cancel();
            numeral.setTranslationY(0f);
            numeral.setAlpha(1f);
            ObjectAnimator no = ObjectAnimator.ofFloat(numeral, View.TRANSLATION_X,
                0f, Round.px(-7f), Round.px(6f), Round.px(-4f), Round.px(2f), 0f);
            no.setDuration(Pace.ARRIVE);
            no.start();
        }
    }
}
