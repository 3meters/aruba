package com.proxibase.aircandi.candi.models;

import com.proxibase.aircandi.candi.models.BaseModel.ModelType;
import com.proxibase.aircandi.utils.ImageManager.ImageFormat;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class CandiModelFactory {

	public static CandiModel newCandiModel(ModelType modelType, int modelId, EntityProxy entity) {
		/*
		 * The job of the builder is to handle any external initialization required 
		 * by the component at creation time.
		 */
		final CandiModel candiModel = new CandiModel(modelType, modelId);
		if (entity != null)
		{
			candiModel.setEntityProxy(entity);
			candiModel.setTitleText(entity.label);
			candiModel.setBodyImageId(entity.imageUri);
			candiModel.setBodyImageUri(entity.imageUri);
			candiModel.setBodyImageFormat(entity.imageFormat.equals("html") ? ImageFormat.Html : ImageFormat.Binary);
		}
		
		return candiModel;
	}
}
