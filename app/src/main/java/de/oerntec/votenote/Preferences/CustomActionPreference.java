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
import android.preference.Preference;
import android.util.AttributeSet;

import de.oerntec.votenote.ImportExport.CsvExporter;
import de.oerntec.votenote.ImportExport.XmlExporter;
import de.oerntec.votenote.ImportExport.XmlImporter;

public class CustomActionPreference extends Preference {
    String mActionKey;

    public CustomActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActionKey = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "key");
    }

    @Override
    protected void onClick() {
        switch (mActionKey) {
            case "csv_export":
                CsvExporter.exportDialog(getContext());
                break;
            case "xml_export":
                XmlExporter.exportDialog(getContext());
                break;
            case "xml_import":
                XmlImporter.importDialog(getContext());
                break;
            case "show_eula":

                break;
            default:
                super.onClick();
        }
    }
}
