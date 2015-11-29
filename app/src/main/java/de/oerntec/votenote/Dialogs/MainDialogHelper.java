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

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.view.View;
import android.widget.NumberPicker;

import java.util.List;

import de.oerntec.votenote.AdmissionPercentageFragmentStuff.LessonDialogFragment;
import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

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
     * if only one counter exists, immediately display the change dialog for that one.
     * if more exist, display a list of all available counters
     */
    public static void showPresentationPointDialog(final MainActivity activity, int subjectId){
        final List<AdmissionCounter> list = DBAdmissionCounters.getInstance().getItemsForSubject(subjectId);
        if(list.size() == 1){
            showCounterDialog(activity, list.get(0).id);
        }
        else{
            String[] choices = new String[list.size()];
            for(int i = 0; i < list.size(); i++)
                choices[i] = list.get(i).counterName;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Admission Counter wählen")
                .setItems(choices, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        showCounterDialog(activity, list.get(which).id);
                    }
                }).show();
        }
    }

    /**
     * shows the dialog to change the presentation points
     */
    private static void showCounterDialog(final MainActivity activity, int counterId) {
        //db access
        final DBAdmissionCounters db = DBAdmissionCounters.getInstance();
        final AdmissionCounter item = db.getItem(counterId);

        final View inputView = activity.getLayoutInflater().inflate(R.layout.subject_fragment_dialog_presentation_points, null);
        final NumberPicker presPointPicker = (NumberPicker) inputView.findViewById(R.id.mainfragment_dialog_prespoints_picker);

        presPointPicker.setMinValue(0);
        presPointPicker.setMaxValue(item.targetValue);
        presPointPicker.setValue(item.currentValue);


        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setView(inputView)
                .setPositiveButton(activity.getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //write new prespoint count to db
                        item.currentValue = presPointPicker.getValue();
                        db.changeItem(item);
                        //reload fragment
                        activity.getCurrentAdmissionPercentageFragment().reloadInfo();
                    }
                }).setNegativeButton(activity.getString(R.string.dialog_button_abort), null);
        b.setTitle(activity.getString(R.string.main_dialog_pres_title));
        b.create().show();
    }

    public static void showAllInfoDialog(MainActivity activity, int metaId) {
        AdmissionPercentageMeta meta = DBAdmissionPercentageMeta.getInstance().getItem(metaId);
        meta.loadData(DBAdmissionPercentageData.getInstance(), MainActivity.getPreference("reverse_lesson_sort", true));

        String scheduledWork = activity.getString(R.string.maximum_possible_assignments) + " " + meta.getEstimatedNumberOfAssignments();
        String numberOfNeededAssignmentsString = activity.getString(R.string.needed_work_count) + " " + meta.getNumberOfNeededAssignments();
        String numberOfVotedAssignmentsString = activity.getString(R.string.voted_assignments) + " " + meta.getFinishedAssignments();
        String remainingNeededAssignmentsString = activity.getString(R.string.remaining_needed_assignments) + " " + meta.getRemainingNeededAssignments();
        String numberOfElapsedLessonsString = activity.getString(R.string.past_lessons) + " " + meta.getNumberOfElapsedLessons();
        String numberOfLessonsLeftString = activity.getString(R.string.future_lessons) + " " + meta.getNumberOfLessonsLeft();
        String neededAssignmentsPerUebungString = activity.getString(R.string.assignments_per_lesson_for_exam) + " " + meta.getNeededAssignmentsPerUebung();

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setPositiveButton(R.string.dialog_button_ok, null)
                .setTitle(meta.name + ":");
        b.setItems(new String[]{scheduledWork,
                numberOfNeededAssignmentsString,
                numberOfVotedAssignmentsString,
                remainingNeededAssignmentsString,
                numberOfElapsedLessonsString,
                numberOfLessonsLeftString,
                neededAssignmentsPerUebungString}, null).show();
    }
}
