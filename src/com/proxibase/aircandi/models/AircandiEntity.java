package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

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

	public void insertAsync(IQueryListener listener) {
		ProxibaseService.getInstance().insertAsync(this, getCollection(), mServiceUri, listener);
	}

	public void updateAsync(IQueryListener listener) {
		ProxibaseService.getInstance().updateAsync(this, this.getEntryUri(), listener);
	}

	public void deleteAsync(IQueryListener listener) {
		ProxibaseService.getInstance().deleteAsync(this.getEntryUri(), listener);
	}

	public String insert() {
		try {
			String response = ProxibaseService.getInstance().insert(this, getCollection(), mServiceUri);
			return response;
		}
		catch (ProxibaseException exception) {
			Exceptions.Handle(exception);
		}
		return null;
	}

	public boolean update() {
		try {
			ProxibaseService.getInstance().update(this, this.getEntryUri());
			return true;
		}
		catch (ProxibaseException exception) {
			Exceptions.Handle(exception);
		}
		return false;
	}

	public boolean delete() {
		try {
			ProxibaseService.getInstance().delete(this.getEntryUri());
			return true;
		}
		catch (ProxibaseException exception) {
			Exceptions.Handle(exception);
		}
		return false;
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
