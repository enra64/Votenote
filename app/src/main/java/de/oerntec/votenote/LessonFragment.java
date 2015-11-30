package de.oerntec.votenote;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;

import de.oerntec.votenote.AdmissionPercentageFragmentStuff.AdmissionPercentageFragment;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;

public class LessonFragment extends Fragment {

    private static final String ARG_SUBJECT_ID = "subject_id";
    private static final String ARG_PERCENTAGE_ID = "meta_id";
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

    private int mSubjectId;

    /**
     * empty fragment constructor
     */
    public LessonFragment(){}

    /**
     * static fragment factory
     * @param subjectId what subject is this fragment for
     * @return a newly instanced fragment
     */
    public static LessonFragment newInstance(int subjectId, int metaId) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(ARG_SUBJECT_ID, subjectId);
        args.putInt(ARG_PERCENTAGE_ID, metaId);

        LessonFragment fragment = new LessonFragment();
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
        View v = inflater.inflate(R.layout.fragment_lesson_tabbed, container, false);
        mAdmissionPercentageAdapter = new AdmissionPercentageAdapter(getChildFragmentManager(), DBAdmissionPercentageMeta.getInstance(), mSubjectId);

        mViewPager = (ViewPager) v.findViewById(R.id.fragment_lesson_tabbed_pager);
        mViewPager.setAdapter(mAdmissionPercentageAdapter);

        if(mAdmissionPercentageAdapter.getCount() == 1)
            mViewPager.findViewById(R.id.fragment_lesson_tabbed_pager_title_strip).setVisibility(View.GONE);

        return v;
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
        if (percentages.size() > 0) {
            if(getArguments().getInt(ARG_PERCENTAGE_ID) >= 0)
                mViewPager.setCurrentItem(getArguments().getInt(ARG_PERCENTAGE_ID));
            else
                mViewPager.setCurrentItem(0);
        }
    }

    public void forceSelectPosition(int position) {
        mViewPager.setCurrentItem(position, true);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
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
                throw new AssertionError("could not find anything at " + requestedPosition);
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
