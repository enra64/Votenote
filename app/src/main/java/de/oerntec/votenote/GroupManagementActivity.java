package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class GroupManagementActivity extends Activity {

    static DBGroups groupsDB;
    static DBEntries entriesDB;
    static SimpleCursorAdapter groupAdapter;
    ListView mainList;

    /**
     * Creates the Dialog responsible for asking the user the name
     *
     * @param context    Context to use
     * @param firstGroup Whether we are creating the users first group
     */
    private static void createGroupDialog(final Context context, boolean firstGroup) {
        //inflat view with seekbar and name
        final View input = ((Activity) context).getLayoutInflater().inflate(R.layout.groupmanager_dialog_groupsettings, null);
        final EditText nameInput = (EditText) input.findViewById(R.id.groupmanager_groupsettings_edit_name);
        final TextView presInfo = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_prespoints);
        final SeekBar minPresSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_prespoints);
        final TextView infoView = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_min_votierungs);
        final SeekBar minVoteSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_min_votierungs);

        final TextView estimatedAssignmentsHelp = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_assignments_per_uebung);
        final SeekBar estimatedAssignmentsSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_assignments_per_uebung);

        final TextView estimatedUebungCountHelp = (TextView) input.findViewById(R.id.groupmanager_groupsettings_text_estimated_uebung_count);
        final SeekBar estimatedUebungCountSeek = (SeekBar) input.findViewById(R.id.groupmanager_groupsettings_seek_estimated_uebung_count);

        nameInput.setHint("Übungsname");

        //minpresseek
        presInfo.setText("2");

        minPresSeek.setMax(5);
        minPresSeek.setProgress(2);
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

        //minvoteseek
        minVoteSeek.setMax(100);
        minVoteSeek.setProgress(50);
        minVoteSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                infoView.setText(progress + "%");
            }
        });

        //assignments per uebung help
        int oldScheduledAssignments = 5;
        estimatedAssignmentsHelp.setText(oldScheduledAssignments + "");

        //assignments per uebung seek
        estimatedAssignmentsSeek.setMax(20);
        estimatedAssignmentsSeek.setProgress(oldScheduledAssignments);

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
        int oldScheduledUebungCount = 13;
        estimatedUebungCountHelp.setText(oldScheduledUebungCount + "");

        //ubeung instances seek
        estimatedUebungCountSeek.setMax(20);
        estimatedUebungCountSeek.setProgress(oldScheduledUebungCount);

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

        //build alertdialog
        Builder b = new AlertDialog.Builder(context)
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = nameInput.getText().toString();
                        int minVotValue = minVoteSeek.getProgress();
                        if (groupsDB.addGroup(name, minVotValue, minPresSeek.getProgress(), estimatedUebungCountSeek.getProgress(), estimatedAssignmentsSeek.getProgress()) == -1)
                            Toast.makeText(context, "Übung existiert schon", Toast.LENGTH_SHORT).show();
                        else
                            groupAdapter.swapCursor(groupsDB.getAllGroupsInfos()).close();
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        if (firstGroup) {
            b.setTitle("Erste Übung erstellen")
                    .setMessage("Bitte Übung anwählen");
        } else
            b.setTitle("Übung erstellen");
        b.create().show();
    }

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
                b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                b.create().show();
            }
        }
        //get db
        groupsDB = DBGroups.getInstance();
        entriesDB = DBEntries.getInstance();

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
                showNameActionsDialog(position);
            }
        });
    }

    private void showNameActionsDialog(final int position) {
        //get name of the group supposed to be deleted
        Cursor allCursor = groupsDB.getAllGroupNames();
        allCursor.moveToPosition(position);
        final String oldName = allCursor.getString(1);
        allCursor.close();

        new AlertDialog.Builder(this)
                .setTitle(oldName)
                .setPositiveButton("Ändern", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        showChangeDialog(position);
                    }
                }).setNegativeButton("Löschen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                showDeleteDialog(position);
            }
        }).show();
    }

    protected void showChangeDialog(final int pos) {
        final int databaseID = groupsDB.translatePositionToID(pos);
        //get old name
        Cursor groupNameCursor = groupsDB.getGroupAt(databaseID);
        final String oldName = groupNameCursor.getString(1);
        groupNameCursor.close();

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
        nameInput.setText(oldName);

        //minpreshelp
        int prevMinPresPoints = groupsDB.getMinPresPoints(databaseID);
        presInfo.setText("" + prevMinPresPoints);

        //minpres seek
        minPresSeek.setProgress(prevMinPresPoints);
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
        int oldMinVote = groupsDB.getMinVote(databaseID);
        voteInfo.setText(oldMinVote + "%");

        //minvoteseek
        minVoteSeek.setMax(100);
        minVoteSeek.setProgress(oldMinVote);
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
        int oldScheduledAssignments = groupsDB.getScheduledAssignmentsPerUebung(databaseID);
        estimatedAssignmentsHelp.setText(oldScheduledAssignments + "");

        //assignments per uebung seek
        estimatedAssignmentsSeek.setMax(20);
        estimatedAssignmentsSeek.setProgress(oldScheduledAssignments);

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
        int oldScheduledUebungCount = groupsDB.getScheduledNumberOfLessons(databaseID);
        estimatedUebungCountHelp.setText(oldScheduledUebungCount + "");

        //ubeung instances seek
        estimatedUebungCountSeek.setMax(20);
        estimatedUebungCountSeek.setProgress(oldScheduledUebungCount);

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

        //build alertdialog
        Builder b = new AlertDialog.Builder(this)
                .setView(input)
                .setTitle(oldName + " Ändern")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //change minimum vote, pres value
                        if (groupsDB.setMinVote(databaseID, minVoteSeek.getProgress()) != 1)
                            Log.e("groupmanager:minv", "did not update only or at least one row");
                        if (groupsDB.setMinPresPoints(databaseID, minPresSeek.getProgress()) != 1)
                            Log.e("groupmanager:minpres", "did not update only or at least one row");
                        groupsDB.setScheduledUebungCountAndAssignments(databaseID, estimatedUebungCountSeek.getProgress(), estimatedAssignmentsSeek.getProgress());
                        //change group NAME if it changed
                        String newName = nameInput.getText().toString();
                        Log.d("groupmanager", "trying to update name of " + oldName + " to " + newName);
                        if (!newName.equals(oldName) && newName != "") {
                            groupsDB.changeName(databaseID, oldName, newName);
                        }
                        groupAdapter.swapCursor(groupsDB.getAllGroupsInfos()).close();
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        b.create().show();
    }

    protected void showDeleteDialog(final int deletePosition) {
        //get name of the group supposed to be deleted
        final int databaseID = groupsDB.translatePositionToID(deletePosition);
        final String groupName = groupsDB.getGroupName(databaseID);
        //request confirmation
        new AlertDialog.Builder(this)
                .setTitle(groupName + " löschen?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        groupsDB.deleteRecord(groupName, databaseID);
                        entriesDB.deleteAllEntriesForGroup(databaseID);
                        groupAdapter.swapCursor(groupsDB.getAllGroupsInfos()).close();
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_group) {
            createGroupDialog(this, false);
            return true;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return (true);
            case R.id.action_add_group:
                createGroupDialog(this, false);
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

    /**
     * Dialog for setting the maximum vote amount
     * by setting the uebung count and the estimated count of votes per
     * uebung
     */
    private void setMaximumAchievableVotesDialog(final int groupID, int uebungNummer) {
        //load previously set values for uebung count and work per uebung
        Cursor oldValCursor = groupsDB.getGroupAt(groupID);
        int totalUebungCount = oldValCursor.getInt(2);
        int workPerUebung = oldValCursor.getInt(3);

        //inflate view, find the textview containing the explanation
        final View pickView = this.getLayoutInflater().inflate(R.layout.groupmanager_dialog_scheduleuebungs, null);
        final TextView infoView = (TextView) pickView.findViewById(R.id.infoTextView);

        //find and configure the number pickers
        final NumberPicker totalUebungCountPicker = (NumberPicker) pickView.findViewById(R.id.pickerUebungCount);
        totalUebungCountPicker.setMinValue(0);
        totalUebungCountPicker.setMaxValue(20);
        totalUebungCountPicker.setValue(workPerUebung);
        final NumberPicker workPerUebungPicker = (NumberPicker) pickView.findViewById(R.id.pickerWorkPerUebung);
        workPerUebungPicker.setMinValue(0);
        workPerUebungPicker.setMaxValue(15);
        workPerUebungPicker.setValue(totalUebungCount);

        //set the current values of the picers as explanation text
        infoView.setText(workPerUebungPicker.getValue() + " Aufgaben pro Übung,\n" +
                totalUebungCountPicker.getValue() + " Übungen insgesamt");

        //add change listener to update dialog expl. if pickers changed
        workPerUebungPicker.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker thisPicker, int arg1, int newWorkPerUebung) {
                int totalUebungCount = totalUebungCountPicker.getValue();
                infoView.setText(newWorkPerUebung + " Aufgaben pro Übung,\n" +
                        totalUebungCount + " Übungen insgesamt");
            }
        });
        totalUebungCountPicker.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker thisPicker, int arg1, int newTotalUebungCount) {
                int newWorkPerUebung = workPerUebungPicker.getValue();
                infoView.setText(newWorkPerUebung + " Aufgaben pro Übung,\n" +
                        newTotalUebungCount + " Übungen insgesamt");
            }
        });

        //build alertdialog
        new AlertDialog.Builder(this)
                .setTitle("Maximal erreichbare Zahl")
                .setView(pickView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //does what the name says...
                        groupsDB.setScheduledUebungCountAndAssignments(groupID, totalUebungCountPicker.getValue(), workPerUebungPicker.getValue());
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
    }
}
