package de.oerntec.votenote.helpers;

import android.support.annotation.Nullable;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekerListener implements SeekBar.OnSeekBarChangeListener {
    private final TextView mAffected;
    private final
    @Nullable
    String mEnding;

    public SeekerListener(TextView affected) {
        this(affected, null);
    }

    public SeekerListener(TextView affected, @Nullable String ending) {
        mAffected = affected;
        mEnding = ending;
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
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
