package de.oerntec.votenote.helpers;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.oerntec.votenote.R;

/**
 * Dialog to show
 */
public class DayOfWeekPickerDialog extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnShowListener {
    private int mInitialDay;
    private NumberPicker mNumberPicker;
    private DowPickedListener mListener;

    /**
     * Fragment factory to get android studio to shut up. Creates a fragment
     * to pick a day of week for notifications
     */
    public static DayOfWeekPickerDialog newInstance(int initialDay) {
        DayOfWeekPickerDialog d = new DayOfWeekPickerDialog();
        Bundle b = new Bundle();
        b.putInt("initial_day", initialDay);
        d.setArguments(b);
        return d;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInitialDay = getArguments().getInt("initial_day");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.dow_picker_title))
                .setView(R.layout.dow_picker)
                .setPositiveButton(R.string.dialog_button_ok, this)
                .setNegativeButton(R.string.dialog_button_abort, null).create();

        dialog.setOnShowListener(this);
        return dialog;
    }

    /**
     * Configure min, max, current, set custom display values
     */
    private void configureNumberPicker() {
        mNumberPicker.setMinValue(0);
        mNumberPicker.setMaxValue(6);
        mNumberPicker.setDisplayedValues(getDayStrings());
        mNumberPicker.setValue(mInitialDay);

        // borrowed from slidedaytimepicker, this stops the edittextyness
        // the entries would otherwise have
        for (int i = 0; i < 7; i++) {
            View child = mNumberPicker.getChildAt(i);

            if (child instanceof EditText) {
                child.setFocusable(false);
                return;
            }
        }
    }

    /**
     * While on a scale from 1 to 10 this is bad coding, it should get the job
     * of getting day of week strings aligned with the calendar class done quite
     * solidly.
     *
     * @return An array of strings where the index corresponds to the Calendar dow.
     */
    private String[] getDayStrings() {
        String[] days = new String[7];
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", General.getCurrentLocale(getActivity()));
        for (int i = 0; i < 7; i++) {
            cal.set(Calendar.DAY_OF_WEEK, i);
            Date d = new Date(cal.getTimeInMillis());
            days[i] = sdf.format(d);
        }
        return days;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            mListener.onDowPicked(mNumberPicker.getValue());
        }
    }

    public void setOnDowPickedListener(DowPickedListener listener) {
        mListener = listener;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mNumberPicker = (NumberPicker) getDialog().findViewById(R.id.dow_picker_number_picker);
        configureNumberPicker();
    }

    public interface DowPickedListener {
        void onDowPicked(int dow);
    }
}
