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
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.oerntec.votenote.MainActivity;

public class DBAdmissionCounters {
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
    public void changeAdmissionCounter(AdmissionCounter oldValues, AdmissionCounter newValues) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_COUNTER_COUNTER_NAME, newValues.counterName);
        values.put(DatabaseCreator.ADMISSION_COUNTER_CURRENT, newValues.currentValue);
        values.put(DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID, newValues.subjectId);
        values.put(DatabaseCreator.ADMISSION_COUNTER_TARGET, newValues.targetValue);

        if ((oldValues.id != newValues.id)) throw new AssertionError();

        String[] whereArgs = {String.valueOf(oldValues.id)};
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
    public AdmissionCounter getAdmissionCounter(int id) {
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
     * Get a all admission counters for a subject
     *
     * @param subjectId what subject id are we looking for
     * @return AdmissionCounter object corresponding to the db values
     */
    public List<AdmissionCounter> getAdmissionCounters(int subjectId) {
        String[] whereArgs = {String.valueOf(subjectId)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null, DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID + "=?", whereArgs, null, null, null, null);

        List<AdmissionCounter> counters = new LinkedList<>();

        while (mCursor.moveToNext())
            counters.add(new AdmissionCounter(mCursor.getInt(0),
                    mCursor.getInt(1),
                    mCursor.getString(2),
                    mCursor.getInt(3),
                    mCursor.getInt(4)));

        mCursor.close();
        return counters;
    }

    public void deleteAllCounters(int subjectId) {
        String[] whereArgs = new String[]{String.valueOf(subjectId)};
        int checkValue =
                database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS,
                        DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("dbgroups:delete", "deleting all " + checkValue + " entries of type " + subjectId);
    }

    public void deleteCounter(int id) {
        String[] whereArgs = new String[]{String.valueOf(id)};
        int checkValue =
                database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS,
                        DatabaseCreator.ADMISSION_COUNTER_ID + "=?", whereArgs);
    }

    /**
     * Add an entry to the respective uebung, returns the lesson id
     */
    public boolean addAdmissionCounter(AdmissionCounter newCounter) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_COUNTER_COUNTER_NAME, newCounter.counterName);
        values.put(DatabaseCreator.ADMISSION_COUNTER_CURRENT, newCounter.currentValue);
        values.put(DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID, newCounter.subjectId);
        values.put(DatabaseCreator.ADMISSION_COUNTER_TARGET, newCounter.targetValue);

        return -1 != database.insert(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null, values);
    }

    public int getCount() {
        Cursor c = getAllData();
        int result = c.getCount();
        c.close();
        return result;
    }
}