package de.oerntec.votenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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

    public Cursor getAllData() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_SUBJECTS, new String[0]);
    }

    /**
     * Delete the group with the given name AND the given id
     *
     * @param groupName Name of the group to delete
     * @return Number of affected rows.
     */
    public int deleteRecord(String groupName, int groupId) {
        Log.i("dbgroups:delete", "deleted " + groupName + " at " + groupId);
        String whereClause = DatabaseCreator.SUBJECTS_NAME + "=?" + " AND " + DatabaseCreator.SUBJECTS_ID + "=?";
        String[] whereArgs = new String[]{groupName, String.valueOf(groupId)};
        return database.delete(DatabaseCreator.TABLE_NAME_SUBJECTS, whereClause, whereArgs);
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
        //check whether group name exists; abort if it does
        String[] testColumns = new String[]{DatabaseCreator.SUBJECTS_ID, DatabaseCreator.SUBJECTS_NAME};
        Cursor testCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, testColumns, DatabaseCreator.SUBJECTS_NAME + "=?", new String[]{groupName}, null, null, DatabaseCreator.SUBJECTS_ID + " DESC", null);

        //abort if group already exists
        if (testCursor.getCount() > 0) {
            testCursor.close();
            return -1;
        }
        testCursor.close();

        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_NAME, groupName);
        values.put(DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE, minVot);
        values.put(DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS, minPres);
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS, newScheduledUebungCount);
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON, newScheduledAssignmentsPerUebung);

        //insert name, because it does not exist yet
        Log.i("DBGroups", "adding group");
        database.insert(DatabaseCreator.TABLE_NAME_SUBJECTS, null, values);
        return 1;
    }

    /**
     * This function returns the id of the group that is displayed at drawerselection in the drawer
     *
     * @param drawerSelection
     * @return
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


    public int translatePositionToIDExclusive(int drawerSelection, int excludedID) {
        Cursor groups = getAllButOneGroupNames(excludedID);
        if (groups.getCount() == 0)
            return NO_GROUPS_EXIST;
        groups.moveToPosition(drawerSelection);
        int translatedSection = groups.getInt(0);
        groups.close();
        return translatedSection;
    }


    public int translateIDtoPosition(int dbID) {
        Cursor groups = getAllGroupNames();
        if (groups.getCount() == 0)
            return NO_GROUPS_EXIST;
        int counter = 0;
        while (groups.moveToNext()) {
            if (groups.getInt(0) == dbID)
                break;
            counter++;
        }
        groups.close();
        return counter;
    }

    public int translateIDtoPositionExclusive(int dbID, int excludedID) {
        Cursor groups = getAllButOneGroupNames(excludedID);
        if (groups.getCount() == 0)
            return NO_GROUPS_EXIST;
        int counter = 0;
        while (groups.moveToNext()) {
            if (groups.getInt(0) == dbID)
                break;
            counter++;
        }
        groups.close();
        return counter;
    }

    /**
     * Return a cursor containing all Groups sorted by id desc;
     * sequence: ID, NAME, MINVOTE, MINPRES
     *
     * @return the cursor
     */
    public Cursor getAllGroupNames() {
        //sort cursor by name to have a defined order
        String[] cols = new String[]{DatabaseCreator.SUBJECTS_ID,
                DatabaseCreator.SUBJECTS_NAME,
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, cols, null, null, null, null, DatabaseCreator.SUBJECTS_ID + " DESC", null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public Cursor getAllButOneGroupNames(int excludedID) {
        //sort cursor by name to have a defined order
        String[] cols = new String[]{DatabaseCreator.SUBJECTS_ID, DatabaseCreator.SUBJECTS_NAME};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, cols, DatabaseCreator.SUBJECTS_ID + "!=?", new String[]{excludedID + ""}, null, null, DatabaseCreator.SUBJECTS_ID + " DESC", null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public int getNumberOfSubjects() {
        //sort cursor by name to have a defined order
        String[] cols = new String[]{DatabaseCreator.SUBJECTS_ID};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, cols, null, null, null, null, DatabaseCreator.SUBJECTS_ID + " DESC", null);
        int answer = mCursor.getCount();
        mCursor.close();
        return answer; // iterate to get each value.
    }

    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_SUBJECTS, null, null);
    }

    /**
     * returns a cursor with all
     *
     * @return
     */
    public Cursor getAllGroupsInfos() {
        //sort cursor by name to have a defined reihenfolg
        String[] cols = new String[]{DatabaseCreator.SUBJECTS_ID,
                DatabaseCreator.SUBJECTS_NAME,
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS,
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, cols, null, null, null, null, DatabaseCreator.SUBJECTS_ID + " DESC", null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public List<Subject> getAllLessons() {
        String[] cols = new String[]{DatabaseCreator.SUBJECTS_ID,//0
                DatabaseCreator.SUBJECTS_NAME,//1
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,//2
                DatabaseCreator.SUBJECTS_CURRENT_PRESENTATION_POINTS,//3
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS,//4
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,//5
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};//6
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, cols, null, null, null, null, null, null);
        ArrayList<Subject> resultList = new ArrayList<>();
        while (mCursor.moveToNext()) {
            resultList.add(new Subject(mCursor.getString(0), //id
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
     * Returns a cursor containing only the row with the specified id
     *
     * @return the cursor
     */
    public Cursor getGroupAt(int id) {
        String[] cols = new String[]{DatabaseCreator.SUBJECTS_ID, DatabaseCreator.SUBJECTS_NAME,
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};
        String[] whereArgs = new String[]{String.valueOf(id)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, cols, DatabaseCreator.SUBJECTS_ID + "=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public Subject getGroup(int databaseId) {
        String[] cols = new String[]{DatabaseCreator.SUBJECTS_ID,//0
                DatabaseCreator.SUBJECTS_NAME,//1
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,//2
                DatabaseCreator.SUBJECTS_CURRENT_PRESENTATION_POINTS,//3
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS,//4
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,//5
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};//6
        String[] whereArgs = new String[]{String.valueOf(databaseId)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_SUBJECTS, cols, DatabaseCreator.SUBJECTS_ID + "=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        Subject returnValue = new Subject(mCursor.getString(0), //id
                mCursor.getString(1), //name
                mCursor.getString(2), //minvote
                mCursor.getString(3), //current prespoints
                mCursor.getString(5), //sched lesson count
                mCursor.getString(6), //sched assignments per lesson
                mCursor.getString(4)); //wanted prespoints
        mCursor.close();
        return returnValue;
    }

    public void setScheduledUebungCountAndAssignments(int groupID, int newScheduledUebungCount, int newScheduledAssignmentsPerUebung) {
        Log.i("DBGroups", "changing uebung schedule");
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS, newScheduledUebungCount);
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON, newScheduledAssignmentsPerUebung);

        database.update(DatabaseCreator.TABLE_NAME_SUBJECTS, values, DatabaseCreator.SUBJECTS_ID + "=?", new String[]{String.valueOf(groupID)});
    }

    /**
     * Returns the amount of assignments you could go to at maximum
     *
     * @return the cursor
     */
    public int getScheduledWork(int id) {
        return Integer.valueOf(getGroup(id).subjectScheduledLessonCount) * Integer.valueOf(getGroup(id).subjectScheduledAssignmentsPerLesson);
    }

    /**
     * Returns the entered amount of uebung instances
     *
     * @param id ID of the concerned Group
     * @return see above
     */
    public int getScheduledNumberOfLessons(int id) {
        return Integer.valueOf(getGroup(id).subjectScheduledLessonCount);
    }

    /**
     * Returns the entered amount estimated assignments per uebung
     *
     * @param id ID of the concerned Group
     * @return see above
     */
    public int getScheduledAssignmentsPerLesson(int id) {
        return Integer.valueOf(getGroup(id).subjectScheduledAssignmentsPerLesson);
    }

    /**
     * Return the name of the group with the specified id
     */
    public String getGroupName(int dbID) {
        return getGroup(dbID).subjectName;
    }

    /**
     * Change oldName with database ID dbId to newName
     *
     * @param dbID    database id concerned
     * @param oldName old name for security
     * @param newName new name
     * @return num of affected rows
     */
    public int changeName(int dbID, String oldName, String newName) {
        String[] whereArgs = new String[]{String.valueOf(dbID), oldName};
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_NAME, newName);
        return database.update(DatabaseCreator.TABLE_NAME_SUBJECTS, values, DatabaseCreator.SUBJECTS_ID + "=? AND " + DatabaseCreator.SUBJECTS_NAME + "=?", whereArgs);
    }


    /**
     * Returns the minimum Vote needed for passing from db
     *
     * @param dbID ID of group
     * @return minvote
     */
    public int getMinVote(int dbID) {
        return Integer.valueOf(getGroup(dbID).subjectMinimumVotePercentage);
    }

    /**
     * Set the minimum amount of votes needed for passing class
     *
     * @param databaseID ID of the group concerned
     * @param minValue   Minimum votes
     * @return Affected row count
     */
    public int setMinVote(int databaseID, int minValue) {
        Log.i("DBGroups", "changing minvalue");
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE, minValue);

        String[] whereArgs = {String.valueOf(databaseID)};
        return database.update(DatabaseCreator.TABLE_NAME_SUBJECTS, values, DatabaseCreator.SUBJECTS_ID + "=?", whereArgs);
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
        values.put(DatabaseCreator.SUBJECTS_CURRENT_PRESENTATION_POINTS, presPoints);
        return database.update(DatabaseCreator.TABLE_NAME_SUBJECTS, values, DatabaseCreator.SUBJECTS_ID + "=?", whereArgs);
    }

    /**
     * Helper function for getting the pres points for the specified group
     *
     * @param dbID the database id of the group
     * @return Prespoint number
     */
    public int getPresPoints(int dbID) {
        return Integer.valueOf(getGroup(dbID).subjectCurrentPresentationPoints);
    }

    /**
     * set the minimum amount of presentation points needed for passing
     *
     * @param dbID          Database id of group to change
     * @param minPresPoints presentation points the user has
     * @return The amount of rows updated, should be one
     */
    public int setMinPresPoints(int dbID, int minPresPoints) {
        Log.i("dbgroups", "setting min pres points");
        String[] whereArgs = {String.valueOf(dbID)};
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS, minPresPoints);
        return database.update(DatabaseCreator.TABLE_NAME_SUBJECTS, values, DatabaseCreator.SUBJECTS_ID + "=?", whereArgs);
    }

    /**
     * Get the minimum prespoints for the given group
     *
     * @param dbID ID of the Group
     * @return Minimum number of Prespoints needed for passing
     */
    public int getMinPresPoints(int dbID) {
        return Integer.valueOf(getGroup(dbID).subjectWantedPresentationPoints);
    }

    class Subject {
        String id,
                subjectName,
                subjectMinimumVotePercentage,
                subjectCurrentPresentationPoints,
                subjectScheduledLessonCount,
                subjectScheduledAssignmentsPerLesson,
                subjectWantedPresentationPoints;

        public Subject(String id, String subjectName, String subjectMinimumVotePercentage, String subjectCurrentPresentationPoints, String subjectScheduledLessonCount, String subjectScheduledAssignmentsPerLesson, String subjectWantedPresentationPoints) {
            this.id = id;
            this.subjectName = subjectName;
            this.subjectMinimumVotePercentage = subjectMinimumVotePercentage;
            this.subjectCurrentPresentationPoints = subjectCurrentPresentationPoints;
            this.subjectScheduledLessonCount = subjectScheduledLessonCount;
            this.subjectScheduledAssignmentsPerLesson = subjectScheduledAssignmentsPerLesson;
            this.subjectWantedPresentationPoints = subjectWantedPresentationPoints;
        }
    }
}

