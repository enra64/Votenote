package de.oerntec.votenote.preferences;

import android.content.Context;
import android.preference.PreferenceManager;

public class Preferences {
    public static boolean getPreference(Context context, String key, boolean defaultResult) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultResult);
    }
}
