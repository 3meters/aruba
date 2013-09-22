package com.aircandi.service;

import java.util.ArrayList;
import java.util.List;

import com.aircandi.ServiceConstants;
import com.aircandi.components.Stopwatch;
import com.aircandi.service.AirHttpRequest.Header;
import com.aircandi.service.ServiceRequest.AuthType;

public abstract class BaseConnection implements IConnection {

	@Override
	public abstract ServiceResponse request(ServiceRequest serviceRequest, Stopwatch stopwatch);

	protected static AirHttpRequest buildHttpRequest(final ServiceRequest serviceRequest, final Stopwatch stopwatch) {

		AirHttpRequest airHttpRequest = new AirHttpRequest();
		airHttpRequest.uri = serviceRequest.getUriWithQuery();
		airHttpRequest.responseFormat = serviceRequest.getResponseFormat();
		addHeaders(airHttpRequest, serviceRequest);

		/* Construct the request */
		airHttpRequest.requestType = serviceRequest.getRequestType();
		if (serviceRequest.getRequestType() == RequestType.INSERT) {
			if (serviceRequest.getRequestBody() != null) {
				if (serviceRequest.getUseSecret()) {
					airHttpRequest.requestBody = "{\"data\":" + serviceRequest.getRequestBody() + ", \"secret\":\"" + ServiceConstants.INSERT_USER_SECRET
							+ "\"}";
				}
				else {
					airHttpRequest.requestBody = "{\"data\":" + serviceRequest.getRequestBody() + "}";
				}
			}
		}
		else if (serviceRequest.getRequestType() == RequestType.UPDATE) {
			if (serviceRequest.getRequestBody() != null) {
				airHttpRequest.requestBody = "{\"data\":" + serviceRequest.getRequestBody() + "}";
			}
		}
		else if (serviceRequest.getRequestType() == RequestType.METHOD) {

			/* Method parameters */
			StringBuilder jsonBody = new StringBuilder(5000);
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
				airHttpRequest.requestBody = jsonBody.toString();
			}
		}

		return airHttpRequest;
	}

	private static void addHeaders(AirHttpRequest airHttpRequest, ServiceRequest serviceRequest) {
		if (serviceRequest.getRequestType() != RequestType.GET) {
			airHttpRequest.headers.add(new Header("Content-Type", "application/json"));
		}
		if (serviceRequest.getResponseFormat() == ResponseFormat.JSON) {
			airHttpRequest.headers.add(new Header("Accept", "application/json"));
		}
		if (serviceRequest.getResponseFormat() == ResponseFormat.BYTES) {
			airHttpRequest.headers.add(new Header("Accept", "image/jpeg"));
			airHttpRequest.headers.add(new Header("Accept", "image/png"));
			airHttpRequest.headers.add(new Header("Accept", "image/gif"));
		}
		if (serviceRequest.getAuthType() == AuthType.BASIC) {
			airHttpRequest.headers.add(new Header("Authorization", "Basic " + serviceRequest.getPasswordBase64()));
		}
	}
}
