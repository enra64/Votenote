package de.oerntec.votenote.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class CrudDb<T> {
    /**
     * Singleton instance
     */
    private static CrudDb mInstance;
    final String TABLE_NAME;
    /**
     * Database object used for accessing the database
     */
    private final SQLiteDatabase mDatabase;

    /**
     * Private constructor for singleton
     */
    private CrudDb(Context context, String tableName) {
        DatabaseCreator dbHelper = DatabaseCreator.getInstance(context);
        mDatabase = dbHelper.getWritableDatabase();
        TABLE_NAME = tableName;
    }

    abstract public void addItem(T item);

    abstract public void getItem(T item);

    abstract public void changeItem(T item);

    abstract public void deleteItem(T item);

    /**
     * Create a savepoint using the specified id
     *
     * @param id savepoint id, must not start iwth [0-9]
     */
    public void createSavepoint(String id) {
        mDatabase.execSQL(";SAVEPOINT " + id);
    }

    /**
     * Rolback to the specified savepoint, rolling changes back
     *
     * @param id savepoint id, must start with [A-Za-z]
     */
    public void rollbackToSavepoint(String id) {
        mDatabase.execSQL(";ROLLBACK TO SAVEPOINT " + id);
    }

    /**
     * Release sql savepoint, effectively commiting any changes
     *
     * @param id savepoint id, must not start with [0-9]
     */
    public void releaseSavepoint(String id) {
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
    int getCount() {
        Cursor c = mDatabase.rawQuery("SELECT COUNT() AS rowNumber FROM " + TABLE_NAME, null);
        c.moveToFirst();
        int result = c.getInt(c.getColumnIndexOrThrow("rowNumber"));
        c.close();
        return result;
    }
}
