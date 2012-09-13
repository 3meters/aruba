package com.aircandi;

import java.util.ArrayList;
import java.util.List;

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
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.ServiceData;

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
			Entity collection = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, null, EntityTree.Map);
			mCommon.mActionBar.setTitle(collection.title);
		}
		else {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			mCommon.mActionBar.setHomeButtonEnabled(true);
			Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getMapBeaconById(mCommon.mBeaconId);
			mCommon.mActionBar.setTitle(beacon.label);
		}
	}

	public void bind(final Boolean useEntityModel) {

		if (!useEntityModel) {
			if (mCommon.mEntityId == null) {
				Beacon mapBeacon = ProxiExplorer.getInstance().getEntityModel().getMapBeaconById(mCommon.mBeaconId);
				mapBeacon.entities = null;
			}
			else {
				Entity parent = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, null, EntityTree.Map);
				if (parent.children != null) {
					parent.children = null;
				}
			}
		}

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_loading));
			}

			@Override
			protected Object doInBackground(Object... params) {

				ServiceResponse serviceResponse = new ServiceResponse();
				if (mCommon.mEntityId == null) {
					Beacon mapBeacon = ProxiExplorer.getInstance().getEntityModel().getMapBeaconById(mCommon.mBeaconId);
					if (mapBeacon.entities != null && mapBeacon.entities.size() > 0) {
						serviceResponse.data = mapBeacon.entities;
					}
					else {
						ArrayList<String> beaconIdsNew = new ArrayList<String>();
						beaconIdsNew.add(mCommon.mBeaconId);
						serviceResponse = ProxiExplorer.getInstance().getEntitiesForBeacons(beaconIdsNew, null, null, false, false);
						if (serviceResponse.responseCode == ResponseCode.Success) {
							ServiceData serviceData = (ServiceData) serviceResponse.data;
							List<Entity> entities = (List<Entity>) serviceData.data;
							serviceResponse.data = entities;
						}
					}
				}
				else {
					Entity parent = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, null, EntityTree.Map);
					if (parent.children != null && parent.children.size() > 0) {
						serviceResponse.data = parent.children;
					}
					else {
						String jsonFields = "{\"entities\":{},\"children\":{},\"parents\":{},\"comments\":{}}";
						String jsonEagerLoad = "{\"children\":true,\"parents\":true,\"comments\":false}";
						serviceResponse = ProxiExplorer.getInstance().getEntity(mCommon.mEntityId, jsonEagerLoad, jsonFields, null);
						ServiceData serviceData = (ServiceData) serviceResponse.data;
						serviceResponse.data = (EntityList<Entity>) serviceData.data;

					}
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					List<Entity> entities = (List<Entity>) serviceResponse.data;
					/*
					 * Check to see if we got anything back. If not then we want to move up the tree.
					 */
					if (entities.size() == 0) {
						mCommon.showProgressDialog(false, null);
						onBackPressed();
					}
					else {
						mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
						mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
						mEntityModelUser = Aircandi.getInstance().getUser();
						/*
						 * Push to model
						 */
						if (mCommon.mEntityId == null) {
							Beacon mapBeacon = ProxiExplorer.getInstance().getEntityModel().getMapBeaconById(mCommon.mBeaconId);
							mapBeacon.entities = entities;
						}
						else {
							Entity parent = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, null, EntityTree.Map);
							parent.children = (EntityList<Entity>) entities;
						}

						if (serviceResponse.data != null) {
							CandiListAdapter adapter = new CandiListAdapter(MapCandiList.this, entities,
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
		showCandiFormForEntity(entity, MapCandiForm.class);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mCommon.setActiveTab(2);
	}
}