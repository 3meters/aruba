package com.aircandi.service;

import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.utilities.Errors.ErrorResponse;

public class ServiceResponse {

	public Object			data;
	public ResponseCode		responseCode	= ResponseCode.SUCCESS;
	public Integer			statusCode;
	public Float			statusCodeService;
	public String			statusMessage;
	public ErrorResponse	errorResponse;
	public String			activityName;

	public Exception		exception;

	public ServiceResponse() {}

	public ServiceResponse(ResponseCode resultCode, Object data, Exception exception) {
		this.responseCode = resultCode;
		this.data = data;
		this.exception = exception;
	}
}