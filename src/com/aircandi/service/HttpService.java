// $codepro.audit.disable stringConcatenationInLoop
package com.aircandi.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;

import net.minidev.json.JSONValue;
import net.minidev.json.parser.ContainerFactory;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import org.acra.ACRA;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
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
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.graphics.Bitmap;

import com.aircandi.Aircandi;
import com.aircandi.BuildConfig;
import com.aircandi.Constants;
import com.aircandi.ServiceConstants;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ConnectedState;
import com.aircandi.components.Stopwatch;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.service.HttpServiceException.ErrorType;
import com.aircandi.service.ServiceRequest.AuthType;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.AirMarker;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Candigram;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Device;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Post;
import com.aircandi.service.objects.ServiceBase.UpdateScope;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.ServiceObject;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;
import com.aircandi.ui.AircandiForm;

/*
 * Http 1.1 Status Codes (subset)
 * 
 * - 200: OK
 * - 201: CREATED
 * - 202: Accepted
 * - 203: Non-authoritative information
 * - 204: Request fulfilled but no content returned (message body empty).
 * - 3xx: Redirection
 * - 400: Bad request. Malformed syntax.
 * - 401: Unauthorized. Request requires user authentication.
 * - 403: Forbidden
 * - 404: Not found
 * - 405: METHOD not allowed
 * - 408: Request timeout
 * - 415: Unsupported media TYPE
 * - 500: Internal server error
 * - 503: SERVICE unavailable. Caused by temporary overloading or maintenance.
 * 
 * Notes:
 * 
 * - We get a 403 from amazon when trying to fetch something from S3 that isn't there.
 */

/*
 * Timeouts
 * 
 * - Connection timeout is the max time allowed to make initial connection with the remote server.
 * - Sockettimeout is the max inactivity time allowed between two consecutive data packets.
 * - AndroidHttpClient sets BOTH to 60 seconds.
 */

/*
 * Exceptions when executing HTTP methods using HttpClient
 * 
 * - IOException: Generic transport exceptions (unreliable connection, socket timeout, generally non-fatal.
 * ClientProtocolException, SocketException and InterruptedIOException are sub classes of IOException.
 * 
 * - HttpException: Protocol exceptions. These tend to be fatal and suggest something fundamental is wrong with the
 * request such as a violation of the http protocol.
 */

/**
 * Implemented using singleton pattern. The private Constructor prevents any other class from instantiating.
 */
public class HttpService {

	private final DefaultHttpClient	mHttpClient;
	private final HttpParams		mHttpParams;

	private static class HttpServiceHolder {
		public static final HttpService	instance	= new HttpService();
	}

