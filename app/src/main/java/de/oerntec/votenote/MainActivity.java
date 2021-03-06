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
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.oerntec.votenote.chart.ChartActivity;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionCounters;
import de.oerntec.votenote.database.tablehelpers.DBLastViewed;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;
import de.oerntec.votenote.helpers.General;
import de.oerntec.votenote.helpers.dialogs.Dialogs;
import de.oerntec.votenote.import_export.Writer;
import de.oerntec.votenote.navigationdrawer.NavigationDrawerFragment;
import de.oerntec.votenote.percentage_tracker_fragment.PercentageTrackerFragment;
import de.oerntec.votenote.preferences.Preferences;
import de.oerntec.votenote.subject_management.SubjectManagementListActivity;

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
     * Enable or disable transaction log calls for debugging the good old
     * BEGIN EXCLUSIVE;] cannot start a transaction within a transaction
     */
    @SuppressWarnings("PointlessBooleanExpression")
    public static final boolean ENABLE_TRANSACTION_LOG = ENABLE_DEBUG_LOG_CALLS && false;

    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    private static NavigationDrawerFragment mNavigationDrawerFragment;

    private static int mCurrentSelectedSubjectId;
    private DBLastViewed mLastViewedDb;
    private DBSubjects mSubjectDb;

    /**
     * set on subject fragment load
     */
    private boolean mCurrentSubjectHasAdmissionCounters;

    /**
     * set on subject fragment load
     */
    private boolean mCurrentSubjectHasPercentageCounters;

    private boolean mLastKnownPercentageCounterInfoViewVisibility = true;

    private boolean mLastKnownAdmissionCounterViewVisibility = true;

    /**
     * This is the layout that contains notice & button for when no subjects exist
     */
    private LinearLayout mNoSubjectWarningLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        General.setupDatabaseInstances(getApplicationContext());

        mLastViewedDb = DBLastViewed.getInstance();
        mSubjectDb = DBSubjects.getInstance();

        setContentView(R.layout.activity_main);

        //set toolbar as support actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        //if we can not get our toolbar, its rip time anyways
        setSupportActionBar(toolbar);

        Writer.mToastContext = getApplicationContext();
        General.adjustLanguage(this);

        mNavigationDrawerFragment =
                (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);

        if (Preferences.getPreference(this, "enable_logging", false)) {
            //setup handler getting all exceptions to log file
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable e) {
                    handleUncaughtException(e);
                }
            });
        }

        mNoSubjectWarningLayout = (LinearLayout) findViewById(R.id.main_actvity_no_subject_warning_layout);

        //This button accompanies a notice that no subjects exist, and takes the user to subject manag.
        TextView mNoSubjectWarningButton = (Button) findViewById(R.id.main_actvity_no_subject_warning_button);

        if (mNoSubjectWarningButton == null)
            throw new AssertionError("why does that button not exist?");

        mNoSubjectWarningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SubjectManagementListActivity.class));
            }
        });

        // Set up the drawer.
        mNavigationDrawerFragment.setUp((DrawerLayout) findViewById(R.id.drawer_layout));

        if (ENABLE_DEBUG_LOG_CALLS)
            Log.i("state info", "oncreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        int admissionPercentageId = intent.getIntExtra("admission_percentage_id_notification_action", -2);
        int subjectId = intent.getIntExtra("subject_id_notification_action", -2);
        int notificationId = intent.getIntExtra("notification_id", -2);

        boolean isNotificationIntent = admissionPercentageId != -2 && subjectId != -2 && notificationId != -2;

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("vn autoselect", "mainactivity started via notification action");

        if (isNotificationIntent) {
            mNavigationDrawerFragment.forceAdmissionPercentageFragmentDialogById(subjectId, admissionPercentageId);

            //the action was clicked, but some genius decided not to make an autodismiss api -> we
            //saved the notification id in the intent to kill it now
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }

        // remove the intent data, so that when the activity gets started again, the auto select
        // does not recognize it
        intent.removeExtra("notification_id");
        intent.removeExtra("subject_id_notification_action");
        intent.removeExtra("admission_percentage_id_notification_action");
    }

    /**
     * Either select the last selected subject, or force select the subject given in the intent
     */
    private void considerAutoSelect() {
        int admissionPercentageId = getIntent().getIntExtra("admission_percentage_id_notification_action", -2);
        int subjectId = getIntent().getIntExtra("subject_id_notification_action", -2);

        boolean isIntentGiven = admissionPercentageId != -2 && subjectId != -2;

        //handle the given intent
        if (isIntentGiven)
            handleIntent(getIntent());
            //select the last selected item
        else {
            int subjectCount = mSubjectDb.getCount();
            int lastSelected = mLastViewedDb.getLastSubjectPosition();

            //had some weird problems here
            boolean hasValidLastSelected = true;

            if (lastSelected < 0)
                hasValidLastSelected = false;

            if (lastSelected >= subjectCount)
                hasValidLastSelected = false;

            //try to load last selected fragment
            if (hasValidLastSelected)
                mNavigationDrawerFragment.selectItem(lastSelected);
            else if (subjectCount > 0)
                mNavigationDrawerFragment.selectItem(0);
            else
                onNoSubjectExists();

            if (hasValidLastSelected || subjectCount > 0)
                showBackgroundTutorial(false);
        }
    }

    //http://stackoverflow.com/a/19968400
    private void handleUncaughtException(Throwable e) {
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

            //open drawer on each resume because the user may want that
            if (Preferences.getPreference(this, "open_drawer_on_start", false)) {
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

        considerAutoSelect();

        //tutorials
        if (lessonCount == 0 && subjectCount > 0) {
            if (!Preferences.getPreference(this, "tutorial_lessons_read", false)) {
                @SuppressWarnings("deprecation")
                AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                b.setTitle("Tutorial");
                b.setView(this.getLayoutInflater().inflate(R.layout.tutorial_lessons, null));
                b.setPositiveButton(getString(R.string.dialog_button_ok), null);
                b.create().show();
                setPreference("tutorial_lessons_read", true);
            }
        } else if (subjectCount == 0) {
            if (!Preferences.getPreference(this, "tutorial_base_read", false)) {
                @SuppressWarnings("deprecation")
                AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                b.setTitle(R.string.tutorial_shortcut_title);
                b.setMessage(getString(R.string.create_new_subject_command));
                b.setNegativeButton(R.string.tutorial_dismiss, null);
                b.setPositiveButton(R.string.tutorial_shortcut, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(MainActivity.this, SubjectManagementListActivity.class));
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
        if (!Preferences.getPreference(this, "eula_agreed", false)) {
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
        onNavigationDrawerItemSelected(position, -1, false);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position, int admissionPercentageId, boolean forceLessonAddDialog) {
        //keep track of what fragment is shown
        mCurrentSelectedSubjectId = mSubjectDb.getIdOfSubject(position);

        mCurrentSubjectHasAdmissionCounters = DBAdmissionCounters.getInstance().getItemsForSubject(mCurrentSelectedSubjectId).size() > 0;
        mCurrentSubjectHasPercentageCounters = DBPercentageTracker.getInstance().getItemsForSubject(mCurrentSelectedSubjectId).size() > 0;

        SubjectFragment newFragment;

        //force show a certain admission percentage counter
        if (admissionPercentageId == -1)
            newFragment = SubjectFragment.newInstance(mCurrentSelectedSubjectId, position);
        else
            newFragment = SubjectFragment.newInstance(mCurrentSelectedSubjectId, position, admissionPercentageId, forceLessonAddDialog);


        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.container, newFragment, "subjectFragment").commit();
    }

    /**
     * This explicitly tries to remove the last set subject fragment, as there are no subjects
     */
    private void onNoSubjectExists() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment old = fragmentManager.findFragmentByTag("subjectFragment");
        if (old != null)
            fragmentManager.beginTransaction().remove(old).commit();
        else if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("main", "no old fragment was found");

        showBackgroundTutorial(true);
    }

    private void showBackgroundTutorial(boolean show) {
        mNoSubjectWarningLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Restores the action bar to the desired state (mostly title...) after selecting a new subject
     */
    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayShowTitleEnabled(true);

        String title = "VoteNote";

        if(mCurrentSelectedSubjectId >= 0){
            try{
                title = mSubjectDb.getItem(mCurrentSelectedSubjectId).name;
            } catch (AssertionError e){
                if(ENABLE_DEBUG_LOG_CALLS)
                    Log.w("setTitle", "tried to get title for unknow subject id: " + mCurrentSelectedSubjectId);
                Writer.appendLog("tried to get title for unknow subject id: " + mCurrentSelectedSubjectId);
            }
        }
        actionBar.setTitle(title);


        //basically calls onCreateOptionsMenu
        invalidateOptionsMenu();
    }

    //called once to instantiate the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //prepare animations
        final MenuItem presPointItem = menu.findItem(R.id.action_show_admission_counter_dialog);
        final MenuItem infoItem = menu.findItem(R.id.action_show_info_view);

        presPointItem.setActionView(R.layout.menu_action_view_points);
        infoItem.setActionView(R.layout.menu_action_view_info);

        presPointItem.getActionView().setOnClickListener(this);
        presPointItem.getActionView().setTag(R.id.action_show_admission_counter_dialog);

        infoItem.getActionView().setOnClickListener(this);
        infoItem.getActionView().setTag(R.id.action_show_info_view);


        menu.findItem(R.id.action_show_admission_counter_dialog).setVisible(mLastKnownAdmissionCounterViewVisibility);
        menu.findItem(R.id.action_show_info_view).setVisible(mLastKnownPercentageCounterInfoViewVisibility);

        return true;
    }

    // Called whenever we call invalidateOptionsMenu(), whenever the menu is shown
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem admissionCountersItem = menu.findItem(R.id.action_show_admission_counter_dialog);
        final MenuItem admissionPercentageItem = menu.findItem(R.id.action_show_info_view);

        View admissionCounterActionView = admissionCountersItem.getActionView();
        final View admissionPercentageActionView = admissionPercentageItem.getActionView();

        final boolean navDrawerClosing = !mNavigationDrawerFragment.isDrawerOpen();

        //fade in if the drawer is closing
        Animation fade = AnimationUtils.loadAnimation(this, navDrawerClosing ?
                android.R.anim.fade_in : android.R.anim.fade_out);

        final boolean finalHasPercentageCounters = mCurrentSubjectHasPercentageCounters;
        final boolean finalHasAdmissionCounters = mCurrentSubjectHasAdmissionCounters;

        fade.setInterpolator(new AccelerateInterpolator());
        fade.setDuration(300);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                admissionPercentageItem.setVisible(navDrawerClosing && finalHasPercentageCounters);
                admissionCountersItem.setVisible(navDrawerClosing && finalHasAdmissionCounters);
            }
        }, fade.getDuration() - 10);

        if (mCurrentSubjectHasAdmissionCounters)
            admissionCounterActionView.startAnimation(fade);
        if (mCurrentSubjectHasPercentageCounters)
            admissionPercentageActionView.startAnimation(fade);

        //the new item state is visible if the relevant button should be shown and the nav drawer is
        //closed
        mLastKnownPercentageCounterInfoViewVisibility =
                mCurrentSubjectHasPercentageCounters && navDrawerClosing;
        mLastKnownAdmissionCounterViewVisibility =
                mCurrentSubjectHasAdmissionCounters && navDrawerClosing;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_admission_counter_dialog:
                onAdmissionCounterClick();
                return true;
            case R.id.action_show_diagram:/*
                Intent testIntent = new Intent();
                testIntent.putExtra("admission_percentage_id_notification", 1);
                testIntent.putExtra("subject_id_notification", 24);
                new NotificationAlarmReceiver().onReceive(this, testIntent);*/
                onDiagramClick();
                return true;
            case R.id.action_show_info_view:
                getCurrentAdmissionPercentageFragment().showInfoActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int id = (int) view.getTag();
        if (id == R.id.action_show_info_view)
            getCurrentAdmissionPercentageFragment().showInfoActivity();
        if (id == R.id.action_show_admission_counter_dialog)
            onAdmissionCounterClick();
    }

    private void onDiagramClick() {
        startActivity(new Intent(this, ChartActivity.class));
    }

    private void onAdmissionCounterClick() {
        Dialogs.showAdmissionCounterDialog(this, mCurrentSelectedSubjectId);
    }

    private void setPreference(String key, boolean val) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(key, val).apply();
    }

    private SubjectFragment getCurrentSubjectFragment() {
        return (SubjectFragment) getSupportFragmentManager().findFragmentById(R.id.container);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PercentageTrackerFragment test = getCurrentAdmissionPercentageFragment();
        if (test != null) test.trySavepointRelease();
    }

    public PercentageTrackerFragment getCurrentAdmissionPercentageFragment() {
        SubjectFragment currentSubjectFragment = getCurrentSubjectFragment();
        if (currentSubjectFragment == null)
            return null;
        return currentSubjectFragment.getCurrentFragment();
    }
}