package de.oerntec.votenote.subject_management.subject_creation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.DatabaseCreator;
import de.oerntec.votenote.database.pojo.Subject;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;
import de.oerntec.votenote.helpers.General;
import de.oerntec.votenote.helpers.dialogs.Dialogs;
import de.oerntec.votenote.helpers.lists.LinearLayoutManagerWrapper;
import de.oerntec.votenote.helpers.lists.OnItemClickListener;
import de.oerntec.votenote.helpers.lists.RecyclerItemClickListener;
import de.oerntec.votenote.helpers.notifications.NotificationGeneralHelper;
import de.oerntec.votenote.subject_management.SubjectManagementListActivity;
import de.oerntec.votenote.subject_management.percentage_tracker_creation.PercentageTrackerCreationActivity;
import de.oerntec.votenote.subject_management.percentage_tracker_creation.PercentageTrackerCreationFragment;

/**
 * This fragment holds the actual ui/ux for creating a new subject, as opposed to the subject creation
 * activity, which merely contains this
 */
public class SubjectCreationFragment extends Fragment implements SubjectCreationDialogInterface, View.OnClickListener {
    /**
     * ID used for adding percentage or counter
     */
    public static final int ID_ADD_ITEM = -2;
    /**
     * Lesson should be added, not changed
     */
    private static final int ADD_SUBJECT_CODE = -1;
    private final NotificationReminder mNotificationReminder = new NotificationReminder();
    /**
     * DB for the subjects
     */
    private DBSubjects mSubjectDb;

    /**
     * Need direct database access to start/end transaction
     */
    private SQLiteDatabase mDatabase;

    /**
     * Counter list
     */
    @SuppressWarnings("FieldCanBeLocal")
    private RecyclerView mList;

    /**
     * adapters for Admission counters, admission percentage counters
     */
    private UnifiedCreatorAdapter mAdapter;

    /**
     * Database id of current subject
     */
    private int mSubjectId;

    /**
     * Position this subject had when if it was already displayed in the recyclerView of subject-
     * managementActivity
     */
    private int mRecyclerViewPosition;

    /**
     * convenient info about whether current subject is old or new
     */
    private boolean mIsOldSubject, mIsNewSubject;

    /**
     * Set this to true if the subject was changed in any way, so we can prompt the user for save/abort
     */
    private boolean mSubjectHasBeenChanged = false;

    /**
     * save the old subject name here because otherwise we will not know whether the name has actually changed
     * initialised as empty string, because if you create a new subject the name is empty
     */
    private String mOldSubjectName = "";

    /**
     * This is the delete button in the giant action bar, we need a class variable in case this is a
     * new subject and we want to hide it
     */
    private Button mDeleteButton;

    public SubjectCreationFragment() {
    }

    public static SubjectCreationFragment newInstance(int subjectId, int subjectPosition) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(SubjectCreationActivity.ARG_CREATOR_SUBJECT_ID, subjectId);
        args.putInt(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, subjectPosition);

        SubjectCreationFragment fragment = new SubjectCreationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get arguments from intent
        mSubjectId = getArguments().getInt(SubjectCreationActivity.ARG_CREATOR_SUBJECT_ID, -1);
        mRecyclerViewPosition = getArguments().getInt(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, -1);

        //create a boolean containing whether we create a new subject to ease understanding
        mIsNewSubject = mSubjectId == ADD_SUBJECT_CODE;
        mIsOldSubject = !mIsNewSubject;

        //if this is a new subject, we have to create a subject now to know the subject id, which is conveniently returned by the addItemGetId method
        if (mIsNewSubject)
            mSubjectId = mSubjectDb.addItemGetId("If you see this, i fucked up.");

