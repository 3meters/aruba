package com.proxibase.aircandi.candi.models;

import com.proxibase.sdk.android.proxi.consumer.ProxiEntity;

public class CandiModelBuilder {

	public static CandiModel createCandiModel(ProxiEntity proxiEntity) {
		/*
		 * The job of the builder is to handle any external initialization required 
		 * by the component at creation time.
		 */
		final CandiModel candiModel = new CandiModel(proxiEntity);
		return candiModel;
	}
}
