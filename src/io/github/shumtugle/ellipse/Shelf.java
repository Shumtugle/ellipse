package io.github.shumtugle.ellipse;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shelf of widgets: every application that offers any, by name, one
 * card each, with how many it offers. A card opens in place to show them,
 * each as its own picture of itself, its name, the places it will take on
 * a screen, and what it is for. A touch on one sets it on the screen the
 * shelf was opened from. A field at the top narrows the shelf by the
 * name of an application or of a widget.
 */
final class Shelf extends FrameLayout {

    interface Hand {
        /** How many places across and down a widget will take. */
        int[] span(AppWidgetProviderInfo info);

        void chosen(AppWidgetProviderInfo info);

        /** Whether the launcher's own clock stands on a screen now. */
        boolean clockStands();

        /** The launcher's own clock was chosen. */
        void clock();

        /** The launcher's settings were asked for from the shelf's menu. */
        void settings();
    }

    private static final String WIDGETS = "Search widgets";
    private static final String OWN = "Ellipse";
    private static final String CLOCK = "The clock";
    private static final String STANDS = "The clock \u00B7 on the home screen";
    private static final String SET = "Tap to set it on the home screen";
    private static final String SHOW = "Tap to go to it";
    /** The key the launcher's own card is opened under: no package is named so. */
    private static final String OWN_KEY = "#own";
    private static final String SETTINGS = "Settings";
    private static final String ONE = "1 widget";
    private static final String MANY = " widgets";

    /** One application and what it offers. */
    private static final class Maker {
        final String owner;
        final CharSequence name;
        final Drawable icon;
        final List<AppWidgetProviderInfo> offers = new ArrayList<>();

        Maker(String owner, CharSequence name, Drawable icon) {
            this.owner = owner;
            this.name = name;
            this.icon = icon;
        }
    }

    private final Hand hand;
    private final float density;
    private final float scaled;
    private final Foot foot;
    private final EditText field;
    private final ScrollView scroll;
    private final LinearLayout cards;
    private final List<Maker> makers = new ArrayList<>();
    private String opened;
    private boolean shown;

