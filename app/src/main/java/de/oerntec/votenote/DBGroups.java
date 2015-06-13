package de.oerntec.votenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBGroups{
	public final static int NO_GROUPS_EXIST = -1;
	public final static String TABLE = "uebungen_gruppen";
	
	/* Database creation sql statement from database helper
	 * We need
	 * -key: _id
	 * -which uebung_typ: typ_uebung
	 * -which uebung_number: nummer_uebung
	 * -maximum votierungs: max_votierung
	 * -my votierungs: my_votierung
	 */
	public final static String ID_COLUMN="_id";
	private static DBGroups mInstance;
	private DatabaseCreator dbHelper;
	private SQLiteDatabase database;

	private DBGroups(Context context){
	    dbHelper = new DatabaseCreator(context);
	    database = dbHelper.getWritableDatabase();
	}

	public static DBGroups setupInstance(Context context){
		if(mInstance == null)
			mInstance = new DBGroups(context);
		return mInstance;
	}

	public static DBGroups getInstance(){
		return mInstance;
	}

	public Cursor getAllData() {
        return database.rawQuery("SELECT * FROM " + DatabaseCreator.TABLE_NAME_SUBJECTS, new String[0]);
    }
	
	/**
	 * Delete the group with the given name AND the given id
	 * @param groupName Name of the group to delete
	 * @return Number of affected rows.
	 */
	public int deleteRecord(String groupName, int groupId){
		Log.i("dbgroups:delete", "deleted "+groupName+" at "+groupId);
        String whereClause = DatabaseCreator.SUBJECTS_NAME + "=?" + " AND " + ID_COLUMN + "=?";
        String[]whereArgs = new String[] {groupName, String.valueOf(groupId)};
        return database.delete(TABLE, whereClause, whereArgs);
    }
	
	/**
	 * Adds a group to the Table, if it does not exist;
	 * if a group with the given name exists, we change it.
	 * @param groupName Name of the group.
	 */
	public int changeOrAddGroupName(String groupName){
		//check whether group name exists; abort if it does
        String[] testColumns = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_NAME};
        Cursor testCursor = database.query(true, TABLE, testColumns, null, null, null, null, ID_COLUMN+" DESC", null);
		if (testCursor != null)
			testCursor.moveToFirst();
		while(testCursor.moveToNext()){
			if(testCursor.getString(1).equals(groupName))
				return -1;
		}
		//get a cursor with the id of the given getGroupName
		String[] cols = new String[] {ID_COLUMN};
		String[] whereArgs={groupName};
        Cursor mCursor = database.query(true, TABLE, cols, DatabaseCreator.SUBJECTS_NAME + "=?", whereArgs, null, null, null, null);
        //init cursor
		if (mCursor != null)
			if(!mCursor.moveToFirst())
				Log.w("db:groups", "empty cursor");
			
		//create values for insert or update
		ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_NAME, groupName);

        //name already exists->update
		if(mCursor.getCount()==1){
			Log.i("DBGroups", "changing entry");
			int existingId=mCursor.getInt(0);
			String[] whereArgsUpdate={String.valueOf(existingId)};
			database.update(TABLE, values, ID_COLUMN+"=?", whereArgsUpdate);
		}
		//insert name, because it does not exist yet
		else{
			Log.i("DBGroups", "adding group");
			database.insert(TABLE, null, values);
		}
		//mandatory cursorclosing
		mCursor.close();
		return 1;
	}
	
	/**
	 * Adds a group with the given Parameters
	 * @param groupName Name of the new Group
	 * @param minVot minimum vote
	 * @param minPres minimum presentation points
	 * @return -1 if group exists, 1 else.
	 */
	public int addGroup(String groupName, int minVot, int minPres, int newScheduledUebungCount, int newScheduledAssignmentsPerUebung){
		//check whether group name exists; abort if it does
        String[] testColumns = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_NAME};
        Cursor testCursor = database.query(true, TABLE, testColumns, null, null, null, null, ID_COLUMN+" DESC", null);
		if (testCursor != null)
			testCursor.moveToFirst();
		while(testCursor.moveToNext()){
			if(testCursor.getString(1).equals(groupName))
				return -1;
		}
		//get a cursor with the id of the given getGroupName
		String[] cols = new String[] {ID_COLUMN};
		String[] whereArgs={groupName};
        Cursor mCursor = database.query(true, TABLE, cols, DatabaseCreator.SUBJECTS_NAME + "=?", whereArgs, null, null, null, null);
        //init cursor
		if (mCursor != null)
			if(!mCursor.moveToFirst())
				Log.w("db:groups", "empty cursor");
			
		//create values for insert or update
		ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_NAME, groupName);
        values.put(DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE, minVot);
        values.put(DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS, minPres);
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS, newScheduledUebungCount);
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON, newScheduledAssignmentsPerUebung);

		//name already exists->update
		if(mCursor.getCount()==1){
			Log.i("DBGroups", "changing entry");
			int existingId=mCursor.getInt(0);
			String[] whereArgsUpdate={String.valueOf(existingId)};
			database.update(TABLE, values, ID_COLUMN+"=?", whereArgsUpdate);
		}
		//insert name, because it does not exist yet
		else{
			Log.i("DBGroups", "adding group");
			database.insert(TABLE, null, values);
		}
		//mandatory cursorclosing
		mCursor.close();
		return 1;
	}
	
	/**
	 * This function returns the id of the group that is displayed at drawerselection in the drawer
	 * @param drawerSelection
	 * @return 
	 */
    public int translatePositionToID(int drawerSelection){
        Cursor groups= getAllGroupNames();
        if(groups.getCount()==0)
            return NO_GROUPS_EXIST;
        groups.moveToPosition(drawerSelection);
        int translatedSection=groups.getInt(0);
        groups.close();
        return translatedSection;
    }


    public int translatePositionToIDExclusive(int drawerSelection, int excludedID){
        Cursor groups= getAllButOneGroupNames(excludedID);
        if(groups.getCount()==0)
            return NO_GROUPS_EXIST;
        groups.moveToPosition(drawerSelection);
        int translatedSection=groups.getInt(0);
        groups.close();
        return translatedSection;
    }


    public int translateIDtoPosition(int dbID){
        Cursor groups= getAllGroupNames();
        if(groups.getCount()==0)
            return NO_GROUPS_EXIST;
        int counter = 0;
        while(groups.moveToNext()){
            if(groups.getInt(0) == dbID)
                break;
            counter++;
        }
        groups.close();
        return counter;
    }

    public int translateIDtoPositionExclusive(int dbID, int excludedID){
        Cursor groups= getAllButOneGroupNames(excludedID);
        if(groups.getCount()==0)
            return NO_GROUPS_EXIST;
        int counter = 0;
        while(groups.moveToNext()){
            if(groups.getInt(0) == dbID)
                break;
            counter++;
        }
        groups.close();
        return counter;
    }
	
	/**
	 * Return a cursor containing all Groups sorted by id desc;
	 * sequence: ID, NAME, MINVOTE, MINPRES
	 * @return the cursor
	 */
	public Cursor getAllGroupNames(){
		//sort cursor by name to have a defined order
		String[] cols = new String[] {ID_COLUMN,
                DatabaseCreator.SUBJECTS_NAME,
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS};
        Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor; // iterate to get each value.
	}

    public Cursor getAllButOneGroupNames(int excludedID){
        //sort cursor by name to have a defined reihenfolg
        String[] cols = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_NAME};
        Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"!=?", new String[]{excludedID+""}, null, null, ID_COLUMN+" DESC", null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return mCursor; // iterate to get each value.
    }

    public int getUebungCount(){
        //sort cursor by name to have a defined reihenfolg
        String[] cols = new String[] {ID_COLUMN};
        Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);
        int answer = mCursor.getCount();
        mCursor.close();
        return answer; // iterate to get each value.
    }

    public void dropData() {
        database.delete(DatabaseCreator.TABLE_NAME_SUBJECTS, null, null);
    }

    /**
	 * returns a cursor with all 
	 * @return
	 */
	public Cursor getAllGroupsInfos(){
		//sort cursor by name to have a defined reihenfolg
		String[] cols = new String[] {ID_COLUMN,
                DatabaseCreator.SUBJECTS_NAME,
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS,
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};
        Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor; // iterate to get each value.
	}
	
	/**
	 * Returns a cursor containing only the row with the specified id
	 * @return the cursor
	 */
	public Cursor getGroupAt(int id){
        String[] cols = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_NAME,
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};
        String[] whereArgs = new String[] {String.valueOf(id)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor; // iterate to get each value.
	}

    public Subject getGroup(int databaseId) {
        String[] cols = new String[]{ID_COLUMN,//0
                DatabaseCreator.SUBJECTS_NAME,//1
                DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE,//2
                DatabaseCreator.SUBJECTS_CURRENT_PRESENTATION_POINTS,//3
                DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS,//4
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,//5
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};//6
        String[] whereArgs = new String[]{String.valueOf(databaseId)};
        Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN + "=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        return new Subject(mCursor.getString(0), //id
                mCursor.getString(1), //name
                mCursor.getString(2), //minvote
                mCursor.getString(3), //current prespoints
                mCursor.getString(5), //sched lesson count
                mCursor.getString(6), //sched assignments per lesson
                mCursor.getString(4)); //wanted prespoints
    }

    public void setScheduledUebungCountAndAssignments(int groupID, int newScheduledUebungCount, int newScheduledAssignmentsPerUebung){
        Log.i("DBGroups", "changing uebung schedule");
		ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS, newScheduledUebungCount);
        values.put(DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON, newScheduledAssignmentsPerUebung);

        database.update(TABLE, values, ID_COLUMN+"=?", new String[] {String.valueOf(groupID)});
	}
	
	/**
	 * Returns the amount of assignments you could go to at maximum
	 * @return the cursor
	 */
	public int getScheduledWork(int id){
		String[] cols = new String[] {ID_COLUMN,
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS,
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};
        String[] whereArgs = new String[] {String.valueOf(id)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		int answer=mCursor.getInt(1) * mCursor.getInt(2);
		mCursor.close();
		return answer; // iterate to get each value.
	}

    /**
     * Returns the entered amount of uebung instances
     * @param id ID of the concerned Group
     * @return see above
     */
    public int getScheduledUebungInstanceCount(int id){
        String[] cols = new String[] {ID_COLUMN,
                DatabaseCreator.SUBJECTS_SCHEDULED_NUMBER_OF_LESSONS};
        String[] whereArgs = new String[] {String.valueOf(id)};
        Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        int answer=mCursor.getInt(1);
        mCursor.close();
        return answer; // iterate to get each value.
    }

    /**
     * Returns the entered amount estimated assignments per uebung
     * @param id ID of the concerned Group
     * @return see above
     */
    public int getScheduledAssignmentsPerUebung(int id){
        String[] cols = new String[] {ID_COLUMN,
                DatabaseCreator.SUBJECTS_SCHEDULED_ASSIGNMENTS_PER_LESSON};
        String[] whereArgs = new String[] {String.valueOf(id)};
        Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);
        if (mCursor != null)
            mCursor.moveToFirst();
        int answer=mCursor.getInt(1);
        mCursor.close();
        return answer; // iterate to get each value.
    }
	
	/**
	 * Return the name of the group with the specified id
	 */
	public String getGroupName(int dbID){
        String[] cols = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_NAME};
        String[] whereArgs = new String[] {String.valueOf(dbID)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		String answer=mCursor.getString(1);
		mCursor.close();
		return answer;
	}
	
	/**
	 * Change oldName with database ID dbId to newName
	 * @param dbID database id concerned
	 * @param oldName old name for security
	 * @param newName new name
	 * @return num of affected rows
	 */
	public int changeName(int dbID, String oldName, String newName){
        String[] cols = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_NAME};
        String[] whereArgs = new String[] {String.valueOf(dbID), oldName};
        Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN + "=? AND " + DatabaseCreator.SUBJECTS_NAME + "=?", whereArgs, null, null, ID_COLUMN + " DESC", null);
        if (mCursor != null&&mCursor.getCount()!=0){
			mCursor.moveToFirst();
			ContentValues values = new ContentValues();
            values.put(DatabaseCreator.SUBJECTS_NAME, newName);
            if(mCursor.getString(1).equals(oldName)){
				mCursor.close();
                return database.update(TABLE, values, ID_COLUMN + "=? AND " + DatabaseCreator.SUBJECTS_NAME + "=?", whereArgs);
            }
			else{
				Log.e("dbgroups:changename", "namecheck failed");
				mCursor.close();
				return -1;
			}
		}
		else{//cursor that should contain old name and id is empty -> group does not exist
			mCursor.close();
			return -1;
		}
	}
		
	
	/**
	 * Returns the minimum Vote needed for passing from db
	 * @param dbID ID of group
	 * @return minvote
	 */
	public int getMinVote(int dbID){
        String[] cols = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE};
        String[] whereArgs = new String[] {String.valueOf(dbID)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		//protect from exception
		if(mCursor.getCount()==0)
			return NO_GROUPS_EXIST;
		int minVote=mCursor.getInt(1);
		mCursor.close();
		return minVote;
	}
	
	/**
	 * Set the minimum amount of votes needed for passing class
	 * @param databaseID ID of the group concerned
	 * @param minValue Minimum votes
	 * @return Affected row count
	 */
	public int setMinVote(int databaseID, int minValue) {
		Log.i("DBGroups", "changing minvalue");
		//create values for insert or update
		ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_MINIMUM_VOTE_PERCENTAGE, minValue);

        String[] whereArgs={String.valueOf(databaseID)};
		return database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}
	
	/**
	 * set the presentation points
	 * @param dbID Database id of group to change
	 * @param presPoints presentation points the user has
	 * @return The amount of Rows updated, should be one
	 */
	public int setPresPoints(int dbID, int presPoints){
		String[] whereArgs={String.valueOf(dbID)};
		ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_CURRENT_PRESENTATION_POINTS, presPoints);
        return database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}
	
	/**
	 * Helper function for getting the pres points for the specified group
	 * @param dbID the database id of the group
	 * @return Prespoint number
	 */
	public int getPresPoints(int dbID) {
        String[] cols = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_CURRENT_PRESENTATION_POINTS};
        String[] whereArgs = new String[] {String.valueOf(dbID)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, ID_COLUMN+" DESC", null);
		if (mCursor != null)
			mCursor.moveToFirst();
		int presPoints=mCursor.getInt(1);
		mCursor.close();
		return presPoints;
	}
	
	/**
	 * set the minimum amount of presentation points needed for passing
	 * @param dbID Database id of group to change
	 * @param minPresPoints presentation points the user has
	 * @return The amount of rows updated, should be one
	 */
	public int setMinPresPoints(int dbID, int minPresPoints){
		Log.i("dbgroups", "setting min pres points");
		String[] whereArgs={String.valueOf(dbID)};
		ContentValues values = new ContentValues();
        values.put(DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS, minPresPoints);
        return database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}
	
	/**
	 * Get the minimum prespoints for the given group
	 * @param dbID ID of the Group
	 * @return Minimum number of Prespoints needed for passing
	 */
	public int getMinPresPoints(int dbID) {
        String[] cols = new String[]{ID_COLUMN, DatabaseCreator.SUBJECTS_WANTED_PRESENTATION_POINTS};
        String[] whereArgs = new String[] {String.valueOf(dbID)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, ID_COLUMN+" DESC", null);
		if (mCursor != null)
			mCursor.moveToFirst();
		int maxPresPoints=mCursor.getInt(1);
		mCursor.close();
		return maxPresPoints;
	}

    class Subject {
        private String id,
                subjectName,
                subjectMinimumVotePercentage,
                subjectCurrentPresentationPoints,
                subjectScheduledLessonCount,
                subjectScheduledAssignmentsPerLesson,
                subjectMaximumPresentationPoints;

        public Subject(String id, String subjectName, String subjectMinimumVotePercentage, String subjectCurrentPresentationPoints, String subjectScheduledLessonCount, String subjectScheduledAssignmentsPerLesson, String subjectMaximumPresentationPoints) {
            this.id = id;
            this.subjectName = subjectName;
            this.subjectMinimumVotePercentage = subjectMinimumVotePercentage;
            this.subjectCurrentPresentationPoints = subjectCurrentPresentationPoints;
            this.subjectScheduledLessonCount = subjectScheduledLessonCount;
            this.subjectScheduledAssignmentsPerLesson = subjectScheduledAssignmentsPerLesson;
            this.subjectMaximumPresentationPoints = subjectMaximumPresentationPoints;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getSubjectMinimumVotePercentage() {
            return subjectMinimumVotePercentage;
        }

        public void setSubjectMinimumVotePercentage(String subjectMinimumVotePercentage) {
            this.subjectMinimumVotePercentage = subjectMinimumVotePercentage;
        }

        public String getSubjectCurrentPresentationPoints() {
            return subjectCurrentPresentationPoints;
        }

        public void setSubjectCurrentPresentationPoints(String subjectCurrentPresentationPoints) {
            this.subjectCurrentPresentationPoints = subjectCurrentPresentationPoints;
        }

        public String getSubjectScheduledLessonCount() {
            return subjectScheduledLessonCount;
        }

        public void setSubjectScheduledLessonCount(String subjectScheduledLessonCount) {
            this.subjectScheduledLessonCount = subjectScheduledLessonCount;
        }

        public String getSubjectScheduledAssignmentsPerLesson() {
            return subjectScheduledAssignmentsPerLesson;
        }

        public void setSubjectScheduledAssignmentsPerLesson(String subjectScheduledAssignmentsPerLesson) {
            this.subjectScheduledAssignmentsPerLesson = subjectScheduledAssignmentsPerLesson;
        }

        public String getSubjectMaximumPresentationPoints() {
            return subjectMaximumPresentationPoints;
        }

        public void setSubjectMaximumPresentationPoints(String subjectMaximumPresentationPoints) {
            this.subjectMaximumPresentationPoints = subjectMaximumPresentationPoints;
        }
    }
}

