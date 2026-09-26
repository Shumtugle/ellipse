package io.github.shumtugle.ellipse;

import android.app.admin.DeviceAdminReceiver;

/**
 * The home screen as a keeper of the phone with a single power: to lock
 * it. Asked for only when the owner locks by a gesture this way rather
 * than through an accessibility service.
 */
public final class Warden extends DeviceAdminReceiver {
}
