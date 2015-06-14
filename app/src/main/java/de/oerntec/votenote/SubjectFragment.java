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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
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
    private static DBGroups groupDB = DBGroups.getInstance();
    /**
     * DB Singleton instance
     */
    private static DBEntries entryDB = DBEntries.getInstance();

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
    private ListView voteList;

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
        ((LessonAdapter) voteList.getAdapter()).changeCursor(entryDB.getGroupRecords(databaseID));
        setAverageNeededAssignments();
        setVoteAverage();
        setCurrentPresentationPointStatus();
        Log.i("subfrag", "reloaded");
    }

    /* MAIN FRAGMENT BUILDING
     * Build the main fragment containing uebung specific info, and the main listview
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //translate from position in drawer to db group id
        databaseID = groupDB.translatePositionToID(getArguments().getInt(ARG_SECTION_NUMBER));

        //find and inflate everything
        View rootView = inflater.inflate(R.layout.mainfragment, container, false);
        voteList = (ListView) rootView.findViewById(R.id.mainfragment_list_votelist);

        averageVoteView = (TextView) rootView.findViewById(R.id.mainfragment_text_averagevote);
        presentationPointsView = (TextView) rootView.findViewById(R.id.mainfragment_text_prespoints);
        averageNeededVotesView = (TextView) rootView.findViewById(R.id.mainfragment_text_avg_needed_votes);

        textColor = presentationPointsView.getCurrentTextColor();

        //if translatedSection is -1, no group has been added yet
        if (databaseID == DBGroups.NO_GROUPS_EXIST) {
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
        Cursor allEntryCursor = entryDB.getGroupRecords(databaseID);

        if (allEntryCursor.getCount() == 0)
            Log.e("Main Listview", "Received Empty allEntryCursor for group " + currentGroupName + " with id " + databaseID);

        //set custom adapter
        voteList.setAdapter(new LessonAdapter(getActivity(), allEntryCursor, 0));

        final SubjectFragment thisRef = this;

        //Listener for list click, change entry dialog
        voteList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
                MainDialogHelper.showChangeLessonDialog((MainActivity) getActivity(), databaseID, position + 1);
            }
        });

        //listener for long list click
        voteList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int translatedPosition = position + 1;

                new AlertDialog.Builder(getActivity())
                        .setTitle("Eintrag löschen?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //change db entry
                                entryDB.removeEntry(databaseID, translatedPosition);
                                //reload list- and textview; close old cursor
                                thisRef.notifyOfChangedDataset();
                            }
                        }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
                return true;
            }
        });

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
        presentationPointsView.setText(presentationPoints + " von " + minimumPresentationPoints + (minimumPresentationPoints > 1 ? " Vorträgen" : " Vortrag"));

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
        int numberOfElapsedLessons = entryDB.getGroupRecordCount(databaseID);
        float numberOfLessonsLeft = groupDB.getScheduledNumberOfLessons(databaseID) - numberOfElapsedLessons;
        float neededAssignmentsPerUebung = remainingNeededAssignments / numberOfLessonsLeft;

        if (numberOfLessonsLeft == 0)
            averageNeededVotesView.setText("Angegebene Übungsanzahl erreicht.");
        else if (numberOfLessonsLeft < 0)
            averageNeededVotesView.setText("Mehr Übungen eingetragen als eingestellt!");
        else
            averageNeededVotesView.setText("Noch durchschnittlich " + String.format("%.2f", neededAssignmentsPerUebung) + " Aufgaben pro Übung");

        if (scheduledMaximumAssignments < 0)
            averageVoteView.setText("Fehler erkannt. Werte unbrauchbar.");

        //set color
        if (neededAssignmentsPerUebung > groupDB.getScheduledAssignmentsPerLesson(databaseID))
            averageNeededVotesView.setTextColor(Color.argb(255, 204, 0, 0));//red
        else
            averageNeededVotesView.setTextColor(textColor);
    }

    /**
     * calculate the current average vote
     * @param forSection the subject id
     * @return the average vote
     */
    private float calculateAverageVotierung(int forSection) {
        //get avg cursor
        Cursor avgCursor = entryDB.getGroupRecords(forSection);
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
        if (entryDB.getGroupRecordCount(databaseID) == 0)
            averageVoteView.setText("Füge einen Eintrag ein.");

        //get minvote for section
        int minVote = groupDB.getMinVote(databaseID);

        //write percentage and color coding to summaryview
        if (Float.isNaN(average))
            averageVoteView.setText("Add Entry!");
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

    private class LessonAdapter extends CursorAdapter {
        private float defaultTextSize;
        private boolean savedSizeFlag = false;

        public LessonAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            //find textviews
            TextView upper = (TextView) view.findViewById(android.R.id.text1);
            TextView lower = (TextView) view.findViewById(android.R.id.text2);

            //load strings
            String lessonIndex = cursor.getString(0);
            String myVote = cursor.getString(1);
            String maxVote = cursor.getString(2);
            String voteString = Integer.valueOf(myVote) < 2 ? " Votierung" : " Votierungen";

            //set texts
            upper.setText(myVote + " von " + maxVote + voteString);
            lower.setText(lessonIndex + ". Übung");

            if (!savedSizeFlag) {
                defaultTextSize = upper.getTextSize();
                savedSizeFlag = true;
            }

            upper.setTextSize(defaultTextSize + 0.1f);
        }
    }
}