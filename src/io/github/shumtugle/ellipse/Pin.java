package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * Where another application asks to put something on the home screen: a
 * shortcut to something inside it, or one of its widgets. A card comes
 * up from the foot with what is asked for, its picture and its name, and
 * two words: Cancel and Add. Added, it waits for the home screen, which
 * sets it down in the first free place of the home screen when it is next
 * in front.
 */
public final class Pin extends Activity {

    private static final String ASKING = "Add to home screen";
    private static final String CANCEL = "Cancel";
    private static final String ADD = "Add";
    private static final int ASK_BIND = 21;

    private LauncherApps.PinItemRequest request;
    private int widgetId = -1;
    private AppWidgetHost host;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Tone.read(this);
        LauncherApps launcher = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        try {
            request = launcher.getPinItemRequest(getIntent());
        } catch (RuntimeException broken) {
            request = null;
        }
        if (request == null || !request.isValid()) {
            finish();
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        Drawable picture;
        CharSequence name;
        if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            ShortcutInfo info = request.getShortcutInfo();
            picture = launcher.getShortcutBadgedIconDrawable(info, getResources().getDisplayMetrics().densityDpi);
            name = info.getShortLabel();
        } else {
            AppWidgetProviderInfo info = request.getAppWidgetProviderInfo(this);
            if (info == null) {
                finish();
                return;
            }
            picture = info.loadPreviewImage(this, getResources().getDisplayMetrics().densityDpi);
            if (picture == null) {
                picture = info.loadIcon(this, getResources().getDisplayMetrics().densityDpi);
            }
            name = info.loadLabel(getPackageManager());
        }

        FrameLayout veil = new FrameLayout(this);
        veil.setBackgroundColor(0x66000000);
        veil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setClickable(true);
        card.setBackground(Tone.box(Tone.containerHigh(), 28 * density, 0.5f * density));
        int pad = Math.round(24 * density);
        card.setPadding(pad, pad, pad, Math.round(12 * density));
        TextView caption = new TextView(this);
        caption.setText(ASKING.toUpperCase(Locale.getDefault()));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 12f * scaled);
        caption.setLetterSpacing(0.12f);
        caption.setTextColor(Tone.faint());
        card.addView(caption);
        ImageView image = new ImageView(this);
        image.setImageDrawable(picture);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(Math.round(160 * density));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageParams.topMargin = Math.round(18 * density);
        if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            imageParams.width = Math.round(64 * density);
            imageParams.height = Math.round(64 * density);
        }
        card.addView(image, imageParams);
        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(Tone.onSurface());
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18f * scaled);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, Math.round(12 * density), 0, 0);
        card.addView(label);
        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(Gravity.END);
        TextView cancel = word(CANCEL, Tone.onSurface(), density, scaled);
        TextView add = word(ADD, Tone.primary(), density, scaled);
        foot.addView(cancel);
        foot.addView(add);
        LinearLayout.LayoutParams footParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footParams.topMargin = Math.round(12 * density);
        card.addView(foot, footParams);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        int side = Math.round(12 * density);
        cardParams.setMargins(side, 0, side, Math.round(40 * density));
        veil.addView(card, cardParams);
        setContentView(veil);
        card.setTranslationY(80 * density);
        card.setAlpha(0f);
        card.animate().translationY(0f).alpha(1f).setDuration(Pace.ARRIVE).setInterpolator(Pace.EMPHASIS).start();

        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                accept();
            }
        });
    }

    private TextView word(String text, int colour, float density, float scaled) {
        TextView made = new TextView(this);
        made.setText(text);
        made.setTextColor(colour);
        made.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * scaled);
        int h = Math.round(16 * density);
        int v = Math.round(12 * density);
        made.setPadding(h, v, h, v);
        made.setBackground(Tone.touch(null, 20 * density));
        return made;
    }

    private void accept() {
        try {
            if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                if (request.accept()) {
                    Keep.queue(this, Home.shortcutToken(request.getShortcutInfo()));
                }
                finish();
                return;
            }
            AppWidgetProviderInfo info = request.getAppWidgetProviderInfo(this);
            host = new AppWidgetHost(getApplicationContext(), Home.WIDGET_HOST);
            widgetId = host.allocateAppWidgetId();
            boolean bound = AppWidgetManager.getInstance(this)
                .bindAppWidgetIdIfAllowed(widgetId, info.getProfile(), info.provider, null);
            if (bound) {
                acceptWidget();
                return;
            }
            Intent ask = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            ask.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            ask.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
            ask.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.getProfile());
            startActivityForResult(ask, ASK_BIND);
        } catch (RuntimeException refused) {
            letGo();
            finish();
        }
    }

    private void acceptWidget() {
        Bundle extras = new Bundle();
        extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        if (request.accept(extras)) {
            Keep.queue(this, "#widget:" + widgetId);
            widgetId = -1;
        } else {
            letGo();
        }
        finish();
    }

    private void letGo() {
        if (host != null && widgetId >= 0) {
            host.deleteAppWidgetId(widgetId);
        }
        widgetId = -1;
    }

    @Override
    protected void onActivityResult(int asked, int answer, Intent data) {
        super.onActivityResult(asked, answer, data);
        if (asked == ASK_BIND) {
            if (answer == RESULT_OK) {
                acceptWidget();
            } else {
                letGo();
                finish();
            }
        }
    }
}
