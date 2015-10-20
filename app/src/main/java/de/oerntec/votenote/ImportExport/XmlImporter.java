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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

public class XmlImporter {
    private static boolean success;

    public static void importDialog(final Context activity) {
        //if there already are subjects, ask the user whether he truly wants to delete everything
        if (DBSubjects.getInstance().getCount() > 0) {
            AlertDialog.Builder b = new AlertDialog.Builder(activity);
            b.setTitle(activity.getString(R.string.xml_import_dialog_title));
            b.setMessage(activity.getString(R.string.xml_import_warning_message));
            b.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startDialog(activity);
                }
            });
            b.setNegativeButton(R.string.dialog_button_abort, null);
            b.show();
        } else
            startDialog(activity);
    }

    private static void startDialog(final Context activity) {
        FileDialog fileOpenDialog = new FileDialog(
                activity,
                "FileOpen..",
                new FileDialog.FileDialogListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        success = true;
                        importXml(chosenDir);
                        //check whether an exception was catched
                        if (success)
                            Toast.makeText(activity, activity.getString(R.string.import_result_ok), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(activity, activity.getString(R.string.import_result_bad), Toast.LENGTH_LONG).show();

                        if (activity instanceof SubjectManagementActivity) {
                            ((SubjectManagementActivity) activity).reloadAdapter();
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
            MainActivity.toast("File not found!");
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
            success = false;
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
                success = false;
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
                parser.nextTag();
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
                if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                    Log.i("parser entry", "skip");
                skip(parser);
            }
        }
    }

    private static void parseEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
        DBLessons db = DBLessons.getInstance();
        parser.require(XmlPullParser.START_TAG, null, "row");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "typ_uebung");
        String typ_uebung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "typ_uebung");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "nummer_uebung");
        String nummer_uebung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "nummer_uebung");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "max_votierung");
        String max_votierung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "max_votierung");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "my_votierung");
        String my_votierung = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "my_votierung");

        parser.nextTag();
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

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "_id");
        String id = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "_id");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "uebung_name");
        String uebung_name = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_name");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "uebung_minvote");
        String uebung_minvote = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_minvote");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "uebung_prespoints");
        String uebung_prespoints = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_prespoints");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "uebung_count");
        String uebung_count = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_count");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "uebung_maxvotes_per_ueb");
        String uebung_maxvotes_per_ueb = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_maxvotes_per_ueb");

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "uebung_max_prespoints");
        String uebung_max_prespoints = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "uebung_max_prespoints");

        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, null, "row");
        db.addGroup(
                Integer.valueOf(id),
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
