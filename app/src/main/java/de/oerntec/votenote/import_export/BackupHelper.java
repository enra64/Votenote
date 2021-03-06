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
package de.oerntec.votenote.import_export;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.DatabaseCreator;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;
import de.oerntec.votenote.helpers.Permissions;
import de.oerntec.votenote.subject_management.SubjectManagementListActivity;

public class BackupHelper {
    public static void importDialog(final Context context) {
        //if there already are subjects, ask the user whether he truly wants to delete everything
        if (DBSubjects.getInstance().getCount() > 0) {
            AlertDialog.Builder b = new AlertDialog.Builder(context);
            b.setTitle(context.getString(R.string.xml_import_dialog_title));
            b.setMessage(context.getString(R.string.xml_import_warning_message));
            b.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startDialog(context);
                }
            });
            b.setNegativeButton(R.string.dialog_button_abort, null);
            b.show();
        } else
            startDialog(context);
    }

    private static void startDialog(final Context context) {
        FileDialog fileOpenDialog = new FileDialog(
                context,
                "FileOpen..",
                new FileDialog.FileDialogListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        try {
                            handleFileType(chosenDir, context);
                            if (context instanceof SubjectManagementListActivity)
                                ((SubjectManagementListActivity) context).reloadAdapter();
                        } catch (FileNotFoundException e){
                            Toast.makeText(context, "source file not found, no backup created!", Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(context, context.getString(R.string.import_result_bad), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
        //You can change the default filename using the public variable "Default_File_Name"
        fileOpenDialog.chooseFile_or_Dir();
    }

    private static void handleFileType(String chosenDir, Context context) throws IOException {
        if (chosenDir.contains(".db")) {
            DatabaseCreator.getInstance(context).importDatabase(chosenDir);
            Toast.makeText(context, context.getString(R.string.import_result_ok), Toast.LENGTH_LONG).show();
        } else if (chosenDir.contains(".xml")) {
            int result = XmlImporter.importXml(chosenDir, context) ? R.string.import_result_ok : R.string.import_result_bad;
            Toast.makeText(context, context.getString(result), Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(context, "Nur .xml oder .db unterstützt!", Toast.LENGTH_LONG).show();
    }

    public static void exportDialog(final Context context) {
        FileDialog fileOpenDialog = new FileDialog(
                context,
                "FileSave",
                new FileDialog.FileDialogListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        try {
                            if(MainActivity.ENABLE_DEBUG_LOG_CALLS)
                                Log.i("export target", chosenDir);
                            DatabaseCreator.getInstance(context).exportDatabase(chosenDir);
                            Toast.makeText(context, context.getString(R.string.import_result_ok), Toast.LENGTH_LONG).show();
                            if (context instanceof SubjectManagementListActivity)
                                ((SubjectManagementListActivity) context).reloadAdapter();
                        }
                        catch (FileNotFoundException e){
                            Toast.makeText(context, "source not found", Toast.LENGTH_LONG).show();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(context, context.getString(R.string.import_result_bad), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
        //You can change the default filename using the public variable "Default_File_Name"
        //fileOpenDialog.defaultFileName = "export.xml";
        fileOpenDialog.chooseFile_or_Dir();
    }
}
