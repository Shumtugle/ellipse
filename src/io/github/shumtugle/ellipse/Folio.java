package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * The pages the home screen draws as pages: the colour page, the weather,
 * and the page where the weather's place is found. Each is written whole
 * and shown in a view of web pages that is given nothing from outside: no
 * page but ours is ever loaded, and everything a page can do is a link to
 * this application's own address, read here.
 */
public final class Folio extends Activity {

    static final String PAGE = "page";
    static final String LOOK = "look";
    static final String WEATHER = "weather";
    /** The phone's own state, a page as the weather's. */
    static final String STATE = "state";
    static final String PLACE = "place";
    static final String ABOUT = "about";
    static final String HELP = "help";

    private static final String SCHEME = "ellipse:";
    private static final String BASE = "ellipse://folio/";
    private static final int WHERE = 41;

    private WebView view;
    /** The window onto the home screen above the colour page, and the bridge the page tells it through. */
    private Glimpse glimpse;
    /** The bar at the foot, and the ground the menu of its round button opens over. */
    private Foot foot;
    private FrameLayout host;
    private static final String SETTINGS_LINE = "Settings";

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private String page = LOOK;
    private String opened = LOOK;
    private String[][] finds;
    private String asking = "";

    /** The text of a file kept in the package, or an empty one. */
    static String asset(Context context, String name) {
        try {
            InputStream in = context.getAssets().open(name);
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            byte[] chunk = new byte[2048];
            int n;
            while ((n = in.read(chunk)) > 0) {
                all.write(chunk, 0, n);
            }
            in.close();
            return all.toString("UTF-8").trim();
        } catch (Exception gone) {
            return "";
        }
    }

    /** An address kept in the package, with the weather's place put in; empty with no place. */
    static String aimed(Context context, String file) {
        String[] here = Keep.here(context);
        Sky.place(here[0]);
        if (here[0].length() == 0 || here[1].length() == 0) {
            return "";
        }
        return asset(context, file).replace("%lat", here[1]).replace("%lon", here[2]);
    }

