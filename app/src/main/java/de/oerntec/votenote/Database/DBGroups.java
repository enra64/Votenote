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

import java.util.ArrayList;
import java.util.List;

import de.oerntec.votenote.MainActivity;

public class DBGroups {
    public final static int NO_GROUPS_EXIST = -1;

    private static DBGroups mInstance;
    private SQLiteDatabase database;

    private DBGroups(Context context) {
        DatabaseCreator dbHelper = new DatabaseCreator(context);
        database = dbHelper.getWritableDatabase();

    }

    public static DBGroups setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBGroups(context);
        return mInstance;
    }

    public static DBGroups getInstance() {
        return mInstance;
    }

    public Cursor getDataDump() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_GROUPS, new String[0]);
    }

    /**
     * Delete the group with the given name AND the given id
     * @return Number of affected rows.
     */
    public int deleteSubject(Group delete) {
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("dbgroups:delete", "deleted " + delete.subjectName + " at " + delete.id);
        String whereClause = DatabaseCreator.GROUPS_NAME + "=?" + " AND " + DatabaseCreator.GROUPS_ID + "=?";
        String[] whereArgs = new String[]{delete.subjectName, String.valueOf(delete.id)};
        return database.delete(DatabaseCreator.TABLE_NAME_GROUPS, whereClause, whereArgs);
    }

    /**
     * Adds a group with the given Parameters
     *
     * @param groupName Name of the new Group
     * @param minVot    minimum vote
     * @param minPres   minimum presentation points
     * @return -1 if group exists, 1 else.
     */
    public int addGroup(String groupName, int minVot, int minPres, int newScheduledUebungCount, int newScheduledAssignmentsPerUebung) {
        return addGroup(-1, groupName, minVot, minPres, -1,
                newScheduledUebungCount, newScheduledAssignmentsPerUebung);
    }

    public int addGroup(Group val) {
        return addGroup(
                Integer.valueOf(val.id),
                val.subjectName,
                Integer.valueOf(val.subjectMinimumVotePercentage),
                Integer.valueOf(val.subjectWantedPresentationPoints),
                Integer.valueOf(val.subjectCurrentPresentationPoints),
                Integer.valueOf(val.subjectScheduledLessonCount),
                Integer.valueOf(val.subjectScheduledAssignmentsPerLesson));
    }

    /**
     * Adds a group with the given Parameters
     * @param id Id, overwrites rowid if >0
     * @param groupName Name of the new Group
     * @param minVot    minimum vote
     * @param minPres   minimum presentation points
     * @return -1 if group exists, 1 else.
     */
    public int addGroup(int id, String groupName, int minVot, int minPres, int currentPres,
                        int newScheduledLessonCount, int newScheduledAssignmentsPerUebung) {
        //check whether group name exists; abort if it does
        String[] testColumns = new String[]{DatabaseCreator.GROUPS_ID, DatabaseCreator.GROUPS_NAME};
        Cursor testCursor = database.query(true, DatabaseCreator.TABLE_NAME_GROUPS, testColumns, DatabaseCreator.GROUPS_NAME + "=?", new String[]{groupName}, null, null, DatabaseCreator.GROUPS_ID + " DESC", null);

        //abort if group already exists
        if (testCursor.getCount() > 0) {
            testCursor.close();
            return -1;
        }
        testCursor.close();

        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.GROUPS_NAME, groupName);
        values.put(DatabaseCreator.GROUPS_MINIMUM_VOTE_PERCENTAGE, minVot);
        if (currentPres > 0)
            values.put(DatabaseCreator.GROUPS_CURRENT_PRESENTATION_POINTS, currentPres);
        if (id > 0)
            values.put(DatabaseCreator.GROUPS_ID, id);
        values.put(DatabaseCreator.GROUPS_WANTED_PRESENTATION_POINTS, minPres);
        values.put(DatabaseCreator.GROUPS_SCHEDULED_NUMBER_OF_LESSONS, newScheduledLessonCount);
        values.put(DatabaseCreator.GROUPS_SCHEDULED_ASSIGNMENTS_PER_LESSON, newScheduledAssignmentsPerUebung);

        //insert name, because it does not exist yet
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("DBGroups", "adding group");
        database.insert(DatabaseCreator.TABLE_NAME_GROUPS, null, values);
        return 1;
    }

    /**
     * This function returns the id of the group that is displayed at drawerselection in the drawer
     *
     * @param drawerSelection position in the drawer. _not_ the database id
     * @return the database id corresponding to the position
     */
    public int translatePositionToID(int drawerSelection) {
        Cursor groups = getAllGroupNames();
        if (groups.getCount() == 0)
            return NO_GROUPS_EXIST;
        groups.moveToPosition(drawerSelection);
        int translatedSection = groups.getInt(0);
        groups.close();
        return translatedSection;
    }

    /**
     * returns number of subjects
     */
    public int getCount() {
        Cursor c = getAllGroupNames();
        int val = c.getCount();
        c.close();
        return val;
    }

    /**
     * Return a cursor containing all Groups sorted by id desc;
     * sequence: ID, NAME, MINVOTE, MINPRES
     *
     * @return the cursor
     */
    public Cursor getAllGroupNames() {
        //sort cursor by name to have a defined order
        String[] cols = new String[]{DatabaseCreator.GROUPS_ID,
                DatabaseCreator.GROUPS_NAME,
                DatabaseCreator.GROUPS_MINIMUM_VOTE_PERCENTAGE,
                DatabaseCreator.GROUPS_WANTED_PRESENTATION_POINTS};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_GROUPS, cols, null, null, null, null, DatabaseCreator.GROUPS_ID + " DESC", null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public Cursor getAllButOneGroupNames(int excludedID) {
        //sort cursor by name to have a defined order
        String[] cols = new String[]{DatabaseCreator.GROUPS_ID, DatabaseCreator.GROUPS_NAME};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_GROUPS, cols, DatabaseCreator.GROUPS_ID + "!=?", new String[]{excludedID + ""}, null, null, DatabaseCreator.GROUPS_ID + " DESC", null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public int getNumberOfSubjects() {
        //sort cursor by name to have a defined order
        String[] cols = new String[]{DatabaseCreator.GROUPS_ID};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_GROUPS, cols, null, null, null, null, DatabaseCreator.GROUPS_ID + " DESC", null);
        int answer = mCursor.getCount();
        mCursor.close();
        return answer; // iterate to get each value.
    }

    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_GROUPS, null, null);
    }

    /**
     * returns a cursor with all
     */
    public Cursor getAllGroupsInfos() {
        //sort cursor by name to have a defined reihenfolg
        String[] cols = new String[]{DatabaseCreator.GROUPS_ID,
                DatabaseCreator.GROUPS_NAME,
                DatabaseCreator.GROUPS_MINIMUM_VOTE_PERCENTAGE,
                DatabaseCreator.GROUPS_WANTED_PRESENTATION_POINTS,
                DatabaseCreator.GROUPS_SCHEDULED_NUMBER_OF_LESSONS,
                DatabaseCreator.GROUPS_SCHEDULED_ASSIGNMENTS_PER_LESSON};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_GROUPS, cols, null, null, null, null, DatabaseCreator.GROUPS_ID + " DESC", null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public List<Group> getAllSubjects() {
        String[] cols = new String[]{DatabaseCreator.GROUPS_ID,//0
                DatabaseCreator.GROUPS_NAME,//1
                DatabaseCreator.GROUPS_MINIMUM_VOTE_PERCENTAGE,//2
                DatabaseCreator.GROUPS_CURRENT_PRESENTATION_POINTS,//3
                DatabaseCreator.GROUPS_WANTED_PRESENTATION_POINTS,//4
                DatabaseCreator.GROUPS_SCHEDULED_NUMBER_OF_LESSONS,//5
                DatabaseCreator.GROUPS_SCHEDULED_ASSIGNMENTS_PER_LESSON};//6
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_GROUPS, cols, null, null, null, null, null, null);
        ArrayList<Group> resultList = new ArrayList<>();
        while (mCursor.moveToNext()) {
            resultList.add(new Group(mCursor.getString(0), //id
                    mCursor.getString(1), //name
                    mCursor.getString(2), //minvote
                    mCursor.getString(3), //current prespoints
                    mCursor.getString(5), //sched lesson count
                    mCursor.getString(6), //sched assignments per lesson
                    mCursor.getString(4))); //wanted prespoints
        }
        mCursor.close();
        return resultList;
    }

    /**
     * Get specific group data
     *
     * @param databaseId db id of the group
     * @return data. null when no group was found
     */
    public Group getSubject(int databaseId) {
        String[] cols = new String[]{DatabaseCreator.GROUPS_ID,//0
                DatabaseCreator.GROUPS_NAME,//1
                DatabaseCreator.GROUPS_MINIMUM_VOTE_PERCENTAGE,//2
                DatabaseCreator.GROUPS_CURRENT_PRESENTATION_POINTS,//3
                DatabaseCreator.GROUPS_WANTED_PRESENTATION_POINTS,//4
                DatabaseCreator.GROUPS_SCHEDULED_NUMBER_OF_LESSONS,//5
                DatabaseCreator.GROUPS_SCHEDULED_ASSIGNMENTS_PER_LESSON};//6
        String[] whereArgs = new String[]{String.valueOf(databaseId)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_GROUPS, cols, DatabaseCreator.GROUPS_ID + "=?", whereArgs, null, null, null, null);
        if (mCursor.getCount() > 0)
            mCursor.moveToFirst();
        else
            return null;
        Group returnValue = new Group(mCursor.getString(0), //id
                mCursor.getString(1), //name
                mCursor.getString(2), //minvote
                mCursor.getString(3), //current prespoints
                mCursor.getString(5), //sched lesson count
                mCursor.getString(6), //sched assignments per lesson
                mCursor.getString(4)); //wanted prespoints
        mCursor.close();
        return returnValue;
    }

    /**
     * Returns the amount of assignments you could go to at maximum
     *
     * @return the cursor
     */
    public int getScheduledWork(int id) {
        Group val = getSubject(id);
        return val == null ? -1 : Integer.valueOf(val.subjectScheduledLessonCount) * Integer.valueOf(val.subjectScheduledAssignmentsPerLesson);
    }

    /**
     * Returns the entered amount of uebung instances
     *
     * @param dbID ID of the concerned Group
     * @return see above
     */
    public int getScheduledNumberOfLessons(int dbID) {
        Group val = getSubject(dbID);
        return val == null ? -1 : Integer.valueOf(val.subjectScheduledLessonCount);
    }

    /**
     * Returns the entered amount estimated assignments per uebung
     *
     * @param dbID ID of the concerned Group
     * @return see above
     */
    public int getScheduledAssignmentsPerLesson(int dbID) {
        Group val = getSubject(dbID);
        return val == null ? -1 : Integer.valueOf(val.subjectScheduledAssignmentsPerLesson);
    }

    /**
     * Return the name of the group with the specified id
     */
    public String getGroupName(int dbID) {
        Group val = getSubject(dbID);
        return val == null ? "null" : val.subjectName;
    }


    /**
     * Returns the minimum Vote needed for passing from db
     *
     * @param dbID ID of group
     * @return minvote
     */
    public int getMinVote(int dbID) {
        Group val = getSubject(dbID);
        return val == null ? -1 : Integer.valueOf(val.subjectMinimumVotePercentage);
    }

    /**
     * set the presentation points
     *
     * @param dbID       Database id of group to change
     * @param presPoints presentation points the user has
     * @return The amount of Rows updated, should be one
     */
    public int setPresPoints(int dbID, int presPoints) {
        String[] whereArgs = {String.valueOf(dbID)};
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.GROUPS_CURRENT_PRESENTATION_POINTS, presPoints);
        return database.update(DatabaseCreator.TABLE_NAME_GROUPS, values, DatabaseCreator.GROUPS_ID + "=?", whereArgs);
    }

    /**
     * Helper function for getting the pres points for the specified group
     *
     * @param dbID the database id of the group
     * @return Prespoint number
     */
    public int getPresPoints(int dbID) {
        Group val = getSubject(dbID);
        return val == null ? -1 : Integer.valueOf(val.subjectCurrentPresentationPoints);
    }

    public int changeSubject(Group oldSubject, Group newSubject) {
        String[] whereArgs = {String.valueOf(oldSubject.id)};
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.GROUPS_NAME, newSubject.subjectName);
        values.put(DatabaseCreator.GROUPS_MINIMUM_VOTE_PERCENTAGE, newSubject.subjectMinimumVotePercentage);
        values.put(DatabaseCreator.GROUPS_WANTED_PRESENTATION_POINTS, newSubject.subjectWantedPresentationPoints);
        values.put(DatabaseCreator.GROUPS_SCHEDULED_NUMBER_OF_LESSONS, newSubject.subjectScheduledLessonCount);
        values.put(DatabaseCreator.GROUPS_SCHEDULED_ASSIGNMENTS_PER_LESSON, newSubject.subjectScheduledAssignmentsPerLesson);
        return database.update(DatabaseCreator.TABLE_NAME_GROUPS, values, DatabaseCreator.GROUPS_ID + "=?", whereArgs);
    }

    /**
     * Get the minimum prespoints for the given group
     *
     * @param dbID ID of the Group
     * @return Minimum number of Prespoints needed for passing, -1 on null
     */
    public int getWantedPresPoints(int dbID) {
        Group val = getSubject(dbID);
        return val == null ? -1 : Integer.valueOf(val.subjectWantedPresentationPoints);
    }
}