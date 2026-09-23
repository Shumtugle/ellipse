package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Map;

/**
 * How each kind of thing on a home screen looks.
 *
 * An application, a shortcut and the door are a tile alone, without a name:
 * on the home screens the tile is the name. A folder is its first four
 * applications in small tiles, two by two, with no plate under them. A
 * widget's place, until widgets live here, is a quiet outline the size of
 * the widget with the name of its application in it, so the screens keep
 * their shape.
 */
final class Things {

    private Things() {
    }

    /** A tile standing in its cell, the tile centred and the cell pressable. */
    static ImageView tile(Context context, Icons icons, final App app, String key, int width,
                          final Drawable fallback) {
        ImageView face = new ImageView(context);
        face.setScaleType(ImageView.ScaleType.CENTER);
        face.setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.L));
        if (app != null) {
            icons.put(face, app, width);
            face.setContentDescription(app.label);
        } else {
            icons.put(face, key, width, new Icons.Source() {
                public Drawable icon() {
                    return fallback;
                }
            });
        }
        return face;
    }

    /** A folder: up to four small tiles, two by two. */
    static View folder(Context context, Icons icons, List<Held> held, int width, String name) {
        LinearLayout face = new LinearLayout(context);
        face.setOrientation(LinearLayout.VERTICAL);
        face.setGravity(Gravity.CENTER);
        face.setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.L));
        face.setContentDescription(name);
        int gap = Round.dp(3f);
        int small = (width - gap) / 2;
        int tall = Tile.height(small, icons.look());
        LinearLayout row = null;
        int shown = 0;
        for (final Held app : held) {
            if (shown % 2 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(width, tall);
                rowParams.topMargin = shown == 0 ? 0 : gap;
                face.addView(row, rowParams);
            }
            ImageView mini = new ImageView(context);
            mini.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (app.app != null) {
                icons.put(mini, app.app, small);
            } else {
                final Context where = context;
                final int dpi = context.getResources().getDisplayMetrics().densityDpi;
                icons.put(mini, app.written, small, new Icons.Source() {
                    public android.graphics.drawable.Drawable icon() {
                        return app.icon(where, dpi);
                    }
                });
            }
            LinearLayout.LayoutParams miniParams = new LinearLayout.LayoutParams(small, tall);
            miniParams.leftMargin = shown % 2 == 0 ? 0 : gap;
            row.addView(mini, miniParams);
            shown++;
            if (shown == 4) {
                break;
            }
        }
        if (shown % 2 == 1) {
            row.addView(new View(context), new LinearLayout.LayoutParams(small + gap, tall));
        }
        return face;
    }

    /** A widget's place, waiting for the widget: a press on it places it. */
    static View place(Context context, String provider) {
        return new Place(context, owner(context, provider), Words.s("tap_to_place"));
    }

    /** The name of the application a widget comes from. */
    private static String owner(Context context, String provider) {
        if (provider == null) {
            return "";
        }
        int slash = provider.indexOf('/');
        String pkg = slash > 0 ? provider.substring(0, slash) : provider;
        PackageManager pm = context.getPackageManager();
        try {
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException gone) {
            return pkg;
        }
    }

    /** A quiet outline with a name in it. */
    private static final class Place extends View {

        private final String name;
        private final String hint;
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();

        Place(Context context, String name, String hint) {
            super(context);
            this.name = name;
            this.hint = hint;
            small.setColor(Tone.of(Tone.PRIMARY, 0.9f));
            small.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, Letter.size(Letter.LABEL_M),
                context.getResources().getDisplayMetrics()));
            small.setTextAlign(Paint.Align.CENTER);
            setContentDescription(name);
            setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.XL));
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(Round.px(1f));
            edge.setColor(Tone.of(Tone.OUTLINE, 0.7f));
            edge.setPathEffect(new android.graphics.DashPathEffect(
                new float[] {Round.px(6f), Round.px(5f)}, 0f));
            words.setColor(Tone.of(Tone.ON_SURFACE_VARIANT, 0.8f));
            words.setTextSize(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, Letter.size(Letter.LABEL_L),
                getResources().getDisplayMetrics()));
            words.setTextAlign(Paint.Align.CENTER);
            words.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float inset = Round.px(6f);
            box.set(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRoundRect(box, Round.px(Round.XL), Round.px(Round.XL), edge);
            Paint.FontMetrics m = words.getFontMetrics();
            float y = getHeight() / 2f - (m.ascent + m.descent) / 2f - Round.px(10f);
            canvas.drawText(name, getWidth() / 2f, y, words);
            canvas.drawText(hint, getWidth() / 2f, y + Round.px(24f), small);
        }
    }
}
