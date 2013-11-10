package com.aircandi.ui;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.widget.AbsListView;
import android.widget.GridView;

import com.aircandi.R;
import com.aircandi.events.MessageEvent;
import com.aircandi.ui.base.BaseEntityList;
import com.squareup.otto.Subscribe;

public class EntityGrid extends BaseEntityList {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		/* Set spacing */
		Integer requestedHorizontalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_horizontal);
		Integer requestedVerticalSpacing = mResources.getDimensionPixelSize(R.dimen.grid_spacing_vertical);
		GridView gridView = (GridView) mListView;
		gridView.setHorizontalSpacing(requestedHorizontalSpacing);
		gridView.setVerticalSpacing(requestedVerticalSpacing);

		/* Stash some sizing info */
		final DisplayMetrics metrics = mResources.getDisplayMetrics();
		final Integer availableSpace = metrics.widthPixels - gridView.getPaddingLeft() - gridView.getPaddingRight();

		Integer requestedColumnWidth = mResources.getDimensionPixelSize(R.dimen.grid_column_width_requested_large);

		Integer mNumColumns = (availableSpace + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
		if (mNumColumns <= 0) {
			mNumColumns = 1;
		}

		int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) - ((mNumColumns - 1) * requestedHorizontalSpacing);

		mPhotoWidthPixels = requestedColumnWidth + spaceLeftOver / mNumColumns;
		gridView.setColumnWidth(mPhotoWidthPixels);

		mLoading = LayoutInflater.from(this).inflate(R.layout.temp_grid_item_loading, null);
		Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
		final AbsListView.LayoutParams params = new AbsListView.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - nudge);
		mLoading.setLayoutParams(params);

	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refreshes the comment list to show new comment.
		 */
		if (event.activity.action.toEntity != null && mForEntityId.equals(event.activity.action.toEntity.id)
				&& mListLinkSchema.equals(event.activity.action.entity.schema)) {
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
		Integer itemResId = R.layout.temp_grid_item_entity;
		return itemResId;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.entity_grid;
	}

}
