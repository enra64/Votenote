package de.oerntec.votenote.Preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.R;

public class DeletionConfirmationPreference extends DialogPreference {
    public DeletionConfirmationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(R.string.deletion_confirmation_title);
        setDialogMessage(R.string.deletion_confirmation_message);
        setPositiveButtonText(R.string.dialog_button_ok);
        setNegativeButtonText(R.string.dialog_button_abort);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            DBLessons.getInstance().dropData();
            DBSubjects.getInstance().dropData();
        } else
            super.onDialogClosed(false);
    }
}
