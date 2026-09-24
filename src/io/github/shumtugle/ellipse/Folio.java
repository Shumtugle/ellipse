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
    static final String PLACE = "place";

    private static final String SCHEME = "ellipse:";
    private static final String BASE = "ellipse://folio/";
    private static final int WHERE = 41;

    private WebView view;
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
        super.onCreate(saved);
        Tone.read(this);
        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Tone.surface());
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
                }
                return true;
            }
        });
        root.addView(view, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        getWindow().setStatusBarColor(Tone.surface());
        getWindow().setNavigationBarColor(Tone.surface());
        String asked = getIntent().getStringExtra(PAGE);
        opened = asked == null ? LOOK : asked;
        show(opened);
    }

    private void show(String which) {
        page = which;
        String html;
        if (LOOK.equals(which)) {
            float[] look = Keep.look(this);
            html = Paper.look(look[0], look[1], look[2], Math.round(look[3]), Keep.ground(this), Keep.zoom(this));
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
        if (what.startsWith("look")) {
            float[] was = Keep.look(this);
            Keep.saveLook(this, number(what, "h", Math.round(was[0])), number(what, "s", 58) / 100f,
                number(what, "v", 100) / 100f, number(what, "a", 100));
            Keep.saveGround(this, number(what, "g", Keep.ground(this)));
            int zoom = number(what, "z", Keep.zoom(this));
            Keep.saveZoom(this, zoom);
            view.getSettings().setTextZoom(zoom);
            Tone.read(this);
            getWindow().setStatusBarColor(Tone.surface());
            getWindow().setNavigationBarColor(Tone.surface());
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

    @Override
    public void onBackPressed() {
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
