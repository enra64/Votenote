package de.oerntec.votenote.ImportExport;

import android.app.Activity;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.oerntec.votenote.DBLessons;
import de.oerntec.votenote.DBSubjects;
import de.oerntec.votenote.FileDialog;
import de.oerntec.votenote.GroupManagementActivity;
import de.oerntec.votenote.MainActivity;

public class XmlImporter {
    public static void importDialog(final Activity activity) {
        FileDialog fileOpenDialog = new FileDialog(
                activity,
                "FileOpen..",
                new FileDialog.FileDialogListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        importXml(chosenDir);
                        if (activity instanceof MainActivity) {
                            MainActivity.mNavigationDrawerFragment.reloadAdapter();
                            MainActivity.mNavigationDrawerFragment.selectItem(0);
                        } else if (activity instanceof GroupManagementActivity) {
                            ((GroupManagementActivity) activity).reloadList();
                        }
                    }
                }
        );
        //You can change the default filename using the public variable "Default_File_Name"
        fileOpenDialog.chooseFile_or_Dir();
    }

    private static void importXml(String filename) {
        File file = new File(filename);
        InputStream inputStream;
        DBLessons.getInstance().dropData();
        DBSubjects.getInstance().dropData();
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            MainActivity.toast("File not found");
            return;
        }
        startParser(inputStream);
    }

    private static void startParser(InputStream in) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            parse(parser);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
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

    private static void parseSubjectTable(XmlPullParser parser) throws XmlPullParserException, IOException {
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

    private static void parseEntryTable(XmlPullParser parser) throws XmlPullParserException, IOException {
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

    private static void parseEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
        DBLessons db = DBLessons.getInstance();
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
        db.addLesson(
                Integer.valueOf(typ_uebung),
                Integer.valueOf(max_votierung),
                Integer.valueOf(my_votierung),
                Integer.valueOf(nummer_uebung));
    }

    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result;
        result = parser.nextText();
        //Log.i("parser", result == null ? "null" : result);
        if (parser.getEventType() != XmlPullParser.END_TAG) {
            parser.nextTag();
        }
        return result;
    }

    private static void parseSubject(XmlPullParser parser) throws IOException, XmlPullParserException {
        DBSubjects db = DBSubjects.getInstance();
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
                Integer.valueOf(uebung_prespoints),
                Integer.valueOf(uebung_count),
                Integer.valueOf(uebung_maxvotes_per_ueb));
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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
}
