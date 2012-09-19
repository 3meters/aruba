package com.aircandi.service;

import org.apache.commons.codec.binary.Base64;

import android.os.Bundle;

import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.objects.Session;

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
 * 	serviceRequest.setRequestBody(ProxibaseService.convertObjectToJsonSmart(this, true));
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
	private Session			mSession;
	private String			mUserName;
	private String			mPassword;
	private AuthType		mAuthType	= AuthType.None;
	private boolean			mSuppressUI	= false;

	public enum AuthType {
		None,
		Basic,
		OAuth
	}

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

	public Session getSession() {
		return mSession;
	}

	public ServiceRequest setSession(Session session) {
		mSession = session;
		return this;
	}

	public String getUserName() {
		return mUserName;
	}

	public ServiceRequest setUserName(String userName) {
		mUserName = userName;
		return this;
	}

	public String getPassword() {
		return mPassword;
	}

	public String getPasswordBase64() {
		byte[] accountKeyBytes = Base64.encodeBase64((mPassword + ":" + mPassword).getBytes());
		String accountKeyEnc = new String(accountKeyBytes);
		return accountKeyEnc;
	}
	
	public ServiceRequest setPassword(String password) {
		mPassword = password;
		return this;
	}

	public AuthType getAuthType() {
		return mAuthType;
	}

	public ServiceRequest setAuthType(AuthType authType) {
		mAuthType = authType;
		return this;
	}
}
