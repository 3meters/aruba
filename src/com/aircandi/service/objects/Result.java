package com.aircandi.service.objects;

import java.util.Map;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Result {

	@Expose
	public String	id;

	public Result() {}
	
	public static Result setPropertiesFromMap(Result result, Map map) {
		result.id = (String) map.get("_id");

		return result;
	}
}