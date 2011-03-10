package com.threemeters.aircandi.controller;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.threemeters.aircandi.model.Message;
import com.threemeters.aircandi.model.Resource;
import com.threemeters.aircandi.utilities.DateUtils;
import com.threemeters.sdk.android.core.BaseModifyListener;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.Query;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.Stream;
import com.threemeters.sdk.android.core.RippleService.GsonType;

public class MediaList extends AircandiActivity
{
	private Class				mClass		= Resource.class;
	private ArrayList<Object>	mListItems	= null;
	@SuppressWarnings("unused")
	private Resource			mCurrentResource;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		{
			String jsonStream = extras.getString("stream");
			if (jsonStream != "")
				mStream = RippleService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("stream"), Stream.class);
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
		// Get the point we are rooted on
		Query query = new Query("Resources").filter("EntityId eq guid'" + getCurrentEntity().entityId + "'");
		if (getCurrentEntity() != null)
			if (mListItems == null)
			{
				startProgress();
				RippleRunner ripple = new RippleRunner();
				ripple.select(query, mClass, new ListQueryListener());
			}
			else
			{
				loadTiles(false);
				stopProgress();
			}
	}

	public class ListQueryListener extends BaseQueryListener
	{
		public void onComplete(String response)
		{
			mListItems = RippleService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			MediaList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					loadTiles(false);
					stopProgress();
				}
			});
		}
		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			MediaList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(MediaList.this, "Network error", Toast.LENGTH_SHORT);
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
		loadTiles(false);
	}
	

	public void onCommandClick(View view)
	{
		startProgress();
		String command = (String) view.getTag();
		Message message = new Message(command, "", "", "", "1", getCurrentEntity().entityId, getCurrentUser().id, DateUtils.nowString());
		AircandiUI.showToastNotification(this, "Sending " + command + " command", Toast.LENGTH_SHORT);
		RippleRunner rippleRunner = new RippleRunner();
		rippleRunner.insert(message, "Messages", new MessageSendListener());
	}

	@Override
	public void onItemButtonClick(View view)
	{
		return;
	}

	@Override
	public void onItemClick(View view)
	{
		startProgress();
		Resource resource = (Resource) view.getTag();
		this.mCurrentResource = resource;
		Message message = new Message("play", resource.resourceName, resource.title, resource.artist, "2", getCurrentEntity().entityId,
				getCurrentUser().id, DateUtils.nowString());
		AircandiUI.showToastNotification(this, "Switching to " + resource.title, Toast.LENGTH_SHORT);
		RippleRunner rippleRunner = new RippleRunner();
		rippleRunner.insert(message, "Messages", new MessageSendListener());
	}

	public class MessageSendListener extends BaseModifyListener
	{
		public void onComplete()
		{
			// Post the processed result back to the UI thread
			MediaList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(MediaList.this, "Sent...", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}
	}
	
	// ----------------------------------------------------------------------------------
	// Tile binding
	// ----------------------------------------------------------------------------------

	protected void loadTiles(boolean landscape)
	{
		if (mListItems != null && mListItems.size() > 0)
		{
			// Get the table we use for grouping and clear it
			final TableLayout table = new TableLayout(MediaList.this);

			// Make the first row
			TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
			final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
			rowLp.setMargins(0, 0, 8, 8);
			TableLayout.LayoutParams tableLp;

			// Loop the streams
			int tileCount = 0;
			for (int i = 0; i < mListItems.size(); i++)
			{
				tileCount++;
				Resource resource = (Resource) mListItems.get(i);

				// Make a tile and configure it
				RelativeLayout tile = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_listitem_tile_half, null);
				((TextView) tile.findViewById(R.id.Title)).setText(resource.title);
				((TextView) tile.findViewById(R.id.Description)).setText(resource.artist);

				tile.setTag(resource);

				// Add button to row
				tableRow.addView(tile, rowLp);

				// If we have three in a row then commit it and make a new row
				int newRow = 2;
				if (landscape)
					newRow = 4;

				if ((i + 1) % newRow == 0)
				{
					tableLp = new TableLayout.LayoutParams();
					tableLp.setMargins(0, 0, 0, 0);
					table.addView(tableRow, tableLp);
					tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
				}
			}

			// We might have an uncommited row with tiles in it
			if (tileCount % 2 != 0)
			{
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
			}

			FrameLayout tilesContainer = (FrameLayout) findViewById(R.id.TileContainer);
			tilesContainer.addView(table);
		}
	}
}