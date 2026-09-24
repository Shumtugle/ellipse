package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * The weather where the phone is, for the clock: the warmth now and what
 * the sky is doing, nothing more for the present.
 *
 * A rough place is enough, and only that is asked for, with the owner's
 * leave: the weather of a town does not need the street. The weather is
 * asked for when the home screen is looked at and what it knows is more
 * than half an hour old, never in the background, and kept: a home screen
 * without a network shows the last weather it had.
 *
 * Every network call of this home screen is here, in this one class.
 * Nothing else leaves the phone.
 *
 * The sky is drawn, not shown as pictures: a sun or a moon, a cloud, rain,
 * snow, mist, a bolt, from the one number the weather service gives for
 * what the sky is doing.
 */
final class Sky {

    /** Where the weather comes from, as its licence asks to be said where it is shown. */
    static final String WEATHER_SOURCE = "https://open-meteo.com/";

    private static final String NOW = "https://api.open-meteo.com/v1/forecast";
    private static final String ASKED = "&current=temperature_2m,weather_code,is_day&timezone=auto";
    private static final long STALE = 30L * 60L * 1000L;

    /** The weather as it was last known. */
    static final class Now {
        final float warmth;
        final int code;
        final boolean day;
        final long at;

        Now(float warmth, int code, boolean day, long at) {
            this.warmth = warmth;
            this.code = code;
            this.day = day;
            this.at = at;
        }

        /** The warmth in whole degrees, with its sign when below nought. */
        String said() {
            return String.format(Locale.ROOT, "%d\u00B0", Math.round(warmth));
        }
    }

    private static boolean asking;

    private Sky() {
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences("sky", Context.MODE_PRIVATE);
    }

    /** The weather last known, or none. */
    static Now now(Context context) {
        SharedPreferences kept = store(context);
        if (!kept.contains("at")) {
            return null;
        }
        return new Now(kept.getFloat("warmth", 0f), kept.getInt("code", 3), kept.getBoolean("day", true),
            kept.getLong("at", 0L));
    }

    /** Whether the phone's rough place may be asked for. */
    static boolean mayLocate(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** Asks for the weather if what is known is old; tells when something new has come. */
    static void freshen(final Context context, final Runnable done) {
        Now known = now(context);
        if (asking || !mayLocate(context)
            || (known != null && System.currentTimeMillis() - known.at < STALE)) {
            return;
        }
        asking = true;
        locate(context, new Found() {
            public void found(final android.location.Location place) {
                if (place == null) {
                    asking = false;
                    return;
                }
                new Thread(new Runnable() {
                    public void run() {
                        boolean got = fetch(context, place.getLatitude(), place.getLongitude());
                        asking = false;
                        if (got && done != null) {
                            new Handler(Looper.getMainLooper()).post(done);
                        }
                    }
                }).start();
            }
        });
    }

    private interface Found {
        void found(android.location.Location place);
    }

    /** The phone's rough place: the latest known, or a fresh one when that is old. */
    private static void locate(Context context, final Found then) {
        final android.location.LocationManager places =
            (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (places == null) {
            then.found(null);
            return;
        }
        android.location.Location best = null;
        try {
            for (String provider : places.getProviders(true)) {
                android.location.Location one = places.getLastKnownLocation(provider);
                if (one != null && (best == null || one.getTime() > best.getTime())) {
                    best = one;
                }
            }
        } catch (SecurityException refused) {
            then.found(null);
            return;
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 60L * 60L * 1000L) {
            then.found(best);
            return;
        }
        final android.location.Location fallback = best;
        String provider = null;
        if (android.os.Build.VERSION.SDK_INT >= 31
            && places.isProviderEnabled(android.location.LocationManager.FUSED_PROVIDER)) {
            provider = android.location.LocationManager.FUSED_PROVIDER;
        } else if (places.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            provider = android.location.LocationManager.NETWORK_PROVIDER;
        }
        if (provider == null || android.os.Build.VERSION.SDK_INT < 30) {
            then.found(fallback);
            return;
        }
        try {
            places.getCurrentLocation(provider, null, context.getMainExecutor(),
                new java.util.function.Consumer<android.location.Location>() {
                    public void accept(android.location.Location found) {
                        then.found(found != null ? found : fallback);
                    }
                });
        } catch (RuntimeException refused) {
            then.found(fallback);
        }
    }

    private static boolean fetch(Context context, double lat, double lon) {
        HttpURLConnection line = null;
        try {
            URL asked = new URL(String.format(Locale.ROOT, "%s?latitude=%.3f&longitude=%.3f%s", NOW, lat, lon, ASKED));
            line = (HttpURLConnection) asked.openConnection();
            line.setConnectTimeout(8000);
            line.setReadTimeout(8000);
            InputStream in = line.getInputStream();
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) > 0) {
                all.write(chunk, 0, n);
            }
            in.close();
            JSONObject current = new JSONObject(all.toString("UTF-8")).getJSONObject("current");
            store(context).edit()
                .putFloat("warmth", (float) current.getDouble("temperature_2m"))
                .putInt("code", current.getInt("weather_code"))
                .putBoolean("day", current.optInt("is_day", 1) == 1)
                .putLong("at", System.currentTimeMillis())
                .apply();
            return true;
        } catch (Exception failed) {
            return false;
        } finally {
            if (line != null) {
                line.disconnect();
            }
        }
    }

