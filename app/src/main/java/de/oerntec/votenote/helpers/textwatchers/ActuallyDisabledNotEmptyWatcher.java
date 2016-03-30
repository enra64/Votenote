package de.oerntec.votenote.helpers.textwatchers;

import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;

/**
 * This textWatcher actually disables the given button.
 */
public class ActuallyDisabledNotEmptyWatcher extends AbstractNotEmptyWatcher {
    public ActuallyDisabledNotEmptyWatcher(EditText editText, String emptyText, @Nullable Button disableOnEmpty) {
        super(editText, emptyText, disableOnEmpty);
    }

    public ActuallyDisabledNotEmptyWatcher(
            EditText editText,
            String emptyText,
            boolean enableAtStart,
            @Nullable Button disableOnEmpty) {
        super(editText, emptyText, disableOnEmpty);
        mButton.setEnabled(enableAtStart);
    }

    @Override
    void setButtonDisabled(boolean disable) {
        if (mButton != null)
            mButton.setEnabled(!disable);
    }
}
