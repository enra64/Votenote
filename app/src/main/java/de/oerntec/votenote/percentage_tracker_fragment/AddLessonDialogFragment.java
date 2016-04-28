package de.oerntec.votenote.percentage_tracker_fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.Lesson;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBLessons;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;
import de.oerntec.votenote.helpers.General;
import de.oerntec.votenote.preferences.Preferences;

public class AddLessonDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
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
     * Whether to try and fix invalid number picker values or let the user do it
     */
    private boolean mEnableDataAutoFix;

    /**
     * database instance to use
     */
    private DBLessons mDataDb;

    /**
     * If and only if this is an old subject, this contains the old data
     */
    private Lesson mOldData;

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
     * display the resulting overall percentage
     */
    private TextView mResultingPercentageView;

    private PercentageTrackerPojo mMetaItem;

    private boolean mLiveUpdateResultingPercentage;

    /**
     * default constructor
     */
    public AddLessonDialogFragment() {
    }

    /**
     * get a new instance of the fragment
     *
     * @param apMetaId the ap meta id this lesson is for
     * @param lessonID lesson id to identify the lesson
     * @return new instance
     */
    public static AddLessonDialogFragment newInstance(final int apMetaId, final int lessonID) {
        AddLessonDialogFragment fragment = new AddLessonDialogFragment();
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
            mMetaItem = DBPercentageTracker.getInstance().getItem(mMetaId);
            mMetaItem.loadData(DBLessons.getInstance(), Preferences.getPreference(getActivity(), "reverse_lesson_sort", false));

            //load shared settings
            //Whether the lessons are displayed in reverse  or not
            boolean mReverseLessonSort = Preferences.getPreference(getActivity(), "reverse_lesson_sort", false);
            mEnableDataAutoFix = Preferences.getPreference(getActivity(), "move_max_assignments_picker", true);
            mLiveUpdateResultingPercentage = Preferences.getPreference(getActivity(), "live_resulting_percentage", true);

            //get database instance
            mDataDb = DBLessons.getInstance();

            //first lesson for this pc?
            if (mIsNewLesson)
                mIsFirstLesson = mDataDb.getItemsForMetaId(mMetaId, mReverseLessonSort).size() == 0;
        } else
            throw new AssertionError("Why are there no arguments here?");
    }

    /**
     * Called after onCreate
     */
    private View createView(LayoutInflater inflater) {
        //inflate rootView
        //noinspection RedundantCast because in a dialog, we cant know the parent yet
        final View rootView = inflater.inflate(R.layout.percentage_tracker_change_lesson_dialog, (ViewGroup) null);

        //find all necessary views
        mInfoView = (TextView) rootView.findViewById(R.id.infoTextView);
        mFinishedAssignmentsPicker = (NumberPicker) rootView.findViewById(R.id.pickerMyVote);
        mAvailableAssignmentsPicker = (NumberPicker) rootView.findViewById(R.id.pickerMaxVote);
        mResultingPercentageView = (TextView) rootView.findViewById(R.id.subject_fragment_dialog_newentry_overall_percentage);

        if (mIsOldLesson)
            mOldData = mDataDb.getItem(mLessonId, mMetaId);

        if (!mLiveUpdateResultingPercentage)
            mResultingPercentageView.setVisibility(View.GONE);
        else if (mIsOldLesson)
            onPercentageUpdate(mOldData.finishedAssignments, mOldData.availableAssignments);
        else
            onPercentageUpdate(0, 0);

        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setView(createView(getActivity().getLayoutInflater()))
                .setTitle(getTitle())
                .setPositiveButton("OK", this)
                .setNegativeButton(R.string.dialog_button_abort, null);
        if (mIsOldLesson)
            b.setNeutralButton(R.string.delete_button_text, this);
        return b.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        //set appropriate title
        getDialog().setTitle(getTitle());
        setPickerValues();
        applyPickerListeners();

        //set the current values of the pickers as explanation text
        mInfoView.setText(getString(R.string.lesson_dialog_fragment_picker_info_text, mFinishedAssignmentsPicker.getValue(), mAvailableAssignmentsPicker.getValue()));
    }

    /**
     * get appropriate title
     */
    private String getTitle() {
        if (mIsNewLesson)
            return getActivity().getString(R.string.main_dialog_lesson_new_lesson_title);
        return String.format(getActivity().getString(R.string.lesson_add_dialog_title), mLessonId);
    }

    /**
     * Set the max, min, current value on both pickers considering whether this is a new/old/the first lesson for this ap meta id
     */
    private void setPickerValues() {
        int currentAvailableAssignments, currentFinishedAssignments;

        if (mIsNewLesson) {
            if (mIsFirstLesson) {
                PercentageTrackerPojo metaItem = DBPercentageTracker.getInstance().getItem(mMetaId);
                currentAvailableAssignments = metaItem.userAssignmentsPerLessonEstimation;
                currentFinishedAssignments = currentAvailableAssignments / 2;
            } else {
                Lesson lastEnteredLesson = mDataDb.getNewestItemForMetaId(mMetaId);
                currentAvailableAssignments = lastEnteredLesson.availableAssignments;
                currentFinishedAssignments = lastEnteredLesson.finishedAssignments;
            }
        } else {
            currentAvailableAssignments = mOldData.availableAssignments;
            currentFinishedAssignments = mOldData.finishedAssignments;
        }

        // avoid overflowing to negative ints
        int factor = currentAvailableAssignments < Integer.MAX_VALUE / 2 ? 2 : 1;

        int maxPickerValues = currentAvailableAssignments * factor;

        if (currentAvailableAssignments == 0)
            maxPickerValues = 10;

        setPicker(mAvailableAssignmentsPicker, maxPickerValues, currentAvailableAssignments);
        setPicker(mFinishedAssignmentsPicker, maxPickerValues, currentFinishedAssignments);

        onPercentageUpdate(currentFinishedAssignments, currentAvailableAssignments);
    }

    /**
     * create and apply listeners considering whether the user wants to use autofix or not
     */
    private void applyPickerListeners() {
        //try to fix invalid number of done assignments
        if (mEnableDataAutoFix) {
            mFinishedAssignmentsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(
                        NumberPicker numberPicker,
                        int oldValue,
                        final int finishedAssignments) {
                    int availableAssignments = mAvailableAssignmentsPicker.getValue();

                    // check whether less assignments are available than have been done
                    if (finishedAssignments > availableAssignments) {
                        mAvailableAssignmentsPicker.setValue(finishedAssignments);
                        availableAssignments = finishedAssignments;
                    }

                    // update the resulting percentage view
                    onPercentageUpdate(finishedAssignments, availableAssignments);
                }
            });
            mAvailableAssignmentsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(
                        NumberPicker numberPicker,
                        int oldValue,
                        final int availableAssignments) {
                    int finishedAssignments = mFinishedAssignmentsPicker.getValue();

                    // check whether less assignments are available than have been done
                    if (finishedAssignments > availableAssignments) {
                        mFinishedAssignmentsPicker.setValue(availableAssignments);
                        finishedAssignments = availableAssignments;
                    }

                    int increaseMaximumAvailableAssignmentsTreshold =
                            (int) (((float) numberPicker.getMaxValue()) * 3f / 4f);

                    if(availableAssignments >= increaseMaximumAvailableAssignmentsTreshold)
                        numberPicker.setMaxValue(increaseMaximumAvailableAssignmentsTreshold + 10);

                    // update the resulting percentage view
                    onPercentageUpdate(finishedAssignments, availableAssignments);
                }
            });
        }
        // fallback behaviour if the setting is disabled
        else {
            // listener for the vote picker
            NumberPicker.OnValueChangeListener votePickerListener = new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    //load new values
                    int myVoteValue = mFinishedAssignmentsPicker.getValue();
                    int maxVoteValue = mAvailableAssignmentsPicker.getValue();
                    boolean isValid = myVoteValue <= maxVoteValue;
                    ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isValid);
                    onPercentageUpdate(myVoteValue, maxVoteValue);
                }
            };
            //add change listener to update dialog explanation if pickers changed
            mAvailableAssignmentsPicker.setOnValueChangedListener(votePickerListener);
            mFinishedAssignmentsPicker.setOnValueChangedListener(votePickerListener);
        }
    }

    /**
     * Update the resulting percentage based on the available/finished picker values
     *
     * @param availableAssignments how many assignments are available to be done
     * @param finishedAssignments  how many assignments have been done
     */
    private void onPercentageUpdate(int finishedAssignments, int availableAssignments) {
        if (!mLiveUpdateResultingPercentage)
            return;
        // if the subject is old, we need to subtract the old values from the picker values to offset
        // the old lesson values
        if (mIsOldLesson) {
            availableAssignments = availableAssignments - mOldData.availableAssignments;
            finishedAssignments = finishedAssignments - mOldData.finishedAssignments;
        }

        // set info text
        mInfoView.setText(
                getString(
                        R.string.lesson_dialog_fragment_picker_info_text,
                        mFinishedAssignmentsPicker.getValue(),
                        mAvailableAssignmentsPicker.getValue()));

        // set percentage text
        mResultingPercentageView.setText(
                String.format(
                        General.getCurrentLocale(getActivity()),
                        "%.1f%%",
                        mMetaItem.getAverageFinished(availableAssignments, finishedAssignments)));

        //give the percentage text an appropriate clue color
        General.triStateClueColors(
                mResultingPercentageView,
                getActivity(),
                mMetaItem,
                finishedAssignments,
                availableAssignments);
    }

    /**
     * apply values to given picker
     */
    private void setPicker(NumberPicker item, int max, int current) {
        item.setMinValue(0);
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
                    Lesson val = new Lesson(ADD_LESSON_CODE, mMetaId, ADD_LESSON_CODE, finishedAssignments, availableAssignments);
                    //add lesson, id and lesson id will be added automatically
                    if (mIsNewLesson)
                        ((MainActivity) getActivity()).getCurrentAdmissionPercentageFragment().mAdapter.addLesson(val);
                        //change lesson
                    else {
                        val.id = mOldData.id;
                        val.lessonId = mOldData.lessonId;
                        ((MainActivity) getActivity()).getCurrentAdmissionPercentageFragment().mAdapter.changeLesson(val);
                    }
                } else
                    Toast.makeText(getActivity(), getActivity().getString(R.string.main_dialog_lesson_voted_too_much), Toast.LENGTH_SHORT).show();
                break;
            //delete
            case AlertDialog.BUTTON_NEUTRAL:
                ((MainActivity) getActivity()).getCurrentAdmissionPercentageFragment().deleteLessonAnimated(mOldData);
                break;
        }
    }
}
