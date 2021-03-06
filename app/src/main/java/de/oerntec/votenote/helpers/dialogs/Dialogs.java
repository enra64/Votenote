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
package de.oerntec.votenote.helpers.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import java.util.List;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.AdmissionCounter;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionCounters;
import de.oerntec.votenote.percentage_tracker_fragment.AddLessonDialogFragment;
import de.oerntec.votenote.subject_management.admission_counter_creation.AdmissionCounterDialogFragment;

public class Dialogs {
    private static final int ADD_LESSON_CODE = -2;

    public static void showAddLessonDialog(FragmentManager fragmentManager, int apMetaId) {
        showChangeLessonDialog(fragmentManager, apMetaId, ADD_LESSON_CODE);
    }

    public static void showChangeLessonDialog(FragmentManager fragmentManager, int apMetaId, int lessonId) {
        AddLessonDialogFragment fragment = AddLessonDialogFragment.newInstance(apMetaId, lessonId);
        fragment.show(fragmentManager, "ap_create_dialog");
    }


    /**
     * if only one counter exists, immediately display the change dialog for that one.
     * if more exist, display a list of all available counters
     */
    public static void showAdmissionCounterDialog(final MainActivity activity, int subjectId){
        final List<AdmissionCounter> list = DBAdmissionCounters.getInstance().getItemsForSubject(subjectId);
        if(list.size() == 1){
            showCounterDialog(activity, list.get(0).id);
        }
        else{
            String[] choices = new String[list.size()];
            for(int i = 0; i < list.size(); i++)
                choices[i] = list.get(i).name;
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

        //noinspection RedundantCast because we cant yet know the parent for the dialog
        final View inputView = activity.getLayoutInflater().inflate(R.layout.percentage_tracker_change_counter_dialog, (ViewGroup) null);
        final NumberPicker presPointPicker = (NumberPicker) inputView.findViewById(R.id.mainfragment_dialog_prespoints_picker);

        presPointPicker.setMinValue(0);
        int maxValue = item.targetValue < 8 ? 8 : item.targetValue + 3;
        presPointPicker.setMaxValue(maxValue);
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
        b.setTitle(activity.getString(R.string.admission_counter_change_dialog_title, item.name));
        b.create().show();
    }

    public static void showDeleteDialog(Context context, DialogInterface.OnClickListener onYesListener) {
        showDeleteDialog(context, onYesListener, context.getString(R.string.dialog_title_delete));
    }

    public static void showDeleteDialog(Context context,
                                        DialogInterface.OnClickListener onYesListener,
                                        String title) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(title);
        b.setNegativeButton(R.string.dialog_button_no, null);
        b.setPositiveButton(R.string.dialog_button_yes, onYesListener);
        b.create().show();
    }

    public static void showCounterDialog(android.app.FragmentManager fragmentManager, int subjectId, int counterId, boolean isNew) {
        AdmissionCounterDialogFragment fragment = AdmissionCounterDialogFragment.newInstance(subjectId, counterId, isNew);
        fragment.show(fragmentManager, "ac_create_dialog");
    }
}
