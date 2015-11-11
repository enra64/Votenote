package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import de.oerntec.votenote.R;

public class PercentageFragment extends Fragment {
    private static final String SUBJECT_ID = "subject_id";
    private static final String SUBJECT_IS_NEW = "subject_is_new";

    private int mSubjectId;

    //views needed for configuring a new percentage counter
    private TextView requiredPercentageInfo, estimatedAssignmentsHelp, estimatedUebungCountHelp;
    private SeekBar requiredPercentageSeek, estimatedAssignmentsSeek, estimatedUebungCountSeek;

    private boolean mIsOldSubject, mIsNewSubject;

    public static PercentageFragment newInstance(int subjectId, boolean isNew) {
        PercentageFragment fragment = new PercentageFragment();
        Bundle args = new Bundle();
        args.putInt(SUBJECT_ID, subjectId);
        args.putBoolean(SUBJECT_ID, isNew);
        fragment.setArguments(args);
        return fragment;
    }

    public PercentageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubjectId = getArguments().getInt(SUBJECT_ID);
            mIsNewSubject = getArguments().getBoolean(SUBJECT_IS_NEW);
            mIsOldSubject = !mIsNewSubject;
            //TODO: if this is a new percentage counter, start a nested transaction
        }
        else
            throw new AssertionError("why are there no arguments here?");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.subject_manager_fragment_percentage_creator_fragment, container, false);
        requiredPercentageInfo = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_text_min_votierungs);
        requiredPercentageSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_seek_min_votierungs);

        estimatedAssignmentsHelp = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_text_assignments_per_uebung);
        estimatedAssignmentsSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_seek_assignments_per_uebung);

        estimatedUebungCountHelp = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_text_estimated_uebung_count);
        estimatedUebungCountSeek = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_seek_estimated_uebung_count);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mIsOldSubject)
            loadOldSubjectValues();

        setValuesForViews();
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
}
