package com.aircandi.components;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

@SuppressWarnings("ucd")
public class NotificationsContentProvider extends ContentProvider {

	// Used for the UriMacher
	private static final int		NOTIFICATIONS		= 10;
	private static final int		NOTIFICATION_ID		= 20;

	private static final String		AUTHORITY			= "com.aircandi.beta.notifications.contentprovider";

	private static final String		BASE_PATH			= "notifications";

	public static final Uri			CONTENT_URI			= Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	public static final String		CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/notifications";
	public static final String		CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/notifications";

	private static final UriMatcher	sURIMatcher			= new UriMatcher(UriMatcher.NO_MATCH);

	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, NOTIFICATIONS);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", NOTIFICATION_ID);
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		/* Using SQLiteQueryBuilder instead of query() method */
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		/* Check if the caller has requested a column which does not exists */
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(NotificationTable.TABLE_NOTIFICATIONS);

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
			case NOTIFICATIONS:
				break;
			case NOTIFICATION_ID:
				/* Adding the ID to the original query */
				queryBuilder.appendWhere(NotificationTable.COLUMN_ID + "=" + uri.getLastPathSegment());
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase database = DatabaseHelper.getInstance().getWritableDatabase();
		Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

		/* Make sure that potential listeners are getting notified */
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		long id = 0;
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase database = DatabaseHelper.getInstance().getWritableDatabase();

		switch (uriType) {
			case NOTIFICATIONS:
				id = database.insert(NotificationTable.TABLE_NOTIFICATIONS, null, values);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return Uri.parse(BASE_PATH + "/" + id);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		int rowsDeleted = 0;
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase database = DatabaseHelper.getInstance().getWritableDatabase();

		switch (uriType) {
			case NOTIFICATIONS:
				rowsDeleted = database.delete(NotificationTable.TABLE_NOTIFICATIONS, selection, selectionArgs);
				break;
			case NOTIFICATION_ID:
				String id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = database.delete(NotificationTable.TABLE_NOTIFICATIONS
							, NotificationTable.COLUMN_ID + "=" + id
							, null);
				}
				else {
					rowsDeleted = database.delete(NotificationTable.TABLE_NOTIFICATIONS
							, NotificationTable.COLUMN_ID + "=" + id + " and " + selection
							, selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);

		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		int rowsUpdated = 0;
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = DatabaseHelper.getInstance().getWritableDatabase();

		switch (uriType) {
			case NOTIFICATIONS:
				rowsUpdated = sqlDB.update(NotificationTable.TABLE_NOTIFICATIONS, values, selection, selectionArgs);
				break;
			case NOTIFICATION_ID:
				String id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(NotificationTable.TABLE_NOTIFICATIONS
							, values
							, NotificationTable.COLUMN_ID + "=" + id
							, null);
				}
				else {
					rowsUpdated = sqlDB.update(NotificationTable.TABLE_NOTIFICATIONS
							, values
							, NotificationTable.COLUMN_ID + "=" + id + " and " + selection
							, selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowsUpdated;
	}

	private void checkColumns(String[] projection) {

		String[] available = { NotificationTable.COLUMN_ID
				, NotificationTable.COLUMN_OBJECT
				, NotificationTable.COLUMN_SENT_DATE };

		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			/* Check if all columns which are requested are available */
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}
}
