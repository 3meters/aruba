package com.proxibase.service;

import android.os.Bundle;

import com.proxibase.service.ProxibaseService.RequestListener;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;

/**
 * Here is the typical code to construct a service request:
 * 
 * <pre>
 * 
 * 
 * {
 * 	&#064;code
 * 	ServiceRequest serviceRequest = new ServiceRequest();
 * 	serviceRequest.setUri(mServiceUri + getCollection());
 * 	serviceRequest.setRequestType(RequestType.Insert);
 * 	serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) this, GsonType.ProxibaseService));
 * 	serviceRequest.setResponseFormat(ResponseFormat.Json);
 * 	serviceRequest.setRequestListener(listener);
 * 	NetworkManager.getInstance().requestAsync(serviceRequest);
 * }
 * </pre>
 * 
 * @author Jayma
 */

public class ServiceRequest {

	private String			mUri;
	private String			mRequestBody;
	private Bundle			mParameters;
	private Query			mQuery;
	private RequestType		mRequestType;
	private ResponseFormat	mResponseFormat;
	private RequestListener	mRequestListener;
	private boolean			mSuppressUI = false;

	public ServiceRequest() {}

	public ServiceRequest(String uri, String requestBody, Query query, RequestType requestType, ResponseFormat responseFormat,
			RequestListener requestListener) {
		this.mUri = uri;
		this.mRequestBody = requestBody;
		this.mQuery = query;
		this.mRequestType = requestType;
		this.mResponseFormat = responseFormat;
		this.mRequestListener = requestListener;
	}

	public ServiceRequest(String uri, RequestType requestType, ResponseFormat responseFormat) {
		this.mUri = uri;
		this.mRequestType = requestType;
		this.mResponseFormat = responseFormat;
	}

	public ServiceRequest(String uri, Bundle parameters, RequestType requestType, ResponseFormat responseFormat) {
		this.mUri = uri;
		this.mParameters = parameters;
		this.mRequestType = requestType;
		this.mResponseFormat = responseFormat;
	}

	public ServiceRequest(String uri, Query query, RequestType requestType, ResponseFormat responseFormat) {
		this.mUri = uri;
		this.mQuery = query;
		this.mRequestType = requestType;
		this.mResponseFormat = responseFormat;
	}

	public void setUri(String uri) {
		this.mUri = uri;
	}

	public String getUri() {
		return mUri;
	}

	public void setRequestType(RequestType requestType) {
		this.mRequestType = requestType;
	}

	public RequestType getRequestType() {
		return mRequestType;
	}

	public void setRequestListener(RequestListener requestListener) {
		this.mRequestListener = requestListener;
	}

	public RequestListener getRequestListener() {
		return mRequestListener;
	}

	public void setResponseFormat(ResponseFormat responseFormat) {
		this.mResponseFormat = responseFormat;
	}

	public ResponseFormat getResponseFormat() {
		return mResponseFormat;
	}

	public void setRequestBody(String requestBody) {
		this.mRequestBody = requestBody;
	}

	public String getRequestBody() {
		return mRequestBody;
	}

	public void setQuery(Query query) {
		this.mQuery = query;
	}

	public Query getQuery() {
		return mQuery;
	}

	public void setParameters(Bundle parameters) {
		this.mParameters = parameters;
	}

	public Bundle getParameters() {
		return mParameters;
	}

	public void setSuppressUI(boolean suppressUI) {
		this.mSuppressUI = suppressUI;
	}

	public boolean isSuppressUI() {
		return mSuppressUI;
	}
}
