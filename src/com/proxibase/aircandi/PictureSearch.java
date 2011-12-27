package com.proxibase.aircandi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.code.bing.search.client.BingSearchClient;
import com.google.code.bing.search.client.BingSearchServiceClientFactory;
import com.google.code.bing.search.client.BingSearchClient.SearchRequestBuilder;
import com.google.code.bing.search.schema.AdultOption;
import com.google.code.bing.search.schema.SearchResponse;
import com.google.code.bing.search.schema.SourceType;
import com.google.code.bing.search.schema.multimedia.ImageResult;
import com.proxibase.aircandi.components.DrawableManager;
import com.proxibase.aircandi.components.EndlessAdapter;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.widgets.WebImageView;

/*
 * We often will get duplicates because the ordering of images isn't 
 * guaranteed while paging.
 */
public class PictureSearch extends AircandiActivity {

	private GridView				mGridView;
	@SuppressWarnings("unused")
	private Spinner					mCategory;
	private EditText				mSearch;
	private ArrayList<ImageResult>	mImages;
	private DrawableManager			mDrawableManager;
	private long					mOffset		= 0;
	private String					mQuery;
	private LayoutInflater			mInflater;
	private static long				PAGE_SIZE	= 20L;
	private static long				LIST_MAX	= 100L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();
		GoogleAnalyticsTracker.getInstance().trackPageView("/AircandiGallery");
	}

	@Override
	protected void unpackIntent(Intent intent) {
	/* Prevent logic based on objects passed with the intent */
	}

	protected void bindEntity() {
		String query = Aircandi.settings.getString(Preferences.SETTING_PICTURE_SEARCH, null);
		if (query == null || query.equals("")) {
			query = "trending now site:yahoo.com";
		}
		else {
			mSearch.setText(query);
			query = "wallpaper " + query;
		}
		mImages = loadImages(query, PAGE_SIZE, 0);
		mOffset += PAGE_SIZE;
		mQuery = query;
		mGridView.setAdapter(new EndlessImageAdapter(mImages));
	}

	protected void drawEntity() {}

	private void configure() {
		mDrawableManager = new DrawableManager();
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mGridView = (GridView) findViewById(R.id.grid_gallery);

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				String imageUri = mImages.get(position).getMediaUrl();
				Intent intent = new Intent();
				intent.putExtra(getString(R.string.EXTRA_URI), imageUri);
				setResult(Activity.RESULT_FIRST_USER, intent);
				finish();
			}
		});

		mSearch = (EditText) findViewById(R.id.text_uri);

		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null) {
			mContextButton.setVisibility(View.INVISIBLE);
			showBackButton(true, getString(R.string.form_button_back));
		}
	}

	private ArrayList<ImageResult> loadImages(String query, long count, long offset) {

		BingSearchServiceClientFactory factory = BingSearchServiceClientFactory.newInstance();

		BingSearchClient client = factory.createBingSearchClient();
		SearchRequestBuilder builder = client.newSearchRequestBuilder();

		builder.withAppId(getBingId());
		builder.withQuery(query);
		builder.withSourceType(SourceType.IMAGE);
		builder.withVersion("2.2");
		//builder.withMarket("en-us");
		builder.withAdultOption(AdultOption.MODERATE);
		builder.withImageRequestFilter("Size:Large");
		builder.withImageRequestCount(count);
		builder.withImageRequestOffset(offset);

		SearchResponse response = client.search(builder.getResult());
		if (response == null) {
			return null;
		}
		
		ArrayList<ImageResult> images = (ArrayList<ImageResult>) response.getImage().getResults();
		return images;
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------
	public void onSearchClick(View view) {

		String query = mSearch.getText().toString();
		mOffset = 0;
		Aircandi.settingsEditor.putString(Preferences.SETTING_PICTURE_SEARCH, query);
		Aircandi.settingsEditor.commit();
		if (query.equals("")) {
			query = "trending now site:yahoo.com";
		}
		else {
			query = "wallpaper " + query;
		}
		mImages = loadImages(query, PAGE_SIZE, 0);
		mOffset += PAGE_SIZE;
		mQuery = query;
		mGridView.setAdapter(new EndlessImageAdapter(mImages));
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	class EndlessImageAdapter extends EndlessAdapter {

		ArrayList<ImageResult>	moreImages	= new ArrayList<ImageResult>();

		EndlessImageAdapter(ArrayList<ImageResult> list) {
			super(new ImageAdapter(list));
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
			moreImages = loadImages(mQuery, PAGE_SIZE, mOffset + 5);
			Logger.d(this, "Query Bing for more images: start = " + String.valueOf(mOffset)
							+ " new total = "
							+ String.valueOf(getWrappedAdapter().getCount() + moreImages.size()));
			mOffset += PAGE_SIZE;
			return ((getWrappedAdapter().getCount() + moreImages.size()) < LIST_MAX);
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

	public class ImageAdapter extends ArrayAdapter<ImageResult> {

		public ImageAdapter(ArrayList<ImageResult> list) {
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
				holder.itemImage = (WebImageView) view.findViewById(R.id.item_image);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				holder.data = itemData;
				unbindDrawables(holder.itemImage);
				holder.itemImage.setImageBitmap(null);
				holder.itemImage.setTag(itemData.getThumbnail().getUrl());
				mDrawableManager.fetchDrawableOnThread(itemData.getThumbnail().getUrl(), holder);
			}
			return view;
		}
	}

	public class ViewHolder {

		public WebImageView	itemImage;
		public Object		data;
	}

	private String getBingId() {
		Properties properties = new Properties();
		try {
			properties.load(getClass().getResourceAsStream("bing_api.properties"));
			String appId = properties.getProperty("appId");
			return appId;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to retrieve bing appId");
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.gc();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picture_search;
	}
}