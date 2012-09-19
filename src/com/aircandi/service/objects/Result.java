package com.aircandi.service.objects;

import java.util.HashMap;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
public class Result {

	@Expose
	public String	id;

	public Result() {}
	
	public static Result setFromPropertiesFromMap(Result result, HashMap map) {
		result.id = (String) map.get("_id");

		return result;
	}
}