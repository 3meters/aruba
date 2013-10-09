package com.aircandi.ui;

import com.aircandi.events.MessageEvent;
import com.aircandi.ui.base.BaseEntityList;
import com.squareup.otto.Subscribe;

public class EntityList extends BaseEntityList {
	
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
