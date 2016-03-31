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
package de.oerntec.votenote.percentage_tracker_fragment;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.Lesson;
import de.oerntec.votenote.database.tablehelpers.DBLessons;
import de.oerntec.votenote.helpers.DialogHelper;
import de.oerntec.votenote.helpers.OnItemClickListener;
import de.oerntec.votenote.helpers.RecyclerItemClickListener;
import de.oerntec.votenote.helpers.SwipeAnimationUtils;
import de.oerntec.votenote.percentage_tracker_overview.AdmissionPercentageOverviewActivity;
import de.oerntec.votenote.percentage_tracker_overview.AdmissionPercentageOverviewFragment;
import de.oerntec.votenote.subject_management.SubjectManagementListActivity;

/**
 * This fragment is used to display all lessons of a single admissionPercentageMeta item.
 */
public class PercentageTrackerFragment extends Fragment implements SwipeAnimationUtils.UndoSnackBarHost {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_PERCENTAGE_META_ID = "percentage_id";

    /**
     * Savepoint last used, null if no savepoint can currently be used
     */
    private static String mLastRemovalSavePointId = null;
    /**
     * Adapter to display the lessons
     */
    public PercentageTrackerAdapter mAdapter;
    /**
     * If applicable, the latest snackbar, null otherwise
     */
    private Snackbar mLastRemovalSnackbar = null;
    /**
     * Save the view that was clicked last so we can execute a programmatic swipe deletion
     */
    private View mLastClickedView = null;

    /**
     * The percentage meta id all data in this fragment has
     */
    private int mAdmissionPercentageMetaId;

    /**
     * Parent viewgroup
     */
    private View mRootView;

    /**
     * List containing the vote data
     */
    private RecyclerView mLessonList;

    /**
     * db holding percentage data
     */
    private DBLessons mDataDb = DBLessons.getInstance();

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static PercentageTrackerFragment newInstance(int percentageMetaId) {
        PercentageTrackerFragment fragment = new PercentageTrackerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PERCENTAGE_META_ID, percentageMetaId);
        fragment.setArguments(args);
        return fragment;
    }

    /*
     * Build the menu_main fragment containing uebung specific info, and the menu_main listview
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //save the subject we are currently working for
        mAdmissionPercentageMetaId = getArguments().getInt(ARG_PERCENTAGE_META_ID);

        //inflate root view
        View rootView = inflater.inflate(R.layout.admission_percentage_fragment, container, false);


        //config the recyclerview
        mLessonList = (RecyclerView) rootView.findViewById(R.id.subject_fragment_recyclerview);
        mLessonList.setHasFixedSize(true);

        //give it a layout manager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mLessonList.setLayoutManager(manager);


        //if translatedSection is -1, no group has been added yet, so show the subject manager
        if (mAdmissionPercentageMetaId == -1) {
            Intent intent = new Intent(getActivity(), SubjectManagementListActivity.class);
            intent.putExtra("firstGroup", true);
            getActivity().startActivityForResult(intent, MainActivity.ADD_FIRST_SUBJECT_REQUEST);
            return rootView;
        }

        //create and set adapter for the lesson list -> show lessons for this percentage counter
        mAdapter = new PercentageTrackerAdapter(getActivity(), mAdmissionPercentageMetaId);
        mLessonList.setAdapter(mAdapter);

        //log entry if no subjects are available
        if (mAdapter.getItemCount() == 0)
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.e("Main Listview", "Received Empty allEntryCursor for group " + mAdapter.getCurrentMeta().getDisplayName() + " with id " + mAdmissionPercentageMetaId);

        enableOnClickForLessons();

        //find the add button and add a listener
        FloatingActionButton addFab = (FloatingActionButton) rootView.findViewById(R.id.subject_fragment_add_fab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showAddLessonDialog(getFragmentManager(), mAdmissionPercentageMetaId);
            }
        });

        mRootView = rootView;
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_percentage_counter) {
            showInfoActivity();
            //handy-dandy exception thrower for exception handling testing
            //Integer.valueOf("rip");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showInfoActivity() {
        Intent intent = new Intent(getActivity(), AdmissionPercentageOverviewActivity.class);
        intent.putExtra(AdmissionPercentageOverviewFragment.PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID, mAdmissionPercentageMetaId);
        startActivity(intent);
    }

    /**
     * attach an onClick listener on the lesson list
     */
    private void enableOnClickForLessons() {
        final PercentageTrackerFragment bestCoding = this;
        //onClick for lessons
        mLessonList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mLessonList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if (position != 0) {
                    mLastClickedView = view;
                    DialogHelper.showChangeLessonDialog(getFragmentManager(), mAdmissionPercentageMetaId, (Integer) view.getTag());
                }
            }

            public void onItemLongClick(final View view, int position) {
                if (position != 0) {
                    mLastClickedView = view;
                    DialogHelper.showDeleteDialog(getActivity(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Lesson oldLesson = mDataDb.getItem((int) view.getTag(), mAdmissionPercentageMetaId);
                            deleteLessonAnimated(oldLesson);
                        }
                    });
                }
            }
        }));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLastRemovalSnackbar != null)
            mLastRemovalSnackbar.dismiss();
    }

    public void reloadInfo() {
        mAdapter.notifyItemChanged(0);
    }

    public void showUndoSnackBar(final int lessonId, int lessonPosition) {
        //if another one has been deleted before this one, release that sp now
        trySavepointRelease();
        //remove the lesson. creates a savepoint before that, returns the id
        mLastRemovalSavePointId = mAdapter.removeLesson(new Lesson(mAdmissionPercentageMetaId, lessonId, -1, -1));
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("apf", "creating savepoint " + mLastRemovalSavePointId);
        mLastRemovalSnackbar = Snackbar
                .make(mRootView.findViewById(R.id.subject_fragment_coordinator_layout), getActivity().getString(R.string.undobar_deleted), Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onUndo();
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        switch (event) {
                            case DISMISS_EVENT_MANUAL:
                            case DISMISS_EVENT_TIMEOUT:
                                //DISMISS_EVENT_CONSECUTIVE: the consecutive remove hits after the next one is released, so that one wont find its savepoint
                                trySavepointRelease();
                                mLastRemovalSnackbar = null;
                            default:
                                super.onDismissed(snackbar, event);
                        }
                    }
                });
        mLastRemovalSnackbar.show();
    }

    /**
     * Release the savepoint -> save()
     */
    public void trySavepointRelease() {
        if (mLastRemovalSavePointId != null) {
            mDataDb.releaseSavepoint(mLastRemovalSavePointId);
            mLastRemovalSavePointId = null;
        }
    }

    /**
     * Initiate a rollback to the last savepoint, and delete it, so we know if we fuck up by some
     * fancy ass null pointer exceptions. Also force the adapter to show that lesson again.
     */
    private void onUndo() {
        if (mLastRemovalSavePointId != null) {
            DBLessons.getInstance().rollbackToSavepoint(mLastRemovalSavePointId);
            mLastRemovalSavePointId = null;
            mAdapter.reinstateLesson();
        }
    }

    /**
     * delete a lesson with programmatic swipe
     *
     * @param mOldData the data the dialog was called with
     */
    public void deleteLessonAnimated(Lesson mOldData) {
        if (mLastClickedView == null)
            throw new AssertionError("mLastClickedView must not be null here, because it must only be called after clicking a lesson");
        SwipeAnimationUtils.executeProgrammaticSwipeDeletion(getActivity(), this, mLastClickedView, mAdapter.getRecyclerViewPosition(mOldData));
        mLastClickedView = null;
    }
}