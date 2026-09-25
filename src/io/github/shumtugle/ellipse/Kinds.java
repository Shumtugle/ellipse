package io.github.shumtugle.ellipse;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The kinds the list of every app is sorted into, under tabs. None; by
 * hand, where the owner names the kinds and puts each app in one; or by
 * themselves, where each app goes where it says it belongs, as the
 * platform lets it say. Put in a kind by hand, an app stays there either
 * way; an app that belongs nowhere stands under Other.
 */
final class Kinds {

    static final int NONE = 0;
    static final int BY_HAND = 1;
    static final int BY_THEMSELVES = 2;
    static final String ALL = "All";
    static final String OTHER = "Other";

    private static final String OWN = "kind.";
    private static final String NAMES = "kind_names";

    /** The platform's own kinds, in the order their tabs stand. */
    private static final int[] SAID = {ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_NEWS,
        ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_IMAGE,
        ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_GAME,
        ApplicationInfo.CATEGORY_ACCESSIBILITY};
    private static final String[] SAID_NAMES = {"Social", "News", "Work", "Maps", "Pictures", "Video", "Audio",
        "Games", "Access"};

    private Kinds() {
    }

    static int mode(Context context) {
        int mode = Keep.number(context, Keep.KINDS, NONE);
        return mode < NONE || mode > BY_THEMSELVES ? NONE : mode;
    }

    /** The kind an app stands under: put there by hand, or as it says, or Other. */
    static String of(Context context, Apps.Door door) {
        String own = Keep.word(context, OWN + door.token());
        if (own != null && !own.isEmpty()) {
            return own;
        }
        if (mode(context) == BY_THEMSELVES) {
            int said = door.kind();
            for (int i = 0; i < SAID.length; i++) {
                if (SAID[i] == said) {
                    return SAID_NAMES[i];
                }
            }
        }
        return OTHER;
    }

    /** An app put in a kind by hand; a kind not known yet is added to the owner's own. */
    static void put(Context context, Apps.Door door, String kind) {
        Keep.saveWord(context, OWN + door.token(), kind);
        List<String> named = named(context);
        if (!named.contains(kind) && !kind.equals(OTHER)) {
            named.add(kind);
            Keep.saveWord(context, NAMES, join(named));
        }
    }

    /** The kinds the owner named, in the order named. */
    static List<String> named(Context context) {
        List<String> names = new ArrayList<>();
        String kept = Keep.word(context, NAMES);
        if (kept != null) {
            for (String one : kept.split("\n")) {
                if (!one.trim().isEmpty() && !names.contains(one.trim())) {
                    names.add(one.trim());
                }
            }
        }
        return names;
    }

    /** Every kind an app may be put in, for the menu: the owner's, and, by themselves, the platform's too. */
    static List<String> choices(Context context) {
        Set<String> all = new LinkedHashSet<>(named(context));
        if (mode(context) == BY_THEMSELVES) {
            for (String one : SAID_NAMES) {
                all.add(one);
            }
        }
        all.add(OTHER);
        return new ArrayList<>(all);
    }

    /** The tabs of a list: All, then every kind with an app in it, Other last. */
    static List<String> tabs(Context context, List<Apps.Door> every) {
        Set<String> used = new LinkedHashSet<>();
        for (Apps.Door door : every) {
            used.add(of(context, door));
        }
        List<String> tabs = new ArrayList<>();
        tabs.add(ALL);
        List<String> order = new ArrayList<>(named(context));
        if (mode(context) == BY_THEMSELVES) {
            for (String one : SAID_NAMES) {
                if (!order.contains(one)) {
                    order.add(one);
                }
            }
        }
        for (String one : order) {
            if (used.contains(one)) {
                tabs.add(one);
            }
        }
        for (String one : used) {
            if (!tabs.contains(one) && !one.equals(OTHER)) {
                tabs.add(one);
            }
        }
        if (used.contains(OTHER)) {
            tabs.add(OTHER);
        }
        return tabs;
    }

    private static String join(List<String> names) {
        StringBuilder out = new StringBuilder();
        for (String one : names) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(one);
        }
        return out.toString();
    }
}
