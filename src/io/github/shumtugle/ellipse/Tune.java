package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings of the home screen, in the shape settings of home screens
 * have come to take: a field for finding at the top, the rooms of the
 * settings one under another, each with its drawing, its name and a line
 * about what is in it, and at the foot two tabs, the settings and the
 * style. A room opens sideways, with its name large at its head; Back
 * leaves it, and Back again leaves the settings.
 *
 * So far there is one room, Other: the home screen started afresh, or
 * set back to how it was on first start.
 */
public final class Tune extends Activity {

    /** One line of the settings: a room, or something done inside one. */
    private static final class Line {
        final int glyph;
        final String title;
        final String about;
        final int room;
        final int deed;

        Line(int glyph, String title, String about, int room, int deed) {
            this.glyph = glyph;
            this.title = title;
            this.about = about;
            this.room = room;
            this.deed = deed;
        }
    }

    private static final int ROOT = 0;
    private static final int OTHER = 1;
    private static final int STYLE = 2;

    private static final int NONE = 0;
    private static final int RESTART = 1;
    private static final int RESET = 2;

    private static final String SEARCH = "Search";
    private static final String SETTINGS = "Settings";
    private static final String STYLE_TAB = "Style";
    private static final String NOTHING = "Nothing here yet.";
    private static final String AGAIN = "Tap again to reset everything";

    /** The rooms of the root, in the order they stand. */
    private static final Line[] ROOMS = {
        new Line(Glyph.OTHER, "Other", "Start the home screen afresh, or set it back as it was", OTHER, NONE)
    };

    /** What stands in each room. */
    private static final Line[] INSIDE_OTHER = {
        new Line(Glyph.RESTART, "Restart launcher", "Close the home screen and open it again", OTHER, RESTART),
        new Line(Glyph.RESET, "Reset launcher",
            "Forget everything set by hand and lay the screens out as on first start", OTHER, RESET)
    };

    private float density;
    private float scaled;
    private LinearLayout root;
    private FrameLayout head;
    private EditText field;
    private TextView heading;
    private ScrollView scroll;
    private LinearLayout rows;
    private LinearLayout tabs;
    private int room = ROOT;
    private int tab = ROOT;
    private long armed;

    private int dp(float value) {
        return Math.round(value * density);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Tone.read(this);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        build();
        show(ROOT, 0);
    }

