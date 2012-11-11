package com.aircandi.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import net.minidev.json.JSONValue;
import net.minidev.json.parser.ContainerFactory;
import net.minidev.json.parser.ParseException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.map.ObjectMapper;

import android.graphics.Bitmap;

import com.aircandi.Aircandi;
import com.aircandi.components.DateUtils;
import com.aircandi.components.ImageResult;
import com.aircandi.components.JsonHelper;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.service.ProxibaseServiceException.ErrorCode;
import com.aircandi.service.ProxibaseServiceException.ErrorType;
import com.aircandi.service.ServiceRequest.AuthType;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.Location;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Result;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.ServiceObject;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.Tip;
import com.aircandi.service.objects.User;
import com.aircandi.service.objects.VersionInfo;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

/*
 * Http 1.1 Status Codes (subset) - 200: OK - 201: Created - 202: Accepted - 203: Non-authoritative information - 204:
 * Request fulfilled but no content returned (message body empty). - 3xx: Redirection - 400: Bad request. Malformed
 * syntax. - 401: Unauthorized. Request requires user authentication. - 403: Forbidden - 404: Not found - 405: Method
 * not allowed - 408: Request timeout - 415: Unsupported media type - 500: Internal server error - 503: Service
 * unavailable. Caused by temporary overloading or maintenance. Notes: - We get a 403 from amazon when trying to fetch
 * something from S3 that isn't there.
 */

/*
 * Timeouts - Connection timeout is the max time allowed to make initial connection with the remove server. - Socket
 * timeout is the max inactivity time allowed between two consecutive data packets. - AndroidHttpClient sets both to 60
 * seconds.
 */

/*
 * Exceptions when executing HTTP methods using HttpClient - IOException: Generic transport exceptions (unreliable
 * connection, socket timeout, generally non-fatal. ClientProtocolException, SocketException and InterruptedIOException
 * are sub classes of IOException. - HttpException: Protocol exceptions. These tend to be fatal and suggest something
 * fundamental is wrong with the request such as a violation of the http protocol.
 */

/**
 * Implemented using singleton pattern. The private Constructor prevents any other class from instantiating.
 */
public class ProxibaseService {

	private static ProxibaseService		singletonObject;
	private DefaultHttpClient			mHttpClient;
	private static final int			MAX_BACKOFF_IN_MILLISECONDS		= 5 * 1000;
	private static final int			MAX_BACKOFF_RETRIES				= 6;
	public static final int				DEFAULT_MAX_CONNECTIONS			= 50;
	public static final int				DEFAULT_CONNECTIONS_PER_ROUTE	= 20;
	public static final ObjectMapper	objectMapper					= new ObjectMapper();
	private final HttpParams			mHttpParams;

	@SuppressWarnings("unused")
	private IdleConnectionMonitorThread	mIdleConnectionMonitorThread;

