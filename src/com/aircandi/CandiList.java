package com.aircandi;

import android.os.Bundle;
import android.view.View;

import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.Logger;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityListType;
import com.aircandi.service.objects.Entity;

public class CandiList extends CandiListBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isFinishing()) {
			initialize();
			configureActionBar();
			bind(false);
		}
	}

	private void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mEntityListType == EntityListType.InCollection) {
			Entity collection = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mCollectionId);
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			mCommon.mActionBar.setTitle(collection.name);
		}
		else if (mEntityListType == EntityListType.Collections) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(getString(R.string.name_entity_list_type_collection));
		}
		else if (mEntityListType == EntityListType.CreatedByUser) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(Aircandi.getInstance().getUser().name);
		}
		else if (mEntityListType == EntityListType.TunedPlaces) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(getString(R.string.radar_section_places));
		}
		else if (mEntityListType == EntityListType.SyntheticPlaces) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(getString(R.string.radar_section_synthetics));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onListItemClick(View view) {
		Logger.v(this, "List item clicked");
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		mCommon.showCandiFormForEntity(entity, CandiForm.class);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}
}