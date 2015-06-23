package de.oerntec.votenote;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

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
    private static int textColor;

    /**
     * Contains the databaseID for each fragment
     */
    private int databaseID;

    /**
     * List containing the vote data
     */
    private RecyclerView voteList;

    /**
     * the views of a lesson
     */
    private TextView averageVoteView, presentationPointsView, averageNeededVotesView;

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
        ((LessonAdapter) voteList.getAdapter()).getCursorAdapter().changeCursor(entryDB.getAllLessonsForSubject(databaseID));
        voteList.getAdapter().notifyDataSetChanged();
        setAverageNeededAssignments();
        setVoteAverage();
        setCurrentPresentationPointStatus();
        Log.i("subfrag", "reloaded");
    }

    /* MAIN FRAGMENT BUILDING
     * Build the menu_main fragment containing uebung specific info, and the menu_main listview
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //translate from position in drawer to db group id
        databaseID = groupDB.translatePositionToID(getArguments().getInt(ARG_SECTION_NUMBER));

        //find and inflate everything
        View rootView = inflater.inflate(R.layout.mainfragment, container, false);
        voteList = (RecyclerView) rootView.findViewById(R.id.mainfragment_list_cardlist);

        //config the recyclerview
        voteList.setHasFixedSize(true);

        //give it a layoutmanage (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        voteList.setLayoutManager(manager);

        averageVoteView = (TextView) rootView.findViewById(R.id.mainfragment_text_averagevote);
        presentationPointsView = (TextView) rootView.findViewById(R.id.mainfragment_text_prespoints);
        averageNeededVotesView = (TextView) rootView.findViewById(R.id.mainfragment_text_avg_needed_votes);

        textColor = presentationPointsView.getCurrentTextColor();

        //if translatedSection is -1, no group has been added yet
        if (databaseID == DBSubjects.NO_GROUPS_EXIST) {
            Intent intent = new Intent(getActivity(), GroupManagementActivity.class);
            intent.putExtra("firstGroup", true);
            getActivity().startActivityForResult(intent, MainActivity.ADD_FIRST_SUBJECT_REQUEST);
            return rootView;
        }

        /*
        DISPLAY GROUP INFO
         */
        String currentGroupName = groupDB.getGroupName(databaseID);
        Log.i("Main activity", "Loading Group, DB says ID" + databaseID + ", Name " + currentGroupName);

        //apply presentation point status
        setCurrentPresentationPointStatus();

        //average needed votation
        setAverageNeededAssignments();

        //LISTVIEW FOR uebung instances
        Cursor allEntryCursor = entryDB.getAllLessonsForSubject(databaseID);

        if (allEntryCursor.getCount() == 0)
            Log.e("Main Listview", "Received Empty allEntryCursor for group " + currentGroupName + " with id " + databaseID);

        //set adapter
        voteList.setAdapter(new LessonAdapter(getActivity(), allEntryCursor));

        final SubjectFragment thisRef = this;

        voteList.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), voteList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                MainDialogHelper.showChangeLessonDialog((MainActivity) getActivity(), databaseID, (Integer) view.getTag());
            }

            public void onItemLongClick(final View view, int position) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.subject_fragment_delete_lesson) + " (" + view.getTag() + ")")
                        .setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //change db entry
                                entryDB.removeEntry(databaseID, (Integer) view.getTag());
                                //reload list- and textview; close old cursor
                                thisRef.notifyOfChangedDataset();
                            }
                        }).setNegativeButton(R.string.dialog_button_abort, null).show();
            }
        }));

        //set summaryView to average
        setVoteAverage();
        return rootView;
    }

    /**
     * Write the presentation point status to the textview
     */
    private void setCurrentPresentationPointStatus() {
        int presentationPoints = groupDB.getPresPoints(databaseID);
        int minimumPresentationPoints = groupDB.getWantedPresPoints(databaseID);

        //set text informing of current presentation point level, handling plural by the way.
        presentationPointsView.setText(presentationPoints + " " + getString(R.string.main_dialog_lesson_von) + " " + minimumPresentationPoints + " " +
                (minimumPresentationPoints > 1 ? getString(R.string.presentation_plural) : getString(R.string.presentation_singular)));

        //make view invisible if no presentations are required
        if (minimumPresentationPoints == 0)
            presentationPointsView.setVisibility(View.GONE);
    }

    /**
     * Sets how many Assignments have to be voted for to achieve the set minimum voting.
     */
    private void setAverageNeededAssignments() {
        float scheduledMaximumAssignments = groupDB.getScheduledWork(databaseID);
        float numberOfNeededAssignments = (scheduledMaximumAssignments * (float) groupDB.getMinVote(databaseID)) / (float) 100;
        float numberOfVotedAssignments = entryDB.getCompletedAssignmentCount(databaseID);
        float remainingNeededAssignments = numberOfNeededAssignments - numberOfVotedAssignments;
        int numberOfElapsedLessons = entryDB.getLessonCountForSubject(databaseID);
        float numberOfLessonsLeft = groupDB.getScheduledNumberOfLessons(databaseID) - numberOfElapsedLessons;
        float neededAssignmentsPerUebung = remainingNeededAssignments / numberOfLessonsLeft;

        if (numberOfLessonsLeft == 0)
            averageNeededVotesView.setText(getString(R.string.subject_fragment_reached_scheduled_lesson_count));
        else if (numberOfLessonsLeft < 0)
            averageNeededVotesView.setText(getString(R.string.subject_fragment_overshot_lesson_count));
        else
            averageNeededVotesView.setText(getString(R.string.subject_fragment_on_average) + " " +
                    String.format("%.2f", neededAssignmentsPerUebung) + " " + getString(R.string.subject_fragment_assignments_per_lesson));

        if (scheduledMaximumAssignments < 0)
            averageVoteView.setText(getString(R.string.subject_fragment_error_detected));

        //set color
        if (neededAssignmentsPerUebung > groupDB.getScheduledAssignmentsPerLesson(databaseID))
            averageNeededVotesView.setTextColor(Color.argb(255, 204, 0, 0));//red
        else
            averageNeededVotesView.setTextColor(textColor);
    }

    /**
     * calculate the current average vote
     *
     * @param forSection the subject id
     * @return the average vote
     */
    private float calculateAverageVotierung(int forSection) {
        //get avg cursor
        Cursor avgCursor = entryDB.getAllLessonsForSubject(forSection);
        int maxVoteCount = 0, myVoteCount = 0;
        for (int i = 0; i < avgCursor.getCount(); i++) {
            myVoteCount += avgCursor.getInt(1);
            maxVoteCount += avgCursor.getInt(2);
            avgCursor.moveToNext();
        }
        //close the cursor; it did its job
        avgCursor.close();
        float myVoteFactor = (float) myVoteCount / (float) maxVoteCount;
        //calc percentage
        return myVoteFactor * 100.f;
    }

    /**
     * Set the current average vote
     */
    private void setVoteAverage() {
        float average = calculateAverageVotierung(databaseID);

        //no votes have been given
        if (entryDB.getLessonCountForSubject(databaseID) == 0)
            averageVoteView.setText(getString(R.string.add_lesson_command));

        //get minvote for section
        int minVote = groupDB.getMinVote(databaseID);

        //write percentage and color coding to summaryview
        if (Float.isNaN(average))
            averageVoteView.setText(getString(R.string.add_lesson_command));
        else
            averageVoteView.setText(String.format("%.1f", average) + "%");

        //color text in
        averageVoteView.setTextColor(average >= minVote ? Color.argb(255, 153, 204, 0) : Color.argb(255, 204, 0, 0));//red
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    //see http://stackoverflow.com/questions/24471109/recyclerview-onclick
    public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
        private OnItemClickListener mListener;
        private GestureDetector mGestureDetector;

        public RecyclerItemClickListener(Context context, final RecyclerView recyclerView, OnItemClickListener listener) {
            mListener = listener;

            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (childView != null && mListener != null)
                        mListener.onItemLongClick(childView, recyclerView.getChildPosition(childView));
                }
            });
        }


        @Override
        public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
            View childView = view.findChildViewUnder(e.getX(), e.getY());
            if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e))
                mListener.onItemClick(childView, view.getChildPosition(childView));
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
        }
    }

    public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonHolder> {
        //see stackoverflow: http://stackoverflow.com/questions/26517855/using-the-recyclerview-with-a-database
        // PATCH: Because RecyclerView.Adapter in its current form doesn't natively support
        // cursors, we "wrap" a CursorAdapter that will do all teh job
        // for us
        private CursorAdapter mCursorAdapter;

        private Context mContext;

        public LessonAdapter(Context context, Cursor c) {

            mContext = context;

            //basically the old lesson adapter
            mCursorAdapter = new CursorAdapter(mContext, c, 0) {
                @Override
                public View newView(Context context, Cursor cursor, ViewGroup parent) {
                    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                    return inflater.inflate(R.layout.subject_fragment_card_thingy, parent, false);
                }

                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    //find textviews
                    TextView upper = (TextView) view.findViewById(R.id.subject_fragment_card_upper);
                    TextView lower = (TextView) view.findViewById(R.id.subject_fragment_card_lower);

                    //load strings
                    String lessonIndex = cursor.getString(0);
                    String myVote = cursor.getString(1);
                    String maxVote = cursor.getString(2);
                    String voteString = " " + (Integer.valueOf(myVote) < 2 ? getString(R.string.subject_fragment_singular_vote)
                            : getString(R.string.main_dialog_lesson_votes));

                    //set tag for later identification avoiding all
                    view.setTag(Integer.valueOf(lessonIndex));

                    //set texts
                    upper.setText(myVote + " " + context.getString(R.string.main_dialog_lesson_von) + " " + maxVote + voteString);
                    lower.setText(lessonIndex + context.getString(R.string.main_x_th_lesson));
                }
            };
        }

        public CursorAdapter getCursorAdapter() {
            return mCursorAdapter;
        }

        @Override
        public int getItemCount() {
            return mCursorAdapter.getCount();
        }

        @Override
        public void onBindViewHolder(LessonHolder holder, int position) {
            //move to the correct position
            mCursorAdapter.getCursor().moveToPosition(position);
            // Passing the binding operation to cursor loader
            mCursorAdapter.bindView(holder.itemView, mContext, mCursorAdapter.getCursor());
        }

        @Override
        public LessonHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Passing the inflater job to the cursor-adapter
            View v = mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent);
            return new LessonHolder(v);
        }

        class LessonHolder extends RecyclerView.ViewHolder {
            //View v1;

            public LessonHolder(View itemView) {
                super(itemView);
                //v1 = itemView.findViewById(R.id.v1);
            }
        }

    }
}