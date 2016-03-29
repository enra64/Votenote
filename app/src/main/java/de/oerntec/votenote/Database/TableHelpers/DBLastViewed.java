package de.oerntec.votenote.Database.TableHelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import de.oerntec.votenote.Database.DatabaseCreator;

public class DBLastViewed {
    private static DBLastViewed mInstance;

    /**
     * Database object used for accessing the database
     */
    protected final SQLiteDatabase mDatabase;

    private DBLastViewed(Context context) {
        DatabaseCreator dbHelper = DatabaseCreator.getInstance(context);
        mDatabase = dbHelper.getWritableDatabase();
    }

    public static DBLastViewed setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBLastViewed(context);
        return mInstance;
    }

    public static DBLastViewed getInstance() {
        return mInstance;
    }

    /**
     * Get the position in NavigationDrawerFragment that was selected most recently
     *
     * @return -1 if no records exist, or a position
     * @see #getLastPercentageTrackerPosition(int)
     */
    public int getLastSubjectPosition() {
        Cursor result = mDatabase.query(true,
                DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                new String[]{DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION},
                null,
                null,
                null,
                null,
                DatabaseCreator.LAST_VIEWED_TIMESTAMP + " DESC",
                " 1");
        if (result.getCount() == 0) {
            result.close();
            return -1;
        }
        result.moveToFirst();
        int lastSelectedSubjectPosition = result.getInt(result.getColumnIndex(DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION));
        result.close();
        return lastSelectedSubjectPosition;
    }

    /**
     * Get the most recently selected Percentage Tracker Position for a given subject
     *
     * @param subjectPosition Position of the subject
     * @return -1 if no records exist for the given subject, or a ViewPager position
     * @see #getLastSubjectPosition()
     */
    public int getLastPercentageTrackerPosition(int subjectPosition) {
        Cursor result = mDatabase.query(true,
                DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                new String[]{DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION},
                DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION + "=?",
                new String[]{String.valueOf(subjectPosition)},
                null,
                null,
                DatabaseCreator.LAST_VIEWED_TIMESTAMP + " DESC",
                " 1");
        if (result.getCount() == 0) {
            result.close();
            return -1;
        }
        result.moveToFirst();
        int lastSelectedMetaPosition = result.getInt(result.getColumnIndex(DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION));
        result.close();
        return lastSelectedMetaPosition;
    }

    /**
     * Saves the most recently selected percentage tracker and subject position
     *
     * @param subjectPosition position of the subject
     * @param trackerPosition position of the percentage tracker
     */
    public void saveLastTrackerAndSubjectPosition(int subjectPosition, int trackerPosition) {
        //insert new value if no meta id can be found for this subject position
        if (getLastPercentageTrackerPosition(subjectPosition) == -1)
            addTrackerPositionForSubject(subjectPosition, trackerPosition);
            //update old meta position
        else
            updateTrackerPositionForSubject(subjectPosition, trackerPosition);
    }

    /**
     * Adds a record to the database
     *
     * @param subjectPosition           position of the subject
     * @param percentageTrackerPosition position of the tracker
     * @see #updateTrackerPositionForSubject(int, int)
     */
    private void addTrackerPositionForSubject(int subjectPosition, int percentageTrackerPosition) {
        ContentValues v = new ContentValues();
        v.put(DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION, subjectPosition);
        v.put(DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION, percentageTrackerPosition);
        mDatabase.insert(DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                null,
                v);
    }

    /**
     * Changes a tracker entry in the database
     *
     * @param subjectPosition           position of the subject
     * @param percentageTrackerPosition position of the tracker
     * @see #addTrackerPositionForSubject(int, int)
     */
    private void updateTrackerPositionForSubject(int subjectPosition, int percentageTrackerPosition) {
        ContentValues values = new ContentValues();
        values.put(DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION, percentageTrackerPosition);
        mDatabase.update(DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                values,
                DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION + "=?",
                new String[]{String.valueOf(subjectPosition)}
        );
    }
}