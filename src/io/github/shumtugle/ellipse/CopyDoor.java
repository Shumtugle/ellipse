package io.github.shumtugle.ellipse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * The door a copy of the home screen comes in by from outside: a copy file
 * opened from wherever it lies is handed to the settings, which show its
 * picture and ask before bringing it back. Only this door is open to other
 * apps; the settings themselves stay closed to them.
 */
public final class CopyDoor extends Activity {

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Intent came = getIntent();
        if (came != null && came.getData() != null) {
            Intent in = new Intent(this, Tune.class).setAction(Intent.ACTION_VIEW)
                .setDataAndType(came.getData(), came.getType())
                .putExtra(Tune.COPY_IN, true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(in);
            } catch (RuntimeException refused) {
                // Nothing comes in.
            }
        }
        finish();
    }
}
