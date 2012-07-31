package com.proxibase.aircandi;

import android.app.Activity;
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
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiExplorer.EntityTree;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.User;

public class CandiList extends CandiActivity {
	/*
	 * The base case is binding to radar entity tree.
	 */
	protected ListView	mListView;
	protected Number	mEntityModelRefreshDate;
	protected Number	mEntityModelActivityDate;
	protected User		mEntityModelUser;
	protected String	mFilter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

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
				EntityList<Entity> entitiesRadar = ProxiExplorer.getInstance().getEntityModel()
						.getCollectionById(mCommon.mCollectionId, EntityTree.Radar);
				ServiceResponse serviceResponse = new ServiceResponse();
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
						mCommon.stopTitlebarProgress();
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
		
		Logger.v(this, "List item clicked");
		
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setEntityId(entity.id);
		intentBuilder.setParentEntityId(entity.parentId);
		intentBuilder.setEntityType(entity.type);
		intentBuilder.setEntityTree(EntityTree.Radar);

		intentBuilder.setCollectionId(mCommon.mCollectionId);
		
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

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {

			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View);
			intentBuilder.setEntityId(entity.id);
			intentBuilder.setParentEntityId(entity.parentId);
			intentBuilder.setCollectionId(entity.id);
			intentBuilder.setEntityTree(mCommon.mEntityTree);
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
		if (mCommon.mEntityTree == ProxiExplorer.EntityTree.Radar) {
			mCommon.setActiveTab(0);
		}
		else if (mCommon.mEntityTree == ProxiExplorer.EntityTree.User) {
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