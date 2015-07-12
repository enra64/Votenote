package de.oerntec.votenote.LessonFragmentStuff;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.CardListHelpers.SwipeableRecyclerViewTouchListener;
import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Lesson;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.MainDialogHelper;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

/**
 * A placeholder fragment containing a simple view.
 */
public class LessonFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private static Lesson mLessonToDelete = null;

    /**
     * DB Singleton instance
     */
    private static DBSubjects mSubjectDb = DBSubjects.getInstance();
    /**
     * DB Singleton instance
     */
    private static DBLessons mLessonDb = DBLessons.getInstance();

    /**
     * Save the default text color, because apparently there is no constant for that.
     */
    private SwipeableRecyclerViewTouchListener mSwipeListener;

    /**
     * Contains the database id for the displayed fragment/subject
     */
    private int mSubjectId;

    /**
     * Parent viewgroup
     */
    private View mRootView;

    /**
     * List containing the vote data
     */
    private RecyclerView mLessonList;

    /**
     * Returns a new instance of this fragment for the given section number.
     */

    public static LessonFragment newInstance(int sectionNumber) {
        Log.i("votenote placeholder", "newinstance");
        LessonFragment fragment = new LessonFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public void notifyOfChangedDataset(boolean add) {
        ((LessonAdapter) mLessonList.getAdapter()).onNotified(add);
        ((LessonAdapter) mLessonList.getAdapter()).getCursorAdapter().changeCursor(mLessonDb.getAllLessonsForSubject(mSubjectId));
        mLessonList.getAdapter().notifyDataSetChanged();
        Log.i("subfrag", "reloaded");
    }

    /* MAIN FRAGMENT BUILDING
     * Build the menu_main fragment containing uebung specific info, and the menu_main listview
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //translate from position in drawer to db group id
        mSubjectId = mSubjectDb.translatePositionToID(getArguments().getInt(ARG_SECTION_NUMBER));

        //find and inflate everything
        View rootView = inflater.inflate(R.layout.subject_fragment, container, false);
        mLessonList = (RecyclerView) rootView.findViewById(R.id.subject_fragment_recyclerview);

        //config the recyclerview
        mLessonList.setHasFixedSize(true);


        //give it a layout manager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mLessonList.setLayoutManager(manager);


        //if translatedSection is -1, no group has been added yet
        if (mSubjectId == DBSubjects.NO_GROUPS_EXIST) {
            Intent intent = new Intent(getActivity(), SubjectManagementActivity.class);
            intent.putExtra("firstGroup", true);
            getActivity().startActivityForResult(intent, MainActivity.ADD_FIRST_SUBJECT_REQUEST);
            return rootView;
        }

        /*
        DISPLAY GROUP INFO
         */
        String currentGroupName = mSubjectDb.getGroupName(mSubjectId);
        Log.i("Main activity", "Loading Group, DB says ID" + mSubjectId + ", Name " + currentGroupName);

        //LISTVIEW FOR uebung instances
        Cursor allEntryCursor = mLessonDb.getAllLessonsForSubject(mSubjectId);

        if (allEntryCursor.getCount() == 0)
            Log.e("Main Listview", "Received Empty allEntryCursor for group " + currentGroupName + " with id " + mSubjectId);

        mSwipeListener =
                new SwipeableRecyclerViewTouchListener(mLessonList,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                //nothing can swipe if there is already a lesson to be deleted
                                return position != 0;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int p : reverseSortedPositions) {
                                    showUndoSnackBar((Integer) recyclerView.getChildAt(p).getTag());
                                }
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView, reverseSortedPositions);
                            }
                        });

        //set adapter
        mLessonList.setAdapter(new LessonAdapter(getActivity(), allEntryCursor, mSubjectId));

        mLessonList.addOnItemTouchListener(mSwipeListener);

        mLessonList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mLessonList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                if (position != 0)
                    MainDialogHelper.showChangeLessonDialog((MainActivity) getActivity(), mSubjectId, (Integer) view.getTag());
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        //add listener to fab
        FloatingActionButton addFab = (FloatingActionButton) rootView.findViewById(R.id.subject_fragment_add_fab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainDialogHelper.showAddLessonDialog((MainActivity) getActivity(), mSubjectId);
            }
        });

        //add animator
        mLessonList.setItemAnimator(new SlideInLeftAnimator());

        mRootView = rootView;

        return rootView;
    }

    private void showUndoSnackBar(final int lessonId) {
        mLessonToDelete = mLessonDb.getLesson(mSubjectId, lessonId);
        mLessonDb.removeEntry(mSubjectId, lessonId);
        notifyOfChangedDataset(false);
        Snackbar
                .make(mRootView.findViewById(R.id.subject_fragment_coordinator_layout), "Gelöscht!", Snackbar.LENGTH_LONG)
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
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    //dont do anything, as we only delete the lesson when the undo bar gets hidden
    private void onUndo() {
        if (mLessonToDelete != null) {
            mLessonDb.insertLesson(mLessonToDelete);
            //this should be called in the same thread(context? i have no idea),
            //since onUndo is called by a button
            notifyOfChangedDataset(true);
        }
    }
}