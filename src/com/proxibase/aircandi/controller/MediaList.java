package com.proxibase.aircandi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.model.Resource;
import com.proxibase.sdk.android.core.BaseQueryListener;
import com.proxibase.sdk.android.core.Query;
import com.proxibase.sdk.android.core.ProxibaseRunner;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Stream;
import com.proxibase.sdk.android.core.ProxibaseService.GsonType;
import com.proxibase.sdk.android.core.ProxibaseService.QueryFormat;

public class MediaList extends AircandiActivity {

	private Class				mClass		= Resource.class;
	private List<Object>	mListItems	= null;
	@SuppressWarnings("unused")
	private Resource			mCurrentResource;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String jsonStream = extras.getString("stream");
			if (jsonStream != "")
				mStream = ProxibaseService.getGson(GsonType.Internal).fromJson(
						getIntent().getExtras().getString("stream"), Stream.class);
		}

		int layoutResourceId = this.getResources().getIdentifier(mStream.layoutTemplate, "layout",
				this.getPackageName());
		setContentView(layoutResourceId);
		super.onCreate(savedInstanceState);
		loadData();
	}


	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData() {

		// Get the point we are rooted on
		Query query = new Query("Resources").filter("EntityId eq guid'" + getCurrentEntity().entityId + "'");
		if (getCurrentEntity() != null)
			if (mListItems == null) {
				startProgress();
				ProxibaseRunner ripple = new ProxibaseRunner();
				ripple.select(query, mClass, "", new ListQueryListener());
			}
			else {
				loadTiles(false);
				stopProgress();
			}
	}


	public class ListQueryListener extends BaseQueryListener {

		public void onComplete(String response) {

			mListItems = ProxibaseService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			MediaList.this.runOnUiThread(new Runnable() {

				public void run() {

					loadTiles(false);
					stopProgress();
				}
			});
		}


		@Override
		public void onIOException(IOException e) {

			// TODO Auto-generated method stub
			super.onIOException(e);
			MediaList.this.runOnUiThread(new Runnable() {

				public void run() {

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
	public void onRefreshClick(View view) {

		loadTiles(false);
	}


	public void onCommandClick(View view) {

		startProgress();
		String command = (String) view.getTag();

		AircandiUI.showToastNotification(this, "Sending " + command + " command", Toast.LENGTH_SHORT);
		ProxibaseRunner ProxibaseRunner = new ProxibaseRunner();
		
		Bundle parameters = new Bundle();

		String method = "InsertMessage";
		parameters.putString("commandType", command);
		parameters.putString("parameter1", "");
		parameters.putString("parameter2", "");
		parameters.putString("parameter3", "");
		parameters.putString("priority", "1");
		parameters.putString("targetId", getCurrentEntity().entityId);
		parameters.putString("sourceId", "111111111");

		ProxibaseRunner.post(method, parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE,
				new MessageSendListener());

	}


	@Override
	public void onItemButtonClick(View view) {

		return;
	}


	@Override
	public void onItemClick(View view) {

		startProgress();
		Resource resource = (Resource) view.getTag();
		this.mCurrentResource = resource;

		AircandiUI.showToastNotification(this, "Switching to " + resource.title, Toast.LENGTH_SHORT);
		ProxibaseRunner ProxibaseRunner = new ProxibaseRunner();

		Bundle parameters = new Bundle();

		String method = "InsertMessage";
		parameters.putString("commandType", "play");
		parameters.putString("parameter1", resource.resourceName);
		parameters.putString("parameter2", resource.title);
		parameters.putString("parameter3", resource.artist);
		parameters.putString("priority", "2");
		parameters.putString("targetId", getCurrentEntity().entityId);
		parameters.putString("sourceId", "111111111");

		ProxibaseRunner.post(method, parameters, QueryFormat.Json, Aircandi.URL_AIRCANDI_SERVICE,
				new MessageSendListener());

	}


	public class MessageSendListener extends BaseQueryListener {

		public void onComplete(String response) {

			// Post the processed result back to the UI thread
			MediaList.this.runOnUiThread(new Runnable() {

				public void run() {

					AircandiUI.showToastNotification(MediaList.this, "Sent...", Toast.LENGTH_SHORT);
					stopProgress();
				}
			});
		}
	}


	// ----------------------------------------------------------------------------------
	// Tile binding
	// ----------------------------------------------------------------------------------

	protected void loadTiles(boolean landscape) {

		if (mListItems != null && mListItems.size() > 0) {
			// Get the table we use for grouping and clear it
			final TableLayout table = new TableLayout(MediaList.this);

			// Make the first row
			TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
			final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
			rowLp.setMargins(0, 0, 8, 8);
			TableLayout.LayoutParams tableLp;

			// Loop the streams
			int tileCount = 0;
			for (int i = 0; i < mListItems.size(); i++) {
				tileCount++;
				Resource resource = (Resource) mListItems.get(i);

				// Make a tile and configure it
				RelativeLayout tile = (RelativeLayout) this.getLayoutInflater().inflate(
						R.layout.temp_listitem_tile_half, null);
				((TextView) tile.findViewById(R.id.Title)).setText(resource.title);
				((TextView) tile.findViewById(R.id.Description)).setText(resource.artist);

				tile.setTag(resource);

				// Add button to row
				tableRow.addView(tile, rowLp);

				// If we have three in a row then commit it and make a new row
				int newRow = 2;
				if (landscape)
					newRow = 4;

				if ((i + 1) % newRow == 0) {
					tableLp = new TableLayout.LayoutParams();
					tableLp.setMargins(0, 0, 0, 0);
					table.addView(tableRow, tableLp);
					tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
				}
			}

			// We might have an uncommited row with tiles in it
			if (tileCount % 2 != 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
			}

			FrameLayout tilesContainer = (FrameLayout) findViewById(R.id.TileContainer);
			tilesContainer.addView(table);
		}
	}
}