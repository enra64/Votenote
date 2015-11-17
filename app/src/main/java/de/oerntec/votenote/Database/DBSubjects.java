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

public class DBSubjects {
    private static DBSubjects mInstance;
    private SQLiteDatabase database;

    private DBSubjects(Context context) {
        DatabaseCreator dbHelper = new DatabaseCreator(context);
        database = dbHelper.getWritableDatabase();
    }

    public static DBSubjects setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBSubjects(context);
        return mInstance;
    }

    public static DBSubjects getInstance() {
        return mInstance;
    }

    public Cursor getDataDump() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_GROUPS, null);
    }

    /**
     * Delete the group with the given name AND the given id
     * @return Number of affected rows.
     */
    public int deleteSubject(Subject delete) {
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("dbgroups:delete", "deleted " + delete.name + " at " + delete.id);
        String whereClause = DatabaseCreator.SUBJECTS_NAME + "=?" + " AND " + DatabaseCreator.SUBJECTS_ID + "=?";
        String[] whereArgs = new String[]{delete.name, String.valueOf(delete.id)};
        return database.delete(DatabaseCreator.TABLE_NAME_GROUPS, whereClause, whereArgs);
    }

    /**
     * Adds a group with the given Parameters
     * @param subjectName Name of the new Group
     * @return -1 if the group name is not unique (violates constraint), the id of the added group otherwise
     */
    public int addSubject(String subjectName) {
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_NAME, subjectName);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBGroups", "adding group");

        //try to insert the subject. since
        try{
            database.insert(DatabaseCreator.TABLE_NAME_SUBJECTS, null, values);
        } catch( SQLiteConstraintException e) {
            return -1;
        }

        //get maximum id value. because id is autoincrement, that must be the id of the subject we just added
        Cursor subjectIdCursor = database.rawQuery("SELECT MAX(" + DatabaseCreator.SUBJECTS_ID + ") FROM " + DatabaseCreator.TABLE_NAME_SUBJECTS, null);

        //throw error if we have more or less than one result row, because that is bullshit
        if(subjectIdCursor.getCount() != 1)
            throw new AssertionError("somehow we got more than one result with a max query?");

        //retrieve value and close cursor
        subjectIdCursor.moveToFirst();
        int result = subjectIdCursor.getInt(subjectIdCursor.getColumnIndex(DatabaseCreator.SUBJECTS_ID));
        subjectIdCursor.close();

        return result;
    }

    public int getNumberOfLessonsForSubject(int subjectId) {
        String data = DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_DATA;
        String meta = DatabaseCreator.TABLE_NAME_ADMISSION_PERCENTAGES_META;
        Cursor c = database.rawQuery(
                "SELECT " + data + "." + DatabaseCreator.ADMISSION_PERCENTAGES_DATA_LESSON_ID + ", " +
                        "FROM " + data +
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
        int affectedRows = database.update(DatabaseCreator.TABLE_NAME_SUBJECTS, values, DatabaseCreator.SUBJECTS_ID + "=?", whereArgs);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            if (affectedRows != 0)
                Log.i("AdmissionCounters", "No or more than one entry was changed, something is weird");
        if (affectedRows != 0) throw new AssertionError();
    }

    public List<Subject> getAllSubjects() {
        Cursor mCursor = database.query(true,
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


    public int getIdOfSubject(int position) {
        List<Subject> subjectList = getAllSubjects();
        if (position >= subjectList.size())
            throw new AssertionError("there are not that many subjects!");
        return subjectList.get(position).id;
    }

    public Subject getItem(int subjectId) {
        Cursor mCursor = database.query(
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

    /**
     * returns number of subjects
     */
    public int getCount() {
        Cursor c = getDataDump();
        int val = c.getCount();
        c.close();
        return val;
    }
}