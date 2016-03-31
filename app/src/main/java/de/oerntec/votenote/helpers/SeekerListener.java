package de.oerntec.votenote.helpers;

import android.widget.SeekBar;
import android.widget.TextView;

public class SeekerListener implements SeekBar.OnSeekBarChangeListener {
    private final TextView mAffected;

    public SeekerListener(TextView affected) {
        mAffected = affected;
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        mAffected.setText(String.format(General.getCurrentLocale(seekBar.getContext()), "%d", progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
