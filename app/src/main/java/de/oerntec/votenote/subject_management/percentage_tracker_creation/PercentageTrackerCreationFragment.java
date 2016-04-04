package de.oerntec.votenote.subject_management.percentage_tracker_creation;

import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;
import de.oerntec.votenote.helpers.General;
import de.oerntec.votenote.helpers.Permissions;
import de.oerntec.votenote.helpers.SeekerListener;
import de.oerntec.votenote.helpers.dialogs.DayOfWeekPickerDialog;
import de.oerntec.votenote.helpers.forecasting.ForecastModeSpinnerAdapter;
import de.oerntec.votenote.helpers.notifications.NotificationGeneralHelper;
import de.oerntec.votenote.helpers.textwatchers.FakeDisabledNotEmptyWatcher;
import de.oerntec.votenote.subject_management.subject_creation.SubjectCreationActivity;

public class PercentageTrackerCreationFragment extends Fragment implements Button.OnClickListener {
    public static final String SUBJECT_ID = "subject_id";
    public static final String ADMISSION_PERCENTAGE_ID = "ap_id";
    public static final String SUBJECT_IS_NEW = "subject_is_new";
    public static final String RECURRENCE_STRING = "percentage_recurrence_string";

    private int mSubjectId, mPercentageTrackerId;

    private DBPercentageTracker mDb;

    //views needed for configuring a new percentage counter
    private EditText nameInput;
    private TextView
            mBaselinePercentageCurrentValueTextView,
            mEstimatedAssignmentsPerLessonCurrentValueTextView,
            mEstimatedLessonCountCurrentValueTextView;
    private SeekBar
            mBaselinePercentageSeekBar,
            mEstimatedAssignmentsPerLessonSeekBar,
            mEstimatedLessonCountSeekBar;

    /**
     * Spinner to show the different choices for the estimation mode
     */
    private Spinner mEstimationModeSpinner;

    /**
     * Switch to set notification enabled setting
     */
    private CheckBox mNotificationEnabledCheckBox;

    /**
     * Button used to display the current notification dow and trigger the dow dialog
     */
    private Button mNotificationDowButton;

    /**
     * Button used to display the current notification time and trigger the time dialog
     */
    private Button mNotificationTimeButton;

    /**
     * SeekBar for choosing the value of the required percentage for getting a bonus
     */
    private SeekBar mBonusPercentageSeekBar;

    /**
     * CheckBox to enable the bonus percentage system
     */
    private CheckBox mBonusRequiredPercentageActivationCheckBox;

    /**
     * TextView to show the currently chosen bonus value
     */
    private TextView mBonusPercentageCurrentValueTextView;

    /**
     * Textview showing the title of the settings layout
     */
    private TextView mBonusRequiredPercentageTitleTextView;

    /**
     * whether or not the recurring notifications are enabled
     */
    private boolean mNotificationEnabledHint = false;

    /**
     * If it exists, this string contains the old recurrence string
     */
    @SuppressWarnings("FieldCanBeLocal")
    private String mNotificationRecurrenceStringHint = "0:0:0";

    /**
     * This string is:
     * empty, if there was no recurrence string and none was set
     * ==hint, if the old recurrence string was not changed,
     * the new recurrence string, if a new one was set
     */
    private String mNotificationRecurrenceStringCurrent;

    /**
     * Integer containing the default or the old value for the bonus percentage
     */
    private int mBonusPercentageHint = 80;

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

    public PercentageTrackerCreationFragment() {
    }

