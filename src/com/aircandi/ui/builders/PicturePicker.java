package com.aircandi.ui.builders;

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.BuildConfig;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager.ViewHolder;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceRequest.AuthType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Provider;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PicturePicker extends FormActivity {

	private DrawableManager			mDrawableManager;

	private GridView				mGridView;
	private EditText				mSearch;
	private final List<ImageResult>	mImages			= new ArrayList<ImageResult>();
	private TextView				mTitle;
	private TextView				mMessage;
	private Entity					mEntity;

	private long					mOffset			= 0;
	private String					mQuery;
	private String					mDefaultSearch;
	private LayoutInflater			mInflater;
	private String					mTitleOptional;
	private Boolean					mPlacePhotoMode	= false;
	private Provider				mProvider;
	private Integer					mImageWidthPixels;
	private Integer					mImageMarginPixels;

	private static final long		PAGE_SIZE		= 30L;
	private static final long		LIST_MAX		= 300L;
	private static final String		QUERY_PREFIX	= "";
	private static final String		QUERY_DEFAULT	= "wallpaper unusual places";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	private void initialize() {

		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mDrawableManager = new DrawableManager();

		if (mCommon.mEntityId != null) {
			mPlacePhotoMode = true;
		}

		if (mPlacePhotoMode) {
			mEntity = EntityManager.getInstance().getEntity(mCommon.mEntityId);
			mProvider = ((Place) mEntity).getProvider();
			((ViewGroup) findViewById(R.id.search_group)).setVisibility(View.GONE);
		}
		else {
			final Bundle extras = this.getIntent().getExtras();
			if (extras != null) {
				mDefaultSearch = extras.getString(Constants.EXTRA_SEARCH_PHRASE);
			}
			mSearch = (EditText) findViewById(R.id.search_text);
			mSearch.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View view, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						onSearchClick(view);
						return true;
					}
					else {
						return false;
					}
				}
			});
			if (mDefaultSearch != null) {
				mSearch.setText(mDefaultSearch);
			}
			else {
				mSearch.setText(Aircandi.settings.getString(Constants.SETTING_PICTURE_SEARCH, null));
			}
			mQuery = mSearch.getText().toString();
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.search_text));
		}

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.message));

		/* Stash some sizing info */
		mGridView = (GridView) findViewById(R.id.grid_gallery);
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final Integer layoutWidthPixels = metrics.widthPixels - (mGridView.getPaddingLeft() + mGridView.getPaddingRight());

		Integer desiredWidthPixels = (int) (metrics.xdpi * 0.60f);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.ydpi * 0.60f);
		}
		final Integer count = (int) Math.ceil(layoutWidthPixels / desiredWidthPixels);
		mImageMarginPixels = ImageUtils.getRawPixels(this, 2);
		mImageWidthPixels = (layoutWidthPixels / count) - (mImageMarginPixels * (count - 1));

		if (isDialog()) {
			mTitle = (TextView) findViewById(R.id.title);
			mTitle.setText(mPlacePhotoMode ? R.string.dialog_picture_picker_place_title : R.string.dialog_picture_picker_search_title);
		}
		else {
			if (mPlacePhotoMode) {
				mCommon.mActionBar.setTitle(mEntity.name);
			}
			else {
				mCommon.mActionBar.setTitle(R.string.dialog_picture_picker_search_title);
			}
		}

		mMessage = (TextView) findViewById(R.id.message);
		mGridView.setColumnWidth(mImageWidthPixels);
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
					final String jsonPhoto = HttpService.convertObjectToJsonSmart(photo, false, true);
					intent.putExtra(Constants.EXTRA_PHOTO, jsonPhoto);
					setResult(Activity.RESULT_OK, intent);
					finish();
				}
			}
		});
	}

	private void bind() {
		/*
		 * First check to see if there are any candi picture children.
		 */
		if (mPlacePhotoMode && mEntity != null) {
			List<Entity> entities = (List<Entity>) mEntity.getLinkedEntitiesByLinkType(Constants.TYPE_LINK_POST, null, Direction.in, false);
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

		mGridView.setAdapter(new EndlessImageAdapter(mImages));
	}

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

		serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		final ServiceData serviceData = HttpService.convertJsonToObjectsSmart((String) serviceResponse.data, ServiceDataType.ImageResult);
		final List<ImageResult> images = (ArrayList<ImageResult>) serviceData.data;
		serviceResponse.data = images;

		return serviceResponse;
	}

	private ServiceResponse loadPlaceImages(long count, long offset) {
		final ModelResult result = EntityManager.getInstance().getPlacePhotos(mProvider, count, offset);
		return result.serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSearchClick(View view) {
		mQuery = mSearch.getText().toString();
		mMessage.setVisibility(View.VISIBLE);
		Aircandi.settingsEditor.putString(Constants.SETTING_PICTURE_SEARCH, mQuery);
		Aircandi.settingsEditor.commit();
		mOffset = 0;
		mTitleOptional = mQuery;
		mImages.clear();
		((EndlessImageAdapter) mGridView.getAdapter()).notifyDataSetChanged();
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

	@Override
	protected int getLayoutId() {
		return R.layout.picker_picture;
	}

	@Override
	protected Boolean isDialog() {
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	private class EndlessImageAdapter extends EndlessAdapter {

		private List<ImageResult>	mMoreImages	= new ArrayList<ImageResult>();

		private EndlessImageAdapter(List<ImageResult> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			if (mImages.size() == 0) {
				return new View(PicturePicker.this);
			}
			return mInflater.inflate(R.layout.temp_picture_search_item_placeholder, null);
		}

		@Override
		protected boolean cacheInBackground() {
			/*
			 * This is called on background thread from an AsyncTask started by EndlessAdapter
			 */
			mMoreImages.clear();
			ServiceResponse serviceResponse = new ServiceResponse();
			if (mPlacePhotoMode) {

				Place place = (Place) mEntity;
				if (place.getProvider().type != null && place.getProvider().type.equals("foursquare")) {
					serviceResponse = loadPlaceImages(PAGE_SIZE, mOffset);
					if (serviceResponse.responseCode == ResponseCode.Success) {
						final List<Photo> photos = (ArrayList<Photo>) serviceResponse.data;
						if (mOffset == 0 && photos.size() == 0) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mMessage.setText(getString(R.string.picture_picker_places_empty) + " " + mEntity.name);
									mMessage.setVisibility(View.VISIBLE);

								}
							});
						}
						else {
							mMoreImages = new ArrayList<ImageResult>();
							for (Photo photo : photos) {
								ImageResult imageResult = photo.getAsImageResult();
								imageResult.setPhoto(photo);
								imageResult.getThumbnail().setUrl(photo.getSizedUri(100, 100));
								mMoreImages.add(imageResult);
							}
						}
					}
					else {
						mCommon.handleServiceError(serviceResponse, ServiceOperation.PictureSearch);
						return false;
					}
					mOffset += PAGE_SIZE;
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mCommon.hideBusy(true);
						}
					});
					return mMoreImages.size() >= PAGE_SIZE;
				}
				else {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mCommon.hideBusy(true);
						}
					});
					return false;
				}
			}
			else {
				String queryDecorated = mQuery;
				if (queryDecorated == null || queryDecorated.equals("")) {
					queryDecorated = QUERY_DEFAULT;
				}
				else {
					queryDecorated = QUERY_PREFIX + " " + queryDecorated;
				}

				serviceResponse = loadSearchImages(queryDecorated, PAGE_SIZE, mOffset);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mMoreImages = (ArrayList<ImageResult>) serviceResponse.data;
					if (mOffset == 0 && mMoreImages.size() == 0) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mMessage.setText(getString(R.string.picture_picker_search_empty) + " " + mQuery);
								mMessage.setVisibility(View.VISIBLE);

							}
						});
					}
					Logger.d(this, "Query Bing for more images: start = " + String.valueOf(mOffset)
							+ " new total = "
							+ String.valueOf(getWrappedAdapter().getCount() + mMoreImages.size()));
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.PictureSearch);
					return false;
				}
				mOffset += PAGE_SIZE;
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mCommon.hideBusy(true);
					}
				});
				return (getWrappedAdapter().getCount() + mMoreImages.size()) < LIST_MAX;
			}
		}

		@Override
		protected void appendCachedData() {
			final ArrayAdapter<ImageResult> list = (ArrayAdapter<ImageResult>) getWrappedAdapter();
			for (ImageResult imageResult : mMoreImages) {
				list.add(imageResult);
			}
			notifyDataSetChanged();
		}
	}

	private class ListAdapter extends ArrayAdapter<ImageResult> {

		private ListAdapter(List<ImageResult> list) {
			super(PicturePicker.this, 0, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			final ImageResult itemData = mImages.get(position);

			if (view == null) {
				view = mInflater.inflate(R.layout.temp_picture_search_item, null);
				holder = new ViewHolder();
				holder.itemImage = (ImageView) view.findViewById(R.id.image);
				final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mImageWidthPixels, mImageWidthPixels);
				holder.itemImage.setLayoutParams(params);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
				if (holder.itemImage.getTag().equals(itemData.getThumbnail().getUrl())) {
					return view;
				}
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
					ImageUtils.showDrawableInImageView(bitmapDrawable, holder.itemImage, false, AnimUtils.fadeInMedium());
					return;
				}
			}

			final DrawableHandler handler = new DrawableHandler(this) {

				@Override
				public void handleMessage(Message message) {
					final DrawableManager drawableManager = getDrawableManager().get();
					if (drawableManager != null) {
						if (((String) holder.itemImage.getTag()).equals(uri)) {
							ImageUtils.showDrawableInImageView((Drawable) message.obj, holder.itemImage, true,
									AnimUtils.fadeInMedium());
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

			final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

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