package com.proxibase.aircandi;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.Command.CommandType;
import com.proxibase.aircandi.components.EndlessCandiListAdapter;
import com.proxibase.aircandi.components.EntityProvider;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;

public class CandiList extends CandiActivity {

	public static enum MethodType {
		CandiByUser, CandiForParent, CandiByRadar
	}

	private ListView		mListView;
	private EntityProvider	mEntityProvider;
	private MethodType		mMethodType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Aircandi.getInstance().getLaunchedFromRadar()) {
			/*
			 * Try to detect case where this is being created after
			 * a crash and bail out.
			 */
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
		else {
			initialize();
			bind();
		}
	}

	private void initialize() {
		mMethodType = MethodType.CandiByUser;
		if (mCommon.mEntity != null) {
			mMethodType = MethodType.CandiForParent;
		}
		mEntityProvider = new EntityProvider(mMethodType, Aircandi.getInstance().getUser().id, mCommon.mEntity != null ? mCommon.mEntity.id : null);
	}

	@Override
	public void bind() {
		super.bind();

		mListView = (ListView) findViewById(R.id.list_candi);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {
				List<Entity> entities = mEntityProvider.loadEntities();
				return entities;
			}

			@Override
			protected void onPostExecute(Object entities) {
				if (entities != null) {
					mListView.setAdapter(new EndlessCandiListAdapter(CandiList.this, (List<Entity>) entities, mEntityProvider, R.layout.temp_listitem_candi));
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
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		if (entity.type == CandiConstants.TYPE_CANDI_COMMAND) {

		}
		else {

			IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
			intentBuilder.setCommand(new Command(CommandType.View));
			intentBuilder.setEntity(entity);
			intentBuilder.setEntityType(entity.type);
			if (!entity.root) {
				intentBuilder.setEntityLocation(mEntityProvider.getParentEntity().location);
			}
			else {
				intentBuilder.setBeaconId(entity.beaconId);
			}
			Intent intent = intentBuilder.create();

			startActivityForResult(intent, CandiConstants.ACTIVITY_CANDI_INFO);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}

	}

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {

			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommand(new Command(CommandType.View));
			intentBuilder.setEntity(entity);
			Intent intent = intentBuilder.create();
			startActivityForResult(intent, 0);
		}
	}

	public void onBackPressed() {
		if (mMethodType == MethodType.CandiByUser) {
			Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);
			Intent intent = new Intent(this, CandiRadar.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
		}
		else {
			setResult(mLastResultCode);
			super.onBackPressed();
		}
	}

	public void onRefreshClick(View view) {
		mCommon.startTitlebarProgress();
		mEntityProvider.reset();
		bind();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mLastResultCode = resultCode;
		if (resultCode == CandiConstants.RESULT_ENTITY_UPDATED || resultCode == CandiConstants.RESULT_ENTITY_DELETED
				|| resultCode == CandiConstants.RESULT_ENTITY_INSERTED || resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			mEntityProvider.reset();
			bind();
			if (resultCode == CandiConstants.RESULT_ENTITY_DELETED) {
				mLastResultCode = CandiConstants.RESULT_ENTITY_CHILD_DELETED;
			}
		}
		else if (resultCode == CandiConstants.RESULT_PROFILE_UPDATED) {
			mCommon.updateUserPicture();
			mEntityProvider.reset();
			bind();
		}
		else if (resultCode == CandiConstants.RESULT_USER_SIGNED_IN) {
			mCommon.updateUserPicture();

			/* Need to rebind if showing my candi */
			if (mMethodType == MethodType.CandiByUser) {
				mCommon.startTitlebarProgress();
				mEntityProvider.reset();
				bind();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rebind = mCommon.doOptionsItemSelected(item);
		if (rebind) {
			mEntityProvider.reset();
			bind();
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_list;
	}

}