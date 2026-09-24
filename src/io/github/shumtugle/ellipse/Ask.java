package io.github.shumtugle.ellipse;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A question with a written answer: a card of the menu's make in the
 * middle of the screen, a small caption, a field holding the answer as it
 * stands with all of it chosen so it can be written over, and two words
 * at the foot. The keyboard's own Done answers too; a touch outside the
 * card, or Cancel, leaves the answer as it was.
 */
final class Ask {

    interface Answer {
        void answered(String text);
    }

    private static final String CANCEL = "Cancel";
    private static final String DONE = "Done";

    private Ask() {
    }

    static void show(final FrameLayout host, String caption, String now, final Answer answer) {
        final Context context = host.getContext();
        final float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;

        final FrameLayout veil = new FrameLayout(context);
        veil.setBackgroundColor(0x66000000);
        veil.setClickable(true);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Tone.box(Tone.containerHigh(), 28 * density, 0.5f * density));
        int pad = Math.round(20 * density);
        card.setPadding(pad, Math.round(18 * density), pad, Math.round(10 * density));
        card.setElevation(6 * density);
        card.setClickable(true);

        TextView title = new TextView(context);
        title.setText(caption.toUpperCase(Locale.getDefault()));
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        title.setLetterSpacing(0.12f);
        title.setTextColor(Tone.faint());
        card.addView(title);

        final EditText field = new EditText(context);
        field.setText(now);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        field.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * scaled);
        field.setTextColor(Tone.onSurface());
        field.setBackground(Tone.box(Tone.container(), 16 * density, 0f));
        int inner = Math.round(14 * density);
        field.setPadding(inner, inner, inner, inner);
        field.setHighlightColor((0x59 << 24) | (Tone.primary() & 0x00FFFFFF));
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldParams.topMargin = Math.round(12 * density);
        card.addView(field, fieldParams);

        LinearLayout foot = new LinearLayout(context);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(Gravity.END);
        TextView cancel = word(context, CANCEL, Tone.onSurface(), scaled, density);
        TextView done = word(context, DONE, Tone.primary(), scaled, density);
        foot.addView(cancel);
        foot.addView(done);
        LinearLayout.LayoutParams footParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footParams.topMargin = Math.round(8 * density);
        card.addView(foot, footParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        int side = Math.round(24 * density);
        cardParams.setMargins(side, 0, side, 0);
        veil.addView(card, cardParams);
        host.addView(veil, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final Runnable close = new Runnable() {
            public void run() {
                InputMethodManager keys = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (keys != null) {
                    keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
                }
                veil.animate().alpha(0f).setDuration(Pace.ARRIVE / 2).withEndAction(new Runnable() {
                    public void run() {
                        host.removeView(veil);
                    }
                }).start();
            }
        };
        final Runnable give = new Runnable() {
            public void run() {
                answer.answered(field.getText().toString());
                close.run();
            }
        };
        veil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                close.run();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                close.run();
            }
        });
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                give.run();
            }
        });
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int action, KeyEvent event) {
                give.run();
                return true;
            }
        });

        veil.setAlpha(0f);
        veil.animate().alpha(1f).setDuration(Pace.ARRIVE / 2).start();
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.animate().scaleX(1f).scaleY(1f).setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        field.requestFocus();
        field.selectAll();
        field.postDelayed(new Runnable() {
            public void run() {
                InputMethodManager keys = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (keys != null) {
                    keys.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, Pace.ARRIVE / 2);
    }

    /**
     * A thing said, with a way on: a card of the same make with a small
     * caption, a few words, and two words at the foot, the second of which
     * does what it says.
     */
    static void tell(final FrameLayout host, String caption, String text, String go, final Runnable action) {
        final Context context = host.getContext();
        final float density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        final FrameLayout veil = new FrameLayout(context);
        veil.setBackgroundColor(0x66000000);
        veil.setClickable(true);
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Tone.box(Tone.containerHigh(), 28 * density, 0.5f * density));
        int pad = Math.round(22 * density);
        card.setPadding(pad, Math.round(20 * density), pad, Math.round(10 * density));
        card.setElevation(6 * density);
        card.setClickable(true);
        TextView title = new TextView(context);
        title.setText(caption.toUpperCase(Locale.getDefault()));
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
        title.setLetterSpacing(0.12f);
        title.setTextColor(Tone.faint());
        card.addView(title);
        TextView said = new TextView(context);
        said.setText(text);
        said.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f * scaled);
        said.setTextColor(Tone.onSurface());
        said.setPadding(0, Math.round(12 * density), 0, Math.round(4 * density));
        card.addView(said);
        LinearLayout foot = new LinearLayout(context);
        foot.setGravity(Gravity.END);
        TextView cancel = word(context, CANCEL, Tone.onSurface(), scaled, density);
        TextView yes = word(context, go, Tone.primary(), scaled, density);
        foot.addView(cancel);
        foot.addView(yes);
        card.addView(foot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        int side = Math.round(24 * density);
        cardParams.setMargins(side, 0, side, 0);
        veil.addView(card, cardParams);
        host.addView(veil, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final Runnable close = new Runnable() {
            public void run() {
                veil.animate().alpha(0f).setDuration(Pace.ARRIVE / 2).withEndAction(new Runnable() {
                    public void run() {
                        host.removeView(veil);
                    }
                }).start();
            }
        };
        View.OnClickListener away = new View.OnClickListener() {
            public void onClick(View v) {
                close.run();
            }
        };
        veil.setOnClickListener(away);
        cancel.setOnClickListener(away);
        yes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                close.run();
                action.run();
            }
        });
        veil.setAlpha(0f);
        veil.animate().alpha(1f).setDuration(Pace.ARRIVE / 2).start();
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.animate().scaleX(1f).scaleY(1f).setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
    }

    private static TextView word(Context context, String text, int colour, float scaled, float density) {
        TextView made = new TextView(context);
        made.setText(text);
        made.setTextColor(colour);
        made.setTextSize(TypedValue.COMPLEX_UNIT_PX, 19f * scaled);
        int h = Math.round(16 * density);
        int v = Math.round(12 * density);
        made.setPadding(h, v, h, v);
        made.setBackground(Tone.touch(null, 20 * density));
        return made;
    }
}
