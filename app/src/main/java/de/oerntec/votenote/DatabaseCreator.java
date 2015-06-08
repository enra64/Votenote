package de.oerntec.votenote;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseCreator extends SQLiteOpenHelper {
    private static final String DATABASE = "uebungen";
    
    private static final int DATABASE_VERSION = 12;

    /* Database creation sql statement
     * We need
     * -key: _id
     * -which uebung_typ: typ_uebung
     * -which uebung_number: nummer_uebung
     * -maximum votierungs: max_votierung
     * -my votierungs: my_votierung
     */
    
    //database entries
    public static final String ENTRIES_ID="_id";
    public static final String ENTRIES_TYP_UEBUNG="typ_uebung";
    public static final String ENTRIES_NUMMER_UEBUNG="nummer_uebung";
    public static final String ENTRIES_MAX_VOTES="max_votierung";
    public static final String ENTRIES_MY_VOTES="my_votierung";

    public static final String TABLE_NAME_ENTRIES="uebungen_eintraege";
    
    private static final String CREATE_DATABASE_ENTRIES = 
    		"create table "+TABLE_NAME_ENTRIES+"( "+ENTRIES_ID+" integer primary key," +
    		ENTRIES_TYP_UEBUNG+" int not null, " +
            ENTRIES_NUMMER_UEBUNG+" integer not null," +
    		ENTRIES_MAX_VOTES+" integer not null," +
    		ENTRIES_MY_VOTES+" integer not null);";
    
    //database groups
    public static final String GROUPS_ID="_id";
    public static final String GROUPS_NAMEN="uebung_name";
    public static final String GROUPS_MIN_VOTE="uebung_minvote";
    public static final String GROUPS_PRESENTATIONPOINTS="uebung_prespoints";
    public static final String UEBUNG_MINIMUM_PRESENTATION_POINTS_COLUMN ="uebung_max_prespoints";
    public static final String GROUPS_SCHEDULED_NUMBER_OF_LESSONS ="uebung_count";
    public static final String GROUPS_SCHEDULED_MAXIMUM_VOTIERUNG_PER_LESSON ="uebung_maxvotes_per_ueb";
    
    public static final String TABLE_NAME_GROUPS="uebungen_gruppen";
    
    private static final String CREATE_DATABASE_GROUPS = 
    		"create table "+TABLE_NAME_GROUPS+"( "+GROUPS_ID+" integer primary key," +
    		GROUPS_NAMEN+" string not null," +
    		GROUPS_MIN_VOTE+" integer DEFAULT 50,"+
    		GROUPS_PRESENTATIONPOINTS + " integer DEFAULT 0,"+
                    GROUPS_SCHEDULED_NUMBER_OF_LESSONS + " integer DEFAULT 12,"+
                    GROUPS_SCHEDULED_MAXIMUM_VOTIERUNG_PER_LESSON + " integer DEFAULT 12,"+
                    DatabaseCreator.UEBUNG_MINIMUM_PRESENTATION_POINTS_COLUMN + " integer DEFAULT 2);";

    public DatabaseCreator(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
    	database.execSQL(CREATE_DATABASE_ENTRIES);
    	database.execSQL(CREATE_DATABASE_GROUPS);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion){
        Log.w(DatabaseCreator.class.getName(),
                         "Upgrading database from version " + oldVersion + " to "
                         + newVersion + ", which will destroy all old data");
        if(newVersion==12){
        	Log.i("database:creator", "changed to "+newVersion+" from "+oldVersion);
        	database.execSQL("ALTER TABLE "+TABLE_NAME_GROUPS+" ADD "+ DatabaseCreator.UEBUNG_MINIMUM_PRESENTATION_POINTS_COLUMN +" INTEGER DEFAULT 2");
        }
        if(oldVersion==12){
        	database.execSQL("ALTER TABLE "+TABLE_NAME_GROUPS+" ADD "+ GROUPS_SCHEDULED_NUMBER_OF_LESSONS +" INTEGER DEFAULT 12");
        	database.execSQL("ALTER TABLE "+TABLE_NAME_GROUPS+" ADD "+ GROUPS_SCHEDULED_MAXIMUM_VOTIERUNG_PER_LESSON +" INTEGER DEFAULT 5");
        }
    }
}