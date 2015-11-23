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


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.CardListHelpers.SwipeDeletion;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Dialogs.MainDialogHelper;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

/**
 * A placeholder fragment containing a simple view.
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
    private int mPercentageMetaId;

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
    private DBAdmissionPercentageData mDataDb = DBAdmissionPercentageData.getInstance();

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
        return mPercentageMetaId;
    }

    /* MAIN FRAGMENT BUILDING
     * Build the menu_main fragment containing uebung specific info, and the menu_main listview
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //save the subject we are currently working for
        mPercentageMetaId = getArguments().getInt(ARG_PERCENTAGE_META_ID);

        //inflate and find views
        View rootView = inflater.inflate(R.layout.subject_fragment, container, false);
        FloatingActionButton addFab = (FloatingActionButton) rootView.findViewById(R.id.subject_fragment_add_fab);
        mLessonList = (RecyclerView) rootView.findViewById(R.id.subject_fragment_recyclerview);

        //config the recyclerview
        mLessonList.setHasFixedSize(true);

        //give it a layout manager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mLessonList.setLayoutManager(manager);


        //if translatedSection is -1, no group has been added yet
        if (mPercentageMetaId == -1) {
            Intent intent = new Intent(getActivity(), SubjectManagementActivity.class);
            intent.putExtra("firstGroup", true);
            getActivity().startActivityForResult(intent, MainActivity.ADD_FIRST_SUBJECT_REQUEST);
            return rootView;
        }

        /*
        DISPLAY GROUP INFO
         */
        mAdapter = new AdmissionPercentageAdapter(getActivity(), mPercentageMetaId);
        //set adapter
        mLessonList.setAdapter(mAdapter);

        AdmissionPercentageMeta currentMetaObject = mAdapter.getCurrentMeta();

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("ap fragemnt", "Loading APM, DB says meta ID" + mPercentageMetaId + ", Name " + currentMetaObject.getDisplayName());

        //LISTVIEW FOR uebung instances
        if (mAdapter.getItemCount() == 0)
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.e("Main Listview", "Received Empty allEntryCursor for group " + currentMetaObject.getDisplayName() + " with id " + mPercentageMetaId);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                //no swipey swipey for info view
                if (viewHolder instanceof AdmissionPercentageAdapter.InfoHolder)
                    return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //info view has no tag
                Object viewTag = viewHolder.itemView.getTag();
                if (viewTag != null) {
                    int lessonId = (Integer) viewTag;
                    if (lessonId != 0)
                        showUndoSnackBar(lessonId, mLessonList.getChildAdapterPosition(viewHolder.itemView));
                }
            }
        };

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(mLessonList);

        mLessonList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mLessonList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if (position != 0){
                    mLastClickedView = view;
                    MainDialogHelper.showChangeLessonDialog(getFragmentManager(), mPercentageMetaId, (Integer) view.getTag());
                }
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        //add listener to fab
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainDialogHelper.showAddLessonDialog(getFragmentManager(), mPercentageMetaId);
            }
        });

        mRootView = rootView;
        return rootView;
    }

    public void reloadInfo() {
        mAdapter.notifyItemChanged(0);
    }

    public void showUndoSnackBar(final int lessonId, int lessonPosition) {
        //remove the lesson. creates a savepoint before that, returns the id
        mLastRemovalSavePointId = mAdapter.removeLesson(new AdmissionPercentageData(mPercentageMetaId, lessonId, -1, -1));
        Snackbar
                .make(mRootView.findViewById(R.id.subject_fragment_coordinator_layout), getActivity().getString(R.string.undobar_deleted), Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onUndo();
                    }
                }).show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_PERCENTAGE_META_ID));
    }

    //dont do anything, as we only delete the lesson when the undo bar gets hidden
    private void onUndo() {
        if (mLastRemovalSavePointId != null) {
            DBAdmissionPercentageData.getInstance().rollbackToSavepoint(mLastRemovalSavePointId);
            mLastRemovalSavePointId = null;
            mAdapter.requery();
        }
    }

    /**
     * delete a lesson with programmatic swipe
     * @param mOldData the data the dialog was called with
     */
    public void deleteLesson(AdmissionPercentageData mOldData) {
        if(mLastClickedView == null)
            throw new AssertionError("mLastClickedView must not be null here, because it must only be called from the dialogfragment");
        SwipeDeletion.executeProgrammaticSwipeDeletion(getActivity(), this, mLastClickedView, mAdapter.getRecyclerViewPosition(mOldData));
        mLastClickedView = null;
    }
}