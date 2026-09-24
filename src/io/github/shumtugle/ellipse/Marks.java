package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The home screen's own drawings for the apps every phone has: the phone,
 * messages, the camera, pictures, maps, the calendar, the clock, contacts,
 * settings, the web, mail, the store, files, music, the calculator and the
 * weather. They stand in for an app's own one-colour picture when that
 * picture is poor, or missing, and the owner asks for them.
 *
 * Which app is which is not guessed from its name: it is asked of the
 * phone, by the work the app says it does — the one that dials, the one
 * that takes pictures, the one that opens a calendar — so the drawings find
 * their apps whoever made them.
 */
final class Marks {

    static final int NONE = -1;
    static final int PHONE = 0;
    static final int MESSAGE = 1;
    static final int CAMERA = 2;
    static final int PHOTOS = 3;
    static final int MAPS = 4;
    static final int CALENDAR = 5;
    static final int CLOCK = 6;
    static final int CONTACTS = 7;
    static final int SETTINGS = 8;
    static final int BROWSER = 9;
    static final int MAIL = 10;
    static final int STORE = 11;
    static final int FILES = 12;
    static final int MUSIC = 13;
    static final int CALCULATOR = 14;
    static final int WEATHER = 15;

    private static Map<String, Integer> byOwner;

    private Marks() {
    }

    /** Asks the phone once which app does which work; again when apps come and go. */
    static void learn(Context context, boolean again) {
        if (byOwner == null || again) {
            byOwner = learn(context);
        }
    }

    /** The drawing that stands for an app, or none. */
    static int of(String owner) {
        Integer kind = byOwner == null ? null : byOwner.get(owner);
        return kind == null ? NONE : kind;
    }

