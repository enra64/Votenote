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
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.support.v4.content.FileProvider;
import android.util.AttributeSet;

import de.oerntec.votenote.Database.DatabaseCreator;
import de.oerntec.votenote.ImportExport.BackupHelper;
import de.oerntec.votenote.ImportExport.CsvExporter;
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
            case "backup_export":
                //BackupHelper.exportDialog(getContext());
                final String auth = "de.oerntec.votenote.export_provider";
                final Uri uri = FileProvider.getUriForFile(getContext(), auth, DatabaseCreator.getInstance(getContext()).getDbFile());
                final String type = getContext().getContentResolver().getType(uri);

                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setDataAndType(uri, type);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                final Intent chooser = Intent.createChooser(shareIntent, "Export to");
                getContext().startActivity(chooser);
                break;
            case "backup_import":
                BackupHelper.importDialog(getContext());
                break;
            case "xml_import":
                XmlImporter.importDialog(getContext());
                break;
            default:
                super.onClick();
        }
    }
}
