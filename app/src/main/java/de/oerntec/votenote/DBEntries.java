package de.oerntec.votenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBEntries{

    public final static String TABLE=DatabaseCreator.TABLE_NAME_ENTRIES; // name of table
    public final static String ID_COLUMN = DatabaseCreator.ENTRIES_ID; // id value for employee
    public final static String UEBUNG_TYP_ID_COLUMN = DatabaseCreator.ENTRIES_TYP_UEBUNG;  //name of the uebung

    /* Database creation sql statement from database helper
     * We need
     * -key: _id
     * -which uebung_typ: typ_uebung
     * -which uebung_number: nummer_uebung
     * -maximum votierungs: max_votierung
     * -my votierungs: my_votierung
     */
    public final static String UEBUNG_NUMMER_COLUMN=DatabaseCreator.ENTRIES_NUMMER_UEBUNG;  //which iteration of the uebung is it
    public final static String MAX_VOTE_NUMBER_COLUMN=DatabaseCreator.ENTRIES_MAX_VOTES;  //max possible vote count
    public final static String MY_VOTE_NUMBER_COLUMN=DatabaseCreator.ENTRIES_MY_VOTES;  //my vote count
    private static DBEntries mInstance;
    private DatabaseCreator dbHelper;
    private SQLiteDatabase database;

    private DBEntries(Context context){
        dbHelper = new DatabaseCreator(context);
        database = dbHelper.getWritableDatabase();
    }

    public static DBEntries setupInstance(Context context){
        if(mInstance == null)
            mInstance = new DBEntries(context);
        return mInstance;
    }

    public static DBEntries getInstance(){
        return mInstance;
    }

    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_ENTRIES, null, null);
    }

    /**
     * Adds a group to the Table, if it does not exist;
     * if a group with the given name exists, we change it.
     */
    public void changeOrAddEntry(String uebungTyp, int maxVote, int myVote){
        //get a cursor with all entries
        String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_ID_COLUMN, UEBUNG_TYP_ID_COLUMN};
        Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, null, null);

        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(UEBUNG_TYP_ID_COLUMN, uebungTyp);
        values.put(UEBUNG_NUMMER_COLUMN, 12);
        values.put(MAX_VOTE_NUMBER_COLUMN, maxVote);
        values.put(MY_VOTE_NUMBER_COLUMN, myVote);

        //name already exists->update
        if(mCursor.getCount()==1){
            int existingId=mCursor.getInt(0);
            String[] whereArgs={String.valueOf(existingId)};
            database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
        }
        //insert name, because it does not exist yet
        else
            database.insert(TABLE, null, values);
        //mandatory cursorclosing
        mCursor.close();
    }

    public Cursor getAllData() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_ENTRIES, new String[0]);
    }

    public void changeEntry(int uebungTyp, int uebungNummer, int maxVote, int myVote){
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(MAX_VOTE_NUMBER_COLUMN, maxVote);
        values.put(MY_VOTE_NUMBER_COLUMN, myVote);

        String[] whereArgs={String.valueOf(uebungTyp), String.valueOf(uebungNummer)};
        int affectedRows=database.update(TABLE, values, UEBUNG_TYP_ID_COLUMN +"=?"+" AND "+UEBUNG_NUMMER_COLUMN+"=?", whereArgs);
        Log.i("dbentries:changeentry", "changed "+affectedRows+" entries");
    }

    /**
     *
     * @param uebungTyp Type of the uebung concerned
     * @param uebungNummer Nummer of the entry
     * @return A cursor containing myvote, maxvote and id
     */
    public Cursor getEntry(int uebungTyp, int uebungNummer){
        String[] cols = new String[] {MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN};
        String[] whereArgs={String.valueOf(uebungTyp), String.valueOf(uebungNummer)};
        Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_ID_COLUMN + "=?" + " AND " + UEBUNG_NUMMER_COLUMN + "=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public void deleteAllEntriesForGroup(int groupId) {
        String[] whereArgs = new String[] {String.valueOf(groupId)};
        int checkValue=database.delete(TABLE, UEBUNG_TYP_ID_COLUMN +"=?" , whereArgs);
        Log.i("dbgroups:delete", "deleting all " + checkValue + " entries of type " + groupId);
    }

    /**
     * Add an entry to the respective uebung
     */
    public void addEntry(int uebungTyp, int maxVote, int myVote) {
        Cursor lastEntryNummerCursor = database.query(true, TABLE, new String[]{UEBUNG_NUMMER_COLUMN}, UEBUNG_TYP_ID_COLUMN + "=" + uebungTyp, null, null, null, UEBUNG_NUMMER_COLUMN + " DESC", null);
        //init cursor
        int lastNummer = 1;
        if (lastEntryNummerCursor != null) {
            if (!lastEntryNummerCursor.moveToFirst())
                Log.w("db:groups", "empty cursor");
        }
        if (lastEntryNummerCursor.getCount() != 0)
            lastNummer = lastEntryNummerCursor.getInt(0) + 1;

        addEntry(uebungTyp, maxVote, myVote, lastNummer);
    }

    /**
     * Add an entry to the respective uebung
     */
    public void addEntry(int uebungTyp, int maxVote, int myVote, int uebungNummer) {
        Log.i("db:entries:add", "adding entry with lastnummer" + uebungNummer + " for group " + uebungTyp);

        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(UEBUNG_TYP_ID_COLUMN, uebungTyp);
        values.put(UEBUNG_NUMMER_COLUMN, uebungNummer);
        values.put(MAX_VOTE_NUMBER_COLUMN, maxVote);
        values.put(MY_VOTE_NUMBER_COLUMN, myVote);

        database.insert(TABLE, null, values);
    }

    /**
     * Return a cursor for all entries
     * @return the cursor
     */
    public Cursor getAllRecords(){
        String[] wantedColumns = new String[] {UEBUNG_NUMMER_COLUMN, MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN};
        Cursor mCursor = database.query(true, TABLE, wantedColumns, null, null, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    /**
     * Set the deleted flag for the given entry
     */
    public void removeEntry(int uebungID, int uebungNummer){
        //remove correct entry
        database.delete(TABLE, UEBUNG_TYP_ID_COLUMN + "=" + uebungID + " AND " + UEBUNG_NUMMER_COLUMN + "=" + uebungNummer, null);
        //decrease uebungnummer for all following entries
        String query = "UPDATE "+TABLE+" SET "+UEBUNG_NUMMER_COLUMN+" = "+UEBUNG_NUMMER_COLUMN+" - 1 " +
                "WHERE "+ UEBUNG_TYP_ID_COLUMN + " = ? AND "+UEBUNG_NUMMER_COLUMN+" > ?";
        database.execSQL(query, new String[]{String.valueOf(uebungID), String.valueOf(uebungNummer)});
        //UPDATE uebungen_eintraege SET nummer_uebung = nummer_uebung-1 WHERE typ_uebung=1 AND nummer_uebung>3
    }

    public int getPrevMaxVote(int groupID){
        String[] cols = new String[] {ID_COLUMN, MAX_VOTE_NUMBER_COLUMN};
        String[] whereArgs = new String[] {String.valueOf(groupID)};
        Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_ID_COLUMN +"=?", whereArgs, null, null, UEBUNG_NUMMER_COLUMN+" DESC", null);
        //init standard return
        int returnValue=10;
        if (mCursor != null){
            //sorted by descending, so first value is highest uebung nummer
            mCursor.moveToFirst();
            if(mCursor.getCount()!=0)
                returnValue= mCursor.getInt(1);
        }
        mCursor.close();
        return returnValue;
    }

    public int getPrevVote(int groupID){
        String[] cols = new String[] {ID_COLUMN, MY_VOTE_NUMBER_COLUMN};
        String[] whereArgs = new String[] {String.valueOf(groupID)};
        Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_ID_COLUMN +"=?", whereArgs, null, null, UEBUNG_NUMMER_COLUMN+" DESC", null);
        //init standard return
        int returnValue=3;
        if (mCursor != null){
            //sorted by descending, so first value is highest uebung nummer
            mCursor.moveToFirst();
            if(mCursor.getCount()!=0)
                returnValue= mCursor.getInt(1);
        }
        mCursor.close();
        return returnValue;
    }

    public int getGroupRecordCount(int groupType){
        Cursor mCursor = getGroupRecords(groupType);
        int answer = mCursor.getCount();
        mCursor.close();
        return answer; // iterate to get each value.
    }

    /**
     * Return a cursor for the selected group_type
     * {UEBUNG_NUMMER_COLUMN, MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN}
     * @param groupType The ID or Type of the inquired Group
     * @return the cursor
     */
    public Cursor getGroupRecords(int groupType){
        String[] cols = new String[] {UEBUNG_NUMMER_COLUMN, MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN};
        String[] whereArgs = new String[] {String.valueOf(groupType)};
        Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_ID_COLUMN +"=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public int getCompletedAssignmentCount(int groupID){
        Cursor cursor = database.rawQuery(
                "SELECT SUM("+MY_VOTE_NUMBER_COLUMN+") FROM "+TABLE+" WHERE "+ UEBUNG_TYP_ID_COLUMN +"="+groupID, null);
        if(cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return -1;
    }

    public int getElapsedAssignmentCount(int groupID){
        Cursor cursor = database.rawQuery(
                "SELECT SUM("+MAX_VOTE_NUMBER_COLUMN+") FROM "+TABLE+" WHERE "+ UEBUNG_TYP_ID_COLUMN +"="+groupID, null);
        if(cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return -1;
    }
}