package com.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ListView;

import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.CommandType;
import com.aircandi.components.EntityList;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.ServiceData;

public class MapCandiList extends CandiList {

	protected void initialize() {
		mListView = (ListView) findViewById(R.id.list_candi);
	}

	public void bind() {
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
				if (mCommon.mEntityId == null) {
					ArrayList<String> beaconIdsNew = new ArrayList<String>();
					beaconIdsNew.add(mCommon.mBeaconId);
					serviceResponse = ProxiExplorer.getInstance().getEntitiesForBeacons(beaconIdsNew, null, null, false);
					if (serviceResponse.responseCode == ResponseCode.Success) {
						ServiceData serviceData = (ServiceData) serviceResponse.data;
						List<Entity> entities = (List<Entity>) serviceData.data;
						serviceResponse.data = entities;
					}
				}
				else {
					String jsonFields = "{\"entities\":{},\"children\":{},\"parents\":{},\"comments\":{}}";
					String jsonEagerLoad = "{\"children\":true,\"parents\":true,\"comments\":false}";
					serviceResponse = ProxiExplorer.getInstance().getEntity(mCommon.mEntityId, jsonEagerLoad, jsonFields, null);
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					List<Entity> entities = null;
					if (mCommon.mEntityId == null) {
						entities = (List<Entity>) serviceResponse.data;
					}
					else {
						ServiceData serviceData = (ServiceData) serviceResponse.data;
						Entity entity = (Entity) serviceData.data;
						entities = entity.children;
					}
					
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

		IntentBuilder intentBuilder = new IntentBuilder(this, MapCandiForm.class);
		intentBuilder.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type)
				.setEntityTree(EntityTree.Map)
				.setCollectionId(mCommon.mCollectionId);

		if (entity.parent != null) {
			intentBuilder.setEntityLocation(entity.parent.location);
		}
		else {
			intentBuilder.setBeaconId(entity.beaconId);
		}

		Intent intent = intentBuilder.create();

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiListToCandiForm);
	}
}