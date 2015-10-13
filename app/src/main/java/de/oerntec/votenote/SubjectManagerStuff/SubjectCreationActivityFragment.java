package de.oerntec.votenote.SubjectManagerStuff;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class SubjectCreationActivityFragment extends Fragment {
    /**
     * Lesson should be added, not changed
     */
    public static final int ADD_SUBJECT_CODE = -1;
    /**
     * DB for the subjects
     */
    private final DBSubjects mSubjectDb = DBSubjects.getInstance();
    private EditText nameInput;
    private TextView voteInfo, presInfo, estimatedAssignmentsHelp, estimatedUebungCountHelp;
    private SeekBar minVoteSeek, wantedPresentationPointsSeekbar, estimatedAssignmentsSeek, estimatedUebungCountSeek;
    /**
     * toolbar to avoid constantly getting it
     */
    private ActionBar mActionBar;
    //load default values into variables
    private String nameHint;
    private int presentationPointsHint = 0;
    private int minimumVotePercentageHint = 50;
    private int scheduledAssignmentsPerLesson = 5;
    private int scheduledNumberOfLessons = 10;
    /**
     * Database id of current subject
     */
    private int mDatabaseId;
    /**
     * convenience info about whether current subject is old or new
     */
    private boolean mIsOldSubject, mIsNewSubject;
    /**
     * Old subject data
     */
    private Subject mOldSubjectData;

    public SubjectCreationActivityFragment() {
    }

    public static SubjectCreationActivityFragment newInstance(int subjectId) {
        Bundle args = new Bundle();
        args.putInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, subjectId);

        SubjectCreationActivityFragment fragment = new SubjectCreationActivityFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        nameHint = getString(R.string.subject_add_hint);
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabaseId = getArguments().getInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, -1);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_subject_creation, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View input = inflater.inflate(R.layout.subject_manager_dialog_groupsettings, container, false);

        nameInput = (EditText) input.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);

        voteInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_min_votierungs);
        minVoteSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_min_votierungs);

        presInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_prespoints);
        wantedPresentationPointsSeekbar = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_prespoints);

        estimatedAssignmentsHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_assignments_per_uebung);
        estimatedAssignmentsSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_assignments_per_uebung);

        estimatedUebungCountHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_estimated_uebung_count);
        estimatedUebungCountSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_estimated_uebung_count);

        return input;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadValues();
        insertValues();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                showBackConfirmation();
                return true;
            case R.id.action_delete:
                deleteCurrentSubject();
                return true;
            case R.id.action_save:
                saveData();
                return true;
            case R.id.action_abort:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentSubject() {
        //uuh dat mvp tho
        Toast.makeText(getActivity(), "cant delete yet", Toast.LENGTH_SHORT).show();
        //SwipeDeletion.executeProgrammaticSwipeDeletion(SubjectManagementActivity.this, SubjectManagementActivity.this, animatedView);
    }

    /**
     * Show a confirmation dialog to ask the user whether he really wants to leave
     */
    private void showBackConfirmation() {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle("Abbrechen?");
        b.setMessage("Möchtest du die Übung speichern oder abbrechen?");
        b.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveData();
            }
        });
        b.setNegativeButton("Abbrechen", null);
        b.show();
    }

    /**
     * Write the new data out to db
     */
    private void saveData() {
        //create a new subject containing all known values
        Subject newSubject = new Subject(String.valueOf(mDatabaseId),
                String.valueOf(nameInput.getText().toString()),
                String.valueOf(minVoteSeek.getProgress()),
                "0",
                String.valueOf(estimatedUebungCountSeek.getProgress()),
                String.valueOf(estimatedAssignmentsSeek.getProgress()),
                String.valueOf(wantedPresentationPointsSeekbar.getProgress()));
        if (mIsNewSubject) {
            if (mSubjectDb.addGroup(newSubject) == -1)
                Toast.makeText(getActivity(), getString(R.string.subject_manage_subject_exists_already), Toast.LENGTH_SHORT).show();
        } else {
            if ("".equals(newSubject.subjectName))
                newSubject.subjectName = "empty";
            mSubjectDb.changeSubject(mOldSubjectData, newSubject);
        }
    }

    private void insertValues() {
        //offer hint to user
        nameInput.setHint(nameHint);
        //only set the old name as text if it is not "subject name", so the user can correct his value
        if (mIsOldSubject)
            nameInput.setText(nameHint);

        //minpreshelp
        presInfo.setText("" + presentationPointsHint);

        //minpres seek
        wantedPresentationPointsSeekbar.setProgress(presentationPointsHint);
        wantedPresentationPointsSeekbar.setMax(30);
        wantedPresentationPointsSeekbar.setOnSeekBarChangeListener(new SeekerListener(presInfo));

        //minvotehelp
        //initialize seekbar and seekbar info text
        voteInfo.setText(minimumVotePercentageHint + "%");

        //minvoteseek
        minVoteSeek.setMax(100);
        minVoteSeek.setProgress(minimumVotePercentageHint);
        minVoteSeek.setOnSeekBarChangeListener(new SeekerListener(voteInfo, "%"));

        //assignments per uebung help
        estimatedAssignmentsHelp.setText(scheduledAssignmentsPerLesson + "");

        //assignments per uebung seek
        estimatedAssignmentsSeek.setMax(50);
        estimatedAssignmentsSeek.setProgress(scheduledAssignmentsPerLesson);
        estimatedAssignmentsSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedAssignmentsHelp));

        //uebung instances help
        estimatedUebungCountHelp.setText(scheduledNumberOfLessons + "");

        //ubeung instances seek
        estimatedUebungCountSeek.setMax(50);
        estimatedUebungCountSeek.setProgress(scheduledNumberOfLessons);
        estimatedUebungCountSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedUebungCountHelp));

        String title;
        if (mIsNewSubject) {
            if (mSubjectDb.getNumberOfSubjects() == 0)
                title = getString(R.string.subject_manage_add_title_first_subject);
            else
                title = getString(R.string.subject_manage_add_title_new_subject);
        } else//if old subject
            title = getString(R.string.subject_manage_add_title_change_subject) + " " + nameHint;

        //try to set activity title
        mActionBar.setTitle(title);
    }

    /**
     * Load default values or values from old subject data
     */
    private void loadValues() {
        //try to get old subject data; returns null if no subject is found
        mOldSubjectData = mSubjectDb.getSubject(mDatabaseId);

        //create a boolean containing whether we create a new subject to ease understanding
        mIsNewSubject = mDatabaseId == ADD_SUBJECT_CODE;
        mIsOldSubject = !mIsNewSubject;

        //if we only change the entry, get the previously set values.
        if (mIsOldSubject) {
            //extract subject data
            nameHint = mOldSubjectData.subjectName;
            presentationPointsHint = Integer.parseInt(mOldSubjectData.subjectWantedPresentationPoints);
            minimumVotePercentageHint = Integer.parseInt(mOldSubjectData.subjectMinimumVotePercentage);
            scheduledAssignmentsPerLesson = Integer.parseInt(mOldSubjectData.subjectScheduledAssignmentsPerLesson);
            scheduledNumberOfLessons = Integer.parseInt(mOldSubjectData.subjectScheduledLessonCount);
        }
    }
}
