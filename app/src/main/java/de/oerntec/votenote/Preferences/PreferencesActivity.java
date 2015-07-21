package de.oerntec.votenote.Preferences;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.oerntec.votenote.R;


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

    public void onVersionResult(String result) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(this.getString(R.string.versio_check_title));
        String message;
        boolean isNewest = "1.2.3".equals(result);
        if (isNewest)
            message = this.getString(R.string.version_check_success_message);
        else
            message = this.getString(R.string.version_check_fail_message);
        b.setMessage(message);
        b.setPositiveButton(this.getString(R.string.dialog_button_ok), null);
        if (!isNewest) {
            b.setNeutralButton("Download", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.dropbox.com/s/9a4kpufy1mcalpf/VoteNote_latest.apk?dl=1"));
                    startActivity(browserIntent);
                }
            });
        }
        b.show();
    }

    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.new_preferences);
        }
    }
}
