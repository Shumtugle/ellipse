package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The state of the moment: the time, the day, the charge, and the sky.
 *
 * The first three the phone already knows. The sky is one small request a few
 * times an hour, to an address kept in the package rather than in the source,
 * with fixed coordinates: the place is one the owner named, or where the phone
 * stood when the owner asked it once, and nothing follows the phone about.
 * Every network call of the home screen is here, in this one class.
 */
final class Sky {

    public static final int MISSING = -999;

    public static final int CLEAR = 0;
    public static final int CLOUD = 1;
    public static final int FOG = 2;
    public static final int RAIN = 3;
    public static final int SNOW = 4;
    public static final int STORM = 5;

    private static long asked;
    private static long sniffed;
    private static long counted;
    private static boolean walking;
    private static boolean sniffing;
    private static boolean counting;

    private static int degrees = MISSING;
    private static int feels = MISSING;
    private static int wet = MISSING;
    private static int wind = MISSING;
    private static int whence = MISSING;
    private static int press = MISSING;
    private static int code = -1;
    private static float[] high = new float[0];
    private static float[] low = new float[0];
    private static int[] codes = new int[0];
    private static float[] rain = new float[0];
    private static float[] burn = new float[0];
    private static String dawn = "";
    private static String dusk = "";

    private static float[] soon = new float[0];
    private static float[] flakes = new float[0];
    private static String quarter = "";
    private static float[] hourly = new float[0];
    private static float[] hourCodes = new float[0];
    private static float[] hourWet = new float[0];
    private static String[] hourAt = new String[0];

    private static int air = MISSING;
    private static int dust = MISSING;
    private static float[] pollen = new float[4];
    private static boolean sniffedOnce;

    private static float storm = -1f;
    private static String where = "";

    private Sky() {
    }

    public static String clock() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    /**
     * The date in words, and all of the words come from the dictionary — the
     * day, the month and the order they stand in. A language that puts the
     * month first says so in its own module, not in this file.
     */
    public static String today() {
        return dated(0);
    }

    public static String dated(int ahead) {
        java.util.Calendar when = java.util.Calendar.getInstance();
        when.add(java.util.Calendar.DAY_OF_YEAR, ahead);
        String shape = Words.s("date_form");
        return shape.replace("{w}", weekday(ahead))
            .replace("{d}", String.valueOf(when.get(java.util.Calendar.DAY_OF_MONTH)))
            .replace("{m}", Words.s(MONTHS[when.get(java.util.Calendar.MONTH)]));
    }

    private static final String[] DAYS = {"sunday", "monday", "tuesday", "wednesday",
        "thursday", "friday", "saturday"};

    private static final String[] MONTHS = {"january", "february", "march", "april",
        "may", "june", "july", "august", "september", "october", "november", "december"};

    /** The weekday a given number of days from now, in the chosen language. */
    public static String weekday(int ahead) {
        java.util.Calendar when = java.util.Calendar.getInstance();
        when.add(java.util.Calendar.DAY_OF_YEAR, ahead);
        return Words.s(DAYS[when.get(java.util.Calendar.DAY_OF_WEEK) - 1]);
    }

