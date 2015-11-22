package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Database.DBAdmissionCounters;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SeekerListener;

public class AdmissionCounterFragment extends DialogFragment {
    private static final String SUBJECT_ID = "subject_id";
    private static final String COUNTER_ID = "counter_id";
    private static final String COUNTER_IS_NEW = "subject_is_new";

    private int mSubjectId, mAdmissionCounterId;

    private DBAdmissionCounters mDb;

    //views needed for configuring a new percentage counter
    private EditText mNameInput;
    private TextView mTargetPointCountCurrent;
    private SeekBar mTargetPointCountSeek;

    private String mSavepointId;

    private boolean mIsOld, mIsNew;

    public AdmissionCounterFragment() {
    }

    public static AdmissionCounterFragment newInstance(int subjectId, int counterId, boolean isNew) {
        AdmissionCounterFragment fragment = new AdmissionCounterFragment();
        Bundle args = new Bundle();
        args.putInt(SUBJECT_ID, subjectId);
        args.putInt(COUNTER_ID, counterId);
        args.putBoolean(COUNTER_IS_NEW, isNew);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubjectId = getArguments().getInt(SUBJECT_ID);
            mIsNew = getArguments().getBoolean(COUNTER_IS_NEW);
            mIsOld = !mIsNew;
            mAdmissionCounterId = getArguments().getInt(COUNTER_ID);
            mDb = DBAdmissionCounters.getInstance();

            //create a savepoint now, so we can rollback if the user decides to abort
            mSavepointId = "AC" + mSubjectId;
            mDb.createSavepoint(mSavepointId);

            //create a new table entry for this so we have an id to work with
            if (mIsNew)
                mAdmissionCounterId = mDb.addItemGetId(new AdmissionCounter(-1, mSubjectId, "Counter Name", 0, 3));//these are also the default values, so beware on change!
        } else
            throw new AssertionError("why are there no arguments here?");
    }

    public View createView(LayoutInflater inflater) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.subject_manager_fragment_dialog_admission_counter, null, false);

        mNameInput = (EditText) view.findViewById(R.id.subject_manager_fragment_dialog_admission_counter_name_edittext);
        mTargetPointCountSeek = (SeekBar) view.findViewById(R.id.subject_manager_fragment_dialog_admission_counter_target_seekbar);
        mTargetPointCountCurrent = (TextView) view.findViewById(R.id.subject_manager_fragment_dialog_admission_counter_current);
        return view;
    }

    /**
     * called after onCreate, before onCreateView
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setView(createView(getActivity().getLayoutInflater()))
                .setTitle(getTitle())
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //default to zero points for a new counter
                                int currentPoints = 0;
                                if (mIsOld)
                                    currentPoints = mDb.getItem(mSubjectId).currentValue;
                                //change the item we created at the beginning of this dialog to the actual values
                                mDb.changeItem(new AdmissionCounter(
                                        mAdmissionCounterId,
                                        mSubjectId,
                                        mNameInput.getText().toString(),
                                        currentPoints,
                                        mTargetPointCountSeek.getProgress()
                                ));
                                //commit
                                mDb.releaseSavepoint(mSavepointId);
                                //notify the host fragment that we changed an item
                                SubjectCreationActivity host = (SubjectCreationActivity) getActivity();
                                host.callCreatorFragmentForItemChange(mAdmissionCounterId, false, mIsNew);
                                //bail
                                dialog.dismiss();
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mDb.rollbackToSavepoint(mSavepointId);
                                dialog.dismiss();
                            }
                        }
                );
        return b.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AdmissionCounter inputCounter = mDb.getItem(mAdmissionCounterId);

        //set valid value on seekbar
        mTargetPointCountSeek.setProgress(inputCounter.targetValue);
        mTargetPointCountSeek.setOnSeekBarChangeListener(new SeekerListener(mTargetPointCountCurrent));

        //add a watcher to set an error if the name is empty
        mNameInput.addTextChangedListener(new NotEmptyWatcher(mNameInput, ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)));

        //set old name
        if (mIsOld)
            mNameInput.setText(inputCounter.counterName);
        else {
            mNameInput.setHint("Add subject");
            //since the field is empty, put an error here
            mNameInput.setError("Must not be empty");
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private AdmissionCounter save() {
        int currentPoints;
        //load old point state
        if (mIsOld)
            currentPoints = mDb.getItem(mAdmissionCounterId).currentValue;
            //default starting point state
        else
            currentPoints = 0;

        AdmissionCounter result = new AdmissionCounter(
                mAdmissionCounterId,
                mSubjectId,
                mNameInput.getText().toString(),
                currentPoints,
                mTargetPointCountSeek.getProgress()
        );
        mDb.changeItem(result);
        return result;
    }

    public void abort() {
        mDb.rollbackToSavepoint(mSavepointId);
    }

    public String getTitle() {
        String title;
        if (mIsNew)
            title = "New admission counter";
        else {
            title = "Change " + mDb.getItem(mAdmissionCounterId).counterName;
        }
        return title;
    }
}