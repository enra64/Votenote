package de.oerntec.votenote.Helpers;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * General static helper function class
 */
public class General {
    /**
     * yeah so uuh fuck android keyboard management
     *
     * @param activity must be called from an activity
     *                 https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard/17789187#17789187
     */
    public static void nukeKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * yeah so uuh fuck android keyboard management
     * https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard/17789187#17789187
     */
    public static void nukeKeyboard(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
