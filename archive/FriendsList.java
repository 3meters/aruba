package com.proxibase.aircandi.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.model.FriendsFb;
import com.proxibase.aircandi.utilities.DateUtils;
import com.proxibase.aircandi.utilities.Utilities;
import com.proxibase.sdk.android.core.BaseQueryListener;
import com.proxibase.sdk.android.core.ProxibaseRunner;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Stream;
import com.proxibase.sdk.android.core.UserFb;
import com.proxibase.sdk.android.core.ProxibaseService.GsonType;
import com.proxibase.sdk.android.core.ProxibaseService.QueryFormat;
import com.proxibase.sdk.android.widgets.ImageCache;

public class FriendsList extends AircandiActivity
{
	private String		mFriendsFilter	= "none";
	private ImageCache	mImageCache;

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
		loadData();
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		// Get the post we are rooted on
		if (getIntent().getExtras() != null)
			mFriendsFilter = getIntent().getExtras().getString("FriendsFilter");

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), "aircandy", 100, 16);

		// Get the data
		startProgress();
		ProxibaseRunner ripple = new ProxibaseRunner();
		Bundle parameters = new Bundle();

		parameters.putString("userId", getCurrentUser().id);
		String method = "GetFriends";
		if (mFriendsFilter != null && mFriendsFilter.equals("FriendsByPoint"))
		{
			parameters.putString("entityId", getCurrentEntity().entityId);
			method = "GetFriendsAtPoint";
		}

		ripple.post(method, parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE, new FriendsQueryListener());
	}

	public class FriendsQueryListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			// Process the response here: executed in background thread
			setCurrentFriends(ProxibaseService.getGson(GsonType.Internal).fromJson(response, FriendsFb.class));

			// Post the processed result back to the UI thread
			FriendsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					loadList();
					stopProgress();
				}
			});
		}
		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			FriendsList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(FriendsList.this, "Network error", Toast.LENGTH_SHORT);
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
	protected void onActivityButtonClick(View view)
	{
		return;
	}

	@Override
	protected void onItemButtonClick(View view)
	{
		return;
	}

	@Override
	public void onItemClick(View view)
	{
		ViewHolder holder = (ViewHolder) view.getTag();
		Intent intent = new Intent(this, FacebookFanPage.class);
		intent.putExtra("FriendId", holder.userId);
		startActivity(intent);
	}

	// ----------------------------------------------------------------------------------
	// List binding
	// ----------------------------------------------------------------------------------

	private void loadList()
	{
		if ((getCurrentFriends() != null) && (getCurrentFriends().d.size() != 0))
		{
			ListView listView = (ListView) findViewById(R.id.InfoList);
			ListAdapter adapter = new ListAdapter(FriendsList.this, R.id.ListItem_Body, getCurrentFriends().d);
			listView.setAdapter(adapter);
			listView.setClickable(true);

			Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_normal);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			listView.startAnimation(animation);
		}
	}

	private class ListAdapter extends ArrayAdapter<UserFb>
	{
		private List<UserFb>	items;

		public ListAdapter(Context context, int textViewResourceId, List<UserFb> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			ViewHolder holder;
			UserFb itemData = items.get(position);

			if (view == null)
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_listitem_twoline_icon, null);

				holder = new ViewHolder();
				holder.userId = itemData.id;
				holder.itemIcon = (ImageView) view.findViewById(R.id.ListItem_Icon);
				holder.itemTitle = (TextView) view.findViewById(R.id.ListItem_Title);
				holder.itemBody = (TextView) view.findViewById(R.id.ListItem_Body);
				view.setTag(holder);
			}
			else
				holder = (ViewHolder) view.getTag();

			if (itemData != null)
			{
				holder.data = itemData;
				if (holder.itemTitle != null)
					holder.itemTitle.setText(itemData.name);

				if (holder.itemBody != null)
				{
					if (itemData.label != "")
					{
						Date hookupDate = DateUtils.wcfToDate(itemData.hookupDate);
						holder.itemBody.setText(itemData.label + " " + DateUtils.intervalSince(hookupDate, DateUtils.nowDate()));
					}
					else
						holder.itemBody.setText("");
				}

				if (holder.itemIcon != null)
				{
					Bitmap bitmap = mImageCache.get(itemData.id);
					if (bitmap != null)
					{
						Utilities.Log(Aircandi.APP_NAME, "FriendsList", "Cache hit for image '" + itemData.id + "'");
						holder.itemIcon.setImageBitmap(bitmap);
					}
					else
						new GetFacebookImageTask().execute(holder); // Will set the picture when finished
				}
			}
			return view;
		}
	}

	class GetFacebookImageTask extends AsyncTask<ViewHolder, Void, Bitmap>
	{
		ImageView	imageView	= null;
		ViewHolder	holder;
		String		userId;

		@Override
		protected Bitmap doInBackground(ViewHolder... params)
		{
			// We are on the background thread
			Utilities.Log(Aircandi.APP_NAME, "FriendsList", "Starting AsyncTask to get image (from cache or service) for " + userId);
			holder = params[0];
			userId = params[0].userId;
			Bitmap bitmap = null;
			bitmap = mImageCache.get(params[0].userId);
			if (bitmap == null)
			{
				Utilities.Log(Aircandi.APP_NAME, "FriendsList", "Cache miss: get image from facebook '" + params[0].userId + "'");
				bitmap = FacebookService.getFacebookPicture(params[0].userId, params[0].imageFormat);
				bitmap = AircandiUI.cropToSquare(bitmap);
				mImageCache.put(params[0].userId, bitmap);
			}
			else
			{
				Utilities.Log(Aircandi.APP_NAME, "FriendsList", "Cache hit for image '" + params[0].userId + "'");
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			// We are on the UI thread
			super.onPostExecute(bitmap);
			Utilities.Log(Aircandi.APP_NAME, "FriendsList", "Returning AsyncTask to get image (from cache or service) for " + userId);
			holder.itemIcon.setImageBitmap(bitmap);
		}
	}
}