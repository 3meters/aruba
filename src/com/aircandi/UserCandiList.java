package com.aircandi;

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
import com.aircandi.components.EntityList;
import com.aircandi.components.Events;
import com.aircandi.components.Events.EventHandler;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.User;

public class UserCandiList extends CandiListBase {

	private EventHandler	mEventUserChanged;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		User user = Aircandi.getInstance().getUser();
		/*
		 * If user is null then we are getting restarted after a crash
		 */
		if (user == null) {
			super.onCreate(savedInstanceState);
			finish();
			return;
		}

		mEventUserChanged = new EventHandler() {

			@Override
			public void onEvent(Object data) {

				User user = (User) data;
				if (user.isAnonymous()) {
					/*
					 * If user signed out then we need to navigate to radar
					 */
					IntentBuilder intentBuilder = new IntentBuilder(UserCandiList.this, CandiRadar.class);
					intentBuilder.setNavigationTop(true);
					Intent intent = intentBuilder.create();
					/*
					 * The flags let us use existing instance of radar if its already around.
					 */
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startActivity(intent);
					AnimUtils.doOverridePendingTransition(UserCandiList.this, TransitionType.CandiPageToCandiRadar);
				}
				else {
					bind(true);
				}
			}
		};

		/*
		 * Signin required if user is anonymous
		 */
		Integer messageResId = R.string.signin_message_mycandi;
		Boolean userAnonymous = user.isAnonymous();
		if (userAnonymous) {
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
			bind(false);
		}

	}

	private void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mCommon.mEntityId == null) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(Aircandi.getInstance().getUser().name);
		}
		else {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			mCommon.mActionBar.setHomeButtonEnabled(true);
			Entity collection = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
			mCommon.mActionBar.setTitle(collection.name);
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
					result = ProxiExplorer.getInstance().getEntityModel()
							.getUserEntities(Aircandi.getInstance().getUser().id, refresh, ProxiConstants.RADAR_ENTITY_LIMIT);
				}
				else {
					result = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mCollectionId, refresh, null, null);
					if (result.data != null) {
						result.data = ((Entity) result.data).getChildren();
					}
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Check to see if we got anything back. If not then we want to move up the tree.
					 */
					if (result.data == null || ((EntityList<Entity>) result.data).size() == 0) {
						mCommon.hideProgressDialog();
						onBackPressed();
					}
					else {
						mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
						mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
						mEntityModelUser = Aircandi.getInstance().getUser();

						CandiListAdapter adapter = new CandiListAdapter(UserCandiList.this, (EntityList<Entity>) result.data,
								R.layout.temp_listitem_candi);
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
		mCommon.showCandiFormForEntity(entity, UserCandiForm.class);
	}

	@Override
	protected void onResume() {
		super.onResume();
		synchronized (Events.EventBus.userChanged) {
			Events.EventBus.userChanged.add(mEventUserChanged);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		synchronized (Events.EventBus.userChanged) {
			Events.EventBus.userChanged.remove(mEventUserChanged);
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mCommon.setActiveTab(1);
	}

}