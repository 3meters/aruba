package com.proxibase.aircandi.activities;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.activities.R;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.models.Post;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.Rotate3dAnimation;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.service.ProxibaseRunner;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.BaseQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public class InfoList extends AircandiActivity
{
	private Class				mClass	= Post.class;
	private List<Object>	mListItems;
	private RelativeLayout		mDetailFrame;
	private ScrollView			mTileFrame;
	private LinearLayout		mContainer;
	private WebView				mWebView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		{
			String jsonStream = extras.getString("stream");
			if (jsonStream != "")
				mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("stream"), Command.class);
		}

//		int layoutResourceId = this.getResources().getIdentifier(mCommand.layoutTemplate, "layout", this.getPackageName());
//		setContentView(layoutResourceId);
		super.onCreate(savedInstanceState);
		mDetailFrame = (RelativeLayout) findViewById(R.id.DetailFrame);
		mTileFrame = (ScrollView) findViewById(R.id.TileFrame);
		mContainer = (LinearLayout) findViewById(R.id.Container);
		mContainer.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);
		mWebView = (WebView) findViewById(R.id.WebView);
		mWebView.setWebViewClient(new WebViewClient() {
			// you tell the webclient you want to catch when a url is about to load
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				return true;
			}

			// here you execute an action when the URL you want is about to load
			@Override
			public void onLoadResource(WebView view, String url)
			{
				if (url.equals("http://cnn.com"))
				{
					// do whatever you want
				}
			}
		});
		loadData();
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		// Query query = new Query("Posts").filter("entityId eq guid'" + getCurrentEntity().entityId +
		// "' and Author eq '" + getCurrentEntity().tagId + "'");
		Query query = new Query("Posts").filter("EntityId eq guid'" + getCurrentEntity().getEntityProxy().entityProxyId + "' and Author eq '00:00:00:00:00:00'");
		if (getCurrentEntity() != null)
			if (mListItems == null)
			{
				ProxibaseRunner ripple = new ProxibaseRunner();
				ripple.select(query, mClass, "", new ListQueryListener());
			}
			else
			{
				loadList();
				stopProgress();
			}
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
	}

	class ListQueryListener extends BaseQueryListener
	{
		public void onComplete(String response)
		{
			mListItems = ProxibaseService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			InfoList.this.runOnUiThread(new Runnable() {
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
			InfoList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(InfoList.this, "Network error", Toast.LENGTH_SHORT);
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
		// Branch based on the action string in view.Tag
		// Can be called buttons in either header or footer
		return;
	}

	public void onDetailsClick(View v)
	{
		applyRotation(-1, 360, 270);
	}

	@Override
	public void onBackPressed()
	{
		if (mTileFrame.getVisibility() == View.VISIBLE)
			super.onBackPressed();
		else
		{
			WebView webView = (WebView) mDetailFrame.findViewById(R.id.WebView);
			LinearLayout detailHeader = (LinearLayout) mDetailFrame.findViewById(R.id.DetailHeader);
			mDetailFrame.setVisibility(View.GONE);
			webView.setVisibility(View.GONE);
			detailHeader.setVisibility(View.GONE);
			mDetailFrame.setAnimation(null);

//			Animation animation = AnimationUtils.loadAnimation(InfoList.this, R.anim.fly_in);
//			animation.setFillEnabled(true);
//			animation.setFillAfter(true);
//			mTileFrame.startAnimation(animation);

			mTileFrame.setVisibility(View.VISIBLE);
			return;
		}
	}

	@Override
	public void onItemButtonClick(View view)
	{
		// Branch based on the action string in view.Tag
		return;
	}

	public void onBackButtonClick(View v)
	{
//		Animation animation = AnimationUtils.loadAnimation(InfoList.this, R.anim.fly_in);
//		animation.setFillEnabled(true);
//		animation.setFillAfter(true);
//		mTileFrame.startAnimation(animation);

		@SuppressWarnings("unused")
		WebView webView = (WebView) mDetailFrame.findViewById(R.id.WebView);
		@SuppressWarnings("unused")
		LinearLayout detailHeader = (LinearLayout) mDetailFrame.findViewById(R.id.DetailHeader);
		mDetailFrame.setVisibility(View.GONE);
		mDetailFrame.invalidate();
		mTileFrame.setVisibility(View.VISIBLE);
		// mTileFrame.requestFocus();
	}

	@Override
	public void onItemClick(View view)
	{
		Post post = (Post) view.getTag();
		WebView webView = (WebView) mDetailFrame.findViewById(R.id.WebView);
		TextView title = (TextView) mDetailFrame.findViewById(R.id.Title);
		LinearLayout detailHeader = (LinearLayout) mDetailFrame.findViewById(R.id.DetailHeader);

		title.setText(post.title);
		webView.setBackgroundColor(0);

		String html = "";
		html += "<html>";
		html += "<head>";
		html += "<meta http-equiv='content-type' content='text/html; charset=utf-8'>";
		html += "<link href='" + CandiConstants.URL_AIRCANDI_SERVICE + "styles/infoitem.css' rel='stylesheet' type='text/css' />";
		html += "</head>";
		html += "<body>";
		// html += "<h2>" + post.title + "</h2>";
		html += post.postContent;
		html += "</body>";
		html += "</html>";

		webView.loadData(html, "text/html", "UTF-8");

		Animation animation = AnimationUtils.loadAnimation(InfoList.this, R.anim.fly_in);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		mDetailFrame.startAnimation(animation);

		mDetailFrame.setVisibility(View.VISIBLE);
		mDetailFrame.scrollTo(0, 0);
		webView.setVisibility(View.VISIBLE);
		detailHeader.setVisibility(View.VISIBLE);
		mTileFrame.setVisibility(View.GONE);
		// mDetailFrame.requestFocus();

		// applyRotation(1, 0, 90);

		// Intent intent = new Intent(this, InfoForm.class);
		//
		// // Pass the stream to handle UI config
		// Stream stream = new Stream();
		// stream.showHeader = false;
		// stream.showFooter = false;
		// String jsonStream = ProxibaseService.getGson(GsonType.Internal).toJson(stream);
		// intent.putExtra("stream", jsonStream);
		//
		// // Pass the post because we already have the needed data
		// Post post = (Post) view.getTag();
		// String jsonPost = ProxibaseService.getGson(GsonType.Internal).toJson(post);
		// intent.putExtra("post", jsonPost);
		//
		// startActivity(intent);
	}

	// ----------------------------------------------------------------------------------
	// List binding
	// ----------------------------------------------------------------------------------

	protected void loadList()
	{
		if (mListItems != null && mListItems.size() > 0)
		{
			ListView listView = (ListView) findViewById(R.id.InfoList);
			ListAdapter adapter = new ListAdapter(InfoList.this, R.id.ListItem_Body, mListItems);
			listView.setAdapter(adapter);
			listView.setClickable(true);

			Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_medium);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			listView.startAnimation(animation);
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
			final TableLayout table = new TableLayout(InfoList.this);

			// Make the first row
			TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
			final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
			rowLp.setMargins(0, 0, 8, 8);
			TableLayout.LayoutParams tableLp;

			// Loop the streams
			int tileCount = 0;
			for (int i = 0; i < mListItems.size(); i++)
			{
				tileCount++;
				Post post = (Post) mListItems.get(i);

				// Make a tile and configure it
				RelativeLayout tile = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_listitem_tile, null);
				((TextView) tile.findViewById(R.id.Title)).setText(post.title);
				((TextView) tile.findViewById(R.id.Description)).setText(post.description);

				tile.setTag(post);

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
					tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
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

	/**
	 * Setup a new 3D rotation on the container view.
	 * 
	 * @param position
	 *            the item that was clicked to show a picture, or -1 to show the list
	 * @param start
	 *            the start angle at which the rotation must begin
	 * @param end
	 *            the end angle of the rotation
	 */
	private void applyRotation(int position, float start, float end)
	{
		// Find the center of the container
		final float centerX = mContainer.getWidth() / 2.0f;
		final float centerY = mContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(position));

		mContainer.startAnimation(rotation);
	}

	/**
	 * This class listens for the end of the first half of the animation. It then posts a new action that effectively
	 * swaps the views when the container is rotated 90 degrees and thus invisible.
	 */
	private final class DisplayNextView implements Animation.AnimationListener
	{
		private final int	mPosition;

		private DisplayNextView(int position) {
			mPosition = position;
		}

		public void onAnimationStart(Animation animation)
		{}

		public void onAnimationEnd(Animation animation)
		{
			mContainer.post(new SwapViews(mPosition));
		}

		public void onAnimationRepeat(Animation animation)
		{}
	}

	/**
	 * This class is responsible for swapping the views and start the second half of the animation.
	 */
	private final class SwapViews implements Runnable
	{
		private final int	mPosition;

		public SwapViews(int position) {
			mPosition = position;
		}

		public void run()
		{
			final float centerX = mContainer.getWidth() / 2.0f;
			final float centerY = mContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (mPosition > -1)
			{
				mTileFrame.setVisibility(View.GONE);
				mDetailFrame.setVisibility(View.VISIBLE);
				mDetailFrame.requestFocus();

				rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
			}
			else
			{
				mDetailFrame.setVisibility(View.GONE);
				mTileFrame.setVisibility(View.VISIBLE);
				mTileFrame.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			mContainer.startAnimation(rotation);
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
			Post itemData = (Post) items.get(position);

			if (view == null)
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_listitem_twoline, null);

				holder = new ViewHolder();
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
					holder.itemTitle.setText(itemData.title);

				if (holder.itemBody != null)
					holder.itemBody.setText(itemData.description);
			}
			return view;
		}
	}
}