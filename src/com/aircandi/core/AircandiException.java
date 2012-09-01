package com.aircandi.core;

/**
 * Encapsulation of all exceptions and errors thrown by aircandi
 */
public class AircandiException extends Throwable {

	private static final long	serialVersionUID	= 1L;

	private CandiErrorCode		mErrorCode;
	private Exception			mInnerException;

	public AircandiException(String message, CandiErrorCode code) {
		this(message, code, null);
	}

	public AircandiException(String message, CandiErrorCode code, Exception exception) {
		super(message);
		mErrorCode = code;
		mInnerException = exception;
	}

	/**
	 * NetworkError code can be used as an umbrella for IOException and ClientProtocolException.
	 * OperationFailed is a bucket for any service request that did not complete successfully based
	 * on the nature of the request.
	 */
	public CandiErrorCode getErrorCode() {
		return mErrorCode;
	}

	public Exception getInnerException() {
		return mInnerException;
	}

	public static enum CandiErrorCode {
		WifiDisabled, WifiEnabling, NetworkError, IOException, URISyntaxException, ClientProtocolException, UnsupportedEncodingException, IllegalStateException, OperationFailed, AmazonServiceException, AmazonClientException, FileNotFoundException, RejectedExecutionException
	}
}
