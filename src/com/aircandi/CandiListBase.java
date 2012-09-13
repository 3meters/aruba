package com.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;

import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CommandType;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.User;

public abstract class CandiListBase extends CandiActivity {
	/*
	 * The base case is binding to radar entity tree.
	 */
	protected ListView	mListView;
	protected Number	mEntityModelRefreshDate;
	protected Number	mEntityModelActivityDate;
	protected User		mEntityModelUser;
	protected String	mFilter;

	protected void initialize() {
		mListView = (ListView) findViewById(R.id.list_candi);
	}

	public abstract void bind(Boolean useEntityModel);

	public void doRefresh() {
		/* Called from AircandiCommon */
		bind(false);
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
					.setCollectionId(entity.id)
					.setEntityTree(mCommon.mEntityTree);
			
			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
		}
	}

	public void showCandiFormForEntity(Entity entity, Class<?> clazz) {

		IntentBuilder intentBuilder = new IntentBuilder(this, clazz);
		intentBuilder.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type)
				.setCollectionId(mCommon.mCollectionId)
				.setEntityTree(mCommon.mEntityTree);

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