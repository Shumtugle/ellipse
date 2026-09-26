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
 * A picture of the home screen to show others: the screen itself in the
 * frame of a phone — a dark body, a fine bright edge, the camera's dot —
 * standing on a soft, darkened spread of its own wallpaper; and under it a
 * caption: the home screen's name and version, the phone and its grid,
 * and the day.
 */
final class Portrait {

    private static final int W = 1080;
    private static final int H = 1350;

    private Portrait() {
    }

    static Bitmap compose(Bitmap shot, Bitmap wall, String when, String version, String data, int accent) {
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
        p.setShader(new LinearGradient(0, 0, 0, H, 0x8C000000, 0xD9000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
        /* The phone: a body a little larger than its screen, then the screen within. */
        float screenTall = 1010f;
        float screenWide = screenTall * shot.getWidth() / shot.getHeight();
        float bezel = 16f;
        RectF body = new RectF((W - screenWide) / 2f - bezel, 64f, (W + screenWide) / 2f + bezel,
            64f + screenTall + 2 * bezel);
        float bodyRound = screenWide * 0.12f;
        Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        shade.setColor(0xFF0B0B0C);
        shade.setShadowLayer(60f, 0f, 24f, 0xC0000000);
        c.drawRoundRect(body, bodyRound, bodyRound, shade);
        /* The frame's edge: a fine light along it, brighter at the top. */
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setShader(new LinearGradient(0, body.top, 0, body.bottom, 0x80FFFFFF, 0x26FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(body, bodyRound, bodyRound, p);
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        /* The side keys, barely there. */
        p.setColor(0xFF2A2A2C);
        c.drawRoundRect(body.right - 1f, body.top + 230f, body.right + 5f, body.top + 330f, 3f, 3f, p);
        c.drawRoundRect(body.right - 1f, body.top + 360f, body.right + 5f, body.top + 420f, 3f, 3f, p);
        RectF screen = new RectF(body.left + bezel, body.top + bezel, body.right - bezel, body.bottom - bezel);
        float screenRound = bodyRound - bezel;
        BitmapShader picture = new BitmapShader(shot, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        android.graphics.Matrix fit = new android.graphics.Matrix();
        fit.setScale(screen.width() / shot.getWidth(), screen.height() / shot.getHeight());
        fit.postTranslate(screen.left, screen.top);
        picture.setLocalMatrix(fit);
        p.setShader(picture);
        c.drawRoundRect(screen, screenRound, screenRound, p);
        p.setShader(null);
        /* The camera's dot at the top of the screen. */
        p.setColor(0xFF050505);
        c.drawCircle(screen.centerX(), screen.top + 22f, 9f, p);
        p.setColor(0x33FFFFFF);
        c.drawCircle(screen.centerX() - 2.5f, screen.top + 19.5f, 2.2f, p);
        /* The caption: a short line of the accent, the name and version, the data, the day. */
        float y = body.bottom + 72f;
        p.setColor(accent);
        c.drawRect(W / 2f - 36f, y - 42f, W / 2f + 36f, y - 37f, p);
        Paint words = new Paint(Paint.ANTI_ALIAS_FLAG);
        words.setTextAlign(Paint.Align.CENTER);
        words.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        words.setColor(0xFFF2EDE4);
        words.setTextSize(40f);
        c.drawText("Ellipse " + version, W / 2f, y, words);
        words.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        words.setColor(0xB3F2EDE4);
        words.setTextSize(27f);
        fitText(c, data, W / 2f, y + 46f, W - 160f, words);
        words.setColor(0x80F2EDE4);
        words.setTextSize(25f);
        fitText(c, when, W / 2f, y + 86f, W - 160f, words);
        return out;
    }

    /** Words made smaller until they fit a width. */
    private static void fitText(Canvas c, String text, float x, float y, float room, Paint paint) {
        while (paint.measureText(text) > room && paint.getTextSize() > 16f) {
            paint.setTextSize(paint.getTextSize() - 2f);
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
