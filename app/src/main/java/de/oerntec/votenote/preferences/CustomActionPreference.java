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
package de.oerntec.votenote.preferences;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.support.v4.content.FileProvider;
import android.util.AttributeSet;

import java.io.File;

import de.oerntec.votenote.database.DatabaseCreator;
import de.oerntec.votenote.import_export.BackupHelper;
import de.oerntec.votenote.import_export.CsvExporter;

public class CustomActionPreference extends Preference {
    private final String mActionKey;

    public CustomActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActionKey = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "key");
    }

    @Override
    protected void onClick() {
        switch (mActionKey) {
            case "csv_export":
                // trigger onsharedpreferencechanged
                persistBoolean(true);
                CsvExporter.exportDialog(getContext());
                break;
            case "backup_share":
                final String authority = "de.oerntec.votenote.export_provider";
                final File dbFile = DatabaseCreator.getInstance(getContext()).getDbFileBackup(getContext());
                if(dbFile == null || !dbFile.exists())
                    throw new AssertionError("db not found");
                final Uri uri = FileProvider.getUriForFile(getContext(), authority, dbFile);
                final String type = getContext().getContentResolver().getType(uri);

                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setDataAndType(uri, type);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                final Intent chooser = Intent.createChooser(shareIntent, "Export to");
                getContext().startActivity(chooser);
                break;
            case "backup_export":
                // trigger onsharedpreferencechanged
                persistBoolean(true);
                BackupHelper.exportDialog(getContext());
                break;
            case "backup_import":
                BackupHelper.importDialog(getContext());
                // trigger onsharedpreferencechanged
                persistBoolean(true);
                break;
            default:
                super.onClick();
        }
    }
}
