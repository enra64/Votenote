package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class MainActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Request code for getting notified when the first subject has been added
     */
    public static final int ADD_FIRST_SUBJECT_REQUEST = 0;
    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    protected static NavigationDrawerFragment mNavigationDrawerFragment;
    //database connection
    private static DBGroups groupsDB;
    private static DBEntries entryDB;
    private static int mCurrentSelectedId;
    private static MainActivity me;
    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    public static void toast(String text) {
        Toast.makeText(me, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;
        //database access
        groupsDB = DBGroups.setupInstance(this);
        entryDB = DBEntries.setupInstance(this);

        //to avoid calling groupsDB before having it started, setting the view has been
        //moved here
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment =
                (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
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
        mCurrentSelectedId = groupsDB.translatePositionToID(position);
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
        DBGroups.Subject sectionData = groupsDB.getGroup(groupsDB.translatePositionToID(section));
        mTitle = sectionData == null ? "Übung hinzufügen!" : sectionData.subjectName;
        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) return;
        //noinspection deprecation
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            if (groupsDB.getWantedPresPoints(mCurrentSelectedId) == 0)
                menu.findItem(R.id.action_prespoints).setVisible(false);
            else
                menu.findItem(R.id.action_prespoints).setVisible(true);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int groupID = mCurrentSelectedId;

        switch (item.getItemId()) {
            case R.id.action_groupmanagement:
                Intent intent = new Intent(this, GroupManagementActivity.class);
                startActivityForResult(intent, ADD_FIRST_SUBJECT_REQUEST);
                startActivity(intent);
                break;
            case R.id.action_add_entry:
                MainDialogHelper.showAddLessonDialog(this, groupID);
                break;
            case R.id.action_prespoints:
                MainDialogHelper.showPresentationPointDialog(groupID, this);
                break;
            case R.id.action_export:
                MainDialogHelper.showExportChooseDialog(this);
                break;
            case R.id.action_import:
                new XmlExporter().importDialog(this);
                break;
            case R.id.action_show_diagram:
                //only show diagram if more than 0 entries exist
                if (entryDB.getGroupRecordCount(groupID) > 0) {
                    Intent bintent = new Intent(this, DiagramActivity.class);
                    bintent.putExtra("databaseID", groupID);
                    startActivityForResult(bintent, ADD_FIRST_SUBJECT_REQUEST);
                } else
                    Toast.makeText(getApplicationContext(), "Diese Übung hat keine Daten!", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_FIRST_SUBJECT_REQUEST) {
            mNavigationDrawerFragment.selectItem(0);
            Log.i("on result", "trying to reload fragment");
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Get the currently shown fragment to reload all data
     */
    public void notifyCurrentFragment() {
        ((SubjectFragment) getFragmentManager().findFragmentById(R.id.container)).notifyOfChangedDataset();
    }
}