	public static HttpService getInstance() {
		return HttpServiceHolder.instance;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private HttpService() {

		/* Connection settings */
		mHttpParams = createHttpParams();
		mHttpClient = createHttpClient();
	}

	private HttpParams createHttpParams() {

		HttpParams params = new BasicHttpParams();
		final ConnPerRoute connPerRoute = new ConnPerRoute() {

			@Override
			public int getMaxForRoute(HttpRoute route) {
				return ServiceConstants.DEFAULT_CONNECTIONS_PER_ROUTE;
			}
		};

		/*
		 * Turn off stale checking. Our connections break all the time anyway, and
		 * it's not worth it to pay the penalty of checking every time.
		 * 
		 * Timeouts:
		 * 
		 * - setTimeout: Used when retrieving a ManagedClientConnection from the ClientConnectionManager.
		 * - setConnectionTimeout: Used trying to establish a connection to the server.
		 * - setSoTimeout: How long a socket will wait for data before throwing up.
		 */
		ConnManagerParams.setMaxTotalConnections(params, ServiceConstants.DEFAULT_MAX_CONNECTIONS);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
		ConnManagerParams.setTimeout(params, 1000);
		HttpConnectionParams.setStaleCheckingEnabled(params, false);
		HttpConnectionParams.setConnectionTimeout(params, ServiceConstants.TIMEOUT_CONNECTION);
		HttpConnectionParams.setSoTimeout(params, ServiceConstants.TIMEOUT_SOCKET_QUERIES);
		HttpConnectionParams.setTcpNoDelay(params, true);
		HttpConnectionParams.setSocketBufferSize(params, 8192);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

		params.setBooleanParameter("http.protocol.expect-continue", false);

		return params;
	}

	private DefaultHttpClient createHttpClient() {

		/*
		 * Set to BrowserCompatHostnameVerifier
		 * 
		 * AllowAllHostnameVerifier doesn't verify host names contained IN SSL certificate. It should not be set IN
		 * production environment. It may allow man IN middle attack. Other host name verifiers for specific needs
		 * are StrictHostnameVerifier and BrowserCompatHostnameVerifier.
		 */
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;

		final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);

		final SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));

		/* Create connection manager and http client */
		final ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(mHttpParams, schemeRegistry);
		DefaultHttpClient httpClient = new DefaultHttpClient(connectionManager, mHttpParams);

		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			@Override
			public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
				request.addHeader("USER-Agent", "Mozilla/5.0");
			}
		});
		return httpClient;
	}

	// ----------------------------------------------------------------------------------------
	// Public methods
	// ----------------------------------------------------------------------------------------

	public Object request(final ServiceRequest serviceRequest, final Stopwatch stopwatch) throws HttpServiceException {

		HttpRequestBase httpRequest = buildHttpRequest(serviceRequest, stopwatch);
		HttpResponse httpResponse = null;

		int tryCount = 0;
		while (true) {

			tryCount++;
			try {

				/* Always pre-flight our connection */
				ConnectedState connectedState = NetworkManager.getInstance().checkConnectedState();

				if (stopwatch != null) {
					stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": connected state check completed");
				}
				
				if (connectedState == ConnectedState.NONE) {
					final HttpServiceException proxibaseException = makeHttpServiceException(null, null, new SocketException());
					throw proxibaseException;
				}
				else if (connectedState == ConnectedState.WALLED_GARDEN) {
					final HttpServiceException proxibaseException = HttpService.makeHttpServiceException(null, null, new WalledGardenException());
					throw proxibaseException;
				}

				/* If we get to here, we have a NETWORK connection so give it a try. */
				if (tryCount > 1) {
					/*
					 * We do not RETRY if this is an update/insert/delete.
					 * 
					 * We could be retrying because of a socket timeout exception. A socket timeout exception
					 * could be caused by: 1) poor/slow connection, 2) connectivity changes like drops, or switching
					 * between networks.
					 * 
					 * We put longer and longer pauses between retries and increase the socket timeout.
					 */
					pauseExponentially(tryCount);
					final int newSocketTimeout = HttpConnectionParams.getSoTimeout(mHttpParams) + 2000;
					HttpConnectionParams.setSoTimeout(mHttpParams, newSocketTimeout);
					
					if (stopwatch != null) {
						stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": resetting socket timeout to " + String.valueOf(newSocketTimeout));
					}
				}

				long startTime = System.nanoTime();

				if (stopwatch != null) {
					stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": connection validation completed");
				}

				httpResponse = mHttpClient.execute(httpRequest);

				if (stopwatch != null) {
					stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": request execute completed");
				}

				if (isRequestSuccessful(httpResponse)) {
					/*
					 * Any 2.XX status code is considered success.
					 */
					long bytesDownloaded = (httpResponse.getEntity() != null) ? httpResponse.getEntity().getContentLength() : 0;
					logDownload(startTime, System.nanoTime() - startTime, bytesDownloaded, httpRequest.getURI().toString());
					Object response = handleResponse(httpRequest, httpResponse, serviceRequest.getResponseFormat(), serviceRequest.getRequestListener());
					
					if (stopwatch != null) {
						stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": response content captured");
					}

					/* Check for valid client version even if the call was successful */
					if (serviceRequest.getResponseFormat() == ResponseFormat.JSON && !serviceRequest.getIgnoreResponseData()) {
						/*
						 * We think anything json is coming from the Aircandi SERVICE (except Bing)
						 */
						ServiceData serviceData = (ServiceData) HttpService.jsonToObject((String) response, ObjectType.NONE, ServiceDataWrapper.TRUE);
						
						if (stopwatch != null) {
							stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": response content json (" + String.valueOf(((String)response).length()) + " bytes) decoded to object");
						}
						
						Integer clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);
						if (serviceData != null && serviceData.androidMinimumVersion != null) {
							if (serviceData.androidMinimumVersion.intValue() > clientVersionCode) {
								HttpServiceException exception = new HttpServiceException("Invalid client version", ErrorType.SERVICE,
										new HttpServiceException.ClientVersionException());
								throw exception;
							}
						}
					}

					if (stopwatch != null) {
						stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": successful response processing completed");
					}

					return response;
				}
				else if (isTemporaryRedirect(httpResponse)) {
					/*
					 * If we get a 307 Temporary Redirect, we'll point the HTTP method to the redirected location, and
					 * let the next RETRY deliver the request to the right location.
					 */
					Header[] locationHeaders = httpResponse.getHeaders("location");
					String redirectedLocation = locationHeaders[0].getValue();
					Logger.d(this, "Redirecting to: " + redirectedLocation);
					URI redirectedUri = URI.create(redirectedLocation);
					httpRequest.setURI(redirectedUri);

					if (stopwatch != null) {
						stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": starting temp redirect");
					}

				}
				else {
					/*
					 * We got a non-success http status code so break it down and
					 * decide if makes sense to RETRY.
					 */
					String responseContent = convertStreamToString(httpResponse.getEntity().getContent());

					Float httpStatusCode = (float) httpResponse.getStatusLine().getStatusCode();
					Float httpStatusCodeService = null;
					Logger.d(this, responseContent);

					if (serviceRequest.getResponseFormat() == ResponseFormat.JSON) {
						/*
						 * We think anything json is coming from the Aircandi SERVICE.
						 */
						ServiceData serviceData = (ServiceData) HttpService.jsonToObject(responseContent, ObjectType.NONE, ServiceDataWrapper.TRUE);
						Integer clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);
						if (serviceData != null) {
							if (serviceData.androidMinimumVersion != null && serviceData.androidMinimumVersion.intValue() > clientVersionCode) {
								HttpServiceException exception = new HttpServiceException("Invalid client version", ErrorType.SERVICE,
										new HttpServiceException.ClientVersionException());
								throw exception;
							}
							else if (serviceData.error != null && serviceData.error.code != null) {
								httpStatusCodeService = serviceData.error.code.floatValue();
							}
						}
					}

					HttpServiceException proxibaseException = makeHttpServiceException(httpStatusCode, httpStatusCodeService, null);

					if (!serviceRequest.okToRetry() || !shouldRetry(httpRequest, proxibaseException, tryCount)) {
						/*
						 * If we got a duplicate error code back from the SERVICE, it could be because we tried
						 * to double insert after a RETRY. In that case we want to eat the error and return success
						 * to the caller. That means we also need to return the inserted entity.
						 */
						if (stopwatch != null) {
							stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": throwing exception");
						}
						throw proxibaseException;
					}

					if (stopwatch != null) {
						stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount) + ": failure, retrying");
					}
				}
			}
			catch (IOException e) {
				/*
				 * This could be any of these:
				 * 
				 * Primaries:
				 * - UnknownHostException: hostname didn't exist IN the dns system
				 * - ConnectTimeoutException: timeout expired trying to connect to SERVICE
				 * - SocketException: thrown during socket creation or setting options
				 * - SocketTimeoutException: timeout expired on a socket waiting for data
				 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
				 * 
				 * - SSLException: Special note here. We can get this when a ssl connection has been
				 * established with the SERVICE and then the device switches networks. That causes the
				 * SERVICE to reset the connection and we get a read error.
				 * 
				 * Secondaries
				 * - Zillions
				 */

				if (!serviceRequest.okToRetry() || !shouldRetry(httpRequest, e, tryCount)) {

					final HttpServiceException proxibaseException = makeHttpServiceException(null, null, e);
					if (stopwatch != null) {
						stopwatch.segmentTime("Http SERVICE: try " + String.valueOf(tryCount + 1) + ": throwing IO exception");
					}
					throw proxibaseException;

				}
				else {
					/*
					 * Ok to RETRY, check our connection again
					 */
					ConnectedState connectedState = NetworkManager.getInstance().checkConnectedState();

					if (connectedState == ConnectedState.NONE) {
						final HttpServiceException proxibaseException = makeHttpServiceException(null, null, new SocketException());
						throw proxibaseException;
					}
					else if (connectedState == ConnectedState.WALLED_GARDEN) {
						final HttpServiceException proxibaseException = HttpService.makeHttpServiceException(null, null, new WalledGardenException());
						throw proxibaseException;
					}
				}
			}
		}
	}

	private HttpRequestBase buildHttpRequest(final ServiceRequest serviceRequest, final Stopwatch stopwatch) {

		HttpRequestBase httpRequest = null;
		StringBuilder jsonBody = new StringBuilder(5000);
		Query query = null;
		HttpConnectionParams.setSoTimeout(mHttpParams,
				(serviceRequest.getSocketTimeout() == null)
						? ServiceConstants.TIMEOUT_SOCKET_QUERIES
						: serviceRequest.getSocketTimeout());

		/* Construct the request */
		if (serviceRequest.getRequestType() == RequestType.GET) {
			httpRequest = new HttpGet();
		}
		else if (serviceRequest.getRequestType() == RequestType.INSERT) {
			httpRequest = new HttpPost();
			if (serviceRequest.getRequestBody() != null) {
				if (serviceRequest.getUseSecret()) {
					addEntity((HttpEntityEnclosingRequestBase) httpRequest, "{\"data\":" + serviceRequest.getRequestBody() + ", \"secret\":\""
							+ ServiceConstants.INSERT_USER_SECRET + "\"}");
				}
				else {
					addEntity((HttpEntityEnclosingRequestBase) httpRequest, "{\"data\":" + serviceRequest.getRequestBody() + "}");
				}
			}
		}
		else if (serviceRequest.getRequestType() == RequestType.UPDATE) {
			httpRequest = new HttpPost();
			if (serviceRequest.getRequestBody() != null) {
				addEntity((HttpEntityEnclosingRequestBase) httpRequest, "{\"data\":" + serviceRequest.getRequestBody() + "}");
			}
		}
		else if (serviceRequest.getRequestType() == RequestType.DELETE) {
			httpRequest = new HttpDelete();
		}
		else if (serviceRequest.getRequestType() == RequestType.METHOD) {
			httpRequest = new HttpPost();

			/* METHOD parameters */
			if (serviceRequest.getParameters() != null && serviceRequest.getParameters().size() != 0) {
				if (jsonBody.toString().length() == 0) {
					jsonBody.append("{");

					for (String key : serviceRequest.getParameters().keySet()) {
						if (serviceRequest.getParameters().get(key) != null) {
							if (serviceRequest.getParameters().get(key) instanceof ArrayList<?>) {

								if (key.equals("beaconLevels")) {
									List<Integer> items = serviceRequest.getParameters().getIntegerArrayList(key);
									jsonBody.append("\"" + key + "\":[");
									for (Integer beaconLevel : items) {
										jsonBody.append(String.valueOf(beaconLevel) + ",");
									}
									jsonBody = new StringBuilder(jsonBody.substring(0, jsonBody.length() - 1) + "],"); // $codepro.audit.disable stringConcatenationInLoop
								}
								else {
									List<String> items = serviceRequest.getParameters().getStringArrayList(key);
									if (items.size() == 0) {
										jsonBody.append("\"" + key + "\":[],");
									}
									else {
										jsonBody.append("\"" + key + "\":[");
										for (String itemString : items) {
											if (itemString.startsWith("object:")) {
												jsonBody.append(itemString.substring(7) + ",");
											}
											else {
												jsonBody.append("\"" + itemString + "\",");
											}
										}
										jsonBody = new StringBuilder(jsonBody.substring(0, jsonBody.length() - 1) + "],"); // $codepro.audit.disable stringConcatenationInLoop
									}
								}
							}
							else if (serviceRequest.getParameters().get(key) instanceof String) {
								String value = serviceRequest.getParameters().get(key).toString();
								if (value.startsWith("object:")) {
									jsonBody.append("\"" + key + "\":" + serviceRequest.getParameters().get(key).toString().substring(7) + ",");
								}
								else {
									jsonBody.append("\"" + key + "\":\"" + serviceRequest.getParameters().get(key).toString() + "\",");
								}
							}
							else {
								jsonBody.append("\"" + key + "\":" + serviceRequest.getParameters().get(key).toString() + ",");
							}
						}
					}
					jsonBody = new StringBuilder(jsonBody.substring(0, jsonBody.length() - 1) + "}"); // $codepro.audit.disable stringConcatenationInLoop
				}
				addEntity((HttpEntityEnclosingRequestBase) httpRequest, jsonBody.toString());
			}
		}

		/* ADD headers and set the Uri */
		addHeaders(httpRequest, serviceRequest);
		query = serviceRequest.getQuery(); // $codepro.audit.disable variableDeclaredInLoop
		String uriString = (query == null) ? serviceRequest.getUri() : serviceRequest.getUri() + query.queryString();

		/* ADD session info to uri if supplied */
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

		if (stopwatch != null) {
			stopwatch.segmentTime("Http SERVICE: request construction complete");
		}

		return httpRequest;
	}

	// ----------------------------------------------------------------------------------------
	// Primary Worker methods
	// ----------------------------------------------------------------------------------------

	private Object handleResponse(HttpRequestBase httpAction, HttpResponse response, ResponseFormat responseFormat, RequestListener listener)
			throws IOException {

		/* Looks like success so process the response */
		InputStream inputStream = null;
		BufferedInputStream bis = null;
		if (response.getEntity() != null) {

			try {
				if (responseFormat == ResponseFormat.BYTES) {
					bis = new BufferedInputStream(response.getEntity().getContent());
					final byte[] byteArray = getBytes(bis, response.getEntity().getContentLength(), listener);
					return byteArray;
				}
				else {
					BufferedHttpEntity bufferedHttpEntity = null;
					bufferedHttpEntity = new BufferedHttpEntity(response.getEntity());
					inputStream = bufferedHttpEntity.getContent();

					if (inputStream != null) {
						if (responseFormat == ResponseFormat.STREAM) {
							return inputStream;
						}
						else {
							final String contentAsString = convertStreamToString(inputStream);
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
				if (bis != null) {
					bis.close();
				}
			}
		}
		return null;
	}

	public static HttpServiceException makeHttpServiceException(Float httpStatusCode, Float httpStatusCodeService, Exception exception) {
		/*
		 * This is the only code that creates ProxibaseServiceException objects.
		 */
		HttpServiceException httpException = null;
		Float statusCode = httpStatusCodeService != null ? httpStatusCodeService : httpStatusCode;

		if (exception != null) {

			if (exception instanceof WalledGardenException) {
				httpException = new HttpServiceException("Network connects to a walled garden", ErrorType.CLIENT, exception, statusCode);
			}
			else if (exception instanceof SocketException) {
				httpException = new HttpServiceException("DEVICE is not connected to a NETWORK: "
						+ String.valueOf(ServiceConstants.CONNECT_TRIES) + " tries over "
						+ String.valueOf(ServiceConstants.CONNECT_WAIT * ServiceConstants.CONNECT_TRIES / 1000) + " second window"
						, ErrorType.CLIENT
						, exception
						, statusCode);
			}
			else {
				httpException = new HttpServiceException(exception.getClass().getSimpleName() + ": " + exception.getMessage()
						, ErrorType.CLIENT
						, exception
						, statusCode);
			}
		}
		else if (statusCode != null) {

			if (statusCode == HttpStatus.SC_NOT_FOUND) {
				httpException = new HttpServiceException("SERVICE or target not found", ErrorType.SERVICE, new HttpServiceException.NotFoundException());
			}
			else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
				/* missing, expired or invalid session */
				httpException = new HttpServiceException("Unauthorized", ErrorType.SERVICE, new HttpServiceException.UnauthorizedException());
			}
			else if (statusCode == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
				httpException = new HttpServiceException("Unauthorized credentials", ErrorType.SERVICE, new HttpServiceException.SessionException());
			}
			else if (statusCode == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
				httpException = new HttpServiceException("Expired session", ErrorType.SERVICE, new HttpServiceException.SessionException());
			}
			else if (statusCode == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_WHITELIST) {
				httpException = new HttpServiceException("Unauthorized whitelist", ErrorType.SERVICE, new HttpServiceException.UnauthorizedException());
			}
			else if (statusCode == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_UNVERIFIED) {
				httpException = new HttpServiceException("Unauthorized unverified", ErrorType.SERVICE, new HttpServiceException.UnauthorizedException());
			}
			else if (statusCode == HttpStatus.SC_FORBIDDEN) {
				/* weak password, duplicate email */
				httpException = new HttpServiceException("Forbidden", ErrorType.SERVICE, new HttpServiceException.ForbiddenException());
			}
			else if (statusCode == ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
				httpException = new HttpServiceException("Duplicate", ErrorType.SERVICE, new HttpServiceException.DuplicateException());
			}
			else if (statusCode == ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
				httpException = new HttpServiceException("Weak password", ErrorType.SERVICE, new HttpServiceException.PasswordException());
			}
			else if (statusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
				/* This can happen if SERVICE crashes during request */
				httpException = new HttpServiceException("Gateway timeout", ErrorType.SERVICE, new HttpServiceException.GatewayTimeoutException());
			}
			else if (statusCode == HttpStatus.SC_CONFLICT) {
				httpException = new HttpServiceException("Duplicate key", ErrorType.SERVICE, new HttpServiceException.DuplicateException());
			}
			else if (statusCode == HttpStatus.SC_REQUEST_TOO_LONG) {
				httpException = new HttpServiceException("Request entity too large", ErrorType.SERVICE, new HttpServiceException.AircandiServiceException());
			}
			else {
				httpException = new HttpServiceException("SERVICE error: UNKNOWN status code: " + String.valueOf(httpStatusCode)
						, ErrorType.SERVICE
						, new HttpServiceException.AircandiServiceException());
			}
			httpException.setStatusCode(statusCode);
		}
		return httpException;
	}

	private boolean isRequestSuccessful(HttpResponse response) {
		/*
		 * Any 2xx code is considered success.
		 */
		final int status = response.getStatusLine().getStatusCode();
		return status / 100 == HttpStatus.SC_OK / 100;
	}

	private boolean isTemporaryRedirect(HttpResponse response) {
		final int status = response.getStatusLine().getStatusCode();
		return status == HttpStatus.SC_TEMPORARY_REDIRECT &&
				response.getHeaders("Location") != null &&
				response.getHeaders("Location").length > 0;
	}

	private boolean shouldRetry(HttpRequestBase httpAction, Exception exception, int retries) {

		if (retries > ServiceConstants.MAX_BACKOFF_RETRIES) {
			return false;
		}

		if (httpAction instanceof HttpEntityEnclosingRequest) {
			final HttpEntity entity = ((HttpEntityEnclosingRequest) httpAction).getEntity();
			if (entity != null && !entity.isRepeatable()) {
				return false;
			}
		}

		if (exception instanceof ClientProtocolException) {
			/*
			 * Can't recover from this with a RETRY.
			 */
			return false;
		}

		if (exception instanceof NoHttpResponseException) {
			Logger.d(this, "Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
			return true;
		}

		if (exception instanceof SocketTimeoutException) {
			/*
			 * We timed OUT waiting for data. Could be a poor connection or the SERVICE could be down.
			 */
			Logger.d(this, "Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
			return true;
		}

		if (exception instanceof SocketException) {
			/*
			 * This can be caused by the server refusing the connection or resetting the connection. I've
			 * seen this when a server doesn't have the item being requested. I've also seen this IN
			 * cases where a RETRY succeeds.
			 */
			Logger.d(this, "Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
			return true;
		}

		if (exception instanceof SSLException) {
			/*
			 * This can be caused as a result of the server resetting the connection.
			 * 
			 * Special note here. We can get this when a ssl connection has been
			 * established with the SERVICE and then the device switches networks. That causes the SERVICE to reset the
			 * connection and we get a read error.
			 */
			if (exception.getMessage().toLowerCase(Locale.US).contains("connection reset")) {
				Logger.d(this, "Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
				return true;
			}
		}

		if (exception instanceof HttpServiceException) {
			final HttpServiceException proxibaseException = (HttpServiceException) exception;
			/*
			 * It is possible that a RETRY might succeed after a 500 internal SERVICE error but I think
			 * it is more conservative to just fail and let the user RETRY if they want to.
			 */
			if (proxibaseException.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				return false;
			}

			/*
			 * For 503 SERVICE unavailable errors, we want to RETRY, but we need to use
			 * an exponential back-off strategy so that we don't overload a server with a flood of retries. If we've
			 * surpassed our RETRY limit we handle the error response as a non-retryable error and go ahead and throw it
			 * back to the user as an exception.
			 * 
			 * We also RETRY 504 gateway timeout errors because this could have been
			 * caused by SERVICE crash during the request and the SERVICE will be restarted.
			 */
			if (proxibaseException.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE
					|| proxibaseException.getStatusCode() == HttpStatus.SC_GATEWAY_TIMEOUT) {
				return true;
			}
		}
		return false;
	}

	private void pauseExponentially(int retries) throws HttpClientException {
		/*
		 * Exponential sleep on failed request to avoid flooding a SERVICE with retries.
		 */
		long delay = 0;
		final long scaleFactor = 100;
		delay = (long) (Math.pow(2, retries) * scaleFactor);
		delay = Math.min(delay, ServiceConstants.MAX_BACKOFF_IN_MILLISECONDS);
		Logger.d(this, "Retryable error detected, will RETRY IN " + delay + "ms, attempt number: " + retries);

		try {
			Thread.sleep(delay);
		}
		catch (InterruptedException exception) {
			Logger.d(this, "Retry delay interrupted");
			throw makeHttpServiceException(null, null, exception);
		}
	}

	// ----------------------------------------------------------------------------------------
	// Inner helper methods
	// ----------------------------------------------------------------------------------------

	private static byte[] getBytes(InputStream inputStream, long lengthOfFile, RequestListener listener) throws IOException {

		int len;
		int size = 1024;
		byte[] buf = null;
		long total = 0;

		if (inputStream instanceof ByteArrayInputStream) {
			size = inputStream.available();
			buf = new byte[size];
			len = inputStream.read(buf, 0, size);
		}
		else {
			ByteArrayOutputStream bos = null;
			try {
				bos = new ByteArrayOutputStream();
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
			catch (Exception e) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace();
				}
			}
			finally {
				if (bos != null) {
					bos.close();
				}
			}
		}
		return buf;
	}

	private void addHeaders(HttpRequestBase httpAction, ServiceRequest serviceRequest) {
		if (serviceRequest.getRequestType() != RequestType.GET) {
			httpAction.addHeader("Content-Type", "application/json");
		}
		if (serviceRequest.getResponseFormat() == ResponseFormat.JSON) {
			httpAction.addHeader("ACCEPT", "application/json");
		}
		if (serviceRequest.getResponseFormat() == ResponseFormat.BYTES) {
			httpAction.addHeader("ACCEPT", "image/jpeg");
			httpAction.addHeader("ACCEPT", "image/png");
			httpAction.addHeader("ACCEPT", "image/gif");
		}
		if (serviceRequest.getAuthType() == AuthType.BASIC) {
			httpAction.addHeader("Authorization", "BASIC " + serviceRequest.getPasswordBase64());
		}
	}

	private void addEntity(HttpEntityEnclosingRequestBase httpAction, String json) throws HttpClientException {
		try {
			httpAction.setEntity(new StringEntity(json, HTTP.UTF_8));
		}
		catch (UnsupportedEncodingException exception) {
			throw new HttpClientException(exception.getMessage(), exception);
		}
	}

	private URI uriFromString(String stringUri) throws HttpClientException {
		final URI uri;
		try {
			uri = new URI(stringUri);
		}
		catch (URISyntaxException exception) {
			throw new HttpClientException(exception.getMessage(), exception);
		}
		return uri;
	}

	private static String convertStreamToString(InputStream inputStream) throws IOException {
		
		final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		final StringBuilder stringBuilder = new StringBuilder(); // $codepro.audit.disable defineInitialCapacity

		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line + System.getProperty("line.separator"));
		}
		bufferedReader.close();

		return stringBuilder.toString();
	}

	private void logDownload(long startTime, long elapsedTime, long bytesDownloaded, String subject) {
		final float downloadTimeMills = elapsedTime / 1000000;
		final float bitspersecond = ((bytesDownloaded << 3) * (1000 / downloadTimeMills)) / 1000;
		Logger.v(this, subject + ": Downloaded "
				+ String.valueOf(bytesDownloaded) + " bytes @ "
				+ String.valueOf(Math.round(bitspersecond)) + " kbps: "
				+ String.valueOf(downloadTimeMills) + "ms");
	}

	private String sessionInfo(ServiceRequest serviceRequest) {
		String sessionInfo = "";
		if (serviceRequest.getSession() != null) {
			sessionInfo = "user=" + serviceRequest.getSession().ownerId + "&";
			sessionInfo += "session=" + serviceRequest.getSession().key;
		}
		return sessionInfo;
	}

	// ----------------------------------------------------------------------------------------
	// JSON methods
	// ----------------------------------------------------------------------------------------

	public static Object jsonToObject(final String jsonString, ObjectType objectType) {
		/*
		 * Caller will get back either an array of objectType or a single objectType.
		 */
		return jsonToObject(jsonString, objectType, ServiceDataWrapper.FALSE);
	}

	public static Object jsonToObject(final String jsonString, ObjectType objectType, ServiceDataWrapper serviceDataWrapper) {
		/*
		 * serviceDataWrapper
		 * 
		 * true: Caller will get back a ServiceData object with a data property that is
		 * either an array of objectType or a single objectType.
		 * 
		 * false: Caller will get back either an array of objectType or a single objectType.
		 */
		final Object object = jsonToObjects(jsonString, objectType, serviceDataWrapper);
		if (object != null) {
			if (serviceDataWrapper == ServiceDataWrapper.FALSE) {
				if (object instanceof List) {
					final List<Object> array = (List<Object>) object;
					if (array != null && array.size() > 0) {
						return array.get(0);
					}
				}
			}
			else {
				ServiceData serviceData = (ServiceData) object;
				if (serviceData.data instanceof List) {
					final List<Object> array = (List<Object>) serviceData.data;
					if (array != null && array.size() > 0) {
						serviceData.data = array.get(0);
						return serviceData;
					}
				}
			}
		}
		return object;
	}

	public static Object jsonToObjects(final String jsonString, final ObjectType objectType, ServiceDataWrapper serviceDataWrapper) {

		/*
		 * serviceDataWrapper
		 * 
		 * true: Caller will get back a ServiceData object with a data property that is
		 * either an array of objectType or a single objectType.
		 * 
		 * false: Caller will get back either an array of objectType or a single objectType.
		 */
		try {
			List<LinkedHashMap<String, Object>> maps = null;

			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			ContainerFactory containerFactory = new ContainerFactory() {
				@Override
				public Map createObjectContainer() {
					return new LinkedHashMap();
				}

				@Override
				public List<Object> createArrayContainer() {
					return new ArrayList<Object>();
				}
			};

			Map<String, Object> rootMap = (LinkedHashMap<String, Object>) parser.parse(jsonString, containerFactory);

			if (serviceDataWrapper == ServiceDataWrapper.FALSE) {

				maps = new ArrayList<LinkedHashMap<String, Object>>();
				maps.add((LinkedHashMap<String, Object>) rootMap);
				Object object = mapsToObjects(maps, objectType, false);
				return object;
			}
			else {

				ServiceData serviceData = ServiceData.setPropertiesFromMap(new ServiceData(), (HashMap) rootMap, true);
				/*
				 * The data property of ServiceData is always an array even
				 * if the request could only expect to return a single object.
				 */
				if (serviceData.d != null) {

					/* It's the results of a bing QUERY */
					rootMap = (LinkedHashMap<String, Object>) serviceData.d;
					if (objectType == ObjectType.IMAGE_RESULT) {

						/* Array of objects */
						maps = (List<LinkedHashMap<String, Object>>) rootMap.get("results");
						final List<Object> list = new ArrayList<Object>();
						for (Map<String, Object> map : maps) {
							list.add(ImageResult.setPropertiesFromMap(new ImageResult(), (HashMap) map, true));
						}
						serviceData.data = list;
					}
				}
				else if (serviceData.data != null) {

					if (serviceData.data instanceof List) {
						/* The data property is an array of objects */
						maps = (List<LinkedHashMap<String, Object>>) serviceData.data;
					}
					else {

						/* The data property is an object and we put it IN an array */
						final Map<String, Object> map = (LinkedHashMap<String, Object>) serviceData.data;
						maps = new ArrayList<LinkedHashMap<String, Object>>();
						maps.add((LinkedHashMap<String, Object>) map);
					}
					serviceData.data = mapsToObjects(maps, objectType, true);
				}
				return serviceData;
			}

		}
		catch (ParseException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static Object mapsToObjects(List<LinkedHashMap<String, Object>> maps, final ObjectType objectType, Boolean nameMapping) {

		try {

			final List<Object> list = new ArrayList<Object>();

			/* Decode each map into an object and add to an array */
			for (Map<String, Object> map : maps) {
				if (objectType == ObjectType.SERVICE_ENTRY) {
					list.add(ServiceEntry.setPropertiesFromMap(new ServiceEntry(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.ENTITY) {
					String schema = (String) map.get("schema");
					if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
						list.add(Applink.setPropertiesFromMap(new Applink(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
						list.add(Beacon.setPropertiesFromMap(new Beacon(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						list.add(Comment.setPropertiesFromMap(new Comment(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						list.add(Place.setPropertiesFromMap(new Place(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
						list.add(Post.setPropertiesFromMap(new Post(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
						list.add(Candigram.setPropertiesFromMap(new Candigram(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
						list.add(User.setPropertiesFromMap(new User(), (HashMap) map, nameMapping));
					}
				}
				else if (objectType == ObjectType.USER) {
					list.add(User.setPropertiesFromMap(new User(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.AIR_MARKER) {
					list.add(AirMarker.setPropertiesFromMap(new AirMarker(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.SESSION) {
					list.add(Session.setPropertiesFromMap(new Session(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.BEACON) {
					list.add(Beacon.setPropertiesFromMap(new Beacon(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.PLACE) {
					list.add(Place.setPropertiesFromMap(new Place(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.APPLINK) {
					list.add(Applink.setPropertiesFromMap(new Applink(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.SHORTCUT) {
					list.add(Shortcut.setPropertiesFromMap(new Shortcut(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.RESULT) {
					list.add(CacheStamp.setPropertiesFromMap(new CacheStamp(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.POST) {
					list.add(Post.setPropertiesFromMap(new Post(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.COMMENT) {
					list.add(Comment.setPropertiesFromMap(new Comment(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.AIR_LOCATION) {
					list.add(AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.AIR_NOTIFICATION) {
					AirNotification notification = AirNotification.setPropertiesFromMap(new AirNotification(), (HashMap) map, nameMapping);
					list.add(notification);
				}
				else if (objectType == ObjectType.LINK) {
					list.add(Link.setPropertiesFromMap(new Link(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.IMAGE_RESULT) {
					list.add(ImageResult.setPropertiesFromMap(new ImageResult(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.PHOTO) {
					list.add(Photo.setPropertiesFromMap(new Photo(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.STAT) {
					list.add(Stat.setPropertiesFromMap(new Stat(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.CATEGORY) {
					list.add(Category.setPropertiesFromMap(new Category(), (HashMap) map, nameMapping));
				}
				else if (objectType == ObjectType.DEVICE) {
					list.add(Device.setPropertiesFromMap(new Device(), (HashMap) map, nameMapping));
				}
			}
			return list;
		}
		catch (ClassCastException e) {
			/*
			 * Sometimes we get back something that isn't a json object so we
			 * catch the exception, log it and keep going.
			 */
			ACRA.getErrorReporter().handleSilentException(e);
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String objectToJson(Object object) {
		return objectToJson(object, UseAnnotations.FALSE, ExcludeNulls.TRUE);
	}

	public static String objectToJson(Object object, UseAnnotations useAnnotations, ExcludeNulls excludeNulls) {
		final Map map = HttpService.objectToMap(object, useAnnotations, excludeNulls);
		String json = JSONValue.toJSONString(map);
		return json;
	}

	private static Map<String, Object> objectToMap(Object object, UseAnnotations useAnnotations, ExcludeNulls excludeNullsProposed) {
		final Map<String, Object> map = new HashMap<String, Object>();

		/*
		 * Order of precedent
		 * 1. object.updateScope: PropertyValue = exclude nulls, OBJECT = include nulls.
		 * 2. excludeNulls parameter: forces exclusion even if updateScope = OBJECT.
		 */
		Boolean excludeNulls = (excludeNullsProposed == ExcludeNulls.TRUE);
		try {
			if (((ServiceObject) object).updateScope == UpdateScope.OBJECT) {
				excludeNulls = false;
			}
		}
		catch (Exception e) {}

		Class<?> cls = object.getClass();

		try {
			while (true) {
				if (cls == null) {
					return map;
				}
				final Field[] fields = cls.getDeclaredFields();
				for (Field f : fields) {

					f.setAccessible(true); // Ensure trusted access
					/*
					 * We are only mapping public and protected fields.
					 */
					if (!Modifier.isStatic(f.getModifiers())
							&& (Modifier.isPublic(f.getModifiers()) || Modifier.isProtected(f.getModifiers()))) {

						if (useAnnotations == UseAnnotations.TRUE) {
							if (!f.isAnnotationPresent(Expose.class)) {
								continue;
							}
							else {
								Expose annotation = f.getAnnotation(Expose.class);
								if (!annotation.serialize()) {
									continue;
								}
							}
						}

						String key = f.getName();
						/*
						 * Modify the name key if annotations are active and present.
						 */
						if (useAnnotations == UseAnnotations.TRUE) {
							if (f.isAnnotationPresent(SerializedName.class)) {
								SerializedName annotation = f.getAnnotation(SerializedName.class);
								key = annotation.name();
							}
						}

						Object value = f.get(object);
						/*
						 * Only add to map if has value or meets null requirements.
						 */
						if (value != null || !excludeNulls) {
							/*
							 * Handle nested objects and arrays
							 */
							if (value instanceof ServiceObject) {
								Map childMap = HttpService.objectToMap(value, useAnnotations, excludeNullsProposed);
								map.put(key, childMap);
							}
							else if (value instanceof ArrayList) {
								List<Object> list = new ArrayList<Object>();
								for (Object obj : (ArrayList) value) {
									if (obj != null) {
										if (obj instanceof ServiceObject) {
											Map childMap = HttpService.objectToMap(obj, useAnnotations, excludeNullsProposed);
											list.add(childMap);
										}
										else {
											list.add(obj);
										}
									}
								}
								map.put(key, list);
							}
							else {
								map.put(key, value);
							}
						}
					}
				}
				cls = cls.getSuperclass();
			}
		}
		catch (IllegalArgumentException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		catch (IllegalAccessException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return map;
	}

	// ----------------------------------------------------------------------------------------
	// Classes and enums
	// ----------------------------------------------------------------------------------------

	public static enum UseAnnotations {
		TRUE,
		FALSE
	}

	public static enum ServiceDataWrapper {
		TRUE,
		FALSE
	}

	@SuppressWarnings("ucd")
	public static enum ExcludeNulls {
		TRUE,
		FALSE
	}

	public static class RequestListener {

		public void onStart() {}

		public void onError(Object response) {}

		public void onComplete() {}

		public void onComplete(Object response) {}

		public void onComplete(Object response, Photo photo, Bitmap bitmap, Boolean bitmapLocalOnly) {} // $codepro.audit.disable largeNumberOfParameters

		public void onProgressChanged(int progress) {}
	}

	public static enum ObjectType {
		ENTITY,
		BEACON,
		USER,
		SESSION,
		PHOTO,
		LINK,
		RESULT,
		IMAGE_RESULT,
		AIR_LOCATION,
		CATEGORY,
		NONE,
		STAT,
		SERVICE_ENTRY,
		APPLINK,
		SHORTCUT,
		DEVICE,
		AIR_NOTIFICATION,
		PLACE,
		POST,
		COMMENT, 
		AIR_MARKER
	}

	public static enum RequestType {
		GET, 
		INSERT, 
		UPDATE, 
		DELETE, 
		METHOD
	}

	public static enum ResponseFormat {
		JSON,
		HTML,
		STREAM,
		BYTES
	}
}