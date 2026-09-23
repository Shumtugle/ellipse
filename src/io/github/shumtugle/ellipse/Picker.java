package io.github.shumtugle.ellipse;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Every widget the phone's applications offer, on a sheet drawn up from
 * below: for each, the picture its application gives of it, its name, the
 * application's name, and how many cells it will take.
 *
 * Pictures are read on a quiet thread and set as they come; a widget
 * without a picture shows its application's icon. A press on a widget
 * chooses it and closes the sheet; a touch above the sheet closes it
 * with nothing chosen.
 */
final class Picker extends FrameLayout {

    interface Hand {
        void picked(AppWidgetProviderInfo info);

        void closed();

        /** How many cells, across and down, a widget will take. */
        int[] span(AppWidgetProviderInfo info);
    }

    /** Three across and four down, as a page of the drawer. */
    private static final int COLUMNS = 3;
    private static final int ROWS = 4;

    private final Hand hand;
    private final LinearLayout sheet;
    private Spread spread;
    private List<AppWidgetProviderInfo> all;
    private final ExecutorService painter = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean closing;

    Picker(Context context, List<AppWidgetProviderInfo> offered, int top, int bottom, Hand hand) {
        super(context);
        this.hand = hand;
        setBackgroundColor(0x66000000);
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
            }
        });

        sheet = new LinearLayout(context);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setClickable(true);
        sheet.setBackground(Round.sheet(Tone.of(Tone.SURFACE_CONTAINER), Round.XL));
        sheet.setPadding(Round.dp(16f), Round.dp(12f), Round.dp(16f), 0);

        View grip = new View(context);
        grip.setBackground(Round.box(Tone.of(Tone.ON_SURFACE_VARIANT, 0.5f), Round.FULL));
        LinearLayout.LayoutParams gripPlace = new LinearLayout.LayoutParams(Round.dp(32f), Round.dp(4f));
        gripPlace.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(grip, gripPlace);

        final android.widget.EditText field = new android.widget.EditText(context);
        field.setSingleLine(true);
        field.setHint(Words.s("search"));
        field.setTextColor(Tone.of(Tone.ON_SURFACE));
        field.setHintTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
        field.setBackground(Round.box(Tone.of(Tone.SURFACE_HIGH), Round.FULL));
        field.setPadding(Round.dp(20f), Round.dp(12f), Round.dp(20f), Round.dp(12f));
        field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);

        spread = new Spread(context);
        // The sheet is drawn up the whole way, and the leaves take whatever
        // is left under its head: the names then stand whole.
        sheet.addView(spread, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        // The line to look in stands at the foot, where the drawer keeps
        // its own and where the hand already is.
        sheet.addView(field, spaced(8));

        final PackageManager pm = context.getPackageManager();
        all = new ArrayList<AppWidgetProviderInfo>(offered);
        final Collator order = Collator.getInstance();
        order.setStrength(Collator.PRIMARY);
        Collections.sort(all, new Comparator<AppWidgetProviderInfo>() {
            public int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                int by = order.compare(owner(pm, a), owner(pm, b));
                return by != 0 ? by : order.compare(a.loadLabel(pm), b.loadLabel(pm));
            }
        });
        lay(all, pm);
        field.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence text, int from, int count, int after) {
            }

            public void onTextChanged(CharSequence text, int from, int before, int count) {
                lay(sift(text.toString(), pm), pm);
            }

            public void afterTextChanged(android.text.Editable text) {
            }
        });

        LayoutParams place = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
            Gravity.BOTTOM);
        place.topMargin = top + Round.dp(12f);
        sheet.setPadding(Round.dp(16f), Round.dp(12f), Round.dp(16f), bottom + Round.dp(16f));
        addView(sheet, place);
    }

    private static String owner(PackageManager pm, AppWidgetProviderInfo info) {
        try {
            return String.valueOf(pm.getApplicationLabel(
                pm.getApplicationInfo(info.provider.getPackageName(), 0)));
        } catch (PackageManager.NameNotFoundException gone) {
            return info.provider.getPackageName();
        }
    }

    private static LinearLayout.LayoutParams spaced(int above) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Round.dp(above);
        return params;
    }

    /** Those whose name, or their application's, holds what was typed. */
    private List<AppWidgetProviderInfo> sift(String said, PackageManager pm) {
        String looked = said.trim().toLowerCase(java.util.Locale.getDefault());
        if (looked.length() == 0) {
            return all;
        }
        List<AppWidgetProviderInfo> kept = new ArrayList<AppWidgetProviderInfo>();
        for (AppWidgetProviderInfo one : all) {
            String name = String.valueOf(one.loadLabel(pm)).toLowerCase(java.util.Locale.getDefault());
            String from = owner(pm, one).toLowerCase(java.util.Locale.getDefault());
            if (name.contains(looked) || from.contains(looked)) {
                kept.add(one);
            }
        }
        return kept;
    }

    /** The widgets laid out on leaves, three across and four down, turned by a finger. */
    private void lay(List<AppWidgetProviderInfo> shown, PackageManager pm) {
        spread.removeAllViews();
        int each = COLUMNS * ROWS;
        int leaves = Math.max(1, (shown.size() + each - 1) / each);
        for (int leaf = 0; leaf < leaves; leaf++) {
            LinearLayout page = new LinearLayout(getContext());
            page.setOrientation(LinearLayout.VERTICAL);
            for (int row = 0; row < ROWS; row++) {
                LinearLayout across = new LinearLayout(getContext());
                across.setOrientation(LinearLayout.HORIZONTAL);
                for (int column = 0; column < COLUMNS; column++) {
                    int at = leaf * each + row * COLUMNS + column;
                    LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                    place.setMargins(Round.dp(4f), Round.dp(4f), Round.dp(4f), Round.dp(4f));
                    if (at < shown.size()) {
                        across.addView(entry(shown.get(at), pm), place);
                    } else {
                        across.addView(new View(getContext()), place);
                    }
                }
                page.addView(across, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            }
            spread.addView(page);
        }
        spread.rewind();
        spread.requestLayout();
    }

    /** Leaves of widgets, turned sideways, counted by dots at their foot. */
    private static final class Spread extends Leaves {

        Spread(Context context) {
            super(context);
        }

        @Override
        int leaves() {
            return getChildCount();
        }

        @Override
        float dotsAt() {
            return getHeight() - Round.px(16f);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int height = MeasureSpec.getSize(heightSpec);
            int room = Math.max(0, height - Round.dp(26f));
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(room, MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = getWidth();
            int room = Math.max(0, getHeight() - Round.dp(26f));
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).layout(i * width, 0, (i + 1) * width, room);
            }
            settleAfterLayout();
        }
    }

    /** One widget: its picture on a card of its own, then its names and its size. */
    private View entry(final AppWidgetProviderInfo info, final PackageManager pm) {
        LinearLayout made = new LinearLayout(getContext());
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(Round.dp(8f), Round.dp(8f), Round.dp(8f), Round.dp(10f));
        made.setBackground(Round.touch(Round.box(Tone.of(Tone.SURFACE_HIGH), 24f),
            Tone.of(Tone.ON_SURFACE), 24f));
        made.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!closing) {
                    hand.picked(info);
                    close();
                }
            }
        });

        final ImageView picture = new ImageView(getContext());
        picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable ground = Round.box(Tone.of(Tone.SURFACE_HIGHEST), 16f);
        picture.setBackground(ground);
        picture.setPadding(Round.dp(8f), Round.dp(8f), Round.dp(8f), Round.dp(8f));
        // The picture takes whatever the cell has left over from the names.
        made.addView(picture, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        final Context context = getContext();
        final int dpi = App.dpi(context);
        painter.execute(new Runnable() {
            public void run() {
                Drawable shown = null;
                try {
                    shown = info.loadPreviewImage(context, dpi);
                    if (shown == null) {
                        shown = info.loadIcon(context, dpi);
                    }
                } catch (RuntimeException unreadable) {
                    shown = null;
                }
                final Drawable ready = shown;
                main.post(new Runnable() {
                    public void run() {
                        picture.setImageDrawable(ready);
                    }
                });
            }
        });

        TextView name = Letter.set(new TextView(getContext()), Letter.TITLE_S);
        name.setText(info.loadLabel(pm));
        name.setTextColor(Tone.of(Tone.ON_SURFACE));
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams namePlace = spaced(8);
        made.addView(name, namePlace);

        int[] span = hand.span(info);
        TextView about = Letter.set(new TextView(getContext()), Letter.LABEL_M);
        about.setText(span[0] + " \u00d7 " + span[1]);
        about.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
        about.setSingleLine(true);
        about.setGravity(Gravity.CENTER_HORIZONTAL);
        about.setEllipsize(TextUtils.TruncateAt.END);
        made.addView(about, spaced(2));
        return made;
    }

    /** Drawn up from below the glass. */
    void show() {
        setAlpha(0f);
        animate().alpha(1f).setDuration(Pace.SHEET).setInterpolator(Pace.STANDARD).start();
        sheet.post(new Runnable() {
            public void run() {
                sheet.setTranslationY(sheet.getHeight());
                sheet.animate().translationY(0f).setDuration(Pace.ARRIVE)
                    .setInterpolator(Pace.STANDARD).start();
            }
        });
    }

    void close() {
        if (closing || getParent() == null) {
            return;
        }
        closing = true;
        painter.shutdownNow();
        final FrameLayout stage = (FrameLayout) getParent();
        sheet.animate().translationY(sheet.getHeight()).setDuration(Pace.LEAVE)
            .setInterpolator(Pace.AWAY).start();
        animate().alpha(0f).setDuration(Pace.LEAVE).setInterpolator(Pace.AWAY)
            .withEndAction(new Runnable() {
                public void run() {
                    stage.removeView(Picker.this);
                }
            }).start();
        hand.closed();
    }
}
