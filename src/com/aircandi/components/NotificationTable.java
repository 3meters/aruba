package com.aircandi.components;

import android.database.sqlite.SQLiteDatabase;

public class NotificationTable {
	// Database table
	public static final String	TABLE_NOTIFICATIONS	= "notifications";
	public static final String	COLUMN_ID			= "_id";
	public static final String	COLUMN_SENT_DATE	= "sent_date";
	public static final String	COLUMN_OBJECT		= "object";

	// Database creation SQL statement
	private static final String	DATABASE_CREATE		= "create table "
															+ TABLE_NOTIFICATIONS
															+ "("
															+ COLUMN_ID + " integer primary key autoincrement, "
															+ COLUMN_SENT_DATE + " integer not null, "
															+ COLUMN_OBJECT + " text not null"
															+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Logger.w(null, "Upgrading database from version "
				+ oldVersion
				+ " to "
				+ newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
		onCreate(database);
	}
}
