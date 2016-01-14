package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment;

import android.app.Activity;
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
import de.oerntec.votenote.Database.DatabaseCreator;
import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.Pojo.Subject;
import de.oerntec.votenote.Database.PojoDatabase;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBSubjects;
import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.Dialogs;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.SubjectCreationActivity;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.SubjectCreationDialogInterface;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

/**
 * A placeholder fragment containing a simple view.
 */
public class SubjectCreationActivityFragment extends Fragment implements SubjectCreationDialogInterface {
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

    public SubjectCreationActivityFragment() {
    }

    public static SubjectCreationActivityFragment newInstance(int subjectId, int subjectPosition) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(SubjectCreationActivity.ARG_CREATOR_SUBJECT_ID, subjectId);
        args.putInt(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, subjectPosition);

        SubjectCreationActivityFragment fragment = new SubjectCreationActivityFragment();
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
        if(mIsNewSubject)
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

        if(mSubjectDb == null) throw new AssertionError("could not get a database instance?");

        //begin transaction on database to enable using commit and abort
        mDatabase.beginTransaction();
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
        mList = (RecyclerView) input.findViewById(R.id.subject_manager_admission_counter_list);

        initializeList(mList);

        return input;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mIsOldSubject) {
            Subject oldData = mSubjectDb.getItem(mSubjectId);
            mOldSubjectName = oldData.name;
        }
    }


    @SuppressWarnings("unchecked")
    private void initializeList(RecyclerView recyclerView) {
        //config the recyclerview
        recyclerView.setHasFixedSize(true);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);

        mAdapter = new UnifiedCreatorAdapter(getActivity(), getFragmentManager(), mIsNewSubject ? null : mOldSubjectName, mSubjectId);
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), recyclerView, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if (mAdapter.getItemViewType(position) == UnifiedCreatorAdapter.VIEW_COUNTER)
                    Dialogs.showCounterDialog(getFragmentManager(), mSubjectId, (int) view.getTag(), false);
                else
                    Dialogs.showPercentageDialog(getFragmentManager(), mSubjectId, (int) view.getTag(), false);
            }

            public void onItemLongClick(final View view, int position) {
                if ((int) view.getTag() != ID_ADD_ITEM) {
                    if (mAdapter.getItemViewType(position) == UnifiedCreatorAdapter.VIEW_COUNTER)
                        Dialogs.showCounterDeleteDialog(mAdapter.getCounterPojoAtPosition(position), getActivity(), SubjectCreationActivityFragment.this);
                    else
                        Dialogs.showPercentageDeleteDialog(mAdapter.getPercentagePojoAtPosition(position), getActivity(), SubjectCreationActivityFragment.this);
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
                abort();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void abort() {
        mDatabase.endTransaction();
        getActivity().finish();
    }

    private boolean hasChanged() {
        return !mAdapter.getCurrentName().equals(mOldSubjectName) || mSubjectHasBeenChanged;
    }

    private void deleteCurrentSubject() {
        //abort if this is called for a new subject, that cant work
        if (mIsNewSubject)
            throw new AssertionError("tried to delete a new subject");

        //set the result to be retrieved by subject manager
        setActivityResult(true);

        //kill creator activity
        getActivity().finish();
    }

    /**
     * Show a confirmation dialog to ask the user whether he really wants to leave
     */
    private void showBackConfirmation() {
        //if the subject did not change, we dont need to show this dialog
        boolean hasEmptyName = "".equals(mAdapter.getCurrentName());
        if (!hasChanged()) {
            if(mDatabase.inTransaction())
                mDatabase.endTransaction();
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

                //set the result, delete is false, change is determined by mIsNewSubject
                setActivityResult(false);

                //close the activity containing this fragment
                getActivity().finish();
            }
        });
        b.setNegativeButton("Verwerfen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                abort();
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

    private void setActivityResult(boolean delete) {
        if (delete)
            getActivity().setResult(SubjectManagementActivity.SUBJECT_CREATOR_RESULT_DELETE, getCurrentResultIntent());
        else if (mIsNewSubject)
            getActivity().setResult(SubjectManagementActivity.SUBJECT_CREATOR_RESULT_NEW, getCurrentResultIntent());
        else
            getActivity().setResult(SubjectManagementActivity.SUBJECT_CREATOR_RESULT_CHANGED, getCurrentResultIntent());
    }

    /**
     * Write the new data out to db
     */
    private void saveData() {
        String newName = mAdapter.getCurrentName();
        mSubjectDb.changeItem(new Subject(newName, mSubjectId));
        if (mDatabase == null)
            throw new AssertionError("no db reference?");
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        if(mSubjectHasBeenChanged)
            setActivityResult(false);

        //enable change listeners
        mOldSubjectName = newName;
        mSubjectHasBeenChanged = false;
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
        mAdapter.notifyOfChangeAtId(id, true);
        dialogClosed();
    }

    @Override
    public void deleteAdmissionPercentage(int itemId) {
        mSubjectHasBeenChanged = true;
        mAdapter.removePercentage(itemId);
        dialogClosed();
    }

    public void dialogClosed(){
        General.nukeKeyboard(getActivity());
    }
}
