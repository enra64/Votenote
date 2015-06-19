package de.oerntec.votenote.ImportExport;

import android.app.Activity;
import android.database.Cursor;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import de.oerntec.votenote.DBLessons;
import de.oerntec.votenote.DBSubjects;
import de.oerntec.votenote.R;
import de.oerntec.votenote.Subject;

public class CsvExporter {
    public static FileDialog exportDialog(final Activity activity) {
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

    private static void export(final String path, final Activity activity) {
        //get database access
        final DBSubjects groupsDB = DBSubjects.getInstance();
        final DBLessons entryDB = DBLessons.getInstance();

        StringBuilder s = new StringBuilder();
        //seperator for excel
        s.append("sep=;");
        s.append("\r\n");

        List<Subject> subjectList = groupsDB.getAllLessons();

        for (Subject subject : subjectList) {
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
