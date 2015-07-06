package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.oerntec.votenote.ImportExport.Writer;

/*
* VERSION HISTORY
* 1.0:
* ka
* 1.2:
* XML import and export (with success toast)
* CSV export (with success toast)
* Updated looks for lollipop users
* All-new settings activity
* Choose the lesson order
* Choose whether to start with an open drawer
* More digits on vote percentage
* Control of subjects now in settings activity
* Version checking system
* Better subject dialog
* New lessons dialog
* New dialog to show all calculated values
* New option to delete all entries
* final commit marker
* 1.2.1
* fixed recursive onCreateOptionsMenu call
* fixed text scaling
*/

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
    public static NavigationDrawerFragment mNavigationDrawerFragment;
    //database connection
    private static DBSubjects groupsDB;
    private static DBLessons entryDB;
    private static int mCurrentSelectedId, mCurrentSelectedPosition;
    private static MainActivity me;

    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    private CharSequence mTitle = "rip";

    public static void toast(String text) {
        Toast.makeText(me, text, Toast.LENGTH_SHORT).show();
    }

    public static boolean getPreference(String key, boolean def) {
        return PreferenceManager.getDefaultSharedPreferences(me).getBoolean(key, def);
    }

    public static int getPreference(String key, int def) {
        return PreferenceManager.getDefaultSharedPreferences(me).getInt(key, def);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;
        //database access
        groupsDB = DBSubjects.setupInstance(this);
        entryDB = DBLessons.setupInstance(this);

        //to avoid calling groupsDB before having it started, setting the view has been
        //moved here
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment =
                (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        //setup handler getting all exceptions to log because google play costs 25 euros
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(thread, e);
            }
        });

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        if (getPreference("check_version_at_start", true))
            if (VersionCheckHelper.isOnline(this))
                VersionCheckHelper.checkVersionStealth(this);
    }

    //http://stackoverflow.com/a/19968400
    public void handleUncaughtException(Thread thread, Throwable e) {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        Writer.appendLog(stringWriter.toString());
        System.exit(1);
    }

    public void onVersionResult(String result) {
        if (!"1.2.1".equals(result))
            Toast.makeText(this, getString(R.string.main_version_check_goto_settings), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.reloadAdapter();
            int lastSelected = getPreference("last_selected_dbid", 0);
            if (lastSelected < groupsDB.getCount())
                mNavigationDrawerFragment.selectItem(lastSelected);
            //open drawer on each resume because the user may want that
            if (getPreference("open_drawer_on_start", false))
                mNavigationDrawerFragment.openDrawer();
        }
        Cursor count = entryDB.getAllData();
        if (count.getCount() <= 0) {
            AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            b.setTitle("Tutorial");
            b.setView(this.getLayoutInflater().inflate(R.layout.tutorial_lessons, null));
            b.setPositiveButton(getString(R.string.dialog_button_ok), null);
            b.create().show();
        }
        count.close();
    }

    /**
     * position is 0 indexed; the phf gets position+1
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //keep track of what fragment is shown
        mCurrentSelectedId = groupsDB.translatePositionToID(position);
        mCurrentSelectedPosition = position;
        // update the menu_main content by replacing fragments
        Log.i("votenote main", "selected fragment " + position);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.container,
                        SubjectFragment.newInstance(position)).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //save where the user left
        if (mCurrentSelectedId != -1) {
            setPreference("last_selected_dbid", mCurrentSelectedPosition);
            Log.i("last selected", "" + mCurrentSelectedPosition);
        }
    }

    /**
     * Gets the current section name from the database, and adds it to the view
     *
     * @param section datbase index+1
     */
    public void onSectionAttached(int section) {
        Subject sectionData = groupsDB.getGroup(groupsDB.translatePositionToID(section));
        mTitle = sectionData == null ? getString(R.string.main_add_subject_command) : sectionData.subjectName;
        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) return;
        //noinspection deprecation
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);

        //update title
        setTitle(mTitle);

        //basically calls onCreateOptionsMenu
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.menu_main, menu);

            //update title
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(mTitle);
            }

            //update prespoint action icon
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
        final int lessonId = mCurrentSelectedId;

        switch (item.getItemId()) {
            case R.id.action_add_entry:
                MainDialogHelper.showAddLessonDialog(this, lessonId);
                return true;
            case R.id.action_show_all_info:
                MainDialogHelper.showAllInfoDialog(this, lessonId);
                return true;
            case R.id.action_prespoints:
                MainDialogHelper.showPresentationPointDialog(lessonId, this);
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_show_diagram:
                //only show diagram if more than 0 entries exist
                if (entryDB.getLessonCountForSubject(lessonId) > 0) {
                    Intent bintent = new Intent(this, DiagramActivity.class);
                    bintent.putExtra("databaseID", lessonId);
                    startActivityForResult(bintent, ADD_FIRST_SUBJECT_REQUEST);
                } else
                    Toast.makeText(getApplicationContext(), getString(R.string.main_toast_no_data), Toast.LENGTH_SHORT).show();
                return true;
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

    public void setPreference(String key, int val) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(key, val).apply();
    }

    /**
     * Get the currently shown fragment to reload all data
     */
    public void notifyCurrentFragment() {
        ((SubjectFragment) getFragmentManager().findFragmentById(R.id.container)).notifyOfChangedDataset();
    }
}