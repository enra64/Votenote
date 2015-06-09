package de.oerntec.votenote;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

public class ExportHelper {
    public static void createExportDialog(final MainActivity context) {
        //get database access
        final DBGroups groupsDB = DBGroups.getInstance();

        //inflate layout so we have something to work with
        final View inputView = context.getLayoutInflater().inflate(R.layout.mainfragment_dialog_export, null);

        //set infotext
        TextView info = (TextView) inputView.findViewById(R.id.mainfragment_dialog_export_infotext);
        info.setText("Die Exportdatei wird nach \"" + Environment.getExternalStorageDirectory() + "/Votenote\" gespeichert. (Dein normaler Speicher)");

        //list containing uebungs to export
        final boolean[] untranslatedExportList = new boolean[groupsDB.getUebungCount()];

        // set up the drawer's list view with items and click listener
        Cursor allNamesCursor = groupsDB.getAllGroupNames();
        //define wanted columnsgroupsDB
        String[] sourceColumns = {DatabaseCreator.GROUPS_NAMEN};

        //define id values of views to be set
        int[] targetViews = {R.id.diagramactivity_listview_listitem_text_name};

        // create the adapter using the cursor pointing to the desired data
        SimpleCursorAdapter exportUebungChooserAdapter = new SimpleCursorAdapter(
                context,
                R.layout.diagramactivity_listitem,
                allNamesCursor,
                sourceColumns,
                targetViews,
                0);

        //get listview
        ListView exportUebungChooser = (ListView) inputView.findViewById(R.id.mainfragment_dialog_export_checklist);
        exportUebungChooser.setAdapter(exportUebungChooserAdapter);
        exportUebungChooser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox thisBox = (CheckBox) view.findViewById(R.id.diagramactivity_listview_listitem_checkbox);
                thisBox.setChecked(!thisBox.isChecked());
                //save appropriate value
                untranslatedExportList[position] = thisBox.isChecked();
            }
        });

        //dialog building
        AlertDialog.Builder b = new AlertDialog.Builder(context)
                .setView(inputView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ExportHelper.exportToCSV(untranslatedExportList, context);
                    }
                })
                .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        b.setTitle("Nach CSV exportieren").create().show();
    }

    public static String exportToCSV(boolean[] untranslatedExportList, Context context) {
        //get database access
        final DBGroups groupsDB = DBGroups.getInstance();
        final DBEntries entryDB = DBEntries.getInstance();

        StringBuilder s = new StringBuilder();
        //seperator for excel
        s.append("sep=;");
        s.append("\r\n");
        String fileName = "";
        //for each checked uebung, export:
        //name
        //uebungsnummer, gemachte aufgaben, maximal machbare aufgaben
        //num, gemauf(num), maxauf(num)
        for (int untranslated = 0; untranslated < untranslatedExportList.length; untranslated++) {
            if (untranslatedExportList[untranslated]) {
                int translated = groupsDB.translatePositionToID(untranslated);
                String groupName = groupsDB.getGroupName(translated);
                fileName += groupName + "_";
                s.append(groupName);
                s.append(";;\r\n");
                s.append("Nummer,Gemachte Aufgaben,Maximale Aufgaben");
                s.append("\r\n");
                Cursor entryCursor = entryDB.getGroupRecords(translated);

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
        //get current time for filename
        fileName += getDate();
        writeToFile(fileName, s.toString(), context);
        return s.toString();
    }

    public static String getDate() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.DATE) + "_" + c.get(Calendar.MONTH) + "_" + c.get(Calendar.YEAR) + "_" +
                c.get(Calendar.HOUR_OF_DAY) + "_" + c.get(Calendar.MINUTE) + "_" + c.get(Calendar.SECOND);
    }

    public static boolean writeToFile(String fileName, String exportString, Context context) {
        //write built string to txt file
        try {
            File root = new File(Environment.getExternalStorageDirectory() + File.separator + "Votenote" + File.separator);
            root.mkdirs();
            if (root.exists()) {
                File writeoutFile = new File(root, fileName + ".csv");
                FileWriter writer = new FileWriter(writeoutFile);
                Log.i("exporter", writer.getEncoding());
                writer.append(exportString.toString());
                writer.flush();
                writer.close();
                Toast.makeText(context, "Datei unter " + writeoutFile.getAbsolutePath() + " gespeichert.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Ordner konnte nicht erstellt werden!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            String exportError = e.getMessage();
            Toast.makeText(context, "Export konnte nicht geschrieben werden!", Toast.LENGTH_SHORT).show();
            Log.w("exporter", "ioexception: " + exportError);
            return false;
        }
        return true;
    }
}
