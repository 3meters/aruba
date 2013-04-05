package com.aircandi.test;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.http.client.ClientProtocolException;

import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpServiceException;

public class TestUtils {

	/**
	 * Generates a ProxibaseServiceException for a non-success case.
	 * 
	 * @param httpStatusCode
	 *            <ul>
	 *            <li>HttpStatus.SC_NOT_FOUND
	 *            <li>HttpStatus.SC_UNAUTHORIZED
	 *            <li>HttpStatus.SC_FORBIDDEN
	 *            <li>HttpStatus.SC_GATEWAY_TIMEOUT
	 *            <li>HttpStatus.SC_CONFLICT
	 *            <li>HttpStatus.SC_REQUEST_TOO_LONG
	 *            <li>ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS
	 *            <li>ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED
	 *            <li>ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_EMAIL_NOT_UNIQUE
	 *            <li>ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK
	 *            </ul>
	 * @param type
	 *            Exception class
	 *            <ul>
	 *            <li>ClientProtocolException.class
	 *            <li>IOException.class
	 *            <li>ConnectException.class
	 *            </ul>
	 * @return
	 */

	public static ServiceResponse getFailureServiceResponse(Float httpStatusCode, Class type) {
		Exception exception = null;
		if (type.equals(ClientProtocolException.class)) {
			exception = new ClientProtocolException();
		}
		else if (type.equals(IOException.class)) {
			exception = new IOException();
		}
		else if (type.equals(ConnectException.class)) {
			exception = new ConnectException();
		}

		final HttpServiceException proxibaseException = HttpService.makeHttpServiceException(httpStatusCode, null, exception);
		final ServiceResponse serviceResponse = new ServiceResponse(ResponseCode.Failed, null, proxibaseException);
		return serviceResponse;
	}
}
