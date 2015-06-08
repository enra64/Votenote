package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

@SuppressLint("InflateParams")
public class MainActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    private static NavigationDrawerFragment mNavigationDrawerFragment;
    //database connection
    private static DBGroups groupsDB;
    private static DBEntries entryDB;
    //text color backup
    private static int mCurrentSelectedPosition;
    TextView ppV;
    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //database access
        groupsDB = DBGroups.setupInstance(this);
        entryDB = DBEntries.setupInstance(this);

        //to avoid calling groupsDB before having it started, setting the view has been
        //moved here
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNavigationDrawerFragment != null)
            mNavigationDrawerFragment.reloadAdapter();
    }

    /**
     * position is 0 indexed; the phf gets position+1
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //keep track of what fragment is shown
        mCurrentSelectedPosition = position;
        // update the main content by replacing fragments
        Log.i("votenote main", "selected fragment " + position);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.container,
                        SubjectFragment.newInstance(position)).commit();
    }

    /**
     * Gets the current section name from the database, and adds it to the view
     *
     * @param section datbase index+1
     */
    public void onSectionAttached(int section) {
        Cursor allNames = groupsDB.getAllGroupNames();
        if (allNames.getCount() != 0) {
            allNames.moveToPosition(section);
            mTitle = allNames.getString(1);
        } else
            mTitle = "Übung hinzufügen!";
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int groupID = groupsDB.translatePositionToID(mCurrentSelectedPosition);

        switch (item.getItemId()) {
            case R.id.action_groupmanagement:
                Intent intent = new Intent(this, GroupManagementActivity.class);
                startActivity(intent);
                break;
            case R.id.action_add_entry:
                createEntryDialog(groupID);
                break;
            case R.id.action_prespoints:
                createPresPointsDialog(groupID);
                break;
            case R.id.action_export:
                createExportDialog();
                break;
            case R.id.action_show_diagram:
                //only show diagram if more than 0 entries exist
                if (entryDB.getGroupRecordCount(groupID) > 0) {
                    Intent bintent = new Intent(this, DiagramActivity.class);
                    bintent.putExtra("databaseID", groupID);
                    startActivity(bintent);
                } else
                    Toast.makeText(getApplicationContext(), "Diese Übung hat keine Daten!", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createExportDialog() {
        //inflate layout so we have something to work with
        final View inputView = this.getLayoutInflater().inflate(R.layout.mainfragment_dialog_export, null);

        //set infotext
        TextView info = (TextView) inputView.findViewById(R.id.mainfragment_dialog_export_infotext);
        info.setText("Die Exportdatei wird nach \"" + Environment.getExternalStorageDirectory() + "/Votenote\" gespeichert. (Dein normaler Speicher)");

        //list containing uebungs to export
        final boolean[] untranslatedExportList = new boolean[groupsDB.getUebungCount()];

        // set up the drawer's list view with items and click listener
        Cursor allNamesCursor = groupsDB.getAllGroupNames();
        //define wanted columnsgroupsDB
        String[] sourceColumns = {DatabaseCreator.GROUPS_NAMEN};

        //define id values of views to be set
        int[] targetViews = {R.id.diagramactivity_listview_listitem_text_name};

        // create the adapter using the cursor pointing to the desired data
        SimpleCursorAdapter exportUebungChooserAdapter = new SimpleCursorAdapter(
                this,
                R.layout.diagramactivity_listitem,
                allNamesCursor,
                sourceColumns,
                targetViews,
                0);

        //get listview
        ListView exportUebungChooser = (ListView) inputView.findViewById(R.id.mainfragment_dialog_export_checklist);
        exportUebungChooser.setAdapter(exportUebungChooserAdapter);
        exportUebungChooser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox thisBox = (CheckBox) view.findViewById(R.id.diagramactivity_listview_listitem_checkbox);
                thisBox.setChecked(!thisBox.isChecked());
                if (thisBox.isChecked()) {
                    //box has been checked
                    untranslatedExportList[position] = true;
                } else {
                    //box has been unchecked
                    untranslatedExportList[position] = false;
                }
            }
        });

        //dialog building
        Builder b = new AlertDialog.Builder(this)
                .setView(inputView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        StringBuilder s = new StringBuilder();
                        s.append("sep=;");
                        s.append("\r\n");
                        String fileName = "";
                        //for each checked uebung, export:
                        //name
                        //uebungsnummer, gemachte aufgaben, maximal machbare aufgaben
                        //num, gemauf(num), maxauf(num)
                        for (int untranslated = 0; untranslated < untranslatedExportList.length; untranslated++) {
                            if (untranslatedExportList[untranslated]) {
                                int translated = groupsDB.translatePositionToID(untranslated);
                                String groupName = groupsDB.getGroupName(translated);
                                fileName += groupName + "_";
                                s.append(groupName);
                                s.append(";;\r\n");
                                s.append("Nummer,Gemachte Aufgaben,Maximale Aufgaben");
                                s.append("\r\n");
                                Cursor entryCursor = entryDB.getGroupRecords(translated);

                                //read db to string
                                do {
                                    s.append(entryCursor.getInt(0));
                                    s.append(";");
                                    s.append(entryCursor.getInt(1));
                                    s.append(";");
                                    s.append(entryCursor.getInt(2));
                                    s.append("\r\n");
                                } while (entryCursor.moveToNext());

                                //add some free lines
                                s.append("\r\n\r\n");
                            }
                        }
                        //get current time for filename
                        Calendar c = Calendar.getInstance();
                        fileName = fileName + c.get(Calendar.DATE) + "_" + c.get(Calendar.MONTH) + "_" + c.get(Calendar.YEAR) + "_" +
                                c.get(Calendar.HOUR_OF_DAY) + "_" + c.get(Calendar.MINUTE) + "_" + c.get(Calendar.SECOND);
                        //write built string to txt file
                        try {
                            File root = new File(Environment.getExternalStorageDirectory() + File.separator + "Votenote" + File.separator);
                            root.mkdirs();
                            if (root.exists()) {
                                File writeoutFile = new File(root, fileName + ".csv");
                                FileWriter writer = new FileWriter(writeoutFile);
                                Log.i("exporter", writer.getEncoding());
                                writer.append(s.toString());
                                writer.flush();
                                writer.close();
                                Toast.makeText(getApplicationContext(), "Datei unter " + writeoutFile.getAbsolutePath() + " gespeichert.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Ordner konnte nicht erstellt werden!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            String exportError = e.getMessage();
                            Toast.makeText(getApplicationContext(), "Export konnte nicht geschrieben werden!", Toast.LENGTH_SHORT).show();
                            Log.w("exporter", "ioexception: " + exportError);
                        }
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        b.setTitle("Nach CSV exportieren").create().show();
    }

    private void createPresPointsDialog(final int dataBaseId) {
        final View inputView = this.getLayoutInflater().inflate(R.layout.mainfragment_dialog_prespoints, null);
        final NumberPicker presPointPicker = (NumberPicker) inputView.findViewById(R.id.mainfragment_dialog_prespoints_picker);

        presPointPicker.setMinValue(0);
        presPointPicker.setMaxValue(groupsDB.getMinPresPoints(dataBaseId));
        presPointPicker.setValue(groupsDB.getPresPoints(dataBaseId));

		/*
         * PRESPOINT ALERTDIALOG
		 */
        Builder b = new AlertDialog.Builder(this)
                .setView(inputView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //write new prespoint count to db
                        groupsDB.setPresPoints(dataBaseId, presPointPicker.getValue());
                        //reload fragment
                        onNavigationDrawerItemSelected(mCurrentSelectedPosition);
                        Log.i("votenote:addentry", "reloading fragment " + mCurrentSelectedPosition);
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        b.setTitle("Erreichte Präsentationspunkte");
        b.create().show();
    }

    /**
     * Dialog for creating an entry
     *
     * @param groupID DB id of the group
     */
    private void createEntryDialog(final int groupID) {
        //adding entry, use values put in last
        int maxVoteValue = groupsDB.getScheduledAssignmentsPerUebung(groupID);
        int minVoteValue = entryDB.getPrevVote(groupID);

        //inflate view, find the textview containing the explanation
        final View pickView = this.getLayoutInflater().inflate(R.layout.mainfragment_dialog_newentry, null);
        final TextView infoView = (TextView) pickView.findViewById(R.id.infoTextView);

        //find and configure the number pickers
        final NumberPicker maxVote = (NumberPicker) pickView.findViewById(R.id.pickerMaxVote);
        maxVote.setMinValue(0);
        maxVote.setMaxValue(15);
        maxVote.setValue(maxVoteValue);
        final NumberPicker myVote = (NumberPicker) pickView.findViewById(R.id.pickerMyVote);
        myVote.setMinValue(0);
        myVote.setMaxValue(15);
        myVote.setValue(minVoteValue);

        //set the current values of the pickers as explanation text
        infoView.setText(myVote.getValue() + " von " + maxVote.getValue() + " Votierungen");
        infoView.setTextColor(Color.argb(255, 153, 204, 0));//green

        //add change listener to update dialog expl. if pickers changed
        myVote.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker thisPicker, int arg1, int newVal) {
                int maxVoteValue = maxVote.getValue();
                infoView.setText(newVal + " von " + maxVoteValue + " Votierungen");
                if (maxVoteValue >= newVal)
                    infoView.setTextColor(Color.argb(255, 153, 204, 0));//green
                else
                    infoView.setTextColor(Color.argb(255, 204, 0, 0));//red
            }
        });
        maxVote.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker thisPicker, int arg1, int maxVoteValue) {
                int myVoteValue = myVote.getValue();
                infoView.setText(myVoteValue + " von " + maxVoteValue + " Votierungen");
                if (maxVoteValue >= myVoteValue)
                    infoView.setTextColor(Color.argb(255, 153, 204, 0));//green
                else
                    infoView.setTextColor(Color.argb(255, 204, 0, 0));//red
            }
        });

        //build alertdialog
        new AlertDialog.Builder(this)
                .setTitle("Neue Übungsnotiz")
                .setView(pickView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (myVote.getValue() <= maxVote.getValue()) {
                            //reload current fragment
                            onNavigationDrawerItemSelected(mCurrentSelectedPosition);
                            Log.i("Add uebung entry", "reload fragment " + mCurrentSelectedPosition);
                            entryDB.addEntry(groupID, maxVote.getValue(), myVote.getValue());
                        } else
                            Toast.makeText(getApplicationContext(), "Mehr votiert als möglich!", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
    }
}