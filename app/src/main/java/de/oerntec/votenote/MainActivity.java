package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
    private static int mCurrentSelectedPosition;
    private static MainActivity mInstance;
    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    //public static MainActivity getInstance(){return mInstance;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;

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
    protected void onDestroy() {
        mInstance = null;
        super.onDestroy();
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
                DialogHelper.showAddLessonDialog(this, groupID);
                break;
            case R.id.action_prespoints:
                DialogHelper.showPresentationPointDialog(groupID, this);
                break;
            case R.id.action_export:
                ExportHelper.createExportDialog(this);
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

    public void notifyCurrentFragment() {
        ((SubjectFragment) getFragmentManager().findFragmentById(R.id.container)).notifyOfChangedDataset();
    }
}