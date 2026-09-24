package io.github.shumtugle.ellipse;

import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.HashSet;
import java.util.Set;

/**
 * Which apps have something to say: the phone tells this listener of every
 * notification once the owner gives it that access, and it keeps no more
 * than the names of the apps that have one standing, for a dot on their
 * icons. What the notifications say is never read.
 */
public final class Notices extends NotificationListenerService {

    private static final Set<String> marked = new HashSet<>();
    private static Runnable told;
    private static boolean on;

    /** Whether the listener is at work now. */
    static boolean on() {
        return on;
    }

    /** The apps with a notice standing, each as its package and, after an at sign, its profile. */
    static synchronized Set<String> marked() {
        return new HashSet<>(marked);
    }

    /** Whom to tell when the marks change; on the main thread. */
    static void tell(Runnable whom) {
        told = whom;
    }

    static String mark(String owner, long serial) {
        return serial == 0L ? owner : owner + "@" + serial;
    }

    @Override
    public void onListenerConnected() {
        on = true;
        count();
    }

    @Override
    public void onListenerDisconnected() {
        on = false;
        synchronized (Notices.class) {
            marked.clear();
        }
        say();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        count();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        count();
    }

    /** Counts again from what stands: ongoing work and group summaries do not earn a dot. */
    private void count() {
        Set<String> now = new HashSet<>();
        try {
            StatusBarNotification[] standing = getActiveNotifications();
            if (standing != null) {
                for (StatusBarNotification one : standing) {
                    Notification it = one.getNotification();
                    if ((it.flags & Notification.FLAG_ONGOING_EVENT) != 0
                        || (it.flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
                        continue;
                    }
                    now.add(mark(one.getPackageName(), Apps.serialOf(one.getUser())));
                }
            }
        } catch (RuntimeException refused) {
            return;
        }
        synchronized (Notices.class) {
            if (now.equals(marked)) {
                return;
            }
            marked.clear();
            marked.addAll(now);
        }
        say();
    }

    private static void say() {
        final Runnable whom = told;
        if (whom != null) {
            new Handler(Looper.getMainLooper()).post(whom);
        }
    }
}
