package de.oerntec.votenote.AdmissionPercentageOverview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.R;

public class AdmissionPercentageOverviewActivity extends AppCompatActivity {
    private AdmissionPercentageOverviewFragment mContentFragment;
    private int mAdmissionPercentageId;

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
        mAdmissionPercentageId = getIntent().getExtras().getInt(AdmissionPercentageOverviewFragment.PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID);

        //instantiate and apply fragment
        mContentFragment = AdmissionPercentageOverviewFragment.newInstance(mAdmissionPercentageId);
        getSupportFragmentManager().
                beginTransaction().
                add(R.id.admission_percentage_overview_fragment_container, mContentFragment)
                .commit();
    }

    void setToolbarTitle(String title) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
        else
            throw new AssertionError("no toolbar found?");
    }
}