    /** Asks for the sky, the air and the space weather; tells when anything came. */
    static void freshen(Context context, Runnable then) {
        Sky.ask(aimed(context, "weather.txt"), then);
        Sky.sniff(aimed(context, "air.txt"), then);
        Sky.count(asset(context, "space.txt"), then);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Home.sized(base));
    }

    @Override
    protected void onCreate(Bundle saved) {
        Lapse.watch(this);
        super.onCreate(saved);
        Tone.read(this);
        String asked = getIntent().getStringExtra(PAGE);
        opened = asked == null ? LOOK : asked;
        host = new FrameLayout(this);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(LOOK.equals(opened) ? 0x00000000 : Tone.surface());
        view = new WebView(this);
        view.setBackgroundColor(Tone.surface());
        view.setVerticalScrollBarEnabled(false);
        view.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setTextZoom(Keep.zoom(this));
        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith(SCHEME)) {
                    command(url.substring(SCHEME.length()));
                } else if (url.startsWith("https://") || url.startsWith("http://")) {
                    /* A page elsewhere opens where the phone opens pages. */
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                    } catch (RuntimeException none) {
                        // No browser: the link simply does nothing.
                    }
                }
                return true;
            }
        });
        if (LOOK.equals(opened)) {
            glimpse = new Glimpse(this);
            glimpse.show(Keep.zoom(this));
            root.addView(glimpse, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
            view.addJavascriptInterface(new Bridge(), "Ellipse");
        }
        root.addView(view, new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        /* The bar every settings screen ends in: the way back at its start,
           the page's name, and the round button whose cross leads home. */
        foot = new Foot(this, host, "", new Foot.Owner() {
            public Menu.Section[] sections() {
                return new Menu.Section[] {Home.settingsLine(SETTINGS_LINE, 0)};
            }

            public void picked(int section, int key) {
                startActivity(new Intent(Folio.this, Tune.class));
                finish();
            }

            public void leave() {
                startActivity(new Intent(Folio.this, Home.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }

            public void typed(String text) {
            }
        });
        foot.back(new Runnable() {
            public void run() {
                onBackPressed();
            }
        });
        android.widget.LinearLayout.LayoutParams footAt = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        footAt.setMargins(dp(8), dp(6), dp(8), dp(10));
        /* Where the phone draws its pages under its own bars, the bars' grounds are painted here. */
        overBar = new android.view.View(this);
        underBar = new android.view.View(this);
        host.addView(overBar, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0,
            android.view.Gravity.TOP));
        host.addView(underBar, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0,
            android.view.Gravity.BOTTOM));
        host.setOnApplyWindowInsetsListener(new android.view.View.OnApplyWindowInsetsListener() {
            public android.view.WindowInsets onApplyWindowInsets(android.view.View v, android.view.WindowInsets insets) {
                overBar.getLayoutParams().height = insets.getSystemWindowInsetTop();
                underBar.getLayoutParams().height = insets.getSystemWindowInsetBottom();
                overBar.requestLayout();
                underBar.requestLayout();
                return insets;
            }
        });
        host.addView(root, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(foot, footAt);
        setContentView(host);
        bands(LOOK.equals(opened) ? Tone.frame() : Tone.surface(), Tone.surface());
        Tone.dress(getWindow(), !LOOK.equals(opened));
        show(opened);
    }

    private void show(String which) {
        page = which;
        if (foot != null) {
            foot.named(LOOK.equals(which) ? Words.s("colour_text")
                : PLACE.equals(which) ? Words.s("weather_place") : ABOUT.equals(which) ? "About"
                : HELP.equals(which) ? "Help" : STATE.equals(which) ? Words.t("State") : Words.s("weather"));
        }
        String html;
        if (ABOUT.equals(which) || HELP.equals(which)) {
            /* Kept in English: a page too long for a module, and the same for everyone. */
            html = Paper.text(asset(this, which + ".html").replace("{version}", Copy.version(this)));
        } else if (STATE.equals(which)) {
            html = Paper.state(Phone.read(this));
        } else if (LOOK.equals(which)) {
            float[] look = Keep.look(this);
            Tone.read(this);
            html = Paper.look(Tone.hue(), Tone.rich(), Tone.bright(), Math.round(look[3]), Keep.ground(this),
                Keep.zoom(this), Keep.from(this), android.os.Build.VERSION.SDK_INT >= 31);
        } else if (PLACE.equals(which)) {
            html = Paper.places(Sky.place(), finds, asking);
        } else {
            if (Keep.here(this)[0].length() == 0) {
                needPlace();
                return;
            }
            html = Paper.weather(asset(this, "sky.txt"));
            freshen(this, new Runnable() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (WEATHER.equals(page) && !isFinishing()) {
                                view.loadDataWithBaseURL(BASE, Paper.weather(asset(Folio.this, "sky.txt")),
                                    "text/html", "utf-8", null);
                            }
                        }
                    });
                }
            });
        }
        view.loadDataWithBaseURL(BASE, html, "text/html", "utf-8", null);
    }

    /**
     * What the colour page says while a finger is on a slider. Only our own
     * page is ever loaded here, so only our own page can speak through it.
     */
    private final class Bridge {
        @android.webkit.JavascriptInterface
        public void mix(final int mixed, final int h, final int s, final int v, final int a, final int g,
                        final int z) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (glimpse == null) {
                        return;
                    }
                    Tone.mix(mixed == 1 ? h : -1f, s / 100f, v / 100f, a, g);
                    glimpse.show(z);
                }
            });
        }
    }

    private static String said(String command, String key) {
        int q = command.indexOf('?');
        if (q < 0) {
            return null;
        }
        for (String pair : command.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                return Uri.decode(pair.substring(eq + 1));
            }
        }
        return null;
    }

    private static int number(String command, String key, int fallback) {
        try {
            return Integer.parseInt(said(command, key));
        } catch (Exception broken) {
            return fallback;
        }
    }

    /** What a link on one of the pages asks for. */
    private void command(String what) {
        if (what.equals("battery")) {
            /* The battery's own page of the phone: what used it. */
            try {
                startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
            } catch (RuntimeException none) {
                // Nowhere to go.
            }
            return;
        }
        if (what.startsWith("from")) {
            Keep.saveFrom(this, number(what, "v", Keep.FROM_OWN));
            Tone.read(this);
            if (glimpse != null) {
                glimpse.show(Keep.zoom(this));
            }
            bands(Tone.surface(), Tone.surface());
            Tone.dress(getWindow(), true);
            show(LOOK);
            return;
        }
        if (what.startsWith("look")) {
            float[] was = Keep.look(this);
            int before = Keep.from(this);
            int from = number(what, "f", before);
            if (from == Keep.FROM_OWN) {
                /* Mixed by hand: the sliders' colour is the owner's now. */
                Keep.saveLook(this, number(what, "h", Math.round(was[0])), number(what, "s", 58) / 100f,
                    number(what, "v", 100) / 100f, number(what, "a", 100));
                if (Keep.from(this) != Keep.FROM_OWN) {
                    Keep.saveFrom(this, Keep.FROM_OWN);
                }
            } else {
                Keep.saveLook(this, was[0], was[1], was[2], number(what, "a", 100));
            }
            Keep.saveGround(this, number(what, "g", Keep.ground(this)));
            int zoom = number(what, "z", Keep.zoom(this));
            Keep.saveZoom(this, zoom);
            view.getSettings().setTextZoom(zoom);
            Tone.read(this);
            bands(Tone.surface(), Tone.surface());
            Tone.dress(getWindow(), true);
            if (glimpse != null) {
                glimpse.show(zoom);
            }
            if (before != Keep.FROM_OWN && from == Keep.FROM_OWN) {
                /* The chips above say where the colour comes from: now from the hand. */
                show(LOOK);
            }
            return;
        }
        if (what.startsWith("place")) {
            show(PLACE);
            return;
        }
        if (what.startsWith("standing")) {
            needPlace();
            return;
        }
        if (what.startsWith("find")) {
            asking = said(what, "q");
            final String query = asking == null ? "" : asking;
            Sky.look(asset(this, "places.txt").replace("%s", Uri.encode(query)), new Sky.Answer() {
                public void said(String answer) {
                    finds = Sky.found(answer);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            show(PLACE);
                        }
                    });
                }
            });
            return;
        }
        if (what.startsWith("here")) {
            int at = number(what, "i", -1);
            if (finds != null && at >= 0 && at < finds.length) {
                Keep.saveHere(this, finds[at][0], finds[at][1], finds[at][2]);
                Sky.place(finds[at][0]);
                Keep.nudge(this);
            }
            show(WEATHER);
            return;
        }
        if (what.startsWith("weather")) {
            show(WEATHER);
        }
    }

    /**
     * The first time the weather is opened without a place, the phone is
     * asked where it stands, with the owner's leave; nothing is looked up
     * until it answers, and nothing is assumed about which town that is.
     * Refused, the page for finding a place by name opens instead.
     */
    private void needPlace() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            standing();
        } else {
            requestPermissions(new String[] {android.Manifest.permission.ACCESS_COARSE_LOCATION}, WHERE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int asked, String[] what, int[] answers) {
        super.onRequestPermissionsResult(asked, what, answers);
        if (asked != WHERE) {
            return;
        }
        if (answers.length > 0 && answers[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            standing();
        } else {
            show(PLACE);
        }
    }

    /** Where the phone stands now, once, named if the system can name it. */
    private void standing() {
        Location spot = null;
        try {
            LocationManager maps = (LocationManager) getSystemService(LOCATION_SERVICE);
            String[] ways = {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER};
            for (int i = 0; i < ways.length && spot == null && maps != null; i++) {
                try {
                    spot = maps.getLastKnownLocation(ways[i]);
                } catch (Exception broken) {
                    spot = null;
                }
            }
        } catch (Exception broken) {
            spot = null;
        }
        if (spot == null) {
            show(PLACE);
            return;
        }
        String lat = String.valueOf(Math.round(spot.getLatitude() * 100.0) / 100.0);
        String lon = String.valueOf(Math.round(spot.getLongitude() * 100.0) / 100.0);
        String name = called(spot);
        Keep.saveHere(this, name, lat, lon);
        Sky.place(name);
        Keep.nudge(this);
        show(WEATHER);
    }

    /** The name of a spot, when the system can put one to it. */
    private String called(Location spot) {
        try {
            Geocoder names = new Geocoder(this, java.util.Locale.US);
            java.util.List<Address> found = names.getFromLocation(spot.getLatitude(), spot.getLongitude(), 1);
            if (found != null && !found.isEmpty()) {
                Address one = found.get(0);
                if (one.getLocality() != null) {
                    return one.getLocality();
                }
                if (one.getSubAdminArea() != null) {
                    return one.getSubAdminArea();
                }
                if (one.getAdminArea() != null) {
                    return one.getAdminArea();
                }
            }
        } catch (Exception broken) {
            // Left unnamed.
        }
        return Words.s("here");
    }

    private android.view.View overBar;
    private android.view.View underBar;

    /** The grounds of the phone's bars: their own colours, and the bands under them where pages run beneath. */
    @SuppressWarnings("deprecation")
    private void bands(int top, int bottom) {
        getWindow().setStatusBarColor(top);
        getWindow().setNavigationBarColor(bottom);
        if (overBar != null) {
            overBar.setBackgroundColor(top);
            underBar.setBackgroundColor(bottom);
        }
    }

    @Override
    public void onBackPressed() {
        if (foot != null && foot.menuShown()) {
            foot.shutMenu(true);
            return;
        }
        if (PLACE.equals(page) && !PLACE.equals(opened) && Keep.here(this)[0].length() > 0) {
            show(WEATHER);
            return;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        if (view != null) {
            view.destroy();
        }
        super.onDestroy();
    }
}
