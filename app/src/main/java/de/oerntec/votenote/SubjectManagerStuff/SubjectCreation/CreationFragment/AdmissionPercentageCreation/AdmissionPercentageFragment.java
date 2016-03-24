package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment.AdmissionPercentageCreation;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Helpers.DowPickerDialog;
import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.Helpers.Notifications.NotificationGeneralHelper;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SeekerListener;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.SubjectOverview.SubjectCreationActivity;

public class AdmissionPercentageFragment extends Fragment implements Button.OnClickListener {
    public static final String SUBJECT_ID = "subject_id";
    public static final String ADMISSION_PERCENTAGE_ID = "ap_id";
    public static final String SUBJECT_IS_NEW = "subject_is_new";

    private int mSubjectId, mAdmissionPercentageId;

    private DBAdmissionPercentageMeta mDb;

    //views needed for configuring a new percentage counter
    private EditText nameInput;
    private TextView
            mRequiredPercentageCurrentValueTextView,
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
                mAdmissionPercentageId = mDb.addItemGetId(new AdmissionPercentageMetaPojo(-1, mSubjectId, 0, 0, 0, "if you see this, i fucked up.", "undefined", 0, false, "", false));

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
        View view = inflater.inflate(R.layout.subject_manager_percentage_creator_fragment, container, false);
        nameInput = (EditText) view.findViewById(R.id.percentage_creator_name_edittext);

        View baselinePercentageLayout = view.findViewById(R.id.percentage_creator_baseline_percentage_layout);
        mRequiredPercentageCurrentValueTextView = (TextView) baselinePercentageLayout.findViewById(R.id.current);
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

        View bonusSettingsLayout = view.findViewById(R.id.percentage_creator_bonus_percentage_layout);
        mBonusPercentageSeekBar = (SeekBar) bonusSettingsLayout.findViewById(R.id.seekBar);
        mBonusPercentageCurrentValueTextView = (TextView) bonusSettingsLayout.findViewById(R.id.current);
        mBonusRequiredPercentageTitleTextView = (TextView) bonusSettingsLayout.findViewById(R.id.title);
        mBonusRequiredPercentageTitleTextView.setText(R.string.percentage_creator_bonus_percentage_settigns_title);

        View notificationEnabledLayout = view.findViewById(R.id.percentage_creator_notification_enabled_layout);
        mNotificationEnabledCheckBox = (CheckBox) notificationEnabledLayout.findViewById(R.id.checkbox);
        ((TextView) notificationEnabledLayout.findViewById(R.id.title)).setText(R.string.percentage_creator_notification_enabled_title);
        ((TextView) notificationEnabledLayout.findViewById(R.id.summary)).setText(R.string.percentage_creator_notification_enabled_summary);

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
        AdmissionPercentageMetaPojo oldState = mDb.getItem(mAdmissionPercentageId);

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
        ((AdmissionPercentageCreationActivity) getActivity()).setToolbarTitle(getTitle());

        // try to remove the notification for this. when saving, it will be regenerated if the check-
        // box is checked.
        NotificationGeneralHelper.removeAlarmForNotification(getActivity(), mSubjectId, mAdmissionPercentageId);
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

        initSeekbar(mBaselinePercentageSeekBar, mRequiredPercentageCurrentValueTextView, requiredPercentageHint, 100);
        initSeekbar(mBonusPercentageSeekBar, mBonusPercentageCurrentValueTextView, mBonusPercentageHint, 100);

        initSeekbar(mEstimatedAssignmentsPerLessonSeekBar, mEstimatedAssignmentsPerLessonCurrentValueTextView, estimatedAssignmentsHint, 50);
        initSeekbar(mEstimatedLessonCountSeekBar, mEstimatedLessonCountCurrentValueTextView, estimatedLessonCountHint, 50);

        createBonusSeekbarLimitEnforcer();

        initEstimationModeSpinner();

        initBonusCheckBox();

        updateNotificationButtons(mNotificationEnabledHint);

