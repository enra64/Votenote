package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.ImportExport.XmlImporter;

@SuppressLint("InflateParams")
public class GroupManagementActivity extends Activity {

    private static final int ADD_SUBJECT_CODE = -1;
    static DBSubjects groupsDB;
    static DBLessons entriesDB;
    static SimpleCursorAdapter groupAdapter;
    ListView mainList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_management);

		/*
		 * handle if first group: show dialog
		 * get savedinstance state,
		 */
        //safety
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean("firstGroup", false)) {
                Builder b = new AlertDialog.Builder(this);
                b.setTitle("Tutorial");
                b.setView(this.getLayoutInflater().inflate(R.layout.mainfragment_dialog_firstgroup, null));
                b.setPositiveButton(getString(R.string.dialog_button_ok), null);
                b.create().show();
            }
        }
        //get db
        groupsDB = DBSubjects.getInstance();
        entriesDB = DBLessons.getInstance();

		/*
		 * create listview
		 */
        // set up the drawer's list view with items and click listener
        Cursor allCursor = groupsDB.getAllGroupsInfos();
        //define wanted columns
        String[] columns = {DatabaseCreator.SUBJECTS_NAME,
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS,
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};

        //define id values of views to be set
        int[] to = {R.id.groupmanager_listitem_text_groupname,
                R.id.groupmanager_listitem_text_needed_percentage,
                R.id.groupmanager_listitem_text_prespoints,
                R.id.groupmanager_listitem_text_scheduled_uebungs,
                R.id.groupmanager_listitem_text_scheduled_assignments};

        // create the adapter using the cursor pointing to the desired data
        groupAdapter = new SimpleCursorAdapter(
                this,
                R.layout.groupmanager_listitem,
                allCursor,
                columns,
                to,
                0);

        //get listview
        mainList = (ListView) findViewById(R.id.listGroupManagement);
        mainList.setAdapter(groupAdapter);
        mainList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
                Log.d("gmanage", "handling listview click on " + view.toString());
                showLessonDialog(position);
            }
        });
        mainList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteDialog(position);
                return true;
            }
        });
    }

    private void showLessonDialog(final int changePosition) {
        String nameHint = "Subject name";
        int databaseID = ADD_SUBJECT_CODE;
        int presentationPointsHint = 0;
        int minimumVotePercentageHint = 50;
        int scheduledAssignmentsPerLesson = 5;
        int scheduledNumberOfLessons = 10;

        //if we only change the entry, get the previously set values.
        if (changePosition != ADD_SUBJECT_CODE) {
            databaseID = groupsDB.translatePositionToID(changePosition);
            nameHint = groupsDB.getGroup(databaseID).subjectName;
            presentationPointsHint = groupsDB.getWantedPresPoints(databaseID);
            minimumVotePercentageHint = groupsDB.getMinVote(databaseID);
            scheduledAssignmentsPerLesson = groupsDB.getScheduledAssignmentsPerLesson(databaseID);
            scheduledNumberOfLessons = groupsDB.getScheduledNumberOfLessons(databaseID);
        }

        final String finalOldName = nameHint;
        final int dbId = databaseID;

        //inflate view with seekbar and name
        final View input = this.getLayoutInflater().inflate(R.layout.groupmanager_dialog_groupsettings, null);
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
        if (changePosition == ADD_SUBJECT_CODE) {
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

                        if (changePosition == ADD_SUBJECT_CODE) {
                            if (groupsDB.addGroup(newName, minimumVoteValue, minimumPresentationPoints, scheduledLessonCount, scheduledAssignmentsPerLesson) == -1)
                                Toast.makeText(getApplicationContext(), getString(R.string.subject_manage_subject_exists_already), Toast.LENGTH_SHORT).show();
                            else
                                reloadList();
                            setResult(RESULT_OK);
                        } else {
                            //change minimum vote, pres value
                            if (groupsDB.setMinVote(dbId, minimumVoteValue) != 1)
                                Log.e("groupmanager:minv", "did not update exactly one row");
                            if (groupsDB.setMinPresPoints(dbId, minimumPresentationPoints) != 1)
                                Log.e("groupmanager:minpres", "did not update exactly one row");

                            groupsDB.setScheduledUebungCountAndAssignments(dbId, scheduledLessonCount, scheduledAssignmentsPerLesson);
                            //change group NAME if it changed
                            Log.d("groupmanager", "trying to update name of " + finalOldName + " to " + newName);

                            if (!"".equals(newName))
                                groupsDB.changeName(dbId, finalOldName, newName);
                        }
                        reloadList();
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

    protected void showDeleteDialog(final int deletePosition) {
        //get name of the group supposed to be deleted
        final int databaseID = groupsDB.translatePositionToID(deletePosition);
        final String groupName = groupsDB.getGroupName(databaseID);
        //request confirmation
        new Builder(this)
                .setTitle(groupName + " " + getString(R.string.group_delete_title))
                .setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        groupsDB.deleteRecord(groupName, databaseID);
                        entriesDB.deleteAllEntriesForGroup(databaseID);
                        groupAdapter.swapCursor(groupsDB.getAllGroupsInfos()).close();
                    }
                }).setNegativeButton(getString(R.string.dialog_button_abort), null).show();
    }

    public void reloadList() {
        groupAdapter.changeCursor(groupsDB.getAllGroupsInfos());
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
        getMenuInflater().inflate(R.menu.group_management, menu);
        return true;
    }
}
