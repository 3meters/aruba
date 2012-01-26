package com.proxibase.aircandi;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.components.CandiListAdapter;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.Entity;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class CandiList extends CandiActivity {

	public enum MethodType {
		CandiByUser, CandiForParent
	}

	private ListView		mListView;
	protected List<Entity>	mListEntities;
	private MethodType		mMethodType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bind();
		Tracker.trackPageView("/MyCandiList");
	}

	@Override
	public void bind() {
		super.bind();

		mListView = (ListView) findViewById(R.id.list_candi);

		final Bundle parameters = new Bundle();
		final ServiceRequest serviceRequest = new ServiceRequest();

		if (mCommon.mEntity != null) {
			mMethodType = MethodType.CandiForParent;
			parameters.putInt("entityId", mCommon.mEntity.id);
			parameters.putBoolean("includeChildren", true);
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntity");
		}
		else if (mCommon.mEntity == null) {
			mMethodType = MethodType.CandiByUser;
			parameters.putInt("userId", Integer.parseInt(Aircandi.getInstance().getUser().id));
			parameters.putBoolean("includeChildren", false);
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntitiesForUser");
		}

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mListEntities = (List<Entity>) (List<?>) ProxibaseService.convertJsonToObjects(jsonResponse,
								Entity.class,
								GsonType.ProxibaseService);
					if (mMethodType == MethodType.CandiForParent) {
						mListEntities = mListEntities.get(0).children;
					}
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					if (mListEntities != null) {
						Collections.sort(mListEntities, new SortEntitiesByUpdatedTime());
						mListView.setAdapter(new CandiListAdapter(CandiList.this, Aircandi.getInstance().getUser(), mListEntities));
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
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;

		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
		intentBuilder.setCommand(new Command(CommandVerb.View));
		intentBuilder.setEntity(entity);
		intentBuilder.setEntityType(entity.entityType);
		Intent intent = intentBuilder.create();

		startActivityForResult(intent, CandiConstants.ACTIVITY_CANDI_INFO);
	}

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {

			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommand(new Command(CommandVerb.View));
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
		bind();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mLastResultCode = resultCode;
		if (resultCode == CandiConstants.RESULT_ENTITY_UPDATED
				|| resultCode == CandiConstants.RESULT_ENTITY_DELETED
				|| resultCode == CandiConstants.RESULT_ENTITY_INSERTED
				|| resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			bind();
		}
		else if (resultCode == CandiConstants.RESULT_PROFILE_UPDATED) {
			mCommon.updateUserPicture();
			bind();
		}
		else if (resultCode == CandiConstants.RESULT_USER_SIGNED_IN) {
			mCommon.updateUserPicture();

			/* Need to rebind if showing my candi */
			if (mMethodType == MethodType.CandiByUser) {
				mCommon.startTitlebarProgress();
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
			bind();
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_list;
	}

	class SortEntitiesByUpdatedTime implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {
			int dateObject1 = object1.updatedDate != null ? object1.updatedDate : object1.createdDate;
			int dateObject2 = object2.updatedDate != null ? object2.updatedDate : object2.createdDate;

			if (dateObject1 < dateObject2) {
				return 1;
			}
			else if (dateObject1 == dateObject2) {
				return 0;
			}
			return -1;
		}
	}
}