package de.oerntec.votenote.helpers.textwatchers;

import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

/**
 * Sets an error if the EditText is empty
 */
abstract class AbstractNotEmptyWatcher implements TextWatcher {
    final Button mButton;
    private final EditText mEditText;
    private final String mErrorString;

    AbstractNotEmptyWatcher(
            EditText editText,
            String emptyText,
            @Nullable Button disableOnEmpty) {
        mEditText = editText;
        mButton = disableOnEmpty;
        mErrorString = emptyText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    abstract void setButtonDisabled(boolean disable);

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 0)
            mEditText.setError(mErrorString);

        // enable the button if either the watcher is disabled or the string is longer than 0
        setButtonDisabled(s.length() == 0);
    }
}
