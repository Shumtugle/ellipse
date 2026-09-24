package io.github.shumtugle.ellipse;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

/**
 * The one thing the home screen may not do by itself: lock the phone. The
 * phone allows it to an accessibility service the owner has turned on;
 * this one reads nothing and listens to nothing, and only locks when the
 * home screen asks.
 */
public final class Latch extends AccessibilityService {

    private static Latch running;

    /** Whether the owner has turned the lock on. */
    static boolean ready() {
        return running != null && Build.VERSION.SDK_INT >= 28;
    }

    /** Locks the phone; false when it cannot. */
    static boolean lock() {
        if (!ready()) {
            return false;
        }
        return running.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        running = this;
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        running = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        running = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