    /** Percent of charge, or -1 when the phone declines to say. */
    public static int charge(Context context) {
        try {
            Intent state = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (state == null) {
                return -1;
            }
            int level = state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = state.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level < 0 || scale <= 0) {
                return -1;
            }
            return Math.round(level * 100f / scale);
        } catch (Exception broken) {
            return -1;
        }
    }

    public static int degrees() {
        return degrees;
    }

    public static int feels() {
        return feels;
    }

    public static int wet() {
        return wet;
    }

    public static int wind() {
        return wind;
    }

    /** Where the wind comes from, in degrees: 0 is from the north. */
    public static int whence() {
        return whence;
    }

    public static int press() {
        return press;
    }

    /** The same pressure in the older unit, still the one people quote. */
    public static int inMercury() {
        return press == MISSING ? MISSING : Math.round(press * 0.750062f);
    }

    public static int sky() {
        return sorted(code);
    }

    /** The chance of rain over a whole day: it belongs to the day, not to the moment. */
    public static int rain(int day) {
        return day >= 0 && day < rain.length ? Math.round(rain[day]) : MISSING;
    }

    /**
     * The sun as it stands now, not the day's worst. The hours come with the
     * answer, so the one on the clock is found among them and eased toward
     * the next by the minutes gone: numbers kept from an earlier visit stay
     * true to the hour they are read in.
     */
    public static int burn() {
        int at = thisHour();
        if (at < 0 || at >= burn.length) {
            return MISSING;
        }
        float value = burn[at];
        if (at + 1 < burn.length) {
            float gone = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) / 60f;
            value += (burn[at + 1] - value) * gone;
        }
        return Math.round(value);
    }

    /** Where the clock stands among the hourly stamps; the first if none match. */
    private static int thisHour() {
        if (hourAt.length == 0) {
            return -1;
        }
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String face = (hour < 10 ? "0" : "") + hour + ":00";
        for (int i = 0; i < hourAt.length; i++) {
            if (face.equals(hourAt[i])) {
                return i;
            }
        }
        return 0;
    }

    public static String dawn() {
        return dawn;
    }

    public static String dusk() {
        return dusk;
    }

    /** Rain and snow in quarter hours, two hours ahead. */
    public static int steps() {
        return Math.min(soon.length, flakes.length);
    }

    public static float wetAt(int step) {
        return soon[step];
    }

    public static float snowAt(int step) {
        return flakes[step];
    }

    public static String quarterFrom() {
        return quarter;
    }

    /** The clock at the far end of the quarters, so the scale needs no unit. */
    public static String quarterTo() {
        int cut = quarter.indexOf(':');
        if (cut <= 0) {
            return "";
        }
        try {
            int minutes = Integer.parseInt(quarter.substring(0, cut)) * 60
                + Integer.parseInt(quarter.substring(cut + 1)) + steps() * 15;
            int hour = (minutes / 60) % 24;
            int minute = minutes % 60;
            return (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute;
        } catch (NumberFormatException broken) {
            return "";
        }
    }

    public static int hours() {
        return Math.min(hourly.length,
            Math.min(hourCodes.length, Math.min(hourWet.length, hourAt.length)));
    }

    public static int hourWarm(int at) {
        return Math.round(hourly[at]);
    }

    public static int hourSky(int at) {
        return sorted((int) hourCodes[at]);
    }

    public static int hourRain(int at) {
        return Math.round(hourWet[at]);
    }

    public static String hourWhen(int at) {
        return hourAt[at];
    }

    public static int air() {
        return air;
    }

    public static int dust() {
        return dust;
    }

    public static boolean sniffed() {
        return sniffedOnce;
    }

    /** Alder, birch, grass, mugwort — grains in a cubic metre. */
    public static int pollen(int kind) {
        return Math.round(pollen[kind]);
    }

    public static float storm() {
        return storm;
    }

    public static String place() {
        return where;
    }

    /** A new place throws away what was known about the old one. */
    public static void place(String name) {
        if (name == null || name.equals(where)) {
            return;
        }
        where = name;
        degrees = MISSING;
        storm = -1f;
        sniffedOnce = false;
        asked = 0;
        sniffed = 0;
    }

    /** Names of places that answered to a search, with their numbers. */
    public static String[][] found(String answer) {
        java.util.ArrayList<String[]> out = new java.util.ArrayList<String[]>();
        int at = answer.indexOf("{\"id\"");
        while (at >= 0) {
            int end = answer.indexOf("{\"id\"", at + 1);
            String chunk = end > 0 ? answer.substring(at, end) : answer.substring(at);
            String name = word(chunk, "name");
            String lat = pick(chunk, "latitude");
            String lon = pick(chunk, "longitude");
            String land = word(chunk, "country");
            String near = word(chunk, "admin1");
            if (name.length() > 0 && lat != null && lon != null) {
                out.add(new String[] {name, lat, lon,
                    near.length() > 0 ? near + ", " + land : land});
            }
            at = end;
        }
        return out.toArray(new String[out.size()][]);
    }

    private static String word(String text, String key) {
        int at = text.indexOf("\"" + key + "\":\"");
        if (at < 0) {
            return "";
        }
        int open = at + key.length() + 4;
        int shut = text.indexOf('"', open);
        return shut > open ? text.substring(open, shut) : "";
    }

    /** Ask a place by name; the answer is handed back as it came. */
    public static void look(final String where, final Answer then) {
        new Thread(new Runnable() {
            public void run() {
                String answer = "";
                try {
                    answer = fetch(where);
                } catch (Exception broken) {
                    answer = "";
                }
                if (then != null) {
                    then.said(answer);
                }
            }
        }).start();
    }

    public interface Answer {
        void said(String answer);
    }

    public static int days() {
        return Math.min(codes.length, Math.min(high.length, low.length));
    }

    public static int high(int day) {
        return Math.round(high[day]);
    }

    public static int low(int day) {
        return Math.round(low[day]);
    }

    public static int sky(int day) {
        return sorted(codes[day]);
    }

    /** Six kinds of sky is all a small mark can honestly tell apart. */
    private static int sorted(int what) {
        if (what < 0) {
            return -1;
        }
        if (what <= 1) {
            return CLEAR;
        }
        if (what <= 3) {
            return CLOUD;
        }
        if (what >= 45 && what <= 48) {
            return FOG;
        }
        if (what >= 71 && what <= 77 || what == 85 || what == 86) {
            return SNOW;
        }
        if (what >= 95) {
            return STORM;
        }
        return RAIN;
    }

    /** Yesterday's numbers beat an empty page, so they stand until asked again. */
    public static void ask(final String where, final Runnable then) {
        if (where == null || where.length() == 0 || walking) {
            return;
        }
        long now = System.currentTimeMillis();
        if (degrees != MISSING && now - asked < 20 * 60 * 1000L) {
            return;
        }
        walking = true;
        new Thread(new Runnable() {
            public void run() {
                HttpURLConnection line = null;
                try {
                    line = (HttpURLConnection) new URL(where).openConnection();
                    line.setConnectTimeout(6000);
                    line.setReadTimeout(6000);
                    InputStream from = line.getInputStream();
                    StringBuilder said = new StringBuilder();
                    byte[] chunk = new byte[2048];
                    int read = from.read(chunk);
                    while (read > 0) {
                        said.append(new String(chunk, 0, read, "UTF-8"));
                        read = from.read(chunk);
                    }
                    from.close();
                    sift(said.toString());
                } catch (Exception broken) {
                    // no answer this time; the old numbers stand
                } finally {
                    if (line != null) {
                        line.disconnect();
                    }
                    walking = false;
                    if (then != null) {
                        then.run();
                    }
                }
            }
        }).start();
    }

    private static void sift(String answer) {
        int found = round(pick(answer, "temperature_2m"));
        if (found != MISSING) {
            degrees = found;
            asked = System.currentTimeMillis();
        }
        int mark = round(pick(answer, "weather_code"));
        if (mark != MISSING) {
            code = mark;
        }
        feels = round(pick(answer, "apparent_temperature"));
        wet = round(pick(answer, "relative_humidity_2m"));
        wind = round(pick(answer, "wind_speed_10m"));
        whence = round(pick(answer, "wind_direction_10m"));
        press = round(pick(answer, "surface_pressure"));
        high = row(answer, "temperature_2m_max");
        low = row(answer, "temperature_2m_min");
        /* The hours carry a list under the same key and stand first, so the
           days are looked for only past the start of their own block. */
        int days = answer.indexOf("\"daily\":");
        float[] marks = days < 0 ? new float[0] : row(answer.substring(days), "weather_code");
        int[] kept = new int[marks.length];
        for (int i = 0; i < marks.length; i++) {
            kept[i] = (int) marks[i];
        }
        codes = kept;
        soon = row(answer, "precipitation");
        flakes = row(answer, "snowfall");
        quarter = hour(answer, "time");
        hourly = row(answer, "temperature_2m");
        hourCodes = row(answer, "weather_code");
        hourWet = row(answer, "precipitation_probability");
        hourAt = clock(answer, "time", 1);
        rain = row(answer, "precipitation_probability_max");
        burn = row(answer, "uv_index");
        dawn = hour(answer, "sunrise");
        dusk = hour(answer, "sunset");
    }

    /** Every clock face in the second list of stamps behind this key. */
    private static String[] clock(String text, String key, int skip) {
        String mark = "\"" + key + "\":[";
        int at = text.indexOf(mark);
        for (int i = 0; i < skip && at >= 0; i++) {
            at = text.indexOf(mark, at + 1);
        }
        if (at < 0) {
            return new String[0];
        }
        int open = text.indexOf('[', at);
        int shut = text.indexOf(']', open);
        if (open < 0 || shut < 0) {
            return new String[0];
        }
        String[] parts = text.substring(open + 1, shut).split(",");
        String[] out = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String stamp = parts[i].trim().replace("\"", "");
            int split = stamp.indexOf('T');
            out[i] = split > 0 && stamp.length() >= split + 6
                ? stamp.substring(split + 1, split + 6) : stamp;
        }
        return out;
    }

    /** The clock part of the first stamp behind a key: the day is already known. */
    private static String hour(String text, String key) {
        int at = text.indexOf("\"" + key + "\":[\"");
        if (at < 0) {
            return "";
        }
        int open = text.indexOf('"', text.indexOf('[', at));
        int shut = text.indexOf('"', open + 1);
        if (open < 0 || shut < 0) {
            return "";
        }
        String stamp = text.substring(open + 1, shut);
        int split = stamp.indexOf('T');
        return split > 0 && stamp.length() >= split + 6
            ? stamp.substring(split + 1, split + 6) : stamp;
    }

    private static int round(String found) {
        try {
            return Math.round(Float.parseFloat(found));
        } catch (Exception broken) {
            return MISSING;
        }
    }

    /**
     * The first numeric value behind a key. The answer opens with a block of
     * units, where the same key carries a string, so an occurrence whose value
     * is not a number is stepped over rather than trusted.
     */
    private static String pick(String text, String key) {
        String mark = "\"" + key + "\":";
        int at = text.indexOf(mark);
        while (at >= 0) {
            int colon = at + mark.length() - 1;
            int from = colon + 1;
            while (from < text.length() && text.charAt(from) == ' ') {
                from++;
            }
            int end = from;
            while (end < text.length() && "-0123456789.".indexOf(text.charAt(end)) >= 0) {
                end++;
            }
            if (end > from) {
                return text.substring(from, end);
            }
            at = text.indexOf(mark, at + 1);
        }
        return null;
    }

    /** The first character that is not a space after a position. */
    private static char after(String text, int at) {
        int here = at + 1;
        while (here < text.length() && text.charAt(here) == ' ') {
            here++;
        }
        return here < text.length() ? text.charAt(here) : ' ';
    }

    /** What the air carries: the index, the dust, and four kinds of pollen. */
    public static void sniff(final String where, final Runnable then) {
        if (where == null || where.length() == 0 || sniffing) {
            return;
        }
        if (sniffedOnce && System.currentTimeMillis() - sniffed < 40 * 60 * 1000L) {
            return;
        }
        sniffing = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    String answer = fetch(where);
                    air = round(pick(answer, "european_aqi"));
                    dust = round(pick(answer, "pm2_5"));
                    pollen[0] = one(answer, "alder_pollen");
                    pollen[1] = one(answer, "birch_pollen");
                    pollen[2] = one(answer, "grass_pollen");
                    pollen[3] = one(answer, "mugwort_pollen");
                    sniffed = System.currentTimeMillis();
                    sniffedOnce = true;
                } catch (Exception broken) {
                    // the old numbers stand
                } finally {
                    sniffing = false;
                    if (then != null) {
                        then.run();
                    }
                }
            }
        }).start();
    }

    /**
     * The planetary index of magnetic disturbance, three hours at a time. The
     * answer is a table of rows rather than named fields, so the last row is
     * taken and its second column read.
     */
    public static void count(final String where, final Runnable then) {
        if (where == null || where.length() == 0 || counting) {
            return;
        }
        if (storm >= 0f && System.currentTimeMillis() - counted < 60 * 60 * 1000L) {
            return;
        }
        counting = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    String answer = fetch(where);
                    int last = answer.lastIndexOf("[\"");
                    if (last > 0) {
                        String row = answer.substring(last);
                        String[] parts = row.split(",");
                        if (parts.length > 1) {
                            storm = Float.parseFloat(parts[1].replace("\"", "").trim());
                            counted = System.currentTimeMillis();
                        }
                    }
                } catch (Exception broken) {
                    // the old number stands
                } finally {
                    counting = false;
                    if (then != null) {
                        then.run();
                    }
                }
            }
        }).start();
    }

    private static float one(String text, String key) {
        String found = pick(text, key);
        try {
            return Float.parseFloat(found);
        } catch (Exception broken) {
            return -1f;
        }
    }

    private static String fetch(String where) throws Exception {
        HttpURLConnection line = (HttpURLConnection) new URL(where).openConnection();
        try {
            line.setConnectTimeout(6000);
            line.setReadTimeout(6000);
            InputStream from = line.getInputStream();
            StringBuilder said = new StringBuilder();
            byte[] chunk = new byte[4096];
            int read = from.read(chunk);
            while (read > 0) {
                said.append(new String(chunk, 0, read, "UTF-8"));
                read = from.read(chunk);
            }
            from.close();
            return said.toString();
        } finally {
            line.disconnect();
        }
    }

    private static float[] row(String text, String key) {
        int at = text.indexOf("\"" + key + "\":");
        while (at >= 0 && after(text, text.indexOf(':', at)) != '[') {
            at = text.indexOf("\"" + key + "\":", at + 1);
        }
        if (at < 0) {
            return new float[0];
        }
        int open = text.indexOf('[', at);
        int shut = text.indexOf(']', open);
        if (open < 0 || shut < 0) {
            return new float[0];
        }
        String[] parts = text.substring(open + 1, shut).split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Float.parseFloat(parts[i].trim());
            } catch (Exception broken) {
                out[i] = 0f;
            }
        }
        return out;
    }
}
