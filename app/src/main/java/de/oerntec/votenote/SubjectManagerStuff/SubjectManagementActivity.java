package de.oerntec.votenote.SubjectManagerStuff;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.CardListHelpers.SwipeableRecyclerViewTouchListener;
import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.ImportExport.XmlImporter;
import de.oerntec.votenote.R;

@SuppressLint("InflateParams")
public class SubjectManagementActivity extends Activity implements UndoBarController.AdvancedUndoListener {
    /**
     * Lesson should be added, not changed
     */
    private static final int ADD_SUBJECT_CODE = -1;

    /**
     * default db access
     */
    private static DBSubjects groupsDB;

    /**
     * default db access
     */
    private static DBLessons entriesDB;

    /**
     * the subject adapter used to fill the list
     */
    private static SubjectAdapter mSubjectAdapter;

    /**
     * saves the subject on delete to be able to add it again should the user demand it
     */
    private Subject subjectToBeDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subject_manager_activity);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("firstGroup", false)) {
            Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            b.setTitle("Tutorial");
            b.setView(this.getLayoutInflater().inflate(R.layout.tutorial_subjects, null));
            b.setPositiveButton(getString(R.string.dialog_button_ok), null);
            b.create().show();
        }

        //get db
        groupsDB = DBSubjects.getInstance();
        entriesDB = DBLessons.getInstance();

        //get listview
        RecyclerView mSubjectList = (RecyclerView) findViewById(R.id.subject_manager_recycler_view);

        //config the recyclerview
        mSubjectList.setHasFixedSize(true);

        SwipeableRecyclerViewTouchListener mSwipeListener = new SwipeableRecyclerViewTouchListener(mSubjectList,
                new SwipeableRecyclerViewTouchListener.SwipeListener() {
                    @Override
                    public boolean canSwipe(int position) {
                        return true;
                    }

                    @Override
                    public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        for (int p : reverseSortedPositions) {
                            int subjectId = (Integer) recyclerView.getChildAt(p).getTag();
                            showUndoSnackBar(subjectId);
                        }
                    }

                    @Override
                    public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        onDismissedBySwipeLeft(recyclerView, reverseSortedPositions);
                    }
                });

        //give it a layoutmanage (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mSubjectList.setLayoutManager(manager);

        mSubjectAdapter = new SubjectAdapter(this, groupsDB.getAllGroupsInfos());

        mSubjectList.setAdapter(mSubjectAdapter);
        mSubjectList.addOnItemTouchListener(mSwipeListener);
        mSubjectList.addOnItemTouchListener(new RecyclerItemClickListener(this, mSubjectList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                showLessonDialog((Integer) view.getTag());
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));
    }

    private void showUndoSnackBar(final int lessonId) {
        subjectToBeDeleted = groupsDB.getSubject(lessonId);
        groupsDB.deleteRecord(subjectToBeDeleted.subjectName, lessonId);
        notifyOfChangedDataset();
        new UndoBarController.UndoBar(this)
                .message("Übung gelöscht")
                .style(UndoBarController.UNDOSTYLE)
                .duration(1800)
                .listener(this)
                .show();
    }

    //dont do anything, as we only delete the lesson when the undo bar gets hidden
    @Override
    public void onUndo(Parcelable parcelable) {
        if (subjectToBeDeleted != null) {
            groupsDB.addGroup(subjectToBeDeleted);
            notifyOfChangedDataset();
        }
        subjectToBeDeleted = null;
    }

    @Override
    public void onHide(Parcelable parcelable) {
        //remove all entries of the group
        if (subjectToBeDeleted != null)
            entriesDB.deleteAllEntriesForGroup(Integer.valueOf(subjectToBeDeleted.id));
        //re-enable subject removal
        subjectToBeDeleted = null;
    }

    @Override
    public void onClear() {

    }

    public void notifyOfChangedDataset() {
        mSubjectAdapter.getCursorAdapter().changeCursor(groupsDB.getAllGroupsInfos());
        mSubjectAdapter.getCursorAdapter().notifyDataSetChanged();
        mSubjectAdapter.notifyDataSetChanged();
    }

    private void showLessonDialog(final int databaseId) {
        String nameHint = getString(R.string.subject_add_hint);
        int presentationPointsHint = 0;
        int minimumVotePercentageHint = 50;
        int scheduledAssignmentsPerLesson = 5;
        int scheduledNumberOfLessons = 10;

        //if we only change the entry, get the previously set values.
        if (databaseId != ADD_SUBJECT_CODE) {
            nameHint = groupsDB.getSubject(databaseId).subjectName;
            presentationPointsHint = groupsDB.getWantedPresPoints(databaseId);
            minimumVotePercentageHint = groupsDB.getMinVote(databaseId);
            scheduledAssignmentsPerLesson = groupsDB.getScheduledAssignmentsPerLesson(databaseId);
            scheduledNumberOfLessons = groupsDB.getScheduledNumberOfLessons(databaseId);
        }

        final String finalOldName = nameHint;

        //inflate view with seekbar and name
        final View input = this.getLayoutInflater().inflate(R.layout.subject_manager_dialog_groupsettings, null);
        final EditText nameInput = (EditText) input.findViewById(R.id.groupmanager_groupsettings_edit_name);

        final TextView voteInfo = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_min_votierungs);
        final SeekBar minVoteSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_min_votierungs);

        final TextView presInfo = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_prespoints);
        final SeekBar minPresSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_prespoints);

        final TextView estimatedAssignmentsHelp = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_assignments_per_uebung);
        final SeekBar estimatedAssignmentsSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_assignments_per_uebung);

        final TextView estimatedUebungCountHelp = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_estimated_uebung_count);
        final SeekBar estimatedUebungCountSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_estimated_uebung_count);

        //offer hint to user
        nameInput.setHint(nameHint);

        //minpreshelp
        presInfo.setText("" + presentationPointsHint);

        //minpres seek
        minPresSeek.setProgress(presentationPointsHint);
        minPresSeek.setMax(5);
        minPresSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                presInfo.setText(String.valueOf(progress));
            }
        });

        //minvotehelp
        //initialize seekbar and seekbar info text
        voteInfo.setText(minimumVotePercentageHint + "%");

        //minvoteseek
        minVoteSeek.setMax(100);
        minVoteSeek.setProgress(minimumVotePercentageHint);
        minVoteSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                voteInfo.setText(progress + "%");
            }
        });

        //assignments per uebung help
        estimatedAssignmentsHelp.setText(scheduledAssignmentsPerLesson + "");

        //assignments per uebung seek
        estimatedAssignmentsSeek.setMax(20);
        estimatedAssignmentsSeek.setProgress(scheduledAssignmentsPerLesson);

        estimatedAssignmentsSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                estimatedAssignmentsHelp.setText(progress + "");
            }
        });

        //uebung instances help
        estimatedUebungCountHelp.setText(scheduledNumberOfLessons + "");

        //ubeung instances seek
        estimatedUebungCountSeek.setMax(20);
        estimatedUebungCountSeek.setProgress(scheduledNumberOfLessons);

        estimatedUebungCountSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                estimatedUebungCountHelp.setText(progress + "");
            }
        });

        String title;
        if (databaseId == ADD_SUBJECT_CODE) {
            if (groupsDB.getNumberOfSubjects() == 0)
                title = getString(R.string.subject_manage_add_title_first_subject);
            else
                title = getString(R.string.subject_manage_add_title_new_subject);
        } else
            title = getString(R.string.subject_manage_add_title_change_subject) + " " + nameHint;

        //build alertdialog
        Builder b = new Builder(this)
                .setView(input)
                .setTitle(title)
                .setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newName = nameInput.getText().toString();
                        int minimumVoteValue = minVoteSeek.getProgress();
                        int minimumPresentationPoints = minPresSeek.getProgress();
                        int scheduledLessonCount = estimatedUebungCountSeek.getProgress();
                        int scheduledAssignmentsPerLesson = estimatedAssignmentsSeek.getProgress();

                        if (databaseId == ADD_SUBJECT_CODE) {
                            if (groupsDB.addGroup(newName, minimumVoteValue, minimumPresentationPoints, scheduledLessonCount, scheduledAssignmentsPerLesson) == -1)
                                Toast.makeText(getApplicationContext(), getString(R.string.subject_manage_subject_exists_already), Toast.LENGTH_SHORT).show();
                            else
                                notifyOfChangedDataset();
                            setResult(RESULT_OK);
                        } else {
                            //change minimum vote, pres value
                            if (groupsDB.setMinVote(databaseId, minimumVoteValue) != 1)
                                Log.e("groupmanager:minv", "did not update exactly one row");
                            if (groupsDB.setMinPresPoints(databaseId, minimumPresentationPoints) != 1)
                                Log.e("groupmanager:minpres", "did not update exactly one row");

                            groupsDB.setScheduledUebungCountAndAssignments(databaseId, scheduledLessonCount, scheduledAssignmentsPerLesson);
                            //change group NAME if it changed
                            Log.d("groupmanager", "trying to update name of " + finalOldName + " to " + newName);

                            if (!"".equals(newName))
                                groupsDB.changeName(databaseId, finalOldName, newName);
                        }
                        notifyOfChangedDataset();
                    }
                }).setNegativeButton(getString(R.string.dialog_button_abort), null);
        final AlertDialog dialog = b.create();

        //check for bad characters
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = String.valueOf(s);
                if (value.contains("<") || value.contains(">") || value.contains("/'")) {
                    Toast.makeText(getApplication(), getString(R.string.subject_manage_bad_subject_name), Toast.LENGTH_SHORT).show();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            case R.id.action_read_from_storage:
                XmlImporter.importDialog(this);
                return true;
            case R.id.action_add_group:
                showLessonDialog(ADD_SUBJECT_CODE);
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
}