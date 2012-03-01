package com.proxibase.sdk.android.proxi.service;

/**
 * Extension of ProxibaseClientException that represents an error response returned
 * by the Proxibase web service. Receiving an exception of this type indicates that
 * the caller's request was correctly transmitted to the service, but for some
 * reason, the service was not able to process it, and returned an error
 * response instead.
 * <p>
 * ProxibaseServiceException provides callers several pieces of information that can be used to obtain more information
 * about the error and why it occurred. In particular, the errorType field can be used to determine if the caller's
 * request was invalid, or the service encountered an error on the server side while processing it.
 */
public class ProxibaseServiceException extends ProxibaseClientException {

	private static final long	serialVersionUID	= 1L;
	private ErrorCode			mErrorCode			= ErrorCode.AircandiServiceException;
	private ErrorType			mErrorType			= ErrorType.Unknown;
	private int					mHttpStatusCode;

	public ProxibaseServiceException(String message) {
		this(message, ErrorType.Unknown, ErrorCode.AircandiServiceException);
	}

	public ProxibaseServiceException(String message, ErrorType errorType, ErrorCode errorCode) {
		this(message, errorType, errorCode, null);
	}

	public ProxibaseServiceException(String message, ErrorType errorType, ErrorCode errorCode, Exception exceptionForCause) {
		super(message, exceptionForCause);
		mErrorCode = errorCode;
		mErrorType = errorType;
	}
	
	public void setErrorCode(ErrorCode errorCode) {
		this.mErrorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return mErrorCode;
	}

	public void setErrorType(ErrorType errorType) {
		this.mErrorType = errorType;
	}

	public ErrorType getErrorType() {
		return mErrorType;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.mHttpStatusCode = httpStatusCode;
	}

	public int getHttpStatusCode() {
		return mHttpStatusCode;
	}

	/**
	 * Returns a string summary of the details of this exception including the
	 * HTTP status code, Proxi request ID, Proxi error code and error message.
	 * 
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {
		return "Http Status Code: " + getHttpStatusCode() + ", "
				+ "Proxi Error Code: " + getErrorCode() + ", "
				+ "Proxi Error Message: " + getMessage();
	}

	public static enum ErrorCode {
		URISyntaxException,
		IOException,
		ClientProtocolException,
		ConnectionException,
		UnsupportedEncodingException,
		IllegalStateException,
		AircandiServiceException,
		AmazonServiceException,
		AmazonClientException,
		NotFoundException,
		UpdateException,
		RejectedExecutionException,
		InterruptedException,
		UnknownHostException,
		UnknownException
	}

	/**
	 * Indicates who is responsible (if known) for a failed request.
	 * <p>
	 * For example, if a client is using an invalid Proxibase access key, the returned exception will indicate that
	 * there is an error in the request the caller is sending. Retrying that same request will *not* result in a
	 * successful response. The Client ErrorType indicates that there is a problem in the request the user is sending
	 * (ex: incorrect access keys, invalid parameter value, missing parameter, etc.), and that the caller must take some
	 * action to correct the request before it should be resent. Client errors are typically associated an HTTP error
	 * code in the 4xx range.
	 * <p>
	 * The Service ErrorType indicates that although the request the caller sent was valid, the service was unable to
	 * fulfill the request because of problems on the service's side. These types of errors can be retried by the caller
	 * since the caller's request was valid and the problem occurred while processing the request on the service side.
	 * Service errors will be accompanied by an HTTP error code in the 5xx range.
	 * <p>
	 * Finally, if there isn't enough information to determine who's fault the error response is, an Unknown ErrorType
	 * will be set.
	 */
	public static enum ErrorType {
			Client,
			Service,
			Unknown
	}

}
