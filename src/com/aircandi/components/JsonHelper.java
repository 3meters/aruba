package com.aircandi.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("ucd")
public class JsonHelper {
	public static Object toJSON(Object object) throws JSONException {
		if (object instanceof Map) {
			final JSONObject json = new JSONObject();
			final Map map = (Map) object;
			for (Object key : map.keySet()) {
				json.put(key.toString(), toJSON(map.get(key)));
			}
			return json;
		}
		else if (object instanceof Iterable) {
			final JSONArray json = new JSONArray();
			for (Object value : ((Iterable) object)) {
				json.put(value);
			}
			return json;
		}
		else {
			return object;
		}
	}

	public static boolean isEmptyObject(JSONObject object) {
		return object.names() == null;
	}

	public static Map<String, Object> getMap(JSONObject object, String key) throws JSONException {
		return toMap(object.getJSONObject(key));
	}

	private static Map<String, Object> toMap(JSONObject object) throws JSONException {
		final Map<String, Object> map = new HashMap(50);
		final Iterator keys = object.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			map.put(key, fromJson(object.get(key)));
		}
		return map;
	}

	public static List toList(JSONArray array) throws JSONException {
		final List list = new ArrayList(); // $codepro.audit.disable defineInitialCapacity
		for (int i = 0; i < array.length(); i++) {
			list.add(fromJson(array.get(i)));
		}
		return list;
	}

	private static Object fromJson(Object json) throws JSONException {
		if (json.equals(JSONObject.NULL)) {
			return null;
		}
		else if (json instanceof JSONObject) {
			return toMap((JSONObject) json);
		}
		else if (json instanceof JSONArray) {
			return toList((JSONArray) json);
		}
		else {
			return json;
		}
	}
}