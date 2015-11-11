package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.List;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Database.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.DBAdmissionCounters;
import de.oerntec.votenote.Database.DBGroups;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.DatabaseCreator;
import de.oerntec.votenote.Database.Group;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SeekerListener;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

/**
 * A placeholder fragment containing a simple view.
 */
public class SubjectCreationActivityFragment extends Fragment implements SubjectCreationDialogInterface{
    /**
     * Lesson should be added, not changed
     */
    public static final int ADD_SUBJECT_CODE = -1;

    public static final int ADD_ADMISSION_COUNTER_SIGNAL_ID = -2;

    /**
     * Admission counter that is displayed to have a "button" for adding counters
     */
    private static AdmissionCounter ADD_ADMISSIONS_COUNTER_ITEM;

    /**
     * DB for the subjects
     */
    private final DBSubjects mSubjectDb = DBSubjects.getInstance();

    /**
     * Database containing entries concerning admission counters
     */
    private final DBAdmissionCounters mAdmissionCounterDb = DBAdmissionCounters.getInstance();

    private SQLiteDatabase mDatabase;

    /*
     * Views on the screen
     */
    private EditText nameInput;
    private TextView voteInfo, presInfo, estimatedAssignmentsHelp, estimatedUebungCountHelp;
    private SeekBar minVoteSeek, wantedPresentationPointsSeekbar, estimatedAssignmentsSeek, estimatedUebungCountSeek;

    /**
     * Counter list
     */
    private RecyclerView mAdmissionCounterList;

    private AdmissionCounterAdapter mAdapter;

    /**
     * toolbar to avoid constantly getting it
     */
    private ActionBar mActionBar;

    /*
     * load default values into variables
     */
    private String nameHint;
    private int presentationPointsHint = 0;
    private int minimumVotePercentageHint = 50;
    private int scheduledAssignmentsPerLesson = 5;
    private int scheduledNumberOfLessons = 10;

    /**
     * Database id of current subject
     */
    private int mSubjectId;

    /**
     * Position this subject had when if it was already displayed in the recyclerview of subject-
     * managementactivity
     */
    private int mRecyclerViewPosition;

    /**
     * convenient info about whether current subject is old or new
     */
    private boolean mIsOldSubject, mIsNewSubject;

    /**
     * Old subject data
     */
    private Group mOldSubjectData = null;

    /**
     * The subject data that is inserted into the fragment on start
     */
    private Group mInsertedSubjectData;

    public SubjectCreationActivityFragment() {
    }

