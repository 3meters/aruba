package com.georain.ripple.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import com.georain.ripple.controller.Ripple;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.threemeters.sdk.android.core.Entity;

public class RippleService
{
	public static final int		TIMEOUT_SOCKET				= 15000;
	public static final int		TIMEOUT_CONNECTION_RIPPLE	= 9000;		// May be too small for 3G connections
	public static final int		JSON_NOT_INT_PRIMITIVE		= 999999;
	public static final int 	RESULT_OK					= -1;
	public static final int 	RESULT_FAIL					= 0;

	public RippleService() {}

	public String select(Query query, Class objClass) throws ClientProtocolException, IOException
	{
		InputStream stream = execute(query, QueryFormat.Json);
		if (stream == null)
			return null; // Network or service failure

		String jsonString = convertStreamToString(stream);

		// Type type = TypeToken.get(objClass).getType();
		// ArrayList<Object> objArray = convertJsonToObjects(jsonString, type);
		return jsonString;
	}

	public void insert(Object object, String entityName) throws URISyntaxException, ClientProtocolException, IOException, RippleError
	{
		// Resource descriptor that is being targeted with the update
		URI uri = new URI(Ripple.URL_RIPPLESERVICE_ODATA + entityName);

		// Convert object to json
		Gson gson = getGson(GsonType.RippleService);
		String json = gson.toJson(object);
		json = json.replaceFirst("metadata", "__metadata");

		// Date fixup
		json = json.replace("/Date(", "\\/Date(");
		json = json.replace(")/", ")\\/");

		// Setup headers
		HttpPost httpAction = new HttpPost(uri);
		httpAction.addHeader("Accept", "application/json");
		httpAction.addHeader("Content-Type", "application/json");

		// Payload
		httpAction.setEntity(new StringEntity(json));

		// Connection settings
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION_RIPPLE);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET);

		// Execute HTTP Get Request
		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpResponse response = httpClient.execute(httpAction);

		// Check the response status code
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200 && statusCode != 201 && statusCode != 204)
			throw new RippleError(response.getStatusLine().getStatusCode() + response.getStatusLine().getReasonPhrase());
		else
			return;
	}

	public void update(Object object, String uriString) throws URISyntaxException, ClientProtocolException, IOException, RippleError
	{
		// Resource descriptor that is being targeted with the update
		URI uri = new URI(uriString);

		// Convert object to json
		Gson gson = getGson(GsonType.RippleService);
		String json = gson.toJson(object);
		json = json.replaceFirst("metadata", "__metadata");

		// Date fixup
		json = json.replace("/Date(", "\\/Date(");
		json = json.replace(")/", ")\\/");

		// Setup headers
		HttpPut httpPut = new HttpPut(uri);
		httpPut.addHeader("Accept", "application/json");
		httpPut.addHeader("Content-Type", "application/json");

		// Payload
		httpPut.setEntity(new StringEntity(json));

		// Connection settings
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION_RIPPLE);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET);

		// Execute HTTP Get Request
		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpResponse response = httpClient.execute(httpPut);

		// Check the response status code
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200 && statusCode != 201 && statusCode != 204)
			throw new RippleError(response.getStatusLine().getStatusCode() + response.getStatusLine().getReasonPhrase());
		else
			return;
	}

	public void delete(String uriString) throws URISyntaxException, ClientProtocolException, IOException, RippleError
	{
		// Resource descriptor that is being targeted with the update
		URI uri = new URI(uriString);

		// Setup headers
		HttpDelete httpAction = new HttpDelete(uri);
		httpAction.addHeader("Accept", "application/json");
		httpAction.addHeader("Content-Type", "application/json");

		// Connection settings
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION_RIPPLE);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET);

		// Execute HTTP Get Request
		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpResponse response = httpClient.execute(httpAction);

		// Check the response status code
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200 && statusCode != 204)
			throw new RippleError(response.getStatusLine().getStatusCode() + response.getStatusLine().getReasonPhrase());
		else
			return;
	}

	private InputStream execute(Query query, QueryFormat queryFormat) throws ClientProtocolException, IOException
	{
		String url = Ripple.URL_RIPPLESERVICE_ODATA + query.queryString();
		HttpGet httpGet = new HttpGet(url);
		if (queryFormat == QueryFormat.Json)
			httpGet.addHeader("Accept", "application/json");

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION_RIPPLE);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET);

		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

		// Execute HTTP Get Request
		HttpResponse response = httpClient.execute(httpGet);
		if (response.getStatusLine().getStatusCode() != 200)
			return null;

		InputStream content = response.getEntity().getContent();
		if (content == null)
			return null;

		return content;
	}

	public String getString(String url, QueryFormat queryFormat) throws ClientProtocolException, IOException
	{
		InputStream inputStream = get(url, queryFormat);
		if (inputStream == null)
			return "";
		else
			return convertStreamToString(inputStream);
	}

	public InputStream getStream(String url, QueryFormat queryFormat) throws ClientProtocolException, IOException
	{
		InputStream inputStream = get(url, queryFormat);
		if (inputStream == null)
			return null;
		else
			return inputStream;
	}

	private InputStream get(String url, QueryFormat queryFormat) throws ClientProtocolException, IOException
	{
		HttpGet httpGet = new HttpGet(url);
		if (queryFormat == QueryFormat.Json)
			httpGet.addHeader("Accept", "application/json");

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION_RIPPLE);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET);

		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

		// Execute HTTP Get Request
		HttpResponse response = httpClient.execute(httpGet);
		if (response.getStatusLine().getStatusCode() != 200)
			return null;

		InputStream content = response.getEntity().getContent();
		if (content == null)
			return null;
		else
			return content;
	}

	public String post(String methodName, Bundle parameters, QueryFormat queryFormat) throws URISyntaxException, ClientProtocolException, IOException
	{
		// Resource descriptor that is being targeted with the update
		URI uri = new URI(Ripple.URL_RIPPLESERVICE + methodName);

		// Setup headers
		HttpPost httpAction = new HttpPost(uri);
		httpAction.addHeader("Accept", "application/json");
		httpAction.addHeader("Content-Type", "application/json");

		// Payload
		if (parameters.size() != 0)
		{
			String body = "{";
			for (String key : parameters.keySet())
				if (parameters.get(key) != null)
					body += "\"" + key + "\":\"" + parameters.get(key).toString() + "\",";
			body = body.substring(0, body.length() - 1) + "}";
			httpAction.setEntity(new StringEntity(body));
		}

		// Connection settings
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION_RIPPLE);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET);

		// Execute HTTP Request
		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpResponse response = httpClient.execute(httpAction);

		// Check the response status code
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200 && statusCode != 201 && statusCode != 204)
			return "";

		InputStream content = response.getEntity().getContent();
		if (content == null)
			return "";
		else
		{
			String contentAsString = convertStreamToString(content);
			return contentAsString;
		}
	}

	private static String convertStreamToString(InputStream inputStream)
	{
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder stringBuilder = new StringBuilder();

		String line = null;
		try
		{
			while ((line = bufferedReader.readLine()) != null)
				stringBuilder.append(line + "\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				inputStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return stringBuilder.toString();
	}

	public static String convertPointToString(Entity point)
	{
		Gson gson = RippleService.getGson(GsonType.Internal);
		String json = gson.toJson(point);
		return json;
	}

	public static ArrayList<Object> convertJsonToObjects(String jsonString, Class type)
	{
		ArrayList<Object> array = new ArrayList<Object>();
		Gson gson = getGson(GsonType.RippleService);

		// In general, gson deserializer will ignore elements (fields or classes) in the string that
		// do not exist on the object type.
		// Collections should be treated as generic lists on the target object.

		try
		{
			// Fixup: change __metadata to metadata
			jsonString = jsonString.replaceFirst("__metadata", "metadata");
			jsonString = jsonString.replaceFirst("__type", "type");
			jsonString = jsonString.replaceFirst("RippleService.", "");
			
			// New
			
			JsonParser parser = new JsonParser();
			JsonElement jsonElement = parser.parse(jsonString);
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			jsonElement = jsonObject.get("d");
			
			if (jsonElement.isJsonPrimitive())
			{
				JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
				if (primitive.isString())
				{
					array.add(primitive.getAsString());
				}
				else if (primitive.isNumber())
				{
					array.add(primitive.getAsNumber());
				}
				else if (primitive.isBoolean())
				{
					array.add(primitive.getAsBoolean());
				}
			}
			else if (jsonElement.isJsonArray())
			{
				JsonArray jsonArray = jsonElement.getAsJsonArray();
				for (int i = 0; i < jsonArray.size(); i++)
				{
					JsonObject jsonObjectNew = (JsonObject) jsonArray.get(i);
					array.add(gson.fromJson(jsonObjectNew.toString(), type));
				}
			}
			else if (jsonElement.isJsonObject())
			{
				Object obj = gson.fromJson(jsonElement, type);
				array.add(obj);
			}
			
			// Old
			
//			JSONObject jsonObject = new JSONObject(jsonString);
//
//			int value = jsonObject.optInt("d", JSON_NOT_INT_PRIMITIVE);
//			if (value != JSON_NOT_INT_PRIMITIVE)
//			{
//				array.add(value);
//				return array;
//			}
//			
//			JSONArray jsonArray = jsonObject.optJSONArray("d");
//			if (jsonArray == null)
//			{
//				if (jsonObject.optJSONObject("d") != null)
//				{
//					jsonObject = jsonObject.getJSONObject("d");
//					// Object obj = gson.fromJson(jsonObject.toString(), type);
//					Object obj = gson.fromJson(jsonObject.toString(), type);
//					array.add(obj);
//				}
//			}
//			else
//			{
//				for (int i = 0; i < jsonArray.length(); i++)
//				{
//					JSONObject jsonObjectNew = jsonArray.getJSONObject(i);
//					array.add(gson.fromJson(jsonObjectNew.toString(), type));
//				}
//			}
			return array;
		}
		catch (JsonParseException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static Gson getGson(GsonType gsonType)
	{
		GsonBuilder gsonb = new GsonBuilder();

		// Converting objects to/from json for passing between the client and the service
		// we need to apply some additional behavior on top of the defaults
		if (gsonType == GsonType.RippleService)
		{
			gsonb.excludeFieldsWithoutExposeAnnotation();
			gsonb.setPrettyPrinting(); // TODO: remove this later
			gsonb.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE);
			gsonb.registerTypeAdapter(Date.class, new DateDeserializer());
		}
		Gson gson = gsonb.create();
		return gson;
	}

	public enum GsonType
	{
		Internal, RippleService
	}

	public static boolean isConnectedToNetwork(Context context)
	{
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null)
			return false;

		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo == null)
			return false;

		if (networkInfo.isAvailable() && networkInfo.isConnectedOrConnecting())
			return true;

		return false;
	}

	public static class DateDeserializer implements JsonDeserializer<Date>
	{
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			String JSONDateToMilliseconds = "\\/(Date\\((.*?)(\\+.*)?\\))\\/";
			Pattern pattern = Pattern.compile(JSONDateToMilliseconds);
			Matcher matcher = pattern.matcher(json.getAsJsonPrimitive().getAsString());
			String result = matcher.replaceAll("$2");

			return new Date(new Long(result));
		}
	}

	public static class DateSerializer implements JsonSerializer<Object>
	{
		public JsonElement serialize(Date date, Type typeOfT, JsonSerializationContext context)
		{
			return new JsonPrimitive("/Date(" + date.getTime() + ")/");
		}

		public JsonElement serialize(Object arg0, Type arg1, JsonSerializationContext arg2)
		{

			Date date = (Date) arg0;
			return new JsonPrimitive("/Date(" + date.getTime() + ")/");
		}
	}

	public static Date jsonDateFixup(String jsonDate)
	{
		jsonDate = jsonDate.replace("/Date(", "").replace(")/", "");
		Date jsonDateFixed = new Date(Long.parseLong(jsonDate));
		return jsonDateFixed;
	}

	public enum QueryFormat
	{
		Json, Xml
	}

	public enum UrlEncodingType
	{
		All, SpacesOnly, None
	}
}