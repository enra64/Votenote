package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;

import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;

public class Dialogs {

    public static void showCounterDialog(FragmentManager fragmentManager, int subjectId, int counterId, boolean isNew) {
        AdmissionCounterFragment fragment = AdmissionCounterFragment.newInstance(subjectId, counterId, isNew);
        fragment.show(fragmentManager, "ac_create_dialog");
    }

    public static void showPercentageDialog(FragmentManager fragmentManager, int subjectId, int counterId, boolean isNew) {
        AdmissionPercentageFragment fragment = AdmissionPercentageFragment.newInstance(subjectId, counterId, isNew);
        fragment.show(fragmentManager, "ap_create_dialog");
    }

    public static void showCounterDeleteDialog(final AdmissionCounter inCounter, Activity activity, final SubjectCreationActivityFragment callback) {
        if (inCounter.id < 0)
            throw new AssertionError("negative deleted counter id??");
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Delete " + inCounter.counterName + "?");
        builder.setMessage("Please confirm removing the counter");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.deleteAdmissionCounter(inCounter);
            }
        });
        builder.setNegativeButton("Abort", null);
    }

    public static void showPercentageDeleteDialog(final AdmissionPercentageMeta inItem, Activity activity, final SubjectCreationActivityFragment callback) {
        if (inItem.id < 0)
            throw new AssertionError("negative deleted counter id??");
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Delete " + inItem.name + "?");
        builder.setMessage("Please confirm removing the counter");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.deleteAdmissionPercentage(inItem);
            }
        });
        builder.setNegativeButton("Abort", null);
    }
}
