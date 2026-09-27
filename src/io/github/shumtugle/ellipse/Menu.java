package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
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
 *
 * The menu of a thing on the screens points at it, and has two faces
 * under one head: the thing's name, and a round button at the right. The
 * first face is what the app itself offers, its own shortcuts; the round
 * button, a gear, turns the card to the second face, which is the home
 * screen's own: what can be done to the thing, with taking it away set
 * apart in the colour of taking away. On the second face the button is
 * the way to the app's information.
 */
final class Menu {

    /** The section number a press of the head's round button is reported under. */
    static final int HEAD = -1;

    /** One section: an optional caption, its lines, and the key of the one chosen, if any. */
    static final class Section {
        final String caption;
        final String[] lines;
        final int[] keys;
        int chosen = -1;
        /** A small picture before each line, where there is one. */
        Drawable[] icons;
        /** A drawing of the home screen's own before each line, where there is one. */
        int[] glyphs;
        /** Lines that take something away, in the colour of taking away. */
        boolean danger;

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

    private static final float WIDTH = 272f;
    private static final int DANGER = 0xFFE8674A;

    private final Context context;
    private final FrameLayout host;
    private final Listener listener;
    private final View veil;
    private final LinearLayout card;
    private final LinearLayout head;
    private final TextView title;
    private final FrameLayout button;
    private final Glyph buttonGlyph;
    /** What the card holds, in a scroll of its own for a menu taller than most of the screen. */
    private final android.widget.ScrollView scroll;
    private LinearLayout foot;
    private final LinearLayout body;
    private final float density;
    private final float scaled;
    private Section[] sections = new Section[0];
    private Section[] first;
    private Section[] second;
    private int firstGlyph;
    private int secondGlyph;
    private boolean onSecond;
    private boolean pointing;
    private float anchorX;
    private float anchorY;
    private float anchorGap;
    private boolean shown;
    /** A view under the veil that is the way out, and where it leads. */
    private View exitView;
    private Runnable exit;

