/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.Preferences;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import de.oerntec.votenote.R;
import de.oerntec.votenote.TranslationHelper;


public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        setSupportActionBar((Toolbar) findViewById(R.id.activity_preferences_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.replace(R.id.activity_preferences_container, new PrefsFragment());
        mFragmentTransaction.commit();
    }

    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            findPreference("source_link").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/enra64/Votenote")));
                    return true;
                }
            });

            //try to load already saved language choice
            String languagePreference = TranslationHelper.adjustLanguage(getActivity());

            //no preference given yet; fall back to default
            if ("default".equals(languagePreference))
                languagePreference = getResources().getConfiguration().locale.getLanguage();

            //set description according to current locale
            if ("de".equals(languagePreference))
                findPreference("language").setSummary("Deutsch");
            else
                findPreference("language").setSummary("English");

            //reload activity after changing the language
            findPreference("language").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Toast.makeText(getActivity(), R.string.preferences_language_please_restart, Toast.LENGTH_SHORT).show();
                    //load translation change
                    getActivity().recreate();
                    return true;
                }
            });

            //build dialogs here because we have no acess to a layoutinflater on api<21
            findPreference("used_libraries").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //show a dialog with graphview and stackoverflow
                    AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                    b.setTitle("Hilfen");
                    b.setPositiveButton("OK", null);
                    b.setView(getActivity().getLayoutInflater().inflate(R.layout.preferences_thanks, null));
                    b.show();
                    return true;
                }
            });

            findPreference("used_libraries").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //show a dialog with graphview and stackoverflow
                    AlertDialog.Builder eulaBuilder = new AlertDialog.Builder(getActivity());
                    eulaBuilder.setCancelable(false);
                    eulaBuilder.setTitle("End-User License Agreement for Votenote");
                    eulaBuilder.setView(getActivity().getLayoutInflater().inflate(R.layout.preferences_eula, null));
                    eulaBuilder.setPositiveButton("OK", null);
                    eulaBuilder.show();
                    return true;
                }
            });
        }
    }
}
