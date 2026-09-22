package io.github.shumtugle.ellipse;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
 * The settings, on one screen, in the manner of the rest: large and plain.
 *
 * The screen opens on the tiles themselves: four of the owner's own, the
 * first four of the main home screen, large, on a card of their own, with
 * the look they wear named under them. Every move of a dial below redraws
 * them, so a tile is chosen by eye and not by number, though the number
 * stands beside every dial too.
 *
 * Then come cards, one to a subject. A choice between a few things is a
 * group of cards drawn with what they do rather than a row of words: a
 * shape is its own silhouette, a rim a disc of its metal, a way of the
 * drawer a little screen that runs down or three that lie side by side.
 * The card chosen is filled with the seed's colour and squares its
 * corners a little, as if pressed into place; every card gives under the
 * finger. A count is a large numeral between a minus and a plus.
 *
 * Nothing here reaches into the home screen. A setting is written down;
 * the home screen finds it changed when it is shown again, and builds
 * itself anew. The layout of the home screens and a language module go
 * out to a file and come back from one; at the bottom stands the way out,
 * the system's own choice of home screen.
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

    private final List<Runnable> painters = new ArrayList<Runnable>();
    private final List<Cards> groups = new ArrayList<Cards>();
    private final List<View> cards = new ArrayList<View>();
    private final ImageView[] previews = new ImageView[4];
    private final Drawable[] faces = new Drawable[4];

    private Tile.Look look;
    private Layout grid;
    private FrameLayout root;
    private View sheet;
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
    private TextView named;
    private TextView gridSaid;
    private TextView said;
    private TextView tongueSaid;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Round.measure(this);
        Tone.read(this);
        Words.load(this);
        Tile.materials(getResources());
        look = Keep.tile(this);
        grid = Layout.load(this);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Round.dp(16f), Round.dp(40f), Round.dp(16f), Round.dp(48f));

        TextView title = words(Letter.DISPLAY_S, Words.s("settings"), Tone.ON_SURFACE);
        Letter.serif(title);
        title.setPadding(Round.dp(8f), 0, Round.dp(8f), 0);
        column.addView(title);

        said = words(Letter.BODY_M, "", Tone.PRIMARY);
        tongueSaid = words(Letter.BODY_M, "", Tone.PRIMARY);
        specimenCard = specimen();
        column.addView(specimenCard, spaced(24));
        contents = new LinearLayout(this);
        contents.setOrientation(LinearLayout.VERTICAL);
        column.addView(contents, spaced(16));
        listSections();
        column.addView(way(), spaced(16));

        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception unknown) {
            version = "";
        }
        TextView made = words(Letter.LABEL_M, getString(R.string.app_name) + "  " + version,
            Tone.ON_SURFACE_VARIANT);
        made.setPadding(Round.dp(8f), 0, 0, 0);
        column.addView(made, spaced(28));

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
        if (reopen >= 0) {
            openSection(reopen, false);
        } else {
            arrive();
        }
        sample();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("section", section);
    }

    // ------------------------------------------------------------ the contents

    /**
     * The settings are split by what they are about, each subject a screen
     * of its own; the first screen is their contents. Each line of the
     * contents says, under the subject's name, how it stands now, so most
     * questions are answered without going in.
     */
    private static final int[] GLYPHS = {Sketch.WINDOW_FOLLOWS, Sketch.SCREENS, Sketch.DRAWER,
        Sketch.PALETTE, Sketch.WEATHER, Sketch.FOLDER_OPEN, Sketch.LANGUAGE};
    private static final String[] SUBJECTS = {"tile", "screens", "drawer", "colour", "weather", "files",
        "language"};

    private ScrollView index;
    private LinearLayout indexColumn;
    private LinearLayout contents;
    private View specimenCard;
    private ScrollView page;
    private int section = -1;

    private void listSections() {
        contents.removeAllViews();
        for (int i = 0; i < SUBJECTS.length; i++) {
            final int which = i;
            final LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(Round.dp(16f), Round.dp(14f), Round.dp(16f), Round.dp(14f));
            final FrameLayout disc = new FrameLayout(this);
            final Sketch mark = new Sketch(this, GLYPHS[i]);
            disc.addView(mark, new FrameLayout.LayoutParams(Round.dp(26f), Round.dp(26f), Gravity.CENTER));
            line.addView(disc, new LinearLayout.LayoutParams(Round.dp(52f), Round.dp(52f)));
            LinearLayout both = new LinearLayout(this);
            both.setOrientation(LinearLayout.VERTICAL);
            both.addView(words(Letter.TITLE_L, Words.s(SUBJECTS[i]), Tone.ON_SURFACE));
            TextView now = words(Letter.BODY_M, standing(i), Tone.ON_SURFACE_VARIANT);
            now.setSingleLine(true);
            now.setEllipsize(android.text.TextUtils.TruncateAt.END);
            both.addView(now);
            LinearLayout.LayoutParams bothPlace = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            bothPlace.leftMargin = Round.dp(16f);
            line.addView(both, bothPlace);
            final Sketch on = new Sketch(this, Sketch.FORWARD);
            line.addView(on, new LinearLayout.LayoutParams(Round.dp(20f), Round.dp(20f)));
            painters.add(new Runnable() {
                public void run() {
                    line.setBackground(Round.touch(Round.box(Tone.of(Tone.SURFACE_CONTAINER), 24f),
                        Tone.of(Tone.ON_SURFACE), 24f));
                    disc.setBackground(Round.box(Tone.of(Tone.SECONDARY_CONTAINER), Round.FULL));
                    mark.ink(Tone.of(Tone.ON_SECONDARY_CONTAINER), Tone.of(Tone.ON_SECONDARY_CONTAINER));
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
            place.topMargin = i == 0 ? 0 : Round.dp(6f);
            contents.addView(line, place);
            cards.add(line);
        }
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
            case 2: {
                String[] orders = {"order_name", "order_installed", "order_updated"};
                return Words.s(Keep.across(this) ? "way_across" : "way_down") + "  \u00b7  "
                    + Words.s(orders[Math.max(0, Math.min(2, Keep.order(this)))]);
            }
            case 3:
                return Keep.wall(this) ? Words.s("wallpaper") : Words.s("hue");
            case 4: {
                if (Sky.here(this) && Sky.mayLocate(this)) {
                    return Words.s("here") + (Sky.city(this) != null ? "  \u00b7  " + Sky.city(this) : "");
                }
                return Sky.city(this) != null ? Sky.city(this) : Words.s("city_none");
            }
            case 5: {
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
            case 2: return drawerCard();
            case 3: return colourCard();
            case 4: return weatherCard();
            case 5: return filesCard();
            default: return languageCard();
        }
    }

    /**
     * A subject opens as a screen of its own, sliding in over the contents,
     * which step back to the left. The subject's name stands at its head
     * beside a key back; the tiles go along with the subject of the tiles,
     * so what is changed can be seen changing.
     */
    private void openSection(int which, boolean animate) {
        if (page != null) {
            return;
        }
        section = which;
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Round.dp(16f), Round.dp(40f), Round.dp(16f), Round.dp(48f));

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
                back.setBackground(Round.touch(Round.box(Tone.of(Tone.SURFACE_CONTAINER), Round.FULL),
                    Tone.of(Tone.ON_SURFACE), Round.FULL));
                arrow.ink(Tone.of(Tone.ON_SURFACE), 0);
            }
        });
        head.addView(back, new LinearLayout.LayoutParams(Round.dp(52f), Round.dp(52f)));
        TextView title = words(Letter.DISPLAY_S, Words.s(SUBJECTS[which]), Tone.ON_SURFACE);
        Letter.serif(title);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titlePlace = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titlePlace.leftMargin = Round.dp(14f);
        head.addView(title, titlePlace);
        column.addView(head);

        if (which == 0) {
            ((ViewGroup) specimenCard.getParent()).removeView(specimenCard);
            column.addView(specimenCard, spaced(20));
        }
        View body = build(which);
        if (body instanceof ViewGroup && ((ViewGroup) body).getChildCount() > 0) {
            // The card's own heading says what the head of the screen already says.
            ((ViewGroup) body).removeViewAt(0);
        }
        column.addView(body, spaced(16));

        page = new ScrollView(this);
        page.setOverScrollMode(View.OVER_SCROLL_NEVER);
        page.setVerticalScrollBarEnabled(false);
        page.addView(column);
        page.setClickable(true);
        root.addView(page, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        paint();
        preview();
        float width = getResources().getDisplayMetrics().widthPixels;
        if (animate) {
            page.setTranslationX(width);
            page.animate().translationX(0f).setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
            index.animate().translationX(-width * 0.25f).alpha(0f).setDuration(Pace.ARRIVE)
                .setInterpolator(Pace.EMPHASIS).start();
            body.setAlpha(0f);
            body.setTranslationY(Round.px(24f));
            body.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STAGGER * 2).setDuration(Pace.ARRIVE)
                .setInterpolator(Pace.EMPHASIS).start();
        } else {
            index.setTranslationX(-width * 0.25f);
            index.setAlpha(0f);
        }
    }

    /** Back to the contents, the subject sliding away to the right. */
    private void closeSection() {
        if (page == null) {
            return;
        }
        final ScrollView leaving = page;
        page = null;
        section = -1;
        float width = getResources().getDisplayMetrics().widthPixels;
        leaving.animate().translationX(width).setDuration(Pace.GROW).setInterpolator(Pace.EMPHASIS)
            .withEndAction(new Runnable() {
                public void run() {
                    root.removeView(leaving);
                }
            }).start();
        if (specimenCard.getParent() != indexColumn) {
            ((ViewGroup) specimenCard.getParent()).removeView(specimenCard);
            indexColumn.addView(specimenCard, 1, spaced(24));
        }
        listSections();
        paint();
        index.animate().translationX(0f).alpha(1f).setDuration(Pace.GROW).setInterpolator(Pace.EMPHASIS).start();
    }

    /** The cards come in one after another, rising a little as they come. */
    private void arrive() {
        for (int i = 0; i < cards.size(); i++) {
            View card = cards.get(i);
            card.setAlpha(0f);
            card.setTranslationY(Round.px(24f));
            card.animate().alpha(1f).translationY(0f).setStartDelay(i * Pace.STAGGER)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    // ------------------------------------------------------------ the tiles

    /** Four tiles, two by two, large, and the name of the look they wear. */
    private View specimen() {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Round.dp(20f), Round.dp(24f), Round.dp(20f), Round.dp(20f));
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int c = 0; c < 2; c++) {
                ImageView tile = new ImageView(this);
                tile.setScaleType(ImageView.ScaleType.CENTER);
                previews[r * 2 + c] = tile;
                row.addView(tile, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            card.addView(row, spaced(r == 0 ? 0 : 12));
        }
        named = words(Letter.LABEL_L, "", Tone.ON_SURFACE_VARIANT);
        named.setGravity(Gravity.CENTER);
        card.addView(named, spaced(16));
        painters.add(new Runnable() {
            public void run() {
                card.setBackground(Round.box(Tone.of(Tone.SURFACE_HIGH), 32f));
            }
        });
        cards.add(card);
        return card;
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

    /** The tiles drawn again: four tiles are a few milliseconds, so this is done while the finger moves. */
    private void preview() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int tile = Math.round((width - Round.dp(72f)) / 2f * 0.86f);
        for (int i = 0; i < previews.length; i++) {
            previews[i].setImageBitmap(Tile.render(faces[i], tile, look));
        }
        int shape = shapeOf(look);
        String name = shape >= 0 ? shapeNames()[shape] : Words.s("tile");
        named.setText(name + "  \u00b7  " + number(look.ratio, 2) + "  \u00b7  \u00d7" + number(look.zoom, 2));
        if (roundValue != null) {
            roundValue.setText(number(look.power, 1));
            wideValue.setText(number(look.ratio, 2));
            closeValue.setText("\u00d7" + number(look.zoom, 2));
            if (thickValue != null) {
                thickValue.setText(Math.round(look.width * 100f) + "%");
            }
        }
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
        return card;
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
        }).large();
        card.addView(hue, wideRow());
        card.addView(labelled(Words.s("richness"), null), spaced(12));
        rich = new Dial(this, Tone.rich(), new Dial.Moved() {
            public void moved(float value, boolean done) {
                seed(Tone.hue(), value, false);
            }
        }).large();
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
                    boolean on = Keep.wall(Tune.this);
                    int fill = on ? Tone.of(Tone.TERTIARY_CONTAINER) : 0x00000000;
                    int ink = on ? Tone.of(Tone.ON_TERTIARY_CONTAINER) : Tone.of(Tone.ON_SURFACE);
                    wall.setBackground(Round.touch(on ? Round.box(fill, Round.FULL)
                        : Round.ring(fill, Round.FULL, Tone.of(Tone.OUTLINE)), ink, Round.FULL));
                    wall.setTextColor(ink);
                }
            });
            LinearLayout.LayoutParams chip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Round.dp(48f));
            chip.topMargin = Round.dp(20f);
            card.addView(wall, chip);
        }
        return card;
    }

    /** A colour role as a block of itself, with its ink on it. */
    private TextView swatch(final int fill, final int ink) {
        final TextView made = Letter.set(new TextView(this), Letter.HEADLINE_S);
        made.setText("Aa");
        made.setGravity(Gravity.BOTTOM | Gravity.START);
        made.setPadding(Round.dp(16f), 0, Round.dp(16f), Round.dp(12f));
        painters.add(new Runnable() {
            public void run() {
                made.setBackground(Round.box(Tone.of(fill), 24f));
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
                name.setBackground(Round.box(Tone.of(Tone.SURFACE_HIGH), Round.FULL));
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
            row.setBackground(Round.touch(Round.box(Tone.of(Tone.SURFACE_HIGH), 20f),
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
            row.setBackground(Round.touch(Round.box(Tone.of(Tone.SURFACE_HIGH), 20f),
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

    private void closeSheet() {
        if (sheet != null) {
            root.removeView(sheet);
            sheet = null;
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

    /** The system's choice of home screen, in the seed's own colour: the one door out. */
    private View way() {
        final LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(Round.dp(24f), Round.dp(24f), Round.dp(24f), Round.dp(24f));
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chooseHome();
            }
        });
        made.setStateListAnimator(give());
        made.addView(words(Letter.TITLE_L, Words.s("home_screen"), Tone.ON_PRIMARY_CONTAINER));
        made.addView(words(Letter.BODY_M, Words.s("home_screen_what"), Tone.ON_PRIMARY_CONTAINER),
            spaced(4));
        painters.add(new Runnable() {
            public void run() {
                made.setBackground(Round.touch(Round.box(Tone.of(Tone.PRIMARY_CONTAINER), REST),
                    Tone.of(Tone.ON_PRIMARY_CONTAINER), REST));
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

    /** Every colour on the screen, from the seed as it is now. */
    private void paint() {
        int ground = Tone.of(Tone.SURFACE);
        getWindow().setStatusBarColor(ground);
        getWindow().setNavigationBarColor(ground);
        getWindow().getDecorView().setBackgroundColor(ground);
        for (Runnable painter : painters) {
            painter.run();
        }
        // The dials exist only once their subject's screen has been opened.
        int[] neutral = {Tone.of(Tone.SURFACE_HIGHEST), Tone.of(Tone.OUTLINE)};
        for (Dial dial : new Dial[] {round, wide, close, thick}) {
            if (dial != null) {
                dial.colours(neutral);
                dial.ink(Tone.of(Tone.PRIMARY));
            }
        }
        if (hue != null) {
            int[] circle = new int[25];
            for (int i = 0; i < circle.length; i++) {
                circle[i] = Tone.at(72f, 44.0, i * 15f);
            }
            hue.colours(circle);
            hue.ink(Tone.of(Tone.PRIMARY));
        }
        if (rich != null) {
            int[] way = new int[9];
            for (int i = 0; i < way.length; i++) {
                way[i] = Tone.at(72f, Tone.chromaOf(i / 8f), Tone.hue());
            }
            rich.colours(way);
            rich.ink(Tone.of(Tone.PRIMARY));
        }
        for (Cards group : groups) {
            group.paint(false);
        }
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

    /** A card for one subject, its name large at the top. */
    private LinearLayout card(String name) {
        final LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(Round.dp(20f), Round.dp(22f), Round.dp(20f), Round.dp(20f));
        made.addView(words(Letter.HEADLINE_S, name, Tone.ON_SURFACE));
        painters.add(new Runnable() {
            public void run() {
                made.setBackground(Round.box(Tone.of(Tone.SURFACE_CONTAINER), REST));
            }
        });
        cards.add(made);
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

    private TextView value() {
        return words(Letter.TITLE_M, "", Tone.PRIMARY);
    }

    /** A button: filled in the seed's colour for the one thing most likely wanted, tonal for the rest. */
    private TextView button(String text, final boolean filled, View.OnClickListener click) {
        final TextView made = Letter.set(new TextView(this), Letter.LABEL_L);
        made.setText(text);
        made.setGravity(Gravity.CENTER);
        made.setPadding(Round.dp(20f), 0, Round.dp(20f), 0);
        made.setSingleLine(true);
        made.setEllipsize(TextUtils.TruncateAt.END);
        made.setOnClickListener(click);
        made.setStateListAnimator(give());
        painters.add(new Runnable() {
            public void run() {
                int fill = Tone.of(filled ? Tone.PRIMARY : Tone.SECONDARY_CONTAINER);
                int ink = Tone.of(filled ? Tone.ON_PRIMARY : Tone.ON_SECONDARY_CONTAINER);
                made.setBackground(Round.touch(Round.box(fill, Round.FULL), ink, Round.FULL));
                made.setTextColor(ink);
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
     * name; the chosen one is filled with the seed's colour and squares its
     * corners, the others round theirs back, both in motion.
     */
    private final class Cards {

        private final LinearLayout row;
        private final LinearLayout[] items;
        private final Sketch[] marks;
        private final TextView[] names;
        private final GradientDrawable[] fills;
        private final float[] corners;
        private final Picked picked;
        private int chosen;

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
            fills = new GradientDrawable[labels.length];
            corners = new float[labels.length];
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
                fills[i] = Round.box(0, corners[i]);
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

        void select(int which) {
            if (which != chosen) {
                chosen = which;
                paint(true);
            }
        }

        void paint(boolean moving) {
            for (int i = 0; i < items.length; i++) {
                boolean on = i == chosen;
                int fill = Tone.of(on ? Tone.PRIMARY_CONTAINER : Tone.SURFACE_HIGH);
                int ink = Tone.of(on ? Tone.ON_PRIMARY_CONTAINER : Tone.ON_SURFACE);
                int quiet = Tone.of(on ? Tone.ON_PRIMARY_CONTAINER : Tone.ON_SURFACE_VARIANT, 0.45f);
                fills[i].setColor(fill);
                items[i].setBackground(Round.touch(fills[i], ink, REST));
                names[i].setTextColor(ink);
                marks[i].ink(on ? ink : Tone.of(Tone.ON_SURFACE_VARIANT), quiet);
                items[i].setSelected(on);
                morph(i, on ? CHOSEN : REST, moving);
            }
        }

        /** Corners from where they are to where they go: a pressed card squares, a released one rounds. */
        private void morph(final int i, float to, boolean moving) {
            if (!moving || corners[i] == to) {
                corners[i] = to;
                fills[i].setCornerRadius(Round.px(to));
                return;
            }
            ValueAnimator shift = ValueAnimator.ofFloat(corners[i], to);
            shift.setDuration(Pace.GROW);
            shift.setInterpolator(Pace.EMPHASIS);
            shift.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    corners[i] = (Float) a.getAnimatedValue();
                    fills[i].setCornerRadius(Round.px(corners[i]));
                }
            });
            shift.start();
        }
    }

    /** A count between a minus and a plus, the numeral large between them. */
    /** What a count says when a hand moves it: whether the new count may stand. */
    private interface Changed {
        boolean to(int value);
    }

    private final class Stepper {

        final LinearLayout view;
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
            view.addView(words(Letter.TITLE_M, name, Tone.ON_SURFACE_VARIANT),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            view.addView(key(Sketch.MINUS, -1), new LinearLayout.LayoutParams(Round.dp(56f), Round.dp(56f)));
            numeral = words(Letter.DISPLAY_M, String.valueOf(start), Tone.ON_SURFACE);
            numeral.setGravity(Gravity.CENTER);
            view.addView(numeral, new LinearLayout.LayoutParams(Round.dp(88f),
                ViewGroup.LayoutParams.WRAP_CONTENT));
            view.addView(key(Sketch.PLUS, 1), new LinearLayout.LayoutParams(Round.dp(56f), Round.dp(56f)));
            painters.add(new Runnable() {
                public void run() {
                    view.setBackground(Round.box(Tone.of(Tone.SURFACE_HIGH), 24f));
                }
            });
        }

        private View key(int kind, final int step) {
            final FrameLayout key = new FrameLayout(Tune.this);
            final Sketch mark = new Sketch(Tune.this, kind);
            key.addView(mark, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            key.setStateListAnimator(give());
            key.setContentDescription(step < 0 ? "\u2212" : "+");
            key.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int next = Math.max(least, Math.min(most, value + step));
                    if (next == value || changed == null || !changed.to(next)) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                        return;
                    }
                    value = next;
                    numeral.setText(String.valueOf(value));
                    numeral.setScaleX(1.14f);
                    numeral.setScaleY(1.14f);
                    numeral.animate().scaleX(1f).scaleY(1f).setDuration(Pace.GROW)
                        .setInterpolator(Pace.EMPHASIS).start();
                }
            });
            painters.add(new Runnable() {
                public void run() {
                    key.setBackground(Round.touch(Round.box(Tone.of(Tone.SECONDARY_CONTAINER), Round.FULL),
                        Tone.of(Tone.ON_SECONDARY_CONTAINER), Round.FULL));
                    mark.ink(Tone.of(Tone.ON_SECONDARY_CONTAINER), 0);
                }
            });
            return key;
        }
    }
}
