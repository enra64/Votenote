package de.oerntec.votenote;

import android.database.Cursor;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * XmlBuilder is used to write XML tags (open and close, and a few attributes)
 * to a StringBuilder. Here we have nothing to do with IO or SQL, just a fancy StringBuilder.
 *
 * @author ccollins from http://stackoverflow.com/a/13140325
 */
public class XmlExporter {

    static final String DATASUBDIRECTORY = "Votenote";

    // private final SQLiteDatabase db;
    private XmlBuilder xmlBuilder;

    public void export(String fileName) {
        try {
            xmlBuilder = new XmlBuilder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        xmlBuilder.start(DatabaseCreator.DATABASE_NAME);

        try {
            //export both tables
            exportTable(DBEntries.getInstance().getAllData(), "entries");
            exportTable(DBGroups.getInstance().getAllData(), "subjects");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String xmlString = null;
        try {
            xmlString = xmlBuilder.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writeToFile(xmlString, fileName + ".xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportTable(Cursor allCursor, String name) throws IOException {
        xmlBuilder.openTable(name);
        if (allCursor.moveToFirst()) {
            int cols = allCursor.getColumnCount();
            do {
                xmlBuilder.openRow();
                for (int i = 0; i < cols; i++)
                    xmlBuilder.addColumn(allCursor.getColumnName(i), allCursor.getString(i));
                xmlBuilder.closeRow();
            } while (allCursor.moveToNext());
        }
        allCursor.close();
        xmlBuilder.closeTable();
    }

    private void writeToFile(final String xmlString, final String exportFileName) throws IOException {
        File dir = new File(Environment.getExternalStorageDirectory(), XmlExporter.DATASUBDIRECTORY);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, exportFileName);
        file.createNewFile();

        ByteBuffer buff = ByteBuffer.wrap(xmlString.getBytes());
        FileChannel channel = new FileOutputStream(file).getChannel();
        try {
            channel.write(buff);
        } finally {
            if (null != channel)
                channel.close();
        }
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
        private static final String DB_OPEN = "<database name='";
        private static final String DB_CLOSE = "</database>";
        private static final String TABLE_OPEN = "<table name='";
        private static final String TABLE_CLOSE = "</table>";
        private static final String ROW_OPEN = "<row>";
        private static final String ROW_CLOSE = "</row>";

        //entries stuff
        private static final String ID_OPEN = "<id>";
        private static final String ID_CLOSE = "</id>";

        private static final String TYPE_OPEN = "<type>";
        private static final String TYPE_CLOSE = "</type>";

        private static final String MY_VOTE_OPEN = "<myvote>";
        private static final String MY_VOTE_CLOSE = "</myvote>";

        private static final String LESSON_ID_OPEN = "<lesson_id>";
        private static final String LESSON_ID_CLOSE = "</lesson_id>";

        private static final String MAX_VOTE_OPEN = "<maxvote>";
        private static final String MAX_VOTE_CLOSE = "</maxvote>";

        //subjects stuff
        private static final String SUBJECT_NAME_OPEN = "<maxvote>";
        private static final String SUBJECT_NAME_CLOSE = "</maxvote>";

        private static final String MIN_VOTE_PERCENTAGE_OPEN = "<maxvote>";
        private static final String MIN_VOTE_PERCENTAGE_CLOSE = "</maxvote>";

        private static final String PRESENTATIION_POINTS_OPEN = "<maxvote>";
        private static final String PRESENTATION_POINTS_CLOSE = "</maxvote>";

        private static final String SCHEDULED_ASSIGNMENTS_OPEN = "<maxvote>";
        private static final String SCHEDULED_ASSIGNMENTS_CLOSE = "</maxvote>";

        private static final String SCHEDULED_LESSONS_OPEN = "<maxvote>";
        private static final String SCHEDULED_LESSONS_CLOSE = "</maxvote>";

        private static final String SCHEDULED_PRESPOINTS_OPEN = "<maxvote>";
        private static final String SCHEDULED_PRESPOINTS_CLOSE = "</maxvote>";


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

        void addSubjectName(final String val) {
            sb.append(XmlBuilder.SUBJECT_NAME_OPEN);
            sb.append(val);
            sb.append(XmlBuilder.SUBJECT_NAME_CLOSE);
        }

        void addSubjectId(final String val) {
            sb.append(XmlBuilder.ID_OPEN);
            sb.append(val);
            sb.append(XmlBuilder.ID_CLOSE);
        }

        void addMinimumPercentage(final String val) {
            sb.append(XmlBuilder.MIN_VOTE_PERCENTAGE_OPEN);
            sb.append(val);
            sb.append(XmlBuilder.MIN_VOTE_PERCENTAGE_CLOSE);
        }

        void addPresentationPoints(final String val) {
            sb.append(XmlBuilder.PRESENTATIION_POINTS_OPEN);
            sb.append(val);
            sb.append(XmlBuilder.PRESENTATION_POINTS_CLOSE);
        }

        void addMyVote(final String val) {
            sb.append(XmlBuilder.MY_VOTE_OPEN);
            sb.append(val);
            sb.append(XmlBuilder.MY_VOTE_CLOSE);
        }

        void addMaxVote(final String val) {
            sb.append(XmlBuilder.MAX_VOTE_OPEN);
            sb.append(val);
            sb.append(XmlBuilder.MAX_VOTE_CLOSE);
        }
    }

}