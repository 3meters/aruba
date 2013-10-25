package com.aircandi.ui;

import android.os.Bundle;
import android.view.LayoutInflater;

import com.aircandi.R;
import com.aircandi.events.MessageEvent;
import com.aircandi.ui.base.BaseEntityList;
import com.squareup.otto.Subscribe;

public class EntityList extends BaseEntityList {
	
	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLoading = LayoutInflater.from(this).inflate(R.layout.temp_list_item_loading, null);		
	}
	
	
	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refreshes the comment list to show new comment.
		 */
		if (event.notification.toEntity != null 
				&& mForEntityId.equals(event.notification.toEntity.id)
				&& mListLinkSchema.equals(event.notification.entity.schema)) {
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

}
