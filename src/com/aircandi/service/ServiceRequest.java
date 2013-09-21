package com.aircandi.service;

import org.apache.commons.codec.binary.Base64;

import android.os.Bundle;
import android.text.TextUtils;

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
 * 	serviceRequest.setRequestType(RequestType.INSERT);
 * 	serviceRequest.setRequestBody(ProxibaseService.convertObjectToJsonSmart(this, true));
 * 	serviceRequest.setResponseFormat(ResponseFormat.JSON);
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
	private String			mActivityName;
	private Bundle			mParameters;
	private RequestType		mRequestType;
	private ResponseFormat	mResponseFormat;
	private RequestListener	mRequestListener;
	private Session			mSession;
	private String			mUserName;
	private String			mPassword;
	private AuthType		mAuthType			= AuthType.NONE;
	private Boolean			mUseSecret			= false;
	private Boolean			mIgnoreResponseData	= false;
	private boolean			mSuppressUI			= false;

	@SuppressWarnings("ucd")
	public enum AuthType {
		NONE,
		BASIC,
		OAUTH
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

	public String getUriWithQuery() {
		String uri = mUri;
		String sessionInfo = sessionInfo();
		if (!TextUtils.isEmpty(sessionInfo)) {
			if (uri.contains("?")) {
				uri += "&" + sessionInfo;
			}
			else {
				uri += "?" + sessionInfo;
			}
		}
		return uri;
	}

	private String sessionInfo() {
		String sessionInfo = "";
		if (mSession != null) {
			sessionInfo = "user=" + mSession.ownerId + "&";
			sessionInfo += "session=" + mSession.key;
		}
		return sessionInfo;
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

	public Boolean getUseSecret() {
		return mUseSecret;
	}

	public ServiceRequest setUseSecret(Boolean useSecret) {
		mUseSecret = useSecret;
		return this;
	}

	public Boolean getIgnoreResponseData() {
		return mIgnoreResponseData;
	}

	public ServiceRequest setIgnoreResponseData(Boolean ignoreResponseData) {
		mIgnoreResponseData = ignoreResponseData;
		return this;
	}

	public String getActivityName() {
		return mActivityName;
	}

	public ServiceRequest setActivityName(String activityName) {
		mActivityName = activityName;
		return this;
	}
}