        setHasOptionsMenu(true);
    }

    /**
     * yeah so this is deprecated in api 23, but the replacement method is not called in api<23 except if you use support fragment
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //get a database
        DatabaseCreator dbHelper = DatabaseCreator.getInstance(getActivity().getApplicationContext());
        mDatabase = dbHelper.getWritableDatabase();

        mSubjectDb = DBSubjects.getInstance();

        if (mSubjectDb == null) throw new AssertionError("could not get a database instance?");

        //begin transaction on database to enable using commit and abort
        if (MainActivity.ENABLE_TRANSACTION_LOG)
            Log.i("transact. log", "starting SubjectCreationFragment transaction in onAttach");
        mDatabase.beginTransaction();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_subject_creation, menu);

        if (mIsNewSubject)
            mDeleteButton.setVisibility(View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //inflate view, extract views
        View input = inflater.inflate(R.layout.subject_manager_fragment_subject_settings, container, false);

        if (mIsOldSubject) {
            Subject oldData = mSubjectDb.getItem(mSubjectId);
            mOldSubjectName = oldData.name;
        }

        //this is the list containing both admission counters and admission percentage counters
        mList = (RecyclerView) input.findViewById(R.id.subject_creator_unified_counter_list);

        //find the giant button bar and attach the listeners
        Button okButton = (Button) input.findViewById(R.id.giant_ok_button);
        okButton.setOnClickListener(this);

        input.findViewById(R.id.giant_cancel_button).setOnClickListener(this);

        mDeleteButton = (Button) input.findViewById(R.id.giant_delete_button);
        mDeleteButton.setOnClickListener(this);

        initializeList(mList, okButton);

        return input;
    }

    @SuppressWarnings("unchecked")
    private void initializeList(RecyclerView recyclerView, final Button okButton) {
        //config the recyclerview
        recyclerView.setHasFixedSize(true);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManagerWrapper(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);

        mAdapter = new UnifiedCreatorAdapter(
                getActivity(),
                this,
                getFragmentManager(),
                mIsNewSubject ? null : mOldSubjectName,
                mSubjectId,
                okButton,
                this);
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), recyclerView, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if (mAdapter.getItemViewType(position) == UnifiedCreatorAdapter.VIEW_COUNTER)
                    Dialogs.showCounterDialog(getFragmentManager(), mSubjectId, (int) view.getTag(), false);
                else if (mAdapter.getItemViewType(position) == UnifiedCreatorAdapter.VIEW_PERCENTAGE) {
                    openAdmissionPercentageCreationActivity((int) view.getTag(), false);
                }
            }

            public void onItemLongClick(final View view, final int position) {
                if ((int) view.getTag() != ID_ADD_ITEM) {
                    if (mAdapter.getItemViewType(position) == UnifiedCreatorAdapter.VIEW_COUNTER)
                        Dialogs.showDeleteDialog(
                                getActivity(),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteAdmissionCounter(mAdapter.getCounterPojoAtPosition(position).id);
                                    }
                                },
                                String.format(getString(R.string.dialog_delete_title), mAdapter.getCounterPojoAtPosition(position).name));
                    else if (mAdapter.getItemViewType(position) == UnifiedCreatorAdapter.VIEW_PERCENTAGE)
                        Dialogs.showDeleteDialog(
                                getActivity(),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteAdmissionPercentage(mAdapter.getPercentagePojoAtPosition(position).id);
                                    }
                                },
                                String.format(getString(R.string.dialog_delete_title), mAdapter.getPercentagePojoAtPosition(position).name));
                }
            }
        }));
    }

    void openAdmissionPercentageCreationActivity(int admissionPercentageId, boolean isNew) {
        Intent intent = new Intent(getActivity(), PercentageTrackerCreationActivity.class);
        intent.putExtra(PercentageTrackerCreationFragment.SUBJECT_ID, mSubjectId);
        intent.putExtra(PercentageTrackerCreationFragment.ADMISSION_PERCENTAGE_ID, admissionPercentageId);
        intent.putExtra(PercentageTrackerCreationFragment.SUBJECT_IS_NEW, isNew);
        startActivityForResult(intent, PercentageTrackerCreationActivity.INTENT_REQUEST_CODE_ADMISSION_PERCENTAGE_CREATOR);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                showBackConfirmation();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void abortAndClose() {
        if (MainActivity.ENABLE_TRANSACTION_LOG)
            Log.i("transact. log", "ending SubjectCreationFragment transaction in abortAndClose");

        mDatabase.endTransaction();
        getActivity().finish();
    }

    private boolean hasChanged() {
        return !mAdapter.getCurrentName().equals(mOldSubjectName) || mSubjectHasBeenChanged;
    }

    private void deleteCurrentSubject() {
        // abort if this is called for a new subject, that cant work
        if (mIsNewSubject)
            throw new AssertionError("tried to delete a new subject");

        // ok i promise the next app will have something resembling an architecture, this is getting
        // bad// delete all alarms for this subject
        NotificationGeneralHelper.removeAllAlarmsForSubject(getActivity(), mSubjectId);

        // set the result to be retrieved by subject manager
        setActivityResult(true);

        // kill creator activity
        getActivity().finish();
    }

    /**
     * Show a confirmation dialog to ask the user whether he really wants to leave
     */
    private void showBackConfirmation() {
        //if the subject did not change, we dont need to show this dialog
        boolean hasEmptyName = "".equals(mAdapter.getCurrentName());
        if (!hasChanged()) {
            if (mDatabase.inTransaction()) {
                if (MainActivity.ENABLE_TRANSACTION_LOG)
                    Log.i("transact. log", "ending SubjectCreationFragment transaction in showBackConfirmation");
                mDatabase.endTransaction();
            }
            getActivity().finish();
            return;
        }
        //create dialog if data was changed
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.subject_creation_fragment_back_confirmation_title);
        b.setMessage(R.string.subject_creation_fragment_back_confirmation_message);

        //create buttons
        b.setPositiveButton(R.string.dialog_button_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveAndClose();
            }
        });
        b.setNegativeButton(R.string.dialog_button_discard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                abortAndClose();
            }
        });
        b.setNeutralButton(R.string.dialog_button_abort, null);
        //check for invalid name
        AlertDialog dialog = b.show();
        if (hasEmptyName) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            dialog.setMessage(getActivity().getString(R.string.subject_creation_fragment_back_confirmation_empty_name_warning));
        }
    }

    private void setActivityResult(boolean delete) {
        if (delete)
            getActivity().setResult(SubjectManagementListActivity.SUBJECT_CREATOR_RESULT_DELETE, getCurrentResultIntent());
        else if (mIsNewSubject)
            getActivity().setResult(SubjectManagementListActivity.SUBJECT_CREATOR_RESULT_NEW, getCurrentResultIntent());
        else
            getActivity().setResult(SubjectManagementListActivity.SUBJECT_CREATOR_RESULT_CHANGED, getCurrentResultIntent());
    }

    /**
     * Write the new data out to db
     */
    private void saveAndClose() {
        String newName = mAdapter.getCurrentName();
        mSubjectDb.changeItem(new Subject(newName, mSubjectId));
        if (mDatabase == null)
            throw new AssertionError("no db reference?");
        if (MainActivity.ENABLE_TRANSACTION_LOG)
            Log.i("transact. log", "ending SubjectCreationFragment transaction successfully in saveAndClose");
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        //set notifications
        for (NotificationReminder.Reminder r : mNotificationReminder.notificationList)
            NotificationGeneralHelper.setAlarmForNotification(getActivity(),
                    mSubjectId,
                    r.trackerId,
                    r.recurrenceString);

        if (hasChanged())
            setActivityResult(false);

        // enable change listeners
        //mOldSubjectName = newName;
        //mSubjectHasBeenChanged = false;

        // because we may have some ugly problems if we simply open a new transaction to be able to
        // continue editing, we simply kill the activity...
        getActivity().finish();
    }

    /**
     * save key data about the current fragment subject in an intent, so that onActivityResult can send that intent
     *
     * @return result intent
     */
    private Intent getCurrentResultIntent() {
        //save result data into intent
        Intent returnIntent = new Intent();
        returnIntent.putExtra(SubjectCreationActivity.ARG_CREATOR_SUBJECT_ID, mSubjectId);
        returnIntent.putExtra(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, mRecyclerViewPosition);
        return returnIntent;
    }


    public void onBackPressed() {
        showBackConfirmation();
    }

    @Override
    public void admissionCounterFinished(int id, boolean isNew) {
        mSubjectHasBeenChanged = true;
        if (isNew)
            mAdapter.notifyIdAdded(id, false);
        else
            mAdapter.notifyOfChangeAtId(id, false);
        dialogClosed();
    }

    @Override
    public void deleteAdmissionCounter(int itemId) {
        mSubjectHasBeenChanged = true;
        mAdapter.removeCounter(itemId);
        dialogClosed();
    }

    @Override
    public void admissionPercentageFinished(int id, boolean isNew) {
        mSubjectHasBeenChanged = true;
        if (isNew)
            mAdapter.notifyIdAdded(id, true);
        else
            mAdapter.notifyOfChangeAtId(id, true);
        dialogClosed();
    }

    @Override
    public void deleteAdmissionPercentage(int itemId) {
        mSubjectHasBeenChanged = true;
        mAdapter.removePercentage(itemId);
        dialogClosed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PercentageTrackerCreationActivity.INTENT_REQUEST_CODE_ADMISSION_PERCENTAGE_CREATOR) {
            //this happens on back button press
            if (data == null)
                dialogClosed();
            else {
                int itemId = data.getExtras().getInt(PercentageTrackerCreationFragment.ADMISSION_PERCENTAGE_ID);
                switch (resultCode) {
                    case SubjectCreationActivity.DIALOG_RESULT_ADDED:
                    case SubjectCreationActivity.DIALOG_RESULT_CHANGED:
                        admissionPercentageFinished(itemId,
                                resultCode == SubjectCreationActivity.DIALOG_RESULT_ADDED);
                        break;
                    case SubjectCreationActivity.DIALOG_RESULT_DELETE:
                        deleteAdmissionPercentage(itemId);
                        break;
                    case SubjectCreationActivity.DIALOG_RESULT_CLOSED:
                        dialogClosed();
                        break;
                }
                //check for notification. when deleted, this string extra must be null.
                String recurrenceString =
                        data.getStringExtra(PercentageTrackerCreationFragment.RECURRENCE_STRING);
                if (recurrenceString != null)
                    mNotificationReminder.setReminder(itemId, recurrenceString);
                else
                    mNotificationReminder.removeReminder(itemId);
            }
        }
    }


    public void dialogClosed() {
        General.nukeKeyboard(getActivity());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.giant_cancel_button:
                abortAndClose();
                break;
            case R.id.giant_delete_button:
                deleteCurrentSubject();
                break;
            case R.id.giant_ok_button:
                saveAndClose();
                break;
        }
    }

    /**
     * Class used for remembering whether a certain percentage tracker needs to have a notification
     * added
     */
    private class NotificationReminder {
        private final List<Reminder> notificationList = new ArrayList<>();

        /**
         * remove a reminder
         */
        void removeReminder(int percentageTrackerId) {
            if (notificationList.isEmpty()) return;
            int position = getPosition(percentageTrackerId);
            if (position != -1)
                notificationList.remove(position);
        }

        /**
         * Get Reminder Object
         *
         * @param percentageTrackerId Id Of the percentage tracker the notification belongs to
         * @return Position of the object with id <parameter>percentageTrackerId</parameter> or -1
         * if not exists
         */
        private int getPosition(int percentageTrackerId) {
            int position = notificationList.size() - 1;
            for (; position >= 0; position--)
                if (notificationList.get(position).trackerId == percentageTrackerId)
                    break;
            return position;
        }

        void setReminder(int trackerId, String recurrenceString) {
            int possibleOldPosition = getPosition(trackerId);
            Reminder reminder = new Reminder(trackerId, recurrenceString);
            if (possibleOldPosition == -1)
                notificationList.add(reminder);
            else
                notificationList.set(possibleOldPosition, reminder);
        }

        class Reminder {
            final int trackerId;
            final String recurrenceString;

            public Reminder(int trackerId, String recurrenceString) {
                this.trackerId = trackerId;
                this.recurrenceString = recurrenceString;
            }
        }
    }
}