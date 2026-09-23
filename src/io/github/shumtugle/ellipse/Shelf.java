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
    }

    private static final String WIDGETS = "Widgets";
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
    private final LinearLayout head;
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

        head = new LinearLayout(context);
        head.setOrientation(LinearLayout.VERTICAL);
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(20), dp(6), dp(16), dp(6));
        Glyph lens = new Glyph(context, Glyph.SEARCH, dp(24));
        pill.addView(lens);
        field = new EditText(context);
        field.setBackground(null);
        field.setHint(WIDGETS);
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f * scaled);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.setPadding(dp(18), dp(12), dp(8), dp(12));
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void afterTextChanged(Editable t) {
                build();
            }
        });
        pill.addView(field, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pillParams.setMargins(dp(16), dp(12), dp(16), dp(8));
        head.addView(pill, pillParams);
        column.addView(head);
        pill.setTag(lens);

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
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    /** Keeps the field clear of the status bar and the last card clear of the foot. */
    void inset(int top, int bottom) {
        head.setPadding(0, top, 0, 0);
        scroll.setPadding(0, 0, 0, bottom);
    }

    boolean shown() {
        return shown;
    }

    /** Reads what every application offers, and rises. */
    void show() {
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
        InputMethodManager keys = (InputMethodManager)
            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keys != null) {
            keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
        field.clearFocus();
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
        View pill = head.getChildAt(0);
        pill.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        ((Glyph) pill.getTag()).tint(Tone.faint());
        field.setTextColor(Tone.onSurface());
        field.setHintTextColor(Tone.faint());
    }

    /** The cards, narrowed by the field; the one opened stays open. */
    private void build() {
        cards.removeAllViews();
        String typed = Match.norm(field.getText().toString());
        PackageManager manager = getContext().getPackageManager();
        int shownCount = 0;
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
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
        name.setSingleLine(true);
        words.addView(name);
        TextView count = new TextView(getContext());
        count.setText(offers.size() == 1 ? ONE : offers.size() + MANY);
        count.setTextColor(Tone.faint());
        count.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
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
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, dp(12), 0, 0);
            one.addView(label);
            int[] span = hand.span(info);
            TextView size = new TextView(context);
            size.setText(span[0] + " \u00D7 " + span[1]);
            size.setTextColor(Tone.faint());
            size.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
            size.setGravity(Gravity.CENTER);
            one.addView(size);
            if (Build.VERSION.SDK_INT >= 31) {
                CharSequence about = info.loadDescription(context);
                if (about != null && about.length() > 0) {
                    TextView said = new TextView(context);
                    said.setText(about);
                    said.setTextColor(Tone.faint());
                    said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
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
