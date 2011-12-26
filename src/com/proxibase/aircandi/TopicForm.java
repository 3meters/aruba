package com.proxibase.aircandi;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.TopicEntity;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.NetworkManager.ResponseCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class TopicForm extends EntityBaseForm {

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			TopicEntity entity = new TopicEntity();
			entity.entityType = CandiConstants.TYPE_CANDI_TOPIC;
			entity.parentEntityId = null;
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
				mEntity = (TopicEntity) ProxibaseService.convertJsonToObject(jsonResponse, TopicEntity.class, GsonType.ProxibaseService);
				GoogleAnalyticsTracker.getInstance().dispatch();
			}
		}
		super.bindEntity();
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		if (findViewById(R.id.chk_locked) != null) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(((TopicEntity) mEntity).locked);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave(boolean updateImages) {
		super.doSave(true);
	}

	@Override
	protected void gather() {
		/*
		 * Handle properties that are not part of the base entity
		 */
		final TopicEntity entity = (TopicEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.gather();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.topic_form;
	}
}