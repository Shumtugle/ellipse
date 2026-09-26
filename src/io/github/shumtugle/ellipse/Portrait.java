package io.github.shumtugle.ellipse;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.OutputStream;

/**
 * A picture of the home screen to show others: on a soft, darkened spread
 * of its own wallpaper, the screen itself in a rounded card with a shadow;
 * beside it the weekday and the date, a swatch of the wallpaper, and the
 * home screen's name and version in small letters under a line of accent.
 */
final class Portrait {

    private static final int W = 1600;
    private static final int H = 1200;

    private Portrait() {
    }

    static Bitmap compose(Bitmap shot, Bitmap wall, String weekday, String date, int accent, String version) {
        Bitmap out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        /* The wallpaper spread under all, softened by being drawn small and large again, and darkened. */
        Bitmap small = Bitmap.createScaledBitmap(wall, Math.max(1, wall.getWidth() / 24),
            Math.max(1, wall.getHeight() / 24), true);
        float cover = Math.max(W / (float) small.getWidth(), H / (float) small.getHeight());
        float sw = small.getWidth() * cover;
        float sh = small.getHeight() * cover;
        c.drawBitmap(small, null, new RectF((W - sw) / 2f, (H - sh) / 2f, (W + sw) / 2f, (H + sh) / 2f), p);
        small.recycle();
        p.setShader(new LinearGradient(0, 0, W, 0, 0x99000000, 0xCC000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
        /* The screen, in a card. */
        float tall = H - 160f;
        float wide = tall * shot.getWidth() / shot.getHeight();
        RectF card = new RectF(150f, 80f, 150f + wide, 80f + tall);
        float round = wide * 0.09f;
        Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        shade.setColor(0xFF000000);
        shade.setShadowLayer(48f, 0f, 18f, 0xB0000000);
        c.drawRoundRect(card, round, round, shade);
        BitmapShader screen = new BitmapShader(shot, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        android.graphics.Matrix fit = new android.graphics.Matrix();
        fit.setScale(wide / shot.getWidth(), tall / shot.getHeight());
        fit.postTranslate(card.left, card.top);
        screen.setLocalMatrix(fit);
        p.setShader(screen);
        c.drawRoundRect(card, round, round, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(0x33FFFFFF);
        c.drawRoundRect(card, round, round, p);
        p.setStyle(Paint.Style.FILL);
        /* The day, the wallpaper's swatch, the name. */
        float x = card.right + 110f;
        Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
        words.setColor(0xFFF2EDE4);
        words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        words.setTextSize(46f);
        c.drawText(capital(weekday), x, 250f, words);
        words.setTextSize(80f);
        words.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        fitText(c, date, x, 350f, W - x - 90f, words);
        /* The wallpaper whole, as a small upright picture of its own: not a middle cut, which on a
           ground lit at one place may be only dark. */
        float swatchTall = 500f;
        float swatchWide = swatchTall * wall.getWidth() / wall.getHeight();
        RectF swatch = new RectF(x, 440f, x + swatchWide, 440f + swatchTall);
        BitmapShader ground = new BitmapShader(wall, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        android.graphics.Matrix fit2 = new android.graphics.Matrix();
        fit2.setScale(swatchWide / wall.getWidth(), swatchTall / wall.getHeight());
        fit2.postTranslate(swatch.left, swatch.top);
        ground.setLocalMatrix(fit2);
        float corner = swatchWide * 0.09f;
        c.drawRoundRect(swatch, corner, corner, shade);
        p.setShader(ground);
        c.drawRoundRect(swatch, corner, corner, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(0x33FFFFFF);
        c.drawRoundRect(swatch, corner, corner, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(accent);
        c.drawRect(x, H - 190f, x + 90f, H - 184f, p);
        words.setTextSize(44f);
        words.setColor(0xFFF2EDE4);
        c.drawText("Ellipse", x, H - 120f, words);
        words.setTextSize(28f);
        words.setColor(0x99F2EDE4);
        c.drawText(version, x, H - 78f, words);
        return out;
    }

    private static String capital(String word) {
        return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /** Words made smaller until they fit a width. */
    private static void fitText(Canvas c, String text, float x, float y, float room, Paint paint) {
        while (paint.measureText(text) > room && paint.getTextSize() > 30f) {
            paint.setTextSize(paint.getTextSize() - 4f);
        }
        c.drawText(text, x, y, paint);
    }

    /** The picture among the phone's own, in a folder of the home screen's name; its address returned. */
    static Uri save(Context context, Bitmap picture) {
        ContentValues v = new ContentValues();
        String name = "ellipse-" + new java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.ROOT)
            .format(new java.util.Date()) + ".png";
        v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ellipse");
        Uri made = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (made == null) {
            return null;
        }
        try (OutputStream out = context.getContentResolver().openOutputStream(made)) {
            if (out == null || !picture.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                return null;
            }
        } catch (java.io.IOException failed) {
            return null;
        }
        return made;
    }
}
