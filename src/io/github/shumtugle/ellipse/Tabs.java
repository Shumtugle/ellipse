package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A row of tabs, as the panels and the list wear them: words in capitals,
 * the chosen one in the scheme's words with the accent drawn under it,
 * the others quiet. In a row that must fit, the words are made smaller
 * together, never one alone, until the row fits.
 */
final class Tabs {

    private Tabs() {
    }

    /** A tab's word, its padding and its look, chosen or not. */
    static TextView tab(Context context, String word, float size, int ink, int quiet, boolean on) {
        float density = context.getResources().getDisplayMetrics().density;
        TextView one = new TextView(context);
        one.setText(Words.t(word).toUpperCase(Locale.getDefault()));
        one.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        one.setLetterSpacing(0.08f);
        one.setGravity(Gravity.CENTER);
        one.setSingleLine(true);
        int side = Math.round(10f * density);
        one.setPadding(side, Math.round(12f * density), side, Math.round(12f * density));
        mark(one, on, ink, quiet);
        return one;
    }

    /** A tab marked chosen, or not. */
    static void mark(TextView one, boolean on, int ink, int quiet) {
        float density = one.getResources().getDisplayMetrics().density;
        one.setTextColor(on ? ink : quiet);
        if (on) {
            GradientDrawable line = new GradientDrawable();
            line.setColor(Tone.primary());
            LayerDrawable under = new LayerDrawable(new android.graphics.drawable.Drawable[] {line});
            under.setLayerGravity(0, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL);
            under.setLayerHeight(0, Math.round(3f * density));
            one.setBackground(under);
        } else {
            one.setBackground(null);
        }
    }

    /**
     * The size words must take for tabs to fit a width side by side: their
     * own size, or smaller, the same for all.
     */
    static float fit(Context context, String[] words, float size, float width) {
        float density = context.getResources().getDisplayMetrics().density;
        TextPaint probe = new TextPaint();
        probe.setTextSize(size);
        probe.setLetterSpacing(0.08f);
        float total = 0f;
        for (String word : words) {
            total += probe.measureText(Words.t(word).toUpperCase(Locale.getDefault())) + 20f * density;
        }
        return total <= width ? size : size * Math.max(0.6f, width / total);
    }

    /** A width the row gives a tab: its word's own, so longer words take more. */
    static LinearLayout.LayoutParams share(TextView one) {
        float grow = one.getPaint().measureText(one.getText().toString()) + one.getPaddingLeft() + one.getPaddingRight();
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, grow);
    }
}
