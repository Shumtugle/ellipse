package io.github.shumtugle.ellipse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * What can be done with a thing, on a card that grows out of the thing.
 *
 * The card does not appear beside the thing; it comes out of it. It
 * starts as the thing's own outline and opens, in one movement, into a
 * card below it, or above it when there is no room below, taking its
 * corners from the tile's to its own on the way. What is on the card
 * follows once there is room for it, a line at a time.
 *
 * The card takes its colour from the scheme, like every other surface:
 * one kind of thing, one ground, whatever it opens over.
 */
final class Offer extends FrameLayout {

    /** A count that can be told to show a new value. */
    interface Count {
        void show(String value);
    }

    /** A tile that turns a setting over: it says what the setting now is. */
    interface Turn {
        String turn();
    }

    private static final float WIDTH = 320f;
    private static final float CORNER = 28f;
    private static final float FROM_CORNER = 16f;

    private final FrameLayout stage;
    private final LinearLayout body;
    private final LinearLayout head;
    private final TextView title;
    private final LinearLayout tools;
    private final List<View> lines = new ArrayList<View>();
    private final List<View> extras = new ArrayList<View>();
    private final List<TileSpec> tiles = new ArrayList<TileSpec>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final RectF reveal = new RectF();
    private final RectF from = new RectF();
    private final RectF to = new RectF();
    private float corner = CORNER;
    private boolean built;

    private int ground;
    private int tileFill;
    private int ink;
    private int quiet;
    private int accent;
    private int onAccent;

    /** A tile, kept until the card is shown, since tiles are laid two to a row. */
    private static final class TileSpec {
        View mark;
        String name;
        Runnable deed;
        Runnable held;
        /** For a tile that turns a setting: what it is now, and how it turns. */
        String state;
        Turn turn;
        boolean lit;
    }

