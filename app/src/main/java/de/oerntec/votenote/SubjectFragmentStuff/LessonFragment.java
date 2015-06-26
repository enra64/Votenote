package de.oerntec.votenote.SubjectFragmentStuff;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cocosw.undobar.UndoBarController;

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

/**
 * A placeholder fragment containing a simple view.
 */
public class LessonFragment extends Fragment implements UndoBarController.AdvancedUndoListener {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private static Lesson lessonToDelete = null;

    /**
     * DB Singleton instance
     */
    private static DBSubjects groupDB = DBSubjects.getInstance();
    /**
     * DB Singleton instance
     */
    private static DBLessons entryDB = DBLessons.getInstance();

    /**
     * Save the default text color, because apparently there is no constant for that.
     */
    public SwipeableRecyclerViewTouchListener swipeListener;

    /**
     * Contains the database id for the displayed fragment/subject
     */
    private int subjectId;

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

    public void notifyOfChangedDataset() {
        //noinspection SynchronizeOnNonFinalField
        synchronized (mLessonList) {
            ((LessonAdapter) mLessonList.getAdapter()).getCursorAdapter().changeCursor(entryDB.getAllLessonsForSubject(subjectId));
            mLessonList.getAdapter().notifyDataSetChanged();
            mLessonList.notify();
        }
        Log.i("subfrag", "reloaded");
    }

    /* MAIN FRAGMENT BUILDING
     * Build the menu_main fragment containing uebung specific info, and the menu_main listview
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //translate from position in drawer to db group id
        subjectId = groupDB.translatePositionToID(getArguments().getInt(ARG_SECTION_NUMBER));

        //find and inflate everything
        View rootView = inflater.inflate(R.layout.mainfragment, container, false);
        mLessonList = (RecyclerView) rootView.findViewById(R.id.mainfragment_list_cardlist);

        //config the recyclerview
        mLessonList.setHasFixedSize(true);

        //give it a layoutmanage (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mLessonList.setLayoutManager(manager);

        //if translatedSection is -1, no group has been added yet
        if (subjectId == DBSubjects.NO_GROUPS_EXIST) {
            Intent intent = new Intent(getActivity(), SubjectManagementActivity.class);
            intent.putExtra("firstGroup", true);
            getActivity().startActivityForResult(intent, MainActivity.ADD_FIRST_SUBJECT_REQUEST);
            return rootView;
        }

        /*
        DISPLAY GROUP INFO
         */
        String currentGroupName = groupDB.getGroupName(subjectId);
        Log.i("Main activity", "Loading Group, DB says ID" + subjectId + ", Name " + currentGroupName);

        //LISTVIEW FOR uebung instances
        Cursor allEntryCursor = entryDB.getAllLessonsForSubject(subjectId);

        if (allEntryCursor.getCount() == 0)
            Log.e("Main Listview", "Received Empty allEntryCursor for group " + currentGroupName + " with id " + subjectId);

        swipeListener =
                new SwipeableRecyclerViewTouchListener(mLessonList,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                //nothing can swipe if there is already a lesson to be deleted
                                return position != 0 && lessonToDelete == null;
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
        mLessonList.setAdapter(new LessonAdapter(getActivity(), allEntryCursor, subjectId, swipeListener));

        mLessonList.addOnItemTouchListener(swipeListener);

        mLessonList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mLessonList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                MainDialogHelper.showChangeLessonDialog((MainActivity) getActivity(), subjectId, (Integer) view.getTag());
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        return rootView;
    }

    private void showUndoSnackBar(final int lessonId) {
        lessonToDelete = entryDB.getLesson(subjectId, lessonId);
        entryDB.removeEntry(subjectId, lessonId);
        notifyOfChangedDataset();
        new UndoBarController.UndoBar(getActivity())
                .message("gelöscht")
                .style(UndoBarController.UNDOSTYLE)
                .duration(1800)
                .listener(this)
                .show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    //dont do anything, as we only delete the lesson when the undo bar gets hidden
    @Override
    public void onUndo(Parcelable parcelable) {
        if (lessonToDelete != null) {
            entryDB.insertLesson(lessonToDelete);
            notifyOfChangedDataset();
        }
        lessonToDelete = null;
    }

    @Override
    public void onHide(Parcelable parcelable) {
        //re-enable lesson removal
        lessonToDelete = null;
    }

    @Override
    public void onClear() {

    }
}