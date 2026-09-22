package io.github.shumtugle.ellipse;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Finding an application by a few letters of its name.
 *
 * What is typed is read three ways at once: as it stands; as if it had
 * been typed with the keyboard on the other layout, Cyrillic for Latin and
 * back, since a finger in a hurry often forgets to switch; and spelled in
 * Latin letters, so that Cyrillic names are found by Latin ones. A name is
 * read two ways: as it stands and spelled in Latin letters.
 *
 * A name that begins with what was typed comes first; then one with a
 * word that begins with it; then, once more than one letter is typed, one
 * that holds it anywhere. A single letter finds only beginnings: one
 * letter found anywhere finds nearly everything.
 */
final class Find {

    private static final String LATIN_KEYS = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";
    private static final String CYRILLIC_KEYS = "\u0439\u0446\u0443\u043a\u0435\u043d\u0433\u0448\u0449\u0437\u0445\u044a\u0444\u044b\u0432\u0430\u043f\u0440\u043e\u043b\u0434\u0436\u044d\u044f\u0447\u0441\u043c\u0438\u0442\u044c\u0431\u044e\u0451";

    private static final String[][] SPELLING = {
        {"\u0430", "a"}, {"\u0431", "b"}, {"\u0432", "v"}, {"\u0433", "g"}, {"\u0434", "d"}, {"\u0435", "e"}, {"\u0451", "e"},
        {"\u0436", "zh"}, {"\u0437", "z"}, {"\u0438", "i"}, {"\u0439", "y"}, {"\u043a", "k"}, {"\u043b", "l"}, {"\u043c", "m"},
        {"\u043d", "n"}, {"\u043e", "o"}, {"\u043f", "p"}, {"\u0440", "r"}, {"\u0441", "s"}, {"\u0442", "t"}, {"\u0443", "u"},
        {"\u0444", "f"}, {"\u0445", "h"}, {"\u0446", "ts"}, {"\u0447", "ch"}, {"\u0448", "sh"}, {"\u0449", "sch"}, {"\u044a", ""},
        {"\u044b", "y"}, {"\u044c", ""}, {"\u044d", "e"}, {"\u044e", "yu"}, {"\u044f", "ya"},
    };

    private static final int BEGINS = 3;
    private static final int WORD_BEGINS = 2;
    private static final int HOLDS = 1;

    private Find() {
    }

    /** The applications that answer to what was typed, the best first, then by name. */
    static List<App> rank(List<App> all, String typed) {
        String query = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
        if (query.length() == 0) {
            return all;
        }
        String[] asked = {query, swap(query), spell(query)};
        final List<App> found = new ArrayList<App>();
        final List<Integer> scores = new ArrayList<Integer>();
        for (App app : all) {
            String name = app.label.toLowerCase(Locale.ROOT);
            String[] named = {name, spell(name)};
            int best = 0;
            for (String a : asked) {
                if (a.length() == 0) {
                    continue;
                }
                for (String n : named) {
                    best = Math.max(best, score(n, a));
                }
            }
            if (best > 0) {
                found.add(app);
                scores.add(best);
            }
        }
        final Collator order = Collator.getInstance();
        order.setStrength(Collator.PRIMARY);
        List<Integer> index = new ArrayList<Integer>();
        for (int i = 0; i < found.size(); i++) {
            index.add(i);
        }
        Collections.sort(index, new Comparator<Integer>() {
            public int compare(Integer one, Integer two) {
                int by = scores.get(two) - scores.get(one);
                return by != 0 ? by : order.compare(found.get(one).label, found.get(two).label);
            }
        });
        List<App> ranked = new ArrayList<App>();
        for (int i : index) {
            ranked.add(found.get(i));
        }
        return ranked;
    }

    private static int score(String name, String asked) {
        if (name.startsWith(asked)) {
            return BEGINS;
        }
        int at = name.indexOf(asked);
        while (at > 0) {
            if (!Character.isLetterOrDigit(name.charAt(at - 1))) {
                return WORD_BEGINS;
            }
            at = name.indexOf(asked, at + 1);
        }
        if (asked.length() > 1 && name.contains(asked)) {
            return HOLDS;
        }
        return 0;
    }

    /** What the same keys would have typed on the other layout. */
    static String swap(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int latin = LATIN_KEYS.indexOf(c);
            int cyrillic = CYRILLIC_KEYS.indexOf(c);
            if (latin >= 0) {
                out.append(CYRILLIC_KEYS.charAt(latin));
            } else if (cyrillic >= 0) {
                out.append(LATIN_KEYS.charAt(cyrillic));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Cyrillic spelled in Latin letters; anything else as it is. */
    static String spell(String text) {
        StringBuilder out = new StringBuilder(text.length() + 4);
        for (int i = 0; i < text.length(); i++) {
            String c = String.valueOf(text.charAt(i));
            String spelled = c;
            for (String[] pair : SPELLING) {
                if (pair[0].equals(c)) {
                    spelled = pair[1];
                    break;
                }
            }
            out.append(spelled);
        }
        return out.toString();
    }
}
