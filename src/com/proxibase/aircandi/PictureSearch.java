package com.proxibase.aircandi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;

import com.proxibase.aircandi.components.DrawableManager;
import com.proxibase.aircandi.components.EndlessAdapter;
import com.proxibase.aircandi.components.ImageResult;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.ServiceRequest.AuthType;
import com.proxibase.service.objects.ServiceData;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PictureSearch extends FormActivity {

	private GridView				mGridView;
	@SuppressWarnings("unused")
	private Spinner					mCategory;
	private EditText				mSearch;
	private ArrayList<ImageResult>	mImages;
	private DrawableManager			mDrawableManager;
	private long					mOffset		= 0;
	private String					mQuery;
	private LayoutInflater			mInflater;
	private String					mTitleOptional;
	private static long				PAGE_SIZE	= 30L;
	private static long				LIST_MAX	= 300L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
	}

	private void initialize() {

		mDrawableManager = new DrawableManager();
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mGridView = (GridView) findViewById(R.id.grid_gallery);

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				String imageUri = mImages.get(position).getMediaUrl();
				String imageDescription = mImages.get(position).getTitle();
				Intent intent = new Intent();
				intent.putExtra(getString(R.string.EXTRA_URI), imageUri);
				intent.putExtra(getString(R.string.EXTRA_URI_TITLE), mTitleOptional);
				intent.putExtra(getString(R.string.EXTRA_URI_DESCRIPTION), imageDescription);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});

		mSearch = (EditText) findViewById(R.id.text_uri);
		String query = Aircandi.settings.getString(Preferences.SETTING_PICTURE_SEARCH, null);
		mSearch.setText(query);
	}

	protected void bind() {

		new AsyncTask<Object, Object, Object>() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_searching));
			}

			@Override
			protected Object doInBackground(Object... params) {

				String query = mSearch.getText().toString();
				Aircandi.settingsEditor.putString(Preferences.SETTING_PICTURE_SEARCH, query);
				Aircandi.settingsEditor.commit();

				mOffset = 0;
				mTitleOptional = query;
				if (query == null || query.equals("")) {
					query = "trending now site:yahoo.com";
				}
				else {
					query = "wallpaper " + query;
				}

				mQuery = query;
				ServiceResponse serviceResponse = loadImages(query, PAGE_SIZE, 0);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mImages = (ArrayList<ImageResult>) serviceResponse.data;
					mOffset += PAGE_SIZE;
					mCommon.showProgressDialog(false, null);
					mGridView.setAdapter(new EndlessImageAdapter(mImages));
				}
				else {
					mCommon.handleServiceError(serviceResponse);
				}
			}
		}.execute();

	}

	private ServiceResponse loadImages(String query, long count, long offset) {

		ServiceResponse serviceResponse = new ServiceResponse();
		try {
			query = "%27" + URLEncoder.encode(query, "UTF-8") + "%27";
		}
		catch (UnsupportedEncodingException exception) {
			exception.printStackTrace();
		}
		
		String bingUrl = ProxiConstants.URL_PROXIBASE_SEARCH_IMAGES
				+ "?Query=" + query 
				+ "&Market=%27en-US%27&Adult=%27Moderate%27&ImageFilters=%27size%3alarge%27"
				+ "&$top=" + String.valueOf(count) 
				+ "&$skip=" + String.valueOf(offset) 
				+ "&$format=Json";

		ServiceRequest serviceRequest = new ServiceRequest(bingUrl, RequestType.Get, ResponseFormat.Json);
		serviceRequest.setAuthType(AuthType.Basic)
				.setUserName(null)
				.setPassword(getBingKey());

		serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		ServiceData serviceData = ProxibaseService.convertJsonToObjects((String)serviceResponse.data, ImageResult.class, GsonType.BingService);
		ArrayList<ImageResult> images = (ArrayList<ImageResult>) serviceData.data;
		serviceResponse.data = images;

		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSearchClick(View view) {
		bind();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.gc();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	private String getBingKey() {
		Properties properties = new Properties();
		try {
			properties.load(getClass().getResourceAsStream("bing_api.properties"));
			String appId = properties.getProperty("appKey");
			return appId;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to retrieve bing appKey");
		}
	}

	protected void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			try {
				((ViewGroup) view).removeAllViews();
			}
			catch (Throwable e) {
				// NOP
			}
		}
	}

	protected void unbindImageViewDrawables(ImageView imageView) {
		if (imageView.getBackground() != null) {
			imageView.getBackground().setCallback(null);
		}
		if (imageView.getDrawable() != null) {
			imageView.getDrawable().setCallback(null);
			((BitmapDrawable) imageView.getDrawable()).getBitmap().recycle();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picture_search;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	class EndlessImageAdapter extends EndlessAdapter {

		ArrayList<ImageResult>	moreImages	= new ArrayList<ImageResult>();

		EndlessImageAdapter(ArrayList<ImageResult> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			View view = mInflater.inflate(R.layout.temp_picture_search_item_placeholder, null);
			return (view);
		}

		@Override
		protected boolean cacheInBackground() {
			/* What happens if there is a connectivity error? */
			moreImages.clear();

			ServiceResponse serviceResponse = loadImages(mQuery, PAGE_SIZE, mOffset);
			if (serviceResponse.responseCode == ResponseCode.Success) {
				moreImages = (ArrayList<ImageResult>) serviceResponse.data;
				Logger.d(this, "Query Bing for more images: start = " + String.valueOf(mOffset)
						+ " new total = "
						+ String.valueOf(getWrappedAdapter().getCount() + moreImages.size()));
				mOffset += PAGE_SIZE;
				return ((getWrappedAdapter().getCount() + moreImages.size()) < LIST_MAX);
			}
			else {
				mCommon.handleServiceError(serviceResponse);
				return false;
			}
		}

		@Override
		protected void appendCachedData() {

			@SuppressWarnings("unchecked")
			ArrayAdapter<ImageResult> list = (ArrayAdapter<ImageResult>) getWrappedAdapter();

			for (ImageResult imageResult : moreImages) {
				list.add(imageResult);
			}
		}
	}

	private class ListAdapter extends ArrayAdapter<ImageResult> {

		public ListAdapter(ArrayList<ImageResult> list) {
			super(PictureSearch.this, 0, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			ImageResult itemData = (ImageResult) mImages.get(position);

			if (view == null) {
				view = mInflater.inflate(R.layout.temp_picture_search_item, null);
				holder = new ViewHolder();
				holder.itemImage = (ImageView) view.findViewById(R.id.item_image);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				holder.data = itemData;
				holder.itemImage.setTag(itemData.getThumbnail().getUrl());
				holder.itemImage.setImageBitmap(null);
				mDrawableManager.fetchDrawableOnThread(itemData.getThumbnail().getUrl(), holder);
			}
			return view;
		}
	}

	public static class ViewHolder {

		public ImageView	itemImage;
		public Object		data;
	}

}