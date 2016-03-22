package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment.AdmissionPercentageCreation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import android.widget.Toast;

import com.github.jjobes.slidedaytimepicker.SlideDayTimeListener;
import com.github.jjobes.slidedaytimepicker.SlideDayTimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
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
    private TextView
            mRequiredPercentageCurrentValueTextView,
            mEstimatedAssignmentsPerLessonCurrentValueTextView,
            mEstimatedLessonCountCurrentValueTextView;
    private SeekBar
            mRequiredPercentageSeekBar,
            mEstimatedAssignmentsPerLessonSeekBar,
            mEstimatedLessonCountSeekBar;

    /**
     * Spinner to show the different choices for the estimation mode
     */
    private Spinner mEstimationModeSpinner;

    /**
     * Switch to set notification enabled setting
     */
    private CheckBox mNotificationEnabledSwitch;

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
     * the new recurrence string, if a new one was set
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

    /**
     * TextView used for showing displaying the current notification settings
     */
    private TextView mNoticationCurrentSettingsTextView;

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
        mRequiredPercentageSeekBar = (SeekBar) baselinePercentageLayout.findViewById(R.id.seekBar);
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

        mBonusRequiredPercentageActivationCheckBox = (CheckBox) view.findViewById(R.id.percentage_creator_bonus_checkbox);
        mBonusRequiredPercentageSeekBar = (SeekBar) view.findViewById(R.id.percentage_creator_bonus_seekbar);
        mBonusRequiredPercentageCurrentValueTextView = (TextView) view.findViewById(R.id.percentage_creator_bonus_current);

        mNotificationEnabledSwitch = (CheckBox) view.findViewById(R.id.percentage_creator_notification_checkbox);
        mNoticationCurrentSettingsTextView = (TextView) view.findViewById(R.id.percentage_creator_notification_current);

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
        mBonusRequiredPercentageHint = oldState.bonusTargetPercentage;
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

        updateCurrentNotificationSettingsView();

        initSeekbar(mRequiredPercentageSeekBar, mRequiredPercentageCurrentValueTextView, requiredPercentageHint, 100);
        initSeekbar(mBonusRequiredPercentageSeekBar, mBonusRequiredPercentageCurrentValueTextView, mBonusRequiredPercentageHint, 100);

        initSeekbar(mEstimatedAssignmentsPerLessonSeekBar, mEstimatedAssignmentsPerLessonCurrentValueTextView, estimatedAssignmentsHint, 50);
        initSeekbar(mEstimatedLessonCountSeekBar, mEstimatedLessonCountCurrentValueTextView, estimatedLessonCountHint, 50);

        initEstimationModeSpinner();

        initBonusCheckBox();

        mNotificationEnabledSwitch.setChecked(mNotificationEnabledHint);
        mNotificationEnabledSwitch.setOnCheckedChangeListener(this);
    }

    private void initBonusCheckBox() {
        mBonusRequiredPercentageActivationCheckBox.setChecked(mBonusRequiredPercentageEnabledHint);
        mBonusRequiredPercentageSeekBar.setEnabled(mBonusRequiredPercentageEnabledHint);
        mBonusRequiredPercentageCurrentValueTextView.setVisibility(mBonusRequiredPercentageEnabledHint
                ? View.VISIBLE : View.INVISIBLE);

        mBonusRequiredPercentageActivationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mBonusRequiredPercentageSeekBar.setEnabled(isChecked);
                mBonusRequiredPercentageCurrentValueTextView.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
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
                mRequiredPercentageSeekBar.getProgress(),
                nameInput.getText().toString(),
                mCurrentEstimationMode,
                mBonusRequiredPercentageSeekBar.getProgress(),
                mBonusRequiredPercentageActivationCheckBox.isChecked(),
                mNotificationRecurrenceStringCurrent,
                mNotificationEnabledSwitch.isChecked()
        ));

        // do we need to set an alarm?
        // this works, by the way, because the current string is the hint if not changed, so it
        // contains the old string
        if (mNotificationEnabledSwitch.isChecked()) {
            NotificationGeneralHelper.setAlarmForNotification(getActivity(),
                    mSubjectId,
                    mAdmissionPercentageId,
                    mNotificationRecurrenceStringCurrent);
            // because this is only the percentage creation dialog, we would have to somehow give
            // the parent activity data about the notification time to have really correct save
            // behaviour. Since that would be quite tedious, we simply show a notification to
            // alarm the user that an alarm has now been set.
            Toast.makeText(getActivity(), R.string.percentage_creator_notification_activated_toast, Toast.LENGTH_LONG).show();
        } else
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

    private void updateCurrentNotificationSettingsView() {
        Calendar currentCalendar = NotificationGeneralHelper.getCalendar(mNotificationRecurrenceStringCurrent);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, kk:mm");
        if (currentCalendar != null)
            mNoticationCurrentSettingsTextView.setText(dateFormat.format(new Date(currentCalendar.getTimeInMillis())));
        else
            mNoticationCurrentSettingsTextView.setVisibility(View.GONE);
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            SlideDayTimePicker.Builder builder = new SlideDayTimePicker.Builder(getFragmentManager());
            builder.setListener(new SlideDayTimeListener() {
                @Override
                public void onDayTimeSet(int day, int hour, int minute) {
                    mNotificationRecurrenceStringCurrent = day + ":" + hour + ":" + minute;

                    updateCurrentNotificationSettingsView();

                    mNoticationCurrentSettingsTextView.setVisibility(View.VISIBLE);

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
            mNotificationRecurrenceStringCurrent = null;
            mNoticationCurrentSettingsTextView.setVisibility(View.GONE);
        }
    }
}