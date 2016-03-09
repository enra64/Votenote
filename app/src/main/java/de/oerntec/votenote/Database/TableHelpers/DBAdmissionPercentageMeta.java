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
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.PojoDatabase;
import de.oerntec.votenote.MainActivity;

public class DBAdmissionPercentageMeta extends CrudDb<AdmissionPercentageMeta> implements PojoDatabase<AdmissionPercentageMeta> {
    /**
     * Singleton instance
     */
    private static DBAdmissionPercentageMeta mInstance;

    /**
     * Private constructor for singleton
     */
    private DBAdmissionPercentageMeta(Context context, String tableName) {
        super(context, tableName);
    }

    /**
     * Create the singleton object
     *
     * @param context context needed for creating the db access
     * @return the instance itself
     */
    public static DBAdmissionPercentageMeta setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBAdmissionPercentageMeta(context, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META);
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

    @Override
    public void changeItem(AdmissionPercentageMeta newItem) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID, newItem.subjectId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON, newItem.userAssignmentsPerLessonEstimation);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE, newItem.baselineTargetPercentage);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT, newItem.estimatedLessonCount);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_ESTIMATION_MODE, newItem.getEstimationModeAsString());

        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE, newItem.bonusTargetPercentage);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED, newItem.bonusTargetPercentageEnabled);

        String[] whereArgs = {String.valueOf(newItem.id)};
        int affectedRows = mDatabase.update(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, values, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 1)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 1)
            throw new AssertionError("not exactly one row changed");
    }

    @Override
    public AdmissionPercentageMeta getItem(AdmissionPercentageMeta idItem) {
        return getItem(idItem.id);
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
        Cursor c = mDatabase.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + "=?", whereArgs, null, null, null, null);
        AdmissionPercentageMeta returnValue = null;
        if (c.getCount() != 1)
            throw new AssertionError("more than one item returned if we search via id is bad, found " + c.getCount());
        if (c.moveToFirst())
            returnValue = new AdmissionPercentageMeta(
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ESTIMATION_MODE)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE)),
                    getBoolean(c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED)))
            );
        c.close();
        return returnValue;
    }

    private boolean getBoolean(int bool) {
        if (bool != 0 && bool != 1)
            throw new AssertionError("boolean does not work as i expected");
        return bool != 0;
    }

    /**
     * Get a all admission counters for a subject, ordered by their id
     *
     * @param subjectId what subject id are we looking for
     * @return list of objects corresponding to the db values
     */
    public List<AdmissionPercentageMeta> getItemsForSubject(int subjectId) {
        String[] whereArgs = {String.valueOf(subjectId)};
        Cursor c = mDatabase.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID + "=?",
                whereArgs, null, null, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + " ASC", null);

        List<AdmissionPercentageMeta> items = new LinkedList<>();

        while (c.moveToNext())
            items.add(new AdmissionPercentageMeta(
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ESTIMATION_MODE)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE)),
                    getBoolean(c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED)))
            ));

        c.close();
        return items;
    }

    /**
     * Delete a counter
     *
     * @param id the admission counter id to search for
     */
    @Deprecated
    public void deleteItem(int id) {
        String[] whereArgs = new String[]{String.valueOf(id)};
        int delCount = mDatabase.delete(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + "=?", whereArgs);
        if (delCount > 1)
            throw new AssertionError("deleted more than one item!");
        else if (delCount == 0)
            throw new AssertionError("could not find item to delete!");
    }

    @Override
    public void deleteItem(AdmissionPercentageMeta item) {
        deleteItem(item.id);
    }

    @Override
    public void addItem(AdmissionPercentageMeta item) {
        addItemGetId(item);
    }

    /**
     * Add a new item
     *
     * @param newItem the item to add
     * @return -1 if a constraint was violated, new object row id otherwise
     */
    @Override
    public int addItemGetId(AdmissionPercentageMeta newItem) {
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME, newItem.name);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID, newItem.subjectId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON, newItem.userAssignmentsPerLessonEstimation);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE, newItem.baselineTargetPercentage);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT, newItem.estimatedLessonCount);

        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_ESTIMATION_MODE, newItem.getEstimationModeAsString());

        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE, newItem.bonusTargetPercentage);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED, newItem.bonusTargetPercentageEnabled);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBAP-M", "adding meta item");

        //try to insert the subject. since
        try {
            mDatabase.insert(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, values);
        } catch (SQLiteConstraintException e) {
            return -1;
        }

        //get maximum id value. because id is autoincrement, that must be the id of the subject we just added
        Cursor idCursor = mDatabase.rawQuery("SELECT MAX(" + DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + ") AS max FROM " + DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null);

        //throw error if we have more or less than one result row, because that is bullshit
        if (idCursor.getCount() != 1)
            throw new AssertionError("somehow we got more than one result with a max query?");

        //retrieve value and close cursor
        idCursor.moveToFirst();
        int result = idCursor.getInt(idCursor.getColumnIndexOrThrow("max"));
        idCursor.close();

        return result;
    }

    public List<AdmissionPercentageMeta> getAllItems() {
        Cursor c = mDatabase.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META, null, null,
                null, null, null, DatabaseCreator.ADMISSION_PERCENTAGES_META_ID + " ASC", null);

        List<AdmissionPercentageMeta> items = new LinkedList<>();

        while (c.moveToNext())
            items.add(new AdmissionPercentageMeta(
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_USER_ESTIMATED_ASSIGNMENTS_PER_LESSON)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_LESSON_COUNT)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_TARGET_PERCENTAGE)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_NAME)),
                    c.getString(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_ESTIMATION_MODE)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE)),
                    getBoolean(c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_META_BONUS_TARGET_PERCENTAGE_ENABLED)))
            ));

        c.close();
        return items;
    }
}