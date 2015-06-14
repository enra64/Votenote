package de.oerntec.votenote;

import android.app.Activity;
import android.database.Cursor;

import java.io.IOException;
import java.util.List;

public class ExportHelper {
    public static void exportDialog(final Activity activity) {
        SimpleFileDialog fileOpenDialog = new SimpleFileDialog(
                activity,
                "FileSave",
                new SimpleFileDialog.SimpleFileDialogListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        export(chosenDir);
                    }
                }
        );
        //You can change the default filename using the public variable "Default_File_Name"
        //fileOpenDialog.default_file_name = "export.xml";
        fileOpenDialog.chooseFile_or_Dir(/*fileOpenDialog.default_file_name*/);
    }

    private static void export(final String path) {
        //get database access
        final DBGroups groupsDB = DBGroups.getInstance();
        final DBEntries entryDB = DBEntries.getInstance();

        StringBuilder s = new StringBuilder();
        //seperator for excel
        s.append("sep=;");
        s.append("\r\n");

        List<DBGroups.Subject> subjectList = groupsDB.getAllLessons();

        for (DBGroups.Subject subject : subjectList) {
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
            Cursor entryCursor = entryDB.getGroupRecords(Integer.valueOf(subject.id));

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
            new XmlExporter().writeToFile(s.toString(), path);
        } catch (IOException e) {
            e.printStackTrace();
            MainActivity.toast("Fehlschlag!");
        }
    }
}
