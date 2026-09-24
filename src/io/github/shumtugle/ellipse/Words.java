package io.github.shumtugle.ellipse;

import java.util.HashMap;
import java.util.Map;

/**
 * The string table of the pages the home screen draws as pages: the colour
 * page and the weather. English lives here, keyed by a stable id; every
 * other tongue is to live outside the source, in a module, and a missing
 * key falls back to English silently.
 */
final class Words {

    static final String[] IDS = {
        "look",
        "accent",
        "hue",
        "richness",
        "brightness",
        "solidity",
        "ground",
        "text_size",
        "sample",
        "weather",
        "feels",
        "wind",
        "wet",
        "sun",
        "uv_low",
        "uv_mid",
        "uv_high",
        "press",
        "light",
        "air",
        "aurora",
        "place",
        "find",
        "standing",
        "here",
        "no_pollen",
        "alder",
        "birch",
        "grass",
        "mugwort",
        "dry",
        "raining",
        "snowing",
        "rain_in",
        "snow_in",
        "maybe",
        "quiet",
        "today",
        "none",
        "min",
        "geomagnetic",
        "sunday",
        "monday",
        "tuesday",
        "wednesday",
        "thursday",
        "friday",
        "saturday",
        "january",
        "february",
        "march",
        "april",
        "may",
        "june",
        "july",
        "august",
        "september",
        "october",
        "november",
        "december",
        "date_form",
        "colour_text",
        "weather_place",
        "by_system",
        "by_wall",
        "by_hand"
    };

    private static final String[] EN = {
        "look",
        "the accent",
        "hue",
        "richness",
        "brightness",
        "how solid the cards are",
        "the ground",
        "text size",
        "the quick brown fox jumps over the lazy dog",
        "weather",
        "feels",
        "wind",
        "wet",
        "sunburn",
        "easy",
        "take care",
        "hide",
        "pressure",
        "light",
        "air",
        "aurora",
        "place",
        "find",
        "where I stand",
        "here",
        "nothing in the air today",
        "alder",
        "birch",
        "grass",
        "mugwort",
        "dry",
        "raining",
        "snowing",
        "rain in",
        "snow in",
        "maybe",
        "quiet",
        "today",
        "none yet",
        "min",
        "geomagnetic activity",
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
        "{w}, {d} {m}",
        "colour and text",
        "the weather's place",
        "by the system",
        "by the wallpaper",
        "my own"
    };

    private static final Map<String, String> table = new HashMap<>();

    static {
        for (int i = 0; i < IDS.length; i++) {
            table.put(IDS[i], EN[i]);
        }
    }

    private Words() {
    }

    /** The words for a key, or the key itself when there are none. */
    static String s(String key) {
        String said = table.get(key);
        return said == null ? key : said;
    }
}
