package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

/**
 * Bound by default to the aircandi odata service.
 * 
 * @author Jayma
 */
public class AircandiEntity {

	private String	mServiceUri	= CandiConstants.URL_AIRCANDI_SERVICE_ODATA;

	public String getEntryUri() {
		String root = mServiceUri;
		String entity = this.getCollection();
		String uri = root + entity + "(" + this.getId() + ")";
		return uri;
	}

	public EntityProxy getEntityProxy() {
		return new EntityProxy();
	}

	protected String getId() {
		return null;
	}

	protected String getCollection() {
		return null;
	}

	/**
	 * Provide any required type notation for the id. For example, a guid
	 * must be annotated as "guid".
	 * 
	 * @return
	 */
	protected String getIdType() {
		return null;
	}

	public void setServiceUri(String serviceUri) {
		this.mServiceUri = serviceUri;
	}

	public String getServiceUri() {
		return mServiceUri;
	}

	public class Metadata {

		@Expose
		public String	type;
		@Expose
		public String	uri;
	}
}
