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
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;
import de.oerntec.votenote.MainActivity;

public class DBAdmissionPercentageData extends CrudDb<AdmissionPercentageData> {
    /**
     * Singleton instance
     */
    private static DBAdmissionPercentageData mInstance;

    private DBAdmissionPercentageData(Context context, String tableName) {
        super(context, tableName);
    }

    /**
     * Create the singleton object
     *
     * @param context context needed for creating the db access
     * @return the instance itself
     */
    public static DBAdmissionPercentageData setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBAdmissionPercentageData(context, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA);
        return (DBAdmissionPercentageData) mInstance;
    }

    /**
     * Singleton getter
     *
     * @return the singleton instance
     */
    public static DBAdmissionPercentageData getInstance() {
        return (DBAdmissionPercentageData) mInstance;
    }

    /**
     * Change all values except the database id in oldValues to newValues.
     *
     * @throws AssertionError if old dbId != new dbId
     */
    /*public void changeItem(AdmissionPercentageData oldValues, AdmissionPercentageData newItem) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ID, newItem.id);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID, newItem.admissionPercentageMetaId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID, newItem.lessonId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS, newItem.finishedAssignments);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS, newItem.availableAssignments);

        if ((oldValues.id != newItem.id))
            throw new AssertionError("id not same when changing");

        String[] whereArgs = {String.valueOf(oldValues.id)};
        int affectedRows = database.update(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA, values, DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 0)
                Log.i("AdmissionCounters", "!= 1 entry was changed, something is weird");
        if (affectedRows != 0) throw new AssertionError();
    }*/
    @Override
    public void changeItem(AdmissionPercentageData newItem) {
        ContentValues values = new ContentValues();
        //values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ID, newItem.id);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID, newItem.admissionPercentageMetaId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID, newItem.lessonId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS, newItem.finishedAssignments);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS, newItem.availableAssignments);

        String[] whereArgs = {String.valueOf(newItem.admissionPercentageMetaId), String.valueOf(newItem.lessonId)};
        int affectedRows = mDatabase.update(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA, values,
                DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + "=? AND " +
                        DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 0)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 0)
            throw new AssertionError("no item was changed");
    }

    /**
     * Get a single admission counter.
     *
     * @return AdmissionCounter object corresponding to the db values
     * @throws AssertionError if not exactly one AdmissionCounters are found
     */
    /*public AdmissionPercentageData getItem(int apMetaId, int lessonId) {
        String[] whereArgs = {String.valueOf(id)};
        Cursor c = mDatabase.query(true, DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA, null, DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ID + "=?", whereArgs, null, null, null, null);
        AdmissionPercentageData returnValue = null;
        if (c.getCount() != 1)
            throw new AssertionError("more than one item returned if we search via id is bad");
        if (c.moveToFirst())
            returnValue = new AdmissionPercentageData(
                    //c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS))
            );
        c.close();
        return returnValue;
    }*/
    @Deprecated
    public AdmissionPercentageData getItem(int lessonId, int apMetaId) {
        return getItem(new AdmissionPercentageData(apMetaId, lessonId, -1, -1));
    }

    @Override
    public AdmissionPercentageData getItem(AdmissionPercentageData item) {
        String[] whereArgs = {String.valueOf(item.admissionPercentageMetaId), String.valueOf(item.lessonId)};
        Cursor c = mDatabase.query(true,
                DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA,
                null,
                DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + "=? AND " +
                        DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID + "=?",
                whereArgs, null, null, null, null);
        AdmissionPercentageData returnValue = null;
        if (c.getCount() != 1)
            throw new AssertionError("!=1 id returned: " + c.getCount());
        if (c.moveToFirst())
            returnValue = new AdmissionPercentageData(
                    //c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS))
            );
        c.close();
        return returnValue;
    }


    /**
     * Get a all admission counters for a subject, ordered by their lesson id
     *
     * @param admissionPercentageMetaId what subject id are we looking for
     * @return list of objects corresponding to the db values
     */
    public List<AdmissionPercentageData> getItemsForMetaId(int admissionPercentageMetaId, boolean isLatestLessonFirst) {
        String[] whereArgs = {String.valueOf(admissionPercentageMetaId)};
        String order = isLatestLessonFirst ? " DESC" : " ASC";
        Cursor c = mDatabase.query(
                true,
                DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA,
                null,
                DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + "=?",
                whereArgs,
                null,
                null,
                DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID + order,
                null);

        List<AdmissionPercentageData> items = new LinkedList<>();

        while (c.moveToNext())
            items.add(new AdmissionPercentageData(
                    //c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS)),
                    c.getInt(c.getColumnIndexOrThrow(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS))
            ));

        c.close();
        return items;
    }

    /**
     * Delete a counter
     */
    public void deleteItem(AdmissionPercentageData item) {
        String[] whereArgs = new String[]{String.valueOf(item.admissionPercentageMetaId), String.valueOf(item.lessonId)};
        mDatabase.delete(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA, DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + "=? AND " +
                DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID + "=?", whereArgs);
    }

    /**
     * Add a new item
     *
     * @param newItem the item to add
     */
    public void addItem(AdmissionPercentageData newItem) {
        int lessonId = newItem.id;

        //get the next free lesson id if this is a new data item
        if (lessonId < 0) {
            //+1 because we want the lessons to start at 1, not at 0
            lessonId = getMaxLessonIdForAp(newItem.admissionPercentageMetaId) + 1;
        }

        //create values for insert or update
        ContentValues values = new ContentValues();

        //only put id if not new subject, eg restored item
        //TODO: solve removal with transaction, otherwise we need to increase the id of all following entries on re-insert
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID, newItem.admissionPercentageMetaId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID, lessonId);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_FINISHED_ASSIGNMENTS, newItem.finishedAssignments);
        values.put(DatabaseCreator.ADMISSION_PERCENTAGES_DATA_AVAILABLE_ASSIGNMENTS, newItem.availableAssignments);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBAP-M", "adding meta item");

        //try to insert the subject. since
        try {
            mDatabase.insert(DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA, null, values);
        } catch (SQLiteConstraintException e) {
            throw new AssertionError("not exactly one subject inserted");
        }
    }

    @Override
    public int addItemGetId(AdmissionPercentageData item) {
        addItem(item);
        return getMaxLessonIdForAp(item.admissionPercentageMetaId);
    }

    /**
     * gets the maximum lesson id an admission percentage counter has had yet
     *
     * @param apMetaId the admission percentage counter to look in
     * @return the maximum lesson id for apMetaId
     */
    private int getMaxLessonIdForAp(int apMetaId) {
        int result = 0;
        Cursor lessonIdCursor = mDatabase.rawQuery("SELECT MAX(" + DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID + ") " +
                "AS max_lesson_id FROM " + DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA +
                " WHERE " + DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + "=" + apMetaId, null);
        if (lessonIdCursor.getCount() > 0) {
            lessonIdCursor.moveToFirst();
            result = lessonIdCursor.getInt(lessonIdCursor.getColumnIndexOrThrow("max_lesson_id"));
        }
        lessonIdCursor.close();
        return result;
    }

    public AdmissionPercentageData getNewestItemForMetaId(int apMetaId) {
        int lessonId = getMaxLessonIdForAp(apMetaId);
        //todo: what to do if no lesson has been entered yet?
        return getItem(new AdmissionPercentageData(apMetaId, lessonId, -1, -1));
    }
}