	public static synchronized ProxibaseService getInstance() {
		if (singletonObject == null) {
			singletonObject = new ProxibaseService();
		}
		return singletonObject;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private ProxibaseService() {

		/* Connection settings */
		mHttpParams = new BasicHttpParams();
		ConnPerRoute connPerRoute = new ConnPerRoute() {

			@Override
			public int getMaxForRoute(HttpRoute route) {
				return DEFAULT_CONNECTIONS_PER_ROUTE;
			}
		};

		/*
		 * Turn off stale checking. Our connections break all the time anyway, and it's not worth it to pay the penalty
		 * of checking every time.
		 */
		ConnManagerParams.setMaxTotalConnections(mHttpParams, DEFAULT_MAX_CONNECTIONS);
		ConnManagerParams.setMaxConnectionsPerRoute(mHttpParams, connPerRoute);
		ConnManagerParams.setTimeout(mHttpParams, 1000);
		HttpConnectionParams.setStaleCheckingEnabled(mHttpParams, false);
		HttpConnectionParams.setConnectionTimeout(mHttpParams, ProxiConstants.TIMEOUT_CONNECTION);
		HttpConnectionParams.setSoTimeout(mHttpParams, ProxiConstants.TIMEOUT_SOCKET_QUERIES);
		HttpConnectionParams.setTcpNoDelay(mHttpParams, true);
		HttpConnectionParams.setSocketBufferSize(mHttpParams, 8192);
		HttpProtocolParams.setVersion(mHttpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(mHttpParams, HTTP.UTF_8);
		HttpProtocolParams.setUserAgent(mHttpParams, ProxiConstants.USER_AGENT_DESKTOP);

		mHttpParams.setBooleanParameter("http.protocol.expect-continue", false);

		/* Support http and https */
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER); /* Might not work */
		schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
		HttpsURLConnection.setDefaultHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

		/* Create connection manager and http client */
		ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(mHttpParams, schemeRegistry);

		mHttpClient = new DefaultHttpClient(connectionManager, mHttpParams);

		/* Start a thread to monitor for expired or idle connections */
		// mIdleConnectionMonitorThread = new IdleConnectionMonitorThread(connectionManager);
		// mIdleConnectionMonitorThread.start();
	}

	// ----------------------------------------------------------------------------------------
	// Public methods
	// ----------------------------------------------------------------------------------------

	public Object request(final ServiceRequest serviceRequest)
			throws ProxibaseServiceException {

		HttpRequestBase httpRequest = null;
		String jsonBody = null;
		URI redirectedUri = null;
		int retryCount = 0;
		HttpConnectionParams.setSoTimeout(mHttpParams,
				serviceRequest.getSocketTimeout() == null ? ProxiConstants.TIMEOUT_SOCKET_QUERIES : serviceRequest.getSocketTimeout());

		while (true) {

			/* Construct the request */
			if (serviceRequest.getRequestType() == RequestType.Get) {
				httpRequest = new HttpGet();
			}
			else if (serviceRequest.getRequestType() == RequestType.Insert) {
				httpRequest = new HttpPost();
				if (serviceRequest.getRequestBody() != null) {
					if (serviceRequest.getUseSecret()) {
						addEntity((HttpEntityEnclosingRequestBase) httpRequest, "{\"data\":" + serviceRequest.getRequestBody() + ", \"secret\":\""
								+ ProxiConstants.INSERT_USER_SECRET + "\"}");
					}
					else {
						addEntity((HttpEntityEnclosingRequestBase) httpRequest, "{\"data\":" + serviceRequest.getRequestBody() + "}");
					}
				}
			}
			else if (serviceRequest.getRequestType() == RequestType.Update) {
				httpRequest = new HttpPost();
				if (serviceRequest.getRequestBody() != null) {
					addEntity((HttpEntityEnclosingRequestBase) httpRequest, "{\"data\":" + serviceRequest.getRequestBody() + "}");
				}
			}
			else if (serviceRequest.getRequestType() == RequestType.Delete) {
				httpRequest = new HttpDelete();
			}
			else if (serviceRequest.getRequestType() == RequestType.Method) {
				httpRequest = new HttpPost();

				/* Method parameters */
				if (serviceRequest.getParameters() != null && serviceRequest.getParameters().size() != 0) {
					if (jsonBody == null) {
						jsonBody = "{";

						for (String key : serviceRequest.getParameters().keySet()) {
							if (serviceRequest.getParameters().get(key) != null) {
								if (serviceRequest.getParameters().get(key) instanceof ArrayList<?>) {

									if (key.equals("beaconLevels")) {
										ArrayList<Integer> items = serviceRequest.getParameters().getIntegerArrayList(key);
										jsonBody += "\"" + key + "\":[";
										for (Integer beaconLevel : items) {
											jsonBody += String.valueOf(beaconLevel) + ",";
										}
										jsonBody = jsonBody.substring(0, jsonBody.length() - 1) + "],";
									}
									else {
										ArrayList<String> items = serviceRequest.getParameters().getStringArrayList(key);
										jsonBody += "\"" + key + "\":[";
										for (String beaconId : items) {
											jsonBody += "\"" + beaconId + "\",";
										}
										jsonBody = jsonBody.substring(0, jsonBody.length() - 1) + "],";
									}
								}
								else if (serviceRequest.getParameters().get(key) instanceof String) {
									String value = serviceRequest.getParameters().get(key).toString();
									if (value.startsWith("object:")) {
										jsonBody += "\"" + key + "\":" + serviceRequest.getParameters().get(key).toString().substring(7) + ",";
									}
									else {
										jsonBody += "\"" + key + "\":\"" + serviceRequest.getParameters().get(key).toString() + "\",";
									}
								}
								else {
									jsonBody += "\"" + key + "\":" + serviceRequest.getParameters().get(key).toString() + ",";
								}
							}
						}

						jsonBody = jsonBody.substring(0, jsonBody.length() - 1) + "}";
					}
					addEntity((HttpEntityEnclosingRequestBase) httpRequest, jsonBody);
				}
			}

			/* Add headers and set the Uri */
			addHeaders(httpRequest, serviceRequest);
			if (redirectedUri != null) {
				httpRequest.setURI(redirectedUri);
			}
			else {
				Query query = serviceRequest.getQuery();
				String uriString = query == null ? serviceRequest.getUri() : serviceRequest.getUri() + query.queryString();

				/* Add session info to uri if supplied */
				String sessionInfo = sessionInfo(serviceRequest);
				if (!sessionInfo.equals("")) {
					if (uriString.contains("?")) {
						uriString += "&" + sessionInfo;
					}
					else {
						uriString += "?" + sessionInfo;
					}
				}

				httpRequest.setURI(uriFromString(uriString));
			}

			HttpResponse httpResponse = null;

			try {
				if (retryCount > 0) {
					pauseExponentially(retryCount);
					/* We keep relaxing the socket timeout threshold */
					final int newSocketTimeout = HttpConnectionParams.getSoTimeout(mHttpParams) + 2000;
					HttpConnectionParams.setSoTimeout(mHttpParams, newSocketTimeout);
				}

				retryCount++;
				long startTime = System.nanoTime();
				httpResponse = mHttpClient.execute(httpRequest);
				long bytesDownloaded = httpResponse.getEntity() != null ? httpResponse.getEntity().getContentLength() : 0;
				logDownload(startTime, System.nanoTime() - startTime, bytesDownloaded, httpRequest.getURI().toString());

				/* Check the response status code and handle anything that isn't a possible valid success code. */
				if (isRequestSuccessful(httpResponse)) {
					return handleResponse(httpRequest, httpResponse, serviceRequest.getResponseFormat(), serviceRequest.getRequestListener());
				}
				else if (isTemporaryRedirect(httpResponse)) {
					/*
					 * If we get a 307 Temporary Redirect, we'll point the HTTP method to the redirected location, and
					 * let the next retry deliver the request to the right location.
					 */
					Header[] locationHeaders = httpResponse.getHeaders("location");
					String redirectedLocation = locationHeaders[0].getValue();
					Logger.d(this, "Redirecting to: " + redirectedLocation);
					redirectedUri = URI.create(redirectedLocation);
				}
				else {
					/*
					 * We got a non-success http status code so break it down and
					 * decide if makes sense to retry.
					 */
					String responseContent = convertStreamToString(httpResponse.getEntity().getContent());
					Float httpStatusCode = (float) httpResponse.getStatusLine().getStatusCode();
					Logger.d(this, responseContent);

					if (serviceRequest.getResponseFormat() == ResponseFormat.Json) {
						ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(responseContent, ServiceDataType.None);
						if (serviceData != null && serviceData.error != null && serviceData.error.code != null) {
							httpStatusCode = serviceData.error.code.floatValue();
						}
					}

					ProxibaseServiceException proxibaseException = makeProxibaseServiceException(httpStatusCode, null);
					proxibaseException.setResponseMessage(responseContent);
					if (!serviceRequest.okToRetry() || !shouldRetry(httpRequest, proxibaseException, retryCount)) {
						throw proxibaseException;
					}
				}
			}
			catch (ClientProtocolException exception) {
				/*
				 * Can't recover from this with a retry.
				 */
				ProxibaseServiceException proxibaseException = makeProxibaseServiceException(null, exception);
				throw proxibaseException;
			}
			catch (IOException exception) {
				/*
				 * This could be any of these:
				 * - ConnectTimeoutException: timeout expired trying to connect to service
				 * - SocketTimeoutException: timeout expired on a socket
				 * - SocketException: thrown during socket creation or setting options
				 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
				 * - UnknownHostException: hostname didn't exist in the dns system
				 */
				if (!serviceRequest.okToRetry() || !shouldRetry(httpRequest, exception, retryCount)) {
					ProxibaseServiceException proxibaseException = makeProxibaseServiceException(null, exception);
					throw proxibaseException;
				}
			}
		}
	}

	// ----------------------------------------------------------------------------------------
	// Primary Worker methods
	// ----------------------------------------------------------------------------------------

	private Object handleResponse(HttpRequestBase httpAction, HttpResponse response, ResponseFormat responseFormat, RequestListener listener)
			throws IOException {

		/* Looks like success so process the response */
		InputStream inputStream = null;
		if (response.getEntity() != null) {

			try {
				if (responseFormat == ResponseFormat.Bytes) {
					BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());
					byte[] byteArray = getBytes(bis, response.getEntity().getContentLength(), listener);
					return byteArray;
				}
				else {
					BufferedHttpEntity bufferedHttpEntity = null;
					bufferedHttpEntity = new BufferedHttpEntity(response.getEntity());
					inputStream = bufferedHttpEntity.getContent();

					if (inputStream != null) {
						if (responseFormat == ResponseFormat.Stream) {
							return inputStream;
						}
						else {
							String contentAsString = convertStreamToString(inputStream);
							return contentAsString;
						}
					}
				}
			}
			finally {
				response.getEntity().consumeContent();
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}
		return null;
	}

	public static ProxibaseServiceException makeProxibaseServiceException(Float httpStatusCode, Exception exception) {
		/*
		 * This is the only code that creates ProxibaseServiceException objects.
		 */
		ProxibaseServiceException proxibaseException = null;

		if (exception != null) {
			if (exception instanceof ClientProtocolException) {
				proxibaseException = new ProxibaseServiceException(exception.getClass().getSimpleName() + ": " + exception.getMessage(), ErrorType.Client,
						ErrorCode.ClientProtocolException, exception);
				proxibaseException.setResponseMessage(proxibaseException.getMessage());
			}
			else if (exception instanceof ConnectException) {
				proxibaseException = new ProxibaseServiceException("Not connected to network", ErrorType.Client,
						ErrorCode.ConnectionException, exception);
				proxibaseException.setResponseMessage("Device is not connected to a network: "
						+ String.valueOf(NetworkManager.CONNECT_TRIES) + " tries over "
						+ String.valueOf(NetworkManager.CONNECT_WAIT * NetworkManager.CONNECT_TRIES / 1000) + " second window");
			}
			else if (exception instanceof SocketException) {
				proxibaseException = new ProxibaseServiceException(exception.getClass().getSimpleName() + ": " + exception.getMessage(), ErrorType.Client,
						ErrorCode.SocketException, exception);
				proxibaseException.setResponseMessage(proxibaseException.getMessage());
			}
			else if (exception instanceof IOException) {
				proxibaseException = new ProxibaseServiceException(exception.getClass().getSimpleName() + ": " + exception.getMessage(), ErrorType.Client,
						ErrorCode.IOException, exception);
				proxibaseException.setResponseMessage(proxibaseException.getMessage());
			}
			else if (exception instanceof AmazonClientException) {
				proxibaseException = new ProxibaseServiceException(exception.getClass().getSimpleName() + ": " + exception.getMessage(), ErrorType.Client,
						ErrorCode.AmazonClientException, exception);
				proxibaseException.setResponseMessage(proxibaseException.getMessage());
			}
			else if (exception instanceof AmazonServiceException) {
				proxibaseException = new ProxibaseServiceException(exception.getClass().getSimpleName() + ": " + exception.getMessage(), ErrorType.Client,
						ErrorCode.AmazonServiceException, exception);
				proxibaseException.setResponseMessage(proxibaseException.getMessage());
			}
			else if (exception instanceof InterruptedException) {
				proxibaseException = new ProxibaseServiceException(exception.getClass().getSimpleName() + ": " + exception.getMessage(), ErrorType.Client,
						ErrorCode.InterruptedException, exception);
				proxibaseException.setResponseMessage(proxibaseException.getMessage());
			}
		}
		else if (httpStatusCode != null) {
			if (httpStatusCode == HttpStatus.SC_NOT_FOUND) {
				proxibaseException = new ProxibaseServiceException("Service or target not found");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.NotFoundException);
			}
			else if (httpStatusCode == HttpStatus.SC_UNAUTHORIZED) {
				/* missing, expired or invalid session */
				proxibaseException = new ProxibaseServiceException("Unauthorized");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.UnauthorizedException);
			}
			else if (httpStatusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
				proxibaseException = new ProxibaseServiceException("Unauthorized credentials");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.SessionException);
			}
			else if (httpStatusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
				proxibaseException = new ProxibaseServiceException("Expired session");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.SessionException);
			}
			else if (httpStatusCode == HttpStatus.SC_FORBIDDEN) {
				/* weak password, duplicate email */
				proxibaseException = new ProxibaseServiceException("Forbidden");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.ForbiddenException);
			}
			else if (httpStatusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_EMAIL_NOT_UNIQUE) {
				proxibaseException = new ProxibaseServiceException("Duplicate email");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.DuplicateException);
			}
			else if (httpStatusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
				proxibaseException = new ProxibaseServiceException("Weak password");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.PasswordException);
			}
			else if (httpStatusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
				/* This can happen if service crashes during request */
				proxibaseException = new ProxibaseServiceException("Gateway timeout");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.GatewayTimeoutException);
			}
			else if (httpStatusCode == HttpStatus.SC_CONFLICT) {
				proxibaseException = new ProxibaseServiceException("Duplicate key");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.UpdateException);
			}
			else if (httpStatusCode == HttpStatus.SC_REQUEST_TOO_LONG) {
				proxibaseException = new ProxibaseServiceException("Request entity too large");
				proxibaseException.setErrorType(ErrorType.Client);
				proxibaseException.setErrorCode(ErrorCode.AircandiServiceException);
			}
			else {
				proxibaseException = new ProxibaseServiceException("Service error");
				proxibaseException.setErrorType(ErrorType.Service);
				proxibaseException.setErrorCode(ErrorCode.AircandiServiceException);
			}
			proxibaseException.setHttpStatusCode(httpStatusCode);
		}
		return proxibaseException;
	}

	private boolean isRequestSuccessful(HttpResponse response) {
		/*
		 * Any 2xx code is considered success.
		 */
		int status = response.getStatusLine().getStatusCode();
		return status / 100 == HttpStatus.SC_OK / 100;
	}

	private boolean isTemporaryRedirect(HttpResponse response) {
		int status = response.getStatusLine().getStatusCode();
		return status == HttpStatus.SC_TEMPORARY_REDIRECT &&
				response.getHeaders("Location") != null &&
				response.getHeaders("Location").length > 0;
	}

	public boolean shouldRetry(HttpRequestBase httpAction, Exception exception, int retries) {

		if (retries > MAX_BACKOFF_RETRIES) {
			return false;
		}

		if (httpAction instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) httpAction).getEntity();
			if (entity != null && !entity.isRepeatable()) {
				return false;
			}
		}

		if (exception instanceof NoHttpResponseException
				|| exception instanceof SocketTimeoutException) {
			Logger.d(this, "Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
			return true;
		}

		if (exception instanceof SocketException) {
			/*
			 * This can be caused by the server refusing the connection or resetting the connection. I've
			 * seen this when a server doesn't have the item being requested. I've also seen this in
			 * cases where a retry succeeds.
			 */
			Logger.d(this, "Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
			return true;
		}

		if (exception instanceof ProxibaseServiceException) {
			ProxibaseServiceException proxibaseException = (ProxibaseServiceException) exception;

			/*
			 * For 500 internal server errors and 503 service unavailable errors, we want to retry, but we need to use
			 * an exponential back-off strategy so that we don't overload a server with a flood of retries. If we've
			 * surpassed our retry limit we handle the error response as a non-retryable error and go ahead and throw it
			 * back to the user as an exception. We also retry 504 gateway timeout errors because this could have been
			 * caused by service crash during the request and the service will be restarted.
			 */
			if (proxibaseException.getHttpStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR
					|| proxibaseException.getHttpStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE
					|| proxibaseException.getHttpStatusCode() == HttpStatus.SC_GATEWAY_TIMEOUT) {
				return true;
			}
		}
		return false;
	}

	public long getDelay(int retries) {
		/*
		 * Exponential sleep on failed request to avoid flooding a service with retries.
		 */
		long delay = 0;
		long scaleFactor = 100;
		delay = (long) (Math.pow(2, retries) * scaleFactor);
		delay = Math.min(delay, MAX_BACKOFF_IN_MILLISECONDS);
		return delay;
	}

	private void pauseExponentially(int retries) throws ProxibaseClientException {
		/*
		 * Exponential sleep on failed request to avoid flooding a service with retries.
		 */
		long delay = 0;
		long scaleFactor = 100;
		delay = (long) (Math.pow(2, retries) * scaleFactor);
		delay = Math.min(delay, MAX_BACKOFF_IN_MILLISECONDS);
		Logger.d(this, "Retryable error detected, will retry in " + delay + "ms, attempt number: " + retries);

		try {
			Thread.sleep(delay);
		}
		catch (InterruptedException exception) {
			Logger.d(this, "Retry delay interrupted");
			throw makeProxibaseServiceException(null, exception);
		}
	}

	// ----------------------------------------------------------------------------------------
	// Inner helper methods
	// ----------------------------------------------------------------------------------------

	public static byte[] getBytes(InputStream inputStream, long lengthOfFile, RequestListener listener) throws IOException {

		int len;
		int size = 1024;
		byte[] buf;
		long total = 0;

		if (inputStream instanceof ByteArrayInputStream) {
			size = inputStream.available();
			buf = new byte[size];
			len = inputStream.read(buf, 0, size);
		}
		else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = inputStream.read(buf, 0, size)) != -1) {
				total += len;
				bos.write(buf, 0, len);
				if (listener != null) {
					listener.onProgressChanged(((int) (total * 100 / lengthOfFile)));
				}
			}
			buf = bos.toByteArray();
		}
		return buf;
	}

	private void addHeaders(HttpRequestBase httpAction, ServiceRequest serviceRequest) {
		if (serviceRequest.getRequestType() != RequestType.Get) {
			httpAction.addHeader("Content-Type", "application/json");
		}
		if (serviceRequest.getResponseFormat() == ResponseFormat.Json) {
			httpAction.addHeader("Accept", "application/json");
		}
		if (serviceRequest.getAuthType() == AuthType.Basic) {
			httpAction.addHeader("Authorization", "Basic " + serviceRequest.getPasswordBase64());
		}
	}

	private void addEntity(HttpEntityEnclosingRequestBase httpAction, String json) throws ProxibaseClientException {
		try {
			httpAction.setEntity(new StringEntity(json, HTTP.UTF_8));
		}
		catch (UnsupportedEncodingException exception) {
			throw new ProxibaseClientException(exception.getMessage(), exception);
		}
	}

	private URI uriFromString(String stringUri) throws ProxibaseClientException {
		URI uri;
		try {
			uri = new URI(stringUri);
		}
		catch (URISyntaxException exception) {
			throw new ProxibaseClientException(exception.getMessage(), exception);
		}
		return uri;
	}

	public static String convertStreamToString(InputStream inputStream) throws IOException {

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder stringBuilder = new StringBuilder();

		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line + "\n");
		}
		return stringBuilder.toString();
	}

	private void logDownload(long startTime, long elapsedTime, long bytesDownloaded, String subject) {
		float downloadTimeMills = elapsedTime / 1000000;
		float bitspersecond = ((bytesDownloaded * 8) * (1000 / downloadTimeMills)) / 1000;
		Logger.v(this, subject + ": Downloaded "
				+ String.valueOf(bytesDownloaded) + " bytes @ "
				+ String.valueOf(Math.round(bitspersecond)) + " kbps: "
				+ String.valueOf(downloadTimeMills) + "ms");
	}

	@SuppressWarnings("deprecation")
	public String generateId(int schemaId, Long timeUtc) {

		Date dateUtc = new Date(timeUtc);
		Date midnightUtc = new Date(dateUtc.getYear(), dateUtc.getMonth(), dateUtc.getDate());
		Integer secondsUtc = DateUtils.intervalInSeconds(midnightUtc, dateUtc);
		Integer rand = (int) Math.floor(Math.random() * 1000000);

		String id = String.format("%1$04d.%2$ty%2$tm%2$td.%3$05d.%2$tL.%4$06d", schemaId, timeUtc, secondsUtc, rand);
		return id;
	}

	public String sessionInfo(ServiceRequest serviceRequest) {
		String sessionInfo = "";
		if (serviceRequest.getSession() != null) {
			sessionInfo = "user=" + serviceRequest.getSession().ownerId + "&";
			sessionInfo += "session=" + serviceRequest.getSession().key;
		}
		return sessionInfo;
	}

	// ----------------------------------------------------------------------------------------
	// Json methods
	// ----------------------------------------------------------------------------------------

	public static Object convertJsonToObjectInternalSmart(String jsonString, ServiceDataType serviceDataType) {
		ContainerFactory containerFactory = new ContainerFactory() {
			public Map createObjectContainer() {
				return new LinkedHashMap();
			}

			@Override
			public List<Object> createArrayContainer() {
				return new ArrayList<Object>();
			}
		};

		try {
			LinkedHashMap<String, Object> rootMap = (LinkedHashMap<String, Object>) Aircandi.parser.parse(jsonString, containerFactory);
			if (serviceDataType == ServiceDataType.User) {
				return User.setPropertiesFromMap(new User(), rootMap);
			}
			else if (serviceDataType == ServiceDataType.Session) {
				return Session.setPropertiesFromMap(new Session(), rootMap);
			}
			else if (serviceDataType == ServiceDataType.Location) {
				return Location.setPropertiesFromMap(new Location(), rootMap);
			}
			else if (serviceDataType == ServiceDataType.Category) {
				return Category.setPropertiesFromMap(new Category(), rootMap);
			}
			else if (serviceDataType == ServiceDataType.GeoLocation) {
				return GeoLocation.setPropertiesFromMap(new GeoLocation(), rootMap);
			}
			else if (serviceDataType == ServiceDataType.Entity) {
				return Entity.setPropertiesFromMap(new Entity(), rootMap);
			}
			else {
				return rootMap;
			}
		}
		catch (ParseException exception) {
			exception.printStackTrace();
		}
		return null;
	}

	public static ServiceData convertJsonToObjectSmart(String jsonString, ServiceDataType serviceDataType) {

		ServiceData serviceData = convertJsonToObjectsSmart(jsonString, serviceDataType);
		if (serviceData.data != null) {
			if (serviceData.data instanceof List) {
				List<Object> array = (List<Object>) serviceData.data;
				if (array != null && array.size() > 0) {
					serviceData.data = array.get(0);
				}
			}
		}
		return serviceData;
	}

	public static ServiceData convertJsonToObjectsSmart(String jsonString, ServiceDataType serviceDataType) {

		Boolean methodTimingOnly = false;
		if (!Aircandi.stopwatch.isStarted()) {
			methodTimingOnly = true;
			Aircandi.stopwatch.start();
		}
		Aircandi.stopwatch.segmentTime("Simple data binding start");
		ContainerFactory containerFactory = new ContainerFactory() {
			public Map createObjectContainer() {
				return new LinkedHashMap();
			}

			@Override
			public List<Object> createArrayContainer() {
				return new ArrayList<Object>();
			}
		};

		try {
			LinkedHashMap<String, Object> rootMap = (LinkedHashMap<String, Object>) Aircandi.parser.parse(jsonString, containerFactory);
			ServiceData serviceData = ServiceData.setPropertiesFromMap(new ServiceData(), rootMap);

			/*
			 * The data property of ServiceData is always an array even
			 * if the request could only expect to return a single object.
			 */
			if (serviceData.d != null) {
				/* It's a bing query */
				rootMap = (LinkedHashMap<String, Object>) serviceData.d;
				if (serviceDataType == ServiceDataType.ImageResult) {
					List<LinkedHashMap<String, Object>> maps = (List<LinkedHashMap<String, Object>>) rootMap.get("results");
					List<Object> list = new ArrayList<Object>();
					for (LinkedHashMap<String, Object> map : maps) {
						list.add(ImageResult.setPropertiesFromMap(new ImageResult(), map));
					}
					serviceData.data = list;
				}
			}
			else if (serviceData.data != null) {
				if (serviceDataType == ServiceDataType.Result) {
					serviceData.data = Result.setPropertiesFromMap(new Result(), (HashMap) serviceData.data);
				}
				else {

					List<LinkedHashMap<String, Object>> maps = (List<LinkedHashMap<String, Object>>) serviceData.data;
					List<Object> list = new ArrayList<Object>();
					for (LinkedHashMap<String, Object> map : maps) {
						if (serviceDataType == ServiceDataType.Entity) {
							list.add(Entity.setPropertiesFromMap(new Entity(), map));
						}
						else if (serviceDataType == ServiceDataType.Beacon) {
							list.add(Beacon.setPropertiesFromMap(new Beacon(), map));
						}
						else if (serviceDataType == ServiceDataType.User) {
							list.add(User.setPropertiesFromMap(new User(), map));
						}
						else if (serviceDataType == ServiceDataType.VersionInfo) {
							list.add(VersionInfo.setPropertiesFromMap(new VersionInfo(), map));
						}
						else if (serviceDataType == ServiceDataType.ImageResult) {
							list.add(ImageResult.setPropertiesFromMap(new ImageResult(), map));
						}
						else if (serviceDataType == ServiceDataType.Photo) {
							list.add(Photo.setPropertiesFromMap(new Photo(), map));
						}
						else if (serviceDataType == ServiceDataType.Tip) {
							list.add(Tip.setPropertiesFromMap(new Tip(), map));
						}
						else if (serviceDataType == ServiceDataType.Category) {
							list.add(Category.setPropertiesFromMap(new Category(), map));
						}
					}
					serviceData.data = list;
				}
			}
			Aircandi.stopwatch.segmentTime("Simple data binding complete");
			if (methodTimingOnly) {
				Aircandi.stopwatch.stop();
			}
			return serviceData;
		}
		catch (ParseException exception) {
			exception.printStackTrace();
		}
		Aircandi.stopwatch.segmentTime("Simple data binding complete");
		if (methodTimingOnly) {
			Aircandi.stopwatch.stop();
		}
		return null;
	}

	public static ServiceData convertJsonToObjectNative(String jsonString, ServiceDataType serviceDataType) {
		try {
			ServiceData serviceData = new ServiceData();
			org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);

			if (serviceDataType == ServiceDataType.Entity || serviceDataType == ServiceDataType.Beacon) {
				List<HashMap<String, Object>> maps = (List<HashMap<String, Object>>) JsonHelper.toList(jsonObject.getJSONArray("data"));;
				if (serviceDataType == ServiceDataType.Entity) {
					List<Entity> entities = new ArrayList<Entity>();
					for (HashMap<String, Object> entityMap : maps) {
						entities.add(Entity.setPropertiesFromMap(new Entity(), entityMap));
					}
					serviceData.data = entities;
				}
				else if (serviceDataType == ServiceDataType.Beacon) {
					List<Beacon> beacons = new ArrayList<Beacon>();
					for (HashMap<String, Object> beaconMap : maps) {
						beacons.add(Beacon.setPropertiesFromMap(new Beacon(), beaconMap));
					}
					serviceData.data = beacons;
				}
			}
			Aircandi.stopwatch.segmentTime("Simple data binding complete");
			return serviceData;
		}
		catch (org.json.JSONException exception) {
			exception.printStackTrace();
		}
		Aircandi.stopwatch.segmentTime("Simple data binding complete");
		return null;
	}

	public static String convertObjectToJsonSmart(Object object, Boolean useAnnotations, Boolean excludeNulls) {
		String json = null;
		if (object instanceof ServiceEntry) {
			HashMap map = ((ServiceEntry) object).getHashMap(useAnnotations, excludeNulls);
			json = JSONValue.toJSONString(map);
		}
		else if (object instanceof ServiceObject) {
			HashMap map = ((ServiceObject) object).getHashMap(useAnnotations, excludeNulls);
			json = JSONValue.toJSONString(map);
		}

		return json;
	}

	// ----------------------------------------------------------------------------------------
	// Inner classes and enums
	// ----------------------------------------------------------------------------------------

	public static class IdleConnectionMonitorThread extends Thread {

		private final ClientConnectionManager	connMgr;
		private volatile boolean				shutdown;

		public IdleConnectionMonitorThread(ClientConnectionManager connMgr) {
			super();
			this.connMgr = connMgr;
		}

		@Override
		public void run() {
			try {
				while (!shutdown) {
					synchronized (this) {
						wait(5000);
						// Close expired connections
						connMgr.closeExpiredConnections();
						// Optionally, close connections
						// that have been idle longer than 30 sec
						connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
					}
				}
			}
			catch (InterruptedException ex) {
				// terminate
			}
		}

		public void shutdown() {
			shutdown = true;
			synchronized (this) {
				notifyAll();
			}
		}
	}

	public static class RequestListener {

		public void onComplete(Object response) {}

		public void onComplete(Object response, Object extra) {}

		public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {}

		public Bitmap onFilter(Bitmap bitmap) {
			return bitmap;
		}

		public void onProgressChanged(int progress) {}
	}

	public static enum ServiceDataType {
		Entity,
		Beacon,
		User,
		Session,
		Photo,
		Tip,
		VersionInfo,
		Result,
		ImageResult,
		GeoLocation,
		Category,
		None, Location,
	}

	public static enum RequestType {
		Get, Insert, Update, Delete, Method
	}

	public static enum GsonType {
		Internal,
		ProxibaseService,
		ProxibaseServiceNew,
		BingService
	}

	public static enum ResponseFormat {
		Json,
		Xml,
		Html,
		Stream,
		Bytes
	}

	public static enum UrlEncodingType {
		All,
		SpacesOnly,
		None
	}

	public static enum UriConfig {
		DomainAndFilePath,
		FilePath
	}
}