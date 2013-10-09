package com.aircandi.components;

import android.database.sqlite.SQLiteDatabase;

public class NotificationTable {
	// Database table
	public static final String	TABLE_NOTIFICATIONS				= "notifications";
	public static final String	INDEX_TARGET_ID_ACTION			= "target_id_action_idx";
	public static final String	INDEX_SENT_DATE_ACTION			= "sent_date_idx";
	public static final String	COLUMN_ID						= "_id";
	public static final String	COLUMN_TARGET_ID				= "target_id";
	public static final String	COLUMN_ACTION					= "action";
	public static final String	COLUMN_SENT_DATE				= "sent_date";
	public static final String	COLUMN_OBJECT					= "object";

	// Database creation SQL statement
	private static final String	DATABASE_CREATE					= "create table "
																		+ TABLE_NOTIFICATIONS
																		+ "("
																		+ COLUMN_ID + " integer primary key autoincrement, "
																		+ COLUMN_TARGET_ID + " text not null, "
																		+ COLUMN_ACTION + " text not null, "
																		+ COLUMN_SENT_DATE + " integer not null, "
																		+ COLUMN_OBJECT + " text not null"
																		+ ");";

	private static final String	DATABASE_CREATE_TARGET_ID_INDEX	= "create index "
																		+ INDEX_TARGET_ID_ACTION
																		+ " on "
																		+ TABLE_NOTIFICATIONS + " ("
																		+ COLUMN_TARGET_ID + ", "
																		+ COLUMN_ACTION
																		+ ");";

	private static final String	DATABASE_CREATE_SENT_DATE_INDEX	= "create index "
																		+ INDEX_SENT_DATE_ACTION
																		+ " on "
																		+ TABLE_NOTIFICATIONS + " ("
																		+ COLUMN_SENT_DATE
																		+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		database.execSQL(DATABASE_CREATE_TARGET_ID_INDEX);
		database.execSQL(DATABASE_CREATE_SENT_DATE_INDEX);
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
