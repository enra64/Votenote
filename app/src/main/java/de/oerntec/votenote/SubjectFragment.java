package de.oerntec.votenote;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;

import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBLastViewed;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;
import de.oerntec.votenote.helpers.dialogs.Dialogs;
import de.oerntec.votenote.percentage_tracker_fragment.PercentageTrackerFragment;
import de.oerntec.votenote.preferences.Preferences;

/**
 * Designed to display all percentage trackers and admission point counters for any given subject.
 */

public class SubjectFragment extends Fragment {
    /**
     * Argument name for the subject db id
     */
    private static final String ARG_SUBJECT_ID = "subject_id";

    /**
     * argument name for the subject position in the navigation drawer
     */
    private static final String ARG_SUBJECT_POSITION = "subject_position";

    /**
     * Percentage tracker id to load over the last selected position
     */
    private static final String ARG_FORCED_PERCENTAGE_TRACKER_ID = "admission_id";

    /**
     * Force load a percentage tracker that is not necessarily the most recently selected (used by
     * the notification action button)
     */
    private static final String ARG_FORCE_PERCENTAGE_TRACKER_ID_ENABLED = "admission_id_forced";

    /**
     * Bundle argument to show a lesson add dialog or not
     */
    private static final String ARG_FORCE_DIALOG_SHOW = "show_dialog";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private AdmissionPercentageAdapter mAdmissionPercentageAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    /**
     * subject id we are displaying
     */
    private int mSubjectId;

    /**
     * whether the preference is activated or not
     */
    private boolean mSaveLastMetaId;

    /**
     * Force showing a lesson add dialog?
     */
    private boolean mForceAddLessonDialog;

    /**
     * Force loading a specific percentage tracker?
     */
    private boolean mForceAdmissionPercentageFragmentLoad;

    /**
     * If applicable, the percentage tracker that is to be loaded
     */
    private int mForcedAdmissionPercentageId;

    /**
     * empty fragment constructor
     */
    public SubjectFragment(){}

    /**
     * static fragment factory
     * @param subjectId what subject is this fragment for
     * @return a newly instanced fragment
     */
    public static SubjectFragment newInstance(int subjectId, int subjectPosition) {
        return newInstance(subjectId, subjectPosition, -1, false);
    }

    /**
     * Put the required arguments into a bundle to give to the new fragment
     */
    public static SubjectFragment newInstance(int subjectId, int subjectPosition, int forcedAdmissionPercentageId, boolean forceLessonAddDialog) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(ARG_SUBJECT_ID, subjectId);
        args.putInt(ARG_SUBJECT_POSITION, subjectPosition);

        //forced admission id load?
        args.putBoolean(ARG_FORCE_PERCENTAGE_TRACKER_ID_ENABLED, forcedAdmissionPercentageId != -1);
        if (forcedAdmissionPercentageId != -1)
            args.putInt(ARG_FORCED_PERCENTAGE_TRACKER_ID, forcedAdmissionPercentageId);

        //show lesson add dialog?
        args.putBoolean(ARG_FORCE_DIALOG_SHOW, forceLessonAddDialog);

        SubjectFragment fragment = new SubjectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSubjectId = getArguments().getInt(ARG_SUBJECT_ID, -1);
        if (mSubjectId == -1)
            throw new AssertionError("trying to load a non-existing subject!");

        mForceAddLessonDialog = getArguments().getBoolean(ARG_FORCE_DIALOG_SHOW, false);

        mForceAdmissionPercentageFragmentLoad = getArguments().getBoolean(ARG_FORCE_PERCENTAGE_TRACKER_ID_ENABLED, false);

