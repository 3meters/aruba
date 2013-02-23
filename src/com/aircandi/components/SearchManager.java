package com.aircandi.components;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Browser;

@SuppressWarnings("ucd")
public class SearchManager {

	private static SearchManager	singletonObject;

	public static synchronized SearchManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new SearchManager();
		}
		return singletonObject;
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private SearchManager() {}

	public void setContext(Context context) {
	}

	public List<SearchItem> getBookmarks(ContentResolver contentResolver) {

		List<SearchItem> searchItems = null;

		/* Need to add the created date column to the query */
		final String[] columns = new String[Browser.HISTORY_PROJECTION.length + 1];
		System.arraycopy(Browser.HISTORY_PROJECTION, 0, columns, 0, Browser.HISTORY_PROJECTION.length);
		final int createdColumnIndex = columns.length - 1;
		columns[createdColumnIndex] = Browser.BookmarkColumns.CREATED;

		final String whereClause = Browser.BookmarkColumns.BOOKMARK + " == 1";
		final String orderBy = Browser.BookmarkColumns.CREATED + " DESC";
		final String[] selectionArgs = null;
		final Cursor cursor = contentResolver.query(
				Browser.BOOKMARKS_URI,
				columns,
				whereClause,
				selectionArgs,
				orderBy);

		searchItems = new ArrayList<SearchItem>();
		Boolean succeeded = cursor.moveToFirst();
		byte[] data = null;		
		while (succeeded) {
			SearchItem searchItem = new SearchItem();
			searchItem.type = SearchItemType.Bookmarks;
			searchItem.name = cursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX);
			searchItem.uri = cursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
			data = cursor.getBlob(Browser.HISTORY_PROJECTION_FAVICON_INDEX);
			if (data != null) {
				searchItem.icon = BitmapFactory.decodeByteArray(data, 0, data.length);
			}
			searchItems.add(searchItem);
			succeeded = cursor.moveToNext();
		}

		return searchItems;
	}

	static void updateBookmarkFavicon(ContentResolver contentResolver, String url, Bitmap favicon) {
		if (url == null || favicon == null) {
			return;
		}
		/* Strip the query. */
		final int query = url.indexOf('?');
		String noQuery = url;
		if (query != -1) {
			noQuery = url.substring(0, query);
		}
		url = noQuery + '?';
		/*
		 * Use noQuery to search for the base url (i.e. if the url is http://www.yahoo.com/?rs=1, search for
		 * http://www.yahoo.com) Use url to match the base url with other queries (i.e. if the url is
		 * http://www.google.com/m, search for http://www.google.com/m?some_query)
		 */
		final String[] selArgs = new String[] { noQuery, url };
		final String where = "(" + Browser.BookmarkColumns.URL + " == ? OR "
				+ Browser.BookmarkColumns.URL + " GLOB ? || '*') AND "
				+ Browser.BookmarkColumns.BOOKMARK + " == 1";
		final String[] projection = new String[] { Browser.BookmarkColumns._ID };
		final Cursor c = contentResolver.query(Browser.BOOKMARKS_URI, projection, where,
				selArgs, null);
		boolean succeed = c.moveToFirst();
		ContentValues values = null;
		while (succeed) {
			if (values == null) {
				final ByteArrayOutputStream os = new ByteArrayOutputStream();
				favicon.compress(Bitmap.CompressFormat.PNG, 100, os);
				values = new ContentValues();
				values.put(Browser.BookmarkColumns.FAVICON, os.toByteArray());
			}
			contentResolver.update(ContentUris.withAppendedId(Browser.BOOKMARKS_URI, c
					.getInt(0)), values, null, null);
			succeed = c.moveToNext();
		}
		c.close();
	}

	@SuppressWarnings("ucd")
	public static class SearchItem {

		public String			name;
		public String			uri;
		public String			categoryName;
		public String			categoryIconUri;
		public Bitmap			icon;
		public SearchItemType	type;

	}

	@SuppressWarnings("ucd")
	enum SearchItemType {
		Bookmarks, Suggestions
	}
}
