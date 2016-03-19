package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment.AdmissionPercentageCreation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.github.jjobes.slidedaytimepicker.SlideDayTimeListener;
import com.github.jjobes.slidedaytimepicker.SlideDayTimePicker;

import java.util.Calendar;

import de.oerntec.votenote.Database.Pojo.PercentageMetaStuff.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.Helpers.Notifications.NotificationGeneralHelper;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SeekerListener;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.SubjectOverview.SubjectCreationActivity;

public class AdmissionPercentageFragment extends Fragment implements Button.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public static final String SUBJECT_ID = "subject_id";
    public static final String ADMISSION_PERCENTAGE_ID = "ap_id";
    public static final String SUBJECT_IS_NEW = "subject_is_new";

    private int mSubjectId, mAdmissionPercentageId;

    private DBAdmissionPercentageMeta mDb;

    //views needed for configuring a new percentage counter
    private EditText nameInput;
    private TextView mRequiredPercentageCurrentValueTextView, mEstimatedAssignmentsPerLessonCurrentValueTextView, mEstimatedLessonCountCurrentValueTextView, mEstimationModeDescription, mEstimationModeCurrentValueTextView;
    private SeekBar mRequiredPercentageSeekBar, mEstimatedAssignmentsPerLessonSeekBar, mEstimatedLessonCountSeekBar, mEstimationModeSeekbar;


    /**
     * Switch to set notification enabled setting
     */
    private Switch mNotificationEnabledSwitch;

    /**
     * SeekBar for choosing the value of the required percentage for getting a bonus
     */
    private SeekBar mBonusRequiredPercentageSeekBar;

    /**
     * CheckBox to enable the bonus percentage system
     */
    private CheckBox mBonusRequiredPercentageActivationCheckBox;

    /**
     * TextView to show the currently chosen bonus value
     */
    private TextView mBonusRequiredPercentageCurrentValueTextView;


    /**
     * whether or not the recurring notifications are enabled
     */
    private boolean mNotificationEnabledHint = false;

    /**
     * If it exists, this string contains the old recurrence string
     */
    private String mNotificationRecurrenceStringHint = "";

    /**
     * This string is:
     * empty, if there was no recurrence string and none was set
     * ==hint, if the old recurrence string was not changed,
     * the new recurrence string
     */
    private String mNotificationRecurrenceStringCurrent;

    /**
     * Integer containing the default or the old value for the bonus percentage
     */
    private int mBonusRequiredPercentageHint = 50;

    /**
     * Boolean containing the default/old activation state for the bonus percentage seekbar
     */
    private boolean mBonusRequiredPercentageEnabledHint = false;

    /*
    * load default values into these variables
    */
    private String nameHint;

    /**
     * Contains the currently set estimation mode; defaults (like the db) to user estimation.
     */
    private String mCurrentEstimationMode = "user";

    /**
     * Contains the old (if changing subject) or default value for estimation mode
     */
    private String mEstimationModeHint = "user";

    private int requiredPercentageHint = 50, estimatedAssignmentsHint = 5, estimatedLessonCountHint = 10;

    /**
     * the id used to distinguish our current savepoint
     */
    private String mSavepointId;

    private boolean mIsOldPercentageCounter, mIsNewPercentageCounter;

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
            mIsNewPercentageCounter = getArguments().getBoolean(SUBJECT_IS_NEW);
            mIsOldPercentageCounter = !mIsNewPercentageCounter;
            mDb = DBAdmissionPercentageMeta.getInstance();

            //create a savepoint now, so we can rollback if the user decides to abort
            mSavepointId = "APCMETA" + mSubjectId;
            mDb.createSavepoint(mSavepointId);

            //create a new meta entry for this so we have an id to work with
            if (mIsNewPercentageCounter)
                mAdmissionPercentageId = mDb.addItemGetId(new AdmissionPercentageMeta(-1, mSubjectId, 0, 0, 0, "if you see this, i fucked up.", "undefined", 0, false, "", false));

            //hide the delete button if this is a new subject
            if (mIsNewPercentageCounter)
                ((AdmissionPercentageCreationActivity) getActivity()).hideDeleteButton();

        } else
            throw new AssertionError("why are there no arguments here?");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.subject_manager_fragment_percentage_creator_fragment, container, false);

        nameInput = (EditText) view.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);

        mRequiredPercentageCurrentValueTextView = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_required_percentage_current_value);
        mRequiredPercentageSeekBar = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_required_percentage_seekbar);

        mEstimatedAssignmentsPerLessonCurrentValueTextView = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_assignments_per_lesson_current_value);
        mEstimatedAssignmentsPerLessonSeekBar = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_assignments_per_lesson_seekbar);

        mEstimatedLessonCountCurrentValueTextView = (TextView) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_lesson_count_current_value);
        mEstimatedLessonCountSeekBar = (SeekBar) view.findViewById(R.id.subject_manager_dialog_groupsettings_estimated_lesson_count_seekbar);

        mEstimationModeCurrentValueTextView = (TextView) view.findViewById(R.id.subject_manager_fragment_percentage_creator_fragment_current_value);
        mEstimationModeDescription = (TextView) view.findViewById(R.id.subject_manager_fragment_percentage_creator_fragment_estimation_current_value_description);
        mEstimationModeSeekbar = (SeekBar) view.findViewById(R.id.subject_manager_fragment_percentage_creator_fragment_estimation_seekbar);

        mBonusRequiredPercentageActivationCheckBox = (CheckBox) view.findViewById(R.id.subject_manager_fragment_percentage_creator_fragment_bonus_vote_checkbox);
        mBonusRequiredPercentageSeekBar = (SeekBar) view.findViewById(R.id.subject_manager_fragment_percentage_creator_fragment_bonus_vote_seek_bar);
        mBonusRequiredPercentageCurrentValueTextView = (TextView) view.findViewById(R.id.subject_manager_fragment_percentage_creator_fragment_bonus_vote_current_value);

        mNotificationEnabledSwitch = (Switch) view.findViewById(R.id.subject_manager_fragment_percentage_creator_fragment_notification_switch);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsOldPercentageCounter)
            loadOldSubjectValues();
        setValuesForViews();
    }

    /**
     * Load default values or values from old subject data
     */
    private void loadOldSubjectValues() {
        if (mIsNewPercentageCounter)
            throw new AssertionError("this is a new subject, we cannot load data for it!");

        //try to get old subject data; returns null if no subject is found
        AdmissionPercentageMeta oldState = mDb.getItem(mAdmissionPercentageId);

        //extract subject data
        nameHint = oldState.name;
        requiredPercentageHint = oldState.baselineTargetPercentage;
        estimatedAssignmentsHint = oldState.userAssignmentsPerLessonEstimation;
        estimatedLessonCountHint = oldState.userLessonCountEstimation;
        mEstimationModeHint = oldState.getEstimationModeAsString();
        mBonusRequiredPercentageHint = oldState.bonusTargetPercentage;
        mBonusRequiredPercentageEnabledHint = oldState.bonusTargetPercentageEnabled;

        mNotificationEnabledHint = oldState.notificationEnabled;
        mNotificationRecurrenceStringHint = oldState.notificationRecurrenceString;
        mNotificationRecurrenceStringCurrent = mNotificationRecurrenceStringHint;

        //set the parent activity title
        ((AdmissionPercentageCreationActivity) getActivity()).setToolbarTitle(getTitle());
    }

    /**
     * Either set default values to all views in the creator or set the previously loaded old values
     */
    private void setValuesForViews() {
        //display an error if the edittext is empty
        nameInput.addTextChangedListener(new NotEmptyWatcher(nameInput, ((AdmissionPercentageCreationActivity) getActivity()).getSaveButton()));

        //set old name as hint
        if (mIsOldPercentageCounter)
            nameInput.setText(nameHint);

        //show the user he has to enter a name
        if (mIsNewPercentageCounter) {
            nameInput.setError("Must not be empty");
            ((AdmissionPercentageCreationActivity) getActivity()).getSaveButton().setEnabled(false);
        }

        initSeekbar(mRequiredPercentageSeekBar, mRequiredPercentageCurrentValueTextView, requiredPercentageHint, 100);
        initSeekbar(mBonusRequiredPercentageSeekBar, mBonusRequiredPercentageCurrentValueTextView, mBonusRequiredPercentageHint, 100);

        initSeekbar(mEstimatedAssignmentsPerLessonSeekBar, mEstimatedAssignmentsPerLessonCurrentValueTextView, estimatedAssignmentsHint, 50);
        initSeekbar(mEstimatedLessonCountSeekBar, mEstimatedLessonCountCurrentValueTextView, estimatedLessonCountHint, 50);

        initEstimationModeSeekbar();

        initBonusCheckBox();

        mNotificationEnabledSwitch.setChecked(mNotificationEnabledHint);
        mNotificationEnabledSwitch.setOnCheckedChangeListener(this);
    }

    private void initBonusCheckBox() {
        mBonusRequiredPercentageActivationCheckBox.setChecked(mBonusRequiredPercentageEnabledHint);
        mBonusRequiredPercentageSeekBar.setEnabled(mBonusRequiredPercentageEnabledHint);

        mBonusRequiredPercentageActivationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mBonusRequiredPercentageSeekBar.setEnabled(isChecked);
            }
        });
    }

    private void initEstimationModeSeekbar() {
        int amountOfEstimationModes = AdmissionPercentageMeta.EstimationMode.values().length - 2; // -1 for a) undefined and b) max is 1-indexed
        mEstimationModeSeekbar.setMax(amountOfEstimationModes);

        AdmissionPercentageMeta.EstimationMode estimationMode = AdmissionPercentageMeta.EstimationMode.valueOf(mEstimationModeHint);

        mEstimationModeSeekbar.setProgress(estimationMode.ordinal());

        mEstimationModeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onEstimationModeChange(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        onEstimationModeChange(estimationMode.ordinal());
    }

    private void onEstimationModeChange(int seekbarProgress) {
        switch (AdmissionPercentageMeta.EstimationMode.values()[seekbarProgress]) {
            case user://user
                mCurrentEstimationMode = "user";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_user);
                mEstimationModeDescription.setText(R.string.estimation_description_user);
                break;
            case mean://mean
                mCurrentEstimationMode = "mean";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_mean);
                mEstimationModeDescription.setText(R.string.estimation_description_mean);
                break;
            case worst://worst
                mCurrentEstimationMode = "worst";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_worst);
                mEstimationModeDescription.setText(R.string.estimation_description_worst);
                break;
            case best://best
                mCurrentEstimationMode = "best";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_best);
                mEstimationModeDescription.setText(R.string.estimation_description_best);
                break;
            default:
                throw new AssertionError("unknown progress value!");
        }
    }

    private void initSeekbar(SeekBar item, TextView currentValueView, int hintOrOldValue, int max) {
        //set the textview that displays the current value
        currentValueView.setText(String.valueOf(hintOrOldValue));
        //configure the seekbar maximum
        item.setMax(max);
        //set seekbar progress
        item.setProgress(hintOrOldValue);
        //set listener to update current value view
        item.setOnSeekBarChangeListener(new SeekerListener(currentValueView));
    }

    private String getTitle() {
        String title;
        if (mIsNewPercentageCounter) {
            if (mDb.getCount() == 0)
                title = "Add your first percentage counter";//need a better name
            else
                title = "Add a percentage counter";
        } else//if previously existing percentage counter
            title = "Change" + " " + nameHint;
        return title;
    }

    @Override
    public void onClick(View v) {
        int resultState = -1;
        switch (v.getId()) {
            case R.id.activity_admission_percentage_creation_delete_button:
                mDb.rollbackToSavepoint(mSavepointId);
                resultState = SubjectCreationActivity.DIALOG_RESULT_DELETE;
                break;
            case R.id.activity_admission_percentage_creation_save_button:
                //change the item we created at the beginning of this dialog to the actual values
                mDb.changeItem(new AdmissionPercentageMeta(
                        mAdmissionPercentageId,
                        mSubjectId,
                        mEstimatedAssignmentsPerLessonSeekBar.getProgress(),
                        mEstimatedLessonCountSeekBar.getProgress(),
                        mRequiredPercentageSeekBar.getProgress(),
                        nameInput.getText().toString(),
                        mCurrentEstimationMode,
                        mBonusRequiredPercentageSeekBar.getProgress(),
                        mBonusRequiredPercentageActivationCheckBox.isChecked(),
                        mNotificationRecurrenceStringCurrent,
                        mNotificationEnabledSwitch.isChecked()
                ));
                //commit
                mDb.releaseSavepoint(mSavepointId);
                //notify the host fragment that we changed an item
                if (mIsNewPercentageCounter)
                    resultState = SubjectCreationActivity.DIALOG_RESULT_ADDED;
                else
                    resultState = SubjectCreationActivity.DIALOG_RESULT_CHANGED;
                break;
            case R.id.activity_admission_percentage_creation_cancel_button:
                mDb.rollbackToSavepoint(mSavepointId);
                resultState = SubjectCreationActivity.DIALOG_RESULT_CLOSED;
                break;
        }
        AdmissionPercentageCreationActivity host = (AdmissionPercentageCreationActivity) getActivity();
        host.callCreatorFragmentForItemChange(resultState);
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            SlideDayTimePicker.Builder builder = new SlideDayTimePicker.Builder(getFragmentManager());

            builder.setListener(new SlideDayTimeListener() {
                @Override
                public void onDayTimeSet(int day, int hour, int minute) {
                    mNotificationRecurrenceStringCurrent = day + ":" + hour + ":" + minute;
                    NotificationGeneralHelper.setAlarmForNotification(getActivity(),
                            mSubjectId,
                            mAdmissionPercentageId,
                            day,
                            hour,
                            minute);

                    if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                        Log.i("votenote alarms", "added notification");
                }

                @Override
                public void onDayTimeCancel() {
                    buttonView.setChecked(false);
                }
            });

            builder.setIs24HourTime(true);
            builder.setIndicatorColor(R.color.colorPrimary);

            if (mNotificationRecurrenceStringHint != null && !mNotificationRecurrenceStringHint.isEmpty()) {
                String[] dowTime = mNotificationRecurrenceStringHint.split(":");
                builder.setInitialDay(Integer.parseInt(dowTime[0]))
                        .setInitialHour(Integer.parseInt(dowTime[1]))
                        .setInitialMinute(Integer.parseInt(dowTime[2]));
            } else {
                builder.setInitialDay(Calendar.MONDAY)
                        .setInitialHour(9)
                        .setInitialMinute(0);
            }

            builder.build().show();
        } else {
            NotificationGeneralHelper.removeAlarmForNotification(getActivity(), mSubjectId, mAdmissionPercentageId);
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.i("votenote alarms", "removed notification");
        }
    }
}