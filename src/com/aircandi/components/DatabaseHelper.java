package com.aircandi.components;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.aircandi.Aircandi;

public class DatabaseHelper extends SQLiteOpenHelper {
	/*
	 * Database upgrade
	 * 
	 * - Set DATABASE_VERSION to new version
	 * - Add logic to onUpgrade to handle from old version to new version
	 * - Logic might have to handle multiple step upgrades: 1->2->3
	 * - Simplest logic is to drop and recreate the database tossing all the data.
	 * - Nicer logic would be to script column renames, additions, data transforms, etc.
	 */

	private static final String	DATABASE_NAME		= "aircandi";
	private static final int	DATABASE_VERSION	= 2;

	private static class DatabaseHelperHolder {
		public static final DatabaseHelper	instance	= new DatabaseHelper(Aircandi.applicationContext);
	}

	public static DatabaseHelper getInstance() {
		return DatabaseHelperHolder.instance;
	}

	private DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		NotificationTable.onCreate(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		try {
			if (oldVersion == 1) {
				NotificationTable.onUpgrade(database, oldVersion, newVersion);
				oldVersion += 1;
			}
			if (oldVersion != DATABASE_VERSION) {
				Logger.e(this, "Need to recreate aircandi schema because of unknown aircandi database version: " + oldVersion);
				dropTables(database);
				onCreate(database);
				oldVersion = DATABASE_VERSION;
			}
		}
		catch (SQLiteException exception) {
			Logger.e(NotificationTable.class, "Error upgrading database: SQLiteException, recreating db.");
			Logger.e(NotificationTable.class, "(oldVersion was " + String.valueOf(oldVersion) + ")");
			dropTables(database);
			onCreate(database);
			return; 	
		}
	}
	
	public void dropTables(SQLiteDatabase database) {
		database.execSQL("DROP TABLE IF EXISTS " + NotificationTable.TABLE_NOTIFICATIONS + ";");
	}
}
