package com.proxibase.aircandi;

import java.util.Collection;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.EndlessCandiListAdapter;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.IntentBuilder;
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
import com.proxibase.service.objects.User;

public class CandiList extends CandiActivity {

	private ListView	mListView;
	private Number		mEntityModelRefreshDate;
	private Number		mEntityModelActivityDate;
	private User		mEntityModelUser;
	private Entity		mCollectionEntity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Two sign in cases:
		 * 
		 * - Currently anonymous.
		 * - Session expired.
		 */
		if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByUser) {
			User user = Aircandi.getInstance().getUser();
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
				overridePendingTransition(R.anim.form_in, R.anim.browse_out);
				return;
			}
		}
		
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	protected void initialize() {
		mListView = (ListView) findViewById(R.id.list_candi);
	}

	public void bind() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mCommon.mCollectionId.equals(ProxiConstants.ROOT_COLLECTION_ID)) {
			if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByRadar) {
				mCommon.setActionBarTitleAndIcon(null, R.string.navigation_radar, true);
			}
			else if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByUser) {
				mCommon.mActionBar.setHomeButtonEnabled(false);
			}
		}
		else {
			mCollectionEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mCollectionId, mCommon.mCollectionType);
			mCommon.setActionBarTitleAndIcon(mCollectionEntity, true);
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
				EntityList<Entity> proxiEntities = ProxiExplorer.getInstance().getEntityModel()
						.getCollectionById(mCommon.mCollectionId, mCommon.mCollectionType);
				ServiceResponse serviceResponse = new ServiceResponse();
				serviceResponse.data = proxiEntities;
				/*
				 * If its the user collection and it hasn't been populated yet, chunk in the first set of entities.
				 */
				if (proxiEntities.getCollectionType() == CollectionType.CandiByUser && (proxiEntities.getCursorIds() == null || proxiEntities.size() == 0)) {

					Bundle parameters = new Bundle();
					ServiceRequest serviceRequest = new ServiceRequest();

					/* Set method parameters */
					parameters.putString("userId", Aircandi.getInstance().getUser().id);
					parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
					parameters.putString("options", "object:{\"limit\":"
							+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
							+ ",\"skip\":0"
							+ ",\"sort\":{\"modifiedDate\":-1} "
							+ ",\"children\":{\"limit\":"
							+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
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
						proxiEntities.setCollectionType(CollectionType.CandiByUser);
						proxiEntities.setCursorIds(serviceData.cursor);

						/* Do some fixup migrating settings to the children collection */
						for (Entity entity : proxiEntities) {
							if (entity.children != null) {
								entity.children.setCollectionEntity(entity);
								entity.children.setCollectionType(CollectionType.CandiByUser);
								entity.children.setCursorIds(entity.childCursor); // resets cursorIndex
							}
						}

						/* Assign again since the object was replaced and we use it to pass down the results */
						serviceResponse.data = proxiEntities;
					}
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
					mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
					mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
					mEntityModelUser = Aircandi.getInstance().getUser();
					if (serviceResponse.data != null) {
						mListView.setAdapter(new EndlessCandiListAdapter(CandiList.this, (EntityList<Entity>) serviceResponse.data,
								R.layout.temp_listitem_candi));
					}
				}
				mCommon.showProgressDialog(false, null);
				mCommon.stopTitlebarProgress();
			}

		}.execute();
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		mCommon.startTitlebarProgress();
		bind();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onListItemClick(View view) {
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		if (entity.type == CandiConstants.TYPE_CANDI_COMMAND) {

		}
		else {
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
			intentBuilder.setCommandType(CommandType.View);
			intentBuilder.setEntityId(entity.id);
			intentBuilder.setEntityType(entity.type);
			intentBuilder.setCollectionId(mCommon.mCollectionId);
			intentBuilder.setCollectionType(mCommon.mCollectionType);

			if (!entity.root) {
				//intentBuilder.setEntityLocation(entity.parent.location);
			}
			else {
				intentBuilder.setBeaconId(entity.beaconId);
			}
			Intent intent = intentBuilder.create();

			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}

	}

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {

			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View);
			intentBuilder.setEntityId(entity.id);
			intentBuilder.setCollectionId(entity.id);
			intentBuilder.setCollectionType(mCommon.mCollectionType);
			Intent intent = intentBuilder.create();
			startActivity(intent);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (resultCode == Activity.RESULT_CANCELED) {
				setResult(resultCode);
				finish();
			}
			else {
				initialize();
				bind();
			}
		}
		else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * We have to be pretty aggressive about refreshing the UI because
		 * there are lots of actions that could have happened while this activity
		 * was stopped that change what the user would expect to see.
		 * 
		 * - Entity deleted or modified
		 * - Entity children modified
		 * - New comments
		 * - Change in user which effects which candi and UI should be visible.
		 * - User profile could have been updated and we don't catch that.
		 */
		if (!isFinishing() && mEntityModelUser != null) {
			if (!Aircandi.getInstance().getUser().id.equals(mEntityModelUser.id)
					|| ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
					|| ProxiExplorer.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind();
			}
		}
	}

	@Override
	protected void onRestart() {
		/*
		 * This only gets called when the activity was stopped and is now coming back. All the logic
		 * that would normally be here is in onResume() because restart wasn't getting called
		 * reliably when returning from another activity.
		 */
		super.onRestart();

		/*
		 * Make sure the right tab is active. Restart could be happening because
		 * of the back stack so we can't assume that the tab was selected to get
		 * here.
		 */
		if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByRadar) {
			mCommon.setActiveTab(0);
		}
		else if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByUser) {
			mCommon.setActiveTab(1);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_list;
	}
}