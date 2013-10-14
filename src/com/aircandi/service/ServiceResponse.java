package com.aircandi.service;

import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.utilities.Errors.ErrorResponse;

public class ServiceResponse {

	public Object			data;
	public ResponseCode		responseCode	= ResponseCode.SUCCESS;
	public Integer			statusCode;
	public Float			statusCodeService;
	public String			statusMessage;
	public String			contentType = "none";
	public String			contentEncoding = "none";
	public Long				contentLength = 0L;
	public Long				contentLengthScaled = 0L;
	public Integer			contentHeight = 0;
	public Integer			contentWidth = 0;
	public Integer			contentHeightScaled = 0;
	public Integer			contentWidthScaled = 0;
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