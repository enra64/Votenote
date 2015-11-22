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

import android.app.FragmentManager;

import de.oerntec.votenote.AdmissionPercentageFragmentStuff.LessonDialogFragment;
import de.oerntec.votenote.MainActivity;

public class MainDialogHelper {
    private static final int ADD_LESSON_CODE = -2;

    public static void showAddLessonDialog(FragmentManager fragmentManager, int apMetaId) {
        showChangeLessonDialog(fragmentManager, apMetaId, ADD_LESSON_CODE);
    }

    public static void showChangeLessonDialog(FragmentManager fragmentManager, int apMetaId, int lessonId) {
        LessonDialogFragment fragment = LessonDialogFragment.newInstance(apMetaId, lessonId);
        fragment.show(fragmentManager, "ap_create_dialog");
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
