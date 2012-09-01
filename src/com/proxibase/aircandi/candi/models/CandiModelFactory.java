package com.proxibase.aircandi.candi.models;

import com.aircandi.service.objects.Entity;

public class CandiModelFactory {

	public static CandiModel newCandiModel(String modelId, Entity entity, CandiPatchModel candiPatchModel) {
		/*
		 * The job of the builder is to handle any external initialization required
		 * by the component at creation time.
		 */
		final CandiModel candiModel = new CandiModel(modelId, candiPatchModel);
		if (entity != null) {
			candiModel.setEntity(entity);
			candiModel.setTitleText(entity.label);
		}

		return candiModel;
	}
}
