package com.aircandi.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.aircandi.beta.R;
import com.aircandi.components.MessageEvent;
import com.aircandi.ui.base.BaseEntityList;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class EntityGrid extends BaseEntityList {

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		
		/* Stash some sizing info */
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final Integer layoutWidthPixels = metrics.widthPixels - (mGridView.getPaddingLeft() + mGridView.getPaddingRight());

		Integer desiredWidthPixels = (int) (metrics.xdpi * 1.1f);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.ydpi * 1.1f);
		}
		final Integer count = (int) Math.max(2, Math.ceil(layoutWidthPixels / desiredWidthPixels));
		mPhotoMarginPixels = UI.getRawPixels(this, 2);
		mPhotoWidthPixels = (layoutWidthPixels / count) - (mPhotoMarginPixels * (count - 1));
		mGridView.setColumnWidth(mPhotoWidthPixels);
	}


	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
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

	@Override
	protected Integer getListItemResId(String schema) {
		Integer itemResId = R.layout.temp_griditem_entity;
		return itemResId;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.entity_grid;
	}

}