    public static PercentageTrackerCreationFragment newInstance(int subjectId, int admissionCounterId, boolean isNew) {
        PercentageTrackerCreationFragment fragment = new PercentageTrackerCreationFragment();
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
            mPercentageTrackerId = getArguments().getInt(ADMISSION_PERCENTAGE_ID);
            mIsNewPercentageCounter = getArguments().getBoolean(SUBJECT_IS_NEW);
            mIsOldPercentageCounter = !mIsNewPercentageCounter;
            mDb = DBPercentageTracker.getInstance();

            //create a savepoint now, so we can rollback if the user decides to abort
            mSavepointId = "APCMETA" + mSubjectId;
            mDb.createSavepoint(mSavepointId);

            if (mIsNewPercentageCounter) {
                //create a new meta entry for this so we have an id to work with
                mPercentageTrackerId = mDb.addItemGetId(new PercentageTrackerPojo(-1, mSubjectId, 0, 0, 0, "if you see this, i fucked up.", "undefined", 0, false, "", false));

                PercentageTrackerCreationActivity parent = (PercentageTrackerCreationActivity) getActivity();

                //hide the delete button if this is a new subject
                parent.hideDeleteButton();

                //update the id variable in the parent
                parent.updatePercentageTrackerId(mPercentageTrackerId);
            }


        } else
            throw new AssertionError("why are there no arguments here?");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.creation_percentage_tracker_fragment, container, false);
        nameInput = (EditText) view.findViewById(R.id.percentage_creator_name_edittext);

        View baselinePercentageLayout = view.findViewById(R.id.percentage_creator_baseline_percentage_layout);
        mBaselinePercentageCurrentValueTextView = (TextView) baselinePercentageLayout.findViewById(R.id.current);
        mBaselinePercentageSeekBar = (SeekBar) baselinePercentageLayout.findViewById(R.id.seekBar);
        ((TextView) baselinePercentageLayout.findViewById(R.id.title)).setText(R.string.subjectmanager_needed_work);

        View estimatedAssignmentsPerLessonLayout = view.findViewById(R.id.percentage_creator_estimated_assignments_per_lesson_layout);
        mEstimatedAssignmentsPerLessonCurrentValueTextView = (TextView) estimatedAssignmentsPerLessonLayout.findViewById(R.id.current);
        mEstimatedAssignmentsPerLessonSeekBar = (SeekBar) estimatedAssignmentsPerLessonLayout.findViewById(R.id.seekBar);
        ((TextView) estimatedAssignmentsPerLessonLayout.findViewById(R.id.title)).setText(R.string.subjectmanager_assignments_per_lesson);

        View estimatedLessonCountLayout = view.findViewById(R.id.percentage_creator_estimated_lesson_count_layout);
        mEstimatedLessonCountCurrentValueTextView = (TextView) estimatedLessonCountLayout.findViewById(R.id.current);
        mEstimatedLessonCountSeekBar = (SeekBar) estimatedLessonCountLayout.findViewById(R.id.seekBar);
        ((TextView) estimatedLessonCountLayout.findViewById(R.id.title)).setText(R.string.subjectmanager_lesson_count);

        mEstimationModeSpinner = (Spinner) view.findViewById(R.id.percentage_creator_estimation_mode_spinner);

        View bonusEnabledLayout = view.findViewById(R.id.percentage_creator_bonus_enabled_layout);
        mBonusRequiredPercentageActivationCheckBox = (CheckBox) bonusEnabledLayout.findViewById(R.id.checkbox);
        ((TextView) bonusEnabledLayout.findViewById(R.id.title)).setText(R.string.percentage_creator_bonus_enabled_title);
        ((TextView) bonusEnabledLayout.findViewById(R.id.summary)).setText(R.string.percentage_creator_bonus_enabled_summary);
        bonusEnabledLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBonusRequiredPercentageActivationCheckBox.setChecked(
                        !mBonusRequiredPercentageActivationCheckBox.isChecked());
                enableBonusPercentageSettingsLayout(mBonusRequiredPercentageActivationCheckBox.isChecked());
            }
        });

        View bonusSettingsLayout = view.findViewById(R.id.percentage_creator_bonus_percentage_layout);
        mBonusPercentageSeekBar = (SeekBar) bonusSettingsLayout.findViewById(R.id.seekBar);
        mBonusPercentageCurrentValueTextView = (TextView) bonusSettingsLayout.findViewById(R.id.current);
        mBonusRequiredPercentageTitleTextView = (TextView) bonusSettingsLayout.findViewById(R.id.title);
        mBonusRequiredPercentageTitleTextView.setText(R.string.percentage_creator_bonus_percentage_settigns_title);

        View notificationEnabledLayout = view.findViewById(R.id.percentage_creator_notification_enabled_layout);
        mNotificationEnabledCheckBox = (CheckBox) notificationEnabledLayout.findViewById(R.id.checkbox);
        ((TextView) notificationEnabledLayout.findViewById(R.id.title)).setText(R.string.percentage_creator_notification_enabled_title);
        ((TextView) notificationEnabledLayout.findViewById(R.id.summary)).setText(R.string.percentage_creator_notification_enabled_summary);
        notificationEnabledLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNotificationEnabledCheckBox.setChecked(!mNotificationEnabledCheckBox.isChecked());
                updateNotificationButtons(mNotificationEnabledCheckBox.isChecked());
            }
        });

        View notificationSettingsLayout = view.findViewById(R.id.percentage_creator_notification_buttons_layout);
        mNotificationDowButton = (Button) notificationSettingsLayout.findViewById(R.id.left_button);
        mNotificationDowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDowClicked();
            }
        });
        mNotificationTimeButton = (Button) notificationSettingsLayout.findViewById(R.id.right_button);
        mNotificationTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTimeClicked();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        PercentageTrackerPojo oldState = mDb.getItem(mPercentageTrackerId);

        //extract subject data
        nameHint = oldState.name;
        requiredPercentageHint = oldState.baselineTargetPercentage;
        estimatedAssignmentsHint = oldState.userAssignmentsPerLessonEstimation;
        estimatedLessonCountHint = oldState.userLessonCountEstimation;
        mEstimationModeHint = oldState.getEstimationModeAsString();
        mBonusPercentageHint = oldState.bonusTargetPercentage;
        mBonusRequiredPercentageEnabledHint = oldState.bonusTargetPercentageEnabled;

        mNotificationEnabledHint = oldState.notificationEnabled;
        mNotificationRecurrenceStringHint = oldState.notificationRecurrenceString;
        mNotificationRecurrenceStringCurrent = mNotificationRecurrenceStringHint;

        //set the parent activity title
        ((PercentageTrackerCreationActivity) getActivity()).setToolbarTitle(getTitle());

        // try to remove the notification for this. when saving, it will be regenerated if the check-
        // box is checked.
        NotificationGeneralHelper.removeAlarmForNotification(getActivity(), mSubjectId, mPercentageTrackerId);
    }

    /**
     * Either set default values to all views in the creator or set the previously loaded old values
     */
    private void setValuesForViews() {
        //display an error if the edittext is empty
        final String emptyError = getActivity().getString(R.string.edit_text_empty_error_text);
        nameInput.addTextChangedListener(new FakeDisabledNotEmptyWatcher(
                nameInput,
                emptyError,
                nameInput.getText().length() == 0,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nameInput.setError(emptyError);
                    }
                },
                this,
                ((PercentageTrackerCreationActivity) getActivity()).getSaveButton()
        ));

        //set old name as hint
        if (mIsOldPercentageCounter)
            nameInput.setText(nameHint);

        initSeekbar(mBaselinePercentageSeekBar, mBaselinePercentageCurrentValueTextView, requiredPercentageHint, 100);
        initSeekbar(mBonusPercentageSeekBar, mBonusPercentageCurrentValueTextView, mBonusPercentageHint, 100);

        initSeekbar(mEstimatedAssignmentsPerLessonSeekBar, mEstimatedAssignmentsPerLessonCurrentValueTextView, estimatedAssignmentsHint, 50);
        initSeekbar(mEstimatedLessonCountSeekBar, mEstimatedLessonCountCurrentValueTextView, estimatedLessonCountHint, 50);

        createBonusSeekbarLimitEnforcer();

        initEstimationModeSpinner();

        initBonusCheckBox();

        updateNotificationButtons(mNotificationEnabledHint);

        mNotificationEnabledCheckBox.setChecked(mNotificationEnabledHint);
    }

    private void createBonusSeekbarLimitEnforcer() {
        mBaselinePercentageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBaselinePercentageCurrentValueTextView.setText(String.format(
                        General.getCurrentLocale(getActivity()), "%d", progress));
                int bonusCurrent = mBonusPercentageSeekBar.getProgress();
                if (bonusCurrent < progress)
                    mBonusPercentageSeekBar.setProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mBonusPercentageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int adjustedProgress = progress;
                int baselineProgress = mBaselinePercentageSeekBar.getProgress();
                if (progress < baselineProgress)
                    adjustedProgress = baselineProgress;
                seekBar.setProgress(adjustedProgress);
                mBonusPercentageCurrentValueTextView.setText(String.format(
                        General.getCurrentLocale(getActivity()),
                        "%d",
                        adjustedProgress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void initBonusCheckBox() {
        enableBonusPercentageSettingsLayout(mBonusRequiredPercentageEnabledHint);

        mBonusRequiredPercentageActivationCheckBox.setChecked(mBonusRequiredPercentageEnabledHint);
    }

    private void enableBonusPercentageSettingsLayout(boolean enable) {
        mBonusPercentageSeekBar.setEnabled(enable);
        mBonusPercentageCurrentValueTextView.setEnabled(enable);
        mBonusRequiredPercentageTitleTextView.setEnabled(enable);
    }

    private void initEstimationModeSpinner() {
        PercentageTrackerPojo.EstimationMode estimationMode = PercentageTrackerPojo.EstimationMode.valueOf(mEstimationModeHint);

        mEstimationModeSpinner.setAdapter(new ForecastModeSpinnerAdapter(getActivity()));
        mEstimationModeSpinner.setSelection(estimationMode.ordinal());

        mEstimationModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onEstimationModeChange(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        onEstimationModeChange(estimationMode.ordinal());
    }

    private void onEstimationModeChange(int seekbarProgress) {
        switch (PercentageTrackerPojo.EstimationMode.values()[seekbarProgress]) {
            case user://user
            case worst://user
            case best://user
            case mean://user
                mCurrentEstimationMode = PercentageTrackerPojo.EstimationMode.values()[seekbarProgress].name();
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

    /**
     * Save the database changes
     *
     * @return appropriate result code (changed/new)
     */
    private int save() {
        //change the item we created at the beginning of this dialog to the actual values
        mDb.changeItem(new PercentageTrackerPojo(
                mPercentageTrackerId,
                mSubjectId,
                mEstimatedAssignmentsPerLessonSeekBar.getProgress(),
                mEstimatedLessonCountSeekBar.getProgress(),
                mBaselinePercentageSeekBar.getProgress(),
                nameInput.getText().toString(),
                mCurrentEstimationMode,
                mBonusPercentageSeekBar.getProgress(),
                mBonusRequiredPercentageActivationCheckBox.isChecked(),
                mNotificationRecurrenceStringCurrent,
                mNotificationEnabledCheckBox.isChecked()
        ));

        checkNotificationSettings(false/*not canceling*/);

        //commit
        mDb.releaseSavepoint(mSavepointId);
        //notify the host fragment that we changed an item
        if (mIsNewPercentageCounter)
            return SubjectCreationActivity.DIALOG_RESULT_ADDED;
        else
            return SubjectCreationActivity.DIALOG_RESULT_CHANGED;
    }

    /**
     * This function checks whether we need to recreate the notification alarm, and if so, calls
     * the host activity to set the recurrence string.
     */
    private void checkNotificationSettings(boolean canceling) {
        boolean addNotification = false;
        boolean saving = !canceling;

        // new tracker? only the isChecked state is relevant for adding a notification
        if (saving)
            addNotification = mNotificationEnabledCheckBox.isChecked();

        if (canceling) {
            if (mIsNewPercentageCounter)
                addNotification = false;
            else if (mIsOldPercentageCounter)
                addNotification = mNotificationEnabledHint;
        }

        if (addNotification) {
            PercentageTrackerCreationActivity host = (PercentageTrackerCreationActivity) getActivity();
            //current gets set to hint, so even if not changed, it holds the correct value
            host.setRecurrenceString(mNotificationRecurrenceStringCurrent);
        }
    }

    @Override
    public void onClick(View v) {
        int resultState = -1;
        switch (v.getId()) {
            case R.id.giant_delete_button:
                mDb.rollbackToSavepoint(mSavepointId);
                resultState = SubjectCreationActivity.DIALOG_RESULT_DELETE;
                break;
            case R.id.giant_ok_button:
                resultState = save();
                break;
            case R.id.giant_cancel_button:
                mDb.rollbackToSavepoint(mSavepointId);
                checkNotificationSettings(true/*canceling*/);
                resultState = SubjectCreationActivity.DIALOG_RESULT_CLOSED;
                break;
        }
        PercentageTrackerCreationActivity host = (PercentageTrackerCreationActivity) getActivity();
        host.finishToSubjectCreator(resultState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == Permissions.getRequestCode(android.Manifest.permission.RECEIVE_BOOT_COMPLETED))
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(getActivity(), R.string.permissions_reboot_receiver_success, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getActivity(), R.string.permissions_reboot_receiver_failure, Toast.LENGTH_LONG).show();
    }

    /**
     * Get the cow/time from the currernt recurrence string and set the button texts appropriately
     */
    private void updateNotificationButtons(boolean enableButtons) {
        //we need to activate the boot receiver, so we check whether the permission is given
        if (enableButtons) {
            if (!Permissions.hasPermission(getActivity(), android.Manifest.permission.RECEIVE_BOOT_COMPLETED)) {
                Permissions.requestPermission(getActivity(), android.Manifest.permission.RECEIVE_BOOT_COMPLETED);
            }
        }
        Calendar currentCalendar = NotificationGeneralHelper.getCalendar(mNotificationRecurrenceStringCurrent);

        if (currentCalendar != null) {
            Date currentDate = new Date(currentCalendar.getTimeInMillis());
            SimpleDateFormat dowFormat = new SimpleDateFormat("EEEE",
                    General.getCurrentLocale(getActivity()));
            SimpleDateFormat timeFormat = new SimpleDateFormat("kk:mm",
                    General.getCurrentLocale(getActivity()));

            mNotificationDowButton.setText(dowFormat.format(currentDate));
            mNotificationTimeButton.setText(timeFormat.format(currentDate));
        } else {
            mNotificationDowButton.setText(R.string.percentage_creator_dow_empty_button_text);
            mNotificationTimeButton.setText(R.string.percentage_creator_time_empty_button_text);
        }

        mNotificationDowButton.setEnabled(enableButtons);
        mNotificationTimeButton.setEnabled(enableButtons);

        //noinspection deprecation
        int color = getResources().getColor(enableButtons ?
                R.color.enabled_button_text : R.color.disabled_button_text);

        mNotificationDowButton.setTextColor(color);
        mNotificationTimeButton.setTextColor(color);
    }

    private void onTimeClicked() {
        final int[] settings = NotificationGeneralHelper.
                convertFromRecurrenceRule(mNotificationRecurrenceStringCurrent);
        TimePickerDialog dialog = new TimePickerDialog(
                getActivity(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        mNotificationRecurrenceStringCurrent = settings[0] + ":" + hour + ":" + minute;
                        updateNotificationButtons(mNotificationEnabledCheckBox.isChecked());
                    }
                },
                settings[1],
                settings[2],
                true
        );

        dialog.show();
    }

    private void onDowClicked() {
        final int[] settings = NotificationGeneralHelper.
                convertFromRecurrenceRule(mNotificationRecurrenceStringCurrent);
        DayOfWeekPickerDialog d = DayOfWeekPickerDialog.newInstance(settings[0]);
        d.setOnDowPickedListener(new DayOfWeekPickerDialog.DowPickedListener() {
            @Override
            public void onDowPicked(int dow) {
                mNotificationRecurrenceStringCurrent = dow + ":" + settings[1] + ":" + settings[2];
                updateNotificationButtons(mNotificationEnabledCheckBox.isChecked());
            }
        });
        d.show(getFragmentManager(), "votenote dowpicker");
    }
}