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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.CardListHelpers.SwipeDeletion;
import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.ImportExport.XmlImporter;
import de.oerntec.votenote.R;
import de.oerntec.votenote.TranslationHelper;

@SuppressLint("InflateParams")
public class SubjectManagementActivity extends AppCompatActivity implements SwipeDeletion.UndoSnackBarHost {
    /**
     * Lesson should be added, not changed
     */
    public static final int ADD_SUBJECT_CODE = -1;
    /**
     * default db access
     */
    private static DBSubjects mSubjectDb;
    /**
     * the subject adapter used to fill the list
     */
    private static SubjectAdapter mSubjectAdapter;

    Handler deletionHandler;
    Runnable deletionRunnable;
    /**
     * whenever a dialog is opened, we save its position in the recyclerview in here. while it is
     * quite ugly, it should work without problems since the only use is from the delete button in
     * the change dialog.
     */
    private int undoBarRecyclerViewPosition = -1;
    /**
     * saves the subject on delete to be able to add it again should the user demand it
     */
    private Subject subjectToBeDeleted;
    private int positionOfSubjectToBeDeleted;

    private RecyclerView mSubjectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subject_manager_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.subject_manager_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        //get db
        mSubjectDb = DBSubjects.getInstance();

        TranslationHelper.adjustLanguage(this);

        //tutorial?
        if (!getPreference("tutorial_subjects_read", false)) {
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
                        undoBarRecyclerViewPosition = mSubjectList.getChildAdapterPosition(viewHolder.itemView);
                        showUndoSnackBar(subjectId);
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
                showSubjectActivity((Integer) view.getTag());
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        //add listener to fab
        FloatingActionButton addFab = (FloatingActionButton) findViewById(R.id.subject_manager_add_fab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubjectActivity(ADD_SUBJECT_CODE);
            }
        });
    }

    private void showSubjectActivity(int subjectId) {
        Intent subjectManagerIntent = new Intent(SubjectManagementActivity.this, SubjectCreationActivity.class);
        subjectManagerIntent.putExtra(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, subjectId);
        startActivity(subjectManagerIntent);
    }

    public void showUndoSnackBar(final int subjectId) {
        //delete subject, get backup
        subjectToBeDeleted = mSubjectAdapter.removeSubject(subjectId, undoBarRecyclerViewPosition);
        //save the old position
        positionOfSubjectToBeDeleted = undoBarRecyclerViewPosition;
        //TODO: less fucked up way to save the positions in the recyclerview; currently done via 2 class variables :/
        //make snackbar
        Snackbar
                .make(findViewById(R.id.subject_manager_coordinator_layout), getString(R.string.undobar_deleted), Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onUndo();
                    }
                }).show();

        //delete lessons too after snackbar delay to avoid them popping up later
        deletionHandler = new Handler();
        deletionRunnable = new Runnable() {
            @Override
            public void run() {
                //check whether "undo" was tapped
                DBLessons.getInstance().deleteAllEntriesForGroup(subjectId);
            }
        };
        deletionHandler.postDelayed(deletionRunnable, 3000);
    }

    //dont do anything, as we only delete the lesson when the undo bar gets hidden
    private void onUndo() {
        if (subjectToBeDeleted != null)
            mSubjectAdapter.addSubject(subjectToBeDeleted, positionOfSubjectToBeDeleted);
        if (deletionHandler != null)
            deletionHandler.removeCallbacks(deletionRunnable);
        subjectToBeDeleted = null;
        positionOfSubjectToBeDeleted = -1;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_read_from_storage:
                XmlImporter.importDialog(this);
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
    public void onImport() {
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

    static class SeekerListener implements OnSeekBarChangeListener {
        TextView mAffected;
        String mEndingAddition;

        public SeekerListener(TextView affected, String endingAddition) {
            mAffected = affected;
            mEndingAddition = endingAddition;
        }

        public SeekerListener(TextView affected) {
            this(affected, "");
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            mAffected.setText(progress + mEndingAddition);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}