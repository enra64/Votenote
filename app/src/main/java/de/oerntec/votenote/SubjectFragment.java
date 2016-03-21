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
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBLastViewed;
import de.oerntec.votenote.Dialogs.MainDialogHelper;

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

    private static final String ARG_FORCED_ADMISSION_ID = "admission_id";
    private static final String ARG_FORCE_ADMISSION_ID_ENABLED = "admission_id_forced";
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

    private boolean mForceAddLessonDialog;

    private boolean mForceAdmissionPercentageFragmentLoad;

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

    public static SubjectFragment newInstance(int subjectId, int subjectPosition, int forcedAdmissionPercentageId, boolean forceLessonAddDialog) {
        Bundle args = new Bundle();
        //put arguments into an intent
        args.putInt(ARG_SUBJECT_ID, subjectId);
        args.putInt(ARG_SUBJECT_POSITION, subjectPosition);

        //forced admission id load?
        args.putBoolean(ARG_FORCE_ADMISSION_ID_ENABLED, forcedAdmissionPercentageId != -1);
        if (forcedAdmissionPercentageId != -1)
            args.putInt(ARG_FORCED_ADMISSION_ID, forcedAdmissionPercentageId);

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

        mForceAdmissionPercentageFragmentLoad = getArguments().getBoolean(ARG_FORCE_ADMISSION_ID_ENABLED, false);

        if (mForceAdmissionPercentageFragmentLoad)
            mForcedAdmissionPercentageId = getArguments().getInt(ARG_FORCED_ADMISSION_ID, -1);
    }

    public int getSubjectId(){
        return mSubjectId;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.subject_fragment, container, false);

        //create and assign an adapter for showing the individual admission percentage fragments
        mViewPager = (ViewPager) rootView.findViewById(R.id.fragment_lesson_tabbed_pager);
        mAdmissionPercentageAdapter = new AdmissionPercentageAdapter(getChildFragmentManager(), DBAdmissionPercentageMeta.getInstance(), mSubjectId);
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

    /**
     * get the position in the adapter via the id
     */
    private int getPositionFromId(int id) {
        List<AdmissionPercentageMetaPojo> data = DBAdmissionPercentageMeta.getInstance().getItemsForSubject(mSubjectId);
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
            MainDialogHelper.showAddLessonDialog(getFragmentManager(), admissionPercentageFragmentId);
    }

    /**
     * load either the last selected, the default, or the forced admission percentage fragment
     */
    private void loadAppropriateAdmissionPercentageFragment() {
        List<AdmissionPercentageMetaPojo> percentages = DBAdmissionPercentageMeta.getInstance().getItemsForSubject(mSubjectId);
        final int subjectPosition = getArguments().getInt(ARG_SUBJECT_POSITION);

        if (mForceAdmissionPercentageFragmentLoad) {
            setAdmissionPercentageFragmentById(mForcedAdmissionPercentageId, mForceAddLessonDialog);
            return;
        }

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
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        final int subjectPosition = getArguments().getInt(ARG_SUBJECT_POSITION);

        loadAppropriateAdmissionPercentageFragment();

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
        private List<AdmissionPercentageMetaPojo> mData;
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
