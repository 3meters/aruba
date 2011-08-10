package com.proxibase.aircandi.models;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;

import com.google.gson.annotations.Expose;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.service.ProxibaseError;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.SimpleModifyListener;

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
		String uri = root + entity + "(" + this.getIdType() + "'" + this.getId() + "')";
		return uri;
	}

	public void insertAsync(SimpleModifyListener modifyListener) {
		ProxibaseService.getInstance().insertAsync(this, getCollection(), mServiceUri, modifyListener);
	}

	public void updateAsync(SimpleModifyListener modifyListener) {
		ProxibaseService.getInstance().updateAsync(this, this.getEntryUri(), modifyListener);
	}

	public void deleteAsync(SimpleModifyListener modifyListener) {
		ProxibaseService.getInstance().deleteAsync(this.getEntryUri(), modifyListener);
	}

	public String insert() {
		try {
			String response = ProxibaseService.getInstance().insert(this, getCollection(), mServiceUri);
			return response;
		}
		catch (ClientProtocolException exception) {
			exception.printStackTrace();
		}
		catch (URISyntaxException exception) {
			exception.printStackTrace();
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
		catch (ProxibaseError exception) {
			exception.printStackTrace();
		}
		return "";
	}

	public boolean update() {
		try {
			ProxibaseService.getInstance().update(this, this.getEntryUri());
			return true;
		}
		catch (ClientProtocolException exception) {
			exception.printStackTrace();
		}
		catch (URISyntaxException exception) {
			exception.printStackTrace();
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
		catch (ProxibaseError exception) {
			exception.printStackTrace();
		}
		return false;
	}

	public boolean delete() {
		try {
			ProxibaseService.getInstance().delete(this.getEntryUri());
			return true;
		}
		catch (ClientProtocolException exception) {
			exception.printStackTrace();
		}
		catch (URISyntaxException exception) {
			exception.printStackTrace();
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
		catch (ProxibaseError exception) {
			exception.printStackTrace();
		}
		return false;
	}

	public EntityProxy getEntityProxy() {
		return new EntityProxy();
	}

	protected String getId() {
		return "";
	}

	protected String getCollection() {
		return "";
	}

	/**
	 * Provide any required type notation for the id. For example, a guid
	 * must be annotated as "guid".
	 * 
	 * @return
	 */
	protected String getIdType() {
		return "";
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
