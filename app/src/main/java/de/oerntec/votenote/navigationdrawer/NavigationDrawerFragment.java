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
package de.oerntec.votenote.navigationdrawer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.Subject;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;
import de.oerntec.votenote.helpers.lists.LinearLayoutManagerWrapper;
import de.oerntec.votenote.preferences.PreferencesActivity;
import de.oerntec.votenote.subject_management.SubjectManagementListActivity;

/**
 * Fragment used for managing interactions for and presentation of a navigation
 * drawer. See the <a href=
 * "https://developer.android.com/design/patterns/navigation-drawer.html#Interaction"
 * > design guidelines</a> for a complete explanation of the behaviors
 * implemented here.
 */
public class NavigationDrawerFragment extends Fragment implements SelectionCallback {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the
     * user manually expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;
    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    @SuppressWarnings("deprecation")
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private RecyclerView mSubjectList;
    private View mFragmentContainerView;
    private TextView settingsDescriptionText, editDescriptionText;

    private int mCurrentSelectedPosition = 0;
    private boolean mUserLearnedDrawer;

    private NavigationDrawerAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated
        // awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null)
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of
        // actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.navigation_fragment, container, false);
        mSubjectList = (RecyclerView) root.findViewById(R.id.navigation_drawer_list);

        //save references to be able to update the text
        editDescriptionText = (TextView) root.findViewById(R.id.navigation_drawer_edit_description);
        settingsDescriptionText = (TextView) root.findViewById(R.id.navigation_drawer_settings_description);

        //attach listener to settings text
        root.findViewById(R.id.navigation_drawer_bottom_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), PreferencesActivity.class));
            }
        });
        root.findViewById(R.id.navigation_drawer_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), SubjectManagementListActivity.class));
            }
        });
        return root;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation
     * drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(R.id.navigation_drawer);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the menu_main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mAdapter = new NavigationDrawerAdapter(getActivity(), this);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManagerWrapper(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mSubjectList.setLayoutManager(manager);
        mSubjectList.setAdapter(mAdapter);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("VoteNote");
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        //noinspection deprecation
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                R.string.navigation_drawer_open, /*
                                         * "open drawer" description for
										 * accessibility
										 */
                R.string.navigation_drawer_close /*
                                         * "close drawer" description for
										 * accessibility
										 */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded())
                    return;

                getActivity().invalidateOptionsMenu(); // calls
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                //prevent auto showing drawer
                if (!mUserLearnedDrawer) {
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true)
                            .apply();
                }

                getActivity().invalidateOptionsMenu(); // calls mainactivity onprepare
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }


    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    /**
     * get the position in the adapter via the id
     */
    private int getPositionFromId(int id) {
        List<Subject> data = DBSubjects.getInstance().getAllSubjects();
        for (int position = 0; position < data.size(); position++)
            if (id == data.get(position).id)
                return position;
        return -1;
    }


    public void forceAdmissionPercentageFragmentDialogById(int subjectId, int admissionPercentageId) {
        forceAdmissionPercentageFragmentDialog(getPositionFromId(subjectId), admissionPercentageId);
    }

    private void forceAdmissionPercentageFragmentDialog(int position, int admissionPercentageId) {
        mCurrentSelectedPosition = position;
        if (mAdapter != null)
            mAdapter.setCurrentSelection(position);

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(mFragmentContainerView);

        if (mCallbacks != null) {
            if (admissionPercentageId == -1)
                mCallbacks.onNavigationDrawerItemSelected(position);
            else
                mCallbacks.onNavigationDrawerItemSelected(position, admissionPercentageId, true);

        }
    }

    public void selectItem(int position) {
        forceAdmissionPercentageFragmentDialog(position, -1);
    }

    public void reloadAdapter() {
        mAdapter.requeryAndReload();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //update texts to force translation update
        editDescriptionText.setText(getActivity().getString(R.string.action_control_subjects));
        settingsDescriptionText.setText(getActivity().getString(R.string.action_show_settings));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //gets called upon drawer click
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * called when an item in the navigation list is clicked
     */
    @Override
    public void onItemClick(int position) {
            selectItem(position);
    }

    /**
     * Callbacks interface that all activities using this fragment must
     * implement.
     */
    public interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position);

        void onNavigationDrawerItemSelected(int position, int admissionPercentageId, boolean forceLessonAddDialog);
    }


}