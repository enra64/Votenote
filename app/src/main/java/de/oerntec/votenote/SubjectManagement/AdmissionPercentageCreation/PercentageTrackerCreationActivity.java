package de.oerntec.votenote.SubjectManagement.AdmissionPercentageCreation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.R;

public class PercentageTrackerCreationActivity extends AppCompatActivity {
    public static final int INTENT_REQUEST_CODE_ADMISSION_PERCENTAGE_CREATOR = 123;
    private int mSubjectId, mAdmissionPercentageId;


    /**
     * If the corresponding CheckBox is set in the fragment, it will set this value appropriately
     * to signal that the SubjectCreatorActivity needs to generate a notification
     */
    private String mRecurrenceString = null;

    @SuppressWarnings("ConstantConditions")//i dont have error handling for that shit
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //force the language
        General.adjustLanguage(this);

        setContentView(R.layout.activity_admission_percentage_creation);

        //avoid overlapping fragments
        if (savedInstanceState != null)
            return;

        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.subject_creation_activity_toolbar_toolbar);
        //if we can not get our toolbar, its rip time anyways
        setSupportActionBar(toolbar);

        //get arguments
        mSubjectId = getIntent().getExtras().getInt(PercentageTrackerCreationFragment.SUBJECT_ID);
        mAdmissionPercentageId = getIntent().getExtras().getInt(PercentageTrackerCreationFragment.ADMISSION_PERCENTAGE_ID);
        boolean isNew = getIntent().getExtras().getBoolean(PercentageTrackerCreationFragment.SUBJECT_IS_NEW);

        //instantiate and apply fragment
        PercentageTrackerCreationFragment mContentFragment =
                PercentageTrackerCreationFragment.newInstance(mSubjectId, mAdmissionPercentageId, isNew);
        getSupportFragmentManager().
                beginTransaction().
                add(R.id.admission_percentage_creation_fragment_container, mContentFragment)
                .commit();

        //set fragment to be the listener for our buttons
        findViewById(R.id.giant_ok_button).setOnClickListener(mContentFragment);
        findViewById(R.id.giant_cancel_button).setOnClickListener(mContentFragment);
        findViewById(R.id.giant_delete_button).setOnClickListener(mContentFragment);
    }

    void setRecurrenceString(String recurrence) {
        mRecurrenceString = recurrence;
    }

    @SuppressWarnings("ConstantConditions")
    void hideDeleteButton() {
        findViewById(R.id.giant_delete_button).setVisibility(View.GONE);
    }

    void setToolbarTitle(String title) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
        else
            throw new AssertionError("no toolbar found?");
    }

    Button getSaveButton() {
        return (Button) findViewById(R.id.giant_ok_button);
    }

    void finishToSubjectCreator(int resultState) {
        setResult(resultState, getCurrentResultIntent());
        finish();
    }

    private Intent getCurrentResultIntent() {
        //save result data into intent
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PercentageTrackerCreationFragment.SUBJECT_ID, mSubjectId);
        returnIntent.putExtra(PercentageTrackerCreationFragment.ADMISSION_PERCENTAGE_ID, mAdmissionPercentageId);
        if (mRecurrenceString != null)
            returnIntent.putExtra(PercentageTrackerCreationFragment.RECURRENCE_STRING, mRecurrenceString);
        return returnIntent;
    }

}
