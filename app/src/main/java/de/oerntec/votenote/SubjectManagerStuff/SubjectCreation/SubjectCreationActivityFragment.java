package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Database.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.DBAdmissionCounters;
import de.oerntec.votenote.Database.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.DatabaseCreator;
import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.PojoDatabase;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

/**
 * A placeholder fragment containing a simple view.
 */
public class SubjectCreationActivityFragment extends Fragment implements SubjectCreationDialogInterface{
    /**
     * Lesson should be added, not changed
     */
    public static final int ADD_SUBJECT_CODE = -1;

    /**
     * ID used for adding percentage or counter
     */
    public static final int ID_ADD_ITEM = -2;

    /**
     * DB for the subjects
     */
    private final DBSubjects mSubjectDb = DBSubjects.getInstance();

    /**
     * Need direct database access to start/end transaction
     */
    private SQLiteDatabase mDatabase;

    /**
     * Contains the subject name
     */
    private EditText nameInput;

    /**
     * Counter list
     */
    private RecyclerView mAdmissionCounterList;

    /**
     * Admission percentage list
     */
    private RecyclerView mAdmissionPercentageList;

    /**
     * adapters for Admission counters, admission percentage counters
     */
    private SubjectCreatorAdapter mCounterAdapter = null, mPercentageAdapter = null;

    /**
     * Database id of current subject
     */
    private int mSubjectId;

