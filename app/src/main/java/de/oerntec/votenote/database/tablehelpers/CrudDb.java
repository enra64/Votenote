package de.oerntec.votenote.database.tablehelpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.database.DatabaseCreator;

public abstract class CrudDb<T> {
    private static final boolean ENABLE_SAVEPOINT_LOG = false;
    /**
     * Database object used for accessing the database
     */
    final SQLiteDatabase mDatabase;
    /**
     * name of the table the crud table helper is for
     */
    private final String TABLE_NAME;

    /**
     * Private constructor for singleton
     */
    CrudDb(Context context, String tableName) {
        DatabaseCreator dbHelper = DatabaseCreator.getInstance(context);
        mDatabase = dbHelper.getWritableDatabase();
        TABLE_NAME = tableName;
    }

    abstract public void addItem(T item);

    abstract public int addItemGetId(T item);

    abstract public T getItem(T item);

    abstract public void changeItem(T item);

    abstract public void deleteItem(T item);

    /**
     * Create a savepoint using the specified id
     *
     * @param id savepoint id, must not start iwth [0-9]
     */
    public void createSavepoint(String id) {
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (MainActivity.ENABLE_TRANSACTION_LOG)
            Log.i("db", "creating savepoint: " + id);
        mDatabase.execSQL(";SAVEPOINT " + id);
    }

    /**
     * Rolback to the specified savepoint, rolling changes back
     *
     * @param id savepoint id, must start with [A-Za-z]
     */
    public void rollbackToSavepoint(String id) {
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (MainActivity.ENABLE_TRANSACTION_LOG)
            Log.i("db", "rolling back to savepoint: " + id);
        mDatabase.execSQL(";ROLLBACK TO SAVEPOINT " + id);

        // i think we need this because a ROLLBACK TO does not end the transaction, so we have to
        // specifically say "close the transaction" after we seid "rollback"
        releaseSavepoint(id);
    }

    /**
     * Release sql savepoint, effectively commiting any changes
     *
     * @param id savepoint id, must not start with [0-9]
     */
    public void releaseSavepoint(String id) {
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (MainActivity.ENABLE_TRANSACTION_LOG)
            Log.i("db", "releasing savepoint: " + id);
        mDatabase.execSQL(";RELEASE SAVEPOINT " + id);
    }

    /**
     * Drop the table
     */
    public void dropData() {
        mDatabase.rawQuery("DROP TABLE " + TABLE_NAME, null);
    }

    /**
     * Get the total number of rows in the current table
     */
    public int getCount() {
        Cursor c = mDatabase.rawQuery("SELECT COUNT() AS rowNumber FROM " + TABLE_NAME, null);
        c.moveToFirst();
        int result = c.getInt(c.getColumnIndexOrThrow("rowNumber"));
        c.close();
        return result;
    }

    public Cursor getDataDump() {
        return mDatabase.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }
}
