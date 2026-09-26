package io.github.shumtugle.ellipse;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * The last error the home screen stopped on, kept in a file of its own:
 * when and in which version, on which phone, and the whole trace — for the
 * owner to copy and send as words rather than describe. The phone's own
 * handling of the fall goes on as before.
 */
final class Lapse {

    private static final String FILE = "last-error.txt";
    private static boolean watching;

    private Lapse() {
    }

    /** From now on, a fall of any thread is written down before it goes on as it would. */
    static void watch(final Context context) {
        if (watching) {
            return;
        }
        watching = true;
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable fall) {
                try {
                    keep(app, thread, fall);
                } catch (Throwable unkept) {
                    // Nothing is kept; the fall goes on all the same.
                }
                if (before != null) {
                    before.uncaughtException(thread, fall);
                }
            }
        });
    }

    private static void keep(Context context, Thread thread, Throwable fall) throws IOException {
        StringWriter trace = new StringWriter();
        fall.printStackTrace(new PrintWriter(trace));
        String words = "Ellipse " + Copy.version(context) + "\n"
            + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(new java.util.Date())
            + "\n" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + ", Android "
            + android.os.Build.VERSION.RELEASE + " (" + android.os.Build.VERSION.SDK_INT + ")\n"
            + "Thread: " + thread.getName() + "\n\n" + trace;
        try (OutputStream out = new FileOutputStream(new File(context.getFilesDir(), FILE))) {
            out.write(words.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** The last error kept, as words; or none if none is. */
    static String last(Context context) {
        File file = new File(context.getFilesDir(), FILE);
        if (!file.isFile()) {
            return null;
        }
        try {
            return Copy.load(file);
        } catch (IOException unread) {
            return null;
        }
    }

    static void forget(Context context) {
        new File(context.getFilesDir(), FILE).delete();
    }
}
