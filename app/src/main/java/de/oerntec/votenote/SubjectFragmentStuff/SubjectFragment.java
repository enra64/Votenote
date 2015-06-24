package de.oerntec.votenote.SubjectFragmentStuff;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.oerntec.votenote.DBLessons;
import de.oerntec.votenote.DBSubjects;
import de.oerntec.votenote.GroupManagementActivity;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.MainDialogHelper;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SwipeableRecyclerViewTouchListener;

/**
 * A placeholder fragment containing a simple view.
 */
public class SubjectFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

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
    private RecyclerView voteList;

    /**
     * Returns a new instance of this fragment for the given section number.
     */

    public static SubjectFragment newInstance(int sectionNumber) {
        Log.i("votenote placeholder", "newinstance");
        SubjectFragment fragment = new SubjectFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public void notifyOfChangedDataset() {
        ((LessonAdapter) voteList.getAdapter()).getCursorAdapter().changeCursor(entryDB.getAllLessonsForSubject(subjectId));
        ((LessonAdapter) voteList.getAdapter()).refreshInfoView();
        voteList.getAdapter().notifyDataSetChanged();
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
        voteList = (RecyclerView) rootView.findViewById(R.id.mainfragment_list_cardlist);

        //config the recyclerview
        voteList.setHasFixedSize(true);

        //give it a layoutmanage (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        voteList.setLayoutManager(manager);

        //if translatedSection is -1, no group has been added yet
        if (subjectId == DBSubjects.NO_GROUPS_EXIST) {
            Intent intent = new Intent(getActivity(), GroupManagementActivity.class);
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
                new SwipeableRecyclerViewTouchListener(voteList,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return position != 0;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int p : reverseSortedPositions) {
                                    entryDB.removeEntry(subjectId, (Integer) recyclerView.getChildAt(p).getTag());
                                    //reload list- and textview; close old cursor
                                    SubjectFragment.this.notifyOfChangedDataset();
                                }
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView, reverseSortedPositions);
                            }
                        });

        //set adapter
        voteList.setAdapter(new LessonAdapter(getActivity(), allEntryCursor, subjectId, swipeListener));

        voteList.addOnItemTouchListener(swipeListener);

        voteList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), voteList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                MainDialogHelper.showChangeLessonDialog((MainActivity) getActivity(), subjectId, (Integer) view.getTag());
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }
}