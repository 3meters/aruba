package com.aircandi.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.CommandType;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ArrayListType;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class CandiList extends CandiActivity {

	private ListView		mListView;
	private Number			mEntityModelRefreshDate;
	private Number			mEntityModelActivityDate;
	private User			mEntityModelUser;
	private ArrayListType	mArrayListType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
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
		if (mArrayListType == ArrayListType.InCollection) {
			final Entity collection = ProxiManager.getInstance().getEntityModel().getCacheEntity(mCommon.mCollectionId);
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			mCommon.mActionBar.setTitle(collection.name);
		}
		else if (mArrayListType == ArrayListType.Collections) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(getString(R.string.name_entity_list_type_collection));
		}
		else if (mArrayListType == ArrayListType.CreatedByUser) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(Aircandi.getInstance().getUser().name);
		}
		else if (mArrayListType == ArrayListType.TunedPlaces) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(getString(R.string.radar_section_places));
		}
		else if (mArrayListType == ArrayListType.SyntheticPlaces) {
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
			mCommon.mActionBar.setHomeButtonEnabled(false);
			mCommon.mActionBar.setTitle(getString(R.string.radar_section_synthetics));
		}
	}

	private void initialize() {
		mListView = (ListView) findViewById(R.id.list_candi);
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mArrayListType = ArrayListType.valueOf(extras.getString(CandiConstants.EXTRA_LIST_TYPE));
		}
	}

	private void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetEntities");
				final ModelResult result = ProxiManager.getInstance().getEntityModel()
						.getEntitiesByListType(mArrayListType, refresh, mCommon.mCollectionId, mCommon.mUserId, ProxiConstants.RADAR_ENTITY_LIMIT);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Check to see if we got anything back. If not then we want to move up the tree.
					 */
					if (result.data == null || ((ArrayList<Entity>) result.data).size() == 0) {
						mCommon.hideBusy(true);
						onBackPressed();
					}
					else {
						mEntityModelRefreshDate = ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate();
						mEntityModelActivityDate = ProxiManager.getInstance().getEntityModel().getLastActivityDate();
						mEntityModelUser = Aircandi.getInstance().getUser();

						final CandiListAdapter adapter = new CandiListAdapter(CandiList.this, (ArrayList<Entity>) result.data, R.layout.temp_listitem_candi);
						mListView.setAdapter(adapter);
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiList);
				}
				mCommon.hideBusy(true);
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

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		Logger.v(this, "List item clicked");
		final Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
			if (entity.source.type.equals("twitter")) {
				AndroidManager.getInstance().callTwitterActivity(this, entity.source.id);
			}
			else if (entity.source.type.equals("facebook")) {
				AndroidManager.getInstance().callFacebookActivity(this, entity.source.id);
			}
			else if (entity.source.type.equals("website")) {
				AndroidManager.getInstance().callBrowserActivity(this, entity.source.id);
			}
		}
		else {
			mCommon.showCandiFormForEntity(entity, CandiForm.class);
		}
	}

	@SuppressWarnings("ucd")
	public void onCommentsClick(View view) {
		final Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {

			final IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setEntityId(entity.id)
					.setParentEntityId(entity.parentId)
					.setCollectionId(entity.id);

			final Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
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
					|| ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
					|| ProxiManager.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
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