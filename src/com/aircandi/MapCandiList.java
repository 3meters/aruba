package com.aircandi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.EntityList;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;

public class MapCandiList extends CandiListBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isFinishing()) {
			initialize();
			configureActionBar();
			bind(true);
		}
	}

	private void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mCommon.mEntityId != null) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			mCommon.mActionBar.setHomeButtonEnabled(true);
			Entity collection = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mEntityId);
			mCommon.mActionBar.setTitle(collection.title);
		}
		else if (mCommon.mBeaconId != null) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			mCommon.mActionBar.setHomeButtonEnabled(true);
			Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(mCommon.mBeaconId);
			mCommon.mActionBar.setTitle(beacon.label);
		}
	}

	public void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(getString(R.string.progress_loading), true);
			}

			@Override
			protected Object doInBackground(Object... params) {

				ModelResult result = null;
				if (mCommon.mEntityId == null) {
					result = ProxiExplorer.getInstance().getEntityModel().getBeaconEntities(mCommon.mBeaconId, refresh);
				}
				else {
					result = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mCollectionId, refresh, true, null, null);
					if (result.data != null) {
						result.data = ((Entity) result.data).getChildren();
					}
				}
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
						mCommon.hideProgressDialog();
						onBackPressed();
					}
					else {
						mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
						mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
						mEntityModelUser = Aircandi.getInstance().getUser();

						CandiListAdapter adapter = new CandiListAdapter(MapCandiList.this, (EntityList<Entity>) result.data, R.layout.temp_listitem_candi);
						mListView.setAdapter(adapter);
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiList);
				}
				mCommon.hideProgressDialog();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onListItemClick(View view) {
		Logger.v(this, "List item clicked");
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		showCandiFormForEntity(entity, MapCandiForm.class);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mCommon.setActiveTab(2);
	}
}