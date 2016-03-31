package de.oerntec.votenote.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 * Class designed to be used for requesting permissions on Android 6+
 */
public class Permissions {
    /**
     * Whether we have external write permissions granted
     *
     * @param context the context to act in
     * @return whether we are permitted to write external storage
     * @see #hasPermission(Context, String)
     * @see #hasExternalReadPermission(Context)
     * @see #hasRebootPermission(Context)
     */
    public static boolean hasExternalWritePermission(Context context) {
        return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * @param context the context to act in
     * @return whether we are permitted to receive an intent on reboot
     * @see #hasPermission(Context, String)
     * @see #hasExternalReadPermission(Context)
     * @see #hasExternalWritePermission(Context)
     */
    public static boolean hasRebootPermission(Context context) {
        return hasPermission(context, Manifest.permission.RECEIVE_BOOT_COMPLETED);
    }

    /**
     * @param context the context to act in
     * @return whether we are permitted to read external storage
     * @see #hasPermission(Context, String)
     * @see #hasExternalWritePermission(Context)
     * @see #hasRebootPermission(Context)
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean hasExternalReadPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return true;
        return hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    /**
     * Checks whether an operation is permitted
     *
     * @param context    the context to act in
     * @param permission the operation to check for
     * @return whether the operation is permitted
     * @see #hasExternalReadPermission(Context)
     * @see #hasExternalWritePermission(Context)
     * @see #hasRebootPermission(Context)
     */
    private static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
