package com.aircandi;

import java.util.Collection;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.CommandType;
import com.aircandi.components.DateUtils;
import com.aircandi.components.EntityList;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.GsonType;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;

public class UserCandiList extends CandiListBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/*
		 * Two sign in cases:
		 * 
		 * - Currently anonymous.
		 * - Session expired.
		 */
		User user = Aircandi.getInstance().getUser();
		/*
		 * If user is null then we are getting restarted after a crash
		 */
		if (user == null) {
			super.onCreate(savedInstanceState);
			finish();
			return;
		}
		Boolean expired = false;
		Integer messageResId = R.string.signin_message_mycandi;
		Boolean userAnonymous = user.anonymous;
		if (user.session != null) {
			expired = user.session.renewSession(DateUtils.nowDate().getTime());
		}
		if (userAnonymous || expired) {
			if (expired) {
				messageResId = R.string.signin_message_session_expired;
			}
			IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
			intentBuilder.setCommandType(CommandType.Edit);
			intentBuilder.setMessage(getString(messageResId));
			Intent intent = intentBuilder.create();
			startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
			super.onCreate(savedInstanceState);
			return;
		}
		
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
		if (mCommon.mEntityId != null) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			mCommon.mActionBar.setHomeButtonEnabled(true);
			Entity collection = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, null, EntityTree.User);
			mCommon.mActionBar.setTitle(collection.title);
		}
		else {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(Aircandi.getInstance().getUser().name);
		}
	}
	
	public void bind() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_loading));
			}

			@Override
			protected Object doInBackground(Object... params) {

				ServiceResponse serviceResponse = new ServiceResponse();
				EntityList<Entity> proxiEntities = ProxiExplorer.getInstance().getEntityModel().getCollectionById(mCommon.mCollectionId, EntityTree.User);
				Boolean entityTreeEmpty = (ProxiExplorer.getInstance().getEntityModel().getUserEntities().size() == 0);
				/*
				 * If its the user collection and it hasn't been populated yet, do the work.
				 */
				if (entityTreeEmpty) {

					Bundle parameters = new Bundle();
					ServiceRequest serviceRequest = new ServiceRequest();

					/* Set method parameters */
					parameters.putString("userId", Aircandi.getInstance().getUser().id);
					if (mFilter != null) {
						parameters.putString("filter", mFilter);
					}
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

						proxiEntities.addAll((Collection<? extends Entity>) serviceData.data);
						proxiEntities.setCollectionType(EntityTree.User);

						/* Do some fixup migrating settings to the children collection */
						for (Entity entity : proxiEntities) {
							if (entity.children != null) {
								entity.children.setCollectionType(EntityTree.User);
								for (Entity childEntity : entity.children) {
									childEntity.parent = entity;
									childEntity.parentId = entity.id;
								}
							}
						}

						/* Assign again since the object was replaced and we use it to pass down the results */
						serviceResponse.data = proxiEntities;
					}
				}
				else {
					proxiEntities = ProxiExplorer.getInstance().getEntityModel().getCollectionById(mCommon.mCollectionId, EntityTree.User);
					serviceResponse.data = proxiEntities;
				}
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
							CandiListAdapter adapter = new CandiListAdapter(UserCandiList.this, (EntityList<Entity>) serviceResponse.data,
									R.layout.temp_listitem_candi);
							mListView.setAdapter(adapter);
						}
					}
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiList);
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
		showCandiFormForEntity(entity, UserCandiForm.class);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mCommon.setActiveTab(1);
	}

}