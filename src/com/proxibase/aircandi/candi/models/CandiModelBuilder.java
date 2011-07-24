package com.proxibase.aircandi.candi.models;

import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class CandiModelBuilder {

	public static CandiModel createCandiModel(EntityProxy entityProxy) {
		/*
		 * The job of the builder is to handle any external initialization required 
		 * by the component at creation time.
		 */
		final CandiModel candiModel = new CandiModel(entityProxy);
		return candiModel;
	}
}
