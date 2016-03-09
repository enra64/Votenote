package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment.AdmissionPercentageCreation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.R;

public class AdmissionPercentageCreationActivity extends AppCompatActivity {
    public static final int RESULT_REQUEST_CODE_ADMISSION_PERCENTAGE_CREATOR = 123;
    private AdmissionPercentageFragment mContentFragment;
    private int mSubjectId, mAdmissionPercentageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admission_percentage_creation);

        //avoid overlapping fragments
        if (savedInstanceState != null)
            return;

        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.subject_creation_activity_toolbar_toolbar);
        //if we can not get our toolbar, its rip time anyways
        setSupportActionBar(toolbar);

        //force the language
        General.adjustLanguage(this);

        //get arguments
        mSubjectId = getIntent().getExtras().getInt(AdmissionPercentageFragment.SUBJECT_ID);
        mAdmissionPercentageId = getIntent().getExtras().getInt(AdmissionPercentageFragment.ADMISSION_PERCENTAGE_ID);
        boolean isNew = getIntent().getExtras().getBoolean(AdmissionPercentageFragment.SUBJECT_IS_NEW);

        //instantiate and apply fragment
        mContentFragment = AdmissionPercentageFragment.newInstance(mSubjectId, mAdmissionPercentageId, isNew);
        getSupportFragmentManager().
                beginTransaction().
                add(R.id.admission_percentage_creation_fragment_container, mContentFragment)
                .commit();

        //set fragment to be the listener for our buttons
        findViewById(R.id.activity_admission_percentage_creation_save_button).setOnClickListener(mContentFragment);
        findViewById(R.id.activity_admission_percentage_creation_cancel_button).setOnClickListener(mContentFragment);
        findViewById(R.id.activity_admission_percentage_creation_delete_button).setOnClickListener(mContentFragment);
    }

    void hideDeleteButton() {
        findViewById(R.id.activity_admission_percentage_creation_delete_button).setVisibility(View.GONE);
    }

    void setToolbarTitle(String title) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
        else
            throw new AssertionError("no toolbar found?");
    }

    Button getSaveButton() {
        return (Button) findViewById(R.id.activity_admission_percentage_creation_save_button);
    }

    public void callCreatorFragmentForItemChange(int resultState) {
        setResult(resultState, getCurrentResultIntent());
        finish();
    }

    private Intent getCurrentResultIntent() {
        //save result data into intent
        Intent returnIntent = new Intent();
        returnIntent.putExtra(AdmissionPercentageFragment.SUBJECT_ID, mSubjectId);
        returnIntent.putExtra(AdmissionPercentageFragment.ADMISSION_PERCENTAGE_ID, mAdmissionPercentageId);
        return returnIntent;
    }

}
