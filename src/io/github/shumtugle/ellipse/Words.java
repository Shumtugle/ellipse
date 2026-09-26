package io.github.shumtugle.ellipse;

import java.util.HashMap;
import java.util.Map;

/**
 * The words of the home screen in the language chosen. English is spoken
 * by the sources themselves; another language is a module, a file of words
 * with one phrase a line — the English as the home screen says it, " = ",
 * and the same in the module's language. Two modules come with the home
 * screen; one more may be brought from a file, made from the English
 * template the settings give away. A phrase the module does not have is
 * said in English, silently.
 *
 * The pages drawn as pages keep their own table below, keyed by a stable
 * id; each of their phrases passes through the module as the rest do.
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
    /** The module read: the English phrase to the module's own. */
    private static final Map<String, String> module = new HashMap<>();
    private static String chosen = "";

    /** The modules that come with the home screen: their code and their own name. */
    static final String[] CODES = {"", "fi", "ru"};
    static final String[] NAMES = {"English", "Suomi", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439"};
    /** The word a module brought from a file is chosen by. */
    static final String BROUGHT = "file";
    static final String BROUGHT_FILE = "lang-brought.txt";

    static {
        for (int i = 0; i < IDS.length; i++) {
            table.put(IDS[i], EN[i]);
        }
    }

    private Words() {
    }

    /** The words for a key of the pages, in the language chosen; or the key itself when there are none. */
    static String s(String key) {
        String said = table.get(key);
        return t(said == null ? key : said);
    }

    /** An English phrase in the language chosen; the phrase itself when the module has none. */
    static String t(String english) {
        if (english == null || module.isEmpty()) {
            return english;
        }
        String said = module.get(english);
        return said == null || said.isEmpty() ? english : said;
    }

    /** The same, for words given as a sequence of characters. */
    static CharSequence t(CharSequence english) {
        return english == null ? null : t(english.toString());
    }

    /**
     * An English phrase with places in it, %1 to %9, in the language chosen,
     * the places filled after it is put into words: the order of the places
     * is the module's own, as its language needs.
     */
    static String f(String english, Object... filled) {
        return fill(t(english), filled);
    }

    /**
     * The same, for a phrase that counts: %1 is the count. A module may give
     * the phrase in forms apart by " | ": two forms are one and more than one;
     * three are the forms of languages that count by the last digits (one,
     * a few, many), as 1, 3 and 5 are told apart.
     */
    static String n(String english, int count, Object... more) {
        String said = t(english);
        String[] forms = said.split(" \\| ");
        if (forms.length == 2) {
            said = forms[count == 1 ? 0 : 1];
        } else if (forms.length >= 3) {
            int last = Math.abs(count) % 10;
            int lastTwo = Math.abs(count) % 100;
            said = last == 1 && lastTwo != 11 ? forms[0]
                : last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14) ? forms[1]
                : forms[2];
        }
        Object[] all = new Object[more.length + 1];
        all[0] = count;
        System.arraycopy(more, 0, all, 1, more.length);
        return fill(said, all);
    }

    private static String fill(String said, Object[] filled) {
        if (said == null) {
            return null;
        }
        for (int i = Math.min(9, filled.length); i >= 1; i--) {
            said = said.replace("%" + i, String.valueOf(filled[i - 1]));
        }
        return said;
    }

    /** The module chosen read, once, whenever the choice changes. */
    static void read(android.content.Context context) {
        String now = Keep.word(context, Keep.LANGUAGE);
        now = now == null ? "" : now;
        if (now.equals(chosen)) {
            return;
        }
        chosen = now;
        module.clear();
        if (now.isEmpty()) {
            return;
        }
        try (java.io.InputStream in = BROUGHT.equals(now)
            ? new java.io.FileInputStream(new java.io.File(context.getFilesDir(), BROUGHT_FILE))
            : context.getAssets().open("lang/" + now + ".txt")) {
            parse(Copy.words(in), module);
        } catch (java.io.IOException gone) {
            module.clear();
        }
    }

    /** The language the words are in, as a locale for dates: the module's, or the phone's own. */
    static java.util.Locale locale() {
        if ("ru".equals(chosen) || "fi".equals(chosen)) {
            return new java.util.Locale(chosen);
        }
        return java.util.Locale.getDefault();
    }

    /** The module read again at the next reading, even if the choice is the same: a new file was brought. */
    static void forget() {
        chosen = "\u0000";
    }

    /** A module's lines read into a table; its name, from its head, returned. */
    static String parse(String text, Map<String, String> into) {
        String name = null;
        for (String line : text.split("\n")) {
            String one = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
            if (one.startsWith("#")) {
                String said = one.substring(1).trim();
                if (said.startsWith("name:")) {
                    name = said.substring(5).trim();
                }
                continue;
            }
            int cut = one.indexOf(" = ");
            if (cut > 0) {
                into.put(one.substring(0, cut), one.substring(cut + 3).replace("\\n", "\n"));
            }
        }
        return name;
    }

    /** The name a module brought from a file gives itself; or none if none was brought. */
    static String broughtName(android.content.Context context) {
        java.io.File file = new java.io.File(context.getFilesDir(), BROUGHT_FILE);
        if (!file.isFile()) {
            return null;
        }
        try {
            String name = parse(Copy.load(file), new HashMap<String, String>());
            return name == null ? "From a file" : name;
        } catch (java.io.IOException gone) {
            return null;
        }
    }
}
