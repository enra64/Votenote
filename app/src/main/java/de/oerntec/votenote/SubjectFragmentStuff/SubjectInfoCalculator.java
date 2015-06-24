package de.oerntec.votenote.SubjectFragmentStuff;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import de.oerntec.votenote.DBLessons;
import de.oerntec.votenote.DBSubjects;
import de.oerntec.votenote.R;

public class SubjectInfoCalculator {
    private static DBSubjects groupDB = DBSubjects.getInstance();
    private static DBLessons entryDB = DBLessons.getInstance();

    /**
     * Write the presentation point status to the textview
     */
    protected static void setCurrentPresentationPointStatus(Context context, TextView presentationPointsView, int subjectId) {
        int presentationPoints = groupDB.getPresPoints(subjectId);
        int minimumPresentationPoints = groupDB.getWantedPresPoints(subjectId);

        //set text informing of current presentation point level, handling plural by the way.
        presentationPointsView.setText(presentationPoints + " " + context.getString(R.string.main_dialog_lesson_von) + " " + minimumPresentationPoints + " " +
                (minimumPresentationPoints > 1 ? context.getString(R.string.presentation_plural) : context.getString(R.string.presentation_singular)));

        //make view invisible if no presentations are required
        if (minimumPresentationPoints == 0)
            presentationPointsView.setVisibility(View.GONE);
    }

    /**
     * Sets how many Assignments have to be voted for to achieve the set minimum voting.
     */
    protected static void setAverageNeededAssignments(Context context, TextView averageNeededVotesView, int subjectId) {
        float scheduledMaximumAssignments = groupDB.getScheduledWork(subjectId);
        float numberOfNeededAssignments = (scheduledMaximumAssignments * (float) groupDB.getMinVote(subjectId)) / (float) 100;
        float numberOfVotedAssignments = entryDB.getCompletedAssignmentCount(subjectId);
        float remainingNeededAssignments = numberOfNeededAssignments - numberOfVotedAssignments;
        int numberOfElapsedLessons = entryDB.getLessonCountForSubject(subjectId);
        float numberOfLessonsLeft = groupDB.getScheduledNumberOfLessons(subjectId) - numberOfElapsedLessons;
        float neededAssignmentsPerUebung = remainingNeededAssignments / numberOfLessonsLeft;

        if (numberOfLessonsLeft == 0)
            averageNeededVotesView.setText(context.getString(R.string.subject_fragment_reached_scheduled_lesson_count));
        else if (numberOfLessonsLeft < 0)
            averageNeededVotesView.setText(context.getString(R.string.subject_fragment_overshot_lesson_count));
        else
            averageNeededVotesView.setText(context.getString(R.string.subject_fragment_on_average) + " " +
                    String.format("%.2f", neededAssignmentsPerUebung) + " " + context.getString(R.string.subject_fragment_assignments_per_lesson));

        if (scheduledMaximumAssignments < 0)
            averageNeededVotesView.setText(context.getString(R.string.subject_fragment_error_detected));

        //set color
        if (neededAssignmentsPerUebung > groupDB.getScheduledAssignmentsPerLesson(subjectId))
            averageNeededVotesView.setTextColor(Color.argb(255, 204, 0, 0));//red
        else
            averageNeededVotesView.setTextColor(context.getResources().getColor(R.color.abc_primary_text_material_light));
    }

    /**
     * calculate the current average vote
     *
     * @param forSection the subject id
     * @return the average vote
     */
    private static float calculateAverageVotierung(int forSection) {
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
    protected static void setVoteAverage(Context context, TextView averageVoteView, int subjectId) {
        float average = calculateAverageVotierung(subjectId);

        //no votes have been given
        if (entryDB.getLessonCountForSubject(subjectId) == 0)
            averageVoteView.setText(context.getString(R.string.add_lesson_command));

        //get minvote for section
        int minVote = groupDB.getMinVote(subjectId);

        //write percentage and color coding to summaryview
        if (Float.isNaN(average))
            averageVoteView.setText(context.getString(R.string.add_lesson_command));
        else
            averageVoteView.setText(String.format("%.1f", average) + "%");

        //color text in
        averageVoteView.setTextColor(average >= minVote ? Color.argb(255, 153, 204, 0) : Color.argb(255, 204, 0, 0));//red
    }

}
