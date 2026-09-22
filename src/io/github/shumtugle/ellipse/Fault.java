package io.github.shumtugle.ellipse;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A fall, written down. When anything in this home screen throws what
 * nothing catches, the trace is written at once among the application's
 * own files, with the version, the phone, the settings of the moment and
 * the last of the application's own log; then the system is let do what
 * it does.
 *
 * What has been written is carried out as soon as it can be: into the
 * folder the owner gave, or, without one, into a folder of this home
 * screen's own among the phone's downloads, where any file manager finds
 * it. A fall is carried out while the process is still falling, and
 * whatever could not be is carried out when the home screen next starts.
 */
final class Fault {

    private static final String WAITING = "faults";
    /** How much of the log is kept with a fall. */
    private static final int LOG_LINES = 400;
    private static final int LOG_BYTES = 256 * 1024;

    private static boolean watching;

    private Fault() {
    }

    /** From now on in this process, every fall is written down. What earlier ones left goes out now. */
    static synchronized void watch(Context context) {
        if (watching) {
            return;
        }
        watching = true;
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable fall) {
                try {
                    write(app, thread, fall);
                    deliver(app);
                } catch (Throwable worse) {
                    // A fall while writing a fall: the system's own report still follows.
                }
                if (before != null) {
                    before.uncaughtException(thread, fall);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            }
        });
        new Thread(new Runnable() {
            public void run() {
                try {
                    deliver(app);
                } catch (Throwable none) {
                    // They wait for the next start.
                }
            }
        }).start();
    }

    private static void write(Context context, Thread thread, Throwable fall) throws Exception {
        File dir = new File(context.getFilesDir(), WAITING);
        dir.mkdirs();
        String stamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.ROOT).format(new Date());
        File file = new File(dir, stamp + ".txt");
        StringWriter text = new StringWriter();
        PrintWriter out = new PrintWriter(text);
        String version = "";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception unknown) {
            version = "?";
        }
        out.println("time      " + new Date());
        out.println("version   " + version);
        out.println("system    " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")");
        out.println("phone     " + Build.MANUFACTURER + " " + Build.MODEL);
        out.println("thread    " + thread.getName());
        out.println();
        fall.printStackTrace(out);
        out.flush();
        // The trace first, whole, before anything that could itself go wrong.
        OutputStream first = new FileOutputStream(file);
        try {
            first.write(text.toString().getBytes(StandardCharsets.UTF_8));
        } finally {
            first.close();
        }

        StringBuilder more = new StringBuilder();
        try {
            more.append("\n---- settings\n").append(Keep.export(context).toString(2)).append('\n');
        } catch (Throwable none) {
            more.append("\n---- settings unreadable\n");
        }
        more.append("\n---- log\n");
        try {
            java.lang.Process log = Runtime.getRuntime().exec(new String[] {"logcat", "-d", "-v", "time",
                "-t", String.valueOf(LOG_LINES), "--pid=" + android.os.Process.myPid()});
            InputStream in = log.getInputStream();
            byte[] chunk = new byte[8192];
            int total = 0;
            java.io.ByteArrayOutputStream read = new java.io.ByteArrayOutputStream();
            for (int n; (n = in.read(chunk)) > 0 && total < LOG_BYTES; total += n) {
                read.write(chunk, 0, n);
            }
            in.close();
            log.destroy();
            more.append(new String(read.toByteArray(), StandardCharsets.UTF_8));
        } catch (Throwable none) {
            more.append("unreadable\n");
        }
        OutputStream then = new FileOutputStream(file, true);
        try {
            then.write(more.toString().getBytes(StandardCharsets.UTF_8));
        } finally {
            then.close();
        }
    }

    /** Everything written and not yet carried out, carried out; each one kept until it is. */
    static synchronized void deliver(Context context) {
        File[] waiting = new File(context.getFilesDir(), WAITING).listFiles();
        if (waiting == null) {
            return;
        }
        for (File one : waiting) {
            if (carry(context, one)) {
                one.delete();
            }
        }
    }

    private static boolean carry(Context context, File file) {
        ContentResolver resolver = context.getContentResolver();
        String name = "ellipse-fault-" + file.getName();
        try {
            Uri target = null;
            Uri tree = folder(context);
            if (tree != null) {
                Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree,
                    DocumentsContract.getTreeDocumentId(tree));
                target = DocumentsContract.createDocument(resolver, parent, "text/plain", name);
            } else if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/" + context.getString(R.string.app_name));
                target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }
            if (target == null) {
                return false;
            }
            OutputStream out = resolver.openOutputStream(target);
            if (out == null) {
                return false;
            }
            InputStream in = new FileInputStream(file);
            try {
                byte[] chunk = new byte[8192];
                for (int n; (n = in.read(chunk)) > 0; ) {
                    out.write(chunk, 0, n);
                }
            } finally {
                in.close();
                out.close();
            }
            return true;
        } catch (Exception refused) {
            return false;
        }
    }

    /** The folder the owner gave, while this home screen may still write to it. */
    private static Uri folder(Context context) {
        String kept = Keep.folder(context);
        if (kept == null) {
            return null;
        }
        Uri tree = Uri.parse(kept);
        for (android.content.UriPermission held : context.getContentResolver().getPersistedUriPermissions()) {
            if (held.getUri().equals(tree) && held.isWritePermission()) {
                return tree;
            }
        }
        return null;
    }
}