        if (mForceAdmissionPercentageFragmentLoad)
            mForcedAdmissionPercentageId = getArguments().getInt(ARG_FORCED_PERCENTAGE_TRACKER_ID, -1);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.percentage_tracker_host_fragment, container, false);

        //create and assign an adapter for showing the individual admission percentage fragments
        mViewPager = (ViewPager) rootView.findViewById(R.id.fragment_lesson_tabbed_pager);
        mAdmissionPercentageAdapter = new AdmissionPercentageAdapter(getChildFragmentManager(), DBPercentageTracker.getInstance(), mSubjectId);
        mViewPager.setAdapter(mAdmissionPercentageAdapter);

        //hide the top bar showing the percentage counter titles if only one exists
        if(mAdmissionPercentageAdapter.getCount() == 1)
            mViewPager.findViewById(R.id.fragment_lesson_tabbed_pager_title_strip).setVisibility(View.GONE);

        if (mAdmissionPercentageAdapter.getCount() == 0) {
            rootView.findViewById(R.id.no_admission_percentage_counters_warner_text).setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        } else {
            rootView.findViewById(R.id.no_admission_percentage_counters_warner_text).setVisibility(View.GONE);
            mViewPager.setVisibility(View.VISIBLE);
        }

        //do we need to save that percentage counter we viewed last?
        mSaveLastMetaId = Preferences.getPreference(getActivity(), "save_last_selected_meta_pos", true);

        ((MainActivity) getActivity()).restoreActionBar();

        return rootView;
    }

    /**
     * get the currently displayed fragment
     */
    public PercentageTrackerFragment getCurrentFragment() {
        return mAdmissionPercentageAdapter.getFragmentInstance(mViewPager.getCurrentItem());
    }

    /**
     * get the position in the adapter via the id
     */
    private int getPositionFromId(int id) {
        List<PercentageTrackerPojo> data = DBPercentageTracker.getInstance().getItemsForSubject(mSubjectId);
        for (int position = 0; position < data.size(); position++)
            if (id == data.get(position).id)
                return position;
        return -1;
    }

    /**
     * load an admission percentage fragment via id, also try to show the lesson add dialog
     */
    private void setAdmissionPercentageFragmentById(int admissionPercentageFragmentId, boolean forceLessonAddDialog) {
        //get position corresponding to the id
        int admissionPercentageFragmentPosition = getPositionFromId(admissionPercentageFragmentId);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS) {
            Log.i("subfrag", "id: " + admissionPercentageFragmentId);
            Log.i("subfrag", "calculated position: " + admissionPercentageFragmentPosition);
        }
        mViewPager.setCurrentItem(admissionPercentageFragmentPosition);

        if (forceLessonAddDialog)
            Dialogs.showAddLessonDialog(getFragmentManager(), admissionPercentageFragmentId);
    }

    /**
     * load either the last selected, the default, or the forced admission percentage fragment
     * @param subjectPosition position of the subject to load
     */
    private void loadAppropriateAdmissionPercentageFragment(int subjectPosition) {
        List<PercentageTrackerPojo> percentages = DBPercentageTracker.getInstance().getItemsForSubject(mSubjectId);

        if (mForceAdmissionPercentageFragmentLoad) {
            setAdmissionPercentageFragmentById(mForcedAdmissionPercentageId, mForceAddLessonDialog);
            return;
        }

        //check whether the subject has percentage counters
        if (percentages.size() > 0) {
            //is the subject position valid?
            if(subjectPosition >= 0 && mSaveLastMetaId){
                //see whether we have a saved last viewed admission counter
                int lastSelectedMetaPosition = DBLastViewed.getInstance().getLastPercentageTrackerPosition(subjectPosition);

                // no previous entries exist
                if (lastSelectedMetaPosition < 0) {
                    // is there anything we can select?
                    if (mAdmissionPercentageAdapter.getCount() > 0) {
                        // load some percentage tracker
                        mViewPager.setCurrentItem(0);

                        // because the listener does not get called with setCurrentItem, we save this manually.
                        DBLastViewed.getInstance().saveLastTrackerAndSubjectPosition(subjectPosition, 0);

                        // logging
                        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                            Log.i("subject fragment", "no previous percentage tracker pos recorded");
                    }
                }

                // previous entries exist
                else if (lastSelectedMetaPosition >= 0) {
                    // is that entry valid?
                    if (lastSelectedMetaPosition < mAdmissionPercentageAdapter.getCount()) {
                        // set current item
                        mViewPager.setCurrentItem(lastSelectedMetaPosition);

                        // logging
                        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                            Log.i("subject fragment", "valid percentage tracker position, selected");
                    }
                    // the previous entry is invalid
                    else {
                        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                            Log.i("subject fragment", "invalid percentage tracker position");
                    }
                }
            } else // just load the first available tracker
                mViewPager.setCurrentItem(0);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        final int subjectPosition = getArguments().getInt(ARG_SUBJECT_POSITION);

        loadAppropriateAdmissionPercentageFragment(subjectPosition);

        if(mSaveLastMetaId){
            if (DBPercentageTracker.getInstance().getItemsForSubject(mSubjectId).size() > 1) {
                mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }

                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageSelected(int position) {
                        //only called on change (eg first load not notified)
                        DBLastViewed.getInstance().saveLastTrackerAndSubjectPosition(subjectPosition, position);
                    }
                });
            }
            // there is only one percentage tracker for this subject, but we still need to save the
            // last select subject
            else
                DBLastViewed.getInstance().saveLastTrackerAndSubjectPosition(subjectPosition, 0);
        } else {
            DBLastViewed.getInstance().saveLastTrackerAndSubjectPosition(subjectPosition, 0);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages, e.g. a fragment showing all info for a selected admission
     * percentage counter
     */
    public class AdmissionPercentageAdapter extends FragmentPagerAdapter {
        private final DBPercentageTracker mMetaDb;
        private final int mSubjectId;
        private final HashMap<Integer, PercentageTrackerFragment> mReferenceMap;
        private List<PercentageTrackerPojo> mData;

        public AdmissionPercentageAdapter(FragmentManager fm, DBPercentageTracker dbMeta, int subjectId) {
            super(fm);
            mMetaDb = dbMeta;
            mSubjectId = subjectId;
            reload();

            mReferenceMap = new HashMap<>();
        }

        public void reload(){
            mData = mMetaDb.getItemsForSubject(mSubjectId);
        }

        /**
         * Instantiate a pager fragment
         */
        @Override
        public Fragment getItem(int position) {
            PercentageTrackerFragment fragment = PercentageTrackerFragment.newInstance(mData.get(position).id);
            mReferenceMap.put(position, fragment);
            return fragment;
        }

        /**
         * return the saved instance reference to the given id
         */
        public PercentageTrackerFragment getFragmentInstance(Integer requestedPosition) {
            return mReferenceMap.get(requestedPosition);
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mData.get(position).name;
        }
    }
}
