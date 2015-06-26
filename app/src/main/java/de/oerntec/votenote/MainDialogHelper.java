package de.oerntec.votenote;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Lesson;

public class MainDialogHelper {
    private static final int ADD_LESSON_CODE = -2;

    /**
     * Show an add lesson dialog
     *
     * @param mActivity Activity to use for references
     * @param groupID   id of the group to add a lesson to
     */
    public static void showAddLessonDialog(final MainActivity mActivity, final int groupID) {
        showLessonDialog(mActivity, groupID, ADD_LESSON_CODE);
    }

    /**
     * Show a dialog to change a lesson entry
     *
     * @param mActivity                the activity for reference usage
     * @param groupID                  the id of the subject
     * @param translatedLessonPosition the id of the lesson, translated (+1) from listview
     */
    public static void showChangeLessonDialog(final MainActivity mActivity, final int groupID, final int translatedLessonPosition) {
        showLessonDialog(mActivity, groupID, translatedLessonPosition);
    }

    /**
     * Shows a dialog to edit or add a lesson
     */
    private static void showLessonDialog(final MainActivity mActivity, final int groupID, final int lessonID) {
        //db access
        final DBLessons entryDB = DBLessons.getInstance();

        int maxVoteValue;
        int myVoteValue;

        //set values according to usage (add or change)
        if (lessonID == ADD_LESSON_CODE) {
            maxVoteValue = entryDB.getPreviousMaximumVote(groupID);
            myVoteValue = entryDB.getPreviousMyVote(groupID);
        } else {
            Lesson oldValues = entryDB.getLesson(groupID, lessonID);
            myVoteValue = oldValues != null ? oldValues.myVotes : 0;
            maxVoteValue = oldValues != null ? oldValues.maxVotes : 0;
        }

        //inflate rootView
        final View rootView = mActivity.getLayoutInflater().inflate(R.layout.mainfragment_dialog_newentry, null);

        //find all necessary views
        final TextView infoView = (TextView) rootView.findViewById(R.id.infoTextView);
        final NumberPicker myVote = (NumberPicker) rootView.findViewById(R.id.pickerMyVote);
        final NumberPicker maxVote = (NumberPicker) rootView.findViewById(R.id.pickerMaxVote);

        //config for the maximum vote picker
        maxVote.setMinValue(0);
        maxVote.setMaxValue(15);
        maxVote.setValue(maxVoteValue);

        //config for the actual vote picker
        myVote.setMinValue(0);
        myVote.setMaxValue(15);
        myVote.setValue(myVoteValue);

        //set the current values of the pickers as explanation text
        infoView.setText(myVote.getValue() + " " + mActivity.getString(R.string.main_dialog_lesson_von)
                + " " + maxVote.getValue() + " " + mActivity.getString(R.string.main_dialog_lesson_votes));
        infoView.setTextColor(Color.argb(255, 153, 204, 0));//green

        //build alertdialog
        final AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle(lessonID == ADD_LESSON_CODE ? mActivity.getString(R.string.main_dialog_lesson_new_lesson_title) : "Übung " + lessonID + " " + "ändern?")
                .setView(rootView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (myVote.getValue() <= maxVote.getValue()) {
                            //decide whether to add an entry or change an existing one
                            if (lessonID == ADD_LESSON_CODE)
                                entryDB.addLesson(groupID, maxVote.getValue(), myVote.getValue());
                            else
                                entryDB.changeLesson(groupID, lessonID, maxVote.getValue(), myVote.getValue());
                            //reload current fragment
                            mActivity.notifyCurrentFragment();
                        } else
                            Toast.makeText(mActivity, mActivity.getString(R.string.main_dialog_lesson_voted_too_much), Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(mActivity.getString(R.string.dialog_button_abort), null).create();

        //listener for the vote picker
        NumberPicker.OnValueChangeListener votePickerListener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                //load new values
                int myVoteValue = myVote.getValue();
                int maxVoteValue = maxVote.getValue();
                infoView.setText(myVoteValue + " " + mActivity.getString(R.string.main_dialog_lesson_von)
                        + " " + maxVoteValue + " " + mActivity.getString(R.string.main_dialog_lesson_votes));
                boolean isValid = myVoteValue <= maxVoteValue;
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isValid);
                infoView.setTextColor(isValid ? Color.argb(255, 153, 204, 0) /*green*/ : Color.argb(255, 204, 0, 0));//red
            }
        };

