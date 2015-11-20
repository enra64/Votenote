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
package de.oerntec.votenote.Dialogs;

import android.view.View;

import de.oerntec.votenote.AdmissionPercentageFragmentStuff.AdmissionPercentageFragment;
import de.oerntec.votenote.MainActivity;

public class MainDialogHelper {
    private static final int ADD_LESSON_CODE = -2;

    /**
     * Show an add lesson dialog
     *
     * @param mActivity Activity to use for references
     * @param apMetaId   id of the group to add a lesson to
     */
    public static void showAddLessonDialog(final MainActivity mActivity, final int apMetaId) {
        showLessonDialog(mActivity, null, null, apMetaId, ADD_LESSON_CODE, -1);
    }

    /**
     * Show a dialog to change a lesson entry
     *
     * @param mActivity                the activity for reference usage
     * @param apMetaId                  the id of the subject
     * @param translatedLessonPosition the id of the lesson, translated (+1) from listview
     */
    public static void showChangeLessonDialog(MainActivity mActivity, AdmissionPercentageFragment admissionPercentageFragment, View lessonView, int apMetaId, int translatedLessonPosition, int recyclerviewLessonPosition) {
        showLessonDialog(mActivity, admissionPercentageFragment, lessonView, apMetaId, translatedLessonPosition, recyclerviewLessonPosition);
    }


    /**
     * Shows a dialog to edit or add a lesson
     */
    private static void showLessonDialog(final MainActivity mActivity, final AdmissionPercentageFragment admissionPercentageFragment, final View lessonView, final int apMetaId, final int lessonID, final int lessonPosition) {
        /*final int maxVoteValue;
        final int myVoteValue;

        final DBAdmissionPercentageData apDataDb = DBAdmissionPercentageData.getInstance();

        //whether or not to try automatically fixing invalid choices
        boolean autoNumberPickerFixEnabled = MainActivity.getPreference("move_max_assignments_picker", true);

        Lesson oldValues = null;
        final boolean isNewLesson = lessonID == ADD_LESSON_CODE;
        //set values according to usage (add or change)
        if (isNewLesson) {
            AdmissionPercentageData lastEnteredLesson = apDataDb.getNewestItemForMetaId(apMetaId);
            maxVoteValue = lastEnteredLesson.availableAssignments;
            myVoteValue = lastEnteredLesson.finishedAssignments;
        } else {
            oldValues = entryDB.getLesson(subjectId, lessonID);
            myVoteValue = oldValues != null ? oldValues.myVotes : 0;
            maxVoteValue = oldValues != null ? oldValues.maxVotes : 0;
        }

        //inflate rootView
        final View rootView = mActivity.getLayoutInflater().inflate(R.layout.subject_fragment_dialog_newentry, null);

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
        final Lesson finalOldValues = oldValues;

        String changeTitle;

        if (mActivity.getResources().getConfiguration().locale.getLanguage().equals("de"))
            changeTitle = "Übung " + lessonID + " ändern?";
        else
            changeTitle = "Change lesson " + lessonID + "?";


        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle(isNewLesson ? mActivity.getString(R.string.main_dialog_lesson_new_lesson_title) : changeTitle)
                .setView(rootView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (myVote.getValue() <= maxVote.getValue()) {
                            Lesson val = new Lesson(finalOldValues == null ? -1 : finalOldValues.lessonId, myVote.getValue(), maxVote.getValue(), -1, subjectId);
                            if (lessonID == ADD_LESSON_CODE)
                                mActivity.getCurrentFragment().mAdapter.addLesson(val);
                            else
                                mActivity.getCurrentFragment().mAdapter.changeLesson(finalOldValues, val);
                        } else
                            Toast.makeText(mActivity, mActivity.getString(R.string.main_dialog_lesson_voted_too_much), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(mActivity.getString(R.string.dialog_button_abort), null);

        //enable deleting old lessons
        if (!isNewLesson) {
            builder.setNeutralButton(R.string.delete_button_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SwipeDeletion.executeProgrammaticSwipeDeletion(mActivity, admissionPercentageFragment, lessonView, lessonPosition);
                }
            });
        }

        //create dialog from builder
        final AlertDialog dialog = builder.create();

        //try to fix invalid number of done assignments
        if (autoNumberPickerFixEnabled) {
            myVote.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                    //load new values
                    int myVoteValue = myVote.getValue(), maxVoteValue = maxVote.getValue();
                    if (myVoteValue > maxVoteValue)
                        maxVote.setValue(myVoteValue);
                    //update info text
                    infoView.setText(myVote.getValue() + " " + mActivity.getString(R.string.main_dialog_lesson_von)
                            + " " + maxVote.getValue() + " " + mActivity.getString(R.string.main_dialog_lesson_votes));
                }
            });
            maxVote.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                    //load new values
                    int myVoteValue = myVote.getValue(), maxVoteValue = maxVote.getValue();
                    if (myVoteValue > maxVoteValue)
                        myVote.setValue(maxVoteValue);
                    //update info text
                    infoView.setText(myVote.getValue() + " " + mActivity.getString(R.string.main_dialog_lesson_von)
                            + " " + maxVote.getValue() + " " + mActivity.getString(R.string.main_dialog_lesson_votes));
                }
            });
        }
        //fallback behaviour if the setting is disabled
        else {
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
                    infoView.setTextColor(isValid ?
                            Color.argb(255, 153, 204, 0) :
                            mActivity.getResources().getColor(R.color.warning_red));
                }
            };
            //add change listener to update dialog explanation if pickers changed
            maxVote.setOnValueChangedListener(votePickerListener);
            myVote.setOnValueChangedListener(votePickerListener);
        }

        dialog.show();

        //change delete button text color to red
        if (!isNewLesson)
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(mActivity.getResources().getColor(R.color.warning_red));
                    */
    }


    /**
     * shows the dialog to change the presentation points
     *
     * @param subjectId ID of the group in the database
     * @param activity   reference for various stuff
     */
    public static void showPresentationPointDialog(final int subjectId, final MainActivity activity) {/*
        //db access
        final DBGroups groupsDB = DBGroups.getInstance();

        final View inputView = activity.getLayoutInflater().inflate(R.layout.subject_fragment_dialog_presentation_points, null);
        final NumberPicker presPointPicker = (NumberPicker) inputView.findViewById(R.id.mainfragment_dialog_prespoints_picker);

        presPointPicker.setMinValue(0);
        presPointPicker.setMaxValue(groupsDB.getWantedPresPoints(subjectId));
        presPointPicker.setValue(groupsDB.getPresPoints(subjectId));

		/*
         * PRESPOINT ALERTDIALOG
		 /
        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setView(inputView)
                .setPositiveButton(activity.getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //write new prespoint count to db
                        groupsDB.setPresPoints(subjectId, presPointPicker.getValue());
                        //reload fragment
                        activity.getCurrentFragment().reloadInfo();
                    }
                }).setNegativeButton(activity.getString(R.string.dialog_button_abort), null);
        b.setTitle(activity.getString(R.string.main_dialog_pres_title));
        b.create().show();*/
    }

    public static void showAllInfoDialog(MainActivity activity, int lessonId) {/*
        DBLessons lessonDb = DBLessons.getInstance();
        DBGroups subjectDb = DBGroups.getInstance();

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
                neededAssignmentsPerUebungString}, null).show();*/
    }
}
