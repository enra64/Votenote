package de.oerntec.votenote;

import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
                    if (allCursor.getColumnIndex("_id") != i)
                        xmlBuilder.addColumn(allCursor.getColumnName(i), allCursor.getString(i));
                xmlBuilder.closeRow();
            } while (allCursor.moveToNext());
        }
        allCursor.close();
        xmlBuilder.closeTable();
    }

    public void importXml(String filename) {
        File dir = new File(Environment.getExternalStorageDirectory(), XmlExporter.DATASUBDIRECTORY);
        File file = new File(dir, filename + ".xml");
        InputStream inputStream;
        DBEntries.getInstance().dropData();
        DBGroups.getInstance().dropData();
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            MainActivity.toast("File not found");
            return;
        }
        startParser(inputStream);
    }

    private void startParser(InputStream in) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            parse(parser);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "database");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("table")) {
                parseEntryTable(parser);
                parser.next();
                parseSubjectTable(parser);
            } else {
                skip(parser);
            }
        }
    }

    private void parseSubjectTable(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "table");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("row")) {
                parseSubject(parser);
            } else {
                skip(parser);
            }
        }
    }

    private void parseEntryTable(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "table");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("row")) {
                parseEntry(parser);
            } else {
                Log.i("parser entry", "skip");
                skip(parser);
            }
        }
    }

    private void parseEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
        DBEntries db = DBEntries.getInstance();
        parser.require(XmlPullParser.START_TAG, null, "row");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "typ_uebung");
        String typ_uebung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "typ_uebung");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "nummer_uebung");
        String nummer_uebung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "nummer_uebung");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "max_votierung");
        String max_votierung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "max_votierung");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "my_votierung");
        String my_votierung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "my_votierung");

        parser.next();
        parser.require(XmlPullParser.END_TAG, null, "row");
        db.addEntry(
                Integer.valueOf(typ_uebung),
                Integer.valueOf(max_votierung),
                Integer.valueOf(my_votierung),
                Integer.valueOf(nummer_uebung));
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        result = parser.nextText();
        Log.i("parser", result == null ? "null" : result);
        if (parser.getEventType() != XmlPullParser.END_TAG) {
            parser.nextTag();
        }
        return result;
    }

    private void parseSubject(XmlPullParser parser) throws IOException, XmlPullParserException {
        DBGroups db = DBGroups.getInstance();
        parser.require(XmlPullParser.START_TAG, null, "row");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "uebung_name");
        String uebung_name = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_name");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "uebung_minvote");
        String uebung_minvote = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_minvote");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "uebung_prespoints");
        String uebung_prespoints = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_prespoints");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "uebung_count");
        String uebung_count = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_count");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "uebung_maxvotes_per_ueb");
        String uebung_maxvotes_per_ueb = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_maxvotes_per_ueb");

        parser.next();
        parser.require(XmlPullParser.START_TAG, null, "uebung_max_prespoints");
        String uebung_max_prespoints = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_max_prespoints");

        parser.next();
        parser.require(XmlPullParser.END_TAG, null, "row");
        db.addGroup(
                uebung_name,
                Integer.valueOf(uebung_minvote),
                Integer.valueOf(uebung_max_prespoints),
                Integer.valueOf(uebung_count),
                Integer.valueOf(uebung_maxvotes_per_ueb));
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
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