    Shelf(Context context, Hand hand) {
        super(context);
        this.hand = hand;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setVisibility(GONE);
        setClickable(true);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        cards = new LinearLayout(context);
        cards.setOrientation(LinearLayout.VERTICAL);
        cards.setPadding(dp(12), dp(4), dp(12), dp(16));
        scroll.addView(cards);
        column.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        foot = new Foot(context, this, WIDGETS, new Foot.Owner() {
            public Menu.Section[] sections() {
                return new Menu.Section[] {Home.settingsLine(SETTINGS, 0)};
            }

            public void picked(int section, int key) {
                Shelf.this.hand.settings();
            }

            public void leave() {
                close(true);
            }

            public void typed(String text) {
                build();
            }
        });
        column.addView(foot, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        field = foot.field();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    /** Keeps the field clear of the status bar and the last card clear of the foot. */
    void inset(int top, int bottom) {
        scroll.setPadding(0, top, 0, 0);
        foot.lift(bottom);
    }

    boolean shown() {
        return shown;
    }

    /** Lines of cards that open in place, or every widget at once, two to a row. */
    private boolean grid;

    /** Reads what every application offers, and rises. */
    void show(boolean grid) {
        this.grid = grid;
        Context context = getContext();
        PackageManager manager = context.getPackageManager();
        Map<String, Maker> byOwner = new LinkedHashMap<>();
        for (AppWidgetProviderInfo info : AppWidgetManager.getInstance(context).getInstalledProviders()) {
            String owner = info.provider.getPackageName();
            Maker maker = byOwner.get(owner);
            if (maker == null) {
                CharSequence name = owner;
                Drawable icon = null;
                try {
                    name = manager.getApplicationLabel(manager.getApplicationInfo(owner, 0));
                    icon = manager.getApplicationIcon(owner);
                } catch (PackageManager.NameNotFoundException gone) {
                    // Named by its package, then.
                }
                maker = new Maker(owner, name, icon);
                byOwner.put(owner, maker);
            }
            maker.offers.add(info);
        }
        makers.clear();
        makers.addAll(byOwner.values());
        final Collator order = Collator.getInstance();
        Collections.sort(makers, new Comparator<Maker>() {
            public int compare(Maker a, Maker b) {
                return order.compare(a.name.toString(), b.name.toString());
            }
        });
        opened = null;
        field.setText("");
        tint();
        build();
        Style.apply(this);
        scroll.scrollTo(0, 0);
        shown = true;
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(dp(72));
        animate().cancel();
        animate().alpha(1f).translationY(0f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
    }

    void close(boolean slowly) {
        if (!shown) {
            return;
        }
        shown = false;
        foot.shutMenu(false);
        foot.hideKeys();
        animate().cancel();
        if (!slowly) {
            setVisibility(GONE);
            return;
        }
        animate().alpha(0f).translationY(dp(48)).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        setVisibility(GONE);
                    }
                }
            }).start();
    }

    private void tint() {
        setBackgroundColor(Tone.surface());
        foot.tint();
    }

    /** The cards, narrowed by the field; the one opened stays open. */
    private void build() {
        cards.removeAllViews();
        String typed = Match.norm(field.getText().toString());
        PackageManager manager = getContext().getPackageManager();
        int shownCount = 0;
        /* The launcher's own first, above every application's. */
        if (typed.length() == 0 || Match.rank(Match.norm(OWN), typed) != Match.NONE
            || Match.rank(Match.norm(CLOCK), typed) != Match.NONE) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(6);
            cards.addView(own(typed.length() > 0), params);
            shownCount++;
        }
        for (final Maker maker : makers) {
            List<AppWidgetProviderInfo> offers = maker.offers;
            if (typed.length() > 0 && Match.rank(Match.norm(maker.name.toString()), typed) == Match.NONE) {
                List<AppWidgetProviderInfo> matching = new ArrayList<>();
                for (AppWidgetProviderInfo info : offers) {
                    if (Match.rank(Match.norm(info.loadLabel(manager)), typed) != Match.NONE) {
                        matching.add(info);
                    }
                }
                if (matching.isEmpty()) {
                    continue;
                }
                offers = matching;
            }
            if (grid) {
                shownCount = tiles(maker, offers, shownCount);
                continue;
            }
            boolean open = maker.owner.equals(opened) || (typed.length() > 0 && offers != maker.offers);
            View card = card(maker, offers, open);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(6);
            cards.addView(card, params);
            if (shownCount < 10) {
                card.setAlpha(0f);
                card.setTranslationY(dp(10));
                card.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP / 2 * shownCount)
                    .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
            }
            shownCount++;
        }
    }

    /** An app's widgets as tiles, two to a row, under the app's name; returns how many groups stand now. */
    private int tiles(Maker maker, List<AppWidgetProviderInfo> offers, int count) {
        Context context = getContext();
        TextView caption = new TextView(context);
        caption.setText(maker.name.toString().toUpperCase(java.util.Locale.getDefault()));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setTextColor(Tone.faint());
        caption.setPadding(dp(12), dp(count == 0 ? 8 : 22), dp(12), dp(6));
        cards.addView(caption);
        LinearLayout row = null;
        for (int i = 0; i < offers.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                cards.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            View tile = tile(offers.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(tile, params);
        }
        if (offers.size() % 2 == 1 && row != null) {
            row.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        return count + 1;
    }

    /** One widget as a tile: its picture of itself, its name and the places it takes. */
    private View tile(final AppWidgetProviderInfo info) {
        Context context = getContext();
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
        LinearLayout one = new LinearLayout(context);
        one.setOrientation(LinearLayout.VERTICAL);
        one.setGravity(Gravity.CENTER_HORIZONTAL);
        one.setPadding(dp(10), dp(14), dp(10), dp(14));
        one.setBackground(Tone.touch(Tone.box(Tone.container(), dp(22), 0f), dp(22)));
        Drawable picture = info.loadPreviewImage(context, dpi);
        if (picture == null) {
            picture = info.loadIcon(context, dpi);
        }
        ImageView image = new ImageView(context);
        image.setImageDrawable(picture);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(dp(130));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        one.addView(image, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView label = new TextView(context);
        label.setText(info.loadLabel(context.getPackageManager()));
        label.setTextColor(Tone.onSurface());
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setPadding(0, dp(10), 0, 0);
        one.addView(label);
        int[] span = hand.span(info);
        TextView size = new TextView(context);
        size.setText(span[0] + " \u00D7 " + span[1]);
        size.setTextColor(Tone.faint());
        size.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        size.setGravity(Gravity.CENTER);
        one.addView(size);
        one.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hand.chosen(info);
            }
        });
        return one;
    }

    private View card(final Maker maker, final List<AppWidgetProviderInfo> offers, boolean open) {
        final LinearLayout made = new LinearLayout(getContext());
        made.setOrientation(LinearLayout.VERTICAL);
        made.setBackground(Tone.box(Tone.container(), dp(24), 0f));

        LinearLayout top = new LinearLayout(getContext());
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(20), dp(16), dp(20), dp(16));
        top.setBackground(Tone.touch(null, dp(24)));
        ImageView icon = new ImageView(getContext());
        icon.setImageDrawable(maker.icon);
        top.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout words = new LinearLayout(getContext());
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(20), 0, dp(12), 0);
        TextView name = new TextView(getContext());
        name.setText(maker.name);
        name.setTextColor(Tone.onSurface());
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 21f * scaled);
        name.setSingleLine(true);
        words.addView(name);
        TextView count = new TextView(getContext());
        count.setText(offers.size() == 1 ? Words.t(ONE) : offers.size() + " " + Words.t(MANY.trim()));
        count.setTextColor(Tone.faint());
        count.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        words.addView(count);
        top.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final Glyph chevron = new Glyph(getContext(), Glyph.CHEVRON, dp(24));
        chevron.tint(Tone.faint());
        chevron.setRotation(open ? 180f : 0f);
        top.addView(chevron);
        made.addView(top);

        final LinearLayout inside = new LinearLayout(getContext());
        inside.setOrientation(LinearLayout.VERTICAL);
        inside.setPadding(dp(16), 0, dp(16), dp(12));
        made.addView(inside);
        if (open) {
            fillInside(inside, offers);
        }
        top.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean opening = inside.getChildCount() == 0;
                chevron.animate().rotation(opening ? 180f : 0f).setDuration(Pace.ARRIVE / 2)
                    .setInterpolator(Pace.EMPHASIS).start();
                if (opening) {
                    opened = maker.owner;
                    fillInside(inside, offers);
                } else {
                    if (maker.owner.equals(opened)) {
                        opened = null;
                    }
                    inside.removeAllViews();
                }
            }
        });
        return made;
    }

    /**
     * The launcher's own card: its name and its clock; opened, the clock
     * itself, live, as it will stand, and a tap sets it on the home
     * screen — or, if it stands already, goes to it.
     */
    private View own(boolean open) {
        final Context context = getContext();
        final LinearLayout made = new LinearLayout(context);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setBackground(Tone.box(Tone.container(), dp(24), 0f));
        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(20), dp(16), dp(20), dp(16));
        top.setBackground(Tone.touch(null, dp(24)));
        ImageView icon = new ImageView(context);
        icon.setImageDrawable(context.getApplicationInfo().loadIcon(context.getPackageManager()));
        top.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout words = new LinearLayout(context);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(20), 0, dp(12), 0);
        TextView name = new TextView(context);
        name.setText(OWN);
        name.setTextColor(Tone.onSurface());
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 21f * scaled);
        name.setSingleLine(true);
        words.addView(name);
        TextView what = new TextView(context);
        what.setText(Words.t(hand.clockStands() ? STANDS : CLOCK));
        what.setTextColor(Tone.faint());
        what.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        words.addView(what);
        top.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final Glyph chevron = new Glyph(context, Glyph.CHEVRON, dp(24));
        chevron.tint(Tone.faint());
        chevron.setRotation(open || OWN_KEY.equals(opened) ? 180f : 0f);
        top.addView(chevron);
        made.addView(top);
        final LinearLayout inside = new LinearLayout(context);
        inside.setOrientation(LinearLayout.VERTICAL);
        inside.setPadding(dp(16), 0, dp(16), dp(16));
        made.addView(inside);
        if (open || OWN_KEY.equals(opened)) {
            ownInside(inside);
        }
        top.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean opening = inside.getChildCount() == 0;
                chevron.animate().rotation(opening ? 180f : 0f).setDuration(Pace.ARRIVE / 2)
                    .setInterpolator(Pace.EMPHASIS).start();
                if (opening) {
                    opened = OWN_KEY;
                    ownInside(inside);
                } else {
                    if (OWN_KEY.equals(opened)) {
                        opened = null;
                    }
                    inside.removeAllViews();
                }
            }
        });
        return made;
    }

    /** The clock, live, in a window of the card, and what a tap on it does. */
    private void ownInside(LinearLayout inside) {
        Context context = getContext();
        View clock = Home.timepiece(context, new Almanac.Hand() {
            public void pressed(String window, View from, android.graphics.RectF box) {
                hand.clock();
            }
        });
        ((Timepiece) clock).weather(Keep.flag(context, Keep.WEATHER, true));
        ((Timepiece) clock).ears(-1);
        FrameLayout frame = new FrameLayout(context);
        frame.setBackground(Tone.touch(null, dp(20)));
        frame.addView(clock, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132)));
        View.OnClickListener take = new View.OnClickListener() {
            public void onClick(View v) {
                hand.clock();
            }
        };
        /* A cover over the live clock takes every tap, so no window of it
           opens anything here. */
        View cover = new View(context);
        cover.setOnClickListener(take);
        frame.addView(cover, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132)));
        inside.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView said = new TextView(context);
        said.setText(Words.t(hand.clockStands() ? SHOW : SET));
        said.setTextColor(Tone.faint());
        said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        said.setGravity(Gravity.CENTER_HORIZONTAL);
        said.setPadding(0, dp(8), 0, 0);
        inside.addView(said);
    }

    /** Each widget as a picture of itself, with its name, its places and what it is for. */
    private void fillInside(LinearLayout inside, List<AppWidgetProviderInfo> offers) {
        Context context = getContext();
        PackageManager manager = context.getPackageManager();
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
        for (int i = 0; i < offers.size(); i++) {
            final AppWidgetProviderInfo info = offers.get(i);
            LinearLayout one = new LinearLayout(context);
            one.setOrientation(LinearLayout.VERTICAL);
            one.setGravity(Gravity.CENTER_HORIZONTAL);
            one.setPadding(dp(8), dp(16), dp(8), dp(16));
            one.setBackground(Tone.touch(null, dp(20)));
            Drawable picture = info.loadPreviewImage(context, dpi);
            if (picture == null) {
                picture = info.loadIcon(context, dpi);
            }
            ImageView image = new ImageView(context);
            image.setImageDrawable(picture);
            image.setAdjustViewBounds(true);
            image.setMaxHeight(dp(200));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            one.addView(image, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView label = new TextView(context);
            label.setText(info.loadLabel(manager));
            label.setTextColor(Tone.onSurface());
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, dp(12), 0, 0);
            one.addView(label);
            int[] span = hand.span(info);
            TextView size = new TextView(context);
            size.setText(span[0] + " \u00D7 " + span[1]);
            size.setTextColor(Tone.faint());
            size.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
            size.setGravity(Gravity.CENTER);
            one.addView(size);
            if (Build.VERSION.SDK_INT >= 31) {
                CharSequence about = info.loadDescription(context);
                if (about != null && about.length() > 0) {
                    TextView said = new TextView(context);
                    said.setText(about);
                    said.setTextColor(Tone.faint());
                    said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
                    said.setGravity(Gravity.CENTER);
                    one.addView(said);
                }
            }
            one.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hand.chosen(info);
                }
            });
            inside.addView(one);
            one.setAlpha(0f);
            one.setTranslationY(dp(8));
            one.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP * i)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }
}
