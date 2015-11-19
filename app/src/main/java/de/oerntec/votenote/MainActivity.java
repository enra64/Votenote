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
import android.app.Fragment;
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

import de.oerntec.votenote.Database.DBAdmissionCounters;
import de.oerntec.votenote.Database.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Lesson;
import de.oerntec.votenote.Diagram.DiagramActivity;
import de.oerntec.votenote.Dialogs.MainDialogHelper;
import de.oerntec.votenote.ImportExport.Writer;
import de.oerntec.votenote.AdmissionPercentageFragmentStuff.AdmissionPercentageFragment;
import de.oerntec.votenote.NavigationDrawer.NavigationDrawerFragment;
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
* 1.3
* a lot of translation improvements (partially courtesy of my brother)
* improved handling of error conditions in subject dialog
* added delete button in lesson and subject dialogs
* added possibility to automatically repair erroneous lesson conditions in dialog, made default
* improved tutorial system
* improved eula system
* added file dialog breadcrumbs
* added ability to choose eng/de
* added choice to view chart with percentages on x axis
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
    public static final boolean ENABLE_DEBUG_LOG_CALLS = BuildConfig.DEBUG;

    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    public static NavigationDrawerFragment mNavigationDrawerFragment;
    //database connection
    private static DBSubjects mSubjectDb;
    private static DBAdmissionPercentageMeta mPercentageMetaDb;
    private static DBAdmissionPercentageData mPercentageDataDb;
    private static DBAdmissionCounters mAdmissionCounterDb;

    private static int mCurrentSelectedSubjectId, mCurrentSelectedPosition;
    private static MainActivity me;
    private boolean mCurrentFragmentHasPrespoints;

    public static void toast(String text) {
        Toast.makeText(me, text, Toast.LENGTH_SHORT).show();
    }

    public static boolean getPreference(String key, boolean def) {
        return PreferenceManager.getDefaultSharedPreferences(me).getBoolean(key, def);
    }

    public static String getPreference(String key, String def) {
        return PreferenceManager.getDefaultSharedPreferences(me).getString(key, def);
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
        mAdmissionCounterDb = DBAdmissionCounters.setupInstance(this);
        mPercentageDataDb = DBAdmissionPercentageData.setupInstance(this);
        mPercentageMetaDb = DBAdmissionPercentageMeta.setupInstance(this);

        setContentView(R.layout.activity_main);

        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        //if we can not get our toolbar, its rip time anyways
        setSupportActionBar(toolbar);

        TranslationHelper.adjustLanguage(this);

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
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        if (ENABLE_DEBUG_LOG_CALLS)
            Log.i("state info", "oncreate");

        int lastSelected = getPreference("last_selected_dbid", 0);

        //select the last selected item
        if (lastSelected < mSubjectDb.getCount())
            mNavigationDrawerFragment.selectItem(lastSelected);
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

            //if(ENABLE_DEBUG_LOG_CALLS)
            //    Log.i("state info", "onresume");

            //open drawer on each resume because the user may want that
            if (getPreference("open_drawer_on_start", false)) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (ENABLE_DEBUG_LOG_CALLS)
                            Log.i("autodrawer", "opening");
                        mNavigationDrawerFragment.openDrawer();
                    }
                }, 900);
            }
        }

        int lessonCount = mSubjectDb.getNumberOfLessonsForSubject(mCurrentSelectedSubjectId);
        int subjectCount = mSubjectDb.getCount();

        //check whether we possibly have subjects, but none is loaded for some reason
        int lastSelected = getPreference("last_selected_dbid", -1);
        boolean hasValidLastSelected = lastSelected != -1 && lastSelected < subjectCount;

        Fragment checkFragment = getFragmentManager().findFragmentById(R.id.container);
        //check whether we have a fragment loaded
        if (!(checkFragment != null && checkFragment instanceof AdmissionPercentageFragment)) {
            //no! try to load a fragment
            if (hasValidLastSelected)
                mNavigationDrawerFragment.selectItem(lastSelected);
                //we dont have a valid last selected point.. maybe we can at least load something?
            else if (subjectCount > 0)
                mNavigationDrawerFragment.selectItem(0);
        }

        //tutorials
        if (lessonCount == 0 && subjectCount > 0) {
            if (!getPreference("tutorial_lessons_read", false)) {
                AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                b.setTitle("Tutorial");
                b.setView(this.getLayoutInflater().inflate(R.layout.tutorial_lessons, null));
                b.setPositiveButton(getString(R.string.dialog_button_ok), null);
                b.create().show();
                setPreference("tutorial_lessons_read", true);
            }
        } else if (subjectCount == 0) {
            if (!getPreference("tutorial_base_read", false)) {
                AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                b.setTitle("Tutorial");
                b.setMessage(getString(R.string.create_new_subject_command));
                b.setNegativeButton(R.string.tutorial_dismiss, null);
                b.setPositiveButton(R.string.tutorial_shortcut, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(MainActivity.this, SubjectManagementActivity.class));
                    }
                });
                AlertDialog dialog = b.create();
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        setPreference("tutorial_base_read", true);
                    }
                });
                dialog.show();
            }
        }

        //eula
        if (!getPreference("eula_agreed", false)) {
            AlertDialog.Builder eulaBuilder = new AlertDialog.Builder(this);
            eulaBuilder.setCancelable(false);
            eulaBuilder.setTitle("End-User License Agreement for Votenote");
            eulaBuilder.setNegativeButton(R.string.eula_do_not_accept, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            eulaBuilder.setView(this.getLayoutInflater().inflate(R.layout.preferences_eula, null));
            eulaBuilder.setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setPreference("eula_agreed", true);
                }
            });
            eulaBuilder.show();
        }
        //avoid showing the presentation points icon when no longer necessary
        invalidateOptionsMenu();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //keep track of what fragment is shown
        mCurrentSelectedSubjectId = mSubjectDb.getIdOfSubject(position);
        mCurrentSelectedPosition = position;

        mCurrentFragmentHasPrespoints = DBAdmissionCounters.getInstance().getItemsForSubject(mCurrentSelectedSubjectId).size() > 0;
        // update the menu_main content by replacing fragments
        if (ENABLE_DEBUG_LOG_CALLS)
            Log.i("votenote main", "selected fragment " + position);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.container, LessonFragment.newInstance(mCurrentSelectedSubjectId)).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //save where the user left
        if (mCurrentSelectedSubjectId != -1) {
            setPreference("last_selected_dbid", mCurrentSelectedPosition);
            if (ENABLE_DEBUG_LOG_CALLS)
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
        final MenuItem presPointItem = menu.findItem(R.id.action_prespoints);
        final MenuItem infoItem = menu.findItem(R.id.action_show_all_info);

        View presPoints = presPointItem.getActionView();
        final View info = infoItem.getActionView();

        Animation fade;
        final boolean isVisibleAtEnd;

        if (mNavigationDrawerFragment.isDrawerOpen()) {
            fade = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            isVisibleAtEnd = false;
        } else {
            fade = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            isVisibleAtEnd = true;
        }

        presPointItem.setVisible(mCurrentFragmentHasPrespoints);

        fade.setInterpolator(new AccelerateInterpolator());
        fade.setDuration(350);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                infoItem.setVisible(isVisibleAtEnd);
                if (mCurrentFragmentHasPrespoints)
                    presPointItem.setVisible(isVisibleAtEnd);
            }
        }, fade.getDuration() - 10);

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
        MainDialogHelper.showPresentationPointDialog(mCurrentSelectedSubjectId, this);
    }

    private void onInfoClick() {
        MainDialogHelper.showAllInfoDialog(this, mCurrentSelectedSubjectId);
        //handy-dandy exception thrower for exception handling testing
        //Integer.valueOf("rip");
    }

    private void setPreference(String key, int val) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(key, val).apply();
    }

    private void setPreference(String key, boolean val) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(key, val).apply();
    }

    public AdmissionPercentageFragment getCurrentFragment() {
        return (AdmissionPercentageFragment) getFragmentManager().findFragmentById(R.id.container);
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