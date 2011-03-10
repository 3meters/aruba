package com.georain.ripple.controller;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.georain.ripple.model.Egg;
import com.georain.ripple.utilities.DateUtils;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.RippleService.QueryFormat;

public class EggsGrid extends RippleActivity
{
	private ArrayList<Object>	mListItems	= null;
	private ArrayList<Object>	mQuoteItems	= null;
	private Class				mClass		= Egg.class;
	private ImageCache			mImageCache	= null;
	private GridView			mGridView	= null;
	private ImageView			mEggDetailImage;
	private TextView			mEggDetailTitle;
	private TextView			mEggDetailDesc;
	private TextView			mEggDetailMessage;
	private TextView			mEggDetailFinder;
	private Button				mEggDetailTakeButton;
	private Button				mEggDetailKeepButton;
	private Button				mEggDetailDiscardButton;
	private EditText			mEggsDropMessageEdit;
	private RelativeLayout		mEggDetailRow;
	private RelativeLayout		mEggDropRow;
	private LinearLayout		mEggDropperRow;

	private LocationManager		mLocManager;
	private LocationListener	mLocListener;

	public static EggsGrid		mSelf		= null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.eggs_grid);
		super.onCreate(savedInstanceState);
		mSelf = this;

		mGridView = (GridView) findViewById(R.id.gridview);
		mEggDetailRow = (RelativeLayout) findViewById(R.id.Egg_Detail_Row);
		mEggDropRow = (RelativeLayout) findViewById(R.id.Egg_Drop_Row);
		mEggDropperRow = (LinearLayout) findViewById(R.id.Egg_Dropper_Row);
		mEggDetailImage = (ImageView) findViewById(R.id.Egg_Detail_Image);
		mEggDetailTitle = (TextView) findViewById(R.id.Egg_Detail_Title);
		mEggDetailDesc = (TextView) findViewById(R.id.Egg_Detail_Desc);
		mEggDetailMessage = (TextView) findViewById(R.id.Egg_Detail_Message);
		mEggDetailFinder = (TextView) findViewById(R.id.Egg_Detail_Finder);
		mEggDetailTakeButton = (Button) findViewById(R.id.Egg_Detail_Take_Button);
		mEggDetailKeepButton = (Button) findViewById(R.id.Egg_Detail_Keep_Button);
		mEggDetailDiscardButton = (Button) findViewById(R.id.Egg_Detail_Discard_Button);
		mEggsDropMessageEdit = (EditText) findViewById(R.id.Eggs_Drop_Message_Edit);

		setLayoutAnim_slidedown(mEggDetailRow, EggsGrid.this);

		mGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View view, int position, long id)
			{
				mEggDetailRow.setVisibility(View.VISIBLE);

				ImageHolder imageHolder = (ImageHolder) view.getTag();
				Egg egg = (Egg) imageHolder.data;
				if (egg.eggUnwrapped)
					loadEggDetailUnwrapped(egg, false);
				else
					loadEggDetailWrapped(egg);
			}
		});

		mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocListener = new MyLocationListener();
		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), "aircandy", 100, 16);
	}

	// For this activity, refresh means rescan and reload point data from the service
	@Override
	public void onRefreshClick(View view)
	{
		startProgress();
		loadData();
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();
		String method = "GetEggs";
		parameters.putString("entityId", getCurrentEntity().entityId);
		ripple.post(method, parameters, QueryFormat.Json, new GetEggsListener());
	}

	public class GetEggsListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mListItems = RippleService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					if (mListItems == null || mListItems.size() == 0)
					{
						showEggDropPanel();
						// We are out of eggs so be conservative and make sure
						// the point in the radar get refreshed.
						getCurrentEntity().isDirty = true;
						return;
					}

					mEggDropperRow.setVisibility(View.VISIBLE);
					mEggDetailRow.setVisibility(View.GONE);
					mEggDropRow.setVisibility(View.GONE);

					showDropUserInfo();
					loadGrid();
					stopProgress();
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsGrid.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}

	}

	public void loadQuote()
	{
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();
		String method = "GetQuote";
		ripple.post(method, parameters, QueryFormat.Json, new GetQuoteListener());
	}

	public class GetQuoteListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mQuoteItems = RippleService.convertJsonToObjects(response, String.class);

			// Post the processed result back to the UI thread
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					if (mQuoteItems == null || mQuoteItems.size() == 0)
						mEggsDropMessageEdit.setText("Live long and prosper.");
					else
						mEggsDropMessageEdit.setText(mQuoteItems.get(0).toString());
					stopProgress();
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsGrid.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}

	}

	private void showEggDropPanel()
	{
		mEggDropperRow.setVisibility(View.GONE);
		mEggDetailRow.setVisibility(View.GONE);
		mEggDropRow.setVisibility(View.VISIBLE);
		loadQuote();
	}

	private void loadEggDetailWrapped(Egg egg)
	{
		mEggDetailImage.setVisibility(View.VISIBLE);
		mEggDetailMessage.setVisibility(View.GONE);
		mEggDetailTakeButton.setVisibility(View.VISIBLE);
		mEggDetailTakeButton.setTag(egg);
		mEggDetailKeepButton.setVisibility(View.GONE);
		mEggDetailDiscardButton.setVisibility(View.GONE);

		mEggDetailImage.setTag(egg);
		String eggResourceId = "egg_wrapped_bow.png";
		Bitmap bitmap = mImageCache.get(eggResourceId);
		if (bitmap != null)
			mEggDetailImage.setImageBitmap(bitmap);
		else
		{
			String url = Ripple.URL_RIPPLEMEDIA + "images/eggs/" + eggResourceId;
			ImageHolder imageHolder = new ImageHolder();
			imageHolder.imageId = eggResourceId;
			imageHolder.imageView = mEggDetailImage;
			imageHolder.imageUrl = url;
			new GetImageTask().execute(imageHolder);
		}

		mEggDetailTitle.setText("Wrapped");
		mEggDetailDesc.setText("Take the egg to\nunwrap it");
		mEggDetailFinder.setText("");
	}

	private void loadEggDetailUnwrapped(Egg egg, Boolean localOnly)
	{
		mEggDetailImage.setVisibility(View.VISIBLE);
		mEggDetailImage.setTag(egg);

		// Buttons

		if (egg.addedToCollection)
		{
			mEggDetailTakeButton.setVisibility(View.GONE);
			mEggDetailKeepButton.setVisibility(View.GONE);
			mEggDetailDiscardButton.setVisibility(View.GONE);
		}
		else if (egg.eggUnwrapped)
		{
			mEggDetailTakeButton.setVisibility(View.GONE);
			mEggDetailKeepButton.setVisibility(View.VISIBLE);
			mEggDetailKeepButton.setTag(egg);
			mEggDetailDiscardButton.setVisibility(View.VISIBLE);
			mEggDetailDiscardButton.setTag(egg);
		}

		String eggResourceId = egg.eggResourceId;
		Bitmap bitmap = mImageCache.get(eggResourceId);
		if (bitmap != null)
			mEggDetailImage.setImageBitmap(bitmap);
		else
		{
			String url = Ripple.URL_RIPPLEMEDIA + "images/eggs/" + eggResourceId;
			ImageHolder imageHolder = new ImageHolder();
			imageHolder.imageId = eggResourceId;
			imageHolder.imageView = mEggDetailImage;
			imageHolder.imageUrl = url;
			new GetImageTask().execute(imageHolder);
		}

		// Title

		String eggName = egg.eggName;
		if (egg.categoryId.toLowerCase().equals("rare"))
			eggName += " (Rare)";
		mEggDetailTitle.setText(eggName);

		// Description

		String desc = "";
		if (!egg.eggSetName.toLowerCase().equals("none"))
		{
			desc += egg.eggSetName + " Set";
			mEggDetailDesc.setText(desc);
			mEggDetailDesc.setVisibility(View.VISIBLE);
		}
		else
			mEggDetailDesc.setVisibility(View.GONE);

		// Message

		if (egg.basketMessage != null && !egg.basketMessage.equals(""))
		{
			String message = "\"" + egg.basketMessage + "\"";
			if (egg.addedToCollection)
				message += "\n(added to your collection)";
			mEggDetailMessage.setText(message);
			mEggDetailMessage.setVisibility(View.VISIBLE);
		}
		else
			mEggDetailMessage.setVisibility(View.GONE);

		// Finder

		if (localOnly)
		{
			Date date = DateUtils.nowDate();
			String finder = "Found by you \non " + DateFormat.getInstance().format(date);
			mEggDetailFinder.setText(finder);
			mEggDetailFinder.setVisibility(View.VISIBLE);
		}
		else if (egg.eggFinderId != null)
		{
			String finder = "Found by " + egg.eggFinderName;
			if (egg.eggFoundDate != null)
			{
				Date date = DateUtils.wcfToDate(egg.eggFoundDate);
				finder += "\non " + DateFormat.getInstance().format(date);
			}
			mEggDetailFinder.setText(finder);
			mEggDetailFinder.setVisibility(View.VISIBLE);
		}
		else
		{
			mEggDetailFinder.setVisibility(View.GONE);
		}
	}

	private void loadGrid()
	{
		if (mListItems != null && mListItems.size() > 0)
		{
			ImageAdapter adapter = new ImageAdapter(EggsGrid.this);
			mGridView.setAdapter(adapter);
		}
	}

	public void showDropUserInfo()
	{
		Egg egg = (Egg) mListItems.get(0);
		ImageView userImageView = (ImageView) findViewById(R.id.EggDrop_Icon);
		TextView userTextView = (TextView) findViewById(R.id.EggDrop_Text);

		// Set the text
		Date date = DateUtils.wcfToDate(egg.basketDropDate);
		String description = "Eggs dropped by " + egg.eggDropperName + "\non " + DateFormat.getInstance().format(date);
		userTextView.setText(description);

		// Get their picture
		String url = FacebookService.GRAPH_BASE_URL + egg.basketDropperId + "/picture?type=large";
		String userId = egg.basketDropperId;

		Bitmap bitmap = mImageCache.get(egg.basketDropperId);
		if (bitmap != null)
			userImageView.setImageBitmap(bitmap);
		else
		{
			ImageHolder imageHolder = new ImageHolder();
			imageHolder.imageId = userId;
			imageHolder.imageShape = "square";
			imageHolder.imageView = userImageView;
			imageHolder.imageUrl = url;
			new GetImageTask().execute(imageHolder);
		}
	}

	class GetImageTask extends AsyncTask<ImageHolder, Void, Bitmap>
	{
		ImageHolder	holder;

		@Override
		protected Bitmap doInBackground(ImageHolder... params)
		{
			// We are on the background thread
			holder = params[0];
			Bitmap bitmap = RippleUI.getImage(holder.imageUrl);
			if (bitmap != null && holder.imageShape == "square")
				bitmap = RippleUI.cropToSquare(bitmap);
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			// We are on the UI thread
			super.onPostExecute(bitmap);
			if (bitmap != null)
			{
				holder.imageView.setImageBitmap(bitmap);
				holder.image = bitmap;
				mImageCache.put(holder.imageId, bitmap);
			}
		}
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		
		Location locationLastKnown = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (Ripple.isBetterLocation(locationLastKnown, getCurrentLocation()))
			setCurrentLocation(locationLastKnown);

		if (mLocManager != null)
			mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);

		// Get the data
		startProgress();
		loadData();
	}

	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		mLocManager.removeUpdates(mLocListener);
		setCurrentEntity(null);
	}

	// ----------------------------------------------------------------------------------
	// Event handlers
	// ----------------------------------------------------------------------------------

	public void onBasketDropClick(View view)
	{
		// Bail if we don't have a location or the one we have is more than five minutes old
		if (getCurrentLocation() == null)
		{
			// Jayma: We are using the NEA headquarters lat/lon if we can't get a real one
			// NEA: 37.41977799657014, -122.21218228340149
			Location location = new Location("Fake");
			location.setLatitude(37.41977799657014);
			location.setLongitude(-122.21218228340149);
			setCurrentLocation(location);
			
//			Toast.makeText(getApplicationContext(), "Waiting for location info...", Toast.LENGTH_SHORT).show();
//			return;
		}

		startProgress();
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();

		String method = "InsertBasket";
		parameters.putString("entityId", getCurrentEntity().entityId);
		parameters.putDouble("latitude", getCurrentLocation().getLatitude());
		parameters.putDouble("longitude", getCurrentLocation().getLongitude());
		parameters.putString("userId", getCurrentUser().id);
		parameters.putInt("eggCount", 5);
		parameters.putString("message", mEggsDropMessageEdit.getText().toString());

		ripple.post(method, parameters, QueryFormat.Json, new DropBasketListener());
	}

	public class DropBasketListener extends BaseQueryListener
	{
		public Integer resultCode = 0;

		public void onComplete(final String response)
		{
			ArrayList<Object> resultCodeArray = RippleService.convertJsonToObjects(response, Integer.class);
			BigInteger resultCodeBig = (BigInteger) resultCodeArray.get(0);
			resultCode = resultCodeBig.intValue();

			// Post the processed result back to the UI thread
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					if (resultCode == RippleService.RESULT_OK)
					{
						RippleUI.showToastNotification(EggsGrid.this, "Basket Dropped!", Toast.LENGTH_SHORT);
						mEggDropRow.setVisibility(View.GONE);
						mLocManager.removeUpdates(mLocListener);
						getCurrentEntity().isDirty = true;
						startProgress();
						loadData();
					}
					else
					{
						RippleUI.showToastNotification(EggsGrid.this, "Basket was not dropped.", Toast.LENGTH_SHORT);
					}
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsGrid.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}

	}

	public void onTakeEggClick(View view)
	{
		Egg egg = (Egg) view.getTag();
		egg.eggUnwrapped = true;
		egg.eggFinderId = getCurrentUser().id;
		egg.eggFinderName = getCurrentUser().name;
		mGridView.invalidateViews();
		loadEggDetailUnwrapped(egg, true);
	}

	public void onKeepEggClick(View view)
	{
		startProgress();
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();

		Egg egg = (Egg) view.getTag();

		String method = "UpdateEgg";
		parameters.putString("eggId", egg.eggId);
		parameters.putString("finderId", getCurrentUser().id);
		parameters.putBoolean("eggUnwrapped", true);

		ripple.post(method, parameters, QueryFormat.Json, new KeepEggListener());
	}

	public class KeepEggListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			// Post the processed result back to the UI thread
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsGrid.this, "Added to your collection!", Toast.LENGTH_SHORT);
					startProgress();
					mEggDetailRow.setVisibility(View.GONE);
					loadData();
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsGrid.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}

	}

	public void onDiscardEggClick(View view)
	{
		startProgress();
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();
		Egg egg = (Egg) view.getTag();

		String method = "DeleteEgg";
		parameters.putString("eggId", egg.eggId);

		ripple.post(method, parameters, QueryFormat.Json, new DeleteEggListener());
	}

	public class DeleteEggListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			// Post the processed result back to the UI thread
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					startProgress();
					mEggDetailRow.setVisibility(View.GONE);
					loadData();
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsGrid.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsGrid.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}

	}

	// ----------------------------------------------------------------------------------
	// List binding
	// ----------------------------------------------------------------------------------

	public class ImageAdapter extends BaseAdapter
	{
		private Context	context;

		public ImageAdapter(Context c) {
			context = c;
		}

		// ---returns the number of images---
		public int getCount()
		{
			return mListItems.size();
		}

		// ---returns the ID of an item---
		public Object getItem(int position)
		{
			return position;
		}

		public long getItemId(int position)
		{
			return position;
		}

		// ---returns an ImageView view---
		public View getView(int position, View convertView, ViewGroup parent)
		{
			Egg egg = (Egg) mListItems.get(position);
			ImageView imageView = new ImageView(context);
			ImageHolder imageHolder = new ImageHolder();

			if (convertView == null)
			{
				imageView = new ImageView(context);
				imageView.setLayoutParams(new GridView.LayoutParams(100, 100));
				imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				imageView.setPadding(4, 4, 4, 4);
				imageView.setBackgroundResource(R.drawable.egg_frame_bg);
				imageView.setTag(imageHolder);
				imageHolder.imageView = imageView;
				imageHolder.data = egg;
			}
			else
			{
				imageView = (ImageView) convertView;
				imageHolder = (ImageHolder) imageView.getTag();
				imageHolder.data = egg;
			}

			String eggResourceId = egg.eggResourceId;
			if (!egg.eggUnwrapped)
				eggResourceId = "egg_wrapped_bow.png";

			Bitmap bitmap = mImageCache.get(eggResourceId);
			if (bitmap != null)
				imageHolder.imageView.setImageBitmap(bitmap);
			else
			{
				String url = Ripple.URL_RIPPLEMEDIA + "images/eggs/" + eggResourceId;
				imageHolder.imageId = eggResourceId;
				imageHolder.imageUrl = url;
				new GetImageTask().execute(imageHolder);
			}
			return imageView;
		}
	}

	public void setLayoutAnim_slidedown(ViewGroup panel, Context ctx)
	{

		AnimationSet set = new AnimationSet(true);
		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				-1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(800);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation)
			{
			// TODO Auto-generated method stub
			// MapContacts.this.mapviewgroup.setVisibility(View.VISIBLE);

			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{

			// TODO Auto-generated method stub

			}
		});
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);
		panel.setLayoutAnimation(controller);

	}

	public void setLayoutAnim_slideup(ViewGroup panel, Context ctx)
	{

		AnimationSet set = new AnimationSet(true);

		/*
		 * Animation animation = new AlphaAnimation(1.0f, 0.0f); animation.setDuration(200);
		 * set.addAnimation(animation);
		 */

		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, -1.0f);
		animation.setDuration(800);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation)
			{
			// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
			// MapContacts.this.mapviewgroup.setVisibility(View.INVISIBLE);
			// TODO Auto-generated method stub

			}
		});
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);
		panel.setLayoutAnimation(controller);

	}

	public class MyLocationListener implements LocationListener
	{
		@Override
		public void onLocationChanged(Location location)
		{
			if (Ripple.isBetterLocation(location, getCurrentLocation()))
				setCurrentLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider)
		{
			Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProviderEnabled(String provider)
		{
			Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{}

	}

}