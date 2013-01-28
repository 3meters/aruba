package com.aircandi.ui.builders;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
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
import android.util.FloatMath;
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
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager.ViewHolder;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceRequest.AuthType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.ui.Preferences;
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
	private ArrayList<ImageResult>	mImages			= new ArrayList<ImageResult>();
	private TextView				mTitle;
	private TextView				mMessage;
	private Entity					mEntity;

	private long					mOffset			= 0;
	private String					mQuery;
	private String					mDefaultSearch;
	private LayoutInflater			mInflater;
	private String					mTitleOptional;
	private Boolean					mPlacePhotoMode	= false;
	private String					mSource;
	private String					mSourceId;
	private Integer					mImageWidthPixels;
	private Integer					mImageMarginPixels;

	private static long				PAGE_SIZE		= 30L;
	private static long				LIST_MAX		= 300L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
		bind();
	}

	private void initialize() {

		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mDrawableManager = new DrawableManager();

		if (mCommon.mEntityId != null) {
			mPlacePhotoMode = true;
		}

		if (mPlacePhotoMode) {
			mEntity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
			mSource = mEntity.place.source;
			mSourceId = mEntity.place.sourceId;
			((ViewGroup) findViewById(R.id.search_group)).setVisibility(View.GONE);
		}
		else {
			Bundle extras = this.getIntent().getExtras();
			if (extras != null) {
				mDefaultSearch = extras.getString(CandiConstants.EXTRA_SEARCH_PHRASE);
			}
			mSearch = (EditText) findViewById(R.id.search_text);
			mSearch.setOnKeyListener(new OnKeyListener() {
	            @Override
				public boolean onKey(View view, int keyCode, KeyEvent event) {
	                if (keyCode == KeyEvent.KEYCODE_ENTER) {
	                    onSearchClick(view);
	                    return true;
	                } else {
	                    return false;
	                }
	            }
	        });			
			if (mDefaultSearch != null) {
				mSearch.setText(mDefaultSearch);
			}
			else {
				mSearch.setText(Aircandi.settings.getString(Preferences.SETTING_PICTURE_SEARCH, null));
			}
			mQuery = mSearch.getText().toString();
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.search_text));
		}

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.message));

		/* Stash some sizing info */
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		View parentView = findViewById(R.id.grid_gallery);
		Integer layoutWidthPixels = metrics.widthPixels - (parentView.getPaddingLeft() + parentView.getPaddingRight());
		
		Integer desiredWidthPixels = (int) (metrics.xdpi * 0.60f);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.ydpi * 0.60f);
		}
		Integer count = (int) FloatMath.ceil(layoutWidthPixels / desiredWidthPixels);
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
		mGridView = (GridView) findViewById(R.id.grid_gallery);
		mGridView.setColumnWidth(mImageWidthPixels);
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				if (((EndlessImageAdapter) mGridView.getAdapter()).getItemViewType(position) != Adapter.IGNORE_ITEM_VIEW_TYPE) {
					String imageUri = mImages.get(position).getMediaUrl();
					if (mPlacePhotoMode) {
						Intent intent = new Intent();
						intent.putExtra(CandiConstants.EXTRA_URI, imageUri);
						setResult(Activity.RESULT_OK, intent);
						finish();
					}
					else {
						Intent intent = new Intent();
						intent.putExtra(CandiConstants.EXTRA_URI, imageUri);
						intent.putExtra(CandiConstants.EXTRA_URI_TITLE, mTitleOptional);
						setResult(Activity.RESULT_OK, intent);
						finish();
					}
				}
			}
		});
	}

	private void bind() {
		mGridView.setAdapter(new EndlessImageAdapter(mImages));
	}

	private ServiceResponse loadSearchImages(String query, long count, long offset) {

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

		ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart((String) serviceResponse.data, ServiceDataType.ImageResult);
		ArrayList<ImageResult> images = (ArrayList<ImageResult>) serviceData.data;
		serviceResponse.data = images;

		return serviceResponse;
	}

	private ServiceResponse loadPlaceImages(long count, long offset) {
		ModelResult result = ProxiExplorer.getInstance().getEntityModel().getPlacePhotos(mSource, mSourceId, count, offset);
		return result.serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSearchClick(View view) {
		mQuery = mSearch.getText().toString();
		mMessage.setVisibility(View.VISIBLE);
		Aircandi.settingsEditor.putString(Preferences.SETTING_PICTURE_SEARCH, mQuery);
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
		Properties properties = new Properties();
		try {
			properties.load(getClass().getResourceAsStream("/com/aircandi/bing_api.properties"));
			String appId = properties.getProperty("appKey");
			return appId;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to retrieve bing appKey");
		}
	}

	@Override
	protected int getLayoutID() {
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

		private ArrayList<ImageResult>	moreImages	= new ArrayList<ImageResult>();

		private EndlessImageAdapter(ArrayList<ImageResult> list) {
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
			moreImages.clear();
			ServiceResponse serviceResponse = new ServiceResponse();
			if (mPlacePhotoMode) {
				serviceResponse = loadPlaceImages(PAGE_SIZE, mOffset);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					ArrayList<Photo> photos = (ArrayList<Photo>) serviceResponse.data;
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
						moreImages = new ArrayList<ImageResult>();
						for (Photo photo : photos) {
							ImageResult imageResult = photo.getAsImageResult();
							imageResult.getThumbnail().setUrl(photo.getSizedUri(100, 100));
							moreImages.add(imageResult);
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
				return (moreImages.size() >= PAGE_SIZE);
			}
			else {
				String queryDecorated = mQuery;
				if (queryDecorated == null || queryDecorated.equals("")) {
					queryDecorated = "trending now site:yahoo.com";
				}
				else {
					queryDecorated = "wallpaper " + queryDecorated;
				}

				serviceResponse = loadSearchImages(queryDecorated, PAGE_SIZE, mOffset);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					moreImages = (ArrayList<ImageResult>) serviceResponse.data;
					if (mOffset == 0 && moreImages.size() == 0) {
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
							+ String.valueOf(getWrappedAdapter().getCount() + moreImages.size()));
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
				return ((getWrappedAdapter().getCount() + moreImages.size()) < LIST_MAX);
			}
		}

		@Override
		protected void appendCachedData() {
			ArrayAdapter<ImageResult> list = (ArrayAdapter<ImageResult>) getWrappedAdapter();
			for (ImageResult imageResult : moreImages) {
				list.add(imageResult);
			}
		}
	}

	private class ListAdapter extends ArrayAdapter<ImageResult> {

		public ListAdapter(ArrayList<ImageResult> list) {
			super(PicturePicker.this, 0, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			ImageResult itemData = (ImageResult) mImages.get(position);

			if (view == null) {
				view = mInflater.inflate(R.layout.temp_picture_search_item, null);
				holder = new ViewHolder();
				holder.itemImage = (ImageView) view.findViewById(R.id.image);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mImageWidthPixels, mImageWidthPixels);
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
		private final HashMap<String, SoftReference<Bitmap>>	mBitmapCache;

		public DrawableManager() {
			mBitmapCache = new HashMap<String, SoftReference<Bitmap>>();
		}

		@SuppressLint("HandlerLeak")
		private void fetchDrawableOnThread(final String uri, final ViewHolder holder) {

			synchronized (mBitmapCache) {
				if (mBitmapCache.containsKey(uri) && mBitmapCache.get(uri).get() != null) {
					BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), mBitmapCache.get(uri).get());
					ImageUtils.showDrawableInImageView(bitmapDrawable, holder.itemImage, false, AnimUtils.fadeInMedium());
					return;
				}
			}

			final DrawableHandler handler = new DrawableHandler(this) {

				@Override
				public void handleMessage(Message message) {
					DrawableManager drawableManager = getDrawableManager().get();
					if (drawableManager != null) {
						if (((String) holder.itemImage.getTag()).equals(uri)) {
							ImageUtils.showDrawableInImageView((Drawable) message.obj, holder.itemImage, true,
									AnimUtils.fadeInMedium());
						}
					}
				}
			};

			Thread thread = new Thread() {

				@Override
				public void run() {
					Thread.currentThread().setName("DrawableManagerFetch");
					Drawable drawable = fetchDrawable(uri);
					Message message = handler.obtainMessage(1, drawable);
					handler.sendMessage(message);
				}
			};
			thread.start();
		}

		private Drawable fetchDrawable(final String uri) {

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(uri)
					.setRequestType(RequestType.Get)
					.setResponseFormat(ResponseFormat.Bytes);

			ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

			if (serviceResponse.responseCode == ResponseCode.Success) {

				byte[] imageBytes = (byte[]) serviceResponse.data;
				Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

				if (bitmap == null) {
					throw new IllegalStateException("Stream could not be decoded to a bitmap: " + uri);
				}
				BitmapDrawable drawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
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