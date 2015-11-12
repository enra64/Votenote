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

public class DBAdmissionPercentageMeta implements PojoDatabase<AdmissionPercentageMeta> {
    /**
     * Singleton instance
     */
    private static DBAdmissionPercentageMeta mInstance;

    /**
     * Database object used for accessing the database
     */
    private final SQLiteDatabase database;

    /**
     * Private constructor for singleton
     */
    private DBAdmissionPercentageMeta(Context context) {
        DatabaseCreator dbHelper = new DatabaseCreator(context);
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Create the singleton object
     *
     * @param context context needed for creating the db access
     * @return the instance itself
     */
    public static DBAdmissionPercentageMeta setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBAdmissionPercentageMeta(context);
        return mInstance;
    }

    /**
     * Singleton getter
     *
     * @return the singleton instance
     */
    public static DBAdmissionPercentageMeta getInstance() {
        return mInstance;
    }

    /**
     * Drop all lesson data
     */
    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, null);
    }

    /**
     * Get all data, only for export
     *
     * @return all lesson data
     */
    public Cursor getAllData() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, new String[0]);
    }

    /**
     * Change all values except the database id in oldValues to newValues.
     *
     * @throws AssertionError if old dbId != new dbId
     */
    public void changeItem(AdmissionPercentageMeta oldValues, AdmissionPercentageMeta newItem) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_ASSIGNMENTS_PER_LESSON, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT, newItem.name);

        if ((oldValues.id != newItem.id)) throw new AssertionError("id not same when changing");

        String[] whereArgs = {String.valueOf(oldValues.id)};
        int affectedRows = database.update(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, values, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 0)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 0) throw new AssertionError();
    }

    public void changeItem(AdmissionPercentageMeta newItem) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_ASSIGNMENTS_PER_LESSON, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT, newItem.name);

        String[] whereArgs = {String.valueOf(newItem.id)};
        int affectedRows = database.update(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, values, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 0)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 0) throw new AssertionError();
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
     * Get a single admission counter.
     *
     * @param id what id are we looking for
     * @return AdmissionCounter object corresponding to the db values
     * @throws AssertionError if not exactly one AdmissionCounters are found
     */
    public AdmissionPercentageMeta getItem(int id) {
        String[] whereArgs = {String.valueOf(id)};
        Cursor c = database.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + "=?", whereArgs, null, null, null, null);
        AdmissionPercentageMeta returnValue = null;
        if (c.getCount() != 1)
            throw new AssertionError("more than one item returned if we search via id is bad");
        if (c.moveToFirst())
            returnValue = new AdmissionPercentageMeta(
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_ASSIGNMENTS_PER_LESSON)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME))
            );
        c.close();
        return returnValue;
    }

    /**
     * Get a all admission counters for a subject, ordered by their id
     *
     * @param subjectId what subject id are we looking for
     * @return list of objects corresponding to the db values
     */
    public List<AdmissionPercentageMeta> getItemsForSubject(int subjectId) {
        String[] whereArgs = {String.valueOf(subjectId)};
        Cursor c = database.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID + "=?", whereArgs, null, null, DatabaseCreator.ADMISSION_COUNTER_ID + " ASC", null);

        List<AdmissionPercentageMeta> items = new LinkedList<>();

        while (c.moveToNext())
            items.add(new AdmissionPercentageMeta(
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_ASSIGNMENTS_PER_LESSON)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME))
            ));

        c.close();
        return items;
    }

    public void deleteItemsForSubject(int subjectId) {
        String[] whereArgs = new String[]{String.valueOf(subjectId)};
        int checkValue =
                database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META,
                        DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("dbgroups:delete", "deleting all " + checkValue + " entries of type " + subjectId);
    }

    /**
     * Delete a counter
     *
     * @param id the admission counter id to search for
     */
    public void deleteItem(int id) {
        String[] whereArgs = new String[]{String.valueOf(id)};
        database.delete(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + "=?", whereArgs);
    }


    /**
     * Add a new item
     *
     * @param newItem the item to add
     * @return -1 if a constraint was violated, new object row id otherwise
     */
    public int addItem(AdmissionPercentageMeta newItem) {
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_ASSIGNMENTS_PER_LESSON, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT, newItem.name);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBAP-M", "adding meta item");

        //try to insert the subject. since
        try {
            database.insert(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, values);
        } catch (SQLiteConstraintException e) {
            return -1;
        }

        //get maximum id value. because id is autoincrement, that must be the id of the subject we just added
        Cursor idCursor = database.rawQuery("SELECT MAX(" + DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + ") FROM " + DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null);

        //throw error if we have more or less than one result row, because that is bullshit
        if (idCursor.getCount() != 1)
            throw new AssertionError("somehow we got more than one result with a max query?");

        //retrieve value and close cursor
        idCursor.moveToFirst();
        int result = idCursor.getInt(idCursor.getColumnIndex(DatabaseCreator.ADMISSION_PERCENTAGES_META_ID));
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