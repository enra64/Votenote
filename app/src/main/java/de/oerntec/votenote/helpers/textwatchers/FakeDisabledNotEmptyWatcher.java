package de.oerntec.votenote.helpers.textwatchers;

import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import de.oerntec.votenote.R;

/**
 * This TextWatcher does not disable the button, but instead makes it look disabled.
 * One can give it two OnClickListeners, one to be called in the "disabled" state, and one that is
 * called when the button is "enabled".
 */
public class FakeDisabledNotEmptyWatcher extends AbstractNotEmptyWatcher {
    /**
     * Text color when the button is fake disabled
     */
    private static int mDisabledTextColor;

    /**
     * Text color when the button is enabled
     */
    private static int mEnabledTextColor;

    /**
     * OnClickListener that is used when the button is "disabled"
     */
    private View.OnClickListener mDisabledOnClickListener;

    /**
     * OnClickListener that is used when the button is "enabled"
     */
    private View.OnClickListener mEnabledOnClickListener;

    public FakeDisabledNotEmptyWatcher(
            EditText editText,
            String emptyText,
            boolean disabledAtStart,
            @Nullable View.OnClickListener disabledOnClickListener,
            @Nullable View.OnClickListener enabledOnClickListener,
            @Nullable Button disableOnEmpty) {
        super(editText, emptyText, disableOnEmpty);
        mEnabledTextColor = ContextCompat.getColor(mButton.getContext(), R.color.flat_button_enabled_text);
        mDisabledTextColor = ContextCompat.getColor(mButton.getContext(), R.color.flat_button_disabled_text);
        mDisabledOnClickListener = disabledOnClickListener;
        mEnabledOnClickListener = enabledOnClickListener;

        setButtonDisabled(disabledAtStart);
    }

    private void fakeDisable(boolean disable) {
        if (mButton == null) return; //wat
        mButton.setTextColor(disable ? mDisabledTextColor : mEnabledTextColor);
    }

    @Override
    void setButtonDisabled(boolean disabled) {
        if (mButton != null) {
            fakeDisable(disabled);
            mButton.setOnClickListener(disabled ? mDisabledOnClickListener : mEnabledOnClickListener);
        }
    }
}
