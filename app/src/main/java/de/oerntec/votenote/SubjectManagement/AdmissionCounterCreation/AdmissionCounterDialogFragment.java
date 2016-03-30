package de.oerntec.votenote.SubjectManagement.AdmissionCounterCreation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Helpers.SeekerListener;
import de.oerntec.votenote.Helpers.TextWatchers.ActuallyDisabledNotEmptyWatcher;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagement.SubjectCreation.SubjectCreationActivity;

public class AdmissionCounterDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
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

    public AdmissionCounterDialogFragment() {
    }

    public static AdmissionCounterDialogFragment newInstance(int subjectId, int counterId, boolean isNew) {
        AdmissionCounterDialogFragment fragment = new AdmissionCounterDialogFragment();
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
        // we may pass null to the inflater here, because in a dialog, we simply do not know what
        // parent we attach to
        //noinspection RedundantCast
        View view = inflater.inflate(R.layout.subject_manager_fragment_dialog_admission_counter, (ViewGroup) null, false);

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
                .setPositiveButton("OK", this)
                .setNegativeButton("Cancel", this);
        if (mIsOld)
            b.setNeutralButton("Delete", this);
        AlertDialog d = b.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int resultState = -1;
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL:
                mDb.rollbackToSavepoint(mSavepointId);
                dialog.dismiss();
                resultState = SubjectCreationActivity.DIALOG_RESULT_DELETE;
                break;
            case DialogInterface.BUTTON_POSITIVE:
                //default to zero points for a new counter
                int currentPoints = 0;
                if (mIsOld)
                    currentPoints = mDb.getItem(mAdmissionCounterId).currentValue;
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
                resultState = mIsNew ? SubjectCreationActivity.DIALOG_RESULT_ADDED : SubjectCreationActivity.DIALOG_RESULT_CHANGED;
                //bail
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                mDb.rollbackToSavepoint(mSavepointId);
                resultState = SubjectCreationActivity.DIALOG_RESULT_CLOSED;
                break;
        }
        SubjectCreationActivity host = (SubjectCreationActivity) getActivity();
        if (host == null)
            throw new AssertionError("why cant we find the host?");
        host.callCreatorFragmentForItemChange(mAdmissionCounterId, false, resultState);
    }

    @Override
    public void onStart() {
        super.onStart();
        AdmissionCounter inputCounter = mDb.getItem(mAdmissionCounterId);

        //set valid value on seekbar
        mTargetPointCountSeek.setProgress(inputCounter.targetValue);
        mTargetPointCountCurrent.setText(String.valueOf(inputCounter.targetValue));
        mTargetPointCountSeek.setOnSeekBarChangeListener(new SeekerListener(mTargetPointCountCurrent));

        //add a watcher to set an error if the name is empty
        mNameInput.addTextChangedListener(new ActuallyDisabledNotEmptyWatcher(
                mNameInput,
                getActivity().getString(R.string.edit_text_empty_error_text),
                mIsOld,
                ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)));

        //set old name
        if (mIsOld)
            mNameInput.setText(inputCounter.name);
        else
            mNameInput.setHint(R.string.admission_percentage_creator_name_hint);
    }

    public String getTitle() {
        String title;
        if (mIsNew)
            title = "New admission counter";
        else {
            title = "Change " + mDb.getItem(mAdmissionCounterId).name;
        }
        return title;
    }
}