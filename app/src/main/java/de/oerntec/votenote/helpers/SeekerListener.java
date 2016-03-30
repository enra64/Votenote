package de.oerntec.votenote.helpers;

import android.widget.SeekBar;
import android.widget.TextView;

public class SeekerListener implements SeekBar.OnSeekBarChangeListener {
    TextView mAffected;
    String mEndingAddition;

    public SeekerListener(TextView affected, String endingAddition) {
        mAffected = affected;
        mEndingAddition = endingAddition;
    }

    public SeekerListener(TextView affected) {
        this(affected, "");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        String concatenated = progress + mEndingAddition;
        mAffected.setText(concatenated);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
