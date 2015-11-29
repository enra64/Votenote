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
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.Pojo.Subject;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBSubjects;
import de.oerntec.votenote.MainActivity;

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
        StringBuilder b = new StringBuilder();
        String le = "\r\n";
        String apmHeader = "Name,Required percentage for admission,Estimated Assignments per Lesson,Estimated Lesson Count";
        String apdHeader = "Lesson,Finished Assignments,Available Assignments";
        String acHeader = "Current points,Target point count";
        List<Subject> subjects = getSubjects();
        //separator for excel
        b.append("sep=;").append(le);
        for(Subject s : subjects){
            b.append(s.name).append(le);
            for(AdmissionPercentageMeta apm : s.admissionPercentageMetaList){
                b.append(apmHeader).append(le);
                b.append(apm.getCsvRepresentation()).append(le);
                for(AdmissionPercentageData apd : apm.mDataList){
                    b.append(apd.getCsvRepresentation()).append(le);
                }
            }
            for(AdmissionCounter ac : s.admissionCounterList){
                b.append(acHeader).append(le);
                b.append(ac.getCsvRepresentation()).append(le);
            }
            b.append(le).append(le);
        }
        try {
            Writer.writeToFile(b.toString(), path);
        } catch (IOException e) {
            Toast.makeText(activity, "Exception occured", Toast.LENGTH_LONG);
            e.printStackTrace();
        }
    }

    private static List<Subject> getSubjects(){
        final DBAdmissionPercentageData mApDataDb = DBAdmissionPercentageData.getInstance();
        final DBAdmissionPercentageMeta mApMetaDb = DBAdmissionPercentageMeta.getInstance();
        final DBAdmissionCounters mCountersDb = DBAdmissionCounters.getInstance();
        final DBSubjects mSubjectDb = DBSubjects.getInstance();

        List<Subject> result = mSubjectDb.getAllSubjects();
        for(Subject s : result)
            s.loadAllData(mCountersDb, mApDataDb, mApMetaDb, MainActivity.getPreference("reverse_lesson_sort", false));
        return result;
    }
}
