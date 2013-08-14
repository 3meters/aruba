package com.aircandi.ui;

import android.os.Bundle;
import android.util.DisplayMetrics;

import com.aircandi.beta.R;
import com.aircandi.events.MessageEvent;
import com.aircandi.ui.base.BaseEntityList;
import com.squareup.otto.Subscribe;

public class EntityGrid extends BaseEntityList {

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		/* Set spacing */
		Integer requestedHorizontalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_horizontal);
		Integer requestedVerticalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_vertical);
		mGridView.setHorizontalSpacing(requestedHorizontalSpacing);
		mGridView.setVerticalSpacing(requestedVerticalSpacing);

		/* Stash some sizing info */
		final DisplayMetrics metrics = mResources.getDisplayMetrics();
		final Integer availableSpace = metrics.widthPixels - mGridView.getPaddingLeft() - mGridView.getPaddingRight();

		Integer requestedColumnWidth = mResources.getDimensionPixelSize(R.dimen.grid_column_width_requested_large);

		Integer mNumColumns = (availableSpace + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
		if (mNumColumns <= 0) {
			mNumColumns = 1;
		}

		int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) - ((mNumColumns - 1) * requestedHorizontalSpacing);

		mPhotoWidthPixels = requestedColumnWidth + spaceLeftOver / mNumColumns;
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
