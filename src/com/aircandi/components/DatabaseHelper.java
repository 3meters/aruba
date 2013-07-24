package com.aircandi.components;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.aircandi.Aircandi;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String	DATABASE_NAME		= "aircandi";
	private static final int	DATABASE_VERSION	= 1;

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
		NotificationTable.onUpgrade(database, oldVersion, newVersion);
	}

}
