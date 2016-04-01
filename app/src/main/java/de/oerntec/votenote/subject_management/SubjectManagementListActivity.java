/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.subject_management;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Random;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.AdmissionCounter;
import de.oerntec.votenote.database.pojo.Lesson;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionCounters;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.database.tablehelpers.DBLessons;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;
import de.oerntec.votenote.helpers.General;
import de.oerntec.votenote.helpers.dialogs.Dialogs;
import de.oerntec.votenote.helpers.lists.LinearLayoutManagerWrapper;
import de.oerntec.votenote.helpers.lists.OnItemClickListener;
import de.oerntec.votenote.helpers.lists.RecyclerItemClickListener;
import de.oerntec.votenote.helpers.lists.SwipeAnimationUtils;
import de.oerntec.votenote.helpers.notifications.NotificationGeneralHelper;
import de.oerntec.votenote.import_export.BackupHelper;
import de.oerntec.votenote.subject_management.subject_creation.SubjectCreationActivity;

@SuppressLint("InflateParams")
public class SubjectManagementListActivity extends AppCompatActivity implements SwipeAnimationUtils.UndoSnackBarHost {
    /**
     * result code if the subject was deleted
     */
    public static final int SUBJECT_CREATOR_RESULT_DELETE = 3;
    /**
     * result code of the subject creator when it created a new subject
     */
    public static final int SUBJECT_CREATOR_RESULT_NEW = 1;
    /**
     * result code of the subject creator when it changed a subject
     */
    public static final int SUBJECT_CREATOR_RESULT_CHANGED = 2;
    /**
     * Lesson is to be added, not changed
     */
    private static final int ADD_SUBJECT_CODE = -1;
    /**
     * the subject adapter used to fill the list
     */
    private static SubjectAdapter mSubjectAdapter;

    private String mLastDeletionSavepointId = null;

    /**
     * recyclerview holding all subjects
     */
    private RecyclerView mSubjectList;

    private Snackbar mDeletionSnackbar;

