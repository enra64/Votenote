package de.oerntec.votenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBLessons {
    /**
     * Singleton instance
     */
    private static DBLessons mInstance;

    /**
     * Database object used for accessing the database
     */
    private SQLiteDatabase database;

    /**
     * Private constructor for singleton
     *
     * @param context context needed for database
     */
    private DBLessons(Context context) {
        DatabaseCreator dbHelper = new DatabaseCreator(context);
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Create the singleton object
     * @param context context needed for creating the db access
     * @return the instance itself
     */
    public static DBLessons setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBLessons(context);
        return mInstance;
    }

    /**
     * Singleton getter
     * @return the singleton instance
     */
    public static DBLessons getInstance() {
        return mInstance;
    }

    /**
     * Drop all lesson data
     */
    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_ENTRIES, null, null);
    }

    /**
     * Get all data, only for export
     * @return all lesson data
     */
    public Cursor getAllData() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_ENTRIES, new String[0]);
    }

    /**
     * Change the values of a lesson
     */
    public void changeLesson(int subjectId, int lessonId, int maxVote, int myVote) {
        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ENTRIES_MAX_VOTES, maxVote);
        values.put(DatabaseCreator.ENTRIES_MY_VOTES, myVote);

        String[] whereArgs = {String.valueOf(subjectId), String.valueOf(lessonId)};
        int affectedRows = database.update(DatabaseCreator.TABLE_NAME_ENTRIES, values, DatabaseCreator.ENTRIES_SUBJECT_ID + "=?" + " AND " + DatabaseCreator.ENTRIES_LESSON_ID + "=?", whereArgs);
        Log.i("dbentries:changeentry", "changed " + affectedRows + " entries");
    }

    /**
     * Get a lesson corresponding to that subject and lesson id
     *
     * @param subjectId the subject type the lesson is of
     * @param lessonId  the instance number of the subject
     * @return a Lesson object containing all records of the lesson
     */
    public Lesson getLesson(int subjectId, int lessonId){
        String[] cols = new String[]{DatabaseCreator.ENTRIES_MY_VOTES, DatabaseCreator.ENTRIES_MAX_VOTES, DatabaseCreator.ENTRIES_ID};
        String[] whereArgs = {String.valueOf(subjectId), String.valueOf(lessonId)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols, DatabaseCreator.ENTRIES_SUBJECT_ID + "=?" + " AND " + DatabaseCreator.ENTRIES_LESSON_ID + "=?", whereArgs, null, null, null, null);
        Lesson returnValue = null;
        if (mCursor.moveToFirst())
            returnValue = new Lesson(lessonId, mCursor.getInt(0), mCursor.getInt(1), mCursor.getInt(2), subjectId);
        mCursor.close();
        return returnValue;
    }

    public void deleteAllEntriesForGroup(int subjectId) {
        String[] whereArgs = new String[]{String.valueOf(subjectId)};
        int checkValue =
                database.delete(DatabaseCreator.TABLE_NAME_ENTRIES,
                        DatabaseCreator.ENTRIES_SUBJECT_ID + "=?", whereArgs);
        Log.i("dbgroups:delete", "deleting all " + checkValue + " entries of type " + subjectId);
    }

    /**
     * Add an entry to the respective uebung
     */
    public void addLesson(int subjectId, int maxVote, int myVote) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_LESSON_ID};
        Cursor lastEntryNummerCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols,
                DatabaseCreator.ENTRIES_SUBJECT_ID + "=" + subjectId, null,
                null, null, DatabaseCreator.ENTRIES_LESSON_ID + " DESC", "1");
        //default value
        int lastEntryLessonId = 1;
        //try to get a new lesson id
        if (lastEntryNummerCursor.moveToFirst())
            lastEntryLessonId = lastEntryNummerCursor.getInt(0) + 1;
        lastEntryNummerCursor.close();
        addLesson(subjectId, maxVote, myVote, lastEntryLessonId);
    }

    /**
     * Add an entry to the respective uebung
     */
    public void addLesson(int subjectId, int maxVote, int myVote, int uebungNummer) {
        Log.i("db:entries:add", "adding entry with lastnummer" + uebungNummer + " for group " + subjectId);

        //create values for insert or update
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.ENTRIES_SUBJECT_ID, subjectId);
        values.put(DatabaseCreator.ENTRIES_LESSON_ID, uebungNummer);
        values.put(DatabaseCreator.ENTRIES_MAX_VOTES, maxVote);
        values.put(DatabaseCreator.ENTRIES_MY_VOTES, myVote);

        database.insert(DatabaseCreator.TABLE_NAME_ENTRIES, null, values);
    }

    /**
     * remove the entry and decrease the uebung_nummer for all following entries
     */
    public void removeEntry(int subjectId, int lessonId) {
        //remove correct entry
        database.delete(DatabaseCreator.TABLE_NAME_ENTRIES, DatabaseCreator.ENTRIES_SUBJECT_ID + "=" + subjectId + " AND " + DatabaseCreator.ENTRIES_LESSON_ID + "=" + lessonId, null);
        //decrease uebungnummer for all following entries
        String query = "UPDATE " + DatabaseCreator.TABLE_NAME_ENTRIES + " SET " + DatabaseCreator.ENTRIES_LESSON_ID + " = " + DatabaseCreator.ENTRIES_LESSON_ID + " - 1 " +
                "WHERE " + DatabaseCreator.ENTRIES_SUBJECT_ID + " = ? AND " + DatabaseCreator.ENTRIES_LESSON_ID + " > ?";
        database.execSQL(query, new String[]{String.valueOf(subjectId), String.valueOf(lessonId)});
    }

    /**
     * The previously given maximum vote value, or the scheduled assignment count if no previous entry exists
     *
     * @param subjectId id of the group tp get a valuie for
     * @return maximum possible vote
     */
    public int getPreviousMaximumVote(int subjectId) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_ID, DatabaseCreator.ENTRIES_MAX_VOTES};
        String[] whereArgs = new String[]{String.valueOf(subjectId)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols,
                DatabaseCreator.ENTRIES_SUBJECT_ID + "=?", whereArgs, null, null, DatabaseCreator.ENTRIES_LESSON_ID + " DESC", "1");
        //init return
        int returnValue;
        //sorted by descending, so first value is highest uebung nummer
        if (mCursor.moveToFirst())
            returnValue = mCursor.getInt(1);
        else//default to the scheduled value
            returnValue = DBSubjects.getInstance().getScheduledAssignmentsPerLesson(subjectId);
        mCursor.close();
        return returnValue;
    }

    /**
     * Get the last entered myvote for the given subject
     *
     * @param subjectId id of the subject
     * @return last saved myvote value
     */
    public int getPreviousMyVote(int subjectId) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_ID, DatabaseCreator.ENTRIES_MY_VOTES};
        String[] whereArgs = new String[]{String.valueOf(subjectId)};
        Cursor mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols,
                DatabaseCreator.ENTRIES_SUBJECT_ID + "=?", whereArgs, null, null, DatabaseCreator.ENTRIES_LESSON_ID + " DESC", "1");
        int returnValue;
        //sorted by descending, so first value is highest uebung nummer
        if (mCursor.moveToFirst())
            returnValue = mCursor.getInt(1);
        else//return the scheduled amount of work as default
            returnValue = DBSubjects.getInstance().getScheduledAssignmentsPerLesson(subjectId);
        mCursor.close();
        return returnValue;
    }

    /**
     * Get the count of lessons for the subject
     *
     * @param subjectId subject id
     * @return The number of Lessons added for the subject
     */
    public int getLessonCountForSubject(int subjectId) {
        Cursor mCursor = getAllLessonsForSubject(subjectId);
        int answer = mCursor.getCount();
        mCursor.close();
        return answer; // iterate to get each value.
    }

    /**
     * Return a cursor for the selected group_type
     * {DatabaseCreator.ENTRIES_LESSON_ID, DatabaseCreator.ENTRIES_MY_VOTES, DatabaseCreator.ENTRIES_MAX_VOTES, DatabaseCreator.ENTRIES_ID}
     *
     * @param subjectId The ID or Type of the inquired Group
     * @return the cursor
     */
    public Cursor getAllLessonsForSubject(int subjectId) {
        String[] cols = new String[]{DatabaseCreator.ENTRIES_LESSON_ID, DatabaseCreator.ENTRIES_MY_VOTES, DatabaseCreator.ENTRIES_MAX_VOTES, DatabaseCreator.ENTRIES_ID};
        String[] whereArgs = new String[]{String.valueOf(subjectId)};
        boolean isLatestFirst = MainActivity.getPreference("reverse_lesson_sort", false);
        Cursor mCursor;
        if (!isLatestFirst)
            mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols, DatabaseCreator.ENTRIES_SUBJECT_ID + "=?", whereArgs, null, null, null, null);
        else
            mCursor = database.query(true, DatabaseCreator.TABLE_NAME_ENTRIES, cols, DatabaseCreator.ENTRIES_SUBJECT_ID + "=?", whereArgs, null, null, DatabaseCreator.ENTRIES_LESSON_ID + " DESC", null);
        mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    /**
     * Get the sum of myvotes
     *
     * @param subjectId Id of the subject to work with
     * @return The full amount of assignments done for the subject, as in the sum of myVote
     */
    public int getCompletedAssignmentCount(int subjectId) {
        Cursor cursor = database.rawQuery(
                "SELECT SUM(" + DatabaseCreator.ENTRIES_MY_VOTES + ") FROM " + DatabaseCreator.TABLE_NAME_ENTRIES + " WHERE " + DatabaseCreator.ENTRIES_SUBJECT_ID + "=" + subjectId, null);
        if (cursor.moveToFirst()) {
            int val = cursor.getInt(0);
            cursor.close();
            return val;
        }
        cursor.close();
        return -1;
    }

    public class Lesson {
        public int lessonId, myVotes, maxVotes, id, subjectId;

        public Lesson(int lessonId, int myVotes, int maxVotes, int id, int subjectId) {
            this.lessonId = lessonId;
            this.myVotes = myVotes;
            this.maxVotes = maxVotes;
            this.id = id;
            this.subjectId = subjectId;
        }
    }
}