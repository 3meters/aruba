package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.components.CandiListAdapter;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;

public class CandiList extends CandiActivity {

	public static enum MethodType {
		CandiByUser, CandiForParent
	}

	private ListView		mListView;
	protected List<Entity>	mListEntities;
	private Entity			mListParentEntity;
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
			bind();
		}
	}

	@Override
	public void bind() {
		super.bind();

		mListView = (ListView) findViewById(R.id.list_candi);

		final Bundle parameters = new Bundle();
		final ServiceRequest serviceRequest = new ServiceRequest();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				if (mCommon.mEntity != null) {

					mMethodType = MethodType.CandiForParent;
					ArrayList<String> entityIds = new ArrayList<String>();
					entityIds.add(mCommon.mEntity.id);
					parameters.putStringArrayList("entityIds", entityIds);
					parameters.putString("eagerLoad", "object:{\"children\":true,\"comments\":false}");

					serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities").setRequestType(RequestType.Method)
							.setParameters(parameters).setResponseFormat(ResponseFormat.Json);
					mCommon.track("/CandiList");
				}
				else if (mCommon.mEntity == null) {
					mMethodType = MethodType.CandiByUser;
					parameters.putString("userId", Aircandi.getInstance().getUser().id);
					parameters.putString("eagerLoad", "object:{\"children\":false,\"comments\":false}");

					serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForUser").setRequestType(RequestType.Method)
							.setParameters(parameters).setResponseFormat(ResponseFormat.Json);
					mCommon.track("/MyCandiList");
				}

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mListEntities = (List<Entity>) (List<?>) ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class,
							GsonType.ProxibaseService);
					if (mMethodType == MethodType.CandiForParent) {
						mListParentEntity = mListEntities.get(0);
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
						Collections.sort(mListEntities, new Entity.SortEntitiesByUpdatedTime());
						mListView.setAdapter(new CandiListAdapter(CandiList.this, Aircandi.getInstance().getUser(), R.layout.temp_listitem_candi,
								mListEntities));
					}
					mCommon.showProgressDialog(false, null);
					mCommon.stopTitlebarProgress();
				}
				else {
					mCommon.handleServiceError(serviceResponse);
				}
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
		intentBuilder.setEntityType(entity.type);
		if (!entity.root) {
			intentBuilder.setEntityLocation(mListParentEntity.location);
		}
		else {
			intentBuilder.setBeaconId(entity.beaconId);
		}
		Intent intent = intentBuilder.create();

		startActivityForResult(intent, CandiConstants.ACTIVITY_CANDI_INFO);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

	}

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentsCount > 0) {

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
		if (resultCode == CandiConstants.RESULT_ENTITY_UPDATED || resultCode == CandiConstants.RESULT_ENTITY_DELETED
				|| resultCode == CandiConstants.RESULT_ENTITY_INSERTED || resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			bind();
			if (resultCode == CandiConstants.RESULT_ENTITY_DELETED) {
				mLastResultCode = CandiConstants.RESULT_ENTITY_CHILD_DELETED;
			}
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
		if (rebind) bind();
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_list;
	}

}