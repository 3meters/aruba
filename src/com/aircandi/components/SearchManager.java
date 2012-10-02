package com.aircandi.components;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.objects.Observation;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Browser;
import android.webkit.WebIconDatabase.IconListener;

@SuppressWarnings("unused")
public class SearchManager {

	private static SearchManager	singletonObject;

	private Context					mContext;
	private final IconReceiver		mIconReceiver	= new IconReceiver();

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
		mContext = context;
	}

	public List<SearchItem> getBookmarks(ContentResolver contentResolver) {

		List<SearchItem> searchItems = null;
		
		/* Need to add the created date column to the query */
		String[] columns = new String[Browser.HISTORY_PROJECTION.length + 1];
		System.arraycopy(Browser.HISTORY_PROJECTION, 0, columns, 0, Browser.HISTORY_PROJECTION.length);
		int createdColumnIndex = columns.length - 1;
		columns[createdColumnIndex] = Browser.BookmarkColumns.CREATED;

		String whereClause = Browser.BookmarkColumns.BOOKMARK + " == 1";
		String orderBy = Browser.BookmarkColumns.CREATED + " DESC";
		String[] selectionArgs = null;
		Cursor cursor = contentResolver.query(
				Browser.BOOKMARKS_URI,
				columns,
				whereClause,
				selectionArgs,
				orderBy);

		searchItems = new ArrayList<SearchItem>();
		Boolean succeeded = cursor.moveToFirst();
		while (succeeded) {
			SearchItem searchItem = new SearchItem();
			searchItem.type = SearchItemType.Bookmarks;
			searchItem.name = cursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX);
			searchItem.uri = cursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
			byte[] data = cursor.getBlob(Browser.HISTORY_PROJECTION_FAVICON_INDEX);
			if (data != null) {
				searchItem.icon = BitmapFactory.decodeByteArray(data, 0, data.length);
			}
			searchItems.add(searchItem);
			succeeded = cursor.moveToNext();
		}

		//		Browser.requestAllIcons(mContentResolver,
		//				Browser.BookmarkColumns.FAVICON
		//						+ " is NULL AND "
		//						+ Browser.BookmarkColumns.BOOKMARK
		//						+ " == 1", mIconReceiver);

		return searchItems;
	}
	
	public List<SearchItem> getPlaceSuggestions() {
		
		List<SearchItem> searchItems = null;
		String uri = ProxiConstants.URL_PROXIBASE_SEARCH_VENUES;
		Observation observation = GeoLocationManager.getInstance().getObservation();
		if (observation != null) {
			uri += "&ll=" + String.valueOf(observation.latitude) + "," + String.valueOf(observation.longitude);
			uri += "&limit=20";
		}

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(uri)
				.setRequestType(RequestType.Get)
				.setResponseFormat(ResponseFormat.Json);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		if (serviceResponse.responseCode == ResponseCode.Success) {
			Object results = ProxibaseService.convertJsonToObjectInternalSmart((String) serviceResponse.data, null);
			serviceResponse.data = results;
		}
		
		if (serviceResponse.responseCode == ResponseCode.Success) {
			
			if (serviceResponse.data != null) {

				searchItems = new ArrayList<SearchItem>();
				LinkedHashMap map = (LinkedHashMap) ((LinkedHashMap) serviceResponse.data).get("response");
				List<LinkedHashMap> venues = (List<LinkedHashMap>) map.get("venues");
				for (LinkedHashMap venue : venues) {
					if (venue.get("url") != null) {
						SearchItem suggestion = new SearchItem();
						suggestion.type = SearchItemType.Suggestions;
						suggestion.name = (String) venue.get("name");
						suggestion.uri = (String) venue.get("url");
						List<LinkedHashMap> categories = (List<LinkedHashMap>) venue.get("categories");
						if (categories != null && categories.size() > 0) {
							suggestion.categoryName = (String) categories.get(0).get("name");
							LinkedHashMap icon = (LinkedHashMap) categories.get(0).get("icon");
							if (icon != null) {
								String prefix = (String) icon.get("prefix");
								if (prefix.substring(prefix.length() - 1).equals("_")) {
									prefix = prefix.substring(0, prefix.length() - 1);
								}
								String suffix = (String) icon.get("suffix");
								suggestion.categoryIconUri = prefix + suffix;
							}
						}
						searchItems.add(suggestion);
					}
				}
			}
		}

		return searchItems;
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

	public static class SearchItem {

		public String			name;
		public String			uri;
		public String			categoryName;
		public String			categoryIconUri;
		public Bitmap			icon;
		public SearchItemType	type;

	}

	public enum SearchItemType {
		Bookmarks, Suggestions
	}

	private class IconReceiver implements IconListener {
		public void onReceivedIcon(String url, Bitmap icon) {
			//updateBookmarkFavicon(mContentResolver, url, icon);
		}
	}

}
