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

    private final Hand hand;
    private final LinearLayout sheet;
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

        TextView title = Letter.set(new TextView(context), Letter.HEADLINE_M);
        Letter.serif(title);
        title.setText(Words.s("widgets"));
        title.setTextColor(Tone.of(Tone.ON_SURFACE));
        title.setPadding(Round.dp(8f), Round.dp(16f), Round.dp(8f), Round.dp(12f));
        sheet.addView(title);

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, bottom + Round.dp(24f));
        scroll.addView(list);
        sheet.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final PackageManager pm = context.getPackageManager();
        List<AppWidgetProviderInfo> sorted = new ArrayList<AppWidgetProviderInfo>(offered);
        final Collator order = Collator.getInstance();
        order.setStrength(Collator.PRIMARY);
        Collections.sort(sorted, new Comparator<AppWidgetProviderInfo>() {
            public int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                int by = order.compare(owner(pm, a), owner(pm, b));
                return by != 0 ? by : order.compare(a.loadLabel(pm), b.loadLabel(pm));
            }
        });
        for (int i = 0; i < sorted.size(); i++) {
            list.addView(entry(sorted.get(i), pm), spaced(i == 0 ? 0 : 12));
        }

        LayoutParams place = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM);
        place.topMargin = top + Round.dp(48f);
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

    /** One widget: its picture on a card of its own, then its names and its size. */
    private View entry(final AppWidgetProviderInfo info, final PackageManager pm) {
        LinearLayout made = new LinearLayout(getContext());
        made.setOrientation(LinearLayout.VERTICAL);
        made.setPadding(Round.dp(12f), Round.dp(12f), Round.dp(12f), Round.dp(14f));
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
        picture.setAdjustViewBounds(true);
        picture.setMaxHeight(Round.dp(140f));
        GradientDrawable ground = Round.box(Tone.of(Tone.SURFACE_HIGHEST), 16f);
        picture.setBackground(ground);
        picture.setPadding(Round.dp(12f), Round.dp(12f), Round.dp(12f), Round.dp(12f));
        picture.setMinimumHeight(Round.dp(96f));
        made.addView(picture, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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

        TextView name = Letter.set(new TextView(getContext()), Letter.TITLE_M);
        name.setText(info.loadLabel(pm));
        name.setTextColor(Tone.of(Tone.ON_SURFACE));
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams namePlace = spaced(10);
        made.addView(name, namePlace);

        int[] span = hand.span(info);
        TextView about = Letter.set(new TextView(getContext()), Letter.BODY_M);
        about.setText(owner(pm, info) + "  \u00b7  " + span[0] + " \u00d7 " + span[1]);
        about.setTextColor(Tone.of(Tone.ON_SURFACE_VARIANT));
        about.setSingleLine(true);
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
