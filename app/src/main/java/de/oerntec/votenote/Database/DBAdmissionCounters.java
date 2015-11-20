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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.oerntec.votenote.MainActivity;

public class DBAdmissionCounters implements PojoDatabase<AdmissionCounter> {
    /**
     * Singleton instance
     */
    private static DBAdmissionCounters mInstance;

    /**
     * Database object used for accessing the database
     */
    private final SQLiteDatabase database;

    /**
     * Private constructor for singleton
     */
    private DBAdmissionCounters(Context context) {
        DatabaseCreator dbHelper = new DatabaseCreator(context);
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Create the singleton object
     *
     * @param context context needed for creating the db access
     * @return the instance itself
     */
    public static DBAdmissionCounters setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBAdmissionCounters(context);
        return mInstance;
    }

    /**
     * Singleton getter
     *
     * @return the singleton instance
     */
    public static DBAdmissionCounters getInstance() {
        return mInstance;
    }

    /**
     * Drop all lesson data
     */
    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null, null);
    }

    /**
     * Get all data, only for export
     *
     * @return all lesson data
     */
    public Cursor getAllData() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, new String[0]);
    }

    /**
     * Change all values except the database id in oldValues to newValues.
     *
     * @throws AssertionError if old dbId != new dbId
     */
    public void changeItem(AdmissionCounter newValues) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_COUNTER_COUNTER_NAME, newValues.counterName);
        values.put(DatabaseCreator.ADMISSION_COUNTER_CURRENT, newValues.currentValue);
        values.put(DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID, newValues.subjectId);
        values.put(DatabaseCreator.ADMISSION_COUNTER_TARGET, newValues.targetValue);

        String[] whereArgs = {String.valueOf(newValues.id)};
        int affectedRows = database.update(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, values, DatabaseCreator.ADMISSION_COUNTER_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 0)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 0) throw new AssertionError();
    }

    /**
     * Get a single admission counter.
     *
     * @param id what id are we looking for
     * @return AdmissionCounter object corresponding to the db values
     * @throws AssertionError if not exactly one AdmissionCounters are found
     */
    public AdmissionCounter getItem(int id) {
        String[] whereArgs = {String.valueOf(id)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null, DatabaseCreator.ADMISSION_COUNTER_ID + "=?", whereArgs, null, null, null, null);
        AdmissionCounter returnValue = null;
        //more than one item returned if we search via id is bad
        if (mCursor.getCount() != 1) throw new AssertionError();
        if (mCursor.moveToFirst())
            returnValue = new AdmissionCounter(mCursor.getInt(0),
                    mCursor.getInt(1),
                    mCursor.getString(2),
                    mCursor.getInt(3),
                    mCursor.getInt(4));
        mCursor.close();
        return returnValue;
    }

    /**
     * Get a all admission counters for a subject, ordered by their id
     *
     * @param subjectId what subject id are we looking for
     * @return AdmissionCounter object corresponding to the db values
     */
    public List<AdmissionCounter> getItemsForSubject(int subjectId) {
        String[] whereArgs = {String.valueOf(subjectId)};
        Cursor cursor = database.query(
                true,
                DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS,
                null,
                DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID + "=?",
                whereArgs,
                null,
                null,
                DatabaseCreator.ADMISSION_COUNTER_ID + " ASC", null);

        List<AdmissionCounter> counters = new LinkedList<>();

        //avoid endless loop if the count is zero
        if (cursor.getCount() <= 0) {
            cursor.close();
            return counters;
        }

        //endlosschleife
        while (cursor.moveToNext())
            counters.add(new AdmissionCounter(cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getString(2),
                    cursor.getInt(3),
                    cursor.getInt(4)));

        cursor.close();
        return counters;
    }

    public void deleteItemsForSubject(int subjectId) {
        String[] whereArgs = new String[]{String.valueOf(subjectId)};
        int checkValue =
                database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS,
                        DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("dbgroups:delete", "deleting all " + checkValue + " entries of type " + subjectId);
    }

    /**
     * Delete a counter
     * @param id the admission counter id to search for
     */
    public void deleteItem(int id) {
        String[] whereArgs = new String[]{String.valueOf(id)};
        database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, DatabaseCreator.ADMISSION_COUNTER_ID + "=?", whereArgs);
    }

    public void createSavepoint(String id) {
        database.execSQL(";SAVEPOINT " + id);
    }

    public void rollbackToSavepoint(String id) {
        database.execSQL(";ROLLBACK TO SAVEPOINT " + id);
    }

    public void releaseSavePoint(String id) {
        database.execSQL(";RELEASE SAVEPOINT " + id);
    }

    /**
     * Add an admission counter into the table
     * @param newCounter the counter to add
     * @return false if not exactly one row was inserted
     */
    public int addItem(AdmissionCounter newCounter) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_COUNTER_COUNTER_NAME, newCounter.counterName);
        values.put(DatabaseCreator.ADMISSION_COUNTER_CURRENT, newCounter.currentValue);
        values.put(DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID, newCounter.subjectId);
        values.put(DatabaseCreator.ADMISSION_COUNTER_TARGET, newCounter.targetValue);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBAC", "adding counter");

        //try to insert the subject. since
        try {
            database.insert(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null, values);
        } catch (SQLiteConstraintException e) {
            return -1;
        }

        //get maximum id value. because id is autoincrement, that must be the id of the subject we just added
        Cursor idCursor = database.rawQuery("SELECT MAX(" + DatabaseCreator.ADMISSION_COUNTER_ID + ") FROM " + DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null);

        //throw error if we have more or less than one result row, because that is bullshit
        if (idCursor.getCount() != 1)
            throw new AssertionError("somehow we got more than one result with a max query?");

        //retrieve value and close cursor
        idCursor.moveToFirst();
        int result = idCursor.getInt(idCursor.getColumnIndex(DatabaseCreator.ADMISSION_COUNTER_ID));
        idCursor.close();

        return result;
    }

    public int getCount() {
        Cursor c = getAllData();
        int result = c.getCount();
        c.close();
        return result;
    }
}