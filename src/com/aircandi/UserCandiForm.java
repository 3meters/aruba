package com.aircandi;

import java.util.Collections;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.CommandType;
import com.aircandi.components.EntityList;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.R;

public class UserCandiForm extends CandiForm {

	public void bind(Boolean useProxiExplorer) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		if (useProxiExplorer) {
			/*
			 * Entity is coming from entity model.
			 */
			mEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mParentId, EntityTree.User);
			mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
			mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
			mEntityModelUser = Aircandi.getInstance().getUser();
			
			mCommon.mActionBar.setTitle(mEntity.title);

			/* Was likely deleted from the entity model */
			if (mEntity == null) {
				onBackPressed();
			}
			else {
				/* Get the view pager configured */
				updateViewPager();
			}
		}
		else {
			/*
			 * Entity is coming from service.
			 */
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, getString(R.string.progress_loading));
				}

				@Override
				protected Object doInBackground(Object... params) {
					String jsonFields = "{\"entities\":{},\"children\":{},\"parents\":{},\"comments\":{}}";
					String jsonEagerLoad = "{\"children\":true,\"parents\":true,\"comments\":false}";
					ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntity(mCommon.mEntityId, jsonEagerLoad, jsonFields, null);
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object result) {
					ServiceResponse serviceResponse = (ServiceResponse) result;

					if (serviceResponse.responseCode == ResponseCode.Success) {
						mEntity = (Entity) ((ServiceData) serviceResponse.data).data;

						/* Sort the children if there are any */
						if (mEntity.children != null && mEntity.children.size() > 1) {
							Collections.sort(mEntity.children, new EntityList.SortEntitiesByModifiedDate());
						}

						/* Get the view pager configured */
						updateViewPager();

						mCommon.showProgressDialog(false, null);
					}
					else {
						mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiForm);
					}
				}

			}.execute();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChildrenButtonClick(View v) {
		IntentBuilder intentBuilder = new IntentBuilder(this, UserCandiList.class);

		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setEntityId(mEntity.id);
		intentBuilder.setParentEntityId(mEntity.parentId);
		intentBuilder.setCollectionId(mCommon.mEntityId);
		intentBuilder.setEntityTree(mCommon.mEntityTree);

		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiFormToCandiList);
	}

}