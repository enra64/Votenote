package de.oerntec.votenote;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseCreator extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "uebungen";
    //database entries
    public static final String ENTRIES_ID = "_id";

    /* Database creation sql statement
     * We need
     * -key: _id
     * -which uebung_typ: typ_uebung
     * -which uebung_number: nummer_uebung
     * -maximum votierungs: max_votierung
     * -my votierungs: my_votierung
     */
    public static final String ENTRIES_TYP_UEBUNG = "typ_uebung";
    public static final String ENTRIES_NUMMER_UEBUNG = "nummer_uebung";
    public static final String ENTRIES_MAX_VOTES = "max_votierung";
    public static final String ENTRIES_MY_VOTES = "my_votierung";
    public static final String TABLE_NAME_ENTRIES = "uebungen_eintraege";
    //database groups
    public static final String SUBJECTS_ID = "_id";
    public static final String SUBJECTS_NAME = "uebung_name";
    public static final String SUBJECTS_MINIMUM_VOTE_PERCENTAGE = "uebung_minvote";
    public static final String SUBJECTS_CURRENT_PRESENTATION_POINTS = "uebung_prespoints";
    public static final String SUBJECTS_WANTED_PRESENTATION_POINTS = "uebung_max_prespoints";
    public static final String SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS = "uebung_count";
    public static final String SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON = "uebung_maxvotes_per_ueb";

    public static final String TABLE_NAME_SUBJECTS = "uebungen_gruppen";

    private static final int DATABASE_VERSION = 12;
    private static final String CREATE_DATABASE_ENTRIES =
            "create table " + TABLE_NAME_ENTRIES + "( " + ENTRIES_ID + " integer primary key," +
                    ENTRIES_TYP_UEBUNG + " int not null, " +
                    ENTRIES_NUMMER_UEBUNG + " integer not null," +
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

    public DatabaseCreator(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_DATABASE_ENTRIES);
        database.execSQL(CREATE_DATABASE_GROUPS);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(DatabaseCreator.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        if (newVersion == 12) {
            Log.i("database:creator", "changed to " + newVersion + " from " + oldVersion);
            database.execSQL("ALTER TABLE " + TABLE_NAME_SUBJECTS + " ADD " + DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS + " INTEGER DEFAULT 2");
        }
        if (oldVersion == 12) {
            database.execSQL("ALTER TABLE " + TABLE_NAME_SUBJECTS + " ADD " + SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS + " INTEGER DEFAULT 12");
            database.execSQL("ALTER TABLE " + TABLE_NAME_SUBJECTS + " ADD " + SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON + " INTEGER DEFAULT 5");
        }
    }
}