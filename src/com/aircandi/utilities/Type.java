package com.aircandi.utilities;

public class Type {

	public static String emptyAsNull(String stringValue) {
		if ("".equals(stringValue)) {
			return null;
		}
		return stringValue;
	}

	public static Boolean isTrue(Boolean value) {
		return (value != null && value) ? true : false;
	}

	public static Boolean isFalse(Boolean value) {
		return (value == null || !value) ? true : false;
	}
}