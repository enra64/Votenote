package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import de.oerntec.votenote.Database.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.DBAdmissionPercentageMeta;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SeekerListener;

public class AdmissionPercentageFragment extends Fragment {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubjectId = getArguments().getInt(SUBJECT_ID);
            mAdmissionPercentageId = getArguments().getInt(ADMISSION_PERCENTAGE_ID);
            mIsNew = getArguments().getBoolean(SUBJECT_IS_NEW);
            mIsOldPercentageCounter = !mIsNew;
            mDb = DBAdmissionPercentageMeta.getInstance();

            //create a savepoint now, so we can rollback if the user decides to abort
            mSavepointId = mSubjectId + "APCMETA" + System.currentTimeMillis();
            mDb.createSavepoint(mSavepointId);

            //create a new meta entry for this so we have an id to work with
            if (mIsNew)
                mAdmissionPercentageId = mDb.addItem(new AdmissionPercentageMeta(-1, mSubjectId, 0, 0, 0, "if you see this, i fucked up."));
        } else
            throw new AssertionError("why are there no arguments here?");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.subject_manager_fragment_percentage_creator_fragment, container, false);

        nameInput = (EditText) view.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);

        requiredPercentageInfo = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_text_min_votierungs);
        requiredPercentageSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_seek_min_votierungs);

        estimatedAssignmentsHelp = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_text_assignments_per_uebung);
        estimatedAssignmentsSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_seek_assignments_per_uebung);

        estimatedUebungCountHelp = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_text_estimated_uebung_count);
        estimatedLessonCountSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_seek_estimated_uebung_count);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mIsOldPercentageCounter)
            loadOldSubjectValues();

        setValuesForViews();
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
        /*
        Name input
         */
        if (mIsNew) {
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


        //only set the old name as text if it is not "subject name", so the user can correct his value
        if (mIsOldPercentageCounter)
            nameInput.setText(nameHint);

        //minvotehelp
        //initialize seekbar and seekbar info text
        requiredPercentageInfo.setText(requiredPercentageHint + "%");

        //minvoteseek
        requiredPercentageSeek.setMax(100);
        requiredPercentageSeek.setProgress(requiredPercentageHint);
        requiredPercentageSeek.setOnSeekBarChangeListener(new SeekerListener(requiredPercentageInfo, "%"));

        //assignments per uebung help
        estimatedAssignmentsHelp.setText(estimatedAssignmentsHint + "");

        //assignments per uebung seek
        estimatedAssignmentsSeek.setMax(50);
        estimatedAssignmentsSeek.setProgress(estimatedAssignmentsHint);
        estimatedAssignmentsSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedAssignmentsHelp));

        //uebung instances help
        estimatedUebungCountHelp.setText(estimatedLessonCountHint + "");

        //ubeung instances seek
        estimatedLessonCountSeek.setMax(50);
        estimatedLessonCountSeek.setProgress(estimatedLessonCountHint);
        estimatedLessonCountSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedUebungCountHelp));
    }

    private AdmissionPercentageMeta save() {
        AdmissionPercentageMeta result = new AdmissionPercentageMeta(
                mAdmissionPercentageId,
                mSubjectId,
                estimatedAssignmentsSeek.getProgress(),
                estimatedLessonCountSeek.getProgress(),
                requiredPercentageSeek.getProgress(),
                nameInput.getText().toString()
        );
        mDb.changeItem(result);
        return result;
    }

    public void abort() {
        mDb.rollbackToSavepoint(mSavepointId);
    }

    public String getTitle() {
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
}
