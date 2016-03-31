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
package de.oerntec.votenote.database.tablehelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.database.DatabaseCreator;
import de.oerntec.votenote.database.pojo.Subject;

public class DBSubjects extends CrudDb<Subject> {
    /**
     * Singleton instance
     */
    private static DBSubjects mInstance;

    private DBSubjects(Context context, String tableName) {
        super(context, tableName);
    }

    public static DBSubjects setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBSubjects(context, DatabaseCreator.TABLE_NAME_SUBJECTS);
        return mInstance;
    }

    public static DBSubjects getInstance() {
        return mInstance;
    }

    @Override
    public void addItem(Subject item) {
        throw new Error("not implemented");
    }

    public void addItemWithId(Subject item){
        addItemGetId(item, true);
    }

    public void deleteItem(Subject delete) {
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("dbsubjects", "deleted " + delete.name + " at " + delete.id);
        String whereClause = DatabaseCreator.SUBJECTS_NAME + "=?" + " AND " + DatabaseCreator.SUBJECTS_ID + "=?";
        String[] whereArgs = new String[]{delete.name, String.valueOf(delete.id)};
        int delCount = mDatabase.delete(DatabaseCreator.TABLE_NAME_SUBJECTS, whereClause, whereArgs);
        if (delCount != 1)
            throw new AssertionError("deleted more than 1?");
    }

    /**
     * Add a subject to the database without forcing the id
     */
    public int addItemGetId(Subject item){
        return addItemGetId(item, false);
    }

    /**
     * add a subject to the database
     *
     * @param item    the item to be added
     * @param forceId whether or not the database id should be the one of the object given
     * @return the id of the new subject
     * @see #addItem(Subject)
     */
    public int addItemGetId(Subject item, boolean forceId) {
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_NAME, item.name);
        if(forceId)
            values.put(DatabaseCreator.SUBJECTS_ID, item.id);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBGroups", "adding group");

        //try to insert the subject. since
        try{
            mDatabase.insert(DatabaseCreator.TABLE_NAME_SUBJECTS, null, values);
        } catch( SQLiteConstraintException e) {
            return -1;
        }

        // if the id is forced, this must be the id
        if (forceId)
            return item.id;

        //get maximum id value. because id is autoincrement, that must be the id of the subject we just added
        Cursor subjectIdCursor = mDatabase.rawQuery("SELECT MAX(" + DatabaseCreator.SUBJECTS_ID + ") AS maximum_subject_id FROM " + DatabaseCreator.TABLE_NAME_SUBJECTS, null);

        //throw error if we have more or less than one result row, because that is bullshit
        if(subjectIdCursor.getCount() != 1)
            throw new AssertionError(subjectIdCursor.getCount() + " items in max cursor?");

        //retrieve value and close cursor
        subjectIdCursor.moveToFirst();
        int result = subjectIdCursor.getInt(subjectIdCursor.getColumnIndexOrThrow("maximum_subject_id"));
        subjectIdCursor.close();

        return result;
    }

    /**
     * Adds a group with the given Parameters
     * @param subjectName Name of the new Group
     * @return -1 if the group name is not unique (violates constraint), the id of the added group otherwise
     */
    public int addItemGetId(String subjectName) {
        return addItemGetId(new Subject(subjectName, -1));
    }

    public int getNumberOfLessonsForSubject(int subjectId) {
        String data = DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA;
        String meta = DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META;
        Cursor c = mDatabase.rawQuery(
                "SELECT " + data + "." + DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID +
                        " FROM " + data +
                        " INNER JOIN " + meta +
                        " ON " + data + "." + DatabaseCreator.ADMISSION_PERCENTAGES_DATA_ADMISSION_PERCENTAGE_ID + "=" + meta + "." + DatabaseCreator.ADMISSION_PERCENTAGES_META_ID +
                        " WHERE " + meta + "." + DatabaseCreator.ADMISSION_PERCENTAGES_META_SUBJECT_ID + "=?", new String[]{String.valueOf(subjectId)});
        int number = c.getCount();
        c.close();
        return number;
    }

    public void changeItem(Subject newItem) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_NAME, newItem.name);

        String[] whereArgs = {String.valueOf(newItem.id)};
        int affectedRows = mDatabase.update(DatabaseCreator.TABLE_NAME_SUBJECTS, values, DatabaseCreator.SUBJECTS_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 1)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 1)
            throw new AssertionError("not exactly one group was changed");
    }

    public List<Subject> getAllSubjects() {
        Cursor mCursor = mDatabase.query(true,
                DatabaseCreator.TABLE_NAME_SUBJECTS,
                null,
                null,
                null,
                null,
                null,
                DatabaseCreator.ADMISSION_COUNTER_ID + " ASC",
                null);

        List<Subject> subjects = new LinkedList<>();

        while (mCursor.moveToNext())
            subjects.add(new Subject(
                    mCursor.getString(mCursor.getColumnIndex(DatabaseCreator.SUBJECTS_NAME)),
                    mCursor.getInt(mCursor.getColumnIndex(DatabaseCreator.SUBJECTS_ID))
            ));

        mCursor.close();
        return subjects;
    }

    public boolean isEmpty() {
        return getAllSubjects().size() == 0;
    }


    public int getIdOfSubject(int position) {
        List<Subject> subjectList = getAllSubjects();
        if (position >= subjectList.size())
            throw new AssertionError("there are not that many subjects!");
        return subjectList.get(position).id;
    }

    public Subject getItem(Subject item) {
        return getItem(item.id);
    }

    public Subject getItem(int subjectId) {
        Cursor mCursor = mDatabase.query(
                true,
                DatabaseCreator.TABLE_NAME_SUBJECTS,
                null,
                DatabaseCreator.SUBJECTS_ID + "=" + subjectId,
                null,
                null,
                null,
                DatabaseCreator.ADMISSION_COUNTER_ID + " ASC",
                null);

        if (mCursor.getCount() != 1)
            throw new AssertionError("id is primary key?");

        mCursor.moveToFirst();

        Subject result = new Subject(
                mCursor.getString(mCursor.getColumnIndex(DatabaseCreator.SUBJECTS_NAME)),
                mCursor.getInt(mCursor.getColumnIndex(DatabaseCreator.SUBJECTS_ID)));

        mCursor.close();
        return result;
    }
}