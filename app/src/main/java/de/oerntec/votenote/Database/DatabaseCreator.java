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
package de.oerntec.votenote.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import de.oerntec.votenote.MainActivity;

public class DatabaseCreator extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "uebungen";

    //new
    public static final String TABLE_NAME_SUBJECTS = "subjects";
    public static final String TABLE_NAME_ADMISSION_COUNTERS = "admission_counters";
    public static final String TABLE_NAME_ADMISSION_PERCENTAGES_META = "admission_percentages_meta";
    public static final String TABLE_NAME_ADMISSION_PERCENTAGES_DATA = "admission_percentages_data";

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
    public static final String ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT = "target_lesson_count";
    public static final String ADMISSION_PERCENTAGES_META_TARGET_ASSIGNMENTS_PER_LESSON = "target_assignments_per_lesson";


    /***********************************************************************************************
     * admission percentage data database start
     **********************************************************************************************/
    public static final String ADMISSION_PERCENTAGES_DATA_ID = "_id";
    public static final String ADMISSION_PERCENTAGES_DATA_LESSON_ID = "lesson_id";
    public static final String ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID = "admission_percentage_id";
    public static final String ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS = "finished_assignments";
    public static final String ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS = "available_assignments";


    public static final int DATABASE_VERSION = 13;//switched to db v13 in commit 22.10.15 13:49

    //begin new database system
    private static final String CREATE_TABLE_SUBJECTS = "create table " + TABLE_NAME_SUBJECTS + "( " +
            SUBJECTS_ID + " integer primary key AUTOINCREMENT," + //autoincrement for more clearly defined behaviour
            SUBJECTS_NAME + " text not null UNIQUE);";

    private static final String CREATE_TABLE_ADMISSION_COUNTERS = "create table " + TABLE_NAME_ADMISSION_COUNTERS + "( " +
            ADMISSION_COUNTER_ID + " integer primary key," +
            ADMISSION_COUNTER_SUBJECT_ID + " integer, " +
            ADMISSION_COUNTER_COUNTER_NAME + " text not null," +
            ADMISSION_COUNTER_CURRENT + " integer not null," +
            ADMISSION_COUNTER_TARGET + " integer not null," +
            "FOREIGN KEY (" + ADMISSION_COUNTER_SUBJECT_ID + ") REFERENCES " + TABLE_NAME_SUBJECTS + "(" + SUBJECTS_ID + ")" +
            ");";

    private static final String CREATE_TABLE_ADMISSION_PERCENTAGES_DATA = "create table " + TABLE_NAME_ADMISSION_PERCENTAGES_DATA + "( " +
            ADMISSION_PERCENTAGES_DATA_LESSON_ID + " integer not null," +
            ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS + " integer not null," +
            ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + " integer, " +
            ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS + " integer not null," +
            "FOREIGN KEY (" + ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + ") REFERENCES " + TABLE_NAME_ADMISSION_PERCENTAGES_META + "(" + ADMISSION_PERCENTAGES_META_ID + "), " +
            "PRIMARY KEY(" + ADMISSION_PERCENTAGES_DATA_LESSON_ID + ", " + ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + ")" + //primary key is meta id + lesson id
            ");";

    private static final String CREATE_TABLE_ADMISSION_PERCENTAGES_META = "create table " + TABLE_NAME_ADMISSION_PERCENTAGES_META + "( " +
            ADMISSION_PERCENTAGES_META_ID + " integer primary key, " +
            ADMISSION_PERCENTAGES_META_NAME + " text not null, " +
            ADMISSION_PERCENTAGES_META_SUBJECT_ID + " integer, " +
            ADMISSION_PERCENTAGES_META_TARGET_ASSIGNMENTS_PER_LESSON + " integer not null," +
            ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT + " integer not null," +
            ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE + " integer not null," +
            "FOREIGN KEY (" + ADMISSION_PERCENTAGES_META_SUBJECT_ID + ") REFERENCES " + TABLE_NAME_SUBJECTS + "(" + SUBJECTS_ID + ")" +
            ");";

    public DatabaseCreator(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        //old database system
        //database.execSQL(CREATE_DATABASE_ENTRIES);
        //database.execSQL(CREATE_DATABASE_GROUPS);

        //new system
        database.execSQL(CREATE_TABLE_SUBJECTS);
        database.execSQL(CREATE_TABLE_ADMISSION_COUNTERS);
        database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_META);
        database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_DATA);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.w(DatabaseCreator.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        if (oldVersion == 12 && newVersion == 13) {
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.w(DatabaseCreator.class.getName(), "creating new databases for multiple counters, percentages, new subject db");
            database.execSQL(CREATE_TABLE_SUBJECTS);
            database.execSQL(CREATE_TABLE_ADMISSION_COUNTERS);
            database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_META);
            database.execSQL(CREATE_TABLE_ADMISSION_PERCENTAGES_DATA);
        }
    }

    public void reset(Context c) {
        c.deleteDatabase(DATABASE_NAME);
        onCreate(getWritableDatabase());
    }
}