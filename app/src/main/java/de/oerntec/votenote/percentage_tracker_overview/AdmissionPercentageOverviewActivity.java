package de.oerntec.votenote.percentage_tracker_overview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.oerntec.votenote.R;
import de.oerntec.votenote.helpers.General;

public class AdmissionPercentageOverviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admission_percentage_overview);

        //avoid overlapping fragments
        if (savedInstanceState != null)
            return;

        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_admission_percentage_overview_toolbar);
        //if we can not get our toolbar, its rip time anyways
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //force the language
        General.adjustLanguage(this);

        //get arguments
        int mAdmissionPercentageId = getIntent().getExtras().getInt(AdmissionPercentageOverviewFragment.PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID);

        //instantiate and apply fragment
        AdmissionPercentageOverviewFragment mContentFragment =
                AdmissionPercentageOverviewFragment.newInstance(mAdmissionPercentageId);
        getSupportFragmentManager().
                beginTransaction().
                add(R.id.admission_percentage_overview_fragment_container, mContentFragment)
                .commit();
    }
}
