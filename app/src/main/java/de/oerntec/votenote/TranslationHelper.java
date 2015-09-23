/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
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
