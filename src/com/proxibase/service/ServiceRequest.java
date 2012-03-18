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

	public ServiceRequest setUri(String uri) {
		this.mUri = uri;
		return this;
	}

	public String getUri() {
		return mUri;
	}

	public ServiceRequest setRequestType(RequestType requestType) {
		this.mRequestType = requestType;
		return this;
	}

	public RequestType getRequestType() {
		return mRequestType;
	}

	public ServiceRequest setRequestListener(RequestListener requestListener) {
		this.mRequestListener = requestListener;
		return this;
	}

	public RequestListener getRequestListener() {
		return mRequestListener;
	}

	public ServiceRequest setResponseFormat(ResponseFormat responseFormat) {
		this.mResponseFormat = responseFormat;
		return this;
	}

	public ResponseFormat getResponseFormat() {
		return mResponseFormat;
	}

	public ServiceRequest setRequestBody(String requestBody) {
		this.mRequestBody = requestBody;
		return this;
	}

	public String getRequestBody() {
		return mRequestBody;
	}

	public ServiceRequest setQuery(Query query) {
		this.mQuery = query;
		return this;
	}

	public Query getQuery() {
		return mQuery;
	}

	public ServiceRequest setParameters(Bundle parameters) {
		this.mParameters = parameters;
		return this;
	}

	public Bundle getParameters() {
		return mParameters;
	}

	public ServiceRequest setSuppressUI(boolean suppressUI) {
		this.mSuppressUI = suppressUI;
		return this;
	}

	public boolean isSuppressUI() {
		return mSuppressUI;
	}
}
