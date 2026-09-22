package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Date;
import java.util.Locale;

/**
 * The home screen's own clock: the hour, the date, the weather and the
 * charge, on a plate of the same material the tiles are cut from.
 *
 * It is built as a tile is built, in three layers: the plate, lit from
 * above and shaded below; windows cut in it, each a pane of dark glass
 * with a thin line at its edge; and, if the tiles have it, the glaze of
 * glass across the top. On the left, a round window: the dial of a clock
 * with its hands, or the weather large. On the right, a long window with
 * the hour in figures and the date, and under it small windows: the
 * weather or an application of the owner's choosing, the phone's charge,
 * and, while headphones that tell their charge are near, theirs.
 *
 * The hands move once a second while the clock is in sight, and not at
 * all when it is not. A press on a window opens what it shows about: the
 * clock, the calendar, the chosen application, the phone's battery.
 */
final class Almanac extends View {

    /** What the clock asks the screen to do. */
    interface Hand {
        void pressed(String window);
    }

    static final String DIAL = "dial";
    static final String WEATHER = "weather";
    static final String APP = "app";
    static final String NONE = "none";

    /** How the clock is made: like the tiles, out of glass, or flat as the design system has it. */
    static final String LIKE_TILE = "tile";
    static final String GLASS = "glass";
    static final String FLAT = "flat";

    private Tile.Look look;
    private String style = LIKE_TILE;
    /** How far the plate is let go: nothing at all at one, and only the windows are left standing. */
    private float veil;
    private String big = DIAL;
    private String small = WEATHER;
    private String app;
    private boolean time = true;
    private boolean date = true;
    private boolean showCharge = true;
    private boolean showEars = true;
    private Bitmap chosen;
    private Sky.Now weather = new Sky.Now(false, 0f, 3, true);
    private int charge = -1;
    private boolean charging;
    private int ears = -1;
    private Hand hand;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint glass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dialBox = new RectF();
    private final RectF timeBox = new RectF();
    private final RectF smallBox = new RectF();
    private final RectF chargeBox = new RectF();
    private final RectF earsBox = new RectF();
    private final GestureDetector press;
    private final RectF pressed = new RectF();

    private final Runnable tick = new Runnable() {
        public void run() {
            invalidate();
            long now = System.currentTimeMillis();
            postDelayed(this, 1000L - now % 1000L);
        }
    };

