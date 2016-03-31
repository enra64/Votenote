package de.oerntec.votenote.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * Class designed to be used for requesting permissions on Android 6+
 */
public class Permissions {
    public static final int MY_PERMISSION_REQUEST_CODE_EXTERNAL_READ = 0;
    public static final int MY_PERMISSION_REQUEST_CODE_EXTERNAL_WRITE = 1;
    public static final int MY_PERMISSION_REQUEST_CODE_REBOOT_RECEIVER = 2;

    /**
     * Request a permission to be granted
     */
    public static void requestPermission(Activity activity, String permission) {
        // Here, thisActivity is the current activity
        if (!hasPermission(activity, permission)) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)) {
                //show explanation
                Toast.makeText(activity, getExplanation(permission), Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{permission},
                        getRequestCode(permission));
            }
        }
    }

    /**
     * Get my corresponding request code for the permission
     *
     * @param permission one of the three permission strings requestable for votenote
     * @return one of {@link #MY_PERMISSION_REQUEST_CODE_EXTERNAL_READ},
     * {@link #MY_PERMISSION_REQUEST_CODE_EXTERNAL_WRITE},
     * {@link #MY_PERMISSION_REQUEST_CODE_REBOOT_RECEIVER},
     */
    public static int getRequestCode(String permission) {
        switch (permission) {
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return MY_PERMISSION_REQUEST_CODE_EXTERNAL_READ;
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return MY_PERMISSION_REQUEST_CODE_EXTERNAL_WRITE;
            case Manifest.permission.RECEIVE_BOOT_COMPLETED:
                return MY_PERMISSION_REQUEST_CODE_REBOOT_RECEIVER;
            default:
                throw new AssertionError("this permission was not requested!");
        }
    }

    /**
     * Get an explanation as to why a specific permission is needed
     *
     * @param permission one of the three permission strings requestable for votenote
     */
    private static String getExplanation(String permission) {
        switch (permission) {
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Diese Berechtigung wird benötigt, um Backups laden zu können";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "Diese Berechtigung wird benötigt, um Backups speichern zu können, und Logs schreiben zu können";
            case Manifest.permission.RECEIVE_BOOT_COMPLETED:
                return "Diese Berechtigung wird benötigt, um nach einem Neustart die Erinnerungen wiederherzustellen";
            default:
                throw new AssertionError("this permission was not requested!");
        }
    }

    /**
     * Checks whether an operation is permitted
     *
     * @param context    the context to act in
     * @param permission the operation to check for
     * @return whether the operation is permitted
     */
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