        mNotificationEnabledCheckBox.setChecked(mNotificationEnabledHint);
        mNotificationEnabledCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateNotificationButtons(isChecked);
            }
        });
    }

    private void createBonusSeekbarLimitEnforcer() {
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
        mBonusPercentageSeekBar.setEnabled(mBonusRequiredPercentageEnabledHint);
        mBonusPercentageCurrentValueTextView.setEnabled(mBonusRequiredPercentageEnabledHint);
        mBonusRequiredPercentageTitleTextView.setEnabled(mBonusRequiredPercentageEnabledHint);

        mBonusRequiredPercentageActivationCheckBox.setChecked(mBonusRequiredPercentageEnabledHint);

        mBonusRequiredPercentageActivationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mBonusPercentageSeekBar.setEnabled(isChecked);
                mBonusPercentageCurrentValueTextView.setEnabled(isChecked);
                mBonusRequiredPercentageTitleTextView.setEnabled(isChecked);
            }
        });

    }

    private void initEstimationModeSpinner() {
        AdmissionPercentageMetaPojo.EstimationMode estimationMode = AdmissionPercentageMetaPojo.EstimationMode.valueOf(mEstimationModeHint);

        mEstimationModeSpinner.setAdapter(new EstimationModeAdapter(getActivity(), 0));
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
        switch (AdmissionPercentageMetaPojo.EstimationMode.values()[seekbarProgress]) {
            case user://user
            case worst://user
            case best://user
            case mean://user
                mCurrentEstimationMode = AdmissionPercentageMetaPojo.EstimationMode.values()[seekbarProgress].name();
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

    private int save() {
        //change the item we created at the beginning of this dialog to the actual values
        mDb.changeItem(new AdmissionPercentageMetaPojo(
                mAdmissionPercentageId,
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

        // do we need to set an alarm?
        // this works, by the way, because the current string is the hint if not changed, so it
        // contains the old string
        if (mNotificationEnabledCheckBox.isChecked()) {
            NotificationGeneralHelper.setAlarmForNotification(getActivity(),
                    mSubjectId,
                    mAdmissionPercentageId,
                    mNotificationRecurrenceStringCurrent);
            // because this is only the percentage creation dialog, we would have to somehow give
            // the parent activity data about the notification time to have really correct save
            // behaviour. Since that would be quite tedious, we simply show a notification to
            // alarm the user that an alarm has now been set.
            // also, only show the toast if state changed
            if (!mNotificationEnabledHint)
                Toast.makeText(getActivity(), R.string.percentage_creator_notification_activated_toast, Toast.LENGTH_LONG).show();
        } else if (mNotificationEnabledHint)
            Toast.makeText(getActivity(), R.string.subject_creator_notification_deactivated_toast, Toast.LENGTH_LONG).show();

        //commit
        mDb.releaseSavepoint(mSavepointId);
        //notify the host fragment that we changed an item
        if (mIsNewPercentageCounter)
            return SubjectCreationActivity.DIALOG_RESULT_ADDED;
        else
            return SubjectCreationActivity.DIALOG_RESULT_CHANGED;
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
                resultState = SubjectCreationActivity.DIALOG_RESULT_CLOSED;
                break;
        }
        AdmissionPercentageCreationActivity host = (AdmissionPercentageCreationActivity) getActivity();
        host.callCreatorFragmentForItemChange(resultState);
    }

    /**
     * Get the cow/time from the currernt recurrence string and set the button texts appropriately
     */
    private void updateNotificationButtons(boolean enableButtons) {
        Calendar currentCalendar = NotificationGeneralHelper.getCalendar(mNotificationRecurrenceStringCurrent);

        if (currentCalendar != null) {
            Date currentDate = new Date(currentCalendar.getTimeInMillis());
            SimpleDateFormat dowFormat = new SimpleDateFormat("EEEE");
            SimpleDateFormat timeFormat = new SimpleDateFormat("kk:mm");

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
        DowPickerDialog d = DowPickerDialog.newInstance(settings[0]);
        d.setOnDowPickedListener(new DowPickerDialog.DowPickedListener() {
            @Override
            public void onDowPicked(int dow) {
                mNotificationRecurrenceStringCurrent = dow + ":" + settings[1] + ":" + settings[2];
                updateNotificationButtons(mNotificationEnabledCheckBox.isChecked());
            }
        });
        d.show(getFragmentManager(), "votenote dowpicker");
    }
}