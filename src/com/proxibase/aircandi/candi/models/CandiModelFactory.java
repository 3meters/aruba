package com.proxibase.aircandi.candi.models;

import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class CandiModelFactory {

	public static CandiModel newCandiModel(int modelId, EntityProxy entity, CandiPatchModel candiPatchModel) {
		/*
		 * The job of the builder is to handle any external initialization required 
		 * by the component at creation time.
		 */
		final CandiModel candiModel = new CandiModel(modelId, candiPatchModel);
		if (entity != null)
		{
			candiModel.setEntityProxy(entity);
			candiModel.setTitleText(entity.label);
			candiModel.setBodyImageUri(entity.imageUri);
			candiModel.setBodyImageFormat(entity.imageFormat.equals("html") ? ImageFormat.Html : ImageFormat.Binary);
		}
		
		return candiModel;
	}
}
