package com.proxibase.aircandi;

import java.util.Collection;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.EndlessCandiListAdapter;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer.CollectionType;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.ServiceData;

public class CandiPicker extends FormActivity implements OnItemClickListener {

	private ListView			mListViewUserCandi;
	private ListView			mListViewRadarCandi;
	private EntityList<Entity>	mEntitiesUserCandi;
	private EntityList<Entity>	mEntitiesRadarCandi;
	private ViewFlipper			mViewFlipper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mCommon.track();

		/* Action bar */
		mCommon.mActionBar.setTitle(R.string.form_title_candi_picker);

		mListViewRadarCandi = (ListView) findViewById(R.id.list_radar_candi);
		mListViewRadarCandi.setOnItemClickListener(this);
		mListViewRadarCandi.setDivider(null);
		mListViewRadarCandi.setTextFilterEnabled(true);
		mEntitiesRadarCandi = ProxiExplorer.getInstance().getEntityModel().getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, CollectionType.CandiByRadar);

		mListViewUserCandi = (ListView) findViewById(R.id.list_user_candi);
		mListViewUserCandi.setOnItemClickListener(this);
		mListViewUserCandi.setDivider(null);
		mListViewUserCandi.setTextFilterEnabled(true);
		mEntitiesUserCandi = ProxiExplorer.getInstance().getEntityModel().getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, CollectionType.CandiByUser);

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mCommon.setViewFlipper(mViewFlipper);

		if (mViewFlipper != null) {
			mCommon.setActiveTab(0);
			bind();
		}
	}

	public void bind() {
		mListViewRadarCandi.setAdapter(new EndlessCandiListAdapter(CandiPicker.this, mEntitiesRadarCandi, R.layout.temp_listitem_candi));
		EndlessCandiListAdapter adapter = (EndlessCandiListAdapter) mListViewRadarCandi.getAdapter();
		adapter.getWrappedAdapterX().getFilter().filter("candipatches");
		
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
				ServiceResponse serviceResponse = new ServiceResponse();
				serviceResponse.data = mEntitiesUserCandi;
				/*
				 * If its the user collection and it hasn't been populated yet, chunk in the first set of entities.
				 */
				if (mEntitiesUserCandi.getCollectionType() == CollectionType.CandiByUser && (mEntitiesUserCandi.getCursorIds() == null || mEntitiesUserCandi.size() == 0)) {

					Bundle parameters = new Bundle();
					ServiceRequest serviceRequest = new ServiceRequest();

					/* Set method parameters */
					parameters.putString("userId", Aircandi.getInstance().getUser().id);
					parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
					parameters.putString("options", "object:{\"limit\":"
							+ String.valueOf(ProxiConstants.RADAR_ENTITY_LIMIT)
							+ ",\"skip\":0"
							+ ",\"sort\":{\"modifiedDate\":-1} "
							+ ",\"children\":{\"limit\":"
							+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
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

						mEntitiesUserCandi.addAll((Collection<? extends Entity>) serviceData.data);
						mEntitiesUserCandi.setCollectionType(CollectionType.CandiByUser);
						mEntitiesUserCandi.setCursorIds(serviceData.cursor);

						/* Do some fixup migrating settings to the children collection */
						for (Entity entity : mEntitiesUserCandi) {
							if (entity.children != null) {
								entity.children.setCollectionEntity(entity);
								entity.children.setCollectionType(CollectionType.CandiByUser);
								entity.children.setCursorIds(entity.childCursor); // resets cursorIndex
							}
						}

						/* Assign again since the object was replaced and we use it to pass down the results */
						serviceResponse.data = mEntitiesUserCandi;
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
					if (serviceResponse.data != null) {
						mListViewUserCandi.setAdapter(new EndlessCandiListAdapter(CandiPicker.this, mEntitiesUserCandi, R.layout.temp_listitem_candi));
						EndlessCandiListAdapter adapter = (EndlessCandiListAdapter) mListViewUserCandi.getAdapter();
						adapter.getWrappedAdapterX().getFilter().filter("candipatches");
					}
				}
				mCommon.showProgressDialog(false, null);
				mCommon.stopTitlebarProgress();
			}

		}.execute();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_ID), entity.id);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_picker;
	}
}