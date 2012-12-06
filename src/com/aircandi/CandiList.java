package com.aircandi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
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
		Entity collection = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mCollectionId);
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		mCommon.mActionBar.setTitle(collection.name);
	}

	public void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mCollectionId, refresh, null, null);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Check to see if we got anything back. If not then we want to move up the tree.
					 */
					if (result.data == null) {
						mCommon.hideBusy();
						onBackPressed();
					}
					else {
						mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
						mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
						mEntityModelUser = Aircandi.getInstance().getUser();

						Entity entity = (Entity) result.data;

						CandiListAdapter adapter = new CandiListAdapter(CandiList.this,  entity.getChildren(), R.layout.temp_listitem_candi);
						mListView.setAdapter(adapter);
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiList);
				}
				mCommon.hideBusy();
			}

		}.execute();
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
		mCommon.setActiveTab(0);
	}
}