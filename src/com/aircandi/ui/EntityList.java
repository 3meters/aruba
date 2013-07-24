package com.aircandi.ui;

import com.aircandi.components.MessageEvent;
import com.aircandi.ui.base.BaseEntityList;
import com.squareup.otto.Subscribe;

public class EntityList extends BaseEntityList {
	
	@Override
	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		super.onMessage(event);
		/*
		 * Refreshes the comment list to show new comment.
		 */
		if (mEntityId.equals(event.notification.entity.toId)
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
