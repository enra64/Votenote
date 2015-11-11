/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.Preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBGroups;
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
            DBGroups.getInstance().dropData();
        } else
            super.onDialogClosed(false);
    }
}
