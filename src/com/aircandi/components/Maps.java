package com.aircandi.components;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ucd")
public class Maps {
	public static final <K, V> HashMap<K, V> asHashMap(K[] keys, V[] values) {
		HashMap<K, V> result = new HashMap<K, V>();
		if (keys.length != values.length) {
			throw new IllegalArgumentException();
		}

		for (int i = 0; i < keys.length; i++) {
			result.put(keys[i], values[i]);
		}
		return result;
	}

	public static final <K, V> HashMap<K, V> asHashMap(K key, V value) {
		HashMap<K, V> result = new HashMap<K, V>();
		result.put(key, value);
		return result;
	}

	public static final <K, V> Map<K, V> asMap(K[] keys, V[] values) {
		return asHashMap(keys, values);
	}

	public static final <K, V> Map<K, V> asMap(K key, V value) {
		return asHashMap(key, value);
	}
}
