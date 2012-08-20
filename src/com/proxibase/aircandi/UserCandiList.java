package com.proxibase.aircandi;

import java.util.Collection;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;
import com.proxibase.aircandi.components.CandiListAdapter;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiExplorer.EntityTree;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

public class UserCandiList extends CandiList {

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
	}

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
			Entity collection = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, null, EntityTree.User);
			mCommon.mActionBar.setTitle(collection.title);
		}
		else {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
		}

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
						mCommon.stopTitlebarProgress();
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
				mCommon.showProgressDialog(false, null);
				mCommon.stopTitlebarProgress();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onListItemClick(View view) {
		Logger.v(this, "List item clicked");

		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		IntentBuilder intentBuilder = new IntentBuilder(this, UserCandiForm.class);
		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setEntityId(entity.id);
		intentBuilder.setParentEntityId(entity.parentId);
		intentBuilder.setEntityType(entity.type);
		intentBuilder.setCollectionId(mCommon.mCollectionId);
		intentBuilder.setEntityTree(EntityTree.User);

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