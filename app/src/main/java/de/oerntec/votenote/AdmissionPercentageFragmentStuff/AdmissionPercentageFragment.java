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
package de.oerntec.votenote.AdmissionPercentageFragmentStuff;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import de.oerntec.votenote.AdmissionPercentageOverview.AdmissionPercentageOverviewActivity;
import de.oerntec.votenote.AdmissionPercentageOverview.AdmissionPercentageOverviewFragment;
import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.CardListHelpers.SwipeDeletion;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
import de.oerntec.votenote.Database.Pojo.Lesson;
import de.oerntec.votenote.Database.TableHelpers.DBLessons;
import de.oerntec.votenote.Dialogs.MainDialogHelper;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementListActivity;

/**
 * This fragment is used to display all lessons of a single admissionPercentageMeta item.
 */
public class AdmissionPercentageFragment extends Fragment implements SwipeDeletion.UndoSnackBarHost {
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
    public AdmissionPercentageAdapter mAdapter;

    /**
     * Save the view that was clicked last so we can execute a programmatic swipe deletion
     */
    private View mLastClickedView = null;

    /**
     * The percentage meta id all data in this fragment has
     */
    private int mAdmissionPercentageMetaId;

    /**
     * POJO object to hold the admission percentage meta data from fragment start
     */
    private AdmissionPercentageMetaPojo mMetaObject;

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

    private boolean mEnableSwipeToDelete;

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static AdmissionPercentageFragment newInstance(int percentageMetaId) {
        AdmissionPercentageFragment fragment = new AdmissionPercentageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PERCENTAGE_META_ID, percentageMetaId);
        fragment.setArguments(args);
        return fragment;
    }

    public int getAdmissionPercentageMetaId(){
        return mAdmissionPercentageMetaId;
    }

    /* MAIN FRAGMENT BUILDING
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
        mAdapter = new AdmissionPercentageAdapter(getActivity(), mAdmissionPercentageMetaId);
        mLessonList.setAdapter(mAdapter);

        //get the meta pojo containing available info on this percentage counter
        mMetaObject = mAdapter.getCurrentMeta();

        //log entry if no subjects are available
        if (mAdapter.getItemCount() == 0)
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.e("Main Listview", "Received Empty allEntryCursor for group " + mMetaObject.getDisplayName() + " with id " + mAdmissionPercentageMetaId);

        enableOnClickForLessons();

        enableSwipeToDelete();

        mEnableSwipeToDelete = MainActivity.getPreference("enable_swipe_to_delete_lesson", true);

        //find the add button and add a listener
        FloatingActionButton addFab = (FloatingActionButton) rootView.findViewById(R.id.subject_fragment_add_fab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainDialogHelper.showAddLessonDialog(getFragmentManager(), mAdmissionPercentageMetaId);
            }
        });

        mRootView = rootView;
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_percentage_counter){
            showInfoActivity();
            //handy-dandy exception thrower for exception handling testing
            //Integer.valueOf("rip");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showInfoActivity(){
        Intent intent = new Intent(getActivity(), AdmissionPercentageOverviewActivity.class);
        intent.putExtra(AdmissionPercentageOverviewFragment.PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID, mAdmissionPercentageMetaId);
        startActivity(intent);
    }

    /**
     * Instances a SwipeCallback to enable swipe to delete
     */
    private void enableSwipeToDelete() {
        //check whether we already have asked whether to enable swipe to delete
        boolean enableSwipeDisableDialog = !MainActivity.getPreference("enable_swipe_to_delete_lesson_dialog_shown", false);

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(enableSwipeDisableDialog));
        itemTouchHelper.attachToRecyclerView(mLessonList);
    }

    /**
     * attach an onClick listener on the lesson list
     */
    private void enableOnClickForLessons() {
        //onClick for lessons
        mLessonList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mLessonList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if (position != 0){
                    mLastClickedView = view;
                    MainDialogHelper.showChangeLessonDialog(getFragmentManager(), mAdmissionPercentageMetaId, (Integer) view.getTag());
                }
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));
    }

    private void showSwipeToDeleteDialog(final int swipedLessonId, final int percentageMetaId) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.activate_swipe_to_delete_title)
                .setMessage(R.string.swipe_to_delete_lesson_enable_feature_dialog_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveSwipeToDeleteSelection(true, swipedLessonId, percentageMetaId);
                    }
                })
                .setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveSwipeToDeleteSelection(false, swipedLessonId, percentageMetaId);
                    }
                });
        b.show();
    }

    private void saveSwipeToDeleteSelection(boolean enable, int swipedLessonId, int percentageMetaId) {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("enable_swipe_to_delete_lesson", enable).apply();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("enable_swipe_to_delete_lesson_dialog_shown", true).apply();
        mEnableSwipeToDelete = enable;
        mAdapter.refreshItem(swipedLessonId, percentageMetaId);
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
        Snackbar
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
                            default:
                                super.onDismissed(snackbar, event);
                        }
                    }
                }).show();
    }

    /**
     * Release the savepoint -> save()
     */
    private void trySavepointRelease() {
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
     * @param mOldData the data the dialog was called with
     */
    public void deleteLessonAnimated(Lesson mOldData) {
        if(mLastClickedView == null)
            throw new AssertionError("mLastClickedView must not be null here, because it must only be called from the dialogfragment");
        SwipeDeletion.executeProgrammaticSwipeDeletion(getActivity(), this, mLastClickedView, mAdapter.getRecyclerViewPosition(mOldData));
        mLastClickedView = null;
    }

    private class SwipeCallback extends ItemTouchHelper.SimpleCallback {
        boolean mEnableSwipeDisableDialog;

        public SwipeCallback(boolean enableSwipeDisableDialog) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mEnableSwipeDisableDialog = enableSwipeDisableDialog;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            //no swipey swipey for info view
            if (viewHolder instanceof AdmissionPercentageAdapter.InfoHolder) return 0;
            if (!mEnableSwipeToDelete) return 0;
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            //info view has no tag
            Object viewTag = viewHolder.itemView.getTag();
            if (viewTag != null) {
                int lessonId = (Integer) viewTag;
                if (lessonId != 0) {
                    if (mEnableSwipeDisableDialog)
                        showUndoSnackBar(lessonId, mLessonList.getChildAdapterPosition(viewHolder.itemView));
                    else
                        showSwipeToDeleteDialog(lessonId, mAdmissionPercentageMetaId);
                }
            }
        }
    }
}