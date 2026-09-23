package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/**
 * Anything a folder can hold: an application's front door, or a screen
 * inside an application. A folder keeps the names of what is in it and
 * nothing else; this is what those names turn out to be when they are
 * looked up, so a folder of applications and a folder of screens are
 * drawn and opened by the same code.
 */
final class Held {

    /** The name as the folder keeps it. */
    final String written;
    final String label;
    /** The application, when the thing is one; nothing when it is a screen inside one. */
    final App app;
    final ComponentName where;

    private Held(String written, String label, App app, ComponentName where) {
        this.written = written;
        this.label = label;
        this.app = app;
        this.where = where;
    }

    static Held of(String written, App app) {
        return new Held(written, app.label, app, app.component());
    }

    /**
     * A screen inside an application, by its full name. Nothing comes back
     * for a name that no longer answers: the application was removed, or
     * the screen was taken out of it.
     */
    static Held screen(Context context, String written) {
        ComponentName where = ComponentName.unflattenFromString(written == null ? "" : written);
        if (where == null) {
            return null;
        }
        try {
            PackageManager pm = context.getPackageManager();
            android.content.pm.ActivityInfo about = pm.getActivityInfo(where, 0);
            return new Held(written, String.valueOf(about.loadLabel(pm)), null, where);
        } catch (Exception gone) {
            return null;
        }
    }

    /** The picture of the thing, asked for when a tile of it is being drawn. */
    Drawable icon(Context context, int dpi) {
        if (app != null) {
            return app.icon(dpi);
        }
        try {
            return context.getPackageManager().getActivityInfo(where, 0).loadIcon(context.getPackageManager());
        } catch (Exception gone) {
            return null;
        }
    }

    /** The way in: the application's door, or the screen itself. */
    Intent opening() {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(where)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }
}