    private static Map<String, Integer> learn(Context context) {
        Map<String, Integer> found = new HashMap<>();
        PackageManager manager = context.getPackageManager();
        Object[][] asks = {
            {PHONE, new Intent(Intent.ACTION_DIAL)},
            {MESSAGE, new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))},
            {CAMERA, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)},
            {PHOTOS, main(Intent.CATEGORY_APP_GALLERY)},
            {MAPS, main(Intent.CATEGORY_APP_MAPS)},
            {CALENDAR, main(Intent.CATEGORY_APP_CALENDAR)},
            {CLOCK, new Intent(AlarmClock.ACTION_SHOW_ALARMS)},
            {CONTACTS, main(Intent.CATEGORY_APP_CONTACTS)},
            {SETTINGS, new Intent(Settings.ACTION_SETTINGS)},
            {BROWSER, main(Intent.CATEGORY_APP_BROWSER)},
            {MAIL, main(Intent.CATEGORY_APP_EMAIL)},
            {STORE, main(Intent.CATEGORY_APP_MARKET)},
            {FILES, main("android.intent.category.APP_FILES")},
            {MUSIC, main(Intent.CATEGORY_APP_MUSIC)},
            {CALCULATOR, main(Intent.CATEGORY_APP_CALCULATOR)},
            {WEATHER, main("android.intent.category.APP_WEATHER")},
            {MESSAGE, main(Intent.CATEGORY_APP_MESSAGING)},
        };
        for (Object[] ask : asks) {
            try {
                List<ResolveInfo> doers = manager.queryIntentActivities((Intent) ask[1], 0);
                for (ResolveInfo one : doers) {
                    String owner = one.activityInfo.packageName;
                    if (!found.containsKey(owner)) {
                        found.put(owner, (Integer) ask[0]);
                    }
                }
            } catch (RuntimeException refused) {
                // This work has nobody who does it.
            }
        }
        return found;
    }

    private static Intent main(String category) {
        return Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category);
    }

    /** The drawing as a one-colour picture, in the layers' square of one hundred and eight. */
    static Drawable picture(int kind) {
        return new Picture(kind);
    }

    /**
     * A drawing on a grid of twenty four, set in the middle of the layers'
     * square at the size the platform's own one-colour pictures take, in
     * whatever colour it is tinted.
     */
    private static final class Picture extends Drawable {
        private final int kind;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

        Picture(int kind) {
            this.kind = kind;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setColor(0xFFFFFFFF);
            fill.setColor(0xFFFFFFFF);
        }

        @Override
        public void setTint(int colour) {
            line.setColor(colour);
            fill.setColor(colour);
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            float side = Math.min(getBounds().width(), getBounds().height());
            float u = side * 0.44f / 24f;
            canvas.save();
            canvas.translate(getBounds().exactCenterX() - 12f * u, getBounds().exactCenterY() - 12f * u);
            line.setStrokeWidth(2.1f * u);
            RectF r = new RectF();
            Path p = new Path();
            switch (kind) {
                case PHONE:
                    p.moveTo(6.6f * u, 3.5f * u);
                    p.lineTo(9.2f * u, 3.5f * u);
                    p.lineTo(10.5f * u, 7.5f * u);
                    p.lineTo(8.7f * u, 9.1f * u);
                    p.quadTo(10.3f * u, 12.5f * u, 13.7f * u, 14.1f * u);
                    p.lineTo(15.3f * u, 12.3f * u);
                    p.lineTo(19.3f * u, 13.6f * u);
                    p.lineTo(19.3f * u, 16.2f * u);
                    p.quadTo(19.3f * u, 18f * u, 17.5f * u, 18f * u);
                    p.cubicTo(9.5f * u, 18f * u, 5f * u, 13.5f * u, 5f * u, 5.3f * u);
                    p.quadTo(5f * u, 3.5f * u, 6.6f * u, 3.5f * u);
                    p.close();
                    canvas.drawPath(p, fill);
                    break;
                case MESSAGE:
                    r.set(3.5f * u, 4.5f * u, 20.5f * u, 16f * u);
                    p.addRoundRect(r, 3.5f * u, 3.5f * u, Path.Direction.CW);
                    Path tail = new Path();
                    tail.moveTo(6f * u, 15f * u);
                    tail.lineTo(6f * u, 20.5f * u);
                    tail.lineTo(11f * u, 15f * u);
                    tail.close();
                    p.op(tail, Path.Op.UNION);
                    canvas.drawPath(p, fill);
                    break;
                case CAMERA:
                    r.set(3f * u, 7f * u, 21f * u, 19.5f * u);
                    p.addRoundRect(r, 2.5f * u, 2.5f * u, Path.Direction.CW);
                    r.set(8f * u, 4.5f * u, 16f * u, 8f * u);
                    p.addRoundRect(r, 1.2f * u, 1.2f * u, Path.Direction.CW);
                    Path lens = new Path();
                    lens.addCircle(12f * u, 13.2f * u, 4f * u, Path.Direction.CW);
                    p.op(lens, Path.Op.DIFFERENCE);
                    canvas.drawPath(p, fill);
                    canvas.drawCircle(12f * u, 13.2f * u, 2f * u, fill);
                    break;
                case PHOTOS:
                    r.set(4f * u, 4f * u, 20f * u, 20f * u);
                    canvas.drawRoundRect(r, 3f * u, 3f * u, line);
                    p.moveTo(6.5f * u, 17.5f * u);
                    p.lineTo(10.5f * u, 12f * u);
                    p.lineTo(13.2f * u, 15f * u);
                    p.lineTo(15f * u, 13f * u);
                    p.lineTo(17.5f * u, 17.5f * u);
                    p.close();
                    canvas.drawPath(p, fill);
                    canvas.drawCircle(15.3f * u, 8.7f * u, 1.7f * u, fill);
                    break;
                case MAPS:
                    p.addCircle(12f * u, 9.5f * u, 6.2f * u, Path.Direction.CW);
                    Path point = new Path();
                    point.moveTo(6.6f * u, 12.4f * u);
                    point.lineTo(12f * u, 21f * u);
                    point.lineTo(17.4f * u, 12.4f * u);
                    point.close();
                    p.op(point, Path.Op.UNION);
                    Path hole = new Path();
                    hole.addCircle(12f * u, 9.5f * u, 2.4f * u, Path.Direction.CW);
                    p.op(hole, Path.Op.DIFFERENCE);
                    canvas.drawPath(p, fill);
                    break;
                case CALENDAR:
                    r.set(4f * u, 5.5f * u, 20f * u, 20f * u);
                    canvas.drawRoundRect(r, 2.5f * u, 2.5f * u, line);
                    r.set(4f * u, 5.5f * u, 20f * u, 10f * u);
                    p.addRoundRect(r, new float[] {2.5f * u, 2.5f * u, 2.5f * u, 2.5f * u, 0, 0, 0, 0},
                        Path.Direction.CW);
                    canvas.drawPath(p, fill);
                    canvas.drawLine(8.5f * u, 3.5f * u, 8.5f * u, 6.5f * u, line);
                    canvas.drawLine(15.5f * u, 3.5f * u, 15.5f * u, 6.5f * u, line);
                    r.set(13.2f * u, 13.2f * u, 16.8f * u, 16.8f * u);
                    canvas.drawRoundRect(r, 0.8f * u, 0.8f * u, fill);
                    break;
                case CLOCK:
                    canvas.drawCircle(12f * u, 12f * u, 8.5f * u, line);
                    canvas.drawLine(12f * u, 12f * u, 12f * u, 7.2f * u, line);
                    canvas.drawLine(12f * u, 12f * u, 15.6f * u, 14.2f * u, line);
                    break;
                case CONTACTS:
                    canvas.drawCircle(12f * u, 8.3f * u, 3.8f * u, fill);
                    r.set(5f * u, 14f * u, 19f * u, 26f * u);
                    p.addRoundRect(r, 7f * u, 7f * u, Path.Direction.CW);
                    Path cut = new Path();
                    cut.addRect(0, 20.2f * u, 24f * u, 30f * u, Path.Direction.CW);
                    p.op(cut, Path.Op.DIFFERENCE);
                    canvas.drawPath(p, fill);
                    break;
                case SETTINGS:
                    p.addCircle(12f * u, 12f * u, 6.2f * u, Path.Direction.CW);
                    for (int i = 0; i < 8; i++) {
                        Path tooth = new Path();
                        tooth.addRoundRect(new RectF(10.4f * u, 2.6f * u, 13.6f * u, 7f * u),
                            1f * u, 1f * u, Path.Direction.CW);
                        android.graphics.Matrix turn = new android.graphics.Matrix();
                        turn.setRotate(45f * i, 12f * u, 12f * u);
                        tooth.transform(turn);
                        p.op(tooth, Path.Op.UNION);
                    }
                    Path middle = new Path();
                    middle.addCircle(12f * u, 12f * u, 2.6f * u, Path.Direction.CW);
                    p.op(middle, Path.Op.DIFFERENCE);
                    canvas.drawPath(p, fill);
                    break;
                case BROWSER:
                    canvas.drawCircle(12f * u, 12f * u, 8.5f * u, line);
                    r.set(8.2f * u, 3.5f * u, 15.8f * u, 20.5f * u);
                    canvas.drawOval(r, line);
                    canvas.drawLine(3.8f * u, 12f * u, 20.2f * u, 12f * u, line);
                    break;
                case MAIL:
                    r.set(3.5f * u, 6f * u, 20.5f * u, 18.5f * u);
                    canvas.drawRoundRect(r, 2.2f * u, 2.2f * u, line);
                    p.moveTo(4.8f * u, 7.6f * u);
                    p.lineTo(12f * u, 13f * u);
                    p.lineTo(19.2f * u, 7.6f * u);
                    canvas.drawPath(p, line);
                    break;
                case STORE:
                    r.set(4.5f * u, 8.5f * u, 19.5f * u, 20.5f * u);
                    canvas.drawRoundRect(r, 2.2f * u, 2.2f * u, fill);
                    r.set(8.5f * u, 3.8f * u, 15.5f * u, 11.5f * u);
                    canvas.drawArc(r, 180f, 180f, false, line);
                    canvas.drawLine(8.5f * u, 7.6f * u, 8.5f * u, 9f * u, line);
                    canvas.drawLine(15.5f * u, 7.6f * u, 15.5f * u, 9f * u, line);
                    break;
                case FILES:
                    p.moveTo(3.5f * u, 7f * u);
                    p.quadTo(3.5f * u, 5f * u, 5.5f * u, 5f * u);
                    p.lineTo(9.8f * u, 5f * u);
                    p.lineTo(11.8f * u, 7f * u);
                    p.lineTo(18.5f * u, 7f * u);
                    p.quadTo(20.5f * u, 7f * u, 20.5f * u, 9f * u);
                    p.lineTo(20.5f * u, 17f * u);
                    p.quadTo(20.5f * u, 19f * u, 18.5f * u, 19f * u);
                    p.lineTo(5.5f * u, 19f * u);
                    p.quadTo(3.5f * u, 19f * u, 3.5f * u, 17f * u);
                    p.close();
                    canvas.drawPath(p, fill);
                    break;
                case MUSIC:
                    canvas.drawCircle(7.6f * u, 17.3f * u, 2.9f * u, fill);
                    canvas.drawCircle(16.4f * u, 15.3f * u, 2.9f * u, fill);
                    canvas.drawLine(10.3f * u, 17f * u, 10.3f * u, 5.5f * u, line);
                    canvas.drawLine(19.1f * u, 15f * u, 19.1f * u, 3.5f * u, line);
                    canvas.drawLine(10.3f * u, 5.5f * u, 19.1f * u, 3.5f * u, line);
                    break;
                case CALCULATOR:
                    r.set(5f * u, 3.5f * u, 19f * u, 20.5f * u);
                    canvas.drawRoundRect(r, 2.5f * u, 2.5f * u, line);
                    r.set(8f * u, 6.5f * u, 16f * u, 9.5f * u);
                    canvas.drawRoundRect(r, 0.8f * u, 0.8f * u, fill);
                    for (int row = 0; row < 2; row++) {
                        for (int col = 0; col < 3; col++) {
                            canvas.drawCircle((8.5f + col * 3.5f) * u, (13f + row * 3.6f) * u, 1.1f * u, fill);
                        }
                    }
                    break;
                case WEATHER:
                    canvas.drawCircle(9f * u, 9f * u, 3f * u, line);
                    p.addCircle(11.2f * u, 15.6f * u, 3.4f * u, Path.Direction.CW);
                    p.addCircle(15.4f * u, 13.8f * u, 4f * u, Path.Direction.CW);
                    p.addCircle(18.6f * u, 16.4f * u, 2.6f * u, Path.Direction.CW);
                    p.addRect(11.2f * u, 15.6f * u, 18.6f * u, 19f * u, Path.Direction.CW);
                    Path cloud = new Path();
                    cloud.op(p, Path.Op.UNION);
                    canvas.drawPath(cloud, fill);
                    break;
                default:
                    break;
            }
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            line.setAlpha(alpha);
            fill.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            line.setColorFilter(filter);
            fill.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
