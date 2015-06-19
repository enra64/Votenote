package de.oerntec.votenote.ImportExport;

import android.app.Activity;
import android.database.Cursor;
import android.widget.Toast;

import java.io.IOException;

import de.oerntec.votenote.DBLessons;
import de.oerntec.votenote.DBSubjects;
import de.oerntec.votenote.DatabaseCreator;
import de.oerntec.votenote.R;

/**
 * XmlBuilder is used to write XML tags (open and close, and a few attributes)
 * to a StringBuilder. Here we have nothing to do with IO or SQL, just a fancy StringBuilder.
 *
 * @author ccollins from http://stackoverflow.com/a/13140325
 */
public class XmlExporter {
    private static XmlBuilder xmlBuilder;
    private static boolean success;

    public static void exportDialog(final Activity activity) {
        FileDialog fileOpenDialog = new FileDialog(
                activity,
                "FileSave",
                new FileDialog.FileDialogListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        success = true;
                        XmlExporter.export(chosenDir);
                        //check whether an exception was catched
                        if (success)
                            Toast.makeText(activity, activity.getString(R.string.import_result_ok), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(activity, activity.getString(R.string.import_result_bad), Toast.LENGTH_LONG).show();
                    }
                }
        );
        //You can change the default filename using the public variable "Default_File_Name"
        //fileOpenDialog.defaultFileName = "export.xml";
        fileOpenDialog.chooseFile_or_Dir(/*fileOpenDialog.defaultFileName*/);
    }

    private static void export(String fileName) {
        try {
            xmlBuilder = new XmlBuilder();
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        xmlBuilder.start(DatabaseCreator.DATABASE_NAME);

        try {
            //export both tables
            exportTable(DBLessons.getInstance().getAllData(), "entries");
            exportTable(DBSubjects.getInstance().getAllData(), "subjects");
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }

        String xmlString = null;
        try {
            xmlString = xmlBuilder.end();
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        try {
            Writer.writeToFile(xmlString, fileName);
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        xmlBuilder = null;
    }

    private static void exportTable(Cursor allCursor, String name) throws IOException {
        xmlBuilder.openTable(name);
        if (allCursor.moveToFirst()) {
            int cols = allCursor.getColumnCount();
            do {
                xmlBuilder.openRow();
                for (int i = 0; i < cols; i++)
                    if ("subjects".equals(name) || allCursor.getColumnIndex("_id") != i)
                        xmlBuilder.addColumn(allCursor.getColumnName(i), allCursor.getString(i));
                xmlBuilder.closeRow();
            } while (allCursor.moveToNext());
        }
        allCursor.close();
        xmlBuilder.closeTable();
    }

    /**
     * XmlBuilder is used to write XML tags (open and close, and a few attributes)
     * to a StringBuilder. Here we have nothing to do with IO or SQL, just a fancy StringBuilder.
     *
     * @author ccollins from http://stackoverflow.com/a/13140325
     */
    static class XmlBuilder {
        private static final String OPEN_XML_STANZA = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        private static final String CLOSE_WITH_TICK = "'>";
        private static final String CLOSE_NO_TICK = ">";
        private static final String START_OPEN_TAG = "<";
        private static final String START_CLOSE_TAG = "</";
        private static final String DB_OPEN = "<database xmlversion='1' name='";
        private static final String DB_CLOSE = "</database>";
        private static final String TABLE_OPEN = "<table name='";
        private static final String TABLE_CLOSE = "</table>";
        private static final String ROW_OPEN = "<row>";
        private static final String ROW_CLOSE = "</row>";

        private final StringBuilder sb;

        public XmlBuilder() throws IOException {
            sb = new StringBuilder();
        }

        void start(final String dbName) {
            sb.append(XmlBuilder.OPEN_XML_STANZA);
            sb.append(XmlBuilder.DB_OPEN);
            sb.append(dbName);
            sb.append(XmlBuilder.CLOSE_WITH_TICK);
        }

        String end() throws IOException {
            sb.append(XmlBuilder.DB_CLOSE);
            return sb.toString();
        }

        void openTable(final String tableName) {
            sb.append(XmlBuilder.TABLE_OPEN);
            sb.append(tableName);
            sb.append(XmlBuilder.CLOSE_WITH_TICK);
        }

        void closeTable() {
            sb.append(XmlBuilder.TABLE_CLOSE);
        }

        void openRow() {
            sb.append(XmlBuilder.ROW_OPEN);
        }

        void closeRow() {
            sb.append(XmlBuilder.ROW_CLOSE);
        }

        void addColumn(final String tag, final String val) {
            sb.append(XmlBuilder.START_OPEN_TAG);
            sb.append(tag);
            sb.append(XmlBuilder.CLOSE_NO_TICK);

            sb.append(val);

            sb.append(XmlBuilder.START_CLOSE_TAG);
            sb.append(tag);
            sb.append(XmlBuilder.CLOSE_NO_TICK);
        }
    }

}