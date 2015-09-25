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
import android.content.Context;
import android.content.DialogInterface;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

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
                showSubjectDialog((Integer) view.getTag(), position, view);
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        //add listener to fab
        FloatingActionButton addFab = (FloatingActionButton) findViewById(R.id.subject_manager_add_fab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubjectDialog(ADD_SUBJECT_CODE, -1, null);
            }
        });
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DBLessons.getInstance().deleteAllEntriesForGroup(subjectId);
            }
        }, Snackbar.LENGTH_LONG + 50);
    }

    //dont do anything, as we only delete the lesson when the undo bar gets hidden
    private void onUndo() {
        if (subjectToBeDeleted != null)
            mSubjectAdapter.addSubject(subjectToBeDeleted, positionOfSubjectToBeDeleted);
        subjectToBeDeleted = null;
        positionOfSubjectToBeDeleted = -1;
    }

    private void showSubjectDialog(final int databaseId, final int recyclerViewPosition, final View animatedView) {
        //save open rv position
        undoBarRecyclerViewPosition = recyclerViewPosition;

        //default values
        String nameHint = getString(R.string.subject_add_hint);
        int presentationPointsHint = 0;
        int minimumVotePercentageHint = 50;
        int scheduledAssignmentsPerLesson = 5;
        int scheduledNumberOfLessons = 10;

        //try to get old subject data; returns null if no subject is found
        final Subject knownSubjectData = mSubjectDb.getSubject(databaseId);

        //create a boolean containing whether we create a new subject to ease understanding
        final boolean isNewSubject = databaseId == ADD_SUBJECT_CODE;
        final boolean isOldSubject = !isNewSubject;

        //if we only change the entry, get the previously set values.
        if (isOldSubject) {
            //extract subject data
            nameHint = knownSubjectData.subjectName;
            presentationPointsHint = Integer.parseInt(knownSubjectData.subjectWantedPresentationPoints);
            minimumVotePercentageHint = Integer.parseInt(knownSubjectData.subjectMinimumVotePercentage);
            scheduledAssignmentsPerLesson = Integer.parseInt(knownSubjectData.subjectScheduledAssignmentsPerLesson);
            scheduledNumberOfLessons = Integer.parseInt(knownSubjectData.subjectScheduledLessonCount);
        }

        //inflate view with seekbar and name
        final View input = getLayoutInflater().inflate(R.layout.subject_manager_dialog_groupsettings, null);

        final EditText nameInput = (EditText) input.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);

        final TextView voteInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_min_votierungs);
        final SeekBar minVoteSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_min_votierungs);

        final TextView presInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_prespoints);
        final SeekBar wantedPresentationPointsSeekbar = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_prespoints);

        final TextView estimatedAssignmentsHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_assignments_per_uebung);
        final SeekBar estimatedAssignmentsSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_assignments_per_uebung);

        final TextView estimatedUebungCountHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_estimated_uebung_count);
        final SeekBar estimatedUebungCountSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_estimated_uebung_count);

        //offer hint to user
        nameInput.setHint(nameHint);
        //only set the old name as text if it is not "subject name", so the user can correct his value
        if (isOldSubject)
            nameInput.setText(nameHint);

        //minpreshelp
        presInfo.setText("" + presentationPointsHint);

        //minpres seek
        wantedPresentationPointsSeekbar.setProgress(presentationPointsHint);
        wantedPresentationPointsSeekbar.setMax(30);
        wantedPresentationPointsSeekbar.setOnSeekBarChangeListener(new SeekerListener(presInfo));

        //minvotehelp
        //initialize seekbar and seekbar info text
        voteInfo.setText(minimumVotePercentageHint + "%");

        //minvoteseek
        minVoteSeek.setMax(100);
        minVoteSeek.setProgress(minimumVotePercentageHint);
        minVoteSeek.setOnSeekBarChangeListener(new SeekerListener(voteInfo, "%"));

        //assignments per uebung help
        estimatedAssignmentsHelp.setText(scheduledAssignmentsPerLesson + "");

        //assignments per uebung seek
        estimatedAssignmentsSeek.setMax(50);
        estimatedAssignmentsSeek.setProgress(scheduledAssignmentsPerLesson);
        estimatedAssignmentsSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedAssignmentsHelp));

        //uebung instances help
        estimatedUebungCountHelp.setText(scheduledNumberOfLessons + "");

        //ubeung instances seek
        estimatedUebungCountSeek.setMax(50);
        estimatedUebungCountSeek.setProgress(scheduledNumberOfLessons);
        estimatedUebungCountSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedUebungCountHelp));

        String title;
        if (isNewSubject) {
            if (mSubjectDb.getNumberOfSubjects() == 0)
                title = getString(R.string.subject_manage_add_title_first_subject);
            else
                title = getString(R.string.subject_manage_add_title_new_subject);
        } else//if old subject
            title = getString(R.string.subject_manage_add_title_change_subject) + " " + nameHint;

        //build alertdialog
        Builder builder = new Builder(this)
                .setView(input)
                .setTitle(title)
                .setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //create a new subject containing all known values
                        Subject newSubject = new Subject(String.valueOf(databaseId),
                                String.valueOf(nameInput.getText().toString()),
                                String.valueOf(minVoteSeek.getProgress()),
                                "0",
                                String.valueOf(estimatedUebungCountSeek.getProgress()),
                                String.valueOf(estimatedAssignmentsSeek.getProgress()),
                                String.valueOf(wantedPresentationPointsSeekbar.getProgress()));

                        if (isNewSubject) {
                            if (mSubjectAdapter.addSubject(newSubject, SubjectAdapter.NEW_SUJBECT_CODE) == -1)
                                Toast.makeText(getApplicationContext(), getString(R.string.subject_manage_subject_exists_already), Toast.LENGTH_SHORT).show();
                        } else {
                            if ("".equals(newSubject.subjectName))
                                newSubject.subjectName = "empty";
                            mSubjectAdapter.changeSubject(knownSubjectData, newSubject, recyclerViewPosition);
                        }
                    }
                }).setNegativeButton(getString(R.string.dialog_button_abort), null);


        if (isOldSubject) {
            builder.setNeutralButton(R.string.delete_button_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SwipeDeletion.executeProgrammaticSwipeDeletion(SubjectManagementActivity.this, SubjectManagementActivity.this, animatedView);
                }
            });
        }

        final AlertDialog dialog = builder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //hide the bloody keyboard
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        //check for bad characters
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = String.valueOf(s);
                //deny saving a string breaking the xml backups
                if (value.contains("<") || value.contains(">") || value.contains("/'")) {
                    Toast.makeText(getApplication(), getString(R.string.subject_manage_bad_subject_name), Toast.LENGTH_SHORT).show();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                //deny saving an empty name
                else if (value.equals("")) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                //no bad names detected; allow saving
                else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        dialog.show();

        //change delete button text color to red
        if (isOldSubject)
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.warning_red));

        //disable ok button if new subject until a name is entered
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isOldSubject);
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