package de.oerntec.votenote.SubjectManagerStuff;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.R;

public class CounterDialog {
    public static void openCounterDialog(final AdmissionCounter inputCounter, Activity activity, final SubjectCreationDialogInterface callback){
        //create convenience isNew boolean
        final boolean isNew = inputCounter.id == SubjectCreationActivityFragment.ADD_ADMISSION_COUNTER_SIGNAL_ID;

        //set title
        String title;
        if(isNew)
            title = "New admission counter";
        else
            title = "Change admission counter";

        //inflate rootView
        final View view = activity.getLayoutInflater().inflate(R.layout.subject_manager_fragment_dialog_admission_counter, null);
        final SeekBar targetPointCount = (SeekBar) view.findViewById(R.id.subject_manager_fragment_dialog_admission_counter_target_seekbar);
        final EditText nameEdit = (EditText) view.findViewById(R.id.subject_manager_fragment_dialog_admission_counter_name_edittext);

        //add a watcher to set an error if the name is empty
        nameEdit.addTextChangedListener(new NotEmptyWatcher(nameEdit));

        //set old name
        if(!isNew)
            nameEdit.setText(inputCounter.counterName);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setView(view);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                inputCounter.counterName = nameEdit.getText().toString();
                inputCounter.targetValue = targetPointCount.getProgress();
                callback.admissionCounterFinished(inputCounter);
            }
        });
    }
}
