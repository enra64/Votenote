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
package de.oerntec.votenote.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.import_export.Writer;

public class DatabaseCreator extends SQLiteOpenHelper {
    //new
    public static final String TABLE_NAME_SUBJECTS = "subjects";
    public static final String TABLE_NAME_ADMISSION_COUNTERS = "admission_counters";
    public static final String TABLE_NAME_ADMISSION_PERCENTAGES_META = "admission_percentages_meta";
    public static final String TABLE_NAME_ADMISSION_PERCENTAGES_DATA = "admission_percentages_data";
    public static final String TABLE_NAME_LAST_VIEWED = "last_viewed";
    /***********************************************************************************************
     * admission counter database start
     **********************************************************************************************/
    public static final String SUBJECTS_ID = "_id";
    public static final String SUBJECTS_NAME = "uebung_name";
    /***********************************************************************************************
     * admission counter database start
     **********************************************************************************************/
    public static final String ADMISSION_COUNTER_ID = "_id";
    public static final String ADMISSION_COUNTER_SUBJECT_ID = "subject_id";
    public static final String ADMISSION_COUNTER_COUNTER_NAME = "counter_name";
    public static final String ADMISSION_COUNTER_CURRENT = "current";
    public static final String ADMISSION_COUNTER_TARGET = "target";
    /***********************************************************************************************
     * admission percentage names database start
     **********************************************************************************************/
    public static final String ADMISSION_PERCENTAGES_META_ID = "_id";
    public static final String ADMISSION_PERCENTAGES_META_SUBJECT_ID = "subject_id";
    public static final String ADMISSION_PERCENTAGES_META_NAME = "percentage_name";
    public static final String ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE = "target_percentage";
    public static final String ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE = "bonus_target_percentage";
    public static final String ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED = "bonus_target_percentage_enabled";
    public static final String ADMISSION_PERCENTAGES_META_RECURRENCE_DATA_STRING = "recurrence_string";
    public static final String ADMISSION_PERCENTAGES_META_RECURRENCE_NOTIFICATION_ENABLED = "recurrence_enabled";
    public static final String ADMISSION_PERCENTAGES_META_ESTIMATION_MODE = "estimation_mode";
    public static final String ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT = "target_lesson_count";
    public static final String ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON = "target_assignments_per_lesson";
    /***********************************************************************************************
     * admission percentage data database start
     **********************************************************************************************/
    public static final String ADMISSION_PERCENTAGES_DATA_LESSON_ID = "lesson_id";
    public static final String ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID = "admission_percentage_id";
    public static final String ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS = "finished_assignments";
    public static final String ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS = "available_assignments";
    public static final String LAST_VIEWED_TIMESTAMP = "timestamp";
    public static final String LAST_VIEWED_SUBJECT_POSITION = "subject_id";
    public static final String LAST_VIEWED_PERCENTAGE_META_POSITION = "meta_id";
    private static final String DATABASE_NAME = "uebungen";
    //switched to db v13 in commit 22.10.15 13:49
    //db14: 8.3.16 14:22
    //db15: 9.3.16 20:58
    //db16: 17.3.16. 21:38
    private static final int DATABASE_VERSION = 16;

    //begin new database system
    private static final String CREATE_TABLE_SUBJECTS = "create table " + TABLE_NAME_SUBJECTS + "( " +
            SUBJECTS_ID + " integer primary key AUTOINCREMENT," + //autoincrement for more clearly defined behaviour
            SUBJECTS_NAME + " text not null);";

