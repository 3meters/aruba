package com.aircandi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.EntityList;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.service.objects.Entity;

public class CandiList extends CandiListBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isFinishing()) {
			initialize();
			configureActionBar();
			bind();
		}
	}

	private void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		Entity collection = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mCollectionId, null, EntityTree.Radar);
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		mCommon.mActionBar.setTitle(collection.title);
	}

	public void bind() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_loading));
			}

			@Override
			protected Object doInBackground(Object... params) {
				/*
				 * Load up the data. We are usually displaying data where the initial chunk has
				 * already been fetched from the service. The exception is candi by user. If the
				 * first fetch hasn't happened yet, we handle it here.
				 */
				ServiceResponse serviceResponse = new ServiceResponse();
				EntityList<Entity> entitiesRadar = ProxiExplorer.getInstance().getEntityModel().getCollectionById(mCommon.mCollectionId, EntityTree.Radar);
				serviceResponse.data = entitiesRadar;
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Check to see if we got anything back. If not then we want to move up the tree.
					 */
					if (((EntityList<Entity>) serviceResponse.data).size() == 0) {
						mCommon.showProgressDialog(false, null);
						onBackPressed();
					}
					else {

						mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
						mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
						mEntityModelUser = Aircandi.getInstance().getUser();
						if (serviceResponse.data != null) {
							CandiListAdapter adapter = new CandiListAdapter(CandiList.this, (EntityList<Entity>) serviceResponse.data,
									R.layout.temp_listitem_candi);
							mListView.setAdapter(adapter);
						}
					}
				}
				mCommon.showProgressDialog(false, null);
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onListItemClick(View view) {
		Logger.v(this, "List item clicked");
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		showCandiFormForEntity(entity, CandiForm.class);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mCommon.setActiveTab(0);
	}

}