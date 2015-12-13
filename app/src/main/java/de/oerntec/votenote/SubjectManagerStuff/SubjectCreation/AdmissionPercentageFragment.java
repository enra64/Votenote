package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SeekerListener;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

public class AdmissionPercentageFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String SUBJECT_ID = "subject_id";
    private static final String ADMISSION_PERCENTAGE_ID = "ap_id";
    private static final String SUBJECT_IS_NEW = "subject_is_new";

    private int mSubjectId, mAdmissionPercentageId;

    private DBAdmissionPercentageMeta mDb;

    //views needed for configuring a new percentage counter
    private EditText nameInput;
    private TextView requiredPercentageInfo, estimatedAssignmentsHelp, estimatedUebungCountHelp;
    private SeekBar requiredPercentageSeek, estimatedAssignmentsSeek, estimatedLessonCountSeek;

    /*
    * load default values into these variables
    */
    private String nameHint;
    private int requiredPercentageHint = 50, estimatedAssignmentsHint = 5, estimatedLessonCountHint = 10;

    /**
     * the id used to distinguish our current savepoint
     */
    private String mSavepointId;

    private boolean mIsOldPercentageCounter, mIsNew;

    public AdmissionPercentageFragment() {
    }

    public static AdmissionPercentageFragment newInstance(int subjectId, int admissionCounterId, boolean isNew) {
        AdmissionPercentageFragment fragment = new AdmissionPercentageFragment();
        Bundle args = new Bundle();
        args.putInt(SUBJECT_ID, subjectId);
        args.putInt(ADMISSION_PERCENTAGE_ID, admissionCounterId);
        args.putBoolean(SUBJECT_IS_NEW, isNew);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubjectId = getArguments().getInt(SUBJECT_ID);
            mAdmissionPercentageId = getArguments().getInt(ADMISSION_PERCENTAGE_ID);
            mIsNew = getArguments().getBoolean(SUBJECT_IS_NEW);
            mIsOldPercentageCounter = !mIsNew;
            mDb = DBAdmissionPercentageMeta.getInstance();

            //create a savepoint now, so we can rollback if the user decides to abort
            mSavepointId = "APCMETA" + mSubjectId;
            mDb.createSavepoint(mSavepointId);

            //create a new meta entry for this so we have an id to work with
            if (mIsNew)
                mAdmissionPercentageId = mDb.addItemGetId(new AdmissionPercentageMeta(-1, mSubjectId, 0, 0, 0, "if you see this, i fucked up."));
        } else
            throw new AssertionError("why are there no arguments here?");
    }

    private View createView(LayoutInflater inflater) {
        // Inflate the layout for this fragment
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.subject_manager_fragment_percentage_creator_fragment, null, false);

        nameInput = (EditText) view.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);

        requiredPercentageInfo = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_required_percentage_current_value);
        requiredPercentageSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_required_percentage_seekbar);

        estimatedAssignmentsHelp = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_assignments_per_lesson_current_value);
        estimatedAssignmentsSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_assignments_per_lesson_seekbar);

        estimatedUebungCountHelp = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_lesson_count_current_value);
        estimatedLessonCountSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_lesson_count_seekbar);
        return view;
    }

    /**
     * called after onCreate, before onCreateView
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
            .setView(createView(getActivity().getLayoutInflater()))
            .setTitle("totally no bug")
            .setPositiveButton("OK", this)
            .setNegativeButton("Cancel", this);
        if(mIsOldPercentageCounter)
            b.setNeutralButton("Delete", this);

        AlertDialog d = b.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsOldPercentageCounter)
            loadOldSubjectValues();
        setValuesForViews();

        //set title now, all old data has been loaded
        getDialog().setTitle(getTitle());
    }

    /**
     * Load default values or values from old subject data
     */
    private void loadOldSubjectValues() {
        if (mIsNew)
            throw new AssertionError("this is a new subject, we cannot load data for it!");

        //try to get old subject data; returns null if no subject is found
        AdmissionPercentageMeta oldState = mDb.getItem(mAdmissionPercentageId);

        //extract subject data
        nameHint = oldState.name;
        requiredPercentageHint = oldState.targetPercentage;
        estimatedAssignmentsHint = oldState.estimatedAssignmentsPerLesson;
        estimatedLessonCountHint = oldState.estimatedLessonCount;
    }

    /**
     * Either set default values to all views in the creator or set the previously loaded old values
     */
    private void setValuesForViews() {
        //display an error if the edittext is empty
        nameInput.addTextChangedListener(new NotEmptyWatcher(nameInput, ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)));

        if (mIsOldPercentageCounter)
            nameInput.setText(nameHint);
        else {
            nameInput.setError("Must not be empty");
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        }

        initSeekbar(requiredPercentageSeek, requiredPercentageInfo, requiredPercentageHint, 100);
        initSeekbar(estimatedAssignmentsSeek, estimatedAssignmentsHelp, estimatedAssignmentsHint, 50);
        initSeekbar(estimatedLessonCountSeek, estimatedUebungCountHelp, estimatedLessonCountHint, 50);
    }

    private void initSeekbar(SeekBar item, TextView currentValueView, int hint, int max) {
        //set the textview that displays the current value
        currentValueView.setText(String.valueOf(hint));
        //configure the seekbar maximum
        item.setMax(max);
        //set seekbar progress
        item.setProgress(hint);
        //set listener to update current value view
        item.setOnSeekBarChangeListener(new SeekerListener(currentValueView));
    }

    private String getTitle() {
        String title;
        if (mIsNew) {
            if (mDb.getCount() == 0)
                title = "Add your first percentage counter";//need a better name
            else
                title = "Add a percentage counter";
        } else//if previously existing percentage counter
            title = "Change" + " " + nameHint;
        return title;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int resultState = -1;
        switch(which){
            case DialogInterface.BUTTON_NEGATIVE:
                mDb.rollbackToSavepoint(mSavepointId);
                dialog.dismiss();
                resultState =  SubjectCreationActivity.DIALOG_RESULT_DELETE;
                break;
            case DialogInterface.BUTTON_POSITIVE:
                //change the item we created at the beginning of this dialog to the actual values
                mDb.changeItem(new AdmissionPercentageMeta(
                        mAdmissionPercentageId,
                        mSubjectId,
                        estimatedAssignmentsSeek.getProgress(),
                        estimatedLessonCountSeek.getProgress(),
                        requiredPercentageSeek.getProgress(),
                        nameInput.getText().toString()));
                //commit
                mDb.releaseSavepoint(mSavepointId);
                //notify the host fragment that we changed an item
                resultState = mIsNew ? SubjectCreationActivity.DIALOG_RESULT_ADDED : SubjectCreationActivity.DIALOG_RESULT_CHANGED;
                //bail
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                resultState = SubjectCreationActivity.DIALOG_RESULT_CLOSED;
                break;
        }
        SubjectCreationActivity host = (SubjectCreationActivity) getActivity();
        host.callCreatorFragmentForItemChange(-1, true, resultState);
    }
}