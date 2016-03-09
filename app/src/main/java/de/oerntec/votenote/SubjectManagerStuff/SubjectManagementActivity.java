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
package de.oerntec.votenote.SubjectManagerStuff;

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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.CardListHelpers.SwipeDeletion;
import de.oerntec.votenote.Database.TableHelpers.DBSubjects;
import de.oerntec.votenote.Helpers.General;
import de.oerntec.votenote.ImportExport.BackupHelper;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.SubjectOverview.SubjectCreationActivity;

@SuppressLint("InflateParams")
public class SubjectManagementActivity extends AppCompatActivity implements SwipeDeletion.UndoSnackBarHost {
    /**
     * Lesson is to be added, not changed
     */
    public static final int ADD_SUBJECT_CODE = -1;

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
     * the subject adapter used to fill the list
     */
    private static SubjectAdapter mSubjectAdapter;

    private String mLastDeletionSavepointId = null;

    /**
     * recyclerview holding all subjects
     */
    private RecyclerView mSubjectList;

    private Snackbar mDeletionSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subject_manager_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.subject_manager_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        General.adjustLanguage(this);
        General.setupDatabaseInstances(getApplicationContext());

        //show tutorial if no subjects are present and the tutorial has not yet been read
        if (DBSubjects.getInstance().isEmpty() && !getPreference("tutorial_subjects_read", false)) {
            Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
        mSubjectList.setHasFixedSize(true);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //info view has no tag
                Object viewTag = viewHolder.itemView.getTag();
                if (viewTag != null) {
                    int subjectId = (Integer) viewTag;
                    if (subjectId != 0) {
                        //i hope i never have to touch this again
                        showUndoSnackBar(subjectId, mSubjectList.getChildAdapterPosition(viewHolder.itemView));
                    }
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(mSubjectList);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mSubjectList.setLayoutManager(manager);


        mSubjectAdapter = new SubjectAdapter(this);

        mSubjectList.setAdapter(mSubjectAdapter);
        mSubjectList.addOnItemTouchListener(new RecyclerItemClickListener(this, mSubjectList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if(mDeletionSnackbar != null)
                    mDeletionSnackbar.dismiss();
                showSubjectCreator((Integer) view.getTag(), position);
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        //add listener to fab
        FloatingActionButton addFab = (FloatingActionButton) findViewById(R.id.subject_manager_add_fab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubjectCreator(ADD_SUBJECT_CODE);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        //accept the deletion to release the savepoint if the undobar is ignored
        acceptDeletion();
    }

    private void showSubjectCreator(int subjectId) {
        showSubjectCreator(subjectId, -1);
    }

    private void showSubjectCreator(int subjectId, int recyclerViewPosition) {
        Intent subjectManagerIntent = new Intent(SubjectManagementActivity.this, SubjectCreationActivity.class);
        subjectManagerIntent.putExtra(SubjectCreationActivity.ARG_CREATOR_SUBJECT_ID, subjectId);
        //a -1 does not matter, its default anyways
        subjectManagerIntent.putExtra(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, recyclerViewPosition);
        startActivityForResult(subjectManagerIntent, 420);
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
            if(MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.i("sma", "reloaded adapter to show new subject");
        } else if (resultCode == SUBJECT_CREATOR_RESULT_DELETE) {
            int recyclerViewPosition = data.getIntExtra(SubjectCreationActivity.ARG_CREATOR_VIEW_POSITION, 0);
            SwipeDeletion.executeProgrammaticSwipeDeletion(this, this, mSubjectList.getLayoutManager().getChildAt(recyclerViewPosition), recyclerViewPosition);
        }
    }

    /**
     * delete the subject, get a backup, show a undobar, and enable restoring the subject by tapping undo
     */
    public void showUndoSnackBar(final int subjectId, final int position) {
        mLastDeletionSavepointId = "SP_REM_SUB_ID_" + subjectId;
        DBSubjects.getInstance().createSavepoint(mLastDeletionSavepointId);
        mSubjectAdapter.removeSubject(subjectId, position);
        //make snackbar
        mDeletionSnackbar = Snackbar.make(findViewById(R.id.subject_manager_coordinator_layout), getString(R.string.undobar_deleted), Snackbar.LENGTH_LONG)
            .setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    switch(event){
                        case DISMISS_EVENT_CONSECUTIVE:
                        case DISMISS_EVENT_MANUAL:
                        case DISMISS_EVENT_TIMEOUT:
                            acceptDeletion();
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
    }

    private void acceptDeletion(){
        if(mLastDeletionSavepointId != null)
            DBSubjects.getInstance().releaseSavepoint(mLastDeletionSavepointId);
        mLastDeletionSavepointId = null;
        mDeletionSnackbar = null;
    }

    private void onUndo() {
        if(mLastDeletionSavepointId != null)
            DBSubjects.getInstance().rollbackToSavepoint(mLastDeletionSavepointId);
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
        }
        return super.onOptionsItemSelected(item);
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
    }

    public boolean getPreference(String key, boolean def) {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(key, def);
    }

    public String getPreference(String key, String def) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, def);
    }

    public void setPreference(String key, boolean newValue) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(key, newValue).apply();
    }
}