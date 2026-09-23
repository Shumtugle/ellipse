package io.github.shumtugle.ellipse;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * The one menu of the application: a card that grows out of the point it
 * was asked for at, a round button or a fingertip, and stands beside that
 * point rather than at an edge of the screen. Its lines follow one after
 * another, like icons arriving; a chosen line wears a tonal pill of the
 * accent; a section without a name is set off by a hairline. A touch
 * anywhere else, or Back, sends it back into its point.
 */
final class Menu {

    /** One section: an optional caption, its lines, and the key of the one chosen, if any. */
    static final class Section {
        final String caption;
        final String[] lines;
        final int[] keys;
        int chosen = -1;
        /** A small picture before each line, where there is one. */
        android.graphics.drawable.Drawable[] icons;
        /**
         * When set, the section is not lines but a strip of tools, each a
         * drawing with its word under it, on a tone of its own: the home
         * screen's own doings, kept apart from what an app offers.
         */
        int[] tools;

        Section(String caption, String[] lines, int[] keys) {
            this.caption = caption;
            this.lines = lines;
            this.keys = keys;
        }
    }

    interface Listener {
        void picked(int section, int key);

        /** The menu has started to close, however it was asked to. */
        void closing();
    }

    private static final float WIDTH = 296f;

    private final Context context;
    private final FrameLayout host;
    private final Listener listener;
    private final View veil;
    private final LinearLayout card;
    /** What the card holds, in a scroll of its own for a menu taller than most of the screen. */
    private final android.widget.ScrollView scroll;
    private final LinearLayout body;
    private final float density;
    private final float scaled;
    private Section[] sections = new Section[0];
    private boolean shown;

