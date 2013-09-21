package com.aircandi.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
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

import android.text.TextUtils;

import com.aircandi.Aircandi;
import com.aircandi.BuildConfig;
import com.aircandi.ServiceConstants;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.Stopwatch;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Json;

public class HttpClientConnection extends BaseConnection {

	private final DefaultHttpClient	mHttpClient;
	private final HttpParams		mHttpParams;

	private static final String		HEADER_ACCEPT_ENCODING	= "Accept-Encoding";
	private static final String		ENCODING_GZIP			= "gzip";

	public HttpClientConnection() {
		/* Connection settings */
		mHttpParams = createHttpParams();
		mHttpClient = createHttpClient();
	}

	@Override
	public ServiceResponse request(ServiceRequest serviceRequest, Stopwatch stopwatch) {

		ServiceResponse serviceResponse = new ServiceResponse();
		serviceResponse.activityName = serviceRequest.getActivityName();
		
		try {
			
			AirHttpRequest request = buildHttpRequest(serviceRequest, stopwatch);
			HttpRequestBase httpRequest = null;

			if (request.requestType == RequestType.GET) {
				httpRequest = new HttpGet();
			}
			else if (request.requestType == RequestType.INSERT) {
				httpRequest = new HttpPost();
			}
			else if (serviceRequest.getRequestType() == RequestType.UPDATE) {
				httpRequest = new HttpPost();
			}
			else if (serviceRequest.getRequestType() == RequestType.DELETE) {
				httpRequest = new HttpDelete();
			}
			else if (serviceRequest.getRequestType() == RequestType.METHOD) {
				httpRequest = new HttpPost();
			}

			httpRequest.setURI(new URI(request.uri));

			if (!TextUtils.isEmpty(request.requestBody)) {
				((HttpEntityEnclosingRequestBase)httpRequest).setEntity(new StringEntity(request.requestBody, HTTP.UTF_8));
			}

			HttpConnectionParams.setSoTimeout(mHttpParams, ServiceConstants.TIMEOUT_SOCKET_QUERIES);

			for (AirHttpRequest.Header header : request.headers) {
				httpRequest.addHeader(header.key, header.value);
			}

			HttpResponse httpResponse = null;

			long startTime = System.nanoTime();

			if (stopwatch != null) {
				stopwatch.segmentTime("Http service: connection validation completed");
			}

			httpResponse = mHttpClient.execute(httpRequest);

			if (stopwatch != null) {
				stopwatch.segmentTime("Http service: request execute completed");
			}

			serviceResponse.statusCode = httpResponse.getStatusLine().getStatusCode();
			serviceResponse.statusMessage = httpResponse.getStatusLine().getReasonPhrase();

			if ((Integer) serviceResponse.statusCode / 100 == HttpURLConnection.HTTP_OK / 100) {
				/*
				 * Any 2.XX status code is considered success.
				 */
				long bytesDownloaded = (httpResponse.getEntity() != null) ? httpResponse.getEntity().getContentLength() : 0;
				logDownload(startTime, System.nanoTime() - startTime, bytesDownloaded, httpRequest.getURI().toString());

				Object response = handleResponse(httpRequest, httpResponse, serviceRequest.getResponseFormat(), serviceRequest.getRequestListener());

				if (stopwatch != null) {
					stopwatch.segmentTime("Http service: response content captured");
				}

				/*
				 * Check for valid client version even if the call was successful
				 */
				if (serviceRequest.getResponseFormat() == ResponseFormat.JSON && !serviceRequest.getIgnoreResponseData()) {
					/*
					 * We think anything json is coming from the Aircandi service (except Bing)
					 */
					ServiceData serviceData = (ServiceData) Json.jsonToObject((String) response, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);

					if (stopwatch != null) {
						stopwatch.segmentTime("Http service: response content json ("
								+ String.valueOf(((String) response).length()) + " bytes) decoded to object");
					}

					Integer clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);
					if (serviceData != null && serviceData.androidMinimumVersion != null) {
						if (serviceData.androidMinimumVersion.intValue() > clientVersionCode) {
							return new ServiceResponse(ResponseCode.FAILED, null, new ClientVersionException());
						}
					}
				}

				serviceResponse.data = response;
				return serviceResponse;
			}
			else {
				/*
				 * We got a non-success http status code so break it down and
				 * decide if makes sense to retry.
				 */
				String responseContent = convertStreamToString(httpResponse.getEntity().getContent());
				Logger.d(this, responseContent);

				if (serviceRequest.getResponseFormat() == ResponseFormat.JSON) {
					/*
					 * We think anything json is coming from the Aircandi service.
					 */
					ServiceData serviceData = (ServiceData) Json.jsonToObject(responseContent, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
					Integer clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);
					if (serviceData != null) {
						if (serviceData.androidMinimumVersion != null && serviceData.androidMinimumVersion.intValue() > clientVersionCode) {
							return new ServiceResponse(ResponseCode.FAILED, null, new ClientVersionException());
						}
						else if (serviceData.error != null && serviceData.error.code != null) {
							serviceResponse.statusCodeService = serviceData.error.code.floatValue();
						}
					}
				}
				
				serviceResponse.responseCode = ResponseCode.FAILED;
				return serviceResponse;
			}
		}
		catch (UnsupportedEncodingException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
		catch (URISyntaxException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
		catch (IOException exception) {
			return new ServiceResponse(ResponseCode.FAILED, null, exception);
		}
	}

	private Object handleResponse(HttpRequestBase httpAction, HttpResponse response, ResponseFormat responseFormat, RequestListener listener)
			throws IOException {

		/* Looks like success so process the response */
		InputStream inputStream = null;
		if (response.getEntity() != null) {

			try {
				if (responseFormat == ResponseFormat.BYTES) {
					HttpEntity httpEntity = response.getEntity();
					Header encoding = httpEntity.getContentEncoding();

					if (encoding != null) {
						for (HeaderElement element : encoding.getElements()) {
							if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
								inputStream = new BufferedInputStream(new GZIPInputStream(httpEntity.getContent()));
								Logger.v(this, "Content encoding: gzip");
								break;
							}
						}
					}

					if (inputStream == null) {
						inputStream = new BufferedInputStream(httpEntity.getContent());
						Logger.v(this, "Content encoding: none");
					}

					final byte[] byteArray = getBytes(inputStream, httpEntity.getContentLength(), listener);
					return byteArray;
				}
				else {
					BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(response.getEntity());
					Header encoding = bufferedHttpEntity.getContentEncoding();

					if (encoding != null) {
						for (HeaderElement element : encoding.getElements()) {
							if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
								inputStream = new GZIPInputStream(bufferedHttpEntity.getContent());
								Logger.v(this, "Content encoding: gzip");
								break;
							}
						}
					}

					if (inputStream == null) {
						inputStream = bufferedHttpEntity.getContent();
						Logger.v(this, "Content encoding: none");
					}

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
			}
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------------------------------	

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

	private static byte[] getBytes(InputStream inputStream, Long lengthOfFile, RequestListener listener) throws IOException {

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
					if (listener != null && lengthOfFile != null) {
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

	private void logDownload(long startTime, long elapsedTime, long bytesDownloaded, String subject) {
		final float downloadTimeMills = elapsedTime / 1000000;
		final float bitspersecond = ((bytesDownloaded << 3) * (1000 / downloadTimeMills)) / 1000;
		Logger.v(this, subject + ": Downloaded "
				+ String.valueOf(bytesDownloaded) + " bytes @ "
				+ String.valueOf(Math.round(bitspersecond)) + " kbps: "
				+ String.valueOf(downloadTimeMills) + "ms");
	}

	// --------------------------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------------------------	

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
		 * AllowAllHostnameVerifier doesn't verify host names contained in SSL certificate. It should not be set in
		 * production environment. It may allow man in middle attack. Other host name verifiers for specific needs
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
			public void process(org.apache.http.HttpRequest request, HttpContext context) throws HttpException, IOException {

				/* Some sites refuse requests without a set user agent so we fake one */
				request.addHeader("User-Agent", "Mozilla/5.0");

				/* We can process gzip if the service wants to send it */
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});
		return httpClient;
	}

}
