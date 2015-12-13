package de.oerntec.votenote.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;

import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBLastViewed;
import de.oerntec.votenote.Database.TableHelpers.DBSubjects;

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

    /**
     * Changes the app langugage
     *
     * @param context Context to adjust in
     * @return language code used or "default" if the setting was never changed
     */
    public static String adjustLanguage(Context context) {
        String languagePreference = PreferenceManager.getDefaultSharedPreferences(context).getString("language", "default");
        //no preference given
        if (!"default".equals(languagePreference)) {
            //language switch
            Resources res = context.getResources();
            // Change locale settings in the app.
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = new Locale(languagePreference.toLowerCase());
            res.updateConfiguration(conf, dm);
        }
        return languagePreference;
    }

    /**
     * Set up singletons for all databases
     * @param context
     */
    public static void setupDatabaseInstances(Context context){
        //database access
        DBSubjects.setupInstance(context);
        DBAdmissionCounters.setupInstance(context);
        DBAdmissionPercentageData.setupInstance(context);
        DBAdmissionPercentageMeta.setupInstance(context);
        DBLastViewed.setupInstance(context);
    }
}
