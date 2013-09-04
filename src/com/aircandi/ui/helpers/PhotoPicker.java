package com.aircandi.ui.helpers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.BuildConfig;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager.ViewHolder;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataWrapper;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceRequest.AuthType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Provider;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.widgets.AirAutoCompleteTextView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PhotoPicker extends BaseBrowse {

	private DrawableManager			mDrawableManager;

	private GridView				mGridView;
	private AirAutoCompleteTextView	mSearch;
	private final List<ImageResult>	mImages				= new ArrayList<ImageResult>();
	private TextView				mMessage;
	private Entity					mEntity;
	private String					mEntityId;

	private long					mOffset				= 0;
	private String					mQuery;
	private String					mDefaultSearch;
	private List<String>			mPreviousSearches	= new ArrayList<String>();
	private ArrayAdapter<String>	mSearchAdapter;
	private String					mTitleOptional;
	private Boolean					mPlacePhotoMode		= false;
	private Provider				mProvider;
	private Integer					mPhotoWidthPixels;

	private static final long		PAGE_SIZE			= 30L;
	private static final long		LIST_MAX			= 300L;
	private static final String		QUERY_PREFIX		= "";
	private static final String		QUERY_DEFAULT		= "wallpaper unusual places";

	@Override
	protected void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing() && !mPlacePhotoMode) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		}
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mDrawableManager = new DrawableManager();

		if (mEntityId != null) {
			mPlacePhotoMode = true;
		}

		mSearch = (AirAutoCompleteTextView) findViewById(R.id.search_text);

		if (mPlacePhotoMode) {
			mEntity = EntityManager.getEntity(mEntityId);
			mProvider = ((Place) mEntity).getProvider();
			mSearch.setVisibility(View.GONE);
			showBusy(R.string.progress_searching);
		}
		else {

			final Bundle extras = this.getIntent().getExtras();
			if (extras != null) {
				mDefaultSearch = extras.getString(Constants.EXTRA_SEARCH_PHRASE);
			}

			if (mDefaultSearch != null && !mDefaultSearch.equals("")) {
				mSearch.setText(mDefaultSearch);
			}
			else {
				String lastSearch = Aircandi.settings.getString(Constants.SETTING_PICTURE_SEARCH_LAST, null);
				if (lastSearch != null && !lastSearch.equals("")) {
					mSearch.setText(lastSearch);
				}
			}

			mSearch.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View view, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						startSearch(view);
						return true;
					}
					else {
						return false;
					}
				}
			});

			mSearch.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					startSearch(view);
				}
			});

			mSearch.requestFocus();
		}

		mMessage = (TextView) findViewById(R.id.message);
		mGridView = (GridView) findViewById(R.id.grid);

		/* Set spacing */
		Integer requestedHorizontalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_horizontal);
		Integer requestedVerticalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_vertical);
		mGridView.setHorizontalSpacing(requestedHorizontalSpacing);
		mGridView.setVerticalSpacing(requestedVerticalSpacing);

		/* Stash some sizing info */
		final DisplayMetrics metrics = mResources.getDisplayMetrics();
		final Integer availableSpace = metrics.widthPixels - mGridView.getPaddingLeft() - mGridView.getPaddingRight();

		Integer requestedColumnWidth = mResources.getDimensionPixelSize(R.dimen.grid_column_width_requested_medium);

		Integer mNumColumns = (availableSpace + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
		if (mNumColumns <= 0) {
			mNumColumns = 1;
		}

		int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) - ((mNumColumns - 1) * requestedHorizontalSpacing);

		mPhotoWidthPixels = requestedColumnWidth + spaceLeftOver / mNumColumns;

		if (mPlacePhotoMode) {
			setActivityTitle(mEntity.name);
		}
		else {
			setActivityTitle(getString(R.string.dialog_photo_picker_search_title));
		}

		mGridView.setColumnWidth(mPhotoWidthPixels);
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				if (((EndlessImageAdapter) mGridView.getAdapter()).getItemViewType(position) != Adapter.IGNORE_ITEM_VIEW_TYPE) {

					ImageResult imageResult = mImages.get(position);
					Photo photo = imageResult.getPhoto();
					if (photo == null) {
						photo = new Photo(imageResult.getMediaUrl(), null, null, null, PhotoSource.external);
					}

					final Intent intent = new Intent();
					intent.putExtra(Constants.EXTRA_URI_TITLE, mTitleOptional);
					final String jsonPhoto = HttpService.objectToJson(photo);
					intent.putExtra(Constants.EXTRA_PHOTO, jsonPhoto);
					setResult(Activity.RESULT_OK, intent);
					finish();
				}
			}
		});

		if (mPlacePhotoMode) {
			databind();
		}
		else {
			/* Autocomplete */
			initAutoComplete();
			bindAutoCompleteAdapter();
		}
	}

	@Override
	public void databind() {
		/*
		 * First check to see if there are any candi picture children.
		 */
		if (mPlacePhotoMode && mEntity != null) {

			List<String> schemas = new ArrayList<String>();
			schemas.add(Constants.SCHEMA_ENTITY_PICTURE);
			List<String> linkTypes = new ArrayList<String>();
			linkTypes.add(Constants.TYPE_LINK_PICTURE);

			List<Entity> entities = (List<Entity>) mEntity.getLinkedEntitiesByLinkTypes(linkTypes
					, schemas
					, Direction.in
					, false);

			for (Entity entity : entities) {
				if (entity.photo != null) {
					if (!entity.photo.getSourceName().equals(PhotoSource.foursquare)) {
						ImageResult imageResult = entity.photo.getAsImageResult();
						imageResult.setPhoto(entity.photo);
						mImages.add(imageResult);
					}
				}
			}
		}

		if (mPlacePhotoMode || (mQuery != null && !mQuery.equals(""))) {
			mGridView.setAdapter(new EndlessImageAdapter(mImages));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSearchClick(View view) {
		startSearch(view);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void startSearch(View view) {

		mSearch.dismissDropDown();

		mQuery = mSearch.getText().toString().trim();

		/* Prep the UI */
		mMessage.setVisibility(View.GONE);
		mImages.clear();
		showBusy();

		/* Hide soft keyboard */
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);

		/* Stash query so we can restore it in the future */
		Aircandi.settingsEditor.putString(Constants.SETTING_PICTURE_SEARCH_LAST, mQuery);
		Aircandi.settingsEditor.commit();

		/* Add query to auto complete array */
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Aircandi.settings.getString(Constants.SETTING_PICTURE_SEARCHES, "{}"));
			jsonSearchMap.put(mQuery, mQuery);
			Aircandi.settingsEditor.putString(Constants.SETTING_PICTURE_SEARCHES, jsonSearchMap.toString());
			Aircandi.settingsEditor.commit();
		}
		catch (JSONException exception) {
			exception.printStackTrace();
		}

		/* Make sure the latest search appears in auto complete */
		initAutoComplete();
		bindAutoCompleteAdapter();

		mOffset = 0;
		mTitleOptional = mQuery;

		/* Trigger the adapter */
		mGridView.setAdapter(new EndlessImageAdapter(mImages));
	}

	private String getBingKey() {
		final Properties properties = new Properties();
		try {
			properties.load(getClass().getResourceAsStream("/com/aircandi/bing_api.properties"));
			final String appId = properties.getProperty("appKey");
			return appId;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to retrieve bing appKey");
		}
	}

	private void initAutoComplete() {
		try {
			org.json.JSONObject jsonSearchMap = new org.json.JSONObject(Aircandi.settings.getString(Constants.SETTING_PICTURE_SEARCHES, "{}"));
			mPreviousSearches.clear();
			if (mDefaultSearch != null) {
				jsonSearchMap.put(mDefaultSearch, mDefaultSearch);
			}
			org.json.JSONArray jsonSearches = jsonSearchMap.names();
			if (jsonSearches != null) {
				for (int i = 0; i < jsonSearches.length(); i++) {
					String name = jsonSearches.getString(i);
					mPreviousSearches.add(jsonSearchMap.getString(name));
				}
			}
		}
		catch (JSONException exception) {
			exception.printStackTrace();
		}
	}

	private void bindAutoCompleteAdapter() {
		mSearchAdapter = new ArrayAdapter<String>(this
				, android.R.layout.simple_dropdown_item_1line
				, mPreviousSearches);
		mSearch.setAdapter(mSearchAdapter);
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	private ServiceResponse loadSearchImages(String query, long count, long offset) {

		ServiceResponse serviceResponse = new ServiceResponse();
		try {
			query = "%27" + URLEncoder.encode(query, "UTF-8") + "%27";
		}
		catch (UnsupportedEncodingException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}

		final String bingUrl = ProxiConstants.URL_PROXIBASE_SEARCH_IMAGES
				+ "?Query=" + query
				+ "&Market=%27en-US%27&Adult=%27Strict%27&ImageFilters=%27size%3alarge%27"
				+ "&$top=" + String.valueOf(count)
				+ "&$skip=" + String.valueOf(offset)
				+ "&$format=Json";

		final ServiceRequest serviceRequest = new ServiceRequest(bingUrl, RequestType.Get, ResponseFormat.Json);
		serviceRequest.setAuthType(AuthType.Basic)
				.setUserName(null)
				.setPassword(getBingKey());

		serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);

		final ServiceData serviceData = (ServiceData) HttpService.jsonToObjects((String) serviceResponse.data, ObjectType.ImageResult, ServiceDataWrapper.True);
		final List<ImageResult> images = (ArrayList<ImageResult>) serviceData.data;
		serviceResponse.data = images;

		return serviceResponse;
	}

	private ServiceResponse loadPlaceImages(long count, long offset) {
		final ModelResult result = EntityManager.getInstance().getPlacePhotos(mProvider, count, offset);
		return result.serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.gc();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.photo_picker;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------
	// Adapter
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Adapter
	// --------------------------------------------------------------------------------------------

	private class EndlessImageAdapter extends EndlessAdapter {

		private List<ImageResult>	mMoreImages	= new ArrayList<ImageResult>();

		private EndlessImageAdapter(List<ImageResult> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected boolean cacheInBackground() {
			/*
			 * Triggered first time the adapter runs and when this function reported
			 * more available and the special pending view is being rendered by getView.
			 * Returning true means we think there are more items available to query for.
			 * 
			 * This is called on background thread from an AsyncTask started by EndlessAdapter.
			 * We load some data plus report whether there is more data available. If more data is
			 * available, the pending view is appended.
			 */
			mMoreImages.clear();
			ServiceResponse serviceResponse = new ServiceResponse();
			if (mPlacePhotoMode) {

				Place place = (Place) mEntity;
				/*
				 * Place provider is foursquare
				 */
				if (place.getProvider().type != null && place.getProvider().type.equals("foursquare")) {

					serviceResponse = loadPlaceImages(PAGE_SIZE, mOffset);
					if (serviceResponse.responseCode == ResponseCode.Success) {
						final List<Photo> photos = (ArrayList<Photo>) serviceResponse.data;
						if (photos.size() == 0) {
							if (mOffset == 0) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										mMessage.setText(getString(R.string.picture_picker_places_empty) + " " + mEntity.name);
										mMessage.setVisibility(View.VISIBLE);

									}
								});
							}
							hideBusy();
							return false;
						}
						else {
							mMoreImages = new ArrayList<ImageResult>();
							for (Photo photo : photos) {
								ImageResult imageResult = photo.getAsImageResult();
								imageResult.setPhoto(photo);
								imageResult.getThumbnail().setUrl(photo.getSizedUri(100, 100));
								mMoreImages.add(imageResult);
							}
							mOffset += PAGE_SIZE;
							hideBusy();
							return mMoreImages.size() >= PAGE_SIZE;
						}
					}
					else {
						hideBusy();
						Routing.serviceError(PhotoPicker.this, serviceResponse);
						return false;
					}

				}
				else {
					hideBusy();
					return false;
				}
			}
			else {
				String queryDecorated = mQuery;
				if (queryDecorated == null || queryDecorated.equals("")) {
					queryDecorated = QUERY_DEFAULT;
				}
				else {
					queryDecorated = (QUERY_PREFIX + " " + queryDecorated).trim();
				}

				serviceResponse = loadSearchImages(queryDecorated, PAGE_SIZE, mOffset);

				if (serviceResponse.responseCode == ResponseCode.Success) {

					mMoreImages = (ArrayList<ImageResult>) serviceResponse.data;

					if (mMoreImages.size() == 0) {
						if (mOffset == 0) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mMessage.setText(getString(R.string.picture_picker_search_empty) + " " + mQuery);
									mMessage.setVisibility(View.VISIBLE);

								}
							});
						}

						hideBusy();
						return false;
					}
					else {
						Logger.d(this, "Query Bing for more images: start = " + String.valueOf(mOffset)
								+ " new total = "
								+ String.valueOf(getWrappedAdapter().getCount() + mMoreImages.size()));

						if (mMoreImages.size() < PAGE_SIZE) {
							hideBusy();
							return false;
						}
						else {
							mOffset += PAGE_SIZE;
							hideBusy();
							return (getWrappedAdapter().getCount() + mMoreImages.size()) < LIST_MAX;
						}
					}
				}
				else {
					hideBusy();
					Routing.serviceError(PhotoPicker.this, serviceResponse);
					return false;
				}
			}
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			/*
			 * Gets called when adapter is being asked for a view for the last position
			 * and the previous call to cacheInBackground reported that more = true. Also starts
			 * another call to cacheInBackground().
			 */
			if (mImages.size() == 0) {
				/* If nothing to show, return something empty. */
				return new View(PhotoPicker.this);
			}
			return LayoutInflater.from(PhotoPicker.this).inflate(R.layout.temp_picture_search_item_placeholder, null);
		}

		@Override
		protected void appendCachedData() {
			/*
			 * Is called immediately after cacheInBackground regardless
			 * of whether it returned true/false.
			 */
			final ArrayAdapter<ImageResult> list = (ArrayAdapter<ImageResult>) getWrappedAdapter();
			for (ImageResult imageResult : mMoreImages) {
				list.add(imageResult);
			}
			notifyDataSetChanged();
		}
	}

	private class ListAdapter extends ArrayAdapter<ImageResult> {

		private ListAdapter(List<ImageResult> list) {
			super(PhotoPicker.this, 0, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			final ImageResult itemData = mImages.get(position);

			if (view == null) {
				view = LayoutInflater.from(PhotoPicker.this).inflate(R.layout.temp_picture_search_item, null);
				holder = new ViewHolder();
				holder.photoView = (ImageView) view.findViewById(R.id.photo);
				Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
				final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - nudge);
				holder.photoView.setLayoutParams(params);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
				if (holder.photoView.getTag().equals(itemData.getThumbnail().getUrl())) {
					return view;
				}
			}

			if (itemData != null) {
				holder.data = itemData;
				holder.photoView.setTag(itemData.getThumbnail().getUrl());
				holder.photoView.setImageBitmap(null);
				mDrawableManager.fetchDrawableOnThread(itemData.getThumbnail().getUrl(), holder);
			}
			return view;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private class DrawableManager {
		/*
		 * Serves up BitmapDrawables but caches just the bitmap. The cache holds
		 * a soft reference to the bitmap that allows the gc to collect it if memory
		 * needs to be freed. If collected, we download the bitmap again.
		 */
		private final Map<String, SoftReference<Bitmap>>	mBitmapCache;

		private DrawableManager() {
			mBitmapCache = new HashMap<String, SoftReference<Bitmap>>();
		}

		@SuppressLint("HandlerLeak")
		private void fetchDrawableOnThread(final String uri, final ViewHolder holder) {

			synchronized (mBitmapCache) {
				if (mBitmapCache.containsKey(uri) && mBitmapCache.get(uri).get() != null) {
					final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), mBitmapCache.get(uri).get());
					UI.showDrawableInImageView(bitmapDrawable, holder.photoView, false, Animate.fadeInMedium());
					return;
				}
			}

			final DrawableHandler handler = new DrawableHandler(this) {

				@Override
				public void handleMessage(Message message) {
					final DrawableManager drawableManager = getDrawableManager().get();
					if (drawableManager != null) {
						if (((String) holder.photoView.getTag()).equals(uri)) {
							UI.showDrawableInImageView((Drawable) message.obj, holder.photoView, true,
									Animate.fadeInMedium());
						}
					}
				}
			};

			final Thread thread = new Thread() {

				@Override
				public void run() {
					Thread.currentThread().setName("DrawableManagerFetch");
					final Drawable drawable = fetchDrawable(uri);
					final Message message = handler.obtainMessage(1, drawable);
					handler.sendMessage(message);
				}
			};
			thread.start();
		}

		private Drawable fetchDrawable(final String uri) {

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(uri)
					.setRequestType(RequestType.Get)
					.setResponseFormat(ResponseFormat.Bytes);

			final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);

			if (serviceResponse.responseCode == ResponseCode.Success) {

				final byte[] imageBytes = (byte[]) serviceResponse.data;
				final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

				if (bitmap == null) {
					throw new IllegalStateException("Stream could not be decoded to a bitmap: " + uri);
				}
				final BitmapDrawable drawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
				mBitmapCache.put(uri, new SoftReference(bitmap));
				return drawable;
			}
			return null;
		}

		/*
		 * We add a weak reference to the containing class which can
		 * be checked when handling messages to ensure we don't leak memory.
		 */
	}

	private static class DrawableHandler extends Handler {

		private final WeakReference<DrawableManager>	mDrawableManager;

		private DrawableHandler(DrawableManager drawableManager) {
			mDrawableManager = new WeakReference<DrawableManager>(drawableManager);
		}

		public WeakReference<DrawableManager> getDrawableManager() {
			return mDrawableManager;
		}
	}

}