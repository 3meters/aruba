package com.proxibase.aircandi;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.components.CandiListAdapter;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class CandiList extends AircandiActivity {

	public enum MethodType {
		CandiByUser, CandiForParent
	}

	private ListView			mListView;
	private Context				mContext;
	protected List<EntityProxy>	mListEntities;
	private MethodType			mMethodType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();
		GoogleAnalyticsTracker.getInstance().trackPageView("/YourCandiList");
	}

	private void initialize() {
		mContext = this;
	}

	protected void bind() {

		mListView = (ListView) findViewById(R.id.list_candi);

		final Bundle parameters = new Bundle();
		final ServiceRequest serviceRequest = new ServiceRequest();

		if (mListEntities == null && mEntityProxy != null) {
			mMethodType = MethodType.CandiForParent;
			parameters.putInt("entityId", mEntityProxy.id);
			parameters.putBoolean("includeChildren", true);
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntity");
		}
		else if (mListEntities == null && mEntityProxy == null) {
			mMethodType = MethodType.CandiByUser;
			parameters.putInt("userId", Integer.parseInt(mUser.id));
			parameters.putBoolean("includeChildren", false);
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntitiesForUser");
		}

		showProgressDialog(true, "Loading...");
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {

				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				if (serviceResponse.responseCode != ResponseCode.Success) {
					return serviceResponse;
				}
				else {
					String jsonResponse = (String) serviceResponse.data;
					mListEntities = (List<EntityProxy>) (List<?>) ProxibaseService.convertJsonToObjects(jsonResponse,
								EntityProxy.class,
								GsonType.ProxibaseService);
					if (mMethodType == MethodType.CandiForParent) {
						mListEntities = mListEntities.get(0).children;
					}
					return serviceResponse;
				}
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					if (mListEntities != null) {
						mListView.setAdapter(new CandiListAdapter(mContext, mUser, mListEntities));
						showProgressDialog(false, null);
					}
				}
			}
		}.execute();
	}

	protected void draw() {}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onListItemClick(View view) {
		CandiListViewHolder holder = (CandiListViewHolder) view.getTag();
		Intent intent = Aircandi.buildIntent(this, (EntityProxy) holder.data, 0, false, null, new Command("view"), mCandiTask, null, mUser, CandiForm.class);
		startActivity(intent);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_list;
	}
}