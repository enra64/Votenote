package de.oerntec.votenote;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import java.util.Locale;

public class TranslationHelper {
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
}