    private void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Tone.surface());

        head = new FrameLayout(this);
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(Tone.box(Tone.container(), dp(40), dp(0.5f)));
        pill.setPadding(dp(20), dp(6), dp(16), dp(6));
        Glyph lens = new Glyph(this, Glyph.SEARCH, dp(24));
        lens.tint(Tone.faint());
        pill.addView(lens);
        field = new EditText(this);
        field.setBackground(null);
        field.setHint(SEARCH);
        field.setHintTextColor(Tone.faint());
        field.setTextColor(Tone.onSurface());
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
                if (room == ROOT && tab == ROOT) {
                    fill();
                }
            }
        });
        pill.addView(field, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pillParams.setMargins(dp(16), dp(12), dp(16), dp(8));
        head.addView(pill, pillParams);

        heading = new TextView(this);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32f * scaled);
        heading.setTextColor(Tone.onSurface());
        heading.setPadding(dp(24), dp(20), dp(24), dp(12));
        heading.setVisibility(View.GONE);
        head.addView(heading);
        root.addView(head);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(0, dp(4), 0, dp(16));
        scroll.addView(rows);
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackgroundColor(Tone.container());
        tabs.setPadding(0, dp(12), 0, dp(12));
        tabs.addView(tab(Glyph.GEAR, SETTINGS, ROOT), new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabs.addView(tab(Glyph.BRUSH, STYLE_TAB, STYLE), new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(tabs);

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int top;
                int bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                    top = bars.top;
                    bottom = Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
                } else {
                    top = insets.getSystemWindowInsetTop();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                head.setPadding(0, top, 0, 0);
                tabs.setPadding(0, dp(12), 0, dp(12) + bottom);
                return insets;
            }
        });
        setContentView(root);
        root.requestApplyInsets();
    }

    /** A tab at the foot: its drawing in a pill that shows which tab is on, its word under it. */
    private View tab(int glyph, String word, final int which) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.VERTICAL);
        made.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout pill = new FrameLayout(this);
        Glyph drawing = new Glyph(this, glyph, dp(24));
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER);
        pill.addView(drawing, at);
        made.addView(pill, new LinearLayout.LayoutParams(dp(64), dp(32)));
        TextView name = new TextView(this);
        name.setText(word);
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, 13f * scaled);
        name.setPadding(0, dp(6), 0, 0);
        made.addView(name);
        made.setTag(new Object[] {pill, drawing, name});
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (tab != which || room != ROOT) {
                    tab = which;
                    show(ROOT, 0);
                }
            }
        });
        return made;
    }

    private void markTabs() {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            Object[] parts = (Object[]) tabs.getChildAt(i).getTag();
            boolean on = (i == 0 ? ROOT : STYLE) == tab;
            ((View) parts[0]).setBackground(on ? Tone.box(Tone.containerHigh(), dp(16), 0f) : null);
            ((Glyph) parts[1]).tint(on ? Tone.onSurface() : Tone.faint());
            ((TextView) parts[2]).setTextColor(on ? Tone.onSurface() : Tone.faint());
            ((TextView) parts[2]).setTypeface(null, on ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
        }
    }

    /** Shows a room; a room entered comes in from the side it lies on. */
    private void show(int which, int from) {
        room = which;
        armed = 0L;
        boolean inRoom = room != ROOT;
        head.getChildAt(0).setVisibility(inRoom || tab != ROOT ? View.GONE : View.VISIBLE);
        heading.setVisibility(inRoom ? View.VISIBLE : View.GONE);
        if (room == OTHER) {
            heading.setText(ROOMS[0].title);
        }
        if (!inRoom) {
            hideKeys();
        }
        markTabs();
        fill();
        scroll.scrollTo(0, 0);
        if (from != 0) {
            scroll.setTranslationX(from * dp(48));
            scroll.setAlpha(0f);
            scroll.animate().translationX(0f).alpha(1f).setDuration(Pace.ARRIVE)
                .setInterpolator(Pace.EMPHASIS).start();
        }
    }

    private void fill() {
        rows.removeAllViews();
        if (tab == STYLE && room == ROOT) {
            TextView none = new TextView(this);
            none.setText(NOTHING);
            none.setTextColor(Tone.faint());
            none.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
            none.setPadding(dp(24), dp(40), dp(24), dp(24));
            rows.addView(none);
            return;
        }
        List<Line> lines = new ArrayList<>();
        if (room == OTHER) {
            for (Line line : INSIDE_OTHER) {
                lines.add(line);
            }
        } else {
            String typed = Match.norm(field.getText().toString());
            if (typed.length() == 0) {
                for (Line line : ROOMS) {
                    lines.add(line);
                }
            } else {
                /* Found, a line is shown wherever it lives, rooms and deeds alike. */
                List<Line> every = new ArrayList<>();
                for (Line line : ROOMS) {
                    every.add(line);
                }
                for (Line line : INSIDE_OTHER) {
                    every.add(line);
                }
                for (Line line : every) {
                    if (Match.rank(Match.norm(line.title), typed) != Match.NONE
                        || Match.norm(line.about).contains(typed)) {
                        lines.add(line);
                    }
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            View row = row(lines.get(i));
            rows.addView(row);
            row.setAlpha(0f);
            row.setTranslationY(dp(10));
            row.animate().alpha(1f).translationY(0f).setStartDelay(Pace.STEP * i)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    private View row(final Line line) {
        LinearLayout made = new LinearLayout(this);
        made.setOrientation(LinearLayout.HORIZONTAL);
        made.setGravity(Gravity.CENTER_VERTICAL);
        made.setPadding(dp(28), dp(18), dp(24), dp(18));
        made.setBackground(Tone.touch(null, 0f));
        Glyph drawing = new Glyph(this, line.glyph, dp(28));
        drawing.tint(Tone.primary());
        made.addView(drawing);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(28), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText(line.title);
        title.setTextColor(Tone.onSurface());
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * scaled);
        words.addView(title);
        final TextView about = new TextView(this);
        about.setText(line.about);
        about.setTextColor(Tone.faint());
        about.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        about.setPadding(0, dp(2), 0, 0);
        words.addView(about);
        made.addView(words, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act(line, about);
            }
        });
        return made;
    }

    private void act(Line line, TextView about) {
        if (line.deed == NONE) {
            field.setText("");
            show(line.room, 1);
            return;
        }
        if (line.deed == RESTART) {
            restart();
            return;
        }
        /* Setting everything back is asked twice: the first tap says what
           the second will do, and is forgotten after a few seconds. */
        long now = System.currentTimeMillis();
        if (now - armed > 4000L) {
            armed = now;
            about.setText(AGAIN);
            about.setTextColor(Tone.primary());
            about.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                ? android.view.HapticFeedbackConstants.REJECT
                : android.view.HapticFeedbackConstants.LONG_PRESS);
            return;
        }
        Keep.reset(this);
        restart();
    }

    /** The home screen is closed and opened again, from nothing. */
    private void restart() {
        Intent again = Intent.makeRestartActivityTask(new ComponentName(this, Home.class));
        startActivity(again);
        Runtime.getRuntime().exit(0);
    }

    private void hideKeys() {
        InputMethodManager keys = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keys != null && field != null) {
            keys.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
        if (field != null) {
            field.clearFocus();
        }
    }

    @Override
    public void onBackPressed() {
        if (room != ROOT) {
            show(ROOT, -1);
            return;
        }
        if (tab != ROOT) {
            tab = ROOT;
            show(ROOT, -1);
            return;
        }
        super.onBackPressed();
    }
}