    private static final String CREATE_TABLE_ADMISSION_COUNTERS = "create table " + TABLE_NAME_ADMISSION_COUNTERS + "( " +
            ADMISSION_COUNTER_ID + " integer primary key," +
            ADMISSION_COUNTER_SUBJECT_ID + " integer, " +
            ADMISSION_COUNTER_COUNTER_NAME + " TEXT DEFAULT 'Vortragspunkte'," +
            ADMISSION_COUNTER_CURRENT + " integer not null," +
            ADMISSION_COUNTER_TARGET + " integer not null," +
            "FOREIGN KEY (" + ADMISSION_COUNTER_SUBJECT_ID + ") REFERENCES " + TABLE_NAME_SUBJECTS + "(" + SUBJECTS_ID + ") ON DELETE CASCADE" +
            ");";
    private static final String CREATE_TABLE_ADMISSION_PERCENTAGES_DATA =
            "create table " + TABLE_NAME_ADMISSION_PERCENTAGES_DATA + "( " +
            ADMISSION_PERCENTAGES_DATA_LESSON_ID + " integer not null," +
            ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS + " integer not null," +
            ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + " integer, " +
            ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS + " integer not null," +
            "FOREIGN KEY (" + ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + ") REFERENCES " + TABLE_NAME_ADMISSION_PERCENTAGES_META + "(" + ADMISSION_PERCENTAGES_META_ID + ") ON DELETE CASCADE, " +
            "PRIMARY KEY(" + ADMISSION_PERCENTAGES_DATA_LESSON_ID + ", " + ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + ")" + //primary key is meta id + lesson id
            ");";
    private static final String CREATE_TABLE_ADMISSION_PERCENTAGES_META = "create table " + TABLE_NAME_ADMISSION_PERCENTAGES_META + "( " +
            ADMISSION_PERCENTAGES_META_ID + " integer primary key, " +
            ADMISSION_PERCENTAGES_META_NAME + " text DEFAULT 'Votierungspunkte', " +
            ADMISSION_PERCENTAGES_META_RECURRENCE_DATA_STRING + " text DEFAULT NULL, " +
            ADMISSION_PERCENTAGES_META_SUBJECT_ID + " integer, " +
            ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON + " integer not null," +
            ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT + " integer not null," +
            ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE + " integer not null," +
            ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE + " integer not null default 50," +//v15
            ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED + " boolean not null DEFAULT 0 CHECK (" + ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED + " IN (0,1))," +//v15
            ADMISSION_PERCENTAGES_META_RECURRENCE_NOTIFICATION_ENABLED + " boolean not null DEFAULT 0 CHECK (" + ADMISSION_PERCENTAGES_META_RECURRENCE_NOTIFICATION_ENABLED + " IN (0,1))," +//v16
            ADMISSION_PERCENTAGES_META_ESTIMATION_MODE + " string NOT NULL DEFAULT 'user'," + //v14
            "FOREIGN KEY (" + ADMISSION_PERCENTAGES_META_SUBJECT_ID + ") REFERENCES " + TABLE_NAME_SUBJECTS + "(" + SUBJECTS_ID + ") ON DELETE CASCADE" +
            ");";

    private static final String CREATE_TABLE_LAST_VIEWED = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_LAST_VIEWED + "(" +
            LAST_VIEWED_PERCENTAGE_META_POSITION + " INTEGER, " +
            LAST_VIEWED_SUBJECT_POSITION + " INTEGER, " +
            LAST_VIEWED_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "PRIMARY KEY (" + LAST_VIEWED_PERCENTAGE_META_POSITION + ", " + LAST_VIEWED_SUBJECT_POSITION + ") " +
            ");";

    private static final String ON_UPDATE_UPDATE_TIMESTAMP =
            "CREATE TRIGGER update_time_trigger" +
                    "  AFTER UPDATE ON " + TABLE_NAME_LAST_VIEWED + " FOR EACH ROW" +
                    "  BEGIN " +
                    "UPDATE " + TABLE_NAME_LAST_VIEWED +
                    "  SET " + LAST_VIEWED_TIMESTAMP + " = current_timestamp" +
                    "  WHERE ROWID = old.ROWID;" +
                    "  END";

    private static DatabaseCreator mSingletonInstance = null;

