package com.aircandi;

import java.util.Collection;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.EntityList;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ProxibaseService.GsonType;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.R;

public class CandiPicker extends FormActivity implements ActionBar.TabListener {

	private ListView			mListViewCandi;
	private EntityList<Entity>	mEntities	= new EntityList<Entity>();
	private EntityTree		mMethodType	= EntityTree.Radar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mListViewCandi = (ListView) findViewById(R.id.list_candi);
		mListViewCandi.setDivider(null);
		bind();
	}

	public void bind() {
		if (mMethodType == EntityTree.Radar) {
			EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel()
					.getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, EntityTree.Radar);
			if (entities != null) {
				mEntities.clear();
				for (Entity entity : entities) {
					if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION) && !entity.locked) {
						mEntities.add(entity);
					}
				}
				CandiListAdapter adapter = new CandiListAdapter(CandiPicker.this, mEntities, R.layout.temp_listitem_candi_picker);
				mListViewCandi.setAdapter(adapter);
			}
		}
		else if (mMethodType == EntityTree.User) {

			EntityList<Entity> entitiesUserCandi = ProxiExplorer.getInstance().getEntityModel()
					.getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, EntityTree.User);

			if (entitiesUserCandi != null && entitiesUserCandi.size() > 0) {
				mEntities.clear();
				for (Entity entity : entitiesUserCandi) {
					if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION) && !entity.locked) {
						mEntities.add(entity);
					}
				}
				CandiListAdapter adapter = new CandiListAdapter(CandiPicker.this, mEntities, R.layout.temp_listitem_candi_picker);
				mListViewCandi.setAdapter(adapter);
			}
			else {
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
						Bundle parameters = new Bundle();
						ServiceRequest serviceRequest = new ServiceRequest();
						EntityList<Entity> entitiesUserCandi = ProxiExplorer.getInstance().getEntityModel()
								.getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, EntityTree.User);

						/* Set method parameters */
						parameters.putString("userId", Aircandi.getInstance().getUser().id);
						parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
						parameters.putString("options", "object:{\"limit\":"
								+ String.valueOf(ProxiConstants.RADAR_ENTITY_LIMIT)
								+ ",\"skip\":0"
								+ ",\"sort\":{\"modifiedDate\":-1} "
								+ ",\"children\":{\"limit\":"
								+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
								+ ",\"skip\":0"
								+ ",\"sort\":{\"modifiedDate\":-1}}"
								+ "}");

						serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForUser")
								.setRequestType(RequestType.Method)
								.setParameters(parameters)
								.setResponseFormat(ResponseFormat.Json);

						serviceResponse = NetworkManager.getInstance().request(serviceRequest);

						if (serviceResponse.responseCode == ResponseCode.Success) {

							String jsonResponse = (String) serviceResponse.data;
							ServiceData serviceData = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

							entitiesUserCandi.setCollectionType(EntityTree.User);
							entitiesUserCandi.addAll((Collection<? extends Entity>) serviceData.data);
							
							/* Do some fixup migrating settings to the children collection */
							for (Entity entity : entitiesUserCandi) {
								if (entity.children != null) {
									entity.children.setCollectionType(EntityTree.User);
									for (Entity childEntity : entity.children) {
										childEntity.parent = entity;
										childEntity.parentId = entity.id;
									}
								}
							}

							/* Assign again since the object was replaced and we use it to pass down the results */
							serviceResponse.data = entitiesUserCandi;
						}
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object response) {
						ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.Success) {
							/*
							 * These only get set on the first data pass, chunking does not change them.
							 */
							if (serviceResponse.data != null) {
								EntityList<Entity> entitiesUserCandi = (EntityList<Entity>) serviceResponse.data;
								mEntities.clear();
								for (Entity entity : entitiesUserCandi) {
									if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION) && !entity.locked) {
										mEntities.add(entity);
									}
								}
								CandiListAdapter adapter = new CandiListAdapter(CandiPicker.this, mEntities, R.layout.temp_listitem_candi_picker);
								mListViewCandi.setAdapter(adapter);
							}
						}
						else {
							mCommon.handleServiceError(serviceResponse, ServiceOperation.PickCandi);
						}
						mCommon.showProgressDialog(false, null);
					}

				}.execute();
			}
		}
	}

	public void onListItemClick(View view) {
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_ID), entity.id);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	public void onMoveSoloButtonClick(View view) {
		/*
		 * Not returning an entity id is a message that the user has choosen
		 * to go solo with the candi.
		 */
		Intent intent = new Intent();
		setResult(Activity.RESULT_OK, intent);
		finish();
	}
	
	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_picker;
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		if (((Integer) tab.getTag()) == R.string.candi_picker_tab_radar) {
			mMethodType = ProxiExplorer.EntityTree.Radar;
			bind();
		}
		else if (((Integer) tab.getTag()) == R.string.candi_picker_tab_mycandi) {
			mMethodType = ProxiExplorer.EntityTree.User;
			bind();
		}

	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}
}