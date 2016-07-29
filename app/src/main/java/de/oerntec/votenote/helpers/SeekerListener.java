package de.oerntec.votenote.helpers;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekerListener implements SeekBar.OnSeekBarChangeListener {
    /**
     * The textview we want to change when the seekbar value changes
     */
    private final TextView mAffected;

    /**
     * The view we want to lose focus if the seekbar is moved - originally written as a workaround
     * againt the scrollview moving when the seekbar is used in percentage tracker creation
     */
    private final @Nullable View mReleaseFocusView;

    /**
     * The value suffix to append
     */
    private final @Nullable String mEnding;

    public SeekerListener(TextView affected) {
        this(affected, null);
    }

    public SeekerListener(TextView affected, @Nullable String ending) {
        this(affected, ending, null);
    }

    public SeekerListener(TextView affected, @Nullable String ending, @Nullable EditText removeFocus) {
        mAffected = affected;
        mEnding = ending;
        mReleaseFocusView = removeFocus;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        if (mEnding == null)
            mAffected.setText(
                    String.format(
                            General.getCurrentLocale(seekBar.getContext()),
                            "%d",
                            progress));
        else
            mAffected.setText(String.format(General.getCurrentLocale(seekBar.getContext()), "%d%s", progress, mEnding));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if(mReleaseFocusView != null){
            mReleaseFocusView.clearFocus();
            seekBar.requestFocus();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
