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
package de.oerntec.votenote.Database.TableHelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.oerntec.votenote.Database.DatabaseCreator;
import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.PojoDatabase;
import de.oerntec.votenote.MainActivity;

public class DBAdmissionCounters extends CrudDb<AdmissionCounter> implements PojoDatabase<AdmissionCounter> {
    /**
     * Private constructor for singleton
     */
    private DBAdmissionCounters(Context context, String tableName) {
        super(context, tableName);
    }

    /**
     * Create the singleton object
     *
     * @param context context needed for creating the db access
     * @return the instance itself
     */
    public static DBAdmissionCounters setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBAdmissionCounters(context, DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS);
        return (DBAdmissionCounters) mInstance;
    }

    /**
     * Singleton getter
     *
     * @return the singleton instance
     */
    public static DBAdmissionCounters getInstance() {
        return (DBAdmissionCounters) mInstance;
    }

    /**
     * Change all values except the database id in oldValues to newValues.
     *
     * @throws AssertionError if old dbId != new dbId
     */
    @Override
    public void changeItem(AdmissionCounter newValues) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_COUNTER_COUNTER_NAME, newValues.counterName);
        values.put(DatabaseCreator.ADMISSION_COUNTER_CURRENT, newValues.currentValue);
        values.put(DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID, newValues.subjectId);
        values.put(DatabaseCreator.ADMISSION_COUNTER_TARGET, newValues.targetValue);

        String[] whereArgs = {String.valueOf(newValues.id)};
        int affectedRows = mDatabase.update(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, values, DatabaseCreator.ADMISSION_COUNTER_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 1)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 1)
            throw new AssertionError("not exactly on item changed");
    }

    @Override
    public AdmissionCounter getItem(AdmissionCounter item) {
        return getItem(item.id);
    }

    /**
     * Get a single admission counter.
     *
     * @param id what id are we looking for
     * @return AdmissionCounter object corresponding to the db values
     * @throws AssertionError if not exactly one AdmissionCounter is found
     */
    @Deprecated
    public AdmissionCounter getItem(int id) {
        String[] whereArgs = {String.valueOf(id)};
        Cursor mCursor = mDatabase.query(true,
                DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS,
                null,
                DatabaseCreator.ADMISSION_COUNTER_ID + "=?",
                whereArgs, null, null, null, null);
        AdmissionCounter returnValue = null;
        //more than one item returned if we search via id is bad
        if (mCursor.getCount() != 1)
            throw new AssertionError("could not find item " + id);
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
        Cursor cursor = mDatabase.query(
                true,
                DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS,
                null,
                DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID + "=?",
                whereArgs,
                null,
                null,
                DatabaseCreator.ADMISSION_COUNTER_ID + " ASC", null);

        List<AdmissionCounter> counters = new LinkedList<>();

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

    @Override
    public void deleteItem(AdmissionCounter item) {
        deleteItem(item.id);
    }

    /**
     * Delete a counter
     * @param id the admission counter id to search for
     */
    @Deprecated
    public void deleteItem(int id) {
        String[] whereArgs = new String[]{String.valueOf(id)};
        mDatabase.delete(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, DatabaseCreator.ADMISSION_COUNTER_ID + "=?", whereArgs);
    }

    public void addItem(AdmissionCounter item) {
        addItemGetId(item);
    }

    /**
     * Add an admission counter into the table
     * @param newCounter the counter to add
     * @return false if not exactly one row was inserted
     */
    public int addItemGetId(AdmissionCounter newCounter) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_COUNTER_COUNTER_NAME, newCounter.counterName);
        values.put(DatabaseCreator.ADMISSION_COUNTER_CURRENT, newCounter.currentValue);
        values.put(DatabaseCreator.ADMISSION_COUNTER_SUBJECT_ID, newCounter.subjectId);
        values.put(DatabaseCreator.ADMISSION_COUNTER_TARGET, newCounter.targetValue);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBAC", "adding counter");

        //try to insert the subject. since
        try {
            mDatabase.insert(DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null, values);
        } catch (SQLiteConstraintException e) {
            return -1;
        }

        //get maximum id value. because id is autoincrement, that must be the id of the subject we just added
        Cursor idCursor = mDatabase.rawQuery("SELECT MAX(" + DatabaseCreator.ADMISSION_COUNTER_ID + ") AS max FROM " + DatabaseCreator.TABLE_NAME_ADMISSION_COUNTERS, null);

        //throw error if we have more or less than one result row, because that is bullshit
        if (idCursor.getCount() != 1)
            throw new AssertionError("somehow we got more than one result with a max query?");

        //retrieve value and close cursor
        idCursor.moveToFirst();
        int result = idCursor.getInt(idCursor.getColumnIndexOrThrow("max"));
        idCursor.close();

        return result;
    }
}