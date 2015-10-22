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

    /***********************************************************************************************
     * lesson database start
     ************************************************************************************************/
    public static final String ENTRIES_ID = "_id";
    public static final String ENTRIES_SUBJECT_ID = "typ_uebung";
    public static final String ENTRIES_LESSON_ID = "nummer_uebung";
    public static final String ENTRIES_MAX_VOTES = "max_votierung";
    public static final String ENTRIES_MY_VOTES = "my_votierung";

    public static final String TABLE_NAME_ENTRIES = "uebungen_eintraege";
    /***********************************************************************************************
     lesson database end
     ************************************************************************************************/


    /***********************************************************************************************
     * subject database start
     ************************************************************************************************/
    public static final String SUBJECTS_ID = "_id";
    public static final String SUBJECTS_NAME = "uebung_name";
    public static final String SUBJECTS_MINIMUM_VOTE_PERCENTAGE = "uebung_minvote";
    public static final String SUBJECTS_CURRENT_PRESENTATION_POINTS = "uebung_prespoints";
    public static final String SUBJECTS_WANTED_PRESENTATION_POINTS = "uebung_max_prespoints";
    public static final String SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS = "uebung_count";
    public static final String SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON = "uebung_maxvotes_per_ueb";

    public static final String TABLE_NAME_SUBJECTS = "uebungen_gruppen";
    /***********************************************************************************************
     subject database end
     **********************************************************************************************/
    /***********************************************************************************************
     * admission counter database start
     **********************************************************************************************/
    public static final String ADMISSION_COUNTER_ID = "_id";
    public static final String ADMISSION_COUNTER_SUBJECT_ID = "subject_id";
    public static final String ADMISSION_COUNTER_COUNTER_NAME = "counter_name";
    public static final String ADMISSION_COUNTER_CURRENT = "current";
    public static final String ADMISSION_COUNTER_TARGET = "target";

    public static final String TABLE_NAME_ADMISSION_COUNTERS = "admission_counters";
    /***********************************************************************************************
     admission counter database end
     **********************************************************************************************/

    /***********************************************************************************************
     * admission percentage names database start
     **********************************************************************************************/
    public static final String ADMISSION_PERCENTAGES_NAMES_ID = "_id";
    public static final String ADMISSION_PERCENTAGES_NAMES_SUBJECT_ID = "subject_id";
    public static final String ADMISSION_PERCENTAGES_NAMES_NAME = "percentage_name";
    public static final String ADMISSION_PERCENTAGES_NAMES_TARGET_PERCENTAGE = "target_percentage";
    public static final String ADMISSION_PERCENTAGES_NAMES_TARGET_LESSON_COUNT = "target_lesson_count";
    public static final String ADMISSION_PERCENTAGES_NAMES_TARGET_ASSIGNMENTS_PER_LESSON = "target_assignments_per-lesson";

    public static final String TABLE_NAME_ADMISSION_PERCENTAGES_NAMES = "admission_percentages_names";
    /***********************************************************************************************
     admission percentage names database end
     **********************************************************************************************/

    /***********************************************************************************************
     * admission percentage data database start
     **********************************************************************************************/
    public static final String ADMISSION_PERCENTAGES_DATA_ID = "_id";
    public static final String ADMISSION_PERCENTAGES_DATA_LESSON_ID = "lesson_id";
    public static final String ADMISSION_PERCENTAGES_DATA_SUBJECT_ID = "subject_id";
    public static final String ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS = "finished_assignments";
    public static final String ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS = "available_assignments";

    public static final String TABLE_NAME_ADMISSION_PERCENTAGES_DATA = "admission_percentages_data";
    /***********************************************************************************************
     * admission percentage data database end
     **********************************************************************************************/

    public static final int DATABASE_VERSION = 13;//switched to db v13 in commit 22.10.15 13:49

    private static final String CREATE_DATABASE_ENTRIES =
            "create table " + TABLE_NAME_ENTRIES + "( " + ENTRIES_ID + " integer primary key," +
                    ENTRIES_SUBJECT_ID + " int not null, " +
                    ENTRIES_LESSON_ID + " integer not null," +
                    ENTRIES_MAX_VOTES + " integer not null," +
                    ENTRIES_MY_VOTES + " integer not null);";

    private static final String CREATE_DATABASE_GROUPS =
            "create table " + TABLE_NAME_SUBJECTS + "( " + SUBJECTS_ID + " integer primary key," +
                    SUBJECTS_NAME + " string not null," +
                    SUBJECTS_MINIMUM_VOTE_PERCENTAGE + " integer DEFAULT 50," +
                    SUBJECTS_CURRENT_PRESENTATION_POINTS + " integer DEFAULT 0," +
                    SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS + " integer DEFAULT 12," +
                    SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON + " integer DEFAULT 12," +
                    DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS + " integer DEFAULT 2);";

    private static final String CREATE_DATABASE_ADMISSION_COUNTERS =
            "create table " + TABLE_NAME_ADMISSION_COUNTERS + "( " + ADMISSION_COUNTER_ID + " integer primary key," +
                    ADMISSION_COUNTER_SUBJECT_ID + " integer not null," +
                    ADMISSION_COUNTER_COUNTER_NAME + " integer not null," +
                    ADMISSION_COUNTER_CURRENT + " integer not null," +
                    ADMISSION_COUNTER_TARGET + " integer not null);";

    private static final String CREATE_DATABASE_ADMISSION_PERCENTAGES_DATA =
            "create table " + TABLE_NAME_ADMISSION_PERCENTAGES_DATA + "( " + ADMISSION_PERCENTAGES_DATA_ID + " integer primary key," +
                    ADMISSION_PERCENTAGES_DATA_SUBJECT_ID + " integer not null," +
                    ADMISSION_PERCENTAGES_DATA_LESSON_ID + " integer not null," +
                    ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS + " integer not null," +
                    ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS + " integer not null);";

    private static final String CREATE_DATABASE_ADMISSION_PERCENTAGES_NAMES =
            "create table " + TABLE_NAME_ADMISSION_PERCENTAGES_NAMES + "( " + ADMISSION_PERCENTAGES_NAMES_ID + " integer primary key," +
                    ADMISSION_PERCENTAGES_NAMES_SUBJECT_ID + " integer not null," +
                    ADMISSION_PERCENTAGES_NAMES_NAME + " integer not null," +
                    ADMISSION_PERCENTAGES_NAMES_TARGET_ASSIGNMENTS_PER_LESSON + " integer not null," +
                    ADMISSION_PERCENTAGES_NAMES_TARGET_LESSON_COUNT + " integer not null," +
                    ADMISSION_PERCENTAGES_NAMES_TARGET_PERCENTAGE + " integer not null);";

    public DatabaseCreator(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        //old database system
        //database.execSQL(CREATE_DATABASE_ENTRIES);
        database.execSQL(CREATE_DATABASE_GROUPS);

        //new system
        database.execSQL(CREATE_DATABASE_ADMISSION_COUNTERS);
        database.execSQL(CREATE_DATABASE_ADMISSION_PERCENTAGES_NAMES);
        database.execSQL(CREATE_DATABASE_ADMISSION_PERCENTAGES_DATA);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.w(DatabaseCreator.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        if (oldVersion == 12 && newVersion == 13) {
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.w(DatabaseCreator.class.getName(), "creating new databases for multiple counters, percentages");
            database.execSQL(CREATE_DATABASE_ADMISSION_COUNTERS);
            database.execSQL(CREATE_DATABASE_ADMISSION_PERCENTAGES_NAMES);
            database.execSQL(CREATE_DATABASE_ADMISSION_PERCENTAGES_DATA);
        }
    }
}