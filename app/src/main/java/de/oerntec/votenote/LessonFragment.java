package de.oerntec.votenote;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.oerntec.votenote.AdmissionPercentageFragmentStuff.AdmissionPercentageFragment;
import de.oerntec.votenote.Database.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.DBAdmissionPercentageMeta;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.SubjectCreationActivity;

public class LessonFragment extends Fragment {

    private static final String ARG_SUBJECT_ID = "subject_id";
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
    public static LessonFragment newInstance(int subjectId) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(ARG_SUBJECT_ID, subjectId);

        LessonFragment fragment = new LessonFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSubjectId = getArguments().getInt(SubjectCreationActivity.SUBJECT_CREATOR_SUBJECT_ID_ARGUMENT_NAME, -1);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_lesson_tabbed, container, false);
        mAdmissionPercentageAdapter = new AdmissionPercentageAdapter(getChildFragmentManager(), DBAdmissionPercentageMeta.getInstance(), mSubjectId);

        mViewPager = (ViewPager) v.findViewById(R.id.fragment_lesson_tabbed_pager);
        mViewPager.setAdapter(mAdmissionPercentageAdapter);

        return v;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class AdmissionPercentageAdapter extends FragmentPagerAdapter {
        private DBAdmissionPercentageMeta mMetaDb;
        private List<AdmissionPercentageMeta> mData;
        private int mSubjectId;

        public AdmissionPercentageAdapter(FragmentManager fm, DBAdmissionPercentageMeta dbMeta, int subjectId) {
            super(fm);
            mMetaDb = dbMeta;
            mSubjectId = subjectId;

            reload();
        }

        public void reload(){
            mData = mMetaDb.getItemsForSubject(mSubjectId);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return AdmissionPercentageFragment.newInstance(mData.get(position).id);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return mData.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mData.get(position).name;
        }
    }
}
