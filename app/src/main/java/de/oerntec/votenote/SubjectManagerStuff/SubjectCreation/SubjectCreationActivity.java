package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment.SubjectCreationFragment;

public class SubjectCreationActivity extends AppCompatActivity {
    public static final int DIALOG_RESULT_CLOSED = 0;
    public static final int DIALOG_RESULT_CHANGED = 1;
    public static final int DIALOG_RESULT_ADDED = 2;
    public static final int DIALOG_RESULT_DELETE = 3;

    /**
     * argument in intent bundle containing which id the fragment is for
     */
    public static final String ARG_CREATOR_SUBJECT_ID = "subjectId";

    /**
     * argument in intent bundle containing which position the subject was at in the recyclerview
     * of subjectmanagementactivity
     */
    public static final String ARG_CREATOR_VIEW_POSITION = "recyclerViewPosition";

    /**
     * save the current fragment here to avoid lookups
     */
    private static SubjectCreationFragment currentSubjectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_creation);

        General.adjustLanguage(this);
        General.setupDatabaseInstances(getApplicationContext());

        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.subject_creation_activity_toolbar);
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        int subjectId = getIntent().getExtras().getInt(ARG_CREATOR_SUBJECT_ID, -1);
        int subjectPosition = getIntent().getExtras().getInt(ARG_CREATOR_VIEW_POSITION, -1);

        currentSubjectFragment = SubjectCreationFragment.newInstance(subjectId, subjectPosition);

        //load fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.activity_subject_creation_fragment_container, currentSubjectFragment).commit();
    }

    //double check available db instances, maybe the activity wont get created from scratch
    @Override
    protected void onStart() {
        super.onStart();
        General.setupDatabaseInstances(getApplicationContext());
    }

    public void callCreatorFragmentForItemChange(int itemId, boolean isPercentage, int state) {
        SubjectCreationFragment creator = (SubjectCreationFragment) getFragmentManager().findFragmentById(R.id.activity_subject_creation_fragment_container);
        switch (state){
            case DIALOG_RESULT_ADDED:
                if (isPercentage)
                    creator.admissionPercentageFinished(itemId, true);
                else
                    creator.admissionCounterFinished(itemId, true);
                break;
            case DIALOG_RESULT_CHANGED:
                if (isPercentage)
                    creator.admissionPercentageFinished(itemId, false);
                else
                    creator.admissionCounterFinished(itemId, false);
                break;
            case DIALOG_RESULT_DELETE:
                if (isPercentage)
                    creator.deleteAdmissionPercentage(itemId);
                else
                    creator.deleteAdmissionCounter(itemId);
                break;
            case DIALOG_RESULT_CLOSED:
                creator.dialogClosed();
        }
    }

    @Override
    protected void onPause() {
        //holy shit hide the fucking keyboard
        General.nukeKeyboard(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (currentSubjectFragment != null)
            currentSubjectFragment.onBackPressed();
    }
}