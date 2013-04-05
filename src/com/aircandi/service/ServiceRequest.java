package com.aircandi.service;

import org.apache.commons.codec.binary.Base64;

import android.os.Bundle;

import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
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

@SuppressWarnings("ucd")
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
	private Integer			mSocketTimeout;
	private Boolean			mRetry		= true;
	private Boolean			mUseSecret	= false;
	private boolean			mSuppressUI	= false;

	@SuppressWarnings("ucd")
	public enum AuthType {
		None,
		Basic,
		OAuth
	}

	public ServiceRequest() {}

	public ServiceRequest(String uri, RequestType requestType, ResponseFormat responseFormat) {
		mUri = uri;
		mRequestType = requestType;
		mResponseFormat = responseFormat;
	}

	public ServiceRequest setUri(String uri) {
		mUri = uri;
		return this;
	}

	public String getUri() {
		return mUri;
	}

	public ServiceRequest setRequestType(RequestType requestType) {
		mRequestType = requestType;
		return this;
	}

	public RequestType getRequestType() {
		return mRequestType;
	}

	public ServiceRequest setRequestListener(RequestListener requestListener) {
		mRequestListener = requestListener;
		return this;
	}

	public RequestListener getRequestListener() {
		return mRequestListener;
	}

	public ServiceRequest setResponseFormat(ResponseFormat responseFormat) {
		mResponseFormat = responseFormat;
		return this;
	}

	public ResponseFormat getResponseFormat() {
		return mResponseFormat;
	}

	public ServiceRequest setRequestBody(String requestBody) {
		mRequestBody = requestBody;
		return this;
	}

	public String getRequestBody() {
		return mRequestBody;
	}

	public ServiceRequest setQuery(Query query) {
		mQuery = query;
		return this;
	}

	public Query getQuery() {
		return mQuery;
	}

	public ServiceRequest setParameters(Bundle parameters) {
		mParameters = parameters;
		return this;
	}

	public Bundle getParameters() {
		return mParameters;
	}

	public ServiceRequest setSuppressUI(boolean suppressUI) {
		mSuppressUI = suppressUI;
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
		final byte[] accountKeyBytes = Base64.encodeBase64((mPassword + ":" + mPassword).getBytes());
		final String accountKeyEnc = new String(accountKeyBytes);
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

	public Integer getSocketTimeout() {
		return mSocketTimeout;
	}

	public ServiceRequest setSocketTimeout(int socketTimeout) {
		mSocketTimeout = socketTimeout;
		return this;
	}

	public Boolean okToRetry() {
		return mRetry;
	}

	public ServiceRequest setRetry(Boolean retry) {
		mRetry = retry;
		return this;
	}

	public Boolean getUseSecret() {
		return mUseSecret;
	}

	public ServiceRequest setUseSecret(Boolean useSecret) {
		mUseSecret = useSecret;
		return this;
	}
}
