package io.github.shumtugle.ellipse;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Files handed to another app: a copy sent to a cloud, a mail or a chat.
 * Each is written into a room of its own among the application's caches,
 * and offered by name, for reading only, to whichever app it is sent to.
 */
public final class Handed extends ContentProvider {

    private static final String ROOM = "handed";

    /** The name the files are offered under, the application's own. */
    static String authority(Context context) {
        return context.getPackageName() + ".handed";
    }

    /** Words written into a file of a name and offered to be sent, through the phone's own sheet. */
    static Intent send(Context context, String name, String words, String type) throws IOException {
        File room = new File(context.getCacheDir(), ROOM);
        File[] old = room.listFiles();
        if (old != null) {
            for (File one : old) {
                one.delete();
            }
        }
        room.mkdirs();
        File file = new File(room, name);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(words.getBytes(StandardCharsets.UTF_8));
        }
        Uri uri = new Uri.Builder().scheme("content").authority(authority(context)).appendPath(name).build();
        Intent send = new Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(android.content.ClipData.newRawUri(name, uri));
        return Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private File fileOf(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("..")) {
            throw new FileNotFoundException();
        }
        File file = new File(new File(getContext().getCacheDir(), ROOM), name);
        if (!file.isFile()) {
            throw new FileNotFoundException();
        }
        return file;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileOf(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        try {
            File file = fileOf(uri);
            MatrixCursor made = new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
            made.addRow(new Object[] {file.getName(), file.length()});
            return made;
        } catch (FileNotFoundException gone) {
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        return name != null && name.endsWith(".json") ? "application/json" : "text/plain";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
