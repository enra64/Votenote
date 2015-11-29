package de.oerntec.votenote.Database;

import android.database.sqlite.SQLiteDatabase;

class OldDatabase{
        //old
        private static final String TABLE_NAME_ENTRIES = "uebungen_eintraege";
        private static final String TABLE_NAME_GROUPS = "uebungen_gruppen";

        /***********************************************************************************************
         * lesson database start
         ************************************************************************************************/
        private static final String ENTRIES_ID = "_id";
        private static final String ENTRIES_SUBJECT_ID = "typ_uebung";
        private static final String ENTRIES_LESSON_ID = "nummer_uebung";
        private static final String ENTRIES_MAX_VOTES = "max_votierung";
        private static final String ENTRIES_MY_VOTES = "my_votierung";


        /***********************************************************************************************
         * groups database start
         ************************************************************************************************/
        private static final String GROUPS_ID = "_id";
        private static final String GROUPS_NAME = "uebung_name";
        private static final String GROUPS_MINIMUM_VOTE_PERCENTAGE = "uebung_minvote";
        private static final String GROUPS_CURRENT_PRESENTATION_POINTS = "uebung_prespoints";
        private static final String GROUPS_WANTED_PRESENTATION_POINTS = "uebung_max_prespoints";
        private static final String GROUPS_SCHEDULED_NUMBER_OF_LESSONS = "uebung_count";
        private static final String GROUPS_SCHEDULED_ASSIGNMENTS_PER_LESSON = "uebung_maxvotes_per_ueb";

        //old system
        private static final String CREATE_DATABASE_ENTRIES =
                "create table " + TABLE_NAME_ENTRIES + "( " + ENTRIES_ID + " integer primary key," +
                        ENTRIES_SUBJECT_ID + " int not null, " +
                        ENTRIES_LESSON_ID + " integer not null," +
                        ENTRIES_MAX_VOTES + " integer not null," +
                        ENTRIES_MY_VOTES + " integer not null);";

        private static final String CREATE_DATABASE_GROUPS =
                "create table " + TABLE_NAME_GROUPS + "( " + GROUPS_ID + " integer primary key," +
                        GROUPS_NAME + " string not null," +
                        GROUPS_MINIMUM_VOTE_PERCENTAGE + " integer DEFAULT 50," +
                        GROUPS_CURRENT_PRESENTATION_POINTS + " integer DEFAULT 0," +
                        GROUPS_SCHEDULED_NUMBER_OF_LESSONS + " integer DEFAULT 12," +
                        GROUPS_SCHEDULED_ASSIGNMENTS_PER_LESSON + " integer DEFAULT 12," +
                        GROUPS_WANTED_PRESENTATION_POINTS + " integer DEFAULT 2);";
        
        static void recreateOldSystem(SQLiteDatabase database){
            database.execSQL(CREATE_DATABASE_ENTRIES);
            database.execSQL(CREATE_DATABASE_GROUPS);
        }
    }