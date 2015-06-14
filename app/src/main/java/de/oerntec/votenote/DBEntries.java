package de.oerntec.votenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBEntries {
    private static DBEntries mInstance;
    private DatabaseCreator dbHelper;
    private SQLiteDatabase database;

    private DBEntries(Context context) {
        dbHelper = new DatabaseCreator(context);
        database = dbHelper.getWritableDatabase();
    }

    public static DBEntries setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBEntries(context);
        return mInstance;
    }

    public static DBEntries getInstance() {
        return mInstance;
    }

    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_ENTRIES, null, null);
    }

    public Cursor getAllData() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_ENTRIES, new String[0]);
    }

    public void changeEntry(int uebungTyp, int uebungNummer, int maxVote, int myVote) {
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ENTRIES_MAX_VOTES, maxVote);
        values.put(DatabaseCreator.ENTRIES_MY_VOTES, myVote);

        String[] whereArgs = {String.valueOf(uebungTyp), String.valueOf(uebungNummer)};
        int affectedRows = database.update(DatabaseCreator.TABLE_NAME_ENTRIES, values, DatabaseCreator.ENTRIES_LESSON_ID + "=?" + " AND " + DatabaseCreator.ENTRIES_NUMMER_UEBUNG + "=?", whereArgs);
        Log.i("dbentries:changeentry", "changed " + affectedRows + " entries");
    }


    public Cursor getEntryCursor(int uebungTyp, int uebungNummer) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_MY_VOTES, DatabaseCreator.ENTRIES_MAX_VOTES, DatabaseCreator.ENTRIES_ID};
        String[] whereArgs = {String.valueOf(uebungTyp), String.valueOf(uebungNummer)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols, DatabaseCreator.ENTRIES_LESSON_ID + "=?" + " AND " + DatabaseCreator.ENTRIES_NUMMER_UEBUNG + "=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public void deleteAllEntriesForGroup(int groupId) {
        String[] whereArgs = new String[]{String.valueOf(groupId)};
        int checkValue = database.delete(DatabaseCreator.TABLE_NAME_ENTRIES, DatabaseCreator.ENTRIES_LESSON_ID + "=?", whereArgs);
        Log.i("dbgroups:delete", "deleting all " + checkValue + " entries of type " + groupId);
    }

    /**
     * Add an entry to the respective uebung
     */
    public void addEntry(int uebungTyp, int maxVote, int myVote) {
        Cursor lastEntryNummerCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, new String[]{DatabaseCreator.ENTRIES_NUMMER_UEBUNG}, DatabaseCreator.ENTRIES_LESSON_ID + "=" + uebungTyp, null, null, null, DatabaseCreator.ENTRIES_NUMMER_UEBUNG + " DESC", null);
        //init cursor
        int lastNummer = 1;

        if (lastEntryNummerCursor.getCount() > 0)
            lastNummer = lastEntryNummerCursor.getInt(0) + 1;

        lastEntryNummerCursor.close();
        addEntry(uebungTyp, maxVote, myVote, lastNummer);
    }

    /**
     * Add an entry to the respective uebung
     */
    public void addEntry(int uebungTyp, int maxVote, int myVote, int uebungNummer) {
        Log.i("db:entries:add", "adding entry with lastnummer" + uebungNummer + " for group " + uebungTyp);

        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ENTRIES_LESSON_ID, uebungTyp);
        values.put(DatabaseCreator.ENTRIES_NUMMER_UEBUNG, uebungNummer);
        values.put(DatabaseCreator.ENTRIES_MAX_VOTES, maxVote);
        values.put(DatabaseCreator.ENTRIES_MY_VOTES, myVote);

        database.insert(DatabaseCreator.TABLE_NAME_ENTRIES, null, values);
    }

    /**
     * remove the entry and decrease the uebung_nummer for all following entries
     */
    public void removeEntry(int uebungID, int uebungNummer) {
        //remove correct entry
        database.delete(DatabaseCreator.TABLE_NAME_ENTRIES, DatabaseCreator.ENTRIES_LESSON_ID + "=" + uebungID + " AND " + DatabaseCreator.ENTRIES_NUMMER_UEBUNG + "=" + uebungNummer, null);
        //decrease uebungnummer for all following entries
        String query = "UPDATE " + DatabaseCreator.TABLE_NAME_ENTRIES + " SET " + DatabaseCreator.ENTRIES_NUMMER_UEBUNG + " = " + DatabaseCreator.ENTRIES_NUMMER_UEBUNG + " - 1 " +
                "WHERE " + DatabaseCreator.ENTRIES_LESSON_ID + " = ? AND " + DatabaseCreator.ENTRIES_NUMMER_UEBUNG + " > ?";
        database.execSQL(query, new String[]{String.valueOf(uebungID), String.valueOf(uebungNummer)});
    }

    /**
     * The previously given maximum vote value, or the scheduled assignment count if no previous entry exists
     *
     * @param groupID id of the group tp get a valuie for
     * @return maximum possible vote
     */
    public int getPrevMaxVote(int groupID) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_ID, DatabaseCreator.ENTRIES_MAX_VOTES};
        String[] whereArgs = new String[]{String.valueOf(groupID)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols, DatabaseCreator.ENTRIES_LESSON_ID + "=?", whereArgs, null, null, DatabaseCreator.ENTRIES_NUMMER_UEBUNG + " DESC", null);
        //init standard return
        int returnValue = DBGroups.getInstance().getScheduledAssignmentsPerUebung(groupID);
        if (mCursor != null) {
            //sorted by descending, so first value is highest uebung nummer
            mCursor.moveToFirst();
            if (mCursor.getCount() != 0)
                returnValue = mCursor.getInt(1);
            mCursor.close();
        }
        return returnValue;
    }

    public int getPrevVote(int groupID) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_ID, DatabaseCreator.ENTRIES_MY_VOTES};
        String[] whereArgs = new String[]{String.valueOf(groupID)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols, DatabaseCreator.ENTRIES_LESSON_ID + "=?", whereArgs, null, null, DatabaseCreator.ENTRIES_NUMMER_UEBUNG + " DESC", "LIMIT 1");
        //init standard return
        int returnValue = 3;
        if (mCursor != null) {
            //sorted by descending, so first value is highest uebung nummer
            mCursor.moveToFirst();
            if (mCursor.getCount() != 0)
                returnValue = mCursor.getInt(1);
            mCursor.close();
        }
        return returnValue;
    }

    public int getGroupRecordCount(int groupType) {
        Cursor mCursor = getGroupRecords(groupType);
        int answer = mCursor.getCount();
        mCursor.close();
        return answer; // iterate to get each value.
    }

    /**
     * Return a cursor for the selected group_type
     * {DatabaseCreator.ENTRIES_NUMMER_UEBUNG, DatabaseCreator.ENTRIES_MY_VOTES, DatabaseCreator.ENTRIES_MAX_VOTES, DatabaseCreator.ENTRIES_ID}
     *
     * @param groupType The ID or Type of the inquired Group
     * @return the cursor
     */
    public Cursor getGroupRecords(int groupType) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_NUMMER_UEBUNG, DatabaseCreator.ENTRIES_MY_VOTES, DatabaseCreator.ENTRIES_MAX_VOTES, DatabaseCreator.ENTRIES_ID};
        String[] whereArgs = new String[]{String.valueOf(groupType)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols, DatabaseCreator.ENTRIES_LESSON_ID + "=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public int getCompletedAssignmentCount(int groupID) {
        Cursor cursor = database.rawQuery(
                "SELECT SUM(" + DatabaseCreator.ENTRIES_MY_VOTES + ") FROM " + DatabaseCreator.TABLE_NAME_ENTRIES + " WHERE " + DatabaseCreator.ENTRIES_LESSON_ID + "=" + groupID, null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return -1;
    }
}