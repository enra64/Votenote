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
package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Diagram.DiagramActivity;
import de.oerntec.votenote.ImportExport.Writer;
import de.oerntec.votenote.LessonFragmentStuff.LessonFragment;
import de.oerntec.votenote.NavigationDrawer.NavigationDrawerFragment;
import de.oerntec.votenote.Preferences.PreferencesActivity;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

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
* 1.2.2
* applying material looks
* - cards & recyclerviews instead of the old listviews
* - redid the action bar icons
* - switched to toolbar
* - rewrote adapters for the lists
* - settings activty no longer uses deprecated API calls
* - added floating action buttons for adding lessons/subjects
* - undo bars instead of warning dialogs
* fixed drawer start preference
* added logging system. does not work consistently with no reason
* no more differences between API 15 and API 22
* diagram view now uses percentages for better comparison
* 1.2.3
* added draiochta-level fix to the problem suggested by daniel that
* a subject with one entry could not be displayed
* 1.2.4
* fixed tutorial system
* fixed possible bug when creating a new subject
* 1.2.5
* removed version check system prior to upload to google play
* fixed a bug when sorting by oldest lesson first
*/

@SuppressLint("InflateParams")
public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, View.OnClickListener {
    /**
     * Request code for getting notified when the first subject has been added
     */
    public static final int ADD_FIRST_SUBJECT_REQUEST = 0;

    /**
     * enable or disable log calls for release
     */
    public static final boolean ENABLE_LOG_CALLS = BuildConfig.DEBUG;

    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    public static NavigationDrawerFragment mNavigationDrawerFragment;
    //database connection
    private static DBSubjects mSubjectDb;
    private static DBLessons mLessonDb;
    private static int mCurrentSelectedId, mCurrentSelectedPosition;
    private static MainActivity me;
    private boolean mCurrentFragmentHasPrespoints;

    public static void toast(String text) {
        Toast.makeText(me, text, Toast.LENGTH_SHORT).show();
    }

    public static boolean getPreference(String key, boolean def) {
        return PreferenceManager.getDefaultSharedPreferences(me).getBoolean(key, def);
    }

    private static int getPreference(String key, int def) {
        return PreferenceManager.getDefaultSharedPreferences(me).getInt(key, def);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        //database access
        mSubjectDb = DBSubjects.setupInstance(this);
        mLessonDb = DBLessons.setupInstance(this);

        setContentView(R.layout.activity_main);

        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        //if we can not get our toolbar, its rip time anyways
        setSupportActionBar(toolbar);

        mNavigationDrawerFragment =
                (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);

        if (getPreference("enable_logging", false)) {
            //setup handler getting all exceptions to log because google play costs 25 euros
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable e) {
                    handleUncaughtException(thread, e);
                }
            });
        }


        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    //http://stackoverflow.com/a/19968400
    private void handleUncaughtException(Thread thread, Throwable e) {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        Writer.appendLog(stringWriter.toString());
        System.exit(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.reloadAdapter();

            int lastSelected = getPreference("last_selected_dbid", 0);

            //select the last selected item
            if (lastSelected < mSubjectDb.getCount())
                mNavigationDrawerFragment.selectItem(lastSelected);

            //open drawer on each resume because the user may want that
            if (getPreference("open_drawer_on_start", false))
                mNavigationDrawerFragment.openDrawer();
        }

        int lessonCount = mLessonDb.getCount();
        int subjectCount = mSubjectDb.getCount();

        if (!getPreference("eula_agreed", false)) {
            AlertDialog.Builder eulaBuilder = new AlertDialog.Builder(this);
            eulaBuilder.setCancelable(false);
            eulaBuilder.setTitle("Disclaimer of Warranties");
            eulaBuilder.setPositiveButton("OK", null);
            eulaBuilder.setView(R.layout.preferences_eula);
            eulaBuilder.setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setPreference("eula_agreed", true);
                }
            });
            eulaBuilder.show();
        }

        if (lessonCount <= 0 && subjectCount > 0) {
            AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            b.setTitle("Tutorial");
            b.setView(this.getLayoutInflater().inflate(R.layout.tutorial_lessons, null));
            b.setPositiveButton(getString(R.string.dialog_button_ok), null);
            b.create().show();
        } else if (subjectCount == 0) {
            AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            b.setTitle("Tutorial");
            b.setMessage(getString(R.string.create_new_subject_command));
            b.setPositiveButton(getString(R.string.dialog_button_ok), null);
            b.setNeutralButton("Shortcut", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent(MainActivity.this, SubjectManagementActivity.class));
                }
            });
            b.create().show();
        }
    }

    /**
     * position is 0 indexed; the phf gets position+1
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //keep track of what fragment is shown
        mCurrentSelectedId = mSubjectDb.translatePositionToID(position);
        mCurrentSelectedPosition = position;
        mCurrentFragmentHasPrespoints = mSubjectDb.getWantedPresPoints(mCurrentSelectedId) > 0;
        // update the menu_main content by replacing fragments
        if (ENABLE_LOG_CALLS)
            Log.i("votenote main", "selected fragment " + position);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.container,
                        LessonFragment.newInstance(position)).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //save where the user left
        if (mCurrentSelectedId != -1) {
            setPreference("last_selected_dbid", mCurrentSelectedPosition);
            if (ENABLE_LOG_CALLS)
                Log.i("last selected", "" + mCurrentSelectedPosition);
        }
    }

    /**
     * Gets the current section name from the database, and adds it to the view
     *     * @param section datbase index+1
     */
    public void onSectionAttached(int section) {
        restoreActionBar();
    }

    private void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) return;
        //noinspection deprecation
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);

        //basically calls onCreateOptionsMenu
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //update title
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        //prepare animations
        final MenuItem presPointItem = menu.findItem(R.id.action_prespoints);
        final MenuItem infoItem = menu.findItem(R.id.action_show_all_info);

        presPointItem.setActionView(R.layout.subject_fragment_action_presentation_points);
        infoItem.setActionView(R.layout.subject_fragment_action_info);

        presPointItem.getActionView().setOnClickListener(this);
        presPointItem.getActionView().setTag(R.id.action_prespoints);
        infoItem.getActionView().setOnClickListener(this);
        infoItem.getActionView().setTag(R.id.action_show_all_info);
        return true;
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mNavigationDrawerFragment.isDrawerOpen();

        final MenuItem presPointItem = menu.findItem(R.id.action_prespoints);
        final MenuItem infoItem = menu.findItem(R.id.action_show_all_info);

        View presPoints = presPointItem.getActionView();
        final View info = infoItem.getActionView();

        Animation fade;
        final boolean isVisibleAtEnd;

        if (!drawerOpen) {
            fade = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            isVisibleAtEnd = true;
        } else {
            fade = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            isVisibleAtEnd = false;
        }

        if (!mCurrentFragmentHasPrespoints)
            presPointItem.setVisible(false);

        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().post(new Runnable() {
                    public void run() {
                        infoItem.setVisible(isVisibleAtEnd);
                        if (mCurrentFragmentHasPrespoints)
                            presPointItem.setVisible(isVisibleAtEnd);
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        fade.setInterpolator(new AccelerateInterpolator());
        fade.setDuration(350);

        if (mCurrentFragmentHasPrespoints)
            presPoints.startAnimation(fade);
        info.startAnimation(fade);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_all_info:
                onInfoClick();
                return true;
            case R.id.action_prespoints:
                onPresentationPointsClick();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            case R.id.action_show_diagram:
                onDiagramClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onDiagramClick() {
        startActivity(new Intent(this, DiagramActivity.class));
    }

    private void onPresentationPointsClick() {
        MainDialogHelper.showPresentationPointDialog(mCurrentSelectedId, this);
    }

    private void onInfoClick() {
        MainDialogHelper.showAllInfoDialog(this, mCurrentSelectedId);
        //handy-dandy exception thrower for exception handling testing
        //Integer.valueOf("rip");
    }

    private void setPreference(String key, int val) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(key, val).apply();
    }

    private void setPreference(String key, boolean val) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(key, val).apply();
    }

    public LessonFragment getCurrentFragment() {
        return (LessonFragment) getFragmentManager().findFragmentById(R.id.container);
    }

    @Override
    public void onClick(View view) {
        int id = (int) view.getTag();
        if (id == R.id.action_show_all_info)
            onInfoClick();
        if (id == R.id.action_prespoints)
            onPresentationPointsClick();
    }
}