    Menu(Context context, FrameLayout host, Listener listener) {
        this.context = context;
        this.host = host;
        this.listener = listener;
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;

        veil = new View(context);
        veil.setVisibility(View.GONE);
        veil.setClickable(true);
        /* A touch outside the card closes it; a touch on the way out, when
           one is given, closes it and takes that way. The veil lies over
           everything, so it is the veil that has to tell the two apart. */
        veil.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getActionMasked() != android.view.MotionEvent.ACTION_UP) {
                    return true;
                }
                boolean out = exitView != null && inside(exitView, event.getRawX(), event.getRawY());
                hide(true);
                if (out && exit != null) {
                    exit.run();
                }
                return true;
            }
        });
        host.addView(veil, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setElevation(dp(8));
        card.setVisibility(View.GONE);
        card.setClickable(true);

        head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), dp(2), dp(2), dp(2));
        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * scaled);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        button = new FrameLayout(context);
        buttonGlyph = new Glyph(context, Glyph.SETTINGS, dp(26));
        button.addView(buttonGlyph, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!onSecond && second != null) {
                    turn();
                } else {
                    listener.picked(HEAD, 0);
                }
            }
        });
        head.addView(button, new LinearLayout.LayoutParams(dp(44), dp(44)));
        card.addView(head);

        scroll = new android.widget.ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        card.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        /* The last section — taking away, or the way to the settings — stands under the scroll,
           always in sight however many lines there are above it. */
        foot = new LinearLayout(context);
        foot.setOrientation(LinearLayout.VERTICAL);
        card.addView(foot, new LinearLayout.LayoutParams(
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

    /** While the menu stands open, a touch on this view leaves by this way. */
    void exitThrough(View view, Runnable way) {
        exitView = view;
        exit = way;
    }

    private static boolean inside(View view, float x, float y) {
        int[] at = new int[2];
        view.getLocationOnScreen(at);
        return x >= at[0] && x <= at[0] + view.getWidth() && y >= at[1] && y <= at[1] + view.getHeight();
    }

    /** Marks a line as chosen, whether the menu stands open or not. */
    void choose(int section, int key) {
        if (section >= 0 && section < sections.length) {
            sections[section].chosen = key;
            if (shown) {
                build();
            }
        }
    }

    private void build() {
        body.removeAllViews();
        foot.removeAllViews();
        head.setVisibility(first != null ? View.VISIBLE : View.GONE);
        if (first != null) {
            title.setTextColor(Tone.faint());
            int glyph = onSecond ? secondGlyph : firstGlyph;
            button.setVisibility(glyph >= 0 ? View.VISIBLE : View.GONE);
            if (glyph >= 0) {
                buttonGlyph.setKind(glyph);
                buttonGlyph.tint(Tone.onSurface());
                button.setBackground(Tone.touch(Tone.box(Tone.container(), dp(22), 0f), dp(22)));
            }
        }
        for (int s = 0; s < sections.length; s++) {
            Section section = sections[s];
            boolean last = s > 0 && s == sections.length - 1 && section.caption == null;
            LinearLayout into = last ? foot : body;
            if (s > 0 && section.caption == null) {
                /* A section with no name of its own is set off from the one
                   before by a hairline: it is another kind of line. */
                View rule = new View(context);
                rule.setBackgroundColor(Tone.outline());
                LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
                ruleParams.setMargins(dp(0), dp(8), dp(0), dp(6));
                into.addView(rule, ruleParams);
            }
            if (section.caption != null) {
                TextView caption = new TextView(context);
                caption.setText(Words.t(section.caption).toUpperCase(Locale.getDefault()));
                caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * scaled);
                caption.setLetterSpacing(0.12f);
                caption.setTextColor(Tone.faint());
                caption.setPadding(dp(18), dp(s == 0 ? 6 : 14), dp(18), dp(8));
                body.addView(caption);
            }
            for (int i = 0; i < section.lines.length; i++) {
                into.addView(line(section, s, i), lineParams());
            }
        }
        Style.apply(card);
    }

    private LinearLayout.LayoutParams lineParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(2);
        return params;
    }

    private View line(Section section, final int which, int i) {
        final int key = section.keys[i];
        boolean on = key == section.chosen;
        int ink = section.danger ? DANGER : (on ? Tone.primary() : Tone.onSurface());
        LinearLayout made = new LinearLayout(context);
        made.setOrientation(LinearLayout.HORIZONTAL);
        made.setGravity(Gravity.CENTER_VERTICAL);
        int fill = on ? ((0x2E << 24) | (Tone.primary() & 0x00FFFFFF)) : 0x00000000;
        made.setBackground(Tone.touch(Tone.box(fill, dp(22), 0f), dp(22)));
        boolean pictured = (section.icons != null && i < section.icons.length && section.icons[i] != null)
            || (section.glyphs != null && i < section.glyphs.length && section.glyphs[i] >= 0);
        made.setPadding(dp(pictured ? 12 : 16), dp(pictured ? 9 : 13), dp(16), dp(pictured ? 9 : 13));
        if (section.icons != null && i < section.icons.length && section.icons[i] != null) {
            android.widget.ImageView picture = new android.widget.ImageView(context);
            picture.setImageDrawable(section.icons[i]);
            made.addView(picture, new LinearLayout.LayoutParams(dp(34), dp(34)));
        } else if (section.glyphs != null && i < section.glyphs.length && section.glyphs[i] >= 0) {
            FrameLayout frame = new FrameLayout(context);
            Glyph drawing = new Glyph(context, section.glyphs[i], dp(24));
            drawing.tint(ink);
            frame.addView(drawing, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
            made.addView(frame, new LinearLayout.LayoutParams(dp(34), dp(34)));
        }
        TextView words = new TextView(context);
        words.setText(Words.t(section.lines[i]));
        words.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * scaled);
        words.setSingleLine(true);
        words.setEllipsize(android.text.TextUtils.TruncateAt.END);
        words.setTextColor(ink);
        words.setPadding(pictured ? dp(14) : 0, 0, 0, 0);
        made.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        made.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.picked(which, key);
            }
        });
        return made;
    }

    /** Opens beside a point of the host, with no head and no point of its own. */
    void show(Section[] sections, float x, float y, float gap) {
        first = null;
        second = null;
        onSecond = false;
        pointing = false;
        this.sections = sections;
        open(x, y, gap);
    }

    /**
     * Opens pointing at a thing, under a head with its name. With a first
     * face, it opens on it and the round button turns to the second; with
     * none, it opens on the second. A glyph of less than nought leaves the
     * button off that face.
     */
    void showFaces(String name, Section[] first, int firstGlyph, Section[] second, int secondGlyph,
                   float x, float y, float gap) {
        this.first = first == null || first.length == 0 ? second : first;
        this.second = first == null || first.length == 0 ? null : second;
        this.firstGlyph = first == null || first.length == 0 ? secondGlyph : firstGlyph;
        this.secondGlyph = secondGlyph;
        onSecond = this.second == null;
        pointing = true;
        title.setText(Words.t(name));
        this.sections = this.first;
        open(x, y, gap);
    }

    /** The card turns to its second face where it stands, its lines coming in again. */
    private void turn() {
        onSecond = true;
        sections = second;
        build();
        place(false);
        for (int i = 0; i < body.getChildCount(); i++) {
            View line = body.getChildAt(i);
            line.setAlpha(0f);
            line.setTranslationX(dp(16));
            line.animate().alpha(1f).translationX(0f).setStartDelay(Pace.STEP / 2 * i)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    private void open(float x, float y, float gap) {
        shown = true;
        anchorX = x;
        anchorY = y;
        anchorGap = gap;
        build();
        place(true);
    }

    /**
     * Stands the card beside the point: above it in the lower half of the
     * screen and below it in the upper, leaning toward the middle, never
     * leaving the host; when it points, its point is drawn toward the thing.
     */
    private void place(boolean arriving) {
        float nib = pointing ? dp(10) : 0f;
        card.setPadding(dp(6), dp(8), dp(6), dp(8));
        LinearLayout.LayoutParams fit = (LinearLayout.LayoutParams) scroll.getLayoutParams();
        fit.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        scroll.setLayoutParams(fit);
        card.measure(View.MeasureSpec.makeMeasureSpec(dp(WIDTH), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        float most = host.getHeight() * 0.7f;
        if (card.getMeasuredHeight() + nib > most) {
            fit.height = Math.round(most - nib) - card.getPaddingTop() - card.getPaddingBottom()
                - (head.getVisibility() == View.VISIBLE ? head.getMeasuredHeight() : 0) - foot.getMeasuredHeight();
            scroll.setLayoutParams(fit);
            scroll.scrollTo(0, 0);
            card.measure(View.MeasureSpec.makeMeasureSpec(dp(WIDTH), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        }
        float x = anchorX;
        float y = anchorY;
        float w = dp(WIDTH);
        float h = card.getMeasuredHeight() + nib;
        float edge = dp(8);
        float innerLeft = host.getPaddingLeft() + edge;
        float innerRight = host.getWidth() - host.getPaddingRight() - edge;
        float left = pointing ? x - w / 2f : (x > host.getWidth() / 2f ? x + dp(24) - w : x - dp(24));
        left = Math.max(innerLeft, Math.min(innerRight - w, left));
        boolean above = y > host.getHeight() / 2f;
        float top = above ? y - anchorGap - h : y + anchorGap;
        top = Math.max(edge, Math.min(host.getHeight() - edge - h, top));
        card.setPadding(dp(6), dp(8) + (pointing && !above ? Math.round(nib) : 0),
            dp(6), dp(8) + (pointing && above ? Math.round(nib) : 0));
        card.setBackground(new Balloon(Tone.containerHigh(), dp(24), pointing ? nib : 0f,
            x - left, !above));

        card.setTranslationX(left - host.getPaddingLeft());
        card.setTranslationY(top - host.getPaddingTop());
        card.setPivotX(x - left);
        card.setPivotY(y - top);
        if (!arriving) {
            return;
        }
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

    /** The card's ground: a rounded body, and a small point toward the thing when it has one. */
    private static final class Balloon extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius;
        private final float nib;
        private final float at;
        private final boolean up;
        private final Path shape = new Path();

        Balloon(int colour, float radius, float nib, float at, boolean up) {
            paint.setColor(colour);
            this.radius = radius;
            this.nib = nib;
            this.at = at;
            this.up = up;
        }

        private RectF body() {
            RectF r = new RectF(getBounds());
            if (up) {
                r.top += nib;
            } else {
                r.bottom -= nib;
            }
            return r;
        }

        @Override
        public void draw(Canvas canvas) {
            RectF r = body();
            shape.reset();
            shape.addRoundRect(r, radius, radius, Path.Direction.CW);
            if (nib > 0f) {
                float x = Math.max(r.left + radius + nib, Math.min(r.right - radius - nib, at));
                Path point = new Path();
                if (up) {
                    point.moveTo(x - nib, r.top + 1);
                    point.lineTo(x, r.top - nib);
                    point.lineTo(x + nib, r.top + 1);
                } else {
                    point.moveTo(x - nib, r.bottom - 1);
                    point.lineTo(x, r.bottom + nib);
                    point.lineTo(x + nib, r.bottom - 1);
                }
                point.close();
                shape.op(point, Path.Op.UNION);
            }
            canvas.drawPath(shape, paint);
        }

        @Override
        public void getOutline(Outline outline) {
            RectF r = body();
            outline.setRoundRect(Math.round(r.left), Math.round(r.top), Math.round(r.right),
                Math.round(r.bottom), radius);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
