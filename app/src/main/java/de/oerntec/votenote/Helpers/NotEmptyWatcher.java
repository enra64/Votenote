package de.oerntec.votenote.Helpers;

import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

/**
 * Sets an error if the EditText is empty
 */
public class NotEmptyWatcher implements TextWatcher {
    private final EditText mEditText;
    private final Button mDisableOnEmptyButton;

    public NotEmptyWatcher(EditText editText, @Nullable Button disableOnEmpty) {
        mEditText = editText;
        mDisableOnEmptyButton = disableOnEmpty;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 0) {
            mEditText.setError("Must not be empty!");
            if (mDisableOnEmptyButton != null)
                mDisableOnEmptyButton.setEnabled(false);
        } else {
            if (mDisableOnEmptyButton != null)
                mDisableOnEmptyButton.setEnabled(true);
        }
    }
}
