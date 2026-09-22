package io.github.shumtugle.ellipse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Finished tiles, kept.
 *
 * Tiles are drawn on one quiet thread and handed to the screen when ready;
 * until then a view shows an empty tile of the same size, so nothing
 * jumps when the icon arrives. A view that was given to another
 * application in the meantime, as happens in a scrolled list, is not
 * given the old one's picture: each view remembers what it last asked for.
 *
 * Memory for tiles is a sixth of what the application may use. When it is
 * full, the tiles longest unseen are let go and drawn again if needed.
 */
final class Icons {

    /** Where an icon comes from, asked for on the quiet thread. */
    interface Source {
        Drawable icon();
    }

    private final int dpi;
    private final Tile.Look look;
    /**
     * Kept for the whole of the application's life, not for one home
     * screen's: built anew — after a setting, or a turn of the phone — the
     * tiles are there already, and nothing blinks while they are drawn.
     */
    private static LruCache<String, Bitmap> kept;
    /** Two hands rather than one: a screenful of tiles comes twice as fast. */
    private final ExecutorService hand = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private Bitmap empty;

    Icons(Context context, Tile.Look look) {
        this.look = look;
        dpi = App.dpi(context);
        if (kept == null) {
            int budget = (int) (Runtime.getRuntime().maxMemory() / 1024L / 6L);
            kept = new LruCache<String, Bitmap>(budget) {
                @Override
                protected int sizeOf(String key, Bitmap tile) {
                    return tile.getByteCount() / 1024;
                }
            };
        }
    }

    /** An application's tile into a view. */
    void put(ImageView view, final App app, int width) {
        put(view, app.key, width, new Source() {
            public Drawable icon() {
                return app.icon(dpi);
            }
        });
    }

    /** Any icon's tile into a view, under a name of the caller's choosing. */
    void put(final ImageView view, String name, final int width, final Source source) {
        final String key = name + "@" + width + "@" + look.key();
        view.setTag(key);
        Bitmap ready = kept.get(key);
        if (ready != null) {
            view.setImageBitmap(ready);
            return;
        }
        if (empty == null || empty.getWidth() != width) {
            empty = Tile.render(null, width, look);
        }
        view.setImageBitmap(empty);
        hand.execute(new Runnable() {
            public void run() {
                Drawable icon;
                try {
                    icon = source.icon();
                } catch (RuntimeException gone) {
                    // The application left while its turn was coming.
                    icon = null;
                }
                final Bitmap tile = Tile.render(icon, width, look);
                main.post(new Runnable() {
                    public void run() {
                        kept.put(key, tile);
                        if (key.equals(view.getTag())) {
                            view.setImageBitmap(tile);
                        }
                    }
                });
            }
        });
    }

    /** Lets go of every tile of one package, after it changed or left. */
    void forget(String pkg) {
        String prefix = pkg + "/";
        for (Map.Entry<String, Bitmap> entry : kept.snapshot().entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                kept.remove(entry.getKey());
            }
        }
    }

    Tile.Look look() {
        return look;
    }

    void close() {
        hand.shutdownNow();
    }
}
