package de.oerntec.votenote.SubjectManagerStuff;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.oerntec.votenote.R;

public class SubjectCreationActivity extends AppCompatActivity {
    public static final String SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME = "subjectId";

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //load fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.activity_subject_creation_fragment_container,
                        SubjectCreationActivityFragment.newInstance(getIntent().getExtras().getInt(SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, -1))).commit();
    }

}
