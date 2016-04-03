package de.oerntec.votenote.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteConstraintException;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.DatabaseCreator;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionCounters;
import de.oerntec.votenote.database.tablehelpers.DBLastViewed;
import de.oerntec.votenote.database.tablehelpers.DBLessons;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;

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

    public static Locale getCurrentLocale(Context context) {
        String languagePreference = PreferenceManager.getDefaultSharedPreferences(context).getString("language", "default");
        if ("default".equals(languagePreference))
            return Locale.getDefault();
        else
            return new Locale(languagePreference.toLowerCase());
    }

    /**
     * Set up singletons for all databases
     */
    public static void setupDatabaseInstances(Context context){
        try {
            //database access
            DBSubjects.setupInstance(context);
            DBAdmissionCounters.setupInstance(context);
            DBLessons.setupInstance(context);
            DBPercentageTracker.setupInstance(context);
            DBLastViewed.setupInstance(context);
        } catch (SQLiteConstraintException e) {
            DatabaseCreator.logConstraintException(e);
            Toast.makeText(context, "Foreign key constraint violated!", Toast.LENGTH_LONG).show();
            throw e;
        }
    }

    public static void triStateClueColors(
            TextView textView,
            Context context,
            PercentageTrackerPojo meta,
            int addToFinished,
            int addToAvailable) {
        float average = meta.getAverageFinished(addToAvailable, addToFinished);
        //color text in
        if (!meta.bonusTargetPercentageEnabled) {
            if (average >= meta.baselineTargetPercentage)
                textView.setTextColor(ContextCompat.getColor(context, R.color.ok_green));
            else
                textView.setTextColor(ContextCompat.getColor(context, R.color.warning_red));
        } else {
            if (average < meta.baselineTargetPercentage)
                textView.setTextColor(ContextCompat.getColor(context, R.color.warning_red));
            else if (average < meta.bonusTargetPercentage)
                textView.setTextColor(ContextCompat.getColor(context, R.color.warning_orange));
            else
                textView.setTextColor(ContextCompat.getColor(context, R.color.ok_green));
        }
    }
}
