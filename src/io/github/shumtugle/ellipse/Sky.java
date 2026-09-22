package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The weather over one city.
 *
 * The weather is for one of two places, as the owner chooses: a city
 * chosen once by name, or wherever the phone is. Wherever the phone is,
 * a rough place is enough, and only that is asked for, with the owner's
 * leave: the weather of a town does not need the street. The place is
 * asked for when the home screen is looked at, and a phone that has gone
 * further than a day's walk from where the weather was last asked for
 * asks again at once, so that a holiday brings its own weather without a
 * city being looked up by hand.
 *
 * The weather is asked for when the home screen is looked at and what it
 * knows is more than half an hour old, never in the background, and kept:
 * a home screen without a network shows the last weather it had.
 *
 * Every network call of this home screen is here, in this one class: the
 * name of a city to find, and the weather of a place to know. Nothing else
 * leaves the phone.
 *
 * The weather is drawn, not shown as pictures: a sun or a moon, a cloud,
 * rain, snow, mist, a bolt, from the one number the weather service gives
 * for what the sky is doing.
 */
final class Sky {

    /**
     * Where the weather and the places come from, as their licence asks to
     * be said wherever they are shown, with a way to the licence itself.
     */
    static final String WEATHER_SOURCE = "https://open-meteo.com/";
    static final String PLACES_SOURCE = "https://www.geonames.org/";
    static final String LICENCE = "https://creativecommons.org/licenses/by/4.0/";

    static final String SPACE_SOURCE = "https://www.swpc.noaa.gov/";

    private static final String FIND = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String NOW = "https://api.open-meteo.com/v1/forecast";
    private static final String AIR = "https://air-quality-api.open-meteo.com/v1/air-quality";
    private static final String SPACE = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json";
    private static final String ASKED = "&current=temperature_2m,apparent_temperature,relative_humidity_2m,"
        + "wind_speed_10m,wind_direction_10m,surface_pressure,weather_code,is_day&wind_speed_unit=ms"
        + "&minutely_15=precipitation,snowfall&forecast_minutely_15=8"
        + "&hourly=temperature_2m,weather_code,precipitation_probability,uv_index,is_day&forecast_hours=12"
        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,"
        + "sunrise,sunset&forecast_days=5&timezone=auto";
    private static final String BREATHED = "&current=european_aqi,pm2_5,alder_pollen,birch_pollen,"
        + "grass_pollen,mugwort_pollen&timezone=auto";
    private static final long STALE = 30L * 60L * 1000L;
    /** How far the phone may go before the weather is asked for again: a day's walk. */
    private static final float MOVED = 25000f;

    /** A place found by its name. */
    static final class Place {
        final String name;
        final String region;
        final double lat;
        final double lon;

        Place(String name, String region, double lat, double lon) {
            this.name = name;
            this.region = region;
            this.lat = lat;
            this.lon = lon;
        }
    }

    /** The weather as last known. */
    static final class Now {
        final boolean known;
        final float degrees;
        final int code;
        final boolean day;

        Now(boolean known, float degrees, int code, boolean day) {
            this.known = known;
            this.degrees = degrees;
            this.code = code;
            this.day = day;
        }
    }

    private Sky() {
    }

    private static SharedPreferences store(Context context) {
        return context.getSharedPreferences("sky", Context.MODE_PRIVATE);
    }

    static String city(Context context) {
        return store(context).getString("city", null);
    }

    static void choose(Context context, Place place) {
        store(context).edit().putString("city", place.name)
            .putFloat("lat", (float) place.lat).putFloat("lon", (float) place.lon)
            .putBoolean("here", false).putLong("at", 0L).apply();
    }

    /** Whether the weather follows the phone rather than a chosen city. */
    static boolean here(Context context) {
        return store(context).getBoolean("here", false);
    }

    static void follow(Context context, boolean on) {
        store(context).edit().putBoolean("here", on).putLong("at", 0L).apply();
    }

