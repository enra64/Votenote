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
package de.oerntec.votenote.ImportExport;

import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import de.oerntec.votenote.Database.DBGroups;
import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.Group;
import de.oerntec.votenote.R;

public class CsvExporter {
    public static FileDialog exportDialog(final Context activity) {
        FileDialog fileOpenDialog = new FileDialog(
                activity,
                "FileSave",
                new FileDialog.FileDialogListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        export(chosenDir, activity);
                    }
                }
        );
        //You can change the default filename using the public variable "Default_File_Name"
        fileOpenDialog.defaultFileName = "export.csv";
        fileOpenDialog.chooseFile_or_Dir();
        return fileOpenDialog;
    }

    private static void export(final String path, final Context activity) {
        //get database access
        final DBGroups groupsDB = DBGroups.getInstance();
        final DBLessons entryDB = DBLessons.getInstance();

        StringBuilder s = new StringBuilder();
        //separator for excel
        s.append("sep=;");
        s.append("\r\n");

        List<Group> subjectList = groupsDB.getAllSubjects();

        for (Group subject : subjectList) {
            String groupName = subject.subjectName;
            s.append(groupName);
            s.append(": ");
            s.append(subject.subjectMinimumVotePercentage);
            s.append("%");
            s.append(", ");
            s.append(subject.subjectCurrentPresentationPoints);
            s.append(" von ");
            s.append(subject.subjectWantedPresentationPoints);
            s.append(" Vortragspunkten mit ");
            s.append(subject.subjectScheduledAssignmentsPerLesson);
            s.append(" Aufgaben in ");
            s.append(subject.subjectScheduledLessonCount);
            s.append(" Übungen");

            s.append(";;\r\n");
            s.append("Nummer,Gemachte Aufgaben,Maximale Aufgaben");
            s.append("\r\n");
            Cursor entryCursor = entryDB.getAllLessonsForSubject(Integer.valueOf(subject.id));

            if (entryCursor.getCount() > 0) {
                //read db to string
                do {
                    s.append(entryCursor.getInt(0));
                    s.append(";");
                    s.append(entryCursor.getInt(1));
                    s.append(";");
                    s.append(entryCursor.getInt(2));
                    s.append("\r\n");
                } while (entryCursor.moveToNext());

                //add some free lines
                s.append("\r\n\r\n");
            }
        }
        try {
            Writer.writeToFile(s.toString(), path);
            Toast.makeText(activity, activity.getString(R.string.import_result_ok), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, activity.getString(R.string.import_result_bad), Toast.LENGTH_LONG).show();
        }
    }
}
