package io.github.shumtugle.ellipse;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.view.KeyEvent;

import java.util.List;

/**
 * Whatever plays on the phone now, for the player's ring: its title, how far
 * it has played, whether it plays. The phone shows it to the home screen only
 * while the home screen may read notifications, as it may for the dots on
 * icons; without that, nothing is known, and the ring still pauses and goes on.
 */
final class Playing {

    final String title;
    final long position;
    final long length;
    final boolean playing;

    private Playing(String title, long position, long length, boolean playing) {
        this.title = title;
        this.position = position;
        this.length = length;
        this.playing = playing;
    }

    /** What plays now, or none if nothing does or the phone does not say. */
    static Playing now(Context context) {
        try {
            MediaSessionManager sessions = (MediaSessionManager) context.getSystemService(
                Context.MEDIA_SESSION_SERVICE);
            if (sessions == null) {
                return null;
            }
            List<MediaController> all = sessions.getActiveSessions(new ComponentName(context, Notices.class));
            if (all == null || all.isEmpty()) {
                return null;
            }
            MediaController first = all.get(0);
            for (MediaController one : all) {
                PlaybackState state = one.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    first = one;
                    break;
                }
            }
            PlaybackState state = first.getPlaybackState();
            MediaMetadata about = first.getMetadata();
            String title = about == null ? null : about.getString(MediaMetadata.METADATA_KEY_TITLE);
            long length = about == null ? 0L : about.getLong(MediaMetadata.METADATA_KEY_DURATION);
            boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
            long position = 0L;
            if (state != null) {
                position = state.getPosition();
                if (playing) {
                    /* Where it is now: from where it was said to be, as far on as time has gone since. */
                    long since = SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime();
                    position += (long) (since * state.getPlaybackSpeed());
                }
            }
            return new Playing(title, Math.max(0L, position), Math.max(0L, length), playing);
        } catch (RuntimeException unseen) {
            return null;
        }
    }

    /** Pause, or go on: the key a headset presses, sent to whatever plays. */
    static void toggle(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        long when = SystemClock.uptimeMillis();
        audio.dispatchMediaKeyEvent(new KeyEvent(when, when, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
        audio.dispatchMediaKeyEvent(new KeyEvent(when, when, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
    }
}