    Offer(Context context, FrameLayout stage) {
        super(context);
        this.stage = stage;
        setWillNotDraw(false);
        setClipChildren(true);
        colours(-1f, 0.0);

        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setClickable(true);
        body.setPadding(Round.dp(8f), Round.dp(8f), Round.dp(8f), Round.dp(12f));

        head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Round.dp(14f), Round.dp(6f), Round.dp(4f), Round.dp(10f));
        title = Letter.set(new TextView(context), Letter.HEADLINE_S);
        Letter.serif(title);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tools = new LinearLayout(context);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        head.addView(tools, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Round.dp(48f)));
        body.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(body);

        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(Math.round(reveal.left), Math.round(reveal.top),
                    Math.round(reveal.right), Math.round(reveal.bottom), Round.px(corner));
            }
        });
        setElevation(Round.px(10f));
    }

    // ------------------------------------------------------------ colour

        /**
     * A card is a card, whatever it is about. It was taking its colour
     * from the picture of the thing it opened over, so one application's
     * card came up green and another's brown; a screenful of them read as
     * a handful of badges rather than as one kind of thing. The colour
     * now comes from the scheme, like every other surface.
     */
    Offer tint(Bitmap picture) {
        return this;
    }

    /** The colours: grown from a hue and its richness, or the seed's own when no hue is given. */
    private void colours(float hue, double chroma) {
        if (hue < 0f) {
            ground = Tone.of(Tone.SURFACE_HIGH);
            tileFill = Tone.of(Tone.SURFACE_HIGHEST);
            ink = Tone.of(Tone.ON_SURFACE);
            quiet = Tone.of(Tone.ON_SURFACE_VARIANT);
            accent = Tone.of(Tone.SECONDARY_CONTAINER);
            onAccent = Tone.of(Tone.ON_SECONDARY_CONTAINER);
            return;
        }
        ground = Tone.at(17f, Math.min(chroma * 0.4, 16.0), hue);
        tileFill = Tone.at(25f, Math.min(chroma * 0.5, 22.0), hue);
        ink = Tone.at(94f, Math.min(chroma * 0.15, 8.0), hue);
        quiet = Tone.at(78f, Math.min(chroma * 0.3, 14.0), hue);
        accent = Tone.at(82f, Math.min(chroma, 52.0), hue);
        onAccent = Tone.at(18f, Math.min(chroma * 0.6, 28.0), hue);
    }

    // ------------------------------------------------------------ content

    Offer title(String text) {
        title.setText(text == null ? "" : text);
        title.setTextColor(ink);
        return this;
    }

    /** A round button at the head, for something done to the thing itself. */
    Offer tool(int glyph, String name, final Runnable deed) {
        FrameLayout key = new FrameLayout(getContext());
        key.setBackground(Round.touch(Round.box(tileFill, Round.FULL), ink, Round.FULL));
        key.setContentDescription(name);
        Sketch mark = new Sketch(getContext(), glyph);
        mark.ink(ink, quiet);
        key.addView(mark, new FrameLayout.LayoutParams(Round.dp(22f), Round.dp(22f), Gravity.CENTER));
        key.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
                deed.run();
            }
        });
        key.setStateListAnimator(Give.press());
        LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(Round.dp(44f), Round.dp(44f));
        place.leftMargin = Round.dp(6f);
        place.gravity = Gravity.CENTER_VERTICAL;
        tools.addView(key, place);
        return this;
    }

    /** A tile with a drawn mark on a disc of the card's accent. */
    Offer row(int glyph, String name, Runnable deed) {
        Sketch mark = new Sketch(getContext(), glyph);
        mark.ink(onAccent, onAccent);
        FrameLayout disc = new FrameLayout(getContext());
        disc.setBackground(Round.box(accent, Round.FULL));
        disc.addView(mark, new FrameLayout.LayoutParams(Round.dp(20f), Round.dp(20f), Gravity.CENTER));
        TileSpec spec = new TileSpec();
        spec.mark = disc;
        spec.name = name;
        spec.deed = deed;
        tiles.add(spec);
        return this;
    }

    /**
     * A tile that turns one of the thing's settings over and keeps the card
     * open: under its name, what the setting is now. A setting that is on
     * wears the accent; one that is off, the card's own ground.
     */
    Offer turn(int glyph, String name, String state, boolean on, Turn turn) {
        Sketch mark = new Sketch(getContext(), glyph);
        FrameLayout disc = new FrameLayout(getContext());
        disc.addView(mark, new FrameLayout.LayoutParams(Round.dp(20f), Round.dp(20f), Gravity.CENTER));
        TileSpec spec = new TileSpec();
        spec.mark = disc;
        spec.name = name;
        spec.state = state;
        spec.turn = turn;
        spec.lit = on;
        tiles.add(spec);
        return this;
    }

    private void light(TileSpec spec) {
        FrameLayout disc = (FrameLayout) spec.mark;
        Sketch mark = (Sketch) disc.getChildAt(0);
        disc.setBackground(Round.box(spec.lit ? accent : ground, Round.FULL));
        int strong = spec.lit ? onAccent : quiet;
        mark.ink(strong, (strong & 0x00FFFFFF) | 0x59000000);
        mark.invalidate();
    }

    /** A tile with a picture of its own, such as a shortcut's; a long press lifts it. */
    Offer row(Bitmap picture, String name, Runnable deed, Runnable held) {
        ImageView face = new ImageView(getContext());
        face.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (picture != null) {
            face.setImageBitmap(picture);
        } else {
            face.setBackground(Round.box(accent, Round.FULL));
        }
        TileSpec spec = new TileSpec();
        spec.mark = face;
        spec.name = name;
        spec.deed = deed;
        spec.held = held;
        tiles.add(spec);
        return this;
    }

    /** The accent's two colours, for pictures drawn to match the card. */
    int accent() {
        return accent;
    }

    int onAccent() {
        return onAccent;
    }

    /** A whole line, for what is less often wanted. */
    Offer link(int glyph, String name, final Runnable deed) {
        LinearLayout line = new LinearLayout(getContext());
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(Round.dp(16f), 0, Round.dp(16f), 0);
        line.setBackground(Round.touch(null, ink, 20f));
        Sketch mark = new Sketch(getContext(), glyph);
        mark.ink(quiet, quiet);
        line.addView(mark, new LinearLayout.LayoutParams(Round.dp(20f), Round.dp(20f)));
        TextView words = Letter.set(new TextView(getContext()), Letter.TITLE_S);
        words.setText(name);
        words.setTextColor(quiet);
        LinearLayout.LayoutParams wordsPlace = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wordsPlace.leftMargin = Round.dp(14f);
        line.addView(words, wordsPlace);
        line.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
                deed.run();
            }
        });
        extras.add(line);
        return this;
    }

    /** A quiet line, for something the card cannot offer and why. */
    Offer note(String text) {
        TextView said = Letter.set(new TextView(getContext()), Letter.BODY_M);
        said.setText(text);
        said.setTextColor(quiet);
        said.setPadding(Round.dp(16f), Round.dp(8f), Round.dp(16f), Round.dp(8f));
        extras.add(said);
        return this;
    }

    /** A count between a minus and a plus; the card stays open while it is changed. */
    Count count(String name, String value, final Runnable less, final Runnable more) {
        LinearLayout line = new LinearLayout(getContext());
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(Round.dp(16f), 0, Round.dp(8f), 0);
        TextView words = Letter.set(new TextView(getContext()), Letter.TITLE_M);
        words.setText(name);
        words.setTextColor(ink);
        line.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(key(Sketch.MINUS, less), new LinearLayout.LayoutParams(Round.dp(48f), Round.dp(48f)));
        final TextView number = Letter.set(new TextView(getContext()), Letter.HEADLINE_S);
        number.setText(value);
        number.setGravity(Gravity.CENTER);
        number.setTextColor(accent);
        line.addView(number, new LinearLayout.LayoutParams(Round.dp(52f), ViewGroup.LayoutParams.WRAP_CONTENT));
        line.addView(key(Sketch.PLUS, more), new LinearLayout.LayoutParams(Round.dp(48f), Round.dp(48f)));
        extras.add(line);
        return new Count() {
            public void show(String next) {
                number.setText(next);
                number.setScaleX(1.25f);
                number.setScaleY(1.25f);
                number.animate().scaleX(1f).scaleY(1f).setDuration(Pace.GROW)
                    .setInterpolator(Pace.EMPHASIS).start();
            }
        };
    }

    private View key(int glyph, final Runnable deed) {
        FrameLayout key = new FrameLayout(getContext());
        key.setBackground(Round.touch(Round.box(accent, Round.FULL), onAccent, Round.FULL));
        Sketch mark = new Sketch(getContext(), glyph);
        mark.ink(onAccent, 0);
        key.addView(mark, new FrameLayout.LayoutParams(Round.dp(24f), Round.dp(24f), Gravity.CENTER));
        key.setStateListAnimator(Give.press());
        key.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                deed.run();
            }
        });
        return key;
    }

    /** Lays the tiles two to a row, and the whole lines under them. */
    private void build() {
        if (built) {
            return;
        }
        built = true;
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Round.dp(72f));
                place.topMargin = i == 0 ? 0 : Round.dp(6f);
                body.addView(row, place);
                lines.add(row);
            }
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            place.leftMargin = i % 2 == 0 ? 0 : Round.dp(6f);
            row.addView(tile(tiles.get(i)), place);
        }
        for (View one : extras) {
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, one instanceof TextView
                    ? ViewGroup.LayoutParams.WRAP_CONTENT : Round.dp(56f));
            place.topMargin = Round.dp(4f);
            body.addView(one, place);
            lines.add(one);
        }
    }

    private View tile(final TileSpec spec) {
        LinearLayout made = new LinearLayout(getContext());
        made.setOrientation(LinearLayout.HORIZONTAL);
        made.setGravity(Gravity.CENTER_VERTICAL);
        made.setPadding(Round.dp(12f), 0, Round.dp(10f), 0);
        made.setBackground(Round.touch(Round.box(tileFill, 22f), ink, 22f));
        made.addView(spec.mark, new LinearLayout.LayoutParams(Round.dp(40f), Round.dp(40f)));
        TextView words = Letter.set(new TextView(getContext()), Letter.TITLE_S);
        words.setText(spec.name);
        words.setTextColor(ink);
        words.setMaxLines(spec.turn != null ? 1 : 2);
        words.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams wordsPlace = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        wordsPlace.leftMargin = Round.dp(10f);
        made.setStateListAnimator(Give.press());
        if (spec.turn != null) {
            LinearLayout both = new LinearLayout(getContext());
            both.setOrientation(LinearLayout.VERTICAL);
            both.addView(words);
            final TextView state = Letter.set(new TextView(getContext()), Letter.BODY_M);
            state.setText(spec.state);
            state.setTextColor(quiet);
            state.setSingleLine(true);
            state.setEllipsize(TextUtils.TruncateAt.END);
            both.addView(state);
            made.addView(both, wordsPlace);
            light(spec);
            made.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    String now = spec.turn.turn();
                    if (now == null) {
                        return;
                    }
                    spec.state = now;
                    spec.lit = !now.equals(Words.s("state_none")) && !now.equals(Words.s("hidden"));
                    state.setText(now);
                    light(spec);
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            });
            return made;
        }
        made.addView(words, wordsPlace);
        made.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                close();
                spec.deed.run();
            }
        });
        if (spec.held != null) {
            made.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    close();
                    spec.held.run();
                    return true;
                }
            });
        }
        return made;
    }

    // ------------------------------------------------------------ standing

    /**
     * Grows the card out of a thing: from the thing's outline to a card
     * below it, or above it when below is too short; across, over the
     * thing's middle as far as the glass allows.
     */
    void show(Rect thing) {
        build();
        int room = stage.getWidth();
        int width = Math.min(Round.dp(WIDTH), room - Round.dp(24f));
        body.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(stage.getHeight(), MeasureSpec.AT_MOST));
        int h = body.getMeasuredHeight();
        float margin = Round.px(12f);
        float gap = Round.px(10f);
        float top = thing.bottom + gap;
        if (top + h > stage.getHeight() - margin) {
            top = thing.top - gap - h;
            if (top < margin) {
                top = Math.max(margin, stage.getHeight() - h - margin);
            }
        }
        float left = Math.max(margin, Math.min(room - width - margin, thing.centerX() - width / 2f));

        // The container covers the thing and the card together, so the card
        // can start as the thing and grow into itself.
        float boxLeft = Math.min(left, thing.left);
        float boxTop = Math.min(top, thing.top);
        float boxRight = Math.max(left + width, thing.right);
        float boxBottom = Math.max(top + h, thing.bottom);
        FrameLayout.LayoutParams place = new FrameLayout.LayoutParams(
            Math.round(boxRight - boxLeft), Math.round(boxBottom - boxTop));
        stage.addView(this, place);
        setTranslationX(boxLeft);
        setTranslationY(boxTop);
        FrameLayout.LayoutParams inner = new FrameLayout.LayoutParams(width, h);
        inner.leftMargin = Math.round(left - boxLeft);
        inner.topMargin = Math.round(top - boxTop);
        body.setLayoutParams(inner);

        from.set(thing.left - boxLeft, thing.top - boxTop, thing.right - boxLeft, thing.bottom - boxTop);
        to.set(left - boxLeft, top - boxTop, left - boxLeft + width, top - boxTop + h);
        reveal.set(from);
        corner = FROM_CORNER;

        body.setAlpha(0f);
        ValueAnimator grow = ValueAnimator.ofFloat(0f, 1f);
        grow.setDuration(Pace.GROW);
        grow.setInterpolator(Pace.EMPHASIS);
        grow.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                reveal.set(from.left + (to.left - from.left) * t, from.top + (to.top - from.top) * t,
                    from.right + (to.right - from.right) * t, from.bottom + (to.bottom - from.bottom) * t);
                corner = FROM_CORNER + (CORNER - FROM_CORNER) * t;
                body.setAlpha(Math.max(0f, Math.min(1f, (t - 0.35f) / 0.4f)));
                invalidate();
                invalidateOutline();
            }
        });
        grow.start();
        for (int i = 0; i < lines.size(); i++) {
            View line = lines.get(i);
            line.setTranslationY(Round.px(14f));
            line.setAlpha(0f);
            line.animate().translationY(0f).alpha(1f).setStartDelay(120L + i * 35L)
                .setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();
        }
    }

    /** The card's ground, as far as it has grown, and the card's content clipped to it. */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        float r = Round.px(corner);
        shape.reset();
        shape.addRoundRect(reveal, r, r, Path.Direction.CW);
        paint.setColor(ground);
        canvas.drawPath(shape, paint);
        canvas.save();
        canvas.clipPath(shape);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    /** Whether a point of the stage falls on the card. */
    boolean holds(float x, float y) {
        float left = getTranslationX() + to.left;
        float top = getTranslationY() + to.top;
        return x >= left && x <= left + to.width() && y >= top && y <= top + to.height();
    }

    void close() {
        if (getParent() == null) {
            return;
        }
        animate().cancel();
        stage.removeView(this);
        if (stage instanceof Stage) {
            ((Stage) stage).offered(null);
        }
    }
}
