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

    private DBLastViewed(Context context){
        DatabaseCreator dbHelper = DatabaseCreator.getInstance(context);
        mDatabase = dbHelper.getWritableDatabase();
    }

    public static DBLastViewed setupInstance(Context context) {
        if (mInstance == null)
            mInstance = new DBLastViewed(context);
        return mInstance;
    }

    public static DBLastViewed getInstance(){
        return mInstance;
    }

    public int getLastSelectedSubjectPosition(){
        Cursor result = mDatabase.query(true,
                DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                new String[]{DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION},
                null,
                null,
                null,
                null,
                DatabaseCreator.LAST_VIEWED_TIMESTAMP + " DESC",
                " 1");
        if (result.getCount() == 0){
            result.close();
            return -1;
        }
        result.moveToFirst();
        int lastSelectedSubjectPosition = result.getInt(result.getColumnIndex(DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION));
        result.close();
        return lastSelectedSubjectPosition;
    }

    public void saveSelection(int subjectPosition, int apMetaPosition){
        //insert new value if no meta id can be found for this subject position
        if(getLastSelectedAdmissionCounterForSubjectPosition(subjectPosition) == -1)
            insertSelection(subjectPosition, apMetaPosition);
        //update old meta position
        else
            updateMetaId(subjectPosition, apMetaPosition);
    }

    private void insertSelection(int subjectPosition, int apMetaPosition){
        ContentValues v = new ContentValues();
        v.put(DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION, subjectPosition);
        v.put(DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION, apMetaPosition);
        mDatabase.insert(DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                null,
                v);
    }

    private void updateMetaId(int subjectPosition, int apMetaPosition){
        ContentValues v = new ContentValues();
        v.put(DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION, apMetaPosition);
        mDatabase.update(DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                v,
                DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION + "=?",
                new String[] {String.valueOf(subjectPosition)}
        );
    }

    public int getLastSelectedAdmissionCounterForSubjectPosition(int subjectPosition){
        Cursor result = mDatabase.query(true,
                DatabaseCreator.TABLE_NAME_LAST_VIEWED,
                new String[]{DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION},
                DatabaseCreator.LAST_VIEWED_SUBJECT_POSITION + "=?",
                new String[]{String.valueOf(subjectPosition)},
                null,
                null,
                DatabaseCreator.LAST_VIEWED_TIMESTAMP + " DESC",
                " 1");
        if (result.getCount() == 0){
            result.close();
            return -1;
        }
        result.moveToFirst();
        int lastSelectedMetaPosition = result.getInt(result.getColumnIndex(DatabaseCreator.LAST_VIEWED_PERCENTAGE_META_POSITION));
        result.close();
        return lastSelectedMetaPosition;
    }
}