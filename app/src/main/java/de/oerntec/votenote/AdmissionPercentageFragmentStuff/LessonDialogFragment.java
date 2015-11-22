package de.oerntec.votenote.AdmissionPercentageFragmentStuff;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

public class LessonDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String ARG_AP_META_ID = "apmetaid";
    private static final String ARG_LESSON_ID = "lessonid";

    private static final int ADD_LESSON_CODE = -2;

    /**
     * Admission percentage counter meta id for this dialog
     */
    private int mMetaId;

    /**
     * Lesson id for this dialog
     */
    private int mLessonId;

    /**
     * Whether the lesson is old or new
     */
    private boolean mIsNewLesson, mIsOldLesson;

    /**
     * is this the first lesson to be added to the pc?
     */
    private boolean mIsFirstLesson;

    /**
     * Whether the lessons are displayed in reverse  or not
     */
    private boolean mReverseLessonSort;

    /**
     * Whether to try and fix invalid number picker values or let the user do it
     */
    private boolean mEnableDataAutoFix;

    /**
     * database instance to use
     */
    private DBAdmissionPercentageData mDataDb;

    /**
     * If and only if this is an old subject, this contains the old data
     */
    private AdmissionPercentageData mOldData;

    /**
     * text view used to display information about the current picker status
     */
    private TextView mInfoView;

    /**
     * picker used to choose how many assignments have been done
     */
    private NumberPicker mFinishedAssignmentsPicker;

    /**
     * picker used to choose how many assignments are available to be done
     */
    private NumberPicker mAvailableAssignmentsPicker;

    /**
     * default constructor
     */
    public LessonDialogFragment() {
    }

    /**
     * get a new instance of the fragment
     *
     * @param apMetaId the ap meta id this lesson is for
     * @param lessonID lesson id to identify the lesson
     * @return new instance
     */
    public static LessonDialogFragment newInstance(final int apMetaId, final int lessonID) {
        LessonDialogFragment fragment = new LessonDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_AP_META_ID, apMetaId);
        args.putInt(ARG_LESSON_ID, lessonID);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * load arguments etc
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //load given arguments
            mMetaId = getArguments().getInt(ARG_AP_META_ID);
            mLessonId = getArguments().getInt(ARG_LESSON_ID);
            mIsNewLesson = mLessonId == ADD_LESSON_CODE;
            mIsOldLesson = !mIsNewLesson;

            //load shared settings
            mReverseLessonSort = MainActivity.getPreference("reverse_lesson_sort", false);
            mEnableDataAutoFix = MainActivity.getPreference("move_max_assignments_picker", true);

            //get database instance
            mDataDb = DBAdmissionPercentageData.getInstance();

            //first lesson for this pc?
            if(mIsNewLesson)
                mIsFirstLesson = mDataDb.getItemsForMetaId(mMetaId, mReverseLessonSort).size() == 0;
        } else
            throw new AssertionError("Why are there no arguments here?");
    }

    /**
     * Create the view and save references
     */
    private View createView(LayoutInflater inflater) {
        //inflate rootView
        final View rootView = inflater.inflate(R.layout.subject_fragment_dialog_newentry, null);

        //find all necessary views
        mInfoView = (TextView) rootView.findViewById(R.id.infoTextView);
        mFinishedAssignmentsPicker = (NumberPicker) rootView.findViewById(R.id.pickerMyVote);
        mAvailableAssignmentsPicker = (NumberPicker) rootView.findViewById(R.id.pickerMaxVote);

        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setView(createView(getActivity().getLayoutInflater()))
                .setTitle(getTitle())
                .setPositiveButton("OK", this)
                .setNegativeButton(R.string.dialog_button_abort, null);
        if(mIsNewLesson)
            b.setNeutralButton(R.string.delete_button_text, this);
        return b.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mIsOldLesson)
            mOldData = mDataDb.getItem(mLessonId, mMetaId);

        //set appropriate title
        getDialog().setTitle(getTitle());
        setPickerValues();
        applyPickerListeners();

        //set the current values of the pickers as explanation text
        mInfoView.setText(mFinishedAssignmentsPicker.getValue() + " " + getActivity().getString(R.string.main_dialog_lesson_von)
                + " " + mAvailableAssignmentsPicker.getValue() + " " + getActivity().getString(R.string.main_dialog_lesson_votes));

        mInfoView.setTextColor(Color.argb(255, 153, 204, 0));//green
    }

    /**
     * get appropriate title
     */
    private String getTitle() {
        if(mIsNewLesson)
            return getActivity().getString(R.string.main_dialog_lesson_new_lesson_title);
        if (getActivity().getResources().getConfiguration().locale.getLanguage().equals("de"))
            return "Übung " + mLessonId + " ändern?";
        else
            return "Change lesson " + mLessonId + "?";
    }

    /**
     * Set the max, min, current value on both pickers considering whether this is a new/old/the first lesson for this ap meta id
     */
    private void setPickerValues(){
        int currentAvailableAssignments, currentFinishedAssignments;

        if(mIsNewLesson){
            if(mIsFirstLesson){
                AdmissionPercentageMeta metaItem = DBAdmissionPercentageMeta.getInstance().getItem(mMetaId);
                currentAvailableAssignments = metaItem.estimatedAssignmentsPerLesson;
                currentFinishedAssignments = currentAvailableAssignments / 2;
            } else {
                AdmissionPercentageData lastEnteredLesson = mDataDb.getNewestItemForMetaId(mMetaId);
                currentAvailableAssignments = lastEnteredLesson.availableAssignments;
                currentFinishedAssignments = lastEnteredLesson.finishedAssignments;
            }
        } else {
            currentAvailableAssignments = mOldData.availableAssignments;
            currentFinishedAssignments = mOldData.finishedAssignments;
        }

        setPicker(mAvailableAssignmentsPicker, 0, currentAvailableAssignments * 2, currentAvailableAssignments);
        setPicker(mFinishedAssignmentsPicker, 0, currentAvailableAssignments * 2, currentFinishedAssignments);
    }

    /**
     * create and apply listeners considering whether the user wants to use autofix or not
     */
    private void applyPickerListeners(){
        //try to fix invalid number of done assignments
        if (mEnableDataAutoFix) {
            mFinishedAssignmentsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                    //load new values
                    int myVoteValue = mFinishedAssignmentsPicker.getValue();
                    int maxVoteValue = mAvailableAssignmentsPicker.getValue();
                    if (myVoteValue > maxVoteValue)
                        mAvailableAssignmentsPicker.setValue(myVoteValue);
                    //update info text
                    mInfoView.setText(mFinishedAssignmentsPicker.getValue() + " " + getActivity().getString(R.string.main_dialog_lesson_von)
                            + " " + mAvailableAssignmentsPicker.getValue() + " " + getActivity().getString(R.string.main_dialog_lesson_votes));
                }
            });
            mAvailableAssignmentsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                    //load new values
                    int myVoteValue = mFinishedAssignmentsPicker.getValue();
                    int maxVoteValue = mAvailableAssignmentsPicker.getValue();
                    if (myVoteValue > maxVoteValue)
                        mFinishedAssignmentsPicker.setValue(maxVoteValue);
                    //update info text
                    mInfoView.setText(mFinishedAssignmentsPicker.getValue() + " " + getActivity().getString(R.string.main_dialog_lesson_von)
                            + " " + mAvailableAssignmentsPicker.getValue() + " " + getActivity().getString(R.string.main_dialog_lesson_votes));
                }
            });
        }
        //fallback behaviour if the setting is disabled
        else {
            //listener for the vote picker
            NumberPicker.OnValueChangeListener votePickerListener = new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    //load new values
                    int myVoteValue = mFinishedAssignmentsPicker.getValue();
                    int maxVoteValue = mAvailableAssignmentsPicker.getValue();
                    mInfoView.setText(myVoteValue + " " + getActivity().getString(R.string.main_dialog_lesson_von)
                            + " " + maxVoteValue + " " + getActivity().getString(R.string.main_dialog_lesson_votes));
                    boolean isValid = myVoteValue <= maxVoteValue;
                    ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isValid);
                    mInfoView.setTextColor(isValid ?
                            Color.argb(255, 153, 204, 0) :                            getActivity().getResources().getColor(R.color.warning_red));
                }
            };
            //add change listener to update dialog explanation if pickers changed
            mAvailableAssignmentsPicker.setOnValueChangedListener(votePickerListener);
            mFinishedAssignmentsPicker.setOnValueChangedListener(votePickerListener);
        }
    }

    /**
     * apply values to given picker
     */
    private void setPicker(NumberPicker item, int min, int max, int current){
        item.setMinValue(min);
        item.setMaxValue(max);
        item.setValue(current);
    }

    /**
     * handle dialog button clicks
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            //save
            case AlertDialog.BUTTON_POSITIVE:
                int finishedAssignments = mFinishedAssignmentsPicker.getValue();
                int availableAssignments = mAvailableAssignmentsPicker.getValue();
                if (finishedAssignments <= availableAssignments) {
                    AdmissionPercentageData val = new AdmissionPercentageData(ADD_LESSON_CODE, mMetaId, ADD_LESSON_CODE, finishedAssignments, availableAssignments);
                    //add lesson, id and lesson id will be added automatically
                    if (mIsNewLesson)
                        ((MainActivity) getActivity()).getCurrentFragment().mAdapter.addLesson(val);
                        //change lesson
                    else {
                        val.id = mOldData.id;
                        val.lessonId = mOldData.lessonId;
                        ((MainActivity) getActivity()).getCurrentFragment().mAdapter.changeLesson(val);
                    }
                }
                else
                    Toast.makeText(getActivity(), getActivity().getString(R.string.main_dialog_lesson_voted_too_much), Toast.LENGTH_SHORT).show();
                break;
            //delete
            case AlertDialog.BUTTON_NEUTRAL:
                ((MainActivity) getActivity()).getCurrentFragment().deleteLesson(mOldData);
                break;
        }
    }
}
