package com.proxibase.aircandi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.model.PromotionsAtPoint;
import com.proxibase.aircandi.model.Theme;
import com.proxibase.aircandi.utilities.Utilities;
import com.proxibase.sdk.android.core.BaseQueryListener;
import com.proxibase.sdk.android.core.ProxibaseRunner;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Stream;
import com.proxibase.sdk.android.core.ProxibaseService.GsonType;
import com.proxibase.sdk.android.core.ProxibaseService.QueryFormat;
import com.proxibase.sdk.android.widgets.ImageCache;

public class RewardsList extends AircandiActivity
{
	private List<Object>	mListItems		= null;
	private Object				mPointTheme		= null;
	private Boolean				mHasPointTheme	= false;
	private Class				mClass			= PromotionsAtPoint.class;
	private ImageCache			mImageCache		= null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		{
			String jsonStream = extras.getString("stream");
			if (jsonStream != "")
				mStream = ProxibaseService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("stream"), Stream.class);
		}

		int layoutResourceId = this.getResources().getIdentifier(mStream.layoutTemplate, "layout", this.getPackageName());
		setContentView(layoutResourceId);
		super.onCreate(savedInstanceState);

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), "aircandy", 100, 16);

		// Get the data
		startProgress();

		// Get theme if the point has one
		if (getCurrentEntity().themeId != null && getCurrentEntity().themeId != "")
		{
			mHasPointTheme = true;
			loadTheme();
		}

		// Get the data for the list
		loadData();
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		startProgress();

		ProxibaseRunner ripple = new ProxibaseRunner();
		Bundle parameters = new Bundle();

		String method = "GetPromosAtPoint";
		parameters.putString("entityId", getCurrentEntity().entityId);
		parameters.putString("userId", getCurrentUser().id);

		ripple.post(method, parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new ListQueryListener());
	}

	public void loadTheme()
	{
		ProxibaseRunner ripple = new ProxibaseRunner();
		Bundle parameters = new Bundle();

		String method = "GetTheme";
		parameters.putString("themeId", getCurrentEntity().themeId);
		ripple.post(method, parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new ThemeQueryListener());
	}

	public void theming()
	{
		Theme theme = (Theme) mPointTheme;
		LinearLayout layoutOuter = (LinearLayout) findViewById(R.id.LayoutOuter);
		TableRow activityHeader = (TableRow) findViewById(R.id.Activity_Header_Row);
		TextView title = (TextView) findViewById(R.id.Activity_Title);
		ImageView icon = (ImageView) findViewById(R.id.Activity_Icon);
		activityHeader.setBackgroundDrawable(null);
		activityHeader.setBackgroundColor(Color.parseColor(theme.headerBackColor));
		layoutOuter.setBackgroundColor(Color.parseColor(theme.bodyBackColor));
		title.setTextColor(Color.parseColor(theme.headerColor));
		title.setText(getCurrentEntity().label);

		Bitmap bitmap = null;
		String url = theme.headerImage;
		bitmap = mImageCache.get(url);
		if (bitmap == null)
		{
			bitmap = AircandiUI.getImage(url);
			mImageCache.put(url, bitmap);
		}
		icon.setBackgroundDrawable(null);
		icon.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
		icon.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
		icon.setImageBitmap(bitmap);
	}

	public class ListQueryListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mListItems = ProxibaseService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			RewardsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					if (!mHasPointTheme || mPointTheme != null)
					{
						loadList();
						stopProgress();
					}
				}
			});
		}
		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			RewardsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(RewardsList.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}
		
	}

	public class ThemeQueryListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mPointTheme = ProxibaseService.convertJsonToObjects(response, Theme.class).get(0);

			// Post the processed result back to the UI thread
			RewardsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					theming();
					if (mListItems != null)
					{
						loadList();
						stopProgress();
					}
				}
			});
		}
		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			RewardsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(RewardsList.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}

	}

	public class InsertUserInPromotionListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			// Post the processed result back to the UI thread
			RewardsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(RewardsList.this, "Enrolled!", Toast.LENGTH_SHORT);
					loadData();
				}
			});
		}
		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			RewardsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(RewardsList.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}
		
	}

	// ----------------------------------------------------------------------------------
	// Event handlers
	// ----------------------------------------------------------------------------------
	@Override
	public void onRefreshClick(View view)
	{
		loadData();
	}	

	@Override
	public void onActivityButtonClick(View view)
	{
		return;
	}

	public void onItemRewardButtonClick(View view)
	{
		startProgress();
		ProxibaseRunner ripple = new ProxibaseRunner();
		Bundle parameters = new Bundle();
		ViewHolder holder = (ViewHolder) view.getTag();
		PromotionsAtPoint promotionAtPoint = (PromotionsAtPoint) holder.data;

		String method = "InsertUserInPromotion";
		parameters.putString("promotionId", promotionAtPoint.promotionId);
		parameters.putString("entityId", getCurrentEntity().entityId);
		parameters.putString("userId", getCurrentUser().id);

		ripple.post(method, parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new InsertUserInPromotionListener());
	}

	@Override
	public void onItemClick(View view)
	{
		return;
	}

	// ----------------------------------------------------------------------------------
	// List binding
	// ----------------------------------------------------------------------------------

	private void loadList()
	{
		if (mListItems != null && mListItems.size() > 0)
		{
			ListView listView = (ListView) findViewById(R.id.InfoList);
			ListAdapter adapter = new ListAdapter(RewardsList.this, R.id.ListItem_Body, mListItems);
			listView.setAdapter(adapter);
			listView.setClickable(true);

			Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_normal);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			listView.startAnimation(animation);
		}
	}

	private class ListAdapter extends ArrayAdapter<Object>
	{
		private List<Object>	items;

		public ListAdapter(Context context, int textViewResourceId, List<Object> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			ViewHolder holder;
			PromotionsAtPoint itemData = (PromotionsAtPoint) items.get(position);

			if (view == null)
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				try
				{
					view = inflater.inflate(R.layout.temp_listitem_rewards, null);
				}
				catch (Exception e)
				{
					Utilities.Log(CandiConstants.APP_NAME, "RewardsList", e.getMessage());
				}
				holder = new ViewHolder();
				holder.itemIcon = (ImageView) view.findViewById(R.id.ListItem_Icon);
				holder.itemTitle = (TextView) view.findViewById(R.id.ListItem_Title);
				holder.itemBody = (TextView) view.findViewById(R.id.ListItem_Body);
				holder.itemButton = (Button) view.findViewById(R.id.ListItem_Button);
				holder.itemSidebarContainer = (LinearLayout) view.findViewById(R.id.ListItem_Sidebar_Container);
				holder.itemSidebarText = (TextView) view.findViewById(R.id.ListItem_Sidebar_Text);
				view.setTag(holder);
			}
			else
				holder = (ViewHolder) view.getTag();

			if (itemData != null)
			{
				holder.data = itemData;
				if (holder.itemTitle != null)
					holder.itemTitle.setText(itemData.title);

				if (holder.itemBody != null)
					holder.itemBody.setText(itemData.description);

				if (holder.itemButton != null)
				{
					holder.itemButton.setTag(holder);
					if (!itemData.enrolled)
						holder.itemButton.setText("Activate");
					else
					{
						holder.itemButton.setClickable(false);
						holder.itemButton.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_reward_filter), PorterDuff.Mode.MULTIPLY);

						if (itemData.status.equals("---") || itemData.status.equals(""))
							holder.itemButton.setText("Enrolled");
						else
						{
							holder.itemButton.setText("Enrolled\n" + itemData.status);
						}
					}
				}

				if (holder.itemIcon != null && itemData.image != "")
				{
					holder.itemIconUrl = itemData.image;
					new GetImageTask().execute(holder); // Will set the picture when finished
				}

				if (itemData.couponCount == 0)
					holder.itemSidebarContainer.setVisibility(View.GONE);
				else
				{
					holder.itemSidebarContainer.setVisibility(View.VISIBLE);
					String sidebarText = String.valueOf(itemData.couponCount) + " reward coupon";
					if (itemData.couponCount > 1)
						sidebarText += "s";
					holder.itemSidebarText.setText(sidebarText);
				}
			}
			return view;
		}
	}

	class GetImageTask extends AsyncTask<ViewHolder, Void, Bitmap>
	{
		ImageView	imageView	= null;
		ViewHolder	holder;

		@Override
		protected Bitmap doInBackground(ViewHolder... params)
		{
			// We are on the background thread
			holder = params[0];
			Utilities.Log(CandiConstants.APP_NAME, "RewardsList", "Starting AsyncTask to get image (from cache or service) for " + holder.itemIconUrl);
			Bitmap bitmap = null;
			bitmap = mImageCache.get(holder.itemIconUrl);
			if (bitmap == null)
			{
				Utilities.Log(CandiConstants.APP_NAME, "RewardsList", "Cache miss: get image from facebook '" + holder.itemIconUrl + "'");
				bitmap = AircandiUI.getImage(holder.itemIconUrl);
				if (bitmap != null)
				{
					// bitmap = AircandiUI.cropToSquare(bitmap);
					mImageCache.put(holder.itemIconUrl, bitmap);
				}
			}
			else
			{
				Utilities.Log(CandiConstants.APP_NAME, "RewardsList", "Cache hit for image '" + holder.itemIconUrl + "'");
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			// We are on the UI thread
			super.onPostExecute(bitmap);
			Utilities.Log(CandiConstants.APP_NAME, "RewardsList", "Returning AsyncTask to get image (from cache or service) for " + holder.itemIconUrl);
			holder.itemIcon.setImageBitmap(bitmap);
		}
	}
}