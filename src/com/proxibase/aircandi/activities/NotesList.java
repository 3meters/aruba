package com.proxibase.aircandi.activities;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.activities.R;
import com.proxibase.aircandi.models.Post;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.service.ProxibaseRunner;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.BaseQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public class NotesList extends AircandiActivity
{
	private Class				mClass				= Post.class;
	private List<Object>	mListItems			= null;
	private static final int	NOTE_ACTION_VIEW	= 1;
	private static final int	NOTE_ACTION_NEW		= 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		{
			String jsonCommand = extras.getString("command");
			if (jsonCommand != "")
				mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("stream"), Command.class);
		}

		//int layoutResourceId = this.getResources().getIdentifier(mCommand.layoutTemplate, "layout", this.getPackageName());
		//setContentView(layoutResourceId);
		super.onCreate(savedInstanceState);
		loadData();
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		Query query = new Query("Posts").filter("entityId eq guid'" + getCurrentEntity().getEntityProxy().entityProxyId + "' and Author eq '" + "Anonymous" + "'");

		if (getCurrentEntity() != null)
			if (mListItems == null)
			{
				startProgress();
				ProxibaseRunner ripple = new ProxibaseRunner();
				ripple.select(query, mClass, "", new ListQueryListener());
			}
			else
			{
				loadList();
				stopProgress();
			}
	}

	public class ListQueryListener extends BaseQueryListener
	{
		public void onComplete(String response)
		{
			mListItems = ProxibaseService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			NotesList.this.runOnUiThread(new Runnable() {
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
			NotesList.this.runOnUiThread(new Runnable() {
				public void run()
				{
					AircandiUI.showToastNotification(NotesList.this, "Network error", Toast.LENGTH_SHORT);
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
	public void onItemClick(View view)
	{
		ViewHolder holder = (ViewHolder) view.getTag();
		Post post = (Post) holder.data;
		setCurrentPost(post);
		Intent intent = new Intent(this, NoteForm.class);
		startActivityForResult(intent, NOTE_ACTION_VIEW);
	}

	@Override
	public void onActivityButtonClick(View view)
	{
		// This is a call to create a new note
		setCurrentPost(null);
		Intent intent = new Intent(this, NoteEditor.class);
		startActivityForResult(intent, NOTE_ACTION_NEW);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// We know that new means refresh but view could as well because there are
		// options available to edit or delete from the view form.
		// TODO: Only re-populate the list if it has been changed
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK)
			mListItems = null; // Setting to null forces a requery
		loadData();
	}

	// ----------------------------------------------------------------------------------
	// List binding
	// ----------------------------------------------------------------------------------

	public void loadList()
	{
		if (mListItems != null && mListItems.size() > 0)
		{
			ListView listView = (ListView) findViewById(R.id.InfoList);
			ListAdapter adapter = new ListAdapter(this, R.id.ListItem_Body, mListItems);
			listView.setAdapter(adapter);
			listView.setClickable(true);

			Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_medium);
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
					if (itemData.description != "")
						holder.itemBody.setText(itemData.description);
					else if (itemData.postContent != "")
					{
						String content = itemData.postContent;
						String replaced = content.replaceAll("<br />", "\n");

						holder.itemBody.setText(replaced);
					}
			}
			return view;
		}
	}
}