    /** Whether this home screen may know roughly where the phone is. */
    static boolean mayLocate(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** Forgets when the weather was last asked for, so the next look asks again. */
    static void forget(Context context) {
        store(context).edit().putLong("at", 1L).apply();
    }

    static Now now(Context context) {
        SharedPreferences kept = store(context);
        return new Now(kept.getLong("at", 0L) > 0L, kept.getFloat("degrees", 0f),
            kept.getInt("code", 0), kept.getBoolean("day", true));
    }

    /**
     * Asks for the weather if what is known is old, on a thread of its own,
     * and says when there is something new. Nothing happens without a city.
     */
    static void freshen(final Context context, final Runnable then) {
        final SharedPreferences kept = store(context);
        if (kept.getBoolean("here", false) && mayLocate(context)) {
            locate(context, new Found() {
                public void found(android.location.Location place) {
                    if (place != null) {
                        float[] apart = new float[1];
                        android.location.Location.distanceBetween(kept.getFloat("lat", 0f), kept.getFloat("lon", 0f),
                            place.getLatitude(), place.getLongitude(), apart);
                        boolean moved = apart[0] > MOVED || kept.getString("city", null) == null;
                        kept.edit().putFloat("lat", (float) place.getLatitude())
                            .putFloat("lon", (float) place.getLongitude()).apply();
                        if (moved) {
                            kept.edit().putLong("at", 0L).apply();
                            name(context, place.getLatitude(), place.getLongitude());
                        }
                    }
                    ask(context, then);
                }
            });
            return;
        }
        ask(context, then);
    }

    /** The weather, if what is known is old, for the place last kept. */
    private static void ask(final Context context, final Runnable then) {
        final SharedPreferences kept = store(context);
        boolean somewhere = kept.getString("city", null) != null
            || (kept.getBoolean("here", false) && kept.contains("lat"));
        if (!somewhere || System.currentTimeMillis() - kept.getLong("at", 0L) < STALE) {
            return;
        }
        final double lat = kept.getFloat("lat", 0f);
        final double lon = kept.getFloat("lon", 0f);
        new Thread(new Runnable() {
            public void run() {
                String where = "?latitude=" + lat + "&longitude=" + lon;
                try {
                    String whole = get(NOW + where + ASKED);
                    JSONObject current = new JSONObject(whole).getJSONObject("current");
                    kept.edit().putFloat("degrees", (float) current.getDouble("temperature_2m"))
                        .putInt("code", current.getInt("weather_code"))
                        .putBoolean("day", current.optInt("is_day", 1) == 1)
                        .putString("forecast", whole)
                        .putLong("at", System.currentTimeMillis()).apply();
                } catch (Exception unreachable) {
                    // No network, or no answer: the last weather stays.
                }
                try {
                    kept.edit().putString("air", get(AIR + where + BREATHED)).apply();
                } catch (Exception unreachable) {
                    // The air is not known everywhere; what was known stays.
                }
                try {
                    kept.edit().putFloat("kp", lastIndex(get(SPACE))).apply();
                } catch (Exception unreachable) {
                    // The magnetic sky is a nicety; without it, nothing is said of it.
                }
                if (then != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(then);
                }
            }
        }).start();
    }

    /**
     * The last reading of the planetary index. The answer is a table, row
     * after row; its rows have been lists of words and have been objects,
     * so both are read.
     */
    private static float lastIndex(String answer) throws Exception {
        JSONArray rows = new JSONArray(answer);
        for (int i = rows.length() - 1; i >= 0; i--) {
            Object row = rows.get(i);
            if (row instanceof JSONArray) {
                JSONArray cells = (JSONArray) row;
                if (cells.length() > 1) {
                    try {
                        return Float.parseFloat(cells.getString(1).trim());
                    } catch (NumberFormatException header) {
                        continue;
                    }
                }
            } else if (row instanceof JSONObject) {
                JSONObject one = (JSONObject) row;
                double kp = one.optDouble("Kp", one.optDouble("kp_index", one.optDouble("kp", Double.NaN)));
                if (!Double.isNaN(kp)) {
                    return (float) kp;
                }
            }
        }
        return -1f;
    }

    // ------------------------------------------------------------ the whole weather

    /** Everything known of the weather, read once from what was kept. */
    static final class Whole {
        boolean known;
        String place = "";
        float degrees;
        float feels = Float.NaN;
        int humidity = -1;
        float wind = Float.NaN;
        int whence = -1;
        float pressure = Float.NaN;
        int code;
        boolean day = true;
        float uv = Float.NaN;
        String[] quarters = new String[0];
        float[] water = new float[0];
        float[] snow = new float[0];
        String[] hours = new String[0];
        float[] hourWarm = new float[0];
        int[] hourCode = new int[0];
        int[] hourRain = new int[0];
        boolean[] hourDay = new boolean[0];
        String[] dates = new String[0];
        int[] dayCode = new int[0];
        float[] high = new float[0];
        float[] low = new float[0];
        int[] dayRain = new int[0];
        String dawn = "";
        String dusk = "";
        int air = -1;
        int dust = -1;
        /** Alder, birch, grass and mugwort, grains in a cubic metre; below zero when not known. */
        int[] pollen = {-1, -1, -1, -1};
        float kp = -1f;
    }

    static Whole whole(Context context) {
        SharedPreferences kept = store(context);
        Whole w = new Whole();
        w.place = kept.getString("city", "");
        w.kp = kept.getFloat("kp", -1f);
        try {
            JSONObject all = new JSONObject(kept.getString("forecast", "{}"));
            JSONObject now = all.optJSONObject("current");
            if (now != null) {
                w.known = true;
                w.degrees = (float) now.optDouble("temperature_2m", 0);
                w.feels = (float) now.optDouble("apparent_temperature", Double.NaN);
                w.humidity = now.has("relative_humidity_2m") ? now.optInt("relative_humidity_2m", -1) : -1;
                w.wind = (float) now.optDouble("wind_speed_10m", Double.NaN);
                w.whence = now.has("wind_direction_10m") ? now.optInt("wind_direction_10m", -1) : -1;
                w.pressure = (float) now.optDouble("surface_pressure", Double.NaN);
                w.code = now.optInt("weather_code", 3);
                w.day = now.optInt("is_day", 1) == 1;
            }
            JSONObject soon = all.optJSONObject("minutely_15");
            if (soon != null) {
                w.quarters = clocks(soon.optJSONArray("time"));
                w.water = numbers(soon.optJSONArray("precipitation"));
                w.snow = numbers(soon.optJSONArray("snowfall"));
            }
            JSONObject hourly = all.optJSONObject("hourly");
            if (hourly != null) {
                w.hours = clocks(hourly.optJSONArray("time"));
                w.hourWarm = numbers(hourly.optJSONArray("temperature_2m"));
                w.hourCode = whole(hourly.optJSONArray("weather_code"));
                w.hourRain = whole(hourly.optJSONArray("precipitation_probability"));
                int[] light = whole(hourly.optJSONArray("is_day"));
                w.hourDay = new boolean[light.length];
                for (int i = 0; i < light.length; i++) {
                    w.hourDay[i] = light[i] != 0;
                }
                float[] burn = numbers(hourly.optJSONArray("uv_index"));
                if (burn.length > 0) {
                    w.uv = burn[0];
                }
            }
            JSONObject daily = all.optJSONObject("daily");
            if (daily != null) {
                JSONArray dates = daily.optJSONArray("time");
                w.dates = new String[dates == null ? 0 : dates.length()];
                for (int i = 0; i < w.dates.length; i++) {
                    w.dates[i] = dates.optString(i, "");
                }
                w.dayCode = whole(daily.optJSONArray("weather_code"));
                w.high = numbers(daily.optJSONArray("temperature_2m_max"));
                w.low = numbers(daily.optJSONArray("temperature_2m_min"));
                w.dayRain = whole(daily.optJSONArray("precipitation_probability_max"));
                String[] rise = clocks(daily.optJSONArray("sunrise"));
                String[] set = clocks(daily.optJSONArray("sunset"));
                w.dawn = rise.length > 0 ? rise[0] : "";
                w.dusk = set.length > 0 ? set[0] : "";
            }
        } catch (Exception unreadable) {
            w.known = false;
        }
        try {
            JSONObject air = new JSONObject(kept.getString("air", "{}")).optJSONObject("current");
            if (air != null) {
                w.air = number(air, "european_aqi");
                w.dust = number(air, "pm2_5");
                String[] kinds = {"alder_pollen", "birch_pollen", "grass_pollen", "mugwort_pollen"};
                for (int i = 0; i < kinds.length; i++) {
                    w.pollen[i] = number(air, kinds[i]);
                }
            }
        } catch (Exception unreadable) {
            // The air stays unknown.
        }
        return w;
    }

    private static int number(JSONObject from, String key) {
        if (!from.has(key) || from.isNull(key)) {
            return -1;
        }
        double value = from.optDouble(key, Double.NaN);
        return Double.isNaN(value) ? -1 : (int) Math.round(value);
    }

    private static float[] numbers(JSONArray list) {
        if (list == null) {
            return new float[0];
        }
        float[] out = new float[list.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.isNull(i) ? 0f : (float) list.optDouble(i, 0);
        }
        return out;
    }

    private static int[] whole(JSONArray list) {
        if (list == null) {
            return new int[0];
        }
        int[] out = new int[list.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.isNull(i) ? -1 : (int) Math.round(list.optDouble(i, -1));
        }
        return out;
    }

    /** The clock part of each stamp: the day is known from where it stands. */
    private static String[] clocks(JSONArray list) {
        if (list == null) {
            return new String[0];
        }
        String[] out = new String[list.length()];
        for (int i = 0; i < out.length; i++) {
            String stamp = list.optString(i, "");
            int split = stamp.indexOf('T');
            out[i] = split > 0 && stamp.length() >= split + 6 ? stamp.substring(split + 1, split + 6) : stamp;
        }
        return out;
    }

    /** Where the phone is, told once. */
    interface Found {
        void found(android.location.Location place);
    }

    /**
     * Roughly where the phone is: the last place the system knows if it is
     * less than an hour old, else a new one, asked for once. A phone that
     * cannot tell says nothing.
     */
    @SuppressWarnings({"deprecation", "MissingPermission"})
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
        } else if (places.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            provider = android.location.LocationManager.GPS_PROVIDER;
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

    /**
     * The name of the town a place is in, as the phone's own geocoder tells
     * it, kept to be shown; a phone without one shows the word for here.
     */
    @SuppressWarnings("deprecation")
    private static void name(final Context context, final double lat, final double lon) {
        new Thread(new Runnable() {
            public void run() {
                String called = null;
                try {
                    if (android.location.Geocoder.isPresent()) {
                        List<android.location.Address> found = new android.location.Geocoder(context,
                            Locale.getDefault()).getFromLocation(lat, lon, 1);
                        if (found != null && !found.isEmpty()) {
                            android.location.Address one = found.get(0);
                            called = one.getLocality() != null ? one.getLocality()
                                : one.getSubAdminArea() != null ? one.getSubAdminArea() : one.getAdminArea();
                        }
                    }
                } catch (Exception unknown) {
                    called = null;
                }
                store(context).edit().putString("city", called != null ? called : Words.s("here")).apply();
            }
        }).start();
    }

    /** Places that answer to a name, in the phone's language. Called off the main thread. */
    static List<Place> find(String name) throws Exception {
        String url = FIND + "?count=8&language=" + Locale.getDefault().getLanguage()
            + "&name=" + URLEncoder.encode(name, "UTF-8");
        JSONArray found = new JSONObject(get(url)).optJSONArray("results");
        List<Place> places = new ArrayList<Place>();
        if (found == null) {
            return places;
        }
        for (int i = 0; i < found.length(); i++) {
            JSONObject one = found.getJSONObject(i);
            String region = one.optString("admin1", "");
            String country = one.optString("country", "");
            places.add(new Place(one.getString("name"),
                region.length() > 0 && country.length() > 0 ? region + ", " + country : region + country,
                one.getDouble("latitude"), one.getDouble("longitude")));
        }
        return places;
    }

    private static String get(String address) throws Exception {
        HttpURLConnection link = (HttpURLConnection) new URL(address).openConnection();
        link.setConnectTimeout(8000);
        link.setReadTimeout(8000);
        try {
            InputStream in = link.getInputStream();
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                all.write(buffer, 0, n);
            }
            in.close();
            return new String(all.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            link.disconnect();
        }
    }

    // ------------------------------------------------------------ drawing

    /** What the sky is doing, drawn in one ink in a square around a point. */
    static void draw(Canvas canvas, Now now, float cx, float cy, float size, int ink) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ink);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int code = now.known ? now.code : 3;
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
            if (now.day) {
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

    /** Degrees as a whole number with the degree mark; a dash when nothing is known. */
    static String degrees(Now now) {
        return now.known ? Math.round(now.degrees) + "\u00b0" : "\u2013";
    }
}