    /**
     * Position this subject had when if it was already displayed in the recyclerview of subject-
     * managementactivity
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

    public SubjectCreationActivityFragment() {
    }

    public static SubjectCreationActivityFragment newInstance(int subjectId, int subjectPosition) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, subjectId);
        args.putInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME, subjectPosition);

        SubjectCreationActivityFragment fragment = new SubjectCreationActivityFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ActionBar mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();//does this even do anything?

        //get arguments from intent
        mSubjectId = getArguments().getInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, -1);
        mRecyclerViewPosition = getArguments().getInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME, -1);

        //create a boolean containing whether we create a new subject to ease understanding
        mIsNewSubject = mSubjectId == ADD_SUBJECT_CODE;
        mIsOldSubject = !mIsNewSubject;

        //get a database
        DatabaseCreator dbHelper = new DatabaseCreator(getContext());
        mDatabase = dbHelper.getWritableDatabase();

        //begin transaction on database to enable using commit and abort
        mDatabase.beginTransaction();

        //if this is a new subject, we have to create a subject now to know the subject id, which is conveniently returned by the addSubject method
        if(mIsNewSubject)
            mSubjectId = mSubjectDb.addSubject("If you see this, i fucked up.");

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_subject_creation, menu);

        if (mIsNewSubject) {
            menu.findItem(R.id.action_delete).setEnabled(false);
            menu.findItem(R.id.action_delete).setVisible(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //inflate view, extract views
        View input = inflater.inflate(R.layout.subject_manager_fragment_subject_settings, container, false);
        nameInput = (EditText) input.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);
        mAdmissionCounterList = (RecyclerView) input.findViewById(R.id.subject_manager_admission_counter_list);
        mAdmissionPercentageList = (RecyclerView) input.findViewById(R.id.subject_manager_admission_percentage_counter_list);
        return input;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameInput.addTextChangedListener(new NotEmptyWatcher(nameInput));

        if (mIsOldSubject) {
            Subject oldData = mSubjectDb.getItem(mSubjectId);
            nameInput.setText(oldData.name);
        }

        initializeList(mAdmissionCounterList, DBAdmissionCounters.getInstance(), mCounterAdapter, new AdmissionCounter(-1, -1, "", -1, -1));
        initializeList(mAdmissionPercentageList, DBAdmissionPercentageMeta.getInstance(), mPercentageAdapter, new AdmissionPercentageMeta(-1, -1, -1, -1, -1, ""));
    }

    private void initializeList(RecyclerView subject, PojoDatabase database, SubjectCreatorAdapter adapterReference, final NameAndIdPojo identificator) {
        //config the recyclerview
        subject.setHasFixedSize(true);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        subject.setLayoutManager(manager);

        adapterReference = new SubjectCreatorAdapter<>(database, mSubjectId);

        subject.setAdapter(adapterReference);
        subject.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), subject, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if ((int) view.getTag() != ID_ADD_ITEM) {
                    if (identificator instanceof AdmissionCounter)
                        Dialogs.showCounterDialog(getFragmentManager(), mSubjectId, (int) view.getTag(), false);
                    else
                        Dialogs.showPercentageDialog(getFragmentManager(), mSubjectId, (int) view.getTag(), false);
                } else {
                    if (identificator instanceof AdmissionCounter)
                        Dialogs.showCounterDialog(getFragmentManager(), mSubjectId, ID_ADD_ITEM, true);
                    else
                        Dialogs.showPercentageDialog(getFragmentManager(), mSubjectId, ID_ADD_ITEM, true);
                }
            }

            public void onItemLongClick(final View view, int position) {
                if ((int) view.getTag() != ID_ADD_ITEM) {
                    if (identificator instanceof AdmissionCounter)
                        Dialogs.showCounterDeleteDialog((AdmissionCounter) mCounterAdapter.getItemAtPosition(position), getActivity(), SubjectCreationActivityFragment.this);
                    else
                        Dialogs.showPercentageDeleteDialog((AdmissionPercentageMeta) mPercentageAdapter.getItemAtPosition(position), getActivity(), SubjectCreationActivityFragment.this);
                }
            }
        }));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                showBackConfirmation();
                return true;
            case R.id.action_delete:
                deleteCurrentSubject();
                return true;
            case R.id.action_save:
                saveData();
                return true;
            case R.id.action_abort:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentSubject() {
        //abort if this is called for a new subject, that cant work
        if (mIsNewSubject)
            throw new AssertionError("tried to delete a new subject");

        //set the result to be retrieved by subject manager
        getActivity().setResult(SubjectManagementActivity.SUBJECT_CREATOR_RESULT_DELETE, getCurrentResultIntent());

        //kill creator activity
        getActivity().finish();
    }

    /**
     * Show a confirmation dialog to ask the user whether he really wants to leave
     */
    private void showBackConfirmation() {
        //if the subject did not change, we dont need to show this dialog
        boolean hasEmptyName = "".equals(nameInput.toString());
        if (!mSubjectHasBeenChanged) {
            getActivity().finish();
            return;
        }
        //create dialog
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle("Speichern?");
        b.setMessage("Möchtest du deine Änderungen speichern?");

        //create buttons
        b.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //save data first
                saveData();

                //close the activity containing this fragment
                getActivity().finish();
            }
        });
        b.setNegativeButton("Verwerfen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDatabase.endTransaction();
                getActivity().finish();
            }
        });
        b.setNeutralButton("Abbrechen", null);
        //check for invalid name
        AlertDialog dialog = b.show();
        if (hasEmptyName) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            dialog.setMessage("Einen leeren Namen kannst du nicht speichern!");
        }
    }

    /**
     * Write the new data out to db
     */
    private void saveData() {
        mSubjectDb.changeItem(new Subject(nameInput.getText().toString(), mSubjectId));
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * save key data about the current fragment subject in an intent, so that onActivityResult can send that intent
     *
     * @return result intent
     */
    private Intent getCurrentResultIntent() {
        //save result data into intent
        Intent returnIntent = new Intent();
        returnIntent.putExtra(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, mSubjectId);
        returnIntent.putExtra(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_VIEW_POSITION_ARGUMENT_NAME, mRecyclerViewPosition);
        return returnIntent;
    }


    public void onBackPressed() {
        showBackConfirmation();
    }

    @Override
    public void admissionCounterFinished(int id, boolean isNew) {
        mSubjectHasBeenChanged = true;
        mCounterAdapter.notifyOfChangeAtId(id);
    }

    @Override
    public void deleteAdmissionCounter(AdmissionCounter delete) {
        mSubjectHasBeenChanged = true;
        mCounterAdapter.removeItem(delete.id);
    }

    @Override
    public void admissionPercentageFinished(int id, boolean isNew) {
        mSubjectHasBeenChanged = true;
        mPercentageAdapter.notifyOfChangeAtId(id);
    }

    @Override
    public void deleteAdmissionPercentage(AdmissionPercentageMeta delete) {
        mSubjectHasBeenChanged = true;
        mPercentageAdapter.removeItem(delete.id);
    }
}
