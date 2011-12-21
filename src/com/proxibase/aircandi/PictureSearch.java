package com.proxibase.aircandi;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
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
import com.proxibase.aircandi.utils.DrawableManager;
import com.proxibase.aircandi.widgets.WebImageView;

public class PictureSearch extends AircandiActivity {

	private GridView			mGridView;
	@SuppressWarnings("unused")
	private Spinner				mCategory;
	private EditText			mSearch;
	private List<ImageResult>	mImages;
	private DrawableManager		mDrawableManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();
		GoogleAnalyticsTracker.getInstance().trackPageView("/AircandiGallery");
	}

	protected void bindEntity() {
		String query = Aircandi.settings.getString(Preferences.SETTING_PICTURE_SEARCH, null);
		if (query == null) {
			query = "trending now site:yahoo.com";
			mSearch.setText(query);
		}
		else {
			mSearch.setText(query);
			query = "wallpaper " + query;
		}
		loadImages(query);
	}

	protected void drawEntity() {}

	private void configure() {
		mDrawableManager = new DrawableManager();
		mGridView = (GridView) findViewById(R.id.grid_gallery);
		mGridView.setAdapter(new ImageAdapter(this));

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				String imageUri = mImages.get(position).getMediaUrl();
				Intent intent = new Intent();
				intent.putExtra(getString(R.string.EXTRA_URI), imageUri);
				setResult(Activity.RESULT_FIRST_USER, intent);
				finish();
			}
		});

		//		mCategory = (Spinner) findViewById(R.id.cbo_category);
		//		mCategory.setOnItemSelectedListener(new OnItemSelectedListener() {
		//
		//			@Override
		//			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		//				mGridView.setAdapter(new ImageAdapter(AircandiGallery.this));
		//				String item = parent.getItemAtPosition(position).toString();
		//				loadImages("wallpaper " + item);
		//			}
		//
		//			@Override
		//			public void onNothingSelected(AdapterView<?> parent) {
		//			}
		//		});

		mSearch = (EditText) findViewById(R.id.text_uri);

		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null) {
			mContextButton.setVisibility(View.INVISIBLE);
			showBackButton(true, getString(R.string.form_button_back));
		}
	}

	private void loadImages(String query) {
		BingSearchServiceClientFactory factory = BingSearchServiceClientFactory.newInstance();
		BingSearchClient client = factory.createBingSearchClient();
		SearchRequestBuilder builder = client.newSearchRequestBuilder();
		builder.withAppId(getBingId());
		builder.withQuery(query);
		builder.withSourceType(SourceType.IMAGE);
		builder.withVersion("2.2");
		builder.withMarket("en-us");
		builder.withAdultOption(AdultOption.MODERATE);
		builder.withImageRequestFilter("Size:Large");
		builder.withImageRequestCount(30L);
		builder.withImageRequestOffset(0L);
		SearchResponse response = client.search(builder.getResult());
		mImages = response.getImage().getResults();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------
	public void onSearchClick(View view) {
		mGridView.setAdapter(new ImageAdapter(PictureSearch.this));
		String item = mSearch.getText().toString();
		Aircandi.settingsEditor.putString(Preferences.SETTING_PICTURE_SEARCH, item);
		Aircandi.settingsEditor.commit();
		loadImages("wallpaper " + item);
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public class ImageAdapter extends BaseAdapter {

		public ImageAdapter(Context c) {}

		public int getCount() {
			if (mImages != null) {
				return mImages.size();
			}
			else {
				return 0;
			}
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			ImageResult itemData = (ImageResult) mImages.get(position);

			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_galleryitem, null);
			}

			if (itemData != null) {
				WebImageView imageView = (WebImageView) view.findViewById(R.id.item_image);
				imageView.setImageBitmap(null);
				mDrawableManager.fetchDrawableOnThread(itemData.getThumbnail().getUrl(), imageView);
			}
			return view;
		}
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

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picture_search;
	}
}