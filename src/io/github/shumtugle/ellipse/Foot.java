package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.content.Context;
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
import android.widget.LinearLayout;

/**
 * The bar at the foot of every screen that lists things: the list of
 * every application, the shelf of widgets, the makers of shortcuts, the
 * settings. One make everywhere, so every such screen is used the same
 * way: a field to find what is listed, saying what it finds, and the round
 * button of the screen's own menu, three marks one above the other. Open,
 * the menu's marks draw together into a cross; the cross leads home.
 */
final class Foot extends LinearLayout {

    interface Owner {
        /** What the screen's menu holds now. */
        Menu.Section[] sections();

        void picked(int section, int key);

        /** The cross was pressed: the screen is to be left for the home screen. */
        void leave();

        void typed(String text);
    }

    private static final String MENU = "Menu";
    private static final String BACK = "Back";

    private final FrameLayout back;
    private final Glyph backGlyph;

    private final Owner owner;
    private final EditText field;
    private final Blob blob;
    private final Menu menu;
    private final float density;
    private ValueAnimator turning;

    Foot(Context context, FrameLayout host, String hint, final Owner owner) {
        super(context);
        this.owner = owner;
        this.host = host;
        density = context.getResources().getDisplayMetrics().density;
        float scaled = context.getResources().getDisplayMetrics().scaledDensity;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(14), dp(14), dp(12), dp(14));

        field = new EditText(context);
        field.setBackground(null);
        field.setPadding(dp(6), dp(6), dp(10), dp(6));
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        field.setHint(Words.t(hint));
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence t, int a, int b, int c) {
            }

            public void afterTextChanged(Editable t) {
                owner.typed(t.toString());
            }
        });
        /* The way back, at the start of the bar: hidden where there is
           nowhere to go back to within the screen. */
        back = new FrameLayout(context);
        backGlyph = new Glyph(context, Glyph.BACK, dp(24));
        back.addView(backGlyph, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        back.setVisibility(GONE);
        back.setContentDescription(BACK);
        LayoutParams backAt = new LayoutParams(dp(44), dp(44));
        backAt.rightMargin = dp(2);
        addView(back, backAt);
        addView(field, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        blob = new Blob(context, dp(56), Blob.MENU);
        blob.setContentDescription(MENU);
        blob.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (menu.shown()) {
                    /* The cross leads home. */
                    shutMenu(true);
                    owner.leave();
                } else {
                    openMenu();
                }
            }
        });
        addView(blob);

        menu = new Menu(context, host, new Menu.Listener() {
            public void picked(int section, int key) {
                shutMenu(true);
                owner.picked(section, key);
            }

            public void closing() {
                turn(0f);
            }
        });
        /* The cross over the round button leads home. */
        menu.exitThrough(blob, new Runnable() {
            public void run() {
                owner.leave();
            }
        });
        tint();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    EditText field() {
        return field;
    }

    /** Shows the way back at the start of the bar, or hides it when given nothing. */
    void back(final Runnable way) {
        if (way == null) {
            back.setVisibility(GONE);
            back.setOnClickListener(null);
            return;
        }
        back.setVisibility(VISIBLE);
        back.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                hideKeys();
                way.run();
            }
        });
    }

    /** A bar with nothing to find: the field only names the page. */
    void named(String name) {
        field.setHint(Words.t(name));
        field.setFocusable(false);
        field.setFocusableInTouchMode(false);
        field.setCursorVisible(false);
    }

    void tint() {
        setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        backGlyph.tint(Tone.onSurface());
        back.setBackground(Tone.touch(null, dp(22)));
        field.setTextColor(Tone.onSurface());
        field.setHintTextColor(Tone.faint());
        field.setTypeface(Style.face());
        blob.tint();
    }

    /** Stands the bar clear of the phone's own bars and, when it is up, the keyboard. */
    void lift(int bottom) {
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params instanceof MarginLayoutParams) {
            MarginLayoutParams margins = (MarginLayoutParams) params;
            margins.setMargins(dp(8), dp(6), dp(8), bottom + dp(10));
            setLayoutParams(margins);
        }
    }

    void clear() {
        hideKeys();
        if (field.getText().length() > 0) {
            field.setText("");
        }
    }

    void hideKeys() {
        InputMethodManager keys = (InputMethodManager)
            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keys != null) {
            keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
        field.clearFocus();
    }

    boolean menuShown() {
        return menu.shown();
    }

    private void openMenu() {
        hideKeys();
        int[] at = new int[2];
        int[] hostAt = new int[2];
        blob.getLocationOnScreen(at);
        ((View) menuHost()).getLocationOnScreen(hostAt);
        float x = at[0] - hostAt[0] + blob.getWidth() / 2f;
        float y = at[1] - hostAt[1] + blob.getHeight() / 2f;
        menu.show(owner.sections(), x, y, blob.getHeight() / 2f + dp(22));
        turn(1f);
    }

    private FrameLayout host;

    private FrameLayout menuHost() {
        return host;
    }

    void shutMenu(boolean slowly) {
        menu.hide(slowly);
        if (!slowly) {
            if (turning != null) {
                turning.cancel();
            }
            blob.open(0f);
        }
    }

    private void turn(float to) {
        if (turning != null) {
            turning.cancel();
        }
        float from = to > 0.5f ? 0f : 1f;
        turning = ValueAnimator.ofFloat(from, to);
        turning.setDuration(to > 0.5f ? Pace.ARRIVE : Pace.ARRIVE / 2);
        turning.setInterpolator(Pace.EMPHASIS);
        turning.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                blob.open((Float) animation.getAnimatedValue());
            }
        });
        turning.start();
    }
}
