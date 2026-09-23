package io.github.shumtugle.ellipse;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Names compared the forgiving way: case, the two spellings of one letter,
 * accents and punctuation set aside. A name is found by the start of any
 * of its words, and a few letters typed together find it too when they
 * stand anywhere inside it.
 */
final class Match {

    /** How well a name answers: no answer at all, then weakest to strongest. */
    static final int NONE = 0;
    static final int INSIDE = 1;
    static final int WORD = 2;
    static final int START = 3;

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    private Match() {
    }

    static String norm(String text) {
        if (text == null) {
            return "";
        }
        String t = text.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435');
        t = MARKS.matcher(Normalizer.normalize(t, Normalizer.Form.NFD)).replaceAll("");
        return NOT_WORD.matcher(t).replaceAll(" ").trim();
    }

    /**
     * How a name answers what was typed. Every typed word must open some
     * word of the name; failing that, the typed letters, spaces aside, must
     * stand somewhere in the name.
     */
    static int rank(String name, String typed) {
        if (typed.length() == 0) {
            return START;
        }
        if (name.startsWith(typed)) {
            return START;
        }
        String[] parts = name.split(" ");
        String[] words = typed.split(" ");
        boolean all = true;
        for (int i = 0; i < words.length && all; i++) {
            boolean found = false;
            for (int k = 0; k < parts.length && !found; k++) {
                found = parts[k].startsWith(words[i]);
            }
            all = found;
        }
        if (all) {
            return WORD;
        }
        if (name.replace(" ", "").contains(typed.replace(" ", ""))) {
            return INSIDE;
        }
        return NONE;
    }
}
