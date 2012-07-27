package com.proxibase.aircandi.components;

import java.io.ByteArrayOutputStream;

import com.proxibase.aircandi.BookmarkPicker;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.provider.Browser;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebIconDatabase.IconListener;
import android.widget.BaseAdapter;

public class BookmarkAdapter extends BaseAdapter {

	private Cursor				mCursor;
	private int					mCount;
	private ContentResolver		mContentResolver;
	private ChangeObserver		mChangeObserver;
	private DataSetObserver		mDataSetObserver;
	private boolean				mDataValid;
	private BookmarkPicker		mBookmarkList;
	private final IconReceiver	mIconReceiver	= new IconReceiver();

	public BookmarkAdapter(BookmarkPicker bookmarkList, String curPage) {
		this(bookmarkList, curPage, false);
	}

	public BookmarkAdapter(BookmarkPicker bookmarkList, String curPage, boolean createShortcut) {
		mDataValid = false;
		mBookmarkList = bookmarkList;
		mContentResolver = bookmarkList.getContentResolver();
		mChangeObserver = new ChangeObserver();
		mDataSetObserver = new MyDataSetObserver();
		search();
		Browser.requestAllIcons(mContentResolver,
				Browser.BookmarkColumns.FAVICON
						+ " is NULL AND "
						+ Browser.BookmarkColumns.BOOKMARK
						+ " == 1", mIconReceiver);
	}

	public void refreshList() {
		doSearch();
	}

	public void search() {
		doSearch();
	}

	static void updateBookmarkFavicon(ContentResolver contentResolver, String url, Bitmap favicon) {
		if (url == null || favicon == null) {
			return;
		}
		/* Strip the query. */
		int query = url.indexOf('?');
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

	@SuppressWarnings("deprecation")
	private void doSearch() {
		if (mCursor != null) {
			mCursor.unregisterContentObserver(mChangeObserver);
			mCursor.unregisterDataSetObserver(mDataSetObserver);
			mCursor.deactivate();
		}

		/* Need to add the created date column to the query */
		String[] columns = new String[Browser.HISTORY_PROJECTION.length + 1];
		System.arraycopy(Browser.HISTORY_PROJECTION, 0, columns, 0, Browser.HISTORY_PROJECTION.length);
		int createdColumnIndex = columns.length - 1;
		columns[createdColumnIndex] = Browser.BookmarkColumns.CREATED;

		String whereClause = Browser.BookmarkColumns.BOOKMARK + " == 1";
		String orderBy = Browser.BookmarkColumns.CREATED + " DESC";
		String[] selectionArgs = null;
		mCursor = mContentResolver.query(
				Browser.BOOKMARKS_URI,
				columns,
				whereClause,
				selectionArgs,
				orderBy);
		mCursor.registerContentObserver(mChangeObserver);
		mCursor.registerDataSetObserver(mDataSetObserver);

		mDataValid = true;
		notifyDataSetChanged();

		mCount = mCursor.getCount();
	}

	public int getCount() {
		if (mDataValid) {
			return mCount;
		}
		else {
			return 0;
		}
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (!mDataValid) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		if (position < 0 || position > mCount) {
			throw new AssertionError("BrowserBookmarksAdapter tried to get a view out of range");
		}

		if (convertView == null) {
			convertView = new BookmarkItem(mBookmarkList);
		}
		bind((BookmarkItem) convertView, position);

		return convertView;
	}

	public String getTitle(int position) {
		return getString(Browser.HISTORY_PROJECTION_TITLE_INDEX, position);
	}

	public String getUrl(int position) {
		return getString(Browser.HISTORY_PROJECTION_URL_INDEX, position);
	}

	private String getString(int cursorIndex, int position) {
		if (position < 0 || position > mCount) {
			return "";
		}
		mCursor.moveToPosition(position);
		return mCursor.getString(cursorIndex);
	}

	private void bind(BookmarkItem bookmarkItem, int position) {
		mCursor.moveToPosition(position);

		bookmarkItem.setName(mCursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX));
		bookmarkItem.setUrl(mCursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX));
		byte[] data = mCursor.getBlob(Browser.HISTORY_PROJECTION_FAVICON_INDEX);
		if (data != null) {
			bookmarkItem.setFavicon(BitmapFactory.decodeByteArray(data, 0, data.length));
		}
		else {
			bookmarkItem.setFavicon(null);
		}
	}

	private class ChangeObserver extends ContentObserver {
		public ChangeObserver() {
			super(new Handler());
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			refreshList();
		}
	}

	private class MyDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			mDataValid = true;
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			mDataValid = false;
			notifyDataSetInvalidated();
		}
	}

	private class IconReceiver implements IconListener {
		public void onReceivedIcon(String url, Bitmap icon) {
			updateBookmarkFavicon(mContentResolver, url, icon);
		}
	}

}