    private DatabaseCreator(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static DatabaseCreator getInstance(Context context) {
        if (mSingletonInstance == null)
            mSingletonInstance = new DatabaseCreator(context);
        return mSingletonInstance;
    }

    /**
     * Creates the specified <code>toFile</code> as a byte for byte copy of the
     * <code>fromFile</code>. If <code>toFile</code> already exists, then it
     * will be replaced with a copy of <code>fromFile</code>. The name and path
     * of <code>toFile</code> will be that of <code>toFile</code>.<br/>
     * <br/>
     * <i> Note: <code>fromFile</code> and <code>toFile</code> will be closed by
     * this function.</i>
     *
     * @param fromFile - FileInputStream for the file to copy from.
     * @param toFile   - FileInputStream for the file to copy to.
     */
    private static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
        FileChannel fromChannel = null;
        FileChannel toChannel = null;
        try {
            fromChannel = fromFile.getChannel();
            toChannel = toFile.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }

    public static void logConstraintException(SQLiteConstraintException e) {
        Writer.appendLog("Foreign key constraint violated. Please search your backup file for" +
                "data violating the foreign key constraints. " + e.getMessage());
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.e("sql exc", "SQLiteConstraintException thrown!");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // obviously this needs to be executed, otherwise on delete cascade is weird
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_SUBJECTS);
        database.execSQL(CREATE_TABLE_LAST_VIEWED);
        database.execSQL(CREATE_TABLE_ADMISSION_COUNTERS);
        database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_META);
        database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_DATA);
        database.execSQL(ON_UPDATE_UPDATE_TIMESTAMP);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.w(DatabaseCreator.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which is basically pray'n'spray");
        // 12 to 13 creates all new tables using the newest table creation statements, so we only
        // need to execute that.
        if (oldVersion <= 12)
            transferFrom12To13(database);
        else {
            if (oldVersion <= 13)
                transferFrom13To14(database);

            if (oldVersion <= 14)
                transferFrom14To15(database);

            if (oldVersion <= 15)
                transferFrom15To16(database);
        }

    }

    private void transferFrom15To16(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE " + TABLE_NAME_ADMISSION_PERCENTAGES_META + " ADD COLUMN " +
                ADMISSION_PERCENTAGES_META_RECURRENCE_DATA_STRING + " text DEFAULT NULL");
        database.execSQL("ALTER TABLE " + TABLE_NAME_ADMISSION_PERCENTAGES_META + " ADD COLUMN " +
                ADMISSION_PERCENTAGES_META_RECURRENCE_NOTIFICATION_ENABLED + " boolean not null DEFAULT 0 CHECK (" + ADMISSION_PERCENTAGES_META_RECURRENCE_NOTIFICATION_ENABLED + " IN (0,1))");
    }

    private void transferFrom14To15(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE " + TABLE_NAME_ADMISSION_PERCENTAGES_META + " ADD COLUMN " +
                ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE + " integer not null default 50");
        database.execSQL("ALTER TABLE " + TABLE_NAME_ADMISSION_PERCENTAGES_META + " ADD COLUMN " +
                ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED + " boolean not null DEFAULT 0 CHECK (" + ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED + " IN (0,1))");
    }

    @SuppressLint("SQLiteString")
    private void transferFrom13To14(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE " + TABLE_NAME_ADMISSION_PERCENTAGES_META + " ADD COLUMN " +
                ADMISSION_PERCENTAGES_META_ESTIMATION_MODE + " STRING NOT NULL DEFAULT 'user'");
    }

    private void transferFrom12To13(SQLiteDatabase database) {
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.w(DatabaseCreator.class.getName(), "creating new databases for multiple counters, percentages, new subject db");
        database.execSQL(CREATE_TABLE_SUBJECTS);
        database.execSQL(CREATE_TABLE_ADMISSION_COUNTERS);
        database.execSQL(CREATE_TABLE_LAST_VIEWED);
        database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_META);
        database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_DATA);
        database.execSQL(ON_UPDATE_UPDATE_TIMESTAMP);

        //transfer subject information
        database.execSQL("INSERT INTO " + TABLE_NAME_SUBJECTS + "(" + SUBJECTS_ID + "," + SUBJECTS_NAME + ")" +
                " SELECT _id, uebung_name FROM uebungen_gruppen");

        //transfer counter information
        database.execSQL("INSERT INTO " + TABLE_NAME_ADMISSION_COUNTERS +
                "(" + ADMISSION_COUNTER_SUBJECT_ID + ","
                + ADMISSION_COUNTER_CURRENT + ","
                + ADMISSION_COUNTER_TARGET + ")" +
                " SELECT _id, uebung_prespoints, uebung_max_prespoints FROM uebungen_gruppen");

        //transfer percentage meta information
        database.execSQL("INSERT INTO " + TABLE_NAME_ADMISSION_PERCENTAGES_META +
                "(" + ADMISSION_PERCENTAGES_META_SUBJECT_ID + ","
                + ADMISSION_PERCENTAGES_META_ID + ","
                + ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON + ","
                + ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT + ","
                + ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE + ")" +
                " SELECT _id, _id, uebung_maxvotes_per_ueb, uebung_count, uebung_minvote FROM uebungen_gruppen");

        //transfer lessons
        database.execSQL("INSERT INTO " + TABLE_NAME_ADMISSION_PERCENTAGES_DATA +
                "(" + ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + ","
                + ADMISSION_PERCENTAGES_DATA_LESSON_ID + ","
                + ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS + ","
                + ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS + ")" +
                " SELECT typ_uebung, nummer_uebung, max_votierung, my_votierung FROM uebungen_eintraege");
    }

    public void reset() {
        getWritableDatabase().execSQL("DROP TABLE " + TABLE_NAME_LAST_VIEWED);
        getWritableDatabase().execSQL("DROP TABLE " + TABLE_NAME_SUBJECTS);
        getWritableDatabase().execSQL("DROP TABLE " + TABLE_NAME_ADMISSION_PERCENTAGES_META);
        getWritableDatabase().execSQL("DROP TABLE " + TABLE_NAME_ADMISSION_PERCENTAGES_DATA);
        getWritableDatabase().execSQL("DROP TABLE " + TABLE_NAME_ADMISSION_COUNTERS);
        onCreate(getWritableDatabase());
    }

    /**
     * Copies the database file at the specified location over the current
     * internal application database.
     * */
    public void importDatabase(String externalDbPath) throws IOException {
        String targetPath = getReadableDatabase().getPath();
        File source = new File(externalDbPath);
        File target = new File(targetPath);
        if (source.exists()) {
            copyFile(new FileInputStream(source), new FileOutputStream(target));
            // Access the copied database so SQLiteHelper will cache it and mark
            // it as created.
            try {
                getWritableDatabase();
            } catch (SQLiteConstraintException e) {
                logConstraintException(e);
                throw e;
            }
        }
        else
            throw new FileNotFoundException("could not find source file");
    }

    //http://stackoverflow.com/a/6542214

    public File getDbFileBackup(Context context) {
        File source = new File(getReadableDatabase().getPath());
        File targetDirectory = new File(context.getFilesDir().getPath() + "/rodb");
        removeOldBackups(targetDirectory);
        //noinspection ResultOfMethodCallIgnored
        targetDirectory.mkdirs();
        SimpleDateFormat timeStamp = new SimpleDateFormat("y_MM_d_k_m_s", Locale.getDefault());
        String name = "Votenote Backup " + timeStamp.format(Calendar.getInstance().getTime()) + ".db";
        File targetFile = new File(targetDirectory.getPath() + "/" + name);
        try {
            copyFile(new FileInputStream(source), new FileOutputStream(targetFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!targetFile.isFile())
            throw new AssertionError("wat (db bkp file no file)");
        return targetFile;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void removeOldBackups(File targetDirectory) {
        if (targetDirectory.exists() && targetDirectory.isDirectory()) {
            for (File f : targetDirectory.listFiles())
                f.delete();
        } else if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("rodb del", "no old dir found");
    }

    public void exportDatabase(String externalDbPath) throws IOException {
        String path = getReadableDatabase().getPath();
        File source = new File(path);
        File target = new File(externalDbPath);
        if (source.exists()) {
            copyFile(new FileInputStream(source), new FileOutputStream(target));
            getWritableDatabase();
        } else
            throw new FileNotFoundException("could not find source file");
    }
}