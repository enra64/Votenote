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
package de.oerntec.votenote.preferences;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.Toast;

import de.oerntec.votenote.R;
import de.oerntec.votenote.helpers.General;
import de.oerntec.votenote.helpers.Permissions;


public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        setSupportActionBar((Toolbar) findViewById(R.id.activity_preferences_toolbar));

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        else
            throw new AssertionError("are you happy now, AS? found no action bar btw");

        //try to load already saved language choice
        General.adjustLanguage(this);

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
            String languagePreference = General.adjustLanguage(getActivity());

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
                    b.setTitle(R.string.used_libraries_title);
                    b.setPositiveButton(R.string.dialog_button_ok, null);
                    //noinspection RedundantCast because we cant yet know the parent for the dialog
                    b.setView(getActivity().getLayoutInflater().inflate(R.layout.preferences_thanks, (ViewGroup) null));
                    b.show();
                    return true;
                }
            });

            //permissions...
            Preference.OnPreferenceChangeListener writeExternalStorageRequestListener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!Permissions.hasPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        Permissions.requestPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return false;
                }
            };

            findPreference("csv_export").setOnPreferenceChangeListener(writeExternalStorageRequestListener);
            findPreference("backup_export").setOnPreferenceChangeListener(writeExternalStorageRequestListener);
            findPreference("enable_logging").setOnPreferenceChangeListener(writeExternalStorageRequestListener);

            findPreference("backup_import").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        if (!Permissions.hasPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE))
                            Permissions.requestPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                    return false;
                }
            });

            findPreference("show_eula").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //show a dialog with graphview and stackoverflow
                    AlertDialog.Builder eulaBuilder = new AlertDialog.Builder(getActivity());
                    eulaBuilder.setCancelable(false);
                    eulaBuilder.setTitle(R.string.eula_dialog_title);
                    //noinspection RedundantCast because we cant yet know the parent for the dialog
                    eulaBuilder.setView(getActivity().getLayoutInflater().inflate(R.layout.preferences_eula, (ViewGroup) null));
                    eulaBuilder.setPositiveButton(R.string.dialog_button_ok, null);
                    eulaBuilder.show();
                    return true;
                }
            });
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
            if (requestCode == Permissions.getRequestCode(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(getActivity(), R.string.permissions_ext_write_success, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), R.string.permissions_ext_write_failure, Toast.LENGTH_LONG).show();
            } else if (requestCode == Permissions.getRequestCode(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(getActivity(), R.string.permissions_ext_read_success, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), R.string.permissions_ext_read_failure, Toast.LENGTH_LONG).show();
            }
        }
    }
}
