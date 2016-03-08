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

import de.oerntec.votenote.AdmissionPercentageFragmentStuff.AdmissionPercentageFragment;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBLastViewed;

/**
 * This class shows **all** info available on a subject, including all percentage counters (swipe to the left/right) and all point counters
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
     * empty fragment constructor
     */
    public SubjectFragment(){}

    /**
     * static fragment factory
     * @param subjectId what subject is this fragment for
     * @return a newly instanced fragment
     */
    public static SubjectFragment newInstance(int subjectId, int subjectPosition) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(ARG_SUBJECT_ID, subjectId);
        args.putInt(ARG_SUBJECT_POSITION, subjectPosition);

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
    }

    public int getSubjectId(){
        return mSubjectId;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lesson_tabbed, container, false);

        //create and assign an adapter for showing the individual admission percentage fragments
        mViewPager = (ViewPager) rootView.findViewById(R.id.fragment_lesson_tabbed_pager);
        mAdmissionPercentageAdapter = new AdmissionPercentageAdapter(getChildFragmentManager(), DBAdmissionPercentageMeta.getInstance(), mSubjectId);
        mViewPager.setAdapter(mAdmissionPercentageAdapter);

        //hide the top bar showing the percentage counter titles if only one exists
        if(mAdmissionPercentageAdapter.getCount() == 1)
            mViewPager.findViewById(R.id.fragment_lesson_tabbed_pager_title_strip).setVisibility(View.GONE);

        //do we need to save that percentage counter we viewed last?
        mSaveLastMetaId = MainActivity.getPreference("save_last_selected_meta_pos", true);

        ((MainActivity) getActivity()).restoreActionBar();

        return rootView;
    }

    /**
     * get the currently displayed fragment
     */
    public AdmissionPercentageFragment getCurrentFragment() {
        AdmissionPercentageFragment current = mAdmissionPercentageAdapter.getFragmentInstance(mViewPager.getCurrentItem());
        if (current == null)
            throw new AssertionError("could not find current fragment");
        return current;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        List<AdmissionPercentageMeta> percentages = DBAdmissionPercentageMeta.getInstance().getItemsForSubject(mSubjectId);

        final int subjectPosition = getArguments().getInt(ARG_SUBJECT_POSITION);

        //check whether the subject has percentage counters
        if (percentages.size() > 0) {
            //is the subject position valid?
            if(subjectPosition >= 0 && mSaveLastMetaId){
                //see whether we have a saved last viewed admission counter
                int lastSelectedMetaPosition = DBLastViewed.getInstance().getLastSelectedAdmissionCounterForSubjectPosition(subjectPosition);
                //valid position?
                if (lastSelectedMetaPosition >= 0
                        && lastSelectedMetaPosition < mAdmissionPercentageAdapter.getCount())
                    //valid last viewed admission percentage position -> view that
                    mViewPager.setCurrentItem(lastSelectedMetaPosition, true);
                    //nah, log that
                else if(MainActivity.ENABLE_DEBUG_LOG_CALLS)
                    Log.i("subject fragment", "invalid meta position");
            }
            else
                mViewPager.setCurrentItem(0);
        }
        if(mSaveLastMetaId){
            if (DBAdmissionPercentageMeta.getInstance().getItemsForSubject(mSubjectId).size() > 1) {
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
                        DBLastViewed.getInstance().saveSelection(subjectPosition, position);
                    }
                });
            } else
                DBLastViewed.getInstance().saveSelection(subjectPosition, 0);
        } else {
            DBLastViewed.getInstance().saveSelection(subjectPosition, 0);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages, e.g. a fragment showing all info for a selected admission
     * percentage counter
     */
    public class AdmissionPercentageAdapter extends FragmentPagerAdapter {
        private DBAdmissionPercentageMeta mMetaDb;
        private List<AdmissionPercentageMeta> mData;
        private int mSubjectId;
        private HashMap<Integer, AdmissionPercentageFragment> mReferenceMap;

        public AdmissionPercentageAdapter(FragmentManager fm, DBAdmissionPercentageMeta dbMeta, int subjectId) {
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
            AdmissionPercentageFragment fragment = AdmissionPercentageFragment.newInstance(mData.get(position).id);
            mReferenceMap.put(position, fragment);
            return fragment;
        }

        /**
         * return the saved instance reference to the given id
         */
        public AdmissionPercentageFragment getFragmentInstance(Integer requestedPosition) {
            AdmissionPercentageFragment instance = mReferenceMap.get(requestedPosition);
            if(instance == null)
                throw new AssertionError("could not find an admission percentage fragment at " + requestedPosition);
            return instance;
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
