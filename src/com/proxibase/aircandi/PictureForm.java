package com.proxibase.aircandi;

import android.app.Activity;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PictureEntity;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.NetworkManager.ResponseCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class PictureForm extends EntityBaseForm {

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			PictureEntity entity = new PictureEntity();
			entity.entityType = CandiConstants.TYPE_CANDI_PICTURE;
			if (mParentEntityId != 0) {
				entity.parentEntityId = mParentEntityId;
			}
			else {
				entity.parentEntityId = null;
			}
			entity.imageUri = (String) mImagePicture.getTag();
			entity.imageFormat = ImageFormat.Binary.name().toLowerCase();
			mEntity = entity;
		}
		else if (mCommand.verb.equals("edit")) {
			ServiceResponse serviceResponse = NetworkManager.getInstance().request(
					new ServiceRequest(mEntityProxy.getEntryUri(), RequestType.Get, ResponseFormat.Json));

			if (serviceResponse.responseCode != ResponseCode.Success) {
				setResult(Activity.RESULT_CANCELED);
				finish();
				overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
			}
			else {
				String jsonResponse = (String) serviceResponse.data;
				mEntity = (PictureEntity) ProxibaseService.convertJsonToObject(jsonResponse, PictureEntity.class, GsonType.ProxibaseService);
				GoogleAnalyticsTracker.getInstance().dispatch();
			}
		}
		super.bindEntity();
	}
	
	@Override
	protected void gather() {
		final PictureEntity entity = (PictureEntity) mEntity;
		entity.mediaUri = entity.imageUri;
		entity.mediaFormat = entity.imageFormat;
		super.gather();
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave(boolean updateImages) {
		super.doSave(true);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.picture_form;
	}
}