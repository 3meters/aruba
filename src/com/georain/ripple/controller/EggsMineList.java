package com.georain.ripple.controller;

import java.io.IOException;
import java.util.ArrayList;

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

import com.georain.ripple.model.EggSetByUser;
import com.georain.ripple.utilities.Utilities;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.Stream;
import com.threemeters.sdk.android.core.RippleService.GsonType;
import com.threemeters.sdk.android.core.RippleService.QueryFormat;

public class EggsMineList extends RippleActivity
{
	private ImageCache			mImageCache;
	private Class				mClass		= EggSetByUser.class;
	private ArrayList<Object>	mListItems	= null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.eggs_mine_list);
		super.onCreate(savedInstanceState);

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), "aircandy", 100, 16);

		loadData();
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		// Get the data
		if (getCurrentUser() == null)
			return;
		startProgress();
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();
		parameters.putString("userId", getCurrentUser().id);
		String method = "GetEggSetsForUser";

		ripple.post(method, parameters, QueryFormat.Json, new FriendsQueryListener());
	}

	public class FriendsQueryListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mListItems = RippleService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			EggsMineList.this.runOnUiThread(new Runnable() {
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
			EggsMineList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					RippleUI.showToastNotification(EggsMineList.this, "Network error", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
			
		}
	}

	// ----------------------------------------------------------------------------------
	// Event handlers
	// ----------------------------------------------------------------------------------

	public void onEggMapClick(View view)
	{
		Intent intent = new Intent(this, EggsMap.class);
		startActivity(intent);
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
		Intent intent = new Intent(this, EggsMine.class);
		intent.putExtra("EggSetName", ((EggSetByUser) holder.data).eggSetName);

		Stream stream = new Stream();
		stream.showHeader = true;
		stream.showFooter = false;
		stream.headerTitle = "Egg Set: " + ((EggSetByUser) holder.data).eggSetName;
		stream.headerIconResource = "none";
		stream.layoutTemplate = "eggs_mine";
		String jsonStream = RippleService.getGson(GsonType.Internal).toJson(stream);
		intent.putExtra("stream", jsonStream);

		startActivity(intent);
	}

	// For this activity, refresh means rescan and reload point data from the service
	@Override
	public void onRefreshClick(View view)
	{
		loadData();
	}
	
	// ----------------------------------------------------------------------------------
	// List binding
	// ----------------------------------------------------------------------------------

	private void loadList()
	{
		if (mListItems != null && mListItems.size() > 0)
		{
			ListView listView = (ListView) findViewById(R.id.InfoList);
			ListAdapter adapter = new ListAdapter(this, R.id.ListItem_Body, mListItems);
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
		private ArrayList<Object>	items;

		public ListAdapter(Context context, int textViewResourceId, ArrayList<Object> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			ViewHolder holder;
			EggSetByUser itemData = (EggSetByUser) items.get(position);

			if (view == null)
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_listitem_twoline_icon, null);

				holder = new ViewHolder();
				holder.itemIcon = (ImageView) view.findViewById(R.id.ListItem_Icon);
				holder.itemTitle = (TextView) view.findViewById(R.id.ListItem_Title);
				holder.itemBody = (TextView) view.findViewById(R.id.ListItem_Body);
				holder.data = itemData;
				view.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) view.getTag();
				holder.itemIcon.setImageBitmap(null);
			}

			if (itemData != null)
			{
				holder.data = itemData;
				if (holder.itemTitle != null)
					holder.itemTitle.setText(itemData.eggSetName);

				if (holder.itemBody != null)
				{
					String desc = String.valueOf(itemData.setCount) + " of " + String.valueOf(itemData.setMax) + " collected so far";
					holder.itemBody.setText(desc);
				}

				if (holder.itemIcon != null)
				{
					Utilities.Log("Ripple", "EggMineList: getting image for " + itemData.eggSetName);
					Bitmap bitmap = mImageCache.get(itemData.eggSetResourceId);
					if (bitmap != null)
						holder.itemIcon.setImageBitmap(bitmap);
					else
					{
						String url = Ripple.URL_RIPPLEMEDIA + "images/eggs/" + itemData.eggSetResourceId;
						holder.itemIconUrl = url;
						new GetImageTask().execute(holder); // Will set the picture when finished
					}
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
			Bitmap bitmap = null;
			bitmap = RippleUI.getImage(holder.itemIconUrl);
			if (bitmap != null)
				mImageCache.put(holder.itemIconUrl, bitmap);
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			// We are on the UI thread
			super.onPostExecute(bitmap);
			holder.itemIcon.setImageBitmap(bitmap);
		}
	}
}