    /** The sky drawn in one ink, in a square of the given size about a centre. */
    static void draw(Canvas canvas, Now now, float cx, float cy, float size, int ink) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ink);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int code = now != null ? now.code : 3;
        boolean day = now == null || now.day;
        boolean clear = code == 0 || code == 1;
        boolean part = code == 2 || code == 1;
        boolean fog = code == 45 || code == 48;
        boolean snow = (code >= 71 && code <= 77) || code == 85 || code == 86;
        boolean rain = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
        boolean bolt = code >= 95;
        float s = size;
        if (clear || part) {
            float bx = part ? cx - s * 0.16f : cx;
            float by = part ? cy - s * 0.14f : cy;
            float r = s * (part ? 0.2f : 0.24f);
            if (day) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(s * 0.06f);
                canvas.drawCircle(bx, by, r, paint);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    canvas.drawLine(bx + (float) Math.cos(a) * r * 1.45f, by + (float) Math.sin(a) * r * 1.45f,
                        bx + (float) Math.cos(a) * r * 1.9f, by + (float) Math.sin(a) * r * 1.9f, paint);
                }
            } else {
                Path moon = new Path();
                moon.addCircle(bx, by, r * 1.2f, Path.Direction.CW);
                Path bite = new Path();
                bite.addCircle(bx + r * 0.6f, by - r * 0.45f, r * 1.05f, Path.Direction.CW);
                moon.op(bite, Path.Op.DIFFERENCE);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(moon, paint);
            }
            if (clear && !part) {
                return;
            }
        }
        if (fog) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(s * 0.07f);
            for (int i = -1; i <= 1; i++) {
                float y = cy + i * s * 0.18f;
                canvas.drawLine(cx - s * 0.34f + (i == 0 ? s * 0.06f : 0f), y,
                    cx + s * 0.34f - (i == 1 ? s * 0.08f : 0f), y, paint);
            }
            return;
        }
        float lift = rain || snow || bolt ? s * 0.1f : 0f;
        cloud(canvas, paint, part ? cx + s * 0.08f : cx, cy - lift + (part ? s * 0.08f : 0f), s * (part ? 0.8f : 1f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.055f);
        float base = cy + s * 0.2f;
        if (rain) {
            for (int i = -1; i <= 1; i++) {
                float x = cx + i * s * 0.18f;
                canvas.drawLine(x, base, x - s * 0.06f, base + s * 0.2f, paint);
            }
        } else if (snow) {
            paint.setStyle(Paint.Style.FILL);
            for (int i = -1; i <= 1; i++) {
                canvas.drawCircle(cx + i * s * 0.18f, base + s * 0.1f + (i == 0 ? s * 0.08f : 0f), s * 0.045f, paint);
            }
        } else if (bolt) {
            Path zig = new Path();
            zig.moveTo(cx + s * 0.04f, base - s * 0.02f);
            zig.lineTo(cx - s * 0.08f, base + s * 0.16f);
            zig.lineTo(cx + s * 0.02f, base + s * 0.16f);
            zig.lineTo(cx - s * 0.06f, base + s * 0.32f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(zig, paint);
        }
    }

    private static void cloud(Canvas canvas, Paint paint, float cx, float cy, float s) {
        Path cloud = new Path();
        cloud.addCircle(cx - s * 0.16f, cy + s * 0.02f, s * 0.15f, Path.Direction.CW);
        cloud.addCircle(cx + s * 0.02f, cy - s * 0.08f, s * 0.2f, Path.Direction.CW);
        cloud.addCircle(cx + s * 0.2f, cy + s * 0.03f, s * 0.14f, Path.Direction.CW);
        cloud.addRoundRect(new RectF(cx - s * 0.31f, cy + s * 0.0f, cx + s * 0.34f, cy + s * 0.17f),
            s * 0.08f, s * 0.08f, Path.Direction.CW);
        cloud.setFillType(Path.FillType.WINDING);
        Path solid = new Path();
        solid.op(cloud, Path.Op.UNION);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.055f);
        canvas.drawPath(solid, paint);
    }
}
