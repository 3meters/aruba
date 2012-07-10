package com.proxibase.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.service.ProxibaseServiceException.ErrorCode;
import com.proxibase.service.ProxibaseServiceException.ErrorType;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.Session;
import com.proxibase.service.objects.User;

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
	private int							mTimeoutSocket					= ProxiConstants.TIMEOUT_SOCKET;
	private int							mTimeoutConnection				= ProxiConstants.TIMEOUT_CONNECTION;
	private static final int			MAX_BACKOFF_IN_MILLISECONDS		= 5 * 1000;
	private static final int			MAX_BACKOFF_RETRIES				= 6;
	public static final int				DEFAULT_MAX_CONNECTIONS			= 50;
	public static final int				DEFAULT_CONNECTIONS_PER_ROUTE	= 20;
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
		mTimeoutConnection = 5000;
		mTimeoutSocket = 3000;
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
		HttpConnectionParams.setConnectionTimeout(mHttpParams, mTimeoutConnection);
		HttpConnectionParams.setSoTimeout(mHttpParams, mTimeoutSocket);
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
		HttpConnectionParams.setSoTimeout(mHttpParams, 3000);

		while (true) {

			/* Construct the request */
			if (serviceRequest.getRequestType() == RequestType.Get) {
				httpRequest = new HttpGet();
			}
			else if (serviceRequest.getRequestType() == RequestType.Insert) {
				httpRequest = new HttpPost();
				if (serviceRequest.getRequestBody() != null) {
					addEntity((HttpEntityEnclosingRequestBase) httpRequest, "{\"data\":" + serviceRequest.getRequestBody() + "}");
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
									ArrayList<String> items = serviceRequest.getParameters().getStringArrayList(key);
									jsonBody += "\"" + key + "\":[";
									for (String beaconId : items) {
										jsonBody += "\"" + beaconId + "\",";
									}
									jsonBody = jsonBody.substring(0, jsonBody.length() - 1) + "],";
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
			addHeaders(httpRequest, serviceRequest.getResponseFormat());
			if (redirectedUri != null) {
				httpRequest.setURI(redirectedUri);
			}
			else {
				Query query = serviceRequest.getQuery();
				String uriString = query == null ? serviceRequest.getUri() : serviceRequest.getUri() + query.queryString();
				
				/* Add session info to uri if supplied */
				String sessionInfo = sessionInfo(serviceRequest);
				if (!sessionInfo.equals("")){
					if (uriString.contains("?")) {
						uriString += "&" + sessionInfo;
					}
					else {
						uriString += "?" + sessionInfo;
					}
				}
				
				httpRequest.setURI(uriFromString(uriString));
			}

			HttpResponse response = null;

			try {
				if (retryCount > 0) {
					pauseExponentially(retryCount);
					/* We keep relaxing the socket timeout threshold */
					final int newSocketTimeout = HttpConnectionParams.getSoTimeout(mHttpParams) + 2000;
					HttpConnectionParams.setSoTimeout(mHttpParams, newSocketTimeout);
				}

				retryCount++;
				long startTime = System.nanoTime();
				response = mHttpClient.execute(httpRequest);
				long bytesDownloaded = response.getEntity() != null ? response.getEntity().getContentLength() : 0;
				logDownload(startTime, System.nanoTime() - startTime, bytesDownloaded, httpRequest.getURI().toString());

				/* Check the response status code and handle anything that isn't a possible valid success code. */
				if (isRequestSuccessful(response)) {
					return handleResponse(httpRequest, response, serviceRequest.getResponseFormat(), serviceRequest.getRequestListener());
				}
				else if (isTemporaryRedirect(response)) {
					/*
					 * If we get a 307 Temporary Redirect, we'll point the HTTP method to the redirected location, and
					 * let the next retry deliver the request to the right location.
					 */
					Header[] locationHeaders = response.getHeaders("location");
					String redirectedLocation = locationHeaders[0].getValue();
					Logger.d(this, "Redirecting to: " + redirectedLocation);
					redirectedUri = URI.create(redirectedLocation);
				}
				else {
					ProxibaseServiceException exception = handleErrorResponse(response);
					String errorResponse = "Service Response: " + convertStreamToString(response.getEntity().getContent());
					Logger.d(this, errorResponse);
					exception.setResponseMessage(errorResponse);
					if (!shouldRetry(httpRequest, exception, retryCount)) {
						throw exception;
					}
				}
			}
			catch (ClientProtocolException exception) {
				/* Can't recover from this with a retry. */
				String message = "Unable to execute Http request: ClientProtocolException: " + exception.getMessage();
				Logger.w(this, message);
				ProxibaseServiceException proxibaseException = new ProxibaseServiceException(message, ErrorType.Client, ErrorCode.ClientProtocolException,
						exception);
				proxibaseException.setResponseMessage(message);
				throw proxibaseException;
			}
			catch (IOException exception) {
				/*
				 * This could be any of these: - ConnectTimeoutException: timeout expired trying to connect to service -
				 * SocketTimeoutException: timeout expired on a socket - SocketException: thrown during socket creation
				 * or setting options - NoHttpResponseException: target server failed to respond with a valid HTTP
				 * response - UnknownHostException: hostname didn't exist in the dns system
				 */
				if (!shouldRetry(httpRequest, exception, retryCount)) {
					String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
					Logger.w(this, message);
					ProxibaseServiceException proxibaseException = new ProxibaseServiceException(message, ErrorType.Client, ErrorCode.IOException, exception);
					proxibaseException.setResponseMessage(message);
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

	private ProxibaseServiceException handleErrorResponse(HttpResponse response) {

		ProxibaseServiceException exception = null;
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == HttpStatus.SC_NOT_FOUND) {
			exception = new ProxibaseServiceException("Service or target not found");
			exception.setErrorType(ErrorType.Service);
			exception.setErrorCode(ErrorCode.NotFoundException);
			exception.setHttpStatusCode(statusCode);
		}
		if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			exception = new ProxibaseServiceException("Service not found");
			exception.setErrorType(ErrorType.Service);
			exception.setErrorCode(ErrorCode.UnauthorizedException);
			exception.setHttpStatusCode(statusCode);
		}
		else if (statusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
			/*
			 * This can happen if service crashes during request
			 */
			exception = new ProxibaseServiceException("Gateway timeout");
			exception.setErrorType(ErrorType.Service);
			exception.setErrorCode(ErrorCode.AircandiServiceException);
			exception.setHttpStatusCode(statusCode);
		}
		else if (statusCode == HttpStatus.SC_CONFLICT) {
			exception = new ProxibaseServiceException("Duplicate key");
			exception.setErrorType(ErrorType.Service);
			exception.setErrorCode(ErrorCode.UpdateException);
			exception.setHttpStatusCode(statusCode);
		}
		else if (statusCode == HttpStatus.SC_REQUEST_TOO_LONG) {
			exception = new ProxibaseServiceException("Request entity too large");
			exception.setErrorType(ErrorType.Client);
			exception.setErrorCode(ErrorCode.AircandiServiceException);
			exception.setHttpStatusCode(statusCode);
		}
		else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_CUSTOM_SESSION_EXPIRED) {
			exception = new ProxibaseServiceException("Session expired");
			exception.setErrorType(ErrorType.Service);
			exception.setErrorCode(ErrorCode.SessionException);
			exception.setHttpStatusCode(statusCode);
		}
		else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_CUSTOM_PASSWORD_STRENGTH) {
			exception = new ProxibaseServiceException("Weak password");
			exception.setErrorType(ErrorType.Client);
			exception.setErrorCode(ErrorCode.PasswordException);
			exception.setHttpStatusCode(statusCode);
		}
		else {
			exception = new ProxibaseServiceException("Service error");
			exception.setErrorType(ErrorType.Service);
			exception.setErrorCode(ErrorCode.AircandiServiceException);
			exception.setHttpStatusCode(statusCode);
		}
		return exception;
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
				|| exception instanceof SocketException
				|| exception instanceof SocketTimeoutException) {
			Logger.d(this, "Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
			return true;
		}

		if (exception instanceof ProxibaseServiceException) {
			ProxibaseServiceException pse = (ProxibaseServiceException) exception;

			/*
			 * For 500 internal server errors and 503 service unavailable errors, we want to retry, but we need to use
			 * an exponential back-off strategy so that we don't overload a server with a flood of retries. If we've
			 * surpassed our retry limit we handle the error response as a non-retryable error and go ahead and throw it
			 * back to the user as an exception. We also retry 504 gateway timeout errors because this could have been
			 * caused by service crash during the request and the service will be restarted.
			 */
			if (pse.getHttpStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR
					|| pse.getHttpStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE
					|| pse.getHttpStatusCode() == HttpStatus.SC_GATEWAY_TIMEOUT) {
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
			throw new ProxibaseServiceException("Retry delay interrupted");
		}
	}

	// ----------------------------------------------------------------------------------------
	// Setters/Getters
	// ----------------------------------------------------------------------------------------

	public void setTimeoutSocket(int timeoutSocket) {
		this.mTimeoutSocket = timeoutSocket;
	}

	public void setTimeoutConnection(int timeoutConnection) {
		this.mTimeoutConnection = timeoutConnection;
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

	private void addHeaders(HttpRequestBase httpAction, ResponseFormat responseFormat) {
		httpAction.addHeader("Content-Type", "application/json");
		if (responseFormat == ResponseFormat.Json) {
			httpAction.addHeader("Accept", "application/json");
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

	private static String convertStreamToString(InputStream inputStream) throws IOException {

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder stringBuilder = new StringBuilder();

		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line + "\n");
		}
		return stringBuilder.toString();
	}

	public boolean isConnectedToNetwork(Context context) {

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) {
			return false;
		}

		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo == null) {
			return false;
		}

		if (networkInfo.isAvailable() && networkInfo.isConnectedOrConnecting()) {
			return true;
		}

		return false;
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
			sessionInfo = "user=" + serviceRequest.getSession().id + "&";
			sessionInfo += "session=" + serviceRequest.getSession().key;
		}
		return sessionInfo;
	}
	// ----------------------------------------------------------------------------------------
	// Json methods
	// ----------------------------------------------------------------------------------------

	public static String convertObjectToJson(Object object, GsonType gsonType) {
		Gson gson = ProxibaseService.getGson(gsonType);
		String json = gson.toJson(object);
		return json;
	}

	public static ServiceData convertJsonToObject(String jsonString, Class type, GsonType gsonType) {
		ServiceData serviceData = convertJsonToObjects(jsonString, type, gsonType);
		if (serviceData.data != null) {
			List<Object> array = (List<Object>) serviceData.data;
			if (array != null && array.size() > 0) {
				serviceData.data = array.get(0);
			}
		}
		return serviceData;
	}

	public static ServiceData convertJsonToObjects(String jsonString, Class type, GsonType gsonType) {

		Gson gson = ProxibaseService.getGson(gsonType);
		ServiceData serviceData = new ServiceData();

		/*
		 * In general, gson deserializer will ignore elements (fields or classes) in the string that do not exist on the
		 * object type. Collections should be treated as generic lists on the target object.
		 */
		try {

			JsonParser parser = new JsonParser();
			JsonElement jsonElement = parser.parse(jsonString);
			if (jsonElement.isJsonObject()) {
				JsonObject jsonObject = jsonElement.getAsJsonObject();

				if (jsonObject.has("data")) {
					jsonElement = jsonObject.get("data");
					List<Object> array = new ArrayList<Object>();
					if (jsonElement.isJsonPrimitive()) {
						JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
						if (primitive.isString()) {
							array.add(primitive.getAsString());
						}
						else if (primitive.isNumber()) {
							array.add(primitive.getAsNumber());
						}
						else if (primitive.isBoolean()) {
							array.add(primitive.getAsBoolean());
						}
					}
					else if (jsonElement.isJsonArray()) {
						JsonArray jsonArray = jsonElement.getAsJsonArray();
						for (int i = 0; i < jsonArray.size(); i++) {
							JsonObject jsonObjectNew = (JsonObject) jsonArray.get(i);
							array.add(gson.fromJson(jsonObjectNew.toString(), type));
						}
					}
					else if (jsonElement.isJsonObject()) {
						Object obj = gson.fromJson(jsonElement.toString(), type);
						array.add(obj);
					}
					serviceData.data = array;
				}

				if (jsonObject.has("cursor")) {
					JsonArray jsonArray = jsonObject.get("cursor").getAsJsonArray();
					List<String> cursorIds = new ArrayList<String>();
					for (int i = 0; i < jsonArray.size(); i++) {
						cursorIds.add(jsonArray.get(i).getAsString());
					}
					serviceData.cursor = cursorIds;
				}
				if (jsonObject.has("date")) {
					serviceData.date = jsonObject.get("date").getAsJsonPrimitive().getAsNumber();
				}
				if (jsonObject.has("user")) {
					User user = gson.fromJson(jsonObject.get("user").toString(), User.class);
					serviceData.user = user;
				}
				if (jsonObject.has("session")) {
					Session session = gson.fromJson(jsonObject.get("session").toString(), Session.class);
					serviceData.session = session;
				}
				if (jsonObject.has("count")) {
					serviceData.count = jsonObject.get("count").getAsJsonPrimitive().getAsNumber();
				}
				if (jsonObject.has("info")) {
					serviceData.info = jsonObject.get("info").getAsJsonPrimitive().getAsString();
				}
				if (jsonObject.has("more")) {
					serviceData.more = jsonObject.get("more").getAsJsonPrimitive().getAsBoolean();
				}
				if (jsonObject.has("time")) {
					serviceData.time = jsonObject.get("time").getAsJsonPrimitive().getAsNumber();
				}
			}

		}
		catch (JsonParseException exception) {
			Logger.e(singletonObject, "convertJsonToObjects: " + exception.getMessage());
		}
		catch (IllegalStateException exception) {
			Logger.e(singletonObject, "convertJsonToObjects: " + exception.getMessage());
		}
		catch (Exception exception) {
			Logger.e(singletonObject, "convertJsonToObjects: " + exception.getMessage());
		}
		return serviceData;
	}

	public static Gson getGson(GsonType gsonType) {
		GsonBuilder gsonb = new GsonBuilder();

		/*
		 * Converting objects to/from json for passing between the client and the service we need to apply some
		 * additional behavior on top of the defaults
		 */
		if (gsonType == GsonType.ProxibaseService) {
			gsonb.excludeFieldsWithoutExposeAnnotation();
			gsonb.setPrettyPrinting(); /* TODO: remove this later */
		}
		Gson gson = gsonb.create();
		return gson;
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

		public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap) {}

		public void onProgressChanged(int progress) {}
	}

	public static enum RequestType {
		Get, Insert, Update, Delete, Method
	}

	public static enum GsonType {
		Internal,
		ProxibaseService,
		ProxibaseServiceNew
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