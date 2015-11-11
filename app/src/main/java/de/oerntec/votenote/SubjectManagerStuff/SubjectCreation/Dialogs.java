package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Helpers.NotEmptyWatcher;
import de.oerntec.votenote.R;

public class Dialogs {
    public static void counterChangeDialog(final AdmissionCounter inputCounter, Activity activity, final SubjectCreationDialogInterface callback){
        //create convenience isNew boolean
        final boolean isNew = inputCounter.id == SubjectCreationActivityFragment.ADD_ADMISSION_COUNTER_SIGNAL_ID;

        //set title
        String title;
        if(isNew)
            title = "New admission counter";
        else
            title = "Change " + inputCounter.counterName;

        //inflate rootView
        final View view = activity.getLayoutInflater().inflate(R.layout.subject_manager_fragment_dialog_admission_counter, null);
        final SeekBar targetPointCount = (SeekBar) view.findViewById(R.id.subject_manager_fragment_dialog_admission_counter_target_seekbar);
        final EditText nameEdit = (EditText) view.findViewById(R.id.subject_manager_fragment_dialog_admission_counter_name_edittext);

        //set valid value on seekbar
        targetPointCount.setProgress(inputCounter.targetValue);

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
                callback.admissionCounterFinished(inputCounter, isNew);
            }
        });
    }

    public static void counterDeleteDialog(final AdmissionCounter inCounter, Activity activity, final SubjectCreationActivityFragment callback) {
        if(inCounter.id < 0)
            throw new AssertionError("negative deleted counter id??");
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Delete " + inCounter.counterName + "?");
        builder.setMessage("Please confirm removing the counter");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.admissionCounterDelete(inCounter);
            }
        });builder.setPositiveButton("Abort", null);
    }
}