    public static SubjectCreationActivityFragment newInstance(int subjectId, int subjectPosition) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, subjectId);
        args.putInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME, subjectPosition);

        SubjectCreationActivityFragment fragment = new SubjectCreationActivityFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get subject hint default
        nameHint = getString(R.string.subject_add_hint);

        //get an actionbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        //get arguments from intent
        mSubjectId = getArguments().getInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, -1);
        mRecyclerViewPosition = getArguments().getInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME, -1);

        //create a boolean containing whether we create a new subject to ease understanding
        mIsNewSubject = mSubjectId == ADD_SUBJECT_CODE;
        mIsOldSubject = !mIsNewSubject;

        //get a database
        DatabaseCreator dbHelper = new DatabaseCreator(getContext());
        mDatabase = dbHelper.getWritableDatabase();

        //begin transaction on database to enable using commit and abort
        mDatabase.beginTransaction();

        //if this is a new subject, we have to create a subject now to know the subject id, which is conveniently returned by the addSubject method
        if(mIsNewSubject)
            mSubjectId = mSubjectDb.addSubject("If you see this, i fucked up.");

        //create a counter item used as an "add"-button
        ADD_ADMISSIONS_COUNTER_ITEM = new AdmissionCounter(ADD_ADMISSION_COUNTER_SIGNAL_ID, mSubjectId, "New Counter", 0, 3);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_subject_creation, menu);

        if (mIsNewSubject) {
            menu.findItem(R.id.action_delete).setEnabled(false);
            menu.findItem(R.id.action_delete).setVisible(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View input = inflater.inflate(R.layout.subject_manager_fragment_subject_settings, container, false);

        nameInput = (EditText) input.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);

        voteInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_min_votierungs);
        minVoteSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_min_votierungs);

        presInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_prespoints);
        wantedPresentationPointsSeekbar = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_prespoints);

        estimatedAssignmentsHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_assignments_per_uebung);
        estimatedAssignmentsSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_assignments_per_uebung);

        estimatedUebungCountHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_estimated_uebung_count);
        estimatedUebungCountSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_estimated_uebung_count);

        mAdmissionCounterList = (RecyclerView) input.findViewById(R.id.subject_manager_admission_counter_list);

        return input;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mIsOldSubject)
            loadOldSubjectValues();

        setValuesForViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
        //abort if this is called for a new subject, that cant work
        if (mIsNewSubject)
            throw new AssertionError("tried to delete a new subject");

        //set the result to be retrieved by subject manager
        getActivity().setResult(SubjectManagementActivity.SUBJECT_CREATOR_RESULT_DELETE, getCurrentResultIntent());

        //kill creator activity
        getActivity().finish();
    }

    /**
     * Show a confirmation dialog to ask the user whether he really wants to leave
     */
    private void showBackConfirmation() {
        //if the subject did not change, we dont need to show this dialog
        Group currentSubject = createSubjectFromInputFields();
        boolean hasEmptyName = "".equals(currentSubject.subjectName);
        if (mInsertedSubjectData.equals(currentSubject)) {
            getActivity().finish();
            return;
        }
        //create dialog
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle("Speichern?");
        b.setMessage("Möchtest du die Übung speichern?");

        //create buttons
        b.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //save data first
                saveData();

                //close the activity containing this fragment
                getActivity().finish();
            }
        });
        b.setNegativeButton("Verwerfen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDatabase.endTransaction();
                getActivity().finish();
            }
        });
        b.setNeutralButton("Abbrechen", null);
        //check for invalid name
        AlertDialog dialog = b.show();
        if (hasEmptyName) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            dialog.setMessage("Einen leeren Namen kannst du nicht speichern!");
        }
    }

    /**
     * Read all input fields and create a subject from that
     */
    private Group createSubjectFromInputFields() {
        return new Group(String.valueOf(mSubjectId),
                String.valueOf(nameInput.getText().toString()),
                String.valueOf(minVoteSeek.getProgress()),
                "0",
                String.valueOf(estimatedUebungCountSeek.getProgress()),
                String.valueOf(estimatedAssignmentsSeek.getProgress()),
                String.valueOf(wantedPresentationPointsSeekbar.getProgress()));
    }

    /**
     * Write the new data out to db
     */
    private void saveData() {
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * save key data about the current fragment subject in an intent, so that onActivityResult can send that intent
     *
     * @return result intent
     */
    private Intent getCurrentResultIntent() {
        //save result data into intent
        Intent returnIntent = new Intent();
        returnIntent.putExtra(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, mSubjectId);
        returnIntent.putExtra(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME, mRecyclerViewPosition);
        return returnIntent;
    }

    /**
     * Either set default values to all views in the creator or set the previously loaded old values
     */
    private void setValuesForViews() {
        /*
        Name input
         */
        if (mIsNewSubject) {
            //set an error if the view is empty
            nameInput.setError(nameHint);
            //delete said error if the view is no longer empty
            nameInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (charSequence.length() == 0)
                        nameInput.setError(nameHint);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
        } else
            nameInput.setHint(nameHint);

        /*
            Admission counter list code
         */
        List<AdmissionCounter> admissionCounters = mAdmissionCounterDb.getAdmissionCounters(mSubjectId);
        admissionCounters.add(ADD_ADMISSIONS_COUNTER_ITEM);

        //config the recyclerview
        mAdmissionCounterList.setHasFixedSize(true);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mAdmissionCounterList.setLayoutManager(manager);

        mAdapter = new AdmissionCounterAdapter(getContext(), mSubjectId);

        mAdmissionCounterList.setAdapter(mAdapter);
        mAdmissionCounterList.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), mAdmissionCounterList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                Dialogs.counterChangeDialog(mAdapter.getItemAtPosition(position), getActivity(), SubjectCreationActivityFragment.this);
            }

            public void onItemLongClick(final View view, int position) {
                Dialogs.counterDeleteDialog(mAdapter.getItemAtPosition(position), getActivity(), SubjectCreationActivityFragment.this);
            }
        }));

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

    public void onBackPressed() {
        showBackConfirmation();
    }

    /**
     * Load default values or values from old subject data
     */
    private void loadOldSubjectValues() {
        //try to get old subject data; returns null if no subject is found
        mOldSubjectData = mSubjectDb.getSubject(mSubjectId);

        //extract subject data
        nameHint = mOldSubjectData.subjectName;
        presentationPointsHint = Integer.parseInt(mOldSubjectData.subjectWantedPresentationPoints);
        minimumVotePercentageHint = Integer.parseInt(mOldSubjectData.subjectMinimumVotePercentage);
        scheduledAssignmentsPerLesson = Integer.parseInt(mOldSubjectData.subjectScheduledAssignmentsPerLesson);
        scheduledNumberOfLessons = Integer.parseInt(mOldSubjectData.subjectScheduledLessonCount);
    }

    @Override
    public void admissionCounterFinished(AdmissionCounter result, boolean isNew) {
        if(isNew){
            mAdapter.addItem(result);
        }
    }

    @Override
    public void admissionCounterDelete(AdmissionCounter delete) {

    }

    @Override
    public void admissionPercentageFinished(AdmissionPercentageMeta result, boolean isNew) {

    }
}