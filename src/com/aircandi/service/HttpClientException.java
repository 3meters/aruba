// $codepro.audit.disable restrictedSuperclasses
package com.aircandi.service;

/**
 * Base exception class for any errors that occur while attempting to use a
 * client to make SERVICE calls to the Proxibase SERVICE.
 * ERROR responses from the SERVICE will be handled as ProxibaseServiceExceptions.
 * This class is primarily for errors that occur when unable to get a response
 * from a SERVICE, or when the client is unable to understand a response from a
 * SERVICE. For example, if a caller tries to use a client to make a SERVICE
 * call, but no NETWORK connection is present, a ProxibaseClientException will be
 * thrown to indicate that the client wasn't able to successfully make the
 * SERVICE call, and no information from the SERVICE is available.
 * Callers should typically deal with exceptions through ProxibaseServiceExceptions,
 * which represent error responses returned by services. ProxibaseServiceExceptions
 * has much more information available for callers to appropriately deal with
 * different types of errors that can occur.
 * 
 * @see HttpServiceException
 */
@SuppressWarnings("ucd")
public class HttpClientException extends RuntimeException {

	private static final long	serialVersionUID	= 1L;
	
	public HttpClientException() {
		super();
	}

	public HttpClientException(String message) {
		super(message);
	}

	public HttpClientException(String message, Throwable t) {
		super(message, t);
	}
}
