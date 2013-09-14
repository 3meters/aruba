package com.aircandi.service;

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
@SuppressWarnings("ucd")
public class HttpServiceException extends HttpClientException {

	private static final long	serialVersionUID	= 1L;
	private ErrorType			mErrorType			= ErrorType.UNKNOWN;
	private Float				mStatusCode;
	private Exception			mInnerException;

	public HttpServiceException(String message) {
		this(message, ErrorType.UNKNOWN);
	}

	public HttpServiceException(String message, ErrorType errorType) {
		this(message, errorType, null);
	}

	public HttpServiceException(String message, ErrorType errorType, Exception exceptionForCause) {
		super(message, exceptionForCause);
		mInnerException = exceptionForCause;
		mErrorType = errorType;
	}

	public HttpServiceException(String message, ErrorType errorType, Exception exceptionForCause, Float statusCode) {
		super(message, exceptionForCause);
		mInnerException = exceptionForCause;
		mErrorType = errorType;
		mStatusCode = statusCode;
	}

	public void setErrorType(ErrorType errorType) {
		mErrorType = errorType;
	}

	public ErrorType getErrorType() {
		return mErrorType;
	}

	public void setStatusCode(float statusCode) {
		mStatusCode = statusCode;
	}

	public Float getStatusCode() {
		return mStatusCode;
	}

	public Exception getInnerException() {
		return mInnerException;
	}

	/**
	 * Indicates who is responsible (if known) for a failed request.
	 * <p>
	 * For example, if a client is using an invalid Proxibase access key, the returned exception will indicate that
	 * there is an error in the request the caller is sending. Retrying that same request will *not* result in a
	 * successful response. The client ErrorType indicates that there is a problem in the request the user is sending
	 * (ex: incorrect access keys, invalid parameter value, missing parameter, etc.), and that the caller must take some
	 * action to correct the request before it should be resent. client errors are typically associated an HTTP error
	 * code in the 4xx RANGE.
	 * <p>
	 * The service ErrorType indicates that although the request the caller sent was valid, the service was unable to
	 * fulfill the request because of problems on the service's side. These types of errors can be retried by the caller
	 * since the caller's request was valid and the problem occurred while processing the request on the service side.
	 * service errors will be accompanied by an HTTP error code in the 5xx RANGE.
	 * <p>
	 * Finally, if there isn't enough information to determine who's fault the error response is, an Unknown ErrorType
	 * will be set.
	 */
	public static enum ErrorType {
		CLIENT,
		SERVICE,
		UNKNOWN
	}

	public static class NotFoundException extends RuntimeException {
		private static final long	serialVersionUID	= 4547766119289288452L;
	}

	public static class UnauthorizedException extends RuntimeException {
		private static final long	serialVersionUID	= -6925534025164860340L;
	}

	public static class SessionException extends RuntimeException {
		private static final long	serialVersionUID	= -127555462863603660L;
	}

	public static class ForbiddenException extends RuntimeException {
		private static final long	serialVersionUID	= -1916399848421176931L;
	}

	public static class DuplicateException extends RuntimeException {
		private static final long	serialVersionUID	= -4233932252195294079L;
	}

	public static class PasswordException extends RuntimeException {
		private static final long	serialVersionUID	= 3558926785824409259L;
	}

	public static class GatewayTimeoutException extends RuntimeException {
		private static final long	serialVersionUID	= -8228623746732379029L;
	}

	public static class AircandiServiceException extends RuntimeException {
		private static final long	serialVersionUID	= -713258658623534414L;
	}
	
	public static class ClientVersionException extends RuntimeException {
		private static final long	serialVersionUID	= -713258658623539999L;
	}
}
