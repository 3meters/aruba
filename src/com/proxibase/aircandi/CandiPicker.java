package com.proxibase.aircandi;

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
import com.proxibase.aircandi.components.CandiListAdapter;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiExplorer.CollectionType;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.ServiceData;

public class CandiPicker extends FormActivity implements ActionBar.TabListener {

	private ListView			mListViewCandi;
	private EntityList<Entity>	mEntities	= new EntityList<Entity>();
	private CollectionType		mMethodType	= CollectionType.CandiByRadar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mCommon.track();

		/* Action bar */
		mCommon.mActionBar.setTitle(R.string.form_title_candi_picker);

		mListViewCandi = (ListView) findViewById(R.id.list_candi);
		mListViewCandi.setDivider(null);

		mCommon.setActiveTab(0);
		bind();
	}

	public void bind() {
		if (mMethodType == CollectionType.CandiByRadar) {
			EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel()
					.getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, CollectionType.CandiByRadar);
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
		else if (mMethodType == CollectionType.CandiByUser) {

			EntityList<Entity> entitiesUserCandi = ProxiExplorer.getInstance().getEntityModel()
					.getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, CollectionType.CandiByUser);

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
								.getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, CollectionType.CandiByUser);

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

							entitiesUserCandi.setCollectionType(CollectionType.CandiByUser);
							entitiesUserCandi.addAll((Collection<? extends Entity>) serviceData.data);
							
							/* Do some fixup migrating settings to the children collection */
							for (Entity entity : entitiesUserCandi) {
								if (entity.children != null) {
									entity.children.setCollectionType(CollectionType.CandiByUser);
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
						mCommon.showProgressDialog(false, null);
						mCommon.stopTitlebarProgress();
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
			mMethodType = ProxiExplorer.CollectionType.CandiByRadar;
			bind();
		}
		else if (((Integer) tab.getTag()) == R.string.candi_picker_tab_mycandi) {
			mMethodType = ProxiExplorer.CollectionType.CandiByUser;
			bind();
		}

	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}
}