    Menu(Context context, FrameLayout host, Listener listener) {
        this.context = context;
        this.host = host;
        this.listener = listener;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;

        veil = new View(context);
        veil.setVisibility(View.GONE);
        veil.setClickable(true);
        veil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hide(true);
            }
        });
        host.addView(veil, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setElevation(dp(6));
        card.setVisibility(View.GONE);
        card.setClickable(true);
        scroll = new android.widget.ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        card.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        host.addView(card, new FrameLayout.LayoutParams(dp(WIDTH),
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START));
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    boolean shown() {
        return shown;
    }

    /** Marks a line as chosen, whether the menu stands open or not. */
    void choose(int section, int key) {
        if (section < sections.length) {
            sections[section].chosen = key;
            if (shown) {
                build();
            }
        }
    }

    private void build() {
        body.removeAllViews();
        card.setBackground(Tone.box(Tone.containerHigh(), dp(28), dp(0.5f)));
        for (int s = 0; s < sections.length; s++) {
            Section section = sections[s];
            if (section.tools != null) {
                body.addView(strip(section, s));
                continue;
            }
            if (s > 0 && section.caption == null) {
                /* A section with no name of its own is set off from the one
                   before by a hairline: it is another kind of line. */
                View rule = new View(context);
                rule.setBackgroundColor(Tone.outline());
                LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
                ruleParams.setMargins(dp(16), dp(8), dp(16), dp(6));
                body.addView(rule, ruleParams);
            }
            if (section.caption != null) {
                TextView caption = new TextView(context);
                caption.setText(section.caption.toUpperCase(Locale.ROOT));
                caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
                caption.setLetterSpacing(0.12f);
                caption.setTextColor(Tone.faint());
                caption.setPadding(dp(16), dp(s == 0 ? 6 : 14), dp(16), dp(8));
                body.addView(caption);
            }
            for (int i = 0; i < section.lines.length; i++) {
                final int which = s;
                final int key = section.keys[i];
                boolean on = key == section.chosen;
                TextView line = new TextView(context);
                line.setText(section.lines[i]);
                line.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
                line.setSingleLine(true);
                line.setGravity(Gravity.CENTER_VERTICAL);
                line.setPadding(dp(18), dp(16), dp(18), dp(16));
                int fill = on ? ((0x2E << 24) | (Tone.primary() & 0x00FFFFFF)) : 0x00000000;
                line.setBackground(Tone.touch(Tone.box(fill, dp(20), 0f), dp(20)));
                line.setTextColor(on ? Tone.primary() : Tone.onSurface());
                if (section.icons != null && i < section.icons.length && section.icons[i] != null) {
                    android.graphics.drawable.Drawable icon = section.icons[i].mutate();
                    icon.setBounds(0, 0, dp(32), dp(32));
                    line.setCompoundDrawables(icon, null, null, null);
                    line.setCompoundDrawablePadding(dp(14));
                    line.setPadding(dp(14), dp(12), dp(18), dp(12));
                }
                line.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        listener.picked(which, key);
                    }
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = dp(2);
                body.addView(line, params);
            }
        }
    }

    /** A strip of tools: equal parts side by side, a drawing over a word. */
    private View strip(Section section, final int which) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackground(Tone.box(Tone.container(), dp(22), 0f));
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int i = 0; i < section.lines.length; i++) {
            final int key = section.keys[i];
            LinearLayout tool = new LinearLayout(context);
            tool.setOrientation(LinearLayout.VERTICAL);
            tool.setGravity(Gravity.CENTER_HORIZONTAL);
            tool.setPadding(dp(4), dp(12), dp(4), dp(10));
            tool.setBackground(Tone.touch(null, dp(18)));
            Glyph drawing = new Glyph(context, section.tools[i], dp(28));
            drawing.tint(Tone.onSurface());
            tool.addView(drawing, new LinearLayout.LayoutParams(dp(28), dp(28)));
            TextView word = new TextView(context);
            word.setText(section.lines[i]);
            word.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
            word.setTextColor(Tone.onSurface());
            word.setSingleLine(true);
            word.setGravity(Gravity.CENTER);
            word.setPadding(0, dp(6), 0, 0);
            tool.addView(word);
            tool.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.picked(which, key);
                }
            });
            row.addView(tool, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(which > 0 ? 10 : 2);
        row.setLayoutParams(params);
        return row;
    }

    /**
     * Opens beside a point of the host. The card keeps a gap from the point,
     * stands above it in the lower half of the screen and below it in the
     * upper half, leans toward the middle, and never leaves the host.
     */
    void show(Section[] sections, float x, float y, float gap) {
        this.sections = sections;
        shown = true;
        build();
        LinearLayout.LayoutParams fit = (LinearLayout.LayoutParams) scroll.getLayoutParams();
        fit.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        scroll.setLayoutParams(fit);
        card.measure(View.MeasureSpec.makeMeasureSpec(dp(WIDTH), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        float most = host.getHeight() * 0.7f;
        if (card.getMeasuredHeight() > most) {
            fit.height = Math.round(most) - card.getPaddingTop() - card.getPaddingBottom();
            scroll.setLayoutParams(fit);
            scroll.scrollTo(0, 0);
            card.measure(View.MeasureSpec.makeMeasureSpec(dp(WIDTH), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        }
        float w = dp(WIDTH);
        float h = card.getMeasuredHeight();
        float edge = dp(8);
        float innerLeft = host.getPaddingLeft() + edge;
        float innerRight = host.getWidth() - host.getPaddingRight() - edge;
        float left = x > host.getWidth() / 2f ? x + dp(24) - w : x - dp(24);
        left = Math.max(innerLeft, Math.min(innerRight - w, left));
        float top = y > host.getHeight() / 2f ? y - gap - h : y + gap;
        top = Math.max(edge, Math.min(host.getHeight() - edge - h, top));

        card.setTranslationX(left - host.getPaddingLeft());
        card.setTranslationY(top - host.getPaddingTop());
        card.setPivotX(x - left);
        card.setPivotY(y - top);
        veil.setVisibility(View.VISIBLE);
        card.setVisibility(View.VISIBLE);
        veil.bringToFront();
        card.bringToFront();
        card.animate().cancel();
        card.setAlpha(0f);
        card.setScaleX(0.2f);
        card.setScaleY(0.2f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(Pace.ARRIVE)
            .setInterpolator(Pace.EMPHASIS).withEndAction(null).start();
        for (int i = 0; i < body.getChildCount(); i++) {
            View line = body.getChildAt(i);
            line.setAlpha(0f);
            line.setTranslationY(dp(10));
            line.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP * (i + 1))
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    /** Back into the point it came from; at once, when nobody is looking. */
    void hide(boolean slowly) {
        if (!shown) {
            return;
        }
        shown = false;
        veil.setVisibility(View.GONE);
        listener.closing();
        card.animate().cancel();
        if (!slowly) {
            card.setVisibility(View.GONE);
            return;
        }
        card.animate().alpha(0f).scaleX(0.2f).scaleY(0.2f).setDuration(Pace.ARRIVE / 2)
            .setInterpolator(Pace.EMPHASIS).withEndAction(new Runnable() {
                public void run() {
                    if (!shown) {
                        card.setVisibility(View.GONE);
                    }
                }
            }).start();
    }
}