        //add change listener to update dialog explanation if pickers changed
        myVote.setOnValueChangedListener(votePickerListener);
        maxVote.setOnValueChangedListener(votePickerListener);

        dialog.show();
    }

    /**
     * shows the dialog to change the presentation points
     *
     * @param dataBaseId ID of the group in the database
     * @param activity   reference for various stuff
     */
    public static void showPresentationPointDialog(final int dataBaseId, final MainActivity activity) {
        //db access
        final DBSubjects groupsDB = DBSubjects.getInstance();

        final View inputView = activity.getLayoutInflater().inflate(R.layout.mainfragment_dialog_prespoints, null);
        final NumberPicker presPointPicker = (NumberPicker) inputView.findViewById(R.id.mainfragment_dialog_prespoints_picker);

        presPointPicker.setMinValue(0);
        presPointPicker.setMaxValue(groupsDB.getWantedPresPoints(dataBaseId));
        presPointPicker.setValue(groupsDB.getPresPoints(dataBaseId));

		/*
         * PRESPOINT ALERTDIALOG
		 */
        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setView(inputView)
                .setPositiveButton(activity.getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //write new prespoint count to db
                        groupsDB.setPresPoints(dataBaseId, presPointPicker.getValue());
                        //reload fragment
                        activity.notifyCurrentFragment();
                    }
                }).setNegativeButton(activity.getString(R.string.dialog_button_abort), null);
        b.setTitle(activity.getString(R.string.main_dialog_pres_title));
        b.create().show();
    }

    public static void showAllInfoDialog(MainActivity activity, int lessonId) {
        DBLessons lessonDb = DBLessons.getInstance();
        DBSubjects subjectDb = DBSubjects.getInstance();

        int scheduledMaximumAssignments = subjectDb.getScheduledWork(lessonId);
        float numberOfNeededAssignments = ((float) scheduledMaximumAssignments * (float) subjectDb.getMinVote(lessonId)) / (float) 100;
        int numberOfVotedAssignments = lessonDb.getCompletedAssignmentCount(lessonId);
        float remainingNeededAssignments = numberOfNeededAssignments - numberOfVotedAssignments;
        int numberOfElapsedLessons = lessonDb.getLessonCountForSubject(lessonId);
        int numberOfLessonsLeft = subjectDb.getScheduledNumberOfLessons(lessonId) - numberOfElapsedLessons;
        float neededAssignmentsPerUebung = remainingNeededAssignments / (float) numberOfLessonsLeft;

        String scheduledWork = activity.getString(R.string.maximum_possible_assignments) + " " + scheduledMaximumAssignments;
        String numberOfNeededAssignmentsString = activity.getString(R.string.needed_work_count) + " " + numberOfNeededAssignments;
        String numberOfVotedAssignmentsString = activity.getString(R.string.voted_assignments) + " " + numberOfVotedAssignments;
        String remainingNeededAssignmentsString = activity.getString(R.string.remaining_needed_assignments) + " " + remainingNeededAssignments;
        String numberOfElapsedLessonsString = activity.getString(R.string.past_lessons) + " " + numberOfElapsedLessons;
        String numberOfLessonsLeftString = activity.getString(R.string.future_lessons) + " " + numberOfLessonsLeft;
        String neededAssignmentsPerUebungString = activity.getString(R.string.assignments_per_lesson_for_exam) + " " + neededAssignmentsPerUebung;

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setPositiveButton(R.string.dialog_button_ok, null)
                .setTitle(R.string.calculated_values);
        b.setItems(new String[]{scheduledWork,
                numberOfNeededAssignmentsString,
                numberOfVotedAssignmentsString,
                remainingNeededAssignmentsString,
                numberOfElapsedLessonsString,
                numberOfLessonsLeftString,
                neededAssignmentsPerUebungString}, null).show();
    }
}