    Almanac(Context context) {
        super(context);
        edge.setStyle(Paint.Style.STROKE);
        edge.setColor(0x66000000);
        press = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (hand == null) {
                    return false;
                }
                float x = e.getX();
                float y = e.getY();
                for (RectF box : new RectF[] {dialBox, timeBox, smallBox, chargeBox, earsBox}) {
                    if (box.contains(x, y)) {
                        pressed.set(box);
                    }
                }
                if (dialBox.contains(x, y)) {
                    hand.pressed(big);
                } else if (timeBox.contains(x, y)) {
                    hand.pressed(date ? "calendar" : DIAL);
                } else if (smallBox.contains(x, y)) {
                    hand.pressed(APP.equals(small) ? app : small);
                } else if (chargeBox.contains(x, y) || earsBox.contains(x, y)) {
                    hand.pressed("battery");
                }
                playSoundEffect(android.view.SoundEffectConstants.CLICK);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                performLongClick();
            }
        });
    }

    /**
     * What the clock shows, part by part, from its own settings: the round
     * window a dial, the weather or nothing; the hour and the date each
     * shown or not; the small window the weather, an application or
     * nothing; the charges each shown or not. Any of them can go, down to
     * the date alone.
     */
    void dress(Tile.Look look, java.util.Map<String, String> options, Bitmap chosen, Hand hand) {
        this.look = look;
        style = part(options, "style", LIKE_TILE);
        veil = 0f;
        try {
            String said = options.get("veil");
            veil = said == null ? 0f : Math.max(0f, Math.min(1f, Integer.parseInt(said) / 100f));
        } catch (NumberFormatException none) {
            veil = 0f;
        }
        big = part(options, "big", DIAL);
        String kept = part(options, "small", WEATHER);
        app = options.get("app");
        if (!WEATHER.equals(kept) && !APP.equals(kept) && !NONE.equals(kept)) {
            // An application kept by its name alone, as earlier clocks kept it.
            app = kept;
            kept = APP;
        }
        small = kept;
        time = !"off".equals(options.get("time"));
        date = !"off".equals(options.get("date"));
        showCharge = !"off".equals(options.get("charge"));
        showEars = !"off".equals(options.get("ears"));
        this.chosen = chosen;
        this.hand = hand;
        invalidate();
    }

    private static String part(java.util.Map<String, String> options, String key, String otherwise) {
        String value = options.get(key);
        return value == null || value.length() == 0 ? otherwise : value;
    }

    /** The application the small window opens, by its name, if one is chosen. */
    String app() {
        return app;
    }

    /** The window last pressed, in this view's own measure. */
    RectF pressed() {
        return new RectF(pressed);
    }

    void weather(Sky.Now now) {
        weather = now;
        invalidate();
    }

    void charge(int level, boolean plugged) {
        charge = level;
        charging = plugged;
        invalidate();
    }

    void ears(int level) {
        ears = level;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return press.onTouchEvent(e) || super.onTouchEvent(e);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        removeCallbacks(tick);
        if (visibility == VISIBLE) {
            post(tick);
        }
    }

    // ------------------------------------------------------------ drawing

    @Override
    protected void onDraw(Canvas canvas) {
        if (look == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float inset = Round.px(4f);
        Path plate = Tile.curve(inset, inset, w - 2f * inset, h - 2f * inset, Math.max(6f, look.power * 1.6f));
        // The default draws the clock the design system's way: a card of the
        // surface, the dial on a scalloped face, the windows each in a
        // container of its own colour, all flat.
        // Like the tiles means like the tiles themselves: whatever material
        // they are cut from, the clock is cut from. Only when they have no
        // material of their own — no rim, or no mask over the icons — does
        // the clock fall back on the flat way or on a plate of shade.
        boolean glass = GLASS.equals(style);
        boolean material = !glass && !FLAT.equals(style)
            && look.rim != Tile.Look.BARE && look.window != Tile.Look.RAW;
        boolean flat = !glass && !material && (FLAT.equals(style) || Cast.flat);
        int held = -1;
        if (veil > 0.01f) {
            // The plate let go: what is under it, the table itself, comes through.
            held = canvas.saveLayerAlpha(0f, 0f, w, h, Math.round(255f * (1f - veil)));
        }
        if (glass) {
            // A pane of smoked glass lying on the wallpaper, with the light
            // caught along its upper edge: nothing of the table is hidden,
            // only darkened.
            // Glass is seen through: a breath of smoke over the table, a
            // brighter line where the light lies along its upper edge, and a
            // sheen falling across its top.
            paint.setShader(new RadialGradient(w * 0.5f, h * 0.15f, Math.max(w, h) * 0.9f,
                0x40312A22, 0x73090807, Shader.TileMode.CLAMP));
            canvas.drawPath(plate, paint);
            paint.setShader(new LinearGradient(0f, inset, 0f, h * 0.55f, 0x26FFFFFF, 0x00FFFFFF,
                Shader.TileMode.CLAMP));
            canvas.drawPath(plate, paint);
            paint.setShader(null);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(Math.max(1f, Round.px(1f)));
            edge.setShader(new LinearGradient(0f, inset, 0f, h - inset, 0x8CFFFFFF, 0x14FFFFFF,
                Shader.TileMode.CLAMP));
            canvas.drawPath(plate, edge);
            edge.setShader(null);
        } else if (flat) {
            paint.setShader(null);
            paint.setColor(Tone.of(Tone.SURFACE_CONTAINER));
            float corner = Math.min(Round.px(28f), (h - 2f * inset) / 2f);
            canvas.drawRoundRect(inset, inset, w - inset, h - inset, corner, corner, paint);
        } else if (material) {
            canvas.save();
            canvas.translate(0f, 0f);
            Tile.plate(canvas, plate, look.rim, w, h);
            canvas.restore();
        } else {
            paint.setShader(null);
            paint.setColor(0x80000000);
            canvas.drawPath(plate, paint);
        }
        if (held >= 0) {
            canvas.restoreToCount(held);
        }
        int accent = flat ? Tone.of(Tone.PRIMARY) : glass ? 0xFFF4ECDD : Tile.accentOf(look.rim);
        int ink = 0xFFF4ECDD;
        int quiet = 0xB3F4ECDD;
        int dialInk = flat ? Tone.of(Tone.ON_PRIMARY_CONTAINER) : ink;
        int dialQuiet = flat ? (dialInk & 0x00FFFFFF) | 0x99000000 : quiet;
        int timeInk = flat ? Tone.of(Tone.ON_SECONDARY_CONTAINER) : ink;
        int timeQuiet = flat ? (timeInk & 0x00FFFFFF) | 0xB3000000 : quiet;
        final int pillInk = flat ? Tone.of(Tone.ON_TERTIARY_CONTAINER) : ink;
        final int pillMark = flat ? pillInk : glass ? ink : Tile.accentOf(look.rim);
        edge.setStrokeWidth(Math.max(1f, w * 0.0025f));

        // What is shown decides how the room is shared. The round window
        // takes a little under half the width, or the whole height, or, with
        // nothing beside it, stands in the middle; the long window takes the
        // upper part of what is left, the small windows the lower; either
        // takes all of it when the other is not there.
        float pad = Math.max(Round.px(12f), look.width * h * 1.2f);
        float gap = Round.px(10f);
        boolean round = !NONE.equals(big);
        boolean long_ = time || date;
        List<RectF> smalls = new ArrayList<RectF>();
        List<String> kinds = new ArrayList<String>();
        if (!NONE.equals(small)) {
            kinds.add("small");
        }
        if (showCharge) {
            kinds.add("charge");
        }
        if (showEars && ears >= 0) {
            kinds.add("ears");
        }
        boolean right = long_ || !kinds.isEmpty();
        float column = h - 2f * pad;
        dialBox.setEmpty();
        timeBox.setEmpty();
        smallBox.setEmpty();
        chargeBox.setEmpty();
        earsBox.setEmpty();
        float left = pad + inset;
        if (round) {
            float side = right ? Math.min(column, (w - 2f * pad - 2f * inset) * 0.44f) : column;
            float x = right ? pad + inset : (w - side) / 2f;
            dialBox.set(x, (h - side) / 2f, x + side, (h + side) / 2f);
            left = dialBox.right + gap * 1.4f;
        }
        float rightEdge = w - pad - inset;
        float top = pad;
        float bottom = h - pad;
        if (long_) {
            float lower = kinds.isEmpty() ? bottom : top + (column - gap) * 0.58f;
            timeBox.set(left, top, rightEdge, lower);
        }
        if (!kinds.isEmpty()) {
            float pillsTop = long_ ? timeBox.bottom + gap : top;
            float pillW = (rightEdge - left - gap * (kinds.size() - 1)) / kinds.size();
            for (int i = 0; i < kinds.size(); i++) {
                float x = left + i * (pillW + gap);
                RectF box = "small".equals(kinds.get(i)) ? smallBox
                    : "charge".equals(kinds.get(i)) ? chargeBox : earsBox;
                box.set(x, pillsTop, x + pillW, bottom);
            }
        }

        // The round window: the dial, or the weather large.
        if (round) {
            float side = dialBox.width();
            pane(canvas, dialBox, true, Tone.PRIMARY_CONTAINER);
            if (WEATHER.equals(big)) {
                Sky.draw(canvas, weather, dialBox.centerX(), dialBox.centerY() - side * 0.08f, side * 0.5f, dialInk);
                words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                words.setTextSize(side * 0.2f);
                words.setColor(dialInk);
                words.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(Sky.degrees(weather), dialBox.centerX(), dialBox.centerY() + side * 0.34f, words);
            } else {
                dial(canvas, dialBox, dialInk, dialQuiet, accent);
            }
        }

        // The long window: the hour in figures, the date under it; or either alone, larger.
        if (long_) {
            pane(canvas, timeBox, false, Tone.SECONDARY_CONTAINER);
            Date now = new Date();
            words.setTextAlign(Paint.Align.CENTER);
            if (time) {
                String hour = android.text.format.DateFormat.getTimeFormat(getContext()).format(now);
                words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                words.setColor(timeInk);
                float share = date ? 0.5f : 0.62f;
                words.setTextSize(fit(hour, timeBox.width() * 0.84f, timeBox.height() * share));
                Paint.FontMetrics f = words.getFontMetrics();
                float baseline = date ? timeBox.top + timeBox.height() * 0.56f
                    : timeBox.centerY() - (f.ascent + f.descent) / 2f;
                canvas.drawText(hour, timeBox.centerX(), baseline, words);
            }
            if (date && time) {
                String day = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "EEEdMMMM"), Locale.getDefault()).format(now);
                words.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                words.setColor(timeQuiet);
                words.setTextSize(fit(day, timeBox.width() * 0.84f, timeBox.height() * 0.17f));
                canvas.drawText(day, timeBox.centerX(), timeBox.top + timeBox.height() * 0.84f, words);
            } else if (date) {
                // The date alone is a leaf of a calendar: the day and month
                // large, the day of the week under them.
                String leaf = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "dMMMM"), Locale.getDefault()).format(now);
                String weekday = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now);
                words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                words.setColor(timeInk);
                words.setTextSize(fit(leaf, timeBox.width() * 0.84f, timeBox.height() * 0.36f));
                canvas.drawText(leaf, timeBox.centerX(), timeBox.top + timeBox.height() * 0.52f, words);
                words.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                words.setColor(accent);
                words.setTextSize(fit(weekday, timeBox.width() * 0.84f, timeBox.height() * 0.18f));
                canvas.drawText(weekday, timeBox.centerX(), timeBox.top + timeBox.height() * 0.8f, words);
            }
        }

        // The small windows: the weather or the chosen application, the charges.
        if (!smallBox.isEmpty()) {
            pane(canvas, smallBox, false, Tone.TERTIARY_CONTAINER);
            if (WEATHER.equals(small)) {
                pill(canvas, smallBox, Sky.degrees(weather), pillInk, new Mark() {
                    public void draw(Canvas c, float cx, float cy, float s) {
                        Sky.draw(c, weather, cx, cy, s, pillMark);
                    }
                });
            } else if (chosen != null) {
                float s = Math.min(smallBox.height() * 0.78f, smallBox.width() * 0.8f / look.ratio);
                RectF at = new RectF(smallBox.centerX() - s * look.ratio / 2f, smallBox.centerY() - s / 2f,
                    smallBox.centerX() + s * look.ratio / 2f, smallBox.centerY() + s / 2f);
                canvas.drawBitmap(chosen, null, at, paint);
            }
        }
        if (!chargeBox.isEmpty()) {
            pane(canvas, chargeBox, false, Tone.TERTIARY_CONTAINER);
            pill(canvas, chargeBox, charge >= 0 ? charge + "%" : "\u2013", pillInk, new Mark() {
                public void draw(Canvas c, float cx, float cy, float s) {
                    battery(c, cx, cy, s, charge, charging, pillMark);
                }
            });
        }
        if (!earsBox.isEmpty()) {
            pane(canvas, earsBox, false, Tone.TERTIARY_CONTAINER);
            pill(canvas, earsBox, ears + "%", pillInk, new Mark() {
                public void draw(Canvas c, float cx, float cy, float s) {
                    headphones(c, cx, cy, s, pillMark);
                }
            });
        }

        if (material && look.gloss && veil < 0.02f) {
            Tile.glaze(canvas, plate, inset, inset, w - 2f * inset, h - 2f * inset);
        }
    }

    /**
     * A pane of dark glass, round or long, with the thin line of its cut;
     * drawn flat, a container of one of the scheme's colours, the round one
     * scalloped like a clock's face.
     */
    private void pane(Canvas canvas, RectF box, boolean round, int role) {
        if (GLASS.equals(style)) {
            // Windows cut in the pane: a little darker than it, their cut
            // catching the same light.
            glass.setShader(null);
            glass.setColor(0x59000000);
            edge.setColor(0x40FFFFFF);
            if (round) {
                canvas.drawOval(box, glass);
                canvas.drawOval(box, edge);
            } else {
                float r = Math.min(box.height() / 2f, Round.px(24f));
                canvas.drawRoundRect(box, r, r, glass);
                canvas.drawRoundRect(box, r, r, edge);
            }
            edge.setColor(0x66000000);
            return;
        }
        if (FLAT.equals(style) || (Cast.flat && LIKE_TILE.equals(style))) {
            glass.setShader(null);
            glass.setColor(Tone.of(role));
            if (round) {
                canvas.drawPath(Cast.cookie(box.centerX(), box.centerY(), box.width() / 2f, 12, 0.07f), glass);
            } else {
                float r = Math.min(box.height() / 2f, Round.px(24f));
                canvas.drawRoundRect(box, r, r, glass);
            }
            return;
        }
        glass.setShader(new RadialGradient(box.centerX(), box.top + box.height() * 0.35f,
            Math.max(box.width(), box.height()) * 0.8f, 0xE62A2520, 0xF20C0B0A, Shader.TileMode.CLAMP));
        if (round) {
            canvas.drawOval(box, glass);
            canvas.drawOval(box, edge);
        } else {
            float r = Math.min(box.height() / 2f, Round.px(24f));
            canvas.drawRoundRect(box, r, r, glass);
            canvas.drawRoundRect(box, r, r, edge);
        }
    }

    /** Something drawn in a small window beside its words. */
    private interface Mark {
        void draw(Canvas canvas, float cx, float cy, float size);
    }

    private void pill(Canvas canvas, RectF box, String text, int ink, Mark mark) {
        float s = Math.min(box.height() * 0.5f, box.width() * 0.3f);
        words.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        words.setColor(ink);
        words.setTextAlign(Paint.Align.LEFT);
        words.setTextSize(fit(text, box.width() * 0.5f, box.height() * 0.36f));
        float textW = words.measureText(text);
        float all = s + Round.px(6f) + textW;
        float start = box.centerX() - all / 2f;
        mark.draw(canvas, start + s / 2f, box.centerY(), s);
        Paint.FontMetrics f = words.getFontMetrics();
        canvas.drawText(text, start + s + Round.px(6f), box.centerY() - (f.ascent + f.descent) / 2f, words);
    }

    /** The largest size, up to a height, at which words fit a width. */
    private float fit(String text, float width, float height) {
        words.setTextSize(height);
        float measured = words.measureText(text);
        return measured > width ? height * width / measured : height;
    }

    /** A dial with twelve marks and three hands, the seconds in the material's own colour. */
    private void dial(Canvas canvas, RectF box, int ink, int quiet, int accent) {
        float cx = box.centerX();
        float cy = box.centerY();
        float r = box.width() / 2f;
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            boolean main = i % 3 == 0;
            paint.setColor(main ? ink : quiet);
            paint.setStrokeWidth(r * (main ? 0.04f : 0.025f));
            float from = r * (main ? 0.72f : 0.8f);
            float to = r * 0.86f;
            canvas.drawLine(cx + (float) Math.sin(a) * from, cy - (float) Math.cos(a) * from,
                cx + (float) Math.sin(a) * to, cy - (float) Math.cos(a) * to, paint);
        }
        Calendar now = Calendar.getInstance();
        float seconds = now.get(Calendar.SECOND);
        float minutes = now.get(Calendar.MINUTE) + seconds / 60f;
        float hours = now.get(Calendar.HOUR) + minutes / 60f;
        hand(canvas, cx, cy, hours / 12f, r * 0.46f, r * 0.075f, ink);
        hand(canvas, cx, cy, minutes / 60f, r * 0.68f, r * 0.05f, ink);
        hand(canvas, cx, cy, seconds / 60f, r * 0.76f, r * 0.018f, accent);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        canvas.drawCircle(cx, cy, r * 0.045f, paint);
    }

    private void hand(Canvas canvas, float cx, float cy, float turn, float length, float width, int colour) {
        double a = Math.PI * 2 * turn;
        paint.setColor(colour);
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx - (float) Math.sin(a) * length * 0.12f, cy + (float) Math.cos(a) * length * 0.12f,
            cx + (float) Math.sin(a) * length, cy - (float) Math.cos(a) * length, paint);
    }

    private void battery(Canvas canvas, float cx, float cy, float s, int level, boolean plugged, int colour) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(colour);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.07f);
        RectF body = new RectF(cx - s * 0.2f, cy - s * 0.34f, cx + s * 0.2f, cy + s * 0.38f);
        canvas.drawRoundRect(body, s * 0.07f, s * 0.07f, p);
        p.setStyle(Paint.Style.FILL);
        canvas.drawRect(cx - s * 0.08f, cy - s * 0.44f, cx + s * 0.08f, cy - s * 0.36f, p);
        float full = Math.max(0f, Math.min(1f, level / 100f));
        float inner = body.height() - s * 0.16f;
        canvas.drawRect(body.left + s * 0.08f, body.bottom - s * 0.08f - inner * full,
            body.right - s * 0.08f, body.bottom - s * 0.08f, p);
        if (plugged) {
            p.setColor(0xFF1A1714);
            Path bolt = new Path();
            bolt.moveTo(cx + s * 0.04f, cy - s * 0.2f);
            bolt.lineTo(cx - s * 0.08f, cy + s * 0.04f);
            bolt.lineTo(cx + s * 0.01f, cy + s * 0.04f);
            bolt.lineTo(cx - s * 0.04f, cy + s * 0.24f);
            bolt.lineTo(cx + s * 0.09f, cy - s * 0.02f);
            bolt.lineTo(cx, cy - s * 0.02f);
            bolt.close();
            canvas.drawPath(bolt, p);
        }
    }

    private void headphones(Canvas canvas, float cx, float cy, float s, int colour) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(colour);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.08f);
        p.setStrokeCap(Paint.Cap.ROUND);
        RectF arc = new RectF(cx - s * 0.34f, cy - s * 0.36f, cx + s * 0.34f, cy + s * 0.32f);
        canvas.drawArc(arc, 180f, 180f, false, p);
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(cx - s * 0.4f, cy - s * 0.02f, cx - s * 0.2f, cy + s * 0.34f),
            s * 0.06f, s * 0.06f, p);
        canvas.drawRoundRect(new RectF(cx + s * 0.2f, cy - s * 0.02f, cx + s * 0.4f, cy + s * 0.34f),
            s * 0.06f, s * 0.06f, p);
    }
}
