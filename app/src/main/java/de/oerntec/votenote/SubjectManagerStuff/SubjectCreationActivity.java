package de.oerntec.votenote.SubjectManagerStuff;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.R;

public class SubjectCreationActivity extends AppCompatActivity {
    /**
     * argument in intent bundle containing which id the fragment is for
     */
    public static final String SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME = "subjectId";

    /**
     * argument in intent bundle containing which position the subject was at in the recyclerview
     * of subjectmanagementactivity
     */
    public static final String SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME = "recyclerViewPosition";

    /**
     * save the current fragment here to avoid lookups
     */
    private static SubjectCreationActivityFragment currentSubjectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_creation);

        //set action bar to toolbar
        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.subject_creation_activity_toolbar);
        //if we can not get our toolbar, its rip time anyways
        setSupportActionBar(toolbar);

        //are fucked anyway if this does not work, rather crash now
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        int subjectId = getIntent().getExtras().getInt(SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, -1);
        int subjectPosition = getIntent().getExtras().getInt(SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME, -1);

        currentSubjectFragment = SubjectCreationActivityFragment.newInstance(subjectId, subjectPosition);

        //load fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.activity_subject_creation_fragment_container, currentSubjectFragment).commit();
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