    /**
     * Due to the fucked up nature of deleting in this app, we need to save the last long pressed id
     * in this variable, so we can remove the notifications should the removal not be undone by the
     * UndoSnackBar
     */
    private int mLastDeletedSubjectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subject_manager_activity_list);
        setSupportActionBar((Toolbar) findViewById(R.id.subject_manager_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        General.adjustLanguage(this);
        General.setupDatabaseInstances(getApplicationContext());

        //show tutorial if no subjects are present and the tutorial has not yet been read
        if (DBSubjects.getInstance().isEmpty() && !getPreference("tutorial_subjects_read", false)) {
            Builder b = new AlertDialog.Builder(this);
            b.setTitle("Tutorial");
            b.setView(this.getLayoutInflater().inflate(R.layout.tutorial_subjects, null));
            b.setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setPreference("tutorial_subjects_read", true);
                }
            });
            b.create().show();
        }

        //get listview
        mSubjectList = (RecyclerView) findViewById(R.id.subject_manager_recycler_view);

        //config the recyclerview
        if (mSubjectList == null) throw new AssertionError("subject list not found");
        mSubjectList.setHasFixedSize(true);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManagerWrapper(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mSubjectList.setLayoutManager(manager);


        mSubjectAdapter = new SubjectAdapter(this);

        final SubjectManagementListActivity veryGoodCoding = this;

        mSubjectList.setAdapter(mSubjectAdapter);
        mSubjectList.addOnItemTouchListener(new RecyclerItemClickListener(this, mSubjectList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if (mDeletionSnackbar != null)
                    mDeletionSnackbar.dismiss();
                showSubjectCreator((Integer) view.getTag(), position);
            }

            public void onItemLongClick(final View view, final int position) {
                Dialogs.showDeleteDialog(veryGoodCoding, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SwipeAnimationUtils.executeProgrammaticSwipeDeletion(veryGoodCoding, veryGoodCoding, view, position);
                        mLastDeletedSubjectId = (int) view.getTag();
                    }
                });
            }
        }));

        //add listener to fab
        FloatingActionButton addFab = (FloatingActionButton) findViewById(R.id.subject_manager_add_fab);
        if (addFab == null)
            throw new AssertionError("could not find floating action button");
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubjectCreatorForNewSubject();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        //accept the deletion to release the savepoint if the undobar is ignored
        acceptDeletionIfExists();
    }

    private void checkDisplayBackgroundTutorial() {
        FrameLayout frame = (FrameLayout) findViewById(R.id.subject_manager_list_tutorial_frame);
        if (frame == null)
            throw new AssertionError("could not find tutorial frame");
        if (DBSubjects.getInstance().isEmpty())
            getLayoutInflater().inflate(R.layout.tutorial_subjects, frame, true);
        else
            frame.removeAllViews();
    }

    private void showSubjectCreatorForNewSubject() {
        showSubjectCreator(SubjectManagementListActivity.ADD_SUBJECT_CODE, -1);
    }

    private void showSubjectCreator(int subjectId, int recyclerViewPosition) {
        Intent subjectManagerIntent = new Intent(SubjectManagementListActivity.this, SubjectCreationActivity.class);
        subjectManagerIntent.putExtra(SubjectCreationActivity.ARG_CREATOR_SUBJECT_ID, subjectId);
        //a -1 does not matter, its default anyways
        subjectManagerIntent.putExtra(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, recyclerViewPosition);
        startActivityForResult(subjectManagerIntent, 420);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDisplayBackgroundTutorial();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //abort and send further if not our request
        if (requestCode != 420) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode == SUBJECT_CREATOR_RESULT_CHANGED) {
            int recyclerViewPosition = data.getIntExtra(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, 0);
            //notify which item has changed
            mSubjectAdapter.notifyOfChangedSubject(recyclerViewPosition);
        } else if (resultCode == SUBJECT_CREATOR_RESULT_NEW) {
            //i have no idea where it was inserted, so fuck it, i'm recreating the adapter
            //reloadAdapter();
            mSubjectAdapter.notifySubjectAdded();
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.i("sma", "reloaded adapter to show new subject");
        } else if (resultCode == SUBJECT_CREATOR_RESULT_DELETE) {
            int recyclerViewPosition = data.getIntExtra(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, 0);
            SwipeAnimationUtils.executeProgrammaticSwipeDeletion(this, this, mSubjectList.getLayoutManager().getChildAt(recyclerViewPosition), recyclerViewPosition);
        }

    }


    /**
     * delete the subject, get a backup, show a undobar, and enable restoring the subject by tapping undo
     */
    public void showUndoSnackBar(final int subjectId, final int position) {
        acceptDeletionIfExists();
        mLastDeletionSavepointId = "SP_REM_SUB_ID_" + subjectId;
        DBSubjects.getInstance().createSavepoint(mLastDeletionSavepointId);
        mSubjectAdapter.removeSubject(subjectId, position);
        //make snackbar
        //noinspection ConstantConditions
        mDeletionSnackbar = Snackbar.make(findViewById(R.id.subject_manager_coordinator_layout), getString(R.string.undobar_deleted), Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        switch (event) {
                            case DISMISS_EVENT_CONSECUTIVE:
                            case DISMISS_EVENT_MANUAL:
                            case DISMISS_EVENT_TIMEOUT:
                                acceptDeletionIfExists();
                                break;
                            default:
                                super.onDismissed(snackbar, event);
                        }
                    }
                })
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onUndo();
                    }
                });
        mDeletionSnackbar.show();
        checkDisplayBackgroundTutorial();
    }

    private void acceptDeletionIfExists() {
        if (mLastDeletionSavepointId != null)
            DBSubjects.getInstance().releaseSavepoint(mLastDeletionSavepointId);
        NotificationGeneralHelper.removeAllAlarmsForSubject(this, mLastDeletedSubjectId);
        mLastDeletionSavepointId = null;
        mDeletionSnackbar = null;
    }

    private void onUndo() {
        if (mLastDeletionSavepointId != null) {
            DBSubjects.getInstance().rollbackToSavepoint(mLastDeletionSavepointId);
            checkDisplayBackgroundTutorial();
        }
        else
            throw new AssertionError("could not rollback subject deletion!");
        mLastDeletionSavepointId = null;
        mDeletionSnackbar = null;
        mSubjectAdapter.notifySubjectAdded();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_read_from_storage:
                BackupHelper.importDialog(this);
                return true;
            case R.id.action_create_example:
                createExample();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createExampleForReal() {
        setPreference("example_created", true);

        Toast.makeText(this, R.string.subject_management_example_created_toast, Toast.LENGTH_LONG).show();

        //these all need instances
        DBSubjects dbSubjects = DBSubjects.setupInstance(this);
        DBAdmissionPercentageMeta dbAdmissionPercentageMeta = DBAdmissionPercentageMeta.setupInstance(this);
        DBLessons dbLessons = DBLessons.setupInstance(this);
        DBAdmissionCounters dbAdmissionCounters = DBAdmissionCounters.setupInstance(this);

        int subjectId;

        subjectId = dbSubjects.addItemGetId("Modul");

        PercentageTrackerPojo percentage1 = new PercentageTrackerPojo(
                -1,
                subjectId,
                5,
                12,
                66,
                getString(R.string.example_subject_percentage_counter_1_name),
                "user",
                80,
                true,
                "1:14:0",
                false);

        PercentageTrackerPojo percentage2 = new PercentageTrackerPojo(
                -1,
                subjectId,
                3,
                12,
                50,
                getString(R.string.example_subject_percentage_counter_2_name),
                "user",
                80,
                false,
                "4:12:0",
                false);

        int percentage1Id = dbAdmissionPercentageMeta.addItemGetId(percentage1);
        int percentage2Id = dbAdmissionPercentageMeta.addItemGetId(percentage2);

        Random r = new Random();

        for (int i = 0; i < 20; i++) {
            int percentageId = i < 8 ? percentage1Id : percentage2Id;
            int available = r.nextInt(6);
            int finished = available > 0 ? r.nextInt(available) : 0;
            dbLessons.addItem(new Lesson(percentageId, 1, finished, available));
        }

        dbAdmissionCounters.addItem(new AdmissionCounter(-1,
                subjectId,
                getString(R.string.example_subject_presentation_points),
                2,
                3));

        dbAdmissionCounters.addItem(new AdmissionCounter(-1,
                subjectId,
                getString(R.string.example_subject_proof_assignments_counter_name),
                0,
                1));

        mSubjectAdapter.notifySubjectAdded();
    }

    private void createExample() {
        boolean exampleWasCreatedOnce = getPreference("example_created", false);

        if (!exampleWasCreatedOnce)
            createExampleForReal();

        if (exampleWasCreatedOnce) {
            AlertDialog.Builder b = new Builder(this);
            b.setTitle(R.string.subject_management_add_example_dialog_title);
            b.setMessage(R.string.subject_management_add_example_dialog_message);
            b.setPositiveButton(R.string.dialog_button_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    createExampleForReal();
                }
            });
            b.setNegativeButton(R.string.dialog_button_no, null);
            b.create().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_subject_management, menu);
        return true;
    }

    /**
     * fuck it recreate dat adapter
     */
    public void reloadAdapter() {
        mSubjectList.swapAdapter(new SubjectAdapter(this), false);
        checkDisplayBackgroundTutorial();
    }

    private boolean getPreference(String key, boolean def) {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(key, def);
    }

    private void setPreference(String key, boolean newValue) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(key, newValue).apply();
    }
}