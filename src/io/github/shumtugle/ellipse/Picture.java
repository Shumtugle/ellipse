package io.github.shumtugle.ellipse;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The owner's own picture as the wallpaper. The phone does not let a home
 * screen read the wallpaper it has without leave to read every file, so a
 * picture of the owner's own is brought in here instead: kept by the home
 * screen, set again at a touch after any other ground, and carried in
 * copies, so that no ground drawn later can take it away for good.
 */
final class Picture {

    /** The longer side a kept picture is brought down to, enough for any phone's screen. */
    private static final int MOST = 3840;

    private Picture() {
    }

    /** Where the picture is kept, among the home screen's own files. */
    static File file(Context context) {
        File room = new File(context.getFilesDir(), "walls");
        room.mkdirs();
        return new File(room, "own.jpg");
    }

    static boolean kept(Context context) {
        return file(context).isFile();
    }

    /**
     * A chosen picture read and kept: turned upright as the camera meant it,
     * brought down to a size a screen can use, and written as a photograph.
     */
    static boolean keep(Context context, Uri from) {
        try {
            BitmapFactory.Options size = new BitmapFactory.Options();
            size.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(from)) {
                BitmapFactory.decodeStream(in, null, size);
            }
            if (size.outWidth <= 0 || size.outHeight <= 0) {
                return false;
            }
            int longer = Math.max(size.outWidth, size.outHeight);
            BitmapFactory.Options read = new BitmapFactory.Options();
            read.inSampleSize = 1;
            while (longer / (read.inSampleSize * 2) >= MOST) {
                read.inSampleSize *= 2;
            }
            Bitmap picture;
            try (InputStream in = context.getContentResolver().openInputStream(from)) {
                picture = BitmapFactory.decodeStream(in, null, read);
            }
            if (picture == null) {
                return false;
            }
            float scale = Math.min(1f, MOST / (float) Math.max(picture.getWidth(), picture.getHeight()));
            Matrix turn = new Matrix();
            turn.postRotate(upright(context, from));
            turn.postScale(scale, scale);
            Bitmap made = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), turn, true);
            File out = file(context);
            File fresh = new File(out.getParentFile(), "own.part");
            try (OutputStream write = new FileOutputStream(fresh)) {
                made.compress(Bitmap.CompressFormat.JPEG, 92, write);
            }
            if (made != picture) {
                made.recycle();
            }
            picture.recycle();
            return fresh.renameTo(out);
        } catch (IOException | RuntimeException | OutOfMemoryError unread) {
            return false;
        }
    }

    /** How far the picture is to be turned to stand as it was taken, in degrees. */
    private static int upright(Context context, Uri from) {
        try (InputStream in = context.getContentResolver().openInputStream(from)) {
            if (in == null) {
                return 0;
            }
            android.media.ExifInterface said = new android.media.ExifInterface(in);
            int way = said.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL);
            return way == android.media.ExifInterface.ORIENTATION_ROTATE_90 ? 90
                : way == android.media.ExifInterface.ORIENTATION_ROTATE_180 ? 180
                : way == android.media.ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
        } catch (IOException | RuntimeException unread) {
            return 0;
        }
    }

    /** The kept picture set: on the home screen, and on the lock screen unless the owner keeps it. */
    static void set(Context context) throws IOException {
        int where = WallpaperManager.FLAG_SYSTEM;
        if (Keep.flag(context, Keep.GROUND_LOCK, true)) {
            where |= WallpaperManager.FLAG_LOCK;
        }
        Keep.saveClock(context, Keep.GROUND_SET_AT, System.currentTimeMillis());
        try (InputStream in = new FileInputStream(file(context))) {
            WallpaperManager.getInstance(context).setStream(in, null, true, where);
        }
        Keep.saveFlag(context, Keep.GROUND_WORN, false);
        Keep.saveFlag(context, Keep.PICTURE_WORN, true);
    }

    /**
     * Whether a ground may be set without asking: the wallpaper now is the
     * home screen's own, or its owner has said once that it may go.
     */
    static boolean replaceable(Context context) {
        return Keep.flag(context, Keep.WALL_WARNED, false) || Keep.flag(context, Keep.GROUND_WORN, false)
            || Keep.flag(context, Keep.PICTURE_WORN, false);
    }
}
