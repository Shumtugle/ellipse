package io.github.shumtugle.ellipse;

/**
 * The pages the home screen draws as pages, in the one make they share: the
 * colour page and the weather, and the page where the weather's place is
 * found. A page is a ground of the surface with a slow glow of the accent
 * moving behind it, a large plain title, cards a step lighter than the
 * ground, full round pills, labels small and faint above what they name.
 * Everything a page can do is a link to this application's own address.
 */
final class Paper {

    private Paper() {
    }

private static String safe(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** The marks of the settings rows, on the same grid as the drawn marks. */
    private static String sign(String kind, boolean quiet) {
        return sign(kind, quiet ? Tone.hex(Tone.faint()) : Tone.hex(Tone.onAccent()));
    }

    /** The same marks in a given ink: on a bare row they are drawn in the accent. */
    private static String sign(String kind, String ink) {
        StringBuilder b = new StringBuilder();
        b.append("<svg width=22 height=22 viewBox='0 0 24 24' fill=none stroke='")
            .append(ink).append("' stroke-width=2 stroke-linecap=round ")
            .append("stroke-linejoin=round>");
        if ("look".equals(kind)) {
            b.append("<path d='M12 3.6C12 3.6 6.2 10.1 6.2 13.7a5.8 5.8 0 0 0 11.6 0")
                .append("C17.8 10.1 12 3.6 12 3.6z'/>");
        } else if ("marks".equals(kind)) {
            b.append("<path d='M7 4h10v16l-5-4.8L7 20z'/>");
        } else if ("trail".equals(kind)) {
            b.append("<circle cx=12 cy=12 r=\"8\"/><path d='M12 7.4v5l3.4 2'/>");
        } else if ("search".equals(kind)) {
            b.append("<circle cx=11 cy=11 r=\"6.6\"/><path d='M15.9 15.9L20.3 20.3'/>");
        } else if ("tongue".equals(kind)) {
            b.append("<circle cx=12 cy=12 r=\"8\"/><path d='M4 12h16'/>")
                .append("<path d='M12 4a13 13 0 0 1 0 16a13 13 0 0 1 0-16'/>");
        } else if ("widget".equals(kind)) {
            b.append("<rect x=4 y=6.5 width=16 height=11 rx=\"3\"/>")
                .append("<circle cx=16.4 cy=14 r=\"1.4\"/>");
        } else if ("inside".equals(kind)) {
            b.append("<path d='M5 4h9l5 5v11H5z'/><path d='M8.5 12h7'/>")
                .append("<path d='M8.5 15.5h4'/>");
        } else if ("bench".equals(kind)) {
            b.append("<path d='M3 13h18'/><path d='M6 13v7'/><path d='M18 13v7'/>")
                .append("<path d='M9 10h6v3H9z'/>");
        } else if ("calm".equals(kind)) {
            b.append("<path d='M4 8h16'/><path d='M4 12h10'/><path d='M4 16h13'/>")
                .append("<path d='M17 6.5L21.5 11'/>");
        } else if ("site".equals(kind)) {
            b.append("<rect x=\"4\" y=\"6\" width=\"16\" height=\"12\" rx=\"2.5\"/>")
                .append("<path d='M4.4 7L12 13.2L19.6 7'/>");
        } else if ("source".equals(kind)) {
            b.append("<path d='M9 7.5L4.5 12L9 16.5'/><path d='M15 7.5L19.5 12L15 16.5'/>")
                .append("<path d='M13 6L11 18'/>");
        } else if ("weather".equals(kind)) {
            b.append("<path d='M7 15h9.5a3.8 3.8 0 0 0 .2-7.6 5.5 5.5 0 0 0-10.4 1.4")
                .append("A3.1 3.1 0 0 0 7 15z'/>");
        } else if ("media".equals(kind)) {
            b.append("<rect x=\"3.5\" y=\"5\" width=\"17\" height=\"14\" rx=\"3\"/>")
                .append("<path d='M10 9.2v5.6l4.6-2.8z'/>");
        } else if ("video".equals(kind)) {
            b.append("<rect x=\"3\" y=\"6.5\" width=\"12.5\" height=\"11\" rx=\"2.5\"/>")
                .append("<path d='M15.5 10.5l5-3v9l-5-3'/>");
        } else if ("audio".equals(kind)) {
            b.append("<circle cx=\"8\" cy=\"17\" r=\"2.6\"/>")
                .append("<circle cx=\"16\" cy=\"15\" r=\"2.6\"/>")
                .append("<path d='M10.6 17V6.5l8-1.8V15'/>");
        } else if ("stream".equals(kind)) {
            b.append("<circle cx=\"12\" cy=\"12\" r=\"1.6\"/>")
                .append("<path d='M8.2 8.2a5.4 5.4 0 0 0 0 7.6'/>")
                .append("<path d='M15.8 8.2a5.4 5.4 0 0 1 0 7.6'/>")
                .append("<path d='M5.2 5.2a9.6 9.6 0 0 0 0 13.6'/>")
                .append("<path d='M18.8 5.2a9.6 9.6 0 0 1 0 13.6'/>");
        } else if ("playlist".equals(kind)) {
            b.append("<path d='M4 6.5h12'/><path d='M4 11h12'/><path d='M4 15.5h6'/>")
                .append("<path d='M14.5 14v6l5-3z'/>");
        } else if ("player".equals(kind)) {
            b.append("<rect x=\"3.5\" y=\"4.5\" width=\"17\" height=\"15\" rx=\"2.5\"/>")
                .append("<path d='M3.5 8.5h17'/><path d='M10.5 11.5v5l4-2.5z'/>");
        } else if ("cookies".equals(kind)) {
            b.append("<circle cx=\"12\" cy=\"12\" r=\"8.5\"/>")
                .append("<circle cx=\"9.5\" cy=\"9.5\" r=\"1\"/>")
                .append("<circle cx=\"14.5\" cy=\"13\" r=\"1\"/>")
                .append("<circle cx=\"10\" cy=\"15\" r=\"1\"/>");
        } else if ("storage".equals(kind)) {
            b.append("<ellipse cx=\"12\" cy=\"6.5\" rx=\"7\" ry=\"2.5\"/>")
                .append("<path d='M5 6.5v11c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5v-11'/>")
                .append("<path d='M5 12c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5'/>");
        } else if ("help".equals(kind)) {
            b.append("<circle cx=\"12\" cy=\"12\" r=\"8.5\"/>")
                .append("<path d='M9.7 9.6a2.4 2.4 0 1 1 3.3 2.2c-.7.3-1 .8-1 1.5v.5'/>")
                .append("<path d='M12 16.6h.01'/>");
        } else if ("home".equals(kind)) {
            b.append("<path d='M4 10.5 12 4l8 6.5'/>")
                .append("<path d='M6.5 9.5V19a1 1 0 0 0 1 1h9a1 1 0 0 0 1-1V9.5'/>")
                .append("<path d='M10 20v-5h4v5'/>");
        } else if ("news".equals(kind)) {
            b.append("<path d='M5 5.5h11v13H6.5A1.5 1.5 0 0 1 5 17z'/>")
                .append("<path d='M16 9h3v8.5a1.5 1.5 0 0 1-3 0'/>")
                .append("<path d='M8 9h5'/><path d='M8 12h5'/><path d='M8 15h3'/>");
        } else if ("carry".equals(kind)) {
            b.append("<path d='M4 8.5h13'/><path d='M13.5 5l3.5 3.5-3.5 3.5'/>")
                .append("<path d='M20 15.5H7'/><path d='M10.5 12L7 15.5l3.5 3.5'/>");
        } else if ("parcel".equals(kind)) {
            b.append("<path d='M12 4v10'/><path d='M8 10.5l4 4 4-4'/>")
                .append("<path d='M4.5 15v3.5a1.5 1.5 0 0 0 1.5 1.5h12a1.5 1.5 0 0 0 1.5-1.5V15'/>");
        } else if ("picture".equals(kind)) {
            b.append("<rect x=\"3.5\" y=\"5\" width=\"17\" height=\"14\" rx=\"2.5\"/>")
                .append("<path d='M5.5 17l4.5-5 3.5 3.5 2-2 3 3.5'/>")
                .append("<circle cx=\"15.5\" cy=\"9.5\" r=\"1.4\"/>");
        } else if ("box".equals(kind)) {
            b.append("<path d='M4 8.5h16v10a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5z'/>")
                .append("<path d='M3.5 5h17v3.5h-17z'/><path d='M10 12h4'/>");
        } else if ("log".equals(kind)) {
            b.append("<path d='M5 7h14'/><path d='M5 12h9'/><path d='M5 17h12'/>");
        } else {
            b.append("<path d='M7 3.6h7l4 4v12.8H7z'/><path d='M14 3.6v4h4'/>");
        }
        return b.append("</svg>").toString();
    }

    /**
     * The sky as a drawn mark: sun, cloud, fog, rain, snow, storm. Two units
     * of stroke on a grid of twenty four, the same rule every drawn mark keeps.
     */
    private static String mark(int sky) {
        if (sky < 0) {
            return "";
        }
        String ink = Tone.hex(Tone.primary());
        StringBuilder b = new StringBuilder();
        b.append("<svg width=26 height=26 viewBox='0 0 24 24' fill=none stroke='")
            .append(ink).append("' stroke-width=2 stroke-linecap=round ")
            .append("stroke-linejoin=round style='vertical-align:-4px;margin-right:8px'>");
        if (sky == 0) {
            b.append("<circle cx=12 cy=12 r=\"4.4\"/>");
            for (int i = 0; i < 8; i++) {
                double turn = Math.PI * i / 4.0;
                double x1 = 12 + Math.cos(turn) * 7.2;
                double y1 = 12 + Math.sin(turn) * 7.2;
                double x2 = 12 + Math.cos(turn) * 9.6;
                double y2 = 12 + Math.sin(turn) * 9.6;
                b.append("<path d='M").append(round(x1)).append(" ").append(round(y1))
                    .append("L").append(round(x2)).append(" ").append(round(y2)).append("'/>");
            }
        } else {
            b.append("<path d='M7.5 16.5h9a3.6 3.6 0 0 0 .2-7.2 5.2 5.2 0 0 0-9.9 1.3 ")
                .append("3 3 0 0 0 .7 5.9z'/>");
            if (sky == 2) {
                b.append("<path d='M5 20h9'/><path d='M8 22.4h8'/>");
            } else if (sky == 3) {
                b.append("<path d='M9 19.4l-1 2.6'/><path d='M13 19.4l-1 2.6'/>")
                    .append("<path d='M17 19.4l-1 2.6'/>");
            } else if (sky == 4) {
                b.append("<path d='M9 20.6h.01'/><path d='M13 22.4h.01'/>")
                    .append("<path d='M16.6 20.6h.01'/>");
            } else if (sky == 5) {
                b.append("<path d='M13.4 18.6l-3.4 2.8h3.4L10 24'/>");
            }
        }
        return b.append("</svg>").toString();
    }

private static String round(double value) {
        return String.valueOf(Math.round(value * 10.0) / 10.0);
    }

    /**
     * The weather, whole, as a page.
     *
     * The page reads from now outward: one card for this moment, then the
     * hours, then the days. A number stands where its time is, so a chance
     * of rain for the whole day sits on the day, not beside the wind.
     */
    static String weather(String sources) {
        StringBuilder b = new StringBuilder();
        b.append(head(Words.s("weather"), sections()));

        if (Sky.degrees() == Sky.MISSING) {
            b.append("<p style='color:").append(Tone.hex(Tone.faint())).append("'>")
                .append(safe(Words.s("none"))).append("</p></body></html>");
            return b.toString();
        }

        int rank = 0;
        b.append(now(rank++));

        if (Sky.hours() > 0) {
            b.append(rise(rank++)).append("<div class=strip>");
            for (int i = 0; i < Sky.hours(); i++) {
                b.append("<span class=hourly><span class=at>")
                    .append(safe(Sky.hourWhen(i))).append("</span>")
                    .append("<span class=hmark>").append(mark(Sky.hourSky(i)))
                    .append("</span><span class=hwarm>").append(Sky.hourWarm(i))
                    .append("&#176;</span><span class=hwet>")
                    .append(Sky.hourRain(i)).append("%</span></span>");
            }
            b.append("</div></div>");
        }

        b.append(rise(rank++));
        for (int i = 0; i < Sky.days(); i++) {
            int wet = Sky.rain(i);
            b.append("<div class=day2><span class=mark>").append(mark(Sky.sky(i)))
                .append("</span><span class=when>")
                .append(i == 0 ? safe(Words.s("today")) : safe(Sky.weekday(i)))
                .append("</span><span class=dwet>")
                .append(wet == Sky.MISSING ? "" : (wet + "%"))
                .append("</span><span class=hot>").append(Sky.high(i))
                .append("&#176;</span><span class=cold>").append(Sky.low(i))
                .append("&#176;</span></div>");
        }
        b.append("</div>");

        b.append("<p class=source>").append(safe(sources)).append("</p>");
        b.append("</body></html>");
        return b.toString();
    }

    /**
     * A page of words the home screen keeps as a fragment of its own:
     * the shell every page shares, its words in paragraphs, and links in
     * the accent that open in the phone's browser.
     */
    static String text(String body) {
        /* The fragment's own heading becomes the page's. */
        String title = "";
        int open = body.indexOf("<h1>");
        int close = body.indexOf("</h1>");
        if (open >= 0 && close > open) {
            title = body.substring(open + 4, close);
            body = body.substring(0, open) + body.substring(close + 5);
        }
        StringBuilder b = new StringBuilder();
        b.append(head(title, "a{color:" + Tone.hex(Tone.primary()) + ";text-decoration:none;border-bottom:1px solid "
            + Tone.rgba(Tone.primary(), .45f) + "}p{margin:0 0 14px}p.lead{font-size:18px;color:"
            + Tone.hex(Tone.onVariant()) + "}b{font-weight:500}"));
        b.append("<div class=part>").append(body).append("</div></body></html>");
        return b.toString();
    }

    /** Each section arrives a beat after the one above it. */
    private static String rise(int rank) {
        return "<div class=part style='animation-delay:" + (rank * 60) + "ms'>";
    }

    /**
     * Everything that is true at this moment, on one card: the sky and the
     * warmth large, the next two hours under them, and the rest as capsules.
     * A capsule takes the accent only when its number asks something of you,
     * so colour on this card is news, and a quiet day is a quiet card.
     */
    private static String now(int rank) {
        StringBuilder b = new StringBuilder();
        b.append(rise(rank)).append("<div class=hero style='background:")
            .append("linear-gradient(155deg,").append(Tone.hex(Tone.lit(0.22f, 0.9f)))
            .append(" 0%,").append(Tone.hex(Tone.container())).append(" 58%)'>");

        b.append("<div class=top><span class=sky>").append(mark(Sky.sky()))
            .append("</span><span class=temp>").append(Sky.degrees())
            .append("&#176;</span><span class=where>")
            .append("<a class=place href='ellipse:place'>").append(safe(Sky.place()))
            .append("</a>");
        if (Sky.feels() != Sky.MISSING) {
            b.append("<span class=feel>").append(safe(Words.s("feels"))).append(" ")
                .append(Sky.feels()).append("&#176;</span>");
        }
        b.append("</span></div>");

        b.append(soon(rank));
        b.append(capsules(rank));
        b.append(grains(rank));
        return b.append("</div></div>").toString();
    }

    /**
     * The next two hours in quarters. A column for each, its height the water
     * falling in that quarter, and a plain sentence over them when it starts.
     * Two dry hours are one sentence and the span it covers: eight empty
     * columns in a row only look like something still loading.
     */
    private static String soon(int rank) {
        int steps = Sky.steps();
        if (steps <= 0) {
            return "";
        }
        float most = 0f;
        int first = -1;
        boolean snow = false;
        for (int i = 0; i < steps; i++) {
            float water = Sky.wetAt(i) + Sky.snowAt(i);
            if (water > most) {
                most = water;
            }
            if (first < 0 && water > 0.02f) {
                first = i;
                snow = Sky.snowAt(i) > Sky.wetAt(i);
            }
        }
        StringBuilder b = new StringBuilder();
        if (first < 0) {
            return b.append("<div class=soon><span class=says>")
                .append(safe(Words.s("dry"))).append("</span><span class=span>")
                .append(safe(Sky.quarterFrom())).append(" \u2013 ")
                .append(safe(Sky.quarterTo())).append("</span></div>").toString();
        }
        b.append("<div class='soon wet'><span class=says>");
        if (first == 0) {
            b.append(safe(snow ? Words.s("snowing") : Words.s("raining")));
        } else {
            b.append(safe(snow ? Words.s("snow_in") : Words.s("rain_in")))
                .append(" ").append(first * 15).append(" ").append(safe(Words.s("min")));
        }
        b.append("</span><span class=bars>");
        for (int i = 0; i < steps; i++) {
            float water = Sky.wetAt(i) + Sky.snowAt(i);
            int tall = most <= 0f ? 4 : Math.max(4, Math.round(water / most * 40f));
            b.append("<span class=bar style='height:").append(tall)
                .append("px;animation-delay:").append(rank * 60 + i * 40).append("ms;")
                .append("background:").append(water > 0.02f
                    ? Tone.hex(Tone.primary()) : Tone.hex(Tone.containerHigh()))
                .append("'></span>");
        }
        b.append("</span><span class=ticks><span>").append(safe(Sky.quarterFrom()))
            .append("</span><span>").append(safe(Sky.quarterTo())).append("</span></span>");
        return b.append("</div>").toString();
    }

    /** The small facts of the moment, each a capsule, in the order they are looked for. */
    private static String capsules(int rank) {
        StringBuilder b = new StringBuilder("<div class=caps>");
        int[] beat = {rank * 60 + 120};

        if (Sky.wind() != Sky.MISSING) {
            b.append(capsule(blow(Sky.whence()) + Sky.wind(), Words.s("wind"),
                Sky.wind() >= 11, beat));
        }
        if (Sky.wet() != Sky.MISSING) {
            b.append(capsule(Sky.wet() + "%", Words.s("wet"), false, beat));
        }
        if (Sky.press() != Sky.MISSING) {
            b.append(capsule(Sky.inMercury() + " \u00B7 " + Sky.press(), Words.s("press"),
                false, beat));
        }
        int burn = Sky.burn();
        if (burn != Sky.MISSING) {
            b.append(capsule(String.valueOf(burn), burn >= 3
                ? Words.s("sun") + " \u00B7 " + burnt(burn) : Words.s("sun"), burn >= 3, beat));
        }
        if (Sky.sniffed()) {
            if (Sky.air() != Sky.MISSING) {
                b.append(capsule(String.valueOf(Sky.air()), Words.s("air"),
                    Sky.air() >= 60, beat));
            }
            if (Sky.dust() != Sky.MISSING) {
                b.append(capsule(String.valueOf(Sky.dust()), "pm2.5", Sky.dust() >= 25, beat));
            }
            int worst = worstPollen();
            if (Sky.pollen(worst) > 0) {
                b.append(capsule(String.valueOf(Sky.pollen(worst)), pollenName(worst),
                    Sky.pollen(worst) >= 10, beat));
            } else {
                b.append(capsule("", Words.s("no_pollen"), false, beat));
            }
        }
        /* The two long capsules close the list, so the short ones pack
           together above them instead of leaving holes at line ends. */
        if (Sky.dawn().length() > 0) {
            b.append(capsule(Sky.dawn() + " \u2013 " + Sky.dusk(), Words.s("light"),
                false, beat));
        }
        float kp = Sky.storm();
        if (kp >= 0f) {
            int rounded = Math.round(kp);
            String verdict = rounded >= 5 ? Words.s("aurora")
                : (rounded >= 4 ? Words.s("maybe") : Words.s("quiet"));
            b.append(capsule(String.valueOf(rounded),
                Words.s("geomagnetic") + " \u00B7 " + verdict, rounded >= 4, beat));
        }
        return b.append("</div>").toString();
    }

    /** One capsule; the beat moves on so each arrives a little after the last. */
    private static String capsule(String value, String name, boolean asks, int[] beat) {
        String out = "<span class='cap" + (asks ? " asks" : "")
            + "' style='animation-delay:" + beat[0] + "ms'>"
            + (value.length() > 0 ? "<span class=cv>" + value + "</span>" : "")
            + "<span class=cl>" + safe(name) + "</span></span>";
        beat[0] += 40;
        return out;
    }

private static int worstPollen() {
        int worst = 0;
        for (int i = 1; i < 4; i++) {
            if (Sky.pollen(i) > Sky.pollen(worst)) {
                worst = i;
            }
        }
        return worst;
    }

private static String pollenName(int kind) {
        String[] names = {"alder", "birch", "grass", "mugwort"};
        return Words.s(names[kind]);
    }

    /**
     * What is flying, kind by kind. Nine months a year every count is zero,
     * and four empty bars say nothing worth the room: the bars appear when
     * something is in the air, and a single capsule says so when nothing is.
     */
    private static String grains(int rank) {
        if (!Sky.sniffed()) {
            return "";
        }
        int flying = 0;
        for (int i = 0; i < 4; i++) {
            if (Sky.pollen(i) > 0) {
                flying++;
            }
        }
        if (flying == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder("<div class=grains>");
        for (int i = 0; i < 4; i++) {
            int count = Sky.pollen(i);
            int width = count <= 0 ? 3 : Math.min(100, Math.round(count / 50f * 100f));
            b.append("<span class=grain><span class=gname>").append(safe(pollenName(i)))
                .append("</span><span class=gbar><span class=gfill style='width:")
                .append(width).append("%;animation-delay:")
                .append(rank * 60 + 300 + i * 50).append("ms;background:")
                .append(count >= 10 ? Tone.hex(Tone.primary())
                    : Tone.hex(Tone.faint()))
                .append("'></span></span><span class=gnum>")
                .append(count < 0 ? "\u2014" : String.valueOf(count))
                .append("</span></span>");
        }
        return b.append("</div>").toString();
    }

    /** Motion and shape for the sky page: arrivals, columns, capsules, grains. */
    private static String sections() {
        String on = Tone.hex(Tone.onSurface());
        String faint = Tone.hex(Tone.faint());
        StringBuilder b = new StringBuilder();
        b.append(".part{opacity:0;transform:translateY(14px);")
            .append("animation:part 460ms cubic-bezier(.2,0,0,1) forwards}")
            .append("@keyframes part{to{opacity:1;transform:translateY(0)}}")

            /* The card of the moment: the largest thing on the page, so the
               largest radius the form allows. */
            .append(".hero{border-radius:34px;padding:22px 20px 20px;margin:4px 0 14px}")
            .append(".top{display:flex;align-items:center;gap:12px}")
            .append(".sky{display:flex}.sky svg{width:52px;height:52px}")
            .append(".temp{font-size:68px;line-height:1;letter-spacing:-2px;color:")
            .append(on).append("}")
            .append(".where{flex:1;min-width:0;display:flex;flex-direction:column;")
            .append("align-items:flex-end;text-align:right;gap:2px}")
            .append("a.place{max-width:100%;overflow:hidden;text-overflow:ellipsis;")
            .append("white-space:nowrap;text-decoration:none;font-size:16px;color:")
            .append(on).append("}")
            .append(".feel{font-size:13px;color:").append(faint).append("}")

            .append(".soon{margin-top:18px}")
            .append(".soon:not(.wet){display:flex;align-items:baseline;")
            .append("justify-content:space-between;gap:12px}")
            .append(".says{font-size:17px;color:").append(on).append("}")
            .append(".soon.wet .says{display:block;margin-bottom:10px}")
            .append(".span{font-size:12px;color:").append(faint).append("}")
            .append(".bars{display:flex;align-items:flex-end;gap:6px;height:44px}")
            .append(".bar{flex:1;border-radius:4px;transform-origin:bottom;")
            .append("transform:scaleY(0);animation:grow 420ms cubic-bezier(.2,0,0,1) forwards}")
            .append("@keyframes grow{to{transform:scaleY(1)}}")
            .append(".ticks{display:flex;justify-content:space-between;margin-top:8px;")
            .append("font-size:11px;color:").append(faint).append("}")

            /* Capsules: full round ends, the tone above the card, and the
               accent only for a number that asks something of you. */
            .append(".caps{display:flex;flex-wrap:wrap;gap:8px;margin-top:18px}")
            .append(".cap{display:inline-flex;align-items:baseline;gap:7px;")
            .append("padding:8px 15px;border-radius:999px;background:")
            .append(Tone.hex(Tone.containerHigh())).append(";opacity:0;")
            .append("animation:part 460ms cubic-bezier(.2,0,0,1) forwards}")
            .append(".cv{font-size:16px;color:").append(on).append("}")
            .append(".cl{font-size:12px;color:").append(Tone.hex(Tone.onVariant())).append("}")
            .append(".cap.asks{background:").append(Tone.hex(Tone.primary())).append("}")
            .append(".cap.asks .cv,.cap.asks .cl{color:")
            .append(Tone.hex(Tone.onAccent())).append("}")
            .append(".cap.asks svg{stroke:").append(Tone.hex(Tone.onAccent())).append("}")

            .append(".grains{margin-top:16px}")
            .append(".grain{display:flex;align-items:center;gap:12px;margin:8px 0}")
            .append(".gname{width:74px;font-size:13px;color:")
            .append(Tone.hex(Tone.onVariant())).append("}")
            .append(".gbar{flex:1;height:8px;border-radius:4px;overflow:hidden;background:")
            .append(Tone.hex(Tone.containerHigh())).append("}")
            .append(".gfill{display:block;height:100%;border-radius:4px;")
            .append("transform-origin:left;transform:scaleX(0);")
            .append("animation:wide 480ms cubic-bezier(.2,0,0,1) forwards}")
            .append("@keyframes wide{to{transform:scaleX(1)}}")
            .append(".gnum{width:38px;text-align:right;font-size:12px;color:")
            .append(faint).append("}")

            .append(".strip{display:flex;gap:8px;overflow-x:auto;padding-bottom:6px;")
            .append("margin-bottom:12px}")
            .append(".hourly{flex:none;width:62px;display:flex;flex-direction:column;")
            .append("align-items:center;gap:5px;padding:12px 0;border-radius:22px;")
            .append("background:").append(Tone.hex(Tone.container())).append("}")
            .append(".at{font-size:12px;color:").append(faint).append("}")
            .append(".hmark svg{width:22px;height:22px}")
            .append(".hwarm{font-size:16px;color:").append(on).append("}")
            .append(".hwet{font-size:11px;color:").append(Tone.hex(Tone.primary())).append("}")
            .append(".dwet{font-size:12px;width:44px;text-align:right;color:")
            .append(Tone.hex(Tone.primary())).append("}")

            .append(".source{font-size:11px;line-height:1.5;margin:22px 0 0;text-align:center;")
            .append("color:").append(faint).append("}");
        return b.toString();
    }

    /** The room the colour page lives in, lent to any page that wants it. */
    private static String glow() {
        StringBuilder b = new StringBuilder();
        b.append("#glow{position:fixed;left:0;right:0;top:0;bottom:0;overflow:hidden;")
            .append("pointer-events:none;z-index:0;filter:blur(86px);opacity:.9}")
            .append(".blob{position:absolute;border-radius:50%;will-change:transform}")
            .append("#b1{width:96vw;height:52vh;left:-18vw;top:-18vh;")
            .append("animation:d1 38s ease-in-out infinite alternate}")
            .append("#b2{width:80vw;height:44vh;left:28vw;top:-6vh;")
            .append("animation:d2 47s ease-in-out infinite alternate}")
            .append("#b3{width:104vw;height:46vh;left:-24vw;top:14vh;")
            .append("animation:d3 61s ease-in-out infinite alternate}")
            .append("@keyframes d1{from{transform:translate3d(0,0,0) scale(1)}")
            .append("to{transform:translate3d(15vw,8vh,0) scale(1.2)}}")
            .append("@keyframes d2{from{transform:translate3d(0,0,0) scale(1.06)}")
            .append("to{transform:translate3d(-19vw,11vh,0) scale(.88)}}")
            .append("@keyframes d3{from{transform:translate3d(0,0,0) scale(.94)}")
            .append("to{transform:translate3d(11vw,-9vh,0) scale(1.24)}}")
            .append("#veil{position:fixed;left:0;right:0;top:0;bottom:0;pointer-events:none;")
            .append("z-index:1;background:linear-gradient(180deg,")
            .append(Tone.rgba(Tone.surface(), .05f)).append(" 0%,")
            .append(Tone.rgba(Tone.surface(), .34f)).append(" 42%,")
            .append(Tone.rgba(Tone.surface(), .78f)).append(" 72%,")
            .append(Tone.hex(Tone.surface())).append(" 92%)}")
            .append("body>*:not(#glow):not(#veil){position:relative;z-index:2}");
        return b.toString();
    }

    /**
     * An arrow turned to where the wind comes from. A compass letter would
     * need translating and a number would need reading; an arrow needs
     * neither, and the speed stands beside it.
     */
    private static String blow(int whence) {
        if (whence == Sky.MISSING) {
            return "";
        }
        return "<svg width=16 height=16 viewBox='0 0 24 24' fill=none stroke='"
            + Tone.hex(Tone.primary()) + "' stroke-width=2 stroke-linecap=round "
            + "style='vertical-align:-2px;margin-right:5px;transform:rotate("
            + ((whence + 180) % 360) + "deg)'>"
            + "<path d='M12 4v16'/><path d='M7 9L12 4l5 5'/></svg>";
    }

    /** What the number of the sun means for a body standing under it. */
    private static String burnt(int burn) {
        if (burn <= 2) {
            return Words.s("uv_low");
        }
        return burn <= 5 ? Words.s("uv_mid") : Words.s("uv_high");
    }

    /**
     * Where the weather is for: a name typed and found, or where the phone
     * stood when asked once. Nothing follows the phone about.
     */
    static String places(String now, String[][] found, String asked) {
        StringBuilder b = new StringBuilder();
        b.append(head(Words.s("place")));
        b.append("<label>").append(safe(Words.s("place"))).append("</label>");
        b.append("<input id=q value='").append(safe(asked)).append("' placeholder='")
            .append(safe(now)).append("'>");
        b.append("<div><a class='pill go' href='javascript:void(0)' onclick=\"")
            .append("location.href='ellipse:find?q='+encodeURIComponent(q.value)\">")
            .append(safe(Words.s("find"))).append("</a>")
            .append("<a class=pill href='ellipse:standing'>")
            .append(safe(Words.s("standing"))).append("</a></div>");
        if (found != null) {
            for (int i = 0; i < found.length; i++) {
                b.append("<a class='card").append(found[i][0].equals(now) ? " here" : "")
                    .append("' href='ellipse:here?i=").append(i)
                    .append("'><span class=plate>").append(sign("weather", false))
                    .append("</span><span class=two><span class=name>")
                    .append(safe(found[i][0])).append("</span><span class=host>")
                    .append(safe(found[i][3])).append("</span></span></a>");
            }
        }
        b.append("</body></html>");
        return b.toString();
    }

private static String chips() {
        StringBuilder b = new StringBuilder();
        b.append(".chips{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:16px}")
            .append("a.chip{text-decoration:none;font-size:15px;padding:10px 18px;")
            .append("border-radius:20px;border:1px solid ").append(Tone.hex(Tone.outline()))
            .append(";color:").append(Tone.hex(Tone.onSurface())).append(";background:")
            .append(Tone.hex(Tone.container())).append("}")
            .append("a.chip.on{background:").append(Tone.hex(Tone.primary()))
            .append(";color:").append(Tone.hex(Tone.onAccent())).append(";border-color:")
            .append(Tone.hex(Tone.primary())).append("}")
            .append("a.chip.quiet{color:").append(Tone.hex(Tone.faint())).append("}")
            .append(".num{opacity:.6;font-size:13px}");
        return b.toString();
    }

    /** The shell every page of the home screen shares. */
    private static String head(String title) {
        return head(title, "");
    }

private static String head(String title, String extra) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html><head><meta charset=utf-8>")
            .append("<meta name=format-detection content='telephone=no,date=no,address=no'>")
            .append("<meta name=viewport ")
            .append("content='width=device-width,initial-scale=1'><style>")
            /* Our own screens are not for picking up by hand: a caret and a
               bar of somebody else's making over a list of settings is an
               artefact, not a feature. The log and the fields still are. */
            .append("*{box-sizing:border-box;-webkit-user-select:none;user-select:none;")
            .append("-webkit-tap-highlight-color:transparent}")
            .append("pre,input,textarea{-webkit-user-select:text;user-select:text}")
            .append("body{margin:0;padding:26px 20px 90px;background:")
            .append(Tone.hex(Tone.surface())).append(";color:")
            .append(Tone.hex(Tone.onSurface())).append(";font:16px/1.5 sans-serif}")
            .append("h1{font-size:30px;font-weight:400;margin:6px 0 22px}")
            .append("h2{font-size:13px;font-weight:400;color:").append(Tone.hex(Tone.faint()))
            .append(";margin:30px 0 10px}")
            .append("input{width:100%;background:").append(Tone.hex(Tone.container()))
            .append(";border:1px solid ").append(Tone.hex(Tone.containerHigh()))
            .append(";border-radius:22px;padding:13px 18px;color:")
            .append(Tone.hex(Tone.onSurface())).append(";font-size:16px}")
            .append("a.card,div.card{display:flex;align-items:center;gap:14px;text-decoration:none;")
            .append("background:").append(Tone.hex(Tone.container()))
            .append(";border:2px solid transparent;border-radius:30px;")
            .append("padding:12px 20px;margin-bottom:9px}")
            .append("a.card.here{background:").append(Tone.hex(Tone.containerHigh()))
            .append(";border-color:").append(Tone.hex(Tone.primary())).append("}")
            .append(".plate{width:34px;height:34px;border-radius:50%;flex:none;display:flex;")
            .append("align-items:center;justify-content:center;font-size:16px;background:")
            .append(Tone.hex(Tone.primary())).append(";color:")
            .append(Tone.hex(Tone.onAccent())).append("}")
            .append(".two{display:flex;flex-direction:column;min-width:0}")
            .append(".name{display:-webkit-box;-webkit-line-clamp:2;")
            .append("-webkit-box-orient:vertical;overflow:hidden;font-size:17px;color:").append(Tone.hex(Tone.onSurface())).append("}")
            .append(".host{font-size:12px;color:").append(Tone.hex(Tone.faint())).append("}")
            .append("a.pill{display:inline-block;background:")
            .append(Tone.hex(Tone.containerHigh())).append(";color:")
            .append(Tone.hex(Tone.onSurface()))
            .append(";border-radius:24px;padding:13px 22px;margin:14px 8px 0 0;")
            .append("text-decoration:none;font-size:16px}")
            .append("a.go{background:").append(Tone.hex(Tone.primary())).append(";color:")
            .append(Tone.hex(Tone.onAccent())).append("}")
            .append("label{display:block;font-size:12px;margin:16px 0 4px;color:")
            .append(Tone.hex(Tone.faint())).append("}")
            .append("pre{white-space:pre-wrap;margin:0;font:14px/1.5 sans-serif;color:")
            .append(Tone.hex(Tone.onVariant())).append("}")
            .append(".plate.pale{background:").append(Tone.hex(Tone.containerHigh()))
            .append(";color:").append(Tone.hex(Tone.faint())).append("}")
            .append("a.head{text-decoration:none;display:flex;align-items:flex-end;")
            .append("gap:16px;margin:0 0 22px;")
            .append("padding:20px 22px;border-radius:34px;background:")
            .append(Tone.hex(Tone.container())).append("}")
            .append("a.wire{text-decoration:none;display:flex;align-items:center;gap:12px;")
            .append("margin:0 0 22px;padding:16px 22px;border-radius:30px;background:")
            .append(Tone.hex(Tone.container())).append("}")
            .append("a.wire .two{flex:1;min-width:0}")
            .append("a.wire .name{font-size:17px}")
            .append(".flock{display:flex;width:26px;height:26px}")
            .append(".flock svg{width:26px;height:26px}")
            .append(".seed{width:22px;height:22px;margin-left:-8px;border-radius:50%;")
            .append("display:flex;align-items:center;justify-content:center;font-size:11px;")
            .append("border:2px solid ").append(Tone.hex(Tone.container())).append(";background:")
            .append(Tone.hex(Tone.containerHigh())).append(";color:")
            .append(Tone.hex(Tone.onVariant())).append("}")
            .append(".count{margin-left:10px;font-size:30px;line-height:1;color:")
            .append(Tone.hex(Tone.primary())).append("}")
            .append(".now{display:flex;flex-direction:column;flex:1;min-width:0}")
            .append(".clock{font-size:52px;line-height:1;letter-spacing:-2px;color:")
            .append(Tone.hex(Tone.onSurface())).append("}")
            .append(".day{font-size:13px;margin-top:8px;color:")
            .append(Tone.hex(Tone.faint())).append("}")
            .append(".side{display:flex;flex-direction:column;align-items:flex-end;gap:12px}")
            .append(".warm{font-size:28px;line-height:1;color:")
            .append(Tone.hex(Tone.primary())).append("}")
            .append(".power{display:flex;align-items:center;gap:9px}")
            .append(".cell{position:relative;display:block;width:58px;height:10px;")
            .append("border-radius:5px;overflow:hidden;background:")
            .append(Tone.hex(Tone.containerHigh())).append("}")
            .append(".juice{position:absolute;left:0;top:0;bottom:0;border-radius:5px;")
            .append("background:").append(Tone.hex(Tone.primary())).append("}")
            .append(".percent{font-size:12px;color:").append(Tone.hex(Tone.onVariant()))
            .append("}")
            .append(".wall{border-radius:30px;padding:22px;margin-bottom:8px;")
            .append("background:repeating-linear-gradient(135deg,")
            .append(Tone.hex(Tone.containerHigh())).append(" 0 14px,")
            .append(Tone.hex(Tone.container())).append(" 14px 28px)}")
            .append(".sample{display:flex;align-items:center;border-radius:24px;")
            .append("padding:18px 20px;background:").append(Tone.hex(Tone.surface())).append("}")
            .append(".stime{flex:1;font-size:30px;color:")
            .append(Tone.hex(Tone.onSurface())).append("}")
            .append(".sorb{width:40px;height:40px;border-radius:50%;background:")
            .append(Tone.hex(Tone.primary())).append("}")
            .append("input[type=range]{-webkit-appearance:none;width:100%;height:44px;")
            .append("border-radius:22px;outline:none;border:1px solid ")
            .append(Tone.hex(Tone.containerHigh())).append("}")
            .append("input[type=range]::-webkit-slider-thumb{-webkit-appearance:none;")
            .append("width:34px;height:34px;border-radius:50%;background:")
            .append(Tone.hex(Tone.onSurface())).append(";border:2px solid ")
            .append(Tone.hex(Tone.faint())).append("}")
            .append("img.face{width:34px;height:34px;border-radius:9px;flex:none}")
            .append(".quietline{font-size:14px;margin:4px 0 0;color:")
            .append(Tone.hex(Tone.faint())).append("}")
            .append(".day2{display:flex;align-items:center;gap:14px;padding:14px 20px;")
            .append("border-radius:26px;margin-bottom:8px;background:")
            .append(Tone.hex(Tone.container())).append("}")
            .append(".mark svg{width:26px;height:26px;vertical-align:-6px}")
            .append(".when{flex:1;font-size:17px;color:")
            .append(Tone.hex(Tone.onSurface())).append("}")
            .append(".hot{font-size:18px;color:").append(Tone.hex(Tone.onSurface())).append("}")
            .append(".cold{font-size:18px;width:52px;text-align:right;color:")
            .append(Tone.hex(Tone.faint())).append("}")
            .append(".acts{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:14px}")
            .append("pre.log{font:11px/1.45 monospace;white-space:pre-wrap;")
            .append("word-break:break-all;color:").append(Tone.hex(Tone.onVariant()))
            .append(";background:").append(Tone.hex(Tone.container()))
            .append(";border-radius:20px;padding:16px}")
            .append(".row{display:flex;align-items:center;gap:8px}")
            .append(".row a.card{flex:1;min-width:0}")
            .append("a.drop{flex:none;width:44px;height:44px;border-radius:50%;display:flex;")
            .append("align-items:center;justify-content:center;text-decoration:none;")
            .append("font-size:20px;background:").append(Tone.hex(Tone.container()))
            .append(";color:").append(Tone.hex(Tone.faint())).append("}")
            .append("pre.fine{font-size:11px;line-height:1.6;color:")
            .append(Tone.hex(Tone.faint())).append("}")
            .append("a.drop svg{width:20px;height:20px}")
            .append("a.drop.on{background:").append(Tone.hex(Tone.primary())).append("}")
            .append(glow())
            .append(extra)
            .append("</style></head><body>")
            .append("<div id=glow><span class=blob id=b1 style='background:")
            .append(Tone.hex(Tone.lit(0.52f, 0.95f))).append("'></span>")
            .append("<span class=blob id=b2 style='background:")
            .append(Tone.hex(Tone.lit(0.42f, 0.8f))).append("'></span>")
            .append("<span class=blob id=b3 style='background:")
            .append(Tone.hex(Tone.lit(0.34f, 0.7f))).append("'></span></div>")
            .append("<div id=veil></div>")
            .append("<h1>").append(safe(title)).append("</h1>");
        return b.toString();
    }

    /** The colour page: three bands and a veil, mixed under the thumb. */
    static String look(float hue, float sat, float val, int solid, int ground, int zoom, int from,
                       boolean system) {
        StringBuilder rules = new StringBuilder();
        /* The sliders keep a wide margin from the screen's edges, where the
           phone's own gesture for going back begins. */
        rules.append("body{padding-left:34px;padding-right:34px}")
            .append(".pair{display:flex;gap:14px;margin:0 0 22px}")
            .append("a.orb{width:66px;height:66px;border-radius:50%;display:flex;")
            .append("align-items:center;justify-content:center;font-size:27px;")
            .append("text-decoration:none;background:").append(Tone.hex(Tone.containerHigh()))
            .append(";border:2px solid transparent}")
            .append("a.orb.on{border-color:").append(Tone.hex(Tone.primary())).append("}")
            .append("#sample{margin:10px 2px 24px;line-height:1.5;color:")
            .append(Tone.hex(Tone.onVariant())).append("}")
            .append("#show{height:96px;border-radius:30px;display:flex;align-items:center;")
            .append("justify-content:center;font-size:19px;margin:8px 0 6px}")
            .append("input[type=range]{-webkit-appearance:none;width:100%;height:44px;")
            .append("border-radius:22px;outline:none;border:1px solid ")
            .append(Tone.hex(Tone.containerHigh())).append("}")
            .append("input[type=range]::-webkit-slider-thumb{-webkit-appearance:none;")
            .append("width:34px;height:34px;border-radius:50%;background:")
            .append(Tone.hex(Tone.onSurface())).append(";border:2px solid ")
            .append(Tone.hex(Tone.faint())).append("}");

        rules.append(chips());

        StringBuilder b = new StringBuilder();
        b.append(head(Words.s("look"), rules.toString()));
        /* Where the accent comes from: the phone's own colour, the
           wallpaper's, or the owner's mixing. Moving the hue, the richness
           or the brightness is mixing, and says so. */
        b.append("<div class=chips>");
        if (system) {
            b.append("<a class='chip").append(from == Keep.FROM_SYSTEM ? " on" : "")
                .append("' href='ellipse:from?v=1'>").append(safe(Words.s("by_system"))).append("</a>");
        }
        b.append("<a class='chip").append(from == Keep.FROM_WALL ? " on" : "")
            .append("' href='ellipse:from?v=2'>").append(safe(Words.s("by_wall"))).append("</a>");
        b.append("<a class='chip").append(from == Keep.FROM_OWN ? " on" : "")
            .append("' href='ellipse:from?v=0'>").append(safe(Words.s("by_hand"))).append("</a>");
        b.append("</div>");
        b.append("<div id=show>").append(safe(Words.s("accent"))).append("</div>");
        b.append("<label>").append(safe(Words.s("hue")))
            .append("</label><input id=h type=range min=0 max=360 value='")
            .append(Math.round(hue)).append("'>");
        b.append("<label>").append(safe(Words.s("richness")))
            .append("</label><input id=s type=range min=0 max=100 value='")
            .append(Math.round(sat * 100f)).append("'>");
        b.append("<label>").append(safe(Words.s("brightness")))
            .append("</label><input id=v type=range min=40 max=100 value='")
            .append(Math.round(val * 100f)).append("'>");
        b.append("<label>").append(safe(Words.s("solidity")))
            .append("</label><input id=a type=range min=55 max=100 value='")
            .append(solid).append("'>");
        /* The ground: from the old near black to a deep colour of the accent's
           own hue. The page itself shows it while the finger moves. */
        b.append("<label>").append(safe(Words.s("ground")))
            .append("</label><input id=g type=range min=0 max=100 value='")
            .append(ground).append("'>");

        /* A size is a size of something: the words move while the finger does. */
        b.append("<label>").append(safe(Words.s("text_size")))
            .append("</label><input id=z type=range min=70 max=" + Keep.ZOOM_MOST + " value='")
            .append(zoom).append("'>");
        b.append("<p id=sample>").append(safe(Words.s("sample"))).append("</p>");

        /* No save button. A slider that has come to rest under the finger has
           already said everything a button would ask it to repeat. */

        b.append("<script>var mixed=0;")
            .append("function mix(H,S,V){S/=100;V/=100;var C=V*S,X=C*(1-Math.abs((H/60)%2-1)),")
            .append("m=V-C,r,g,bl;")
            .append("if(H<60){r=C;g=X;bl=0}else if(H<120){r=X;g=C;bl=0}")
            .append("else if(H<180){r=0;g=C;bl=X}else if(H<240){r=0;g=X;bl=C}")
            .append("else if(H<300){r=X;g=0;bl=C}else{r=C;g=0;bl=X}")
            .append("return 'rgb('+Math.round((r+m)*255)+','+Math.round((g+m)*255)+',' ")
            .append("+Math.round((bl+m)*255)+')'}")
            .append("function paint(){var c=mix(+h.value,+s.value,+v.value);")
            .append("var box=document.getElementById('show');box.style.background=c;")
            .append("box.style.color=(+v.value>70&&+s.value<80)?'#120D00':'#F6F4F0';")
            .append("h.style.background='linear-gradient(90deg,#f00,#ff0,#0f0,#0ff,#00f,#f0f,#f00)';")
            .append("s.style.background='linear-gradient(90deg,#888,'+mix(+h.value,100,+v.value)+')';")
            .append("v.style.background='linear-gradient(90deg,#000,'+mix(+h.value,+s.value,100)+')';")
            .append("var b1=document.getElementById('b1'),b2=document.getElementById('b2'),")
            .append("b3=document.getElementById('b3');")
            .append("b1.style.background=mix(+h.value,+s.value*.95,+v.value*.5);")
            .append("b2.style.background=mix((+h.value+34)%360,+s.value*.8,+v.value*.4);")
            .append("b3.style.background=mix((+h.value+326)%360,+s.value*.7,+v.value*.32);")
            .append("a.style.background='linear-gradient(90deg,")
            .append(Tone.hex(Tone.surface())).append(",").append(Tone.hex(Tone.containerHigh()))
            .append(")';")
            .append("var r=+s.value/100,e=+g.value/100;")
            .append("function lerp(x,y){return x+(y-x)*e}")
            .append("var floor=mix(+h.value,100*lerp(Math.min(.30,r*.4),Math.min(.80,.30+r*.6)),")
            .append("100*lerp(.035,.19));")
            .append("var deep=mix(+h.value,100*Math.min(.80,.30+r*.6),48);")
            .append("document.body.style.background=floor;")
            .append("var veil=document.getElementById('veil');if(veil){")
            .append("veil.style.background='linear-gradient(180deg,transparent 0%,'+floor+' 92%)'}")
            .append("g.style.background='linear-gradient(90deg,")
            .append("#090909,'+deep+')';")
            .append("if(window.Ellipse)Ellipse.mix(mixed,+h.value,+s.value,+v.value,+a.value,+g.value,")
            .append("+z.value);")
            .append("var ink=(+v.value>70&&+s.value<80)?'#120D00':'#F6F4F0';")
            .append("var g=document.querySelector('a.go');")
            .append("if(g){g.style.background=c;g.style.color=ink}}")
            .append("z.style.background='linear-gradient(90deg,")
            .append(Tone.hex(Tone.surface())).append(",")
            .append(Tone.hex(Tone.containerHigh())).append(")';")
            .append("function size(){document.getElementById('sample').style.fontSize=")
            .append("(16*z.value/100)+'px';")
            .append("if(window.Ellipse)Ellipse.mix(mixed,+h.value,+s.value,+v.value,+a.value,+g.value,")
            .append("+z.value)}z.oninput=size;size();")

            .append("function keep(){location.href='ellipse:look?h='+h.value+'&s='+s.value")
            .append("+'&v='+v.value+'&a='+a.value+'&g='+g.value+'&z='+z.value+'&f='+(mixed?0:")
            .append(from).append(")}")
            .append("h.onchange=s.onchange=v.onchange=a.onchange=g.onchange=z.onchange=keep;")
            .append("h.oninput=s.oninput=v.oninput=function(){mixed=1;paint()};")
            .append("a.oninput=g.oninput=paint;paint();")
            .append("</script></body></html>");
        return b.toString();
    }
}
