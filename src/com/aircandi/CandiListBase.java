package com.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CommandType;
import com.aircandi.components.EntityList;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer.EntityListType;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public abstract class CandiListBase extends CandiActivity {
	/*
	 * The base case is binding to radar entity tree.
	 */
	protected ListView			mListView;
	protected Number			mEntityModelRefreshDate;
	protected Number			mEntityModelActivityDate;
	protected User				mEntityModelUser;
	protected String			mFilter;
	protected EntityListType	mEntityListType;

	protected void initialize() {
		mListView = (ListView) findViewById(R.id.list_candi);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityListType = EntityListType.valueOf(extras.getString(CandiConstants.EXTRA_LIST_TYPE));
		}
	}

	public void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = ProxiExplorer.getInstance().getEntityModel()
						.getEntitiesByListType(mEntityListType, refresh, mCommon.mCollectionId, mCommon.mUserId, ProxiConstants.RADAR_ENTITY_LIMIT);
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
						mCommon.hideBusy();
						onBackPressed();
					}
					else {
						mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
						mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
						mEntityModelUser = Aircandi.getInstance().getUser();

						CandiListAdapter adapter = new CandiListAdapter(CandiListBase.this, (EntityList<Entity>) result.data, R.layout.temp_listitem_candi);
						mListView.setAdapter(adapter);
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiList);
				}
				mCommon.hideBusy();
			}

		}.execute();
	}


	public void doRefresh() {
		/* Called from AircandiCommon */
		bind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public abstract void onListItemClick(View view);

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {

			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setEntityId(entity.id)
					.setParentEntityId(entity.parentId)
					.setCollectionId(entity.id);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
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
				bind(true);
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
		 * - User signed out.
		 * - User profile could have been updated and we don't catch that.
		 */
		if (!isFinishing() && mEntityModelUser != null) {
			if (!Aircandi.getInstance().getUser().id.equals(mEntityModelUser.id)
					|| ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
					|| ProxiExplorer.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
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